package edu.colorado.plv.fixr;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import edu.colorado.plv.fixr.graphs.CDFGToDotGraph;
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph;
import soot.Body;
import soot.CompilationDeathException;
import soot.G;
import soot.PackManager;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Timers;
import soot.options.Options;
import soot.toolkits.graph.DirectedGraph;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

import soot.tagkit.AbstractHost;
import soot.tagkit.Tag;
import soot.tagkit.SourceLnPosTag;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;
import soot.tagkit.SourceLnNamePosTag;

public class SootHelper {
  public static void reset() {
    G.reset();
  }

  public static void configure(String classpath, boolean readFromSources) {
    configure(classpath, readFromSources, null);
  }

  public static void configure(String classpath, boolean readFromSources, java.util.List<String> processDir) {
    Options.v().set_verbose(false);
    Options.v().set_keep_line_number(true);
    Options.v().set_keep_offset(true);
    Options.v().set_src_prec(Options.src_prec_class);
    Options.v().set_soot_classpath(classpath);

    Options.v().set_prepend_classpath(true);
    //Options.v().set_allow_phantom_refs(true);

    if (null != processDir) {
      Options.v().set_process_dir(processDir);
    }

    if (readFromSources) {
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
      PhaseOptions.v().setPhaseOption("jb", "enabled:false");
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
    }
    else {
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

      /* Jimple body creation - neeed when processing classes */
      PhaseOptions.v().setPhaseOption("jb", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb", "use-original-names:true");
      // jb.ls is enabled, otherwise soot raises
      // the exception java.lang.Exception: null typing passed to useChecker
      PhaseOptions.v().setPhaseOption("jb.ls", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.a", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.ule", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.tr", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.ulp", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.lns", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.cp", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.dae", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.cp-ule", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.lp", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.ne", "enabled:true");
      PhaseOptions.v().setPhaseOption("jb.uce", "enabled:true");
    }

    PhaseOptions.v().setPhaseOption("cg", "enabled:false");
    PhaseOptions.v().setPhaseOption("wjtp", "enabled:false");

    PhaseOptions.v().setPhaseOption("wjop", "enabled:false");
    PhaseOptions.v().setPhaseOption("wjap", "enabled:false");

    PhaseOptions.v().setPhaseOption("jtp", "enabled:true");
    PhaseOptions.v().setPhaseOption("jop", "enabled:false");
    PhaseOptions.v().setPhaseOption("jap", "enabled:false");

    PhaseOptions.v().setPhaseOption("bb", "enabled:false");
    /* This pack is empty in an unmodified copy of soot.
     * It can be used to add our own transformations (we don't use it
     * though).
     * */
    PhaseOptions.v().setPhaseOption("wjpp", "enabled:true");
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

    Options.v().set_whole_program(false);
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
    if (b1.getLocalCount() != b2.getLocalCount()) return false;

    for (soot.Local l1 : b1.getLocals()) {
      boolean found = false;
      for (soot.Local l2 : b1.getLocals()) {
        if (l1.getName() == l2.getName() &&
            l1.getType() == l2.getType() &&
            l1.getNumber() == l2.getNumber()) {
          found = true;
        }
      }
      if (! found) return false;
    }

    String reprB1 = b1.getUnits().toString();
    String reprB2 = b2.getUnits().toString();

    return 0 == reprB1.compareToIgnoreCase(reprB2);
  }


  public static void dumpToDot(DirectedGraph<?> g, Body b, String fileName) {

    if (g instanceof UnitCdfgGraph) {
      CDFGToDotGraph toDot = new CDFGToDotGraph();
      DotGraph viewgraph = toDot.drawCFG(((UnitCdfgGraph) g), b);
      viewgraph.plot(fileName);
    }
    else {
      CFGToDotGraph gr = new CFGToDotGraph();
      DotGraph viewgraph = gr.drawCFG(g, b);
      viewgraph.plot(fileName);
    }
  }

  public static void run(String[] args) {
    // Replicates the soot.Main.run() method skipping the output
    Date start = new Date();

    try {
      Timers.v().totalTimer.start();

      if (!Options.v().parse(args))
        throw new CompilationDeathException(CompilationDeathException.COMPILATION_ABORTED,
                                            "Option parse error");

      G.v().out.println("Soot started on " + start);

      Scene.v().loadNecessaryClasses();
      PackManager.v().runPacks();

      Timers.v().totalTimer.end();

      // Print out time stats.
      if (Options.v().time())
        Timers.v().printProfilingInformation();

    } catch (CompilationDeathException e) {
      Timers.v().totalTimer.end();
      if(e.getStatus()!=CompilationDeathException.COMPILATION_SUCCEEDED)
        throw e;
      else
        return;
    }

    Date finish = new Date();

    G.v().out.println("Soot finished on " + finish);
    long runtime = finish.getTime() - start.getTime();
    G.v().out.println("Soot has run for "
                      + (runtime / 60000)
                      + " min. "
                      + ((runtime % 60000) / 1000)
                      + " sec.");
  }

  /** Returns the line number of the code element host
   * or 0 if the line number tag does not exists
   *
   * @param host code element
   * @return the line number of host if it exsits, 0 otherwise
   */
  public static int getLineNumber(AbstractHost code)
  {
    int lineNumber = 0;

    /* solution that should works both on bytecode and on sources */
    Tag lineNumberTag = code.getTag("SourceLnPosTag");
    if (null != lineNumberTag && lineNumberTag instanceof SourceLnPosTag) {
      lineNumber = ((SourceLnPosTag) lineNumberTag).startLn();
    }
    else {
      lineNumberTag = code.getTag("LineNumberTag");
      if (null != lineNumberTag && lineNumberTag instanceof LineNumberTag) {
        lineNumber = ((LineNumberTag) lineNumberTag).getLineNumber();
      }
    }

    return lineNumber;
  }


  /** Returns the file name of the code element host
   * or an empty string otherwise
   *
   * @param host code element
   * @return the file name of host if it exsits, "" otherwise
   */
  public static String getFileName(AbstractHost code)
  {
    String fileName = "";
    Tag fileNameTag = code.getTag("SourceFileTag");
    if (null != fileNameTag && fileNameTag instanceof SourceFileTag) {
      fileName = ((SourceFileTag) fileNameTag).getSourceFile();
    }

    return fileName;
  }

  public static String getAbsFileName(AbstractHost code)
  {
    String fileName = "";
    Tag fileNameTag = code.getTag("SourceFileTag");
    if (null != fileNameTag && fileNameTag instanceof SourceFileTag) {
      fileName = ((SourceFileTag) fileNameTag).getAbsolutePath();
      if (null == fileName) {
        fileNameTag = code.getTag("SourceLnNamePosTag");
        if (null != fileNameTag && fileNameTag instanceof SourceLnNamePosTag) {
          fileName = ((SourceLnNamePosTag) fileNameTag).getFileName();
          if (fileName == null) fileName = "";
        }
      }
    }

    return fileName;
  }
}
