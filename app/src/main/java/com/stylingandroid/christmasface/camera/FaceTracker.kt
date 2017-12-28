package com.stylingandroid.christmasface.camera

import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.stylingandroid.christmasface.FestiveGraphic

class FaceTracker(
        private val overlay: GraphicOverlay,
        private val faceGraphic: FestiveGraphic = FestiveGraphic(overlay)
) : Tracker<Face>() {

    var drawableId: Int
        get() = faceGraphic.drawableId
        set(value) {
            faceGraphic.drawableId = value
        }

    override fun onUpdate(detections: Detector.Detections<Face>, face: Face) {
        if (detections.detectorIsOperational()) {
            overlay.add(faceGraphic)
            faceGraphic.updateFace(face)
        }
    }

    override fun onMissing(detections: Detector.Detections<Face>) =
            overlay.remove(faceGraphic)

    override fun onDone() =
            overlay.remove(faceGraphic)

}
