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
import com.novacore.piximaxaiultra.ui.theme.*
import com.novacore.piximaxaiultra.utils.ImageProcessor
import com.novacore.piximaxaiultra.utils.ImageUtils
import com.startapp.sdk.adsbase.StartAppAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ObjectRemoverScreen(isProUser: Boolean = false) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }

    var maskWidthDp by remember { mutableStateOf(100f) }
    var maskHeightDp by remember { mutableStateOf(100f) }

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
        // Upload Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Brush.linearGradient(listOf(AccentBlue, AccentCyan)), RoundedCornerShape(12.dp))
                .background(AccentBlue.copy(alpha = 0.05f))
                .clickable { imagePickerLauncher.launch("image/*") }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎯 اضغط هنا لاختيار صورة لإزالة العناصر والأشخاص",
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
                        contentDescription = "Object Removal Preview",
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
                            .border(BorderStroke(2.dp, AccentCyan), RoundedCornerShape(4.dp))
                            .background(AccentCyan.copy(alpha = 0.2f))
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
                    )
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
                    CircularProgressIndicator(color = AccentBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "قم بسحب وإعادة تحجيم مربع التحديد فوق العنصر المراد مسحه",
            color = TextSub,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "عرض منطقة التحديد", color = TextSub, fontSize = 11.sp)
                Text(text = "${maskWidthDp.toInt()} dp", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = maskWidthDp,
                onValueChange = { maskWidthDp = it },
                valueRange = 20f..200f,
                colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue, inactiveTrackColor = BorderColor)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "ارتفاع منطقة التحديد", color = TextSub, fontSize = 11.sp)
                Text(text = "${maskHeightDp.toInt()} dp", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = maskHeightDp,
                onValueChange = { maskHeightDp = it },
                valueRange = 20f..200f,
                colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue, inactiveTrackColor = BorderColor)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Button
        Button(
            onClick = {
                val currentBmp = originalBitmap ?: return@Button
                if (!isProUser) {
                    StartAppAd.showAd(context)
                }

                val liveMaskOffset = maskOffset
                val liveMaskWidthPx = maskWidthPx
                val liveMaskHeightPx = maskHeightPx

                isProcessing = true
                coroutineScope.launch {
                    val result = withContext(Dispatchers.Default) {
                        val touchX = liveMaskOffset.x - drawLeft
                        val touchY = liveMaskOffset.y - drawTop

                        val relativeLeft = touchX / drawW
                        val relativeTop = touchY / drawH
                        val relativeW = liveMaskWidthPx / drawW
                        val relativeH = liveMaskHeightPx / drawH

                        ImageProcessor.removeObject(
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
                    Toast.makeText(context, "تم مسح العنصر بنجاح! ✨", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Unspecified),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(AccentBlue, AccentCyan))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✨ إزالة العنصر الموائم تلقائياً", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val outBmp = processedBitmap ?: return@Button
                if (!isProUser) {
                    StartAppAd.showAd(context)
                }
                val savedUri = ImageUtils.saveBitmapToGallery(context, outBmp, "PixiMax_ObjectRemoved_${System.currentTimeMillis()}.png")
                if (savedUri != null) {
                    Toast.makeText(context, "تم الحفظ! ✨", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "فشل حفظ الملف", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Unspecified),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(BorderColor),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💾 حفظ وتصدير الصورة النظيفة", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
