package edu.colorado.plv.fixr.slicing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import soot.Body;
import soot.PatchingChain;
import soot.Unit;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;
import soot.toolkits.graph.pdg.ProgramDependenceGraph;

/**
 * Slice a CFG of a method using as seeds the nodes of the API calls
 * 
 * Ad-hoc implementation of the slicer.
 * We have to make it general (e.g. an interface that is agnostic of the cfg type).
 * We have to pass as input the filter used in the slicing (e.g. prefix of the package of method calls).
 * 
 * @author Sergio Mover
 *
 */
public class APISlicer {
	private UnitGraph cfg;
	private Body body;
	private ProgramDependenceGraph pdg;
	
	public APISlicer(UnitGraph cfg, Body body) {
		super();
		this.cfg = cfg;
		this.body = body;		
	}	 
 
	/**
	 * Computes the slice of this.cfg according to the android API method calls.
	 * @return
	 */
	public UnitGraph slice(SlicingCriterion sc) {
		/**
		 * 1. Computes the set of relevant variables for each CFG node. 
		 */		
		SliceStmtAnalysis sa = new SliceStmtAnalysis(this.cfg, sc); 
		
		/**
		 * 2. Construct the new CFG 
		 */
		UnitGraph slice = buildSlice(sa);
		
		return slice;
	}
	
	/**
	 * Return a slice (as a block graph) of the cfg that just contains 
	 * the CFG nodes that have a non-empty set of relevant variables
	 *  
	 * TODO probably this has to be implemented in the SlicedEnhancedBlockGraph
	 *  
	 * @return a sliced block graph
	 */
	private UnitGraph buildSlice(SliceStmtAnalysis sa) {		
		PatchingChain<Unit> pc = this.cfg.getBody().getUnits();

		Set<Unit> toRemove = new HashSet<Unit>();
		for (Unit u : pc) {
			if (! sa.isInSlice(u)) {
				toRemove.add(u);
			}
		}
		
		for (Unit u : toRemove) {
			pc.remove(u);
		}
		
		System.out.println(pc);
		
		return null;		
	}	
}
