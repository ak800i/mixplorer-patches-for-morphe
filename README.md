# MiXplorer Sharing Fix

A Morphe patch bundle that fixes the reproduced multi-file sharing failure by returning null for `_data` to external apps. MiXplorer's own queries, filenames, sizes, MIME types, and content URI access are preserved.

This independent project is not affiliated with MiXplorer or Morphe. No modified MiXplorer APK is distributed.

**Stable support is in source version 0.2.0.** The published 0.1.0 bundle is beta-only. To patch stable MiXplorer now, [build from source](#build-from-source) and import the resulting `patches-0.2.0.mpp` locally. The source manifest remains on 0.1.0 until a new bundle is published.

## Add to Morphe

Open this link on the Android device where Morphe is installed:

**[Add MiXplorer Sharing Fix to Morphe](https://morphe.software/add-source?github=ak800i/mixplorer-patches-for-morphe)**

Alternatively, paste this repository URL into Morphe's add-source field:

```text
https://github.com/ak800i/mixplorer-patches-for-morphe
```

For local import, download the `.mpp` from the [latest release](https://github.com/ak800i/mixplorer-patches-for-morphe/releases/latest). The root [source manifest](patches-bundle.json) points Morphe to the published bundle.

## Supported input

| Edition | Package | Version | ARM64 build |
| --- | --- | --- | --- |
| Stable (primary) | `com.mixplorer` | **6.71.15** | **26090422** |
| Beta | `com.mixplorer.beta` | **6.71.15-BETA** | **26090412** |

Android 11+; tested on Galaxy A71, Android 13 / One UI 5.1.

- Stable: `MiXplorer_v6.71.15_B26090422-arm64.apk` from the [official download folder](https://drive.google.com/drive/folders/1Rj8kOmcZwXkWhjQI48wBd21v7yG73x7D). SHA-256: `bc2627659872cfc9895155d129c03eb8ac2b3c112cbdbe7303feb718f10c83f0`.
- Beta: [official input APK](https://mixplorer.com/beta/MiXplorer_v6.71.15-BETA_B26090412-arm64.apk). SHA-256: `62b0f397ee751e90b5b0f619004dff2c39ba466a5fe5d2f8ae1a23413cf8ff98`.

Silver, other builds (including the universal APK), and other architectures are not supported. Do not force compatibility or continue after a patch failure. Missing, ambiguous, or previously patched bytecode patterns are rejected.

## Apply with Morphe Desktop

Use [Morphe Desktop 1.16.0](https://github.com/MorpheApp/morphe-desktop/releases/tag/v1.16.0) with Java 21 or newer. Its CLI was used for the on-device tests below.

1. Enable **Settings > Advanced > Expert mode**.
2. Select one of the original APKs listed above.
3. For stable, use **Local patch file > Browse** and select the source-built `patches-0.2.0.mpp`. Beta also works with the published source linked above.
4. Enable **Fix Telegram multi-file sharing** and apply it. Its necessary local-signing support is included automatically.
5. Install the APK produced by Morphe. No root or additional receiving-app storage permission is needed.

**Back up before replacing an existing installation.** Export MiXplorer settings using Settings > More settings > Export, and keep anything important outside its app-private storage. Android will not install a re-signed APK over the officially signed version of the same package (`com.mixplorer` or `com.mixplorer.beta`). After verifying the backup, remove only that edition if it is already installed, install the patched APK, and restore settings. Other editions use different package names and can remain installed. Keep your Morphe signing key for subsequent patched updates; returning to the official app also requires a backup and reinstall.

CLI equivalent, from a folder containing the three named input files:

```powershell
java -jar .\morphe-desktop-1.16.0-all.jar patch .\MiXplorer_v6.71.15_B26090422-arm64.apk -p .\patches-0.2.0.mpp --exclusive -e "Fix Telegram multi-file sharing" -o .\MiXplorer-patched.apk
```

The source manifest advertises the released patch bundle. It does not expand compatibility beyond the supported input listed above.

## What changes

- **Provider fix:** A small UID check is inserted into `FileProvider.query()`'s `_data` branch. Different UID: store a null value and follow the original column continuation. Same UID: follow the original path branch. Explicit `_data` projections are covered, and the separate `path` alias is untouched.
- **Required signing support:** Both supported APKs contain the same guarded self-fingerprint helper, which calculates the signing certificate's CRC-32 and checks an allowed list. An unchanged, re-signed beta was verified to exit at startup. The internal dependency adds the actual installed app's fingerprint to the existing list. It does not fabricate a developer signature or change Android's signature verification. Only the free stable and beta editions are targeted.

Only two existing classes are modified. No extension library, new permissions, resource changes, UI changes, or Telegram modifications are injected.

## Verification

- Eight Kotlin/dex tests pass, covering exact stable/beta compatibility, high-numbered registers, original branches, missing/ambiguous matches, and repeat-application rejection.
- Official Morphe applies source bundle 0.2.0 to both exact APKs in default `STRIP_FAST` mode without compatibility overrides and signs the results.
- The patched stable app starts and browses normally. Its native single-file share preserves the filename, size, MIME type, read grant, and exact payload hash.
- [Stable native two-file capture](evidence/patched-two-files.json): default and explicit `_data` are null, both URI grants work, and both ordinary Telegram-style multi-file reads match the original SHA-256 hashes without using the diagnostic metadata adapter or receiver storage permission.
- [Stable same-UID instrumentation results](evidence/internal-query.txt): both fixtures retain MiXplorer's own `_data` and `path` values, and raw-path and URI payloads match. Test source is in [device-tests/InternalQueryTest.java](device-tests/InternalQueryTest.java).
- Beta previously passed the same on-device checks with published bundle 0.1.0; its APK patch application was rechecked with 0.2.0.

Public evidence uses disposable synthetic files. Device-local UID numbers have been removed; no phone serial, account data, private files, or signing keys are published.

**Limit:** This validates the actual patched MiXplorer provider, not a live Telegram UI/network upload. The test phone has no Telegram account. The receiver models the relevant file-reading behavior in Telegram 12.10.3. Broader MiXplorer features and cross-app compatibility have not been exhaustively tested.

## Build from source

Requires JDK 21+ and authenticated read access to Morphe's GitHub Packages registry. Set `JAVA_HOME` to the JDK. Use the official `gpr.user` / `gpr.key` Gradle properties or `GITHUB_ACTOR` / `GITHUB_TOKEN` environment variables. Do not place credentials in this repository. On Windows, [build.ps1](build.ps1) can use an already authenticated GitHub CLI transiently without printing or persisting its token.

```powershell
.\build.ps1
```

Or with credentials already configured:

```shell
./gradlew :patches:test :patches:buildAndroid
```

Build output: `patches/build/libs/patches-0.2.0.mpp`. The build does not publish releases or update the source manifest automatically. The project uses Morphe Patcher 1.13.0, the official patches Gradle plugin 1.3.4, and the checksum-pinned Gradle 9.3.1 wrapper.

The optional instrumentation test is built with [device-tests/build.ps1](device-tests/build.ps1), supplying Android Build Tools, an API 33 android.jar, and a standard debug keystore with alias `androiddebugkey` and password `android`. It defaults to stable `com.mixplorer`; pass `-TargetPackage com.mixplorer.beta` for beta. The patched test app must be signed with that same key. It reads only the fixture URI and expected path supplied to `am instrument`.