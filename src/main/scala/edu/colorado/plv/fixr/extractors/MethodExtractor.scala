package edu.colorado.plv.fixr.extractors

import soot.Scene
import soot.SootClass
import soot.SootMethod
import soot.PhaseOptions
import soot.PackManager
import soot.options.Options
import soot.Transform
import edu.colorado.plv.fixr.SootHelper

/**
  * Extract a single method
  *
  * @author Sergio Mover
  */
class MethodExtractor(options : ExtractorOptions) extends Extractor(options) {
  val transformer : MethodsTransformer = new MethodsTransformer(options)

  def extract() : Unit = {
    assert(null != options.className)
    assert(null != options.methodName)

    // Inject the graph extractor into Soot
    PackManager.v().getPack("jtp").add(new Transform("jtp.graphExtractor",
      transformer))

    PhaseOptions.v().setPhaseOption("jtp", "on");
    Options.v().set_main_class(options.className)
    SootHelper.run(Array(options.className))
  }

  def getTransformer : MethodsTransformer = transformer
}
