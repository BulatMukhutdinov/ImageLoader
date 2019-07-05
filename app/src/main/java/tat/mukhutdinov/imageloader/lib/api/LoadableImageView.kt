package tat.mukhutdinov.imageloader.lib.api

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import tat.mukhutdinov.imageloader.R
import tat.mukhutdinov.imageloader.lib.ScopedImageLoader

class LoadableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ImageView(context, attrs, defStyle), LoadResult {

    private val paint = Paint()

    private var loadJob: Job? = null

    private var isLoading = false

    init {
        paint.color = ContextCompat.getColor(context, R.color.colorAccent)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
    }

    fun load(url: String, placeholder: Int? = null) {
        isLoading = true
        invalidate()

        loadJob = ScopedImageLoader.instance.load(
            url,
            this@LoadableImageView,
            placeholder,
            width,
            height,
            this@LoadableImageView
        )
    }

    override fun onSuccess() {
        isLoading = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadJob?.cancel()
    }
}