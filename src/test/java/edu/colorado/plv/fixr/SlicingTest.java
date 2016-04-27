package edu.colorado.plv.fixr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import edu.colorado.plv.fixr.slicing.APISlicer;
import edu.colorado.plv.fixr.slicing.MethodPackageSeed;
import edu.colorado.plv.fixr.slicing.RVDomain;
import edu.colorado.plv.fixr.slicing.RelevantVariablesAnalysis;
import soot.Body;
import soot.IntType;
import soot.Local;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.Unit;
import soot.jimple.Jimple;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;

public class SlicingTest {
	static final String CLASS_NAME = "slice.TestSlice";	
	
	@Before
	public void setup()
	{
		SootHelper.reset();					
		
		SootHelper.configure("src/test/resources/jimple", true);
	}

	private Body getBody(String className, String methodName)
	{
		Scene.v().addBasicClass(className, SootClass.HIERARCHY);
  	/* load dependencies */
  	Scene.v().loadNecessaryClasses();

  	/* get the method body (in jimple) */  	
  	Body body = SootHelper.getMethodBody(className, methodName);  	
  	return body;
	}			 

	private void assertVarAtUnit(RelevantVariablesAnalysis rv, Unit unit,
				Local[] vars_in, Local[] vars_out)
	{  	  	
  		RVDomain dom = rv.getFlowBefore(unit);
  		if (null != vars_in) {
  			for (Local v : vars_in) {
  				assertTrue(dom.hasVar(v));
  			}
  		}
  		if (null != vars_out) {
  			for (Local v : vars_out) {
  				assertFalse(dom.hasVar(v));
  			}
  		}
	}
			
	private void assertVarAtBody(RelevantVariablesAnalysis rv, Body body,
				Local[][] vars_in, Local[][] vars_out) 
	{
		PatchingChain<Unit> units_in_body = body.getUnits();
		
		int i = 0;
		for (Unit u : units_in_body) {
			Local[] vi = null;
			Local[] vo = null;
			if (null != vars_in) vi = vars_in[i];
			if (null != vars_out) vo = vars_out[i];			
			assertVarAtUnit(rv, u, vi, vo);
			i += 1;
		}
	}		
	
	private void testRV(String className, String methodName,
			Local[][] vars_in, Local[][] vars_out)
	{
		Body body = getBody(CLASS_NAME, methodName);  	  	  	
  	RelevantVariablesAnalysis rv = new RelevantVariablesAnalysis(new EnhancedUnitGraph(body),
  				new MethodPackageSeed("java.lang.Math"));

//  	// DEBUG
//  	printGraph(body);
//    EnhancedUnitGraph jimpleUnitGraph= new EnhancedUnitGraph(body);
//    SootHelper.dumpToDot(jimpleUnitGraph, body, "test_graph.dot");
//  	System.out.println(body.getUnits());
//  	System.out.println(rv.toString());
//  	
//  	// DEBUG
//  	DominatorTreeAdapter dominatorTree = new DominatorTreeAdapter(new DominatorTree(new MHGDominatorsFinder<Unit>(new EnhancedUnitGraph(body))));  	
//  	SootHelper.dumpToDot(dominatorTree, body, "dom_tree.dot");
  	
  	assertVarAtBody(rv, body, vars_in, vars_out);				 
	}

	private void testSlice(String className, String methodName)
	{
		Body body = getBody(CLASS_NAME, methodName);
  	EnhancedUnitGraph jimpleUnitGraph= new EnhancedUnitGraph(body);
  	SootHelper.dumpToDot(jimpleUnitGraph, jimpleUnitGraph.getBody(), methodName + ".dot");
  	
  	APISlicer slicer = new APISlicer(jimpleUnitGraph, body);
  	UnitGraph g = slicer.slice(new MethodPackageSeed("java.lang.Math"));  	  	
  	//SootHelper.dumpToDot(g, g.getBody(), methodName + "_sliced.dot");
	}
	
	@Test
	public void rvTest()
	{
		Local var_a = Jimple.v().newLocal("a", IntType.v());

		Local[][] in_vars = {{},{},{var_a},{}};
		Local[][] out_vars = {{var_a},{var_a},{},{var_a}};		

		testRV(CLASS_NAME, "m1", in_vars, out_vars);			  	  			
	}
	
	@Test
	public void rvTest2()
	{
		Local a = Jimple.v().newLocal("a", IntType.v());
		Local b = Jimple.v().newLocal("b", IntType.v());
		Local temp0 = Jimple.v().newLocal("temp$0", IntType.v());
		Local temp1 = Jimple.v().newLocal("temp$1", IntType.v());		

		Local[] all_vars = {a,b,temp0,temp1};
		Local[][] in_vars = {{}, {}, {temp0}, {a}, {a}, {a}, {}};		
		Local[][] out_vars = {all_vars, all_vars, {a,b,temp1}, {b,temp0,temp1}, {b,temp0,temp1}, {b,temp0,temp1}, all_vars};
		
		testRV(CLASS_NAME, "m2", in_vars, out_vars);		  			
	}

	@Test
	public void rvTest3()
	{
		Local a = Jimple.v().newLocal("a", IntType.v());
		Local b = Jimple.v().newLocal("b", IntType.v());
		Local temp0 = Jimple.v().newLocal("temp$0", IntType.v());
		Local temp1 = Jimple.v().newLocal("temp$1", IntType.v());		

		Local[] all_vars = {a,b,temp0,temp1};
		Local[][] in_vars = {{}, {}, {}, {},
				{temp1}, {b}, {a}, {}};		
		Local[][] out_vars = {all_vars,all_vars,all_vars,all_vars,
				{a,b,temp0},{a,temp0,temp1},{b,temp0,temp1},all_vars};
		
		testRV(CLASS_NAME, "m3", in_vars, out_vars);	
	}
	
	@Test
	public void rvTest4()
	{
		Local a = Jimple.v().newLocal("a", IntType.v());
		Local b = Jimple.v().newLocal("b", IntType.v());				
		Local temp0 = Jimple.v().newLocal("temp$0", IntType.v());
		Local temp1 = Jimple.v().newLocal("temp$1", IntType.v());
		Local temp2 = Jimple.v().newLocal("temp$2", IntType.v());
		Local temp3 = Jimple.v().newLocal("temp$3", IntType.v());
		Local temp4 = Jimple.v().newLocal("temp$4", IntType.v());
		Local temp5 = Jimple.v().newLocal("temp$5", IntType.v());

		Local[] all_vars = {a,b,temp0,temp1,temp2,temp3,temp4,temp5};
		Local[] no_b = {a,temp0,temp1,temp2,temp3,temp4,temp5};
		Local[] no_a = {b,temp0,temp1,temp2,temp3,temp4,temp5};		
		Local[][] in_vars = {{},{},{},{},{},
				{},{b},{b},{b},{b},
				{b},{b},{b},{b},{b},
				{b},{b},{b},{a},{}};
		Local[][] out_vars = {all_vars,all_vars,all_vars,all_vars,{a,b,temp0,temp2,temp3,temp4,temp5},
				no_b,no_b,no_b,no_b,no_b,
				no_b,no_b,no_b,no_b,no_b,
				no_b,no_b,no_b,no_a,{}};
		
		testRV(CLASS_NAME, "m4", in_vars, out_vars);	
	}	

	@Test
	public void rvTest5()
	{
		Local a = Jimple.v().newLocal("a", IntType.v());
		Local b = Jimple.v().newLocal("b", IntType.v());			
		Local temp0 = Jimple.v().newLocal("temp$0", IntType.v());
		Local temp1 = Jimple.v().newLocal("temp$1", IntType.v());
		Local temp2 = Jimple.v().newLocal("temp$2", IntType.v());
		Local temp3 = Jimple.v().newLocal("temp$3", IntType.v());
		Local temp4 = Jimple.v().newLocal("temp$4", IntType.v());
		Local temp5 = Jimple.v().newLocal("temp$5", IntType.v());

		Local[] all_vars = {a,b,temp0,temp1,temp2,temp3,temp4,temp5};
		Local[] no_b = {a,temp0,temp1,temp2,temp3,temp4,temp5};
		Local[] no_a = {b,temp0,temp1,temp2,temp3,temp4,temp5};		
		Local[][] in_vars = {{},{},{},{},{},
				{},{b},{b},{b},{b},
				{b},{b},{b},{b},{b},
				{b},{b},{b},{a},{}};
		Local[][] out_vars = {all_vars,all_vars,all_vars,all_vars,{a,b,temp0,temp2,temp3,temp4,temp5},
				no_b,no_b,no_b,no_b,no_b,
				no_b,no_b,no_b,no_b,no_b,
				no_b,no_b,no_b,no_a,{}};
		
		testRV(CLASS_NAME, "m4", in_vars, out_vars);	
	}

	@Test
	public void rvTest6()
	{
		Local a = Jimple.v().newLocal("a", IntType.v());
		Local b = Jimple.v().newLocal("b", IntType.v());
		Local c = Jimple.v().newLocal("c", IntType.v());		
		Local temp0 = Jimple.v().newLocal("temp$0", IntType.v());
		Local temp1 = Jimple.v().newLocal("temp$1", IntType.v());
		Local temp2 = Jimple.v().newLocal("temp$2", IntType.v());
		Local temp3 = Jimple.v().newLocal("temp$3", IntType.v());
		Local temp4 = Jimple.v().newLocal("temp$4", IntType.v());
		Local temp5 = Jimple.v().newLocal("temp$5", IntType.v());

		Local[] all_vars = {a,b,c,temp0,temp1,temp2,temp3,temp4,temp5};
		Local[] no_b = {a,c,temp0,temp1,temp2,temp3,temp4,temp5};
		Local[] no_a = {b,c,temp0,temp1,temp2,temp3,temp4,temp5};
		Local[] no_a_b = {c,temp0,temp1,temp2,temp3,temp4,temp5};		
		Local[][] in_vars = {{},{},{temp0},{a},{a,temp1},
				{a,b},{a,b},{a,b},{a},{b},
				{b},{b},{b},{b},{a},
				{a},{b},{b},{a},{}};
		Local[][] out_vars = {all_vars, all_vars, {a,b,c,temp1,temp2,temp3,temp4,temp5},no_a, {b,c,temp0,temp2,temp3,temp4,temp5},
				no_a_b,no_a_b,no_a_b,no_a,no_b,
				no_b,no_b,no_b,no_b,no_a,
				no_a,no_b,no_b,no_a,all_vars};
		
		testRV(CLASS_NAME, "m5", in_vars, out_vars);	
	}

	@Test
	public void sliceT1()	
	{
		testSlice(CLASS_NAME,"m1");		
	}

	@Test
	public void sliceT2()	
	{
		testSlice(CLASS_NAME,"m2");		
	}

	@Test
	public void sliceT3()	
	{
		testSlice(CLASS_NAME,"m3");		
	}

	@Test
	public void sliceT4()	
	{
		testSlice(CLASS_NAME,"m4");		
	}

	@Test
	public void sliceT6()	
	{
		testSlice(CLASS_NAME,"m6");		
	}
	
	@Test
	public void sliceT7()	
	{
		testSlice(CLASS_NAME,"m7");		
	}
	
	@Test
	public void sliceT8()	
	{
		testSlice(CLASS_NAME,"m8");		
	}			
}
