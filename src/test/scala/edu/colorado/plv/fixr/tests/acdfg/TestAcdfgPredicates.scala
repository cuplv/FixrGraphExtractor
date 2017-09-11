package edu.colorado.plv.fixr.tests.acdfg

import edu.colorado.plv.fixr.tests.TestClassBase
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.abstraction.ConstDataNode
import edu.colorado.plv.fixr.abstraction.{Acdfg, Predicates}
import edu.colorado.plv.fixr.abstraction.{MethodNode, VarDataNode}
import edu.colorado.plv.fixr.abstraction.AcdfgToDotGraph
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.tests.TestParseSources

class TestAcdfgPredicates extends TestClassBase("./src/test/resources/javasources",
  "simple.Predicates", null) {

  test("ACDFGIf",TestParseSources) {
    val sootMethod = this.getTestClass().getMethodByName("testIf")

    val body = sootMethod.retrieveActiveBody()

    /* test the method call */
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    // SootHelper.dumpToDot(cdfg, cdfg.getBody(), "/tmp/cdfg.dot")
    val acdfg : Acdfg = new Acdfg(cdfg, null, null, "app")
    //val g = new AcdfgToDotGraph(acdfg)
    //g.draw().plot("/tmp/acdfg.dot")

    def testRes(acdfg : Acdfg) = {
      val constNode0 = new ConstDataNode(0, "0", "int")
      val varNode1 = new VarDataNode(0, "b", "boolean")
      assert (AcdfgUnitTest.getNode(acdfg, constNode0).size == 1)
      assert (AcdfgUnitTest.getNode(acdfg, varNode1).size == 1)
      val methodNode1 = new MethodNode(1, None, None, Predicates.EQ, Vector())
      val methodNode2 = new MethodNode(1, None, None, Predicates.NEQ, Vector())
      val useEdges0 = AcdfgUnitTest.getEdges(acdfg, constNode0, methodNode1)
      assert (useEdges0.size == 1)
      val useEdges1 = AcdfgUnitTest.getEdges(acdfg, varNode1, methodNode2)
      assert (useEdges1.size == 1)
    }
    testRes(acdfg)

    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }


  test("ACDFGLookupSwitch",TestParseSources) {
    val sootMethod = this.getTestClass().getMethodByName("testLookupSwitch")

    val body = sootMethod.retrieveActiveBody()

    /* test the method call */
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null, "app")

    def testRes(acdfg : Acdfg) = {
      val const2 = new ConstDataNode(0, "2", "int")
      val const10 = new ConstDataNode(0, "10", "int")
      val varNode1 = new VarDataNode(0, "switchVal", "int")

      assert (AcdfgUnitTest.getNode(acdfg, const2).size == 1)
      assert (AcdfgUnitTest.getNode(acdfg, const10).size == 1)
      assert (AcdfgUnitTest.getNode(acdfg, varNode1).size == 1)

      val eqNode = new MethodNode(1, None, None, Predicates.EQ, Vector())
      assert (AcdfgUnitTest.getNode(acdfg, eqNode).size == 2)

      /* WARNING: the count is 2 because we have two method nodes with EQ
       * The testing function in AcdfgUnitTest do not distinguish methods
       * with different arguments */
      assert(AcdfgUnitTest.getEdges(acdfg, const2, eqNode).size == 2)
      assert(AcdfgUnitTest.getEdges(acdfg, const10, eqNode).size == 2)
      assert(AcdfgUnitTest.getEdges(acdfg, varNode1, eqNode).size == 4)
    }
    testRes(acdfg)

    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }

  test("ACDFGTableSwitch",TestParseSources) {
    val sootMethod = this.getTestClass().getMethodByName("testTableSwitch")

    val body = sootMethod.retrieveActiveBody()

    /* test the method call */
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null, "app")

    def testRes(acdfg : Acdfg) = {
      val const0 = new ConstDataNode(0, "0", "int")
      val const1 = new ConstDataNode(0, "1", "int")
      val varNode1 = new VarDataNode(0, "switchVal", "int")

      assert (AcdfgUnitTest.getNode(acdfg, const0).size == 1)
      assert (AcdfgUnitTest.getNode(acdfg, const1).size == 1)
      assert (AcdfgUnitTest.getNode(acdfg, varNode1).size == 1)

      val eqNode = new MethodNode(1, None, None, Predicates.EQ, Vector())
      assert (AcdfgUnitTest.getNode(acdfg, eqNode).size == 2)

      /* WARNING: the count is 2 because we have two method nodes with EQ
       * The testing function in AcdfgUnitTest do not distinguish methods
       * with different arguments */
      assert(AcdfgUnitTest.getEdges(acdfg, const0, eqNode).size == 2)
      assert(AcdfgUnitTest.getEdges(acdfg, const1, eqNode).size == 2)
      assert(AcdfgUnitTest.getEdges(acdfg, varNode1, eqNode).size == 4)
    }
    testRes(acdfg)

    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }

}
