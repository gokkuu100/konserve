package com.example.konserve

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat

class CurvedImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val path = Path()
    private val paint = Paint()

    init {
        paint.isAntiAlias = true
        paint.color = ContextCompat.getColor(context, android.R.color.white)
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // Adjust these values to change the curvature
        val curveHeight = height * 0.2f  // This is the height of the curve
        val curveControlPointX = width / -7.8f  // Midpoint for the control point (x-axis)
        val curveControlPointY = height - curveHeight  // Control point (y-axis)

        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(0f, height)
        path.quadTo(curveControlPointX, curveControlPointY, width, height)
        path.lineTo(width, 0f)
        path.close()

        canvas.clipPath(path)
        super.onDraw(canvas)
    }
}