package edu.colorado.plv.fixr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import edu.colorado.plv.fixr.slicing.APISlicer;
import edu.colorado.plv.fixr.slicing.MethodPackageSeed;
import soot.Body;
import soot.Scene;
import soot.SootClass;

public class SlicingTest2 {
	static final String CLASS_NAME = "slice.TestControlFlow";
	static final String RES_CLASS_NAME = "slice.TestControlFlowRes";	
	
	private SootClass testClass;
	private SootClass testClassRes;	
	
	
	@Before
	public void setup()
	{
		SootHelper.reset();			
		SootHelper.configure("src/test/resources/jimple", true);
		
		/* Load dependencies */
		Scene.v().addBasicClass(CLASS_NAME, SootClass.HIERARCHY);
		Scene.v().addBasicClass(RES_CLASS_NAME, SootClass.HIERARCHY);		
		Scene.v().loadNecessaryClasses();		
		
		testClass = Scene.v().tryLoadClass(CLASS_NAME, SootClass.BODIES);		
		testClassRes = Scene.v().tryLoadClass(RES_CLASS_NAME, SootClass.BODIES);
	}
	
	public boolean compareBodies(Body b1, Body b2) {
		String reprB1 = b1.toString();
		String reprB2 = b2.toString();		
		
		return 0 == reprB1.compareToIgnoreCase(reprB2);
	}
	
	public void testSlice(String methodName) 
	{		
		assertTrue(testClass != null);
		//assertTrue(testClassRes != null);
		Body bodyToSlice = testClass.getMethodByName(methodName).retrieveActiveBody();
		Body expectedRes = testClassRes.getMethodByName(APISlicer.getSlicedMethodName(methodName)).retrieveActiveBody();

		APISlicer slicer = new APISlicer(bodyToSlice);
		Body slicedBody = slicer.slice(new MethodPackageSeed("java.lang.Math"));
		
		assertTrue(compareBodies(expectedRes, slicedBody));		
	}
		
	@Test
	public void t1(){testSlice("testSequence01");}
	
}
