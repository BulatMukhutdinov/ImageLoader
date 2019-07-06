package tat.mukhutdinov.imageloader.lib.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.URL


/**
 * Known issues:
 * 1) Cached image would be used even if a new target has a different size
 * 2)
 */
class ScopedImageLoader private constructor() : CoroutineScope by MainScope() {

    private var cache: Cache? = null

    private var resizer: Resizer? = null

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
            var croppedPlaceholder: Bitmap? = null
            placeholderRes?.let {
                val placeholder = BitmapFactory.decodeResource(into.context.resources, placeholderRes)
                croppedPlaceholder = resizer?.cropCircle(placeholder) ?: placeholder
                into.setImageBitmap(croppedPlaceholder)
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

            into.setImageBitmap(bitmap)

            if (croppedPlaceholder != null) {
                val layers = arrayOfNulls<Drawable>(2)
                layers[0] = BitmapDrawable(into.resources, croppedPlaceholder)
                layers[1] = BitmapDrawable(into.resources, bitmap)

                val transitionDrawable = TransitionDrawable(layers)
                into.setImageDrawable(transitionDrawable)
                transitionDrawable.startTransition(300)
            }

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