package org.tensorflow.lite.examples.poseestimation.exercises

import android.graphics.PointF
import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class ExerciseUtils {

    /**
     * Calculate the angle between three points at point b with the Cosinus Rule
     * @param a: PointF of the first point
     * @param b: PointF of the second point
     * @param c: PointF of the third point
     * @return the angle between the three points at point b
     */
    fun calculateAngleBetweenThreePointsCosinusRule(a: PointF, b: PointF, c: PointF): Double {
        val ab = sqrt((b.x - a.x).toDouble().pow(2.0) + (b.y - a.y).toDouble().pow(2.0))
        val bc = sqrt((c.x - b.x).toDouble().pow(2.0) + (c.y - b.y).toDouble().pow(2.0))
        val ac = sqrt((c.x - a.x).toDouble().pow(2.0) + (c.y - a.y).toDouble().pow(2.0))

        return Math.toDegrees(acos((ab.pow(2.0) + bc.pow(2.0) - ac.pow(2.0)) / (2 * ab * bc)))
    }

    /**
     * Calculate the angle between three points at point b
     * @param a: PointF of the first point
     * @param b: PointF of the second point
     * @param c: PointF of the third point
     * @return the angle between the three points at point b
     */
    fun calculateAngleBetweenThreePoints(a: PointF, b: PointF, c: PointF): Double {
        val ba = PointF((a.x - b.x), a.y - b.y)
        val bc = PointF(c.x - b.x, c.y - b.y)

        val dotProduct = ba.x * bc.x + ba.y * bc.y

        val magnitudeBA = sqrt(ba.x.pow(2) + ba.y.pow(2))
        val magnitudeBC = sqrt(bc.x.pow(2) + bc.y.pow(2))

        return Math.toDegrees(acos(dotProduct / (magnitudeBA * magnitudeBC)).toDouble())
    }

    fun extractKeypoint(person: Person, bodyPart: BodyPart): PointF {
        return person.keyPoints[bodyPart.position].coordinate
    }

    fun extractAvgKeypoint(person: Person, bodyPart1: BodyPart, bodyPart2: BodyPart): PointF {
        val keypoint1 = person.keyPoints[bodyPart1.position].coordinate
        val keypoint2 = person.keyPoints[bodyPart2.position].coordinate

        return PointF((keypoint1.x + keypoint2.x) / 2, (keypoint1.y + keypoint2.y) / 2)
    }
}