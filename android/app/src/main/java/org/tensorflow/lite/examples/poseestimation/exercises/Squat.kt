package org.tensorflow.lite.examples.poseestimation.exercises

import org.tensorflow.lite.examples.poseestimation.data.BodyPart
import org.tensorflow.lite.examples.poseestimation.data.Person
class Squat {
    companion object {
        private const val KNEE_ANGLE_THRESHOLD_MIN = 80
        private const val KNEE_ANGLE_THRESHOLD_MAX = 160
        private const val HIP_ANGLE_THRESHOLD_MIN = 80
        private const val HIP_ANGLE_THRESHOLD_MAX = 160

        val exerciseUtils = ExerciseUtils()
    }

    enum class SquatState {
        UP,
        DOWN,
        TRANSITION
    }

    var currentState = SquatState.UP
    var squatCount = 0

    fun isSquatCorrect(person: Person): Boolean {

        val leftHip = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_HIP)
        val rightHip = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_HIP)
        val leftKnee = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_KNEE)
        val rightKnee = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_KNEE)
        val leftAnkle = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_ANKLE)
        val rightAnkle = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_ANKLE)
        val leftShoulder = exerciseUtils.extractKeypoint(person, BodyPart.LEFT_SHOULDER)
        val rightShoulder = exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_SHOULDER)


        val kneeAngleLeft =
            exerciseUtils.calculateAngleBetweenThreePoints(leftAnkle, leftKnee, leftHip)
        val kneeAngleRight =
            exerciseUtils.calculateAngleBetweenThreePoints(rightAnkle, rightKnee, rightHip)
        val hipAngleLeft =
            exerciseUtils.calculateAngleBetweenThreePoints(leftShoulder, leftHip, leftKnee)
        val hipAngleRight =
            exerciseUtils.calculateAngleBetweenThreePoints(rightShoulder, rightHip, rightKnee)


        return (kneeAngleLeft >= KNEE_ANGLE_THRESHOLD_MIN && kneeAngleLeft <= KNEE_ANGLE_THRESHOLD_MAX &&
                kneeAngleRight >= KNEE_ANGLE_THRESHOLD_MIN && kneeAngleRight <= KNEE_ANGLE_THRESHOLD_MAX &&
                hipAngleLeft >= HIP_ANGLE_THRESHOLD_MIN && hipAngleLeft <= HIP_ANGLE_THRESHOLD_MAX &&
                hipAngleRight >= HIP_ANGLE_THRESHOLD_MIN && hipAngleRight <= HIP_ANGLE_THRESHOLD_MAX)
    }

    fun updateSquatState(person: Person): Int {
        val currentKneeAngle = (exerciseUtils.calculateAngleBetweenThreePoints(
            exerciseUtils.extractKeypoint(person, BodyPart.LEFT_ANKLE),
            exerciseUtils.extractKeypoint(person, BodyPart.LEFT_KNEE),
            exerciseUtils.extractKeypoint(person, BodyPart.LEFT_HIP)
        ) +
                exerciseUtils.calculateAngleBetweenThreePoints(
                    exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_ANKLE),
                    exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_KNEE),
                    exerciseUtils.extractKeypoint(person, BodyPart.RIGHT_HIP)
                )) / 2

        when (currentState) {
            SquatState.UP -> if (currentKneeAngle < KNEE_ANGLE_THRESHOLD_MAX) {
                currentState = SquatState.DOWN
            }

            SquatState.DOWN -> if (currentKneeAngle > KNEE_ANGLE_THRESHOLD_MIN) {
                currentState = SquatState.UP
                squatCount += 1 // Zähle einen korrekten Squat, wenn wir von DOWN zu UP wechseln
            }

            else -> { /* Keine Aktion erforderlich im Zustand TRANSITION */
            }
        }

        return squatCount
    }
}