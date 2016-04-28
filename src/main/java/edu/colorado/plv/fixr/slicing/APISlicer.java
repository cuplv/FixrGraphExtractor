package edu.colorado.plv.fixr.slicing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.colorado.plv.fixr.SootHelper;
import edu.colorado.plv.fixr.graphs.DataDependencyGraph;
import soot.Body;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.TableSwitchStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;
import soot.toolkits.graph.pdg.PDGNode;

/**
 * Slice a CFG of a method using as seeds the nodes of the API calls
 * 
 * Ad-hoc implementation of the slicer.
 * We have to make it general (e.g. an interface that is agnostic of the cfg 
 * type). We have to pass as input the filter used in the slicing (e.g. prefix 
 * of the package of method calls).
 * 
 * @author Sergio Mover
 *
 */
public class APISlicer {
	private UnitGraph cfg;
	private PDGSlicer pdg; 
	private DataDependencyGraph ddg;
	
	public APISlicer(UnitGraph cfg, Body body) {
		super();
		this.cfg = cfg;
		
		/* Computes the program dependency graph */
		this.pdg= new PDGSlicer((UnitGraph) this.cfg);
		this.ddg = new DataDependencyGraph(this.cfg);		
	}	 
 
	/**
	 * Computes the slice of this.cfg according to the android API method calls.
	 * @return
	 */	
	public Body slice(SlicingCriterion sc) {				
		SootHelper.dumpToDot(ddg, this.cfg.getBody(), "/tmp/ddg.dot");		
		SootHelper.dumpToDot(pdg, this.cfg.getBody(), "/tmp/pdg.dot");
				
		/* 2.1 Find the slice units */
		Set<Unit> seeds = getSeeds(sc);		
		
		/* 2.2 Get the CFG units that are relevant for the slice */
		Set<Unit> unitsInSlice = findReachableUnits(seeds);
				
		/* 3. Construct the sliced CFG */			
		Body slice = buildSlice(unitsInSlice);				
		
		return slice;
	}

	/**
	 * Finds all the seeds (units) in the PDG according to the slicing criterion
	 * 
	 * The method returns a pair (PDGNode, 
	 * 
	 * @return
	 */
	private Set<Unit> getSeeds(SlicingCriterion sc)
	{		
		Set<Unit> seeds = new HashSet<Unit>();
		for (Object node : pdg.getNodes()) {
			if (node instanceof PDGNode) {
				Object innerNode = ((PDGNode) node).getNode();
				if (innerNode instanceof Block) {
					Block b = (Block) innerNode;
					for (Iterator<Unit> iter = b.iterator(); iter.hasNext();) {
						Unit u = iter.next();
						if (sc.is_seed(u)) seeds.add(u);
					}
				}
			}
		}
		
		return seeds;
	}
	
	/**
	 * Finds all the units that are <b>backward</b> reachable from seeds (i.e. seeds are the 
	 * initial states) from the combined representation of the PDG and the DDG.     
	 * 
	 * @return the backward reachable nodes in PDG and DDG from seeds
	 */
	private Set<Unit> findReachableUnits(Set<Unit> seeds) 
	{		
		Set<Unit> visitedNodes = new HashSet<Unit>();
		Set<PDGNode> visitedPdgNodes = new HashSet<PDGNode>();
		Stack<Unit> unitsWorkList = new Stack<Unit>();
		Stack<PDGNode> pdgNodesWorkList = new Stack<PDGNode>();
		
		for (Unit u : seeds) unitsWorkList.push(u);		

		while (! (unitsWorkList.isEmpty() && pdgNodesWorkList.isEmpty()))
		{		
			while (! pdgNodesWorkList.isEmpty())
			{
				/* push the predecessor in pdgNodesWorkList */
				PDGNode current = pdgNodesWorkList.pop();
				if (visitedPdgNodes.contains(current)) continue;

				visitedPdgNodes.add(current);
				
				/* Visit the block of the PDG node. 
				 * If the unit is a conditional or a label, add it to the units work 
				 * lists. 
				 */
				Object pdgObject = current.getNode();		
				if (pdgObject instanceof Block) {
					assert pdgObject instanceof Block; 
					for (Iterator<Unit> iter = ((Block) pdgObject).iterator(); iter.hasNext();) {
						Unit unit = iter.next();
						if (isControlFlow(unit)) {
							if (! visitedNodes.contains(unit)) {
								unitsWorkList.push(unit);
							}
						}
					}
				}

				/* Add the backward reachable PDG nodes */
				for (Object pdgObj : pdg.getPredsOf((current))) {
					assert pdgObj instanceof PDGNode;
					PDGNode pred = (PDGNode) pdgObj;				 
					if (! visitedPdgNodes.contains(pred)) pdgNodesWorkList.push(pred);
				}
			}
			
			while (! unitsWorkList.isEmpty()) {
				Unit current = unitsWorkList.pop();
				if (visitedNodes.contains(current)) continue;
				
				visitedNodes.add(current);
				
				/* get the list of predecessors in the PDG and add them to the worklist */
				PDGNode pdgNode = pdg.getPDGNodeFromUnit(current);
				
				Object pdgObject = pdgNode.getNode(); 
				if (pdgObject instanceof Block) {
					assert pdgObject instanceof Block; 
					for (Iterator<Unit> iter = ((Block) pdgObject).iterator(); iter.hasNext();) {
						Unit unit = iter.next();
						if (unit instanceof GotoStmt) {
							// Add the goto statement to the slice
							visitedNodes.add(unit);
						}
					}
				}
				
				for (Object predObj : pdg.getPredsOf(pdgNode)) {
					assert predObj instanceof PDGNode;
					PDGNode pred = (PDGNode) predObj;
					if (! visitedPdgNodes.contains(pred)) pdgNodesWorkList.push(pred);			
				}
								
				/* get the list of predecessor in the DDG */
				for (Unit u : ddg.getPredsOf(current)) {
					if (! visitedNodes.contains(u)) {
						unitsWorkList.push(u);
					}
				}
			}						
		}

		return visitedNodes;
	}	 
	
	/**
	 * Return a slice (as a block graph) of the cfg that just contains 
	 * the CFG nodes that have a non-empty set of relevant variables
	 *  
	 * TODO probably this has to be implemented in the SlicedEnhancedBlockGraph
	 *  
	 * @return a sliced block graph
	 */
	private Body buildSlice(Set<Unit> unitsInSlice) {
		Body srcBody = this.cfg.getBody();
		SootMethod srcMethod = srcBody.getMethod();
		SootClass srcClass = srcMethod.getDeclaringClass();
		assert (null != srcMethod);
		String methodName = srcMethod.getName();
		methodName = methodName + "__sliced__";
		
		SootMethod dstMethod = new SootMethod(methodName,
				srcMethod.getParameterTypes(), srcMethod.getReturnType(),
				srcMethod.getModifiers());
		JimpleBody dstBody = Jimple.v().newBody(dstMethod);
		dstMethod.setActiveBody(dstBody);
		srcClass.addMethod(dstMethod);

		Map<Object,Object> src2dst = dstBody.importBodyContentsFrom(srcBody);
				
		PatchingChain<Unit> srcPc = srcBody.getUnits();
		PatchingChain<Unit> dstPc = dstBody.getUnits();
		
		
		for (Unit srcUnit : srcPc) {	
			if (! unitsInSlice.contains(srcUnit)) {
				Unit dstUnit = (Unit) src2dst.get(srcUnit);
				assert null != dstUnit;
				dstPc.remove(dstUnit);
			}
		}

		return dstBody;		
	}			
	
	private boolean isControlFlow(Unit u) {
		return (u instanceof IfStmt ||
					u instanceof GotoStmt ||
					u instanceof TableSwitchStmt ||
					u instanceof LookupSwitchStmt);
	}
}
