<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: README.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Goldilocks Coset Low-Degree Extension Hardware Accelerator

[简体中文](README.zh-CN.md)

This repository provides a standalone Goldilocks Coset-LDE hardware accelerator developed in Chisel. It computes evaluations over an extended multiplicative coset from polynomial evaluations or coefficients. Through its native interface, it can be integrated into ASIC or FPGA designs for low-degree extension in STARK, FRI, and polynomial commitment systems using the same field and evaluation conventions.

It is licensed under the MIT License, permitting free commercial use, modification, and redistribution, provided the copyright and license notices are retained; see [Third-party notices](THIRD_PARTY_NOTICES.en.md) for parameter sources.

[Release v1.1.0](https://github.com/OpenRTL-Lab/coset-lde-goldilocks/releases/tag/v1.1.0) · [Native interface](docs/interface.en.md) · [Reproduction guide](docs/reproduction.en.md) · [Performance metrics](docs/performance.en.md) · [Synthesis results](docs/synthesis-summary.en.md)

## Computation and task units

One task accepts N inputs for a polynomial of degree below N and produces M = N * B evaluations, where B is the expansion factor. The bundled example has N = 65,536, B = 4 and M = 262,144. In evaluation-input mode 0, input index r carries f(omega_N^r); in coefficient-input mode 1, index j carries the coefficient of X^j. Here omega_N and omega_M are the configured roots of orders N and M. Output index k carries f(g * omega_M^k) for the task's nonzero coset shift g, in increasing k without bit reversal.

All elements are unsigned 64-bit canonical Goldilocks values, 0 <= x < p for p = 2^64 - 2^32 + 1. A data beat carries two consecutive elements: a task uses N/2 input beats and M/2 output beats. One butterfly lane is shared across transform stages; it does not process a complete transform per cycle. Generated capacity limits M, while a runtime task may select smaller dimensions.

## At a glance

| Integration question | Released hardware |
| --- | --- |
| Computation | Polynomial low-degree extension (LDE) to a multiplicative coset, from evaluations or coefficients |
| Field | Goldilocks p = 2^64 - 2^32 + 1; natural-order input and output |
| Bundled configuration | N = 65,536; expansion factor 4; M = 262,144; one butterfly lane |
| Product top | `GoldilocksCosetLde`; one shared INTT/scaling/NTT datapath |
| Native boundary | Task, twiddle table, two-element input/output beats and completion ready/valid channels |

## Quick start

Download the source archive from [Release v1.1.0](https://github.com/OpenRTL-Lab/coset-lde-goldilocks/releases/tag/v1.1.0), or clone the current release snapshot:

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/coset-lde-goldilocks.git
cd coset-lde-goldilocks
```

Install the exact prerequisites in [Toolchain guide](docs/toolchain.en.md), then run from the repository root with a fresh output directory:

```bash
python3 tools/emit_sv.py --out build/emit --maxlog 18 --logn 16 --logb 2 --tasks 8 --mode 0
```

This emission command is not a complete RTL regression. The documented verification entry is:

```bash
python3 tools/verify.py --out build/verify --jobs 8
```

Follow [Reproduction guide](docs/reproduction.en.md) for expected results, negative-control interpretation and the distinction between short functional and full performance runs.

## Verification and integration boundary

The default verification intentionally uses short 16-input, 4x-extension tasks at the released hardware capacity. It compares all output words, generated/bundled product circuits and independent C plus small-size direct-DFT/Horner references, with ideal, protocol, reset and deliberate-error tests. Separate documented commands reproduce full N65,536 performance in both input modes and the arithmetic boundary suite.

Evaluation-input mode needs INTT before scaling, padding and NTT; coefficient-input mode skips INTT. Load the required valid twiddle tables before admission, and reload them after reset. A successful short functional run is not the full-size performance run. The surrounding prover supplies shifts, data layout, memory mapping and commitments; this is not a complete STARK system.

### Reading performance correctly

Under ideal native interfaces, evaluation-input mode 0 has latency 3,278,429 cycles and a steady task interval of 3,278,430 cycles; coefficient-input mode 1 has latency 2,753,645 cycles and interval 2,753,646 cycles. A task includes input loading, transforms, scaling, padding, output and completion, but excludes prior twiddle-table loading. At an assumed 500 MHz the measured intervals convert to 152.512 and 181.577 tasks/s, respectively; these are not additional physical measurements.

These are documented RTL cycle measurements under ideal native interfaces. MHz-based time/rate conversions are not board or silicon measurements. The reported Design Compiler results apply only to the documented configuration, library corner and constraints; they do not establish place-and-route signoff or system throughput. See [Performance metrics](docs/performance.en.md) and [Synthesis results](docs/synthesis-summary.en.md) for conditions.

## Repository contents

- [Chisel design](operators/coset-lde/src/main/scala/): the standalone operator, field-arithmetic pipeline, synchronous storage, and verification environments.
- [Generated SV](rtl/v1.1.0/n65536-b4-lane1/): one butterfly lane, a maximum output length of 262144, and a representative 65,536-input, 4×-extension task. Use the [Product file list](rtl/v1.1.0/n65536-b4-lane1/files.f) for tool input.
- [Generation and verification tools](tools/): the [C reference model](operators/coset-lde/cmodel/) and [Simulation drivers](operators/coset-lde/sim/) provide the complete reproduction chain.
- [Hardware architecture](docs/architecture.en.md) and [Native interface](docs/interface.en.md) describe pipelines, storage, and integration.

## Applications

Applications include Goldilocks coset evaluation extension for STARK trace polynomials, FRI input codewords, and data preparation for related polynomial commitments. Integrators select matching roots, coset shifts, and input modes. [Native interface](docs/interface.en.md) specifies handshakes and ordering.

## Performance

For 65536 inputs with 4× extension at 500 MHz, task latency is 6.556858 ms for evaluation input or 5.507290 ms for coefficient input; steady throughput is 152.512 or 181.577 tasks/s, respectively. Definitions, methodology, and arithmetic utilization are in [Performance metrics](docs/performance.en.md).

## Mathematical model

The operator produces naturally ordered polynomial evaluations on a multiplicative coset over the Goldilocks field, using either a normalized inverse transform or direct coefficient input. See [Mathematical model](docs/mathematical-model.en.md) for the exact definition.

## Reproduction

The public flow includes independent references, Chisel generation, product-SV equivalence checks, full output comparison, and protocol, reset, and negative controls. [Reproduction guide](docs/reproduction.en.md) provides installation steps and commands.

## Synthesis results

DC setup passes at 500 MHz with 10% clock uncertainty using TSMC28 nm HPC+ at SSG0.81 V,−40 °C. All 76771 measured paths are below 50 logic levels, with a maximum of 37. Conditions and area breakdowns are in [Synthesis results](docs/synthesis-summary.en.md).

## Publication scope

Design sources, generated SV, parameters, reference models, and simulation tools are provided. We apologize that process-library and commercial-tool licensing prevents us from publishing libraries, memory macro models, original DC scripts, reports, or synthesis snapshots. Public synthesis information is our account in [Synthesis results](docs/synthesis-summary.en.md). This repository delivers a standalone operator with native interfaces; integrators connect system buses and proof protocols.

## OpenRTL and custom integration

Browse [OpenRTL](https://github.com/OpenRTL-Lab), the [ZK accelerator catalogue](https://github.com/OpenRTL-Lab/zk-accelerators) and [OpenRTL website](https://openrtl.top/) for related modules and the cooperation/contact entry. Custom datapaths, interfaces and target-platform adaptation are optional services; MIT commercial use does not require buying a service. Do not submit confidential specifications in public issues.

## Release sources and integration

The version tag identifies the current source tree, including source notices and documentation. Release downloads provide hardware sources and bundled RTL for this version. Archive annotations and documentation can differ from the online tree. For RTL integration, use the product file list rather than verification environments; use the verification and synthesis documents for supported configurations and measurement conditions.
