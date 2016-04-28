package edu.colorado.plv.fixr.graphs;

import java.util.Iterator;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.util.dot.DotGraph;
import soot.util.dot.DotGraphConstants;

public class CDFGToDotGraph extends CFGToDotGraph {

	public CDFGToDotGraph() {

	}

	public DotGraph drawCFG(UnitCdfgGraph graph, Body body) {
		DotGraph canvas = initDotGraph(body);
		DotNamer namer = new DotNamer((int)(graph.size()/0.7f), 0.7f);
		NodeComparator comparator = new NodeComparator(namer);

		// To facilitate comparisons between different graphs of the same
		// method, prelabel the nodes in the order they appear
		// in the iterator, rather than the order that they appear in the
		// graph traversal (so that corresponding nodes are more likely
		// to have the same label in different graphs of a given method).
		for (Iterator nodesIt = graph.iterator(); nodesIt.hasNext(); ) {
			String junk = namer.getName(nodesIt.next());
		}
		
		for (Iterator<Local> iterLocals = graph.localsIter();
				iterLocals.hasNext(); ) {
			Local var = iterLocals.next();			
			namer.getName(var);
			canvas.drawNode(namer.getName(var));
		}

		for (Iterator nodesIt = graph.iterator(); nodesIt.hasNext(); ) {
			Object node = nodesIt.next();
			canvas.drawNode(namer.getName(node));
			
			for (Iterator succsIt = sortedIterator(graph.getSuccsOf((Unit) node), comparator);
					succsIt.hasNext(); ) {
				Object succ = succsIt.next();
				canvas.drawEdge(namer.getName(node), namer.getName(succ));
			}
			
			for (Local succ : graph.getDefVars((Unit) node)) {
				canvas.drawEdge(namer.getName(succ), namer.getName(node));
			}			
		}

		for (Iterator<Local> iterLocals = graph.localsIter();
				iterLocals.hasNext(); ) {
			Local v = iterLocals.next();
			for (Unit u : graph.getUseUnits(v)) {
				canvas.drawEdge(namer.getName(u), namer.getName(v));				
			}
		}

		
		setStyle(graph.getHeads(), canvas, namer,
				DotGraphConstants.NODE_STYLE_FILLED, headAttr);
		setStyle(graph.getTails(), canvas, namer, 
				DotGraphConstants.NODE_STYLE_FILLED, tailAttr);
		if (! isBrief) {
			formatNodeText(body, canvas, namer);
		}

		return canvas;		
	}



}
