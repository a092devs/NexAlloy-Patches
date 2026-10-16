package io.github.nexalloy.a092devs.linkedin

// Kept in its own file so unit tests can load it without the patcher runtime, like LinkCleaner.

/** Byte-level ASCII search, the check upstream's ProtoScanner.containsAscii does on the payload. */
internal fun containsAscii(haystack: ByteArray, needle: String): Boolean {
    if (needle.isEmpty() || haystack.size < needle.length) return false
    outer@ for (start in 0..haystack.size - needle.length) {
        for (offset in needle.indices) {
            if (haystack[start + offset] != needle[offset].code.toByte()) continue@outer
        }
        return true
    }
    return false
}
