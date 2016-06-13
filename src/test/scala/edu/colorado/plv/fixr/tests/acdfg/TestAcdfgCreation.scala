package edu.colorado.plv.fixr.tests.acdfg

import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.abstraction.Acdfg
import edu.colorado.plv.fixr.graphs.{CDFGToDotGraph, UnitCdfgGraph}
import edu.colorado.plv.fixr.slicing.{APISlicer, MethodPackageSeed}
import edu.colorado.plv.fixr.tests.TestClassBase
import soot.Body
import soot.toolkits.graph.pdg.EnhancedUnitGraph
import soot.util.dot.DotGraph


/**
  * TestAcdfgCreation
  *   Class implementing tests for ACFG Creation
  *
  *   @author Rhys Braginton Pettee Olsen <rhol9958@colorado.edu>
  *   @group  University of Colorado at Boulder CUPLV
  */

abstract class TestAcdfgCreation (classPath : String, testClassName : String,
                                  resClassName : String)
  extends TestClassBase(classPath, testClassName, resClassName) {
  assert(true)

  /**
    * Get a list of packages to be included in the slice
    */
  def getPackages() : List[String];

  def testCdfg(methodName : String) : Unit = {
    val body: Body = SootHelper.getMethodBody(testClassName, methodName);

    val jimpleUnitGraph: EnhancedUnitGraph = new EnhancedUnitGraph(body);
    val slicer: APISlicer = new APISlicer(jimpleUnitGraph, body);
    val slicedJimple: Body = slicer.slice(new MethodPackageSeed(getPackages()));
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(slicedJimple);

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
    val toDot: CDFGToDotGraph = new CDFGToDotGraph();
    val viewgraph: DotGraph = toDot.drawCFG(cdfg, cdfg.getBody());
    viewgraph.plot("/tmp/test_cdfg.dot");
    // ACDFG
    val acdfg: Acdfg = new Acdfg(cdfg)
  }
}
