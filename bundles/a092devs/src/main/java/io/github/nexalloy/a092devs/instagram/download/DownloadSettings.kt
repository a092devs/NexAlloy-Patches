package io.github.nexalloy.a092devs.instagram.download

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore

// Added for NexAlloy: the download options of the a092devs contract — where a save lands, whether
// each poster gets their own folder, and whether file names carry the time of the save. They live
// in the app's own preferences, written by the settings dialog in DownloadPatches and read once at
// the start of a download, so a save costs one small read and nothing else.

/**
 * The options a save obeys.
 */
internal class DownloadSettings private constructor(private val prefs: SharedPreferences) {

    /** The directory saves start at: [DOWNLOADS] under Download, [GALLERY] under Pictures and Movies. */
    enum class Location(val label: String) {
        DOWNLOADS("Downloads"),
        GALLERY("Pictures and Movies"),
    }

    var location: Location
        get() = Location.values().firstOrNull { it.name == prefs.getString(KEY_LOCATION, null) } ?: Location.DOWNLOADS
        set(value) { prefs.edit().putString(KEY_LOCATION, value.name).apply() }

    /** Whether each poster's saves land in a folder named after them. */
    var perUsername: Boolean
        get() = prefs.getBoolean(KEY_PER_USERNAME, false)
        set(value) { prefs.edit().putBoolean(KEY_PER_USERNAME, value).apply() }

    /** Whether file names carry the time of the save. */
    var timestamp: Boolean
        get() = prefs.getBoolean(KEY_TIMESTAMP, false)
        set(value) { prefs.edit().putBoolean(KEY_TIMESTAMP, value).apply() }

    /** The folder under the base directory, with [username]'s subfolder when asked for. */
    fun folder(username: String?): String {
        val name = username?.takeIf { perUsername }
            ?.filter { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
            ?.takeIf { it.isNotBlank() }
        return if (name != null) "$FOLDER/$name" else FOLDER
    }

    /** The public directory a save of this kind starts at. */
    fun baseDirectory(isVideo: Boolean): String = when {
        location == Location.DOWNLOADS -> Environment.DIRECTORY_DOWNLOADS
        isVideo -> Environment.DIRECTORY_MOVIES
        else -> Environment.DIRECTORY_PICTURES
    }

    /** The MediaStore collection a save of this kind is inserted into. */
    fun collection(isVideo: Boolean): Uri = when {
        location == Location.DOWNLOADS -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
        isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    /** Where a save of [username] lands, for the toasts and the log. */
    fun destination(username: String?, isVideo: Boolean) = "${baseDirectory(isVideo)}/${folder(username)}"

    companion object {
        private const val PREFS = "nexalloy_downloads"
        private const val KEY_LOCATION = "save_location"
        private const val KEY_PER_USERNAME = "per_username_folders"
        private const val KEY_TIMESTAMP = "timestamp_names"

        /** The folder of the a092devs contract, under whichever base the person chose. */
        const val FOLDER = "Instagram"

        fun open(context: Context) = DownloadSettings(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
    }
}
