package com.ssccscanner.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Decode [uri] scaled to roughly [maxWidth] px on the long edge — OCR wants
     * resolution but not the full 12MP frame — honoring EXIF rotation.
     */
    fun loadScaled(context: Context, uri: Uri, maxWidth: Int = 1400): Bitmap? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while ((maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2)) >= maxWidth) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        val rotation = resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        return if (rotation != 0f) {
            val m = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        } else {
            bitmap
        }
    }

    /**
     * Small JPEG thumbnail under [byteBudget] — the only image ever persisted,
     * mirroring the prototype's ~15KB budget.
     */
    fun thumbnailJpeg(source: Bitmap, byteBudget: Int = 15_000): ByteArray {
        var width = 360
        var quality = 70
        while (true) {
            val scale = width.toFloat() / source.width
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(source, width, (source.height * scale).toInt().coerceAtLeast(1), true)
            } else {
                source
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            if (bytes.size <= byteBudget || (width <= 160 && quality <= 30)) return bytes
            if (quality > 30) quality -= 15 else width = (width * 3) / 4
        }
    }
}
