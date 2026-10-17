#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Coset-LDE Hardware Accelerator
# Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
# Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
# File: tools/verify.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""Check independent references, RTL equivalence, protocol, reset and negative controls."""
import argparse
import json
import re
from pathlib import Path
from emit_sv import ROOT, OP, arguments, generate, run
from metrics import records, performance

def normalized(path):
    text = re.sub(r'/\*.*?\*/|//[^\n]*', '', path.read_text(), flags=re.S)
    return re.sub(r'\s+', '', text)

def main():
    parser=argparse.ArgumentParser(description=__doc__); arguments(parser)
    parser.add_argument('--jobs', type=int, default=4)
    parser.add_argument('--ideal-only', action='store_true')
    args=parser.parse_args()
    out, selected=generate(args.out,args.maxlog,args.logn,args.logb,args.tasks,args.mode)
    report={'status':1,'config':{'maxLogM':args.maxlog,'logN':args.logn,'logB':args.logb,'tasks':args.tasks,'mode':args.mode},'tests':{},'ideal_interfaces':True}
    try:
        if args.maxlog==18:
            bundled=ROOT/'rtl/v1.1.0/n65536-b4-lane1'
            assert {p.name for p in selected}=={p.name for p in bundled.glob('*.sv')}
            assert all(normalized(p)==normalized(bundled/p.name) for p in selected), 'Bundled product differs from generated product'
            run(['verilator','--lint-only','--assert','--top-module','GoldilocksCosetLde','-f',bundled/'files.f'],out,'bundled-lint')
            report['bundled_product_equivalent']=True
        run(['verilator','--lint-only','--assert','--top-module','GoldilocksCosetLde','-f',out/'rtl/product.f'],out,'product-lint')
        obj=out/'obj'
        run(['verilator','-O3','--assert','--cc','--exe','--build','-j',args.jobs,'-DENABLE_INITIAL_MEM_','--top-module','CosetLdeSim','-Mdir',obj,'-CFLAGS','-O2 -std=c++17','-f',out/'rtl/simulation.f',OP/'sim/driver.cpp'],out,'verilator')
        variants=[(0,'ideal')] if args.ideal_only else [(0,'ideal'),(1,'protocol'),(2,'reset'),(3,'negative')]
        for variant,label in variants:
            run([obj/'VCosetLdeSim',variant],out,label,1 if variant==3 else 0)
            text=(out/(label+'.log')).read_text();results=records(text,'SIM_DONE'); assert len(results)==1
            result=results[0]
            assert result['checked']==args.tasks*(1<<(args.logn+args.logb))
            assert len(records(text,'BAD_PARAM_PASS'))==(12 if variant==2 else 6)
            assert len(records(text,'BAD_FRAME_PASS'))==(6 if variant==2 else 3)
            if variant==1: assert len(records(text,'BAD_TABLE_PASS'))==2
            assert result['mismatches']==(1 if variant==3 else 0) and result['pass']==(0 if variant==3 else 1)
            if variant==2: assert result['reset']==1
            report['tests'][label]=result
        report['performance']=performance((out/'ideal.log').read_text(),args.logn,args.logb,args.tasks)
        report['status']=0
    finally:
        (out/'results.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n')
    print(json.dumps({'status':report['status'],'results':str((out/'results.json').relative_to(ROOT))}))

if __name__=='__main__':
    main()
