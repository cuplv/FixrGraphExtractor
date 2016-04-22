package edu.colorado.plv.fixr.slicing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.colorado.plv.fixr.SootHelper;
import soot.Body;
import soot.Immediate;
import soot.Local;
import soot.PatchingChain;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.EnhancedBlockGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.graph.pdg.ProgramDependenceGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

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
	public BlockGraph slice() {
		/**
		 * 1. Computes the set of relevant variables for each CFG node. 
		 */		
		SlicingCriterion sc = new AndroidMethodsCriterion();
		RelevantVariablesAnalysis rv = new RelevantVariablesAnalysis(this.cfg, sc);
		
		for (Iterator<Unit> iter = this.cfg.iterator(); iter.hasNext();) {
			Unit u = iter.next();
			RVDomain d = rv.getFlowAfter(u);
			
			System.out.println("Rel var for " + u + ":");
			for (Iterator<Local> rvIter = d.iterator(); rvIter.hasNext();) {
				Local l = rvIter.next();
				System.out.print(" " + l);			
			}
			System.out.println("\n---");
		}
		
		
		/**
		 * 2. Construct the new CFG 
		 */
		BlockGraph slice = buildSlice(rv);
		
		return null;
	}
	
	/**
	 * Return a slice (as a block graph) of the cfg that just contains 
	 * the CFG nodes that have a non-empty set of relevant variables
	 *  
	 * TODO probably this has to be implemented in the SlicedEnhancedBlockGraph
	 *  
	 * @return a sliced block graph
	 */
	private BlockGraph buildSlice(RelevantVariablesAnalysis rv) {
		//EnhancedBlockGraph ebg = new EnhancedBlockGraph();
		
		return null;
	}	
}
