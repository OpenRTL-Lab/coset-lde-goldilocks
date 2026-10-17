<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/synthesis-summary.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Synthesis results

[简体中文](synthesis-summary.md)

The representative Goldilocks Coset-LDE configuration passes DC setup at 500 MHz with 10% clock uncertainty using TSMC28 nm HPC+ at SSG0.81 V,−40 °C. All 76771 measured paths are below 50 levels, with a maximum of 37. This document provides our account of synthesis conditions and results, without distributing commercial tools or process data.

Here maxLogM=18 means capacity for 2^18 output elements; the representative N65,536/B4 task has 65,536 inputs and expands fourfold. 1R1W denotes one read and one write port per logical memory bank. WNS is the minimum setup slack and TNS sums negative setup slacks.

## Version and conditions

| Item | Condition |
| --- | --- |
| Version / top | v1.1.0 / `GoldilocksCosetLde` |
| Configuration | `maxLogM=18`, representativeN65536/B4, one butterfly lane |
| Tool | Synopsys Design Compiler NXT X-2025.06-SP3, linux64 |
| Standard-cell process | TSMC28 nm HPC+ |
| Corner | SSG,0.81 V,−40 °C |
| Clock | 2 ns period,500 MHz |
| Setup uncertainty | 0.2 ns,10% of period |
| Clock transition | 0.1 ns |
| Input / output delay | Maximum1 ns, minimum0 ns |
| Input / maximum data transition | 0.125 ns /0.25 ns |
| Output pin load | 0.02 pF |
| Maximum fanout | 32 |
| Wire-load model | ZeroWireload |
| Storage mapping | One-cycle synchronous-read1R1W SRAM,768 physical macros |

Public SV provides synchronous storage behavior and standard RTL computation. The synthesis above uses licensed standard cells and SRAM mapping. See [Native interface](interface.en.md) and [Hardware architecture](architecture.en.md) for behavior.

## Setup and logic depth

| Metric | Result |
| --- | ---: |
| Setup | Pass |
| WNS (ns) | +0.000018477 |
| TNS (ns) | 0 |
| Violating endpoints | 0 |
| Constrained endpoints | 76771 |
| Maximum logic depth | 37 |
| Strictly below 50 levels | 100% |
| Above 30 levels | 3308 paths,4.3089% |
| Above 40 /50 /60 levels | 0 /0 /0 |

The worst max data path is selected for each constrained setup endpoint:75788 internal-to-internal,172 internal-to-IO, and 811 IO-to-internal paths. Each data pin whose leaf name is exactly`Z` or`ZN` counts as one logic level. Report rows, nets, launch clock-to-Q, and clock trees are not counted. These figures describe setup and data paths under the stated synthesis conditions.

## Cell area

| Category | Area (µm²) | Share |
| --- | ---: | ---: |
| Combinational logic excluding buffers/inverters | 28834.834 | 0.1018% |
| Buffers / inverters | 2482.144 | 0.0088% |
| Sequential cells | 17579.240 | 0.0621% |
| SRAM macros | 28270926.000 | 99.8273% |
| Total cell area | 28319822.218 | 100% |

Total cell area is 28.319822 mm², with 0.048896 mm² of non-memory standard cells. Area sums mapped cells and memory macros; it is not die area and excludes layout whitespace. Data planes and twiddle tables total 6 MiB.

## Availability

We apologize that TSMC process-library, memory-macro, and commercial-tool licensing prevents us from publishing those libraries, original DC Tcl/SDC, reports, or synthesis snapshots. Values above are our checked account of archived results. Design and simulation reproduction is in [Reproduction guide](reproduction.en.md).
