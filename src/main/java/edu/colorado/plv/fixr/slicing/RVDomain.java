package edu.colorado.plv.fixr.slicing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.Local;
import soot.toolkits.scalar.FlowSet;


/**
 * Data structure used in the RelevantVariablesAnalysis as in and out set associated to each node.
 *
 * @author Sergio Mover
 *
 */
public class RVDomain implements FlowSet {
	
	/**
	 * True if the node can reach a seed in the CFG
	 * Lattice view: all the elements in relevantVariables have the same value of reachSeed
	 */
	private boolean reachSeed; 
	// TODO: Replace with an implementation of Soot FlowSet object (maybe bounded) for efficiency	
	/** 
	 * Set of relevant variables a
	 */
	private Set<Local> relevantVariables;
	
	/**
	 * @return the reachSeed
	 */
	public boolean isReachSeed() {
		return reachSeed;
	}

	
	public RVDomain() {
		this.reachSeed = false;
		this.relevantVariables = new HashSet<Local>();		
	}
		
	@Override
	public boolean isEmpty() {
		return this.relevantVariables.isEmpty();
	}

	@Override
	public void add(Object arg0) {
		assert arg0 instanceof Local;
		
		if (! this.reachSeed) this.reachSeed = true;
		this.relevantVariables.add((Local) arg0);	
	}

	@Override
	public void add(Object arg0, FlowSet dest) {
		//TODO implement add
		assert false;		
	}		 

	/**
	 * @param reachSeed the reachSeed to set
	 */
	public void setReachSeed(boolean reachSeed) {
		/* if reachSeed is false, then the relevant var set is empty */
		assert reachSeed || (this.relevantVariables.isEmpty()); 
		this.reachSeed = reachSeed;
	}


	@Override
	public void clear() {
		this.reachSeed = false;
		this.relevantVariables.clear();	
	}

	@Override
	public FlowSet clone() {
		RVDomain rv = new RVDomain();
		rv.reachSeed = this.reachSeed;		
		rv.relevantVariables.addAll(this.relevantVariables);		
		return rv;
	}

	@Override
	public boolean contains(Object arg0) {
		assert arg0 instanceof Local;
		
		return this.relevantVariables.contains((Local) arg0);	
	}
		
	public boolean hasVar(Object arg0) {
		assert arg0 instanceof Local;
		Local other = (Local) arg0;
		
		for (Local v : this.relevantVariables) {
			if (v.getName() == other.getName() &&
					v.getType().equals(other.getType())) {
				return true;
			}				
		}
		
		return false;
	}

	
	
	@Override
	public void copy(FlowSet dest) {
		RVDomain rv = (RVDomain) dest;
		rv.reachSeed = this.reachSeed;
		// No deep copy of the elements
		rv.relevantVariables.clear();
		rv.relevantVariables.addAll(this.relevantVariables);							
	}

	@Override
	public void difference(FlowSet other) {
		RVDomain rv = (RVDomain) other;
		if ((! this.reachSeed) || (! rv.reachSeed)) {
			// Bottom \ something = bottom
			// something \ Bottom = something
			return;
		}
		else {			
			this.relevantVariables.remove(rv.relevantVariables);
		}		
	}

	@Override
	public void difference(FlowSet other, FlowSet dest) {
		// dest = this - other
		RVDomain otherRv = (RVDomain) other;
		RVDomain destRv = (RVDomain) dest;

		if (! this.reachSeed) {
			dest.clear();
		}
		else if (! otherRv.reachSeed) {
			this.copy(dest);
		}
		else {
			assert this.reachSeed && otherRv.reachSeed;
			destRv.clear();
			destRv.reachSeed = true;			
			destRv.relevantVariables.addAll(this.relevantVariables);
			destRv.relevantVariables.removeAll(otherRv.relevantVariables);
		}
	}

	@Override
	public FlowSet emptySet() {
		RVDomain rv = new RVDomain();
		return rv;
	}

	@Override
	public void intersection(FlowSet other) {
		RVDomain otherRv = (RVDomain) other;
		this.reachSeed = this.reachSeed && otherRv.reachSeed;
		
		if (! this.reachSeed) {
			this.relevantVariables.clear();
		}
		else {
			this.relevantVariables.retainAll(otherRv.relevantVariables);
		}		
	}

	@Override
	public void intersection(FlowSet other, FlowSet dest) {
		// dest = this /\ other
		RVDomain otherRv = (RVDomain) other;
		RVDomain destRv = (RVDomain) dest;

		if (! this.reachSeed || !otherRv.reachSeed) {
			// Empty intersection
			dest.clear();
		}
		else {
			assert this.reachSeed && otherRv.reachSeed;
			destRv.clear();
			destRv.reachSeed = true;			
			destRv.relevantVariables.addAll(this.relevantVariables);
			destRv.relevantVariables.retainAll(otherRv.relevantVariables);
		}
	}

	/**
	 *  
	 * @param other
	 * @return true if the intersection of this with other is no-empty
	 */
	public boolean intersect(RVDomain other)
	{
		if ((! this.reachSeed) || (! other.reachSeed)) {
			return false;
		}
		else {
			/* dumb implementation if intersect */
			Set<Local> small;
			Set<Local> big;
			
			
			if (this.size() > other.size()) {
				big = this.relevantVariables;
				small = other.relevantVariables;
			}
			else {
				small = this.relevantVariables;
				big = other.relevantVariables;
			}
			
			for (Local l : small) {
				if (big.contains(l)) {
					/* non-empty intersection */
					return true;
				}
			}
			
			return false;
		}
	}
	
	@Override
	public Iterator<Local> iterator() {
		return this.relevantVariables.iterator();
	}

	@Override
	public void remove(Object arg0) {
		assert false;		
	}

	@Override
	public void remove(Object arg0, FlowSet arg1) {
		assert false;		
	}

	@Override
	public int size() {
		return this.relevantVariables.size();
	}

	@Override
	public List<?> toList() { 
		return null;
	}

	@Override
	public void union(FlowSet other) {
		RVDomain otherRv = (RVDomain) other;
		this.reachSeed = this.reachSeed || otherRv.reachSeed;
				
		if (this.reachSeed) {
			this.relevantVariables.addAll(otherRv.relevantVariables);
		}		
	}

	@Override
	public void union(FlowSet other, FlowSet dest) {
		// dest = this \/ other
		RVDomain otherRv = (RVDomain) other;
		RVDomain destRv = (RVDomain) dest;

		if (! this.reachSeed) {
			other.copy(dest);
		}
		else if (! otherRv.reachSeed) {
			this.copy(dest);
		}
		else {
			assert this.reachSeed && otherRv.reachSeed;
			destRv.clear();
			destRv.reachSeed = true;			
			destRv.relevantVariables.addAll(this.relevantVariables);
			destRv.relevantVariables.addAll(otherRv.relevantVariables);
		}		
	}

	@Override
	public boolean isSubSet(FlowSet other) {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		assert this.reachSeed || this.relevantVariables.size() == 0;
		
		if (! this.reachSeed) {
			return 0;
		}
		else {
			return this.relevantVariables.hashCode();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		RVDomain otherRV = (RVDomain) other;
		if (this.reachSeed != otherRV.reachSeed) {
			return false;
		}
		else {
			return this.relevantVariables.equals(otherRV.relevantVariables);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// TODO use a string buffer
		String str = "" + this.reachSeed;
		for (Local i : this.relevantVariables) str = str + " " + i.toString();

		return str;
	}

}
