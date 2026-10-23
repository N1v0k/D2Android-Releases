

















import hashlib
import os
import sys

PATCHES = {
    "dxvk-d3d11": {
        "file": "d3d11.dll (syswow64, dxvk-2.4.1)",
        "before": "0b560b0d24b14ac2ee3dbc05a12d480eed341a575d713647305d7a040f33abb9",
        "after": "c09491f3e80f2ec2f24792b1542df17fdb2523c79e549293c839db368e68b0ee",
        "check_at": 0x892D4,
        "check": "80bd00ffffff0074a0f683c40000000274978b46388b9520ffffffc74424040000000089542408890424e8c5d60c00",
        "at": 0x892E4,
        "old": "74",
        "new": "eb",
        "doc": "tools/patches/dxvk-2.4.1-d3d11-skip-buffer-clear.patch",
    },
    "d2dx-glide3x": {
        "file": "glide3x.dll (D2DX 0.99.529)",
        "before": "06cc9e9b49902f1ef3aa28180a9859327df717d9022fb1774eeda6f0ba0546d8",
        "after": "5c4997371d9ca183bd7f0b17bc2f33eb9193f85e34cd94cc7153127b68bc5725",
        "check_at": 0x10FC0,
        "check": "558bec83ec1053568b750c8bd9578b7d108bd6578b4b0ce844320000",
        "at": 0x10FD4,
        "old": "8b4b0c",
        "new": "8b4d08",
        "doc": "tools/patches/d2dx-0.99.529-comexception-hresult.patch",
    },
}


class PatchError(Exception):
    pass


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def apply(name, data):

    p = PATCHES.get(name)
    if p is None:
        raise PatchError("unknown patch %s (known: %s)" % (name, ", ".join(sorted(PATCHES))))
    got = sha256(data)
    if got == p["after"]:
        return data
    if got != p["before"]:
        raise PatchError("%s: %s has sha256 %s, expected %s (or the patched %s): not patched"
                         % (name, p["file"], got, p["before"], p["after"]))
    check, old, new = bytes.fromhex(p["check"]), bytes.fromhex(p["old"]), bytes.fromhex(p["new"])
    rel = p["at"] - p["check_at"]
    if len(old) != len(new) or rel < 0 or rel + len(old) > len(check) or check[rel:rel + len(old)] != old:
        raise PatchError("%s: the patch table is inconsistent" % name)
    if data[p["check_at"]:p["check_at"] + len(check)] != check:
        raise PatchError("%s: bytes at 0x%X are not the expected ones: not patched" % (name, p["check_at"]))
    out = data[:p["at"]] + new + data[p["at"] + len(new):]
    if sha256(out) != p["after"]:
        raise PatchError("%s: result has sha256 %s, expected %s" % (name, sha256(out), p["after"]))
    return out


def patch_file(name, path):
    with open(path, "rb") as f:
        data = f.read()
    out = apply(name, data)
    if out is data:
        return "already patched"
    with open(path + ".tmp", "wb") as f:
        f.write(out)
    os.replace(path + ".tmp", path)
    return "patched"


def main(argv):
    if argv[1:] == ["--list"]:
        for name, p in sorted(PATCHES.items()):
            print("%s: %s, offset 0x%X %s -> %s (%s)" % (name, p["file"], p["at"], p["old"], p["new"], p["doc"]))
        return 0
    if len(argv) != 3:
        print(__doc__, file=sys.stderr)
        return 2
    try:
        print("binpatch %s: %s %s" % (argv[1], argv[2], patch_file(argv[1], argv[2])))
    except (PatchError, OSError) as e:
        print("binpatch: %s" % e, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
