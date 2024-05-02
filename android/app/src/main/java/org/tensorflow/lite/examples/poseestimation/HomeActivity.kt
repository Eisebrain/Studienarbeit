package org.tensorflow.lite.examples.poseestimation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.content.Context

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        markFirstStartCompleted()
    }
    fun goToSelection(view: View) {
        val intent = Intent(this, SelectionActivity::class.java)
        startActivity(intent)
    }
    private fun markFirstStartCompleted() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("isFirstStart", false).apply()
    }
}