package org.tensorflow.lite.examples.poseestimation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }
    fun goToSelection(view: View) {
        val intent = Intent(this, SelectionActivity::class.java)
        startActivity(intent)
    }
}