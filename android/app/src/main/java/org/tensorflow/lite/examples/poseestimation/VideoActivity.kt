package org.tensorflow.lite.examples.poseestimation

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
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
import kotlin.properties.Delegates

class VideoActivity : AppCompatActivity() {

    /** A [SurfaceView] for video preview.   */
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

    private lateinit var tvScore: TextView
    private lateinit var tvFPS: TextView
    private lateinit var spnDevice: Spinner
    private lateinit var spnModel: Spinner
    private lateinit var spnTracker: Spinner
    private lateinit var vTrackerOption: View

    /** Button to abort the exercise and return to [SelectionActivity]*/
    private lateinit var btnAbord: Button

    /** Selected exercise from [SelectionActivity]
     * selectedExercise == R.id.imageView1 -> L-Sit
     * selectedExercise == R.id.imageView2 -> Squat */
    private var selectedExercise by Delegates.notNull<Int>()

    private var videoHPE: VideoHPE? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tvScore = findViewById(R.id.tvScore)
        tvFPS = findViewById(R.id.tvFps)
        spnModel = findViewById(R.id.spnModel)
        spnDevice = findViewById(R.id.spnDevice)
        spnTracker = findViewById(R.id.spnTracker)
        vTrackerOption = findViewById(R.id.vTrackerOption)
        surfaceView = findViewById(R.id.surfaceViewVideo)

        btnAbord = findViewById(R.id.btnAbord)

        selectedExercise = SelectionActivity.selectedImage


        btnAbord.setOnClickListener {
            onPause()
            val i = Intent(this@VideoActivity, SelectionActivity::class.java)
            startActivity(i)
        }

        showStartTimerDialog()
        openVideo()
    }

    private fun showStartTimerDialog() {
        AlertDialog.Builder(this).apply {
            // display the selected exercise
            if (selectedExercise == R.id.imageView1) {
                setTitle("L-Sit")
                setMessage(
                    "You are going to have 10 seconds to get ready and after that you need to perform the L-Sit for 30 seconds. Pay attention to the following things to perform the exercise correctly:\n" +
                            "\n- Straight back\n" +
                            "- Keep rings stable\n" +
                            "- Angle 90°\n" +
                            "- Legs horizontal to the floor\n" +
                            "- Hold for 3 seconds "
                )
            } else if (selectedExercise == R.id.imageView2) {
                setTitle("Squat")
                setMessage(
                    "You are going to have 10 seconds to get ready and after that you need to perform the Squat for 30 seconds. Pay attention to the following things to perform the exercise correctly:\n" +
                            "\n- Keep your back straight\n" +
                            "- Go down to 90°\n" +
                            "- Keep your head straight and look forward"
                )
            }


            setPositiveButton("Start") { dialog, which ->
                // Startet den Timer, wenn der Nutzer auf "Start" klickt
                //startCountdownTimer()
            }
            setNegativeButton("Back to selection") { dialog, which ->
                // go back to selection
                finish()

            }
            setCancelable(false) // Verhindert das Schließen des Dialogs durch Zurück-Taste oder Tippen außerhalb
        }.show()
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
        videoHPE = VideoHPE(surfaceView, videoUri, object : VideoHPE.VideoHPEListener {
            override fun onFPSListener(fps: Int) {
                tvFPS.text = getString(R.string.tfe_pe_tv_fps, fps)
            }

            override fun onDetectedInfo(
                personScore: Float?,
                poseLabels: List<Pair<String, Float>>?
            ) {
                tvScore.text = getString(R.string.tfe_pe_tv_score, personScore ?: 0f)
            }

        })
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