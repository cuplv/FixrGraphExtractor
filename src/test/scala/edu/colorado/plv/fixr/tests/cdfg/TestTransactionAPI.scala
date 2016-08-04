package edu.colorado.plv.fixr.tests.cdfg 

class TestTransactionAPI extends TestCdfg("./src/test/resources/jimple:./src/test/resources/libs/android-17.jar",
    "androidtests.TransactionAPI",null) {
  
  override def getPackages() : List[String] = {
    return List[String]("android.")
  }
  
  test("syncUsers") {testCdfg("syncUsers")}
  test("wrongTransaction") {testCdfg("wrongTransaction")}
  test("rightTransaction") {testCdfg("rightTransaction")}
}