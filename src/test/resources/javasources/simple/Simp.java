package simple;

class Simp {

  class SimpBase {
  }

  class SimpExtended extends SimpBase {
  }

  public static SimpBase getSimpBase() { return null; }

  public static void doSomething(SimpExtended extended) {}

  public void testAppCast() {
    SimpExtended extended;
    extended = (SimpExtended) getSimpBase();
    doSomething(extended);
  }

  // Expected: base = ... (no Jimple intermediate vars)
  public void testAssignments() {
    SimpBase base;
    base = getSimpBase();
  }


  // Expected output: z = 1
  public void testAssignments2() {
    int x, y, z;

    x = 1;
    y = x;
    z = y;
  }

  // Expected output: y = 1; z = y + y
  public void testAssignments3() {
    int x, y, z;

    x = 1;
    y = x;
    z = y + y; // don't handle rhs that are not variables
  }

  // Expected: z = 3; y = 1;
  public void testAssignments4() {
    int x, y, z;

    x = 1;
    z = 3;
    y = x;
  }

  // Expected: y = 1
  public void testAssignments5(int a) {
    int x, y, z;

    x = 1;
    if (a > 0) {
      y = x;
    }
  }

  // Expected: no changes (we do not handle this case)
  public void testAssignments6(int a) {
    int x, y, z;

    x = 1;
    if (a > 0) {
      y = x;
    }
    z = x;
  }


}
