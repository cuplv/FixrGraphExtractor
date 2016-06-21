package edu.colorado.plv.fixr.extractors

import soot.Scene
import soot.SootClass
import soot.SootMethod
import soot.SourceLocator
import scala.collection.JavaConversions._
import edu.colorado.plv.fixr.SootHelper
import soot.PackManager
import soot.PhaseOptions
import soot.Transform
import soot.options.Options

/**
  * Extract all the methods of all the classes found in options.processDir
  *
  * @author Sergio Mover
  */
class MultipleExtractor(options : ExtractorOptions) extends Extractor(options) {

  def extract() : Unit = {
    assert(null != options.processDir || null != options.className)
    assert(null == options.processDir || null == options.className)
    assert(null == options.methodName)

    // Inject the analysis tagger into Soot
    PackManager.v().getPack("jtp").add(new Transform("jtp.myTransforma",
      new MethodsTransformer(options)));

    PhaseOptions.v().setPhaseOption("jtp", "on");

    // Invoke soot.Main with arguments given
    Options.v().set_main_class(options.className)

    val args : Array[String] =
      if (options.className != null) Array(options.className) else Array[String]();
    SootHelper.run(args)

    return
  }
}
