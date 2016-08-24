package edu.colorado.plv.fixr.tests.regression

import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.extractors.MethodExtractor;
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions

class TestBug46 extends FunSuite with BeforeAndAfter {
  before {
    SootHelper.reset();
  }

  test("bug_046") {
    val options : ExtractorOptions = new ExtractorOptions();
    options.className = "bugs.Bug_046"
    options.methodName = "getLog"
    options.readFromSources = false
    options.sliceFilter = List("android")
    options.sootClassPath = "./src/test/resources/libs/android-17.jar:./src/test/resources/classes"
    options.outputDir = null
    options.provenanceDir = null
    options.processDir = null
    options.useJPhantom = false
    options.outPhantomJar = null

    var extractor : Extractor = new MethodExtractor(options);
    extractor.extract()
  }
}
