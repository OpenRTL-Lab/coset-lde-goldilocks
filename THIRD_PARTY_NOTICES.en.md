<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: THIRD_PARTY_NOTICES.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Third-party notices

[简体中文](THIRD_PARTY_NOTICES.md)

This project uses the publicly defined Goldilocks field and power-of-two roots consistent with Plonky3. Design, reference, and verification code are released under MIT; third-party tools and materials retain their own licenses. This document identifies parameter sources and tool boundaries.

Root values come from the [Plonky3 Goldilocks definition](https://github.com/Plonky3/Plonky3/blob/main/goldilocks/src/goldilocks.rs). [Fixed parameters](operators/coset-lde/parameters.json) and [Mathematical model](docs/mathematical-model.en.md) pin numerical values without a runtime dependency on the upstream branch. See the [Plonky3 MIT license](https://github.com/Plonky3/Plonky3/blob/main/LICENSE-MIT) and its repository license notices.

Chisel, CIRCT/firtool, Mill, OpenJDK, Verilator, compilers, and Python are external tools, not redistributed or relicensed payloads here. Install them under their respective licenses. [Toolchain guide](docs/toolchain.en.md) lists reproduction versions. Commercial process libraries and memory macros are not part of the source archive.
