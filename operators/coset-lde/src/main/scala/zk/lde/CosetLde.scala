/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/src/main/scala/zk/lde/CosetLde.scala
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
import ntt.asic.AsicSram1R1W

case class LdeConfig(maxLog: Int) {
  require(maxLog >= 4 && maxLog <= 18)
  val size = 1 << maxLog
  val beats = size / 2
}
class LdeTask extends Bundle {
  val tag = UInt(32.W); val mode = UInt(2.W)
  val logN = UInt(5.W); val logB = UInt(5.W); val g = UInt(64.W)
}
class LdeBeat extends Bundle {
  val data = Vec(2, UInt(64.W)); val last = Bool(); val count = UInt(2.W)
}
class LdeTable extends Bundle {
  val inverse = Bool(); val data = UInt(64.W); val last = Bool()
}
class LdeCompletion extends Bundle {
  val tag = UInt(32.W); val mode = UInt(2.W); val status = UInt(2.W)
}
class LdeIO extends Bundle {
  val task = Flipped(Decoupled(new LdeTask))
  val table = Flipped(Decoupled(new LdeTable))
  val input = Flipped(Decoupled(new LdeBeat))
  val output = Decoupled(new LdeBeat)
  val completion = Decoupled(new LdeCompletion)
  val configured = Output(UInt(2.W))
}
class LdePlane(c: LdeConfig) extends Module {
  val io = IO(new Bundle {
    val re = Input(Bool()); val ri = Input(Vec(2, UInt(c.maxLog.W)))
    val rd = Output(Vec(2, UInt(64.W)))
    val we = Input(Vec(2, Bool())); val wi = Input(Vec(2, UInt(c.maxLog.W)))
    val wd = Input(Vec(2, UInt(64.W)))
  })
  val banks = Seq.fill(2)(Module(new AsicSram1R1W(c.beats)))
  val rb = io.ri.map(_.xorR); val wb = io.wi.map(_.xorR)
  when(io.re) { assert(rb(0) =/= rb(1)) }
  when(io.we(0) && io.we(1)) { assert(wb(0) =/= wb(1)) }
  for (b <- 0 until 2) {
    val hit0 = wb(0) === (b == 1).B
    banks(b).io.readEnable := io.re
    banks(b).io.readAddress := Mux(rb(0) === (b == 1).B, io.ri(0)(c.maxLog-1,1), io.ri(1)(c.maxLog-1,1))
    banks(b).io.writeEnable := (io.we(0) && hit0) || (io.we(1) && !hit0)
    banks(b).io.writeAddress := Mux(hit0, io.wi(0)(c.maxLog-1,1), io.wi(1)(c.maxLog-1,1))
    banks(b).io.writeData := Mux(hit0, io.wd(0), io.wd(1))
  }
  for (i <- 0 until 2) { io.rd(i) := Mux(RegNext(rb(i)), banks(1).io.readData, banks(0).io.readData) }
}

class GoldilocksCosetLde(c: LdeConfig) extends Module {
  val io = IO(new LdeIO)
  val p = "hffffffff00000001".U(64.W)
  object S extends ChiselEnum {
    val Idle, Load, NttIssue, NttDrain, SeedIssue, SeedWait, ScaleRead,
        ScaleValue, ScalePower, ScaleDrain, Pad, Output, Complete = Value
  }
  val state = RegInit(S.Idle)
  val taskQueue=Module(new Queue(new LdeTask,1,pipe=false,flow=false))
  val tableQueue=Module(new Queue(new LdeTable,2,pipe=false,flow=false))
  val inputQueue=Module(new Queue(new LdeBeat,2,pipe=false,flow=false))
  taskQueue.io.enq.bits:=io.task.bits;taskQueue.io.enq.valid:=io.task.valid && state===S.Idle
  io.task.ready:=taskQueue.io.enq.ready && state===S.Idle
  tableQueue.io.enq.bits:=io.table.bits;tableQueue.io.enq.valid:=io.table.valid && state===S.Idle
  io.table.ready:=tableQueue.io.enq.ready && state===S.Idle
  val taskIn=taskQueue.io.deq;val tableIn=tableQueue.io.deq;val inputIn=inputQueue.io.deq
  tableIn.ready:=state===S.Idle
  taskIn.ready:=state===S.Idle && !tableIn.valid
  inputIn.ready:=state===S.Load
  val inputAccepted=RegInit(0.U(c.maxLog.W));val inputAllAccepted=RegInit(false.B)
  inputQueue.io.enq.bits:=io.input.bits
  inputQueue.io.enq.valid:=io.input.valid && state===S.Load && !inputAllAccepted
  io.input.ready:=inputQueue.io.enq.ready && state===S.Load && !inputAllAccepted
  val active = Reg(new LdeTask)
  val status = RegInit(0.U(2.W))
  val logM = Reg(UInt(5.W))
  val sizeN = Reg(UInt((c.maxLog+1).W))
  val sizeM = Reg(UInt((c.maxLog+1).W))
  val halfN = sizeN >> 1; val halfM = sizeM >> 1
  val source = RegInit(false.B)
  val inverse = RegInit(false.B)
  val stage = RegInit(0.U(5.W))
  val idx = RegInit(0.U(c.maxLog.W))
  val outIdx = RegInit(0.U(c.maxLog.W))
  val allOutput = RegInit(false.B)
  val configured = RegInit(VecInit(Seq.fill(2)(false.B)))
  val tp = RegInit(VecInit(Seq.fill(2)(0.U((c.maxLog-1).W))))
  val good = RegInit(VecInit(Seq.fill(2)(true.B)))
  val planes = Seq.fill(2)(Module(new LdePlane(c)))
  val table = Module(new AsicSram1R1W(c.size))
  io.configured := configured.asUInt
  val td = tableIn.bits.inverse.asUInt
  val tl = tp(td) === (c.beats-1).U
  table.io.writeEnable := tableIn.fire && tableIn.bits.data < p
  table.io.writeAddress := Cat(tableIn.bits.inverse, tp(td))
  table.io.writeData := tableIn.bits.data
  when(tableIn.fire) {
    configured(td) := false.B
    good(td) := good(td) && tableIn.bits.data < p && (tableIn.bits.last === tl)
    when(tl) {
      configured(td) := good(td) && tableIn.bits.data < p && tableIn.bits.last
      tp(td) := 0.U; good(td) := true.B
    }.otherwise { tp(td) := tp(td)+1.U }
  }
  io.completion.valid := state === S.Complete
  io.completion.bits.tag := active.tag; io.completion.bits.mode := active.mode
  io.completion.bits.status := status
  val legal = taskIn.bits.mode <= 1.U && taskIn.bits.logN >= 4.U &&
    (taskIn.bits.logN +& taskIn.bits.logB) <= c.maxLog.U && taskIn.bits.g =/= 0.U && taskIn.bits.g < p
  val tableReady = configured(0) && (taskIn.bits.mode === 1.U || configured(1))
  when(taskIn.fire) {
    active := taskIn.bits; idx := 0.U; stage := 0.U; source := false.B
    logM := (taskIn.bits.logN +& taskIn.bits.logB)(4,0)
    sizeN := (1.U((c.maxLog+1).W) << taskIn.bits.logN)(c.maxLog,0)
    sizeM := (1.U((c.maxLog+1).W) << (taskIn.bits.logN +& taskIn.bits.logB))(c.maxLog,0)
    inputAccepted:=0.U;inputAllAccepted:=false.B
    inverse := taskIn.bits.mode === 0.U; outIdx := 0.U; allOutput := false.B
    status := Mux(!legal, 1.U, Mux(!tableReady, 3.U, 0.U))
    state := Mux(legal && tableReady, S.Load, S.Complete)
  }
  when(io.input.fire) {inputAccepted:=inputAccepted+1.U;when(inputAccepted===halfN-1.U){inputAllAccepted:=true.B}}
  val bad = inputIn.bits.count =/= 2.U || inputIn.bits.last =/= (idx === halfN-1.U) ||
    inputIn.bits.data.map(_ >= p).reduce(_ || _)
  when(inputIn.fire) {
    when(bad) { status := 2.U }
    idx := idx+1.U
    when(idx === halfN-1.U) {
      idx := 0.U
      state := Mux(status =/= 0.U || bad, S.Complete, Mux(inverse, S.NttIssue, S.SeedIssue))
    }
  }
  val activeLog = Mux(inverse, active.logN, logM)
  val half = Mux(inverse, halfN, halfM)
  val span = (1.U(c.maxLog.W) << (activeLog-1.U-stage))(c.maxLog-1,0)
  val mask = span-1.U
  val group = idx & ~mask
  val src0 = ((group << 1) | (idx & mask))(c.maxLog-1,0)
  val src1 = src0 | span
  val nttIssue = state === S.NttIssue
  table.io.readEnable := nttIssue
  val twAddr = (group << (c.maxLog.U-activeLog))(c.maxLog-2,0)
  table.io.readAddress := Cat(inverse, twAddr)
  when(nttIssue) { idx := idx+1.U; when(idx === half-1.U) { state := S.NttDrain } }
  val nttValid = ShiftRegister(nttIssue, 2, false.B, true.B)
  val issueLatency=LdeGoldilocksButterfly.Latency+3
  val scalarLatency=LdeGoldilocksButterfly.Latency+1
  val nttIdx = ShiftRegister(idx, issueLatency)
  val nttLast = ShiftRegister(nttIssue && idx === half-1.U, issueLatency, false.B, true.B)
  val sourceData = Mux(source, planes(1).io.rd, planes(0).io.rd)
  val nttA = RegNext(sourceData(0)); val nttB = RegNext(sourceData(1)); val nttT = RegNext(table.io.readData)
  val powers = Reg(Vec(18, UInt(64.W)))
  val seed = RegInit(1.U(64.W)); val seedIdx = RegInit(0.U(5.W))
  val stride = Reg(UInt(64.W)); val slot = RegInit(0.U(5.W))
  val scalarIdx = RegInit(0.U(c.maxLog.W))
  val padIdx = RegInit(0.U(c.maxLog.W))
  val scalarValid = state === S.SeedIssue || state === S.ScaleValue || state === S.ScalePower
  // 0 NTT, 1 coefficient, 2 power update, 3 seed.
  val kind = Mux(nttValid, 0.U, Mux(state === S.ScaleValue, 1.U, Mux(state === S.ScalePower, 2.U, 3.U)))
  val butterfly = Module(new LdeGoldilocksButterfly)
  butterfly.io.valid := RegNext(nttValid || scalarValid,false.B)
  butterfly.io.inverse := RegNext(nttValid && inverse)
  butterfly.io.a := RegNext(Mux(nttValid, nttA, 0.U))
  butterfly.io.b := RegNext(Mux(nttValid, nttB, Mux(state === S.SeedIssue, seed,
    Mux(state === S.ScaleValue, Mux(scalarIdx(0), sourceData(1), sourceData(0)), powers(slot)))))
  butterfly.io.twiddle := RegNext(Mux(nttValid, nttT, Mux(state === S.SeedIssue, active.g,
    Mux(state === S.ScaleValue, powers(slot), stride))))
  val outKind = ShiftRegister(kind, scalarLatency)
  val outScalarIdx = ShiftRegister(scalarIdx, scalarLatency)
  val outSlot = ShiftRegister(slot, scalarLatency)
  val lastScalar = ShiftRegister(state === S.ScalePower && scalarIdx === sizeN-1.U, scalarLatency, false.B, true.B)
  when(butterfly.io.outValid && outKind === 0.U && nttLast) {
    source := !source; idx := 0.U
    when(stage === activeLog-1.U) {
      stage := 0.U
      when(inverse) { inverse := false.B; state := S.SeedIssue }.otherwise { state := S.Output }
    }.otherwise { stage := stage+1.U; state := S.NttIssue }
  }
  when(state === S.SeedIssue) {
    when(seedIdx === 0.U) { powers(0) := 1.U }
    state := S.SeedWait
  }
  when(butterfly.io.outValid && outKind === 3.U) {
    seed := butterfly.io.out0
    when(seedIdx === 17.U) {
      stride := butterfly.io.out0; scalarIdx := 0.U; slot := 0.U; state := S.ScaleRead
    }.otherwise { powers(seedIdx+1.U) := butterfly.io.out0; seedIdx := seedIdx+1.U; state := S.SeedIssue }
  }
  when(state === S.ScaleRead) { state := S.ScaleValue }
  when(state === S.ScaleValue) { state := S.ScalePower }
  when(state === S.ScalePower) {
    when(scalarIdx === sizeN-1.U) { state := S.ScaleDrain }.otherwise {
      scalarIdx := scalarIdx+1.U; slot := Mux(slot === 17.U, 0.U, slot+1.U); state := S.ScaleValue
    }
  }
  when(butterfly.io.outValid && outKind === 2.U) {
    powers(outSlot) := butterfly.io.out0
    when(lastScalar) {
      source := !source; padIdx := sizeN(c.maxLog-1,0)
      seed := 1.U; seedIdx := 0.U
      state := Mux(sizeN === sizeM, S.NttIssue, S.Pad)
    }
  }
  when(state === S.Pad) {
    padIdx := padIdx+2.U
    when(padIdx === sizeM-2.U) { idx := 0.U; stage := 0.U; state := S.NttIssue }
  }
  val q = Module(new Queue(new LdeBeat, 4, pipe=false, flow=false))
  io.output <> q.io.deq
  val pending = RegInit(false.B)
  val outputRead = state === S.Output && !allOutput && (q.io.count +& pending.asUInt) < 4.U
  pending := outputRead
  q.io.enq.valid := pending; q.io.enq.bits.data := sourceData
  q.io.enq.bits.last := RegNext(outIdx === halfM-1.U); q.io.enq.bits.count := 2.U
  when(pending) { assert(q.io.enq.ready) }
  when(outputRead) { outIdx := outIdx+1.U; when(outIdx === halfM-1.U) { allOutput := true.B } }
  when(io.output.fire && io.output.bits.last) { state := S.Complete }
  when(io.completion.fire) { state := S.Idle }
  val scaleRead = state === S.ScaleRead || (state === S.ScalePower && scalarIdx =/= sizeN-1.U)
  val scaleReadIdx = Mux(state === S.ScaleRead, 0.U, scalarIdx+1.U)
  val alignedScale = Cat(scaleReadIdx(c.maxLog-1,1), 0.U(1.W))
  for (b <- 0 until 2) {
    val isSource = source === (b == 1).B
    val load = inputIn.fire && (b == 0).B
    val ntWrite = butterfly.io.outValid && outKind === 0.U && !isSource
    val scWrite = butterfly.io.outValid && outKind === 1.U && !isSource
    val pad = state === S.Pad && isSource
    planes(b).io.re := isSource && (nttIssue || scaleRead || outputRead)
    planes(b).io.ri(0) := Mux(outputRead, (outIdx << 1), Mux(scaleRead, alignedScale, src0))
    planes(b).io.ri(1) := Mux(outputRead, (outIdx << 1)|1.U, Mux(scaleRead, alignedScale|1.U, src1))
    planes(b).io.we(0) := load || ntWrite || scWrite || pad
    planes(b).io.we(1) := load || ntWrite || pad
    planes(b).io.wi(0) := Mux(load, idx << 1, Mux(pad, padIdx, Mux(scWrite, outScalarIdx, nttIdx)))
    planes(b).io.wi(1) := Mux(load, (idx << 1)|1.U, Mux(pad, padIdx|1.U,
      nttIdx | half(c.maxLog-1,0)))
    planes(b).io.wd(0) := Mux(load, inputIn.bits.data(0), Mux(pad, 0.U, butterfly.io.out0))
    planes(b).io.wd(1) := Mux(load, inputIn.bits.data(1), Mux(pad, 0.U, butterfly.io.out1))
  }
  // Only bound verification layers hold counters; none enter product.f / DC.
  layer.block(layers.Verification) {
    val tick = RegInit(0.U(64.W)); tick := tick+1.U
    val counts = RegInit(VecInit(Seq.fill(4)(0.U(64.W))))
    val clocks = RegInit(0.U(64.W)); val busy = RegInit(0.U(64.W))
    val occupied = RegInit(0.U(64.W))
    val reads = RegInit(VecInit(Seq.fill(4)(0.U(64.W))))
    val writes = RegInit(VecInit(Seq.fill(4)(0.U(64.W))))
    val twiddleReads = RegInit(0.U(64.W))
    val history = RegInit(0.U(17.W)); history := Cat(history(15,0), butterfly.io.valid)
    val metricKind=RegNext(kind)
    val inFlight=RegInit(false.B)
    when(io.task.fire){inFlight:=true.B}.elsewhen(io.completion.fire){inFlight:=false.B}
    val phase = Mux(state === S.ScalePower, S.ScaleValue.asUInt,
      Mux(state === S.SeedWait, S.SeedIssue.asUInt, Mux(state === S.NttDrain, S.NttIssue.asUInt, state.asUInt)))
    val prev = RegNext(phase)
    when(io.task.fire) {
      counts.foreach(_ := 0.U); clocks := 0.U; busy := 0.U; occupied := 0.U
      reads.foreach(_ := 0.U); writes.foreach(_ := 0.U); twiddleReads := 0.U
      printf(p"ACCEPT t=${tick} tag=${io.task.bits.tag} mode=${io.task.bits.mode}\n")
    }.elsewhen(inFlight) {
      clocks := clocks+1.U
      when(butterfly.io.valid) { counts(metricKind) := counts(metricKind)+1.U; busy := busy+1.U }
      when(butterfly.io.valid || history.orR) { occupied := occupied+1.U }
      when(table.io.readEnable) { twiddleReads := twiddleReads+1.U }
      for (plane <- 0 until 2; bank <- 0 until 2) {
        when(planes(plane).io.re) { reads(2*plane+bank) := reads(2*plane+bank)+1.U }
        val hit = (planes(plane).io.we(0) && planes(plane).io.wi(0).xorR === (bank==1).B) ||
          (planes(plane).io.we(1) && planes(plane).io.wi(1).xorR === (bank==1).B)
        when(hit) { writes(2*plane+bank) := writes(2*plane+bank)+1.U }
      }
    }
    when(prev =/= phase) { printf(p"PHASE t=${tick} tag=${active.tag} state=${phase} inverse=${inverse}\n") }
    when(io.completion.fire) {
      printf(p"METRIC t=${tick} tag=${active.tag} mode=${active.mode} clocks=${clocks} busy=${busy} occupied=${occupied} ntt=${counts(0)} coefficient=${counts(1)} power=${counts(2)} seed=${counts(3)} status=${status}\n")
      printf(p"MEMORY tag=${active.tag} status=${status} r0=${reads(0)} r1=${reads(1)} r2=${reads(2)} r3=${reads(3)} w0=${writes(0)} w1=${writes(1)} w2=${writes(2)} w3=${writes(3)} twiddle=${twiddleReads}\n")
    }
  }
}
