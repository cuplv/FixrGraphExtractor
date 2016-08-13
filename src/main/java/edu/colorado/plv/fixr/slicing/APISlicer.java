package edu.colorado.plv.fixr.slicing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.colorado.plv.fixr.graphs.DataDependencyGraph;
import edu.colorado.plv.fixr.SootHelper;

import soot.Body;
import soot.PatchingChain;
import soot.util.Chain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.Local;
import soot.ValueBox;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NopStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.internal.JNopStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.ConditionalPDGNode;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;
import soot.toolkits.graph.pdg.LoopedPDGNode;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.graph.pdg.PDGRegion;

/**
 * Slice a CFG of a method using as seeds the nodes of the API calls.
 *
 * @author Sergio Mover
 *
 */
public class APISlicer {
  private UnitGraph cfg;
  private PDGSlicer pdg;
  private DataDependencyGraph ddg;

  /**
   * @return the cfg
   */
  public UnitGraph getCfg() {
    return cfg;
  }

  /**
   * @return the pdg
   */
  public PDGSlicer getPdg() {
    return pdg;
  }

  /**
   * @return the ddg
   */
  public DataDependencyGraph getDdg() {
    return ddg;
  }

  public APISlicer(UnitGraph cfg, Body body) {
    super();

    if (null == cfg) {
      cfg= new EnhancedUnitGraph(body);
    }
    this.cfg = cfg;

    /* Computes the program dependency graph */
    this.pdg= new PDGSlicer((UnitGraph) this.cfg);
    this.ddg = new DataDependencyGraph(this.cfg);
  }

  public APISlicer(Body body) {
    this(null, body);
  }

  /**
   * Computes the slice of this.cfg according to the android API method calls.
   *
   * @return a slice for the sc criterion or null if there are no seeds.
   */
  public Body slice(SlicingCriterion sc) {
    /* 2.1 Find the slice units */
    Set<Unit> seeds = getSeeds(sc);

    if (seeds.size() > 0) {
      /* 2.2 Get the CFG units that are relevant for the slice */
      Set<Unit> unitsInSlice = findReachableUnits(seeds);

      /* 3. Construct the sliced body */
      SlicerGraph sg = new SlicerGraph(this.cfg, unitsInSlice);
      Body slice = sg.getSlicedBody();

      // Body slice = buildSlice(unitsInSlice);

      // DEBUG
      {
        SootHelper.dumpToDot(this.cfg, this.cfg.getBody(), "/tmp/cfg.dot");
        SootHelper.dumpToDot(ddg, this.cfg.getBody(), "/tmp/ddg.dot");
        SootHelper.dumpToDot(pdg, this.cfg.getBody(), "/tmp/pdg.dot");

        EnhancedUnitGraph slicedGraph = new EnhancedUnitGraph(slice);
        SootHelper.dumpToDot(slicedGraph, slice, "/tmp/sliced.dot");
      }

      return slice;
    }
    else {
      return null;
    }
  }

  public static String getSlicedMethodName(String methodName) {
    return methodName + "__sliced__";
  }

  /**
   * Finds all the seeds (units) in the PDG according to the slicing criterion
   *
   * The method returns a pair (PDGNode,
   *
   * @return
   */
  private Set<Unit> getSeeds(SlicingCriterion sc)
  {
    Set<Unit> seeds = new HashSet<Unit>();
    for (Object node : pdg.getNodes()) {
      if (node instanceof PDGNode) {
        Object innerNode = ((PDGNode) node).getNode();
        if (innerNode instanceof Block) {
          Block b = (Block) innerNode;
          for (Iterator<Unit> iter = b.iterator(); iter.hasNext();) {
            Unit u = iter.next();
            if (sc.is_seed(u)) seeds.add(u);
          }
        }
      }
    }

    return seeds;
  }

  /**
   * Find all the units that are <b>backward</b> reachable from the units
   * contained in seeds in the graph represented by the combination
   * of the PDG and the DDG.
   *
   * The combination of the PDG and DDG graph is trivial: the graph is formed
   * by the PDG and all the edges from unit to unit in the DDG (the PDG
   * already contains the units that may appear as a seed).
   *
   * @param seeds Set of units used as seeds
   * @return the set of backward reachable nodes in the PDG and DDG. graph
   */
  private Set<Unit> findReachableUnits(Set<Unit> seeds)
  {
    /* set of reachable units */
    Set<Unit> reachableUnits = new HashSet<Unit>();
    Set<Unit> reachableConditionals = new HashSet<Unit>();

    /* Set of visited nodes either in the PDG or in the DDG
     * The set may either contain a PDGNode or a Unit */
    Set<Object> visitedNodes = new HashSet<Object>();
    /* Stack used in the search */
    Stack<Object> toProcess = new Stack<Object>();

    /* 1. All the nodes in the seeds must be processed */
    for (Unit seed : seeds) toProcess.push(seed);

    /* 2. Reachability algorithm */
    while (! toProcess.isEmpty()) {
      Object current = toProcess.pop();
      if (visitedNodes.contains(current)) continue; /* skip visited nodes */

      if (current instanceof Unit) {
        Unit currentUnit = (Unit) current;

        if (currentUnit instanceof IfStmt) {
          reachableConditionals.add(currentUnit);
          reachableUnits.add(currentUnit);
        }
        else {
          assert (null != currentUnit);
          reachableUnits.add(currentUnit);
        }

        /* get the predecessors in the DDG */
        toProcess.addAll(this.ddg.getPredsOf(currentUnit));

        /* get the PDGNode that contains the unit node */
        PDGNode currentPdgNode = this.pdg.getPDGNodeFromUnit(currentUnit);
        assert (null != currentPdgNode);

        /* Add the "gotos" or conditions (if they exists) in the cfg block */
        {
          Unit blockLabel = getBlockGoto(currentPdgNode);
          if (null != blockLabel) {
            reachableUnits.add(blockLabel);
          }
        }

        /* Get the predecessors of the pdg node.
         * We do not add the unit's pdg node to the search, otherwise we would
         * possibly end up adding all the units in the same pdg node
         * (which can be executed in parallel if there are no data dependencies).
         *
         * Instead, we add the predecessors of the PDG node.
         * */
        List<PDGNode> pdgPredecessors = this.pdg.getPredsOf(currentPdgNode);
        toProcess.addAll(pdgPredecessors);
      }
      else if (current instanceof PDGNode) {
        /* get predecessors in the PDG */
        PDGNode currentPdgNode = (PDGNode) current;

        if (currentPdgNode.getType() == PDGNode.Type.CFGNODE) {
            /* Add *all* the unit in the CFG block in the nodes to be processed
             * A subset of teh reachable units is control dependent from
             * this PDGNode
             *  */
            Object pdgObject = currentPdgNode.getNode();
            if (pdgObject instanceof Block) {
              Block block = (Block) pdgObject;
              for (Iterator<Unit> iter = (block).iterator(); iter.hasNext();) {
                Unit unit = iter.next();
                toProcess.add(unit);
              }
            }
        } else {
          /* Region, do nothing  */
        }
        List<PDGNode> pdgPredecessors = this.pdg.getPredsOf(currentPdgNode);
        toProcess.addAll(pdgPredecessors);
      }
      else {
        throw new RuntimeException("APISlicer: Unknown type of node!");
      }

      visitedNodes.add(current);
    }
    return reachableUnits;
  }

  private Unit getBlockGoto(PDGNode pdgNode)
  {
    Unit blockLabel = null;
    assert(pdgNode.getType() == PDGNode.Type.CFGNODE);
    Object pdgObject = pdgNode.getNode();

    if (pdgObject instanceof Block) {
      Block block = (Block) pdgObject;
      Unit appBlock = null;
      for (Iterator<Unit> iter = (block).iterator(); iter.hasNext();) {
        appBlock = iter.next();

        if (appBlock instanceof GotoStmt || appBlock instanceof IfStmt) {
          /* Assume at most one goto statement in the block */
          assert (appBlock == null);
          blockLabel = appBlock;
        }
      }
    }

    return blockLabel;
  }

  /**
   * Return a slice (as a block graph) of the cfg nodes in unitsInSlice
   *
   * Builds a new body (dstBody) that only contains the unti found in
   * unitsInSlice.
   * dstBody is the sliced body.
   *
   * @return a sliced block graph
   */
  private Body buildSlice(Set<Unit> unitsInSlice) {
    Body srcBody = this.cfg.getBody();
    SootMethod srcMethod = srcBody.getMethod();
    SootClass srcClass = srcMethod.getDeclaringClass();
    assert (null != srcMethod);
    String methodName = srcMethod.getName();
    methodName = getSlicedMethodName(methodName);

    SootMethod dstMethod = new SootMethod(methodName,
                                          srcMethod.getParameterTypes(), srcMethod.getReturnType(),
                                          srcMethod.getModifiers());
    JimpleBody dstBody = Jimple.v().newBody(dstMethod);
    dstMethod.setActiveBody(dstBody);
    srcClass.addMethod(dstMethod);

    Map<Object,Object> src2dst = dstBody.importBodyContentsFrom(srcBody);

    PatchingChain<Unit> dstPc = dstBody.getUnits();

    unitsInSlice.add(dstBody.getUnits().getFirst());
    unitsInSlice.add(dstBody.getUnits().getLast());

    /* remove all the units that are not in the relevant nodes */
    dstPc.addLast(new JNopStmt());
    for (Unit srcUnit : srcBody.getUnits()) {
      if (! unitsInSlice.contains(srcUnit)) {
        Unit dstUnit = (Unit) src2dst.get(srcUnit);
        assert null != dstUnit;
        dstPc.remove(dstUnit);
      }
    }

    /* add the gotos informations */
    /* fixFlow(dstPc, src2dst, unitsInSlice); */

    return dstBody;
  }

  private class SlicerGraph {

    Body srcBody;
    boolean[][] edges;
    /* keep the label on the edge - to be merged with edges */
    List<Object>[][] edgeLabels;
    boolean[]  unitsInSlice;
    Unit[] idToUnit;
    Map<Unit,Integer> unitToId;


    public SlicerGraph(UnitGraph g, Set<Unit> unitsInSlice) {
      srcBody = g.getBody();
      int size = srcBody.getUnits().size();
      edges = new boolean[size][size];
      edgeLabels = new List[size][size];
      this.unitsInSlice = new boolean[size];
      idToUnit = new Unit[size];
      unitToId = new HashMap<Unit, Integer>();

      /* Initializes the nodes */
      int id = 0;
      for (Unit u : g.getBody().getUnits()) {
        idToUnit[id] = u;
        this.unitsInSlice[id] = unitsInSlice.contains(u);
        unitToId.put(u, new Integer(id));

        for (int j = 0; j < edges.length; j++) {
          edges[id][j] = false;
          edgeLabels[id][j] = null;
        }

        id = id + 1;
      }

      /* Fill the edges matrix */
      for (Unit u : g.getBody().getUnits()) {
        id = unitToId.get(u).intValue();

        LabelHandler handler = new LabelHandler(u);

        for (Unit succ : g.getSuccsOf(u)) {
          int dstId = unitToId.get(succ).intValue();
          edges[id][dstId] = true;

          List<Object> conditions = handler.getConditions(idToUnit[dstId]);
          if (null != conditions) {
            edgeLabels[id][dstId] = conditions;
          }
        }
      }

      printGraph("/tmp/pre_slice.dot");

      /* computes the transitive edges across the nodes not in the slice */
      transitiveClosure();

      /* remove all the edges not in the slice */
        for (int i = 0; i < edges.length; i++) {
          for (int j = 0; j < edges.length; j++) {
          if (! isEdgeInSlice(i,j)) edges[i][j] = false;
        }
      }

      printGraph("/tmp/post_slice.dot");
    }

    private void printGraph(String fname) {
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
        writer.write("digraph G {\n");
        for (int i = 0; i < edges.length; i++) {
          for (int j = 0; j < edges.length; j++) {
            if (edges[i][j]) {
              writer.write("n" + i + " -> " + "n" + j + "[ label = \"" + this.edgeLabels[i][j] + "\" ];\n") ;
            }
          }
        }
        writer.write("}\n");
        writer.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    private boolean isEdgeInSlice(int i, int j)
    {
      return this.unitsInSlice[i] && this.unitsInSlice[j];
    }

    private boolean hasEdges(int srcUnitId) {
      boolean hasEdges = false;
      for (int j = 0; j < edges.length && ! hasEdges; j++)
        hasEdges = hasEdges || (edges[srcUnitId][j]);
      return hasEdges;
    }
    /**
     * Compute the closure of the transitions among the node not in the slice.
     *
     * Use a modified version of Floyd-Warshall
     *
     */
    private void transitiveClosure() {
      for (int k = 0; k < edges.length; k++) {
        /* Skip all the executions where k (the intermediate node) is in the
         * slice.
         *
         * In this case we do not want to create a transitive edge.
         * */
        if (this.unitsInSlice[k]) continue;
        for (int j = 0; j < edges.length; j++) {
          for (int i = 0; i < edges.length; i++) {
            if (edges[i][j]) continue; /* already an edge */
            if (edges[i][k] && edges[k][j]) {
              edges[i][j] = true;

              if (this.unitsInSlice[i]) {
                /* set the label for the edge */
                edgeLabels[i][j] = edgeLabels[i][k];
              }
            }
          }
        }
      }
    }

    private Body getEmptyDstBody(Body srcBody)
    {
      SootMethod srcMethod = srcBody.getMethod();
      SootClass srcClass = srcMethod.getDeclaringClass();
      assert (null != srcMethod);
      String methodName = srcMethod.getName();
      methodName = getSlicedMethodName(methodName);

      SootMethod dstMethod = new SootMethod(methodName,
                                            srcMethod.getParameterTypes(),
                                            srcMethod.getReturnType(),
                                            srcMethod.getModifiers());
      JimpleBody dstBody = Jimple.v().newBody(dstMethod);
      dstMethod.setActiveBody(dstBody);
      srcClass.addMethod(dstMethod);

      return dstBody;
    }

    public Body getSlicedBody()
    {
      HashMap<Object, Object> bindings = new HashMap<Object, Object>();
      PatchingChain<Unit> srcChain = srcBody.getUnits();
      Stack<Integer> toVisit = new Stack<Integer>();
      Map<Unit, Integer> statusMap = new HashMap<Unit, Integer>();
      Unit[] idToDstUnit = new Unit[idToUnit.length];

      Body dstBody = getEmptyDstBody(srcBody);
      PatchingChain<Unit> dstChain = dstBody.getUnits();

      Unit dstFirst = Jimple.v().newNopStmt();
      Unit dstLast = Jimple.v().newNopStmt();
      dstChain.addFirst(dstFirst);
      dstChain.addLast(dstLast);

      assert (unitToId.containsKey(srcChain.getFirst()));
      toVisit.push(unitToId.get(srcChain.getFirst()));

      while (! toVisit.isEmpty()) {
        int status = 0;

        int srcUnitId = toVisit.pop().intValue();
        Unit srcUnit = this.idToUnit[srcUnitId];
        Unit dstUnit = null;

        Integer statusInt = statusMap.get(srcUnit);
        if (null != statusInt) status = statusInt.intValue();

        switch (status) {
        case 0:
          /* Never visited node:
             - Copy the node
             - Add the binding
             - Add node to the dst patching chain
             - Schedule the visit of all the children
           */
          dstUnit = (Unit) srcUnit.clone();
          idToDstUnit[srcUnitId] = dstUnit;
          bindings.put(srcUnit, dstUnit);
          dstUnit.addAllTagsOf(srcUnit);
          dstChain.insertBeforeNoRedirect(dstUnit, dstLast);
          //insertBefore(dstUnit, dstLast);

          /* Schedule the visit of all the children.
             Here we insert as LAST element the next element in the patching
             chain, while before we insert all the branches.
          */
          List<Integer> successors = new ArrayList<Integer>();
          for (int j = 0; j < edges[srcUnitId].length; j++) {
            if (! unitsInSlice[j]) continue;

            if (edges[srcUnitId][j] &&
                !statusMap.containsKey(new Integer(j))) {
              if (null != this.edgeLabels[srcUnitId][j]) {
                /* label is not null, it is a jump */
                successors.add(0, new Integer(j));
              }
              else {
                successors.add(new Integer(j));
              }
            }
          }

          /* add elements to the stack */
          toVisit.push(srcUnitId);
          for (Integer i : successors) toVisit.push(i);

          /* Visited in pre-order */
          statusMap.put(srcUnit, 1);

          break;
        case 1:
          /* Already visited once in preorder.

             - Adjust the gotos to the children.
             The children have been created, we just need to fix the gotos.
          */
          System.out.println("Post-visit " + srcUnitId);
          dstUnit = idToDstUnit[srcUnitId];

          /* redirect gotos */
          LabelHandler labelHandler = new LabelHandler(dstUnit);
          Map<Object, List<Unit>> c2t = getConditions2Targets(srcUnitId, idToDstUnit);
          labelHandler.fixGotos(c2t);

          /* redirect to final node */
          if (! hasEdges(srcUnitId)) {
            dstChain.insertAfter(Jimple.v().newGotoStmt(dstLast), dstUnit);
          }

          /* Visited in post-order */
          statusMap.put(srcUnit, 2);

          break;
        case 2:
          /* already visited in postorder - do nothing */
          break;
        default:
          assert false;
        }
      }


      {
        // Clone local units.
        Chain<Local> dstLocalChain = dstBody.getLocals();
        for (soot.Local srcLocal : srcBody.getLocals()) {
          soot.Local dstLocal = (soot.Local) srcLocal.clone();

          // Add cloned unit to our trap list.
          dstLocalChain.addLast(dstLocal);

          // Build old <-> new mapping.
          bindings.put(srcLocal, dstLocal);
        }
      }

      // TODO add exceptions?

      {
        // backpatching all local variables.
        for (ValueBox vb : dstBody.getUseBoxes()) {
          if(vb.getValue() instanceof Local)
            vb.setValue((Value) bindings.get(vb.getValue()));
        }
        for (ValueBox vb : dstBody.getDefBoxes()) {
          if(vb.getValue() instanceof Local)
            vb.setValue((Value) bindings.get(vb.getValue()));
        }
      }

      return dstBody;
    }


    private Map<Object, List<Unit>> getConditions2Targets(int srcUnitId,
                                                          Unit[] idToDstUnit)
    {
      Map<Object, List<Unit>> c2t = new HashMap<Object, List<Unit>>();

      for (int j = 0; j < edges[srcUnitId].length; j++) {
        if (isEdgeInSlice(srcUnitId, j) &&
            (null != this.edgeLabels[srcUnitId][j])) {
          List<Object> conditions = this.edgeLabels[srcUnitId][j];

          for (Object condition : conditions) {
            List<Unit> targets = c2t.get(condition);
            if (null == targets) {
              targets = new ArrayList<Unit>();
              c2t.put(condition, targets);
            }
            targets.add(idToDstUnit[j]);
          }
        }
      }

      return c2t;
    }

    private class LabelHandler {
      Unit sourceUnit;
      private Map<Unit,List<Object>> target2conditions;
      private Map<Object,List<Unit>> condition2targets;

      private final static String DEFAULT = "default";

      public LabelHandler(Unit sourceUnit)
      {
        this.sourceUnit = sourceUnit;
        buildMaps();
      }

      public List<Object> getConditions(Unit target) {
        List<Object> conditions = this.target2conditions.get(target);
        return conditions;
      }

      public void fixGotos(Map<Object, List<Unit>> c2t) {
        if (sourceUnit instanceof GotoStmt) {
          GotoStmt gotoStmt = (GotoStmt) sourceUnit;

          /* use the target as label */
          List<Unit> targets = c2t.get(DEFAULT);
          assert (targets.size() == 1);
          Unit target = targets.get(0);
          gotoStmt.setTarget(target);
        } else if (sourceUnit instanceof IfStmt) {
          IfStmt ifstmt = (IfStmt) sourceUnit;

          assert(c2t.containsKey(DEFAULT));

          List<Unit> targets = c2t.get(DEFAULT);
          assert (targets.size() == 1);
          Unit target = targets.get(0);
          ifstmt.setTarget(target);
        }
        else if (sourceUnit instanceof LookupSwitchStmt) {
          LookupSwitchStmt switchStmt = (LookupSwitchStmt) sourceUnit;

          for (int i = 0; i < switchStmt.getTargets().size(); i ++) {
            Object condition = switchStmt.getLookupValue(i);
            assert(c2t.containsKey(condition));
            List<Unit> targets = c2t.get(condition);
            assert(targets.size() > 0);
            Unit target = targets.get(0);
            switchStmt.setTarget(i, target);
            targets.remove(0); /* consume the target */
          }

          /* add default */
          List<Unit> targets = c2t.get(DEFAULT);
          if (targets.size() > 0)
            switchStmt.setDefaultTarget(targets.get(0));
        }
        else if (sourceUnit instanceof TableSwitchStmt) {
          TableSwitchStmt switchStmt = (TableSwitchStmt) sourceUnit;

          for (int i = switchStmt.getLowIndex();
               i <= switchStmt.getHighIndex(); i++) {
            Object condition = new Integer(i);
            assert(c2t.containsKey(condition));
            List<Unit> targets = c2t.get(condition);
            assert(targets != null);
            assert(targets.size() > 0);
            Unit target = targets.get(0);
            switchStmt.setTarget(i, target);
            targets.remove(0); /* consume the target */
          }

          /* add default */
          List<Unit> targets = c2t.get(DEFAULT);
          if (targets.size() > 0)
            switchStmt.setDefaultTarget(targets.get(0));
        }
        else {
          assert(c2t.size() == 0);
        }
      }

      private void buildMaps()
      {
        target2conditions = new HashMap<Unit,List<Object>>();
        condition2targets = new HashMap<Object,List<Unit>>();

        if (sourceUnit instanceof GotoStmt) {
          GotoStmt gotoStmt = (GotoStmt) sourceUnit;
          Unit target = gotoStmt.getTarget();

          /* use the target as label */
          addToMapO(condition2targets, DEFAULT, target);
          addToMapU(target2conditions, target, DEFAULT);
        }
        else if (sourceUnit instanceof IfStmt) {
          IfStmt ifstmt = (IfStmt) sourceUnit;
          Unit target = ifstmt.getTarget();

          addToMapO(condition2targets, DEFAULT, target);
          addToMapU(target2conditions, target, DEFAULT);
        }
        else if (sourceUnit instanceof LookupSwitchStmt) {
          LookupSwitchStmt switchStmt = (LookupSwitchStmt) sourceUnit;

          int i = 0;
          for (Unit target : switchStmt.getTargets()) {
            Object condition = switchStmt.getLookupValue(i);
            addToMapO(condition2targets, condition, target);
            addToMapU(target2conditions, target, condition);

            i = i + 1;
          }

          Unit defaultTarget = switchStmt.getDefaultTarget();
          if (defaultTarget != null) {
            addToMapO(condition2targets, DEFAULT, defaultTarget);
            addToMapU(target2conditions, defaultTarget, DEFAULT);
          }
        }
        else if (sourceUnit instanceof TableSwitchStmt) {
          TableSwitchStmt switchStmt = (TableSwitchStmt) sourceUnit;

          List<? extends Unit> targets = switchStmt.getTargets();
          for (int i = switchStmt.getLowIndex(); i <= switchStmt.getHighIndex();
               i++) {
            Unit target = targets.get(i);
            Object condition = new Integer(i);
            addToMapO(condition2targets, condition, target);
            addToMapU(target2conditions, target, condition);
          }

          Unit defaultTarget = switchStmt.getDefaultTarget();
          if (defaultTarget != null) {
            addToMapO(condition2targets, DEFAULT, defaultTarget);
            addToMapU(target2conditions, defaultTarget, DEFAULT);
          }
        }
      }

      private void addToMapU(Map<Unit, List<Object>> map, Unit key, Object element) {
        List<Object> list = map.get(key);
        if (null == list) {
          list = new ArrayList<Object>();
          map.put(key, list);
        }
        list.add(element);
      }
      private void addToMapO(Map<Object, List<Unit>> map, Object key,
          Unit element) {
        List<Unit> list = map.get(key);
        if (null == list) {
          list = new ArrayList<Unit>();
          map.put(key, list);
        }
        list.add(element);
      }
    }
  }
}
