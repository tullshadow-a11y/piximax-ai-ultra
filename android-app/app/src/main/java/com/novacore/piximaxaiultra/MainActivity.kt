package com.novacore.piximaxaiultra

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.novacore.piximaxaiultra.ui.screens.*
import com.novacore.piximaxaiultra.ui.theme.*
import com.novacore.piximaxaiultra.utils.ExportManager
import com.startapp.sdk.ads.banner.Banner

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseFirestore: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            FirebaseApp.initializeApp(this)
            firebaseAuth = FirebaseAuth.getInstance()
            firebaseFirestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        com.startapp.sdk.adsbase.StartAppSDK.init(this, "207695262", false)
        com.startapp.sdk.adsbase.StartAppSDK.enableReturnAds(false)

        com.novacore.piximaxaiultra.utils.LanguageManager.init(this)
        sharedPreferences = getSharedPreferences("piximax_prefs", Context.MODE_PRIVATE)

        setContent {
            PixiMaxTheme {
                var selectedTab by remember {
                    mutableStateOf(sharedPreferences.getInt("active_tab", 0))
                }
                var isProUser by remember {
                    mutableStateOf(sharedPreferences.getBoolean("piximax_pro_auth", false))
                }
                var showAuthDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    firebaseAuth?.currentUser?.let { user ->
                        firebaseFirestore?.collection("users")?.document(user.uid)?.get()
                            ?.addOnSuccessListener { doc ->
                                if (doc != null && doc.getBoolean("isPro") == true) {
                                    isProUser = true
                                    sharedPreferences.edit().putBoolean("piximax_pro_auth", true).apply()
                                }
                            }
                    }
                }

                LaunchedEffect(selectedTab) {
                    sharedPreferences.edit().putInt("active_tab", selectedTab).apply()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BgColor
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 12.dp, bottom = innerPadding.calculateBottomPadding())
                    ) {
                        AppHeader(
                            isProUser = isProUser,
                            onSignInClick = { showAuthDialog = true },
                            onUpgradeClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whop.com/google-390e/piximax-ai-ultra/"))
                                startActivity(intent)
                            }
                        )

                        NavigationTabs(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> ObjectRemoverScreen(isProUser)
                                1 -> BgRemovalScreen(isProUser)
                                2 -> FaceRetouchScreen(isProUser)
                                3 -> WatermarkScreen(isProUser)
                                4 -> EnhanceScreen(isProUser)
                            }
                        }

                        if (!isProUser) {
                            AndroidView(
                                modifier = Modifier.fillMaxWidth(),
                                factory = { ctx -> Banner(ctx) }
                            )
                        }
                    }

                    if (showAuthDialog) {
                        ProAuthDialog(
                            onDismiss = { showAuthDialog = false },
                            onAuthenticated = {
                                isProUser = true
                                sharedPreferences.edit().putBoolean("piximax_pro_auth", true).apply()
                                showAuthDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppHeader(
    isProUser: Boolean,
    onSignInClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val currentLang = com.novacore.piximaxaiultra.utils.LanguageManager.currentLang.value
    var langMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Logo & "PixiMax Pro" Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Neon Glowing 'P' Aperture Icon Canvas
            Canvas(modifier = Modifier.size(26.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val glowRadius = size.width / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00D2FF), Color(0x668A2BE2), Color(0x00000000)),
                        center = center,
                        radius = glowRadius
                    )
                )

                // Outer Ring
                drawCircle(
                    brush = Brush.linearGradient(listOf(Color(0xFF00D2FF), Color(0xFF8A2BE2))),
                    radius = size.width * 0.42f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )

                // Neon 'P' Symbol
                val pPath = Path().apply {
                    val w = size.width
                    val h = size.height
                    moveTo(w * 0.36f, h * 0.26f)
                    lineTo(w * 0.36f, h * 0.74f)
                    lineTo(w * 0.43f, h * 0.74f)
                    lineTo(w * 0.43f, h * 0.51f)
                    lineTo(w * 0.54f, h * 0.51f)
                    cubicTo(w * 0.65f, h * 0.51f, w * 0.65f, h * 0.26f, w * 0.54f, h * 0.26f)
                    close()

                    moveTo(w * 0.43f, h * 0.33f)
                    lineTo(w * 0.53f, h * 0.33f)
                    cubicTo(w * 0.58f, h * 0.33f, w * 0.58f, h * 0.44f, w * 0.53f, h * 0.44f)
                    lineTo(w * 0.43f, h * 0.44f)
                    close()
                }
                drawPath(
                    path = pPath,
                    brush = Brush.linearGradient(listOf(Color(0xFF00D2FF), Color(0xFF8A2BE2)))
                )
            }

            Column {
                Text(
                    text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("app_title"),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("app_subtitle"),
                    color = TextSub,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }

        // Right: Language Selector (🌐 EN) | Free Usage Counter ("16/16 Free") | Login Button (🔑 Login) | PRO Button (⚡ PRO 4K)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current

            // 1. Language Selector (🌐 EN)
            Box {
                val currentOpt = com.novacore.piximaxaiultra.utils.LanguageManager.supportedLanguages.firstOrNull { it.code == currentLang }
                    ?: com.novacore.piximaxaiultra.utils.LanguageManager.supportedLanguages.first()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x268A2BE2))
                        .border(1.dp, AccentPurple, RoundedCornerShape(8.dp))
                        .clickable { langMenuExpanded = true }
                        .padding(horizontal = 5.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🌐 ${currentOpt.code.uppercase()}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                DropdownMenu(
                    expanded = langMenuExpanded,
                    onDismissRequest = { langMenuExpanded = false },
                    modifier = Modifier.background(CardBg)
                ) {
                    com.novacore.piximaxaiultra.utils.LanguageManager.supportedLanguages.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "🌐 ${option.code.uppercase()} (${option.name})",
                                    color = if (option.code == currentLang) AccentCyan else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (option.code == currentLang) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                com.novacore.piximaxaiultra.utils.LanguageManager.setLanguage(context, option.code)
                                langMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. Free Usage Counter ("16/16 Free")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2600D2FF))
                    .border(1.dp, AccentCyan, RoundedCornerShape(8.dp))
                    .padding(horizontal = 5.dp, vertical = 4.dp)
            ) {
                Text(
                    text = ExportManager.getRemainingExportsText(context, isProUser, currentLang),
                    color = AccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // 3. Login Button (🔑 Login)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isProUser) Color(0x2600D2FF) else Color(0x260088FF))
                    .border(1.dp, if (isProUser) AccentCyan else AccentBlue, RoundedCornerShape(8.dp))
                    .clickable { onSignInClick() }
                    .padding(horizontal = 5.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isProUser) com.novacore.piximaxaiultra.utils.LanguageManager.getString("pro_badge")
                           else com.novacore.piximaxaiultra.utils.LanguageManager.getString("login_btn"),
                    color = if (isProUser) AccentCyan else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // 4. PRO Button (⚡ PRO 4K)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFF9900), Color(0xFFFF5500))))
                    .clickable { onUpgradeClick() }
                    .padding(horizontal = 5.dp, vertical = 4.dp)
            ) {
                Text(
                    text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("upgrade_4k"),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun NavigationTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(
            com.novacore.piximaxaiultra.utils.LanguageManager.getString("tab_object_remover"),
            com.novacore.piximaxaiultra.utils.LanguageManager.getString("tab_bg_removal"),
            com.novacore.piximaxaiultra.utils.LanguageManager.getString("tab_face_retouch"),
            com.novacore.piximaxaiultra.utils.LanguageManager.getString("tab_watermark"),
            com.novacore.piximaxaiultra.utils.LanguageManager.getString("tab_enhance")
        )

        tabs.forEachIndexed { index, title ->
            val isActive = selectedTab == index
            val tabBackground = if (isActive) {
                Brush.linearGradient(listOf(AccentBlue.copy(alpha = 0.3f), AccentPurple.copy(alpha = 0.3f)))
            } else {
                Brush.linearGradient(listOf(CardBg, CardBg))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(tabBackground)
                    .border(
                        width = 1.dp,
                        color = if (isActive) AccentBlue else BorderColor,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isActive) Color.White else TextSub,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ProAuthDialog(
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var keyText by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = {
            Text(
                text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("dialog_title"),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("dialog_desc"),
                    color = TextSub,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = emailText,
                    onValueChange = { emailText = it },
                    placeholder = { Text(com.novacore.piximaxaiultra.utils.LanguageManager.getString("email_placeholder"), color = TextSub) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    placeholder = { Text(com.novacore.piximaxaiultra.utils.LanguageManager.getString("password_placeholder"), color = TextSub) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (emailText.contains("@") && passwordText.length >= 6) {
                                try {
                                    FirebaseAuth.getInstance().signInWithEmailAndPassword(emailText.trim(), passwordText.trim())
                                        .addOnSuccessListener { authResult ->
                                            val uid = authResult.user?.uid ?: ""
                                            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                                                .addOnSuccessListener { doc ->
                                                    if (doc.getBoolean("isPro") == true) {
                                                        onAuthenticated()
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Success! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    }
                                                }
                                        }
                                        .addOnFailureListener {
                                            android.widget.Toast.makeText(context, "Login failed: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Success! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Invalid email / password", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text(com.novacore.piximaxaiultra.utils.LanguageManager.getString("btn_login"), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (emailText.contains("@") && passwordText.length >= 6) {
                                try {
                                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailText.trim(), passwordText.trim())
                                        .addOnSuccessListener { authResult ->
                                            val uid = authResult.user?.uid ?: ""
                                            val userMap = mapOf("email" to emailText.trim(), "isPro" to false)
                                            FirebaseFirestore.getInstance().collection("users").document(uid).set(userMap)
                                            android.widget.Toast.makeText(context, "Success! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                        .addOnFailureListener {
                                            android.widget.Toast.makeText(context, "Failed: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Success! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Invalid email / password", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text(com.novacore.piximaxaiultra.utils.LanguageManager.getString("btn_register"), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    placeholder = { Text(com.novacore.piximaxaiultra.utils.LanguageManager.getString("whop_key_placeholder"), color = TextSub) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyText.trim().length >= 3) {
                        try {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user != null) {
                                val userMap = mapOf("email" to (user.email ?: ""), "isPro" to true)
                                FirebaseFirestore.getInstance().collection("users").document(user.uid).set(userMap)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onAuthenticated()
                    } else {
                        android.widget.Toast.makeText(context, "Enter valid Whop Pro key", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900))
            ) {
                Text(com.novacore.piximaxaiultra.utils.LanguageManager.getString("btn_activate_pro"), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(com.novacore.piximaxaiultra.utils.LanguageManager.getString("btn_cancel"), color = TextSub)
            }
        }
    )
}
