package io.github.nexalloy.`2eno`

import io.github.nexalloy.AppPatchInfo
import io.github.nexalloy.AppTarget
import io.github.nexalloy.PatchBundle
import io.github.nexalloy.`2eno`.instagram.InstagramPatches
import io.github.nexalloy.`2eno`.spotify.SpotifyPatches

/** Entry point of the 2eno Patches bundle, named in its manifest. */
object Patches2eno : PatchBundle {
    override val apps = listOf(
        // Targets are the versions the patches were last tested with.
        AppPatchInfo(
            "Spotify", "com.spotify.music", SpotifyPatches,
            targets = listOf(AppTarget("9.1.88.2204")), appIconColor = 0x1ED760,
        ),
        AppPatchInfo(
            "Instagram", "com.instagram.android", InstagramPatches,
            targets = listOf(AppTarget("447.0.0.55.81")),
            appIconColor = 0xFC483C,
        ),
    )
}
