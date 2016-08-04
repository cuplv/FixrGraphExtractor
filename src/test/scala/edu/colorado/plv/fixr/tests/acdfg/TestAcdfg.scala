package edu.colorado.plv.fixr.tests.acdfg

import edu.colorado.plv.fixr.abstraction.{Acdfg, GitHubRecord, ControlEdge, Edge, TransControlEdge}

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

      // DEBUG - Print all the meaningful graphs
      // Original CFG
      SootHelper.dumpToDot(jimpleUnitGraph, jimpleUnitGraph.getBody(), "/tmp/cfg.dot")
      // Sliced CFG
      SootHelper.dumpToDot(new EnhancedUnitGraph(slicedJimple),
        slicedJimple, "/tmp/sliced_cfg.dot")
      // PDG
      SootHelper.dumpToDot(slicer.getPdg(),
        slicer.getCfg().getBody(), "/tmp/pdg.dot")
      // DDG
      SootHelper.dumpToDot(slicer.getDdg(),
        slicer.getCfg().getBody(), "/tmp/ddg.dot")
      // CDFG
      val toDot : CDFGToDotGraph = new CDFGToDotGraph()
      val viewgraph : DotGraph = toDot.drawCFG(cdfg, cdfg.getBody())
      viewgraph.plot("/tmp/test_cdfg.dot")
      // ACDFG
      val acdfg: Acdfg = new Acdfg(cdfg, GitHubRecord("a", "b", "c", "d"))
      // logger.debug("ACDFG: " + acdfg.toString)
      // logger.debug("Protobuf: " + acdfg.toProtobuf.toString)
      // logger.debug("Recovered ACDFG: " + new Acdfg(acdfg.toProtobuf).toString)
      val newAcdfg = new Acdfg(acdfg.toProtobuf)
      // logger.error("ACDFG: " + acdfg.toString)
      info(acdfg.disjointUnion(newAcdfg).toString())
      assert(acdfg.==(newAcdfg))

      /*
      val filePrefix: String = "/tmp/"

      val provFile: File = new File(filePrefix)
      if (!provFile.exists) {
        provFile.mkdir
      }

      val filename: Acdfg = "/tmp/acdfg.bin"
      val acdfgFile
      */
    }
  }
