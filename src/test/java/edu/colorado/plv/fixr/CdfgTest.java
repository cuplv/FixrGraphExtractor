package edu.colorado.plv.fixr;

import org.junit.Before;
import org.junit.Test;

import edu.colorado.plv.fixr.graphs.CDFGToDotGraph;
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph;
import edu.colorado.plv.fixr.slicing.APISlicer;
import edu.colorado.plv.fixr.slicing.MethodPackageSeed;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;
import soot.util.dot.DotGraph;


public class CdfgTest {
	static final String CLASS_NAME = "slice.TestSlice";	
	
	@Before
	public void setup()
	{
		SootHelper.reset();					
		
		SootHelper.configure("src/test/resources/jimple", true);
	}
	
	public void testCdfg(String className, String methodName) {		
		Scene.v().addBasicClass(className, SootClass.HIERARCHY);  	
  	Scene.v().loadNecessaryClasses();
  	Body body = SootHelper.getMethodBody(className, methodName);  

  	EnhancedUnitGraph jimpleUnitGraph= new EnhancedUnitGraph(body);
  	APISlicer slicer = new APISlicer(jimpleUnitGraph, body);  	
   	Body slicedJimple = slicer.slice(new MethodPackageSeed("java.lang.Math"));
   	
  	UnitCdfgGraph cdfg = new UnitCdfgGraph(slicedJimple);
  	CDFGToDotGraph toDot = new CDFGToDotGraph(); 
  	DotGraph viewgraph = toDot.drawCFG(cdfg, cdfg.getBody()); 	     	
  	viewgraph.plot("test_cdfg.dot");  	
	}
	
	@Test
	public void cdfgT1()	
	{
		testCdfg(CLASS_NAME,"m1");		
	}			
}
