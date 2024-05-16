package org.tensorflow.lite.examples.poseestimation

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
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
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

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

    private lateinit var videoUri: Uri


    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission_storage))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) {uri: Uri ->
        videoUri = uri
    }

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

        if (!isExternalStoragePermissionGranted()) {
            requestExternalStoragePermission()
        } else {
            showStartTimerDialog()
            selectVideoFromGalery()
        }
    }

    private fun selectVideoFromGalery() {
        getContent.launch("video/*")
    }

    private fun showStartTimerDialog() {
        AlertDialog.Builder(this).apply {
            // display the selected exercise
            if (selectedExercise == R.id.imageView1) {
                setTitle("L-Sit")
                setMessage(R.string.l_sit_explanation)
            } else if (selectedExercise == R.id.imageView2) {
                setTitle("Squat")
                setMessage(R.string.squat_explanation)
            }

            setPositiveButton("Start") { dialog, which ->
                // Startet den Timer, wenn der Nutzer auf "Start" klickt
                //startCountdownTimer()
                //hide the dialog
                dialog.dismiss()
                openVideo()
            }
            setNegativeButton("Back to selection") { dialog, which ->
                // go back to selection
                val i = Intent(this@VideoActivity, SelectionActivity::class.java)
                startActivity(i)
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

        // Initialize VideoHPE
        videoHPE = VideoHPE(surfaceView, selectedExercise, videoUri, object : VideoHPE.VideoHPEListener {
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

    // check if permission is granted or not.
    private fun isExternalStoragePermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
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

    private fun requestExternalStoragePermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                // You can use the API that requires the permission.
                showStartTimerDialog()
            }

            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }


    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // do nothing
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}