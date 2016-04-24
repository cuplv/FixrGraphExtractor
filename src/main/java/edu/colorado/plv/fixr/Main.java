package edu.colorado.plv.fixr;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.pdg.EnhancedBlockGraph;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;
import edu.colorado.plv.fixr.slicing.APISlicer;
import edu.colorado.plv.fixr.slicing.RelevantVariablesAnalysis;

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

  	/* get the method body (in jimple) */
  	Body body = SootHelper.getMethodBody(className, methodName);

  	EnhancedUnitGraph jimpleUnitGraph= new EnhancedUnitGraph(body);
  	
  	
  	/* Perform the slicing */
  	APISlicer slicer = new APISlicer(jimpleUnitGraph, body);
  	BlockGraph slicedCFG = slicer.slice();
  	
  	/* Dump the CFG to dot */
  	//EnhancedBlockGraph ebg = new EnhancedBlockGraph(body);
  	//SootHelper.dumpToDot(ebg, body, "enanched_block_graph.dot");	
  	
  	SootHelper.dumpToDot(jimpleUnitGraph, body, "enanched_unit_graph.dot");
  	//RelevantVariablesAnalysis rv = new RelevantVariablesAnalysis(jimpleUnitGraph);
  	
  	System.out.println("Done");
  }
}
