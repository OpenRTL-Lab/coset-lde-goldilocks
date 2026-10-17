/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/src/main/scala/ntt/asic/AsicPrefixArithmetic.scala
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

object AsicPrefixArithmetic {
  def add(a: UInt, b: UInt, width: Int, carryIn: Bool = false.B): UInt = {
    require(width >= 2)
    val left = Wire(UInt(width.W)); left := a
    val right = Wire(UInt(width.W)); right := b
    val bits = left ^ right
    var propagate: UInt = bits
    var generate: UInt = left & right
    var distance = 1
    while (distance < width) {
      val shiftedGenerate = (generate << distance)(width - 1, 0)
      val shiftedPropagate = Cat(propagate(width - distance - 1, 0),
        ((BigInt(1) << distance) - 1).U(distance.W))
      val nextGenerate = generate | (propagate & shiftedGenerate)
      val nextPropagate = propagate & shiftedPropagate
      generate = nextGenerate
      propagate = nextPropagate
      distance *= 2
    }
    val carries = ((generate << 1) | ((propagate << 1) & Fill(width + 1, carryIn)) |
      carryIn.asUInt)(width - 1, 0)
    bits ^ carries
  }
  def subtract(a: UInt, b: UInt, width: Int): UInt = {
    val right = Wire(UInt(width.W)); right := b
    add(a, ~right, width, true.B)
  }
}
