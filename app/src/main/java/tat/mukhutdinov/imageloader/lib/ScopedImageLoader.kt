package tat.mukhutdinov.imageloader.lib

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.annotation.DrawableRes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tat.mukhutdinov.imageloader.lib.api.Cache
import tat.mukhutdinov.imageloader.lib.api.LoadResult
import tat.mukhutdinov.imageloader.lib.api.Resizer
import timber.log.Timber
import java.net.URL

/**
 * Known issues:
 * 1) Cached image would be used even if a new target has a different size
 * 2)
 */
class ScopedImageLoader private constructor() : CoroutineScope by MainScope() {

    var cache: Cache? = null

    var resizer: Resizer? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
    }

    init {
        launch(Dispatchers.IO) {
            cache?.init()
        }
    }

    fun setCache(cache: Cache): ScopedImageLoader {
        this.cache = cache
        return this
    }

    fun setResizer(resizer: Resizer): ScopedImageLoader {
        this.resizer = resizer
        return this
    }

    fun load(
        url: String,
        into: ImageView,
        @DrawableRes placeholderRes: Int?,
        targetWidth: Int,
        targetHeight: Int,
        callback: LoadResult? = null
    ): Job {
        return launch(exceptionHandler) {
            placeholderRes?.let {
                val placeholder = BitmapFactory.decodeResource(into.context.resources, placeholderRes)
                into.setImageBitmap(resizer?.cropCircle(placeholder) ?: placeholder)
            }

            val bitmap = withContext(Dispatchers.IO) {
                val cache = cache

                var image: Bitmap?

                image = cache?.get(url)

                if (image == null) {
                    image = onCacheMissed(resizer, url, targetWidth, targetHeight)
                }

                image?.let { resizer?.cropCircle(it) }
            }

            Timber.e("FINISH")

            into.setImageBitmap(bitmap)

            callback?.onSuccess()
        }
    }

    private fun onCacheMissed(resizer: Resizer?, url: String, targetWidth: Int, targetHeight: Int) =
        if (resizer != null) {
            resizer.decodeSampledBitmapFromNet(url, targetWidth, targetHeight)
                ?.also { cache?.set(url, it) }
        } else {
            val resolvedUrl = URL(url)
            resolvedUrl.openStream()
                .use {
                    BitmapFactory.decodeStream(it)
                }
                ?.also { cache?.set(url, it) }
        }

    companion object {
        val instance = ScopedImageLoader()
    }
}