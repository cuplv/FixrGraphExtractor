package edu.colorado.plv.fixr.graphs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.colorado.plv.fixr.slicing.ReachingDefinitions;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.toolkits.graph.BriefUnitGraph;
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
			/*
			for (Unit pred : ddg.getPredsOf(u)) {			
				Collection<Local> defsInPred = defEdges.get(pred);
				if (null != defsInPred) {
					for (Local l : defsInPred) {
						useEdges.get(l).add(u);					
					}
				}
			}
			*/
		}
		for (Local l : localsList) {
			List<Unit> usesInL = useEdges.get(l);
			if (null == usesInL) {
				usesInL = new ArrayList<Unit>();
				useEdges.put(l, usesInL);
			}
			usesInL.addAll(ddg.graphNodes);
		}
	}
	
	/**
	 * Generates the data flow graph
	 */
	private void addDataDependentNodesOld()
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
