package edu.colorado.plv.fixr.tests.regression

import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.extractors.MultipleExtractor
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions

class TestBug27 extends FunSuite with BeforeAndAfter {
  before {
    SootHelper.reset();
  }
  
  test("bug_027") {
  	val options : ExtractorOptions = new ExtractorOptions();
    options.className = "bugs.Bug_022";  
    options.methodName = null;
    options.readFromSources = true;
    options.sliceFilter = "";
    options.sootClassPath = "./src/test/resources/javasources";
    options.outputDir = null;
    options.provenanceDir = null;
    options.processDir = null;

    var extractor : Extractor = new MultipleExtractor(options);
  } 
  
}