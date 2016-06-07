package slice;

import java.util.Random;
import java.lang.Math.*;

public class TestSliceMethods {
  private int innerField1;
  private int innerField2;  

  private static int staticField = 0;
  
  /**
   * Multiple seeds for random API
   */
  public void testSliceMethods01()
  {
    int n1, n2;
    Random r = new Random(0);

    n2 = 1;
    n1 = 0;
    
    r.nextInt(n1);

    /* should not be in the slice */
    n1 = java.lang.Math.abs(n1);

    /* in the slide */
    r.setSeed(n2);
  }

  private int innerMethod(int a) {
    return a;
  }

  public void testSliceMethods02()
  {
    int n1;
    n1 = innerMethod(0);
    Random r = new Random(n1);
  }

  public void testSliceMethods03()
  {
    int n1;
    int n2;

    n2 = 0;
    n1 = 0;
    innerMethod(n2);

    Random r = new Random(n1);
  }

  public void testSliceMethods04()
  {
    int n1;

    n1 = 0;

    if (innerField1 == 0) {
      innerField1 = n1 + 1;
    }

    Random r = new Random(innerField1);
  }

  public void testSliceMethods05()
  {
    int[] intArray = new int[5];

    innerField1 = intArray[0];
    Random r = new Random(innerField1);
  }


  /* Cannot deal with double indirection in jimple
   * Assign the ref of intIntArray to an intermediate variable
   * Then the intermediate variable changes the content of the intIntArray
   *
   * We do not resolve the aliasing on the intermediate variable,
   * hence we lose the dependency.
   */
  public void testSliceMethods06()
  {
    int n1;
    int[] intArray = new int[5];
    int[][] intIntArray = new int[5][6];

    n1 = 0;
    intArray[0] = n1;
    intIntArray[1][1] = intArray[0];
    innerField1 = intIntArray[1][1];
    Random r = new Random(innerField1);
  }

      /* static field */
  /* n1 = staticField; */
}
