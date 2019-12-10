package edu.colorado.plv.fixr.tests.regression

import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.abstraction.Acdfg
import edu.colorado.plv.fixr.extractors.MethodExtractor
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions
import edu.colorado.plv.fixr.tests.TestParseSources

class TestBug058 extends FunSuite with BeforeAndAfter {
  before {
    SootHelper.reset();
  }

  test("bug_058", TestParseSources) {
    val options : ExtractorOptions = new ExtractorOptions();
    options.className = "bugs.Bug_058"
    options.methodName = "bug_058"
    options.configCode = SootHelper.READ_FROM_BYTECODE    
    options.sliceFilter = List("android")
    options.sootClassPath = "./src/test/resources/libs/android-17.jar:./src/test/resources/javasources"
    options.outputDir = null
    options.provenanceDir = null
    options.processDir = null
    options.useJPhantom = false
    options.outPhantomJar = null
    options.storeAcdfg = true

    var extractor : Extractor = new MethodExtractor(options);
    extractor.extract()

    val transformer = extractor.getTransformer
    transformer.acdfgListBuffer.foreach( (x : edu.colorado.plv.fixr.abstraction.Acdfg)  => {

      val count = x.nodes.foldLeft(0)( (count : Int, l : (Long, edu.colorado.plv.fixr.abstraction.Node) ) => {
        if (l._2.isInstanceOf[edu.colorado.plv.fixr.abstraction.MethodNode]) {
          val methodNode = l._2.asInstanceOf[edu.colorado.plv.fixr.abstraction.MethodNode]
          if (! Acdfg.isFakeMethod(methodNode.name)) {
            count + 1
          }
          else {
            count
          }
        }
        else count
      })

      assert(count == 1)
    })
  }
}
