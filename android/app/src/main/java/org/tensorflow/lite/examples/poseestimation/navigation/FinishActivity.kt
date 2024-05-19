package org.tensorflow.lite.examples.poseestimation.navigation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import org.tensorflow.lite.examples.poseestimation.R

class FinishActivity : AppCompatActivity() {
    private lateinit var btnFinish: Button
    private lateinit var textFeedback: TextView

    /**
     * The selected exercise from the SelectionActivity.
     */
    private var selectedExercise = SelectionActivity.selectedImage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finish)
        //markFirstStartCompleted()

        btnFinish = findViewById(R.id.finishButton)
        textFeedback = findViewById(R.id.feedbackText)
        var feedback = ""

        when (selectedExercise) {
            R.id.imageView1 -> {
                feedback = getLSitFeedback()
            }
            R.id.imageView2 -> {
                feedback= getSquatFeedback()
            }
        }
        textFeedback.text = feedback


        btnFinish.setOnClickListener {
            val i = Intent(this@FinishActivity, SelectionActivity::class.java)
            startActivity(i)
            finish()
        }
    }

    private fun getLSitFeedback(): String {
        val lSitSecondCounter = intent.getStringExtra("LSitSecondCounter").toString()
        val lSitDetectedCounter = intent.getIntExtra("LSitDetectedCounter",0)
        val lSitPerfectCounter = intent.getIntExtra("LSitPerfectCounter",0)

        var execution = 0.0f
        var executionString = ""

        if (lSitDetectedCounter == 0) {
            executionString = "0"
        } else {
            execution = ((lSitPerfectCounter.toFloat() / lSitDetectedCounter.toFloat()) * 100)
            executionString = String.format("%.2f", execution)
        }

        val feedback = if (execution > 75) "You did a great job!" else "Keep practicing!"

        return "LSit-Exercise: \n\nSeconds held: $lSitSecondCounter \nExecution Score: $executionString% \n\n$feedback"
    }

    private fun getSquatFeedback(): String {
        val squatCorrectCounter = intent.getIntExtra("SquatCorrectCounter",0).toString()
        val squatTooDeepCounter = intent.getIntExtra("SquatTooDeepCounter",0).toString()
        val squatNotDeepEnoughCounter = intent.getIntExtra("SquatNotDeepEnoughCounter",0).toString()
        val spineStraightPercentage = intent.getDoubleExtra("SpineStraightPercentage",0.0)

        val spineStraightPercentageString = String.format("%.2f", spineStraightPercentage)

        return "Squat-Exercise: \n\nCorrect Squats: $squatCorrectCounter \nSquats too deep: $squatTooDeepCounter \nSquats not deep enough: $squatNotDeepEnoughCounter \nSpine straight: $spineStraightPercentageString%"
    }


}