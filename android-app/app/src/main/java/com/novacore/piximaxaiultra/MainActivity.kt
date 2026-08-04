package com.novacore.piximaxaiultra

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novacore.piximaxaiultra.ui.screens.BgRemovalScreen
import com.novacore.piximaxaiultra.ui.screens.EnhanceScreen
import com.novacore.piximaxaiultra.ui.screens.WatermarkScreen
import com.novacore.piximaxaiultra.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Replace browser localStorage with native SharedPreferences
        sharedPreferences = getSharedPreferences("piximax_prefs", Context.MODE_PRIVATE)

        setContent {
            PixiMaxTheme {
                var selectedTab by remember {
                    mutableStateOf(sharedPreferences.getInt("active_tab", 0))
                }

                // Persist selection change
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
                        // Header matching original visual identity
                        AppHeader()

                        // Tabs Row matching original tabs structure
                        NavigationTabs(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )

                        // Main Content switcher
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> EnhanceScreen()
                                1 -> BgRemovalScreen()
                                2 -> WatermarkScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PixiMax AI",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFFF9900), Color(0xFFFF5500))))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ULTRA 4K",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "NovaCore Software",
                color = TextSub,
                fontSize = 11.sp
            )
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
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val tabs = listOf(
            "✨ رفع الجودة والأنيميشن",
            "✂️ إزالة الخلفية",
            "🧼 مسح العلامة المائية"
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(tabBackground)
                    .border(1.dp, if (isActive) AccentBlue else BorderColor, RoundedCornerShape(10.dp))
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isActive) Color.White else TextSub,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
