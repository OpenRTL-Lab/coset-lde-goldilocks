/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/cmodel/arithmetic_vectors.c
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

/* 与RTL独立的128位C整数模运算；复用已核验参考的域定义。 */
#define main lde_reference_main
#include "reference.c"
#undef main
int main(int argc,char **argv){
  assert(argc==2);FILE *f=fopen(argv[1],"w");assert(f);
  const uint64_t edge[]={0,1,2,UINT64_C(0xffffffff),UINT64_C(0x100000000),P-2,P-1};
  for(unsigned i=0;i<1024;i++){
    uint64_t a=i<343?edge[i%7]:random64();
    uint64_t b=i<343?edge[(i/7)%7]:random64();
    uint64_t w=i<343?edge[(i/49)%7]:random64();
    unsigned inverse=i&1,valid=i%13!=0;uint64_t v=mul(b,w),x=add(a,v),y=sub(a,v);
    if(inverse){x=mul(x,(P+1)/2);y=mul(y,(P+1)/2);}
    fprintf(f,"%x%016"PRIx64"%016"PRIx64"%016"PRIx64"%016"PRIx64"%016"PRIx64"%016"PRIx64"\n",
      inverse+2*valid,y,x,mul(a,b),w,b,a);
  }
  fclose(f);puts("ARITHMETIC_REFERENCE_PASS vectors=1024");return 0;
}
