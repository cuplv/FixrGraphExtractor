package edu.colorado.plv.fixr

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.{FileWriter, File, FileInputStream, FileOutputStream}

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import edu.colorado.plv.fixr.abstraction.Acdfg
import edu.colorado.plv.fixr.abstraction.AcdfgToDotGraph
import edu.colorado.plv.fixr.abstraction.AcdfgToProtobuf
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions
import edu.colorado.plv.fixr.extractors.MultipleExtractor
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.provenance.Provenance
import edu.colorado.plv.fixr.slicing.APISlicer
import edu.colorado.plv.fixr.slicing.MethodPackageSeed
import edu.colorado.plv.fixr.slicing.SlicingCriterion
import soot.Body
import soot.Scene
import soot.SootClass
import soot.toolkits.graph.pdg.EnhancedUnitGraph
import edu.colorado.plv.fixr.extractors.MethodExtractor




object Main {
  val logger : Logger = LoggerFactory.getLogger(this.getClass)
  
  case class MainOptions(
    sootClassPath : String = null,
    readFromJimple : Boolean = false,
    sliceFilter : String = null,
    processDir : String = null,
    className : String = null,
    methodName : String = null,
    outputDir : String = null,
    provenanceDir : String = null)
    
  /**
    * Now the program takes as input the classpath, the class name and the method name for which we have to build the graph
    *
    * @param args classpath, class name (e.g. package.ClassName), method name
    */
  def main(args: Array[String]) {

    val parser = new scopt.OptionParser[MainOptions]("scopt") {
      head("GraphExtractor", "0.1")
      //
      opt[String]('l', "cp") action { (x, c) =>
      c.copy(sootClassPath = x) } text("cp is the soot classpath")
      //
      opt[Boolean]('j', "read-from-jimple") action { (x, c) =>
      c.copy(readFromJimple = x) } text("Set to true to use Jimple as input")
      //
      opt[String]('f', "slice-filter") action { (x, c) =>
      c.copy(sliceFilter = x) } text("Package prefix to use as seed for slicing")
      //
      opt[String]('p', "process-dir") action { (x, c) =>
      c.copy(processDir = x) } text("Comma (:) separated list of input directories to process")
      //
      opt[String]('c', "class-name") action { (x, c) =>
      c.copy(className = x) } text("Name of the class to be processed.")
      //
      opt[String]('m', "method-name") action { (x, c) =>
      c.copy(methodName = x) } text("Name of the method to be processed.")
      //
      opt[String]('o', "output-dir") action { (x, c) =>
      c.copy(outputDir= x) } text("Path of the output directory for the ACDFG.")
      //
      opt[String]('d', "provenance-dir") action { (x, c) =>
      c.copy(outputDir= x) } text("Path of the directory used to store the provenance information.")      
    }   
    parser.parse(args, MainOptions()) match {
      case Some(mainopt) => {               
        logger.debug("cp: {}", mainopt.sootClassPath)
        logger.debug("read-from-jimpl: {}", mainopt.readFromJimple)
        logger.debug("slice-filter: {}", mainopt.sliceFilter)
        logger.debug("process-dir: {}", mainopt.processDir)
        logger.debug("class-name: {}", mainopt.className)
        logger.debug("method-name: {}", mainopt.methodName)                
        logger.debug("output-dir: {}", mainopt.outputDir)
        logger.debug("provenance-dir: {}\n", mainopt.provenanceDir)        
        
        if (null != mainopt.processDir &&
            (null != mainopt.className || null != mainopt.methodName)) {
           logger.error("The process-dir option is mutually exclusive " +
               "with the class-name and method-name options");
           System.exit(1)
        }
        
        if ( (null != mainopt.className && null == mainopt.methodName) ||
          (null == mainopt.className && null != mainopt.methodName)) {
          logger.error("The options class-name and method-name " +
              "must be specified together")
           System.exit(1)              
        }
        
        val options : ExtractorOptions = new ExtractorOptions() 
        options.className = mainopt.className;
        options.methodName = mainopt.methodName;
        options.readFromJimple = mainopt.readFromJimple;
        options.sliceFilter = mainopt.sliceFilter;
        options.sootClassPath = mainopt.sootClassPath;
        options.outputDir = mainopt.outputDir;
        options.provenanceDir = mainopt.provenanceDir;               
                
        if (null != mainopt.processDir) {
          //List[String]("/home/sergio/works/projects/muse/repos/FixrGraphExtractor/src/test/resources/javasources")/
          val myArray : Array[String] = mainopt.processDir.split(":");           
          options.processDir = myArray.toList
        }
        
        val extractor : Extractor = 
          if (options.processDir == null) new MethodExtractor(options)
          else new MultipleExtractor(options)     
        extractor.extract()

        System.exit(0)
      }
      case None => {System.exit(1)}
    }
    
    logger.info("Terminated extraction...")
  }
}
