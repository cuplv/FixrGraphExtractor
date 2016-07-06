package edu.colorado.plv.fixr.tests.cdfg

import java.io.ByteArrayOutputStream
import java.nio.file.Paths

import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.abstraction.Acdfg
import edu.colorado.plv.fixr.graphs.{CDFGToDotGraph, UnitCdfgGraph}
import edu.colorado.plv.fixr.protobuf.ProtoAcdfg.Acdfg.MethodNode
import edu.colorado.plv.fixr.slicing.{APISlicer, MethodPackageSeed}
import soot.Body
import soot.util.dot.DotGraph

import scala.collection.mutable

class TestCdfgDot extends TestCdfg("./src/test/resources/jimple",
    "slice.TestSlice", null) {
  
  override def getPackages() : List[String] = {
    return List[String]("java.lang.Math")
  }

  def testSlice(methodName : String) : Unit = {
    assert(testClass != null)
    assert(resClass != null)

    val bodyToSlice: Body = testClass.getMethodByName(methodName).retrieveActiveBody();
    val expectedRes: Body = resClass.getMethodByName(APISlicer.getSlicedMethodName(methodName)).retrieveActiveBody();

    val slicer: APISlicer = new APISlicer(bodyToSlice);
    val slicedBody: Body = slicer.slice(new MethodPackageSeed(getPackages()));

    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(slicedBody)
    // val filePrefix : String = Paths.get("/tmp/" + methodName).toString()

    val toDot: CDFGToDotGraph = new CDFGToDotGraph
    val viewgraph: DotGraph = toDot.drawCFG(
      cdfg.asInstanceOf[UnitCdfgGraph],
      slicedBody
    )

    val baos = new ByteArrayOutputStream()

    viewgraph.render(baos, 0)
    val cdfgDotString = baos.toString
    val cdfgDotLines = cdfgDotString.split("\n")
    val acdfg = new Acdfg(cdfg)

    var idToNodeNumber = new mutable.HashMap[Long, Int]()

    acdfg.nodes.values.filter(_.isInstanceOf[MethodNode]).foreach { methodNode =>
      val isIn = cdfgDotString.contains(methodNode.asInstanceOf[MethodNode].getName)
      assert(cdfgDotString.contains(methodNode.asInstanceOf[MethodNode].getName))
      if (isIn) {
        val line = cdfgDotLines.find(_.contains(methodNode.asInstanceOf[MethodNode])).get
        val nodeNumber = line.split("\"")(1).toInt
        idToNodeNumber += ((methodNode.id, nodeNumber))
      }
    }
    acdfg.edges.values.filter(edge =>
      idToNodeNumber.contains(edge.from) || idToNodeNumber.contains(edge.to)
    ).foreach {
      case edu.colorado.plv.fixr.abstraction.DefEdge =>
        "    \"" + 3 + "\"->\"" + 1 + "\" [color=blue,];"
    }
    }

    cdfgDotString.contains()

    assert(null != slicedBody) /* at least one seed in the test*/
    /* obtained and expected results must be the same */
    assert(SootHelper.compareBodies(expectedRes, slicedBody))
  }


  test("cdfg_m1") {testCdfg("m1")} 
}
