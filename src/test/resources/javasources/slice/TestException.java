package slice;

import java.lang.Math.*;

public class TestException {
  public void m1() {
    float[] d = new float[1];

    try {
        d[1] = Math.abs(2);
    }
    
    catch (IndexOutOfBoundsException e) {
	System.err.println("Caught IndexOutOfBoundsException: " +
            e.getMessage()
        );
	d[0] = Math.abs(9);
    }
    
    finally {
	d[0] = Math.abs(0);
    }
  }
}
