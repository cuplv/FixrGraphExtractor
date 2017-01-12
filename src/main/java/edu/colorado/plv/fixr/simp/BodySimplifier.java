package edu.colorado.plv.fixr.simp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.colorado.plv.fixr.graphs.DataDependencyGraph;
import edu.colorado.plv.fixr.slicing.ReachingDefinitions;
import soot.Body;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.UnitGraph;

/**
 * Simplifies a Soot body of a method.
 * 
 * @author Sergio Mover
 *
 */
public class BodySimplifier {
  private Body body;
  protected DataDependencyGraph ddg = null;
  
  public BodySimplifier(UnitGraph graph) {    
    this.body = graph.getBody();
    this.ddg = new DataDependencyGraph(graph);
  }
  
  public Body getSimplifiedBody() {
    // inline the equalities
    inlineEqualities(this.body);
    
    return (Body) body;
  }
  /**
   * Removes the intermediate statements created by Jimple that just assign 
   * the result of some computation to some intermediate variable that is only 
   * used in a further assignment afterwards. 
   * 
   * The example of code is the following:
   * x = m()
   * y = x
   * 
   * We just want the code: 
   * y = m()
   * and ignore x.
   * 
   * The substitution can be done if the variable x assigned by x = m() is not 
   * used in other places.
   * 
   * This is not a general inlining and is used to get rid of this patological 
   * case.
   * 
   * The function proceeds as follows:
   *   - It collects all the assignment units
   *   - Until there are no new changes (Fixed point):
   *     - for all the assignments a in the assignment set:
   *       - pick an assignment to a variable (e.g. x = m())
   *       - It finds all the units that USE this definition (e.g. y = x)
   *       - If we have only a single unit that is an assignment, we perform the 
   *         substitution (x = m() becomes y = m(), and we remove y = x)
   * 
   */
  private void inlineEqualities(Body body) {
    boolean fixPoint = false;
    
    ReachingDefinitions rd = this.ddg.getReachingDefinitions();
    Map<Unit, Set<Unit>> duChain = rd.getDefinedUnits();
    
    /* get all the assignments */
    Set<Unit> assignments = new HashSet<Unit>();
    for (Unit u : body.getUnits()) 
      if (u instanceof AssignStmt) assignments.add(u);
    
    while (! fixPoint) {
      fixPoint = true;
      List<Unit> toIgnore= new ArrayList<Unit>();
      List<Unit> toAdd= new ArrayList<Unit>();
      
      for (Unit u : assignments) {
        Set<Unit> useUnits = duChain.get(u);                
        
        if (null == useUnits) {
          toIgnore.add(u);
        }
        else if (useUnits.size() > 1) {
          toIgnore.add(u);
        } else if (useUnits.size() == 1) {
          Unit singleUse = useUnits.iterator().next();
          if (singleUse instanceof AssignStmt) {
            // we can perform the substitution
            
            // create a new assignment
            AssignStmt newAssign = soot.jimple.Jimple.v().newAssignStmt(
                ((AssignStmt) singleUse).getLeftOp(),
                ((AssignStmt) u).getRightOp());
            
            System.out.println("U: " + u.toString());
            System.out.println("SingleUse: " + singleUse.toString());
            System.out.println("NewAssing: " + newAssign.toString());
                        
            body.getUnits().insertAfter(newAssign, u);
            body.getUnits().remove(u);
            body.getUnits().remove(singleUse);
            
            toAdd.add(newAssign);
            
            // keep track of the uses of unit.
            Set<Unit> useSingleUse = duChain.get(singleUse);
            if (null != useSingleUse) {
              duChain.put(newAssign, useSingleUse);
            }
            duChain.put(u, null);
            duChain.put(singleUse, null);
            
            toIgnore.add(u);
            fixPoint = false;
          }
        }
      }
      
      assignments.removeAll(toIgnore);
      assignments.addAll(toAdd);      
    }        
  }
}
