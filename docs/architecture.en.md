<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/architecture.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Hardware architecture

[简体中文](architecture.md)

One Goldilocks butterfly pipeline with a one-cycle initiation interval performs INTT, coset coefficient scaling, and NTT using dual-plane synchronous SRAM and native handshake queues. Evaluation and coefficient input share the arithmetic hardware; complete tasks execute serially in order.

A task takes N inputs and produces M=N*B outputs for expansion factor B, all over p = 2^64 - 2^32 + 1 with canonical field words 0 <= x < p. Mode 0 supplies base-domain evaluations; mode 1 supplies polynomial coefficients in increasing degree. Generation fixes maxLogM, with M <= 2^maxLogM. The representative task is N=65,536, B=4, M=262,144 at maxLogM=18. II means the interval between arithmetic launches, not between complete tasks. See [Mathematical model](mathematical-model.en.md) for the roots and coset mapping.

## Datapath

Evaluation input follows load, normalized $N$-point INTT, coset-power scaling, zero padding to $M$, $M$-point NTT, and naturally ordered output. Coefficient input skips INTT. Both control and data planes use native ready/valid interfaces, without a SoC or bus binding.

Multiplication latency is 18 cycles and butterfly latency28 cycles, both with II=1. Four parallel32×32 partial products implement one64-bit field multiplier, not four independent multiplier lanes. Wide addition and subtraction use16-bit segmented pipelines.

Scaling reuses the butterfly's first result, `out0`, alternating coefficient scaling with power updates. Eighteen power-state slots start at g^0 through g^17. Coefficient j uses slot j mod 18; that slot is then multiplied by g^18 for its next use. These chains hide feedback latency within one task; they are not 18 task engines. Seed generation, actual synchronous reads, pipeline filling, and draining contribute to task latency.

## Storage and handshakes

Each of two planes has two XOR-parity banks so both radix-2 addresses fall into distinct banks. The representative configuration has four131072×64 data banks and one262144×64 twiddle table, totaling6 MiB. Each bank provides1R1W access with one-cycle synchronous reads, without initialization or reset clearing.

Task, table, and input use non-flow-through FIFOs with 1,2,2 slots respectively. The output queue has 4 slots with space reserved for pending synchronous read responses. Reset discards in-flight control, queues, and table-valid state; the integrator reloads tables before restarting.

## Product and verification

The product top is`GoldilocksCosetLde`; product file lists include only its dependencies. `CosetLdeSim` and reference ROMs are verification-only. C uses independent radix-2 DIT; Python cross-checks direct inverse DFT and Horner evaluation. Bound verification layers count valid issue, pipeline occupancy, and memory activity without entering product RTL.
