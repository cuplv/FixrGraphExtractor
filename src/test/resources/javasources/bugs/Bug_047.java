package bugs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Bug_047 {

  private String bug_047_method() {
    List<String> changelog = new ArrayList<String>();

    List<Integer> versions = new ArrayList<Integer>(0);
    for (int i = 0, len = 10; i < len; i++) {
    }

    List<String> changes = new ArrayList<String>();
    for (Integer version : versions) {
      changelog.get(0);
      for (String change : changes) {
      }
    }

    return "";
  }
}
