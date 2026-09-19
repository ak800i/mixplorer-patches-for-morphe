# Any-Version Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Offer the scoped-storage sharing fix for any version of MiXplorer stable and beta and publish stable patch release `v0.2.3`.

**Architecture:** Keep compatibility package-scoped and represent any app version with one `AppTarget(version = null, minSdk = 30)` per package. Preserve the existing structural bytecode checks as the runtime compatibility boundary, and teach the generated README to render null-version targets explicitly as `Any version`.

**Tech Stack:** Kotlin/JUnit 5, Morphe Patcher compatibility metadata, Python 3 standard-library `unittest`, Gradle 9.7.1/JDK 21, semantic-release, GitHub Actions/CLI.

## Global Constraints

- Target only `com.mixplorer` and `com.mixplorer.beta`.
- Advertise any app version while retaining Android 11+ through `minSdk = 30`.
- Do not constrain ABI or version code.
- Preserve fail-closed bytecode and signing guards.
- Distinguish advertised compatibility from the two APKs already tested on-device.
- Publish through the existing `dev` prerelease and `dev` to `main` stable promotion flow.

---

### Task 1: Package-Scoped Any-Version Metadata

**Files:**
- Modify: `patches/src/test/kotlin/dev/local/mixplorer/SharingFixPatchTest.kt`
- Modify: `patches/src/main/kotlin/dev/local/mixplorer/SharingFixPatch.kt`

**Interfaces:**
- Consumes: Morphe `Compatibility`, `AppTarget`, and `minSdk` metadata.
- Produces: one null-version target with `minSdk = 30` for each supported package.

- [ ] **Step 1: Write the failing compatibility test**

Replace the exact-build test with:

```kotlin
@Test
fun `supports any stable and beta MiXplorer version on Android 11 or later`() {
    val supported = fixScopedStorageFileSharingPatch.compatibility.orEmpty().associateBy { it.packageName }
    assertEquals(setOf("com.mixplorer", "com.mixplorer.beta"), supported.keys)
    supported.values.forEach { compatibility ->
        val target = compatibility.targets.single()
        assertNull(target.version)
        assertNull(target.versionCodes)
        assertEquals(30, target.minSdk)
    }
}
```

Replace the unused `SupportedAbi` import with `kotlin.test.assertNull`.

- [ ] **Step 2: Run the focused test and verify red**

Run:

```powershell
.\build.ps1 -Tasks @(':patches:test', '--tests', 'dev.local.mixplorer.SharingFixPatchTest.supports any stable and beta MiXplorer version on Android 11 or later')
```

Expected: FAIL because the stable target version is `6.71.15`, not null.

- [ ] **Step 3: Implement minimal any-version metadata**

Remove the `SupportedAbi` import and define both package targets as:

```kotlin
targets = listOf(
    AppTarget(
        version = null,
        minSdk = 30,
    ),
),
```

Do not change `patchProvider`, `localSigningPatch`, dependencies, or package names.

- [ ] **Step 4: Run the focused test and verify green**

Run the command from Step 2.

Expected: PASS, with one test executed and no failures.

- [ ] **Step 5: Commit the metadata behavior**

```powershell
git add patches/src/main/kotlin/dev/local/mixplorer/SharingFixPatch.kt patches/src/test/kotlin/dev/local/mixplorer/SharingFixPatchTest.kt
git commit -m 'bump: support any MiXplorer version'
```

---

### Task 2: Render Any-Version Targets

**Files:**
- Create: `.github/scripts/test_generate_patches_readme.py`
- Modify: `.github/scripts/generate_patches_readme.py`
- Modify: `.github/workflows/release.yml`

**Interfaces:**
- Consumes: catalogue targets whose JSON `version` is null.
- Produces: a Markdown supported-version cell containing `Any version`.

- [ ] **Step 1: Add a subprocess regression test**

Create a standard-library `unittest` that writes a temporary catalogue and README, invokes `generate_patches_readme.py`, then asserts the result contains both `**🎯 Supported versions:**` and `| Any version |`:

```python
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


class GeneratePatchesReadmeTest(unittest.TestCase):
    def test_renders_null_target_version_as_any_version(self):
        catalogue = {
            "version": "0.2.3",
            "patches": [{
                "name": "Example patch",
                "description": "Example description.",
                "compatiblePackages": [{
                    "packageName": "com.mixplorer",
                    "name": "MiXplorer",
                    "targets": [{"version": None, "isExperimental": False}],
                }],
                "options": [],
            }],
        }
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            catalogue_path = root / "patches-list.json"
            readme_path = root / "README.md"
            catalogue_path.write_text(json.dumps(catalogue), encoding="utf-8")
            readme_path.write_text(
                "<!-- PATCHES_START -->\nold\n<!-- PATCHES_END -->\n",
                encoding="utf-8",
            )
            subprocess.run(
                [
                    sys.executable,
                    str(Path(__file__).with_name("generate_patches_readme.py")),
                    "owner/repo",
                    "main",
                    str(catalogue_path),
                    str(readme_path),
                ],
                check=True,
            )
            generated = readme_path.read_text(encoding="utf-8")

        self.assertIn("**🎯 Supported versions:**", generated)
        self.assertIn("| Any version |", generated)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the generator test and verify red**

```powershell
python .github/scripts/test_generate_patches_readme.py
```

Expected: FAIL because `versions_table()` currently skips null versions and omits the section.

- [ ] **Step 3: Render the null target explicitly**

In `versions_table()`, replace the null-version skip with:

```python
ver = t["version"]
label = "Any version" if ver is None else ver
if t.get("isExperimental"):
    label = f"🧪&nbsp;{label}"
cells.append(label)
```

- [ ] **Step 4: Run the generator test and verify green**

Run the command from Step 2.

Expected: one passing test.

- [ ] **Step 5: Add the regression test to release CI**

Change the workflow's test command to:

```yaml
run: |
  ./gradlew :patches:test --no-daemon
  python3 .github/scripts/test_generate_patches_readme.py
```

- [ ] **Step 6: Commit generator coverage**

```powershell
git add .github/scripts/generate_patches_readme.py .github/scripts/test_generate_patches_readme.py .github/workflows/release.yml
git commit -m 'fix: render any-version patch targets'
```

---

### Task 3: Compatibility Documentation

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: package-scoped any-version metadata and existing device evidence.
- Produces: accurate public distinction between offered and tested compatibility.

- [ ] **Step 1: Update the manual compatibility section**

Rename `Supported input` to `Compatibility and tested input`. State that both package IDs are offered for any version on Android 11+, while the listed stable ARM64 `6.71.15` and beta ARM64 `6.71.15-BETA` APKs are the device-tested inputs. Replace the claim that other builds and architectures are unsupported with a statement that they are not device-tested and may fail closed when structural guards do not match. Keep Silver and unrelated package IDs unsupported.

- [ ] **Step 2: Align nearby claims**

Update references to “both supported APKs,” “exact compatibility,” and the source manifest so they describe the two device-tested APKs and the new any-version metadata without claiming exhaustive cross-version validation.

- [ ] **Step 3: Validate documentation consistency**

```powershell
Select-String -Path README.md -Pattern 'not supported|exact stable/beta compatibility|does not expand compatibility'
git diff --check
```

Expected: no stale compatibility claims and no whitespace errors.

- [ ] **Step 4: Commit documentation**

```powershell
git add README.md
git commit -m 'docs: explain any-version compatibility limits'
```

---

### Task 4: Full Verification And Release

**Files:**
- Generated during verification/release: `patches-list.json`, `README.md`, `CHANGELOG.md`, `gradle.properties`, `patches-bundle.json`
- Published: `patches/build/libs/patches-0.2.3.mpp`

**Interfaces:**
- Consumes: committed implementation on `dev` and repository release automation.
- Produces: verified GitHub prerelease followed by stable `v0.2.3`.

- [ ] **Step 1: Run all local checks from a clean build**

```powershell
python .github/scripts/test_generate_patches_readme.py
.\build.ps1 -Tasks @('clean', ':patches:test', ':patches:generatePatchesList')
git diff --check
```

Expected: Python test passes, all Kotlin tests pass, catalogue generation succeeds, and the bundle contains `classes.dex`.

- [ ] **Step 2: Inspect generated compatibility and restore release-owned files**

```powershell
$catalogue = Get-Content patches-list.json -Raw | ConvertFrom-Json
$packages = $catalogue.patches[0].compatiblePackages
if (@($packages.packageName | Sort-Object) -join ',' -ne 'com.mixplorer,com.mixplorer.beta') { throw 'Unexpected packages' }
foreach ($package in $packages) {
    if ($package.targets.Count -ne 1) { throw "Unexpected target count for $($package.packageName)" }
    $target = $package.targets[0]
    if ($null -ne $target.version -or $null -ne $target.versionCodes -or $target.minSdk -ne 30) {
        throw "Unexpected target for $($package.packageName)"
    }
}
git restore -- patches-list.json
if (git status --short) { throw 'Working tree is not clean' }
```

Expected: both package targets pass the assertions and the working tree is clean.

- [ ] **Step 3: Push `dev` and verify the prerelease workflow**

```powershell
git push origin dev
$head = git rev-parse HEAD
$run = gh run list --workflow Release --branch dev --limit 1 --json databaseId,status,conclusion,headSha | ConvertFrom-Json | Select-Object -First 1
if ($run.headSha -ne $head) { throw 'Latest release run does not match pushed HEAD' }
gh run watch $run.databaseId --exit-status --compact *> .git/dev-release-run.log
gh run view $run.databaseId --json status,conclusion,jobs
```

Expected: success and release `v0.2.3-dev.1` with exactly one non-source `.mpp` asset.

- [ ] **Step 4: Merge the automated promotion PR**

Identify the single open `dev` to `main` PR, verify its head and base, then merge with a merge commit and without deleting `dev`:

```powershell
$prs = @(gh pr list --base main --head dev --state open --json number,baseRefName,headRefName,headRefOid | ConvertFrom-Json)
if ($prs.Count -ne 1 -or $prs[0].baseRefName -ne 'main' -or $prs[0].headRefName -ne 'dev') {
    throw 'Expected exactly one dev to main promotion PR'
}
if ($prs[0].headRefOid -ne $head) { throw 'Promotion PR head differs from verified prerelease commit' }
gh pr merge $prs[0].number --merge --match-head-commit $head
```

- [ ] **Step 5: Verify stable publication**

Use the GitHub releases API to select the exact non-draft, non-prerelease `v0.2.3` release. Confirm exactly one `.mpp` asset, download it, compare its size and SHA-256 with the API digest, and inspect the archive manifest for `Version: 0.2.3` plus the presence of `classes.dex`.

- [ ] **Step 6: Verify public metadata and synchronize `dev`**

Confirm `patches-bundle.json` points to the public `v0.2.3` asset, `patches-list.json` reports version `0.2.3` with null targets and `minSdk` 30, and anonymous download succeeds. Fetch and fast-forward the local `dev` branch after the release backmerge, then require a clean working tree.