# Changelog

All notable changes to the NexAlloyNext patch bundles are documented here. Releases are dispatched
automatically on every push to `main` for the version of the first `X.Y.Z` section below when that
version has no release yet. The section matching the release tag becomes the release notes,
falling back to generated commit notes when no section exists.

## 2.6.0

- `a092devs` 1.4.0:
  * Integrated comprehensive Instagram (`com.instagram.android`) patch suite ported from `SysAdminDoc/HushGram`, targeting `451.0.0.53.80`, `450.0.0.46.79`, and `447.0.0.55.81`:
    - Hide ads: suppresses sponsored posts, feed ad units (`INTENT_AWARE_AD_PIVOT`, `STAND_ALONE_MULTI_AD_PIVOT`), injected reels, and profile ads.
    - Hide suggested posts & reels: filters explore chaining, suggestions in feed, and recommended accounts.
    - Ghost mode: anonymous story viewing (blocks `media/seen/` receipts), anonymous live watching (blocks heartbeat presence pings), anonymous DMs (blocks seen/read receipts), and disables typing indicators.
    - Privacy: removes `FLAG_SECURE` for screenshots and screen recordings in disappearing chats, blocks screenshot detection callbacks and observers, sanitizes tracking tags (`igsh`, `utm_*`, `fbclid`) from share URLs, bypasses in-app browser to open links externally, and disables analytics/telemetry.
    - Download any media without watermarks, ask before like, ask before refresh, and unlocked developer options.
  * Expanded Facebook (`com.facebook.katana`) patches ported from `SysAdminDoc/Hushfacebook` targeting `582.0.0.50.54` and `581.0.0.45.54`:
    - Hide sponsored posts, feed ads, comment sheet ad pills, sponsored reels, and marketplace ads.
    - Block ad telemetry, background ad prefetch jobs, and advertising ID tracking sinks.
    - Bypass in-app browser to open links in default system browser, and download media.
- `piko`:
  * Migrated Instagram patches into `a092devs.nxb` to focus solely on X (Twitter).
- Repository maintenance:
  * Removed untracked/redundant `reports/` directory from commit tree and updated `.gitignore`.

## 2.5.0

- `a092devs` 1.4.0:
  * Expanded Proton VPN (`ch.protonvpn.android`) runtime patches ported from `hxreborn/morphe-patches` and `hoo-dles/morphe-patches`:
    - Remove server change delay: eliminates cooldown wait periods between server switching on free plans (`AppConfigResponse`).
    - Unlock advanced settings & NetShield: unlocks NetShield ad/tracker blocking, split tunneling (app & IP routing), custom DNS, and allow LAN connections.
    - Suppress upgrade promotions: eliminates onboarding upgrade dialogs, promotional alerts, and account upgrade banners.
    - Updated verified compatibility targets to include `5.20.74.0`, `5.20.57.0`, and `5.20.39.0` alongside `5.19.x`.

## 2.4.0

- `nexalloy` 1.0.7: ported four runtime player and layout patches from Morphe/yollAmeF:
  * Ambient Mode: options to bypass power-saving restrictions and disable Ambient mode dimming in fullscreen.
  * Miniplayer: modern miniplayer flag overrides, horizontal drag support, and offscreen repositioning hooks.
  * Picture-in-picture seek buttons: rewind and forward (±5s) controls injected directly into Android Picture-in-Picture window actions.
  * Disable playlist autoplay: suppresses automatic queue advancement to the next playlist video.
- `a092devs` 1.3.0:
  * Proton VPN (`ch.protonvpn.android`): runtime patches ported from Paresh-Patches to disable telemetry/observability workers, suppress upgrade onboarding dialogs, and unlock NetShield UI feature availability.
  * Gboard (`com.google.android.inputmethod.latin`): expanded with dynamic Phenotype flag class detection and SharedPreferences hook for Belka rounded keycaps, Gemini AI writing tools, Scan Text OCR, floating screenshot chips, Emoji Palette V2, Smart Compose predictions, and spellchecker suggestion chips from PixelBoard.
- `2eno`: Spotify runtime updates for APS banner handling, upsell suppression, and fingerprint stability.

## 2.3.0

- `nexalloy`: version-gated fingerprint updates for YouTube 21.41 (background playback settings,
  player type target and the shorts-resume constructor split).
- `a092devs`: Discord "Use your phone's font" option (ReactFontManager system-typeface hook ported
  from Venus Patches; off by default) and seven Reddit runtime ports from Adobo — screenshot-banner
  disable, external links without `out.reddit.com`, home-feed auto-refresh disable, home-screen
  redirect disable, transparent navigation bar, upvote-score hiding and post view-count hiding.
  The Reddit matchers were verified against 2026.41.0 fingerprints; score/view-count/nav-bar and
  auto-refresh ports are opt-in, and the score/view-count ports stub getters only (upstream also
  rewrites toString fields, which runtime hooks cannot).
- `a092devs` 1.2.0: three new app tables ported as runtime hooks. Gboard 18.4.1 gets 13 flag
  patches (cursor trackpad, quick insert, emoji size, access points menu redesign, Bluetooth
  microphone, close proactive suggestions, access point count, clipboard character limit, flow
  mode, advanced voice typing, grammar checker, inline suggestions, key shapes) from
  jasonwu1994/Gboard-patches; all of them share one flag-getter hook, and the access point count
  and advanced voice patches are off by default. Facebook 582 gets allow-screenshots and
  block-screenshot-detection from SysAdminDoc/Hushfacebook. LinkedIn gets hide sponsored
  messages, ghost mode typing indicator and read receipts, disable double-tap like and hide feed
  ads from heyymichii/michii-patches. The parts that need patch-time resources are not ported
  (Gboard OCR plumbing and AI writing tools, LinkedIn DownloadMedia, the Facebook feed patches);
  each bundle's NOTICE lists the ported material per source.
- Repository tooling: release notes now come from this changelog, and a push to `main` dispatches
  the release for the first version section automatically.

## 2.1.0 and earlier

See the GitHub Releases page for earlier release notes.
