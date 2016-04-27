package edu.colorado.plv.fixr.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.SimpleLocalDefs;

/**
 * Data dependency graph
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
		
		buildGraph();
	}

	/**
	 * Build the dependency graph of this.srcGraph 
	 */
	protected void buildGraph()
	{
		/* compute graph */
		for (Unit u : this.srcGraph) {			
			Set<Unit> defsOfUnit = getDefsOf(u);
			List<Unit> succList = getSuccsOf(u);
			
			graphNodes.add(u);
			
			for (Unit reachedUnits : defsOfUnit) {
				List<Unit> predList = getPredsOf(reachedUnits);
				succList.add(reachedUnits);
				predList.add(u);				  
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
	 * Returns all the dependencies of the variables in unit 
	 * 
	 * @param unit
	 * @return
	 */
	protected Set<Unit> getDefsOf(Unit srcUnit) {
		Set<Unit> res = new HashSet<Unit>();
		for (ValueBox b : srcUnit.getUseBoxes()) {
			Value v = b.getValue();
			// TODO Consider other interesting cases (e.g. access to arrays...)
			if (v instanceof Local) { 
				for (Unit dstUnit : this.ld.getDefsOfAt((Local) v, srcUnit)) {
					res.add(dstUnit);	
				}
			}
		}
		return res;
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
