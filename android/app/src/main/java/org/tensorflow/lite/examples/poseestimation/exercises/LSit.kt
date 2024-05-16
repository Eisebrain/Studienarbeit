package org.tensorflow.lite.examples.poseestimation.exercises

import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person

class LSit {
    companion object {
        private const val THRESHOLD_MIN = 172
        private const val THRESHOLD_PERFECT = 176
        private const val THRESHOLD_MAX = 183
        private const val THRESHOLD_HIP = 96

        // private const val THRESHOLD_DIFF = 10
        val exerciseUtils = ExerciseUtils()
    }

    /**
     * Check if the person is doing an L-Sit.
     * @param person Person to check.
     * @return 0 if not an L-Sit, 1 if an L-Sit but not perfect, 2 if a perfect L-Sit.
     */
    fun isLSit(person: Person): Int {
        val leftKnee = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_KNEE)
        val rightKnee = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_KNEE)
        val leftHip = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_HIP)
        val rightHip = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_HIP)
        val leftAnkle = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_ANKLE)
        val rightAnkle = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_ANKLE)
        val leftShoulder = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_SHOULDER)
        val rightShoulder = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_SHOULDER)
        val leftElbow = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_ELBOW)
        val rightElbow = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_ELBOW)
        val leftWrist = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_WRIST)
        val rightWrist = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_WRIST)

        val angleLeftKnee =
            exerciseUtils.calculateAngleBetweenThreePoints(leftHip, leftKnee, leftAnkle)
        val angleRightKnee =
            exerciseUtils.calculateAngleBetweenThreePoints(rightHip, rightKnee, rightAnkle)
        //println("Right knee angle: $angleRightKnee")
        val angleLeftElbow =
            exerciseUtils.calculateAngleBetweenThreePoints(leftShoulder, leftElbow, leftWrist)
        val angleRightElbow =
            exerciseUtils.calculateAngleBetweenThreePoints(rightShoulder, rightElbow, rightWrist)
        //println("Right elbow angle: $angleRightElbow")
        val angleLeftHip =
            exerciseUtils.calculateAngleBetweenThreePoints(leftShoulder, leftHip, leftKnee)
        val angleRightHip =
            exerciseUtils.calculateAngleBetweenThreePoints(rightShoulder, rightHip, rightKnee)
        //println("Hip angle: angleLeftHip")

        if (angleLeftHip < THRESHOLD_HIP || angleRightHip < THRESHOLD_HIP) {
            // something like a LSit
            if ((THRESHOLD_PERFECT < angleLeftKnee && angleLeftKnee < THRESHOLD_MAX &&
                THRESHOLD_PERFECT < angleLeftElbow && angleLeftElbow < THRESHOLD_MAX) ||
                (THRESHOLD_PERFECT < angleRightKnee && angleRightKnee < THRESHOLD_MAX &&
                THRESHOLD_PERFECT < angleRightElbow && angleRightElbow < THRESHOLD_MAX)
            ) {
                // perfect LSit
                return 2
            } else if ((THRESHOLD_MIN < angleLeftKnee && angleLeftKnee < THRESHOLD_MAX &&
                THRESHOLD_MIN < angleLeftElbow && angleLeftElbow < THRESHOLD_MAX) ||
                (THRESHOLD_MIN < angleRightKnee && angleRightKnee < THRESHOLD_MAX &&
                THRESHOLD_MIN < angleRightElbow && angleRightElbow < THRESHOLD_MAX)
            ) {
                // not perfect LSit, but still LSit
                return 1
            }
        }
        return 0
    }
}
