package simple;

/**
 * Test for handling of constants in the ACDFG
 *
 */
public class Constants {
  public static final int constantInt = 0;

  public void accessConstant()
  {
    /* use of a constant field */
    java.lang.Math.abs(0);
  }

  public void accessConstantVar()
  {
    final int constantIntLocal = 0;

    /* use of a constant field */
    java.lang.Math.abs(constantIntLocal);
  }

  public void accessConstantField()
  {
    /* use of a constant field */
    java.lang.Math.abs(constantInt);
  }
}
