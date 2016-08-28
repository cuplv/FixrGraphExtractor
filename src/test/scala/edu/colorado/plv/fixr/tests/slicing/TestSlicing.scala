package edu.colorado.plv.fixr.tests.slicing

import edu.colorado.plv.fixr.tests.TestClassBase
import edu.colorado.plv.fixr.slicing.APISlicer
import soot.Body
import edu.colorado.plv.fixr.slicing.MethodPackageSeed
import scala.collection.JavaConversions._
import org.scalatest._
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.slicing.APISlicer
import edu.colorado.plv.fixr.tests.TestClassBase
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.slicing.APISlicer
import edu.colorado.plv.fixr.tests.TestClassBase
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.slicing.APISlicer
import edu.colorado.plv.fixr.tests.TestClassBase
import edu.colorado.plv.fixr.SootHelper
import edu.colorado.plv.fixr.slicing.APISlicer
import edu.colorado.plv.fixr.tests.TestClassBase

/**
  * Test the slicing of a method.
  * @author: Sergio Mover
  * @mail: sergio.mover@colorado.edu
  */
abstract class TestSlicing(classPath : String, testClassName : String,
  resClassName : String)
    extends TestClassBase(classPath, testClassName, resClassName) {

  /**
    * Get a list of packages to be included in the slice
    */
  def getPackages() : List[String];

  /**
    * Test the slice of a method
    */
  def testSlice(methodName : String) : Unit = {
    assert(testClass != null)
    assert(resClass != null)

    val bodyToSlice : Body = testClass.getMethodByName(methodName).retrieveActiveBody();
    val expectedRes : Body = resClass.getMethodByName(APISlicer.getSlicedMethodName(methodName)).retrieveActiveBody();

    val slicer : APISlicer = new APISlicer(bodyToSlice);
    val slicedBody : Body = slicer.slice(new MethodPackageSeed(getPackages()));

    assert(null != slicedBody) /* at least one seed in the test*/
    /* obtained and expected results must be the same */
    assert(SootHelper.compareBodies(expectedRes, slicedBody));
  }
}
