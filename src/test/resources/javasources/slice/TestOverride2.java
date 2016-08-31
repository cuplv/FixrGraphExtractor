package slice;

import slice.TestOverride;
import java.util.BitSet;

public class TestOverride2 extends TestOverride {

  public void callerMethod() {
    /* test overriden method by TestOverride */
    intersects(null);

    /* method overriden by TestOverride */
    flip(0);

    /* test covariant return type */
    get(0,0);
    get2(0,0);
  }

  /* test covariant return type */
  @Override
  public TestOverride get(int fromIndex, int toIndex) {
    return this;
  }

  /* not override */
  public int get2(int fromIndex, int toIndex) {
    return 0;
  }

  @Override
  public boolean intersects(BitSet set) {
    return true;
  }


}
