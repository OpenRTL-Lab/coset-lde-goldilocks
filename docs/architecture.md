<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/architecture.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 硬件架构

[English](architecture.en.md)

本加速器用一个可每拍发射的Goldilocks蝶形流水线完成INTT、coset系数缩放和NTT，配合双平面同步SRAM及原生握手队列，提供评估输入与系数输入两条任务路径；计算硬件复用，任务整体按序串行执行。

一个任务输入 N 个、按扩展倍率 B 输出 M=N×B 个元素，域模数 p = 2^64 - 2^32 + 1，规范值为 0 ≤ x < p。模式0提供原求值域上的求值，模式1按次数递增提供多项式系数。生成时固定 maxLogM，任务须有 M ≤ 2^maxLogM；典型任务为 maxLogM=18、N=65,536、B=4、M=262,144。II 指相邻算术发射间隔，不是完整任务间隔；单位根和陪集映射见 [数学模型](mathematical-model.md)。

## 数据路径

评估输入路径为输入装载、N点归一化INTT、系数乘以coset幂、补零到M、M点NTT、自然序输出；系数输入跳过INTT。控制面与数据面均为原生ready/valid接口，没有SoC或总线绑定。

模乘18拍、蝶形28拍，二者II均为1。模乘内部四个32×32部分积并行形成一个64位域乘法，不是四个独立乘法通道。宽加减使用16位分块流水结构。

缩放复用蝶形的第一个结果 `out0`，交替发射系数缩放和幂更新。18个幂状态槽初始化为 g^0 到 g^17；系数 j 使用 j mod 18 槽，再将该槽乘 g^18 供下次使用。这是在同一任务内隐藏反馈等待的幂链，不是18套任务引擎；种子生成、真实同步读、流水填充和排空均计入任务延迟。

## 存储与握手

两平面各有两个XOR奇偶银行，保证每个radix-2蝶形的两个地址落在不同银行。典型配置四个131072×64数据银行及一个262144×64旋转因子表，总计6 MiB。每个存储 bank 有一读一写端口（1R1W）、一拍同步读，存储无初始化与复位清零。

任务、表和输入分别有1、2、2槽非直通FIFO，输出有4槽队列并为同步读响应预留容量。复位丢弃在途控制、队列和表有效状态，集成方重新配置旋转因子表后启动新任务。

## 产品与验证

产品顶层为GoldilocksCosetLde，随包产品清单仅列产品依赖。CosetLdeSim和参考ROM仅用于验证；C采用独立radix-2 DIT，Python采用直接逆DFT与Horner交叉核对。性能绑定层统计有效发射、流水占用和存储端口活动，不加入产品电路。
