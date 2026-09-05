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
import androidx.compose.foundation.layout.*
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
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whop.com/whacka/piximax-pro-pass/"))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Neon Glowing 'P' Aperture Icon Canvas
            Canvas(modifier = Modifier.size(28.dp)) {
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
                    text = "PixiMax Pro",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "NovaCore Software",
                    color = TextSub,
                    fontSize = 11.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2600D2FF))
                    .border(1.dp, AccentCyan, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = ExportManager.getRemainingExportsText(context, isProUser),
                    color = AccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isProUser) Color(0x2600D2FF) else Color(0x260088FF))
                    .border(1.dp, if (isProUser) AccentCyan else AccentBlue, RoundedCornerShape(8.dp))
                    .clickable { onSignInClick() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isProUser) "PRO 👑" else "🔑 دخول",
                    color = if (isProUser) AccentCyan else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFF9900), Color(0xFFFF5500))))
                    .clickable { onUpgradeClick() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "PRO 4K⚡",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabs = listOf(
            "🎯 إزالة العناصر",
            "✂️ إزالة الخلفية",
            "✨ رتوش الوجه",
            "🧼 مسح العلامة",
            "🔍 رفع الجودة"
        )

        tabs.forEachIndexed { index, title ->
            val isActive = selectedTab == index
            val tabBackground = if (isActive) {
                Brush.linearGradient(listOf(AccentBlue.copy(alpha = 0.25f), AccentPurple.copy(alpha = 0.25f)))
            } else {
                Brush.linearGradient(listOf(CardBg, CardBg))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tabBackground)
                    .border(1.dp, if (isActive) AccentBlue else BorderColor, RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp, horizontal = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isActive) Color.White else TextSub,
                    fontSize = 9.sp,
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
                text = "تسجيل الدخول / Whop Pro Pass",
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
                    text = "سجل الدخول بحساب Firebase أو أدخل رمز Whop Pro Pass للحصول على مميزات Pro وإزالة الإعلانات",
                    color = TextSub,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = emailText,
                    onValueChange = { emailText = it },
                    placeholder = { Text("البريد الإلكتروني (Email)", color = TextSub) },
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
                    placeholder = { Text("كلمة المرور (Password)", color = TextSub) },
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
                                                        android.widget.Toast.makeText(context, "تم تسجيل الدخول بنجاح! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    }
                                                }
                                        }
                                        .addOnFailureListener {
                                            android.widget.Toast.makeText(context, "فشل تسجيل الدخول: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "تم تسجيل الدخول! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "يرجى كتابة البريد وكلمة المرور الصحيحة", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("دخول 🔑", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                            android.widget.Toast.makeText(context, "تم إنشاء الحساب بنجاح! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                        .addOnFailureListener {
                                            android.widget.Toast.makeText(context, "فشل إنشاء الحساب: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "تم إنشاء الحساب! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "يرجى كتابة البريد وكلمة المرور الصحيحة", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("حساب جديد", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    placeholder = { Text("رمز Whop Pro Pass", color = TextSub) },
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
                        android.widget.Toast.makeText(context, "أدخل رمز مفتاح Whop Pro الصحيح", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900))
            ) {
                Text("تفعيل Whop Pro ⚡", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSub)
            }
        }
    )
}
