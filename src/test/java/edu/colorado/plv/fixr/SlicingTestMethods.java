package edu.colorado.plv.fixr;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

public class SlicingTestMethods extends TestSlicing {
	@Override
	public String getTestClassName() {return "slice.TestSliceMethods";}
	@Override
	public String getResClassName() {return "slice.TestSliceMethodsRes";}

	@Override
	public Collection<String> getPackages() {
		return Collections.singletonList("java.util.Random");
	}

	@Test
	public void t1(){testSlice("testSliceMethods01");}
	
	@Test
	public void t2(){testSlice("testSliceMethods02");}
	
	@Test
	public void t3(){testSlice("testSliceMethods03");}
	
	@Test
	public void t4(){testSlice("testSliceMethods04");}
	
	@Test
	public void t5(){testSlice("testSliceMethods05");}
	
	@Test
	public void t6(){testSlice("testSliceMethods06");}	
}
