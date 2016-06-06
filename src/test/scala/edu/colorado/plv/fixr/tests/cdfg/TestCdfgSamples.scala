package edu.colorado.plv.fixr.tests.cdfg 

class TestCdfgSamples extends TestCdfg("./src/test/resources/jimple:./src/test/resources/libs/android-17.jar",
    "androidtests.Samples", null) {
  
  override def getPackages() : List[String] = {
    return List[String]("android.")
  }
  
  test("update") {testCdfg("update")}
  test("deleteRow") {testCdfg("deleteRow")}
  test("deleteNUsers") {testCdfg("deleteNUsers")}
  test("retrieveData") {testCdfg("retrieveData")}  
}

