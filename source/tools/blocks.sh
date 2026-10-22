
BLOCKS_DIR="${D2_BLOCKS_DIR:-/nonexistent}"

fetch_block() {
  local rel="$1" sha="$2" dest="$3" url="${4:-}"
  if [ -f "$dest" ] && echo "$sha  $dest" | sha256sum -c --status; then return 0; fi
  mkdir -p "$(dirname "$dest")"
  if [ -f "$BLOCKS_DIR/$rel" ]; then
    cp "$BLOCKS_DIR/$rel" "$dest.tmp"
  elif [ -n "$url" ]; then
    echo "Downloading $url …"
    if ! curl -fL --retry 3 -o "$dest.tmp" "$url"; then
      echo "Download failed: $url" >&2
      echo "Check the network, or set D2_BLOCKS_DIR to a folder with the pinned files." >&2
      rm -f "$dest.tmp"
      return 1
    fi
  else
    echo "$rel is missing: set D2_BLOCKS_DIR to a folder with the pinned files." >&2
    return 1
  fi
  if ! echo "$sha  $dest.tmp" | sha256sum -c --status; then
    echo "SHA-256 mismatch for $rel" >&2
    rm -f "$dest.tmp"
    return 1
  fi
  mv "$dest.tmp" "$dest"
}
