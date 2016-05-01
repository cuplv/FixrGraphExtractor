package edu.colorado.plv.fixr;

import org.junit.Test;

public class SlicingTestBase extends TestSlicing {
	static final String CLASS_NAME = "slice.TestSlice";
	static final String RES_CLASS_NAME = "slice.TestSliceRes";
	
	@Test
	public void sliceT1()	{testSlice("m1");}

	@Test
	public void sliceT2()	{testSlice("m2");}

	@Test
	public void sliceT3()	{testSlice("m3");}

	@Test
	public void sliceT4()	{testSlice("m4");}
	
	@Test
	public void sliceT5()	{testSlice("m5");}

	@Test
	public void sliceT6()	{testSlice("m6");}
	
	@Test
	public void sliceT7()	{testSlice( "m7");}
	
	@Test
	public void sliceT8()	{testSlice("m8");}

	@Override
	public String getTestClassName() {
		return CLASS_NAME;
	}

	@Override
	public String getResClassName() {
		return RES_CLASS_NAME;
	}			
}
