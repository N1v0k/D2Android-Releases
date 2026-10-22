cmake_minimum_required(VERSION 3.22.1)

# D2Android: only Winlator's X server and helper library; the Vortek/VirGL/Gladio renderers,
# adrenotools and the MIDI synthesizer are not needed for Diablo II (Turnip + DXVK run inside the rootfs).
add_subdirectory(winlator)
