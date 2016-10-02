package edu.colorado.plv.fixr.tests.acdfg

import edu.colorado.plv.fixr.abstraction.{Acdfg, GitHubRecord, ControlEdge,
  Edge, TransControlEdge, SourceInfo}

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
import org.slf4j.{Logger, LoggerFactory}


/**
  * TestAcdfgCreation
  *   Class implementing tests for ACFG Creation
  *
  *   @author Rhys Braginton Pettee Olsen <rhol9958@colorado.edu>
  *   @group  University of Colorado at Boulder CUPLV
  */

abstract class TestAcdfg(classPath : String, testClassName : String,
                         resClassName : String)
  extends TestClassBase(classPath, testClassName, resClassName) {

    /**
      * Get a list of packages to be included in the slice
      */
    def getPackages(): List[String]

    def testAcdfg(methodName: String): Unit = {
      val logger : Logger = LoggerFactory.getLogger(this.getClass)
      val body : Body = SootHelper.getMethodBody(testClassName, methodName)

      val jimpleUnitGraph : EnhancedUnitGraph = new EnhancedUnitGraph(body)
      val slicer : APISlicer = new APISlicer(jimpleUnitGraph, body)
      val slicedJimple : Body = slicer.slice(new MethodPackageSeed(getPackages()))
      val cdfg : UnitCdfgGraph = new UnitCdfgGraph(slicedJimple)

      // ACDFG
      val gr = GitHubRecord("a", "b", "c", "d")
      val si = SourceInfo("PackageName", "ClassName", "MethodName",
        1, 2, "SourceClassName", "AbsSourceFileName")
      val acdfg: Acdfg = new Acdfg(cdfg, gr, si)
      val newAcdfg = new Acdfg(acdfg.toProtobuf)
      info(acdfg.disjointUnion(newAcdfg).toString())
      assert(acdfg.==(newAcdfg))
    }    
  }
