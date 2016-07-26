package edu.colorado.plv.fixr.tests.acdfg

class TestAcdfgSamples extends TestAcdfg("./src/test/resources/jimple:./src/test/resources/libs/android-17.jar",
  "androidtests.Samples", null) {

  override def getPackages() : List[String] =
    List[String]("android.")

  test("update")       {testAcdfg("update")}
  test("deleteRow")    {testAcdfg("deleteRow")}
  test("deleteNUsers") {testAcdfg("deleteNUsers")}
  test("retrieveData") {testAcdfg("retrieveData")}
}
