package org.tensorflow.lite.examples.poseestimation.navigation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.poseestimation.R

class StartScreenActivity : AppCompatActivity() {
    private lateinit var btnGetStarted: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_screen)
        //markFirstStartCompleted()

        btnGetStarted = findViewById(R.id.getStartedButton)


        btnGetStarted.setOnClickListener {
            val i = Intent(this@StartScreenActivity, SelectionActivity::class.java)
            startActivity(i)
            finish()
        }
    }
}