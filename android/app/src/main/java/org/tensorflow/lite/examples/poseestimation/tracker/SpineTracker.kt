package org.tensorflow.lite.examples.poseestimation.tracker

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import android.util.Log
import org.apache.commons.math3.fitting.WeightedObservedPoints
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class SpineTracker {
    companion object {
        private const val paddingTop = 25
        private const val paddingBottom = 25
        private const val paddingLeft = 25
        private const val paddingRight = 25
        private const val TAG = "SpineTracker"
    }


    /**
     * Track the spine of a person in a frame.
     * @param person: Person object containing the keypoints of the person
     * @param bitmap: Bitmap of the frame
     * @return true if the spine is straight, false if the spine is not straight, null if the spine is not detected
     */
    fun trackSpine(person: Person, bitmap: Bitmap): Boolean? {
        val hipKeypoint = extractHipKeypoint(person)
        val shoulderKeypoint = extractShoulderKeypoint(person)

        // crop bitmap to the region around the spine
        val spineRegion = Rect(
            (hipKeypoint.x - paddingLeft).toInt(),
            (shoulderKeypoint.y - paddingTop).toInt(),
            (hipKeypoint.x + paddingRight).toInt(),
            (hipKeypoint.y + paddingBottom).toInt()
        )
        val bitmapROI = cropBitmap(bitmap, spineRegion)

        // detect spine in the cropped bitmap
        val spinePoints = detectSpine(bitmapROI)

        // extract x and y coordinates of the spine
        val spineX = spinePoints.map { point -> point.x }
        val spineY = spinePoints.map { point -> point.y }

        // ToDo: Skip the frame if spine is not detected properly - use previous frame instead
//        val maxDistance = getMaxDistance(spineX, spineY)
//        if (maxDistance < 150) {
//            return previousFrame
//        }

        try {
            // Fit spine curve
            val fitter = PolynomialCurveFitter.create(2) // Fit a polynomial of degree 2 (quadratic curve)
            val obs = WeightedObservedPoints()
            for (i in spineX.indices) {
                obs.add(spineX[i].toDouble(), spineY[i].toDouble())
            }
            val fittedParameters = fitter.fit(obs.toList())

            // Check if curve is straight (polynomial coefficient for x^1)
            val coefficient = fittedParameters[1]
            return abs(coefficient) <= 0.0045
        } catch (e: Exception) {
            Log.e("SpineCurve", "No curve found.", e)
            return null
        }
    }

    private fun detectSpine(bitmap: Bitmap): List<PointF> {
        // Convert Bitmap to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        val imgGray = Mat()
        Imgproc.cvtColor(mat, imgGray, Imgproc.COLOR_BGR2GRAY)

        // Apply Gaussian Blur
        val imgBlur = Mat()
        Imgproc.GaussianBlur(imgGray, imgBlur, Size(5.0, 5.0), 0.0)

        // Apply Canny Edge Detection
        val imgCanny = Mat()
        Imgproc.Canny(imgBlur, imgCanny, 100.0, 110.0)

        // Extract contours
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(imgCanny, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

        // Sort contours by x and y coordinates
        contours.sortBy { contour -> Imgproc.boundingRect(contour).tl().x + Imgproc.boundingRect(contour).tl().y }

        // Select the leftmost contour
        val leftmostContour = if (contours.isNotEmpty()) contours[0] else null

        // Convert leftmost contour to list of PointF
        val points = ArrayList<PointF>()
        leftmostContour?.toList()?.forEach { point ->
            points.add(PointF(point.x.toFloat(), point.y.toFloat()))
        }

        return points
    }

    fun getMaxDistance(x: DoubleArray, y: DoubleArray): Double {
        var maxDistance = 0.0
        for (i in x.indices) {
            for (j in i + 1 until x.size) {
                val distance = sqrt((x[j] - x[i]).pow(2.0) + (y[j] - y[i]).pow(2.0))
                if (distance > maxDistance) {
                    maxDistance = distance
                }
            }
        }
        return maxDistance
    }

    private fun cropBitmap(bitmap: Bitmap, rect: Rect): Bitmap {
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }

    private fun extractHipKeypoint(person: Person): PointF {
        val leftHip = person.keyPoints[BodyPart.LEFT_HIP.position].coordinate
        val rightHip = person.keyPoints[BodyPart.RIGHT_HIP.position].coordinate

        return PointF((leftHip.x + rightHip.x) / 2, (leftHip.y + rightHip.y) / 2)

    }

    private fun extractShoulderKeypoint(person: Person): PointF {
        val leftShoulder = person.keyPoints[BodyPart.LEFT_SHOULDER.position].coordinate
        val rightShoulder = person.keyPoints[BodyPart.RIGHT_SHOULDER.position].coordinate

        return PointF(
            (leftShoulder.x + rightShoulder.x) / 2,
            (leftShoulder.y + rightShoulder.y) / 2
        )
    }
}