/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/src/main/scala/zk/lde/LdeArithmetic.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package zk.lde

import chisel3._
import chisel3.util._
import ntt.asic.AsicPrefixArithmetic
import ntt.GoldilocksReference

/** 两拍、II=1。最多五个16位块；不把宽进位链留在一个时钟内。 */
class LdeSegmentedAdd(width: Int) extends Module {
  require(width >= 2 && width <= 66)
  override def desiredName: String = s"LdeSegmentedAdd_$width"
  val io = IO(new Bundle {
    val a = Input(UInt(width.W)); val b = Input(UInt(width.W))
    val carry = Input(Bool()); val sum = Output(UInt(width.W))
  })
  val blocks = (width+15)/16
  val zero = (0 until blocks).map { i =>
    val w = math.min(16,width-16*i)
    RegNext(AsicPrefixArithmetic.add(io.a(16*i+w-1,16*i),io.b(16*i+w-1,16*i),w+1))
  }
  val one = (0 until blocks).map { i =>
    val w = math.min(16,width-16*i)
    RegNext(AsicPrefixArithmetic.add(io.a(16*i+w-1,16*i),io.b(16*i+w-1,16*i),w+1,true.B))
  }
  val propagate = (0 until blocks).map { i =>
    val w = math.min(16,width-16*i)
    RegNext((io.a(16*i+w-1,16*i)^io.b(16*i+w-1,16*i)).andR)
  }
  var carry = RegNext(io.carry)
  val chosen = (0 until blocks).map { i =>
    val w = math.min(16,width-16*i)
    val value = Mux(carry,one(i)(w-1,0),zero(i)(w-1,0))
    carry = zero(i)(w) || (propagate(i) && carry)
    value
  }
  io.sum := RegNext(Cat(chosen.reverse))
}
object LdeArithmetic {
  def add(a: UInt,b: UInt,width: Int,carry: Bool=false.B): UInt = {
    val unit=Module(new LdeSegmentedAdd(width))
    unit.io.a:=a;unit.io.b:=b;unit.io.carry:=carry;unit.io.sum
  }
  def subtract(a: UInt,b: UInt,width: Int): UInt = {
    val right=Wire(UInt(width.W));right:=b
    add(a,~right,width,true.B)
  }
}
object LdeGoldilocksMul { val Latency=18 }
class LdeGoldilocksMul extends Module {
  val io=IO(new Bundle {val a=Input(UInt(64.W));val b=Input(UInt(64.W));val product=Output(UInt(64.W))})
  val p=GoldilocksReference.Modulus.U(66.W)
  val p0=RegNext(io.a(31,0)*io.b(31,0));val p1=RegNext(io.a(31,0)*io.b(63,32))
  val p2=RegNext(io.a(63,32)*io.b(31,0));val p3=RegNext(io.a(63,32)*io.b(63,32))
  val cross=LdeArithmetic.add(p1,p2,65)
  val outerLow=ShiftRegister(p0,2);val outerHigh=ShiftRegister(p3,2)
  val low=LdeArithmetic.add(outerLow,Cat(cross(31,0),0.U(32.W)),65)
  val crossHigh=ShiftRegister(cross(64,32),2);val highDelayed=ShiftRegister(outerHigh,2)
  val productHigh=LdeArithmetic.add(Cat(0.U(1.W),highDelayed),Cat(0.U(32.W),crossHigh),65,low(64))
  val productLow=ShiftRegister(low(63,0),2)
  val t0=RegNext(productLow(31,0))
  val highSum=RegNext(AsicPrefixArithmetic.add(productLow(63,32),productHigh(31,0),33))
  val subtrahend=RegNext(AsicPrefixArithmetic.add(productHigh(31,0),productHigh(63,32),33))
  val positive=LdeArithmetic.add(Cat(0.U(1.W),highSum,t0),p,66)
  val delayedSub=ShiftRegister(subtrahend,2)
  val candidate=LdeArithmetic.subtract(positive,Cat(0.U(33.W),delayedSub),66)
  val minusP=LdeArithmetic.subtract(candidate,p,66)
  val once=RegNext(Mux(ShiftRegister(candidate>=p,2),minusP,ShiftRegister(candidate,2)))
  val minusP2=LdeArithmetic.subtract(once,p,66)
  val finalProduct=RegNext(Mux(ShiftRegister(once>=p,2),minusP2,ShiftRegister(once,2)))
  io.product:=finalProduct(63,0)
}
object LdeGoldilocksButterfly { val Latency=28 }
class LdeGoldilocksButterfly extends Module {
  val io=IO(new Bundle {
    val valid=Input(Bool());val inverse=Input(Bool());val a=Input(UInt(64.W));val b=Input(UInt(64.W))
    val twiddle=Input(UInt(64.W));val outValid=Output(Bool());val out0=Output(UInt(64.W));val out1=Output(UInt(64.W))
  })
  val p=GoldilocksReference.Modulus.U(65.W)
  val multiplier=Module(new LdeGoldilocksMul);multiplier.io.a:=io.b;multiplier.io.b:=io.twiddle
  val a=ShiftRegister(io.a,LdeGoldilocksMul.Latency)
  val sum=LdeArithmetic.add(a,multiplier.io.product,65)
  val differenceBase=LdeArithmetic.add(Cat(0.U(1.W),a),p,65)
  val product=ShiftRegister(multiplier.io.product,2)
  val difference=LdeArithmetic.subtract(differenceBase,product,65)
  val sumMinus=LdeArithmetic.subtract(sum,p,65)
  val correctedSum=RegNext(Mux(ShiftRegister(sum>=p,2),sumMinus,ShiftRegister(sum,2)))
  val result0=ShiftRegister(correctedSum(63,0),2)
  val differenceMinus=LdeArithmetic.subtract(difference,p,65)
  val result1Wide=RegNext(Mux(ShiftRegister(difference>=p,2),differenceMinus,ShiftRegister(difference,2)))
  val result1=result1Wide(63,0)
  val inverse27=ShiftRegister(io.inverse,27)
  def half(value: UInt): UInt = {
    val plusP=LdeArithmetic.add(Cat(0.U(1.W),value),p,65)
    val plain=ShiftRegister(Cat(0.U(1.W),value),2)
    val extended=Mux(inverse27 && ShiftRegister(value(0),2),plusP,plain)
    RegNext(Mux(inverse27,extended(64,1),extended(63,0)))
  }
  io.out0:=half(result0);io.out1:=half(result1)
  io.outValid:=ShiftRegister(io.valid,LdeGoldilocksButterfly.Latency,false.B,true.B)
}
