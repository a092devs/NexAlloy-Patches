# NexAlloy: Upstream Audit, App Compatibility & LSPosed Integration Report

**Date:** 2026-10-15  
**Target Repository:** `https://github.com/a092devs/NexAlloyNext-Patches`  
**Workspace Repositories:**
- **Manager / LSPosed Module:** `NexAlloyNext` (Branch: `main`)
- **Patch Collection:** `NexAlloyNext-Patches` (Branch: `main`)

---

## 1. Executive Summary & Root Cause Analysis

### 1.1 Root Cause of Outdated Version Recommendations (e.g., YouTube `21.16.256` vs installed `21.41.525`)
Two architectural factors in `NexAlloyNext` and `NexAlloyNext-Patches` caused outdated version recommendations and false "Untested" warnings:

1. **Target Generation Source Pinning (`MorpheTargets.java`):**
   - In `NexAlloyNext-Patches`, `GenerateMorpheTargetsTask` generates `MorpheTargets.java` by reading patch source files from the `morphe-patches` submodule.
   - The repository's submodule was pinned to Morphe `v1.46.0` (commit `477fbc0`). In that release:
     - Versions `21.40.161`, `21.39.522`, `21.38.130`, `21.28.208`, and `21.23.492` were all marked `isExperimental = true`.
     - The first non-experimental target was `21.16.256`.
   - Upstream Morphe has since released `v1.47.0` (stable `e546439`) and `v1.48.0-dev.1` through `v1.48.0-dev.8` (`5a794d2`). In `v1.48.0-dev.8` (PR #3658, commit `95eb1fb` / `a81a933`), `21.41.525` was added as an experimental target, and `21.20.405` was established as the primary stable target.
   - When submodules were uninitialized or pinned to older tags, `GenerateMorpheTargetsTask` produced outdated targets or fell back to empty target lists.

2. **Resolution Logic in `NexAlloyNext` (`InstalledBundle.kt` & `AppCard.kt`):**
   - `InstalledBundle.recommendedVersion(packageName)` determines the recommended version as:
     ```kotlin
     targets[packageName]?.let { targets -> (targets.firstOrNull { !it.experimental } ?: targets.firstOrNull())?.version }
     ```
     This correctly prefers stable targets over experimental targets for recommendations.
   - However, in `AppCard.kt`:
     ```kotlin
     val versionCompare = recommendedVersion?.let { compareVersions(installed, it) }
     if (versionCompare != null && versionCompare > 0) append(", $untestedLabel")
     ```
     The UI compared the installed version *strictly* against `recommendedVersion`. If a user installed an officially targeted experimental version such as `21.41.525`, `compareVersions("21.41.525", "21.20.405") > 0` caused `AppCard` to display the "Untested" badge, even though `21.41.525` was present in the bundle's target manifest!
   - **Resolution Applied:**
     - In `NexAlloyNext-Patches`, `bundles/nexalloy/build.gradle.kts` now incorporates `VERIFIED_UPSTREAM_TARGETS` as authoritative fallbacks and adds the missing version flags (`is_21_23_or_greater`, `is_21_41_or_greater`, `is_9_21_or_greater`, `is_9_35_or_greater`, `is_9_41_or_greater`, `is_2026_39_0_or_greater`).
     - In `NexAlloyNext`, `AppCard.kt` now receives the full `targets` list from `HomeScreen.kt`. If the installed version matches any declared target (stable or experimental), it is recognized as a known target and the "Untested" badge is suppressed.

### 1.2 Root Cause of Missing LSPosed Scope Notifications
- In `NexAlloyNext`, commit `b514ab9` had stripped `app/src/main/resources/META-INF/xposed/scope.list` down to only `io.github.nexalloy.next`.
- LSPosed and Vector framework daemons inspect `META-INF/xposed/scope.list` inside the module APK during package installation and updates. If target apps are not listed there, LSPosed will not dispatch system notifications prompting the user to enable the module for those apps.
- **Resolution Applied:** Restored the curated list of 8 supported packages in `scope.list` while maintaining `staticScope=false` in `module.prop` for dynamic user toggling.

---

## 2. Upstream Research & Baseline Analysis

| Upstream Project | Upstream Repository | Latest Releases / Commits | Supported App Targets & Changes |
| :--- | :--- | :--- | :--- |
| **Morphe Patches** | `MorpheApp/morphe-patches` | `v1.47.0` (commit `e546439`)<br>`v1.48.0-dev.8` (commit `5a794d2`) | **YouTube:** `21.41.525` (experimental), `21.20.405` (stable)<br>**YT Music:** `9.41.52` (experimental), `9.20.53` (stable)<br>**Reddit:** `2026.41.0` (experimental), `2026.24.0` (stable) |
| **Morphe Library** | `MorpheApp/morphe-patches-library` | `v1.8.0` / `v1.9.0-dev.9` (commit `62707db`) | Smali compiler updates, hook dispatch refinements, compatibility with API 37 |
| **2eno Patches** | `2eno/2eno-patches` | `main` (commit `5b8fa63`) | **Spotify:** Fingerprint-based version-agnostic patching (`targets = emptyList()`)<br>**Instagram:** Baseline target `447.0.0.55.81` |
| **Piko (Crimera)** | `crimera/piko` | `v3.10.0` (commit `a0ed9a5`) | **Instagram:** `447.0.0.55.81`<br>**X / Twitter:** `12.19.1-release.0` |

**Verification update (2026-10-16, direct GitHub API check of `MorpheApp/morphe-patches`):**
- The `dev` branch declares `version = 1.48.0-dev.8` in `gradle.properties` (release commit `f53dd172f171`, 2026-10-15), confirming the version referenced above.
- Recent YouTube work on `dev` includes experimental support for `21.42.240` (`463a4ba0a33c`), fixes for "some 21.42+ fingerprints" (`7bf6e1fe85a7`), new feed-card layouts without action buttons (`096b0bb3e7a9`, `02a4c3d36745`), and a minimal comments-button layout fix (`30a9dd17c876`).
- The full recursive `dev` tree (3664 entries) contains **no Telegram/Nagram patch targets**.
- The local submodule remains pinned to Morphe `v1.46.0` (`477fbc0f5`, fork branch `nexalloy`); generated targets reflect v1.46.0 until that pin is bumped.

---

## 3. App Compatibility Matrix & Implementation Details

### 3.1 YouTube (`com.google.android.youtube`)
- **Installed / Desired Target:** `21.41.525`
- **Recommended Stable Target:** `21.20.405`
- **Target Declarations:**
  - `21.41.525` (Experimental: `true`)
  - `21.40.161` (Experimental: `true`)
  - `21.39.522` (Experimental: `true`)
  - `21.23.492` (Experimental: `true`)
  - `21.20.405` (Experimental: `false`)
- **Version Flags Updated in `VersionCheckPatch.kt`:**
  - Added `is_21_23_or_greater` (threshold: `"21.23.000"`).
  - Added `is_21_41_or_greater` (threshold: `"21.41.000"`).
- **Patch Feasibility:** Upstream Morphe PR #3658 validated client spoofing, protobuf deciphering, and GmsCore authentication against `21.41.525`.

### 3.2 YouTube Music (`com.google.android.apps.youtube.music`)
- **Target Versions:**
  - `9.41.52` (Experimental: `true`)
  - `9.35.54` (Experimental: `true`)
  - `9.20.53` (Experimental: `false`)
- **Version Flags Updated in `VersionCheckPatch.kt`:**
  - Added `is_9_21_or_greater` (threshold: `"9.21.00"`).
  - Added `is_9_35_or_greater` (threshold: `"9.35.00"`).
  - Added `is_9_41_or_greater` (threshold: `"9.41.00"`).

### 3.3 Reddit (`com.reddit.frontpage`)
- **Target Versions:**
  - `2026.41.0` (Experimental: `true`)
  - `2026.39.0` (Experimental: `true`)
  - `2026.24.0` (Experimental: `false`)
- **Version Flags Updated in `VersionCheckPatch.kt`:**
  - Added `is_2024_03_0_or_greater` through `is_2026_39_0_or_greater`.
  - Replaced legacy lexical string comparison with `VersionUtil.isVersionAtLeast`.

### 3.4 Spotify (`com.spotify.music`)
- **Architecture:** Upstream `2eno-patches` hooks Spotify using dynamic dex bytecode fingerprint matching rather than rigid hardcoded version checks.
- **Target Definition:** Updated in `Patches2eno.kt` to `targets = emptyList()`.
- **Result:** Displayed in NexAlloy UI as *"Works with any version"*. Prevents spurious update or untested notifications when Spotify auto-updates.

### 3.5 Instagram (`com.instagram.android`)
- **Target Version:** `447.0.0.55.81`
- **Patches Maintained:** Ghost mode, story download, hide sponsored feed items, hide reels ads.

### 3.6 Duolingo (`com.duolingo`)
- **User Preference:** User maintains an active Duolingo Premium subscription.
- **Policy:** As requested, Duolingo support has been retained. The patches remain in the codebase and can be selectively enabled or disabled by the user without removing the implementation.

### 3.7 Curation of Obsolete Apps
Confirmed removal of 7 uninstalled/obsolete applications:
1. InterPals (`com.interpals`)
2. Kleinanzeigen (`com.ebay.kleinanzeigen`)
3. Untappd (`com.untappd.android`)
4. AllTrails (`com.alltrails`)
5. Photomath (`com.photomath`)
6. Strava (`com.strava`)
7. Standalone React Native ad placement patches for uninstalled apps

---

## 4. UI Resolution & Version Comparison Engine

### 4.1 Strict Version Utility (`VersionUtil.kt`)
The legacy regex splitting extracted raw digits from non-numeric segments, meaning malformed strings like `21abc` or `21.foo.39` were incorrectly parsed.
The hardened `VersionUtil.kt`:
- Parses numeric segments strictly (`toLongOrNull() ?: return Invalid`).
- Truncates trailing zero variants (`20.22.00` == `20.22.0` == `20.22`).
- Handles pre-release suffixes (`-alpha`, `-beta`, `-release.0`).
- Rejects malformed strings (`21abc`, `21.foo.39`, `v21.39.0`, empty/blank strings).
- **Test Suite:** 91 test assertions covering all edge cases pass with 100% success.

### 4.2 UI Target Matching (`AppCard.kt` & `HomeScreen.kt`)
- In `HomeScreen.kt`:
  ```kotlin
  AppCard(
      ...,
      recommendedVersion = bundles.firstNotNullOfOrNull { it.recommendedVersion(packageName) },
      targets = bundles.flatMap { it.targets[packageName].orEmpty() },
      inScope = packageName !in state.missingScope,
      onClick = { onOpenApp(packageName) },
  )
  ```
- In `AppCard.kt`:
  ```kotlin
  val isKnownTarget = targets.any { compareVersions(installed, it.version) == 0 }
  val isUntested = !isKnownTarget && (versionCompare != null && versionCompare > 0)
  ```
  If YouTube `21.41.525` is installed, it is identified as a known target. The UI displays the version cleanly without false warnings.

---

## 5. LSPosed Scope Integration

### 5.1 Restored `scope.list`
File: `NexAlloyNext/app/src/main/resources/META-INF/xposed/scope.list`
```text
io.github.nexalloy.next
com.google.android.youtube
com.google.android.apps.youtube.music
com.reddit.frontpage
com.google.android.apps.photos
com.instagram.android
com.spotify.music
com.twitter.android
```

### 5.2 Dynamic vs Static Scoping
- In `NexAlloyNext/app/src/main/assets/module.prop`, `staticScope=false` is preserved.
- This allows LSPosed to notify the user upon APK installation while still permitting dynamic toggling inside LSPosed Manager or NexAlloy settings.

---

## 6. GitHub Actions CI/CD Hardening

### 6.1 Workflow Changes (`.github/workflows/android.yml` & `release.yml`)
1. **Recursive Submodules:**
   ```yaml
   - uses: actions/checkout@v6
     with:
       submodules: recursive
   ```
2. **Android SDK & API 37 Setup:**
   ```yaml
   - name: set up Android SDK
     uses: android-actions/setup-android@v4
     with:
       packages: 'platforms;android-37 build-tools;37.0.0 platform-tools'
   ```
3. **Artifact Integrity Verification:**
   ```yaml
   - name: Verify generated bundles
     run: |
       ls -la bundles/*/build/outputs/bundle/*.nxb
       count=$(ls -1 bundles/*/build/outputs/bundle/*.nxb 2>/dev/null | wc -l)
       if [ "$count" -eq 0 ]; then
         echo "Error: No .nxb patch bundles were generated!" >&2
         exit 1
       fi
       for f in bundles/*/build/outputs/bundle/*.nxb; do
         if [ ! -s "$f" ]; then
           echo "Error: Bundle artifact $f is empty!" >&2
           exit 1
         fi
         echo "Verified non-empty bundle: $f ($(stat -c%s "$f") bytes)"
       done
   ```

### 6.2 CI Release Safety
- `android.yml` runs on push to `main` and pull requests: **only** tests, builds, and uploads artifacts. It does **not** publish releases.
- `release.yml` triggers **only** on push of version tags (`v*`). Pushing commits to `main` will never inadvertently publish a release.

---

## 7. Verification & Validation Summary

| Component | Test Method | Result | Notes |
| :--- | :--- | :--- | :--- |
| `VersionUtil.kt` | Standalone JVM Runner (`VersionUtilValidationRunner`) | **PASSED (91/91)** | Validated numeric comparison, zero trailing segments, pre-release tags, and rejection of malformed tokens. |
| Formatting & Whitespace | `git diff --check` across both repos | **PASSED (0 errors)** | Clean formatting, no trailing whitespace, clean diff blocks. |
| Remote Mutations | Git remotes check | **VERIFIED (0 mutations)** | Strict constraint observed: no tags pushed, no commits pushed, no remotes modified. |
| Local Gradle Build | `./gradlew testDebugUnitTest --rerun` (2026-10-16) | **PASSED (13/13)** | Android SDK configured on the Windows host. All bundle test tasks executed: `VersionUtilTest` 5, `InstalledAppAuditTest` 1, `InstagramSharedTest` 4, `TwitterSharedTest` 3 — 0 failures. Fingerprint tests over `binaries/` APKs report no parameters because those reference APKs are untracked. |

---

## 9. Follow-up: Morphe Community Catalog Cross-Reference & `a092devs` Bundle (2026-10-16)

### 9.1 Catalog extraction
- **Source:** `https://morphe-patches.software/data/bundles.json`. Per-package patch lists resolved via
  `compatiblePackagesKey ?? compatibilityKey` → top-level `compatibilities[]`, giving exact patch names and
  tested target versions for every bundle in the catalog.
- **Result:** 20 packages match the installed-app audit (Discord, LinkedIn, MiXplorer, Reddit, Chrome,
  Facebook, Twitch, Google Maps, MX Player Pro, Instagram, Bitwarden, X, Google Calendar, Amazon Shopping,
  Flipkart, Myntra, Duolingo, Proton VPN, Google, Nagram XF) from ~30 distinct publishers. Full table in
  `docs/installed-app-patch-audit.md`, Section 4.

### 9.2 Bundle identity verification (before writing any code)
- NexAlloy's bundle loader keys identity off the bundle APK's `applicationId` plus its `meta-data`
  (`nexalloy.bundle.entry/apiVersion/author/description`); the `.nxb` filename is only an artifact name.
  A literal file named `a092devs.nxb` is therefore **not** produced — the artifact is
  `<module>-<versionName>.nxb`.
- Chosen identity: module `bundles/a092devs`, applicationId `io.github.nexalloy.patches.a092devs`,
  author `a092devs`, entry `io.github.nexalloy.a092devs.A092DevsPatches`, `resourcePackageId = 0x4e`
  (0x4b/0x4c/0x4d were taken by nexalloy/2eno/piko). Verified post-build by dumping the produced APK's
  manifest (`aapt2 dump xmltree`): package, entry, apiVersion, author, description and label all correct.

### 9.3 Ports (native `.nxb` hooks only)
- **Discord — 4 patches (VenusIsJaded/Venus-Patches, GPL-3.0):** crash reporting, telemetry and touch
  logging, install attribution, advertising identifiers. 135 native entry points transcribed 1:1 from
  upstream (42/64/27/2 per table); two obfuscated, version-specific upstream targets (`Lio/sentry/android/core/c1;`,
  `Lpe/a;`) are skipped and documented. Upstream rewrites methods at build time; the port installs runtime
  hooks that replace each method with the same result (promises resolved, React callbacks invoked, ad-ID
  object stubbed `("", true)`, etc.).
- **LinkedIn — 3 patches (heyymichii/michii-patches, GPL-3.0):** share-link sanitizer (framework hooks on
  `ClipboardManager.setPrimaryClip` + `Intent.putExtra(EXTRA_TEXT)` with before-hook argument mutation),
  direct `safety/go` link unwrap (`NavigateToUrlActionViewData` constructors + `RichTextUtils.getLinkShimmingLink`),
  metric-queue tracking block (`BlockTracking`, `use = false` to match upstream's off-by-default setting).
  The background `lnkd.in` resolver is not ported (network state machine; upstream gates it separately).
- **MiXplorer — 1 patch (ak800i/mixplorer-patches-for-morphe, GPL-3.0):** `FileProvider.query` result is
  wrapped in a `CursorWrapper` that serves `null` for `_data` when the caller is external
  (`Binder.getCallingUid() != Process.myUid()`) and the projection includes `_data`, on Android 11+ —
  matching upstream's rewritten-branch semantics without touching the APK.

### 9.4 Verification (real, not simulated)
- `./gradlew :bundles:a092devs:testDebugUnitTest :bundles:a092devs:assembleBundle` — **BUILD SUCCESSFUL**;
  `a092devs-1.0.0.nxb` produced (27,760 bytes at build time).
- Unit tests executed (not cached): `PrivacyTargetsTest` 6, `LinkCleanerTest` 11, `ScopedStorageTest` 7 —
  **24/24, 0 failures**.
- Full repo re-run `./gradlew testDebugUnitTest --rerun` — **BUILD SUCCESSFUL**: a092devs 24, nexalloy 7
  (`VersionUtilTest` 5, `InstalledAppAuditTest` 1, fingerprint container 1), piko 8 (`InstagramSharedTest` 4,
  `TwitterSharedTest` 3, container 1), 2eno 1 (fingerprint container) — **40 invocations, 0 failures**.
- **Not verified:** no device is connected (`adb devices` = 0) and no reference APKs are present in
  `bundles/*/binaries/`, so no hook of the new bundle has been exercised against a real app process. All
  `a092devs` target versions are upstream's tested versions, not this repository's device-verified versions.

### 9.5 Docs updated in this pass
`docs/installed-app-patch-audit.md` (header, catalog sources, 20 matrix rows, Section 3 patches/candidates/
exclusions, new Section 4 cross-reference), `README.md` (bundle 4 + credits), `bundles/README.md`
(attribution example), this report.

---

## 8. Safe Deployment Instructions for the User

When you are ready to push these changes to your personal GitHub repository (`https://github.com/a092devs/NexAlloyNext-Patches`):

1. **In `NexAlloyNext-Patches`:**
   ```bash
   cd C:\Users\arsal\Desktop\code\NexAlloy\NexAlloyNext-Patches
   git add -A
   git commit -m "Update upstream targets, version flags, Spotify handling, and hardened CI"
   git push personal main
   ```
   *This will trigger `.github/workflows/android.yml`, which will run tests and build all `.nxb` bundles in GitHub Actions.*

2. **To publish a release bundle:**
   ```bash
   git tag v1.0.0
   git push personal v1.0.0
   ```
   *This will trigger `.github/workflows/release.yml`, which builds the `.nxb` bundles and attaches them to a new GitHub Release.*

3. **In `NexAlloyNext`:**
   ```bash
   cd C:\Users\arsal\Desktop\code\NexAlloy\NexAlloyNext
   git add -A
   git commit -m "Restore LSPosed scope list and update AppCard target handling"
   ```

