package com.sdk.billinglibrary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

public class CrossedTextView extends androidx.appcompat.widget.AppCompatTextView {

    private Paint paint;

    public CrossedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public CrossedTextView(Context context) {
        super(context);
        init();
    }

    public CrossedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(getResources().getDisplayMetrics().density * 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawLine(0, getHeight()/2, getWidth(), getHeight()/2, paint);
    }

}
