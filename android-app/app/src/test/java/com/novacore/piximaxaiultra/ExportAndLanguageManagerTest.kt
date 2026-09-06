package com.novacore.piximaxaiultra

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.novacore.piximaxaiultra.utils.ExportManager
import com.novacore.piximaxaiultra.utils.LanguageManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExportAndLanguageManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("piximax_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun testExportManagerMaxLimitIs16() {
        assertEquals(16, ExportManager.MAX_FREE_DAILY_EXPORTS)

        // Initially 0 used
        assertEquals(0, ExportManager.getDailyExportsUsed(context))
        assertTrue(ExportManager.canExport(context, isProUser = false))

        // Record 16 exports
        for (i in 1..16) {
            assertTrue(ExportManager.canExport(context, isProUser = false))
            ExportManager.recordExportAndShowAd(context, isProUser = false)
        }

        // Now 16 used, should not be able to export
        assertEquals(16, ExportManager.getDailyExportsUsed(context))
        assertFalse(ExportManager.canExport(context, isProUser = false))

        // Pro user can always export
        assertTrue(ExportManager.canExport(context, isProUser = true))
    }

    @Test
    fun testRemainingExportsTextFormatting() {
        var text = ExportManager.getRemainingExportsText(context, isProUser = false, lang = "ar")
        assertTrue(text.contains("16/16"))
        assertTrue(text.contains("مجاني"))

        text = ExportManager.getRemainingExportsText(context, isProUser = false, lang = "en")
        assertTrue(text.contains("16/16"))
        assertTrue(text.contains("Free"))

        // Pro user
        text = ExportManager.getRemainingExportsText(context, isProUser = true, lang = "en")
        assertTrue(text.contains("Unlimited"))
    }

    @Test
    fun testLanguageManagerSwitching() {
        LanguageManager.init(context)
        assertEquals("en", LanguageManager.currentLang.value)

        LanguageManager.setLanguage(context, "ar")
        assertEquals("ar", LanguageManager.currentLang.value)
        assertEquals("PixiMax Pro", LanguageManager.getString("app_title"))
        assertEquals("🔑 دخول", LanguageManager.getString("login_btn"))

        LanguageManager.setLanguage(context, "en")
        assertEquals("en", LanguageManager.currentLang.value)
        assertEquals("🔑 Login", LanguageManager.getString("login_btn"))

        LanguageManager.setLanguage(context, "fr")
        assertEquals("fr", LanguageManager.currentLang.value)
        assertEquals("🔑 Connexion", LanguageManager.getString("login_btn"))

        LanguageManager.setLanguage(context, "es")
        assertEquals("es", LanguageManager.currentLang.value)
        assertEquals("🔑 Entrar", LanguageManager.getString("login_btn"))

        LanguageManager.setLanguage(context, "de")
        assertEquals("de", LanguageManager.currentLang.value)
        assertEquals("🔑 Anmelden", LanguageManager.getString("login_btn"))
    }
}
