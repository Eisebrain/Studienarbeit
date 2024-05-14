package org.tensorflow.lite.examples.poseestimation.exercises

import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person

class LSit {
    companion object {
        private const val thresholdMin = 185
        private const val thresholdMax = 195
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
        val angleLeftElbow = exerciseUtils.calculateAngleBetweenThreePoints(leftShoulder, leftElbow, leftWrist)
        val angleRightElbow = exerciseUtils.calculateAngleBetweenThreePoints(rightShoulder, rightElbow, rightWrist)

        // ToDo: test if left and right joints are in the camera view or if only one joint is in the camera view
        return thresholdMin < angleLeftKnee && angleLeftKnee < thresholdMax &&
                thresholdMin < angleRightKnee && angleRightKnee < thresholdMax &&
                thresholdMin < angleLeftElbow && angleLeftElbow < thresholdMax &&
                thresholdMin < angleRightElbow && angleRightElbow < thresholdMax
    }
}
