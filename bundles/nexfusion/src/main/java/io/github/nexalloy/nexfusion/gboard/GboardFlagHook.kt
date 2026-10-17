package io.github.nexalloy.nexfusion.gboard

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.PatcherLog
import io.github.nexalloy.PatchExecutor
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

// Ported from jasonwu1994/Gboard-patches (GPL-3.0) and Akshayykadam/PixelBoard (GPL-3.0):
// - GboardFlagFamilyComposer and *Policy.java handle Phenotype flag overrides.
// - PixelBoard provides Belka rounded keycap and AI writing assistant flag mappings and
//   SharedPreferences persistence.

/** What an override does with a flag's stock value. */
class FlagOverride private constructor(
    private val accepts: (Any?) -> Boolean,
    private val replacement: Any,
) {
    fun applyTo(stock: Any?): Any? = if (accepts(stock)) replacement else stock

    companion object {
        /** Flips a stock false to true, like upstream's policies that check `Boolean.FALSE.equals`. */
        fun promote(): FlagOverride = FlagOverride({ it == false }, true)

        /** Replaces any non-null boolean (upstream checks only the type). */
        fun forceBoolean(): FlagOverride = FlagOverride({ it is Boolean }, true)

        /** Replaces any boxed long, like the count and character-limit policies. */
        fun forceLong(value: Long): FlagOverride = FlagOverride({ it is Long }, value)

        /** Replaces any string, like the "all languages" allowlist policies. */
        fun forceString(value: String): FlagOverride = FlagOverride({ it is String }, value)
    }
}

/**
 * Known obfuscated class names that implement the Phenotype flag interface across Gboard releases.
 * `actb` is the plain flag and `actq` carries the metadata on Gboard 18.4.1.
 */
private val KNOWN_FLAG_CLASS_NAMES = listOf(
    "actb", "actq", // Gboard 18.4.1 beta
    "acps", "obw", "ntk", "nuw", "lhf", "lhg", "kgj", "acta", "actc", "jzp", "jzq"
)

private val overrides = LinkedHashMap<String, FlagOverride>()
private val nameFields = ConcurrentHashMap<Class<*>, Field>()
private var installed = false
private var roundedKeysPrefHooked = false

/**
 * Ensures Gboard's rounded keys preference is permanently enabled in both SharedPreferences
 * storage and runtime lookups.
 */
fun PatchExecutor.enableGboardRoundedKeysPreference() {
    runCatching {
        val prefs = appContext.getSharedPreferences(
            "com.google.android.inputmethod.latin_preferences",
            android.content.Context.MODE_PRIVATE
        )
        prefs.edit().putBoolean("enable_rounded_keys", true).apply()
    }

    if (!roundedKeysPrefHooked) {
        roundedKeysPrefHooked = true
        runCatching {
            val sharedPrefsImpl = runCatching { classLoader.loadClass("android.app.SharedPreferencesImpl") }.getOrNull()
                ?: Class.forName("android.app.SharedPreferencesImpl")
            XposedHelpers.findAndHookMethod(
                sharedPrefsImpl,
                "getBoolean",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String
                        if (key == "enable_rounded_keys") {
                            param.result = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Registers [entries] and installs the shared value-getter hook on first use. Called from each
 * Gboard patch while it runs, so a disabled patch never contributes its flags.
 */
fun PatchExecutor.gboardFlagOverrides(vararg entries: Pair<String, FlagOverride>) {
    synchronized(overrides) {
        for ((name, override) in entries) overrides.putIfAbsent(name, override)
    }
    if (installed) return

    val hook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val receiver = param.thisObject ?: return
            val override = synchronized(overrides) { overrides[flagName(receiver)] } ?: return
            param.result = override.applyTo(param.result)
        }
    }

    var hooked = 0
    for (className in KNOWN_FLAG_CLASS_NAMES) {
        val flagClass = runCatching { classLoader.loadClass(className) }.getOrNull() ?: continue
        val hasGetter = flagClass.declaredMethods.any {
            it.name == "g" && it.parameterTypes.isEmpty() && it.returnType == Any::class.java
        }
        if (!hasGetter) continue
        XposedBridge.hookAllMethods(flagClass, "g", hook)
        hooked++
    }

    if (hooked == 0) {
        PatcherLog.info { "NexFusion: Gboard flag getter not found among known classes in this build" }
    } else {
        installed = true
        PatcherLog.info { "NexFusion: Gboard flag hook installed on $hooked class(es)" }
    }
}

/** The flag's name, read from the name field of its class; null when the receiver is unexpected. */
private fun flagName(receiver: Any): String? {
    val receiverClass = receiver.javaClass
    val field = nameFields[receiverClass] ?: run {
        val resolved = runCatching {
            receiverClass.getDeclaredField("a").apply { isAccessible = true }
        }.getOrNull() ?: runCatching {
            receiverClass.declaredFields.firstOrNull { it.type == String::class.java }?.apply { isAccessible = true }
        }.getOrNull() ?: return null
        nameFields.putIfAbsent(receiverClass, resolved) ?: resolved
    }
    return runCatching { field.get(receiver) as? String }.getOrNull()
}

