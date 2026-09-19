# Any-Version Compatibility Design

## Goal

Advertise the scoped-storage sharing fix for any version of the existing MiXplorer stable and beta packages, then publish the change as patch release `v0.2.3`.

## Compatibility Contract

- Keep the patch scoped to `com.mixplorer` and `com.mixplorer.beta`.
- Give each package one `AppTarget` whose version is `null`, matching Morphe's package-scoped any-version convention.
- Retain `minSdk = 30` because the fix addresses Android scoped-storage behavior introduced with Android 11.
- Do not constrain ABI or version code.
- Do not make the patch universal across unrelated Android applications.

## Failure Behavior

The patch continues to resolve MiXplorer's `FileProvider` and signing guard structurally. Existing checks reject missing, ambiguous, already-patched, or changed bytecode before the affected method is mutated. Any-version metadata therefore means Morphe may offer the patch for any release of either package; it does not bypass implementation checks or promise that an unknown future layout will patch successfully.

## Catalogue And Documentation

The generated catalogue will serialize the target version as `null`. The README generator will render that target as `Any version` instead of omitting the supported-version section. Manual documentation will distinguish the broad advertised compatibility from the stable and beta builds that were actually tested on-device.

## Testing

1. Change the compatibility unit test first and confirm it fails against the exact-version metadata.
2. Update the patch metadata and confirm the focused test passes.
3. Add generator coverage for rendering a null target as `Any version` and confirm it fails before changing the generator.
4. Run the full patch tests, catalogue generation, release packaging checks, and repository diagnostics.

## Release

Commit the implementation on `dev` with a conventional patch-release commit. Push `dev`, verify the generated prerelease and asset, merge the automated `dev` to `main` promotion without squash or rebase, then verify the published `v0.2.3` release, asset digest, source manifest, catalogue, and branch synchronization.