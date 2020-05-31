package covid.trace.morocco.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import covid.trace.morocco.R;

/**
 * I forked code from
 * https://github.com/marcokstephen/StepProgressBar
 */
public class StepProgressBar extends View {

    private Drawable inactiveDrawable;
    private Drawable activeDrawable;

    private float dashSpacing;

    private int maxNumDashes;
    private int currentlyActiveDash;
    private boolean cumulativeDashes;

    private static final int MIN_DOTS = -1;
    private static final String OUT_OF_BOUNDS_ERROR = "Progress bar out of bounds!";

    public StepProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.StepProgressBar, 0, 0);
        try {
            inactiveDrawable = ta.getDrawable(R.styleable.StepProgressBar_inactiveDotIcon);
            activeDrawable = ta.getDrawable(R.styleable.StepProgressBar_activeDotIcon);
            dashSpacing = ta.getDimensionPixelSize(R.styleable.StepProgressBar_spacing, 15);
            maxNumDashes = ta.getInt(R.styleable.StepProgressBar_numberDots, 5);
            currentlyActiveDash = ta.getInt(R.styleable.StepProgressBar_activeDotIndex, 0);
            cumulativeDashes = ta.getBoolean(R.styleable.StepProgressBar_cumulativeDots, false);
        } finally {
            ta.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = activeDrawable.getIntrinsicHeight() + getPaddingBottom() + getPaddingTop();
        setMeasuredDimension(widthMeasureSpec, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Centering the dots in the middle of the canvas
        float singleDotSize = dashSpacing + activeDrawable.getIntrinsicWidth();
        float combinedDotSize = singleDotSize * maxNumDashes - dashSpacing;
        int startingX = (int) ((getWidth() - combinedDotSize) / 2);

        for (int i = 0; i < maxNumDashes; i++) {
            int x = (int) (startingX + i * singleDotSize);
            if ((cumulativeDashes && i < currentlyActiveDash) || i == currentlyActiveDash) {
                activeDrawable.setBounds(x, 0, x + activeDrawable.getIntrinsicWidth(), activeDrawable.getIntrinsicHeight());
                activeDrawable.draw(canvas);
            } else {
                inactiveDrawable.setBounds(x, 0, x + inactiveDrawable.getIntrinsicWidth(), inactiveDrawable.getIntrinsicHeight());
                inactiveDrawable.draw(canvas);
            }
        }
    }

    public void setCurrentProgressDot(int i) {
        if (i >= maxNumDashes || i < MIN_DOTS) {
            throw new IndexOutOfBoundsException(OUT_OF_BOUNDS_ERROR);
        }
        currentlyActiveDash = i;
        invalidate();
    }

    public int getCurrentProgressDot() {
        return currentlyActiveDash;
    }

}
