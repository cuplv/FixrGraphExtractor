package edu.colorado.plv.fixr.slicing;

import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.internal.InvokeExprBox;
import soot.jimple.internal.RValueBox;

public class AndroidMethodsCriterion implements SlicingCriterion {

	public AndroidMethodsCriterion() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Boolean is_seed(Unit unit) {
		
		
		for (ValueBox valBox : unit.getUseBoxes()) {
			// DEBUG
			// System.out.println(valBox + " : " + valBox.getClass());
			
			Value v = valBox.getValue();			
			if (v instanceof InvokeExpr) {
				String declaringClassName = ((InvokeExpr) v).getMethod().getDeclaringClass().getName();
				if (declaringClassName.startsWith("android.")) {
					return true;
				}
				
				// DEBUG
//				System.out.println("---\nv= " + v);
//				System.out.println("Method name: " + ((InvokeExpr) v).getMethod().getName());
//				System.out.println("Decl class name: " + ((InvokeExpr) v).getMethod().getDeclaringClass().getName());
//				
//				((InvokeExpr) v).get
//				
//				if (v instanceof SpecialInvokeExpr) {
//					System.out.println("Special invoke...");
//				}
//				
//				System.out.print("Parameters:");			
//				for (Value valArgs : ((InvokeExpr) v).getArgs()) {
//					System.out.print(", " + valArgs);
//				}
//				System.out.println("\n---");				
			}			 
		}
		
		return false;
	}

}
