package covid.trace.morocco.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import covid.trace.morocco.R

/**
 * I forked code from
 * https://github.com/marcokstephen/StepProgressBar
 */
class StepProgressBar(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    companion object {
        private const val MIN_DOTS = -1
        private const val OUT_OF_BOUNDS_ERROR = "Progress bar out of bounds!"
    }

    private var inactiveDrawable: Drawable? = null
    private var activeDrawable: Drawable? = null
    private var dashSpacing = 0f
    private var maxNumDashes = 0
    private var currentlyActiveDash = 0
    private var cumulativeDashes = false

    var currentProgressDot: Int
        get() = currentlyActiveDash
        set(i) {
            if (i >= maxNumDashes || i < MIN_DOTS) {
                throw IndexOutOfBoundsException(OUT_OF_BOUNDS_ERROR)
            }
            currentlyActiveDash = i
            invalidate()
        }

    init {
        val ta = context.theme.obtainStyledAttributes(attrs, R.styleable.StepProgressBar, 0, 0)
        try {
            inactiveDrawable = ta.getDrawable(R.styleable.StepProgressBar_inactiveDotIcon)
            activeDrawable = ta.getDrawable(R.styleable.StepProgressBar_activeDotIcon)
            dashSpacing = ta.getDimensionPixelSize(R.styleable.StepProgressBar_spacing, 15).toFloat()
            maxNumDashes = ta.getInt(R.styleable.StepProgressBar_numberDots, 5)
            currentlyActiveDash = ta.getInt(R.styleable.StepProgressBar_activeDotIndex, 0)
            cumulativeDashes = ta.getBoolean(R.styleable.StepProgressBar_cumulativeDots, false)
        } finally {
            ta.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = activeDrawable!!.intrinsicHeight + paddingBottom + paddingTop
        setMeasuredDimension(widthMeasureSpec, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Centering the dots in the middle of the canvas
        val singleDotSize = dashSpacing + activeDrawable!!.intrinsicWidth
        val combinedDotSize = singleDotSize * maxNumDashes - dashSpacing
        val startingX = ((width - combinedDotSize) / 2).toInt()
        for (i in 0 until maxNumDashes) {
            val x = (startingX + i * singleDotSize).toInt()
            if (cumulativeDashes && i < currentlyActiveDash || i == currentlyActiveDash) {
                activeDrawable!!.setBounds(
                    x,
                    0,
                    x + activeDrawable!!.intrinsicWidth,
                    activeDrawable!!.intrinsicHeight
                )
                activeDrawable!!.draw(canvas)
            } else {
                inactiveDrawable!!.setBounds(
                    x,
                    0,
                    x + inactiveDrawable!!.intrinsicWidth,
                    inactiveDrawable!!.intrinsicHeight
                )
                inactiveDrawable!!.draw(canvas)
            }
        }
    }
}