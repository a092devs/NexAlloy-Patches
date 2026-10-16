package io.github.nexalloy.a092devs.gboard

import io.github.nexalloy.patch

// Ported from jasonwu1994/Gboard-patches (GPL-3.0): each patch mirrors the matching
// *Policy.java under extensions/extension/src/main/java/dev/jason/gboardpatches/extension/,
// installed here as flag overrides on the shared getter hook (see GboardFlagHook.kt).
//
// All of these features already exist in Gboard behind flags the server rollout leaves off; the
// patches only substitute the stock value, like the upstream flag-family patches do.

val GboardCursorTrackpad = patch(
    name = "Cursor trackpad",
    description = "Enables free-cursor mode: slide on the spacebar to move the text cursor.",
) {
    gboardFlagOverrides(
        "free_cursor" to FlagOverride.promote(),
        "free_cursor_lock_mode" to FlagOverride.promote(),
    )
}

val GboardQuickInsert = patch(
    name = "Quick insert",
    description = "Enables Gboard's super-insert (quick insert) suggestions row.",
) {
    gboardFlagOverrides(
        "super_insert" to FlagOverride.forceBoolean(),
        "super_insert_vk" to FlagOverride.forceBoolean(),
    )
}

val GboardEmojiSize = patch(
    name = "Emoji size setting",
    description = "Enables the emoji size slider in Gboard's emoji panel.",
) {
    gboardFlagOverrides("emoji_scale_supported" to FlagOverride.promote())
}

val GboardAccessPointsMenu = patch(
    name = "Access points menu redesign",
    description = "Uses the redesigned toolbar access-points menu.",
) {
    gboardFlagOverrides("enable_access_points_menu_redesign" to FlagOverride.forceBoolean())
}

val GboardBluetoothMicrophone = patch(
    name = "Bluetooth microphone setting",
    description = "Shows the setting that lets voice typing use a Bluetooth microphone.",
) {
    gboardFlagOverrides("enable_use_bluetooth_setting" to FlagOverride.forceBoolean())
}

val GboardCloseProactiveSuggestions = patch(
    name = "Close proactive suggestions",
    description = "Adds the access point that closes the proactive suggestions row.",
) {
    gboardFlagOverrides("enable_close_proactive_suggestions_access_point" to FlagOverride.forceBoolean())
}

val GboardAccessPointCount = patch(
    name = "Access point count",
    description = "Fixes the toolbar access-point count at 6 (upstream's default; range 3-8).",
    use = false,
) {
    gboardFlagOverrides(
        "config_max_access_points" to FlagOverride.forceLong(6L),
        "config_default_access_points_num_on_bar" to FlagOverride.forceLong(6L),
    )
}

val GboardClipboardCharacterLimit = patch(
    name = "Clipboard character limit",
    description = "Raises the clipboard item text limit to 20000 characters (upstream's default).",
) {
    gboardFlagOverrides("text_clip_item_char_limit" to FlagOverride.forceLong(20_000L))
}

val GboardFlowMode = patch(
    name = "Flow mode",
    description = "Enables the llm_pc flow-mode keys for every language.",
) {
    gboardFlagOverrides(
        "enable_llm_pc" to FlagOverride.forceBoolean(),
        "enable_llm_pc_flow_mode" to FlagOverride.forceBoolean(),
        "llm_pc_supported_language_tags" to FlagOverride.forceString("*"),
    )
}

val GboardAdvancedVoice = patch(
    name = "Advanced voice typing",
    description = "Enables the advanced (NGA) dictation flags. Off by default: the flags can start " +
        "on-device model downloads.",
    use = false,
) {
    gboardFlagOverrides(
        "enable_nga" to FlagOverride.forceBoolean(),
        "enable_advanced_features_in_consolidated_sd_stack" to FlagOverride.forceBoolean(),
        "enable_dictation_feature_split_install" to FlagOverride.forceBoolean(),
        "handle_fallback_inside_sd_stack" to FlagOverride.forceBoolean(),
        "enable_sticky_mic_background" to FlagOverride.forceBoolean(),
        "enable_soda_longform_experiment" to FlagOverride.forceBoolean(),
    )
}

val GboardGrammarChecker = patch(
    name = "Grammar checker",
    description = "Enables Gboard's on-device grammar checker.",
) {
    gboardFlagOverrides("enable_grammar_checker" to FlagOverride.promote())
}

val GboardInlineSuggestions = patch(
    name = "Inline suggestions",
    description = "Enables client-side inline suggestions.",
) {
    gboardFlagOverrides("enable_inline_suggestions_on_client_side" to FlagOverride.promote())
}

val GboardKeyShapeSelection = patch(
    name = "Key shape selection",
    description = "Enables the extra key shapes in the theme editor (more_pill_keys).",
) {
    gboardFlagOverrides("more_pill_keys" to FlagOverride.promote())
}

val GboardAiWritingTools = patch(
    name = "AI Writing Tools",
    description = "Enables Gboard's Gemini-powered AI writing assistant, Proofread, and style rephrasing across apps.",
) {
    gboardFlagOverrides(
        "config_proofread" to FlagOverride.promote(),
        "writing_tools" to FlagOverride.forceBoolean(),
        "enable_writing_tools_v2" to FlagOverride.forceBoolean(),
        "writing_tools_v2" to FlagOverride.forceBoolean(),
        "enable_writing_tools_cooperative_mode" to FlagOverride.forceBoolean(),
        "writing_helper" to FlagOverride.forceBoolean(),
        "writing_helper_on_selected_text" to FlagOverride.forceBoolean(),
        "writing_helper_enable_text_stylization_internal" to FlagOverride.forceBoolean(),
        "writing_helper_supported_language_tags" to FlagOverride.forceString("*"),
        "llm_pc_supported_language_tags" to FlagOverride.forceString("*"),
        "enable_writing_tools_v2_on_toolbar" to FlagOverride.forceBoolean(),
        "enable_writing_tools_suggest_style" to FlagOverride.forceBoolean(),
        "writing_tools_backend_type" to FlagOverride.forceLong(1L),
        "enable_device_intelligence" to FlagOverride.forceBoolean(),
        "writing_helper_chip_in_spellchecker" to FlagOverride.forceBoolean(),
    )
}

val GboardRoundedKeycaps = patch(
    name = "Rounded keycaps (Belka UI)",
    description = "Enables Material 3 Belka rounded pill keycaps, circular layout styling, and persistent preference.",
) {
    gboardFlagOverrides(
        "belka_rounded_keyboard" to FlagOverride.promote(),
        "belka_ui" to FlagOverride.promote(),
        "belka_background" to FlagOverride.promote(),
        "more_pill_keys" to FlagOverride.promote(),
        "enable_rounded_keys" to FlagOverride.promote(),
        "enable_rounded_key_by_default" to FlagOverride.promote(),
    )
    enableGboardRoundedKeysPreference()
}

val GboardScanTextOcr = patch(
    name = "Scan text (Camera OCR)",
    description = "Unlocks the Camera OCR scan text viewfinder in the Gboard toolbar to extract physical text.",
) {
    gboardFlagOverrides(
        "enable_ocr" to FlagOverride.forceBoolean(),
        "config_ocr" to FlagOverride.forceBoolean(),
        "ocr_password_allowlist" to FlagOverride.forceBoolean(),
        "enabled_ocr_language_tags" to FlagOverride.forceString("*"),
    )
}

val GboardScreenshotChips = patch(
    name = "Screenshot suggestion chips",
    description = "Shows floating suggestion chips for recently captured screenshots and clipboard images.",
) {
    gboardFlagOverrides(
        "enable_clipboard_image_suggestion" to FlagOverride.forceBoolean(),
        "enable_screenshot_notification_flow" to FlagOverride.forceBoolean(),
        "enable_screenshot_chip" to FlagOverride.forceBoolean(),
        "enable_clipboard_recent_screenshot" to FlagOverride.forceBoolean(),
        "clipboard_screenshot_enabled_at_least_once" to FlagOverride.forceBoolean(),
    )
}

val GboardEmojiPaletteV2 = patch(
    name = "Emoji Palette V2",
    description = "Enables the redesigned emoji drawer layout with dedicated frequent/recent emoji tabs.",
) {
    gboardFlagOverrides(
        "emoji_palette_v2" to FlagOverride.promote(),
        "enable_frequent_emoji_recent_tab" to FlagOverride.promote(),
        "enable_emoji_frequent_recent_switch_option" to FlagOverride.promote(),
    )
}

val GboardSmartCompose = patch(
    name = "Smart Compose autocompletion",
    description = "Enables in-line predictive text completion and ghost text across all input fields.",
) {
    gboardFlagOverrides(
        "enable_smart_compose" to FlagOverride.promote(),
        "enable_inline_suggestions_on_all_fields" to FlagOverride.promote(),
        "enable_inline_completion" to FlagOverride.promote(),
        "enable_delight5_smart_compose" to FlagOverride.promote(),
        "smart_compose_status" to FlagOverride.forceLong(1L),
        "enable_inline_suggestions_on_client_side" to FlagOverride.promote(),
    )
}

val GboardSpellCheckerChips = patch(
    name = "Spell checker suggestion chips",
    description = "Forces modern inline spell checker suggestion chips and grammar highlight strip on Android 12+.",
) {
    gboardFlagOverrides(
        "spellchecker_enable_chips_ui" to FlagOverride.promote(),
        "spellchecker_enable_chips_ui_web_view" to FlagOverride.promote(),
        "enable_spell_checker_extension" to FlagOverride.promote(),
        "writing_helper_chip_in_spellchecker" to FlagOverride.promote(),
        "enable_spell_checker_highlight_on_web_input" to FlagOverride.promote(),
        "enable_spell_checker_training_cache" to FlagOverride.promote(),
    )
}

val GboardPatches = arrayOf(
    GboardCursorTrackpad,
    GboardQuickInsert,
    GboardEmojiSize,
    GboardAccessPointsMenu,
    GboardBluetoothMicrophone,
    GboardCloseProactiveSuggestions,
    GboardAccessPointCount,
    GboardClipboardCharacterLimit,
    GboardFlowMode,
    GboardAdvancedVoice,
    GboardGrammarChecker,
    GboardInlineSuggestions,
    GboardKeyShapeSelection,
    GboardAiWritingTools,
    GboardRoundedKeycaps,
    GboardScanTextOcr,
    GboardScreenshotChips,
    GboardEmojiPaletteV2,
    GboardSmartCompose,
    GboardSpellCheckerChips,
)

