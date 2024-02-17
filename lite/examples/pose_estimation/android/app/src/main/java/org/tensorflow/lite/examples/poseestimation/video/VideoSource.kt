package org.tensorflow.lite.examples.poseestimation.video

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import android.view.SurfaceView
import androidx.core.app.ActivityCompat.startActivityForResult
import org.tensorflow.lite.examples.poseestimation.camera.CameraSource

class VideoSource(
    private val surfaceView: SurfaceView,
    // private val listener: VideoListener? = null
    private val PICK_VIDEO_REQUEST: Int = 1
) {
    init {
        println("Open VideoSource")
        openGallery()
    }

    private fun openGallery() {
        val activity: Activity = Activity()
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(activity, intent, PICK_VIDEO_REQUEST, null)
    }


//    interface VideoListener {
//        fun onFPSListener(fps: Int)
//
//        fun onDetectedInfo(personScore: Float?, poseLabels: List<Pair<String, Float>>?)
//    }

}