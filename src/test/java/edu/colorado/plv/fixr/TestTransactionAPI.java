package edu.colorado.plv.fixr;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

public class TestTransactionAPI extends TestCdfg {

	@Override
	public Collection<String> getPackages() {
		return Collections.singletonList("android.");
	}

	@Override
	public String getTestClassName() {
		return "androidtests.TransactionAPI";		
	}

	@Override
	public String getResClassName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.colorado.plv.fixr.TestClassBase#getClassPath()
	 */
	@Override
	public String getClassPath() {
		String classpath = this.classPath + ":./src/test/resources/libs/android.jar"; 
		return classpath;
	}

	@Test
	public void t1() { testCdfg("syncUsers"); }
	
	@Test
	public void t2() { testCdfg("wrongTransaction"); }	

	@Test
	public void t3() { testCdfg("rightTransaction"); }
}
