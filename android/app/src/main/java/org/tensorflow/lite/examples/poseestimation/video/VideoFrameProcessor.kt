package org.tensorflow.lite.examples.poseestimation.video

import org.opencv.android.CameraBridgeViewBase
import org.opencv.core.Mat

class VideoFrameProcessor : CameraBridgeViewBase.CvCameraViewListener2 {
    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        // Hier können Sie die Frames bearbeiten
        return inputFrame.rgba()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        // Initialisierung bei Start
    }

    override fun onCameraViewStopped() {
        // Aufräumarbeiten bei Stopp
    }
}
