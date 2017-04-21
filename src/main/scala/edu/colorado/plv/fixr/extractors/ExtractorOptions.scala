package edu.colorado.plv.fixr.extractors

import edu.colorado.plv.fixr.SootHelper;

/**
 * Options used in the ACDFG extraction
 *
 * @author Sergio Mover
 */
class ExtractorOptions {
  // Soot options
  var sootClassPath : String = null;
  var configCode : Int = SootHelper.READ_FROM_BYTECODE;

  // JPhantom options
  var useJPhantom : Boolean = false;
  var outPhantomJar : String = null;

  // slicing options
  var sliceFilter : List[String] = null;

  // Input options
  var processDir : List[String] = null;
  var className : String = null;
  var methodName : String = null;
  var userName : String = null;
  var repoName : String = null;
  var androidJars : String = null;
  var url : String = null;
  var commitHash : String = null;

  // Output options
  var outputDir : String = null;
  var provenanceDir : String = null;

  var storeAcdfg : Boolean = false;
  var to : Long = 0;
}
