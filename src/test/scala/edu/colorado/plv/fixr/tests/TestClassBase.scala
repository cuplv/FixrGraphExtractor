package edu.colorado.plv.fixr.tests

import soot.SootClass
import edu.colorado.plv.fixr.SootHelper
import soot.Scene
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import soot.options.Options
import org.scalatest.Tag

object TestParseSources extends Tag("edu.colorado.plv.fixr.tests.TestParseSources")

/**
  * Basic class to be used for testing.
  * 
  * @author: Sergio Mover
  * @mail: sergio.mover@colorado.edu
  */
class TestClassBase(classPath : String, testClassName : String,
  resClassName : String) extends FunSuite with BeforeAndAfter {
  var testClass : SootClass = null;
  var resClass : SootClass = null;
  
  def getTestClass() : SootClass = testClass;
  def setTestClass(testClass : SootClass) : Unit = {
    this.testClass = testClass;
  }

  def getResClass() : SootClass = resClass;
  def setResClass(resClass : SootClass) : Unit = {
    this.resClass = resClass;
  }

  def getClassPath() : String = classPath;
  
  /**
    * Setup the soot environment loading the test class and the result class 
    *
    */
  before {
    SootHelper.reset();
    SootHelper.configure(getClassPath(), SootHelper.READ_FROM_SOURCES, null);
    // TODO: merge the test with the extraction part, where we use Soot in the canonical way
    
    Options.v().set_whole_program(true);
    
    /* Load dependencies */
    if (null != testClassName) {
      Scene.v().addBasicClass(testClassName, SootClass.HIERARCHY);
    }
    if (null != resClassName) {
      Scene.v().addBasicClass(resClassName, SootClass.HIERARCHY);
    }
    Scene.v().loadNecessaryClasses();
    
    if (null != testClassName) {
      testClass = Scene.v().tryLoadClass(testClassName, SootClass.BODIES);
      assert(! testClass.isPhantom());
      assert(testClass != null);
    }
    
    if (null != resClassName) {
      resClass = Scene.v().tryLoadClass(resClassName, SootClass.BODIES);
      assert(! resClass.isPhantom());
      assert(resClass != null);
    }
  }
}
