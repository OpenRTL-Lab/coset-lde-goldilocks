<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: THIRD_PARTY_NOTICES.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 第三方声明

[English](THIRD_PARTY_NOTICES.en.md)

本项目使用公开定义的 Goldilocks 有限域和 Plonky3 一致的幂次根数值，设计、参考与验证代码按 MIT 许可证发布；第三方工具及资料保留各自授权，本文件说明参数来源与工具边界。

根数值来源为 [Plonky3 Goldilocks 定义](https://github.com/Plonky3/Plonky3/blob/main/goldilocks/src/goldilocks.rs)。本仓库的[固定参数](operators/coset-lde/parameters.json)和[数学模型](docs/mathematical-model.md)固定具体数值，不在运行时依赖上游分支。Plonky3授权见 [Plonky3 MIT 许可证](https://github.com/Plonky3/Plonky3/blob/main/LICENSE-MIT)及其仓库许可说明。

Chisel、CIRCT/firtool、Mill、OpenJDK、Verilator、编译器与 Python 是外部工具，不作为本仓库重新授权的载荷；用户按其各自许可证安装。[工具链说明](docs/toolchain.md)给出复现版本。商业工艺库和存储宏不包含在源码包中。
