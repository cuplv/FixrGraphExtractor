package edu.colorado.plv.fixr.graphs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.colorado.plv.fixr.slicing.ReachingDefinitions;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.InstanceFieldRef;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.SimpleLocalDefs;

/**
 * Data dependency graph
 * 
 * Nodes of the graph are of Unit  type (i.e. statements)
 * There is an edge e from a unit u1 to a unit u2 if a variable defined in u1 
 * is used in u2.
 * 
 * Note: the data dependency graph is not complete. It does not compute the 
 * edges of type antidependence, output dependence and input dependence (we 
 * don't need them for slicing).
 * They are useful to parallelize the execution of instructions. 
 * 
 * @author Sergio Mover
 *
 */
public class DataDependencyGraph implements DirectedGraph<Unit> {
	protected UnitGraph srcGraph;
	
	protected List<Unit> graphNodes;
	protected List<Unit> heads;
	protected List<Unit> tails;	
	protected Map<Unit, List<Unit>> preds;
	protected Map<Unit, List<Unit>> succ;
	protected ReachingDefinitions reachingDefinitions;
	protected LocalDefs ld; 
	
	/**
	 * Generate a data dependency graph from a CFG
	 * 
	 * @param graph
	 */
	public DataDependencyGraph(UnitGraph graph) {			
		this.srcGraph = graph;
		
		graphNodes = new ArrayList<Unit>();
		heads = new ArrayList<Unit>();
		tails = new ArrayList<Unit>();			
		preds = new HashMap<Unit, List<Unit>>();
		succ = new HashMap<Unit, List<Unit>>();		
		
		/* Computes the use-dependency (UD) relation */
		ld = new SimpleLocalDefs(graph);
		reachingDefinitions = new ReachingDefinitions(graph);
		buildGraph();
	}

	/**
	 * Build the dependency graph of this.srcGraph 
	 */
	protected void buildGraph()
	{
		/* compute graph */
		for (Unit u : this.srcGraph) {			
			Collection<Unit> defsOfUnit = getDefsOf(u);
			List<Unit> predList = getPredsOf(u);
			
			graphNodes.add(u);
			
			for (Unit reachedUnits : defsOfUnit) {
				List<Unit> succList = getSuccsOf(reachedUnits);
				succList.add(u);
				predList.add(reachedUnits);				  
			}								
		}
		
		/* compute heads and tails */
		for (Unit u : graphNodes) {
			List<Unit> succList = getSuccsOf(u);
			List<Unit> predList = getPredsOf(u);
			
			if (succList.isEmpty()) tails.add(u);
			if (predList.isEmpty()) heads.add(u);
		}
	}
	
	private List<Unit> getListFromMap(Map<Unit, List<Unit>> map, Unit u)
	{
			List<Unit> list = map.get(u); 
			if (null == list) {
				list = new ArrayList<Unit>(); 
				map.put(u, list);				
			}
			return list;
	}
	
	/**
	 * Returns all the units that define a variable used in srcUnit. 
	 * 
	 * @param unit
	 * @return
	 */
	protected Collection<Unit> getDefsOf(Unit srcUnit) {
		return DataDependencyGraph.getDefsOf(srcUnit, this.reachingDefinitions);		
	}

	/**
	 * Returns all the units that define a variable used in srcUnit. 
	 * 
	 * @param unit
	 * @return
	 */
	public static Collection<Unit> getDefsOf(Unit srcUnit, ReachingDefinitions rd) {
		Set<Unit> defsOf = new HashSet<Unit>();
		for (Unit u : rd.getReachableAt(srcUnit)) {
			if (rd.unitDefines(srcUnit, u, true)) {
				defsOf.add(u);
			}
		}
		return defsOf;
	}

	
	/**
	 * Returns all the units that define a variable used in srcUnit. 
	 * 
	 * @param unit
	 * @return
	 */
	@Deprecated
	public static Set<Unit> getDefsOf(Unit srcUnit, LocalDefs ld) {
		Set<Unit> res = new HashSet<Unit>();
		
		/* get variables used by srcUnit */
		for (ValueBox b : srcUnit.getUseBoxes()) {
			Local local = DataDependencyGraph.getLocalFromValue(b.getValue());
			if (null != local) {
				for (Unit dstUnit : ld.getDefsOfAt(local, srcUnit)) {
					/* Add a unit to the result if it defines v */					
					res.add(dstUnit);	
				}				
			}						
		}
		return res;
	}
	
	private static Local getLocalFromValue(Value v) {			
			// TODO Consider other interesting cases (e.g. access to arrays...)
			if (v instanceof Local) {
				return (Local) v;
			}
			else if (v instanceof ArrayRef) {				
				Value arrayBase = ((ArrayRef) v).getBase();
				assert arrayBase instanceof Local;
				return (Local) arrayBase;
			}
			else if (v instanceof InstanceFieldRef) {
				Value instanceBase = ((InstanceFieldRef) v).getBase();
				assert instanceBase instanceof Local;
				return (Local) instanceBase;				
			}
//			else if (v instanceof FieldRef) {
//				SootField f = ((FieldRef) v).getField();
//				
//			}
			
			return null; 
	}
	
	@Override
	public List<Unit> getHeads() { 
		return heads;
	}

	@Override
	public List<Unit> getPredsOf(Unit u) {
		return getListFromMap(preds, u);
	}

	@Override
	public List<Unit> getSuccsOf(Unit u) {
		return getListFromMap(succ, u);
	}

	@Override
	public List<Unit> getTails() {
		return tails;
	}

	@Override
	public Iterator<Unit> iterator() {
		return graphNodes.iterator();
	}

	@Override
	public int size() {
		return graphNodes.size();
	}

}
