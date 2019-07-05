package tat.mukhutdinov.imageloader.lib.api

import android.graphics.Bitmap

interface Resizer {

    fun decodeSampledBitmapFromNet(urlRequest: String, targetWidth: Int, targetHeight: Int): Bitmap?

    fun cropCircle(bitmap: Bitmap): Bitmap
}