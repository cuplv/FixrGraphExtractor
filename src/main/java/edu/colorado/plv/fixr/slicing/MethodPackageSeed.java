package edu.colorado.plv.fixr.slicing;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import soot.SootClass;
import soot.SootMethod;
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
              InvokeExpr expr = (InvokeExpr) v;
              SootMethod method = expr.getMethod();
              SootClass sootClass = method.getDeclaringClass();
              String declaringClassName = sootClass.getName();

                for (String prefix : this.packagePrefixes) {
                    if (declaringClassName.startsWith(prefix)) {
                        return true;
                    }
                }
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
