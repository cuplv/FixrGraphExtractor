package edu.colorado.plv.fixr.tests.acdfg

class TestTransEdgeSamples extends TestTransEdge("./src/test/resources/jimple:./src/test/resources/libs/android-17.jar",
  "androidtests.Samples", null) {

  override def getPackages() : List[String] =
    List[String]("android.")

  test("update")       {testTransEdge("update")}
  test("deleteRow")    {testTransEdge("deleteRow")}
  test("deleteNUsers") {testTransEdge("deleteNUsers")}
  test("retrieveData") {testTransEdge("retrieveData")}
}
