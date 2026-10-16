package io.github.nexalloy.a092devs.mixplorer

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScopedStorageTest {

    private val selfUid = 10123
    private val otherUid = 10500
    private val withData = arrayOf<Any?>("document_id", "_data", "path")
    private val withoutData = arrayOf<Any?>("document_id", "path")

    @Test
    fun `masks external callers on Android 11 and later`() {
        assertTrue(ScopedStorage.masksDataColumn(30, otherUid, selfUid, withData))
        assertTrue(ScopedStorage.masksDataColumn(34, otherUid, selfUid, withData))
    }

    @Test
    fun `keeps everything on older Android`() {
        assertFalse(ScopedStorage.masksDataColumn(29, otherUid, selfUid, withData))
    }

    @Test
    fun `keeps same-app queries`() {
        assertFalse(ScopedStorage.masksDataColumn(30, selfUid, selfUid, withData))
    }

    @Test
    fun `keeps projections without the data column`() {
        assertFalse(ScopedStorage.masksDataColumn(30, otherUid, selfUid, withoutData))
    }

    @Test
    fun `masks when every column was queried`() {
        assertTrue(ScopedStorage.masksDataColumn(30, otherUid, selfUid, null))
    }

    @Test
    fun `recognizes the data column name`() {
        assertTrue(ScopedStorage.isDataColumn("_data"))
        assertFalse(ScopedStorage.isDataColumn("path"))
        assertFalse(ScopedStorage.isDataColumn(null))
    }

    @Test
    fun `the scoped storage gate matches upstream compatibility`() {
        assertTrue(ScopedStorage.MIN_SDK == 30)
    }
}
