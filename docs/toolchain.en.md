<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/toolchain.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Toolchain guide

[简体中文](toolchain.md)

This repository generates circuits using pinned Scala, Chisel, and Mill versions, then verifies them with independent C/Python references and Verilator. Tool installation is separate from project sources; this document lists reproduced versions and environment requirements.

| Tool | Reproduced version / requirement |
| --- | --- |
| OpenJDK | 21.0.11, JDK21 |
| Mill | 1.1.7 |
| Scala | 2.13.18, fetched by Mill |
| Chisel / plugin | 7.13.0, fetched by Mill |
| CIRCT/firtool | 1.149.0, fetched by Chisel's resolver |
| Verilator | 5.050 |
| GCC / G++ | 16.2.0, C11, C++17, and`__uint128_t` |
| Python | 3.12, standard library |
| GNU Make | Builds the Verilator-generated model |

On Linux, install JDK21, Mill1.1.7, Verilator, C/C++ compilers, Make, and Python. Ensure`java`, `mill`, `verilator`, `cc`, `g++`, `make`, and`python3` are available on PATH. Initial generation accesses Maven dependencies and downloads the corresponding firtool binary through Chisel's resolver. Upstreams are [Mill](https://mill-build.org/), [Chisel](https://www.chisel-lang.org/), [CIRCT](https://circt.llvm.org/), and [Verilator](https://verilator.org/).

`build.mill` pins generation versions. If Mill has a different command name, set`MILL` to the command and optional arguments, for example`MILL="mill"`, without editing scripts. Tool and cache locations are controlled by the user's environment, not machine-bound repository paths. [Reproduction guide](reproduction.en.md) covers commands after installation.
