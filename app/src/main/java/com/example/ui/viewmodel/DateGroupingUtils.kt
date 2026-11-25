package com.example.ui.viewmodel

import com.example.domain.model.GalleryMedia
import com.example.domain.model.MediaItemRef
import com.example.domain.model.VaultMedia
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DateSection<T : MediaItemRef>(
    val header: String,
    val items: List<T>
)

object DateGroupingUtils {
    private val monthYearFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    }
    private val shortDateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    }
    private val fullDateTimeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    }
    private val headerDayDateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
    }
    private val searchDateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("MMMM yyyy MMM d yyyy EEEE EEE yyyy-MM-dd MM/dd/yyyy dd/MM/yyyy", Locale.getDefault())
    }

    fun formatSectionLabel(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        if (epochMs <= 0L) return "Recent"
        val calNow = Calendar.getInstance().apply { timeInMillis = nowMs }
        val calItem = Calendar.getInstance().apply { timeInMillis = epochMs }

        val nowDay = calNow.get(Calendar.DAY_OF_YEAR)
        val nowYear = calNow.get(Calendar.YEAR)
        val itemDay = calItem.get(Calendar.DAY_OF_YEAR)
        val itemYear = calItem.get(Calendar.YEAR)

        if (nowYear == itemYear && nowDay == itemDay) return "Today"

        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, -1)
        }
        if (yesterdayCal.get(Calendar.YEAR) == itemYear &&
            yesterdayCal.get(Calendar.DAY_OF_YEAR) == itemDay
        ) {
            return "Yesterday"
        }

        return monthYearFormat.get()!!.format(Date(epochMs))
    }

    private class FastLabelResolver(nowMs: Long = System.currentTimeMillis()) {
        private val cal = Calendar.getInstance()
        private val todayStartMs: Long
        private val todayEndMs: Long
        private val yesterdayStartMs: Long
        private val monthCache = HashMap<Int, String>(24)
        private val formatter = monthYearFormat.get()!!
        private val reusableDate = Date()

        init {
            cal.timeInMillis = nowMs
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            todayStartMs = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            todayEndMs = cal.timeInMillis
            cal.timeInMillis = todayStartMs
            cal.add(Calendar.DAY_OF_YEAR, -1)
            yesterdayStartMs = cal.timeInMillis
        }

        fun labelFor(epochMs: Long): String {
            if (epochMs <= 0L) return "Recent"
            if (epochMs >= todayStartMs && epochMs < todayEndMs) return "Today"
            if (epochMs >= yesterdayStartMs && epochMs < todayStartMs) return "Yesterday"

            cal.timeInMillis = epochMs
            val monthKey = cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH)
            return monthCache.getOrPut(monthKey) {
                reusableDate.time = epochMs
                formatter.format(reusableDate)
            }
        }
    }

    fun groupGalleryByDate(items: List<GalleryMedia>): List<DateSection<MediaItemRef.Gallery>> {
        if (items.isEmpty()) return emptyList()
        val resolver = FastLabelResolver()
        val grouped = LinkedHashMap<String, MutableList<MediaItemRef.Gallery>>()
        for (item in items) {
            val label = resolver.labelFor(item.effectiveDateMs)
            grouped.getOrPut(label) { mutableListOf() }.add(MediaItemRef.Gallery(item))
        }
        return grouped.map { (header, list) -> DateSection(header, list) }
    }

    fun groupVaultByDate(items: List<VaultMedia>): List<DateSection<MediaItemRef.Vault>> {
        if (items.isEmpty()) return emptyList()
        val resolver = FastLabelResolver()
        val grouped = LinkedHashMap<String, MutableList<MediaItemRef.Vault>>()
        for (item in items) {
            val ts = if (item.dateTakenMs > 0L) item.dateTakenMs else item.addedToVaultMs
            val label = resolver.labelFor(ts)
            grouped.getOrPut(label) { mutableListOf() }.add(MediaItemRef.Vault(item))
        }
        return grouped.map { (header, list) -> DateSection(header, list) }
    }

    class FastDateMatcher(nowMs: Long = System.currentTimeMillis()) {
        private val cal = Calendar.getInstance()
        private val todayStartMs: Long
        private val todayEndMs: Long
        private val yesterdayStartMs: Long
        private val dayCache = HashMap<Int, String>(128)
        private val formatter = searchDateFormat.get()!!
        private val reusableDate = Date()
        private val locale = Locale.getDefault()

        init {
            cal.timeInMillis = nowMs
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            todayStartMs = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            todayEndMs = cal.timeInMillis
            cal.timeInMillis = todayStartMs
            cal.add(Calendar.DAY_OF_YEAR, -1)
            yesterdayStartMs = cal.timeInMillis
        }

        fun matches(epochMs: Long, queryLower: String): Boolean {
            if (epochMs <= 0L || queryLower.isBlank()) return false
            if (epochMs >= todayStartMs && epochMs < todayEndMs && "today".contains(queryLower)) {
                return true
            }
            if (epochMs >= yesterdayStartMs && epochMs < todayStartMs && "yesterday".contains(queryLower)) {
                return true
            }
            cal.timeInMillis = epochMs
            val dayKey = cal.get(Calendar.YEAR) * 400 + cal.get(Calendar.DAY_OF_YEAR)
            val searchable = dayCache.getOrPut(dayKey) {
                reusableDate.time = epochMs
                formatter.format(reusableDate).lowercase(locale)
            }
            return searchable.contains(queryLower)
        }
    }

    fun matchesDateQuery(
        epochMs: Long,
        queryLower: String,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        return FastDateMatcher(nowMs).matches(epochMs, queryLower)
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatFullDateTime(epochMs: Long): String {
        if (epochMs <= 0L) return "Unknown date"
        return fullDateTimeFormat.get()!!.format(Date(epochMs))
    }

    fun formatShortDate(epochMs: Long): String {
        if (epochMs <= 0L) return "Recent"
        return shortDateFormat.get()!!.format(Date(epochMs))
    }

    fun formatHeaderDayDate(epochMs: Long = System.currentTimeMillis()): String {
        return headerDayDateFormat.get()!!.format(Date(epochMs))
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.2f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun remainingTrashDays(dateExpiresSec: Long?): Int {
        if (dateExpiresSec == null || dateExpiresSec <= 0L) return 30
        val nowSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        val diffDays = TimeUnit.SECONDS.toDays((dateExpiresSec - nowSec).coerceAtLeast(0L))
        return diffDays.toInt().coerceIn(0, 30)
    }
}
