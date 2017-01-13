package edu.colorado.plv.fixr.tests.acdfg

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.abstraction.MiscNode
import edu.colorado.plv.fixr.abstraction.ConstDataNode
import edu.colorado.plv.fixr.abstraction.VarDataNode
import edu.colorado.plv.fixr.tests.TestClassBase
import edu.colorado.plv.fixr.abstraction.Acdfg
import edu.colorado.plv.fixr.abstraction.Predicates
import edu.colorado.plv.fixr.abstraction.MethodNode
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.abstraction.AcdfgToDotGraph
import edu.colorado.plv.fixr.simp.BodySimplifier
import soot.toolkits.graph.ExceptionalUnitGraph

class TestAcdfgSimp  extends TestClassBase("./src/test/resources/javasources",
  "simple.Simp", null) {

  def getCdfg(methodName : String) : UnitCdfgGraph = {
    val sootMethod = this.getTestClass().getMethodByName(methodName)
    val body = sootMethod.retrieveActiveBody()
    val simp : BodySimplifier = new BodySimplifier(new ExceptionalUnitGraph(body))
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(simp.getSimplifiedBody())
    
//    System.out.println(cdfg.getBody());    
//    SootHelper.dumpToDot(cdfg, cdfg.getBody(), "/tmp/cdfg.dot")
    
    cdfg        
  }
  
  def testRes(cdfg : UnitCdfgGraph, notInList : List[String],
      inList : List[String]) = {
    val bodyStr = cdfg.getBody().toString()
    val notIn = notInList.foldLeft(true)((res, elem) => bodyStr.indexOf(elem) < 0 && res)
    val in = inList.length == 0 ||
      inList.foldLeft(false)((res, elem) => bodyStr.indexOf(elem) > 0 || res) 
    assert(notIn && in)
  }
  
  def singleTest(methodName : String,
      notIn : List[String],
      in : List[String]) {
    val cdfg = getCdfg(methodName)
    testRes(cdfg, notIn, in)    
  }
  
//  test("ACDFGCast") {
//    val sootMethod = this.getTestClass().getMethodByName("testAppCast")
//    
//  }
  
  test("ACDFGAssignments") {
    singleTest("testAssignments", List("temp$0 = "), List("base = staticinvoke "))
  }
  
  test("ACDFGAssignments2") {
    singleTest("testAssignments2", 
        List("x = 1", "y = x", "z = y", "temp$0 = "),
        List("z = 1"))
  }

  test("ACDFGAssignments3") {
    singleTest("testAssignments3", 
        List("x = 1", "temp$0 = "),
        List())
  }

  test("ACDFGAssignments4") {
    singleTest("testAssignments4", 
        List("x = 1", "temp$0 = "),
        List("z = 3", "y = 1"))
  }

  test("ACDFGAssignments5") {
    singleTest("testAssignments5", 
        List("x = 1", "temp$0 = "),
        List("y = 1"))
  }

  test("ACDFGAssignments6") {
    singleTest("testAssignments6", 
        List("y = 1"),
        List("x = 1"))
  }          
}