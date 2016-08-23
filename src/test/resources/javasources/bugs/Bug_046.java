package bugs;

class Bug_046 {
  public void bug_046_method() {
    int i = 0;

    while (i == 0) {
      java.lang.Math.abs(i);
    }
    return;
  }
}
