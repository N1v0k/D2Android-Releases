#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
CC=${CC:-i686-w64-mingw32-gcc}
OUT=winlator/app/src/main/assets/d2/mod
mkdir -p "$OUT"
FLAGS=(-O2 -s -std=gnu11 -Wall -Wextra -Wno-unused-parameter -Wno-cast-function-type -static-libgcc -fno-asynchronous-unwind-tables)
"$CC" "${FLAGS[@]}" -shared -o "$OUT/d2android-mod.dll" mod/d2mod.c -lws2_32
"$CC" "${FLAGS[@]}" -mwindows -o "$OUT/D2Android.exe" mod/loader.c
INSTALL=winlator/app/src/main/assets/d2/install
mkdir -p "$INSTALL"
"$CC" "${FLAGS[@]}" -mwindows -o "$INSTALL/d2install.exe" tools/installer/d2install.c -luser32
RES=$(mktemp -d)
"${WINDRES:-i686-w64-mingw32-windres}" tools/installer/placeholder.rc -O coff -o "$RES/placeholder.res"
"$CC" "${FLAGS[@]}" -mwindows -o "$INSTALL/placeholder-game.exe" tools/installer/placeholder.c "$RES/placeholder.res"
rm -rf "$RES"
ls -l "$OUT" "$INSTALL"
