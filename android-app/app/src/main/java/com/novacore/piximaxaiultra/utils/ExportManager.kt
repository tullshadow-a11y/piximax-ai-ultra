package com.novacore.piximaxaiultra.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import com.startapp.sdk.adsbase.StartAppAd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    private const val PREFS_NAME = "piximax_prefs"
    private const val KEY_EXPORT_DATE = "piximax_export_date"
    private const val KEY_EXPORT_COUNT = "piximax_export_count"
    private const val MAX_FREE_DAILY_EXPORTS = 3

    private val usageUpdateTrigger = mutableStateOf(0)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getDailyExportsUsed(context: Context): Int {
        val prefs = getPrefs(context)
        val savedDate = prefs.getString(KEY_EXPORT_DATE, "")
        val today = getTodayDateString()
        return if (savedDate != today) {
            prefs.edit()
                .putString(KEY_EXPORT_DATE, today)
                .putInt(KEY_EXPORT_COUNT, 0)
                .apply()
            0
        } else {
            prefs.getInt(KEY_EXPORT_COUNT, 0)
        }
    }

    fun canExport(context: Context, isProUser: Boolean): Boolean {
        if (isProUser) return true
        val used = getDailyExportsUsed(context)
        return used < MAX_FREE_DAILY_EXPORTS
    }

    fun recordExportAndShowAd(context: Context, isProUser: Boolean) {
        if (isProUser) return

        val prefs = getPrefs(context)
        val currentCount = getDailyExportsUsed(context)
        prefs.edit().putInt(KEY_EXPORT_COUNT, currentCount + 1).apply()
        usageUpdateTrigger.value += 1

        try {
            if (context is android.app.Activity) {
                StartAppAd.showAd(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRemainingExportsText(context: Context, isProUser: Boolean): String {
        @Suppress("UNUSED_VARIABLE")
        val dummy = usageUpdateTrigger.value // Trigger Compose recomposition when usage changes
        if (isProUser) return "غير محدود 👑"
        val used = getDailyExportsUsed(context)
        val remaining = (MAX_FREE_DAILY_EXPORTS - used).coerceAtLeast(0)
        return "متبقي: $remaining/3"
    }
}
