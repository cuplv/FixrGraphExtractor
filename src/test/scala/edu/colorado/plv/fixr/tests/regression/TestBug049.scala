package edu.colorado.plv.fixr.tests.regression

import org.scalatest.BeforeAndAfter
import edu.colorado.plv.fixr.SootHelper
import org.scalatest.FunSuite
import edu.colorado.plv.fixr.extractors.MethodExtractor
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions
import edu.colorado.plv.fixr.tests.{TestClassBase, TestParseSources}
import edu.colorado.plv.fixr.slicing.APISlicer
import edu.colorado.plv.fixr.slicing.MethodPackageSeed
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph

import scala.collection.JavaConversions._
import scala.collection.JavaConversions.mapAsScalaMap
import soot.SootClass
import edu.colorado.plv.fixr.SootHelper
import soot.Scene
import soot.Body
import soot.Local
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.slicing.APISlicer
import soot.toolkits.graph.pdg.EnhancedUnitGraph

class TestBug049 extends TestClassBase("./src/test/resources/libs/android-17.jar:./src/test/resources/javasources",
    "bugs.Bug_049", null) {

  def testCDFG() : Unit = {
    val className = this.getTestClass().getName()
      val body : Body = SootHelper.getMethodBody(className,
          "bug")

      val jimpleUnitGraph : EnhancedUnitGraph = new EnhancedUnitGraph(body)
      val slicer : APISlicer = new APISlicer(jimpleUnitGraph, body)
      val slicedJimple : Body = slicer.slice(new MethodPackageSeed("android"))
      val cdfg : UnitCdfgGraph = new UnitCdfgGraph(slicedJimple)


      val useEdges : scala.collection.mutable.Map[Local,java.util.List[soot.Unit]] = mapAsScalaMap(cdfg.useEdges())

      val size = useEdges.values.foldLeft(0)({ (count,list) =>
        count + list.size()})

      assert(size < 6)
  }

   test("Bug049", TestParseSources) {testCDFG()}
}
