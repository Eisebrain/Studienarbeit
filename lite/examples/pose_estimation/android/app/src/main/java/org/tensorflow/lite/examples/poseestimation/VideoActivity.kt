package org.tensorflow.lite.examples.poseestimation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VideoActivity : AppCompatActivity() {

    private lateinit var btnSwitch2UploadVideo: Button
    private lateinit var btnSwitch2TestVido: Button
    private lateinit var btnSwitch2Camera: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        btnSwitch2UploadVideo = findViewById(R.id.btnUploadVideo)
        btnSwitch2TestVido = findViewById(R.id.btnTestVideo)
        btnSwitch2Camera = findViewById(R.id.btnUseCamera)

        btnSwitch2UploadVideo.setOnClickListener(View.OnClickListener {
            // ToDo: let user upload video from files
            // 1) request permission to access files
            // 2) open file picker
            // 3) get video file and pass to function
            showToast("Function not implemented yet")
        })

        btnSwitch2TestVido.setOnClickListener {
            val i = Intent(this@VideoActivity, VideoActivity::class.java)
            startActivity(i)
        }

        btnSwitch2Camera.setOnClickListener(View.OnClickListener {
            val i = Intent(this@VideoActivity, MainActivity::class.java)
            startActivity(i)
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}