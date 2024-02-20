package org.tensorflow.lite.examples.poseestimation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.poseestimation.video.VideoHPE

class VideoActivity : AppCompatActivity() {

    // surface view to display video
    private lateinit var surfaceView: SurfaceView

    private var videoHPE: VideoHPE? = null


    // buttons to switch between activities
    private lateinit var btnSwitch2UploadVideo: Button
    private lateinit var btnSwitch2TestVido: Button
    private lateinit var btnSwitch2Camera: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        surfaceView = findViewById(R.id.surfaceView)
        btnSwitch2UploadVideo = findViewById(R.id.btnUploadVideo)
        btnSwitch2TestVido = findViewById(R.id.btnTestVideo)
        btnSwitch2Camera = findViewById(R.id.btnUseCamera)

        btnSwitch2UploadVideo.setOnClickListener {
            // ToDo: let user upload video from files
            // 1) request permission to access files
            // 2) open file picker
            // 3) get video file and pass to function
            showToast("Function not implemented yet")
        }

        btnSwitch2TestVido.setOnClickListener {
            val i = Intent(this@VideoActivity, VideoActivity::class.java)
            startActivity(i)
        }

        btnSwitch2Camera.setOnClickListener {
            val i = Intent(this@VideoActivity, MainActivity::class.java)
            startActivity(i)
        }

        openVideo()
    }

    private fun openVideo() {
        // setup viedo view
        val videoUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.test_video_functionalshirt)

        // add HPE to frame
        videoHPE = VideoHPE(surfaceView, videoUri)

        lifecycleScope.launch(Dispatchers.Main) {
            videoHPE?.initVideo()
        }

        // show video
        //videoView.start()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}