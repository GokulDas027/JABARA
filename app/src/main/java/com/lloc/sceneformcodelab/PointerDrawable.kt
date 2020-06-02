package com.lloc.sceneformcodelab

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable


class PointerDrawable : Drawable() {
    private val paint = Paint()
    private var enabled= false

    fun isEnabled(): Boolean {
        return enabled
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun draw(canvas: Canvas) {
        val cx: Float = canvas.width.toFloat() / 2
        val cy: Float = canvas.height.toFloat() / 2
        if (enabled) {
            paint.color = Color.GREEN
            canvas.drawCircle(cx, cy, 10.toFloat(), paint)
        } else {
            paint.color = Color.GRAY
            canvas.drawText("X", cx, cy, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOpacity(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}