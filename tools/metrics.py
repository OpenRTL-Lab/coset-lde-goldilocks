# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Coset-LDE Hardware Accelerator
# Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
# Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
# File: tools/metrics.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""Cycle-based latency, throughput, and valid-operation accounting."""
import re
def records(log,prefix):
    return [dict((k,int(v)) for k,v in re.findall(r'(\w+)=\s*(\d+)',s)) for s in re.findall('^'+prefix+r' (.*)$',log,re.M)]
def performance(log,ln,lb,tasks):
    frames=records(log,'RESULT');work=[r for r in records(log,'METRIC') if r['status']==0]
    memory=[r for r in records(log,'MEMORY') if r['status']==0]
    phases=records(log,'PHASE')
    assert len(frames)==len(work)==tasks
    n=1<<ln;m=n<<lb
    for f,w in zip(frames,work):
        assert w['ntt']==(m//2*(ln+lb)+(n//2*ln if f['mode']==0 else 0)),w
        assert w['coefficient']==w['power']==n and w['seed']==18,w
        assert w['busy']==w['ntt']+2*n+18 and w['clocks']==f['latency']-1,(f,w)
    groups={};phase_rows=[]
    for f,w in zip(frames,work):
        ps=[q for q in phases if w['t']-f['latency']<q['t']<=w['t'] and q['tag']==w['tag']]
        times={}
        for i,q in enumerate(ps[:-1]):
            key='inverse_ntt' if q['state']==2 and q['inverse'] else 'forward_ntt' if q['state']==2 else str(q['state'])
            times[key]=times.get(key,0)+ps[i+1]['t']-q['t']
        phase_rows.append({'tag':w['tag'],'mode':w['mode'],'phase_clocks':times})
    for mode in (0,1):
        fs=[f for f in frames if f['mode']==mode];ws=[w for w in work if w['mode']==mode]
        if not fs:continue
        row={'latency_min':min(f['latency'] for f in fs),'latency_max':max(f['latency'] for f in fs),
             'tasks':len(fs),'mul_busy_fraction':sum(w['busy'] for w in ws)/sum(f['latency'] for f in fs),
             'mul_occupied_fraction':sum(w['occupied'] for w in ws)/sum(f['latency'] for f in fs),
             'busy_per_task':ws[0]['busy'],'idle_per_task':fs[0]['latency']-ws[0]['busy'],
             'operations':ws[0], 'phases':next(q['phase_clocks'] for q in phase_rows if q['mode']==mode),
             'memory':next(q for q in memory if q['tag']==ws[0]['tag'])}
        if len(fs)==tasks and tasks>=8:
            # 去掉首、尾两任务，从第2至倒数第3完成测饱和串行服务。
            first,last=fs[1],fs[-3];intervals=last['frame']-first['frame'];duration=last['c']-first['c']
            row.update(steady_cycles=duration,steady_intervals=intervals,tasks_per_cycle=intervals/duration,
                       initiation_interval=duration/intervals,input_words_per_cycle=n*intervals/duration,
                       output_words_per_cycle=m*intervals/duration,
                       acceptance_intervals=[fs[i+1]['a']-fs[i]['a'] for i in range(1,tasks-3)])
        groups[str(mode)]=row
    return {'frames':frames,'work':work,'memory':memory,'phase_rows':phase_rows,'modes':groups,'definition':'C-A；连续任务真实握手；全任务有效模乘发射/占用；验证层计数不进入产品'}
