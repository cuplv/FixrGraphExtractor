package edu.colorado.plv.fixr.tests.regression

import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.extractors.MultipleExtractor
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions
import edu.colorado.plv.fixr.tests.TestParseSources

class TestBug27 extends FunSuite with BeforeAndAfter {
  before {
    SootHelper.reset();
  }

  test("bug_027", TestParseSources) {
    val options : ExtractorOptions = new ExtractorOptions();
    options.className = "bugs.Bug_022";
    options.methodName = null;
    options.configCode = SootHelper.READ_FROM_SOURCES
    options.sliceFilter = List("");
    options.sootClassPath = "./src/test/resources/javasources";
    options.outputDir = null;
    options.provenanceDir = null;
    options.processDir = null;

    var extractor : Extractor = new MultipleExtractor(options);
  }

}
