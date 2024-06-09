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
        private const val KNEE_ANGLE_THRESHOLD_MIN = 65
        private const val KNEE_ANGLE_THRESHOLD_MAX = 85
        private const val KNEE_ANGLE_THRESHOLD_STAND = 120

        private var previousKneeAngle = Double.MAX_VALUE
    }

    enum class SquatState {
        Stand,        // Starting and ending state of a squat (standing)
        Squat,      // Lowest state of a squat
        TRANSITION // Intermediate state, can be descending or ascending
    }

    var currentState = SquatState.Stand
    var squatCount = 0
    var isKneeAngleCorrectDuringSquat = true
    var squatTooDeep = false
    var squatNotDeepEnough = false
    var squatNotCorrect = false
    var minSquatDepthReached = false


    fun isSquatCorrect(person: Person): Boolean {
        val angles = calculateAngles(person)
        return angles.kneeLeft >= KNEE_ANGLE_THRESHOLD_MIN && angles.kneeLeft <= KNEE_ANGLE_THRESHOLD_MAX &&
                angles.kneeRight >= KNEE_ANGLE_THRESHOLD_MIN && angles.kneeRight <= KNEE_ANGLE_THRESHOLD_MAX
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
        val kneeRight =
            exerciseUtils.calculateAngleBetweenThreePoints(rightAnkle, rightKnee, rightHip)
        val hipLeft =
            exerciseUtils.calculateAngleBetweenThreePoints(leftShoulder, leftHip, leftKnee)
        val hipRight =
            exerciseUtils.calculateAngleBetweenThreePoints(rightShoulder, rightHip, rightKnee)
        return Quadruple(hipLeft, hipRight, kneeLeft, kneeRight)
    }


    fun updateSquatState(person: Person): Int {
        val currentKneeAngle = calculateAverageKneeAngle(person)
        println(currentKneeAngle)
        when (currentState) {
            SquatState.Stand -> {
                if (currentKneeAngle < previousKneeAngle && currentKneeAngle <= KNEE_ANGLE_THRESHOLD_STAND) {
                    currentState = SquatState.Squat
                    // Reset conditions when starting a new squat
                    squatTooDeep = false
                    squatNotDeepEnough = false
                    squatNotCorrect = false
                    isKneeAngleCorrectDuringSquat =
                        currentKneeAngle >= KNEE_ANGLE_THRESHOLD_MIN && currentKneeAngle <= KNEE_ANGLE_THRESHOLD_MAX
                }
            }

            SquatState.Squat -> {
                if (currentKneeAngle < KNEE_ANGLE_THRESHOLD_MIN) {
                    // If squat is too deep, set the flag
                    // Overwrite the flag for  "squat not deep enough"
                    squatNotDeepEnough = false
                    squatTooDeep = true
                    squatNotCorrect = true

                }
                // check if min depth is reached and set flag so
                if (currentKneeAngle < KNEE_ANGLE_THRESHOLD_MAX) {
                    minSquatDepthReached = true
                }
                // only set not deep enough if min depth is never reached and its not too deep
                else if (currentKneeAngle > KNEE_ANGLE_THRESHOLD_MAX && minSquatDepthReached != true) {
                    if (!squatTooDeep) {
                        squatNotDeepEnough = true
                        squatNotCorrect = true
                    }
                }

                if (currentKneeAngle > previousKneeAngle && currentKneeAngle >= KNEE_ANGLE_THRESHOLD_STAND) {
                    currentState = SquatState.Stand
                    if (!squatNotCorrect) {
                        println("Moving Up - Squat korrekt ausgeführt. Aktuelle Squat-Zahl: $squatCount")
                        return 1
                    } else {
                        println("Moving Up - Squat nicht korrekt ausgeführt.")
                        if (squatTooDeep) {
                            println("Squat war zu tief.")
                            return 2
                        }
                        if (squatNotDeepEnough) {
                            println("Squat war nicht tief genug.")
                            return 3
                        }
                    }
                    // Reset conditions when squat cycle is complete
                    isKneeAngleCorrectDuringSquat = true
                    squatTooDeep = false
                    squatNotDeepEnough = false
                    squatNotCorrect = false
                }
            }

            SquatState.TRANSITION -> {
                // Handle additional logic for transitions if needed
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
