package edu.colorado.plv.fixr.tests.slicing


import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.extractors.MethodExtractor
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions

class TestOverride extends FunSuite with BeforeAndAfter {
  before {
    SootHelper.reset();
  }

  test("testoverride") {
    val options : ExtractorOptions = new ExtractorOptions();
    options.className = "slice.TestOverride2"
    options.methodName = "callerMethod"
    options.readFromSources = true
    options.sliceFilter = List("java.util")
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
    transformer.acdfgListBuffer.foreach( (x : edu.colorado.plv.fixr.abstraction.Acdfg)  => {
      /* count the method nodes */    
      val count = x.nodes.foldLeft(0)( (count : Int, l : (Long, edu.colorado.plv.fixr.abstraction.Node) ) => {
        if (l._2.isInstanceOf[edu.colorado.plv.fixr.abstraction.MethodNode]) count + 1
        else count
      })

      assert(count == 3)
    })
  }
}
