#!/usr/bin/env python3
import pathlib, sys, zipfile

source, replacement, output = map(pathlib.Path, sys.argv[1:4])
target = "classes4.dex"
with zipfile.ZipFile(source, "r") as zin:
    names = [i.filename for i in zin.infolist()]
    if names.count(target) != 1:
        raise SystemExit(f"Expected exactly one {target} in source APK, found {names.count(target)}")
    replacement_bytes = replacement.read_bytes()
    if not replacement_bytes.startswith(b"dex\n"):
        raise SystemExit("Replacement is not a DEX file")
    with zipfile.ZipFile(output, "w") as zout:
        zout.comment = zin.comment
        for info in zin.infolist():
            data = replacement_bytes if info.filename == target else zin.read(info.filename)
            zout.writestr(info, data)
print(f"Replaced only {target} in {output}; preserved all other APK entries")
