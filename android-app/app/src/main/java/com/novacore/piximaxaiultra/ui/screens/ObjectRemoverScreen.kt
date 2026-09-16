package com.novacore.piximaxaiultra.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novacore.piximaxaiultra.ui.theme.*
import com.novacore.piximaxaiultra.utils.BrushSoundManager
import com.novacore.piximaxaiultra.utils.ExportManager
import com.novacore.piximaxaiultra.utils.ImageProcessor
import com.novacore.piximaxaiultra.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DrawnStroke(
    val path: Path,
    val strokeWidthDp: Float
)

@Composable
fun BeforeAfterSlider(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (size.width > 0) {
                        sliderPosition = (sliderPosition + dragAmount.x / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            if (canvasW <= 0f || canvasH <= 0f) return@Canvas

            val afterImage = afterBitmap.asImageBitmap()
            drawImage(
                image = afterImage,
                dstSize = IntSize(canvasW.toInt(), canvasH.toInt())
            )

            val splitX = canvasW * sliderPosition
            clipRect(left = 0f, top = 0f, right = splitX, bottom = canvasH) {
                val beforeImage = beforeBitmap.asImageBitmap()
                drawImage(
                    image = beforeImage,
                    dstSize = IntSize(canvasW.toInt(), canvasH.toInt())
                )
            }

            drawLine(
                color = Color.White,
                start = Offset(splitX, 0f),
                end = Offset(splitX, canvasH),
                strokeWidth = 3.dp.toPx()
            )

            val handleRadius = 14.dp.toPx()
            val handleCenter = Offset(splitX, canvasH / 2f)
            drawCircle(
                color = AccentCyan,
                radius = handleRadius,
                center = handleCenter
            )
            drawCircle(
                color = Color.White,
                radius = handleRadius * 0.7f,
                center = handleCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(text = "قبل (Before)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(text = "بعد (After)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ObjectRemoverScreen(
    isProUser: Boolean = false,
    initialUri: Uri? = null,
    onUriChanged: (Uri?) -> Unit = {},
    onDraftCleared: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang = com.novacore.piximaxaiultra.utils.LanguageManager.currentLang.value
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val soundManager = remember(context) { BrushSoundManager(context) }
    var isAudioFeedbackEnabled by remember { mutableStateOf(true) }

    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var initialOriginalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var isComparisonActive by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }

    val undoStack = remember { mutableStateListOf<Bitmap>() }
    val redoStack = remember { mutableStateListOf<Bitmap>() }

    var brushSizeDp by remember { mutableStateOf(30f) }
    val strokeList = remember { mutableStateListOf<DrawnStroke>() }
    var redrawSignal by remember { mutableIntStateOf(0) }

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
            onUriChanged(uri)
            isProcessing = true
            coroutineScope.launch {
                try {
                    val bmp = withContext(Dispatchers.IO) {
                        ImageUtils.loadBitmapFromUri(context, uri)
                    }
                    if (initialOriginalBitmap != bmp && initialOriginalBitmap != originalBitmap && initialOriginalBitmap != processedBitmap) {
                        ImageUtils.safeRecycle(initialOriginalBitmap)
                    }
                    if (originalBitmap != bmp && originalBitmap != processedBitmap) {
                        ImageUtils.safeRecycle(originalBitmap)
                    }
                    if (processedBitmap != bmp) {
                        ImageUtils.safeRecycle(processedBitmap)
                    }
                    initialOriginalBitmap = bmp
                    originalBitmap = bmp
                    processedBitmap = bmp
                    isComparisonActive = false
                    undoStack.clear()
                    redoStack.clear()
                    strokeList.clear()
                    redrawSignal++
                } catch (e: Throwable) {
                    e.printStackTrace()
                    Toast.makeText(context, "فشل تحميل الصورة", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    LaunchedEffect(initialUri) {
        if (initialUri != null && originalBitmap == null) {
            selectedImageUri = initialUri
            isProcessing = true
            try {
                val bmp = withContext(Dispatchers.IO) {
                    ImageUtils.loadBitmapFromUri(context, initialUri)
                }
                initialOriginalBitmap = bmp
                originalBitmap = bmp
                processedBitmap = bmp
                isComparisonActive = false
                strokeList.clear()
                redrawSignal++
            } catch (e: Throwable) {
                e.printStackTrace()
                onDraftCleared()
            } finally {
                isProcessing = false
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
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upload Box
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
                    text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("obj_upload_prompt", currentLang),
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preview Canvas Area
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
                if (isComparisonActive && initialOriginalBitmap != null) {
                    BeforeAfterSlider(
                        beforeBitmap = initialOriginalBitmap!!,
                        afterBitmap = processedBitmap!!,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = processedBitmap!!.asImageBitmap(),
                            contentDescription = "Object Removal Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // Finger Paint Touch Canvas Overlay with Low-Latency Redraw Signal
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(drawLeft, drawTop, drawW, drawH, brushSizeDp) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (drawW > 0f && drawH > 0f) {
                                                val relX = (offset.x - drawLeft).coerceIn(0f, drawW)
                                                val relY = (offset.y - drawTop).coerceIn(0f, drawH)
                                                val newPath = Path().apply { moveTo(relX, relY) }
                                                strokeList.add(DrawnStroke(newPath, brushSizeDp))
                                                redrawSignal++
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            soundManager.playBrushStrokeSound(isAudioFeedbackEnabled)
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            if (drawW > 0f && drawH > 0f) {
                                                val offset = change.position
                                                val relX = (offset.x - drawLeft).coerceIn(0f, drawW)
                                                val relY = (offset.y - drawTop).coerceIn(0f, drawH)
                                                strokeList.lastOrNull()?.path?.lineTo(relX, relY)
                                                redrawSignal++
                                            }
                                        }
                                    )
                                }
                        ) {
                            @Suppress("UNUSED_VARIABLE")
                            val dummy = redrawSignal // Observe redrawSignal for low-latency invalidation
                            translate(left = drawLeft, top = drawTop) {
                                strokeList.forEach { stroke ->
                                    val strokePx = stroke.strokeWidthDp * density.density
                                    drawPath(
                                        path = stroke.path,
                                        color = AccentCyan.copy(alpha = 0.45f),
                                        style = Stroke(
                                            width = strokePx,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
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

        if (initialOriginalBitmap != null && processedBitmap != null && processedBitmap != initialOriginalBitmap) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !isComparisonActive,
                    onClick = { isComparisonActive = false },
                    label = { Text("وضع الفرشاة 🖌️", fontSize = 11.sp) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = isComparisonActive,
                    onClick = { isComparisonActive = true },
                    label = { Text("مقارنة قبل / بعد ↔", fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("obj_draw_instruction", currentLang),
            color = TextSub,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Edit Action Row: Rotate 90° & Crop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val curr = processedBitmap ?: return@Button
                    isProcessing = true
                    coroutineScope.launch {
                        val rotated = withContext(Dispatchers.Default) {
                            ImageProcessor.rotateAndFlip(curr, 90, false)
                        }
                        if (initialOriginalBitmap != null) {
                            val rotatedInit = withContext(Dispatchers.Default) {
                                ImageProcessor.rotateAndFlip(initialOriginalBitmap!!, 90, false)
                            }
                            initialOriginalBitmap = rotatedInit
                        }
                        undoStack.add(curr)
                        redoStack.clear()
                        originalBitmap = rotated
                        processedBitmap = rotated
                        strokeList.clear()
                        redrawSignal++
                        isProcessing = false
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1624)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Text("↻ تدوير 90°", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showCropDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1624)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Text("✂️ قص الصورة", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showCropDialog) {
            AlertDialog(
                onDismissRequest = { showCropDialog = false },
                containerColor = CardBg,
                title = {
                    Text("قص الصورة (Crop Image)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("اختر نسبة القص المطلوبة:", color = TextSub, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        val presets = listOf("1:1" to "1:1 مربع", "4:3" to "4:3 نسبة", "16:9" to "16:9 عريض", "freeform" to "قص حر (80%)")
                        presets.forEach { (ratioKey, ratioLabel) ->
                            Button(
                                onClick = {
                                    showCropDialog = false
                                    val curr = processedBitmap ?: return@Button
                                    isProcessing = true
                                    coroutineScope.launch {
                                        val cropped = withContext(Dispatchers.Default) {
                                            ImageProcessor.cropBitmap(curr, ratioKey)
                                        }
                                        if (initialOriginalBitmap != null) {
                                            val croppedInit = withContext(Dispatchers.Default) {
                                                ImageProcessor.cropBitmap(initialOriginalBitmap!!, ratioKey)
                                            }
                                            initialOriginalBitmap = croppedInit
                                        }
                                        undoStack.add(curr)
                                        redoStack.clear()
                                        originalBitmap = cropped
                                        processedBitmap = cropped
                                        strokeList.clear()
                                        redrawSignal++
                                        isProcessing = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Text(ratioLabel, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCropDialog = false }) {
                        Text("إلغاء", color = TextSub)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Undo & Redo Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (undoStack.isNotEmpty()) {
                        val prev = undoStack.removeAt(undoStack.size - 1)
                        processedBitmap?.let { redoStack.add(it) }
                        processedBitmap = prev
                        originalBitmap = prev
                        strokeList.clear()
                        redrawSignal++
                    }
                },
                enabled = undoStack.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1624)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Text("↩️ تراجع", color = if (undoStack.isNotEmpty()) TextMain else TextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (redoStack.isNotEmpty()) {
                        val next = redoStack.removeAt(redoStack.size - 1)
                        processedBitmap?.let { undoStack.add(it) }
                        processedBitmap = next
                        originalBitmap = next
                        strokeList.clear()
                        redrawSignal++
                    }
                },
                enabled = redoStack.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1624)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Text("↪️ إعادة", color = if (redoStack.isNotEmpty()) TextMain else TextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Brush Controls
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("brush_size", currentLang), color = TextSub, fontSize = 11.sp)
                Text(text = "${brushSizeDp.toInt()} px", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = brushSizeDp,
                onValueChange = { brushSizeDp = it },
                valueRange = 10f..80f,
                colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue, inactiveTrackColor = BorderColor)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "صوت الفرشاة 🔊", color = TextSub, fontSize = 11.sp)
                Switch(
                    checked = isAudioFeedbackEnabled,
                    onCheckedChange = { isAudioFeedbackEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentCyan,
                        checkedTrackColor = AccentBlue.copy(alpha = 0.5f),
                        uncheckedThumbColor = BorderColor,
                        uncheckedTrackColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    strokeList.clear()
                    redrawSignal++
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("clear_selection", currentLang), color = TextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Process Button
        Button(
            onClick = {
                val currentBmp = originalBitmap
                if (currentBmp == null || drawW <= 0f || drawH <= 0f) {
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_select_first", currentLang), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (strokeList.isEmpty()) {
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_draw_first", currentLang), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!ExportManager.canExport(context, isProUser)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whop.com/google-390e/piximax-ai-ultra/"))
                    context.startActivity(intent)
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_limit_reached", currentLang), Toast.LENGTH_LONG).show()
                    return@Button
                }
                ExportManager.recordExportAndShowAd(context, isProUser)

                val liveStrokes = strokeList.toList()
                val liveDrawW = drawW
                val liveDrawH = drawH
                val densityVal = density.density

                isProcessing = true
                coroutineScope.launch {
                    try {
                        val result = withContext(Dispatchers.Default) {
                            val maskBmp = Bitmap.createBitmap(currentBmp.width, currentBmp.height, Bitmap.Config.ARGB_8888)
                            val maskCanvas = android.graphics.Canvas(maskBmp)
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                style = android.graphics.Paint.Style.STROKE
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                isAntiAlias = true
                            }

                            val scaleX = currentBmp.width.toFloat() / liveDrawW
                            val scaleY = currentBmp.height.toFloat() / liveDrawH

                            liveStrokes.forEach { stroke ->
                                paint.strokeWidth = stroke.strokeWidthDp * densityVal * scaleX
                                val nativePath = stroke.path.asAndroidPath()
                                val matrix = android.graphics.Matrix().apply {
                                    setScale(scaleX, scaleY)
                                }
                                val scaledPath = android.graphics.Path()
                                nativePath.transform(matrix, scaledPath)
                                maskCanvas.drawPath(scaledPath, paint)
                            }

                            val res = ImageProcessor.removeObjectWithMask(currentBmp, maskBmp)
                            ImageUtils.safeRecycle(maskBmp)
                            res
                        }

                        if (processedBitmap != null) {
                            undoStack.add(processedBitmap!!)
                            redoStack.clear()
                        }
                        processedBitmap = result
                        originalBitmap = result
                        isComparisonActive = true
                        strokeList.clear()
                        redrawSignal++
                        Toast.makeText(context, context.getString(com.novacore.piximaxaiultra.R.string.msg_object_removed), Toast.LENGTH_LONG).show()
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        Toast.makeText(context, "حدث خطأ أثناء معالجة الصورة", Toast.LENGTH_SHORT).show()
                    } finally {
                        isProcessing = false
                    }
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
                Text(text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("btn_erase_object", currentLang), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Export Button
        Button(
            onClick = {
                val outBmp = processedBitmap
                if (outBmp == null) {
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_select_first", currentLang), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val savedUri = ImageUtils.saveBitmapToGallery(context, outBmp, "PixiMax_ObjectRemoved_${System.currentTimeMillis()}.png")
                if (savedUri != null) {
                    onDraftCleared()
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_success_saved", currentLang), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, com.novacore.piximaxaiultra.utils.LanguageManager.getString("toast_failed_save", currentLang), Toast.LENGTH_SHORT).show()
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
                Text(text = com.novacore.piximaxaiultra.utils.LanguageManager.getString("btn_save_clean_image", currentLang), color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
