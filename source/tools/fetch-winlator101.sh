#!/usr/bin/env bash
set -euo pipefail

URL="https://github.com/brunodev85/winlator/releases/download/v10.1.0/Winlator_10.1.apk"
SHA256="c46ec3fc96548cecb3716ada8733ebdea4fb25c3c945e0695f2c992c8d3ecf4e"
CACHE_DIR="${D2_CACHE_DIR:-$HOME/.cache/d2android}"
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$REPO_DIR/winlator/app/src/main/assets/d2"
. "$REPO_DIR/tools/blocks.sh"

APK="$CACHE_DIR/Winlator_10.1.apk"
D2DX_URL="https://github.com/bolrog/d2dx/releases/download/0.99.529/D2DX.zip"
D2DX_SHA256="94241eaf97a7eba6800370a65477ed08f91e53da01d18b253b96cf44f506efa9"
D2DX_ZIP="$CACHE_DIR/D2DX-0.99.529.zip"
WINE="$CACHE_DIR/wine-9.2-x86_64.txz"
WINE_SHA256="def29b48f80286026ee101e9df65b7eb885703e478ac87cfb2bb31fc94fb6e23"
PLUGY_URL="http://plugy.free.fr/PlugY_The_Survival_Kit_v14.03.zip"
PLUGY_SHA256="449ca7b84414ea8b48a54e1114e30795cdfbaeff6f41574c02395377c59b6741"
PLUGY_ZIP="$CACHE_DIR/PlugY_The_Survival_Kit_v14.03.zip"
PLUGY_LICENSE_URL="https://raw.githubusercontent.com/ChaosMarc/PlugY/64792db947bafeadb9dd9ead8827eefbb07a48bf/LICENSE"
PLUGY_LICENSE_SHA256="8ceb4b9ee5adedde47b31e975c1d90c73ad27b6b165a1dcd80c7c545eb65b903"
PLUGY_LICENSE="$CACHE_DIR/PlugY-LICENSE"

mkdir -p "$CACHE_DIR" "$OUT_DIR"

if [ -f "$BLOCKS_DIR/winlator-10.1/wine-9.2-x86_64.txz" ]; then
  fetch_block winlator-10.1/wine-9.2-x86_64.txz "$WINE_SHA256" "$WINE" || exit 1
elif [ -s "$WINE" ] && ! echo "$WINE_SHA256  $WINE" | sha256sum -c --status; then
  echo "NOTE $WINE is not the pinned Wine ($WINE_SHA256): repacked here; the runtime version differs from the official build" >&2
fi
if [ ! -s "$WINE" ]; then
  fetch_block "(Winlator_10.1.apk, not in the copy)" "$SHA256" "$APK" "$URL"
  echo "Repacking Wine 9.2 …"
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  unzip -p "$APK" assets/rootfs.txz | tar -xJ -C "$tmp" ./opt/wine
  mv "$tmp/opt/wine" "$tmp/wine-9.2-x86_64"
  (cd "$tmp/wine-9.2-x86_64/bin" && rm -f function_grep.pl widl winebuild winecpp winedump wineg++ winegcc winemaker wmc wrc)
  rm -rf "$tmp/wine-9.2-x86_64/include" "$tmp/wine-9.2-x86_64/share/man"
  tar -C "$tmp" --owner=0 --group=0 --sort=name -cf - wine-9.2-x86_64 | xz -6 -T0 > "$WINE.tmp"
  mv "$WINE.tmp" "$WINE"
fi

fetch_block d2dx/D2DX-0.99.529.zip "$D2DX_SHA256" "$D2DX_ZIP" "$D2DX_URL"
fetch_block plugy/PlugY_The_Survival_Kit_v14.03.zip "$PLUGY_SHA256" "$PLUGY_ZIP" "$PLUGY_URL"
fetch_block plugy/LICENSE "$PLUGY_LICENSE_SHA256" "$PLUGY_LICENSE" "$PLUGY_LICENSE_URL"

rm -f "$OUT_DIR/box64-0.3.6.tzst" "$OUT_DIR/wine-9.2-x86_64.txz"
rm -rf "$OUT_DIR/d2dx"
mkdir -p "$OUT_DIR/d2dx"
unzip -q -o -j "$D2DX_ZIP" glide3x.dll README.md 'licenses/*' -d "$OUT_DIR/d2dx"
rm -rf "$OUT_DIR/plugy"
mkdir -p "$OUT_DIR/plugy"
unzip -q -o "$PLUGY_ZIP" PlugY.dll 'PlugY/*' PlugY_The_Survival_Kit_-_Readme.txt -d "$OUT_DIR/plugy"
cp "$PLUGY_LICENSE" "$OUT_DIR/plugy/LICENSE.txt"
ls -lR "$OUT_DIR"
