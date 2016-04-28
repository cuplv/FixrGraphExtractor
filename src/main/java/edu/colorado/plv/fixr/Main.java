package edu.colorado.plv.fixr;

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph;
import edu.colorado.plv.fixr.slicing.APISlicer;
import edu.colorado.plv.fixr.slicing.MethodPackageSeed;
import edu.colorado.plv.fixr.slicing.SlicingCriterion;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;

public class Main {
	
	/**
	 * Now the program takes as input the classpath, the class name and the method name for which we have to build the graph
	 * 
	 * @param args classpath, class name (e.g. package.ClassName), method name
	 */
  public static void main(String[] args) {  
  	if (args.length != 3) {
  		System.err.println("Missing classpath, class name and method name");
  		return;
  	}
  	
  	String classPath = args[0];
  	String className = args[1];
  	String methodName = args[2];
  	
  	/* Configure soot */
  	SootHelper.configure(classPath, false);
  	/* trick to make soot happy */
  	Scene.v().addBasicClass(className, SootClass.HIERARCHY);
  	/* load dependencies */
  	Scene.v().loadNecessaryClasses();

  	/* Get the method body (in jimple)  and perform the slicing */
  	Body body = SootHelper.getMethodBody(className, methodName);
  	EnhancedUnitGraph jimpleUnitGraph= new EnhancedUnitGraph(body);
  	APISlicer slicer = new APISlicer(jimpleUnitGraph, body);
  	SlicingCriterion sc = MethodPackageSeed.createAndroidSeed();
   	Body slicedJimple = slicer.slice(sc);
  	
  	/* build the CdFG graph */
  	UnitCdfgGraph cdfg = new UnitCdfgGraph(slicedJimple); 
  	
  	/* Dump the sliced graph */
  	SootHelper.dumpToDot(cdfg, cdfg.getBody(), "sliced_graph.dot");	  	 
  	
  	System.out.println("Done");
  }
}
