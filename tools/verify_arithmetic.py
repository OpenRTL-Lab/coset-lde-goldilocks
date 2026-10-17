#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Coset-LDE Hardware Accelerator
# Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
# Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
# File: tools/verify_arithmetic.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""Verify field multiplication and forward/inverse butterfly arithmetic independently."""
import argparse
import json
import os
from pathlib import Path
import re
import shlex
from emit_sv import ROOT, OP, run

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--out',required=True,type=Path);p.add_argument('--jobs',type=int,default=4)
    a=p.parse_args();out=a.out.resolve()
    if not out.is_relative_to(ROOT/'build') or out.exists():raise ValueError('Use a new directory in build/')
    out.mkdir(parents=True)
    run(['cc','-std=c11','-O2','-Wall','-Wextra',OP/'cmodel/arithmetic_vectors.c','-o',out/'reference'],out,'c-compile')
    run([out/'reference',(out/'vectors.hex').relative_to(ROOT)],out,'c-reference')
    run(shlex.split(os.environ.get('MILL','mill'))+['-i','cosetLde.tool.runMain','zk.lde.EmitArithmetic',str((out/'rtl').relative_to(ROOT)),str((out/'vectors.hex').relative_to(ROOT))],out,'emit')
    sv=sorted((out/'rtl').rglob('*.sv'))
    listing=out/'simulation.f'
    listing.write_text(''.join('+incdir+'+str(p.relative_to(ROOT))+'\n' for p in sorted({p.parent for p in sv}))+''.join(str(p.relative_to(ROOT))+'\n' for p in sv))
    run(['verilator','-O3','--assert','--cc','--exe','--build','-j',a.jobs,'-DENABLE_INITIAL_MEM_','--top-module','LdeArithmeticSim','-Mdir',out/'obj','-CFLAGS','-O2 -std=c++17','-f',listing,OP/'sim/arithmetic_driver.cpp'],out,'verilator')
    binary=out/'obj/VLdeArithmeticSim'
    run([binary],out,'arithmetic');run([binary,'negative'],out,'negative',1)
    assert re.search(r'ARITHMETIC_PASS pass=1 checked=1024',(out/'arithmetic.log').read_text())
    assert re.search(r'ARITHMETIC_PASS pass=0 checked=1024',(out/'negative.log').read_text())
    result={'status':0,'vectors':1024,'negative_control_detected':True}
    (out/'results.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))

if __name__=='__main__':main()
