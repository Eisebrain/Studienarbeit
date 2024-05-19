package org.tensorflow.lite.examples.poseestimation.video

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.SurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.poseestimation.VisualizationUtils
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.ml.PoseClassifier
import org.tensorflow.lite.examples.poseestimation.ml.PoseDetector
import org.tensorflow.lite.examples.poseestimation.tracker.SpineTracker
import java.util.Timer
import org.opencv.android.OpenCVLoader
import org.tensorflow.lite.examples.poseestimation.R
import org.tensorflow.lite.examples.poseestimation.navigation.SelectionActivity
import java.lang.IllegalStateException

import org.tensorflow.lite.examples.poseestimation.exercises.LSit
import org.tensorflow.lite.examples.poseestimation.exercises.Squat


class VideoHPE(
    private val surfaceView: SurfaceView,
    /** Selected exercise from [SelectionActivity]
     * selectedExercise == R.id.imageView1 -> L-Sit
     * selectedExercise == R.id.imageView2 -> Squat */
    private val exerciseType: Int,
    private val videoUri: Uri,
    private val listener: VideoHPEListener? = null
) {
    companion object {
        //private const val PREVIEW_WIDTH = 540
        //private const val PREVIEW_HEIGHT = 1080

        /** Threshold for confidence score. */
        private const val MIN_CONFIDENCE = .2f

        //private const val TAG = "Video"
        private val LSitValidator = LSit()
        private val SquatValidator = Squat()
    }


    private val lock = Any()
    private var detector: PoseDetector? = null
    private var classifier: PoseClassifier? = null
    private var isTrackerEnabled = false
    private var spineTracker: SpineTracker? = null
    private var isSpineStraight: Boolean? = null

    /** Frame count that have been processed so far in an one second interval to calculate FPS. */
    // private var fpsTimer: Timer? = null
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0
    private var frameRate = 29.99f

    private var currentTimeMs = 0L
    private var noLSitCounter = 0
    private var lSitTimer = 0
    private var lSitSecondCounter = 0
    private var lSitDetectedCounter = 0
    private var lSitPerfectCounter = 0


    // Squat Counter
    private var squatCorrectCounter = 0
    private var squatTooDeepCounter = 0
    private var squatNotDeepEnoughCounter = 0

    private var totalSquatFrames = 0
    private var spineStraightCount = 0
    private var spineStraightPercentage = 0.0


    private var retriever: MediaMetadataRetriever? = null

    private var previousSquatCount = 0

    suspend fun initVideo() {
        if (OpenCVLoader.initDebug()) {
            println("OpenCV is loaded")
        } else {
            println("OpenCV is not loaded")
            return
        }
        noLSitCounter = 0
        lSitTimer = 0

        GlobalScope.launch(Dispatchers.IO) {
            // get bitmap from video
            retriever = MediaMetadataRetriever()
            retriever!!.setDataSource(surfaceView.context, videoUri)

            val duration = retriever!!.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = duration?.toLong() ?: 0

            val frameIntervalMs =
                (1000 / frameRate).toLong() // Intervall zwischen den Frames in Millisekunden

            // process bitmap
            while (currentTimeMs < durationMs) {
                try {
                    val bitmap = retriever!!.getFrameAtTime(
                        currentTimeMs * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    if (bitmap != null) {
                        processImage(bitmap)
                    }
                    // Inkrementiere die aktuelle Zeit um das Intervall zwischen den Frames
                    currentTimeMs += frameIntervalMs

                    listener?.onFPSListener((frameIntervalMs / 10).toInt())
                } catch (e: IllegalStateException) {
                    println("Error: ${e.message}")
                    break
                }
            }
            retriever?.close()
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

    fun setSpineTracker(spineTracker: SpineTracker) {
        this.spineTracker = spineTracker
    }

    fun close() {
        retriever?.close()
        synchronized(lock) {
            detector?.close()
            detector = null
            classifier?.close()
            classifier = null
        }
    }

    // process image
    private fun processImage(bitmap: Bitmap) {
        val persons = mutableListOf<Person>()
        var classificationResult: List<Pair<String, Float>>? = null

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
                        if (lSitTimer == 0) {
                            lSitTimer = currentTimeMs.toInt()
                        } else {
                            if (currentTimeMs - lSitTimer > 1000) {
                                println("LSit 1 sec hold")
                                // reset counter and timer
                                noLSitCounter = 0
                                // reset timer
                                lSitTimer = 0
                                // increment counter
                                lSitSecondCounter++
                            }
                        }
                        incrementCounter(isLSit)
                    } else {
                        // increment counter
                        noLSitCounter++
                        // if no LSit is detected for 10 frames, reset counter and timer
                        if (noLSitCounter > 10) {
                            // reset counter
                            noLSitCounter = 0
                            // reset timer
                            lSitTimer = 0
                        }
                    }
                }

                R.id.imageView2 -> {
                    // Squat
                    // ToDo: Implement Squat exercise -> look at [CameraHPE.kt]
                    /** the metrics should be the same as in [CameraHPE.kt], so you can make class for both */


                    val currentSquatCount = SquatValidator.updateSquatState(persons[0])
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

                    // ToDo: @Mick change this to squatCounter
                    // set squat-counter in VideoActivity


                    if (SquatValidator.currentState == Squat.SquatState.Squat) {
                        isSpineStraight = spineTracker?.trackSpine(persons[0], bitmap)
                        if (isSpineStraight == true) {
                            spineStraightCount++
                        }
                        totalSquatFrames++
                    }

                    // Wenn der Squat endet, berechne den Prozentsatz der korrekten Wirbelsäulenhaltung
                    if (SquatValidator.currentState == Squat.SquatState.Stand && totalSquatFrames > 0) {
                        spineStraightPercentage =
                            (spineStraightCount.toDouble() / totalSquatFrames) * 100
                        println("Percentage of time spine was straight during squat: $spineStraightPercentage%")
                        // Reset der Counter für den nächsten Squat
                        spineStraightCount = 0
                        totalSquatFrames = 0
                    }

                    listener?.onSquatCounter(
                        squatCorrectCounter,
                        squatTooDeepCounter,
                        squatNotDeepEnoughCounter,
                        spineStraightPercentage
                    )
                }
            }
        }
        visualize(persons, bitmap)
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

    /**
     * Visualize the bitmap on the surface view with pose estimation.
     */
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

    /**
     * Visualize the bitmap on the surface view without any pose estimation.
     */
    private fun visualize(bitmap: Bitmap) {
        val holder = surfaceView.holder
        val surfaceCanvas = holder.lockCanvas()
        surfaceCanvas?.let { canvas ->
            val screenWidth: Int
            val screenHeight: Int
            val left: Int
            val top: Int

            if (canvas.height > canvas.width) {
                val ratio = bitmap.height.toFloat() / bitmap.width
                screenWidth = canvas.width
                left = 0
                screenHeight = (canvas.width * ratio).toInt()
                top = (canvas.height - screenHeight) / 2
            } else {
                val ratio = bitmap.width.toFloat() / bitmap.height
                screenHeight = canvas.height
                top = 0
                screenWidth = (canvas.height * ratio).toInt()
                left = (canvas.width - screenWidth) / 2
            }
            val right: Int = left + screenWidth
            val bottom: Int = top + screenHeight

            canvas.drawBitmap(
                bitmap, Rect(0, 0, bitmap.width, bitmap.height),
                Rect(left, top, right, bottom), null
            )
            surfaceView.holder.unlockCanvasAndPost(canvas)
        }
    }


    interface VideoHPEListener {
        fun onFPSListener(fps: Int)

        fun onDetectedInfo(personScore: Float?, poseLabels: List<Pair<String, Float>>?)

        fun onLSitCounter(lSitSecondCounter: Int, lSitDetectedCounter: Int, lSitPerfectCounter: Int)

        fun onSquatCounter(
            squatCorrectCounter: Int,
            squatTooDeepCounter: Int,
            squatNotDeepEnoughCounter: Int,
            spineStraightPercentage: Double
        )
    }

}