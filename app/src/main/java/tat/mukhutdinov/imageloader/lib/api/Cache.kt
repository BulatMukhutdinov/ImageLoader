package tat.mukhutdinov.imageloader.lib.api

import android.graphics.Bitmap

interface Cache {

    fun init()

    operator fun get(key: String): Bitmap?

    operator fun set(key: String, value: Bitmap)
}