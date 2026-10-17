/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/src/main/scala/zk/lde/Environment.scala
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
import chisel3.util.experimental.loadMemoryFromFileInline

class RomWord(w:Int) extends Bundle { val data=UInt(w.W); val last=Bool() }
class EnvRom(w:Int, depth:Int, file:String) extends Module {
  val aw=log2Ceil(depth); val cw=log2Ceil(depth+1)
  val io=IO(new Bundle {val start=Input(Bool());val base=Input(UInt(aw.W));val length=Input(UInt(cw.W));val out=Decoupled(new RomWord(w))})
  val mem=SyncReadMem(depth,UInt(w.W));loadMemoryFromFileInline(mem,file)
  val q=Module(new Queue(new RomWord(w),2,pipe=true))
  val pos=RegInit(0.U(aw.W));val left=RegInit(0.U(cw.W));val pending=RegInit(false.B)
  val issue= !io.start && left=/=0.U && ((q.io.count+&pending.asUInt)<2.U || io.out.fire)
  val data=mem.read(pos,issue);val last=RegEnable(left===1.U,issue)
  pending:=issue;q.io.enq.valid:=pending;q.io.enq.bits.data:=data;q.io.enq.bits.last:=last
  io.out<>q.io.deq
  when(pending){assert(q.io.enq.ready)}
  when(io.start){assert(left===0.U && !pending && q.io.count===0.U);pos:=io.base;left:=io.length}
    .elsewhen(issue){pos:=pos+1.U;left:=left-1.U}
}
class CosetLdeSim(c:LdeConfig,ln:Int,lb:Int,tasks:Int,mode:Int,dir:String) extends Module {
  val io=IO(new Bundle {val trigger=Input(Bool());val variant=Input(UInt(3.W));val done=Output(Bool());val pass=Output(Bool());
    val checked=Output(UInt(64.W));val mismatches=Output(UInt(32.W));val cycles=Output(UInt(64.W));val abortReady=Output(Bool())})
  val n=1<<ln;val m=1<<(ln+lb);val nbeats=n/2;val mbeats=m/2
  val dut=Module(new GoldilocksCosetLde(c))
  val in=Module(new EnvRom(128,tasks*nbeats,s"$dir/input.hex"))
  val expected=Module(new EnvRom(128,tasks*mbeats,s"$dir/expected.hex"))
  val fwd=Module(new EnvRom(64,c.beats,s"$dir/forward.hex"));val inv=Module(new EnvRom(64,c.beats,s"$dir/inverse.hex"))
  object E extends ChiselEnum {val Idle, Unconfigured, UnconfiguredWait, BadTables, BadTablesCheck, BadTableProbeWait,
    TablesStart, Tables, Prefetch, Task, Input, Wait,
    BadParam, BadParamWait, BadStart, BadTask, BadInput, BadWait, Done=Value}
  val state=RegInit(E.Idle);val frame=RegInit(0.U(log2Ceil(tasks+1).W));val badIndex=RegInit(0.U(c.maxLog.W))
  val paramProbe=RegInit(0.U(3.W));val frameProbe=RegInit(0.U(2.W))
  val tableProbe=RegInit(0.U(2.W))
  val tick=RegInit(0.U(64.W));val checked=RegInit(0.U(64.W));val errors=RegInit(0.U(32.W))
  val variant=Reg(UInt(3.W));val direction=RegInit(false.B)
  val outputs=RegInit(0.U(c.maxLog.W));val inputs=RegInit(0.U(c.maxLog.W))
  val acceptTick=Reg(UInt(64.W));val firstTick=Reg(UInt(64.W));val lastTick=Reg(UInt(64.W))
  val running=state=/=E.Idle && state=/=E.Done
  when(running){tick:=tick+1.U;assert(tick<200000000.U)}
  io.done:=state===E.Done;io.pass:=errors===0.U;io.checked:=checked;io.mismatches:=errors;io.cycles:=tick
  io.abortReady:=state===E.Wait && frame===0.U && outputs===0.U
  val g=VecInit(Seq(BigInt(1),BigInt(7),BigInt("ffffffff00000000",16),BigInt(19)).map(_.U(64.W)))
  dut.io.task.valid:=false.B;dut.io.task.bits.tag:=frame+1.U
  dut.io.task.bits.mode:=(if(mode==2) frame(0).asUInt else mode.U)
  dut.io.task.bits.logN:=ln.U;dut.io.task.bits.logB:=lb.U;dut.io.task.bits.g:=g(frame(1,0))
  dut.io.table.valid:=false.B;dut.io.table.bits.inverse:=direction;dut.io.table.bits.data:=Mux(direction,inv.io.out.bits.data,fwd.io.out.bits.data)
  dut.io.table.bits.last:=Mux(direction,inv.io.out.bits.last,fwd.io.out.bits.last)
  dut.io.input.valid:=false.B;dut.io.input.bits.count:=2.U;dut.io.input.bits.last:=in.io.out.bits.last
  dut.io.input.bits.data(0):=in.io.out.bits.data(63,0);dut.io.input.bits.data(1):=in.io.out.bits.data(127,64)
  dut.io.output.ready:=expected.io.out.valid && (variant=/=1.U || tick(3,0)=/=0.U)
  dut.io.completion.ready:=(variant=/=1.U || tick(2,0)===0.U)
  for(r<-Seq(in,expected,fwd,inv)){r.io.start:=false.B;r.io.base:=0.U;r.io.length:=1.U;r.io.out.ready:=false.B}
  expected.io.out.ready:=dut.io.output.fire
  when(dut.io.output.fire){
    val reference=Mux(variant===3.U && checked===0.U,expected.io.out.bits.data^1.U,expected.io.out.bits.data)
    val actual=Cat(dut.io.output.bits.data(1),dut.io.output.bits.data(0))
    when(actual=/=reference){errors:=errors+1.U;printf(p"MISMATCH frame=${frame} beat=${outputs} got=${Hexadecimal(actual)} expected=${Hexadecimal(reference)}\n")}
    assert(dut.io.output.bits.last===expected.io.out.bits.last && dut.io.output.bits.count===2.U)
    outputs:=outputs+1.U;checked:=checked+2.U;lastTick:=tick
  }
  when(state===E.Idle && io.trigger){variant:=io.variant;state:=E.Unconfigured}
  when(state===E.Unconfigured){dut.io.task.valid:=true.B;when(dut.io.task.fire){state:=E.UnconfiguredWait}}
  when(state===E.UnconfiguredWait && dut.io.completion.fire){assert(dut.io.completion.bits.status===3.U)
    state:=Mux(variant===1.U,E.BadTables,E.TablesStart);badIndex:=0.U}
  when(state===E.BadTables){
    dut.io.table.valid:=true.B;dut.io.table.bits.inverse:=false.B
    dut.io.table.bits.data:=Mux(tableProbe===1.U && badIndex===0.U,"hffffffff00000001".U,1.U)
    dut.io.table.bits.last:=Mux(tableProbe===0.U,badIndex===0.U,badIndex===(c.beats-1).U)
    when(dut.io.table.fire){badIndex:=badIndex+1.U;when(badIndex===(c.beats-1).U){state:=E.BadTablesCheck}}
  }
  when(state===E.BadTablesCheck){assert(dut.io.configured===0.U);dut.io.task.valid:=true.B
    when(dut.io.task.fire){state:=E.BadTableProbeWait}}
  when(state===E.BadTableProbeWait && dut.io.completion.fire){assert(dut.io.completion.bits.status===3.U)
    printf(p"BAD_TABLE_PASS probe=${tableProbe}\n")
    when(tableProbe===1.U){state:=E.TablesStart}.otherwise{tableProbe:=tableProbe+1.U;badIndex:=0.U;state:=E.BadTables}}
  when(state===E.TablesStart){
    fwd.io.start:=true.B;fwd.io.length:=c.beats.U;inv.io.start:=true.B;inv.io.length:=c.beats.U;state:=E.Tables
  }
  when(state===E.Tables){
    dut.io.table.valid:=Mux(direction,inv.io.out.valid,fwd.io.out.valid)
    fwd.io.out.ready:=dut.io.table.ready && !direction;inv.io.out.ready:=dut.io.table.ready && direction
    when(dut.io.table.fire && dut.io.table.bits.last){when(direction){state:=E.BadParam}.otherwise{direction:=true.B}}
  }
  when(state===E.Prefetch){
    in.io.start:=true.B;in.io.base:=frame*nbeats.U;in.io.length:=nbeats.U
    expected.io.start:=true.B;expected.io.base:=frame*mbeats.U;expected.io.length:=mbeats.U
    inputs:=0.U;outputs:=0.U;state:=E.Task
  }
  when(state===E.Task){dut.io.task.valid:=true.B
    when(dut.io.task.fire){acceptTick:=tick;state:=E.Input;printf(p"START frame=${frame} t=${tick}\n")}}
  when(state===E.Input){
    dut.io.input.valid:=in.io.out.valid && (variant=/=1.U || tick(2,0)=/=0.U)
    in.io.out.ready:=dut.io.input.ready && dut.io.input.valid
    when(dut.io.input.fire){when(inputs===0.U){firstTick:=tick};inputs:=inputs+1.U;when(in.io.out.bits.last){state:=E.Wait}}
  }
  when(state===E.Wait && dut.io.completion.fire){
    assert(dut.io.completion.bits.status===0.U && dut.io.completion.bits.tag===frame+1.U && outputs===mbeats.U)
    printf(p"RESULT frame=${frame} mode=${dut.io.completion.bits.mode} a=${acceptTick} f=${firstTick} o=${lastTick} c=${tick} latency=${tick-acceptTick}\n")
    when(frame===(tasks-1).U){state:=E.Done;printf(p"ENV_PASS checked=${checked} mismatches=${errors} cycles=${tick}\n")}.otherwise{
      in.io.start:=true.B;in.io.base:=(frame+1.U)*nbeats.U;in.io.length:=nbeats.U
      expected.io.start:=true.B;expected.io.base:=(frame+1.U)*mbeats.U;expected.io.length:=mbeats.U
      inputs:=0.U;outputs:=0.U;frame:=frame+1.U;state:=E.Task
    }
  }
  when(state===E.BadParam){dut.io.task.valid:=true.B
    when(paramProbe===0.U){dut.io.task.bits.g:=0.U}
    when(paramProbe===1.U){dut.io.task.bits.g:="hffffffff00000001".U}
    when(paramProbe===2.U){dut.io.task.bits.mode:=2.U}
    when(paramProbe===3.U){dut.io.task.bits.logN:=3.U}
    when(paramProbe===4.U){dut.io.task.bits.logN:=(c.maxLog+1).U}
    when(paramProbe===5.U){dut.io.task.bits.logB:=(c.maxLog+1).U}
    when(dut.io.task.fire){state:=E.BadParamWait}}
  when(state===E.BadParamWait && dut.io.completion.fire){assert(dut.io.completion.bits.status===1.U)
    printf(p"BAD_PARAM_PASS probe=${paramProbe}\n")
    when(paramProbe===5.U){state:=E.BadStart}.otherwise{paramProbe:=paramProbe+1.U;state:=E.BadParam}}
  when(state===E.BadStart){badIndex:=0.U;state:=E.BadTask}
  when(state===E.BadTask){dut.io.task.valid:=true.B;when(dut.io.task.fire){state:=E.BadInput}}
  when(state===E.BadInput){dut.io.input.valid:=true.B;dut.io.input.bits.data.foreach(_:=0.U)
    dut.io.input.bits.last:=Mux(frameProbe===0.U,badIndex===0.U,badIndex===(nbeats-1).U)
    when(frameProbe===1.U && badIndex===0.U){dut.io.input.bits.data(0):="hffffffff00000001".U}
    dut.io.input.bits.count:=Mux(frameProbe===2.U,1.U,2.U)
    when(dut.io.input.fire){badIndex:=badIndex+1.U;when(badIndex===(nbeats-1).U){state:=E.BadWait}}}
  when(state===E.BadWait && dut.io.completion.fire){assert(dut.io.completion.bits.status===2.U)
    printf(p"BAD_FRAME_PASS probe=${frameProbe} consumed=${badIndex}\n")
    when(frameProbe===2.U){state:=E.Prefetch}.otherwise{frameProbe:=frameProbe+1.U;state:=E.BadStart}}
  val heldOutput=RegNext(dut.io.output.valid && !dut.io.output.ready,false.B)
  val heldBits=RegEnable(dut.io.output.bits.asUInt,dut.io.output.valid && !dut.io.output.ready)
  when(heldOutput){assert(dut.io.output.valid && dut.io.output.bits.asUInt===heldBits,"output changed while stalled")}
  val heldCompletion=RegNext(dut.io.completion.valid && !dut.io.completion.ready,false.B)
  val heldCompletionBits=RegEnable(dut.io.completion.bits.asUInt,dut.io.completion.valid && !dut.io.completion.ready)
  when(heldCompletion){assert(dut.io.completion.valid && dut.io.completion.bits.asUInt===heldCompletionBits)}
}
