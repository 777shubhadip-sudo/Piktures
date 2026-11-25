package com.example

import com.example.ui.viewmodel.DateGroupingUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateGroupingUtilsTest {

    @Test
    fun formatDuration_formatsMinutesAndHours() {
        assertEquals("0:09", DateGroupingUtils.formatDuration(9_000L))
        assertEquals("2:05", DateGroupingUtils.formatDuration(125_000L))
        assertEquals("1:01:05", DateGroupingUtils.formatDuration(3_665_000L))
    }

    @Test
    fun formatFileSize_formatsBytesKbMb() {
        assertEquals("512 B", DateGroupingUtils.formatFileSize(512L))
        assertEquals("2.0 KB", DateGroupingUtils.formatFileSize(2048L))
        assertEquals("1.50 MB", DateGroupingUtils.formatFileSize(1_572_864L))
    }

    @Test
    fun formatSectionLabel_returnsTodayAndYesterday() {
        val now = 1_760_000_000_000L
        assertEquals("Today", DateGroupingUtils.formatSectionLabel(now, now))
        val yesterday = now - 24 * 60 * 60 * 1000L
        assertEquals("Yesterday", DateGroupingUtils.formatSectionLabel(yesterday, now))
        assertTrue(DateGroupingUtils.matchesDateQuery(now, "today", now))
    }
}
