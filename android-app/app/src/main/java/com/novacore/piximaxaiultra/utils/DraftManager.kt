package com.novacore.piximaxaiultra.utils

import android.content.Context
import android.net.Uri

data class EditSessionDraft(
    val imageUri: Uri?,
    val activeTab: Int
)

object DraftManager {
    private const val PREFS_NAME = "piximax_draft_prefs"
    private const val KEY_DRAFT_URI = "draft_image_uri"
    private const val KEY_DRAFT_TAB = "draft_active_tab"

    fun saveDraft(context: Context, imageUri: Uri?, activeTab: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (imageUri != null) {
                putString(KEY_DRAFT_URI, imageUri.toString())
            } else {
                remove(KEY_DRAFT_URI)
            }
            putInt(KEY_DRAFT_TAB, activeTab)
            apply()
        }
    }

    fun getDraft(context: Context): EditSessionDraft? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_DRAFT_URI, null) ?: return null
        val tab = prefs.getInt(KEY_DRAFT_TAB, 0)
        return try {
            EditSessionDraft(Uri.parse(uriString), tab)
        } catch (e: Throwable) {
            null
        }
    }

    fun clearDraft(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
