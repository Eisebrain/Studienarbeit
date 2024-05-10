package org.tensorflow.lite.examples.poseestimation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class StartScreenActivity : AppCompatActivity() {
    private lateinit var btnGetStarted: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_screen)
        //markFirstStartCompleted()

        btnGetStarted = findViewById(R.id.getStartedButton)


        btnGetStarted.setOnClickListener {
            showToast("Function not implemented yet!")

//            val i = Intent(this@StartScreenActivity, StartScreenActivity::class.java)
//            startActivity(i)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}