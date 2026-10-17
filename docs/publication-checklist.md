<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Coset-LDE Hardware Accelerator
Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
File: docs/publication-checklist.md

SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 发布内容检查

[English](publication-checklist.en.md)

本发布提供可复现的独立Coset-LDE设计、生成SV、双语说明和参考验证链路，并以转述形式提供综合结果；下列检查覆盖产品一致性、功能、性能、文档完整性和载荷隐私。

- 产品17个SV与公开Chisel重新生成的产品闭包一致，清单不含测试ROM或性能计数器。
- 两输入模式的大任务各比对8388608个元素，零差异；小任务另有独立Python直接数学交叉核对。
- 理想接口、背压协议、参数/表/数据错误、在途复位恢复和错误参考负控均按预期完成。
- 模乘与正/逆蝶形1024组独立算术向量通过；算术错误参考被检出。
- 任务延迟、稳态吞吐、计算单元忙率及阶段时长都有独立定义和可执行复现入口。
- Markdown全部具有中英配对、首段总结、有效相对链接；英文阅读链使用英文文档。
- 公开文件不含机器绝对路径、访问凭证、签名、本地追溯摘要、原始DC脚本/报告或工艺库。
- Release提供完整源码包；源代码及生成SV适用仓库MIT许可证，第三方工具保持各自授权。

相关说明见 [快速复现](reproduction.md)、[性能指标](performance.md)、[综合结果](synthesis-summary.md)和 [第三方声明](../THIRD_PARTY_NOTICES.md)。
