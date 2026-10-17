<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/mathematical-model.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 数学模型

[English](mathematical-model.en.md)

本电路将 Goldilocks 域上的低度多项式扩展为乘法陪集上的自然序评估值，支持从原始域评估值恢复系数或直接输入系数；以下定义固定有限域、根、归一化、排序及两个输入模式的精确关系。

## 有限域与根

$$p=2^{64}-2^{32}+1=18446744069414584321,\qquad \mathbb F_p=\mathbb Z/p\mathbb Z.$$

所有输入和输出域元素使用64位无符号标准代表元，范围为 $0\le x<p$。参数中固定的阶为 $2^{32}$ 的单位根为 $\omega_{32}=\mathtt{0x185629dcda58878c}$，具有乘法阶 $2^{32}$。定义

$$\omega_\ell=\omega_{32}^{\,2^{32-\ell}}\pmod p.$$

其中 $\omega_{16}=\mathtt{0x54df9630bf79450e}$，$\omega_{18}=\mathtt{0x81281a7b05f9beac}$。这里下标表示二进制长度指数，不是点数。[固定参数](../operators/coset-lde/parameters.json)给出数值；[第三方声明](../THIRD_PARTY_NOTICES.md)说明来源。

## 尺寸与输入

生成尺寸 $h=\mathrm{maxLogM}$ 满足 $4\le h\le18$。每个任务选择 $n=\mathrm{logN}$、$b=\mathrm{logB}$，满足 $4\le n\le h$、$0\le b\le h-n$；令 $N=2^n$、$B=2^b$、$M=NB$。陪集偏移 $g$ 为非零域元素，即 $0<g<p$。

设 $f(X)=\sum_{j=0}^{N-1}a_jX^j$，次数小于 $N$。

评估输入模式0接收 $v_r=f(\omega_n^r)$，并计算

$$a_j=N^{-1}\sum_{r=0}^{N-1}v_r\omega_n^{-rj}\pmod p,\qquad 0\le j<N.$$

系数输入模式1直接接收 $a_0,a_1,\ldots,a_{N-1}$，不执行逆变换。

## 输出

定义扩展系数

$$c_j=\begin{cases}a_jg^j,&0\le j<N,\\0,&N\le j<M.\end{cases}$$

两种模式都输出

$$y_k=\sum_{j=0}^{M-1}c_j\omega_{n+b}^{jk}=f(g\omega_{n+b}^{k})\pmod p,\qquad 0\le k<M.$$

输出从 $y_0$ 开始按自然序排列，不需位反转。$g=1$ 是合法任务；若证明协议要求扩展陪集与 $M$ 点子群不相交，则集成方选择满足 $g^M\ne1$ 的偏移。电路支持全部非零 $g$，不额外限制这个协议条件。

## 旋转因子和帧

初始化分别装载 $\omega_h^j$ 与 $\omega_h^{-j}$，$0\le j<2^{h-1}$，两表合计 $2^h$ 个域元素。电路检查表帧格式和元素范围；根的数值及幂次一致性由表提供方保证。公开参考代码生成上述固定表。

原生数据每拍两个连续元素，`data_0`为较小索引；打包128位文件中位于低64位。合法输入含 $N/2$ 拍，输出含 $M/2$ 拍，每拍`count=2`，仅末拍`last=1`。标签原样返回，不参与域运算。详见 [原生接口](interface.md)。
