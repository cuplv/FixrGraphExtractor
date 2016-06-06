package edu.colorado.plv.fixr;

import org.junit.Before;

import soot.Scene;
import soot.SootClass;

public abstract class TestClassBase {
	protected final String classPath = "src/test/resources/jimple";

	protected SootClass testClass = null;
	protected SootClass resClass = null;

	public TestClassBase() {	
	}

	/**
	 * @return the testClass
	 */
	public SootClass getTestClass() {
		return testClass;
	}

	/**
	 * @param testClass the testClass to set
	 */
	public void setTestClass(SootClass testClass) {
		this.testClass = testClass;
	}

	/**
	 * @return the resClass
	 */
	public SootClass getResClass() {
		return resClass;
	}

	/**
	 * @param resClass the resClass to set
	 */
	public void setResClass(SootClass resClass) {
		this.resClass = resClass;
	}
	
	public abstract String getTestClassName();	
	public abstract String getResClassName();	
	
	public String getClassPath() {
		return this.classPath;		
	}	
	
	@Before
	public void setup()
	{
		SootHelper.reset();			
		SootHelper.configure(getClassPath(), true);
		
		/* Load dependencies */
		if (null != getTestClassName()) {
			Scene.v().addBasicClass(getTestClassName(), SootClass.HIERARCHY);
		}
		if (null != getResClassName()) {
			Scene.v().addBasicClass(getResClassName(), SootClass.HIERARCHY);			
		}

		Scene.v().loadNecessaryClasses();		
		
		if (null != getTestClassName())
			setTestClass(Scene.v().tryLoadClass(getTestClassName(), SootClass.BODIES));			
		if (null != getResClassName())				
			setResClass(Scene.v().tryLoadClass(getResClassName(), SootClass.BODIES));
	}	
}
