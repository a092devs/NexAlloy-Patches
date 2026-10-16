# Installed Applications Audit & Runtime Feasibility Report

**Date:** 2026-10-15  
**Ecosystem:** NexAlloy Next & NexAlloyNext-Patches  
**Runtime Architecture:** Native `.nxb` bundles, LSPosed API 101, DexKit runtime bytecode hooking  
**Core Directive:** Pure runtime hooks only. Strictly NO APK patching, rebuilding, re-signing, or `.mpp` renaming.

---

## 1. Executive Architecture Summary

NexAlloyNext operates as a modern root + LSPosed manager application loading `.nxb` (NexAlloy Bundle) bytecode extensions into target process memory spaces via Xposed/DexKit. 

Unlike ahead-of-time (AOT) patchers such as ReVanced CLI / Patcher which decompile APKs to Smali, edit dex structures on disk, inject static resource IDs, and sign new APKs, **NexAlloy runtime hooks operate dynamically at process startup**:
1. **Target Attachment:** The LSPosed zygote hook injects `PatcherBridge` into the target package classloader.
2. **DexKit Pattern Matching:** Methods and fields are located dynamically by opcode signatures and string references (fingerprints), resisting ProGuard/R8 obfuscation drift across versions.
3. **Execution Hooking:** Dynamic interceptors (`XC_MethodHook`, `XC_MethodReplacement`) redirect execution flow, strip ads, bypass telemetry, and spoof device capabilities in RAM.
4. **Zero Package Mutation:** The original APK package signature, integrity hashes, and system integrity (Play Protect/Play Integrity) remain completely untouched on disk.

Consequently, any patch requiring modifications to `AndroidManifest.xml` (e.g. adding exported components, system permission requests), static binary replacement (native C++ `.so` libraries), or compile-time XML resources that cannot be dynamically inflated through `XResources` / dynamic context creation cannot be executed as a runtime `.nxb` patch.

---

## 2. Comprehensive Inventory Audit & Feasibility Matrix

The following matrix covers the user's complete installed application inventory, categorizing each by runtime feasibility and porting status.

| Application Name | Package Identifier | Category | Patch Source / Ecosystem | Runtime Feasibility (LSPosed / DexKit) | Porting Status & Implementation | Technical Notes & Constraints |
|---|---|---|---|:---:|---|---|
| **YouTube** | `com.google.android.youtube` | Media & Entertainment | Official Upstream + Custom | **High** | **Active & Maintained** (`bundles/nexalloy`) | 25 patches active. Background playback, SponsorBlock, layout filtering, video speed dialog, swipe controls, DeArrow, Litho node bypass. |
| **YouTube Music** | `com.google.android.apps.youtube.music` | Media & Entertainment | Official Upstream + Custom | **High** | **Active & Maintained** (`bundles/nexalloy`) | 9 patches active. Audio ad removal, background playback, exclusive audio playback, hide upgrade buttons. |
| **Reddit** | `com.reddit.frontpage` | Communication & Social | Official Upstream + Custom | **High** | **Active & Maintained** (`bundles/nexalloy`) | 3 patches active. Feed ad hiding, comment ad state bypass, tracking link sanitizer. Semantic version guarding handles `SubmittedListing` changes. |
| **Google Photos** | `com.google.android.apps.photos` | Productivity & Google | Custom NexAlloy | **High** | **Active & Maintained** (`bundles/nexalloy`) | 2 patches active. `SpoofFeaturesPatch` (Pixel XL unlimited storage spoofing) and `EnableDCIMFoldersBackupControl`. Version-agnostic runtime feature injection. |
| **Instagram** | `com.instagram.android` | Communication & Social | Piko & 2eno Collections | **High** | **Active & Maintained** (`bundles/piko`, `bundles/2eno`) | 32 patches active. Ad blocking (feed/stories/reels), media downloader, ghost/anonymous view mode, link sanitization, stealth follow. |
| **Spotify** | `com.spotify.music` | Media & Entertainment | 2eno Collection | **High** | **Active & Maintained** (`bundles/2eno`) | 12 patches active. Audio ad muting, video/upsell ad view suppression, hide Premium navigation tab, Material You color palette, tracking cleaner. |
| **X (Twitter)** | `com.twitter.android` | Communication & Social | Piko Collection | **High** | **Active & Maintained** (`bundles/piko`) | 19 patches active. Ad & timeline promotion removal, timeline clutter filters, media downloader, sensitive media unblur, tracking parameter stripper. |
| **Duolingo** | `com.duolingo` | Productivity & Education | Upstream Research | **Medium** | **Documented Feasible** | Client indicators and debug flags can be hooked. User currently holds active official subscription; maintained in research matrix. |
| **Amazon Shopping** | `in.amazon.mShop.android.shopping` / `com.amazon.mShop.android.shopping` | Shopping & Commerce | Third-Party Community | **Medium-Low** | **Feasible for Telemetry/UI** | Video autoplay and search telemetry hooks feasible; localized India store package IDs require distinct DexKit fingerprints. |
| **Facebook** | `com.facebook.katana` | Communication & Social | Hushfacebook / Community | **Medium-Low** | **Excluded (High Churn)** | Extreme bytecode obfuscation and frequent weekly layout mutations make pure DexKit maintenance unstable compared to Instagram. |
| **LinkedIn** | `com.linkedin.android` | Communication & Social | Research Candidate | **Medium** | **Candidate for Link Stripper** | Share link `trk` stripping and sponsored feed hooks feasible; low client modification demand. |
| **Discord** | `com.discord` | Communication & Social | Aliucord / Vendetta Ecosystem | **Infeasible** | **Excluded** | Modern Discord runs on React Native / Hermes bytecode. Java bytecode hooks cannot modify UI; requires JavaScript engine patching. |
| **Snapchat** | `com.snapchat.android` | Communication & Social | SnapEnhance Ecosystem | **High Risk** | **Strictly Excluded** | Snapchat employs aggressive native memory integrity scanners and server-side attestation. Loading general Xposed modules triggers account bans. |
| **WhatsApp** | `com.whatsapp` | Communication & Social | Standalone Modules | **High Risk** | **Strictly Excluded** | Meta actively bans modified WhatsApp accounts via client hash checks and signature attestation. Out of scope for general bundle manager. |
| **Phone & Contacts** | `com.google.android.dialer`, `com.android.contacts` | Communication & Utility | System Privileged | **Low Utility** | **Excluded** | System framework dialers run in core OS boundaries; high risk of carrier dialer instability with zero functional gain. |
| **Messages (Google)** | `com.google.android.apps.messaging` | Communication & Social | Google System Apps | **Low Utility** | **Excluded** | Carrier RCS messaging relies on Google Play Services attestation and end-to-end encryption state machines. |
| **Google Chrome** | `com.android.chrome` | Productivity & Browsing | Chromium Engine | **Infeasible** | **Excluded** | Chrome network pipeline and rendering engine are compiled in native C++ (`libmonochrome.so`). Java hooks cannot reliably block network ads. |
| **Google Drive / Docs** | `com.google.android.apps.docs` | Productivity & Google | Google Workspace | **Low Utility** | **Excluded** | Cloud storage client; zero client-side advertisements or locked local features. |
| **Google Gemini / AI** | `com.google.android.apps.bard` | Productivity & AI | Google Cloud AI | **Infeasible** | **Excluded** | Pure thin client wrapper around server-side LLM endpoints; all reasoning, rate limits, and tokens are evaluated in Google cloud. |
| **PhonePe** | `com.phonepe.app` | Payments & Banking | Banking / UPI | **Strictly Prohibited** | **Strictly Excluded** | Hardware KeyStore, NPCI UPI security policy, SafetyNet/Play Integrity attestation. Hooking financial applications violates core module safety policies. |
| **Google Pay** | `com.google.android.apps.nbu.paisa.user` | Payments & Banking | Banking / UPI | **Strictly Prohibited** | **Strictly Excluded** | Hardware Keystore, Play Integrity MEETS_STRONG_INTEGRITY, RBI security rules. Hooking prohibited. |
| **Paytm / Navi / YONO SBI** | `net.one97.paytm`, `com.naviapp`, `com.sbi.lotusintouch` | Payments & Banking | Financial Institutions | **Strictly Prohibited** | **Strictly Excluded** | Financial risk, compliance violations, and immediate device flag. |
| **Bitwarden / Authenticator** | `com.x8bit.bitwarden`, `com.azure.authenticator` | Security & Auth | Password & 2FA Tokens | **Strictly Prohibited** | **Strictly Excluded** | Security credentials and TOTP generators must remain isolated from dynamic memory hooking. |
| **MX Player Pro** | `com.mxtech.videoplayer.pro` | Media & Entertainment | Standalone | **Not Needed** | **Excluded** | Paid license is ad-free; custom AC3/DTS audio codec packs are loaded natively via external storage, requiring no bytecode injection. |
| **CloudStream** | `com.lagradost.cloudstream3` | Media & Entertainment | Open Source | **Not Needed** | **Excluded** | Completely open-source (GPLv3), ad-free by design, and supports modular runtime provider extensions natively. |
| **Multiplayer Games** (Clash Royale, BGMI, 8 Ball Pool) | Various | Gaming | Server Authoritative | **Strictly Prohibited** | **Strictly Excluded** | Game logic is verified server-side; anti-cheat engines (Tencent ACE, BattlEye) trigger immediate permanent account bans upon detecting loaded hooks. |

---

## 3. Curated Inventory Status

To maintain clean repository hygiene, zero build overhead, and maximum stability, all non-installed target applications previously in the repository have been removed:
- **Photomath** (`com.microblink.photomath`): Removed (not installed; deprecated upstream).
- **Threads** (`com.instagram.barcelona`): Removed (not installed; zero shared code with Instagram bundle).
- **Strava** (`com.strava`): Removed (not installed).
- **AllTrails** (`com.alltrails.alltrails`): Removed (not installed).
- **Kleinanzeigen** (`com.ebay.kleinanzeigen`): Removed (German regional marketplace, not installed).
- **Untappd** (`com.untappdllc.app`): Removed (not installed; generic Google Ads helper preserved in `bundles/2eno`).
- **InterPals** (`net.interpals`): Removed (not installed).

---

## 4. Active Target Applications and Bundle Mapping

The curated patch catalog is organized into three clean native `.nxb` bundles:

1. **`nexalloy` (`nexalloy-1.0.5.nxb`):**
   - `com.google.android.youtube` (25 patches)
   - `com.google.android.apps.youtube.music` (9 patches)
   - `com.reddit.frontpage` (3 patches)
   - `com.google.android.apps.photos` (2 patches)
2. **`2eno` (`2eno-1.1.0-8.nxb`):**
   - `com.spotify.music` (12 patches)
   - `com.instagram.android` (Stealth Follow patch)
3. **`piko` (`piko-1.1.7.nxb`):**
   - `com.instagram.android` (31 patches)
   - `com.twitter.android` (19 patches)

All active bundles compile cleanly, pass all unit test suites, and package directly into native `.nxb` artifacts.

