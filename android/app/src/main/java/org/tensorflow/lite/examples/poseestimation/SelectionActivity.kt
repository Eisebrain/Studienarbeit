package org.tensorflow.lite.examples.poseestimation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)
    }
    fun onImageViewClick(view: View) {
        val imageView = view as ImageView
        // Zurücksetzen des Bilds aller ImageViews
        resetImageViews()

        // Ausgewähltes ImageView markieren
        selectImageView(imageView)
    }

    private fun resetImageViews() {
        findViewById<ImageView>(R.id.imageView1).setImageResource(R.drawable.l_sit)
        findViewById<ImageView>(R.id.imageView2).setImageResource(R.drawable.squat)
    }

    private fun selectImageView(imageView: ImageView) {
        // Ändern des Bilds für das ausgewählte ImageView
        imageView.setImageResource(R.drawable.border_selected_image)
    }

    fun goToVideoUpload(view: View){
        val intent = Intent(this, VideoUploadActivity::class.java)
        startActivity(intent)
    }
    fun goToLiveAnalysis(view: View){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }



}