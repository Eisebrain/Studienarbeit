package org.tensorflow.lite.examples.poseestimation.exercises

import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person

class LSit {
    companion object {
        private const val THRESHOLD_MIN = 173
        private const val THRESHOLD_PERFECT = 176
        private const val THRESHOLD_MAX = 182
        val exerciseUtils = ExerciseUtils()
    }

    fun isLSit(person: Person): Boolean {
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

        val angleLeftKnee = exerciseUtils.calculateAngleBetweenThreePoints(leftHip, leftKnee, leftAnkle)
        val angleRightKnee = exerciseUtils.calculateAngleBetweenThreePoints(rightHip, rightKnee, rightAnkle)
        //println("Right knee angle: $angleRightKnee")
        val angleLeftElbow = exerciseUtils.calculateAngleBetweenThreePoints(leftShoulder, leftElbow, leftWrist)
        val angleRightElbow = exerciseUtils.calculateAngleBetweenThreePoints(rightShoulder, rightElbow, rightWrist)
        //println("Right elbow angle: $angleRightElbow")

        // ToDo: return int (eg. 0 for not detected, 1 for detected but not correct, 2 for correct)
        return (THRESHOLD_MIN < angleLeftKnee && angleLeftKnee < THRESHOLD_MAX &&
                THRESHOLD_MIN < angleRightKnee && angleRightKnee < THRESHOLD_MAX) ||
                (THRESHOLD_MIN < angleLeftElbow && angleLeftElbow < THRESHOLD_MAX &&
                THRESHOLD_MIN < angleRightElbow && angleRightElbow < THRESHOLD_MAX)
    }
}
