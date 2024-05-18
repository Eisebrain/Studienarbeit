package org.tensorflow.lite.examples.poseestimation.exercises

import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person

class Squat {
    companion object {
        private const val KNEE_ANGLE_THRESHOLD_MIN = 60
        private const val KNEE_ANGLE_THRESHOLD_MAX = 85

        private var previousKneeAngle = Double.MAX_VALUE
    }

    enum class SquatState {
        UP,        // Starting and ending state of a squat (standing)
        DOWN,      // Lowest state of a squat
        TRANSITION // Intermediate state, can be descending or ascending
    }

    var currentState = SquatState.UP
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
            SquatState.UP -> {
                // Moving down if the knee angle is decreasing and is less than the maximum threshold
                if (currentKneeAngle < previousKneeAngle && currentKneeAngle <= KNEE_ANGLE_THRESHOLD_MAX) {
                    currentState = SquatState.DOWN
                    println("Moving Down")
                }
            }
            SquatState.DOWN -> {
                // Moving up if the knee angle is increasing and surpasses the minimum threshold
                if (currentKneeAngle > previousKneeAngle && currentKneeAngle >= KNEE_ANGLE_THRESHOLD_MIN) {
                    currentState = SquatState.UP
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
