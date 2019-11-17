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
  def sliceJimple() : Boolean = {
    def sliceJimpleRec(unitIter: Iterator[SootUnit],
      unitsToRemove: List[SootUnit],
      isEmpty : Boolean): (List[SootUnit], Boolean) = {
      if (unitIter.hasNext()) {
        val unit: SootUnit = unitIter.next()
        val unit_is_seed = sc.is_seed(unit)

        val (toRemove, isEmptyRes) = unit.getUseBoxes.foldLeft((false, isEmpty)) { (res, valBox) => {
          val v = valBox.getValue
          val isMethodInvocation = v.isInstanceOf[InvokeExpr]

          if (isMethodInvocation && (! unit_is_seed)) {
            logger.info("Removing..." + unit.toString())

            res match {
              case  (appToRemove, appIsEmpty) => (true, appIsEmpty)
              case _ => res
            }
          } else if (isMethodInvocation) {
            res match {
              case  (appToRemove, appIsEmpty) => (appToRemove, false)
              case _ => res
            }
          } else {
            res
          }
        }}
        
        if (toRemove) {
          sliceJimpleRec(unitIter, unit :: unitsToRemove, isEmptyRes)
        }
        else {
          sliceJimpleRec(unitIter, unitsToRemove, isEmptyRes)
        }
      }
      else {
        (unitsToRemove, isEmpty)
      }
    }

    val pc: PatchingChain[SootUnit] = body.getUnits()
    // Find all the units that are not method calls and do not match the seed
    val (unitsToRemove, isEmpty) = sliceJimpleRec(pc.iterator(), List[SootUnit](), true)

    if (! isEmpty)
      unitsToRemove.foreach(u => pc.remove(u))

    isEmpty
  }
}
