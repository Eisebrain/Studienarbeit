# Studienarbeit - Android App

### Overview
This is an Android application that uses TensorFlow MoveNet to perform real-time pose estimation on a live camera feed 
or on a video file. The app displays the video feed and overlays the detected poses on top of it.
The app analyzes the gymnastic exercises _Squat_ and _L-Sit_ and provides feedback to the user.
The app can run on any device with a camera and Android 6.0 (API level 23) or higher.

## Build the demo using Android Studio

### Prerequisites

* If you don't have it already, install **[Android Studio Iguana](
  https://developer.android.com/studio/releases/past-releases/as-iguana-release-notes?hl=en)**, following the instructions on the website.

* Android device and Android development environment with minimum API 23.

### Building
* Open Android Studio, and from the `Welcome` screen, select
`Open an existing Android Studio project`.

* From the `Open File or Project` window that appears, navigate to and select
 the `android` directory from wherever you
 cloned the `Studienarbeit` GitHub repo. Click `OK`.

* If it asks you to do a `Gradle Sync`, click `OK`.

* You may also need to install various platforms and tools, if you get errors
 like `Failed to find target with hash string 'android-21'` and similar. Click
 the `Run` button (the green arrow) or select `Run` > `Run 'android'` from the
 top menu. You may need to rebuild the project using `Build` > `Rebuild Project`.

* If it asks you to use `Instant Run`, click `Proceed Without Instant Run`.

* If there are build errors in the `OpenCV` module, you may reinstall the
 OpenCV SDK by following the instructions in `install_openCV.md`.

* If you want to run the app on a smartphone, you need to have an Android device plugged in with developer options
 enabled at this point. See **[here](
 https://developer.android.com/studio/run/device)** for more details
 on setting up developer devices.

### Additional Note
_Please do not delete the assets folder content_. If you explicitly deleted the
 files, then please choose `Build` > `Rebuild` from menu to re-download the
 deleted model files into assets folder.
