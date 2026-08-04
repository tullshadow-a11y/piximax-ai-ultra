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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novacore.piximaxaiultra.ui.theme.*
import com.novacore.piximaxaiultra.utils.ImageProcessor
import com.novacore.piximaxaiultra.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WatermarkScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }

    // Coordinates for the draggable mask
    var maskOffset by remember { mutableStateOf(Offset(100f, 100f)) }
    val maskSizeDp = Pair(100.dp, 50.dp) // Fixed physical size for high-accuracy dragging

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

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
                processedBitmap = bmp // Initial display is original
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
        // Upload Card
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

        // Draggable Image Area
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
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Render image
                    Image(
                        bitmap = processedBitmap!!.asImageBitmap(),
                        contentDescription = "Watermark Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Draggable mask overlay
                    val contextDensity = androidx.compose.ui.platform.LocalDensity.current
                    val maskWidthPx = with(contextDensity) { maskSizeDp.first.toPx() }
                    val maskHeightPx = with(contextDensity) { maskSizeDp.second.toPx() }

                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(contextDensity) { maskOffset.x.toDp() },
                                y = with(contextDensity) { maskOffset.y.toDp() }
                            )
                            .size(maskSizeDp.first, maskSizeDp.second)
                            .border(BorderStroke(2.dp, Color(0xFF00FFCC)), RoundedCornerShape(4.dp))
                            .background(Color(0xFF00FFCC).copy(alpha = 0.2f))
                            .pointerInput(containerSize) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val limitX = (containerSize.width - maskWidthPx).coerceAtLeast(0f)
                                    val limitY = (containerSize.height - maskHeightPx).coerceAtLeast(0f)
                                    maskOffset = Offset(
                                        x = (maskOffset.x + dragAmount.x).coerceIn(0f, limitX),
                                        y = (maskOffset.y + dragAmount.y).coerceIn(0f, limitY)
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
                    CircularProgressIndicator(color = Color(0xFF00B09B))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "حدد المربع القابل للسحب فوق العلامة المائية تماماً",
            color = TextSub,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Smart Erase Button
        Button(
            onClick = {
                val currentBmp = originalBitmap
                val containerW = containerSize.width.toFloat()
                val containerH = containerSize.height.toFloat()

                if (currentBmp == null || containerW <= 0f || containerH <= 0f) {
                    Toast.makeText(context, "اختر صورة أولاً!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isProcessing = true
                coroutineScope.launch {
                    val result = withContext(Dispatchers.Default) {
                        // Calculate precise sub-region bounding box percentages
                        val imgAspect = currentBmp.width.toFloat() / currentBmp.height.toFloat()
                        val containerAspect = containerW / containerH

                        val drawW: Float
                        val drawH: Float
                        val drawLeft: Float
                        val drawTop: Float

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

                        // Convert physical sizes to pixels for calculation
                        val contextDensity = context.resources.displayMetrics.density
                        val maskW = 100f * contextDensity
                        val maskH = 50f * contextDensity

                        val relativeLeft = (maskOffset.x - drawLeft) / drawW
                        val relativeTop = (maskOffset.y - drawTop) / drawH
                        val relativeW = maskW / drawW
                        val relativeH = maskH / drawH

                        ImageProcessor.eraseWatermark(
                            src = currentBmp,
                            leftPercent = relativeLeft.coerceIn(0f, 1f),
                            topPercent = relativeTop.coerceIn(0f, 1f),
                            widthPercent = relativeW.coerceIn(0f, 1f),
                            heightPercent = relativeH.coerceIn(0f, 1f)
                        )
                    }

                    processedBitmap = result
                    originalBitmap = result // So that we can run multi-erase on the same file!
                    isProcessing = false
                    Toast.makeText(context, "تم مسح العلامة المائية بنجاح! ✨", Toast.LENGTH_LONG).show()
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

        // Save Clean Image Button
        Button(
            onClick = {
                val outBmp = processedBitmap
                if (outBmp == null) {
                    Toast.makeText(context, "اختر صورة أولاً!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val savedUri = ImageUtils.saveBitmapToGallery(context, outBmp, "PixiMax_Cleaned_${System.currentTimeMillis()}.png")
                if (savedUri != null) {
                    Toast.makeText(context, "تم الحفظ بنجاح! ✨", Toast.LENGTH_LONG).show()
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
