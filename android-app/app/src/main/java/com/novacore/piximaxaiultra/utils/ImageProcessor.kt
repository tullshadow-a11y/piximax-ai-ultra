package com.novacore.piximaxaiultra.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF

object ImageProcessor {

    /**
     * Resizes, crops, and processes contrast/brightness on a bitmap.
     * Re-tuned with conservative default values (105% sharpness, 102% contrast)
     * to prevent harsh text distortion and pixelation.
     */
    fun enhance(
        bitmap: Bitmap,
        ratio: String,
        sharpnessPercent: Float = 105f,
        contrastPercent: Float = 102f
    ): Bitmap {
        val originalW = bitmap.width
        val originalH = bitmap.height

        var targetW = originalW
        var targetH = originalH

        when (ratio) {
            "1:1" -> targetH = targetW
            "9:16" -> targetH = Math.round(targetW * (16f / 9f))
            "16:9" -> targetH = Math.round(targetW * (9f / 16f))
        }

        // Calculate scaling and center-cropping offset
        val imgAspect = originalW.toFloat() / originalH.toFloat()
        val targetAspect = targetW.toFloat() / targetH.toFloat()
        var renderW = targetW.toFloat()
        var renderH = targetH.toFloat()
        var offsetX = 0f
        var offsetY = 0f

        if (imgAspect > targetAspect) {
            renderH = targetH.toFloat()
            renderW = targetH * imgAspect
            offsetX = (targetW - renderW) / 2f
        } else {
            renderW = targetW.toFloat()
            renderH = targetW / imgAspect
            offsetY = (targetH - renderH) / 2f
        }

        // Apply contrast and brightness adjustments via Canvas & ColorMatrixColorFilter
        val tempBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tempBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val contrastScale = contrastPercent / 100f
        val brightnessScale = 1.01f
        val scale = contrastScale * brightnessScale
        val translate = (0.5f * (1f - contrastScale) * brightnessScale) * 255f

        val colorMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        // Draw with filter
        canvas.drawBitmap(bitmap, null, RectF(offsetX, offsetY, offsetX + renderW, offsetY + renderH), paint)

        // Now apply conservative sharpening kernel
        return sharpen(tempBitmap, sharpnessPercent)
    }

    /**
     * Sharpens a bitmap using a fast 3x3 convolution kernel.
     */
    fun sharpen(src: Bitmap, sharpnessPercent: Float): Bitmap {
        if (sharpnessPercent <= 100f) return src

        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        val outputPixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val k = ((sharpnessPercent - 100f) / 100f) * 0.15f
        if (k <= 0f) return src

        val centerWeight = 1f + 4f * k
        val surroundWeight = -k

        for (x in 0 until width) {
            outputPixels[x] = pixels[x]
            outputPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            outputPixels[y * width] = pixels[y * width]
            outputPixels[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x

                val p01 = pixels[idx - width]
                val p10 = pixels[idx - 1]
                val p11 = pixels[idx]
                val p12 = pixels[idx + 1]
                val p21 = pixels[idx + width]

                val r11 = (p11 shr 16) and 0xFF
                val rSurround = (((p01 shr 16) and 0xFF) +
                                 ((p10 shr 16) and 0xFF) +
                                 ((p12 shr 16) and 0xFF) +
                                 ((p21 shr 16) and 0xFF))
                var r = r11 * centerWeight + rSurround * surroundWeight

                val g11 = (p11 shr 8) and 0xFF
                val gSurround = (((p01 shr 8) and 0xFF) +
                                 ((p10 shr 8) and 0xFF) +
                                 ((p12 shr 8) and 0xFF) +
                                 ((p21 shr 8) and 0xFF))
                var g = g11 * centerWeight + gSurround * surroundWeight

                val b11 = p11 and 0xFF
                val bSurround = ((p01 and 0xFF) +
                                 (p10 and 0xFF) +
                                 (p12 and 0xFF) +
                                 (p21 and 0xFF))
                var b = b11 * centerWeight + bSurround * surroundWeight

                val a = p11 and 0xFF000000.toInt()

                if (r < 0f) r = 0f else if (r > 255f) r = 255f
                if (g < 0f) g = 0f else if (g > 255f) g = 255f
                if (b < 0f) b = 0f else if (b > 255f) b = 255f

                outputPixels[idx] = a or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
            }
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return outBitmap
    }

    /**
     * Erases unwanted objects, clutter, or people from a designated region.
     */
    fun removeObject(
        src: Bitmap,
        leftPercent: Float,
        topPercent: Float,
        widthPercent: Float,
        heightPercent: Float
    ): Bitmap {
        return eraseWatermark(src, leftPercent, topPercent, widthPercent, heightPercent)
    }

    /**
     * Erases unwanted objects using a freehand brush mask Bitmap (white pixels = erase).
     */
    fun removeObjectWithMask(src: Bitmap, mask: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        val srcPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        val isMasked = BooleanArray(width * height)
        var hasMask = false

        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val maskColor = maskPixels[idx]
                if ((maskColor and 0x00FFFFFF) != 0 || ((maskColor ushr 24) > 50)) {
                    isMasked[idx] = true
                    hasMask = true
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (!hasMask) return src

        val topUnmaskedY = IntArray(width * height)
        val bottomUnmaskedY = IntArray(width * height)
        val leftUnmaskedX = IntArray(width * height)
        val rightUnmaskedX = IntArray(width * height)

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val idx = y * width + x
                if (!isMasked[idx]) continue

                var ty = y - 1
                while (ty >= 0 && isMasked[ty * width + x]) {
                    ty--
                }
                topUnmaskedY[idx] = ty.coerceAtLeast(0)

                var by = y + 1
                while (by < height && isMasked[by * width + x]) {
                    by++
                }
                bottomUnmaskedY[idx] = by.coerceAtMost(height - 1)

                var lx = x - 1
                while (lx >= 0 && isMasked[y * width + lx]) {
                    lx--
                }
                leftUnmaskedX[idx] = lx.coerceAtLeast(0)

                var rx = x + 1
                while (rx < width && isMasked[y * width + rx]) {
                    rx++
                }
                rightUnmaskedX[idx] = rx.coerceAtMost(width - 1)
            }
        }

        val patchPixels = IntArray(width * height)

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val idx = y * width + x
                if (!isMasked[idx]) {
                    patchPixels[idx] = srcPixels[idx]
                    continue
                }

                val topY = topUnmaskedY[idx]
                val bottomY = bottomUnmaskedY[idx]
                val leftX = leftUnmaskedX[idx]
                val rightX = rightUnmaskedX[idx]

                val distTop = (y - topY + 1).toFloat()
                val distBottom = (bottomY - y + 1).toFloat()
                val distLeft = (x - leftX + 1).toFloat()
                val distRight = (rightX - x + 1).toFloat()

                val wT = 1f / (distTop * distTop)
                val wB = 1f / (distBottom * distBottom)
                val wL = 1f / (distLeft * distLeft)
                val wR = 1f / (distRight * distRight)

                val totalWeight = wT + wB + wL + wR

                val pT = srcPixels[topY * width + x]
                val pB = srcPixels[bottomY * width + x]
                val pL = srcPixels[y * width + leftX]
                val pR = srcPixels[rightX * width + x] // using right boundary

                val r = (((pT shr 16) and 0xFF) * wT + ((pB shr 16) and 0xFF) * wB +
                         ((pL shr 16) and 0xFF) * wL + ((pR shr 16) and 0xFF) * wR) / totalWeight
                val g = (((pT shr 8) and 0xFF) * wT + ((pB shr 8) and 0xFF) * wB +
                         ((pL shr 8) and 0xFF) * wL + ((pR shr 8) and 0xFF) * wR) / totalWeight
                val b = ((pT and 0xFF) * wT + (pB and 0xFF) * wB +
                         (pL and 0xFF) * wL + (pR and 0xFF) * wR) / totalWeight
                val a = (srcPixels[idx] and 0xFF000000.toInt())

                patchPixels[idx] = a or (r.toInt().coerceIn(0, 255) shl 16) or
                        (g.toInt().coerceIn(0, 255) shl 8) or b.toInt().coerceIn(0, 255)
            }
        }

        // Pass 2: Local 3x3 smoothing over inpainted mask region
        val outputPixels = srcPixels.clone()
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val idx = y * width + x
                if (!isMasked[idx]) continue

                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var count = 0

                for (dy in -1..1) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    for (dx in -1..1) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val p = patchPixels[ny * width + nx]
                        sumR += (p shr 16) and 0xFF
                        sumG += (p shr 8) and 0xFF
                        sumB += p and 0xFF
                        count++
                    }
                }

                val alpha = srcPixels[idx] and 0xFF000000.toInt()
                val finalR = (sumR / count).toInt().coerceIn(0, 255)
                val finalG = (sumG / count).toInt().coerceIn(0, 255)
                val finalB = (sumB / count).toInt().coerceIn(0, 255)

                outputPixels[idx] = alpha or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return outBitmap
    }

    /**
     * Applies skin tone smoothing (surface blur on skin colors) while keeping facial clarity.
     */
    fun faceRetouch(
        src: Bitmap,
        smoothPercent: Float,
        clarityPercent: Float
    ): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        val outputPixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val smoothRadius = (smoothPercent * 0.05f).toInt().coerceIn(1, 8)
        val threshold = 35.0f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val centerP = pixels[idx]
                val cr = (centerP shr 16) and 0xFF
                val cg = (centerP shr 8) and 0xFF
                val cb = centerP and 0xFF

                val isSkin = cr > 45 && cg > 25 && cb > 15 &&
                             cr > cg && cr > cb && Math.abs(cr - cg) > 10

                if (isSkin && smoothRadius > 0) {
                    var sumR = 0f
                    var sumG = 0f
                    var sumB = 0f
                    var weightSum = 0f

                    for (dy in -smoothRadius..smoothRadius) {
                        val ny = (y + dy).coerceIn(0, height - 1)
                        for (dx in -smoothRadius..smoothRadius) {
                            val nx = (x + dx).coerceIn(0, width - 1)
                            val p = pixels[ny * width + nx]
                            val r = (p shr 16) and 0xFF
                            val g = (p shr 8) and 0xFF
                            val b = p and 0xFF

                            val colorDiff = Math.sqrt(((r - cr) * (r - cr) + (g - cg) * (g - cg) + (b - cb) * (b - cb)).toDouble()).toFloat()
                            if (colorDiff < threshold) {
                                val weight = 1.0f - (colorDiff / threshold)
                                sumR += r * weight
                                sumG += g * weight
                                sumB += b * weight
                                weightSum += weight
                            }
                        }
                    }

                    if (weightSum > 0f) {
                        val finalR = (sumR / weightSum).toInt().coerceIn(0, 255)
                        val finalG = (sumG / weightSum).toInt().coerceIn(0, 255)
                        val finalB = (sumB / weightSum).toInt().coerceIn(0, 255)
                        val a = centerP and 0xFF000000.toInt()
                        outputPixels[idx] = a or (finalR shl 16) or (finalG shl 8) or finalB
                    } else {
                        outputPixels[idx] = centerP
                    }
                } else {
                    outputPixels[idx] = centerP
                }
            }
        }

        val smoothedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        smoothedBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)

        return enhance(smoothedBitmap, "original", 100f + (clarityPercent * 0.3f), 100f + (clarityPercent * 0.15f))
    }

    /**
     * Detects corner background color and removes background matching it within a tolerance.
     */
    fun removeBackground(src: Bitmap, tolerance: Int): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val cornerPixel = pixels[0]
        val cornerR = (cornerPixel shr 16) and 0xFF
        val cornerG = (cornerPixel shr 8) and 0xFF
        val cornerB = cornerPixel and 0xFF

        val threshold = tolerance * 2.5f
        val thresholdSq = threshold * threshold

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF

            val diffSq = (r - cornerR) * (r - cornerR) +
                         (g - cornerG) * (g - cornerG) +
                         (b - cornerB) * (b - cornerB)

            if (diffSq < thresholdSq) {
                pixels[i] = p and 0x00FFFFFF
            }
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outBitmap
    }

    /**
     * Rotates a bitmap by a specified angle and flips it horizontally if requested.
     */
    fun rotateAndFlip(src: Bitmap, degrees: Int, flipHorizontal: Boolean): Bitmap {
        val matrix = android.graphics.Matrix().apply {
            postRotate(degrees.toFloat())
            if (flipHorizontal) {
                postScale(-1f, 1f)
            }
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    /**
     * Erases watermark or unwanted objects using 4-boundary spatial interpolation
     * and edge smoothing to eliminate cloned box artifacts.
     */
    fun eraseWatermark(
        src: Bitmap,
        leftPercent: Float,
        topPercent: Float,
        widthPercent: Float,
        heightPercent: Float
    ): Bitmap {
        val width = src.width
        val height = src.height

        val maskX = (leftPercent * width).toInt().coerceIn(0, width - 1)
        val maskY = (topPercent * height).toInt().coerceIn(0, height - 1)
        val maskW = (widthPercent * width).toInt().coerceAtMost(width - maskX)
        val maskH = (heightPercent * height).toInt().coerceAtMost(height - maskY)

        if (maskW <= 0 || maskH <= 0) return src

        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val topY = (maskY - 1).coerceAtLeast(0)
        val bottomY = (maskY + maskH).coerceAtMost(height - 1)
        val leftX = (maskX - 1).coerceAtLeast(0)
        val rightX = (maskX + maskW).coerceAtMost(width - 1)

        val tempPatch = IntArray(maskW * maskH)

        for (y in maskY until (maskY + maskH)) {
            val distTop = (y - maskY + 1).toFloat()
            val distBottom = (maskY + maskH - y).toFloat()
            val wT = 1f / (distTop * distTop)
            val wB = 1f / (distBottom * distBottom)

            for (x in maskX until (maskX + maskW)) {
                val distLeft = (x - maskX + 1).toFloat()
                val distRight = (maskX + maskW - x).toFloat()
                val wL = 1f / (distLeft * distLeft)
                val wR = 1f / (distRight * distRight)

                val totalWeight = wT + wB + wL + wR

                val pT = pixels[topY * width + x]
                val pB = pixels[bottomY * width + x]
                val pL = pixels[y * width + leftX]
                val pR = pixels[y * width + rightX]

                val r = (((pT shr 16) and 0xFF) * wT + ((pB shr 16) and 0xFF) * wB +
                         ((pL shr 16) and 0xFF) * wL + ((pR shr 16) and 0xFF) * wR) / totalWeight
                val g = (((pT shr 8) and 0xFF) * wT + ((pB shr 8) and 0xFF) * wB +
                         ((pL shr 8) and 0xFF) * wL + ((pR shr 8) and 0xFF) * wR) / totalWeight
                val b = ((pT and 0xFF) * wT + (pB and 0xFF) * wB +
                         (pL and 0xFF) * wL + (pR and 0xFF) * wR) / totalWeight
                val a = (pT and 0xFF000000.toInt())

                val localIdx = (y - maskY) * maskW + (x - maskX)
                tempPatch[localIdx] = a or (r.toInt().coerceIn(0, 255) shl 16) or
                        (g.toInt().coerceIn(0, 255) shl 8) or b.toInt().coerceIn(0, 255)
            }
        }

        for (y in maskY until (maskY + maskH)) {
            for (x in maskX until (maskX + maskW)) {
                val ly = y - maskY
                val lx = x - maskX

                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var count = 0

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val ny = ly + dy
                        val nx = lx + dx
                        if (nx in 0 until maskW && ny in 0 until maskH) {
                            val p = tempPatch[ny * maskW + nx]
                            sumR += (p shr 16) and 0xFF
                            sumG += (p shr 8) and 0xFF
                            sumB += p and 0xFF
                            count++
                        }
                    }
                }

                val targetIdx = y * width + x
                val alpha = pixels[targetIdx] and 0xFF000000.toInt()
                val finalR = (sumR / count).toInt().coerceIn(0, 255)
                val finalG = (sumG / count).toInt().coerceIn(0, 255)
                val finalB = (sumB / count).toInt().coerceIn(0, 255)

                pixels[targetIdx] = alpha or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outBitmap
    }
}
