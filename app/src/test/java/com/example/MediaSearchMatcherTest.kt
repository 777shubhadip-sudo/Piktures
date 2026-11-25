package com.example

import com.example.domain.model.MediaType
import com.example.ui.viewmodel.DateGroupingUtils
import com.example.ui.viewmodel.MediaSearchMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class MediaSearchMatcherTest {

    private fun sampleNowMs(): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 21, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun matches(
        displayName: String,
        bucketName: String,
        relativePath: String,
        mimeType: String,
        mediaType: MediaType,
        isFavorite: Boolean,
        dateMs: Long,
        query: String,
        nowMs: Long
    ): Boolean {
        val tokens = query.trim().lowercase(Locale.getDefault()).split(Regex("\\s+")).filter { it.isNotEmpty() }
        return MediaSearchMatcher.matchesItem(
            displayName = displayName,
            bucketName = bucketName,
            relativePath = relativePath,
            mimeType = mimeType,
            mediaType = mediaType,
            isFavorite = isFavorite,
            effectiveDateMs = dateMs,
            tokens = tokens,
            dateMatcher = DateGroupingUtils.FastDateMatcher(nowMs)
        )
    }

    @Test
    fun matchesPhotoVideoAndFavoriteSynonyms() {
        val now = sampleNowMs()
        assertTrue(matches("IMG_1.jpeg", "Camera", "DCIM/Camera/", "image/jpeg", MediaType.IMAGE, true, now, "photos", now))
        assertTrue(matches("IMG_1.jpeg", "Camera", "DCIM/Camera/", "image/jpeg", MediaType.IMAGE, true, now, "photo", now))
        assertTrue(matches("IMG_1.jpeg", "Camera", "DCIM/Camera/", "image/jpeg", MediaType.IMAGE, true, now, "favorites", now))
        assertFalse(matches("IMG_1.jpeg", "Camera", "DCIM/Camera/", "image/jpeg", MediaType.IMAGE, true, now, "videos", now))
        assertTrue(matches("VID_1.mp4", "Concerts", "Movies/Concerts/", "video/mp4", MediaType.VIDEO, false, now, "videos", now))
        assertFalse(matches("VID_1.mp4", "Concerts", "Movies/Concerts/", "video/mp4", MediaType.VIDEO, false, now, "photos", now))
    }

    @Test
    fun matchesMultiWordExtensionPathAndDates() {
        val now = sampleNowMs()
        val yesterday = now - 24L * 60L * 60L * 1000L
        val oct5 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 5, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertTrue(matches("IMG_sunset.jpeg", "Camera", "DCIM/Camera/", "image/jpeg", MediaType.IMAGE, false, now, "camera photo", now))
        assertTrue(matches("IMG_sunset.jpeg", "Camera", "DCIM/Camera/", "image/jpeg", MediaType.IMAGE, false, now, "jpg today", now))
        assertTrue(matches("VID_clip.mp4", "Concerts", "Movies/Concerts/", "video/mp4", MediaType.VIDEO, false, yesterday, "yesterday mp4", now))
        assertTrue(matches("Screenshot_1.png", "Screenshots", "Pictures/Screenshots/", "image/png", MediaType.IMAGE, false, oct5, "october pictures", now))
        assertTrue(matches("Screenshot_1.png", "Screenshots", "Pictures/Screenshots/", "image/png", MediaType.IMAGE, false, oct5, "2026-10-05", now))
        assertFalse(matches("Screenshot_1.png", "Screenshots", "Pictures/Screenshots/", "image/png", MediaType.IMAGE, false, oct5, "camera video", now))
    }
}
