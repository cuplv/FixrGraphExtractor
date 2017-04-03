package edu.colorado.plv.fixr.tests.acdfg

import edu.colorado.plv.fixr.tests.TestClassBase
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.abstraction.Acdfg
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.abstraction.AcdfgToDotGraph
import edu.colorado.plv.fixr.abstraction.{MethodNode, ConstDataNode}

class TestAcdfgConstants() extends TestClassBase("./src/test/resources/javasources",
  "simple.Constants", null) {

  test("ACDFGConstant") {
    val sootMethod = this.getTestClass().getMethodByName("accessConstantVar")

    val body = sootMethod.retrieveActiveBody()

    /* test the method call */
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null, "app")

    def testRes(acdfg : Acdfg) = {
      val constNode = new ConstDataNode(0, "0", "int")
      assert (AcdfgUnitTest.getNode(acdfg, constNode).size == 1)
      val methodNode = new MethodNode(4, None, None, "java.lang.Math.abs", Vector())
      val useEdges = AcdfgUnitTest.getEdges(acdfg, constNode, methodNode)
      assert (useEdges.size == 1)
    }
    testRes(acdfg)

    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }
}
