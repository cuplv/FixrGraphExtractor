package edu.colorado.plv.fixr.tests.slicing

class TestSliceException extends TestSlicing("./src/test/resources/jimple",
    "slice.TestException", "slice.TestExceptionRes") {

  override def getPackages() : List[String] = {
    return List[String]("java.lang.Math")
  }

  test("TestException:tryCatch") {testSlice("tryCatch")}
  test("TestException:tryCatchNotImportant") {testSlice("tryCatchNotImportant")}
  test("TestException:tryFinally") {testSlice("tryFinally")}
  test("TestException:tryCatchFinally") {testSlice("tryCatchFinally")}
  test("TestException:tryCatchNested") {testSlice("tryCatchNested")}
}
