package edu.colorado.plv.fixr

import org.scalatest.junit.JUnitWrapperSuite
import org.scalatest.{FunSuite, Matchers, FlatSpec}

/*
 * Tester.scala
 * Harness for performing ScalaTest tests against the graph extractor
 * @author: Rhys Braginton Pettee Olsen <rhol9958@colorado.edu>
 */

// @RunWith(classOf[JUnitRunner])
class Tester extends FunSuite with Matchers {
  /* Harness for executing old JUnit tests in ScalaTest
   * Please DO NOT write further JUnit tests
   */
  // "cdfgTest" should "pass" in {
  test("Harness for JUnit") {
    val cdfgTest = new CdfgTest()
    var jUnitWrapperSuite = new JUnitWrapperSuite(
      cdfgTest.getClass.getName,
      cdfgTest.getClass.getClassLoader
    )
    jUnitWrapperSuite.execute(stats = true)
  // }

  // "SlicingTestBase" should "pass" in {
    val slicingTestBase = new SlicingTestBase()
    jUnitWrapperSuite = new JUnitWrapperSuite(
      slicingTestBase.getClass.getName,
      slicingTestBase.getClass.getClassLoader
    )
    jUnitWrapperSuite.execute(stats = true)
  // }

  // "SlicingTestCF" should "pass" in {
    val slicingTestCF = new SlicingTestCF()
    jUnitWrapperSuite = new JUnitWrapperSuite(
      slicingTestCF.getClass.getName,
      slicingTestCF.getClass.getClassLoader
    )
    jUnitWrapperSuite.execute(stats = true)
  // }

  // "TestSlicing" should "pass" in {
    val testSamples = new TestSamples()
    jUnitWrapperSuite = new JUnitWrapperSuite(
    testSamples.getClass.getName,
    testSamples.getClass.getClassLoader
    )
    jUnitWrapperSuite.execute(stats = true)
  // }
  }
}
