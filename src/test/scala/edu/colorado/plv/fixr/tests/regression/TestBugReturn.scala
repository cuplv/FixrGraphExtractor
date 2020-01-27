package edu.colorado.plv.fixr.tests.regression

import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.extractors.MethodExtractor
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions
import edu.colorado.plv.fixr.tests.TestParseSources

class TestBugReturn extends FunSuite with BeforeAndAfter {
  before {
    SootHelper.reset();
  }

  test("testreturn", TestParseSources) {
    val options : ExtractorOptions = new ExtractorOptions();
    options.className = "bugs.BugReturn"
    options.methodName = "bug_return"
    options.configCode = SootHelper.READ_FROM_BYTECODE
    options.sliceFilter = List("java")
    options.sootClassPath = ":./src/test/resources/classes"
    options.outputDir = "/tmp/"
    options.provenanceDir = "/tmp/provenance"
    options.processDir = null
    options.useJPhantom = false
    options.outPhantomJar = null
    options.userName = ""
    options.repoName = ""
    options.commitHash =""
    options.url = ""
        
    var extractor : Extractor = new MethodExtractor(options);
    extractor.extract()
  }
}
