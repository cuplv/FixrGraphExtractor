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
  def extract() : Unit
  
  private def initExtractor() : Unit = {
    logger.debug("Initializing soot...")
    if (null == options.processDir) { 
      SootHelper.configure(options.sootClassPath, options.readFromJimple)
    }
    else {
      SootHelper.configure(options.sootClassPath, options.readFromJimple, options.processDir)      
    }
  }
  
  protected def extractMethod(sootClass : SootClass, sootMethod : SootMethod) : Unit = {
    logger.info("Extracting graph for - class {} - method: {}{}",
        sootClass.getName(), sootMethod.getName(), "")    
    
    val body: Body = sootMethod.retrieveActiveBody()
    val jimpleUnitGraph: EnhancedUnitGraph = new EnhancedUnitGraph(body)
    val slicer: APISlicer = new APISlicer(jimpleUnitGraph, body)
        
    var sc: SlicingCriterion = null
    if (null == options.sliceFilter)
      sc = MethodPackageSeed.createAndroidSeed() 
    else
      sc = new MethodPackageSeed(options.sliceFilter)
    
    logger.debug("Slicing...")
    val slicedJimple: Body = slicer.slice(sc)
    
    if (null == slicedJimple) {
      /* Do not print the graph for empty slices */
      logger.warn("Empty slice for - class {} - method: {}{}",
        sootClass.getName(), sootMethod.getName(), "")
      logger.warn("Empty slice for - class {} - method: {}\nFilter: {}\nBody:\n{}\n",
        sootClass.getName(), sootMethod.getName(), sc.getCriterionDescription(),
        body.toString())
    }
    else {
    	logger.debug("CDFG construction...")    
    	val cdfg: UnitCdfgGraph = new UnitCdfgGraph(slicedJimple)
    	logger.debug("ACDFG construction...")
    	val acdfg : Acdfg = new Acdfg(cdfg)

    	val name : String = sootClass.getName() + "_" +
    			sootMethod.getName();

    	logger.info("Writing data for - class {} - method: {}{}",
    			sootClass.getName(), sootMethod.getName(), "")
    	writeData(name, acdfg, cdfg, body, slicedJimple, slicer.getCfg());    
    	logger.info("Created graph for - class {} - method: {}{}",
    			sootClass.getName(), sootMethod.getName(), "")      
    }
  }
  
  protected def writeJimple(body : Body, fileName : String) : Unit = {
    val streamOut : OutputStream = new FileOutputStream(fileName); 
    val writerOut : PrintWriter = new PrintWriter(new OutputStreamWriter(streamOut));
    Printer.v().printTo(body, writerOut);
    writerOut.flush();
    streamOut.close();    
  }
  
  /**
   * Write the data to the output folder
   */
  private def writeData(outFileNamePrefix : String,
      acdfg : Acdfg,
      cdfg : UnitCdfgGraph,
      body : Body,
      slicedBody : Body,      
      cfg : UnitGraph) : Unit = {
    val currentDir = System.getProperty("user.dir")
    val outputDir = if (null == options.outputDir) currentDir else options.outputDir

    logger.debug("Writing ACDFG data to: {}", outputDir)

    // Write the acdfg
    val acdfgFileName : String = Paths.get(outputDir,
        outFileNamePrefix + ".acdfg.bin").toString();
    val output : FileOutputStream = new FileOutputStream(acdfgFileName)
    acdfg.toProtobuf.writeTo(output)
    output.close()
    
    // Write the povenance information
    if (options.provenanceDir != null) {
      logger.debug("Writing provenance data to: {}", options.provenanceDir)
      val filePrefix : String = Paths.get(options.provenanceDir, outFileNamePrefix).toString();

      try {
        val provFile : File = new File(filePrefix)
        if (!provFile.getParentFile.exists) {
          provFile.getParentFile.mkdir
        }
      }
      catch {
        case ex: Exception =>
          logger.error("Unable to create required new provenance directory")
      }

      // ACDFG DOT
      val acdfg_dot : String = filePrefix + ".acdfg.dot";
      val dotGraph : AcdfgToDotGraph = new AcdfgToDotGraph(acdfg)
      dotGraph.draw().plot(acdfg_dot)

      // CFG
      SootHelper.dumpToDot(cfg, cfg.getBody(), filePrefix + ".cfg.dot")

      // CDFG
      SootHelper.dumpToDot(cdfg, cdfg.getBody(), filePrefix + ".cdfg.dot")

      // JIMPLE
      val jimpleFileName: String = filePrefix + ".jimple";
      writeJimple(body, jimpleFileName)

      // SLICED JIMPLE
      val slicedJimpleName : String = filePrefix + ".sliced.jimple";
      writeJimple(slicedBody, slicedJimpleName)

      val provenance : Provenance = new Provenance(
        null,
        body,
        slicedBody,
        outFileNamePrefix,
        cfg,
        cdfg,
        acdfg
      )

      try {
        val provFileName : String = filePrefix + ".html"
        val provFile : File = new File(provFileName)
        val writer: Writer = new BufferedWriter(
          new OutputStreamWriter(
            new FileOutputStream(provFile),
            "utf-8"
          )
        )
        writer.write(provenance.toHtml.toString)
        writer.close()
      }
      catch {
        case ex: Exception =>
          logger.error("Unable to write to " + filePrefix + ".html")
      }
    }
  }
}
