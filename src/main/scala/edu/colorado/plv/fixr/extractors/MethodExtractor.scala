package edu.colorado.plv.fixr.extractors

import soot.Scene
import soot.SootClass
import soot.SootMethod

/**
 * Extract a single method
 * 
 * @author Sergio Mover
 */
class MethodExtractor(options : ExtractorOptions) extends Extractor(options) {
    def extract() : Unit = {
    assert(null != options.className)
    assert(null != options.methodName)
    
    Scene.v().addBasicClass(options.className, SootClass.HIERARCHY)    
    val sootClass : SootClass = Scene.v().loadClassAndSupport(options.className)
    val sootMethod : SootMethod = sootClass.getMethodByName(options.methodName)
    extractMethod(sootClass, sootMethod)
  }
}