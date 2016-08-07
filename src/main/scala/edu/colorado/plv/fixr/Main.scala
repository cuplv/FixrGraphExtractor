package edu.colorado.plv.fixr

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.{FileWriter, File, FileInputStream, FileOutputStream}

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import edu.colorado.plv.fixr.abstraction.Acdfg
import edu.colorado.plv.fixr.abstraction.AcdfgToDotGraph
import edu.colorado.plv.fixr.extractors.Extractor
import edu.colorado.plv.fixr.extractors.ExtractorOptions
import edu.colorado.plv.fixr.extractors.MultipleExtractor
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
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
    readFromSources : Boolean = true,
    useJPhantom : Boolean = false,
    outPhantomJar : String = null,    
    sliceFilter : String = null,
    processDir : String = null,
    className : String = null,
    methodName : String = null,
    outputDir : String = null,
    provenanceDir : String = null,
    var userName : String = null,
    var repoName : String = null,
    var url : String = null,
    var commitHash : String = null,
    to : Long = 0)

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
      opt[Boolean]('s', "read-from-sources") action { (x, c) =>
        c.copy(readFromSources = x) } text("Set to true to use Jimple as input")
      //
      opt[Boolean]('j', "jphanthom") action { (x, c) =>
        c.copy(useJPhantom = x) } text("Set to true to use JPhantom")
      opt[String]('z', "jphantom-folder") action { (x, c) =>
        c.copy(outPhantomJar = x) } text("Path to the generated JPhantom classes")
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
      c.copy(outputDir = x) } text("Path of the output directory for the ACDFG.")
      //
      opt[String]('d', "provenance-dir") action { (x, c) =>
      c.copy(provenanceDir = x) } text("Path of the directory used to store the provenance information.")
      //
      opt[Long]('t', "time-out") action { (x, c) =>
        c.copy(to= x) } text("Set the time out (0 for no time out)")
      // Manually added for GitHub provenance --Rhys
      opt[String]('n', "user-name") action { (x, c) =>
        c.copy(userName = x) } text("GitHub username of the user who owns the repository being ingested (case-sensitive).")
      opt[String]('r', "repo-name") action { (x, c) =>
        c.copy(repoName = x) } text("Name of the GitHub repository being ingested (case-sensitive).")
      opt[String]('u', "url") action { (x, c) =>
        c.copy(url = x) } text("URL of the git repo; should be of the form `https://github.com/user_name/repo_name`.")
      opt[String]('h', "commit-hash") action { (x, c) =>
        c.copy(commitHash = x) } text("SHA-1 hash of the commit being ingested.")
    }
    parser.parse(args, MainOptions()) match {
      case Some(mainopt) => {
        logger.debug("cp: {}", mainopt.sootClassPath)
        logger.debug("read-from-sources: {}", mainopt.readFromSources)
        logger.debug("jphantom: {}\n", mainopt.useJPhantom)
        logger.debug("jphantom-folder: {}\n", mainopt.outPhantomJar)        
        logger.debug("slice-filter: {}", mainopt.sliceFilter)
        logger.debug("process-dir: {}", mainopt.processDir)
        logger.debug("class-name: {}", mainopt.className)
        logger.debug("method-name: {}", mainopt.methodName)
        logger.debug("output-dir: {}", mainopt.outputDir)
        logger.debug("provenance-dir: {}\n", mainopt.provenanceDir)
        logger.debug("time-out: {}\n", mainopt.to)        

        if ( null != mainopt.url &&
          null != mainopt.userName &&
          null != mainopt.repoName &&
          mainopt.url != "https://github.com/" +
            mainopt.userName +
            "/" +
            mainopt.repoName
        ) {
          logger.warn("URL " + mainopt.url + "is not of the expected form " +
            "https://github.com/" +
            mainopt.userName +
            "/" +
            mainopt.repoName +
            ". Proceeding anyways..."
          )
        }
        if (null == mainopt.url &&
          null != mainopt.userName &&
          null != mainopt.repoName) {
          logger.info("No GitHub repo URL supplied. " +
            "Generating from username and repo name and proceeding..."
          )
          mainopt.url = "https://github.com/" + mainopt.userName + "/" + mainopt.repoName
        }
        if (null == mainopt.userName) {
          logger.info("GitHub user name not supplied. Proceeding with empty string...")
          mainopt.userName = ""
        }
        if (null == mainopt.repoName) {
          logger.info("GitHub repository name not supplied. Proceeding with empty string...")
          mainopt.repoName = ""
        }
        if (null == mainopt.url) {
          logger.info("GitHub repo URL not supplied. Proceeding with empty string...")
          mainopt.url = ""
        }
        if (null == mainopt.commitHash) {
          logger.info("Commit hash not supplied. Proceeding with empty string...")
          mainopt.commitHash = ""
        }

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
        options.className = mainopt.className
        options.methodName = mainopt.methodName
        options.useJPhantom = mainopt.useJPhantom
        options.outPhantomJar = mainopt.outPhantomJar
        options.readFromSources = mainopt.readFromSources
        options.sootClassPath = mainopt.sootClassPath
        options.outputDir = mainopt.outputDir
        options.provenanceDir = mainopt.provenanceDir
        options.to = mainopt.to
        options.repoName = mainopt.repoName
        options.userName = mainopt.userName
        options.url = mainopt.url
        options.commitHash = mainopt.commitHash
        
        if (null != mainopt.sliceFilter) {
          options.sliceFilter = mainopt.sliceFilter.split(":").toList
        }

        if (null != mainopt.processDir) {
          //List[String]("/home/sergio/works/projects/muse/repos/FixrGraphExtractor/src/test/resources/javasources")/
          val myArray : Array[String] = mainopt.processDir.split(":")
          options.processDir = myArray.toList
        }

        val extractor : Extractor =
          if (options.processDir == null) new MethodExtractor(options)
          else new MultipleExtractor(options)
        extractor.extract()
      }
      case None => System.exit(1)
    }

    logger.info("Terminated extraction...")
    System.exit(0)
  }
}
