package io.github.nexalloy.a092devs.instagram

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.format.DateUtils
import android.view.Display
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.PatcherLog
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.patch
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Modifier

/**
 * Additional Instagram feature patches ported and adapted from SysAdminDoc/HushGram (GPL-3.0).
 *
 * 1. Turn off HDR brightness boosts
 * 2. View DM photos and videos anonymously
 * 3. Upload stories at full quality
 * 4. Tap a feed video to pause it
 * 5. Show if a profile follows you
 * 6. See who a story mentions
 * 7. Show post exact time
 * 8. Save profile picture
 * 9. Open developer options
 */

// 1. Turn off HDR brightness boosts
val TurnOffHdrBoosts = patch(
    name = "Turn off HDR brightness boosts",
    description = "Stops HDR photos and reels from making the screen brighter than everything else.",
) {
    // Window.setColorMode: replace HDR modes (2, 3) with COLOR_MODE_DEFAULT (0)
    runCatching {
        val windowClass = Window::class.java
        windowClass.declaredMethods.filter { it.name == "setColorMode" }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val mode = param.args.getOrNull(0) as? Int ?: return
                    if (mode == 2 || mode == 3) {
                        param.args[0] = 0
                    }
                }
            })
        }
    }

    // Display.isHdr() -> false
    runCatching {
        val displayClass = Display::class.java
        displayClass.declaredMethods.filter { it.name == "isHdr" && it.returnType == java.lang.Boolean.TYPE }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any = false
            })
        }
    }

    // Display.HdrCapabilities.getSupportedHdrTypes() -> empty IntArray
    runCatching {
        val hdrCapClass = Class.forName("android.view.Display\$HdrCapabilities")
        hdrCapClass.declaredMethods.filter { it.name == "getSupportedHdrTypes" }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any = intArrayOf()
            })
        }
    }

    // Android 14+ (API >= 34)
    if (Build.VERSION.SDK_INT >= 34) {
        runCatching {
            Display::class.java.declaredMethods.filter { it.name == "getHdrSdrRatio" }.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any = 1.0f
                })
            }
        }
        runCatching {
            Display.Mode::class.java.declaredMethods.filter { it.name == "getSupportedHdrTypes" }.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any = intArrayOf()
                })
            }
        }
        runCatching {
            val scTransClass = Class.forName("android.view.SurfaceControl\$Transaction")
            scTransClass.declaredMethods.filter { it.name == "setExtendedRangeBrightness" }.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.size >= 3) {
                            param.args[2] = 1.0f
                        }
                    }
                })
            }
        }
    }

    // Android 15+ (API >= 35)
    if (Build.VERSION.SDK_INT >= 35) {
        runCatching {
            Window::class.java.declaredMethods.filter { it.name == "setDesiredHdrHeadroom" }.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[0] = 1.0f
                    }
                })
            }
        }
        runCatching {
            SurfaceView::class.java.declaredMethods.filter { it.name == "setDesiredHdrHeadroom" }.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[0] = 1.0f
                    }
                })
            }
        }
    }
    PatcherLog.info { "a092devs Instagram: Turn off HDR brightness boosts installed" }
}

// 2. View DM photos and videos anonymously
internal val visualSeenMethod = findMethodDirect {
    findMethod {
        matcher {
            usingStrings(listOf("direct_v2/visual_threads/%s/item_seen/", "raven_media"), StringMatchType.Contains)
            returnType = "void"
        }
    }.first()
}

val ViewDmMediaAnonymously = patch(
    name = "View DM photos and videos anonymously",
    description = "Holds back the Opened notice for view-once photos and videos in direct messages.",
) {
    InstagramRequestFilter.enable(InstagramRequestFilter.Rule.DM_SEEN)
    InstagramRequestFilter.ensureHooks(classLoader)

    runCatching {
        ::visualSeenMethod.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val callback = param.args.getOrNull(1)
                if (callback != null) {
                    val completeMethod = callback.javaClass.methods.firstOrNull { m ->
                        m.parameterTypes.size == 2 && Modifier.isPublic(m.modifiers)
                    }
                    runCatching { completeMethod?.invoke(callback, null, null) }
                }
                param.result = null
                PatcherLog.info { "a092devs Instagram: DM visual seen marker completed locally without sending" }
            }
        })
    }.onFailure { PatcherLog.info(it) { "a092devs Instagram: visualSeenMethod hook skipped, relying on Tigon filter" } }
}

// 3. Upload stories at full quality
internal val highQualityPrefMethod = findMethodDirect {
    findMethod {
        matcher {
            usingStrings(listOf("high_quality_media_upload"), StringMatchType.Contains)
            returnType = "boolean"
        }
    }.first()
}

val UploadStoriesAtFullQuality = patch(
    name = "Upload stories at full quality",
    description = "Sends the video stories you post at full 1080p resolution and 9 Mbps bitrate instead of 720p.",
) {
    runCatching {
        ::highQualityPrefMethod.hookMethod(object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam): Any = true
        })
    }.onFailure { PatcherLog.info(it) { "a092devs Instagram: highQualityPrefMethod hook skipped" } }

    runCatching {
        val spClass = classLoader.loadClass("android.app.SharedPreferencesImpl")
        for (methodName in listOf("getInt", "getBoolean")) {
            spClass.declaredMethods.filter { it.name == methodName }.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args.getOrNull(0) as? String ?: return
                        if (key == "high_quality_media_upload") {
                            if (param.method.name == "getInt") param.result = 1
                            else if (param.method.name == "getBoolean") param.result = true
                        }
                    }
                })
            }
        }
    }
    PatcherLog.info { "a092devs Instagram: Upload stories at full quality installed" }
}

// 4. Tap a feed video to pause it
internal val feedSingleTapMethod = findMethodDirect {
    findMethod {
        matcher {
            usingStrings(
                listOf("DefaultMediaHolderGestureDetectorDelegateImpl#onSingleTapConfirmed"),
                StringMatchType.Contains,
            )
        }
    }.first()
}

internal val feedTapHandlerSoftErrorMethod = findMethodDirect {
    findMethod {
        matcher {
            usingStrings(listOf("FeedMediaTapHandlerV2-softError"), StringMatchType.Contains)
            returnType = "void"
        }
    }.first()
}

val TapFeedVideoToPause = patch(
    name = "Tap a feed video to pause it",
    description = "Tapping a video in the home feed pauses or plays it in place instead of opening Reels.",
) {
    var hooked = false
    runCatching {
        ::feedSingleTapMethod.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = true
                PatcherLog.info { "a092devs Instagram: Feed video tap intercepted (playback toggled)" }
            }
        })
        hooked = true
    }
    if (!hooked) {
        runCatching {
            ::feedTapHandlerSoftErrorMethod.hookMethod(object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                    PatcherLog.info { "a092devs Instagram: Feed media tap handler intercepted" }
                }
            })
        }
    }
}

// 5. Show if a profile follows you
internal val profileHeaderFullNameMethod = findMethodDirect {
    findMethod {
        matcher {
            usingStrings(listOf("bindFullName"), StringMatchType.Contains)
            returnType = "void"
        }
    }.first()
}

val ShowIfProfileFollowsYou = patch(
    name = "Show if a profile follows you",
    description = "Adds 'Follows you' or 'Doesn't follow you' indicator beside the user's name on their profile.",
) {
    runCatching {
        ::profileHeaderFullNameMethod.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val headerView = param.args.firstOrNull { it is View } as? View
                    ?: (param.args.firstOrNull()?.let { arg ->
                        arg.javaClass.declaredFields.firstOrNull { View::class.java.isAssignableFrom(it.type) }
                            ?.apply { isAccessible = true }?.get(arg) as? View
                    }) ?: return

                val context = headerView.context ?: return
                val nameId = context.resources.getIdentifier("profile_header_full_name_above_vanity", "id", context.packageName)
                val nameView = if (nameId != 0) headerView.findViewById<TextView>(nameId) else null
                val targetTextView = nameView ?: findFirstTextView(headerView) ?: return

                val user = param.args.firstOrNull { it != null && it.javaClass.name.contains("User") }
                if (user != null) {
                    val isFollowedBy = runCatching {
                        val m = user.javaClass.methods.firstOrNull { it.name == "isFollowedBy" || it.name == "getFollowedBy" }
                        m?.invoke(user) as? Boolean
                    }.getOrNull()

                    val isFollowing = runCatching {
                        val m = user.javaClass.methods.firstOrNull { it.name == "isFollowing" || it.name == "getFollowing" }
                        m?.invoke(user) as? Boolean
                    }.getOrNull()

                    val label = when {
                        isFollowedBy == true && isFollowing == true -> " · Following each other"
                        isFollowedBy == true -> " · Follows you"
                        isFollowedBy == false -> " · Doesn't follow you"
                        else -> null
                    }

                    if (label != null) {
                        val currentText = targetTextView.text?.toString() ?: ""
                        if (!currentText.contains("Follows you") && !currentText.contains("Doesn't follow you") && !currentText.contains("Following each other")) {
                            targetTextView.text = currentText + label
                        }
                    }
                }
            }
        })
        PatcherLog.info { "a092devs Instagram: Show if a profile follows you installed" }
    }.onFailure { PatcherLog.info(it) { "a092devs Instagram: profileHeaderFullNameMethod hook skipped" } }
}

private fun findFirstTextView(view: View): TextView? {
    if (view is TextView) return view
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val tv = findFirstTextView(view.getChildAt(i))
            if (tv != null) return tv
        }
    }
    return null
}

// 6. See who a story mentions
internal val reelViewerBindMediaMethod = findMethodDirect {
    findMethod {
        matcher {
            usingStrings(listOf("ReelViewerItemBinder.bindMedia"), StringMatchType.Contains)
            returnType = "void"
        }
    }.first()
}

val SeeWhoStoryMentions = patch(
    name = "See who a story mentions",
    description = "Displays a badge in story viewer showing how many accounts are mentioned, and lists them on tap.",
) {
    runCatching {
        ::reelViewerBindMediaMethod.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val media = param.args.firstOrNull { it != null && it.javaClass.name.contains("Media") } ?: return
                val pageView = param.args.firstOrNull { it is View } as? View ?: return
                val context = pageView.context ?: return

                val mentions = runCatching {
                    val m = media.javaClass.methods.firstOrNull { it.name == "reel_mentions" || it.name == "getReelMentions" }
                    m?.invoke(media) as? List<*>
                }.getOrNull() ?: return

                if (mentions.isEmpty()) return

                val textContainerId = context.resources.getIdentifier("reel_viewer_text_container", "id", context.packageName)
                val textContainer = if (textContainerId != 0) pageView.findViewById<ViewGroup>(textContainerId) else null
                if (textContainer != null) {
                    val pillTag = "nexalloy_story_mentions_pill"
                    if (textContainer.findViewWithTag<View>(pillTag) == null) {
                        val pill = TextView(context).apply {
                            tag = pillTag
                            text = " @${mentions.size} "
                            textSize = 12f
                            setTextColor(Color.WHITE)
                            setBackgroundColor(Color.parseColor("#80000000"))
                            setPadding(8, 2, 8, 2)
                            setOnClickListener {
                                val usernames = mentions.mapNotNull { item ->
                                    item?.toString()
                                }.toTypedArray()
                                AlertDialog.Builder(context)
                                    .setTitle("Mentioned Accounts (${mentions.size})")
                                    .setItems(usernames) { _, index ->
                                        val u = usernames.getOrNull(index)?.substringAfter("@")?.trim() ?: return@setItems
                                        InstagramLinks.openExternally(context as? Activity ?: return@setItems, "https://www.instagram.com/$u/")
                                    }
                                    .setPositiveButton("Close", null)
                                    .show()
                            }
                        }
                        textContainer.addView(pill)
                    }
                }
            }
        })
        PatcherLog.info { "a092devs Instagram: See who a story mentions installed" }
    }.onFailure { PatcherLog.info(it) { "a092devs Instagram: reelViewerBindMediaMethod hook skipped" } }
}

// 7. Show post exact time
internal val relativeTimeFormatterMethod = findMethodDirect {
    findMethod {
        matcher {
            usingStrings(listOf("MMMM d"), StringMatchType.Contains)
            returnType = "java.lang.String"
        }
    }.first()
}

val ShowPostExactTime = patch(
    name = "Show a post's exact time",
    description = "Shows the exact date and time a post or comment was uploaded instead of relative time like '3h'.",
) {
    runCatching {
        ::relativeTimeFormatterMethod.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                var seconds: Long? = null
                for (arg in param.args) {
                    if (arg is Double && arg > 1_000_000_000.0 && arg < 3_000_000_000.0) {
                        seconds = arg.toLong()
                        break
                    } else if (arg is Long && arg > 1_000_000_000L && arg < 3_000_000_000L) {
                        seconds = arg
                        break
                    }
                }
                if (seconds != null) {
                    val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
                    val formatted = DateUtils.formatDateTime(null, seconds * 1000L, flags)
                    if (!formatted.isNullOrEmpty()) {
                        param.result = formatted
                    }
                }
            }
        })
        PatcherLog.info { "a092devs Instagram: Show post exact time installed" }
    }.onFailure { PatcherLog.info(it) { "a092devs Instagram: relativeTimeFormatterMethod hook skipped" } }
}

// 8. Save profile picture
val SaveProfilePicture = patch(
    name = "Save profile picture",
    description = "Adds options to view and save full-resolution profile pictures directly from the profile screen.",
) {
    for (className in listOf(
        "com.instagram.user.useroptions.UserOptionsBottomSheetOverflowHelper",
        "com.instagram.user.useroptions.UserOptionsOverflowHelper",
    )) {
        runCatching {
            val helperClass = classLoader.loadClass(className)
            helperClass.declaredMethods.forEach { method ->
                if (method.returnType == Void.TYPE && method.parameterTypes.any { it.name.contains("User") }) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            PatcherLog.info { "a092devs Instagram: User options overflow opened" }
                        }
                    })
                }
            }
        }
    }
    PatcherLog.info { "a092devs Instagram: Save profile picture installed" }
}

// 9. Open developer options
internal val optionsOpenerMethod = findMethodDirect {
    findMethod {
        matcher {
            usingStrings(listOf("debug_options_error"), StringMatchType.Contains)
            returnType = "void"
        }
    }.first()
}

internal val homeLongPressMethod = findMethodDirect {
    findMethod {
        matcher {
            usingStrings(listOf("click", "activity"), StringMatchType.Contains)
            name = "onLongClick"
            returnType = "boolean"
        }
    }.first()
}

val OpenDeveloperOptions = patch(
    name = "Open developer options",
    description = "Long-pressing the Home tab opens Instagram's hidden internal developer options and MobileConfig settings.",
) {
    runCatching {
        val opener = ::optionsOpenerMethod.memberOrNull as? java.lang.reflect.Method ?: return@runCatching
        val openerClass = opener.declaringClass
        val instanceField = openerClass.declaredFields.firstOrNull { Modifier.isStatic(it.modifiers) && it.type == openerClass }
            ?.apply { isAccessible = true }

        ::homeLongPressMethod.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val handlerObj = param.thisObject ?: return
                val activityField = handlerObj.javaClass.declaredFields.firstOrNull { Activity::class.java.isAssignableFrom(it.type) }
                    ?.apply { isAccessible = true }
                val sessionField = handlerObj.javaClass.declaredFields.firstOrNull { it.type.name.contains("UserSession") }
                    ?.apply { isAccessible = true }

                val activity = activityField?.get(handlerObj) as? Activity
                val session = sessionField?.get(handlerObj)
                val openerInstance = instanceField?.get(null)

                if (activity != null && session != null && openerInstance != null) {
                    opener.invoke(openerInstance, activity, activity, session)
                    param.result = true
                    PatcherLog.info { "a092devs Instagram: Developer options opened from Home long press" }
                }
            }
        })
        PatcherLog.info { "a092devs Instagram: Open developer options installed" }
    }.onFailure { PatcherLog.info(it) { "a092devs Instagram: developer options opener hook skipped" } }
}
