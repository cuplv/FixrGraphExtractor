package edu.colorado.plv.fixr.slicing;

import java.util.HashSet;
import java.util.Set;

import soot.Unit;

/**
 * Slice according to a set of statements in the graph
 *
 * @author Sergio Mover
 *
 */
public class SlicingStmts implements SlicingCriterion {
  private Set<Unit> stmts;

  public SlicingStmts(Set<Unit> stmts) {
    this.stmts = new HashSet<Unit>();
    this.stmts.addAll(stmts);
  }

  public SlicingStmts(Unit stmt) {
    this.stmts = new HashSet<Unit>();
    this.stmts.add(stmt);
  }

  @Override
  public boolean is_seed(Unit unit) {
    return this.stmts.contains(unit);
  }

  public String getCriterionDescription() {
    StringBuffer sBuffer = new StringBuffer("SlicingStmts criterion\n" +
                                            "The seed are all the statements\n");
    return sBuffer.toString();
  }
}
