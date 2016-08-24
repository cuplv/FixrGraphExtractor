package bugs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import android.content.Context;


import android.util.SparseArray;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.app.AlertDialog;

class Bug_046 {
  private String getLog(Context mContext) {
    mContext.getResources();
    SparseArray<String> changelog = new SparseArray<String>();

    List<Integer> versions = new ArrayList<Integer>(0);
    for (int i = 0, len = 10; i < len; i++) {
    }

    List<String> changes = new ArrayList<String>();

    for (Integer version : versions) {
      changelog.get(0, null);
      for (String change : changes) {
      }
    }

    return "";
  }
}
