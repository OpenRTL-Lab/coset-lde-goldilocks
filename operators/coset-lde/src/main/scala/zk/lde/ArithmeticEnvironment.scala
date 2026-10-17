/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/src/main/scala/zk/lde/ArithmeticEnvironment.scala
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

/** C向量逐拍对照。验证层ROM和参考流水绝不纳入产品闭包。 */
class LdeArithmeticSim(file:String) extends Module {
  val io=IO(new Bundle {val trigger=Input(Bool());val negative=Input(Bool());val done=Output(Bool());
    val pass=Output(Bool());val checked=Output(UInt(32.W))})
  val rom=Module(new EnvRom(386,1024,file));rom.io.start:=io.trigger;rom.io.base:=0.U;rom.io.length:=1024.U
  rom.io.out.ready:=true.B
  val d=rom.io.out.bits.data;val take=rom.io.out.fire
  val mul=Module(new LdeGoldilocksMul);mul.io.a:=d(63,0);mul.io.b:=d(127,64)
  val bf=Module(new LdeGoldilocksButterfly);bf.io.a:=d(63,0);bf.io.b:=d(127,64)
  bf.io.twiddle:=d(191,128);bf.io.inverse:=d(384);bf.io.valid:=take && d(385)
  val mv=ShiftRegister(take,18,false.B,true.B);val bv=ShiftRegister(take && d(385),28,false.B,true.B)
  val mExpected=ShiftRegister(d(255,192),18);val bExpected=ShiftRegister(d(383,256),28)
  val error=RegInit(false.B);val checks=RegInit(0.U(32.W));val first=RegInit(true.B)
  when(mv){checks:=checks+1.U
    when(mul.io.product=/=Mux(io.negative && first,mExpected^1.U,mExpected)){error:=true.B};first:=false.B}
  when(bv && Cat(bf.io.out1,bf.io.out0)=/=bExpected){error:=true.B}
  assert(bf.io.outValid===bv)
  val done=RegInit(false.B)
  when(ShiftRegister(take && rom.io.out.bits.last,29,false.B,true.B)){done:=true.B
    printf(p"ARITHMETIC_DONE checked=${checks} error=${error} mul_latency=18 butterfly_latency=28\n")}
  io.done:=done;io.pass:= !error;io.checked:=checks
}
