package bugs;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;


public class BugReturn {
  private void bug_return(int state) {
    int i = 0;
    List<String> app = new ArrayList<String>();

    try {
      String[] array = null;
      app.toArray(array);
      return;
    } finally {
      app.clear();
    }
  }
}
