package org.tensorflow.lite.examples.poseestimation.video

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.ImageReader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.view.SurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.poseestimation.VisualizationUtils
import org.tensorflow.lite.examples.poseestimation.YuvToRgbConverter
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.tensorflow.lite.examples.poseestimation.ml.PoseClassifier
import org.tensorflow.lite.examples.poseestimation.ml.PoseDetector
import java.util.Timer

class VideoHPE(
    private val surfaceView: SurfaceView,
    private val videoUri: Uri,
    private val listener: VideoHPEListener? = null
) {
    companion object {
        private const val PREVIEW_WIDTH = 540
        private const val PREVIEW_HEIGHT = 1080

        /** Threshold for confidence score. */
        private const val MIN_CONFIDENCE = .2f
        private const val TAG = "Camera Source"
    }


    private val lock = Any()
    private var detector: PoseDetector? = null
    private var classifier: PoseClassifier? = null
    private var isTrackerEnabled = false
    private var yuvConverter: YuvToRgbConverter = YuvToRgbConverter(surfaceView.context)
    private lateinit var imageBitmap: Bitmap

    /** Frame count that have been processed so far in an one second interval to calculate FPS. */
    private var fpsTimer: Timer? = null
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0
    private var frameRate = 1000/30

    /** Readers used as buffers for camera still shots */
    private var imageReader: ImageReader? = null

    /** [HandlerThread] where all buffer reading operations run */
    private var imageReaderThread: HandlerThread? = null

    /** [Handler] corresponding to [imageReaderThread] */
    private var imageReaderHandler: Handler? = null

    suspend fun initVideo() {
        GlobalScope.launch(Dispatchers.IO) {
            // get bitmap from video
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(surfaceView.context, videoUri)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = duration?.toLong() ?: 0

            val frameIntervalMs = (1000 / 29.99).toLong() // Intervall zwischen den Frames in Millisekunden

            // process bitmap
            var currentTimeMs = 0L
            while (currentTimeMs < durationMs) {
                val bitmap = retriever.getFrameAtTime(currentTimeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    visualize(bitmap)
                }
                // Inkrementiere die aktuelle Zeit um das Intervall zwischen den Frames
                currentTimeMs += frameIntervalMs
            }
        }


//        imageReader =
//            ImageReader.newInstance(
//                PREVIEW_WIDTH,
//                PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 3
//            )
//        imageReader?.setOnImageAvailableListener({ reader ->
//            val image = reader.acquireLatestImage()
//            if (image != null) {
//                if (!::imageBitmap.isInitialized) {
//                    imageBitmap =
//                        Bitmap.createBitmap(
//                            PREVIEW_WIDTH,
//                            PREVIEW_HEIGHT,
//                            Bitmap.Config.ARGB_8888
//                        )
//                }
//                //yuvConverter.yuvToRgb(image, imageBitmap)
//                // Create rotated version for portrait display
//                val rotateMatrix = Matrix()
//                rotateMatrix.postRotate(90.0f)
//
//                val rotatedBitmap = Bitmap.createBitmap(
//                    imageBitmap, 0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT,
//                    rotateMatrix, false
//                )
//                processImage(rotatedBitmap)
//                image.close()
//            }
//        }, imageReaderHandler)

//        imageReader?.surface?.let { surface ->
//            session = createSession(listOf(surface))
//            val cameraRequest = camera?.createCaptureRequest(
//                CameraDevice.TEMPLATE_PREVIEW
//            )?.apply {
//                addTarget(surface)
//            }
//            cameraRequest?.build()?.let {
//                session?.setRepeatingRequest(it, null, null)
//            }
//        }

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
        }
        visualize(persons, bitmap)
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
        val surfaceCanvas = holder.lockCanvas()
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
    }

}