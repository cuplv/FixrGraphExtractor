package edu.colorado.plv.fixr.tests.regression

import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.extractors.MethodExtractor;
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions

class TestBug47 extends FunSuite with BeforeAndAfter {
  before {
    SootHelper.reset();
  }

  ignore("bug_047") {
    val options : ExtractorOptions = new ExtractorOptions();
    options.className = "bugs.Bug_047"
    options.methodName = "bug_047_method"
    options.configCode = SootHelper.READ_FROM_SOURCES
    options.sliceFilter = List("android")
    options.sootClassPath = ":./src/test/resources/javasources"
    options.outputDir = "/tmp/"
    options.provenanceDir = null
    options.processDir = null
    options.useJPhantom = false
    options.outPhantomJar = null

    var extractor : Extractor = new MethodExtractor(options);
    extractor.extract()
  }
}
