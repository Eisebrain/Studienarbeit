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

package org.tensorflow.lite.examples.poseestimation.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import kotlinx.coroutines.suspendCancellableCoroutine
import org.tensorflow.lite.examples.poseestimation.R
import org.tensorflow.lite.examples.poseestimation.VisualizationUtils
import org.tensorflow.lite.examples.poseestimation.YuvToRgbConverter
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.exercises.LSit
import org.tensorflow.lite.examples.poseestimation.exercises.Squat
import org.tensorflow.lite.examples.poseestimation.ml.MoveNetMultiPose
import org.tensorflow.lite.examples.poseestimation.ml.PoseClassifier
import org.tensorflow.lite.examples.poseestimation.ml.PoseDetector
import org.tensorflow.lite.examples.poseestimation.ml.TrackerType
import org.tensorflow.lite.examples.poseestimation.navigation.SelectionActivity
import org.tensorflow.lite.examples.poseestimation.tracker.SpineTracker
import org.tensorflow.lite.examples.poseestimation.video.VideoHPE
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class CameraHPE(
    private val surfaceView: SurfaceView,

    /** Selected exercise from [SelectionActivity]
     * exerciseType == R.id.imageView1 -> L-Sit
     * exerciseType == R.id.imageView2 -> Squat */
    private val exerciseType: Int,
    private val listener: CameraSourceListener? = null
) {

    companion object {
        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480

        /** Threshold for confidence score. */
        private const val MIN_CONFIDENCE = .2f
        private const val TAG = "Camera Source"
        private val SquatValidator = Squat()

    }

    private val lock = Any()
    private var detector: PoseDetector? = null
    private var classifier: PoseClassifier? = null
    private var isTrackerEnabled = false
    private var yuvConverter: YuvToRgbConverter = YuvToRgbConverter(surfaceView.context)
    private lateinit var imageBitmap: Bitmap
    private var spineTracker: SpineTracker? = null
    private var isSpineStraight: Boolean? = null

    /** Frame count that have been processed so far in an one second interval to calculate FPS. */
    private var fpsTimer: Timer? = null
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = surfaceView.context
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** Readers used as buffers for camera still shots */
    private var imageReader: ImageReader? = null

    /** The [CameraDevice] that will be opened in this fragment */
    private var camera: CameraDevice? = null

    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
    private var session: CameraCaptureSession? = null

    /** [HandlerThread] where all buffer reading operations run */
    private var imageReaderThread: HandlerThread? = null

    /** [Handler] corresponding to [imageReaderThread] */
    private var imageReaderHandler: Handler? = null
    private var cameraId: String = ""

    private val LSitValidator = LSit()
    private var lSitTimer = 0L
    private var lSitSecondCounter = 0
    private var lSitDetectedCounter = 0
    private var lSitPerfectCounter = 0
    private var noLSitCounter = 0



    // Squat Counter
    private var squatCorrectCounter = 0
    private var squatTooDeepCounter = 0
    private var squatNotDeepEnoughCounter = 0

    private var totalSquatFrames = 0
    private var spineStraightCount = 0
    private var spineStraightPercentage = 0.0
    private var totalSpineStraightPercentage = 0.0
    private var countSpineStraightMeasurements = 0
    private var averageSpineStraightPercentage = 0.0


    suspend fun initCamera() {
        camera = openCamera(cameraManager, cameraId)
        imageReader =
            ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 3)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                if (!::imageBitmap.isInitialized) {
                    imageBitmap =
                        Bitmap.createBitmap(
                            PREVIEW_WIDTH,
                            PREVIEW_HEIGHT,
                            Bitmap.Config.ARGB_8888
                        )
                }
                yuvConverter.yuvToRgb(image, imageBitmap)
                // Create rotated version for portrait display
                val rotateMatrix = Matrix()
                rotateMatrix.postRotate(90.0f)

                val rotatedBitmap = Bitmap.createBitmap(
                    imageBitmap, 0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT,
                    rotateMatrix, false
                )
                processImage(rotatedBitmap)
                image.close()
            }
        }, imageReaderHandler)

        imageReader?.surface?.let { surface ->
            session = createSession(listOf(surface))
            val cameraRequest = camera?.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )?.apply {
                addTarget(surface)
            }
            cameraRequest?.build()?.let {
                session?.setRepeatingRequest(it, null, null)
            }
        }
    }

    private suspend fun createSession(targets: List<Surface>): CameraCaptureSession =
        suspendCancellableCoroutine { cont ->
            camera?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(captureSession: CameraCaptureSession) =
                    cont.resume(captureSession)

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    cont.resumeWithException(Exception("Session error"))
                }
            }, null)
        }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(manager: CameraManager, cameraId: String): CameraDevice =
        suspendCancellableCoroutine { cont ->
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) = cont.resume(camera)

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    if (cont.isActive) cont.resumeWithException(Exception("Camera error"))
                }
            }, imageReaderHandler)
        }

    fun prepareCamera() {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            // We don't use a front facing camera in this sample.
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection != null &&
                cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
            ) {
                continue
            }
            this.cameraId = cameraId
        }
    }

    fun setDetector(detector: PoseDetector) {
        synchronized(lock) {
            if (this.detector != null) {
                this.detector?.close()
                this.detector = null
            }
            this.detector = detector
        }
    }

    fun setClassifier(classifier: PoseClassifier?) {
        synchronized(lock) {
            if (this.classifier != null) {
                this.classifier?.close()
                this.classifier = null
            }
            this.classifier = classifier
        }
    }

    /**
     * Set Tracker for Movenet MuiltiPose model.
     */
    fun setTracker(trackerType: TrackerType) {
        isTrackerEnabled = trackerType != TrackerType.OFF
        (this.detector as? MoveNetMultiPose)?.setTracker(trackerType)
    }

    fun resume() {
        imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
        imageReaderHandler = Handler(imageReaderThread!!.looper)
        fpsTimer = Timer()
        fpsTimer?.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    framesPerSecond = frameProcessedInOneSecondInterval
                    frameProcessedInOneSecondInterval = 0
                }
            },
            0,
            1000
        )
    }

    fun close() {
        session?.close()
        session = null
        camera?.close()
        camera = null
        imageReader?.close()
        imageReader = null
        stopImageReaderThread()
        detector?.close()
        detector = null
        classifier?.close()
        classifier = null
        fpsTimer?.cancel()
        fpsTimer = null
        frameProcessedInOneSecondInterval = 0
        framesPerSecond = 0
    }

    // process image
    private fun processImage(bitmap: Bitmap) {
        val persons = mutableListOf<Person>()
        var classificationResult: List<Pair<String, Float>>? = null
        val currentTime = System.currentTimeMillis()

        synchronized(lock) {
            detector?.estimatePoses(bitmap)?.let {
                persons.addAll(it)

                // if the model only returns one item, allow running the Pose classifier.
                if (persons.isNotEmpty()) {
                    classifier?.run {
                        classificationResult = classify(persons[0])
                    }
                }
            }
        }
        frameProcessedInOneSecondInterval++
        if (frameProcessedInOneSecondInterval == 1) {
            // send fps to view
            listener?.onFPSListener(framesPerSecond)
        }

        // if the model returns only one item, show that item's score.
        if (persons.isNotEmpty()) {
            listener?.onDetectedInfo(persons[0].score, classificationResult)

            // Selected exercise from [SelectionActivity]
            when (exerciseType) {
                R.id.imageView1 -> {
                    // L-Sit
                    /** the metrics should be the same as in [CameraHPE.kt], so you can make class for both */
                    val isLSit = LSitValidator.isLSit(persons[0])
                    // println("isLSit: $isLSit\n")
                    if (isLSit != 0) {
                        // initiate timer for 2 seconds, check if max. 10 isLSit == 0 are detected
                        if (lSitTimer == 0L) {
                            lSitTimer = currentTime
                        } else {
                            if (currentTime - lSitTimer > 1000) {
                                println("LSit 1 sec hold")
                                // reset counter and timer
                                noLSitCounter = 0
                                // reset timer
                                lSitTimer = 0L
                                // increment counter
                                lSitSecondCounter++
                            }
                        }
                        incrementCounter(isLSit)
                    } else {
                        // increment counter
                        noLSitCounter++
                        if (noLSitCounter > 10) {
                            // reset counter
                            noLSitCounter = 0
                            // reset timer
                            lSitTimer = 0L
                        }
                    }
                }
                R.id.imageView2 -> {
                    // Squat
                    // ToDo: Implement Squat exercise -> look at [VideoHPE.kt]
                    /** the metrics should be the same as in [VideoHPE.kt], so you can make class for both */
                    val currentSquatCount = CameraHPE.SquatValidator.updateSquatState(persons[0])
                    // 1 is correct Squat, 2 is too deep, 3 is not deep enough
                    if (currentSquatCount == 1) {
                        squatCorrectCounter++
                    }
                    if (currentSquatCount == 2) {
                        squatTooDeepCounter++
                    }
                    if (currentSquatCount == 3) {
                        squatNotDeepEnoughCounter++
                    }




                    if (CameraHPE.SquatValidator.currentState == Squat.SquatState.Squat) {
                        isSpineStraight = spineTracker?.trackSpine(persons[0], bitmap)
                        if (isSpineStraight == true) {
                            spineStraightCount++
                        }
                        totalSquatFrames++
                    }

                    // Wenn der Squat endet, berechne den Prozentsatz der korrekten Wirbelsäulenhaltung
                    if (CameraHPE.SquatValidator.currentState == Squat.SquatState.Stand && totalSquatFrames > 0) {
                        spineStraightPercentage =
                            (spineStraightCount.toDouble() / totalSquatFrames) * 100
                        println("Percentage of time spine was straight during squat: $spineStraightPercentage%")

                        totalSpineStraightPercentage += spineStraightPercentage
                        countSpineStraightMeasurements++

                        averageSpineStraightPercentage = totalSpineStraightPercentage / countSpineStraightMeasurements
                        println("Average percentage of time spine was straight across all squats: $averageSpineStraightPercentage%")

                        // Reset der Counter für den nächsten Squat
                        spineStraightCount = 0
                        totalSquatFrames = 0
                    }

                    listener?.onSquatCounter(
                        squatCorrectCounter,
                        squatTooDeepCounter,
                        squatNotDeepEnoughCounter,
                        averageSpineStraightPercentage
                    )
                }
            }
        }
        visualize(persons, bitmap)
    }

    private fun visualize(persons: List<Person>, bitmap: Bitmap) {

        val outputBitmap = VisualizationUtils.drawBodyKeypoints(
            bitmap,
            persons.filter { it.score > MIN_CONFIDENCE }, isTrackerEnabled
        )

        val holder = surfaceView.holder
        val surfaceCanvas = holder?.lockCanvas()
        surfaceCanvas?.let { canvas ->
            val screenWidth: Int
            val screenHeight: Int
            val left: Int
            val top: Int

            if (canvas.height > canvas.width) {
                val ratio = outputBitmap.height.toFloat() / outputBitmap.width
                screenWidth = canvas.width
                left = 0
                screenHeight = (canvas.width * ratio).toInt()
                top = (canvas.height - screenHeight) / 2
            } else {
                val ratio = outputBitmap.width.toFloat() / outputBitmap.height
                screenHeight = canvas.height
                top = 0
                screenWidth = (canvas.height * ratio).toInt()
                left = (canvas.width - screenWidth) / 2
            }
            val right: Int = left + screenWidth
            val bottom: Int = top + screenHeight

            canvas.drawBitmap(
                outputBitmap, Rect(0, 0, outputBitmap.width, outputBitmap.height),
                Rect(left, top, right, bottom), null
            )
            surfaceView.holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun incrementCounter(lSit: Int) {
        if (lSit == 1) {
            lSitDetectedCounter++
        } else if (lSit == 2) {
            lSitPerfectCounter++
            lSitDetectedCounter++
        }
        listener?.onLSitCounter(lSitSecondCounter, lSitDetectedCounter, lSitPerfectCounter)
    }

    private fun stopImageReaderThread() {
        imageReaderThread?.quitSafely()
        try {
            imageReaderThread?.join()
            imageReaderThread = null
            imageReaderHandler = null
        } catch (e: InterruptedException) {
            Log.d(TAG, e.message.toString())
        }
    }

    interface CameraSourceListener {
        fun onFPSListener(fps: Int)

        fun onDetectedInfo(personScore: Float?, poseLabels: List<Pair<String, Float>>?)

        fun onLSitCounter(lSitSecondCounter: Int, lSitDetectedCounter: Int, lSitPerfectCounter: Int)

        fun onSquatCounter(
            squatCorrectCounter: Int,
            squatTooDeepCounter: Int,
            squatNotDeepEnoughCounter: Int,
            averageSpineStraightPercentage: Double
        )
    }
}
