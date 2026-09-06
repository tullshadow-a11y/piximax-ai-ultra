package com.novacore.piximaxaiultra.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

data class LanguageOption(
    val code: String,
    val name: String
)

object LanguageManager {

    private const val PREFS_NAME = "piximax_prefs"
    private const val KEY_LANG = "piximax_lang"

    val supportedLanguages = listOf(
        LanguageOption("en", "English"),
        LanguageOption("ar", "العربية"),
        LanguageOption("fr", "Français"),
        LanguageOption("es", "Español"),
        LanguageOption("de", "Deutsch")
    )

    private val _currentLang = mutableStateOf("en")
    val currentLang: State<String> = _currentLang

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(KEY_LANG, "en") ?: "en"
        _currentLang.value = savedLang
    }

    fun setLanguage(context: Context, langCode: String) {
        if (supportedLanguages.none { it.code == langCode }) return
        _currentLang.value = langCode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, langCode).apply()
    }

    fun getString(key: String, lang: String = _currentLang.value): String {
        return stringsMap[key]?.get(lang)
            ?: stringsMap[key]?.get("en")
            ?: stringsMap[key]?.get("ar")
            ?: key
    }

    private val stringsMap: Map<String, Map<String, String>> = mapOf(
        "app_title" to mapOf(
            "ar" to "PixiMax Pro",
            "en" to "PixiMax Pro",
            "fr" to "PixiMax Pro",
            "es" to "PixiMax Pro",
            "de" to "PixiMax Pro"
        ),
        "app_subtitle" to mapOf(
            "ar" to "NovaCore Software",
            "en" to "NovaCore Software",
            "fr" to "NovaCore Software",
            "es" to "NovaCore Software",
            "de" to "NovaCore Software"
        ),
        "login_btn" to mapOf(
            "ar" to "🔑 دخول",
            "en" to "🔑 Login",
            "fr" to "🔑 Connexion",
            "es" to "🔑 Entrar",
            "de" to "🔑 Anmelden"
        ),
        "pro_badge" to mapOf(
            "ar" to "PRO 👑",
            "en" to "PRO 👑",
            "fr" to "PRO 👑",
            "es" to "PRO 👑",
            "de" to "PRO 👑"
        ),
        "upgrade_4k" to mapOf(
            "ar" to "⚡ PRO 4K",
            "en" to "⚡ PRO 4K",
            "fr" to "⚡ PRO 4K",
            "es" to "⚡ PRO 4K",
            "de" to "⚡ PRO 4K"
        ),
        "tab_object_remover" to mapOf(
            "ar" to "🎯 إزالة العناصر",
            "en" to "🎯 Object Remover",
            "fr" to "🎯 Effacer Objets",
            "es" to "🎯 Eliminar Objetos",
            "de" to "🎯 Objekte Entfernen"
        ),
        "tab_bg_removal" to mapOf(
            "ar" to "✂️ إزالة الخلفية",
            "en" to "✂️ BG Removal",
            "fr" to "✂️ Supprimer Fond",
            "es" to "✂️ Quitar Fondo",
            "de" to "✂️ Hintergr. Entfernen"
        ),
        "tab_face_retouch" to mapOf(
            "ar" to "✨ رتوش الوجه",
            "en" to "✨ Face Retouch",
            "fr" to "✨ Retouche Visage",
            "es" to "✨ Retoque Facial",
            "de" to "✨ Retusche Gesicht"
        ),
        "tab_watermark" to mapOf(
            "ar" to "🧼 مسح العلامة",
            "en" to "🧼 Watermark Eraser",
            "fr" to "🧼 Effacer Filigrane",
            "es" to "🧼 Borrar Marca",
            "de" to "🧼 Wasserzeichen"
        ),
        "tab_enhance" to mapOf(
            "ar" to "🔍 رفع الجودة",
            "en" to "🔍 Quality Enhancer",
            "fr" to "🔍 Améliorer Qualité",
            "es" to "🔍 Mejorar Calidad",
            "de" to "🔍 Qualität Erhöhen"
        ),
        // Object Remover Screen
        "obj_upload_prompt" to mapOf(
            "ar" to "🎯 اضغط هنا لاختيار صورة لإزالة العناصر بالأصبع (Brush)",
            "en" to "🎯 Tap here to select image to remove objects with brush",
            "fr" to "🎯 Appuyez ici pour sélectionner une image à effacer avec le pinceau",
            "es" to "🎯 Toque aquí para seleccionar una imagen y borrar con el pincel",
            "de" to "🎯 Hier tippen, um ein Bild auszuwählen und mit dem Pinsel zu entfernen"
        ),
        "no_image_selected" to mapOf(
            "ar" to "لم يتم اختيار صورة بعد",
            "en" to "No image selected yet",
            "fr" to "Aucune image sélectionnée",
            "es" to "Aún no se ha seleccionado ninguna imagen",
            "de" to "Noch kein Bild ausgewählt"
        ),
        "obj_draw_instruction" to mapOf(
            "ar" to "ارسم بأصبعك فوق العنصر المراد مسحه بالكامل",
            "en" to "Draw with your finger over the object to erase completely",
            "fr" to "Dessinez avec votre doigt sur l'objet à effacer complètement",
            "es" to "Dibuje con su dedo sobre el objeto para borrarlo por completo",
            "de" to "Zeichnen Sie mit dem Finger über das Objekt, um es vollständig zu löschen"
        ),
        "brush_size" to mapOf(
            "ar" to "حجم الفرشاة (Brush Size)",
            "en" to "Brush Size",
            "fr" to "Taille du Pinceau",
            "es" to "Tamaño del Pincel",
            "de" to "Pinselgröße"
        ),
        "clear_selection" to mapOf(
            "ar" to "مسح التحديد 🗑️",
            "en" to "Clear Selection 🗑️",
            "fr" to "Effacer la Sélection 🗑️",
            "es" to "Borrar Selección 🗑️",
            "de" to "Auswahl Löschen 🗑️"
        ),
        "btn_erase_object" to mapOf(
            "ar" to "✨ إزالة العنصر المحدد بالأصبع",
            "en" to "✨ Erase Selected Object",
            "fr" to "✨ Effacer l'objet sélectionné",
            "es" to "✨ Borrar Objeto Seleccionado",
            "de" to "✨ Ausgewähltes Objekt Entfernen"
        ),
        "btn_save_clean_image" to mapOf(
            "ar" to "💾 حفظ وتصدير الصورة النظيفة",
            "en" to "💾 Save & Export Clean Image",
            "fr" to "💾 Enregistrer l'image propre",
            "es" to "💾 Guardar Imagen Limpia",
            "de" to "💾 Sauberes Bild Speichern"
        ),
        // BG Removal Screen
        "bg_upload_prompt" to mapOf(
            "ar" to "✂️ اضغط هنا لاختيار صورة لتفريغ الخلفية (PNG)",
            "en" to "✂️ Tap here to select image to remove background (PNG)",
            "fr" to "✂️ Appuyez ici pour sélectionner une image pour supprimer le fond (PNG)",
            "es" to "✂️ Toque aquí para seleccionar una imagen y quitar el fondo (PNG)",
            "de" to "✂️ Hier tippen, um ein Bild zur Hintergrundentfernung auszuwählen (PNG)"
        ),
        "bg_dimensions" to mapOf(
            "ar" to "أبعاد الصورة الشفافة:",
            "en" to "Transparent Image Dimensions:",
            "fr" to "Dimensions de l'image transparente:",
            "es" to "Dimensiones de la imagen transparente:",
            "de" to "Abmessungen des transparenten Bildes:"
        ),
        "tolerance" to mapOf(
            "ar" to "حساسية التفريغ (Tolerance)",
            "en" to "Tolerance Sensitivity",
            "fr" to "Sensibilité de tolérance",
            "es" to "Sensibilidad de tolerancia",
            "de" to "Toleranzempfindlichkeit"
        ),
        "btn_export_transparent" to mapOf(
            "ar" to "💾 تصدير PNG شفافة بدقة فائقة",
            "en" to "💾 Export Transparent Ultra HD PNG",
            "fr" to "💾 Exporter PNG Transparent Ultra HD",
            "es" to "💾 Exportar PNG Transparente Ultra HD",
            "de" to "💾 Transparente Ultra-HD-PNG Exportieren"
        ),
        // Face Retouch Screen
        "face_upload_prompt" to mapOf(
            "ar" to "✨ اضغط هنا لاختيار صورة لتنعيم وتجميل البشرة",
            "en" to "✨ Tap here to select image for skin smoothing & retouching",
            "fr" to "✨ Appuyez ici pour sélectionner une image pour la retouche de peau",
            "es" to "✨ Toque aquí para seleccionar una imagen para suavizar la piel",
            "de" to "✨ Hier tippen, um ein Bild zur Hautglättung auszuwählen"
        ),
        "image_dimensions" to mapOf(
            "ar" to "أبعاد الصورة:",
            "en" to "Image Dimensions:",
            "fr" to "Dimensions de l'image:",
            "es" to "Dimensiones de la imagen:",
            "de" to "Bildabmessungen:"
        ),
        "skin_smooth" to mapOf(
            "ar" to "تنعيم وتصفية البشرة (Skin Smooth)",
            "en" to "Skin Smoothing",
            "fr" to "Lissage de la peau",
            "es" to "Suavizado de piel",
            "de" to "Hautglättung"
        ),
        "clarity" to mapOf(
            "ar" to "إبراز تفاصيل العيون والشعر (Clarity)",
            "en" to "Eye & Hair Clarity",
            "fr" to "Clarté des yeux et cheveux",
            "es" to "Claridad de ojos y cabello",
            "de" to "Klarheit von Augen & Haaren"
        ),
        "btn_export_retouched" to mapOf(
            "ar" to "💾 تصدير الوجه المشرق (Retouched HD)",
            "en" to "💾 Export Retouched Face HD",
            "fr" to "💾 Exporter Visage Retouché HD",
            "es" to "💾 Exportar Rostro Retocado HD",
            "de" to "💾 Retuschiertes Gesicht HD Exportieren"
        ),
        // Watermark Screen
        "watermark_upload_prompt" to mapOf(
            "ar" to "🧼 اضغط هنا لاختيار صورة لمسح العلامة المائية",
            "en" to "🧼 Tap here to select image to erase watermark",
            "fr" to "🧼 Appuyez ici pour sélectionner une image à effacer le filigrane",
            "es" to "🧼 Toque aquí para seleccionar una imagen y borrar la marca de agua",
            "de" to "🧼 Hier tippen, um ein Bild auszuwählen und das Wasserzeichen zu löschen"
        ),
        "watermark_instruction" to mapOf(
            "ar" to "حدد المربع القابل للسحب فوق العلامة المائية تماماً",
            "en" to "Position the draggable box right over the watermark",
            "fr" to "Placez le rectangle glissant directement sur le filigrane",
            "es" to "Coloque el recuadro arrastrable justo sobre la marca de agua",
            "de" to "Platzieren Sie das ziehbare Feld direkt über dem Wasserzeichen"
        ),
        "box_width" to mapOf(
            "ar" to "عرض صندوق التحديد (Width)",
            "en" to "Selection Box Width",
            "fr" to "Largeur de la boîte",
            "es" to "Ancho de caja",
            "de" to "Boxbreite"
        ),
        "box_height" to mapOf(
            "ar" to "ارتفاع صندوق التحديد (Height)",
            "en" to "Selection Box Height",
            "fr" to "Hauteur de la boîte",
            "es" to "Alto de caja",
            "de" to "Boxhöhe"
        ),
        "btn_apply_watermark_eraser" to mapOf(
            "ar" to "🧼 تطبيق المسح الذكي الحقيقي",
            "en" to "🧼 Apply Smart Eraser",
            "fr" to "🧼 Appliquer l'effaceur intelligent",
            "es" to "🧼 Aplicar Borrador Inteligente",
            "de" to "🧼 Intelligenten Radierer Anwenden"
        ),
        "btn_export_cleaned" to mapOf(
            "ar" to "💾 تصدير الملف النظيف",
            "en" to "💾 Export Clean File",
            "fr" to "💾 Exporter fichier propre",
            "es" to "💾 Exportar Archivo Limpio",
            "de" to "💾 Saubere Datei Exportieren"
        ),
        // Enhance Screen
        "enhance_upload_prompt" to mapOf(
            "ar" to "🔍 اضغط هنا لاختيار صورة لرفع الجودة (Real-ESRGAN)",
            "en" to "🔍 Tap here to select image to enhance quality (Real-ESRGAN)",
            "fr" to "🔍 Appuyez ici pour sélectionner une image à améliorer la qualité",
            "es" to "🔍 Toque aquí para seleccionar una imagen y mejorar la calidad",
            "de" to "🔍 Hier tippen, um ein Bild auszuwählen und die Qualität zu verbessern"
        ),
        "final_dimensions" to mapOf(
            "ar" to "أبعاد الملف النهائي:",
            "en" to "Final File Dimensions:",
            "fr" to "Dimensions finales du fichier:",
            "es" to "Dimensiones finales del archivo:",
            "de" to "Endgültige DateIABmessungen:"
        ),
        "ratio_original" to mapOf("ar" to "أصلي", "en" to "Original", "fr" to "Original", "es" to "Original", "de" to "Original"),
        "ratio_1_1" to mapOf("ar" to "1:1 مربع", "en" to "1:1 Square", "fr" to "1:1 Carré", "es" to "1:1 Cuadrado", "de" to "1:1 Quadrat"),
        "ratio_9_16" to mapOf("ar" to "9:16 طولي", "en" to "9:16 Portrait", "fr" to "9:16 Portrait", "es" to "9:16 Retrato", "de" to "9:16 Hochformat"),
        "ratio_16_9" to mapOf("ar" to "16:9 عرضي", "en" to "16:9 Landscape", "fr" to "16:9 Paysage", "es" to "16:9 Paisaje", "de" to "16:9 Querformat"),
        "sharpness_balanced" to mapOf(
            "ar" to "الحدة (Sharpness) - متوازن",
            "en" to "Sharpness - Balanced",
            "fr" to "Netteté - Équilibré",
            "es" to "Nitidez - Equilibrado",
            "de" to "Schärfe - Ausgewogen"
        ),
        "contrast_balanced" to mapOf(
            "ar" to "التباين (Contrast) - متوازن",
            "en" to "Contrast - Balanced",
            "fr" to "Contraste - Équilibré",
            "es" to "Contraste - Equilibrado",
            "de" to "Kontrast - Ausgewogen"
        ),
        "btn_export_4k" to mapOf(
            "ar" to "💾 تصدير وحفظ الملف (4K HD)",
            "en" to "💾 Export & Save File (4K HD)",
            "fr" to "💾 Exporter & Enregistrer (4K HD)",
            "es" to "💾 Exportar y Guardar (4K HD)",
            "de" to "💾 Exportieren & Speichern (4K HD)"
        ),
        // Auth Dialog
        "dialog_title" to mapOf(
            "ar" to "تسجيل الدخول / Whop Pro Pass",
            "en" to "Login / Whop Pro Pass",
            "fr" to "Connexion / Whop Pro Pass",
            "es" to "Iniciar Sesión / Whop Pro Pass",
            "de" to "Anmelden / Whop Pro Pass"
        ),
        "dialog_desc" to mapOf(
            "ar" to "سجل الدخول بحساب Firebase أو أدخل رمز Whop Pro Pass للحصول على مميزات Pro وإزالة الإعلانات",
            "en" to "Sign in with Firebase or enter your Whop Pro Pass key to unlock Pro features & remove ads",
            "fr" to "Connectez-vous avec Firebase ou entrez votre clé Whop Pro Pass pour débloquer les fonctionnalités Pro",
            "es" to "Inicie sesión con Firebase o ingrese su clave Whop Pro Pass para desbloquear funciones Pro",
            "de" to "Melden Sie sich mit Firebase an oder geben Sie Ihren Whop Pro Pass-Schlüssel ein, um Pro freizuschalten"
        ),
        "email_placeholder" to mapOf(
            "ar" to "البريد الإلكتروني (Email)",
            "en" to "Email Address",
            "fr" to "Adresse Email",
            "es" to "Correo Electrónico",
            "de" to "E-Mail-Adresse"
        ),
        "password_placeholder" to mapOf(
            "ar" to "كلمة المرور (Password)",
            "en" to "Password",
            "fr" to "Mot de passe",
            "es" to "Contraseña",
            "de" to "Passwort"
        ),
        "whop_key_placeholder" to mapOf(
            "ar" to "رمز Whop Pro Pass",
            "en" to "Whop Pro Pass Key",
            "fr" to "Clé Whop Pro Pass",
            "es" to "Clave Whop Pro Pass",
            "de" to "Whop Pro Pass Schlüssel"
        ),
        "btn_login" to mapOf("ar" to "دخول 🔑", "en" to "Login 🔑", "fr" to "Connexion 🔑", "es" to "Entrar 🔑", "de" to "Anmelden 🔑"),
        "btn_register" to mapOf("ar" to "حساب جديد", "en" to "New Account", "fr" to "Nouveau Compte", "es" to "Nueva Cuenta", "de" to "Neues Konto"),
        "btn_activate_pro" to mapOf(
            "ar" to "تفعيل Whop Pro ⚡",
            "en" to "Activate Whop Pro ⚡",
            "fr" to "Activer Whop Pro ⚡",
            "es" to "Activar Whop Pro ⚡",
            "de" to "Whop Pro Aktivieren ⚡"
        ),
        "btn_cancel" to mapOf("ar" to "إلغاء", "en" to "Cancel", "fr" to "Annuler", "es" to "Cancelar", "de" to "Abbrechen"),
        // Toasts & Alerts
        "toast_select_first" to mapOf(
            "ar" to "اختر صورة أولاً!",
            "en" to "Select an image first!",
            "fr" to "Sélectionnez d'abord une image!",
            "es" to "¡Seleccione una imagen primero!",
            "de" to "Wählen Sie zuerst ein Bild aus!"
        ),
        "toast_draw_first" to mapOf(
            "ar" to "قم بالرسم بأصبعك فوق العنصر المراد مسحه أولاً!",
            "en" to "Draw over the object to erase first!",
            "fr" to "Dessinez d'abord sur l'objet à effacer!",
            "es" to "¡Dibuje primero sobre el objeto a borrar!",
            "de" to "Zeichnen Sie zuerst über das zu löschende Objekt!"
        ),
        "toast_limit_reached" to mapOf(
            "ar" to "وصلت للحد اليومي المجاني (16 استخدام). اشترك في Whop Pro للوصول غير المحدود!",
            "en" to "Daily free limit reached (16 uses). Subscribe to Whop Pro for unlimited access!",
            "fr" to "Limite quotidienne gratuite atteinte (16 utilisations). Abonnez-vous à Whop Pro pour un accès illimité!",
            "es" to "Límite diario gratuito alcanzado (16 usos). ¡Suscríbase a Whop Pro para acceso ilimitado!",
            "de" to "Tägliches kostenloses Limit erreicht (16 Nutzungen). Abonnieren Sie Whop Pro für unbegrenzten Zugriff!"
        ),
        "toast_success_saved" to mapOf(
            "ar" to "تم الحفظ! ✨",
            "en" to "Saved! ✨",
            "fr" to "Enregistré! ✨",
            "es" to "¡Guardado! ✨",
            "de" to "Gespeichert! ✨"
        ),
        "toast_failed_save" to mapOf(
            "ar" to "فشل حفظ الملف",
            "en" to "Failed to save file",
            "fr" to "Échec de l'enregistrement",
            "es" to "Error al guardar el archivo",
            "de" to "Fehler beim Speichern der Datei"
        )
    )
}
