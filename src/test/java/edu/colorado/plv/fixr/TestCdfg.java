package edu.colorado.plv.fixr;

import static org.junit.Assert.assertTrue;

import java.util.Collection;

import edu.colorado.plv.fixr.graphs.CDFGToDotGraph;
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph;
import edu.colorado.plv.fixr.slicing.APISlicer;
import edu.colorado.plv.fixr.slicing.MethodPackageSeed;
import soot.Body;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;
import soot.util.dot.DotGraph;

public abstract class TestCdfg extends TestClassBase {

	public TestCdfg() {
		// TODO Auto-generated constructor stub
	}

	public abstract Collection<String> getPackages();
	
	public void testCdfg(String methodName) {		
		String className = getTestClassName();
		assertTrue(null != className);
  	
  	Body body = SootHelper.getMethodBody(getTestClassName(), methodName);  

  	EnhancedUnitGraph jimpleUnitGraph = new EnhancedUnitGraph(body);
  	APISlicer slicer = new APISlicer(jimpleUnitGraph, body);
   	Body slicedJimple = slicer.slice(new MethodPackageSeed(getPackages()));   	
  	UnitCdfgGraph cdfg = new UnitCdfgGraph(slicedJimple);
  	
  	// DEBUG - Print all the meaningful graphs
  	// Original CFG
  	SootHelper.dumpToDot(jimpleUnitGraph, jimpleUnitGraph.getBody(), "/tmp/cfg.dot");
  	// Sliced CFG
  	SootHelper.dumpToDot(new EnhancedUnitGraph(slicedJimple),
  			slicedJimple, "/tmp/sliced_cfg.dot");
  	// PDG
  	SootHelper.dumpToDot(slicer.getPdg(),
  			slicer.getCfg().getBody(), "/tmp/pdg.dot");  	
  	// DDG
  	SootHelper.dumpToDot(slicer.getDdg(),
  			slicer.getCfg().getBody(), "/tmp/ddg.dot");  	
  	// CDFG
  	CDFGToDotGraph toDot = new CDFGToDotGraph(); 
  	DotGraph viewgraph = toDot.drawCFG(cdfg, cdfg.getBody()); 	     	
  	viewgraph.plot("/tmp/test_cdfg.dot");  	
	}
	
}
