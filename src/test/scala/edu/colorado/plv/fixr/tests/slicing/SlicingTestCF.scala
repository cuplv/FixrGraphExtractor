package edu.colorado.plv.fixr.tests.slicing

class SlicingTestCF extends TestSlicing("./src/test/resources/jimple",
    "slice.TestControlFlow", "slice.TestControlFlowRes") {
  
  override def getPackages() : List[String] = {
    return List[String]("java.lang.Math")
  }

  test("testSequence01") {testSlice("testSequence01")}
  test("testSequence02") {testSlice("testSequence02")}
  test("testSequence03") {testSlice("testSequence03")}
  test("testConditional01") {testSlice("testConditional01")}
  test("testConditional02") {testSlice("testConditional02")}
  test("testConditional03") {testSlice("testConditional03")}
  test("testLoop01") {testSlice("testLoop01")}
  test("testLoop02") {testSlice("testLoop02")}
  test("testLoop03") {testSlice("testLoop03")}
}