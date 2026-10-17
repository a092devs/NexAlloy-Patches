package io.github.nexalloy.nexfusion.mixplorer

import android.database.Cursor
import android.database.CursorWrapper
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Process
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.PatcherLog
import io.github.nexalloy.patch

// Ported from ak800i/mixplorer-patches-for-morphe (GPL-3.0), SharingFixPatch.kt.
//
// Upstream rewrites FileProvider.query so external callers get a null `_data` value. Wrapping the
// returned cursor hides the same value; the column list and same-app queries stay untouched, and
// the patch only applies on Android 11+ where the hidden paths are unreadable anyway.

val FixScopedStorageFileSharing = patch(
    name = "Fix scoped-storage file sharing",
    description = "Hides unreadable _data filesystem paths from other apps when MiXplorer shares files on Android 11+.",
) {
    try {
        val provider = classLoader.loadClass("com.mixplorer.providers.FileProvider")
        XposedHelpers.findAndHookMethod(
            provider, "query",
            Uri::class.java, Array<String>::class.java, String::class.java,
            Array<String>::class.java, String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val cursor = param.result as? Cursor ?: return
                    val projection = param.args.getOrNull(1) as? Array<*>
                    val external = ScopedStorage.masksDataColumn(
                        Build.VERSION.SDK_INT, Binder.getCallingUid(), Process.myUid(), projection,
                    )
                    if (!external) return
                    param.result = HiddenDataCursor(cursor)
                }
            },
        )
    } catch (t: Throwable) {
        PatcherLog.info(t) { "NexFusion: MiXplorer FileProvider.query not found" }
    }
}

val MixplorerPatches = arrayOf(FixScopedStorageFileSharing)

/** Serves null for the `_data` column, like a cursor row where the value was never written. */
private class HiddenDataCursor(cursor: Cursor) : CursorWrapper(cursor) {
    private val dataColumn = cursor.getColumnIndex(ScopedStorage.DATA_COLUMN)

    override fun getString(columnIndex: Int): String? =
        if (columnIndex == dataColumn) null else super.getString(columnIndex)

    override fun isNull(columnIndex: Int): Boolean =
        if (columnIndex == dataColumn) true else super.isNull(columnIndex)

    override fun getType(columnIndex: Int): Int =
        if (columnIndex == dataColumn) Cursor.FIELD_TYPE_NULL else super.getType(columnIndex)
}
