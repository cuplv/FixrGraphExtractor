package bugs;

import android.media.MediaRecorder;

class Bug_049 {
  private MediaRecorder recorder;

  private double bug(double var) {
    double passo = 0;

    recorder = new MediaRecorder();

    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

    return passo;
  }
}
