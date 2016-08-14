package edu.colorado.plv.fixr.tests.slicing 

class SlicingTestMethods extends TestSlicing("./src/test/resources/jimple",
    "slice.TestSliceMethods", "slice.TestSliceMethodsRes") {

  override def getPackages() : List[String] = {
    return List[String]("java.util.Random")
  }

  test("testSliceMethods01") {testSlice("testSliceMethods01")}
  test("testSliceMethods02") {testSlice("testSliceMethods02")}
  test("testSliceMethods03") {testSlice("testSliceMethods03")}
  test("testSliceMethods04") {testSlice("testSliceMethods04")}
  test("testSliceMethods05") {testSlice("testSliceMethods05")}

  /* still not pass */
  ignore("testSliceMethods06") {testSlice("testSliceMethods06")}
}
