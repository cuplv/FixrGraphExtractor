package edu.colorado.plv.fixr.tests.cdfg

import scala.collection.JavaConversions.seqAsJavaList

import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.graphs.CDFGToDotGraph
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.slicing.APISlicer
import edu.colorado.plv.fixr.slicing.MethodPackageSeed
import soot.Body
import soot.toolkits.graph.pdg.EnhancedUnitGraph
import soot.util.dot.DotGraph
import edu.colorado.plv.fixr.tests.TestClassBase

/**
 * Test the cdfg construction.
 * @author: Sergio Mover
 * @mail: sergio.mover@colorado.edu
 */
abstract class TestCdfg(classPath : String, testClassName : String,
    resClassName : String) 
  extends TestClassBase(classPath, testClassName, resClassName) {

  /**
   * Get a list of packages to be included in the slice
   */
  def getPackages() : List[String];
  
  def testCdfg(methodName : String) : Unit = {
  	val body : Body = SootHelper.getMethodBody(testClassName, methodName);  

  	val jimpleUnitGraph : EnhancedUnitGraph = new EnhancedUnitGraph(body);
  	val slicer : APISlicer = new APISlicer(jimpleUnitGraph, body);
   	val slicedJimple : Body = slicer.slice(new MethodPackageSeed(getPackages()));   	
  	val cdfg : UnitCdfgGraph = new UnitCdfgGraph(slicedJimple);
  	
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
  	val toDot : CDFGToDotGraph = new CDFGToDotGraph(); 
  	val viewgraph : DotGraph = toDot.drawCFG(cdfg, cdfg.getBody()); 	     	
  	viewgraph.plot("/tmp/test_cdfg.dot");  	
	}
	
}
