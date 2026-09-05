package com.novacore.piximaxaiultra

import android.graphics.Bitmap
import com.novacore.piximaxaiultra.utils.ImageProcessor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageProcessorTest {

    @Test
    fun testEnhanceCreatesValidBitmap() {
        // Create a 100x100 dummy bitmap
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        assertNotNull(bitmap)

        // Enhance with conservative defaults
        val enhancedOriginal = ImageProcessor.enhance(bitmap, "original", 105f, 102f)
        assertNotNull(enhancedOriginal)
        assertEquals(100, enhancedOriginal.width)
        assertEquals(100, enhancedOriginal.height)

        // Enhance with 1:1 ratio
        val enhanced1To1 = ImageProcessor.enhance(bitmap, "1:1", 105f, 102f)
        assertEquals(100, enhanced1To1.width)
        assertEquals(100, enhanced1To1.height)

        // Enhance with 16:9 ratio
        val enhanced16To9 = ImageProcessor.enhance(bitmap, "16:9", 105f, 102f)
        assertEquals(100, enhanced16To9.width)
        assertEquals(56, enhanced16To9.height) // 100 * 9 / 16 = 56.25 -> 56
    }

    @Test
    fun testRotateAndFlip() {
        val bitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, android.graphics.Color.RED)

        // Rotate 90 degrees and flip horizontal
        val rotated = ImageProcessor.rotateAndFlip(bitmap, 90, true)
        assertNotNull(rotated)
        assertEquals(50, rotated.width) // rotated 90 deg width becomes original height
        assertEquals(100, rotated.height) // rotated 90 deg height becomes original width
    }

    @Test
    fun testRemoveBackground() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, android.graphics.Color.RED)
        bitmap.setPixel(5, 5, android.graphics.Color.RED)
        bitmap.setPixel(2, 2, android.graphics.Color.BLUE)

        val transparentBitmap = ImageProcessor.removeBackground(bitmap, 25)

        assertEquals(0, android.graphics.Color.alpha(transparentBitmap.getPixel(0, 0)))
        assertEquals(0, android.graphics.Color.alpha(transparentBitmap.getPixel(5, 5)))
        assertEquals(255, android.graphics.Color.alpha(transparentBitmap.getPixel(2, 2)))
    }

    @Test
    fun testEraseWatermarkAndRemoveObject() {
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        for (x in 0 until 20) {
            bitmap.setPixel(x, 2, android.graphics.Color.RED)
        }
        for (y in 5 until 10) {
            for (x in 5 until 15) {
                bitmap.setPixel(x, y, android.graphics.Color.GREEN)
            }
        }

        val erasedBitmap = ImageProcessor.eraseWatermark(
            src = bitmap,
            leftPercent = 0.25f,
            topPercent = 0.25f,
            widthPercent = 0.5f,
            heightPercent = 0.25f
        )

        assertEquals(android.graphics.Color.RED, erasedBitmap.getPixel(10, 7))

        val objectRemovedBitmap = ImageProcessor.removeObject(
            src = bitmap,
            leftPercent = 0.25f,
            topPercent = 0.25f,
            widthPercent = 0.5f,
            heightPercent = 0.25f
        )
        assertNotNull(objectRemovedBitmap)
    }

    @Test
    fun testFaceRetouch() {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val retouched = ImageProcessor.faceRetouch(bitmap, 40f, 20f)
        assertNotNull(retouched)
        assertEquals(50, retouched.width)
        assertEquals(50, retouched.height)
    }
}
