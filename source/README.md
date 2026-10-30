# D2Android 1.5.1 – source code

D2Android runs the original Diablo II: Lord of Destruction 1.13d on Android phones (ARM64) with touch controls.
It is an unofficial fan project, **not affiliated with, endorsed or supported by Blizzard Entertainment**. You need
your own copy of the game; no game files are part of this archive.

This archive is the source of the public app (package `at.d2andrpub`) of release 1.5.1: the Android app
(`winlator/`, Java and a small native part), the scripts that put together the runtime (Box64, Wine, Turnip, DXVK),
and the mod (`mod/`, C for MinGW) that runs inside `Game.exe`. It holds only what the build needs. Comments are
removed; every line keeps its number, so the code is line for line the code of the APK.

## Not included

- Game files, signing keys, prebuilt third-party binaries: the scripts below download every third-party part from
  its original location and check it against a pinned SHA-256.
- Tests and development tools.

## Build (Linux x86_64)

Needs: JDK 17, the Android SDK with `platforms;android-35`, `build-tools;34.0.0`, `ndk;24.0.8215888` and
`cmake;3.22.1`, Python 3, curl, unzip, xz, zstd, `gcc-aarch64-linux-gnu`, `libasound2-dev`, `libx11-dev`
and `gcc-mingw-w64-i686` (with `binutils-mingw-w64-i686`).

```bash
export ANDROID_HOME=$HOME/Android/Sdk          # your SDK
bash tools/fetch-winlator101.sh                # Wine 9.2 (from Winlator 10.1), D2DX, PlugY
bash tools/collect-licenses.sh                 # licence texts for the app's "Licenses" page
bash tools/build-runtime.sh --public           # runtime archive for at.d2andrpub (a few minutes)
bash tools/build-mod.sh                        # mod DLL, loader and installer helpers
cd winlator
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleRelease -Pd2VersionName=1.5.1 -Pd2VersionCode=<number> \
    -Pd2ReleaseKeystore=/path/to/your-key.p12 -Pd2ReleaseKeystorePassword=...
```

`d2VersionCode` must be higher than the one of an installed build for Android to accept it as an update.

Without `-Pd2ReleaseKeystore` the result is unsigned (`app/build/outputs/apk/release/app-release-unsigned.apk`); sign
it with `apksigner` and a key of your own. The keystore given to Gradle is PKCS#12 with the key alias `d2android`, e.g.
`keytool -genkeypair -storetype PKCS12 -keystore your-key.p12 -alias d2android -keyalg RSA -keysize 4096 -validity 10000`.
An APK signed with your key cannot update the official app: uninstall that first. That also removes the game and your
saved games inside the app: share your saved games first and load them again afterwards.

`fetch-winlator101.sh` repacks Wine from the Winlator 10.1 APK; the bytes differ from the official build, so the
runtime gets another version (the build says so, it works the same). To use the official runtime instead, take
`assets/d2/runtime.txz` from the public APK of the same release and put it with a `runtime-info.txt` (from the
`--public` build) into `winlator/app/src/release/assets/d2/`.

**The build is not byte-identical to the official APK.** The runtime archive (with the repacked Wine package),
timestamps inside the build and the signature differ. The code is the same: this archive is made from the commit
the release was built from, with nothing changed but the comments.

## Licences

| Path | Licence |
|---|---|
| `winlator/app/src/main/java/com/winlator/d2/` (D2Android's own app code), `mod/`, `tools/` except `tools/alsa-plugin/`, `README.md` | MIT (`LICENSE`), Copyright (c) 2026 The D2Android authors |
| everything else under `winlator/`: the other packages under `com/winlator/`, `app/src/main/cpp/`, resources, assets, manifests and Gradle files of the project taken over from Winlator 11.2 (https://github.com/brunodev85/winlator-app) | GNU LGPL 2.1 (`winlator/LICENSE`); a file D2Android changed says so in its first line ("Modified by D2Android") |
| `tools/alsa-plugin/` (Winlator's `android_alsa/module_pcm_android_aserver.c` with D2Android's changes) | GNU LGPL 2.1 |
| code ported from elsewhere inside D2Android's own files: `D2Xbr.java` (Hyllian's xBR-lv2), `D2StormAudio.java` (StormLib), `D2Mpq.java` (blast.c by Mark Adler) | their own licences (MIT, MIT, zlib), as stated in each file |
| except `winlator/gradlew`, `winlator/gradlew.bat`, `winlator/gradle/wrapper/` | Apache 2.0 (Gradle) |

The MIT licence covers D2Android's own code only; it does not change the licence of the Winlator parts or of any
third-party code. The third-party parts the build downloads (Box64, Wine, Mesa Turnip, DXVK, D2DX, PlugY and the
libraries of the runtime), with their licences and sources, are listed in
`winlator/app/src/main/assets/licenses/index.tsv`, on the app's page "Licenses" and in SOURCE-OFFER.txt of the release.
