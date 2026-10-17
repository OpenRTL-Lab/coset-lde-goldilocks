#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Coset-LDE Hardware Accelerator
# Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
# Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
# File: operators/coset-lde/tools/check_reference.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""用Python整数直接逆DFT与Horner独立核对C参考小尺寸，非产品/参考FFT同构。"""
import argparse,json
from pathlib import Path
OP=Path(__file__).resolve().parents[1]
def main():
    ap=argparse.ArgumentParser();ap.add_argument('vectors',type=Path);ap.add_argument('ln',type=int);ap.add_argument('lb',type=int);ap.add_argument('mode',type=int)
    a=ap.parse_args();params=json.loads((OP/'parameters.json').read_text());p=int(params['prime']);base=int(params['omega32_hex'],16)
    assert pow(base,1<<32,p)==1 and pow(base,1<<31,p)!=1
    assert pow(base,1<<14,p)==int(params['omega18_hex'],16)
    n=1<<a.ln;m=n<<a.lb;assert m<=1024
    def words(file):
        result=[]
        for line in (a.vectors/file).read_text().splitlines():
            v=int(line,16);result.extend([v&((1<<64)-1),v>>64])
        return result
    inp=words('input.hex');expected=words('expected.hex');tasks=len(inp)//n;assert len(expected)==tasks*m
    wn=pow(base,1<<(32-a.ln),p);wm=pow(base,1<<(32-a.ln-a.lb),p)
    assert pow(wm,1<<a.lb,p)==wn
    checked=0
    for t in range(tasks):
        values=inp[t*n:(t+1)*n];mode=(t&1) if a.mode==2 else a.mode
        if mode==0:
            inverse=pow(wn,p-2,p);scale=pow(n,p-2,p)
            c=[sum(v*pow(inverse,j*k,p) for k,v in enumerate(values))*scale%p for j in range(n)]
        else:c=values
        g=[1,7,p-1,19][t&3]
        x=g
        for k in range(m):
            y=0
            for coefficient in reversed(c):y=(y*x+coefficient)%p
            assert y==expected[t*m+k],(t,k,y,expected[t*m+k]);checked+=1;x=x*wm%p
    print(json.dumps({'status':0,'tasks':tasks,'direct_output_words':checked,'method':'Python整数直接逆DFT+Horner'}))
if __name__=='__main__':main()
