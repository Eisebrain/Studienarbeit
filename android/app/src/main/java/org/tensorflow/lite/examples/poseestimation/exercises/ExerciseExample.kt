package org.tensorflow.lite.examples.poseestimation.exercises

import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person

class ExerciseExample {
    companion object {
        private const val thresholdMin = 88;
        private const val thresholdMax = 93;
        val exerciseUtils = ExerciseUtils()
    }

    fun isExercise(person: Person): Boolean {
        val leftHip = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_HIP)
        val rightHip = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_HIP)
        val leftKnee = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_KNEE)
        val rightKnee = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_KNEE)
        val leftAnkle = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_ANKLE)
        val rightAnkle = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_ANKLE)


        val angleLeftKnee = exerciseUtils.calculateAngleBetweenThreePoints(leftHip, leftKnee, leftAnkle)

        return thresholdMin < angleLeftKnee && angleLeftKnee < thresholdMax
    }
}