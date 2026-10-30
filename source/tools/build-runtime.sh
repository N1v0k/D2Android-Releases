#!/usr/bin/env bash












set -euo pipefail

RUNTIME_VERSION=2

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CACHE_DIR="${D2_CACHE_DIR:-$HOME/.cache/d2android}"
OUT_DIR="$REPO_DIR/winlator/app/src/main/assets/d2"


PACKAGE=at.d2android
if [ "${1:-}" = "--public" ]; then
  PACKAGE=$(sed -n 's/^d2PublicPackage=//p' "$REPO_DIR/winlator/gradle.properties")
  [ -n "$PACKAGE" ] || { echo "no d2PublicPackage in winlator/gradle.properties" >&2; exit 1; }
  OUT_DIR="$REPO_DIR/winlator/app/src/release/assets/d2"
fi
WINLATOR_COMMIT=3981d86efa4f333b2a34a7da8b6521476cd8c8b9
BASE="https://raw.githubusercontent.com/brunodev85/winlator-app/$WINLATOR_COMMIT/app/src/main/assets"
SRC="$CACHE_DIR/winlator-11.2-assets"
WINE="$CACHE_DIR/wine-9.2-x86_64.txz"
. "$REPO_DIR/tools/blocks.sh"




SOURCES="
rootfs.tzst aeea2938cc63b0708cb2da999b5a0e3915229b6ab4eac13de061c87f34ffc1dc
rootfs_patches.tzst 49ab7a5158ef7ccff5ae96dfd539df48483454ed11c0d6788a4f95e0104c864a
box64/box64-0.4.4.tzst e1a34fed50bb38ef7fbf51ad5e5b9a76ec4c8178f9cfb4b9f6def0497f2f446c
graphics_driver/turnip-26.2.0.tzst 1e5bd643d64b450960ea3208cc96599a4de577f178e9ba0843b1c37083ce50d2
dxwrapper/dxvk-2.4.1.tzst 897cc48500241006c15c62f200e9a6e1ea8a674bd285da25df6f68fdcdbfe42e
dxwrapper/dxvk-1.10.3.tzst 18ed7c263e0d52c4bbd0e7345b4f22908c10966f37d7d6d80c639ec45123075a
"

mkdir -p "$SRC" "$OUT_DIR"
echo "$SOURCES" | while read -r path sha; do
  [ -n "$path" ] || continue
  fetch_block "winlator-11.2/$path" "$sha" "$SRC/$path" "$BASE/$path" || exit 1
done

[ -s "$WINE" ] || bash "$REPO_DIR/tools/fetch-winlator101.sh"



if [ "$PACKAGE" != at.d2android ]; then
  wine_pin=$(sed -n 's/^WINE_SHA256="\([0-9a-f]\{64\}\)"$/\1/p' "$REPO_DIR/tools/fetch-winlator101.sh")
  if [ -z "$wine_pin" ] || ! echo "$wine_pin  $WINE" | sha256sum -c --status; then
    if [ "${D2_REQUIRE_PINNED_WINE:-}" = 1 ] || { [ -n "$BLOCKS_DIR" ] && [ -f "$BLOCKS_DIR/winlator-10.1/wine-9.2-x86_64.txz" ]; }; then
      echo "$WINE is not the pinned Wine ${wine_pin:-?}: run tools/fetch-winlator101.sh with D2_BLOCKS_DIR set" >&2
      exit 1
    fi
    echo "NOTE $WINE is not the pinned Wine ${wine_pin:-?}: repacked here, the runtime version differs from the official build" >&2
  fi
fi

ALSA_PLUGIN="$REPO_DIR/tools/alsa-plugin/module_pcm_android_aserver_d2.c"


VORTEK_COMMIT=b1730c5def9b575672e671aee11d79ae7adc63d1
VORTEK_SRC="$CACHE_DIR/vortek-$VORTEK_COMMIT.tar.gz"
fetch_block "vortek/vortek-$VORTEK_COMMIT.tar.gz" eef892505c77fb6f3cc7bc78a151662ffb77bab934e5eeff8ead7270a6734c82 "$VORTEK_SRC" \
  "https://codeload.github.com/brunodev85/vortek/tar.gz/$VORTEK_COMMIT" || exit 1
VULKAN_HEADERS="$CACHE_DIR/Vulkan-Headers-1.3.302.tar.gz"
fetch_block "khronos/Vulkan-Headers-1.3.302.tar.gz" 996c3f4220971e3b3cd6b8933e9e81f0bc82b6d2d6449b6f02c66946d65bf944 "$VULKAN_HEADERS" \
  "https://codeload.github.com/KhronosGroup/Vulkan-Headers/tar.gz/refs/tags/v1.3.302" || exit 1
VORTEK_PATCHES=$(ls "$REPO_DIR"/tools/patches/vortek-client-*.patch 2>/dev/null | sort)


key=$( { echo "$RUNTIME_VERSION"; echo "versioned-by-key"; [ "$PACKAGE" = at.d2android ] || echo "$PACKAGE"; echo "$SOURCES"; sha256sum "$WINE" "$REPO_DIR/tools/build-runtime.py" "$REPO_DIR/tools/binpatch.py" "$ALSA_PLUGIN" "$VORTEK_SRC" "$VULKAN_HEADERS" $VORTEK_PATCHES | cut -d' ' -f1; } | sha256sum | cut -c1-16)
cached="$CACHE_DIR/runtime-$key.txz"
if [ ! -s "$cached" ] || [ ! -s "$cached.info" ]; then


  if ! command -v aarch64-linux-gnu-gcc >/dev/null || [ ! -f /usr/include/alsa/asoundlib.h ] || [ ! -f /usr/include/X11/Xlib.h ]; then
    if [ -n "${CI:-}" ]; then
      sudo apt-get update -q && sudo apt-get install -y -q gcc-aarch64-linux-gnu libasound2-dev libx11-dev
    else
      echo "need gcc-aarch64-linux-gnu, libasound2-dev and libx11-dev (see the local build notes)" >&2; exit 1
    fi
  fi
  python3 -I "$REPO_DIR/tools/build-runtime.py" --winlator-assets "$SRC" --wine "$WINE" \
    --version "$RUNTIME_VERSION-$key" --out "$CACHE_DIR/runtime-build.txz" --alsa-plugin-src "$ALSA_PLUGIN" \
    --vortek-src "$VORTEK_SRC" --vulkan-headers "$VULKAN_HEADERS" $(for f in $VORTEK_PATCHES; do echo --vortek-patch "$f"; done) \
    --package "$PACKAGE" --work "$CACHE_DIR/runtime-work"
  mv "$CACHE_DIR/runtime-info.txt" "$cached.info"
  mv "$CACHE_DIR/runtime-build.txz" "$cached"
fi
cp "$cached" "$OUT_DIR/runtime.txz"
cp "$cached.info" "$OUT_DIR/runtime-info.txt"
ls -l "$OUT_DIR/runtime.txz"
cat "$OUT_DIR/runtime-info.txt"
