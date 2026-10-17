<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/synthesis-summary.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 综合结果

[English](synthesis-summary.en.md)

Goldilocks Coset-LDE典型配置已在TSMC28 nm HPC+、SSG0.81 V−40 °C、500 MHz和10%时钟不确定度条件下通过DC setup，全部76771条已统计路径小于50级、最大37级；本文件提供综合条件与结果的转述，不分发商业工具或工艺数据。

maxLogM=18 表示最多 2^18 个输出元素；典型 N65,536/B4 任务指65,536输入、四倍扩展。1R1W 指每个逻辑存储 bank 各有一个读端口和一个写端口。WNS 是最差 setup 裕量，TNS 是负 setup 裕量之和。

## 版本与条件

| 项目 | 条件 |
| --- | --- |
| 版本 / 顶层 | v1.1.0 / `GoldilocksCosetLde` |
| 配置 | `maxLogM=18`，典型N65536、B4、单蝶形通道 |
| 工具 | Synopsys Design Compiler NXT X-2025.06-SP3，linux64 |
| 标准单元工艺 | TSMC28 nm HPC+ |
| 工艺角 | SSG，0.81 V，−40 °C |
| 时钟 | 周期2 ns，500 MHz |
| Setup不确定度 | 0.2 ns，周期的10% |
| 时钟跃迁 | 0.1 ns |
| 输入 / 输出延迟 | 最大1 ns，最小0 ns |
| 输入跃迁 / 最大数据跃迁 | 0.125 ns / 0.25 ns |
| 输出引脚负载 | 0.02 pF |
| 最大扇出 | 32 |
| 线负载模型 | ZeroWireload |
| 存储映射 | 一拍同步读1R1W SRAM，768个物理宏 |

公开SV提供同步存储行为和标准RTL计算单元；上述综合使用已授权的标准单元与SRAM映射流程。原生接口与存储行为见 [原生接口](interface.md)和 [硬件架构](architecture.md)。

## Setup和逻辑级数

| 指标 | 结果 |
| --- | ---: |
| Setup | 通过 |
| WNS（ns） | +0.000018477 |
| TNS（ns） | 0 |
| 违例端点 | 0 |
| 已约束端点 | 76771 |
| 最大逻辑级数 | 37 |
| 严格小于50级 | 100% |
| 大于30级 | 3308条，4.3089% |
| 大于40 / 50 / 60级 | 0 / 0 / 0 |

每个已约束setup端点取最差的一条max数据路径，内部→内部75788条、内部→IO172条、IO→内部811条。只计路径中叶名严格等于`Z`或`ZN`的数据引脚，每个计一级；不按报告行数换算，不计net、起点clock-to-Q和时钟树。上述结果为该综合条件下的setup与数据路径统计。

## Cell面积

| 类别 | 面积（µm²） | 占总面积 |
| --- | ---: | ---: |
| 组合逻辑（不含缓冲/反相） | 28834.834 | 0.1018% |
| 缓冲 / 反相 | 2482.144 | 0.0088% |
| 时序单元 | 17579.240 | 0.0621% |
| SRAM宏 | 28270926.000 | 99.8273% |
| 总cell面积 | 28319822.218 | 100% |

总cell面积为28.319822 mm²，其中非存储标准单元合计0.048896 mm²。面积口径为映射cell及存储宏之和，非芯片die面积，不含布局留白。数据平面和旋转因子表合计6 MiB。

## 提供范围

很抱歉，受TSMC工艺库、存储宏与商业工具授权限制，我们无法公开相关库、原始DC Tcl/SDC、报告或综合快照。上表由归档结果核对后转述；设计和仿真复现入口见 [快速复现](reproduction.md)。
