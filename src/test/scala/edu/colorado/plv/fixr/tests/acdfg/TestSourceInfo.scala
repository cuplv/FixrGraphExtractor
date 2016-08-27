package edu.colorado.plv.fixr.tests.acdfg

import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.extractors.MethodExtractor;
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions

class TestSourceInfo extends FunSuite with BeforeAndAfter {
  before {
    SootHelper.reset();
  }

  test("testSourceInfo_01") {
    SootHelper.reset();
    val options : ExtractorOptions = new ExtractorOptions();
    options.className = "slice.TestControlFlow"
    options.methodName = "testSequence01"
    options.readFromSources = false
    options.sliceFilter = List("java.lang.Math")
    options.sootClassPath = "./src/test/resources/javasources"
    options.outputDir = null
    options.provenanceDir = null
    options.processDir = null
    options.useJPhantom = false
    options.outPhantomJar = null
    options.storeAcdfg = true

    var extractor : Extractor = new MethodExtractor(options);
    extractor.extract()

    val transformer = extractor.getTransformer
    transformer.acdfgListBuffer.foreach( x => {
      val sourceInfo = x.getSourceInfo

      /* source lines from sources does not work still */
      assert(sourceInfo.packageName == "slice" &&
        sourceInfo.className == "slice.TestControlFlow" &&
        sourceInfo.methodName == "testSequence01" &&
        sourceInfo.sourceClassName == "TestControlFlow.java")
    })
  }

  test("testSourceInfo_02") {
    SootHelper.reset();
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
    options.storeAcdfg = true

    var extractor : Extractor = new MethodExtractor(options);
    extractor.extract()

    val transformer = extractor.getTransformer
    transformer.acdfgListBuffer.foreach( x => {
      val sourceInfo = x.getSourceInfo

      /* source lines from sources does not work still */
      assert(sourceInfo.packageName == "bugs" &&
        sourceInfo.className == "bugs.Bug_046" &&
        sourceInfo.methodName == "getLog" &&
        sourceInfo.classLineNumber == 0 &&
        sourceInfo.methodLineNumber == 18 &&
        sourceInfo.sourceClassName == "Bug_046.java")
    })
  }

}
