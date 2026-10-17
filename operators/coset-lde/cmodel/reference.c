/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Coset-LDE Hardware Accelerator
 * Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
 * Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
 * File: operators/coset-lde/cmodel/reference.c
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

/* 独立 radix-2 DIT 参考。产品使用 Stockham，参考不用产品地址调度。 */
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <assert.h>
#define P UINT64_C(18446744069414584321)
static uint64_t add(uint64_t a,uint64_t b){return (uint64_t)(((__uint128_t)a+b)%P);}
static uint64_t mul(uint64_t a,uint64_t b){return (uint64_t)((__uint128_t)a*b%P);}
static uint64_t sub(uint64_t a,uint64_t b){return a>=b?a-b:P-(b-a);}
static uint64_t power(uint64_t a,uint64_t b){uint64_t r=1;for(;b;b>>=1,a=mul(a,a))if(b&1)r=mul(r,a);return r;}
static uint64_t root(unsigned l){return power(UINT64_C(1753635133440165772),UINT64_C(1)<<(32-l));}
static void fft(uint64_t *a,unsigned l,int inverse){
  size_t n=(size_t)1<<l;
  uint64_t w=root(l);if(inverse)w=power(w,P-2);
  for(size_t i=1,j=0;i<n;i++){size_t bit=n>>1;for(;j&bit;bit>>=1)j^=bit;j^=bit;if(i<j){uint64_t t=a[i];a[i]=a[j];a[j]=t;}}
  for(size_t len=2;len<=n;len<<=1){uint64_t wl=power(w,n/len);
    for(size_t base=0;base<n;base+=len){uint64_t z=1;
      for(size_t j=0;j<len/2;j++,z=mul(z,wl)){uint64_t u=a[base+j],v=mul(a[base+j+len/2],z);a[base+j]=add(u,v);a[base+j+len/2]=sub(u,v);}
    }
  }
  if(inverse){uint64_t scale=power(n,P-2);for(size_t i=0;i<n;i++)a[i]=mul(a[i],scale);}
}
static uint64_t rngstate=UINT64_C(0x04c05e71);
static uint64_t random64(void){rngstate^=rngstate<<13;rngstate^=rngstate>>7;rngstate^=rngstate<<17;return rngstate%P;}
static void packed(FILE*f,const uint64_t*a,size_t n){for(size_t i=0;i<n;i+=2)fprintf(f,"%016"PRIx64"%016"PRIx64"\n",a[i+1],a[i]);}
static FILE* openfile(const char*d,const char*f){char p[4096];snprintf(p,sizeof(p),"%s/%s",d,f);FILE*r=fopen(p,"w");assert(r);return r;}
int main(int argc,char**argv){
  assert(argc==7);const char*d=argv[1];unsigned maxlog=atoi(argv[2]),ln=atoi(argv[3]),lb=atoi(argv[4]),tasks=atoi(argv[5]),mode=atoi(argv[6]);
  assert(ln>=4&&maxlog<=18&&ln+lb<=maxlog&&mode<=2&&tasks>=4);
  size_t n=(size_t)1<<ln,m=(size_t)1<<(ln+lb),max=(size_t)1<<maxlog;
  assert(power(root(maxlog),max)==1&&power(root(maxlog),max/2)!=1);
  assert(power(root(ln+lb),(size_t)1<<lb)==root(ln));
  FILE* in=openfile(d,"input.hex"),*out=openfile(d,"expected.hex");
  uint64_t*c=calloc(n,8),*a=calloc(m,8),*iv=calloc(n,8);assert(c&&a&&iv);
  const uint64_t gs[4]={1,7,P-1,19};
  size_t direct=0;
  for(unsigned t=0;t<tasks;t++){
    for(size_t j=0;j<n;j++)c[j]=t==0?0:t==1?(j==0):t==2?P-1:t==3?(j==1):random64();
    unsigned kind=mode==2?(t&1):mode;uint64_t g=gs[t&3];
    for(size_t j=0;j<n;j++)iv[j]=c[j];
    if(kind==0){fft(iv,ln,0);for(size_t j=0;j<n;j++)a[j]=iv[j];fft(a,ln,1);for(size_t j=0;j<n;j++)assert(a[j]==c[j]);}
    packed(in,iv,n);
    uint64_t z=1;for(size_t j=0;j<m;j++){a[j]=j<n?mul(c[j],z):0;z=mul(z,g);}fft(a,ln+lb,0);
    if(m<=1024){uint64_t x=g,w=root(ln+lb);for(size_t k=0;k<m;k++,x=mul(x,w)){uint64_t v=0;for(size_t j=n;j>0;j--)v=add(mul(v,x),c[j-1]);assert(v==a[k]);direct++;}}
    // 独立闭式常量/单项式预期，典型尺寸也执行。
    if(t<4){uint64_t x=g,w=root(ln+lb);for(size_t k=0;k<m;k++,x=mul(x,w)){
      if(t==0)assert(a[k]==0);if(t==1)assert(a[k]==1);if(t==3)assert(a[k]==x);
    }}
    packed(out,a,m);
  }
  fclose(in);fclose(out);
  for(int inverse=0;inverse<2;inverse++){FILE*f=openfile(d,inverse?"inverse.hex":"forward.hex");uint64_t w=root(maxlog),z=1;if(inverse)w=power(w,P-2);
    for(size_t i=0;i<max/2;i++,z=mul(z,w))fprintf(f,"%016"PRIx64"\n",z);fclose(f);}
  printf("REFERENCE_PASS tasks=%u n=%zu m=%zu mode=%u direct=%zu root=%016"PRIx64"\n",tasks,n,m,mode,direct,root(maxlog));
  free(c);free(a);free(iv);return 0;
}
