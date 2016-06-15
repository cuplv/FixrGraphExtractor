package bugs;

abstract class Bug_022 {
  // Pure abstract method
  abstract void m1(); 

  void m2() {
    int i = 0;
    i = i + 1;
  }
}
