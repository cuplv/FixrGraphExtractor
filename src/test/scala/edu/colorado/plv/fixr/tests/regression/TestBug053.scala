package edu.colorado.plv.fixr.tests.regression

import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.extractors.MethodExtractor
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions
import edu.colorado.plv.fixr.tests.TestParseSources

class TestBug053 extends FunSuite with BeforeAndAfter {
  before {
    SootHelper.reset();
  }

  // Ignored --- parser used by soot is not updated for recent jvms
  ignore("bug_053", TestParseSources) {
    val options : ExtractorOptions = new ExtractorOptions();
    options.className = "bugs.Bug_053"
    options.methodName = "bug_053"
    options.configCode = SootHelper.READ_FROM_SOURCES
    options.sliceFilter = List("android")
    options.sootClassPath = "./src/test/resources/libs/android-17.jar:./src/test/resources/javasources/"
    options.outputDir = null
    options.provenanceDir = null
    options.processDir = null
    options.useJPhantom = false
    options.outPhantomJar = null

    var extractor : Extractor = new MethodExtractor(options);
    extractor.extract()
  }
}
