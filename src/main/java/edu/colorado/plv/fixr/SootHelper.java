package edu.colorado.plv.fixr;

import soot.Body;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

import soot.toolkits.graph.DirectedGraph;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

public class SootHelper {
	public static void configure(String classpath) {
		Options.v().set_verbose(false);
		Options.v().set_keep_line_number(true);
		Options.v().set_src_prec(Options.src_prec_class);
		Options.v().set_soot_classpath(classpath);
		Options.v().set_prepend_classpath(true);

		//jj.uce
		
		PhaseOptions.v().setPhaseOption("jb", "off");		
		PhaseOptions.v().setPhaseOption("bb", "off");
		PhaseOptions.v().setPhaseOption("wjpp", "off");
		PhaseOptions.v().setPhaseOption("wspp", "off");
		PhaseOptions.v().setPhaseOption("cg", "off");
		PhaseOptions.v().setPhaseOption("wstp", "off");
		PhaseOptions.v().setPhaseOption("wsop", "off");
		PhaseOptions.v().setPhaseOption("wjtp", "off");
		PhaseOptions.v().setPhaseOption("wjop", "off");
		PhaseOptions.v().setPhaseOption("wjap", "off");
		PhaseOptions.v().setPhaseOption("shimple", "off");
		PhaseOptions.v().setPhaseOption("stp", "off");
		PhaseOptions.v().setPhaseOption("sop", "off");
		PhaseOptions.v().setPhaseOption("jtp", "off");
		PhaseOptions.v().setPhaseOption("jop", "off");
		PhaseOptions.v().setPhaseOption("jap", "off");
		PhaseOptions.v().setPhaseOption("gb", "off");
		PhaseOptions.v().setPhaseOption("gop", "off");
		PhaseOptions.v().setPhaseOption("bb", "off");
		PhaseOptions.v().setPhaseOption("bop", "off");
		PhaseOptions.v().setPhaseOption("tag", "off");
		PhaseOptions.v().setPhaseOption("db", "off");
		
		PhaseOptions.v().setPhaseOption("tag.ln", "on");
		PhaseOptions.v().setPhaseOption("jj.uce", "on");

		Options.v().set_whole_program(true);
	}	
	
	public static Body getMethodBody(String className, String methodName) {
		SootClass c = Scene.v().loadClassAndSupport(className);
  	c.setApplicationClass();
  	SootMethod m = c.getMethodByName(methodName);
  	Body b = m.retrieveActiveBody();
  	return b;
	}
	
	public static void dumpToDot(DirectedGraph g, Body b, String fileName) {
		  	CFGToDotGraph gr = new CFGToDotGraph();
  	DotGraph viewgraph = gr.drawCFG(g,b);
  	viewgraph.plot(fileName);
	}
	
}
