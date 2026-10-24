#!/usr/bin/env python3
import pathlib, re, sys

root = pathlib.Path(sys.argv[1])
targets = {
    "a.smali": "G(I)V",
    "c.smali": "C(I)V",
}
engine = "com.samsung.SMT"
patched = []

for filename, method_sig in targets.items():
    matches = list(root.glob("smali*/com/kyobo/ebook/common/b2c/viewer/common/tts/" + filename))
    if len(matches) != 1:
        raise SystemExit(f"Expected one {filename}, found {len(matches)}: {matches}")
    path = matches[0]
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    start = next((i for i, line in enumerate(lines) if line.startswith(".method ") and line.rstrip().endswith(method_sig)), None)
    if start is None:
        raise SystemExit(f"Method {method_sig} not found in {path}")
    end = next((i for i in range(start + 1, len(lines)) if lines[i].strip() == ".end method"), None)
    if end is None:
        raise SystemExit(f"Method end not found in {path}")
    block = lines[start:end+1]
    pattern = re.compile(r"^\s*invoke-direct\s+\{([^}]*)\},\s*Landroid/speech/tts/TextToSpeech;-><init>\(Landroid/content/Context;Landroid/speech/tts/TextToSpeech\$OnInitListener;\)V\s*$")
    hits = [i for i, line in enumerate(block) if pattern.match(line)]
    if len(hits) != 1:
        raise SystemExit(f"Expected one default TextToSpeech constructor call in {path}:{method_sig}, found {len(hits)}")
    directive = next((i for i, line in enumerate(block) if line.strip().startswith(".locals ")), None)
    if directive is None:
        # Apktool normally emits .locals. Support .registers only if all parameters use p-register aliases.
        directive = next((i for i, line in enumerate(block) if line.strip().startswith(".registers ")), None)
        if directive is None:
            raise SystemExit(f"No .locals/.registers directive in {path}:{method_sig}")
        old = int(block[directive].strip().split()[1])
        # Both target methods have one explicit int parameter and are instance methods.
        block[directive] = f"    .locals {old - 2 + 1}"
    else:
        old = int(block[directive].strip().split()[1])
        block[directive] = re.sub(r"\.locals\s+\d+", f".locals {old + 1}", block[directive])
    call_i = hits[0]
    m = pattern.match(block[call_i])
    regs = [r.strip() for r in m.group(1).split(",")]
    # New local is the previous .locals count.
    local_line = next(i for i, line in enumerate(block) if line.strip().startswith(".locals "))
    new_local = int(block[local_line].strip().split()[1]) - 1
    vreg = f"v{new_local}"
    block.insert(call_i, f'    const-string {vreg}, "{engine}"')
    call_i += 1
    block[call_i] = re.sub(
        r"^\s*invoke-direct\s+\{[^}]*\},\s*Landroid/speech/tts/TextToSpeech;-><init>\(Landroid/content/Context;Landroid/speech/tts/TextToSpeech\$OnInitListener;\)V\s*$",
        "    invoke-direct {" + ", ".join(regs + [vreg]) + "}, Landroid/speech/tts/TextToSpeech;-><init>(Landroid/content/Context;Landroid/speech/tts/TextToSpeech$OnInitListener;Ljava/lang/String;)V",
        block[call_i],
    )
    lines[start:end+1] = block
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    patched.append(f"{path.relative_to(root)}::{method_sig}")

if len(patched) != 2:
    raise SystemExit(f"Patch incomplete: {patched}")
print("Patched TextToSpeech engine constructor to com.samsung.SMT in:")
print("\n".join(patched))
