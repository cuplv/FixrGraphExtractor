package edu.colorado.plv.fixr.simp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.colorado.plv.fixr.graphs.DataDependencyGraph;
import edu.colorado.plv.fixr.slicing.APISlicer;
import edu.colorado.plv.fixr.slicing.ReachingDefinitions;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
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
  private Logger logger = LoggerFactory.getLogger(APISlicer.class);  
      
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
   * NOTE: it only works for assignments between locals and constants
   */
  private void inlineEqualities(Body body) {
    logger.warn("Inlining equalities...");
    boolean fixPoint = false;
    
    ReachingDefinitions rd = this.ddg.getReachingDefinitions();
    Map<Unit, Set<Unit>> duChain = rd.getDefinedUnits();
    filterDuChain(duChain);
    
    /* get all the assignments */
    Set<Unit> assignments = new HashSet<Unit>();
    for (Unit u : body.getUnits()) 
      if (isUnitAssignment(u))
          assignments.add(u);              
    
    while (! fixPoint) {
      fixPoint = true;
      Set<Unit> newAssignments = new HashSet<Unit>();
      newAssignments.addAll(assignments);      
      
      for (Unit toSubstitute : assignments) {
        Set<Unit> useUnits = duChain.get(toSubstitute);                
        
        if (null == useUnits) {
          newAssignments.remove(toSubstitute);
        }
        else if (useUnits.size() > 1) {
          // Warning: relaxing this case may be tricky (due to interdependency
          // with other statements
          newAssignments.remove(toSubstitute);          
        } else if (useUnits.size() == 1) {
          Unit singleUse = useUnits.iterator().next();
          if (singleUse instanceof AssignStmt) {
            // we can perform the substitution
                                   
            // create a new assignment
            AssignStmt newAssign = soot.jimple.Jimple.v().newAssignStmt(
                ((AssignStmt) singleUse).getLeftOp(),
                ((AssignStmt) toSubstitute).getRightOp());
                                    
            body.getUnits().insertAfter(newAssign, singleUse);
            body.getUnits().remove(toSubstitute);
            body.getUnits().remove(singleUse);
            
            newAssignments.add(newAssign);
            newAssignments.remove(toSubstitute);
            newAssignments.remove(singleUse);
            
            // keep track of the uses of unit.
            Set<Unit> useSingleUse = duChain.get(singleUse);
            if (null != useSingleUse) {
              duChain.put(newAssign, useSingleUse);
            }
            duChain.put(toSubstitute, null);
            duChain.put(singleUse, null);
            // Not efficient - to improve
            updateDuChain(duChain, toSubstitute, newAssign, singleUse);
                                   
            fixPoint = false;
          }
        }
      }     
      assignments = newAssignments;
    }        
  }
  
  /**
   * 
   * @param u
   * @return true iff u is an assisngment that involves locals
   */
  private boolean isUnitAssignment(Unit u) {
    if (u instanceof AssignStmt) {
      Value lhs = ((AssignStmt) u).getLeftOp();
      return (lhs instanceof Local);
    }
    return false;
  }
  
  private void filterDuChain(Map<Unit, Set<Unit>> duChain) {
    Set<Unit> keyToRemove = new HashSet<Unit>();
    
    // Filter the DuChain on locals
    for (Map.Entry<Unit, Set<Unit>> entry : duChain.entrySet()) {
      Unit key = entry.getKey();
      
      if (isUnitAssignment(key)) {
        Set<Unit> newSet = new HashSet<Unit>();
        Set<Unit> oldSet = entry.getValue();
        for (Unit value : oldSet) {
          if (isUnitAssignment(value)) {
            Value rhs = ((AssignStmt) value).getRightOp();
            if (rhs instanceof Local)
              newSet.add(value);
          }
        }
        oldSet.clear();
        oldSet.addAll(newSet);
      }
      else {
        keyToRemove.add(key);
      }   
    }
  }
  
  private void updateDuChain(Map<Unit, Set<Unit>> duChain,
      Unit replacedUnit,
      Unit addedUnit,
      Unit removedUnit) {
    for (Map.Entry<Unit, Set<Unit>> entry : duChain.entrySet()) {
      Set<Unit> uses = entry.getValue();
      
      if (null != uses) {
        if (uses.contains(replacedUnit)) {
          uses.remove(replacedUnit);
          uses.add(addedUnit);          
        }
        uses.remove(removedUnit);
      }
    }
  }
}
