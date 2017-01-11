package edu.colorado.plv.fixr.tests.acdfg

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.abstraction.ConstDataNode
import edu.colorado.plv.fixr.abstraction.VarDataNode
import edu.colorado.plv.fixr.tests.TestClassBase
import edu.colorado.plv.fixr.abstraction.Acdfg
import edu.colorado.plv.fixr.abstraction.Predicates
import edu.colorado.plv.fixr.abstraction.MethodNode
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.abstraction.AcdfgToDotGraph

class TestAcdfgSimp  extends TestClassBase("./src/test/resources/javasources",
  "simple.Simp", null) {

  test("ACDFGCast") {
    val sootMethod = this.getTestClass().getMethodByName("testAppCast")

    val body = sootMethod.retrieveActiveBody()

    /* test the method call */
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    //SootHelper.dumpToDot(cdfg, cdfg.getBody(), "/tmp/cdfg.dot")
    val acdfg : Acdfg = new Acdfg(cdfg, null, null)
    //val g = new AcdfgToDotGraph(acdfg)
    //g.draw().plot("/tmp/acdfg.dot")

//    def testRes(acdfg : Acdfg) = {
//      val constNode0 = new ConstDataNode(0, "0", "int")
//      val varNode1 = new VarDataNode(0, "b", "boolean")
//      assert (AcdfgUnitTest.getNode(acdfg, constNode0).size == 1)
//      assert (AcdfgUnitTest.getNode(acdfg, varNode1).size == 1)
//      val methodNode1 = new MethodNode(1, None, None, Predicates.EQ, Vector())
//      val methodNode2 = new MethodNode(1, None, None, Predicates.NEQ, Vector())
//      val useEdges0 = AcdfgUnitTest.getEdges(acdfg, constNode0, methodNode1)
//      assert (useEdges0.size == 1)
//      val useEdges1 = AcdfgUnitTest.getEdges(acdfg, varNode1, methodNode2)
//      assert (useEdges1.size == 1)
//    }
//    testRes(acdfg)
//
//    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
//    testRes(acdfgFromProto)
  }
  
  test("ACDFGAssignments") {
    val sootMethod = this.getTestClass().getMethodByName("testAssignments")

    val body = sootMethod.retrieveActiveBody()

    /* test the method call */
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    SootHelper.dumpToDot(cdfg, cdfg.getBody(), "/tmp/cdfg.dot")
    val acdfg : Acdfg = new Acdfg(cdfg, null, null)
    val g = new AcdfgToDotGraph(acdfg)
    g.draw().plot("/tmp/acdfg.dot")

//    def testRes(acdfg : Acdfg) = {
//      val constNode0 = new ConstDataNode(0, "0", "int")
//      val varNode1 = new VarDataNode(0, "b", "boolean")
//      assert (AcdfgUnitTest.getNode(acdfg, constNode0).size == 1)
//      assert (AcdfgUnitTest.getNode(acdfg, varNode1).size == 1)
//      val methodNode1 = new MethodNode(1, None, None, Predicates.EQ, Vector())
//      val methodNode2 = new MethodNode(1, None, None, Predicates.NEQ, Vector())
//      val useEdges0 = AcdfgUnitTest.getEdges(acdfg, constNode0, methodNode1)
//      assert (useEdges0.size == 1)
//      val useEdges1 = AcdfgUnitTest.getEdges(acdfg, varNode1, methodNode2)
//      assert (useEdges1.size == 1)
//    }
//    testRes(acdfg)
//
//    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
//    testRes(acdfgFromProto)
  }
}