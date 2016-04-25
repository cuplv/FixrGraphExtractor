package edu.colorado.plv.fixr.slicing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import soot.Local;
import soot.Unit;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;

/**
 * Computes the relevant variables for the slicing criterion for each node of the CFGs.  
 *  
 * @author Sergio Mover
 *
 */
public class RelevantVariablesAnalysis extends BackwardFlowAnalysis<Unit, RVDomain> {
	private SlicingCriterion sc;
	
	private UseHelper helper;
	
	/* set of seeds */
	private Set<Unit> seeds;
	/* nodes in relevantStmt */
	private Set<Unit> relevantStmt;
		
	
	public RelevantVariablesAnalysis(DirectedGraph<Unit> graph, SlicingCriterion criterion) {
		super(graph);
		this.sc = criterion;		
		
		this.helper = UseHelper.getHelper();
		this.seeds = new HashSet<Unit>();
		this.relevantStmt = new HashSet<Unit>();		
		
		this.doAnalysis();
	}
	
	public Set<Unit> getSeeds() {
		return this.seeds;
	}
	
	protected void addToRelevantStmt(Unit unit) 
	{
		if (! this.relevantStmt.contains(unit)) this.relevantStmt.add(unit);		
	}
	
	@Override
	protected void flowThrough(RVDomain in, Unit unit, RVDomain out) {
		RVDomain def_unit = this.helper.get_defs(unit);
		RVDomain use_unit = this.helper.get_uses(unit);
		
		/*  
		 * In changes either if: 
		 *   - in is not a path from a seed but unit is a seed
		 *   - in is on a path from a seed 
		 */
		boolean toProcess = in.isReachSeed();
		assert this.sc != null;
		boolean is_seed = this.sc.is_seed(unit);
		if (! toProcess && is_seed) {
			/* first visit of the seed */
			use_unit.copy(out);
		  out.setReachSeed(true);
						
			this.seeds.add(unit);
			this.addToRelevantStmt(unit);			
		}
		
		if (toProcess) {
			/* We are visiting the node in unit.
			 * 
			 * in is the set of relevant variables from a predecessor
			 * out is the set of relevant variables for unit
			 * 
			 * We define out as follows:
			 * 
 			 * out = (in \ DEF(unit)) U (USE(unit) if intersection(in, DEF(unit)) is not empty )    
			 */
			in.difference(def_unit, out);
			if (in.intersect(def_unit)) {
				out.union(use_unit);
				this.addToRelevantStmt(unit);
			}			
		}						
	}

	/**
	 * @return the relevantStmt
	 */
	public Set<Unit> getRelevantStmt() {
		return relevantStmt;
	}

	@Override
	protected void copy(RVDomain source, RVDomain dest) {
		source.copy(dest);
	}

	@Override
	protected RVDomain entryInitialFlow() {			
		return new RVDomain();
	}

	@Override
	protected void merge(RVDomain in1, RVDomain in2, RVDomain out) {
		out.union(in1);
		out.union(in2);
	}

	@Override
	protected RVDomain newInitialFlow() {			
		return new RVDomain();
	}
	

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String res = "";
		for (Iterator<Unit> iter = this.graph.iterator(); iter.hasNext();) {
			Unit u = iter.next();
			RVDomain d = getFlowBefore(u);
			
			res = res + "Rel var for " + u + ":";
			for (Iterator<Local> rvIter = d.iterator(); rvIter.hasNext();) {
				Local l = rvIter.next();
				res = res + " " + l;			
			}
			res = res + "\n---\n";			
		}		 
		
		return res;
	}
	
	
}
