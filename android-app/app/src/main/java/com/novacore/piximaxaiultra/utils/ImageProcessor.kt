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
     * Matches the Web App's "Enhance" filter logic.
     */
    fun enhance(
        bitmap: Bitmap,
        ratio: String,
        sharpnessPercent: Float,
        contrastPercent: Float
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
        }

        val contrastScale = contrastPercent / 100f
        val brightnessScale = 1.02f // matches Web App "brightness(102%)"
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

        // Now apply sharpening kernel
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

        // Calculate kernel strength based on percentage (100% to 200%)
        val k = ((sharpnessPercent - 100f) / 100f) * 0.25f
        if (k <= 0f) return src

        val centerWeight = 1f + 4f * k
        val surroundWeight = -k

        // Copy edge pixels directly
        for (x in 0 until width) {
            outputPixels[x] = pixels[x]
            outputPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            outputPixels[y * width] = pixels[y * width]
            outputPixels[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }

        // Apply 3x3 kernel
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x

                // Surrounding pixels
                val p01 = pixels[idx - width]     // top
                val p10 = pixels[idx - 1]         // left
                val p11 = pixels[idx]             // center
                val p12 = pixels[idx + 1]         // right
                val p21 = pixels[idx + width]     // bottom

                // Red
                val r11 = (p11 shr 16) and 0xFF
                val rSurround = (((p01 shr 16) and 0xFF) +
                                 ((p10 shr 16) and 0xFF) +
                                 ((p12 shr 16) and 0xFF) +
                                 ((p21 shr 16) and 0xFF))
                var r = r11 * centerWeight + rSurround * surroundWeight

                // Green
                val g11 = (p11 shr 8) and 0xFF
                val gSurround = (((p01 shr 8) and 0xFF) +
                                 ((p10 shr 8) and 0xFF) +
                                 ((p12 shr 8) and 0xFF) +
                                 ((p21 shr 8) and 0xFF))
                var g = g11 * centerWeight + gSurround * surroundWeight

                // Blue
                val b11 = p11 and 0xFF
                val bSurround = ((p01 and 0xFF) +
                                 (p10 and 0xFF) +
                                 (p12 and 0xFF) +
                                 (p21 and 0xFF))
                var b = b11 * centerWeight + bSurround * surroundWeight

                val a = p11 and 0xFF000000.toInt()

                // Clamp to [0, 255]
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
     * Detects corner background color and removes background matching it within a tolerance.
     * Matches Web App's "Background Removal" logic.
     */
    fun removeBackground(src: Bitmap, tolerance: Int): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Top-left pixel as background color
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
                // Set alpha to 0 (make transparent)
                pixels[i] = p and 0x00FFFFFF
            }
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outBitmap
    }

    /**
     * Rotates a bitmap by a specified angle and flips it horizontally if requested.
     * Matches Web App's "Rotate & Flip" logic.
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
     * Smudges out watermark region using colors sampled above it.
     * Matches Web App's "Watermark Eraser" logic.
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

        val sampleY = (maskY - 3).coerceAtLeast(0)

        for (y in maskY until (maskY + maskH)) {
            for (x in maskX until (maskX + maskW)) {
                val targetIdx = y * width + x
                val sampleIdx = sampleY * width + x
                pixels[targetIdx] = pixels[sampleIdx]
            }
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outBitmap
    }
}
