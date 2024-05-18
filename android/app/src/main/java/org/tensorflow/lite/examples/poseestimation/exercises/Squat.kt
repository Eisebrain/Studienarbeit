package org.tensorflow.lite.examples.poseestimation.exercises

import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person

class Squat {

    /**
     * This class is to check if the squat exercise is done correctly.
     * In addition, it also counts the number of squats done.
     *
     * Following criteria are checked to determine if the squat is done correctly:
     * 1. The knee angle should be between 60 and 85 degrees.
     * 2.
     */
    companion object {
        private const val KNEE_ANGLE_THRESHOLD_MIN = 60
        private const val KNEE_ANGLE_THRESHOLD_MAX = 85
        private const val KNEE_ANGLE_THRESHOLD_STAND = 110

        private var previousKneeAngle = Double.MAX_VALUE
    }

    enum class SquatState {
        Stand,        // Starting and ending state of a squat (standing)
        Squat,      // Lowest state of a squat
        TRANSITION // Intermediate state, can be descending or ascending
    }

    var currentState = SquatState.Stand
    var squatCount = 0

    fun isSquatCorrect(person: Person): Boolean {
        val angles = calculateAngles(person)
        return angles.hipLeft >= KNEE_ANGLE_THRESHOLD_MIN && angles.hipLeft <= KNEE_ANGLE_THRESHOLD_MAX &&
                angles.hipRight >= KNEE_ANGLE_THRESHOLD_MIN && angles.hipRight <= KNEE_ANGLE_THRESHOLD_MAX

    }

    private fun calculateAngles(person: Person): Quadruple<Double, Double, Double, Double> {
        val exerciseUtils = ExerciseUtils()
        val leftHip = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_HIP)
        val rightHip = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_HIP)
        val leftKnee = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_KNEE)
        val rightKnee = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_KNEE)
        val leftAnkle = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_ANKLE)
        val rightAnkle = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_ANKLE)
        val leftShoulder = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_SHOULDER)
        val rightShoulder = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_SHOULDER)

        val kneeLeft = exerciseUtils.calculateAngleBetweenThreePoints(leftAnkle, leftKnee, leftHip)
        val kneeRight = exerciseUtils.calculateAngleBetweenThreePoints(rightAnkle, rightKnee, rightHip)
        val hipLeft = exerciseUtils.calculateAngleBetweenThreePoints(leftShoulder, leftHip, leftKnee)
        val hipRight = exerciseUtils.calculateAngleBetweenThreePoints(rightShoulder, rightHip, rightKnee)

        return Quadruple(hipLeft, hipRight, kneeLeft, kneeRight)
    }

    fun updateSquatState(person: Person): Int {
        val currentKneeAngle = calculateAverageKneeAngle(person)

        // Update the state based on knee angle changes
        when (currentState) {
            SquatState.Stand -> {
                if (currentKneeAngle < previousKneeAngle && currentKneeAngle <= KNEE_ANGLE_THRESHOLD_STAND) {
                    currentState = SquatState.Squat
                    println("Moving Down")
                }
            }
            SquatState.Squat -> {
                if (currentKneeAngle > previousKneeAngle && currentKneeAngle >= KNEE_ANGLE_THRESHOLD_STAND) {
                    currentState = SquatState.Stand
                    squatCount++
                    println("Moving Up")
                }
            }
            SquatState.TRANSITION -> {
                // Additional logic can be placed here if needed
            }
        }

        previousKneeAngle = currentKneeAngle
        return squatCount
    }

    private fun calculateAverageKneeAngle(person: Person): Double {
        val exerciseUtils = ExerciseUtils()
        val kneeLeft = exerciseUtils.calculateAngleBetweenThreePoints(
            exerciseUtils.extractKeypoint(person, BodyPart.LEFT_ANKLE),
            exerciseUtils.extractKeypoint(person, BodyPart.LEFT_KNEE),
            exerciseUtils.extractKeypoint(person, BodyPart.LEFT_HIP)
        )
        val kneeRight = exerciseUtils.calculateAngleBetweenThreePoints(
            exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_ANKLE),
            exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_KNEE),
            exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_HIP)
        )
        return (kneeLeft + kneeRight) / 2
    }
}

data class Quadruple<A, B, C, D>(val hipLeft: A, val hipRight: B, val kneeLeft: C, val kneeRight: D)
