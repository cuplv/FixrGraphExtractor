package bugs;

import android.widget.Button;


public class Bug_053 {
  private static final int STATE_NORMAL = 1;
  private static final int STATE_RECORDING = 2;
  private static final int STATE_WANT_TO_CANCEL = 3;
  Button b;

  private void bug_053(int state) {
    switch (state) {
    case STATE_NORMAL:
      b.setText("");
      break;
    case STATE_RECORDING:
      b.setText("");
      break;
    case STATE_WANT_TO_CANCEL:
      b.setText("");
      break;
    }
  }
}
