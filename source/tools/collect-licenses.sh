#!/bin/bash






set -euo pipefail
NOTICES=""
if [ "${1:-}" = "--notices" ]; then NOTICES="${2:?--notices needs a file}"; fi
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
source "$REPO_DIR/tools/blocks.sh"
ASSETS="$REPO_DIR/winlator/app/src/main/assets"
OUT="$ASSETS/licenses"
SPDX=https://raw.githubusercontent.com/spdx/license-list-data/v3.24.0/text


while IFS='|' read -r dest sha url; do
  [ -z "$dest" ] && continue
  fetch_block "licenses/$dest" "$sha" "$OUT/$dest" "$url"
done <<LIST
box64/LICENSE|18731c3637a3dfc5859be67db7d315d438c531396a4ed15a9c88421b520e7883|https://raw.githubusercontent.com/ptitSeb/box64/v0.4.4/LICENSE
dxvk/LICENSE|a5cb1a6ded7d2d7e92d550ba28edd21be2d1d4044662b399887351023e30ce64|https://raw.githubusercontent.com/doitsujin/dxvk/v2.4.1/LICENSE
mesa/license.rst|0d1a0472ecc81830e75c20d59b0ea02841e3db21255e0ebad97ab682c54d6615|https://gitlab.freedesktop.org/mesa/mesa/-/raw/mesa-26.2.0/docs/license.rst
wine/COPYING.LIB|e237fa56668030e928551ddd60f05df5fe957f75eab874bbd017e085ed722e7c|https://gitlab.winehq.org/wine/wine/-/raw/wine-9.2/COPYING.LIB
wine/LICENSE|51433d87540ee2f30e3cff986e00e1477880002bf993106a13775ed60d4e0af4|https://gitlab.winehq.org/wine/wine/-/raw/wine-9.2/LICENSE
zstd-jni/LICENSE|c6a6a8926f2f1732603e4b75779a5f6ce79238eb79bc36dcc5b7feb509bc6cd9|https://raw.githubusercontent.com/luben/zstd-jni/v1.5.2-3/LICENSE
commons-compress/NOTICE.txt|964f986652d38e0e250cff9ba0e63e933f48dd6fdb3afecade167ab9f7cde9af|https://raw.githubusercontent.com/apache/commons-compress/rel/1.20/NOTICE.txt
xz-java/COPYING|ab6169735486872e2cebd634522ae2487e48af180b116d517cb2d357c23b6bd5|https://raw.githubusercontent.com/tukaani-project/xz-java/v1.7/COPYING
alsa-plugin/LICENSE|20c17d8b8c48a600800dfd14f95d5cb9ff47066a9641ddeab48dc54aec96e331|https://raw.githubusercontent.com/brunodev85/winlator/b6b2259158cf38d06067c34430d840d56b46d220/LICENSE
text/LGPL-2.1-only.txt|5749785c8bdefafcb5d798270ed0a967036fe2ca63dcedade1627565dfef81d2|$SPDX/LGPL-2.1-only.txt
text/GPL-2.0-only.txt|aaf135472f81c5b4a0dca9367e5bb5e9750032b5bebe5442b36e4c0a47430df3|$SPDX/GPL-2.0-only.txt
text/GPL-3.0-only.txt|fb981668c18a279e285fc4d83fba1e836cc84dd4daa73c9697d3cfd2d8aca6e0|$SPDX/GPL-3.0-only.txt
text/AGPL-3.0-only.txt|d8a6cc31abc16b6748c7a21f21611f5a1ec33f67d22ca23d7da1c19b95496bee|$SPDX/AGPL-3.0-only.txt
text/Apache-2.0.txt|074e6e32c86a4c0ef8b3ed25b721ca23aca83df277cd88106ef7177c354615ff|$SPDX/Apache-2.0.txt
text/MIT.txt|b85dcd3e453d05982552c52b5fc9e0bdd6d23c6f8e844b984a88af32570b0cc0|$SPDX/MIT.txt
text/Zlib.txt|bfb1112d49db5b1daecdfef24bd7e2f3ea0bafb33aa67aa0ab51e2bf8407c03d|$SPDX/Zlib.txt
text/Bitstream-Vera.txt|5b3b4d3cef4029bda987a71f4bfb6087e40c5e91a2cb853fe7f6ccb78d9d8e38|$SPDX/Bitstream-Vera.txt
mingw-w64/COPYING.MinGW-w64-runtime.txt|e9b2dc02451ea29092a1f25fa0f3c07207ed421f1807dffb0c4e6dce69dee7bd|https://raw.githubusercontent.com/mingw-w64/mingw-w64/v11.0.1/COPYING.MinGW-w64-runtime/COPYING.MinGW-w64-runtime.txt
text/CC-BY-SA-3.0.txt|3f941b3b89cf7b8370ceb83cc76d2120d471b58735d8ca60238a751a48d7f72f|$SPDX/CC-BY-SA-3.0.txt
text/GCC-exception-3.1.txt|7103d4f7f7e2f8ce10d282a05e0689637f8d6d9ef7b399d808d1da313e69b960|$SPDX/GCC-exception-3.1.txt
rootfs/zlib.txt|845efc77857d485d91fb3e0b884aaa929368c717ae8186b66fe1ed2495753243|https://raw.githubusercontent.com/madler/zlib/v1.3.1/LICENSE
rootfs/libpng.txt|7317e078e2d3b5d7ba5a6159e650945153262b44b76f6700f8e9edb261c5143e|https://raw.githubusercontent.com/pnggroup/libpng/v1.6.44/LICENSE
rootfs/vulkan-loader.txt|43c0a37e6a0fa7ff3c843b3ec5a4fac84b712558ddac103fbd4c1649662a9ece|https://raw.githubusercontent.com/KhronosGroup/Vulkan-Loader/v1.3.301/LICENSE.txt
rootfs/brotli.txt|3d180008e36922a4e8daec11c34c7af264fed5962d07924aea928c38e8663c94|https://raw.githubusercontent.com/google/brotli/v1.1.0/LICENSE
rootfs/glibc.txt|bba29819a3e938fae92674d6a88533e10605e389da8b51cdcba0a94f6221c5bc|https://raw.githubusercontent.com/bminor/glibc/glibc-2.39/LICENSES
rootfs/bzip2.txt|c6dbbf828498be844a89eaa3b84adbab3199e342eb5cb2ed2f0d4ba7ec0f38a3|https://sourceware.org/git/?p=bzip2.git;a=blob_plain;f=LICENSE;hb=bzip2-1.0.8
text/LGPL-3.0-only.txt|996af0513df21f7496288951c41428a03c174e9e4a9d63665c57d670f845ccb1|$SPDX/LGPL-3.0-only.txt
rootfs/xorg.txt|2e7012a140f000735a7172674a2d314398d79622444fba65d108b029b29ab283|https://gitlab.freedesktop.org/xorg/lib/libx11/-/raw/libX11-1.8.10/COPYING
rootfs/xcb.txt|c5ffbfeaa501071ceeb97b7de2c0d703fdaa35de01c0fb6cbac1c28453a3e9fd|https://gitlab.freedesktop.org/xorg/lib/libxcb/-/raw/libxcb-1.16.1/COPYING
rootfs/freetype.txt|08c135755dd589039470f1fdbb400daaabaaa50d0b366d19cebff4d22986baa1|https://gitlab.freedesktop.org/freetype/freetype/-/raw/VER-2-13-3/docs/FTL.TXT
rootfs/fontconfig.txt|51a51aa9823704fd90bccc616cdd17ebabb5b2b3e9cbde886ca02c7002288067|https://gitlab.freedesktop.org/fontconfig/fontconfig/-/raw/2.15.0/COPYING
rootfs/expat.txt|46336ab2fec900803e2f1a4253e325ac01d998efb09bc6906651f7259e636f76|https://raw.githubusercontent.com/libexpat/libexpat/R_2_2_6/expat/COPYING
text/BSD-2-Clause.txt|f32fb3b417a194167cfad068223fc975ba96c5960513a10f66a3c28720aec1df|$SPDX/BSD-2-Clause.txt
xbr/xbr-lv2.glsl|9cf5b08c7378305b558204354e898f6837c481fd5b4371dfbe6b5092cffe5062|https://raw.githubusercontent.com/libretro/glsl-shaders/ce9a41242e27f41e16bc35c28943466cee3e8ff8/xbr/shaders/xbr-lv2.glsl
stormlib/LICENSE|50b128e6750ed9d887e7a5748a6021238bb2f3bf34a5dd2768c410d932069e35|https://raw.githubusercontent.com/ladislav-zezula/StormLib/116135c27e8c0dcf8ea0a0c549a6d737ce10fc73/LICENSE
bncs-py/LICENSE|dc76edc16e51df21634d459c316a9fd155b041ad6e459d6cc21c760f74c588bf|https://raw.githubusercontent.com/Davnit/bncs.py/c0dde7ba752638fafcbc786cbab47716aebe7005/LICENSE
LIST


mkdir -p "$OUT/winlator"
cp "$REPO_DIR/winlator/LICENSE" "$OUT/winlator/LICENSE"





missing=0
while IFS=$'\t' read -r id name version license source files note; do
  case "$id" in ''|'#'*) continue ;; esac
  IFS=',' read -r -a list <<< "$files"
  for f in "${list[@]}"; do
    [ "$f" = "-" ] && continue
    p="$ASSETS/$f"
    if [ -d "$p" ]; then [ -n "$(ls -A "$p")" ] || { echo "empty: $f ($id)" >&2; missing=1; }
    elif [ ! -s "$p" ]; then echo "missing: $f ($id)" >&2; missing=1; fi
  done
done < "$OUT/index.tsv"
if [ -n "$NOTICES" ]; then

  {
    echo "D2Android – third-party notices"
    echo "Generated from assets/licenses/index.tsv by tools/collect-licenses.sh. D2Android is an unofficial fan project,"
    echo "not affiliated with Blizzard Entertainment; it contains no game files."
    echo
    declare -A seen=()
    order=()
    while IFS=$'\t' read -r id name version license source files note; do
      case "$id" in ''|'#'*) continue ;; esac
      echo "== $name =="
      echo "Version: $version"
      echo "License: $license"
      echo "Source:  $source"
      [ -n "$note" ] && echo "Note:    $note"
      [ "$files" != "-" ] && echo "Texts:   $files"
      echo
      IFS=',' read -r -a list <<< "$files"
      for f in "${list[@]}"; do [ "$f" = "-" ] || [ -n "${seen[$f]:-}" ] || { seen[$f]=1; order+=("$f"); }; done
    done < "$OUT/index.tsv"
    for f in "${order[@]}"; do
      p="$ASSETS/$f"
      if [ -d "$p" ]; then for c in "$p"/*; do echo "===== ${f}/$(basename "$c") ====="; cat "$c"; echo; done
      elif [ -f "$p" ]; then echo "===== $f ====="; cat "$p"; echo; fi
    done
  } > "$NOTICES"
  echo "notices: $NOTICES ($(wc -c < "$NOTICES") bytes)"
fi

[ "$missing" = 0 ] && echo "licenses: all files of index.tsv present" || { echo "licenses: files missing (run tools/fetch-winlator101.sh first for D2DX/PlugY)" >&2; exit 1; }
