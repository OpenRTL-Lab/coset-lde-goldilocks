<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/performance.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Performance metrics

[简体中文](performance.md)

Both modes of the 65536-input,4×-extension configuration pass full output comparison for 32 tasks, delivering152.512 or 181.577 tasks/s when converted at 500 MHz. This document separately defines task latency, measured steady throughput, and valid-operation utilization, with reproducible methodology and detailed results.

One task accepts N=2^logN elements and returns M=N*B elements for B=2^logB. Evaluation-input mode 0 first recovers coefficients with INTT; coefficient-input mode 1 skips INTT. Both then scale coefficients by coset powers, pad, and perform the M-point NTT. Frequency conversions below are not board or silicon measurements.

## Configuration and definitions

Generated capacity is`maxLogM=18`; tasks use`logN=16, logB=2`, so $N=65536$ and $M=262144$, with one butterfly lane. Forward and inverse tables are loaded once before timing tasks. Data is continuously supplied when accepted; output and completion are always ready. Storage uses the actual one-cycle synchronous-read RTL model, without extra system bandwidth or latency constraints.

Let $A_i$ be the task-acceptance edge, $O_i$ the final output-acceptance edge, and $C_i$ the completion-acceptance edge. Latency is $L_i=C_i-A_i$, including input loading, arithmetic, power-chain seeding, padding, synchronous reads, pipeline fill/drain, output, and completion.

Each mode submits32 continuous tasks. The steady window spans the completion edges of tasks 2 through 30, yielding28 intervals. Throughput is $T=28/(C_{30}-C_2)$ in tasks/cycle. Task initiation intervals are independently measured between actual acceptance edges and checked against completion intervals, rather than inferred from inverse latency. Conversion uses $fT$ tasks/s and $L/f$ seconds, with $f=500\times10^6$ Hz from the passing frequency in [Synthesis results](synthesis-summary.en.md).

## Latency and throughput

| Metric | Evaluation-input mode 0 | Coefficient-input mode 1 |
| --- | ---: | ---: |
| Tasks / compared elements | 32 / 8388608 | 32 / 8388608 |
| Minimum / maximum latency (cycles) | 3278429 / 3278429 | 2753645 / 2753645 |
| Latency at 500 MHz (ms) | 6.556858 | 5.507290 |
| Steady window (cycles) | 91796040 | 77102088 |
| Measured task initiation interval | 3278430 | 2753646 |
| Throughput (tasks/cycle) | 0.0000003050240511 | 0.0000003631548863 |
| Throughput at 500 MHz (tasks/s) | 152.512026 | 181.577443 |
| Input throughput (field elements/cycle) | 0.0199900562 | 0.0237997186 |
| Output throughput (field elements/cycle) | 0.0799602249 | 0.0951988745 |
| Output mismatches | 0 | 0 |

Arithmetic II=1 differs from the task initiation interval. One butterfly pipeline sequentially processes the stages of each task without inter-task overlap. Coefficient mode uses available coefficients to enter scaling and extension directly.

## Arithmetic units and utilization

The main multiplier comprises four parallel32×32 partial products, segmented addition, and Goldilocks reduction with 18-cycle latency. The butterfly adds modular sum, difference, and inverse normalization with 28-cycle latency. Valid issue counts useful work; invalid-cycle register switching is not counted as useful computation. Pipeline occupancy is measured separately using the verification layer's18-cycle valid history.

| Per-task metric | Evaluation input | Coefficient input |
| --- | ---: | ---: |
| NTT/INTT valid issues | 2883584 | 2359296 |
| Scaling / power update / seed issues | 65536 / 65536 / 18 | 65536 / 65536 / 18 |
| Main multiplier useful cycles | 3014674 | 2490386 |
| Task cycles without valid issue | 263755 | 263259 |
| Full-task valid utilization | 91.954836% | 90.439617% |
| Verification multiplier occupancy | 91.982318% | 90.462460% |
| Compute-phase duration (cycles) | 3016280 | 2491496 |
| Compute-phase valid utilization | 99.946756% | 99.955448% |

Compute-phase duration sums inverse/forward NTT, seed generation, the initial scaling read, coefficient/power scaling, and scaling drain, excluding input loading, padding, and output. The four partial-product units share the main multiplier's valid issue count; they are not four independent throughput lanes. Both butterfly results are useful during NTT. Scaling and power updates use the butterfly's first result `out0`; its unused second result `out1` is not counted as useful work.

## Phase and memory activity

| Phase (cycles) | Evaluation input | Coefficient input |
| --- | ---: | ---: |
| Input loading | 32769 | 32769 |
| N-point INTT including drain | 524784 | 0 |
| Power-chain seeding | 540 | 540 |
| Initial synchronous scaling read | 1 | 1 |
| Coefficient scaling and power update | 131072 | 131072 |
| Scaling drain | 29 | 29 |
| Zero padding | 98304 | 98304 |
| M-point NTT including drain | 2359854 | 2359854 |
| Output | 131074 | 131074 |

These are verification-state residence times. Acceptance and completion boundaries add 2 cycles; the phase sum plus2 equals task latency. For the four data banks, evaluation mode has read counts1507328,1507328,1572864,1572864 and write counts1474560,1474560,1572864,1572864. Coefficient mode has read counts1245184,1245184,1310720,1310720 and write counts1212416,1212416,1310720,1310720. Twiddle reads are2883584 and 2359296, respectively. Counts represent enabled memory activity, not assumed full-bandwidth transfers throughout every phase.

## Reproduction

The two large-task commands measure modes 0/1 separately. `results.json` contains per-task acceptance/output/completion edges, useful work, phases, memory counts, and aggregate metrics. See [Reproduction guide](reproduction.en.md) and [Performance statistics tool](../tools/metrics.py). Performance uses ideal interfaces; protocol backpressure and reset are separate functional tests. Counters reside in bound verification layers outside the product file list.
