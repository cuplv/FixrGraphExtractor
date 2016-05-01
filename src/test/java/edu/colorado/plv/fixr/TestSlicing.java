package edu.colorado.plv.fixr;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;

import edu.colorado.plv.fixr.slicing.APISlicer;
import edu.colorado.plv.fixr.slicing.MethodPackageSeed;
import soot.Body;
import soot.SootClass;

public abstract class TestSlicing extends TestClassBase {

	public abstract Collection<String> getPackages();
	
	public void testSlice(String methodName) 
	{		
		SootClass testClass = this.getTestClass();
		SootClass resClass = this.getResClass();
		
		assertTrue(testClass != null);
		assertTrue(resClass!= null);
		
		Body bodyToSlice = testClass.getMethodByName(methodName).retrieveActiveBody();
		Body expectedRes = resClass.getMethodByName(APISlicer.getSlicedMethodName(methodName)).retrieveActiveBody();

		APISlicer slicer = new APISlicer(bodyToSlice);
		Body slicedBody = slicer.slice(new MethodPackageSeed(getPackages()));
		
//		// DEBUG
//		{
//			UnitGraph u1 = new BriefUnitGraph(bodyToSlice);
//			UnitGraph u2 = new BriefUnitGraph(slicedBody);
//			SootHelper.dumpToDot(u1, bodyToSlice, "/tmp/toslice.dot");
//			SootHelper.dumpToDot(u2, slicedBody, "/tmp/sliced.dot");			
//		}
		
		assertTrue(SootHelper.compareBodies(expectedRes, slicedBody));		
	}

}
