package edu.colorado.plv.fixr.extractors

import soot.Scene
import soot.SootClass
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import edu.colorado.plv.fixr.slicing.SlicingCriterion
import edu.colorado.plv.fixr.abstraction.AcdfgToDotGraph
import edu.colorado.plv.fixr.abstraction.AcdfgToProtobuf
import edu.colorado.plv.fixr.slicing.APISlicer
import java.io.FileOutputStream
import soot.SootMethod
import edu.colorado.plv.fixr.slicing.MethodPackageSeed
import soot.toolkits.graph.pdg.EnhancedUnitGraph
import edu.colorado.plv.fixr.abstraction.Acdfg
import soot.Body
import java.io.PrintWriter
import java.io.OutputStream
import soot.Printer
import java.io.OutputStreamWriter
import java.nio.file.Paths
import soot.toolkits.graph.UnitGraph
import edu.colorado.plv.fixr.SootHelper
import scala.collection.JavaConversions.seqAsJavaList

/**
 * Extract an ACDFG from source code
 * 
 * @author Sergio Mover
 */
class Extractor(options : ExtractorOptions) {  
  /**
   * Extract the ACDFG - implement the right iteration
   */
  def extract() : Unit = {
    
  }
  
  private def initExtractor() : Unit = {
    SootHelper.configure(options.sootClassPath, options.readFromJimple, options.processDir)
        
    // Scene.v().addBasicClass(className, SootClass.HIERARCHY)
    Scene.v().loadNecessaryClasses()    
  }
  
  private def extractMethod(sootClass : SootClass, sootMethod : SootMethod) : Unit = {    
    val body: Body = sootMethod.getActiveBody()
    val jimpleUnitGraph: EnhancedUnitGraph = new EnhancedUnitGraph(body)
    val slicer: APISlicer = new APISlicer(jimpleUnitGraph, body)
    
    var sc: SlicingCriterion = null
    if (null == options.sliceFilter)
      sc = MethodPackageSeed.createAndroidSeed    
    else
      sc = new MethodPackageSeed(options.sliceFilter)
    
    val slicedJimple: Body = slicer.slice(sc)    
    val cdfg: UnitCdfgGraph = new UnitCdfgGraph(slicedJimple)    
    val acdfg : Acdfg = new Acdfg(cdfg)

    val name : String = sootClass.getName() + "_" +
      sootMethod.getName();
    writeData(name, acdfg, cdfg, body, slicedJimple, slicer.getCfg());    
  }
  
  private def writeJimple(body : Body, fileName : String) : Unit = {
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
    // Write the acdfg
    val acdfgFileName : String = Paths.get(options.outputDir,
        outFileNamePrefix + ".acdfg.bin").toString();
    val output : FileOutputStream = new FileOutputStream(acdfgFileName)
    val acdfgToProtobuf = new AcdfgToProtobuf(acdfg)    
    acdfgToProtobuf.protobuf.writeTo(output)
    output.close()    
    
    if (options.provenanceDir != null) {
      val filePrefix : String = Paths.get(options.provenanceDir, outFileNamePrefix).toString();
      // ACDFG DOT
      val acdfg_dot : String = filePrefix + ".acdfg.dot";
      val dotGraph : AcdfgToDotGraph = new AcdfgToDotGraph(acdfg)
      dotGraph.draw().plot(acdfg_dot)
      
      // CDFG
      SootHelper.dumpToDot(cdfg, cdfg.getBody(), filePrefix + ".cdfg.dot")
            
      // JIMPLE
      val jimpleFileName: String = filePrefix + ".jimple";
      writeJimple(body, jimpleFileName)
      // CFG             
      SootHelper.dumpToDot(cdfg, cfg.getBody(), filePrefix + ".cfg.dot")
      // SLICED JIMPLE
      val slicedJimpleName : String = filePrefix + ".sliced.jimple";
      writeJimple(slicedBody, slicedJimpleName)           
    }                  
  }
}