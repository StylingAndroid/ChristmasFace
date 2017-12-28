package com.stylingandroid.christmasface

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.support.v4.content.res.ResourcesCompat
import com.google.android.gms.vision.face.Face
import com.stylingandroid.christmasface.camera.GraphicOverlay

class FestiveGraphic(
        private val overlay: GraphicOverlay,
        private val resources: Resources = overlay.resources
) : GraphicOverlay.Graphic(overlay) {
    private var face: Face? = null

    @DrawableRes
    override var drawableId: Int = 0
        set(value) {
            value.takeIf { it != field }?.also {
                field = it
                drawable = ResourcesCompat.getDrawable(resources, it, overlay.context.theme)
            }
        }

    private var drawable: Drawable? = null

    fun updateFace(newFace: Face) {
        face = newFace
        postInvalidate()
    }

    override fun draw(canvas: Canvas) {
        canvas.save().also {
            face?.also {
                drawable?.apply {
                    draw(canvas, it)
                }
            }
            canvas.restoreToCount(it)
        }
    }

    private fun Drawable.draw(canvas: Canvas, face: Face) {
        half(face.width, face.height) { halfWidth, halfHeight ->
            bounds.left = (translateX(face.position.x + halfWidth) - scaleX(halfWidth)).toInt()
            bounds.right = (translateX(face.position.x + halfWidth) + scaleX(halfWidth)).toInt()
            bounds.top = (translateY(face.position.y + halfHeight) - scaleY(halfHeight)).toInt() - face.height.toInt()
            bounds.bottom = (translateY(face.position.y + halfHeight) + scaleY(halfHeight)).toInt() + halfHeight.toInt()

        }
        canvas.rotate(rotate(face.eulerZ), bounds.exactCenterX(), bounds.exactCenterY())
        draw(canvas)
    }

    private fun half(width: Float, height: Float, function: (halfWidth: Float, halfHeight: Float) -> Unit) {
        function(width / 2f, height / 2f)
    }
}
