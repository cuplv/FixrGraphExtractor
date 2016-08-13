package edu.colorado.plv.fixr.slicing;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;

public class MethodPackageSeed implements SlicingCriterion {
	private List<String> packagePrefixes;
	
	public MethodPackageSeed(Collection<String> packages) {
		//TODO extend package prefix to a collection of regexp, 
		this.packagePrefixes = new LinkedList<String>(packages); 
	}

	public MethodPackageSeed(String packagePrefix) {			
		Collection<String> packageList = new LinkedList<String>();
		packageList.add(packagePrefix);
		
		this.packagePrefixes = new LinkedList<String>(packageList); 
	}
	
	public static MethodPackageSeed createAndroidSeed() {
		Collection<String> packageList = new LinkedList<String>();
		packageList.add("android.");
		MethodPackageSeed s = new MethodPackageSeed(packageList);
		return s;
	}
	
	@Override
	public Boolean is_seed(Unit unit) {
		for (ValueBox valBox : unit.getUseBoxes()) {
			// DEBUG
			// System.out.println(valBox + " : " + valBox.getClass());

			Value v = valBox.getValue();			
			if (v instanceof InvokeExpr) {
				String declaringClassName = ((InvokeExpr) v).getMethod().getDeclaringClass().getName();
				
				for (String prefix : this.packagePrefixes) {
					if (declaringClassName.startsWith(prefix)) {
						return true;
					}
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
	
	public String getCriterionDescription() {
		StringBuffer sBuffer = new StringBuffer("MethodPacakgeSeed criterion\n" +
				"The seed are all the method invocation that belongs to the " +
				"following packages:\n");
		for (String packageName : this.packagePrefixes) {
			sBuffer.append(packageName + "\n");
		}
    return sBuffer.toString();
	}
	
}
