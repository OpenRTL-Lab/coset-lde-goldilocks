/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/sim/driver.cpp
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

#include "VCosetLdeSim.h"
#include "verilated.h"
#include <cstdio>
#include <cstdlib>
#include <memory>
int main(int argc,char**argv){
  Verilated::commandArgs(argc,argv);auto d=std::make_unique<VCosetLdeSim>();
  auto step=[&](){d->clock=0;d->eval();d->clock=1;d->eval();};
  int variant=argc>1?atoi(argv[1]):0;d->reset=1;d->io_trigger=0;d->io_variant=variant;
  for(int i=0;i<4;i++)step();d->reset=0;d->io_trigger=1;step();d->io_trigger=0;
  bool resetDone=false;
  for(unsigned long long i=0;i<200000000ULL && !d->io_done;i++){
    step();if(variant==2 && !resetDone && d->io_abortReady){
      if(d->io_checked!=0)return 2;d->reset=1;for(int j=0;j<4;j++)step();d->reset=0;
      d->io_variant=0;d->io_trigger=1;step();d->io_trigger=0;resetDone=true;
    }
  }
  printf("SIM_DONE pass=%u checked=%llu mismatches=%u cycles=%llu reset=%u\n",unsigned(d->io_done&&d->io_pass),
    (unsigned long long)d->io_checked,unsigned(d->io_mismatches),(unsigned long long)d->io_cycles,unsigned(resetDone));
  bool okay=d->io_done&&d->io_pass&&(variant!=2||resetDone);d->final();return okay?0:1;
}
