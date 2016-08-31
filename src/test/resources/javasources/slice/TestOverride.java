package slice;

import java.util.BitSet;

public class TestOverride extends java.util.BitSet {

  @Override
  public boolean intersects(BitSet set) {
    return true;
  }

  @Override
  public void flip(int i) {
    return;
  }
}
