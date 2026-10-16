package io.github.nexalloy.a092devs.linkedin

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.PatcherLog
import io.github.nexalloy.patch
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

// Ported from heyymichii/michii-patches (GPL-3.0):
// - extensions/extension/src/main/java/app/linkedin/extension/MessagingPatch.java
// - extensions/extension/src/main/java/app/linkedin/extension/DoubleTapPatch.java
// - extensions/extension/src/main/java/app/linkedin/extension/HideAdsPatch.java and SduiComponentFilter.java
//   (the sponsored feed update rules only)
// - patches/src/main/kotlin/app/linkedin/patches/{messaging,feed,ads}/
//
// Upstream rewrites the LinkedIn call sites so its extension receives the model objects; this port
// hooks the same methods at runtime and replaces the in-app settings with the manager's per-patch
// toggles. LinkedIn keeps the model field names its own code reads, so the filters below read the
// fields reflectively instead of being compiled against the app.

val HideSponsoredMessages = patch(
    name = "Hide sponsored messages",
    description = "Removes sponsored InMail conversations from the messaging list.",
) {
    try {
        val input = classLoader.loadClass(
            "com.linkedin.android.messaging.conversationlist.ConversationListItemTransformer\$TransformerInput",
        )
        val constructor = input.declaredConstructors.firstOrNull { candidate ->
            val types = candidate.parameterTypes
            types.size == 5 &&
                types[0] == List::class.java &&
                types[1] == List::class.java &&
                types[2] == ArrayList::class.java &&
                types[3] == Boolean::class.javaPrimitiveType
        } ?: throw NoSuchMethodException("TransformerInput(List, List, ArrayList, boolean, Urn)")
        XposedBridge.hookMethod(
            constructor,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val conversations = param.args.getOrNull(0) as? List<*> ?: return
                    param.args[0] = conversations.filterNot(::isSponsoredConversation)
                }
            },
        )
    } catch (t: Throwable) {
        PatcherLog.info(t) { "a092devs: LinkedIn conversation list target not found" }
    }
}

val GhostNoTypingIndicator = patch(
    name = "Ghost mode: hide typing indicator",
    description = "Stops LinkedIn from sending the \"typing...\" state to the other person.",
) {
    try {
        val store = classLoader.loadClass(
            "com.linkedin.android.messenger.data.networking.impl.ConversationWriteNetworkStoreImpl",
        )
        val success = classLoader.loadClass("com.linkedin.android.architecture.data.Resource")
            .getMethod("success", Any::class.java)
        val targets = store.declaredMethods.filter {
            it.name == "sendTypingIndicator" && it.returnType == Any::class.java
        }
        if (targets.isEmpty()) throw NoSuchMethodException("sendTypingIndicator(...)")
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = success.invoke(null, null as Any?)
            }
        }
        targets.forEach { XposedBridge.hookMethod(it, hook) }
    } catch (t: Throwable) {
        PatcherLog.info(t) { "a092devs: LinkedIn typing indicator target not found" }
    }
}

val GhostNoReadReceipts = patch(
    name = "Ghost mode: no read receipts",
    description = "Keeps conversations unread on the other side while you are chatting.",
) {
    try {
        val impl = classLoader.loadClass("com.linkedin.android.messaging.sdk.MessagingSdkWriteFlowFeatureImpl")
        val targets = impl.declaredMethods.filter {
            it.name == "updateConversationReadStatus" &&
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == List::class.java &&
                it.parameterTypes[1] == Boolean::class.javaPrimitiveType
        }
        if (targets.isEmpty()) throw NoSuchMethodException("updateConversationReadStatus(List, boolean)")
        val liveDataClass = classLoader.loadClass("androidx.lifecycle.MutableLiveData")
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (param.args.getOrNull(1) != true) return
                if (!calledFromChatList()) return
                param.result = liveDataClass.getDeclaredConstructor().newInstance()
            }
        }
        targets.forEach { XposedBridge.hookMethod(it, hook) }
    } catch (t: Throwable) {
        PatcherLog.info(t) { "a092devs: LinkedIn read status target not found" }
    }
}

val DisableDoubleTapLike = patch(
    name = "Disable double-tap like",
    description = "Stops a double tap on a feed post from liking it.",
) {
    try {
        val clickActions = classLoader.loadClass("com.linkedin.sdui.viewdata.action.ClickActions")
        // Upstream's fingerprint is (ActionListViewData, ActionListViewData, ActionListViewData);
        // 4.1.1261 appended a fourth InteractionPhaseActions parameter, with onDoubleClick still in
        // the third slot.
        val targets = clickActions.declaredConstructors.filter { candidate ->
            val types = candidate.parameterTypes
            types.size >= 3 &&
                (0..2).all { types[it].name == "com.linkedin.sdui.viewdata.action.ActionListViewData" }
        }
        if (targets.isEmpty()) throw NoSuchMethodException("ClickActions(ActionListViewData x3)")
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.args[2] = null
            }
        }
        targets.forEach { XposedBridge.hookMethod(it, hook) }
    } catch (t: Throwable) {
        PatcherLog.info(t) { "a092devs: LinkedIn click actions target not found" }
    }
}

val HideFeedAds = patch(
    name = "Hide feed ads",
    description = "Removes promoted posts from the feed, in the server-driven and legacy lists.",
) {
    // Server driven feed: every component passes through ComponentTransformer.doTransform, and it
    // already returns null for components it does not render.
    try {
        val transformer = classLoader.loadClass("com.linkedin.sdui.transformer.impl.ComponentTransformer")
        val targets = transformer.declaredMethods.filter {
            it.name == "doTransform" &&
                it.parameterTypes.firstOrNull()?.name == "proto.sdui.components.core.Component"
        }
        if (targets.isEmpty()) throw NoSuchMethodException("ComponentTransformer.doTransform(Component, ...)")
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val component = param.args.getOrNull(0) ?: return
                if (isSponsoredComponent(component)) param.result = null
            }
        }
        targets.forEach { XposedBridge.hookMethod(it, hook) }
    } catch (t: Throwable) {
        PatcherLog.info(t) { "a092devs: LinkedIn SDUI transformer target not found" }
    }

    // Legacy feed: the model filter rebuilds the collection through copyWithNewElements.
    try {
        val template = classLoader.loadClass("com.linkedin.android.pegasus.gen.collection.CollectionTemplate")
        val targets = template.declaredMethods.filter {
            it.name == "copyWithNewElements" && it.parameterTypes.contentEquals(arrayOf(List::class.java))
        }
        if (targets.isEmpty()) throw NoSuchMethodException("CollectionTemplate.copyWithNewElements(List)")
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val updates = param.args.getOrNull(0) as? List<*> ?: return
                param.args[0] = filterSponsoredUpdates(updates)
            }
        }
        targets.forEach { XposedBridge.hookMethod(it, hook) }
    } catch (t: Throwable) {
        PatcherLog.info(t) { "a092devs: LinkedIn collection template target not found" }
    }
}

/** Sponsored conversation list items, the same two markers upstream's isSponsored checks. */
private fun isSponsoredConversation(conversation: Any?): Boolean {
    if (conversation == null) return false
    val contentMetadata = fieldValue(fieldValue(conversation, "entityData"), "contentMetadata")
    if (fieldValue(contentMetadata, "hasConversationAdContentValue") == true) return true
    val latestMessage = fieldValue(conversation, "latestMessage") ?: return false
    return runCatching {
        val messageItemKt = latestMessage.javaClass.classLoader
            .loadClass("com.linkedin.android.messenger.data.extensions.MessageItemKt")
        val isSpInMail = messageItemKt.getMethod("isSpInMail", latestMessage.javaClass)
        isSpInMail.invoke(null, latestMessage) == true
    }.getOrDefault(false)
}

/** Automatic read marks only come from the open chat screen, like upstream's stack check. */
private fun calledFromChatList(): Boolean =
    Throwable().stackTrace.any { it.className.startsWith("com.linkedin.android.messaging.messagelist.") }

/** A feed-full-update component that is an ad: sponsored tracking metadata or the payload marker. */
private fun isSponsoredComponent(component: Any): Boolean {
    return try {
        val viewTracking = if (callOrNull(component, "hasViewTracking") == true) {
            callOrNull(component, "getViewTracking")
        } else {
            null
        }
        callOrNull(viewTracking, "getViewName") == "feed-full-update" &&
            (hasSponsoredTracking(viewTracking) || payloadContains(component, SPONSORED_CONTENT_MARKER))
    } catch (t: Throwable) {
        false
    }
}

/** viewTracking.metadata[] contains a TrackingMetadata whose sponsoredData is set. */
private fun hasSponsoredTracking(viewTracking: Any?): Boolean {
    val metadata = callOrNull(viewTracking, "getMetadataList") as? List<*> ?: return false
    return metadata.any { item -> callOrNull(item, "hasSponsoredData") == true }
}

/** A feed Update is an ad when update.metadata.trackingData.sponsoredTracking is set. */
private fun filterSponsoredUpdates(updates: List<*>): List<*> = try {
    updates.filterNot { update ->
        val trackingData = fieldValue(fieldValue(update, "metadata"), "trackingData")
        fieldValue(trackingData, "sponsoredTracking") != null
    }
} catch (t: Throwable) {
    updates
}

private const val SPONSORED_CONTENT_MARKER = "urn:li:sponsoredContentV2:"

/** Scans the serialized protobuf for the marker, like upstream's ProtoScanner.containsAscii. */
private fun payloadContains(component: Any, marker: String): Boolean {
    val payload = callOrNull(component, "toByteArray") as? ByteArray ?: return false
    return containsAscii(payload, marker)
}

private val fieldCache = ConcurrentHashMap<Class<*>, Map<String, Field>>()
private val methodCache = ConcurrentHashMap<Class<*>, Map<String, Method>>()

/** Reads a field by name, searching superclasses like upstream's Reflect.get. */
private fun fieldValue(target: Any?, name: String): Any? {
    if (target == null) return null
    val fields = fieldCache.computeIfAbsent(target.javaClass) { cls ->
        buildMap {
            var current: Class<*>? = cls
            while (current != null && current != Any::class.java) {
                for (field in current.declaredFields) {
                    if (!containsKey(field.name)) {
                        runCatching { field.isAccessible = true }
                        put(field.name, field)
                    }
                }
                current = current.superclass
            }
        }
    }
    val field = fields[name] ?: return null
    return runCatching { field.get(target) }.getOrNull()
}

/** Invokes a public no-arg method; the call/string/is helpers of upstream's Proto. */
private fun callOrNull(target: Any?, name: String): Any? {
    if (target == null) return null
    val method = methodCache.computeIfAbsent(target.javaClass) { cls ->
        cls.methods.filter { it.parameterTypes.isEmpty() }.associateBy { it.name }
    }[name] ?: return null
    return runCatching { method.invoke(target) }.getOrNull()
}
