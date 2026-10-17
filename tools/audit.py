#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Coset-LDE Hardware Accelerator
# Chisel hardware for polynomial low-degree extension on a Goldilocks coset.
# Repository: https://github.com/OpenRTL-Lab/coset-lde-goldilocks
# File: tools/audit.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Coset-LDE contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""Check publication file boundaries, documentation pairs, links and sensitive text."""
from pathlib import Path
import re
import json

ROOT=Path(__file__).resolve().parents[1]

def files():
    return sorted(p for p in ROOT.rglob('*') if p.is_file() and not any(x in p.relative_to(ROOT).parts for x in ('build','out','.git','__pycache__')))

PATTERNS={
    'machine path':r'(?:/(?:home|mnt|opt|root|work|private|[a-z]+-data)/|[A-Za-z]:\\Users\\)',
    'credential':r'(?:gh[pousr]_[A-Za-z0-9_]{15,}|github_pat_[A-Za-z0-9_]+|-----BEGIN [A-Z ]*PRIVATE KEY-----)',
    'provenance digest':r'(?i)(?<![a-z0-9])[a-f0-9]{40,64}(?![a-z0-9])',
    'internal network':r'\b(?:10\.\d+\.\d+\.\d+|192\.168\.\d+\.\d+)\b',
}

def audit():
    payload=files(); errors=[]; names={};mds=[]
    allowed_root={'.gitignore','LICENSE','VERSION','build.mill','README.md','README.zh-CN.md','THIRD_PARTY_NOTICES.md','THIRD_PARTY_NOTICES.en.md','VERSION.license'}
    for p in payload:
        rel=p.relative_to(ROOT)
        if len(rel.parts)==1:
            assert p.name in allowed_root,rel
        else:
            assert rel.parts[0] in ('docs','tools','operators','rtl'),rel
        assert p.suffix not in ('.tcl','.sdc','.rpt','.ddc','.db','.lib','.log','.key'),rel
        text=p.read_text()
        for category,pattern in PATTERNS.items():
            if re.search(pattern,text):errors.append((str(rel),category))
        if p.suffix!='.md':continue
        mds.append(p)
        english=p.name=='README.md' or p.name.endswith('.en.md')
        pair=p.with_name('README.zh-CN.md' if p.name=='README.md' else 'README.md' if p.name=='README.zh-CN.md'
                         else p.name.replace('.en.md','.md') if p.name.endswith('.en.md') else p.stem+'.en.md')
        assert pair.exists(),(rel,'missing language pair')
        assert '\\operatorname' not in text
        paragraphs=[q for q in re.sub(r'\A<!--\nOpenRTL-Header-Begin.*?-->\n\n', '', text, count=1, flags=re.S).split('\n\n') if q.strip()]
        assert len(paragraphs)>1 and not paragraphs[1].startswith(('#','|','-','```')),rel
        for label,target in re.findall(r'\[([^\]]+)\]\(([^)]+)\)',text):
            if re.match(r'https?://',target):continue
            resolved=(p.parent/target.split('#')[0]).resolve()
            assert resolved.is_relative_to(ROOT) and resolved.exists(),(rel,target)
            if label==('简体中文' if english else 'English') and resolved==pair.resolve():continue
            if resolved.is_file():
                key=('en' if english else 'zh',str(resolved.relative_to(ROOT)))
                if key in names:assert names[key]==label,(rel,key,label,names[key])
                names[key]=label
            target_english=resolved.name=='README.md' or resolved.name.endswith('.en.md')
            if english and resolved.suffix=='.md' and not target_english:
                assert resolved == pair.resolve(),(rel,'English document links to Chinese content')
    assert not errors,errors
    # Positive algorithm constants remain legal; known leak signatures must be detected.
    for sample in ('/'+'work/example','github_pat_'+'A'*30,'d'*64):
        assert any(re.search(p,sample) for p in PATTERNS.values())
    return {'status':0,'files':len(payload),'markdown_files':len(mds),'negative_controls':True}

if __name__=='__main__':print(json.dumps(audit(),ensure_ascii=False))
