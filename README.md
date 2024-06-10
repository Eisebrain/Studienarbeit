# Studienarbeit - Entwicklung einer App zur Überprüfung und Korrektur der Körperhaltung bei der Ausführung von Kraftsportübungen
This repository contains the code for the app developed in the context of the study project "Development of an app for checking and correcting body posture during strength training exercises".
The app is based on the TensorFlow Lite Pose Estimation model and is intended to help users to perform strength training exercises with the correct posture.

The repository is forked from the official [TensorFlow Repository](https://github.com/tensorflow/examples) and contains the necessary changes to the Pose Estimation model to calculate the angles of the joints and to display them.

For detailed information on how to setup this repository please refere to [android/README.md](android/README.md).


***
<details>
    <summary>README from the original repository</summary>
Go to lite/examples/pose_estimation/android/app/src/main/java/org/tensorflow/lite/examples/poseestimation

- Changes(angle calculation & logging) were made in MoveNet.kt
- Small adjustments in Mainactivity (in comments, not finished)



# TensorFlow Examples

<div align="center">
  <img src="https://www.tensorflow.org/images/tf_logo_social.png" /><br /><br />
</div>

<h2>Most important links!</h2>

* [Community examples](./community)
* [Course materials](./courses/udacity_deep_learning) for the [Deep Learning](https://www.udacity.com/course/deep-learning--ud730) class on Udacity

If you are looking to learn TensorFlow, don't miss the
[core TensorFlow documentation](http://github.com/tensorflow/docs)
which is largely runnable code.
Those notebooks can be opened in Colab from
[tensorflow.org](https://tensorflow.org).

<h2>What is this repo?</h2>

This is the TensorFlow example repo.  It has several classes of material:

* Showcase examples and documentation for our fantastic [TensorFlow Community](https://tensorflow.org/community)
* Provide examples mentioned on TensorFlow.org
* Publish material supporting official TensorFlow courses
* Publish supporting material for the [TensorFlow Blog](https://blog.tensorflow.org) and [TensorFlow YouTube Channel](https://youtube.com/tensorflow)

We welcome community contributions, see [CONTRIBUTING.md](CONTRIBUTING.md) and, for style help,
[Writing TensorFlow documentation](https://www.tensorflow.org/community/contribute/docs_style)
guide.

To file an issue, use the tracker in the
[tensorflow/tensorflow](https://github.com/tensorflow/tensorflow/issues/new?template=20-documentation-issue.md) repo.

## License

[Apache License 2.0](LICENSE)

</details>
