package com.sdk.billinglibrary

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class CrossedTextView : AppCompatTextView {

    private var paint: Paint? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {
        paint = Paint()
        paint!!.color = Color.RED
        paint!!.strokeWidth = resources.displayMetrics.density * 1
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(0f,
            (height / 2).toFloat(),
            width.toFloat(),
            (height / 2).toFloat(),
            paint!!)
    }
}
