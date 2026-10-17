/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/src/main/scala/ntt/asic/AsicSram1R1W.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package ntt.asic

import chisel3._
import chisel3.util._

/** Portable latency-one 1R1W SRAM contract; owner maps these to ASIC macros.
  * No memory reset/init, FPGA primitive, placement or synthesis attribute.
  */
class AsicSram1R1W(val rows: Int) extends Module {
  require(rows >= 2 && isPow2(rows))
  override def desiredName: String = s"AsicSram1R1W_${rows}x64"
  val io = IO(new Bundle {
    val readEnable = Input(Bool())
    val readAddress = Input(UInt(log2Ceil(rows).W))
    val readData = Output(UInt(64.W))
    val writeEnable = Input(Bool())
    val writeAddress = Input(UInt(log2Ceil(rows).W))
    val writeData = Input(UInt(64.W))
  })
  private val memory = SyncReadMem(rows, UInt(64.W))
  io.readData := memory.read(io.readAddress, io.readEnable)
  when(io.writeEnable) { memory.write(io.writeAddress, io.writeData) }
}

