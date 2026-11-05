# MediaTek Mali-G76: hardware-buffer metadata FD selected as GPU memory

## Confirmed on one device

- KONKR Pocket Advance / ARBOR GT78-VN
- MediaTek MT6785V/CD, Mali-G76 MC4, Android 12 (API 31)
- OpenGL ES driver `v1.r26p0-01eac0.e086051d656b225ed0668b49f6ea2e59`
- Vulkan 1.1.0, approximately 3.8 GB system RAM
- Official D2Android 1.5.1 (2592), English Diablo II + LoD 1.13d

The built-in installer completed installation, patching and file checks. The custom
start page rendered. Starting the game's menus repeatedly failed with:

```text
D2DX Fatal Error
 d2dx::RenderContext::RenderContext line 183
 HRESULT: 0x80004005
 Message: Call failed.
```

DXVK logged repeated `VK_ERROR_MEMORY_MAP_FAILED` errors, eventually followed by
`D3D11CoreCreateDevice: Failed to create D3D11 device`. The device had approximately
2.4 GB free RAM at diagnosis; this failure was not established to be RAM exhaustion.

## Investigation

The app correctly enabled Vortek. Forcing
`VortekRendererComponent.Options.resourceMemoryType = 3` (Android hardware buffer)
on its own did **not** fix the failure.

Instrumenting the Vortek client's `vt_call_vkMapMemory2KHR` showed that receiving
the descriptor succeeded, but mapping it failed. A representative sanitized sample:

```text
map2: one FD received, result=VK_SUCCESS, allocationSize=16777216, offset=0
mmap2: MAP_FAILED, errno=13 (Permission denied)
/proc/<wine-pid>/fd/<received-fd> -> anon_inode:gralloc_extra
fdinfo flags: 00
```

In the app process, the observed hardware-buffer-related descriptors included a
read-only `anon_inode:gralloc_extra` followed by a read/write `/dmabuf:` descriptor.
The received metadata descriptor cannot provide the read/write mapping requested
by DXVK. This explains why reducing the allocation size did not help.

[`AHardwareBuffer_getFd`](../winlator/app/src/main/cpp/winlator/src/gpu_image.c)
currently returns `nativeHandle->data[0]` whenever the handle has any descriptors.
[`ResourceMemory_allocate`](../winlator/app/src/main/cpp/vortekrenderer/src/resource_memory.c)
uses that descriptor when importing an Android hardware buffer and passes a
duplicate to the guest for CPU mapping.

## Successful device-specific experiment

A separate app package was used to preserve the official installation. The
experiment combined:

1. Forced Android-hardware-buffer resource memory (`resourceMemoryType = 3`).
2. Requiring at least two native-handle FDs and returning `data[1]` instead of
   `data[0]` in `AHardwareBuffer_getFd`.

The second change removed the permission-denied mapping errors. Verified results:

- Title screen, main menu and class-selection screen rendered.
- A level-one Barbarian loaded into the Rogue Encampment.
- Movement and the recipes panel worked during gameplay.
- A final repackaged APK also reached gameplay after refreshing its runtime from
  its bundled archive, without the diagnostic Vortek-client logging.

Combat, audio, physical gamepad mappings and sustained performance were not tested.

[The source-equivalent experimental patch](patches/mali-g76-fd2-experiment.patch)
is included to make the tested changes explicit. It was translated from the
smali/native-instruction edits used for the device experiment; it was checked for
application against the published 1.5.1 source, but not built independently through
the complete Android build pipeline.

**Do not apply the fixed FD index universally.** Native-handle layout varies by
allocator/vendor. The experiment rejects single-FD buffers and could select the
wrong descriptor on other devices. A production fix should identify the actual
shared buffer descriptor, handle missing/invalid descriptors, and preserve normal
single-FD layouts. It should be tested against both this MediaTek layout and the
existing supported devices. Access flags and descriptor targets are useful
diagnostics, not a complete portable identification contract.

## Reproduction and next validation

1. On the device above, install official 1.5.1 and set up an owned 1.13d game.
2. Open **Classic start (the game's menus)** and inspect the DXVK failure log.
3. In a locally debuggable diagnostic build, instrument the guest's
   `vt_call_vkMapMemory2KHR` around `recv_fds` and `mmap`; inspect the descriptor
   target and `fdinfo` while the failed process remains alive.
4. Repeat with forced AHardwareBuffer memory only, then with the experimental
   FD-selection change as well. Compare mapping errors and actual game rendering.
5. For a generalized fix, verify native-handle FD count/order and mappings on
   single-FD and multi-FD implementations; do not infer support from app launch alone.

This report includes no game files, saves, CD keys, raw diagnosis archives or
modified APKs. The successful workaround does not establish compatibility for
other Mali-G76 devices or driver versions.
