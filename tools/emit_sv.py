#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Coset-LDE Hardware Accelerator
# Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
# Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
# File: tools/emit_sv.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""Generate product RTL and a separate reference-driven verification environment."""
from source_headers import stamp_generated
import argparse
import json
import os
from pathlib import Path
import re
import shlex
import subprocess

ROOT = Path(__file__).resolve().parents[1]
OP = ROOT / 'operators/coset-lde'

def run(command, out, label, expected=0):
    with (out / (label + '.log')).open('w') as log:
        result = subprocess.run([str(x) for x in command], cwd=ROOT,
                                stdout=log, stderr=subprocess.STDOUT)
    if result.returncode != expected:
        raise RuntimeError(f'{label} failed: see {out.relative_to(ROOT)}/{label}.log')

def closure(rtl):
    stamp_generated(rtl)
    definitions, texts = {}, {}
    for path in sorted(rtl.glob('*.sv')):
        code = re.sub(r'/\*.*?\*/|//[^\n]*', '', path.read_text(), flags=re.S)
        texts[path] = code
        for name in re.findall(r'\bmodule\s+(\w+)', code):
            assert name not in definitions
            definitions[name] = path
    pending, selected = ['GoldilocksCosetLde'], set()
    while pending:
        path = definitions[pending.pop()]
        if path in selected:
            continue
        selected.add(path)
        pending.extend(set(re.findall(r'^[ \t]*(\w+)\b[ \t]+(?:#\s*\([^;]*?\)\s*)?\w+\s*\(', texts[path], re.M | re.S)) & definitions.keys())
    for path in selected:
        code = re.sub(r'"(?:[^"\\]|\\.)*"', '""', texts[path])
        assert not re.search(r'\$readmem|%|\b(?:DSP48\w*|RAMB\w*|URAM\w*|BUFG\w*|IOBUF)\b', code)
        assert 'Sim' not in texts[path] and 'EnvRom' not in texts[path]
    (rtl / 'product.f').write_text(''.join(str(p.relative_to(ROOT)) + '\n' for p in sorted(selected)))
    simulation = sorted(rtl.rglob('*.sv'))
    (rtl / 'simulation.f').write_text(''.join('+incdir+' + str(p.relative_to(ROOT)) + '\n' for p in sorted({p.parent for p in simulation})) + ''.join(str(p.relative_to(ROOT)) + '\n' for p in simulation))
    return selected

def generate(out, maxlog, logn, logb, tasks, mode):
    out = out.resolve()
    if not out.is_relative_to(ROOT / 'build') or out.exists():
        raise ValueError('Use a new output directory inside build/')
    if not (4 <= logn <= maxlog <= 18 and 0 <= logb <= maxlog-logn and tasks >= 4 and mode in (0,1,2)):
        raise ValueError('Invalid dimensions, mode, or task count')
    out.mkdir(parents=True)
    vectors = out / 'vectors'; vectors.mkdir()
    run(['cc', '-std=c11', '-O2', '-Wall', '-Wextra', OP/'cmodel/reference.c', '-o', out/'reference'], out, 'c-compile')
    run([out/'reference', vectors.relative_to(ROOT), maxlog, logn, logb, tasks, mode], out, 'c-reference')
    if logn + logb <= 10:
        run(['python3', OP/'tools/check_reference.py', vectors.relative_to(ROOT), logn, logb, mode], out, 'python-direct')
    command = shlex.split(os.environ.get('MILL', 'mill'))
    run(command + ['-i', 'cosetLde.tool.runMain', 'zk.lde.Emit', str((out/'rtl').relative_to(ROOT)), str(maxlog), str(logn), str(logb), str(tasks), str(mode), str(vectors.relative_to(ROOT))], out, 'emit')
    selected = closure(out / 'rtl')
    return out, selected

def arguments(parser):
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--maxlog', type=int, default=18)
    parser.add_argument('--logn', type=int, default=4)
    parser.add_argument('--logb', type=int, default=2)
    parser.add_argument('--tasks', type=int, default=8)
    parser.add_argument('--mode', type=int, default=2, choices=(0,1,2))

def main():
    parser = argparse.ArgumentParser(description=__doc__); arguments(parser)
    args = parser.parse_args()
    out, selected = generate(args.out, args.maxlog, args.logn, args.logb, args.tasks, args.mode)
    print(json.dumps({'product_modules': len(selected), 'output': str(out.relative_to(ROOT))}))

if __name__ == '__main__':
    main()
