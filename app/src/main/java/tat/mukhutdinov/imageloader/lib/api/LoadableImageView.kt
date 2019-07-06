package tat.mukhutdinov.imageloader.lib.api

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import tat.mukhutdinov.imageloader.R
import kotlin.math.min

class LoadableImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : ImageView(context, attrs, defStyle), LoadResult {

    private val paint = Paint()
    private val path = Path()
    private val oval = RectF()

    private var loadJob: Job? = null

    private var isLoading = false

    private var angleDelta = 0f

    init {
        paint.color = ContextCompat.getColor(context, R.color.colorAccent)
        paint.strokeWidth = 5f
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
        angleDelta = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isLoading) {
            val radius: Float = min(width, height) / 2f

            path.addCircle(width / 2f, height / 2f, radius, Path.Direction.CW)

            paint.style = Paint.Style.FILL

            val centerX: Float = width / 2f
            val centerY: Float = height / 2f
            paint.style = Paint.Style.STROKE

            oval.set(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius)

            canvas.drawArc(oval, 90f + angleDelta, 135f, false, paint)

            angleDelta += 3
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadJob?.cancel()
    }
}