package com.novacore.piximaxaiultra.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
fun ObjectRemoverScreen(isProUser: Boolean = false) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }

    var brushSizeDp by remember { mutableStateOf(30f) }
    var strokeList by remember { mutableStateOf(listOf<DrawnStroke>()) }

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
                strokeList = emptyList()
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
                    text = "🎯 اضغط هنا لاختيار صورة لإزالة العناصر بالأصبع (Brush)",
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
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = processedBitmap!!.asImageBitmap(),
                        contentDescription = "Object Removal Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Finger Paint Touch Canvas Overlay
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
                                            strokeList = strokeList + DrawnStroke(newPath, brushSizeDp)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        if (drawW > 0f && drawH > 0f) {
                                            val offset = change.position
                                            val relX = (offset.x - drawLeft).coerceIn(0f, drawW)
                                            val relY = (offset.y - drawTop).coerceIn(0f, drawH)
                                            strokeList.lastOrNull()?.path?.lineTo(relX, relY)
                                            strokeList = strokeList.toList()
                                        }
                                    }
                                )
                            }
                    ) {
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
            text = "ارسم بأصبعك فوق العنصر المراد مسحه بالكامل",
            color = TextSub,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

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
                Text(text = "حجم الفرشاة (Brush Size)", color = TextSub, fontSize = 11.sp)
                Text(text = "${brushSizeDp.toInt()} px", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = brushSizeDp,
                onValueChange = { brushSizeDp = it },
                valueRange = 10f..80f,
                colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue, inactiveTrackColor = BorderColor)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { strokeList = emptyList() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "مسح التحديد 🗑️", color = TextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Process Button
        Button(
            onClick = {
                val currentBmp = originalBitmap
                if (currentBmp == null || drawW <= 0f || drawH <= 0f) {
                    Toast.makeText(context, "اختر صورة أولاً!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (strokeList.isEmpty()) {
                    Toast.makeText(context, "قم بالرسم بأصبعك فوق العنصر المراد مسحه أولاً!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!ExportManager.canExport(context, isProUser)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whop.com/google-390e/piximax-ai-ultra/"))
                    context.startActivity(intent)
                    Toast.makeText(context, "وصلت للحد اليومي المجاني (3 استخدامات). اشترك في Whop Pro للوصول غير المحدود!", Toast.LENGTH_LONG).show()
                    return@Button
                }
                ExportManager.recordExportAndShowAd(context, isProUser)

                val liveStrokes = strokeList
                val liveDrawW = drawW
                val liveDrawH = drawH
                val densityVal = density.density

                isProcessing = true
                coroutineScope.launch {
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

                        ImageProcessor.removeObjectWithMask(currentBmp, maskBmp)
                    }

                    processedBitmap = result
                    originalBitmap = result
                    strokeList = emptyList()
                    isProcessing = false
                    Toast.makeText(context, "تم مسح وإزالة العنصر بنجاح! ✨", Toast.LENGTH_LONG).show()
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
                Text(text = "✨ إزالة العنصر المحدد بالأصبع", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Export Button
        Button(
            onClick = {
                val outBmp = processedBitmap
                if (outBmp == null) {
                    Toast.makeText(context, "اختر صورة أولاً!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!ExportManager.canExport(context, isProUser)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whop.com/google-390e/piximax-ai-ultra/"))
                    context.startActivity(intent)
                    Toast.makeText(context, "وصلت للحد اليومي المجاني (3 تصديرات). اشترك في Whop Pro للتصدير غير المحدود!", Toast.LENGTH_LONG).show()
                    return@Button
                }
                ExportManager.recordExportAndShowAd(context, isProUser)

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
