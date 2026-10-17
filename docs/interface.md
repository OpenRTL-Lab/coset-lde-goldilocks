<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/interface.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 原生接口

[English](interface.en.md)

产品顶层`GoldilocksCosetLde`使用单时钟、同步高有效复位及五组ready/valid通道，控制面提交任务和旋转因子，数据面按自然序传递两个64位域元素，完成通道返回原标签、模式和状态。

生成参数 h=maxLogM（4..18）是最大输出长度的二进制指数。任务选 n=logN、b=logB，要求 4 ≤ n ≤ h、0 ≤ b ≤ h-n；输入 N=2^n 个，扩展倍率 B=2^b，输出 M=N×B 个。随包容量 h=18，典型任务 N=65,536、B=4。模式0输入原求值域上的求值，模式1按次数递增输入系数。偏移满足 0 < g < p，域元素为无符号规范整数 0 ≤ x < p，p = 2^64 - 2^32 + 1；精确单位根及输出点顺序见 [数学模型](mathematical-model.md)。

## 信号与握手

全部传输在时钟上升沿`valid && ready`时发生。发送方在`valid && !ready`时保持有效位和所有载荷稳定。任务整体按序执行，完成前不接受下一任务；内部算术流水线可每拍发射。

| 通道 | 相对产品方向 | 载荷 |
| --- | --- | --- |
| `io_task` | 输入 | `tag[31:0]`、`mode[1:0]`、`logN[4:0]`、`logB[4:0]`、`g[63:0]` |
| `io_table` | 输入 | `inverse`、`data[63:0]`、`last` |
| `io_input` | 输入 | `data_0[63:0]`、`data_1[63:0]`、`count[1:0]`、`last` |
| `io_output` | 输出 | 同数据输入格式 |
| `io_completion` | 输出 | `tag[31:0]`、`mode[1:0]`、`status[1:0]` |

每个通道有`valid`和`ready`。其余信号为`clock`、`reset`、`io_configured[1:0]`。展开SV以`io_task_bits_tag`等命名载荷。

## 初始化和任务顺序

复位后装载正向和逆向表。`inverse=0/1`选择对应表，每张表固定 $2^{h-1}$ 拍；最后一拍`last=1`。`configured[0]`表示正向表有效，`configured[1]`表示逆向表有效。末次外部 table 握手只表示进入入口 FIFO，应等待对应 configured 位确认内部提交。通常装载两张表后再提交任务；系数模式只需要正向表。空闲期允许重新装载，表处理优先于已排队任务。

任务模式0为评估输入，1为系数输入；尺寸、元素、偏移和排序详见 [数学模型](mathematical-model.md)。提交任务后发送恰好 $N/2$ 拍输入，每拍两个元素、`count=2`，仅末拍`last=1`。完整输出为 $M/2$ 拍；完成消息在末输出握手之后产生，原样返回标签和模式。

## 状态与恢复

| status | 含义 | 行为 |
| --- | --- | --- |
| 0 | 成功 | 完整输出及完成消息 |
| 1 | 参数非法 | 不消费数据，不输出数据 |
| 2 | 数据帧非法 | 消费本任务规定的输入拍数，不输出数据 |
| 3 | 所需表无效 | 不消费数据，不输出数据 |

参数错误优先于表状态。非法表帧按固定长度消费后标记对应表无效；数据帧包括`last`、`count`和标准代表元范围检查，接收方不会因提前`last`改变期望拍数。复位丢弃在途任务和队列，清除表有效位；存储不清零。复位后重新装载所需表并提交新任务。
