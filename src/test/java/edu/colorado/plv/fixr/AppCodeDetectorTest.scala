package edu.colorado.plv.fixr

import org.scalatest.FunSuite

class AppCodeDetectorTest extends FunSuite {
  val awesomeApp = getClass.getResource("/apks/awesomeapp.apk")
  val javasources = List("HelloWorldActivity.java","Samples.java")
    .map(a => getClass.getResource(s"/javasources/androidtests/$a").getPath)
  assert(awesomeApp != null)
  test("testPackageListFromFile") {
    val packageList = AppCodeDetector.packageListFromFileList(javasources.mkString(":"))
    assert(packageList == "androidtests.HelloWorldActivity:androidtests.Samples")
  }

  test("testMainPackageFromApk") {
    val mainPackage = AppCodeDetector.mainPackageFromApk(awesomeApp.getPath)
    assert(mainPackage == "fixr.plv.colorado.edu.awesomeapp")
  }

}
