package edu.colorado.plv.fixr.extractors

import soot.Scene
import soot.SootClass
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.slicing.SlicingCriterion
import edu.colorado.plv.fixr.abstraction.AcdfgToDotGraph
import edu.colorado.plv.fixr.slicing.APISlicer
import java.io.{IOException, Writer, _}

import soot.SootMethod
import edu.colorado.plv.fixr.slicing.MethodPackageSeed
import soot.toolkits.graph.pdg.EnhancedUnitGraph
import edu.colorado.plv.fixr.abstraction.Acdfg
import soot.Body
import soot.Printer
import java.nio.file.Paths

import soot.toolkits.graph.UnitGraph
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.provenance.Provenance

import scala.collection.JavaConversions.seqAsJavaList
import org.slf4j.LoggerFactory
import org.slf4j.Logger

/**
  * Base class that extracts an ACDFG from source code
  * 
  * @author Sergio Mover
  */
abstract class Extractor(options : ExtractorOptions) {
  val logger : Logger = LoggerFactory.getLogger(this.getClass())
  initExtractor()
  
  /**
    * Extract the ACDFG - implement the right iteration
    */
  def extract() : Unit;

  private def initExtractor() : Unit = {
    logger.debug("Initializing soot...")
    if (null == options.processDir) {
      SootHelper.configure(options.sootClassPath, options.readFromSources)
    }
    else {
      SootHelper.configure(options.sootClassPath, options.readFromSources, options.processDir)
    }
  }
}
