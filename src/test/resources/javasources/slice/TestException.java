package slice;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

public class TestException {
  /* Test the excaptional control flow */

  public void tryCatch() {
    int i = 0;

    try {
      java.lang.Math.abs(i);
    } catch (Exception e) {
      java.lang.Math.abs(i);
    }
  }

  public void tryCatchNotImportant() {
    int i = 0;

    try {
      java.lang.Math.abs(i);
    } catch (Exception e) {
      int j = 0;
    }
  }

  public void tryFinally() {
    int i = 0;
    int j = 1;
    int z = 1;
    try {
      java.lang.Math.abs(i);
    } finally {
      java.lang.Math.abs(j);
    }
  }

  public void tryCatchFinally() {
    int i = 0;
    int j = 1;
    int z = 1;
    try {
      java.lang.Math.abs(i);
    }
    catch (Exception e) {
      java.lang.Math.abs(z);
    } finally {
      java.lang.Math.abs(j);
    }
  }

  public void tryCatchNested() {
    int firstCatch = 1;
    int secondCatch = 1;

    try {
      try {
        int[] array = new int[10];
        java.lang.Math.abs(array[0]);
      }
      catch (IndexOutOfBoundsException e1) {
        java.lang.Math.abs(firstCatch);
      }
    }
    catch (Exception e2) {
      java.lang.Math.abs(secondCatch);
    }
  }

  // // public void nested() {
  // // }

  // public void testThrown () {
  //   PrintWriter pw = null;

  //   try {
  //     File file = new File("fubars.txt");
  //     FileWriter fw = new FileWriter(file, true);
  //     pw = new PrintWriter(fw);
  //     pw.println("Fubars rule!");
  //   } catch (IOException e) {
  //     e.printStackTrace();
  //   } finally {
  //     if (pw != null) {
  //       pw.close();
  //     }
  //   }
  // }
}
