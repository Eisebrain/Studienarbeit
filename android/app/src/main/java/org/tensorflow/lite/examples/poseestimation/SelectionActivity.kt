package org.tensorflow.lite.examples.poseestimation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SelectionActivity : AppCompatActivity() {

    private lateinit var videoUploadButton: Button
    private lateinit var liveAnalysisButton: Button

    private val imageResourceMap = mapOf(
        R.id.imageView1 to Pair(R.drawable.l_sit, R.drawable.l_sit_selected),
        R.id.imageView2 to Pair(R.drawable.squat, R.drawable.squat_selected)
    )

    // Safe selection and pass it to MainActivity
    companion object {
        var selectedImage: Int = 0
    }
    // pass the selected image to MainActivity
    fun onImageViewClick(view: View) {
        val imageView = view as ImageView
        resetImageViews()
        selectImageView(imageView)
        selectedImage = imageView.id
        // Buttons aktivieren, wenn eine Auswahl getroffen wurde
        setButtonsEnabled(true)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)

        // Initialisierung der Buttons
        videoUploadButton = findViewById(R.id.videoUploadButton)
        liveAnalysisButton = findViewById(R.id.liveAnalysisButton)

        // Buttons deaktivieren
        setButtonsEnabled(false)
    }

    private fun resetImageViews() {
        imageResourceMap.forEach { (id, images) ->
            findViewById<ImageView>(id).setImageResource(images.first)
        }
    }

    private fun selectImageView(imageView: ImageView) {
        imageResourceMap[imageView.id]?.let {
            imageView.setImageResource(it.second)
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        videoUploadButton.isEnabled = enabled
        liveAnalysisButton.isEnabled = enabled

        // Optional: Ändern des Button-Aussehens, um zu zeigen, dass sie deaktiviert sind
        videoUploadButton.alpha = if (enabled) 1.0f else 0.5f
        liveAnalysisButton.alpha = if (enabled) 1.0f else 0.5f
    }

    fun goToVideoUpload(view: View) {
        if (videoUploadButton.isEnabled) {
            val intent = Intent(this, VideoUploadActivity::class.java)
            startActivity(intent)
        }
    }

    fun goToLiveAnalysis(view: View) {
        if (liveAnalysisButton.isEnabled) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
