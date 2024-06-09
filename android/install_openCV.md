# Install OpenCV on Android
This document describes how to install OpenCV on Android.

## Prerequisites
- Android Studio
- Android device and Android development environment with minimum API 23.
- [OpenCV SDK](https://opencv.org/releases/) for Android (tested with version 4.8.1)

## Installation
1. Download the OpenCV SDK for Android from the [OpenCV website](https://opencv.org/releases/).
2. Extract the downloaded file and copy the content of the folder into a new directory in `android/opencv-4.8.1` of the project.
3. Go to Android Studio and open the project. Under `File` > `New` > `Import Module`, past the path of OpenCV to the `Source directory` field, specify the `Module name` to `opencv-4.8.1` and click `Finish`.
4. Under `File` > `Project Structure` > `Dependencies`, add the OpenCV module to the app module.

If the module is named differently, you need to adjust the module name in the `build.gradle` file of the app module.