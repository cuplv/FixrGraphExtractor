package edu.colorado.plv.fixr;

import org.junit.Test;

import edu.colorado.plv.fixr.slicing.MethodPackageSeed;
import edu.colorado.plv.fixr.slicing.RelevantVariablesAnalysis;
import edu.colorado.plv.fixr.slicing.SlicingCriterion;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;

import static org.junit.Assert.*;

import org.junit.Before;

public class DummyTest {
	static final String CLASS_NAME = "slice.TestSlice";	
	
	@Before
	public void setup(){
		SootHelper.reset();					
		
		SootHelper.configure("src/test/resources");
	}

	@Test
	public void dummyTest() {
		Scene.v().addBasicClass(CLASS_NAME, SootClass.HIERARCHY);
  	/* load dependencies */
  	Scene.v().loadNecessaryClasses();

  	/* get the method body (in jimple) */
  	Body body = SootHelper.getMethodBody(CLASS_NAME, "m1");		
  	SlicingCriterion sc = new MethodPackageSeed("java.lang.Math");  	  	
  	RelevantVariablesAnalysis rv = new RelevantVariablesAnalysis(new EnhancedUnitGraph(body), sc);
  	
		assertEquals(0, 0);
	}
}
