package bugs;

import android.os.Handler;

public class Bug_051 {
  Handler mHandler = null;
  boolean isRecording = true;
  float mTime = 0;
  private static final int MSG_VOICE_CHANGED = 0X11;

  private void bug_051() {
    Runnable mGetVoiceLevelRunnable = new Runnable() {
        @Override
        public void run() {
          while (isRecording) {
            try {
              Thread.sleep(100);
              mTime += 0.1f;
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            mHandler.sendEmptyMessage(MSG_VOICE_CHANGED);
          }
        }
      };
  }

}
