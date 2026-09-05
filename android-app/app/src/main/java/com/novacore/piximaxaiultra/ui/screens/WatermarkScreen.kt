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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
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
fun WatermarkScreen(isProUser: Boolean = false) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }

    var maskWidthDp by remember { mutableStateOf(100f) }
    var maskHeightDp by remember { mutableStateOf(50f) }

    var maskOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var drawLeft by remember { mutableStateOf(0f) }
    var drawTop by remember { mutableStateOf(0f) }
    var drawW by remember { mutableStateOf(0f) }
    var drawH by remember { mutableStateOf(0f) }

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
                processedBitmap = bmp
                isProcessing = false
                maskOffset = Offset(100f, 100f)
            }
        }
    }

    LaunchedEffect(containerSize, processedBitmap) {
        val bmp = processedBitmap ?: return@LaunchedEffect
        val containerW = containerSize.width.toFloat()
        val containerH = containerSize.height.toFloat()
        if (containerW <= 0f || containerH <= 0f) return@LaunchedEffect

        val imgAspect = bmp.width.toFloat() / bmp.height.toFloat()
        val containerAspect = containerW / containerH

        if (imgAspect > containerAspect) {
            drawW = containerW
            drawH = containerW / imgAspect
            drawLeft = 0f
            drawTop = (containerH - drawH) / 2f
        } else {
            drawH = containerH
            drawW = containerH * imgAspect
            drawLeft = (containerW - drawW) / 2f
            drawTop = 0f
        }

        val maskW = with(density) { maskWidthDp.dp.toPx() }
        val maskH = with(density) { maskHeightDp.dp.toPx() }
        maskOffset = Offset(
            x = drawLeft + (drawW - maskW) / 2f,
            y = drawTop + (drawH - maskH) / 2f
        )
    }

    val maskWidthPx = with(density) { maskWidthDp.dp.toPx() }
    val maskHeightPx = with(density) { maskHeightDp.dp.toPx() }

    LaunchedEffect(maskWidthDp, maskHeightDp, drawLeft, drawTop, drawW, drawH) {
        if (drawW <= 0f || drawH <= 0f) return@LaunchedEffect
        val xMin = drawLeft
        val xMax = (drawLeft + drawW - maskWidthPx).coerceAtLeast(drawLeft)
        val yMin = drawTop
        val yMax = (drawTop + drawH - maskHeightPx).coerceAtLeast(drawTop)

        maskOffset = Offset(
            x = maskOffset.x.coerceIn(xMin, xMax),
            y = maskOffset.y.coerceIn(yMin, yMax)
        )
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
                .border(2.dp, Brush.linearGradient(listOf(Color(0xFF00B09B), Color(0xFF96C93D))), RoundedCornerShape(12.dp))
                .background(Color(0xFF00B09B).copy(alpha = 0.05f))
                .clickable { imagePickerLauncher.launch("image/*") }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🧼 اضغط هنا لاختيار صورة لمسح العلامة المائية",
                    color = Color(0xFF00B09B),
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
                .height(280.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .background(Color.Black)
                .onGloballyPositioned { coordinates ->
                    containerSize = coordinates.size
                },
            contentAlignment = Alignment.Center
        ) {
            if (processedBitmap != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = processedBitmap!!.asImageBitmap(),
                        contentDescription = "Watermark Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(density) { maskOffset.x.toDp() },
                                y = with(density) { maskOffset.y.toDp() }
                            )
                            .size(maskWidthDp.dp, maskHeightDp.dp)
                            .border(BorderStroke(2.dp, SuccessGreen), RoundedCornerShape(4.dp))
                            .background(SuccessGreen.copy(alpha = 0.15f))
                            .pointerInput(drawLeft, drawTop, drawW, drawH, maskWidthPx, maskHeightPx) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val xMin = drawLeft
                                    val xMax = (drawLeft + drawW - maskWidthPx).coerceAtLeast(drawLeft)
                                    val yMin = drawTop
                                    val yMax = (drawTop + drawH - maskHeightPx).coerceAtLeast(drawTop)

                                    maskOffset = Offset(
                                        x = (maskOffset.x + dragAmount.x).coerceIn(xMin, xMax),
                                        y = (maskOffset.y + dragAmount.y).coerceIn(yMin, yMax)
                                    )
                                }
                            }
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(SuccessGreen).align(Alignment.TopStart))
                        Box(modifier = Modifier.size(6.dp).background(SuccessGreen).align(Alignment.TopEnd))
                        Box(modifier = Modifier.size(6.dp).background(SuccessGreen).align(Alignment.BottomStart))
                        Box(modifier = Modifier.size(6.dp).background(SuccessGreen).align(Alignment.BottomEnd))
                    }
                }
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
                    CircularProgressIndicator(color = Color(0xFF00B09B))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "حدد المربع القابل للسحب فوق العلامة المائية تماماً",
            color = TextSub,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                Text(text = "عرض صندوق التحديد (Width)", color = TextSub, fontSize = 11.sp)
                Text(text = "${maskWidthDp.toInt()} dp", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = maskWidthDp,
                onValueChange = { maskWidthDp = it },
                valueRange = 20f..200f,
                colors = SliderDefaults.colors(
                    thumbColor = SuccessGreen,
                    activeTrackColor = SuccessGreen,
                    inactiveTrackColor = BorderColor
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "ارتفاع صندوق التحديد (Height)", color = TextSub, fontSize = 11.sp)
                Text(text = "${maskHeightDp.toInt()} dp", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = maskHeightDp,
                onValueChange = { maskHeightDp = it },
                valueRange = 10f..150f,
                colors = SliderDefaults.colors(
                    thumbColor = SuccessGreen,
                    activeTrackColor = SuccessGreen,
                    inactiveTrackColor = BorderColor
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                val currentBmp = originalBitmap
                if (currentBmp == null || drawW <= 0f || drawH <= 0f) {
                    Toast.makeText(context, "اختر صورة أولاً!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val liveMaskOffset = maskOffset
                val liveMaskWidthPx = maskWidthPx
                val liveMaskHeightPx = maskHeightPx
                val liveDrawLeft = drawLeft
                val liveDrawTop = drawTop
                val liveDrawW = drawW
                val liveDrawH = drawH

                isProcessing = true
                coroutineScope.launch {
                    val result = withContext(Dispatchers.Default) {
                        val touchX = liveMaskOffset.x - liveDrawLeft
                        val touchY = liveMaskOffset.y - liveDrawTop

                        val relativeLeft = touchX / liveDrawW
                        val relativeTop = touchY / liveDrawH
                        val relativeW = liveMaskWidthPx / liveDrawW
                        val relativeH = liveMaskHeightPx / liveDrawH

                        ImageProcessor.eraseWatermark(
                            src = currentBmp,
                            leftPercent = relativeLeft.coerceIn(0f, 1f),
                            topPercent = relativeTop.coerceIn(0f, 1f),
                            widthPercent = relativeW.coerceIn(0f, 1f),
                            heightPercent = relativeH.coerceIn(0f, 1f)
                        )
                    }

                    processedBitmap = result
                    originalBitmap = result
                    isProcessing = false
                    Toast.makeText(context, "تم مسح العلامة المائية! ✨", Toast.LENGTH_LONG).show()
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
                    .background(Brush.linearGradient(listOf(Color(0xFF00B09B), Color(0xFF96C93D)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧼 تطبيق المسح الذكي الحقيقي",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val outBmp = processedBitmap
                if (outBmp == null) {
                    Toast.makeText(context, "اختر صورة أولاً!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!ExportManager.canExport(context, isProUser)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whop.com/whacka/piximax-pro-pass/"))
                    context.startActivity(intent)
                    Toast.makeText(context, "وصلت للحد اليومي المجاني (3 تصديرات). اشترك في Whop Pro للتصدير غير المحدود!", Toast.LENGTH_LONG).show()
                    return@Button
                }
                ExportManager.recordExportAndShowAd(context, isProUser)

                val savedUri = ImageUtils.saveBitmapToGallery(context, outBmp, "PixiMax_Cleaned_${System.currentTimeMillis()}.png")
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
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BorderColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💾 تصدير الملف النظيف",
                    color = TextMain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
