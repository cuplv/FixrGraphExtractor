package edu.colorado.plv.fixr.extractors

import soot.SootClass
import soot.SootMethod
import soot.Body
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import java.io.BufferedWriter
import java.io.PrintWriter
import java.io.OutputStreamWriter

import edu.colorado.plv.fixr.slicing.SlicingCriterion
import edu.colorado.plv.fixr.abstraction.{Acdfg, AcdfgToDotGraph, GitHubRecord}
import edu.colorado.plv.fixr.slicing.APISlicer
import java.io.FileOutputStream

import edu.colorado.plv.fixr.provenance.Provenance
import edu.colorado.plv.fixr.slicing.MethodPackageSeed
import soot.toolkits.graph.pdg.EnhancedUnitGraph
import java.nio.file.Paths

import soot.toolkits.graph.UnitGraph
import org.slf4j.LoggerFactory
import java.io.Writer
import java.io.OutputStream

import org.slf4j.Logger
import soot.Printer
import java.io.File

import soot.BodyTransformer
import soot.options.Options
import soot.PhaseOptions
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future



class MethodsTransformer(options : ExtractorOptions) extends BodyTransformer {
  val logger : Logger = LoggerFactory.getLogger(this.getClass())

  override protected def internalTransform(body : Body,
      phase : String,
      transformOpt : java.util.Map[String,String] ) : Unit = {
    val method : SootMethod = body.getMethod()
    val sootClass : SootClass = method.getDeclaringClass()
    
    if (method.isConcrete() &&
      (method.getName() == options.methodName &&
        sootClass.getName() == options.className) ||
      (null == options.methodName && null != options.className) ||
      (null != options.processDir)) {
      try {
        if (options.to > 0) {
          val executor : ExecutorService = Executors.newSingleThreadExecutor()
          var future : Future[Unit] = null
          

          try {
            future = executor.submit(new TimeOutExecutor(this, sootClass, method))
            logger.info("Started thread...")
            System.out.println(future.get(options.to, TimeUnit.SECONDS))
            logger.info("Finished thread...")
          } catch {
            case e : TimeoutException => {
              logger.info("Thread timed out.")
            }
            case e : Exception => {
              logger.error("Error processing class {}, method {}{}",
        	sootClass.getName(), method.getName(), "");
              logger.error("Exception {}:", e)
            }
          } finally {
            future.cancel(true)
            executor.shutdownNow()
          }
        }
        else {
          // No timeout
          extractMethod(sootClass, method)
        }
      }
      catch {
        case e : Exception => {
          logger.error("Error processing class {}, method {}{}",
            sootClass.getName(), method.getName(), "");
          logger.error("Exception {}:", e)
        }
        case e : StackOverflowError => {
          logger.error("StackOverflowError processing class {}, method {}{}",
            sootClass.getName(), method.getName(), "");
          logger.error("Exception {}:", e)
        }
      }
    }
    else {
      if (! method.isConcrete()) {
        logger.warn("Skipped non-concrete method {} of class {}{}",
          method.getName(), sootClass.getName(), "")
      }
    }
  }
  
  def extractMethod(sootClass : SootClass, sootMethod : SootMethod) : Unit = {
    logger.info("Extracting graph for - class {} - method: {}{}",
      sootClass.getName(), sootMethod.getName(), "")
    
    assert(sootMethod.isConcrete());
    
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
      val acdfg : Acdfg = new Acdfg(cdfg, GitHubRecord(
        options.userName, options.repoName, options.url, options.commitHash
      ))

      val name : String = sootClass.getName() + "_" +
      sootMethod.getName();

      if (null != options.outputDir) {
    	logger.info("Writing data for - class {} - method: {}{}",
    	  sootClass.getName(), sootMethod.getName(), "")
    	writeData(name, acdfg, cdfg, body, slicedJimple, slicer.getCfg());
    	logger.info("Created graph for - class {} - method: {}{}",
    	  sootClass.getName(), sootMethod.getName(), "")
      }
      else {
    	logger.warn("Disabled data writing for - class {} - method: {}{}",
    	  sootClass.getName(), sootMethod.getName(), "")
      }
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



  private class TimeOutExecutor(transformer : MethodsTransformer,
    sootClass : SootClass, sootMethod : SootMethod)
      extends Callable[Unit] {
    
    @throws(classOf[Exception])
    override def call() : Unit = {
      try {
        transformer.extractMethod(sootClass, sootMethod);
      }
      catch {
        case e : StackOverflowError => {
          logger.error("StackOverflowError processing class {}, method {}{}", sootClass.getName(), sootMethod.getName(), "")
          logger.error("Exception {}:", e)
        }
      }
      
      return;
    }
  }
}
