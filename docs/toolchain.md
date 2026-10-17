<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/toolchain.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 工具链说明

[English](toolchain.en.md)

本仓库使用固定的Scala、Chisel与Mill版本生成电路，再通过C/Python独立参考和Verilator验证，工具安装与项目源码分离；下面给出已复现的版本及环境要求。

| 工具 | 已复现版本 / 要求 |
| --- | --- |
| OpenJDK | 21.0.11，JDK21 |
| Mill | 1.1.7 |
| Scala | 2.13.18，由Mill下载 |
| Chisel / 插件 | 7.13.0，由Mill下载 |
| CIRCT/firtool | 1.149.0，由Chisel解析器下载 |
| Verilator | 5.050 |
| GCC / G++ | 16.2.0，C11、C++17及`__uint128_t` |
| Python | 3.12，标准库 |
| GNU Make | Verilator生成模型构建 |

在Linux环境安装JDK21、Mill1.1.7、Verilator、C/C++编译器、Make和Python，使`java`、`mill`、`verilator`、`cc`、`g++`、`make`、`python3`可从PATH找到。首次生成需要访问Maven依赖仓库，Chisel解析器取得对应firtool二进制。公共工具上游为 [Mill](https://mill-build.org/)、[Chisel](https://www.chisel-lang.org/)、[CIRCT](https://circt.llvm.org/)与 [Verilator](https://verilator.org/)。

`build.mill`固定生成相关版本。若Mill命令名称不同，可用环境变量`MILL`指定命令及参数，例如`MILL="mill"`；无需修改脚本。工具安装、缓存位置由用户环境决定，仓库不绑定机器目录。[快速复现](reproduction.md)说明安装后的生成与验证命令。
