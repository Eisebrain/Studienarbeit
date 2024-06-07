/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package org.tensorflow.lite.examples.poseestimation

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.poseestimation.camera.CameraHPE
import org.tensorflow.lite.examples.poseestimation.data.Device
import org.tensorflow.lite.examples.poseestimation.ml.*
import org.tensorflow.lite.examples.poseestimation.navigation.FinishActivity
import org.tensorflow.lite.examples.poseestimation.navigation.SelectionActivity
import kotlin.properties.Delegates


class CameraActivity : AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    /** A [SurfaceView] for camera preview.   */
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

    /** Button to finish the exercise and go to [FinishActivity]*/
    private lateinit var btnFinish: Button

    /** Selected exercise from [SelectionActivity]
     * selectedExercise == R.id.imageView1 -> L-Sit
     * selectedExercise == R.id.imageView2 -> Squat */
    private var selectedExercise by Delegates.notNull<Int>()

    /** Variables for LSit counter in [videoHPE] */
    private var lSitSecondCounter = 0
    private var lSitDetectedCounter = 0
    private var lSitPerfectCounter = 0

    private var cameraHPE: CameraHPE? = null
    private var isClassifyPose = false
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                openCamera()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission_camera))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }

    private var changeModelListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }

        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            changeModel(position)
        }
    }

    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    private var changeTrackerListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeTracker(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tvScore = findViewById(R.id.tvScore)
        tvFPS = findViewById(R.id.tvFps)
        spnModel = findViewById(R.id.spnModel)
        spnDevice = findViewById(R.id.spnDevice)
        spnTracker = findViewById(R.id.spnTracker)
        vTrackerOption = findViewById(R.id.vTrackerOption)
        surfaceView = findViewById(R.id.surfaceView)

        btnAbord = findViewById(R.id.btnAbord)
        btnFinish = findViewById(R.id.btnFinish)

        selectedExercise = SelectionActivity.selectedImage


        // swClassification.setOnCheckedChangeListener(setClassificationListener)
        if (!isCameraPermissionGranted()) {
            requestCameraPermission()
        }

        btnAbord.setOnClickListener {
            onPause()
            val i = Intent(this@CameraActivity, SelectionActivity::class.java)
            startActivity(i)
        }

        btnFinish.setOnClickListener {
            onPause()
            val intent = Intent(this@CameraActivity, FinishActivity::class.java)
            // pass LSit counter to FinishActivity
            intent.putExtra("LSitSecondCounter", lSitSecondCounter.toString())
            intent.putExtra("LSitDetectedCounter", lSitDetectedCounter)
            intent.putExtra("LSitPerfectCounter", lSitPerfectCounter)
            startActivity(intent)
        }

        spnModel.setSelection(modelPos)
        initSpinner()

        showStartTimerDialog()
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
            }
            setNegativeButton("Back to selection") { dialog, which ->
                // go back to selection
                val i = Intent(this@CameraActivity, SelectionActivity::class.java)
                startActivity(i)
                finish()

            }
            setCancelable(false) // Verhindert das Schließen des Dialogs durch Zurück-Taste oder Tippen außerhalb
        }.show()
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        cameraHPE?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraHPE?.close()
        cameraHPE = null
        super.onPause()
    }

    // check if permission is granted or not.
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    // open camera
    private fun openCamera() {
        if (isCameraPermissionGranted()) {
            if (cameraHPE == null) {
                cameraHPE =
                    CameraHPE(surfaceView, selectedExercise, object : CameraHPE.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {
                            tvFPS.text = getString(R.string.tfe_pe_tv_fps, fps)
                        }

                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?
                        ) {
                            tvScore.text = getString(R.string.tfe_pe_tv_score, personScore ?: 0f)
                        }

                        override fun onLSitCounter(
                            lSitSecondCounter: Int,
                            lSitDetectedCounter: Int,
                            lSitPerfectCounter: Int
                        ) {
                            this@CameraActivity.lSitSecondCounter = lSitSecondCounter
                            this@CameraActivity.lSitDetectedCounter = lSitDetectedCounter
                            this@CameraActivity.lSitPerfectCounter = lSitPerfectCounter
                        }

                    }).apply {
                        prepareCamera()
                    }
                isPoseClassifier()
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraHPE?.initCamera()
                }
            }
            createPoseEstimator()
        }
    }


    private fun isPoseClassifier() {
        cameraHPE?.setClassifier(if (isClassifyPose) PoseClassifier.create(this) else null)
    }

    // Initialize spinners to let user select model/accelerator/tracker.
    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_models_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spnModel.adapter = adapter
            spnModel.onItemSelectedListener = changeModelListener
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_device_name, android.R.layout.simple_spinner_item
        ).also { adaper ->
            adaper.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnDevice.adapter = adaper
            spnDevice.onItemSelectedListener = changeDeviceListener
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_tracker_array, android.R.layout.simple_spinner_item
        ).also { adaper ->
            adaper.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnTracker.adapter = adaper
            spnTracker.onItemSelectedListener = changeTrackerListener
        }
    }

    // Change model when app is running
    private fun changeModel(position: Int) {
        if (modelPos == position) return
        modelPos = position
        createPoseEstimator()
    }

    // Change device (accelerator) type when app is running
    private fun changeDevice(position: Int) {
        val targetDevice = when (position) {
            0 -> Device.CPU
            1 -> Device.GPU
            else -> Device.NNAPI
        }
        if (device == targetDevice) return
        device = targetDevice
        createPoseEstimator()
    }

    // Change tracker for Movenet MultiPose model
    private fun changeTracker(position: Int) {
        cameraHPE?.setTracker(
            when (position) {
                1 -> TrackerType.BOUNDING_BOX
                2 -> TrackerType.KEYPOINTS
                else -> TrackerType.OFF
            }
        )
    }

    private fun createPoseEstimator() {
        // For MoveNet MultiPose, hide score and disable pose classifier as the model returns
        // multiple Person instances.
        val poseDetector = when (modelPos) {
            0 -> {
                // MoveNet Lightning (SinglePose)
                showDetectionScore(true)
                showTracker(false)
                MoveNet.create(this, device, ModelType.Lightning)
            }

            1 -> {
                // MoveNet Thunder (SinglePose)
                showDetectionScore(true)
                showTracker(false)
                MoveNet.create(this, device, ModelType.Thunder)
            }

            2 -> {
                // MoveNet (Lightning) MultiPose
                showDetectionScore(false)
                // Movenet MultiPose Dynamic does not support GPUDelegate
                if (device == Device.GPU) {
                    showToast(getString(R.string.tfe_pe_gpu_error))
                }
                showTracker(true)
                MoveNetMultiPose.create(
                    this,
                    device,
                    Type.Dynamic
                )
            }

            3 -> {
                // PoseNet (SinglePose)
                showDetectionScore(true)
                showTracker(false)
                PoseNet.create(this, device)
            }

            else -> {
                null
            }
        }
        poseDetector?.let { detector ->
            cameraHPE?.setDetector(detector)
        }
    }


    // Show/hide the detection score.
    private fun showDetectionScore(isVisible: Boolean) {
        tvScore.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    // Show/hide the tracking options.
    private fun showTracker(isVisible: Boolean) {
        if (isVisible) {
            // Show tracker options and enable Bounding Box tracker.
            vTrackerOption.visibility = View.VISIBLE
            spnTracker.setSelection(1)
        } else {
            // Set tracker type to off and hide tracker option.
            vTrackerOption.visibility = View.GONE
            spnTracker.setSelection(0)
        }
    }

    private fun requestCameraPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                // You can use the API that requires the permission.
                openCamera()
            }

            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
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
