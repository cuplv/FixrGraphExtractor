package edu.colorado.plv.fixr.slicing;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
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
		Body srcBody = this.cfg.getBody();
		SootMethod srcMethod = srcBody.getMethod();
		SootClass srcClass = srcMethod.getDeclaringClass();
		assert (null != srcMethod);
		String methodName = srcMethod.getName();
		methodName = methodName + "__sliced__";
		
		SootMethod dstMethod = new SootMethod(methodName, srcMethod.getParameterTypes(),
				srcMethod.getReturnType(), srcMethod.getModifiers());
		JimpleBody dstBody = Jimple.v().newBody(dstMethod);
		dstMethod.setActiveBody(dstBody);
		srcClass.addMethod(dstMethod);

		dstBody.importBodyContentsFrom(srcBody);
		
		/* copy local variables - to narrow down */
		for (Local l : srcBody.getLocals()) {
			dstBody.getLocals().add(l);
		}
		
		PatchingChain<Unit> pc = srcBody.getUnits();		
//		for (Unit u : pc) {
//			if (pc.getFirst() != u && pc.getLast() != u &&
//					!sa.isInSlice(u)) {					 
//				srcBody.getUnits().remove((Unit) u);
//			}
//			
//			// TODO fix temporary workaround for first and last  
////			if (pc.getFirst() == u || pc.getLast() == u ||
////					sa.isInSlice(u)) {
////				dstBody.getUnits().add((Unit) u.clone());				
////			}
//		}		

		List<Unit> toRemove = new LinkedList<Unit>();
		for (Unit u : pc) {
			if (pc.getFirst() != u && pc.getLast() != u &&
					!sa.isInSlice(u)) {
				toRemove.add(u);
				
			}
		}
		for (Unit u : toRemove) {
			// REMOVE does the magic for us
			srcBody.getUnits().remove((Unit) u);
		}

		//System.out.println(dstBody.getUnits());
		System.out.println(srcBody.getUnits());		
		
		EnhancedUnitGraph ug = new EnhancedUnitGraph(srcBody);
		assert(null != ug);
		
		return ug;		
	}	
}
