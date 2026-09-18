# MiXplorer Sharing Fix

A Morphe patch bundle that fixes the reproduced multi-file sharing failure by returning null for `_data` to external apps. MiXplorer's own queries, filenames, sizes, MIME types, and content URI access are preserved.

This independent project is not affiliated with MiXplorer or Morphe. No modified MiXplorer APK is distributed.

## Add to Morphe

Open this link on the Android device where Morphe is installed:

**[Add MiXplorer Sharing Fix to Morphe](https://morphe.software/add-source?github=ak800i/mixplorer-patches-for-morphe)**

Alternatively, paste this repository URL into Morphe's add-source field:

```text
https://github.com/ak800i/mixplorer-patches-for-morphe
```

For local import, download the `.mpp` from the [latest release](https://github.com/ak800i/mixplorer-patches-for-morphe/releases/latest). The root [source manifest](patches-bundle.json) points Morphe to the published bundle.

## Supported input

- Package: `com.mixplorer.beta` only.
- Version: **6.71.15-BETA**, ARM64 build **26090412**.
- Android 11+; tested on Galaxy A71, Android 13 / One UI 5.1.
- [Official input APK](https://mixplorer.com/beta/MiXplorer_v6.71.15-BETA_B26090412-arm64.apk).
- Original APK SHA-256: `62b0f397ee751e90b5b0f619004dff2c39ba466a5fe5d2f8ae1a23413cf8ff98`.

Stable MiXplorer, Silver, other beta builds, and other architectures are not advertised as supported. Do not force compatibility or continue after a patch failure. Missing, ambiguous, or previously patched bytecode patterns are rejected.

## Apply with Morphe Desktop

Use [Morphe Desktop 1.16.0](https://github.com/MorpheApp/morphe-desktop/releases/tag/v1.16.0) with Java 21 or newer. Its CLI was used for the on-device tests below.

1. Enable **Settings > Advanced > Expert mode**.
2. Select the original APK listed above.
3. Add this repository as a source, or use **Local patch file > Browse** and select the downloaded `.mpp`.
4. Enable **Fix Telegram multi-file sharing** and apply it. Its necessary local-signing support is included automatically.
5. Install the APK produced by Morphe. No root or additional receiving-app storage permission is needed.

**Back up before replacing an existing beta.** Export MiXplorer settings using Settings > More settings > Export, and keep anything important outside its app-private storage. Android will not install a re-signed APK over an official `com.mixplorer.beta` installation. After verifying the backup, remove only that beta if it is already installed, install the patched beta, and restore settings. Separate stable/Silver installations use different package names and need not be removed. Keep your Morphe signing key for subsequent patched updates; returning to the official beta also requires a backup and reinstall.

CLI equivalent, from a folder containing the three named input files:

```powershell
java -jar .\morphe-desktop-1.16.0-all.jar patch .\MiXplorer_v6.71.15-BETA_B26090412-arm64.apk -p .\MiXplorer-Sharing-Fix-0.1.0.mpp --exclusive -e "Fix Telegram multi-file sharing" -o .\MiXplorer-patched.apk
```

The source manifest advertises the released patch bundle. It does not expand compatibility beyond the supported input listed above.

## What changes

- **Provider fix:** A small UID check is inserted into `FileProvider.query()`'s `_data` branch. Different UID: store a null value and follow the original column continuation. Same UID: follow the original path branch. Explicit `_data` projections are covered, and the separate `path` alias is untouched.
- **Required beta signing support:** The original beta also exits after being re-signed without any bytecode changes. Its self-fingerprint helper calculates its signing certificate's CRC-32 and checks an allowed list. The internal dependency adds the actual installed app's fingerprint to that existing list. It does not replace the fingerprint with a fabricated developer signature or change Android's signature verification. The patch only targets the free beta.

Only two existing classes are modified. No extension library, new permissions, resource changes, UI changes, or Telegram modifications are injected.

## Verification

- Seven Kotlin/dex transformation tests pass, including high-numbered registers, original branch preservation, missing/ambiguous matches, and repeat-application rejection.
- Official Morphe loads the `.mpp`, recognizes the exact target, applies the patch in its default `STRIP_FAST` mode, and signs an installable APK.
- The installed patched beta starts and browses normally.
- [Native two-file capture](evidence/patched-two-files.json): default and explicit `_data` are null, both URI grants work, and both ordinary Telegram-style multi-file reads match the original SHA-256 hashes, without using the diagnostic metadata adapter. Every other metadata field matches the original-provider baseline.
- [Same-UID instrumentation result](evidence/internal-query.txt): MiXplorer's own `_data` and `path` values remain unchanged, and raw-path and URI payloads match. Test source is in [device-tests/InternalQueryTest.java](device-tests/InternalQueryTest.java).

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

Build output: `patches/build/libs/patches-0.1.0.mpp`. The build does not publish releases or update the source manifest automatically. The project uses Morphe Patcher 1.13.0, the official patches Gradle plugin 1.3.4, and the checksum-pinned Gradle 9.3.1 wrapper.

The optional instrumentation test is built with [device-tests/build.ps1](device-tests/build.ps1), supplying Android Build Tools, an API 33 android.jar, and a standard debug keystore with alias `androiddebugkey` and password `android`. The patched test app must be signed with that same key. It reads only the fixture URI and expected path supplied to `am instrument`.