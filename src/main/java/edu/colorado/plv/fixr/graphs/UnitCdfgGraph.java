package edu.colorado.plv.fixr.graphs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import edu.colorado.plv.fixr.slicing.ReachingDefinitions;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

/**
 * Data structure for the control-data-flow graph.
 *
 * TODO: instead of extending EnhancedUnitGraph, implements the directed graph
 * interface.
 * It is more work, but we can deal with variables node uniformly.
 *
 * @author Sergio Mover
 *
 */
public class UnitCdfgGraph extends EnhancedUnitGraph {
  protected List<Local> localsList = null;
  protected List<Unit>  unitsList  = null;
  protected Map<Local, List<Unit>> useEdges = null;
  protected Map<Unit, List<Local>> defEdges = null;
  protected DataDependencyGraph ddg = null;

  public UnitCdfgGraph(Body body) {
    super(body);

    this.ddg = new DataDependencyGraph(this);
    addDataDependentNodes();

  }

  public UnitCdfgGraph(Body body, DataDependencyGraph ddg) {
    super(body);

    addDataDependentNodes();
  }

  public Map<Local, List<Unit>> useEdges() {return useEdges;}
  public Map<Unit, List<Local>> defEdges() {return defEdges;}


  public Iterator<Unit> unitIterator() {
    return unitChain.iterator();
  }

  private void addDataDependentNodes()
  {
    /* The construction should happen at most once */
    assert useEdges == null && defEdges == null;
    useEdges = new HashMap<Local, List<Unit>>();
    defEdges = new HashMap<Unit, List<Local>>();

    /* Generate the list of all the locals */
    localsList = new ArrayList<Local>();
    for (Local l : this.getBody().getLocals()) {
      localsList.add(l);
      assert ! useEdges.containsKey(l);
      useEdges.put(l, new ArrayList<Unit>());
    }

    /* Generate the list of the all units */
    unitsList = new ArrayList<Unit>();
    for (Unit u : this.getBody().getUnits()) {
      unitsList.add(u);
      assert ! defEdges.containsKey(u);
      defEdges.put(u, new ArrayList<Local>());
    }

    /* Add the define edges -
     * NOTE: now we are not computing the transitive closure
     * */
    for (Unit u : unitsList) {
      ReachingDefinitions rd = ddg.getReachingDefinitions();
      List<Local> defsInU = defEdges.get(u);
      if (null == defsInU) {
        defsInU = new ArrayList<Local>();
        defEdges.put(u, defsInU);
      }
      defsInU.addAll(rd.getDefLocals(u, true));

      /* Add the use edges - inefficient */
      for (Unit pred : ddg.getPredsOf(u)) {
        Collection<Local> defsInPred = defEdges.get(pred);
        if (null != defsInPred) {
          for (Local l : defsInPred) {
            useEdges.get(l).add(u);
          }
        }
      }
    }
  }

  public Iterator<Local> localsIter()
  {
    return localsList.iterator();
  }

  public List<Local> getDefVars(Unit u) {
    List<Local> defsList = defEdges.get(u);
    assert defsList != null;
    return defsList;
  }

  public List<Unit> getUseUnits(Local l) {
    List<Unit> unitList= useEdges.get(l);
    assert unitList != null;
    return unitList;
  }

  public void checkGraph() {
    int i = 0;

    for (Iterator<Unit> iter = this.unitIterator();
         iter.hasNext(); iter.next()) {
      i += 1;
    };
    System.out.println("FOUND " + i);
  }
}
