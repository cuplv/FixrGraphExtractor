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

  public void testAssignments() {
    SimpBase base;
    base = getSimpBase();
  }



}
