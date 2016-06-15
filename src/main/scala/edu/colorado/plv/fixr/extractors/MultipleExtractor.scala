package edu.colorado.plv.fixr.extractors

import soot.Scene
import soot.SootClass
import soot.SootMethod
import soot.SourceLocator
import scala.collection.JavaConversions._

/**
 * Extract all the methods of all the classes found in options.processDir
 * 
 * @author Sergio Mover
 */
class MultipleExtractor(options : ExtractorOptions) extends Extractor(options) {
  
  

  def extract() : Unit = {
    assert(null != options.processDir)
    assert(null == options.methodName)
    assert(null == options.className)    

    def processDirs(dirs : List[String], classList : List[SootClass]) : List[SootClass] = {
      def processClasses(classesNames : List[String], classList : List[SootClass]) : List[SootClass] = {
        classesNames match {
          case className :: xs => {
            Scene.v().addBasicClass(className, SootClass.HIERARCHY);
            val sootClass : SootClass = Scene.v().loadClassAndSupport(className);
            processClasses(xs, sootClass :: classList)            
          }
          case _ => classList
        }
      }
      
      dirs match {
        case x::xs => {
          val classStringList : Seq[String] = SourceLocator.v().getClassesUnder(x);
          val list : List[String] = List();
          val newList : List[String] = list++(classStringList)
          processDirs(xs, processClasses(newList, classList))
        }
        case _ => classList
      }
    }
    
    val sootClasses : List[SootClass] = processDirs(options.processDir, List[SootClass]())
        
    // Process each method 
    sootClasses.foreach { sootClass => {
      val methodSeq : Seq[SootMethod] = sootClass.getMethods()      
      methodSeq.foreach { sootMethod => {
      	if (sootMethod.isConcrete()) {    
      		extractMethod(sootClass, sootMethod)
      	}
      	else {
      		logger.warn("Skipped non-concrete method {} of class {}{}",
      				sootMethod.getName(), sootClass.getName(), "")
      	}
      }} // end of methodSeq.foreach      
    }}
  }   
}
  