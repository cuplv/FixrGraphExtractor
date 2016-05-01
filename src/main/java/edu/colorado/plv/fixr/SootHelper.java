package edu.colorado.plv.fixr;

import edu.colorado.plv.fixr.graphs.CDFGToDotGraph;
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph;
import soot.Body;
import soot.G;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JimpleBody;
import soot.options.Options;

import soot.toolkits.graph.DirectedGraph;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

public class SootHelper {
	
	public static void reset() {
		G.reset();
	}
	
	public static void configure(String classpath, boolean readFromJimple) {
		Options.v().set_verbose(false);
		Options.v().set_keep_line_number(true);
		Options.v().set_src_prec(Options.src_prec_class);
		//Options.v().set_soot_classpath("/usr/lib/jvm/jdk1.7.0/jre/lib/rt.jar:/home/sergio/works/projects/muse/repos/FixrGraphExtractor/src/test/resources");
		Options.v().set_soot_classpath(classpath);		
		Options.v().set_prepend_classpath(true);	
		
		PhaseOptions.v().setPhaseOption("jb", "enabled:false");
		/* We want to parse the code from source
		 * Phase
     * jj Creates a JimpleBody for each method directly from source
     *
     * Subphases
     * jj.ls        Local splitter: one local per DU-UD web 
     * jj.a        Aggregator: removes some unnecessary copies 
     * jj.ule      Unused local eliminator 
     * jj.tr       Assigns types to locals 
     * jj.ulp      Local packer: minimizes number of locals 
     * jj.lns      Local name standardizer 
     * jj.cp       Copy propagator 
     * jj.dae      Dead assignment eliminator
     * jj.cp-ule   Post-copy propagation unused local eliminator 
     * jj.lp       Local packer: minimizes number of locals 
     * jj.ne       Nop eliminator 
     * jj.uce      Unreachable code eliminator  
     */		
		PhaseOptions.v().setPhaseOption("jj", "use-original-names:true");
		//PhaseOptions.v().setPhaseOption("jj", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.ls", "enabled:false");
		PhaseOptions.v().setPhaseOption("jj.a", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.ule", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.tr", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.ulp", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.lns", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.cp", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.dae", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.cp-ule", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.lp", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.ne", "enabled:true");
		PhaseOptions.v().setPhaseOption("jj.uce", "enabled:true");
		
		PhaseOptions.v().setPhaseOption("cg", "enabled:false");
		PhaseOptions.v().setPhaseOption("wjtp", "enabled:false");
		
		PhaseOptions.v().setPhaseOption("wjop", "enabled:false");
		PhaseOptions.v().setPhaseOption("wjap", "enabled:false");
		
		PhaseOptions.v().setPhaseOption("jtp", "enabled:true");
		PhaseOptions.v().setPhaseOption("jop", "enabled:false");
		PhaseOptions.v().setPhaseOption("jap", "enabled:false");
		
		PhaseOptions.v().setPhaseOption("bb", "enabled:false");
		if (readFromJimple) {
			PhaseOptions.v().setPhaseOption("wjpp", "enabled:true");
		}
		PhaseOptions.v().setPhaseOption("wspp", "enabled:false");
		PhaseOptions.v().setPhaseOption("wstp", "enabled:false");
		PhaseOptions.v().setPhaseOption("wsop", "enabled:false");
		
		
		PhaseOptions.v().setPhaseOption("shimple", "enabled:false");
		PhaseOptions.v().setPhaseOption("stp", "enabled:false");
		PhaseOptions.v().setPhaseOption("sop", "enabled:false");		
		
		
		PhaseOptions.v().setPhaseOption("gb", "enabled:false");
		PhaseOptions.v().setPhaseOption("gop", "enabled:false");
		PhaseOptions.v().setPhaseOption("bb", "enabled:false");
		PhaseOptions.v().setPhaseOption("bop", "enabled:false");
		PhaseOptions.v().setPhaseOption("tag", "enabled:false");
		PhaseOptions.v().setPhaseOption("db", "enabled:false");
		
		Options.v().set_whole_program(true);
	}			
	
	public static Body getMethodBody(String className, String methodName) {		
		SootClass c = Scene.v().loadClassAndSupport(className);
  	c.setApplicationClass();
  	SootMethod m = c.getMethodByName(methodName);
  	Body b = m.retrieveActiveBody();  	
  	return b;
	}

	/**
	 * Compares two bodies.
	 * 
	 * NOTE: this is a shallow implementation now, it just perform a
	 * case insensitive comparison of the string representation of the bodies
	 * 
	 * @param b1
	 * @param b2
	 * @return true if the string representation of b1 and b2 are equal
	 */
	public static boolean compareBodies(Body b1, Body b2) {
		String reprB1 = b1.toString();
		String reprB2 = b2.toString();		
		
		return 0 == reprB1.compareToIgnoreCase(reprB2);
	}
	
	
	public static void dumpToDot(DirectedGraph<?> g, Body b, String fileName) {
		
		if (g instanceof UnitCdfgGraph) {
			CDFGToDotGraph toDot = new CDFGToDotGraph(); 
			DotGraph viewgraph = toDot.drawCFG(g, b); 	     	
			viewgraph.plot(fileName);			
			System.out.println("ciao");
		}
		else {		
			CFGToDotGraph gr = new CFGToDotGraph();
			DotGraph viewgraph = gr.drawCFG(g,b); 	   
  	
			viewgraph.plot(fileName);
		}
	}	
}
