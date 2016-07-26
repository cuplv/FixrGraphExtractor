package edu.colorado.plv.fixr.extractors

/**
 * Options used in the ACDFG extraction
 * 
 * @author Sergio Mover
 */
class ExtractorOptions {
  // Soot options
  var sootClassPath : String = null;
  var readFromSources : Boolean = true;
  
  // JPhantom options
  var useJPhantom : Boolean = false;
  var outPhantomJar : String = null;
  
  // slicing options
  var sliceFilter : String = null;
  
  // Input options  
  var processDir : List[String] = null;
  var className : String = null;
  var methodName : String = null;
  var userName : String = null;
  var repoName : String = null;
  var url : String = null;
  var commitHash : String = null;
   
  // Output options
  var outputDir : String = null;
  var provenanceDir : String = null;
  
  var to : Long = 0;
}