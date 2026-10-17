<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/interface.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Native interface

[简体中文](interface.md)

The product top`GoldilocksCosetLde` uses one clock, synchronous active-high reset, and five ready/valid channels. The control plane carries tasks and twiddle tables; the data plane transfers two naturally ordered64-bit field elements per beat; completion returns the original tag, mode, and status.

Generation fixes h=maxLogM (4..18), the maximum output length exponent. Each task selects n=logN and b=logB with 4 <= n <= h and 0 <= b <= h-n: N=2^n inputs, expansion B=2^b, and M=N*B outputs. The bundled capacity has h=18; its representative task uses N=65,536 and B=4. Mode 0 accepts base-domain evaluations and mode 1 accepts coefficients in increasing degree. The shift g satisfies 0 < g < p; field words are canonical unsigned integers 0 <= x < p, p = 2^64 - 2^32 + 1. Exact roots and output-point order are defined in [Mathematical model](mathematical-model.en.md).

## Signals and handshakes

A transfer occurs on a rising clock edge when`valid && ready`. A sender holds valid and all payload fields stable while`valid && !ready`. Tasks execute in order without overlap; the next task is accepted after completion. Internal arithmetic can issue every cycle.

| Channel | Direction relative to product | Payload |
| --- | --- | --- |
| `io_task` | Input | `tag[31:0]`, `mode[1:0]`, `logN[4:0]`, `logB[4:0]`, `g[63:0]` |
| `io_table` | Input | `inverse`, `data[63:0]`, `last` |
| `io_input` | Input | `data_0[63:0]`, `data_1[63:0]`, `count[1:0]`, `last` |
| `io_output` | Output | Same format as data input |
| `io_completion` | Output | `tag[31:0]`, `mode[1:0]`, `status[1:0]` |

Each channel has`valid` and`ready`. Other signals are`clock`, `reset`, and`io_configured[1:0]`. Flattened SV names payload fields such as`io_task_bits_tag`.

## Initialization and task order

After reset, load the forward and inverse tables. The`inverse` bit selects the table; each table contains exactly $2^{h-1}$ beats, with`last=1` on the final beat. `configured[0]` marks a valid forward table; `configured[1]` marks a valid inverse table. The final external table transfer only enters its FIFO; wait for the corresponding configured bit to confirm internal commitment. Normally load both before submitting tasks; coefficient mode needs only the forward table. Tables can be reloaded while idle and have processing priority over queued tasks.

Task mode 0 accepts evaluations; mode 1 accepts coefficients. [Mathematical model](mathematical-model.en.md) defines dimensions, elements, shifts, and ordering. After submitting a task, send exactly $N/2$ input beats with two elements and`count=2`, setting`last=1` only on the last beat. Output contains $M/2$ beats. Completion follows the final output handshake and returns tag and mode unchanged.

## Status and recovery

| status | Meaning | Behavior |
| --- | --- | --- |
| 0 | Success | Complete output and completion |
| 1 | Invalid parameters | No input data consumed or output produced |
| 2 | Invalid data frame | Consume the prescribed number of input beats; no output |
| 3 | Required table invalid | No input data consumed or output produced |

Parameter errors take precedence over table state. Invalid table frames consume their fixed length and invalidate that table. Data framing checks cover`last`, `count`, and canonical element ranges; an early`last` does not change the required beat count. Reset discards in-flight tasks and queues and clears table-valid bits without clearing storage. Reload the required tables before starting a new task.
