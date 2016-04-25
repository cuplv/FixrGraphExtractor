package slice;

import java.lang.Math.*;

public class TestSlice {
  public void m1() {
    int a = 0;

    java.lang.Math.abs(a);
  }
  
  public void m2() {
    // Slice a sequence
    int a;
    int b;
    a = 0;
    b = 0;    
    java.lang.Math.abs(a);
  }

  public void m3() {
    // Slice a sequence - get rid of b blocks

    int a;
    int b;
    a = 0;
    b = 0;
    a = b;
    java.lang.Math.abs(a);
  }

  public void m4() {
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
    java.lang.Math.abs(a);
  }

  public void m5() {
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
      b = a;
    }

    a = b;
    java.lang.Math.abs(a);
  }

  public void m6() {
    int a;
    int b;
    int c;
    
    a = 0;
    b = 0;
    c = 3;
    
    if (b == 0) {
      a = c;
    }
    else {
      b = a;
    }

    java.lang.Math.abs(a);
  }
  
  public void m7() {
    int i;
    int a;
    int b;

    a = 0;
    b = 0;

    for (i = 0; i <= 10; i++) {
      java.lang.Math.abs(a);
      a = b + 1;
    }
  }
  
}
