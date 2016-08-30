package slice;

import java.lang.Math.*;

/**
 * Test cases for slicing.
 *
 */
public class TestControlFlow {


  public int testSequence01() {
    int a;
    a = 0;
    java.lang.Math.abs(a);
    a = 3;
    a = a + 2;
    return 0;
  }

  public int testSequence02() {
    int a;
    int b;
    a = 0;
    b = 0;
    java.lang.Math.abs(a);

    return 0;
  }

  public int testSequence03() {
    int a;
    int b;
    a = 0;
    b = 0;
    a = b;
    java.lang.Math.abs(a);

    return 0;
  }

  public int testConditional01() {
    int a;
    int b;

    a = 0;
    b = 0;

    if (b != 0) {
      b = b + 1;
    }
    else {
      b = a;
    }

    java.lang.Math.abs(a);

    return 0;
  }

  public int testConditional02() {
    int a;
    int b;

    a = 0;
    b = 0;

    if (b != 0) {
      b = a + 1;
    }
    else {
      a = 0;
    }

    java.lang.Math.abs(a);

    return 0;
  }

  public int testConditional03() {
    int a;
    int b;

    a = 0;
    b = 0;

    if (a != 0) {
      b = b + 1;
    }
    else {
      b = 0;
    }

    java.lang.Math.abs(a);

    return 0;
  }

  public int testLoop01() {
    int a;
    int b;

    a = 0;
    b = 0;

    for (a = 0; a <= 10; a++) {
      b = b + 1;
    }

    java.lang.Math.abs(a);

    return 0;
  }

  public int testLoop02() {
    int a;
    int b;

    a = 0;
    b = 0;

    for (b = 0; b <= 10; b++) {
      a = a + 1;
    }

    java.lang.Math.abs(a);

    return 0;
  }

  public int testLoop03() {
    int a;
    int b;

    a = 0;
    b = 0;

    for (b = 0; b <= 10; b++) {
      b = b + 1;
    }

    java.lang.Math.abs(a);

    return 0;
  }



}
