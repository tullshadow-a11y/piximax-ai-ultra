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
import com.novacore.piximaxaiultra.ui.screens.*
import com.novacore.piximaxaiultra.ui.theme.*
import com.startapp.sdk.ads.banner.Banner

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            // 4-Point AI Sparkle Icon Canvas
            Canvas(modifier = Modifier.size(28.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val glowRadius = size.width / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00D2FF), Color(0x000088FF)),
                        center = center,
                        radius = glowRadius
                    )
                )

                val path = Path().apply {
                    moveTo(center.x, 2f)
                    quadraticTo(center.x, center.y, 2f, center.y)
                    quadraticTo(center.x, center.y, center.x, size.height - 2f)
                    quadraticTo(center.x, center.y, size.width - 2f, center.y)
                    quadraticTo(center.x, center.y, center.x, 2f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF8A2BE2)))
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
    var keyText by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = {
            Text(
                text = "تسجيل دخول PixiMax Pro",
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
                    text = "أدخل مفتاح اشتراك Whop Pro Pass لتفعيل المميزات وإزالة الإعلانات",
                    color = TextSub,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
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
                        onAuthenticated()
                    } else {
                        android.widget.Toast.makeText(context, "أدخل رمز مفتاح Whop Pro الصحيح", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("تفعيل PRO ✨", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSub)
            }
        }
    )
}
