/**
 * 
 */
package edu.colorado.plv.fixr.slicing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.colorado.plv.fixr.SootHelper;
import soot.Unit;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.DominatorNode;
import soot.toolkits.graph.DominatorTree;
import soot.toolkits.graph.DominatorTreeAdapter;
import soot.toolkits.graph.MHGDominatorsFinder;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.ProgramDependenceGraph;

/**
 * Analysis used to computes the statements of a cfg contained in a slice.
 *  
 * @author Sergio Mover
 *
 */
public class SliceStmtAnalysis {
	private DirectedGraph<Unit> graph;
	private SlicingCriterion sc;
			
	private Map<Unit,Unit> closerControlPoint;
	
	private Set<Unit> stmts;
	private Set<Unit> branches;
	private Map<Unit, RVDomain> rel_var;
	private UseHelper helper;
	
	/* Direct dependencies for branches */
	private Map<Unit, RelevantVariablesAnalysis> rel_var_branch;
	
	public SliceStmtAnalysis(DirectedGraph<Unit> graph, SlicingCriterion criterion) {
		this.graph = graph;		
		this.sc = criterion;
		
		buildControlPoint();
		this.stmts = new HashSet<Unit>();
		this.branches = new HashSet<Unit>();
		this.rel_var = new HashMap<Unit,RVDomain>();
		this.rel_var_branch = new HashMap<Unit,RelevantVariablesAnalysis>();
		this.helper = UseHelper.getHelper();
		
		/* Initial value for sets */
  	RelevantVariablesAnalysis rv = new RelevantVariablesAnalysis(graph, this.sc);
		for (Unit u : this.graph) {
			RVDomain dom = rv.getFlowBefore(u);
			rel_var.put(u, dom);
		}  	
		for (Unit u : rv.getRelevantStmt()) {
			this.stmts.add(u);			
		}
		addBranches(rv.getRelevantStmt());
		
		fixPoint(this.branches);
	}

	private void buildControlPoint()
	{
		DominatorTree dominatorTree;		
		
		dominatorTree = new DominatorTree(new MHGDominatorsFinder<Unit>(this.graph));

//		// DEBUG
//		ProgramDependenceGraph pdg = new HashMutablePDG((UnitGraph) this.graph);
//		SootHelper.dumpToDot(pdg, ((UnitGraph) this.graph).getBody(), "/home/sergio/pdf.dot");
//		System.out.println(pdg.toString());
//		
//		// DEBUG
//		DominatorTreeAdapter da = new DominatorTreeAdapter(dominatorTree);  	
//		SootHelper.dumpToDot(da, ((UnitGraph) this.graph).getBody() , "/home/sergio/test.dot");		
		
		/* visits the tree and build the control relationship */
		this.closerControlPoint = new HashMap<Unit, Unit>(); 
		buildControlPointRec(dominatorTree.getHead(), null);		
	}
	
	private void buildControlPointRec(DominatorNode node, Unit head) 
	{		
		Unit unitNode = (Unit) node.getGode();
		this.closerControlPoint.put(unitNode, head);
		
		List<?> children = node.getChildren();		
		if (children.size() > 1) head = unitNode;		
		for (Object childObj : children) {
			DominatorNode child = (DominatorNode) childObj;
			buildControlPointRec(child, head);
		}
	}
	
	private void addBranches(Collection<Unit> in_slice) 
	{
		for (Unit u : in_slice) {
			Unit newB = this.closerControlPoint.get(u);
			if (newB != null) this.branches.add(newB);
		}
	}
	
	/**
	 * Computes the fixed point of the data equations 
	 */
	private void fixPoint(Set<Unit> newBranches)
	{				
		/* Compute R^{k+1} for all the new nodes. */	
		Set<Unit> changed_r = new HashSet<Unit>();				
		for (Unit u : this.graph) {
			RVDomain var_u = this.rel_var.get(u);
			int old_size = var_u.size();
			
			for (Unit branch : newBranches) {
				RelevantVariablesAnalysis rv = this.rel_var_branch.get(branch);			
				if (null == rv) {
					rv = new RelevantVariablesAnalysis(graph, new SlicingStmts(branch));
					this.rel_var_branch.put(branch, rv);
				}
				var_u.union(rv.getFlowBefore(branch));	
			}
			
			if (old_size != var_u.size()) {
				changed_r.add(u);								
			}
		}

		/* Compute S^{k+1} */
		boolean same_stmt;
		same_stmt = true;
		{
			int old_size = this.stmts.size();
			
			this.stmts.addAll(newBranches);
						
			/* get the DEF of all the predecessors */
			for (Unit changed : changed_r) {
				for (Unit pred : this.graph.getPredsOf(changed)) {
					RVDomain pred_use = this.helper.getRVDomainDefs(pred);

					if (pred_use.intersect(this.rel_var.get(changed))) {
						this.stmts.add(pred);
					}
				}
			}
			
			
			same_stmt = same_stmt && old_size == this.stmts.size();			
		}

		// TODO Improve efficiency, now it is a mess
		/* Update B_c^k */
		if (! same_stmt) {
			Set<Unit> addedBranches = new HashSet<Unit>();
			
			this.addBranches(this.stmts);			

			addedBranches.addAll(this.branches);
			addedBranches.removeAll(newBranches);

			fixPoint(addedBranches);
		}		
	}

	/**
	 * @return the stmts
	 */
	public Set<Unit> getStmts() {
		return stmts;
	}

	public boolean isInSlice(Unit u) {
		if (this.stmts.contains(u)) return true;
		if (this.branches.contains(u)) return true;
		
		return false;
	}

	
	
}
