package edu.colorado.plv.fixr.tests.acdfg

import edu.colorado.plv.fixr.tests.TestClassBase
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.abstraction.ConstDataNode
import edu.colorado.plv.fixr.abstraction.{Acdfg, Predicates}
import edu.colorado.plv.fixr.abstraction.{MethodNode, VarDataNode}

class TestAcdfgPredicates extends TestClassBase("./src/test/resources/javasources",
  "simple.Predicates", null) {
  
  test("ACDFGIf") {
    val sootMethod = this.getTestClass().getMethodByName("testIf")

    val body = sootMethod.retrieveActiveBody()

    /* test the method call */
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null)

    def testRes(acdfg : Acdfg) = {
      val constNode = new VarDataNode(0, "b", "boolean")
      assert (AcdfgUnitTest.getNode(acdfg, constNode).size == 1)
      val methodNode = new MethodNode(1, None, None, Predicates.IS_TRUE, Vector())
      val useEdges = AcdfgUnitTest.getEdges(acdfg, constNode, methodNode)
      assert (useEdges.size == 1)
    }
    testRes(acdfg)

    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  } 
}