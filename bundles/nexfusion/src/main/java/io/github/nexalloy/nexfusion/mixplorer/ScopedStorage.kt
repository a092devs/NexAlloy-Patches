package io.github.nexalloy.nexfusion.mixplorer

// Ported from ak800i/mixplorer-patches-for-morphe (GPL-3.0), SharingFixPatch.kt.
// Plain Kotlin (no Android types) so it can be tested off device.

/**
 * Decides when MiXplorer's `FileProvider.query` result hides the `_data` filesystem path.
 *
 * The upstream patch nulls the `_data` value for callers outside MiXplorer on Android 11+ (where
 * raw paths are unreadable under scoped storage) and leaves the `path` alias and content URIs
 * alone, so receiving apps fall back to the content stream.
 */
internal object ScopedStorage {
    const val DATA_COLUMN = "_data"

    /** Lowest Android version where scoped storage makes raw paths unreadable, like upstream's minSdk. */
    const val MIN_SDK = 30

    /**
     * @param projection The columns the caller queried; null means every column.
     * @return Whether the `_data` value must be hidden from this caller.
     */
    fun masksDataColumn(sdkInt: Int, callerUid: Int, selfUid: Int, projection: Array<*>?): Boolean {
        if (sdkInt < MIN_SDK) return false
        if (callerUid == selfUid) return false
        return projection == null || projection.any { it == DATA_COLUMN }
    }

    /** Whether [columnName] is the hidden `_data` column. */
    fun isDataColumn(columnName: String?): Boolean = columnName == DATA_COLUMN
}
