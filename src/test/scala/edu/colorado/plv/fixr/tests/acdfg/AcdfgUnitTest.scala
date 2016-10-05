package edu.colorado.plv.fixr.tests.acdfg

import edu.colorado.plv.fixr.tests.{TestClassBase}

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.abstraction.{Acdfg, AdjacencyList, EdgeLabel, FakeMethods}
import edu.colorado.plv.fixr.abstraction.{Node, Edge, MethodNode, UseEdge, MiscNode}

import soot.{SootClass, SootMethod, Body}
import soot.{IntType, Type, RefType, ArrayType, VoidType}
import soot.jimple.IntConstant
import soot.jimple.ConditionExpr
import soot.{Modifier}
import java.util.Arrays
import soot.jimple.JimpleBody
import soot.jimple.Jimple
import edu.colorado.plv.fixr.SootHelper
import soot.Scene
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import soot.options.Options
import scala.collection.JavaConversions._
import edu.colorado.plv.fixr.abstraction.AcdfgToDotGraph
import edu.colorado.plv.fixr.abstraction.ExceptionalControlEdge
import edu.colorado.plv.fixr.abstraction.{DataNode, VarDataNode, ConstDataNode}
import edu.colorado.plv.fixr.abstraction.AcdfgToDotGraph

/**
  * The class invokes Soot and parses the acdfg.UnitTest class in the testClass
  * field (can be accessed with getTestClass.
  *
  * The test cases are obtained by changing the test class using the soot API
  * (e.g. see https://github.com/Sable/soot/wiki/Creating-a-class-from-scratch)
  *
  * This is done for every new test (to not analyze a dirty environment)
  *
  */
class AcdfgUnitTest() extends TestClassBase("./src/test/resources/jimple",
  "acdfg.UnitTest", null) {

  def getEmptyMethod() : SootMethod = {
    val refType = RefType.v("java.lang.String")
    val f = ArrayType.v(refType, 1)
    val paramArray = Array[Type](f)

    val method : SootMethod = new SootMethod("myTest", paramArray.toList,
      VoidType.v(), Modifier.PUBLIC);

    val body : soot.jimple.JimpleBody = Jimple.v().newBody(method);
    method.setActiveBody(body);

    /* add the this pointer */
    val thisRef = Jimple.v().newThisRef(testClass.getType)
    val thisVar = SootHelper.addLocal(body, "this", testClass.getType)

    /* add the identity statement to init this */
    val units = body.getUnits
    val identityStmt = soot.jimple.Jimple.v().newIdentityStmt(thisVar, thisRef)
    units.add(identityStmt);

    this.getTestClass().addMethod(method)

    method
  }

  test("ACDFGMethodCall") {
    val body = getEmptyMethod.getActiveBody

    /* instantiate locals */
    val l0 = SootHelper.addLocal(body, "l0", IntType.v())
    val l1 = SootHelper.addLocal(body, "l1", IntType.v())
    val l2 = SootHelper.addLocal(body, "l2", IntType.v())

    body.getUnits().add(Jimple.v().newAssignStmt(l0, IntConstant.v(0)))
    body.getUnits().add(Jimple.v().newAssignStmt(l1, IntConstant.v(0)))
    body.getUnits().add(Jimple.v().newAssignStmt(l2, IntConstant.v(0)))

    /* this.testMethod(l0,l1,l2) */
    val toCall : SootMethod = this.getTestClass().getMethodByName("testMethod")
    val virtualInvoke = Jimple.v().newVirtualInvokeExpr(body.getThisLocal,
      toCall.makeRef(), List(l0,l1,l2))
    val invokeStmt = Jimple.v().newInvokeStmt(virtualInvoke)
    body.getUnits().add(invokeStmt);
    body.getUnits.add(Jimple.v().newReturnVoidStmt())

    /* test the method call */
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null)

    val l0Node = new VarDataNode(0, "l0", "int")
    val l1Node = new VarDataNode(1, "l1", "int")
    val l2Node = new VarDataNode(2, "l2", "int")
    val thisNode = new VarDataNode(3, "this", "acdfg.UnitTest")
    val callNode = new MethodNode(4, None, Some(3), "acdfg.UnitTest.testMethod", Vector(0,1,2))

    def testRes(acdfg : Acdfg) = {
      assert (AcdfgUnitTest.getNode(acdfg, callNode).size == 1)
      assert (AcdfgUnitTest.getNode(acdfg, l0Node).size == 1)
      assert (AcdfgUnitTest.getNode(acdfg, l1Node).size == 1)
      assert (AcdfgUnitTest.getNode(acdfg, l2Node).size == 1)
      assert (AcdfgUnitTest.getNode(acdfg, thisNode).size == 1)

      assert(AcdfgUnitTest.getEdges(acdfg, l0Node, callNode).size == 1)
      assert(AcdfgUnitTest.getEdges(acdfg, l1Node, callNode).size == 1)
      assert(AcdfgUnitTest.getEdges(acdfg, l2Node, callNode).size == 1)
      assert(AcdfgUnitTest.getEdges(acdfg, thisNode, callNode).size == 1)
    }
    testRes(acdfg)

    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }


  test("ACDFGDom") {
    val body = getEmptyMethod.getActiveBody
    val l0 = SootHelper.addLocal(body, "l0", IntType.v())

    val toCallA : SootMethod = this.getTestClass().getMethodByName("testMethodA")
    val toCallB : SootMethod = this.getTestClass().getMethodByName("testMethodB")
    val toCallC : SootMethod = this.getTestClass().getMethodByName("testMethodC")
    val toCallD : SootMethod = this.getTestClass().getMethodByName("testMethodD")

    val nA = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(body.getThisLocal, toCallA.makeRef(), List[soot.Value]()))
    val nB = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(body.getThisLocal, toCallB.makeRef(), List[soot.Value]()))
    val nC = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(body.getThisLocal, toCallC.makeRef(), List[soot.Value]()))
    val nD = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(body.getThisLocal, toCallD.makeRef(), List[soot.Value]()))
    val goto= Jimple.v().newGotoStmt(Jimple.v().newStmtBox(nD))
    val condition = Jimple.v().newEqExpr(l0, IntConstant.v(0))

    val nif = Jimple.v().newIfStmt(condition, nB)

    body.getUnits.add(nA)
    body.getUnits.add(nif)
    body.getUnits.add(nC)
    body.getUnits.add(goto)
    body.getUnits.add(nB)
    body.getUnits.add(nD)
    body.getUnits.add(Jimple.v().newReturnVoidStmt())

    /* test the method call */
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null)

    val nA_ = new MethodNode(4, None, Some(3), "acdfg.UnitTest.testMethodA", Vector())
    val nB_ = new MethodNode(4, None, Some(3), "acdfg.UnitTest.testMethodB", Vector())
    val nC_ = new MethodNode(4, None, Some(3), "acdfg.UnitTest.testMethodC", Vector())
    val nD_ = new MethodNode(4, None, Some(3), "acdfg.UnitTest.testMethodD", Vector())

    assert (AcdfgUnitTest.getNode(acdfg, nA_).size == 1)
    assert (AcdfgUnitTest.getNode(acdfg, nB_).size == 1)
    assert (AcdfgUnitTest.getNode(acdfg, nC_).size == 1)
    assert (AcdfgUnitTest.getNode(acdfg, nD_).size == 1)

    def testRes(acdfg : Acdfg) = {
      val domList = List(EdgeLabel.SRC_DOMINATE_DST)
      val edges0 = AcdfgUnitTest.getEdges(acdfg, nA_, nB_)
      assert(edges0.size == 1 && AcdfgUnitTest.hasLabel(acdfg, edges0.get(0), domList))
      val edges1 = AcdfgUnitTest.getEdges(acdfg, nA_, nC_)
      assert(edges1.size == 1 && AcdfgUnitTest.hasLabel(acdfg, edges1.get(0), domList))
      val edges2 = AcdfgUnitTest.getEdges(acdfg, nA_, nD_)
      assert(edges2.size == 1 && AcdfgUnitTest.hasLabel(acdfg, edges2.get(0), List(EdgeLabel.SRC_DOMINATE_DST, EdgeLabel.DST_POSDOMINATE_SRC)))

      val edges3 = AcdfgUnitTest.getEdges(acdfg, nB_, nD_)
      assert(edges3.size == 1 && AcdfgUnitTest.hasLabel(acdfg, edges3.get(0), List(EdgeLabel.DST_POSDOMINATE_SRC)))
      val edges4 = AcdfgUnitTest.getEdges(acdfg, nB_, nD_)
      assert(edges4.size == 1 && AcdfgUnitTest.hasLabel(acdfg, edges4.get(0), List(EdgeLabel.DST_POSDOMINATE_SRC)))
    }
    testRes(acdfg)

    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }

  test("ACDFGExceptionalFlow") {
    val sootMethod = this.getTestClass().getMethodByName("testMethodE")

    val body = sootMethod.retrieveActiveBody()

    /* test the method call */
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null)

    def testRes(acdfg : Acdfg) {
      val absNode = new MethodNode(4, None, None, "java.lang.Math.abs", Vector())
      val caughtNode = new MethodNode(4, None, None, "java.lang.Math.abs", Vector())
      val absNodes = AcdfgUnitTest.getNode(acdfg, absNode)
      assert (absNodes.size == 1)

      val node = absNodes.get(0)
      val nodeEdges = AcdfgUnitTest.getEdges(acdfg, node)
      assert (nodeEdges.size == 5)
      val exEdges = nodeEdges.filter (x => x.isInstanceOf[ExceptionalControlEdge])
      assert (exEdges.size == 1)
    }
    testRes(acdfg)
    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }

  test("ACDFGVoidReturn") {
    val sootMethod = this.getTestClass().getMethodByName("voidMethod")
    val body = sootMethod.retrieveActiveBody()
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null)

    val returnNode = new MethodNode(4, None, None, FakeMethods.RETURN_METHOD, Vector())

    def testRes(acdfg : Acdfg) {
      assert (AcdfgUnitTest.getNode(acdfg, returnNode).size == 1)
    }
    testRes(acdfg)
    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }

  test("ACDFGReturn") {
    val sootMethod = this.getTestClass().getMethodByName("testMethodA")
    val body = sootMethod.retrieveActiveBody()
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null)

    val retNode = new MethodNode(4, None, None, FakeMethods.RETURN_METHOD, Vector())
    val constNode = new ConstDataNode(0, "0", "int")

    def testRes(acdfg : Acdfg) {
      val retNodes = AcdfgUnitTest.getNode(acdfg, retNode)
      assert (retNodes.size == 1)
      val constNodes = AcdfgUnitTest.getNode(acdfg, constNode)
      assert (constNodes.size == 1)
      val useEdges = AcdfgUnitTest.getEdges(acdfg, constNode, retNode)
      assert (useEdges.size == 1)
    }
    testRes(acdfg  : Acdfg)
    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }

  test("ACDFGFieldAccess") {
    val sootMethod = this.getTestClass().getMethodByName("testFieldAccess")
    val body = sootMethod.retrieveActiveBody()
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null)

    def testRes(acdfg : Acdfg) {
      val access = new MethodNode(4, None, None,
        FakeMethods.GET_METHOD + ".acdfg.UnitTest.pero_int", Vector())
      val nodes = AcdfgUnitTest.getNode(acdfg, access)
      assert (nodes.size == 1)
    }

    testRes(acdfg  : Acdfg)
    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }

  test("ACDFGFieldSet") {
    val sootMethod = this.getTestClass().getMethodByName("testFieldSet")
    val body = sootMethod.retrieveActiveBody()
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(body)
    val acdfg : Acdfg = new Acdfg(cdfg, null, null)

    def testRes(acdfg : Acdfg) {
      val set = new MethodNode(4, None, None,
        FakeMethods.SET_METHOD +".acdfg.UnitTest.pero_int", Vector())
      val nodes = AcdfgUnitTest.getNode(acdfg, set)
      assert (nodes.size == 1)
    }

    testRes(acdfg  : Acdfg)
    val acdfgFromProto = new Acdfg(acdfg.toProtobuf)
    testRes(acdfgFromProto)
  }
}

object AcdfgUnitTest {

  /**
    * True if the acdfg has the edge from fromNode to toNode
    *
    * The check disregard the edge id.
    * Warning: the lookup on nodes is *NOT* unique (some nodes are not
    * distinguishable if we disregard the id)
    *
    */
  def getEdges(acdfg : Acdfg, fromNode : Node, toNode : Node) : List[Edge] = {
    val nodesFrom = AcdfgUnitTest.getNode(acdfg, fromNode)
    val nodesTo = AcdfgUnitTest.getNode(acdfg, toNode)

    val edgeList = nodesFrom.foldLeft(List[Edge]()) ( (edgeList, fromNode) => {
      nodesTo.foldLeft(edgeList) ( (edgeList, toNode) => {
        acdfg.edges.foldLeft (edgeList) {
          (edgeList, pair) => {
            val edge = pair._2
            val toNodeEdge : Node = acdfg.nodes.get(edge.to) match {
              case Some(n : Node) => n
              case _ => throw new RuntimeException("Missing node")
            }
            val fromNodeEdge : Node = acdfg.nodes.get(edge.from) match {
              case Some(n : Node) => n
              case _ => throw new RuntimeException("Missing node")
            }

            if (AcdfgUnitTest.eqNodes(toNodeEdge, toNode) &&
              AcdfgUnitTest.eqNodes(fromNodeEdge, fromNode)) {edge :: edgeList}
            else edgeList
          }
        }
      }) /* foldLeft on nodesTo */
    }) /* foldLeft on nodesFrom */

    edgeList
  }

  def getEdges(acdfg : Acdfg, fromNode : Node) : List[Edge] = {
    val nodesFrom = AcdfgUnitTest.getNode(acdfg, fromNode)
    val edgeList = nodesFrom.foldLeft(List[Edge]()) ( (edgeList, fromNode) => {
      acdfg.edges.foldLeft (edgeList) ({
        (res,edge) => if (edge._2.from == fromNode.id) edge._2 :: res else res
      })
    })
    edgeList
  }


  /**
    * Eq n1 and n2.
    * The equality is shallow, since it does not compare subnodes (they are ids)
    */
  def eqNodes(n1 : Node, n2 : Node) : Boolean = {
    val v = Vector(1,2,3)

    def compareArgs(v1 : Vector[Long], v2 : Vector[Long], index : Int) : Boolean = {
      assert (v1.size == v2.size)
      if (v1.size >= index) true
      else if (v1.get(index) != v2.get(index)) false
      else compareArgs(v1,v2, index + 1)
    }

    if (n1.getClass() != n2.getClass) false
    else {
      (n1,n2) match {
        case (n1 : DataNode, n2 : DataNode) => n1.name == n2.name && 0 == n1.datatype.compareTo(n2.datatype)
        case (n1 : MethodNode, n2 : MethodNode) => n1.name == n2.name
        case (n1 : MiscNode, n2 : MiscNode) => true
      }
    }
  }

  /**
    * Get all the nodes in acdfg that match node.
    * */
  def getNode(acdfg : Acdfg, node : Node) : List[Node] = {
    def getNodeAux(acdfg : Acdfg, node : Node, res : List[Node]) : List[Node] = {
      acdfg.nodes.foldLeft (res) ( (res, pair) => {
        val nodeAcdfg = pair._2
        if (AcdfgUnitTest.eqNodes(node, nodeAcdfg)) {
          nodeAcdfg :: res
        }
        else res
      })
    }
    getNodeAux(acdfg, node, List[Node]())
  }

  def hasLabel(acdfg : Acdfg,  e : Edge, refLabels : List[EdgeLabel.Value]) = {
    acdfg.edgesLabel.get(e.id) match {
      case Some(labelSet : Acdfg.LabelsSet) => {
        val refSet = scala.collection.immutable.HashSet[EdgeLabel.Value]() ++ refLabels
        labelSet.equals(refSet)
      }
      case _ => false
    }
  }

}
