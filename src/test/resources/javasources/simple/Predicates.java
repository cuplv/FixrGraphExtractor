package simple;


class Predicates {
  public void testIf() {
    boolean b;
    int cav;

    cav = 0;
    b = true;

    if (b) {
      cav = 1;
    }
  }

  public void testLookupSwitch(int switchVal) {
    int val = 0;
    switch (switchVal) {
    case 10:
      val = 1;
      break;
    case 2:
      val = 2;
      break;
    default:
      val = -1;
    }
  }

  public void testTableSwitch(int switchVal) {
    int val = 0;
    switch (switchVal) {
    case 0:
      val = 1;
      break;
    case 1:
      val = 2;
      break;
    default:
      val = -1;
    }
  }
}
