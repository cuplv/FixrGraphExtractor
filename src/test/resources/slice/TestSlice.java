package slice;

import java.lang.Math.*;

public class TestSlice {
  public void m1() {
    int a = 0;

    java.lang.Math.abs(a);
  }
  
  public void m2() {
    // Slice a sequence - get rid of b blocks

    int a;
    int b;

    a = 0; // {a}
    b = 0; // {}
    
    java.lang.Math.abs(a); // {a}
  }

  public void m3() {
    // Slice a sequence - get rid of b blocks

    int a;
    int b;
    a = 0;
    b = 0;
    a = b;    
    java.lang.Math.abs(a); // {a,b}
  }

    public void m4() {
    // Slice a sequence - get rid of b blocks

    int a;
    int b;
    int c;

    a = 0;
    b = 0;
    c = 0;

    if (b == 0) {
      a = a + b;
    }
    else {
      c = a;
    }

    a = b;
    java.lang.Math.abs(a); // {a}
  }

}
