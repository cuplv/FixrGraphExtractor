package simple;

import java.util.List;
import java.util.ArrayList;

class Simp {

  class SimpBase {
  }

  class SimpExtended extends SimpBase {
  }

  public static SimpBase getSimpBase() { return null; }

  public static void doSomething(SimpExtended extended) {}

  public static void testList(List<Object> objectList) {}

  // Do nothing, just shows that there are no casts
  // introduced by Jimple
  public void testImplicitCast() {
    ArrayList<Object> l = null;

    testList(l);
  }

  // The cast to the app specific types should be ignored
  public void testAppCast() {
    SimpExtended extended;
    extended = (SimpExtended) getSimpBase();
    doSomething(extended);
  }

  // Test cast to the framework specific types should be
  // considered instead
  public void testFmwkCast() {
    Object l = null;
    List<Object> baseList = (List<Object>) l;

    testList(baseList);
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

  // Expected: don't change
  // The inlining just look at a straightline code
  public void testAssignments4() {
    int x, y, z;

    x = 1;
    z = 3;
    y = x;
  }

  // Expected: no changes (we do not handle this case)
  public void testAssignments5(int a) {
    int x, y, z;

    x = 1;
    if (a > 0) {
      y = x;
    }
    z = x;
  }

  // Expected: no changes (we do not handle this case)
  public void testAssignments6(int a) {
    int x, y;

    if (a > 0) {
      x = 1;
    } else {
      x = 2
    }

    y = x;
  }


}
