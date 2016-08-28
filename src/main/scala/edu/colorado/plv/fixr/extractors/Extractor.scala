package edu.colorado.plv.fixr.extractors

import scala.collection.JavaConversions.seqAsJavaList

import org.clyze.jphantom.Driver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import edu.colorado.plv.fixr.SootHelper

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
    var sootClassPath :String = null;

    if (options.useJPhantom) {
      logger.debug("Invoking JPhantom...")
      assert(options.outPhantomJar != null);

      var classPath : List[String] = List[String]();
      classPath = options.sootClassPath.split(":").foldLeft(classPath)(
          (classPath,y) => y::classPath);
      if (options.processDir != null) {
        classPath = options.processDir.foldLeft(classPath)(
            (classPath,y) => y::classPath);
      }

      Driver.createPhantomClassJar(classPath, options.outPhantomJar)

      sootClassPath = options.sootClassPath + ":" + options.outPhantomJar;
    }
    else {
      sootClassPath = options.sootClassPath;
    }

    logger.debug("Initializing soot...")
    if (null == options.processDir) {
      SootHelper.configure(sootClassPath, options.readFromSources)

    }
    else {
      SootHelper.configure(sootClassPath, options.readFromSources,
        options.processDir)
    }
  }

  def getTransformer : MethodsTransformer
}
