/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/src/tool/scala/zk/lde/EmitArithmetic.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package zk.lde
import circt.stage.ChiselStage
import java.nio.file.{Files,Paths}
object EmitArithmetic extends App {
  require(args.length==2)
  val target=Paths.get(args(0));require(!target.isAbsolute && target.startsWith("build") && !Files.exists(target))
  Files.createDirectories(target)
  ChiselStage.emitSystemVerilog(new LdeArithmeticSim(args(1)),firtoolOpts=Array("-O=release",
    "-disable-all-randomization","--lowering-options=disallowLocalVariables","-split-verilog",s"-o=$target"))
}
