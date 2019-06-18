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
import edu.colorado.plv.fixr.abstraction.{Acdfg, AcdfgToDotGraph, GitHubRecord, SourceInfo}
import edu.colorado.plv.fixr.slicing.APISlicer
import java.io.FileOutputStream
import edu.colorado.plv.fixr.extractors.slicing.JimpleSlicer

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

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import edu.colorado.plv.fixr.simp.BodySimplifier
import soot.toolkits.graph.ExceptionalUnitGraph

class MethodsTransformer(options : ExtractorOptions) extends BodyTransformer {
  val acdfgListBuffer : ListBuffer[Acdfg] = ListBuffer[Acdfg]()
  val logger : Logger = LoggerFactory.getLogger(this.getClass())

  override protected def internalTransform(body : Body,
    phase : String,
    transformOpt : java.util.Map[String,String] ) : Unit = {
    val method : SootMethod = body.getMethod()
    val sootClass : SootClass = method.getDeclaringClass()
    val className : String = sootClass.getName()

    /* filter methods to extract by package */
    val skipMethod = null != options.extractFromPackages &&
      ! (options.extractFromPackages.foldLeft (false) ( (r,e) => className.startsWith(e) || r))

    if (skipMethod) {
      logger.info("Skipping " + className)
      return
    }

    if (method.isConcrete() &&
      (! className.startsWith("android.")) &&
      (! className.startsWith("androidx.")) &&
      (! className.startsWith("com.google.android.")) &&
      (! className.startsWith("com.google.protobuf.")) &&
      (! className.startsWith("com.android.")) &&
      (! className.startsWith("com.googlecode.")) &&
      ((method.getName() == options.methodName && sootClass.getName() == options.className) ||
        (null == options.methodName && null == options.className && (null != options.processDir))
      )) {
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

  def sliceBody(sc : SlicingCriterion, jimpleUnitGraph: EnhancedUnitGraph,
    body : Body) : Option[(APISlicer, Body)] = {

    logger.info("Creating the slicer...")
    val slicer: APISlicer = new APISlicer(jimpleUnitGraph, body)
    logger.info("Slicer created...")
    logger.info("Slicing...")
    val slicedJimple: Body = slicer.slice(sc)
    logger.info("Slicing done...")
    Some((slicer, slicedJimple))
  }

  def extractMethod(sootClass : SootClass, sootMethod : SootMethod) : Unit = {
    logger.info("Extracting graph for - class {} - method: {}{}",
      sootClass.getName(), sootMethod.getName(), "")

    assert(sootMethod.isConcrete());

    val name : String = sootClass.getName() + "_" + sootMethod.getName()
    val outputFile : File= getAcdfgOutName(options.outputDir, name)
    if (outputFile.exists()) {
      // Do not overwrite a graph
      logger.info("File {} already exists, skipping it...", outputFile)
      return;
    }

    val body: Body = sootMethod.retrieveActiveBody()

    var sc: SlicingCriterion = null
    if (null == options.sliceFilter)
      sc = MethodPackageSeed.createAndroidSeed()
    else
      sc = new MethodPackageSeed(options.sliceFilter)

    logger.info("Jimple slicing...")
    val jimpleSlicer = new JimpleSlicer(body, sc)
    val isEmpty = jimpleSlicer.sliceJimple()
    logger.info("Jimple slicing end...")

    if (isEmpty) {
      logger.warn("(Jimple) Empty slice for - class {} - method: {}\nFilter: {}\n\n",
        sootClass.getName(), sootMethod.getName(), sc.getCriterionDescription())
    }

    logger.info("Creating the enhanced unit graph...")
    val jimpleUnitGraph: EnhancedUnitGraph = new EnhancedUnitGraph(body)
    logger.info("Enhanched unit graph created... (size = " + jimpleUnitGraph.size() + ")" );

    val sliceResult : Option[(APISlicer, Body)] = sliceBody(sc, jimpleUnitGraph, body)

    val bodyToUse =
      sliceResult match {
        case None => body
        case Some((slicer, slicedJimple)) => slicedJimple
      }

    if (null == bodyToUse) {
      logger.warn("Empty slice for - class {} - method: {}\nFilter: {}\n\n",
        sootClass.getName(), sootMethod.getName(), sc.getCriterionDescription())
    } else {
      logger.debug("CDFG construction...")
      val simp : BodySimplifier =
        if (null != options.sliceFilter) {
          new BodySimplifier(new ExceptionalUnitGraph(bodyToUse), options.sliceFilter)
        } else {
          new BodySimplifier(new ExceptionalUnitGraph(bodyToUse), List("android."))
        }
      val cdfg: UnitCdfgGraph = new UnitCdfgGraph(simp.getSimplifiedBody())
      logger.debug("CDFG built...")

      val sourceInfo : SourceInfo = SourceInfo(sootClass.getPackageName(),
        sootClass.getName(),
        sootMethod.getName(),
        SootHelper.getLineNumber(sootClass),
        SootHelper.getLineNumber(sootMethod),
        SootHelper.getFileName(sootClass),
        SootHelper.getAbsFileName(sootClass)
      )
      val gitHubRecord : GitHubRecord = GitHubRecord(options.userName,
        options.repoName, options.url, options.commitHash)

      logger.debug("ACDFG construction...")
      val acdfg : Acdfg =
        if (options.provenanceDir != null) {
          val filePrefix : String = name
          val provFileName : String = filePrefix + ".html"
          new Acdfg(cdfg, gitHubRecord, sourceInfo, provFileName)
        }
        else {
          new Acdfg(cdfg, gitHubRecord, sourceInfo, "")
        }
      logger.debug("ACDFG built...")

      if (options.storeAcdfg) acdfgListBuffer += acdfg;

      if (null != options.outputDir) {
        logger.info("Writing data for - class {} - method: {}{}",
          sootClass.getName(), sootMethod.getName(), "")

        sliceResult match {
          case None => {
            writeData(name, acdfg, cdfg, body, None, None);
            logger.info("Created graph for - class {} - method: {}{}",
              sootClass.getName(), sootMethod.getName(), "")
          }
          case Some((slicer, slicedJimple)) => {
            writeData(name, acdfg, cdfg, body, Some(slicedJimple),
              Some(slicer.getCfg()));
            logger.info("Created graph for - class {} - method: {}{}",
              sootClass.getName(), sootMethod.getName(), "")
          }
        }
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

  private def getAcdfgOutName(outputDir : String, outFileNamePrefix : String) : File = {
    val acdfgFileName : String = Paths.get(outputDir,
      outFileNamePrefix + ".acdfg.bin").toString();
    val outputFile : File = new File(acdfgFileName)
    return outputFile
  }

  /**
    * Write the data to the output folder
    */
  private def writeData(outFileNamePrefix : String,
    acdfg : Acdfg,
    cdfg : UnitCdfgGraph,
    body : Body,
    slicedBodyOption : Option[Body],
    slicedCfgOption : Option[UnitGraph]) : Unit = {
    val currentDir = System.getProperty("user.dir")
    val outputDir = if (null == options.outputDir) currentDir else options.outputDir

    logger.debug("Writing ACDFG data to: {}", outputDir)

    // Write the acdfg
    val outputFile : File= getAcdfgOutName(outputDir, outFileNamePrefix)
    val outputDirPath = new File(outputDir)
    try {
      if (! outputDirPath.exists()) {
        val created = outputDirPath.mkdirs()
        if (! created) {
          throw new Exception("Error creating " + outputDir)
        }
      }
    }
    catch {
      case ex: Exception =>
        logger.error("Unable to create required new output directory")
        throw ex
    }
    val output : FileOutputStream = new FileOutputStream(outputFile)
    val protobuf = acdfg.toProtobuf
    protobuf.writeTo(output)

    output.close()

    // Write the povenance information
    if (options.provenanceDir != null) {
      logger.debug("Writing provenance data to: {}", options.provenanceDir)
      val filePrefix : String = Paths.get(options.provenanceDir, outFileNamePrefix).toString();

      try {
        val provFile : File = new File(filePrefix)
        if (! provFile.getParentFile.exists) {
          provFile.getParentFile.mkdir
        }
      }
      catch {
        case ex: Exception =>
          logger.error("Unable to create required new provenance directory")
          throw ex
      }

      // ACDFG DOT
      val acdfg_dot : String = filePrefix + ".acdfg.dot";
      val dotGraph : AcdfgToDotGraph = new AcdfgToDotGraph(acdfg)
      dotGraph.draw().plot(acdfg_dot)

      // CFG
      slicedCfgOption match {
        case None => ()
        case Some(cfg) => SootHelper.dumpToDot(cfg, cfg.getBody(),
          filePrefix + ".cfg.dot")
      }

      // CDFG
      SootHelper.dumpToDot(cdfg, cdfg.getBody(), filePrefix + ".cdfg.dot")

      // JIMPLE
      val jimpleFileName: String = filePrefix + ".jimple";
      writeJimple(body, jimpleFileName)

      // SLICED JIMPLE
      slicedBodyOption match {
        case None => ()
        case Some(slicedBody) => {
          val slicedJimpleName : String = filePrefix + ".sliced.jimple";
          writeJimple(slicedBody, slicedJimpleName)
        }
      }

      val provenance : Provenance = new Provenance(null, body,
        slicedBodyOption, outFileNamePrefix, slicedCfgOption, cdfg, acdfg)

      try {
        val provFileName : String = filePrefix + ".html"
        val provFile : File = new File(provFileName)
        val writer: Writer = new BufferedWriter(
          new OutputStreamWriter(new FileOutputStream(provFile),"utf-8")
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
          logger.error("StackOverflowError processing class {}, method {}{}",
            sootClass.getName(), sootMethod.getName(), "")
          logger.error("Exception {}:", e)
        }
        case e : Exception => {
          logger.error("Exception thrown while processing class {}, method {}{}",
            sootClass.getName(), sootMethod.getName(), "")
          logger.error("Exception {}:", e)
        }
      }

      return;
    }
  }
}
