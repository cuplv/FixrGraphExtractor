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


  public void testIf() {
    int i = 0;
    int j = 0;

    if (i <= 10) {
      i = i + 1;
    }
    else {
      j = j + 1;
    }

    java.lang.Math.abs(i);
  }

  public void testLoop() {
    int i;
    int j;

    j = 0;
    for (i = 0; i <= 10;) {
      j = j;
    }

    java.lang.Math.abs(i);
  }

  public void testLoopData() {
    int i;

    for (i = 0; i <= 10;) {
      i = i + 1;
    }

    java.lang.Math.abs(i);
  }

  public void testLoopDataIf() {
    int i;
    int j = 0;
    int k = 0;

    for (i = 0; i <= 10;) {
      if (j == 0) {
        k = i + 1;
      }
      else {
        i = i -1;
      }
    }

    java.lang.Math.abs(i);
  }

  public void testSwitch() {
    int i = 0;

    switch (i) {
    case 0:
      i = i + 0;
      break;        
    case 3:
      i = i + 1;
      break;
    default:
      i = i + 2;
    }

    java.lang.Math.abs(i);
  }

  public void testSwitch2() {
    int i = 0;

    switch (i) {
    case 0:
      i = i + 0;
    case 1:
      i = i + 1;
    default:
      i = i + 2;
    }

    java.lang.Math.abs(i);
  }

  public void testLoopNested() {
    int i;
    int j = 0;
    int k = 0;

    for (i = 0; i <= 10;) {

      j = j + 2;

      for (j = j + 1; j <= 10;) {
        k = k + 1;
      }

      i = i + 1;
    }

    java.lang.Math.abs(i);
    System.out.println("" + k);
  }  

  public void testBreak() {
    int i;
    for (i = 0; i <= 10;) {
      if (i == 9) {
        break;
      }
    }
    
    java.lang.Math.abs(i);
  }

  public void testBreakData() {
    int i;
    for (i = 0; i <= 10;) {
      i = i + 1;
      if (i == 9) {
        break;
      }
    }
    
    java.lang.Math.abs(i);
  }

  public void testContinue() {
    int i;
    File file = null;

    for (i = 0; i <= 10; i++) {
      if (i == 9) {
        i = i + 1;

        continue;
      }
      i = i + 2;
    }
    
    file = new File("fubars.txt" + String.valueOf(i));
  }


  public void tryCatch() {
    File file = null;

    try {
      file = new File("fubars.txt");
    } catch (NullPointerException e) {
      System.out.println("Catch");
    }
  }

  public void tryFinally() {
    File file = null;

    try {
      file = new File("fubars.txt");
    } finally {
      System.out.println("Finally");
    }
  }

  public void tryCatchFinally() {
    File file = null;

    try {
      file = new File("fubars.txt");
    } catch (NullPointerException e) {
      System.out.println("Catch");
    } finally {
      System.out.println("Finally");
    }
  }

  // public void nested() {
  // }

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
