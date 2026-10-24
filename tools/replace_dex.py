#!/usr/bin/env python3
import pathlib, sys, zipfile

if len(sys.argv) < 4 or (len(sys.argv) - 3) % 2 != 0:
    raise SystemExit("Usage: replace_dex.py SOURCE.apk OUTPUT.apk DEX_NAME DEX_FILE [DEX_NAME DEX_FILE ...]")
source = pathlib.Path(sys.argv[1])
output = pathlib.Path(sys.argv[2])
replacements = {}
for i in range(3, len(sys.argv), 2):
    name = sys.argv[i]
    path = pathlib.Path(sys.argv[i + 1])
    if not name.endswith(".dex") or "/" in name or name in replacements:
        raise SystemExit(f"Invalid or duplicate DEX name: {name}")
    data = path.read_bytes()
    if not data.startswith(b"dex\n"):
        raise SystemExit(f"Replacement is not a DEX file: {path}")
    replacements[name] = data

with zipfile.ZipFile(source, "r") as zin:
    names = [i.filename for i in zin.infolist()]
    for target in replacements:
        if names.count(target) != 1:
            raise SystemExit(f"Expected exactly one {target} in source APK, found {names.count(target)}")
    with zipfile.ZipFile(output, "w") as zout:
        zout.comment = zin.comment
        for info in zin.infolist():
            data = replacements.get(info.filename, zin.read(info.filename))
            zout.writestr(info, data)
print("Replaced " + ", ".join(sorted(replacements)) + f" in {output}; preserved all other APK entries")
