package bugs;

import android.widget.Button;


public class Bug_058 {
  private static final int STATE_NORMAL = 1;
  private static final int STATE_RECORDING = 2;
  private static final int STATE_WANT_TO_CANCEL = 3;
  Button b;


  public Button setB(Button c) {
    return c
  }

  private void bug_058(int state) {
    b = setB(b);
    b.setText("");
  }
}
