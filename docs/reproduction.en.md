<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/reproduction.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Reproduction guide

[简体中文](reproduction.md)

This flow generates inputs and expected outputs with an independent C reference, regenerates SV from Chisel, checks the bundled product circuit, and compares every output under ideal, backpressure, reset, and negative-control scenarios. Independent arithmetic verification and full performance tests for both modes are also provided.

## Obtain the release snapshot

[Release v1.1.0](https://github.com/OpenRTL-Lab/coset-lde-goldilocks/releases/tag/v1.1.0)

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/coset-lde-goldilocks.git
cd coset-lde-goldilocks
```

The version tag identifies the current source tree, including source notices and documentation. Release downloads provide hardware sources and bundled RTL for this version. Archive annotations and documentation can differ from the online tree. For RTL integration, use the product file list rather than verification environments; use the verification and synthesis documents for supported configurations and measurement conditions.

## 1. Preparation

Install tools and configure PATH using [Toolchain guide](toolchain.en.md), then enter the repository root. Run all commands below from that root. Outputs must use a new directory under`build/`; select a different name when repeating a run. Chisel resolves pinned dependencies on its first run and can reuse caches afterward.

Command parameters use `--maxlog h` for maximum hardware output length 2^h, `--logn n` for N=2^n test inputs, and `--logb b` for expansion B=2^b (M=N*B outputs). `--tasks` is the number of verification tasks. `--mode 0/1` selects evaluation/coefficient test input; `--mode 2` alternates those two legal task modes in the environment and is not a hardware task mode. `--jobs` controls compilation parallelism only.

## 2. Complete small-task verification

The following command generates the release capacity and checks8 alternating-mode tasks with 16 inputs and 4× extension. It compares all 17 generated product SV files with the bundled files after removing comments/whitespace, lints both product lists, and executes all four test variants:

```sh
python3 tools/verify.py --out build/verify --jobs 8
```

Defaults are`maxlog=18, logn=4, logb=2, tasks=8, mode=2`. The C reference uses independent radix-2 DIT. For output sizes up to 1024, Python checks the reference with direct inverse DFT and Horner evaluation. Each simulation compares512 outputs. Ideal, protocol, and reset tests require zero mismatches. The negative control flips one expected value and must detect exactly one mismatch; a simulator exit code of 1 is expected, while the complete verification script exits0.

Protocol testing checks task/data/table errors, output/completion backpressure, and payload stability. Reset testing interrupts a task, reloads tables, and successfully completes fresh tasks. References cover zero, constants, maximum canonical values, a monomial, and deterministic pseudorandom data; shifts cycle through 1,7,$p-1$,19.

## 3. Independent arithmetic verification

The following command checks field multiplication and forward/inverse butterflies using 1024 boundary and pseudorandom vectors, including an incorrect-reference negative control:

```sh
python3 tools/verify_arithmetic.py --out build/arithmetic --jobs 8
```

## 4. Circuit generation only

To generate the product and separate verification environment without compiling the simulator, run:

```sh
python3 tools/emit_sv.py --out build/emit --maxlog 18 --logn 16 --logb 2 --tasks 8 --mode 0
```

`build/emit/rtl/product.f` lists only`GoldilocksCosetLde` and its dependencies. `simulation.f` includes`CosetLdeSim`, reference ROMs, and bound verification layers for simulation. Product memories are not preloaded and do not contain reference ROMs or performance counters. `maxlog` selects generated capacity; `logn/logb` select test dimensions; `tasks/mode` configure the verification environment without changing product capacity.

## 5. Performance reproduction

The following command continuously executes32 evaluation-input tasks with ideal interfaces, compares all 8388608 outputs, and measures task latency and steady throughput:

```sh
python3 tools/verify.py --out build/performance-eval --logn 16 --logb 2 --tasks 32 --mode 0 --ideal-only --jobs 8
```

The following command tests32 coefficient-input tasks at the same dimensions:

```sh
python3 tools/verify.py --out build/performance-coeff --logn 16 --logb 2 --tasks 32 --mode 1 --ideal-only --jobs 8
```

Each output directory contains`results.json` with`status=0`, test results, task timestamps, utilization, phases, and memory counts. `ideal.log` contains`RESULT/METRIC/MEMORY/PHASE` records; `emit.log` and`verilator.log` capture generation and build diagnostics. [Performance metrics](performance.en.md) defines expected values. `cycles` is the complete simulation count, including initialization and error checks, not single-task latency.

## 6. Configuration changes

The following command verifies a smaller capacity with 1024 inputs and 4× extension, including all four simulation variants:

```sh
python3 tools/verify.py --out build/size1024 --maxlog 12 --logn 10 --logb 2 --jobs 8
```

Generated capacity ranges from 4 to 18; task dimensions must satisfy [Mathematical model](mathematical-model.en.md). `mode=0/1` fixes the test mode;`mode=2` alternates modes in verification, while actual task fields accept only0/1. Success summaries and`results.json` are reproduction checkpoints. See [Native interface](interface.en.md) for integration details.
