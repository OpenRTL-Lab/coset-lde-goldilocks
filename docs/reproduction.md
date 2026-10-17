<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/reproduction.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 快速复现

[English](reproduction.en.md)

本流程从独立C参考生成输入与期望输出，通过Chisel重新生成SV、核对随包产品电路，再逐元素验证理想接口、协议背压、复位与负控；也提供独立算术验证和两种模式的完整性能测试命令。

## 获取版本快照

[Release v1.1.0](https://github.com/OpenRTL-Lab/coset-lde-goldilocks/releases/tag/v1.1.0)

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/coset-lde-goldilocks.git
cd coset-lde-goldilocks
```

版本标签标识包含来源声明和文档的当前源码树；Release 下载提供本版本硬件源码与随包 RTL。源码包与在线目录的注释及文档可能不同。RTL 集成使用产品文件清单，不把验证环境作为产品电路；支持配置、验证范围和测量条件见验证及综合文档。

## 1. 准备

先按 [工具链说明](toolchain.md)安装工具并配置PATH，然后进入本仓库根目录。所有以下命令均从根目录执行；输出目录必须为尚不存在的`build/`子目录，重跑时更换输出目录名。Chisel首次运行自动解析固定依赖，后续可复用缓存。

命令中 `--maxlog h` 设置硬件最大输出长度 2^h，`--logn n` 设置测试输入 N=2^n，`--logb b` 设置扩展倍率 B=2^b（输出 M=N×B）。`--tasks` 是验证任务数；`--mode 0/1` 选择求值/系数测试输入，`--mode 2` 仅让环境交替提交两种合法任务，不是硬件任务模式。`--jobs` 只控制编译并行度。

## 2. 完整小任务验证

下面命令以发布容量生成电路，测试8个16点输入、4倍扩展任务，两种模式交替；同时核对17个生成产品SV与随包SV去除注释/空白后的内容一致，分别lint两份产品清单，再运行所有四类测试：

```sh
python3 tools/verify.py --out build/verify --jobs 8
```

默认`maxlog=18, logn=4, logb=2, tasks=8, mode=2`。C参考采用独立radix-2 DIT；小于等于1024输出点时Python用直接逆DFT和Horner再次核对参考。每个成功仿真逐元素比较512个输出；理想、协议及复位测试要求零差异。错误参考负控主动翻转一个期望值，要求检测到恰好一个差异；因此负控模拟器返回1是预期成功，而整个验证脚本仍返回0。

协议测试检查任务/数据/表错误、输出和完成背压以及载荷稳定；复位测试中断在途任务后重新装表并成功完成新任务。参考任务覆盖零、常数、全最大标准代表元、单项式和确定性伪随机输入，偏移循环为1、7、$p-1$、19。

## 3. 独立算术验证

下面命令对模乘和正/逆蝶形核对1024组边界及伪随机向量，并验证错误参考负控：

```sh
python3 tools/verify_arithmetic.py --out build/arithmetic --jobs 8
```

## 4. 仅生成电路

需要生成产品与单独验证环境而不编译模拟器时，执行：

```sh
python3 tools/emit_sv.py --out build/emit --maxlog 18 --logn 16 --logb 2 --tasks 8 --mode 0
```

`build/emit/rtl/product.f`仅列出产品顶层`GoldilocksCosetLde`及其依赖；`simulation.f`包含`CosetLdeSim`、参考ROM和绑定验证层，用于仿真。产品同步存储不预装数据，不包含验证ROM和性能计数器。`maxlog`是生成容量，`logn/logb`是测试任务尺寸，`tasks/mode`配置验证环境，不改变产品电路容量。

## 5. 性能复现

下面命令在理想接口下连续执行32个评估输入任务，完整核对8388608个输出，并测量任务延迟和稳态吞吐：

```sh
python3 tools/verify.py --out build/performance-eval --logn 16 --logb 2 --tasks 32 --mode 0 --ideal-only --jobs 8
```

下面命令用同一配置测试32个系数输入任务：

```sh
python3 tools/verify.py --out build/performance-coeff --logn 16 --logb 2 --tasks 32 --mode 1 --ideal-only --jobs 8
```

每个输出目录中`results.json`给出`status=0`、测试结果、逐任务时间戳、忙率、阶段与存储统计。`ideal.log`保留`RESULT/METRIC/MEMORY/PHASE`记录，`emit.log`和`verilator.log`分别定位生成与编译问题。[性能指标](performance.md)给出定义及期待值；`cycles`是整场仿真计数，包含初始化和错误测试，不是单任务延迟。

## 6. 改变配置

下面命令验证较小容量的1024输入、4倍扩展，包含四种仿真变体：

```sh
python3 tools/verify.py --out build/size1024 --maxlog 12 --logn 10 --logb 2 --jobs 8
```

生成容量可取4到18；任务尺寸须满足 [数学模型](mathematical-model.md)的范围。`mode=0/1`固定输入模式，`mode=2`在验证中交替，任务字段只有0/1合法。标准输出中的成功摘要以及`results.json`是复现检查入口；详细原生接口见 [原生接口](interface.md)。
