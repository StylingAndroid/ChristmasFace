package com.stylingandroid.christmasface

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.media.MediaActionSound
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor
import com.stylingandroid.christmasface.camera.FaceTracker
import kotlinx.android.synthetic.main.fragment_scanner.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.util.concurrent.TimeUnit

private const val RC_HANDLE_GMS = 9001

class ScannerFragment : Fragment(), PersistentState.Consumer {

    private var faceTracker: FaceTracker? = null

    private var detector: Detector<Face>? = null
        get() =
            if (field != null) {
                field
            } else {
                if (scanner_overlay != null) {
                    faceTracker = FaceTracker(scanner_overlay).apply {
                        drawableId = persistentState?.drawableId ?: R.drawable.ic_santa_beard_hat
                    }
                    field = FaceDetector.Builder(context)
                            .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                            .build()
                            .apply {
                                setProcessor(LargestFaceFocusingProcessor(this, faceTracker))
                            }
                }
                field
            }
        set(value) {
            field?.apply {
                release()
            }
            field = value
        }

    private val googleApiAvailability = GoogleApiAvailability.getInstance()

    private var cameraSource: CameraSource? = null

    override var persistentState: PersistentState? = null

    private var toggleOperationalState = false
    private var logOperationalDetector = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_scanner, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        camera_take_picture?.setOnClickListener {
            takePicture()
        }
    }

    private fun createCameraSource(cameraFacing: Int) {
        stopCameraSource()
        detector = null
        cameraSource = CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(cameraFacing)
                .setRequestedFps(3f)
                .build()
    }

    private fun takePicture() {
        camera_take_picture?.isEnabled = false
        camera_take_picture?.apply {
            (drawable as? Animatable)?.start()
        }
        cameraSource?.takePicture(shutterCallback, pictureCallback)
    }

    private val shutterCallback = CameraSource.ShutterCallback {
        MediaActionSound().apply {
            play(MediaActionSound.SHUTTER_CLICK)
            release()
        }
    }

    private val pictureCallback = CameraSource.PictureCallback { bytes ->
        context?.also {
            persistentState?.apply {
                BitmapGenerator(it, drawableId, preview.width, preview.height, orientationFactor).apply {
                    async (CommonPool) {
                        convert(bytes).apply {
                            activity?.setBitmap(this)
                            async(UI) {
                                (camera_take_picture?.drawable as? Animatable)?.stop()
                                camera_take_picture?.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    private val orientationFactor: Float
        get() = if (cameraSource?.cameraFacing == CameraSource.CAMERA_FACING_FRONT) {
            -1f
        } else {
            1f
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_scanner, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu?.findItem(R.id.decoration_type)?.also {
            if (toggleOperationalState) {
                it.isEnabled = detector?.isOperational ?: false
                toggleOperationalState = false
            }
            it.icon = ResourcesCompat.getDrawable(resources, getTypeMenuId(), context?.theme)
        }
        menu?.findItem(R.id.camera_facing)?.also {
            it.icon = ResourcesCompat.getDrawable(resources, getCameraMenuId(), context?.theme)
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            item.run {
                when (itemId) {
                    R.id.decoration_type -> {
                        toggleType()
                        activity?.invalidateOptionsMenu()
                        true
                    }
                    R.id.camera_facing -> {
                        toggleCamera()
                        activity?.invalidateOptionsMenu()
                        true
                    }
                    else -> true
                }
            }

    private fun getTypeMenuId(): Int =
            if (persistentState?.drawableId == R.drawable.ic_elf_ear_hat) {
                R.drawable.ic_santa_claus
            } else {
                R.drawable.ic_elf
            }

    private fun toggleType() {
        persistentState?.apply {
            drawableId = if (drawableId == R.drawable.ic_santa_beard_hat) {
                R.drawable.ic_elf_ear_hat
            } else {
                R.drawable.ic_santa_beard_hat
            }
            async(CommonPool) {
                faceTracker?.drawableId = drawableId
            }
        }
    }

    private fun getCameraMenuId() =
            if (persistentState?.cameraFacing == CameraSource.CAMERA_FACING_FRONT) {
                R.drawable.ic_camera_rear
            } else {
                R.drawable.ic_camera_front
            }

    private fun toggleCamera() {
        persistentState?.apply {
            cameraFacing = if (cameraFacing == CameraSource.CAMERA_FACING_FRONT) {
                CameraSource.CAMERA_FACING_BACK
            } else {
                CameraSource.CAMERA_FACING_FRONT
            }
            createCameraSource(cameraFacing)
            startCameraSource()
        }
    }

    override fun onResume() {
        super.onResume()
        persistentState?.apply {
            createCameraSource(cameraFacing)
            startCameraSource()
            faceTracker?.drawableId = drawableId
        }
    }

    private fun checkPlayServices(function: () -> Unit) =
            googleApiAvailability.isGooglePlayServicesAvailable(context).also {
                when (it) {
                    ConnectionResult.SUCCESS -> function()
                    else -> googleApiAvailability.getErrorDialog(activity, it, RC_HANDLE_GMS)
                }
            }

    private fun startCameraSource() {
        checkPlayServices {
            cameraSource?.also {
                preview.start(it, scanner_overlay)
                detectorCheck()
            }
        }
    }

    private fun detectorCheck() {
        if (detector?.isOperational == false) {
            if (!logOperationalDetector) {
                logOperationalDetector = true
                if (isResumed) {
                    TransitionManager.beginDelayedTransition(fragment_scanner)
                    not_operational.visibility = View.VISIBLE
                    camera_take_picture.visibility = View.GONE
                    toggleOperationalState = true
                    activity?.invalidateOptionsMenu()
                }
            }
            preview?.postDelayed(::detectorCheck, TimeUnit.SECONDS.toMillis(1))
        } else if (logOperationalDetector) {
            logOperationalDetector = false
            if (isResumed) {
                TransitionManager.beginDelayedTransition(fragment_scanner)
                not_operational.visibility = View.GONE
                camera_take_picture.visibility = View.VISIBLE
                toggleOperationalState = true
                activity?.invalidateOptionsMenu()
            }
        }
    }

    private fun stopCameraSource() {
        cameraSource?.apply {
            preview?.stop()
            stop()
            release()
            detector?.release()
            cameraSource = null
            faceTracker = null
            detector = null
        }
    }

    override fun onPause() {
        stopCameraSource()
        super.onPause()
    }

    private fun Activity.setBitmap(bitmap: Bitmap) {
        if (this is BitmapConsumer) {
            handleBitmap(bitmap)
        }
    }

    interface BitmapConsumer {
        fun handleBitmap(bitmap: Bitmap)
    }
}
