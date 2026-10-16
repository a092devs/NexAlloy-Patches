# Personal Application Patch Matrix & Curation Analysis

**Date:** 2026-10-15  
**Workspace:** `C:\Users\arsal\Desktop\code\NexAlloy`  
**Repository:** `C:\Users\arsal\Desktop\code\NexAlloy\NexAlloyNext-Patches`  
**Authoritative Source:** User Launcher Inventory

---

## 1. Executive Summary & Curation Strategy

This document establishes an evidence-based patch matrix for the personal Android applications visible in the user's launcher. The objective is to curate `NexAlloyNext-Patches` into a focused, highly maintainable Morphe patch repository tailored strictly to the user's active applications while removing obsolete, uninstalled target applications.

### Key Classification Scheme
1. **Existing support:** A relevant patch already exists in our custom repository (`NexAlloyNext-Patches`).
2. **Upstream support:** Compatible patches exist in the official or prominent upstream Morphe collections (`MorpheApp/morphe-patches`, `hoo-dles/morphe-patches`, `2eno/2eno-patches`).
3. **Third-party support:** A compatible patch exists in another external repository (`SysAdminDoc/Hushfacebook`, `rushiranpise/morphe-patches`, `crimera/piko`).
4. **Candidate for new development:** No suitable existing patch was found, but client-side technical modification is feasible and justified.
5. **Not a practical target:** Application is security-critical (banking/authenticator), server-authoritative (multiplayer games), heavily anti-cheat/ban-sensitive (Snapchat), natively open-source/ad-free, or technically infeasible for Xposed/DexKit patching.
6. **Removed (Curated):** Previously included in the repository but verified absent from the user's installed applications and safely eliminated.

### Framework Compatibility Note: Morphe vs ReVanced
- **Morphe Framework (`io.github.nexalloy:patcher` / `libxposed`):** Injects dynamic bytecode hooks at process startup using DexKit method/field descriptor analysis and Xposed runtime interceptors. Requires runtime `appContext`, DexKit search fingerprints, and classloader hooking.
- **ReVanced Framework (`revanced-patcher`):** Performs ahead-of-time bytecode manipulation on APK files using Smali / Dexlib2 instructions before APK installation.
- **Interchangeability Rule:** ReVanced patches **cannot** be dropped into Morphe directly without rewriting bytecode transformations into dynamic DexKit hook definitions.

---

## 2. Personal Application Patch Matrix

| Application Name | Package ID (Verified) | In Launcher | Classification | Existing Custom Support | Upstream Morphe Support | Relevant Third-Party Repos & URLs | Useful Patch Functionality | Supported Versions & Evidence | License & Integration Considerations | Recommended Action | Confidence & Remaining Work |
|---|---|:---:|---|---|---|---|---|---|---|---|---|
| **YouTube** | `com.google.android.youtube` | Yes | **Existing support** | Yes (`bundles/nexalloy`, 25 patches) | Yes ([MorpheApp/morphe-patches](https://github.com/MorpheApp/morphe-patches)) | [ReVanced/revanced-patches](https://github.com/ReVanced/revanced-patches) | Ad blocking, SponsorBlock, background playback, sanitize links, swipe controls, custom video speed/quality, layout/action button hiding, DeArrow | `20.22.00` to `21.39.000` (target `21.39.522` experimental); validated via DexKit fingerprints | GPLv3; uses submodule `morphe-patches` for extensions and resource generation | **Retained & Maintained**. Semantic version comparator implemented in `VersionUtil.kt`. | High. Continuous validation of Litho components for YouTube 21.40+. |
| **YouTube Music** | `com.google.android.apps.youtube.music` | Yes | **Existing support** | Yes (`bundles/nexalloy`, 9 patches) | Yes ([MorpheApp/morphe-patches](https://github.com/MorpheApp/morphe-patches)) | [ReVanced/revanced-patches](https://github.com/ReVanced/revanced-patches) | Audio ad blocking, background playback, hide upgrade buttons, exclusive audio playback | Up to `9.33.00`; validated via DexKit fingerprints | GPLv3; uses submodule `morphe-patches` | **Retained & Maintained**. Gated via `isVersionAtLeast`. | High. Verify exclusive audio playback on v9.34+. |
| **Reddit** | `com.reddit.frontpage` | Yes | **Existing support** | Yes (`bundles/nexalloy`, 3 patches) | Yes ([MorpheApp/morphe-patches](https://github.com/MorpheApp/morphe-patches)) | [ReVanced/revanced-patches](https://github.com/ReVanced/revanced-patches) | Feed ad removal, comment ad state bypass, sanitize sharing URLs, start as guest | Up to `2026.18.0`; `SubmittedListing` capped at `2026.16.0` | GPLv3; uses submodule `morphe-patches` | **Retained & Maintained**. `SubmittedListing` bypass correctly handled by `VersionUtil.kt`. | High. Investigate `SubmittedListing` replacement on newer releases. |
| **Instagram** | `com.instagram.android` | Yes | **Existing support** | Yes (`bundles/piko`, 31 patches; `bundles/2eno`, Stealth Follow) | No (community only) | [crimera/piko](https://github.com/crimera/piko), [2eno/2eno-patches](https://github.com/2eno/2eno-patches) | Hide feed/story/explore ads, media downloader, anonymous story/DM viewing, disable analytics, stealth follow | `447.0.0.55.81` (explicitly targeted and tested) | GPLv3; Piko code is self-contained in Kotlin; 2eno provides isolated StealthFollow | **Retained & Maintained**. Orthogonal implementations preserved. | High. Stealth follow logic verified; no patch duplication. |
| **Spotify** | `com.spotify.music` | Yes | **Existing support** | Yes (`bundles/2eno`, 12 patches) | Yes ([2eno/2eno-patches](https://github.com/2eno/2eno-patches)) | [anddea/revanced-patches](https://github.com/anddea/revanced-patches), [hypnomacka/revanced-spotify-patch](https://github.com/hypnomacka/revanced-spotify-patch) | Audio ad muting, hide video/playlist/upsell ads, hide Premium tab, sanitize links, Material You theme | `9.1.88.2204` (tested via `2eno-patches` submodule) | GPLv3; extension code compiled from `2eno-patches` | **Retained & Maintained**. | High. Monitor React Native ad container changes. |
| **Google Photos** | `com.google.android.apps.photos` | Yes | **Existing support** | Yes (`bundles/nexalloy`, 2 patches) | Yes ([ReVanced/revanced-patches](https://github.com/ReVanced/revanced-patches)) | [ReVanced/revanced-patches](https://github.com/ReVanced/revanced-patches) | Unlimited original-quality backup via Pixel XL feature spoofing, granular DCIM folder backup | Version-agnostic (pure feature-flag / capability spoofing) | GPLv3; self-contained in `revanced/googlephotos` | **Retained & Maintained**. | High. Low maintenance overhead. |
| **X (Twitter)** | `com.twitter.android` | Yes | **Existing support** | Yes (`bundles/piko`, 19 patches) | No (community only) | [crimera/piko](https://github.com/crimera/piko) | Ad blocking, hide recommended users/timeline clutter, remove premium upsells, uncensor sensitive media, no URL tracking | `12.19.1-release.0` | GPLv3; self-contained Kotlin source in `bundles/piko` | **Retained & Maintained**. | High. Verify timeline filter on future X builds. |
| **Duolingo** | `com.duolingo` | Yes | **Upstream support** | None currently | Yes ([hoo-dles/morphe-patches](https://github.com/hoo-dles/morphe-patches)) | [anddea/revanced-patches](https://github.com/anddea/revanced-patches) | Unlock Super/MAX client indicators, enable debug menu, unlimited RampUp time, disable dynamic launcher icon | Multi-version (`v5.x` through `v6.85+`) | GPLv3; clean fingerprint hook pattern | **Candidate for Integration**. (User holds Duolingo Premium subscription; eligible for optional feature porting, retained in matrix). | High. Client-side feature flags are well-documented. |
| **Facebook** | `com.facebook.katana` | Yes | **Third-party support** | None currently | Partial | [SysAdminDoc/Hushfacebook](https://github.com/SysAdminDoc/Hushfacebook) | Block sponsored feed posts, hide reels/stories carousel, remove suggested groups | Targeted to Facebook `v570` - `v575` | Open-source community patches | **Candidate for Future Porting**. High churn rate in Meta bytecode. | Medium. High obfuscation complexity. |
| **Amazon Shopping** | `in.amazon.mShop.android.shopping` / `com.amazon.mShop.android.shopping` | Yes | **Third-party support** | None currently | No | [rushiranpise/morphe-patches](https://github.com/rushiranpise/morphe-patches) | Force AMOLED dark mode, disable search telemetry/keystroke tracking, disable WebView video autoplay | General release builds | Community script / experimental | **Investigate for New Development**. | Low-Medium. Requires India-specific package ID verification. |
| **LinkedIn** | `com.linkedin.android` | Yes | **Candidate for new development** | None | No | Community tracking parameter strippers | Hide sponsored feed posts, sanitize `trk` share URLs | General mobile releases | Proprietary | **Low-Priority Investigation**. Simple URL sanitization feasible. | Medium. |
| **CloudStream** | `com.lagradost.cloudstream3` | Yes | **Not a practical target** | None | No | [CloudStream Official](https://github.com/recloudstream/cloudstream) | None needed (app is open-source, GPLv3, ad-free by default) | Latest GitHub releases | N/A | **Do Not Patch**. Native extensions manage providers. | High. |
| **MX Player Pro** | `com.mxtech.videoplayer.pro` | Yes | **Not a practical target** | None | No | Custom Neon Codec project | Custom AC3/DTS audio codec support | All versions | Proprietary; codecs handled via external ZIP | **Do Not Patch**. Codecs handled natively via storage. | High. |
| **Snapchat** | `com.snapchat.android` | Yes | **Not a practical target** | None | No | [rhunk/SnapEnhance](https://github.com/rhunk/SnapEnhance) | Message auto-save, stealth viewing, anti-expiration | Continuously targeted | GPLv3; requires dedicated native hooks & anti-ban | **Do Not Patch in Morphe**. Snapchat triggers immediate account bans on generic Xposed hooks. | High. Use standalone SnapEnhance if desired. |
| **Discord** | `com.discord` | Yes | **Not a practical target** | None | No | Vendetta, Aliucord, Revenge | Custom themes, plugins, token management | Discord mobile React Native client | Community client mods | **Do Not Patch**. Discord uses React Native bundles; patchers use JS injection. | High. |
| **WhatsApp** | `com.whatsapp` | Yes | **Not a practical target** | None | No | WA Enhancer (LSPosed) | Privacy features, view deleted messages | Sensitive protocol | Strict server-side bans on modded clients | **Do Not Patch**. Security and ban risk too high. | High. |
| **Messages (Google)** | `com.google.android.apps.messaging` | Yes | **Not a practical target** | None | No | None | System messaging / RCS | Android OS builds | System privileged | **Do Not Patch**. Carrier/RCS attestation. | High. |
| **Chrome** | `com.android.chrome` | Yes | **Not a practical target** | None | No | Cromite, Bromite | Ad blocking, privacy | Chromium engine | Native C++ rendering; Java bytecode hooks ineffective | **Do Not Patch**. Use ad-blocking browser or DNS. | High. |
| **Gmail** | `com.google.android.gm` | Yes | **Not a practical target** | None | No | [ReVanced/revanced-patches](https://github.com/ReVanced/revanced-patches) | Hide promotional ads | Version dependent | Low utility; personal tabs have no ads | **Do Not Patch**. High risk of Google sync interference. | High. |
| **Google Search** | `com.google.android.googlequicksearchbox` | Yes | **Not a practical target** | None | No | None | System search/assistant integration | OS builds | System privileged | **Do Not Patch**. | High. |
| **Google Drive** | `com.google.android.apps.docs` | Yes | **Not a practical target** | None | No | None | Cloud document storage | Version independent | Proprietary cloud storage | **Do Not Patch**. | High. |
| **Gemini / ChatGPT / Claude** | `com.google.android.apps.bard`, `com.openai.chatgpt`, `com.anthropic.claude` | Yes | **Not a practical target** | None | No | None | AI assistant client wrappers | Multi-version | Cloud API authenticated | **Do Not Patch**. All logic and tokens are server-side. | High. |
| **Bitwarden** | `com.x8bit.bitwarden` | Yes | **Not a practical target** | None | No | None | Password vault | Open-source | Security-critical vault | **Do Not Patch**. Never hook password managers. | High. |
| **Microsoft Authenticator** | `com.azure.authenticator` | Yes | **Not a practical target** | None | No | None | 2FA TOTP / Push authentication | Microsoft enterprise | Security-critical token storage | **Do Not Patch**. Hooking violates authenticator integrity. | High. |
| **Amazon A to Z** | `com.amazon.atoz` | Yes | **Not a practical target** | None | No | None | Internal employee scheduling | Enterprise internal | Authentication & corporate compliance | **Do Not Patch**. Risk of workplace account flag. | High. |
| **Flipkart** | `com.flipkart.android` | Yes | **Not a practical target** | None | No | None | E-commerce shopping | Fast release cadence | Dynamic React/GraphQL UI | **Do Not Patch**. No compelling client patch. | High. |
| **Naukri / Naukri Gulf** | `com.mrigapps.naukri`, `com.naukrigulf` | Yes | **Not a practical target** | None | No | None | Job search portals | General releases | Server-side job listings | **Do Not Patch**. | High. |
| **Udemy** | `com.udemy.android` | Yes | **Not a practical target** | None | No | None | Video course player | General releases | DRM-protected video streaming | **Do Not Patch**. DRM is hardware/Widevine backed. | High. |
| **Swiggy** | `in.swiggy.android` | Yes | **Not a practical target** | None | No | None | Food ordering / delivery | General releases | Financial transactions & GPS tracking | **Do Not Patch**. Payment gateway attestation. | High. |
| **PhonePe** | `com.phonepe.app` | Yes | **Not a practical target** | None | No | None | UPI banking / payments | General releases | NPCI UPI security, SafetyNet/Play Integrity | **Strictly Prohibited**. Banking security violation. | Absolute. |
| **Google Pay** | `com.google.android.apps.nbu.paisa.user` | Yes | **Not a practical target** | None | No | None | UPI banking / payments | General releases | Hardware KeyStore, SafetyNet/Play Integrity | **Strictly Prohibited**. Banking security violation. | Absolute. |
| **MyJio** | `com.jio.myjio` | Yes | **Not a practical target** | None | No | None | Telecom self-service | General releases | Server-side balance/SIM details | **Do Not Patch**. | High. |
| **Proton VPN** | `ch.protonvpn.android` | Yes | **Not a practical target** | None | No | None | Encrypted VPN tunnel | Open-source | Server-side WireGuard key authentication | **Do Not Patch**. Server-side subscription check. | High. |
| **1DM+** | `idm.internet.download.manager.plus` | Yes | **Not a practical target** | None | No | None | Download manager | Paid app | Standalone utility | **Do Not Patch**. Paid license already active. | High. |
| **Clash Royale / BGMI / 8 Ball Pool** | `com.supercell.clashroyale`, `com.pubg.imobile`, `com.miniclip.eightballpool` | Yes | **Not a practical target** | None | No | None | Multiplayer games | Live game clients | Strict anti-cheat (Tencent, BattlEye, Supercell) | **Do Not Patch**. Triggers instant permanent bans. | High. |
| **Where Is My Train / RailOne** | `com.whereismytrain.android`, `com.cris.railone` | Yes | **Not a practical target** | None | No | None | Train timetables & IRCTC ticketing | Government/utility | Real-time GPS & server APIs | **Do Not Patch**. | High. |
| **MiX / Termux** | `com.mixplorer`, `com.termux` | Yes | **Not a practical target** | None | No | None | File management, terminal emulator | Standalone tools | Native tools, open-source/ad-free | **Do Not Patch**. | High. |
| **NexAlloyNext** | `io.github.nexalloy` | Yes | **Main Project** | Yes (Host Module) | N/A | Current repository | LSPosed module & bundle manager | Active | GPLv3 | **Host Application**. | High. |

---

## 3. Inventory of Obsolete Targets Removed During Curation

The following applications were verified to be **completely absent** from the user's launcher and have been safely removed from `NexAlloyNext-Patches`:

| App Name | Package ID | Previous Bundle | Deleted Source Path | Removal Rationale | Dependency Impact |
|---|---|---|---|---|---|
| **Photomath** | `com.microblink.photomath` | `:bundles:nexalloy` | `bundles/nexalloy/src/main/java/io/github/nexalloy/revanced/photomath` | Not installed; unneeded math solver unlock. | Zero impact; self-contained. |
| **Threads** | `com.instagram.barcelona` | `:bundles:nexalloy` | `bundles/nexalloy/src/main/java/io/github/nexalloy/revanced/meta` | Not installed; unneeded Meta ad patch. | Zero impact; does not affect Instagram. |
| **Strava** | `com.strava` | `:bundles:nexalloy` | `bundles/nexalloy/src/main/java/io/github/nexalloy/revanced/strava` | Not installed; unneeded athletic subscription patch. | Zero impact; self-contained. |
| **AllTrails** | `com.alltrails.alltrails` | `:bundles:nexalloy` | `bundles/nexalloy/src/main/java/io/github/nexalloy/hoodles` | Not installed; unneeded trail navigation patch. | Zero impact; self-contained. |
| **Kleinanzeigen** | `com.ebay.kleinanzeigen` | `:bundles:2eno` | `bundles/2eno/src/main/java/io/github/nexalloy/2eno/kleinanzeigen` | Not installed; regional German classifieds app. | Deregistered in `Patches2eno.kt` and `FingerprintTestTargets.kt`. |
| **Untappd** | `com.untappdllc.app` | `:bundles:2eno` | `bundles/2eno/src/main/java/io/github/nexalloy/2eno/untappd` | Not installed; unneeded beer social network patch. | Deregistered in `Patches2eno.kt`. Generic `googleads` utility preserved. |
| **InterPals** | `net.interpals` | `:bundles:2eno` | `bundles/2eno/src/main/java/io/github/nexalloy/2eno/interpals` & test | Not installed; unneeded penpal service patch. | Deregistered in `Patches2eno.kt`. |

---

## 4. Curated Repository Structure

### Retained Active Applications
1. **YouTube** (`bundles/nexalloy`): 25 patches (Background playback, ad blocking, SponsorBlock, layout hiding, speed/quality, DeArrow, swipe controls, sanitize sharing).
2. **YouTube Music** (`bundles/nexalloy`): 9 patches (Audio ad blocking, background playback, exclusive audio playback, hide upgrade buttons).
3. **Reddit** (`bundles/nexalloy`): 3 patches (Feed ad hiding, comment ad state bypass, sanitize sharing links).
4. **Google Photos** (`bundles/nexalloy`): 2 patches (Unlimited original-quality backup via Pixel feature spoofing).
5. **Instagram** (`bundles/piko` & `bundles/2eno`):
   - `bundles/piko`: 31 patches (Ad blocking, story/post media downloader, anonymous viewing, analytics blocking).
   - `bundles/2eno`: Stealth Follow patch.
6. **Spotify** (`bundles/2eno`): 12 patches (Audio ad muting, hide video/playlist/upsell ads, hide Premium tab, Material You theme, sanitize sharing).
7. **X (Twitter)** (`bundles/piko`): 19 patches (Ad blocking, hide recommended users/timeline clutter, remove premium upsells, uncensor sensitive media).

### Preserved Shared Components
- `HideReactNativeAdsPatch.kt` (`bundles/2eno/src/main/java/io/github/nexalloy/2eno/googleads/`): General-purpose React Native Google Mobile Ads blocker.
- `VersionUtil.kt` (`bundles/nexalloy/src/main/java/io/github/nexalloy/patches/`): Semantic version comparator supporting trailing-zero normalization, SemVer prerelease precedence, and fail-safe malformed input rejection.
- Patcher bridge API (`io.github.nexalloy:patcher`): DexKit integration, ClassLoader chaining, and fingerprint DSL.
- Test infrastructure (`bundles/testing/`): `FingerprintsKtTest`, `ApkContext`, `FilePathArgumentsProvider`.
