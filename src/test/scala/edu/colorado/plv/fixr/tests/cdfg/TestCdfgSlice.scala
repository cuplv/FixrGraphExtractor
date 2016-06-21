package edu.colorado.plv.fixr.tests.cdfg 

import edu.colorado.plv.fixr.tests.slicing.TestSlicing

class TestCdfgSlice extends TestCdfg("./src/test/resources/jimple",
    "slice.TestSlice", null) {
  
  override def getPackages() : List[String] =
    List[String]("java.lang.Math")
  
  test("cdfg_m1") {testCdfg("m1")} 
}
