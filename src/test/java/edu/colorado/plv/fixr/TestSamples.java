package edu.colorado.plv.fixr;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

public class TestSamples extends TestCdfg {

	public TestSamples() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Collection<String> getPackages() { 
		return Collections.singletonList("android.");
	}

	@Override
	public String getTestClassName() {
		return "androidtests.Samples";
	}

	@Override
	public String getResClassName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getClassPath() {
		String classpath = this.classPath + ":./src/test/resources/libs/android.jar"; 
		return classpath;
	}

	@Test
	public void t1() { testCdfg("update"); }
	@Test
	public void t2() { testCdfg("deleteRow"); }
	@Test
	public void t3() { testCdfg("deleteNUsers"); }
	@Test
	public void t4() { testCdfg("retrieveData"); }
	

}
