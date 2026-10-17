<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/mathematical-model.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Mathematical model

[简体中文](mathematical-model.md)

The circuit extends a low-degree polynomial over the Goldilocks field to naturally ordered evaluations on a multiplicative coset, either recovering coefficients from base-domain evaluations or accepting coefficients directly. This document fixes the field, roots, normalization, ordering, and exact relationships between both input modes.

## Field and roots

$$p=2^{64}-2^{32}+1=18446744069414584321,\qquad \mathbb F_p=\mathbb Z/p\mathbb Z.$$

Field elements are64-bit unsigned canonical representatives, $0\le x<p$. The fixed root $\omega_{32}=\mathtt{0x185629dcda58878c}$ has multiplicative order $2^{32}$. Define

$$\omega_\ell=\omega_{32}^{\,2^{32-\ell}}\pmod p.$$

In particular, $\omega_{16}=\mathtt{0x54df9630bf79450e}$ and $\omega_{18}=\mathtt{0x81281a7b05f9beac}$. The subscript is the binary length exponent, not the number of points. [Fixed parameters](../operators/coset-lde/parameters.json) lists the values; [Third-party notices](../THIRD_PARTY_NOTICES.en.md) identifies their source.

## Dimensions and inputs

The generated capacity $h=\mathrm{maxLogM}$ satisfies $4\le h\le18$. Each task selects $n=\mathrm{logN}$ and $b=\mathrm{logB}$ with $4\le n\le h$ and $0\le b\le h-n$. Set $N=2^n$, $B=2^b$, and $M=NB$. The coset shift is a nonzero field element $0<g<p$.

Let $f(X)=\sum_{j=0}^{N-1}a_jX^j$, with degree below $N$.

Evaluation-input mode 0 accepts $v_r=f(\omega_n^r)$ and computes

$$a_j=N^{-1}\sum_{r=0}^{N-1}v_r\omega_n^{-rj}\pmod p,\qquad 0\le j<N.$$

Coefficient-input mode 1 directly accepts $a_0,a_1,\ldots,a_{N-1}$ and skips the inverse transform.

## Outputs

Define the extended coefficients

$$c_j=\begin{cases}a_jg^j,&0\le j<N,\\0,&N\le j<M.\end{cases}$$

Both modes produce

$$y_k=\sum_{j=0}^{M-1}c_j\omega_{n+b}^{jk}=f(g\omega_{n+b}^{k})\pmod p,\qquad 0\le k<M.$$

Outputs begin at $y_0$ in natural order, without bit reversal. A shift of $g=1$ is valid. If a proof protocol requires a coset disjoint from the $M$-point subgroup, the integrator selects $g$ with $g^M\ne1$. The circuit accepts every nonzero $g$ and does not impose this additional protocol condition.

## Twiddles and frames

Initialization loads $\omega_h^j$ and $\omega_h^{-j}$ separately for $0\le j<2^{h-1}$, totaling $2^h$ field elements. The circuit checks table framing and element ranges; the table provider ensures root values and power consistency. Public reference code generates the fixed tables above.

Each data beat contains two consecutive elements; `data_0` carries the lower index and occupies the low64 bits in packed128-bit vector files. Input and output lengths are $N/2$ and $M/2$ beats, respectively. Every beat has`count=2`, with`last=1` only on the final beat. Task tags are returned unchanged and do not participate in arithmetic. See [Native interface](interface.en.md).
