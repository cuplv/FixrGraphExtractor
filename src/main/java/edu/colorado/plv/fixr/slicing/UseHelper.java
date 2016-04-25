package edu.colorado.plv.fixr.slicing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;

/**
 * Helper class used to memoize the set of uses/defines for units
 * 
 * @author Sergio Mover
 *
 */
public class UseHelper {
	private static UseHelper helper = new UseHelper();
	
	/* definitions of node */
	private Map<Unit, RVDomain> defs;
	/* uses of nodes */
	private Map<Unit, RVDomain> uses;
	
	private UseHelper() {
 		this.defs = new HashMap<Unit, RVDomain>();
		this.uses = new HashMap<Unit, RVDomain>();
	}

	public static UseHelper getHelper()
	{
		return helper;
	}
	
	public RVDomain get_defs(Unit unit) {
		RVDomain unit_defs = this.defs.get(unit);
		if (null == unit_defs) {
			/* compute the definition for the unit */
			unit_defs = filterToLocal(unit.getDefBoxes()); 
			this.defs.put(unit, unit_defs);
		}
		return unit_defs;
	}

	public RVDomain get_uses(Unit unit) {
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
