package simple;

public class TestException {

    public static void main(String[] args) {
      int a = 0;
      int b = 0;

      try {
        a = a / b;
      }
      catch (Exception e) {
        a = 0;
      }
      finally {
        b = 0;
      }

      java.lang.Math.abs(a + b);
    }

}
