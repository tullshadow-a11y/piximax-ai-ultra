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
import android.content.Intent
import com.novacore.piximaxaiultra.ui.theme.*
import com.novacore.piximaxaiultra.utils.ExportManager
import com.novacore.piximaxaiultra.utils.ImageProcessor
import com.novacore.piximaxaiultra.utils.ImageUtils
import com.startapp.sdk.adsbase.StartAppAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EnhanceScreen(isProUser: Boolean = false) {
    val context = LocalContext.current
    val currentLang = com.novacore.piximaxaiultra.utils.LanguageManager.currentLang.value
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var selectedRatio by remember { mutableStateOf("original") }
    var sharpness by remember { mutableStateOf(105f) }
    var contrast by remember { mutableStateOf(102f) }
    var isProcessing by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isProcessing = true
            coroutineScope.launch {
                try {
                    val bmp = withContext(Dispatchers.IO) {
                        ImageUtils.loadBitmapFromUri(context, uri)
                    }
                    if (originalBitmap != bmp) {
                        ImageUtils.safeRecycle(originalBitmap)
                    }
                    if (processedBitmap != bmp && processedBitmap != originalBitmap) {
                        ImageUtils.safeRecycle(processedBitmap)
                    }
                    originalBitmap = bmp
                } catch (e: Throwable) {
                    e.printStackTrace()
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_failed_save", currentLang), Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    LaunchedEffect(originalBitmap, selectedRatio, sharpness, contrast) {
        val original = originalBitmap ?: return@LaunchedEffect
        isProcessing = true
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    ImageProcessor.enhance(original, selectedRatio, sharpness, contrast)
                }
                if (processedBitmap != result && processedBitmap != original) {
                    ImageUtils.safeRecycle(processedBitmap)
                }
                processedBitmap = result
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                isProcessing = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("enhance_upload_prompt", currentLang),
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("no_image_selected", currentLang),
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

        Spacer(modifier = Modifier.height(12.dp))

        processedBitmap?.let { bmp ->
            Text(
                text = "${com.novacore.piximaxaiultra.utils.LanguageManager.getString("final_dimensions", currentLang)} ${bmp.width} × ${bmp.height} px",
                color = TextSub,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val ratios = listOf(
                "original" to com.novacore.piximaxaiultra.utils.LanguageManager.getString("ratio_original", currentLang),
                "1:1" to com.novacore.piximaxaiultra.utils.LanguageManager.getString("ratio_1_1", currentLang),
                "9:16" to com.novacore.piximaxaiultra.utils.LanguageManager.getString("ratio_9_16", currentLang),
                "16:9" to com.novacore.piximaxaiultra.utils.LanguageManager.getString("ratio_16_9", currentLang)
            )

            ratios.forEach { (ratioKey, ratioLabel) ->
                val isActive = selectedRatio == ratioKey
                Button(
                    onClick = { selectedRatio = ratioKey },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) AccentBlue else Color(0xFF0F1624),
                        contentColor = if (isActive) Color.White else TextSub
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    border = BorderStroke(1.dp, if (isActive) AccentBlue else BorderColor)
                ) {
                    Text(
                        text = ratioLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("sharpness_balanced", currentLang), color = TextSub, fontSize = 11.sp)
                Text(text = "${sharpness.toInt()}%", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = sharpness,
                onValueChange = { sharpness = it },
                valueRange = 100f..150f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentBlue,
                    activeTrackColor = AccentBlue,
                    inactiveTrackColor = BorderColor
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("contrast_balanced", currentLang), color = TextSub, fontSize = 11.sp)
                Text(text = "${contrast.toInt()}%", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = contrast,
                onValueChange = { contrast = it },
                valueRange = 100f..140f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentBlue,
                    activeTrackColor = AccentBlue,
                    inactiveTrackColor = BorderColor
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                val outBmp = processedBitmap
                if (outBmp == null) {
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_select_first", currentLang), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!ExportManager.canExport(context, isProUser)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whop.com/google-390e/piximax-ai-ultra/"))
                    context.startActivity(intent)
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_limit_reached", currentLang), Toast.LENGTH_LONG).show()
                    return@Button
                }
                ExportManager.recordExportAndShowAd(context, isProUser)

                val savedUri = ImageUtils.saveBitmapToGallery(context, outBmp, "PixiMax_4K_${System.currentTimeMillis()}.png")
                if (savedUri != null) {
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_success_saved", currentLang), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_failed_save", currentLang), Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Unspecified),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("btn_export_4k", currentLang),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
