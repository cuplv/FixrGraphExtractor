package edu.colorado.plv.fixr.tests.acdfg

import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.abstraction._
import edu.colorado.plv.fixr.graphs.{CDFGToDotGraph, UnitCdfgGraph}
import edu.colorado.plv.fixr.slicing.APISlicer
import edu.colorado.plv.fixr.slicing.MethodPackageSeed
import edu.colorado.plv.fixr.tests.TestClassBase
import org.slf4j.{Logger, LoggerFactory}
import soot.Body
import soot.toolkits.graph.pdg.EnhancedUnitGraph
import soot.util.dot.DotGraph

import scala.collection.JavaConversions._

/**
  * Created by cuplv on 7/25/16.
  */
abstract class TestTransEdge(classPath : String, testClassName : String,
                    resClassName : String)
  extends TestClassBase(classPath, testClassName, resClassName) {

  /**
    * Get a list of packages to be included in the slice
    */
  def getPackages(): List[String]

  def testTransEdge(methodName: String): Unit = {
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
    val gr = GitHubRecord("a", "b", "c", "d")
    val si = SourceInfo("PackageName", "ClassName", "MethodName",
      1, 2, "SourceClassName", "AbsSourceClassName"
    )
    val acdfg: Acdfg = new Acdfg(cdfg, gr, si, "app")
    // logger.debug("ACDFG: " + acdfg.toString)
    // logger.debug("Protobuf: " + acdfg.toProtobuf.toString)
    // logger.debug("Recovered ACDFG: " + new Acdfg(acdfg.toProtobuf).toString)
    val newAcdfg = new Acdfg(acdfg.toProtobuf)

    assert(acdfg.edges.exists { case (id : Long, edge: Edge) =>
      edge.isInstanceOf[TransControlEdge]
    })

    acdfg.edges.filter { case (id : Long, edge: Edge) =>
      edge.isInstanceOf[TransControlEdge]
    }.foreach { case (id : Long, edge: TransControlEdge) =>
      val ctrl = acdfg.edges.filter { case (id : Long, ctrl_edge: Edge) =>
        ctrl_edge.isInstanceOf[ControlEdge] || ctrl_edge.isInstanceOf[TransControlEdge]
      }
      assert(ctrl.exists { case (id : Long, from_edge: Edge) =>
        from_edge.from == edge.from
      })
      assert(ctrl.exists { case (id : Long, to_edge: Edge) =>
        to_edge.to == edge.to
      })
    }

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
