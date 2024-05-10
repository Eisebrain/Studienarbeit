package org.tensorflow.lite.examples.poseestimation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.poseestimation.data.Device
import org.tensorflow.lite.examples.poseestimation.ml.ModelType
import org.tensorflow.lite.examples.poseestimation.ml.MoveNet
import org.tensorflow.lite.examples.poseestimation.ml.MoveNetMultiPose
import org.tensorflow.lite.examples.poseestimation.ml.PoseNet
import org.tensorflow.lite.examples.poseestimation.ml.Type
import org.tensorflow.lite.examples.poseestimation.navigation.SelectionActivity
import org.tensorflow.lite.examples.poseestimation.tracker.SpineTracker
import org.tensorflow.lite.examples.poseestimation.video.VideoHPE

class VideoActivity : AppCompatActivity() {

    // surface view to display video
    private lateinit var surfaceView: SurfaceView

    /** Default pose estimation model is 1 (MoveNet Thunder)
     * 0 == MoveNet Lightning model
     * 1 == MoveNet Thunder model
     * 2 == MoveNet MultiPose model
     * 3 == PoseNet model
     **/
    private var modelPos = 1

    /** Default device is CPU */
    private var device = Device.CPU

    private var videoHPE: VideoHPE? = null


    // buttons to switch between activities
    private lateinit var btnAbord: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        /** A [SurfaceView] for video preview.   */
        surfaceView = findViewById(R.id.surfaceViewVideo)

        btnAbord = findViewById(R.id.btnAbord)


        btnAbord.setOnClickListener {
            onPause()
            val i = Intent(this@VideoActivity, SelectionActivity::class.java)
            startActivity(i)
        }
        openVideo()
    }

    override fun onPause() {
        videoHPE?.close()
        videoHPE = null
        super.onPause()
    }

    private fun openVideo() {
        // Load video from resources
        val videoUri =
            Uri.parse("android.resource://" + packageName + "/" + R.raw.test_video_functionalshirt)

        // Initialize VideoHPE
        videoHPE = VideoHPE(surfaceView, videoUri)
        lifecycleScope.launch(Dispatchers.Main) {
            videoHPE?.initVideo()
        }
        // Create PoseEstimator
        createPoseEstimator()
    }

    private fun createPoseEstimator() {
        // For MoveNet MultiPose, hide score and disable pose classifier as the model returns
        // multiple Person instances.
        val poseDetector = when (modelPos) {
            0 -> {
                // MoveNet Lightning (SinglePose)
//                showPoseClassifier(true)
//                showDetectionScore(true)
//                showTracker(false)
                MoveNet.create(this, device, ModelType.Lightning)
            }

            1 -> {
                // MoveNet Thunder (SinglePose)
//                showPoseClassifier(true)
//                showDetectionScore(true)
//                showTracker(false)
                MoveNet.create(this, device, ModelType.Thunder)
            }

            2 -> {
                // MoveNet (Lightning) MultiPose
//                showPoseClassifier(false)
//                showDetectionScore(false)
                // Movenet MultiPose Dynamic does not support GPUDelegate
                if (device == Device.GPU) {
                    showToast(getString(R.string.tfe_pe_gpu_error))
                }
//                showTracker(true)
                MoveNetMultiPose.create(
                    this,
                    device,
                    Type.Dynamic
                )
            }

            3 -> {
                // PoseNet (SinglePose)
//                showPoseClassifier(true)
//                showDetectionScore(true)
//                showTracker(false)
                PoseNet.create(this, device)
            }

            else -> {
                null
            }
        }
        poseDetector?.let { detector ->
            videoHPE?.setDetector(detector)
        }

        // Set SpineTracker
        videoHPE?.setSpineTracker(SpineTracker())
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}