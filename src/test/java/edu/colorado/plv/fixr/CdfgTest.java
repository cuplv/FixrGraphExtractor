package edu.colorado.plv.fixr;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;


public class CdfgTest extends TestCdfg {
	static final String CLASS_NAME = "slice.TestSlice";	
	
	@Override
	public Collection<String> getPackages() {
		return Collections.singletonList("java.lang.Math");	
	}

	@Override
	public String getTestClassName() {
		return CLASS_NAME;
	}
	
	@Override
	public String getResClassName() {
		return null;
	}			
	@Test
	public void cdfgT1()	
	{
		testCdfg("m1");		
	}
}
