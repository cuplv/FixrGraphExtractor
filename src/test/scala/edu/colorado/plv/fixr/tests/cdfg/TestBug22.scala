package edu.colorado.plv.fixr.tests.cdfg

import edu.colorado.plv.fixr.extractors._;
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper

/**
 * Regression tests for bug 22
 */
class TestBug22 extends FunSuite with BeforeAndAfter {


  def testExtraction(className : String, classPath : String) =
  {
    val options : ExtractorOptions = new ExtractorOptions();
      options.className = className;
      options.methodName = null;
      options.readFromSources = true;
      options.sliceFilter = List("");
      options.sootClassPath = classPath;
      options.outputDir = null;
      options.provenanceDir = null;
      options.processDir = null;

      var extractor : Extractor = new MultipleExtractor(options);
      extractor.extract()
  }

  before {
    SootHelper.reset();
  }

  test("abstract_method") {testExtraction("bugs.Bug_022",
    "./src/test/resources/javasources")}
  test("interface") {testExtraction("bugs.Bug_022_02",
    "./src/test/resources/javasources")}
}
