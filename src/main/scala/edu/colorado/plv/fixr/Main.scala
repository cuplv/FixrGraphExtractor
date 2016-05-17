package edu.colorado.plv.fixr

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.slicing.APISlicer
import edu.colorado.plv.fixr.slicing.MethodPackageSeed
import edu.colorado.plv.fixr.slicing.SlicingCriterion
import edu.colorado.plv.fixr.abstraction.Acdfg
import soot.Body
import soot.Scene
import soot.SootClass
import soot.toolkits.graph.pdg.EnhancedUnitGraph

object Main {
  /**
    * Now the program takes as input the classpath, the class name and the method name for which we have to build the graph
    *
    * @param args classpath, class name (e.g. package.ClassName), method name
    */
  def main(args: Array[String]) {
    if (args.length < 3) {
      System.err.println("Missing classpath, class name and method name")
      return
    }
    var filter: String = null
    if (args.length >= 4) {
      filter = args(3)
    }
    val classPath: String = args(0)
    val className: String = args(1)
    val methodName: String = args(2)
    SootHelper.configure(classPath, false)
    Scene.v.addBasicClass(className, SootClass.HIERARCHY)
    Scene.v.loadNecessaryClasses
    val body: Body = SootHelper.getMethodBody(className, methodName)
    val jimpleUnitGraph: EnhancedUnitGraph = new EnhancedUnitGraph(body)
    val slicer: APISlicer = new APISlicer(jimpleUnitGraph, body)
    var sc: SlicingCriterion = null
    if (filter == null) {
      sc = MethodPackageSeed.createAndroidSeed
    }
    else {
      sc = new MethodPackageSeed(filter)
    }
    val slicedJimple: Body = slicer.slice(sc)
    if (null == slicedJimple) {
      System.out.println("Cannot find a relevant method call for slicing")
    }
    else {
      val cdfg: UnitCdfgGraph = new UnitCdfgGraph(slicedJimple)
      // SootHelper.dumpToDot(cdfg, cdfg.getBody, cdfg.getBody.getMethod.getName + ".dot")
      val acdfg : Acdfg = new Acdfg(cdfg)
    }
    System.out.println("Done")
  }
}
