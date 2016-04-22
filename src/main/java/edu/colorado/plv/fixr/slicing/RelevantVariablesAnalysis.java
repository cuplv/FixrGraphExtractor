package edu.colorado.plv.fixr.slicing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
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
	
	/* definitions of node */
	private Map<Unit, RVDomain> defs;
	/* ref of nodes */
	private Map<Unit, RVDomain> uses;
	
	public RelevantVariablesAnalysis(DirectedGraph<Unit> graph, SlicingCriterion criterion) {
		super(graph);
		this.sc = criterion;		
		
		this.defs = new HashMap<Unit, RVDomain>();
		this.uses = new HashMap<Unit, RVDomain>();		
		
		this.doAnalysis();
	}

	@Override
	protected void flowThrough(RVDomain in, Unit unit, RVDomain out) {
		RVDomain def_unit = this.get_defs(unit);
		RVDomain ref_unit = this.get_uses(unit);
		
		/*  
		 * In changes either if: 
		 *   - in is not a path from a seed but unit is a seed
		 *   - in is on a path from a seed 
		 */
		boolean toProcess = in.isReachSeed();
		assert this.sc != null;
		boolean is_seed = this.sc.is_seed(unit);
		if (! toProcess && is_seed) {
			
			/* get DEF and REF of unit */
			
			if (! out.isReachSeed()) {
				/* first visit */
				def_unit.copy(out);
				out.union(ref_unit);
			}
			else {
				toProcess = true;
			}
		}
		
		if (toProcess) {
			/* We are visiting the node in unit.
			 * 
			 * in is the set of relevant variables from a predecessor
			 * out is the set of relevant variables for unit
			 * 
			 * We define out as follows:
			 * 
			 * out = (in \ DEF(unit)) U (REF(unit) if intersection(in, DEF(unit)) is not empty ) 
			 * 
			 * DEF(unit)
			 * REF(unit)  
			 */
			in.difference(def_unit, out);
			if (in.intersect(def_unit)) {
				out.union(def_unit);
			}
		}						
	}

	@Override
	protected void copy(RVDomain source, RVDomain dest) {
//		System.out.println("Copy");		

		source.copy(dest);
		
//		System.out.print("Source:" + source.toString());
//		System.out.print("Dest:" + dest.toString());
	}

	@Override
	protected RVDomain entryInitialFlow() {
//		System.out.println("Entry initial flow");			
		return new RVDomain();
	}

	@Override
	protected void merge(RVDomain in1, RVDomain in2, RVDomain out) {
		out.union(in1);
		out.union(in2);
		
//		System.out.print("Merging\nIn1:" + in1.toString());
//		System.out.println("In2:" + in2.toString());		
//		System.out.println("Out:" + out.toString());		
	}

	@Override
	protected RVDomain newInitialFlow() {			
		return new RVDomain();
	}
	
	protected RVDomain get_defs(Unit unit) {
		RVDomain unit_defs = this.defs.get(unit);
		if (null == unit_defs) {
			/* compute the definition for the unit */
			unit_defs = filterToLocal(unit.getDefBoxes()); 
			this.defs.put(unit, unit_defs);
		}
		return unit_defs;
	}
	
	protected RVDomain get_uses(Unit unit) {
		RVDomain unit_uses= this.uses.get(unit);
		if (null == unit_uses) {			 
			unit_uses = filterToLocal(unit.getUseBoxes());			
			this.uses.put(unit, unit_uses);
		}
		return unit_uses;
	}	
	
	/**
	 * Filter a collection of valuebox (references to values) returning the containing values.
	 * The method only keeps the values that refer to local variables.
	 *  
	 * @param valBoxList
	 * @return an RVDomain containing local variables
	 */
	protected RVDomain filterToLocal(Collection<ValueBox> valBoxList)
	{
		RVDomain d = new RVDomain(); 
		for (ValueBox x : valBoxList) {
			Value v = x.getValue();
			// TODO Consider other interesting cases (e.g. access to arrays...)
			if (v instanceof Local) d.add(v);			
		}
		
		return d;
	}
}
