package edu.colorado.plv.fixr.extractors.slicing

import soot.Body
import soot.PatchingChain
import soot.{Unit => SootUnit}
import edu.colorado.plv.fixr.slicing.SlicingCriterion
import edu.colorado.plv.fixr.SootHelper
import org.slf4j.LoggerFactory
import soot.PatchingChain
import soot.ValueBox
import soot.jimple.InvokeExpr

import scala.collection.JavaConversions._
import org.slf4j.Logger

/**
  * Remove the method invocations from body that to not match the SlicingCriterion.
  * 
  * Possibly we lose the def and use of variables to methods that will be thrown away
  * in the slice.
  * 
  */
class JimpleSlicer(body : Body, sc: SlicingCriterion) {
  val logger : Logger = LoggerFactory.getLogger(this.getClass())
  def sliceJimple() {
    def sliceJimpleRec(unitIter: Iterator[SootUnit],
                       unitsToRemove: List[SootUnit]): List[SootUnit] = {
      if (unitIter.hasNext()) {
        val unit: SootUnit = unitIter.next()
        val unit_is_seed = sc.is_seed(unit)
        val toRemove = unit.getUseBoxes.foldLeft(false) { (res, valBox) => {
          val v = valBox.getValue

          if (v.isInstanceOf[InvokeExpr] && (!unit_is_seed)) {
            logger.info("Removing..." + unit.toString())
            true
          }
          else {
            res
          }
        }
        }
        if (toRemove) {
          sliceJimpleRec(unitIter, unit :: unitsToRemove)
        }
        else {
          sliceJimpleRec(unitIter, unitsToRemove)
        }
      }
      else {
        unitsToRemove
      }
    }

    val pc: PatchingChain[SootUnit] = body.getUnits()
    // Find all the units that are not method calls and do not match the seed
    val unitsToRemove = sliceJimpleRec(pc.iterator(), List[SootUnit]())
    // Remove all these units
    unitsToRemove.foreach(u => pc.remove(u))
  }

}
