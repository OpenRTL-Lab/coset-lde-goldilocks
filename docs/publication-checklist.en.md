<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/publication-checklist.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Publication checklist

[简体中文](publication-checklist.md)

This release provides a reproducible standalone Coset-LDE design, generated SV, bilingual documentation, and reference verification, with synthesis results reported in summary form. Checks cover circuit consistency, functionality, performance, documentation, and payload privacy.

- All 17 product SV files match the product closure regenerated from public Chisel; product lists exclude test ROMs and counters.
- Large tasks compare 8388608 elements per mode with zero mismatches; small tasks add independent direct Python mathematical checks.
- Ideal interfaces, backpressure, parameter/table/frame errors, in-flight reset recovery, and incorrect-reference controls complete as expected.
- Multiplication and forward/inverse butterflies pass1024 independent arithmetic vectors; incorrect arithmetic references are detected.
- Task latency, steady throughput, arithmetic utilization, and phase durations have distinct definitions and executable reproduction paths.
- Every Markdown document has a Chinese/English pair, an opening summary, and valid relative links; English reading chains use English documents.
- Public files exclude machine absolute paths, credentials, signatures, local provenance digests, original DC scripts/reports, and process libraries.
- Release supplies the complete source archive. Sources and generated SV use the repository MIT license; external tools retain their own licenses.

See [Reproduction guide](reproduction.en.md), [Performance metrics](performance.en.md), [Synthesis results](synthesis-summary.en.md), and [Third-party notices](../THIRD_PARTY_NOTICES.en.md).
