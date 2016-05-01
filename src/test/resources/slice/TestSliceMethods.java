package slice;

import java.util.Random;
import java.lang.Math.*;

public class TestSliceMethods {
  private int innnerField1;
  private int innnerField2;  

  
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

    if (innnerField1 == 0) {
      innnerField1 = n1 + 1;
    }

    Random r = new Random(innnerField1);
  }  
}
