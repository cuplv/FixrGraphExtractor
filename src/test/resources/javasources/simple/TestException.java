package simple;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

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


  public void testThrown () {
    PrintWriter pw = null;

    try {
      File file = new File("fubars.txt");
      FileWriter fw = new FileWriter(file, true);
      pw = new PrintWriter(fw);
      pw.println("Fubars rule!");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (pw != null) {
        pw.close();
      }
    }
  }
}
