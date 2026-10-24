#!/usr/bin/env python3
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
engine = "com.samsung.SMT"
pattern = re.compile(
    r"^\s*invoke-direct\s+\{([^}]*)\},\s*"
    r"Landroid/speech/tts/TextToSpeech;-><init>"
    r"\(Landroid/content/Context;Landroid/speech/tts/TextToSpeech\$OnInitListener;\)V\s*$"
)
patched = []
hits_total = 0

for path in sorted(root.glob("smali*/**/*.smali")):
    lines = path.read_text(encoding="utf-8").splitlines()
    changed = False
    i = 0
    while i < len(lines):
        if not lines[i].startswith(".method "):
            i += 1
            continue
        start = i
        end = next((j for j in range(start + 1, len(lines)) if lines[j].strip() == ".end method"), None)
        if end is None:
            raise SystemExit(f"Method end not found in {path}:{start + 1}")
        block = lines[start:end + 1]
        calls = [j for j, line in enumerate(block) if pattern.match(line)]
        if calls:
            locals_idx = next((j for j, line in enumerate(block) if line.strip().startswith(".locals ")), None)
            if locals_idx is None:
                raise SystemExit(f"TextToSpeech constructor uses .registers or has no register directive; refusing unsafe patch: {path}:{block[0]}")
            old_locals = int(block[locals_idx].strip().split()[1])
            vreg = f"v{old_locals}"
            block[locals_idx] = re.sub(r"\.locals\s+\d+", f".locals {old_locals + 1}", block[locals_idx])
            # Reuse the newly allocated local for every TextToSpeech instance in this method.
            for call_i in reversed(calls):
                match = pattern.match(block[call_i])
                regs = [r.strip() for r in match.group(1).split(",")]
                block.insert(call_i, f'    const-string {vreg}, "{engine}"')
                call_i += 1
                block[call_i] = (
                    "    invoke-direct {" + ", ".join(regs + [vreg]) +
                    "}, Landroid/speech/tts/TextToSpeech;-><init>"
                    "(Landroid/content/Context;Landroid/speech/tts/TextToSpeech$OnInitListener;Ljava/lang/String;)V"
                )
                hits_total += 1
            lines[start:end + 1] = block
            end = start + len(block) - 1
            changed = True
            patched.append(f"{path.relative_to(root)}::{block[0].strip()} ({len(calls)} call(s))")
        i = end + 1
    if changed:
        path.write_text("\n".join(lines) + "\n", encoding="utf-8")

if hits_total < 2:
    raise SystemExit(f"Expected at least two TextToSpeech constructor call sites, found {hits_total}")
print(f"Forced TextToSpeech engine to {engine} at {hits_total} constructor call site(s):")
print("\n".join(patched))
