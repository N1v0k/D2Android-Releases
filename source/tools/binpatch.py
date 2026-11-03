



































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
    "d2dx-glide3x-frametime": {
        "file": "glide3x.dll (D2DX 0.99.529 with d2dx-glide3x)",
        "before": "5c4997371d9ca183bd7f0b17bc2f33eb9193f85e34cd94cc7153127b68bc5725",
        "after": "c4edd7b6d2b64373ff6f2f8b7d04437a41b14e57b582d7542f09ec1407ae6f89",
        "runs": [
            {"check_at": 0x5336, "check": "cc" * 42, "at": 0x5336, "old": "cc" * 22,
             "new": "b828000000f20f2ac8f20f5dc1f20f118708010000c3"},
            {"check_at": 0x99CC,
             "check": "8b57140f57c9f20f5ac0f30f5ac80f28c1f20f5c8700010000f20f118f00010000f20f118708010000",
             "at": 0x99CF,
             "old": "0f57c9f20f5ac0f30f5ac80f28c1f20f5c8700010000f20f118f00010000f20f118708010000",
             "new": "f20f5ac0f30f5ac80f28c1f20f5c8700010000f20f118f00010000e847b9ffff660f1f440000"},
        ],
        "doc": "tools/patches/d2dx-0.99.529-frametime-clamp.patch",
    },
    "d2dx-glide3x-atlas512": {
        "file": "glide3x-atlas512.dll (D2DX 0.99.529 with d2dx-glide3x and d2dx-glide3x-frametime)",
        "before": "c4edd7b6d2b64373ff6f2f8b7d04437a41b14e57b582d7542f09ec1407ae6f89",
        "after": "53eab88060db53a34cbff59ee2a223828c862058b590e5167a48369fa631a1e6",
        "check_at": 0x73E8,
        "check": "83ec4053565789" "4de4be00080000",
        "at": 0x73F3,
        "old": "08",
        "new": "02",
        "doc": "tools/patches/d2dx-0.99.529-atlas-512.patch",
    },
    "turnip-a840v2": {
        "file": "usr/lib/libvulkan_freedreno.so (turnip-26.2.0, Mesa 26.2.0 of Winlator 11.2)",
        "before": "99fd7b22666c7b0fa4e86ef675300e05115ffd05acc1a3c178d7343daa7e4584",
        "after": "e9e1dd73e680a5925d10c85a6f7ed3a0d8a888a9dde846ea3f92cf2ac6e34197",
        "runs": [

            {"check_at": 0x71058, "check": "0304000000000000e0b155000000000050ecea0000000000", "at": 0x71060, "old": "e0", "new": "b8"},

            {"check_at": 0xDA6AC8, "check": "080000004000000040000000100000000400000000400000", "at": 0xDA6AD0, "old": "40", "new": "20"},

            {"check_at": 0xDA6B00, "check": "400000001000000000000000080000000400000000000000", "at": 0xDA6B0C, "old": "08", "new": "04"},

            {"check_at": 0xDA6B08, "check": "000000000800000004000000000000000000000000000000", "at": 0xDA6B10, "old": "04", "new": "02"},

            {"check_at": 0xDA6DB0, "check": "010101010001000101010101010101000000010100010101", "at": 0xDA6DBF, "old": "00", "new": "01"},

            {"check_at": 0xEAEC38, "check": "000000000000000041000744ffff0000e0b1550000000000", "at": 0xEAEC40, "old": "410007", "new": "210a05"},

            {"check_at": 0xEAEC40, "check": "41000744ffff0000e0b1550000000000c86ada0000000000", "at": 0xEAEC48, "old": "e0", "new": "b8"},
        ],
        "doc": "tools/patches/turnip-26.2.0-a840-2slice.patch",
    },
    "d2dx-glide3x-atlas1024": {
        "file": "glide3x-atlas1024.dll (D2DX 0.99.529 with d2dx-glide3x and d2dx-glide3x-frametime)",
        "before": "c4edd7b6d2b64373ff6f2f8b7d04437a41b14e57b582d7542f09ec1407ae6f89",
        "after": "50e9a58706c2f9f87cafb975e8698d1928725f9f774a78fb954a5d683c5aa313",
        "check_at": 0x73E8,
        "check": "83ec4053565789" "4de4be00080000",
        "at": 0x73F3,
        "old": "08",
        "new": "04",
        "doc": "tools/patches/d2dx-0.99.529-atlas-1024.patch",
    },
}




_INGAME_CHECK = b'"Ingame Resolution Mode" : 2,'.hex()
for _name, _file, _before, _after640, _after800 in (
        ("d2dx-glide3x", "glide3x.dll (D2DX 0.99.529 with d2dx-glide3x and d2dx-glide3x-frametime)",
         "c4edd7b6d2b64373ff6f2f8b7d04437a41b14e57b582d7542f09ec1407ae6f89",
         "a03a2484f7b97044c8f7fcb73465f1b02139c41b14812a16c1df2ef0ec935eb5",
         "a816d35a5dc3f98606efd72011f4bd6389422c2cfc641461452b9ec0895af634"),
        ("d2dx-glide3x-atlas512", "glide3x-atlas512.dll (D2DX 0.99.529 with d2dx-glide3x-atlas512)",
         "53eab88060db53a34cbff59ee2a223828c862058b590e5167a48369fa631a1e6",
         "1d38ecdfe97a5d3f534484f554b6af30ac4122fcb17276bb4d8d1bdb7f1e85cb",
         "20f9b939fb992942c857294b47f5456e25b949f32e8488dbaa75733cd416e647"),
        ("d2dx-glide3x-atlas1024", "glide3x-atlas1024.dll (D2DX 0.99.529 with d2dx-glide3x-atlas1024)",
         "50e9a58706c2f9f87cafb975e8698d1928725f9f774a78fb954a5d683c5aa313",
         "a519137a405b80df352b67bce7cf1c823d104581beddf7b5df744df89b4052ba",
         "c58e07f1757105e0a83440d05e0f779d1ef9497be6dcef6ab6c2215d73edc2c4")):
    for _size, _digit, _after in (("640", "30", _after640), ("800", "31", _after800)):
        PATCHES["%s-ingame%s" % (_name, _size)] = {
            "file": _file, "before": _before, "after": _after, "runtime": "D2IngameResolution",
            "check_at": 0x3F78D, "check": _INGAME_CHECK, "at": 0x3F7A8, "old": "32", "new": _digit,
            "doc": "tools/patches/d2dx-0.99.529-ingame-resolution.patch",
        }


class PatchError(Exception):
    pass


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def runs(p):

    return p.get("runs", [p])


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
    out = data
    for r in runs(p):
        check, old, new = bytes.fromhex(r["check"]), bytes.fromhex(r["old"]), bytes.fromhex(r["new"])
        rel = r["at"] - r["check_at"]
        if len(old) != len(new) or rel < 0 or rel + len(old) > len(check) or check[rel:rel + len(old)] != old:
            raise PatchError("%s: the patch table is inconsistent" % name)
        if data[r["check_at"]:r["check_at"] + len(check)] != check:
            raise PatchError("%s: bytes at 0x%X are not the expected ones: not patched" % (name, r["check_at"]))
        out = out[:r["at"]] + new + out[r["at"] + len(new):]
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
            for r in runs(p):
                print("%s: %s, offset 0x%X %s -> %s (%s)" % (name, p["file"], r["at"], r["old"], r["new"], p["doc"]))
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
