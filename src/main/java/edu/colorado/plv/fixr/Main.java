package edu.colorado.plv.fixr;

import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;;

public class Main {
	
  public static void main(String[] args) {  
  	if (args.length != 3) {
  		System.err.println("Missing classpath, class name and method name");
  		return;
  	}
  	
  	String classPath = args[0];
  	String className = args[1];
  	String methodName = args[2];
  	
  	System.out.println(classPath);
  	SootHelper.configure(classPath);

  	Scene.v().addBasicClass(className, SootClass.HIERARCHY);
  	Scene.v().loadNecessaryClasses();

  	Body b = SootHelper.getMethodBody(className, methodName);

  	if (b instanceof JimpleBody) {
  		System.out.println(b.toString());
  	}
  	
  	BriefUnitGraph g = new BriefUnitGraph(b);  	
  	SootHelper.dumpToDot(g, b, "brief_unit_graph.dot");
  	
  	BriefBlockGraph g2 = new BriefBlockGraph(b);  	
  	SootHelper.dumpToDot(g2, b, "brief_block_graph.dot");
  	
  	ExceptionalBlockGraph g3 = new ExceptionalBlockGraph(b);   	
  	SootHelper.dumpToDot(g3, b, "exceptional_block_graph.dot");  	  	 	 

  	System.out.println("Done");
  }
}
