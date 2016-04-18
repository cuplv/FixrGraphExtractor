package edu.colorado.plv.fixr;

import soot.Body;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

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
  	
  	Body b = SootHelper.getMethodBody(className, methodName);
  	DirectedGraph g = new BriefUnitGraph(b);
  	SootHelper.dumpToDot(g, b, "~/test.dot");  	
  	
  	System.out.println("Done");
  }
  
}
