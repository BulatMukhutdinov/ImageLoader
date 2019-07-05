package tat.mukhutdinov.imageloader.lib

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import tat.mukhutdinov.imageloader.lib.api.Resizer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import kotlin.math.min

class SimpleResizer : Resizer {

    override fun cropCircle(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val color = -0xbdbdbe
        val paint = Paint(ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)

        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2).toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    override fun decodeSampledBitmapFromNet(urlRequest: String, targetWidth: Int, targetHeight: Int): Bitmap? =
        BitmapFactory.Options()
            .run {
                val url = URL(urlRequest)
                url.openStream().use {
                    val output = ByteArrayOutputStream().apply { it.copyTo(this) }
                    val source = output.toByteArray()
                    val byteArrayInputStream = ByteArrayInputStream(source)

                    // First decode with inJustDecodeBounds=true to check dimensions
                    inJustDecodeBounds = true
                    BitmapFactory.decodeStream(byteArrayInputStream, null, this)

                    // Calculate inSampleSize
                    inSampleSize = calculateInSampleSize(this, targetWidth, targetHeight)

                    byteArrayInputStream.reset()

                    // Decode bitmap with inSampleSize set
                    inJustDecodeBounds = false

                    BitmapFactory.decodeStream(byteArrayInputStream, null, this)
                }
            }

    private fun calculateInSampleSize(options: BitmapFactory.Options, targetWidth: Int, targetHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > targetHeight || width > targetWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= targetHeight && halfWidth / inSampleSize >= targetWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}