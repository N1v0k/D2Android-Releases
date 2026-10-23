























import argparse
import hashlib
import io
import json
import lzma
import os
import re
import shutil
import stat
import sys
import subprocess
import tarfile
import time

import zstandard

PACKAGE_OLD = b"com.winlator"
PACKAGE_NEW = b"at.d2android"


ROOTFS_EXCLUDE = [re.compile(p) for p in (
    r"^opt/wine(/|$)",
    r"(^|/)gstreamer-1\.0(/|$)", r"/libgst[^/]*$",
    r"(^|/)pulseaudio(/|$)", r"/libpulse[^/]*$", r"^etc/pulse(/|$)",
    r"^usr/lib/locale/(?!en_US\.utf8(/|$))",
    r"^usr/share/i18n(/|$)", r"^usr/share/locale(/|$)", r"^usr/lib/gconv(/|$)",
    r"/lib(fdk|mpg123|vorbis|ogg|FLAC|opus|sndfile)[^/]*$",
    r"^usr/bin/(locale|localedef)$",
    r"^usr/share/(man|doc|info)(/|$)",


    r"/lib(mpeg2|mpeg2convert|out123|syn123)\.so[^/]*$", r"^usr/lib/mpg123(/|$)",
    r"/lib(gnutls|gnutlsxx|gnutls-openssl|nettle|hogweed)\.so[^/]*$",
    r"/lib(glib|gio|gobject|gmodule|gthread|girepository)-2\.0\.so[^/]*$", r"^usr/lib/gio(/|$)",
    r"/lib(krb5|krb5support|gssapi_krb5|gssrpc|k5crypto|kadm5clnt|kadm5clnt_mit|kadm5srv|kadm5srv_mit|kdb5|krad|com_err|verto)\.so[^/]*$",
    r"^usr/lib/krb5(/|$)",
    r"/lib(GL|glapi|jpeg|ffi|zstd|brotlienc|xcb-render|xcb-sync|atopology)\.so[^/]*$",
    r"/lib(nss_[a-z]+|nsl|anl|BrokenLocale|memusage|pcprofile|thread_db|c_malloc_debug)\.so[^/]*$",
    r"^usr/lib/(audit|getconf)(/|$)",
)]


PATCHES_KEEP = {
    "usr/lib/alsa-lib/libasound_module_pcm_android_aserver.so": "usr/lib/alsa-lib/libasound_module_pcm_android_aserver.so",
    "home/xuser/.wine/drive_c/windows/winhandler.exe": "opt/d2/winhandler.exe",
}
PATCHES_KEEP_PREFIX = ("usr/share/icons/",)


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            h.update(block)
    return h.hexdigest()


def open_tar(path):

    fh = open(path, "rb")
    if path.endswith(".tzst"):
        stream = zstandard.ZstdDecompressor(max_window_size=2 ** 31).stream_reader(fh)
    elif path.endswith(".txz") or path.endswith(".xz"):
        stream = lzma.open(fh)
    else:
        stream = fh
    return tarfile.open(fileobj=stream, mode="r|")


def norm(name):
    name = name[2:] if name.startswith("./") else name
    return name.rstrip("/")


def safe_join(root, rel):
    dest = os.path.normpath(os.path.join(root, rel))
    if dest != root and not dest.startswith(root + os.sep):
        raise ValueError("path escapes the staging dir: " + rel)
    return dest


def extract(path, root, select):


    written = {}
    count = 0
    with open_tar(path) as tar:
        for m in tar:
            rel = select(norm(m.name))
            if not rel:
                continue
            dest = safe_join(root, rel)
            if m.isdir():
                os.makedirs(dest, exist_ok=True)
                continue
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            if os.path.lexists(dest):
                os.remove(dest) if not os.path.isdir(dest) or os.path.islink(dest) else shutil.rmtree(dest)
            if m.issym():
                os.symlink(m.linkname, dest)
            elif m.islnk():
                target = written.get(norm(m.linkname))
                if target is None:
                    raise ValueError("hard link to a file that was not extracted: " + m.name)
                shutil.copyfile(target, dest)
                os.chmod(dest, m.mode & 0o777)
            elif m.isfile():
                with tar.extractfile(m) as src, open(dest, "wb") as out:
                    shutil.copyfileobj(src, out, 1 << 20)
                os.chmod(dest, m.mode & 0o777)
                written[norm(m.name)] = dest
            else:
                continue
            count += 1
    return count


def rewrite_package(root):
    changed = []
    for dirpath, dirnames, filenames in os.walk(root):
        for name in filenames:
            path = os.path.join(dirpath, name)
            if os.path.islink(path):
                continue
            with open(path, "rb") as f:
                data = f.read()
            if PACKAGE_OLD in data:
                with open(path, "wb") as f:
                    f.write(data.replace(PACKAGE_OLD, PACKAGE_NEW))
                changed.append(os.path.relpath(path, root))

    for dirpath, dirnames, filenames in os.walk(root):
        for name in dirnames + filenames:
            path = os.path.join(dirpath, name)
            if os.path.islink(path):
                target = os.readlink(path)
                if PACKAGE_OLD.decode() in target:
                    os.remove(path)
                    os.symlink(target.replace(PACKAGE_OLD.decode(), PACKAGE_NEW.decode()), path)
                    changed.append(os.path.relpath(path, root) + " (symlink)")
    return changed


def pack(root, out_path, level, fmt):

    raw = io.BytesIO()
    mtime = 1760140800
    files = dirs = links = 0
    with tarfile.open(fileobj=raw, mode="w", format=tarfile.GNU_FORMAT) as tar:
        for dirpath, dirnames, filenames in os.walk(root):
            dirnames.sort()
            entries = sorted(dirnames + filenames)
            for name in entries:
                path = os.path.join(dirpath, name)
                rel = "./" + os.path.relpath(path, root)
                st = os.lstat(path)
                info = tarfile.TarInfo(rel)
                info.uid = info.gid = 0
                info.uname = info.gname = "root"
                info.mtime = mtime
                if stat.S_ISLNK(st.st_mode):
                    info.type = tarfile.SYMTYPE
                    info.linkname = os.readlink(path)
                    info.mode = 0o777
                    tar.addfile(info)
                    links += 1
                elif stat.S_ISDIR(st.st_mode):
                    info.type = tarfile.DIRTYPE
                    info.mode = 0o771
                    tar.addfile(info)
                    dirs += 1
                else:
                    info.type = tarfile.REGTYPE
                    info.mode = st.st_mode & 0o777
                    info.size = st.st_size
                    with open(path, "rb") as f:
                        tar.addfile(info, f)
                    files += 1
    data = raw.getvalue()
    if fmt == "xz":

        comp = lzma.compress(data, format=lzma.FORMAT_XZ, check=lzma.CHECK_CRC64, preset=9 | lzma.PRESET_EXTREME)
    else:
        comp = zstandard.ZstdCompressor(level=level, threads=-1).compress(data)
    with open(out_path, "wb") as f:
        f.write(comp)
    return files, dirs, links, len(data), len(comp)





ALSA_D2_CONF = """pcm.!default { type android_aserver_d2 }
ctl.!default { type android_aserver }
"""


def build_alsa_plugin(args, root):

    lib = os.path.join(root, "usr/lib")
    inc = os.path.join(args.work, "alsa-include")
    shutil.rmtree(inc, ignore_errors=True)
    shutil.copytree(os.path.join(args.alsa_include, "alsa"), os.path.join(inc, "alsa"))
    out = os.path.join(lib, "alsa-lib", "libasound_module_pcm_android_aserver_d2.so")
    subprocess.check_call([args.cc, "-std=gnu11", "-O2", "-Wall", "-fPIC", "-DPIC", "-U_FORTIFY_SOURCE",
                           "-fno-stack-protector", "-isystem", inc, "-shared", "-nostdlib", "-o", out,
                           args.alsa_plugin_src, os.path.join(lib, "libasound.so.2"), os.path.join(lib, "libc.so.6"),
                           "-Wl,--no-undefined", "-Wl,-soname,libasound_module_pcm_android_aserver_d2.so"])
    with open(os.path.join(root, "opt/d2/alsa-d2.conf"), "w") as f:
        f.write(ALSA_D2_CONF)
    print("alsa plugin android_aserver_d2: %d bytes" % os.path.getsize(out))


def main():
    global PACKAGE_NEW
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--winlator-assets", required=True, help="assets dir of winlator-app 11.2 (or the same files)")
    ap.add_argument("--wine", required=True, help="wine-9.2-x86_64.txz from tools/fetch-winlator101.sh")
    ap.add_argument("--out", required=True)
    ap.add_argument("--version", default="1", help="runtime version; the app reinstalls when it changes")
    ap.add_argument("--work", default=os.path.expanduser("~/.cache/d2android/runtime-build"))
    ap.add_argument("--level", type=int, default=19, help="zstd level (only with --format zstd)")
    ap.add_argument("--format", choices=("xz", "zstd"), default="xz")
    ap.add_argument("--keep-staging", action="store_true", help="leave the unpacked tree in --work/rootfs")
    ap.add_argument("--package", default=PACKAGE_NEW.decode(),
                    help="package id the paths point to (12 characters like com.winlator)")
    ap.add_argument("--alsa-plugin-src", help="tools/alsa-plugin/module_pcm_android_aserver_d2.c")
    ap.add_argument("--cc", default="aarch64-linux-gnu-gcc", help="C compiler for aarch64 Linux (glibc)")
    ap.add_argument("--alsa-include", default="/usr/include", help="dir containing alsa/asoundlib.h")
    args = ap.parse_args()
    PACKAGE_NEW = args.package.encode()
    if len(PACKAGE_NEW) != len(PACKAGE_OLD):
        sys.exit("--package must have %d characters like com.winlator: %s" % (len(PACKAGE_OLD), args.package))

    a = args.winlator_assets
    inputs = {
        "rootfs": os.path.join(a, "rootfs.tzst"),
        "rootfs_patches": os.path.join(a, "rootfs_patches.tzst"),
        "box64": os.path.join(a, "box64", "box64-0.4.4.tzst"),
        "turnip": os.path.join(a, "graphics_driver", "turnip-26.2.0.tzst"),
        "dxvk": os.path.join(a, "dxwrapper", "dxvk-2.4.1.tzst"),
        "wine": args.wine,
    }
    for key, path in inputs.items():
        if not os.path.isfile(path):
            sys.exit("missing input %s: %s" % (key, path))

    root = os.path.join(args.work, "rootfs")
    shutil.rmtree(root, ignore_errors=True)
    os.makedirs(root)
    t0 = time.time()

    n = extract(inputs["rootfs"], root,
                lambda p: None if not p or any(r.search(p) for r in ROOTFS_EXCLUDE) else p)
    print("rootfs: %d entries" % n)

    def patches(p):
        if p in PATCHES_KEEP:
            return PATCHES_KEEP[p]
        return p if p.startswith(PATCHES_KEEP_PREFIX) else None
    print("rootfs_patches: %d entries" % extract(inputs["rootfs_patches"], root, patches))
    print("box64: %d" % extract(inputs["box64"], root, lambda p: p if p == "usr/local/bin/box64" else None))
    print("turnip: %d" % extract(inputs["turnip"], root, lambda p: p or None))
    dxvk = {"syswow64/d3d11.dll", "syswow64/dxgi.dll"}
    print("dxvk: %d" % extract(inputs["dxvk"], root, lambda p: "opt/d2/" + p if p in dxvk else None))

    subprocess.check_call([sys.executable, "-I", os.path.join(os.path.dirname(os.path.abspath(__file__)), "binpatch.py"),
                           "dxvk-d3d11", os.path.join(root, "opt/d2/syswow64/d3d11.dll")])

    def wine(p):
        parts = p.split("/", 1)
        if len(parts) < 2 or not parts[0].startswith("wine-"):
            return None
        return "opt/wine/" + parts[1]
    print("wine 9.2: %d" % extract(inputs["wine"], root, wine))

    if args.alsa_plugin_src:
        build_alsa_plugin(args, root)

    for d in ("home/xuser", "tmp"):
        os.makedirs(os.path.join(root, d), exist_ok=True)
    meta = os.path.join(root, ".d2android")
    os.makedirs(meta, exist_ok=True)
    with open(os.path.join(meta, "runtime-version"), "w") as f:
        f.write(args.version + "\n")
    with open(os.path.join(meta, "sources.json"), "w") as f:
        json.dump({k: {"file": os.path.basename(v), "sha256": sha256(v)} for k, v in inputs.items()}, f, indent=2)

    changed = rewrite_package(root)
    print("package path rewritten in %d files" % len(changed))
    for dirpath, dirnames, filenames in os.walk(root):
        for name in filenames:
            path = os.path.join(dirpath, name)
            if not os.path.islink(path):
                with open(path, "rb") as f:
                    if PACKAGE_OLD in f.read():
                        sys.exit("still contains com.winlator: " + path)

    files, dirs, links, raw, comp = pack(root, args.out, args.level, args.format)
    with open(os.path.join(os.path.dirname(os.path.abspath(args.out)), "runtime-info.txt"), "w") as f:
        f.write("version=%s\nbytes=%d\nformat=%s\npackage=%s\n" % (args.version, raw, args.format, args.package))
    print("archive: %d files, %d dirs, %d symlinks, %.1f MB raw, %.1f MB %s -> %s (%.0f s)"
          % (files, dirs, links, raw / 1e6, comp / 1e6, args.format, args.out, time.time() - t0))
    if not args.keep_staging:
        shutil.rmtree(root)


if __name__ == "__main__":
    main()
