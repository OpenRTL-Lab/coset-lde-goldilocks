<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: README.zh-CN.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# coset-lde-goldilocks 硬件加速器

[English](README.md)

本仓库提供使用 Chisel 开发的 Goldilocks Coset-LDE 独立硬件加速器，支持从多项式评估值或系数计算扩展乘法陪集上的评估值。它可通过原生接口集成到 ASIC 或 FPGA，用于采用相同有限域与求值约定的 STARK、FRI 和多项式承诺系统中的低度扩展计算。

采用 MIT 许可证，允许免费商用、修改和再分发，须保留版权及许可声明；参数来源见 [第三方声明](THIRD_PARTY_NOTICES.md)。

[Release v1.1.0](https://github.com/OpenRTL-Lab/coset-lde-goldilocks/releases/tag/v1.1.0) · [原生接口](docs/interface.md) · [快速复现](docs/reproduction.md) · [性能指标](docs/performance.md) · [综合结果](docs/synthesis-summary.md)

## 计算与任务单位

一个任务接收次数小于 N 的多项式的 N 个输入，产生 M = N × B 个评估值，B 是扩展倍率；随包示例为 N = 65,536、B = 4、M = 262,144。求值输入模式 0 按 r 递增输入 f(omega_N^r)；系数输入模式 1 按 j 递增输入 X^j 的系数。omega_N 和 omega_M 分别是配置的 N 阶和 M 阶单位根。输出按 k 递增给出 f(g × omega_M^k)，g 是任务指定的非零陪集偏移，不作位逆序。

全部域元素采用 p = 2^64 - 2^32 + 1 下的无符号 64 位规范值，满足 0 ≤ x < p。一个数据拍携带两个连续元素，因此每任务为 N/2 个输入拍、M/2 个输出拍。单蝶形通道在各级变换间复用，并非每周期处理一次完整变换。生成容量限制最大 M，运行时任务可以选择较小尺寸。

## 配置速览

| 集成关注点 | 已发布硬件 |
| --- | --- |
| 计算 | 从评估值或系数计算乘法陪集上的多项式低度扩展（LDE） |
| 有限域 | Goldilocks p = 2^64 - 2^32 + 1；输入输出均为自然序 |
| 随包配置 | N = 65,536；扩展倍率 4；M = 262,144；单蝶形通道 |
| 产品顶层 | `GoldilocksCosetLde`；一套共享的 INTT/缩放/NTT 数据通路 |
| 原生边界 | 任务、旋转因子表、双元素输入/输出拍及完成 ready/valid 通道 |

## 快速开始

从 [Release v1.1.0](https://github.com/OpenRTL-Lab/coset-lde-goldilocks/releases/tag/v1.1.0) 下载源码包，或克隆当前发布快照：

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/coset-lde-goldilocks.git
cd coset-lde-goldilocks
```

按 [工具链说明](docs/toolchain.md) 安装指定版本的依赖，然后在仓库根目录使用新的输出目录运行：

```bash
python3 tools/emit_sv.py --out build/emit --maxlog 18 --logn 16 --logb 2 --tasks 8 --mode 0
```

此生成命令不是完整 RTL 回归。文档规定的验证入口为：

```bash
python3 tools/verify.py --out build/verify --jobs 8
```

预期结果、负控含义及短功能测试与完整性能测试的区别见 [快速复现](docs/reproduction.md)。

## 验证与集成边界

默认验证有意在已发布硬件容量下使用 16 输入、4 倍扩展的小任务，逐词比对全部输出、生成电路与随包产品电路，并使用独立 C 参考及小规模直接 DFT/Horner 参考，覆盖理想接口、协议、复位和故意注错测试。文档另提供两种输入模式下完整 N65,536 性能测试与算术边界测试的命令。

求值输入模式先经 INTT，再进行缩放、补零和 NTT；系数输入模式跳过 INTT。受理任务前须装入所需的有效旋转因子表，复位后须重新装载。短功能测试成功不等于完成全尺寸性能测试。陪集偏移、数据布局、内存映射和承诺由外围证明器提供，本模块不是完整 STARK 系统。

### 正确理解性能

理想原生接口下，求值输入模式 0 的任务延迟为 3,278,429 周期、稳态任务间隔为 3,278,430 周期；系数输入模式 1 分别为 2,753,645 和 2,753,646 周期。任务包含输入装载、变换、缩放、补零、输出及完成，不包含此前的旋转因子表装载。假定 500 MHz，实测任务间隔分别换算为 152.512 和 181.577 任务/秒；这些换算值不是额外的物理测量。

上述周期数来自理想原生接口下的 RTL 测量，按 MHz 换算的时间和速率不是板级或硅后实测。Design Compiler 结果只适用于文档所列配置、库角和约束，不代表布局布线签核或系统吞吐率；条件见 [性能指标](docs/performance.md) 和 [综合结果](docs/synthesis-summary.md)。

## 仓库内容

- [Chisel 设计](operators/coset-lde/src/main/scala/)：独立算子、域运算流水线、同步存储与验证环境。
- [生成好的 SV](rtl/v1.1.0/n65536-b4-lane1/)：单蝶形通道，最大输出长度262144，典型任务输入65536、扩展倍率4；[产品文件清单](rtl/v1.1.0/n65536-b4-lane1/files.f)可直接用于工具读入。
- [生成与验证工具](tools/)：[C 参考模型](operators/coset-lde/cmodel/)与[仿真驱动](operators/coset-lde/sim/)提供完整复现链路。
- [硬件架构](docs/architecture.md)、[原生接口](docs/interface.md)说明流水线、存储和集成协议。

## 应用场景

可用于 Goldilocks 多项式的陪集求值扩展，例如 STARK 迹多项式扩展、FRI 输入码字生成，以及相关多项式承诺的数据准备；集成方选择对应的根、陪集偏移和输入模式。[原生接口](docs/interface.md)给出握手与数据排序约定。

## 性能指标

65,536 输入、4 倍扩展在 500 MHz 下，求值输入和系数输入模式的任务延迟分别为 6.556858 ms 和 5.507290 ms，稳态吞吐率分别为 152.512 和 181.577 任务/秒；定义、测法及算术单元利用率见 [性能指标](docs/performance.md)。

## 数学模型

在 Goldilocks 域上输出多项式在乘法陪集各点的自然序评估值，提供归一化逆变换和直接系数两种输入方式；严格定义见 [数学模型](docs/mathematical-model.md)。

## 快速复现

公开链路包含独立参考、Chisel生成、产品SV一致性核对、完整输出比对及协议、复位和负控测试；安装步骤与逐项命令见 [快速复现](docs/reproduction.md)。

## 综合结果

TSMC 28 nm HPC+、SSG 0.81 V −40 °C、500 MHz及10%时钟不确定度条件下，DC setup通过，全部76771条已统计路径小于50级，最大37级；条件与面积明细见 [综合结果](docs/synthesis-summary.md)。

## 公开边界

设计、生成SV、参数、参考模型和仿真工具均提供源码。很抱歉，受工艺库及商业工具授权限制，我们无法公开工艺库、存储宏模型、原始DC脚本、报告和综合快照；公开综合信息以 [综合结果](docs/synthesis-summary.md)的转述为准。本仓库交付独立算子和原生接口，系统总线封装及证明协议由集成方连接。

## OpenRTL与定制集成

通过[OpenRTL组织主页](https://github.com/OpenRTL-Lab)、[ZK加速器集合](https://github.com/OpenRTL-Lab/zk-accelerators)及[OpenRTL网站](https://openrtl.top/)查找相关模块与合作联系入口。需要定制数据通路、接口或目标平台适配时可联系维护者；MIT商用权不附加购买服务的要求，不要在公开Issue提交保密规格。

## 发布源码与集成

版本标签标识包含来源声明和文档的当前源码树；Release 下载提供本版本硬件源码与随包 RTL。源码包与在线目录的注释及文档可能不同。RTL 集成使用产品文件清单，不把验证环境作为产品电路；支持配置、验证范围和测量条件见验证及综合文档。
