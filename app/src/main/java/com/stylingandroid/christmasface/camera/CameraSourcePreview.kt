/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stylingandroid.christmasface.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.google.android.gms.vision.CameraSource
import com.stylingandroid.christmasface.forEach
import java.io.IOException

private const val TAG = "CameraSourcePreview"

class CameraSourcePreview(context: Context, attrs: AttributeSet) : ViewGroup(context, attrs) {
    private val surfaceView: SurfaceView
    private var startRequested: Boolean = false
    private var surfaceAvailable: Boolean = false
    private var cameraSource: CameraSource? = null
        set(value) {
            field = value
            startRequested = true
            startIfReady()
        }

    private var overlay: GraphicOverlay? = null

    init {
        startRequested = false
        surfaceAvailable = false

        surfaceView = SurfaceView(context)
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView)
    }

    @Throws(IOException::class)
    fun start(newCameraSource: CameraSource, newOverlay: GraphicOverlay) {
        overlay = newOverlay
        cameraSource = newCameraSource
    }

    fun stop() =
        cameraSource?.stop()

    fun release() {
        cameraSource?.release()
        cameraSource = null
    }

    @Throws(IOException::class)
    @SuppressLint("MissingPermission")
    private fun startIfReady() {
        if (startRequested && surfaceAvailable) {
            cameraSource?.apply {
                start(surfaceView.holder)
                size { width, height ->
                    overlay?.setCameraInfo(width, height, cameraFacing)
                }
            }
            overlay?.clear()
            startRequested = false
        }
    }

    private fun CameraSource.size(function: (width: Int, height: Int) -> Unit) =
            previewSize.also { size ->
                if (isPortrait) {
                    function(Math.min(size.width, size.height), Math.max(size.width, size.height))
                } else {
                    function(Math.max(size.width, size.height), Math.min(size.width, size.height))
                }
            }

    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(surface: SurfaceHolder) {
            surfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
            }

        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        previewSize { previewWidth, previewHeight ->
            viewSize { viewWidth, viewHeight ->
                ratio(previewWidth, previewHeight, viewWidth, viewHeight) { widthRatio, heightRatio ->
                    layoutChildren(previewWidth, previewHeight, viewWidth, viewHeight, widthRatio, heightRatio)
                }
            }
        }

        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(TAG, "Could not start camera source.", e)
        }
    }

    private fun previewSize(function: (previewWidth: Int, previewHeight: Int) -> Unit) =
            correctPreviewRotation(cameraSource?.previewSize?.width ?: 640,
                    cameraSource?.previewSize?.height ?: 480, function)

    private fun correctPreviewRotation(width: Int, height: Int, function: (width: Int, height: Int) -> Unit) =
            if (isPortrait) {
                function(height, width)
            } else {
                function(width, height)
            }

    private fun viewSize(function: (viewWidth: Int, viewHeight: Int) -> Unit) =
            function(right - left, bottom - top)

    private fun ratio(previewWidth: Int,
                      previewHeight: Int,
                      viewWidth: Int,
                      viewHeight: Int,
                      function: (widthRatio: Float, heightRatio: Float) -> Unit) {
        function(viewWidth.toFloat() / previewWidth.toFloat(),
                viewHeight.toFloat() / previewHeight.toFloat())
    }

    private fun layoutChildren(previewWidth: Int,
                               previewHeight: Int,
                               viewWidth: Int,
                               viewHeight: Int,
                               widthRatio: Float,
                               heightRatio: Float) {
        if (widthRatio > heightRatio) {
            (previewHeight.toFloat() * widthRatio).toInt().also { childHeight ->
                layoutChildren(0, (childHeight - viewHeight) / 2, viewWidth, childHeight)
            }
        } else {
            (previewWidth.toFloat() * heightRatio).toInt().also { childWidth ->
                layoutChildren((childWidth - viewWidth) / 2, 0, childWidth, viewHeight)
            }
        }
    }

    private fun layoutChildren(childXOffset: Int, childYOffset: Int, childWidth: Int, childHeight: Int) =
            forEach {
                it.layout(-childXOffset, -childYOffset, childWidth - childXOffset, childHeight - childYOffset)
            }

    private val isPortrait: Boolean =
            context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}
