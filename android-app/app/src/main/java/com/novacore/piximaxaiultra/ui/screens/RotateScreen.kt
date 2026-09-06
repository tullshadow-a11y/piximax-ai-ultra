package com.novacore.piximaxaiultra.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novacore.piximaxaiultra.ui.theme.*
import com.novacore.piximaxaiultra.utils.ExportManager
import com.novacore.piximaxaiultra.utils.ImageProcessor
import com.novacore.piximaxaiultra.utils.ImageUtils
import com.startapp.sdk.adsbase.StartAppAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RotateScreen(isProUser: Boolean = false) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var rotationDegrees by remember { mutableStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isProcessing = true
            coroutineScope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    ImageUtils.loadBitmapFromUri(context, uri)
                }
                originalBitmap = bmp
                rotationDegrees = 0
                flipHorizontal = false
                isProcessing = false
            }
        }
    }

    // Process image reactively when rotation, flip, or original bitmap changes
    LaunchedEffect(originalBitmap, rotationDegrees, flipHorizontal) {
        val original = originalBitmap ?: return@LaunchedEffect
        isProcessing = true
        coroutineScope.launch {
            val result = withContext(Dispatchers.Default) {
                ImageProcessor.rotateAndFlip(original, rotationDegrees, flipHorizontal)
            }
            processedBitmap = result
            isProcessing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upload Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Brush.linearGradient(listOf(AccentBlue, AccentPurple)), RoundedCornerShape(12.dp))
                .background(AccentBlue.copy(alpha = 0.05f))
                .clickable { imagePickerLauncher.launch("image/*") }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🔄 اختر صورة لتدويرها أو عكسها اتجاهياً",
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preview Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (processedBitmap != null) {
                Image(
                    bitmap = processedBitmap!!.asImageBitmap(),
                    contentDescription = "Rotate Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "لم يتم اختيار صورة بعد",
                    color = TextSub,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tool buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rotate Button
            Button(
                onClick = { rotationDegrees = (rotationDegrees + 90) % 360 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F1624),
                    contentColor = TextMain
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Text(
                    text = "↻ تدوير 90°",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Flip Button
            Button(
                onClick = { flipHorizontal = !flipHorizontal },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F1624),
                    contentColor = TextMain
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Text(
                    text = "↔ انعكاس أفقي",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Export/Save Button
        Button(
            onClick = {
                val outBmp = processedBitmap
                if (outBmp == null) {
                    Toast.makeText(context, "اختر صورة أولاً!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!ExportManager.canExport(context, isProUser)) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://whop.com/google-390e/piximax-ai-ultra/"))
                    context.startActivity(intent)
                    Toast.makeText(context, "وصلت للحد اليومي المجاني (3 تصديرات). اشترك في Whop Pro للتصدير غير المحدود!", Toast.LENGTH_LONG).show()
                    return@Button
                }
                ExportManager.recordExportAndShowAd(context, isProUser)

                val savedUri = ImageUtils.saveBitmapToGallery(context, outBmp, "PixiMax_Edited_${System.currentTimeMillis()}.png")
                if (savedUri != null) {
                    Toast.makeText(context, "تم الحفظ! ✨", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "فشل حفظ الملف", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Unspecified),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💾 تصدير الصورة المعدلة",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
