/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/sim/arithmetic_driver.cpp
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

#include "VLdeArithmeticSim.h"
#include "verilated.h"
#include <memory>
#include <cstdio>
int main(int argc,char**argv){
  Verilated::commandArgs(argc,argv);auto d=std::make_unique<VLdeArithmeticSim>();
  auto step=[&](){d->clock=0;d->eval();d->clock=1;d->eval();};
  d->reset=1;d->io_trigger=0;d->io_negative=argc>1;
  for(int i=0;i<4;i++)step();d->reset=0;d->io_trigger=1;step();d->io_trigger=0;
  for(int i=0;i<3000&&!d->io_done;i++)step();
  printf("ARITHMETIC_PASS pass=%u checked=%u\n",unsigned(d->io_done&&d->io_pass),unsigned(d->io_checked));
  bool ok=d->io_done&&d->io_pass&&d->io_checked==1024;d->final();return ok?0:1;
}
