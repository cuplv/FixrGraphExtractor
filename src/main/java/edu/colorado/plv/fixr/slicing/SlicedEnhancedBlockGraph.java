package edu.colorado.plv.fixr.slicing;

import soot.Body;
import soot.toolkits.graph.pdg.EnhancedBlockGraph;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;

/**
 * Block graph built by slicing an EnhancedBlockGraph 
 * 
 * @author Sergio Mover
 *
 */
public class SlicedEnhancedBlockGraph extends EnhancedBlockGraph {

	public SlicedEnhancedBlockGraph(Body body) {
		super(body);
		// TODO Auto-generated constructor stub
	}

	public SlicedEnhancedBlockGraph(EnhancedUnitGraph unitGraph) {
		super(unitGraph);
		// TODO Auto-generated constructor stub
	}

}
