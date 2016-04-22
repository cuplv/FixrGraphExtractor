package edu.colorado.plv.fixr.slicing;

import soot.Unit;

/**
 * Defines a slicing criterion: when a unit of a CFG must be a seed of a slice. 
 * 
 * @author Sergio Mover
 *
 */
public interface SlicingCriterion {
	
	/**
	 * Returns true if the instruction represented by unit should be a seed for the slicing
	 * 
	 * @param unit The instruction currently analyzed
	 * @return True if unit is a seed
	 */
	public Boolean is_seed(Unit unit);
	
}
