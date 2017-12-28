package com.stylingandroid.christmasface

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.support.v4.content.res.ResourcesCompat
import android.util.Log
import android.util.SparseArray
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

const val ASSET_NAME = "image.jpg"

class BitmapGenerator(
        private val context: Context,
        @DrawableRes private val drawableId: Int,
        private val width: Int,
        private val height: Int,
        private val orientationFactor: Float = 1f) {

    private val cacheFile: File by lazyFast {
        File(context.filesDir, ASSET_NAME)
    }

    private val isPortrait: Boolean
        get() = height > width

    private val Bitmap.isPortrait: Boolean
        get() = height > width

    private val drawable: Drawable? by lazyFast {
        ResourcesCompat.getDrawable(context.resources, drawableId, context.theme)
    }

    private var scaleFactor: Float = 1f

    suspend fun convert(bytes: ByteArray): Bitmap = async(CommonPool) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                .rotateIfNecessary().let { newBitmap ->
            newBitmap.detectFace()?.let { face ->
                newBitmap.createOverlaidBitmap(face)
            } ?: newBitmap
        }.also {
            writeCacheFile(it)
        }
    }.await()

    private fun Bitmap.createOverlaidBitmap(face: Face): Bitmap =
            Bitmap.createBitmap(width, height, config).apply {
                Canvas(this).apply {
                    drawBitmap(this@createOverlaidBitmap, 0f, 0f, null)
                    scale(1f / scaleFactor, 1f / scaleFactor)
                    drawable?.draw(this, face)
                }
            }

    private fun Drawable.draw(canvas: Canvas, face: Face) {
        bounds.left = (face.position.x).toInt()
        bounds.right = (face.position.x + face.width).toInt()
        bounds.top = (face.position.y).toInt() - (face.height / 4).toInt()
        bounds.bottom = (face.position.y + face.height).toInt() + (face.height.toInt() / 8)
        canvas.rotate(-face.eulerZ, bounds.exactCenterX(), bounds.exactCenterY())
        draw(canvas)
    }

    private fun Bitmap.detectFace(): Face? =
            createFaceDetector().run {
                detect(Frame.Builder().setBitmap(scale()).build())?.first().apply {
                    release()
                }
            }

    private fun createFaceDetector(): FaceDetector =
            FaceDetector.Builder(context)
                    .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .build()

    private fun Bitmap.rotateIfNecessary(): Bitmap =
            if (shouldRotate(this)) {
                rotate()
            } else {
                this
            }

    private fun shouldRotate(bitmap: Bitmap): Boolean =
            isPortrait != bitmap.isPortrait

    private fun Bitmap.rotate(): Bitmap =
            Bitmap.createBitmap(height, width, config).apply {
                Canvas(this).apply {
                    rotate(90f * orientationFactor)
                    matrix = Matrix().apply {
                        if (orientationFactor > 0f) {
                            postTranslate(0f, -this@rotate.height.toFloat())
                        } else {
                            postTranslate(-this@rotate.width.toFloat(), 0f)
                        }
                    }.also {
                        drawBitmap(this@rotate, it, null)
                    }
                }
            }

    private fun Bitmap.scale(): Bitmap {
        scaleFactor = Math.min(640f / Math.max(width, height).toFloat(), 1f)
        return if (scaleFactor < 1f) {
            Bitmap.createBitmap((width * scaleFactor).toInt(), (height * scaleFactor).toInt(), config).also {
                Canvas(it).apply {
                    scale(scaleFactor, scaleFactor)
                    drawBitmap(this@scale, 0f, 0f, null)
                }
            }
        } else {
            this
        }
    }

    private fun writeCacheFile(bitmap: Bitmap): Boolean {
        var outputStream: OutputStream? = null
        return try {
            outputStream = FileOutputStream(cacheFile)
            bitmap.toJpeg().apply {
                outputStream.write(this, 0, size)
            }
            true
        } catch (e: IOException) {
            Log.e(BitmapGenerator::class.java.name, "Error writing to cache file", e)
            false
        } finally {
            outputStream?.close()
        }
    }

    private fun Bitmap.toJpeg(): ByteArray =
            ByteArrayOutputStream().run {
                compress(Bitmap.CompressFormat.JPEG, 80, this)
                toByteArray()
            }

    private fun <T> SparseArray<T>.first(): T? =
            takeIf { it.size() > 0 }?.get(keyAt(0))

}
