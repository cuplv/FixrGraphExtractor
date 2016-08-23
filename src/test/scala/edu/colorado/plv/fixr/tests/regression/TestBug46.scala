package edu.colorado.plv.fixr.tests.regression

import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.extractors.MultipleExtractor
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions

class TestBug46 extends FunSuite with BeforeAndAfter {
  before {
    SootHelper.reset();
  }
  
  test("bug_046") {
  	val options : ExtractorOptions = new ExtractorOptions();
    options.className = "bugs.Bug_046";  
    options.methodName = "bug_046_method";
    options.readFromSources = true;
    options.sliceFilter = List("");
    options.sootClassPath = "./src/test/resources/javasources";
    options.outputDir = null;
    options.provenanceDir = null;
    options.processDir = null;

    var extractor : Extractor = new MultipleExtractor(options);
  } 
  
}