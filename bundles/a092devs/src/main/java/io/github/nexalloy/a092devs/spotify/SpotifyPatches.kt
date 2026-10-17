package io.github.nexalloy.a092devs.spotify

import io.github.nexalloy.a092devs.spotify.ads.BlockPopupAds
import io.github.nexalloy.a092devs.spotify.ads.HideAdSections
import io.github.nexalloy.a092devs.spotify.ads.HideAdViews
import io.github.nexalloy.a092devs.spotify.ads.HideContextMenuUpsells
import io.github.nexalloy.a092devs.spotify.ads.HidePlaylistAds
import io.github.nexalloy.a092devs.spotify.ads.HideVideoAds
import io.github.nexalloy.a092devs.spotify.ads.MuteAudioAds
import io.github.nexalloy.a092devs.spotify.layout.HidePremiumTab
import io.github.nexalloy.a092devs.spotify.misc.FixThirdPartyLaunchersWidgets
import io.github.nexalloy.a092devs.spotify.privacy.SanitizeSharingLinks
import io.github.nexalloy.a092devs.spotify.theme.MaterialYouTheme
import io.github.nexalloy.a092devs.spotify.theme.RoundedCorners

val SpotifyPatches = arrayOf(
    MuteAudioAds,
    HideAdSections,
    HideAdViews,
    HideContextMenuUpsells,
    BlockPopupAds,
    HidePlaylistAds,
    HideVideoAds,
    HidePremiumTab,
    FixThirdPartyLaunchersWidgets,
    SanitizeSharingLinks,
    MaterialYouTheme,
    RoundedCorners,
)
