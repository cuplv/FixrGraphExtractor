package edu.colorado.plv.fixr.tests.cdfg 


class TestSimple extends TestCdfg("./src/test/resources/jimple",
    "simple.TestException", null) {
  
  override def getPackages() : List[String] = {
    List[String]("java.lang.Math")
  }
  
  test("testSimple") {testCdfg("main")}
}
