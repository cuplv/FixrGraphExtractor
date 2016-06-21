package edu.colorado.plv.fixr.tests.slicing

class TestSliceFirst extends TestSlicing("./src/test/resources/jimple", "slice.TestSlice", "slice.TestSliceRes") {
  
  override def getPackages() : List[String] = {
    return List[String]("java.lang.Math")
  }

  test("sliceT1") {testSlice("m1")}
  test("sliceT2") {testSlice("m2")}
  test("sliceT3") {testSlice("m3")}  
  test("sliceT4") {testSlice("m4")}
  test("sliceT5") {testSlice("m5")}
  test("sliceT6") {testSlice("m6")}
  test("sliceT7") {testSlice("m7")}  
  test("sliceT8") {testSlice("m8")}  
}