package com.novacore.piximaxaiultra.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.OutputStream

object ImageUtils {

    private const val MAX_IMAGE_DIMENSION = 2048

    /**
     * Safely recycles a bitmap if it's not null and not already recycled.
     */
    fun safeRecycle(bitmap: Bitmap?) {
        try {
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * Loads a mutable ARGB_8888 Bitmap from a Uri, downsampled to MAX_IMAGE_DIMENSION to avoid OOM errors.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = MAX_IMAGE_DIMENSION): Bitmap? {
        return try {
            var rawBitmap: Bitmap? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                rawBitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                    val width = info.size.width
                    val height = info.size.height
                    if (width > maxDimension || height > maxDimension) {
                        val maxSide = maxOf(width, height)
                        val sampleSize = (maxSide / maxDimension).coerceAtLeast(1)
                        decoder.setTargetSampleSize(sampleSize)
                    }
                }
            } else {
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream, null, options)
                }
                var sampleSize = 1
                if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                    val maxSide = maxOf(options.outWidth, options.outHeight)
                    sampleSize = (maxSide / maxDimension).coerceAtLeast(1)
                }
                val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inMutable = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    rawBitmap = android.graphics.BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }

            val finalBmp = rawBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            if (rawBitmap != null && rawBitmap != finalBmp) {
                safeRecycle(rawBitmap)
            }
            finalBmp
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a Bitmap to the public Pictures/PixiMax gallery using MediaStore.
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val resolver = context.contentResolver
        val imageDetails = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/PixiMax")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails) ?: return null

        return try {
            resolver.openOutputStream(imageUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imageDetails.clear()
                imageDetails.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, imageDetails, null, null)
            }
            imageUri
        } catch (e: Exception) {
            resolver.delete(imageUri, null, null)
            e.printStackTrace()
            null
        }
    }
}
