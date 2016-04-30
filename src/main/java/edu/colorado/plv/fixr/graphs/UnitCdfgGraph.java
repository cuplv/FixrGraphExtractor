package edu.colorado.plv.fixr.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.toolkits.exceptions.ThrowAnalysis;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
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
public class UnitCdfgGraph extends BriefUnitGraph {
	
	protected ExceptionalUnitGraph unitGraph;
	
	protected List<Local> localsList= null;
	protected Map<Local, List<Unit>> useEdges = null;
	protected Map<Unit, List<Local>> defEdges = null;	
		
	public UnitCdfgGraph(Body body) {
		super(body);		
		
		addDataDependentNodes();
	}
	
	public Iterator<Unit> unitIterator() {
		return unitChain.iterator();
	}		
	
	/**
	 * Generates the data flow graph
	 */
	private void addDataDependentNodes()
	{
		assert useEdges == null && defEdges == null;
		
		SimpleLocalDefs defs = new SimpleLocalDefs(this);		

		/* scan the dependency graph to get the dependencies */
		useEdges = new HashMap<Local, List<Unit>>();
		defEdges = new HashMap<Unit, List<Local>>();
		
		localsList = new ArrayList<Local>();
		for (Local l : this.getBody().getLocals()) {
			localsList.add(l);
			assert ! useEdges.containsKey(l);
			useEdges.put(l, new ArrayList<Unit>());
		}

		for (Iterator<Unit> unitIter = unitIterator(); unitIter.hasNext(); ) {
			Unit unit = unitIter.next();
		
			/* Defs list */
			List<Local> defsList = new ArrayList<Local>();
			defEdges.put(unit, defsList);			
			for (ValueBox b : unit.getDefBoxes()) {
				Value v = b.getValue();
				if (v instanceof Local) defsList.add((Local) v);
			}
			
			/* use list.
			 * 
			 * TODO: change the implementation, it will not scale now. 
			 * 
			 * */
			for (ValueBox b : unit.getUseBoxes()) {
				Value v = b.getValue();
				if (v instanceof Local) {
					for (Unit dstUnit : defs.getDefsOfAt((Local) v, unit)) {
						for (ValueBox dstB : dstUnit.getDefBoxes()) {
							Value dstV = dstB.getValue();							
							/* dstV is used by this unit */
							if (dstV instanceof Local) {
								List<Unit> dstEdges = useEdges.get(dstV);
								assert dstEdges != null;								
								dstEdges.add(dstUnit);
							}
						}													
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
	
}
