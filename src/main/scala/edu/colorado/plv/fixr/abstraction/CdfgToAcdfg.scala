package edu.colorado.plv.fixr.abstraction

import soot.jimple.StmtSwitch
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import soot.toolkits.graph.MHGPostDominatorsFinder
import soot.toolkits.graph.MHGDominatorsFinder
import soot.toolkits.exceptions.ThrowAnalysisFactory

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConverters._
import soot.jimple.AssignStmt
import soot.toolkits.exceptions.ThrowableSet
import soot.jimple.IdentityStmt
import soot.jimple.InvokeStmt
import soot.jimple.internal.JimpleLocal
import scala.collection.mutable.ArrayBuffer
import org.slf4j.LoggerFactory
import org.slf4j.Logger

/**
  *  Populates a acdfg from a cdfg
  *
  *  Assume the acdfg does not have any node/edges
  */
class CdfgToAcdfg(val cdfg : UnitCdfgGraph, val acdfg : Acdfg) {
  val logger : Logger = LoggerFactory.getLogger(classOf[CdfgToAcdfg])

  val ug = cdfg.asInstanceOf[soot.toolkits.graph.DirectedGraph[soot.Unit]]
  val dominators : MHGDominatorsFinder[soot.Unit] =
    new MHGDominatorsFinder[soot.Unit](ug)
  val postDominators : MHGPostDominatorsFinder[soot.Unit] =
    new MHGPostDominatorsFinder[soot.Unit](ug)
  val exceptionMap = CdfgToAcdfg.getExceptionMap(cdfg)

  val defEdges = cdfg.defEdges()
  val useEdges = cdfg.useEdges()

  var unitToId = scala.collection.mutable.HashMap[soot.Unit, Long]()
  var localToId = scala.collection.mutable.HashMap[soot.Local, Long]()
  var idToMethodStrs = scala.collection.mutable.HashMap[Long, Array[String]]()
  var edgePairToId = scala.collection.mutable.HashMap[(Long, Long), Long]()

  /** Add a method node
    */
  private def addMethodNode(unit : soot.Unit, assignee : Option[String],
    invokee : Option[Long],
    name : String,
    argumentStrings : Array[String]) : (Long, Node) = {
    val id = acdfg.getNewId
    val node = new MethodNode(
      id,
      invokee,
      name,
      Vector.fill(argumentStrings.length)(0) : Vector[Long]
    )
    acdfg.addNode(node)
    unitToId += ((unit, id))
    idToMethodStrs += ((id, argumentStrings))
    (id, node)
  }

  private def addMiscNode(unit : soot.Unit) : (Long, Node) = {
    val id = acdfg.getNewId
    val node = new MiscNode(id)
    acdfg.addNode(node)
    unitToId += ((unit, id))
    (id, node)
  }

  private def addUseEdge(fromId : Long, toId : Long): Unit = {
    val id = acdfg.getNewId
    val edge = new UseEdge(id, fromId, toId)
    acdfg.addEdge(edge)
    edgePairToId += (((fromId, toId), id))
  }

  private def addDefEdges(unit : soot.Unit, unitId : Long): Unit = {
    def addDefEdge(fromId : Long, toId : Long): Unit = {
      val id = acdfg.getNewId
      val edge = new DefEdge(id, fromId, toId)
      acdfg.addEdge(edge)
      edgePairToId += (((fromId, toId), id))
    }

    if (!defEdges.containsKey(unit)) {
      return
        // defensive programming; don't know if defEdges has a value for every unit
    }
    val localIds : Array[Long] = defEdges.get(unit).iterator().map({local : soot.Local =>
      localToId(local)
    }).toArray
    localIds.foreach({localId : Long => addDefEdge(unitId, localId)
    })
  }

  private def addUseEdges(local : soot.Local, localId : Long): Unit = {
    if (!useEdges.containsKey(local)) {
      return
        // defensive programming; don't know if useEdges has a value for every local
    }
    val unitIds : Array[Long] = useEdges.get(local).iterator().map({unit : soot.Unit =>
      unitToId(unit)
    }).toArray
    unitIds.foreach({unitId : Long => addUseEdge(localId, unitId)
    })
  }

  private def addControlEdges(unit : soot.Unit, unitId : Long): Unit = {
    def addControlEdge(fromId : Long, toId : Long, labels : Acdfg.LabelsSet): Unit = {
      val id = acdfg.getNewId
      val edge = new ControlEdge(id, fromId, toId)
      acdfg.addEdge(edge, labels)
      edgePairToId += (((fromId, toId), id))
    }

    def addExceptionalEdge(fromId : Long, toId : Long,
      exceptions : List[String], labels : Acdfg.LabelsSet): Unit = {
      val id = acdfg.getNewId
      val edge = new ExceptionalControlEdge(id, fromId, toId, exceptions)
      acdfg.addEdge(edge, labels)
      edgePairToId += (((fromId, toId), id))
    }

    def addControEdgeAux(from : soot.Unit, to : soot.Unit,
      fromId : Long, toId : Long) : Unit= {
      val labelSet = CdfgToAcdfg.getLabelSet(from, to, dominators, postDominators)

      if (! edgePairToId.contains(fromId, toId)) {
        exceptionMap.get((from,to)) match {
          case Some(exceptions) =>
            addExceptionalEdge(fromId, toId, exceptions, labelSet)
          case None => addControlEdge(fromId, toId, labelSet)
        }
      }
    }

    // add predecessor edges, if not extant
    val unitId = unitToId(unit)
    cdfg.getPredsOf(unit).iterator().foreach{ (predUnit) =>
      addControEdgeAux(predUnit, unit, unitToId(predUnit), unitId)
    }
    // add succesor edges, if not extant
    cdfg.getSuccsOf(unit).iterator().foreach{ (succUnit) =>
      addControEdgeAux(unit, succUnit, unitId, unitToId(succUnit))
    }
  }

  private def addTransControlEdge(fromId : Long, toId : Long,
    labels : Acdfg.LabelsSet): Unit = {
    val id = acdfg.getNewId
    val edge = new TransControlEdge(id, fromId, toId)
    acdfg.addEdge(edge, labels)
    edgePairToId += (((fromId, toId), id))
  }

  private def computeTransClosure(): Unit = {
    val commandNodesMap = acdfg.nodes.filter(_._2.isInstanceOf[CommandNode])
    val commandNodes = commandNodesMap.values.toVector
    val commandNodeCount = commandNodes.size

    var idToUnit = unitToId map {_.swap}

    var idToAdjIndex = new scala.collection.mutable.HashMap[Long, Int]
    commandNodesMap.zipWithIndex.foreach {
      case (((id : Long, _),index : Int)) =>
        idToAdjIndex += ((id, index))
    }
    var commandAdjMatrix = Array.ofDim[Boolean](commandNodeCount, commandNodeCount)
    var transAdjMatrix = Array.ofDim[Boolean](commandNodeCount, commandNodeCount)
    var stack      = new scala.collection.mutable.Stack[Node]
    var discovered = new scala.collection.mutable.ArrayBuffer[Node]

    //initialize stack
    // add all non-dominated nodes to work list
    commandNodes.filter {node => true
      /*
       edges.values.toSet.contains {
       edge : Edge =>

       edge : Edge => edge.from == node.id
       }  &&
       !edges.values.toSet.contains {
       edge : Edge => edge.to   == node.id
       }
       */
    }.foreach{ n =>
      stack.push(n)
    }
    // assemble adjacency matrix of commands w/out back-edges from DFS
    while (stack.nonEmpty) {
      val node = stack.pop()
      if ((!discovered.contains(node)) && (!stack.contains(node))) {
        discovered += node
        acdfg.edges.filter { case ((id, edge)) =>
          edge.from == node.id && idToAdjIndex.contains(edge.to)
        }.foreach { case ((id, edge)) =>
            val fromId = idToAdjIndex.get(edge.from).get
            val toId   = idToAdjIndex.get(edge.to).get
            commandAdjMatrix(fromId)(toId) = true
            val newNode = commandNodes(toId)
            if (!discovered.contains(newNode)) {
              stack.push(newNode)
            }
        }
      }
    }

    // assemble adjacency list of transitive closure w/ Floyd-Warshall
    // O((Vertices) ^ 3)
    val indices = 0 until commandNodeCount

    /*
     * NOTE: k,i,j major-to-minor order required;
     * although i,k,j major-to-minor order is best for locality,
     * a data dependency requires k,i,j order
     * to maintain the dynamic programming invariant
     */

    indices.foreach { k =>
      indices.foreach { i =>
        indices.foreach { j =>
          if (
            (commandAdjMatrix(i)(k) || transAdjMatrix(i)(k)) &&
              (commandAdjMatrix(k)(j) || transAdjMatrix(k)(j)) &&
              (!commandAdjMatrix(i)(j)) && (!transAdjMatrix(i)(j))
          ) {
            //changed = true
            transAdjMatrix(i)(j) = true
            val fromNode = idToUnit(commandNodes(i).id)
            val toNode = idToUnit(commandNodes(j).id)
            val labelSet = CdfgToAcdfg.getLabelSet(fromNode, toNode,
              dominators, postDominators)
            addTransControlEdge(commandNodes(i).id, commandNodes(j).id, labelSet)
          }
        }
      }
    }
  }


  def fillAcdfg() = {
    /* Data nodes */
    cdfg.localsIter().foreach {
      case local : JimpleLocal => {
        val id = acdfg.getNewId
        val node = new VarDataNode(id, local.getName, local.getType.toString)
        localToId += ((local, id))
        acdfg.addNode(node)
      }
      case m =>
        logger.debug("    Local of unknown type; ignoring...")
    }

    /* Creates nodes */
    cdfg.unitIterator.foreach {
      case n : IdentityStmt =>
        // must have NO arguments to toString(), which MUST have parens;
        // otherwise needs a pointer to some printer object
        val (id, _) = addMiscNode(n)
        addDefEdges(n, id)
      case n : InvokeStmt =>
        val declaringClass = n.getInvokeExpr.getMethod.getDeclaringClass.getName
        val methodName = n.getInvokeExpr.getMethod.getName
        // must have empty arguments to toString(); otherwise needs a pointer to some printer object
        val arguments = n.getInvokeExpr.getArgs
        val argumentStrings = arguments.iterator.foldRight(new ArrayBuffer[String]())(
          (argument, array) => array += argument.toString
        )
        val (id, _) = addMethodNode(n, None, None, declaringClass + "." + methodName, argumentStrings.toArray)
        addDefEdges(n, id)
      case n : AssignStmt =>
        val assignee  = n.getLeftOp.toString()
        if (n.containsInvokeExpr()) {
          val declaringClass = n.getInvokeExpr.getMethod.getDeclaringClass.getName
          val methodName = n.getInvokeExpr.getMethod.getName
          // must have empty arguments to toString(); otherwise needs a pointer to some printer object
          val arguments = n.getInvokeExpr.getArgs
          val argumentStrings = arguments.iterator.foldRight(new ArrayBuffer[String]())(
            (argument, array) => array += argument.toString()
          )
          val (id, _) = addMethodNode(n, Some(assignee), None,
            declaringClass + "." + methodName, argumentStrings.toArray)
          addDefEdges(n, id)
        } else {
          val (id, _) = addMiscNode(n)
          addDefEdges(n, id)
        }
      case n =>
        val (id, _) = addMiscNode(n)
        addDefEdges(n, id)
    }

    /* Add use edges */
    cdfg.localsIter().foreach {
      case n : JimpleLocal => addUseEdges(n, localToId(n))
      case m =>
        logger.debug("    Data node of unknown type; ignoring...")
    }

    /* add the control edges */
    cdfg.unitIterator.foreach { n => addControlEdges(n, unitToId(n)) }

    /* Remove nodes without incoming/outgoing edges */
    acdfg.nodes.foreach({ case (id, _) =>
      val connection = acdfg.edges.values.find(edge => edge.from == id || edge.to == id)
      if (connection.isEmpty) acdfg.removeNode(id)
    })

    /* Creates the edges from method nodes to data nodes

     [SM] This seems very inefficient.
     For every method node, every argument we remove the method node
     and we add a new one.
     */
    val dataNodes = acdfg.nodes.filter(_._2.isInstanceOf[DataNode])
    val methodNodes = acdfg.nodes.filter(_._2.isInstanceOf[MethodNode])
    methodNodes.foreach {
      case (id, methodNode : MethodNode) =>
        val argumentNames = idToMethodStrs(id)
        argumentNames.zipWithIndex.foreach { case (argumentName, index) =>
          val argumentPairs = dataNodes
            .filter(_._2.asInstanceOf[DataNode].name == argumentName)
          if (argumentPairs.nonEmpty) {
            val argVal : Long = argumentPairs.head._1
            acdfg.nodes.remove(methodNode.id)
            acdfg.nodes += ((methodNode.id, MethodNode(
              methodNode.id,
              methodNode.invokee,
              methodNode.name,
              methodNode.argumentIds.updated(index, argVal)
            )))
          } else {
            val argVal : Long = 0
            acdfg.nodes.remove(methodNode.id)
            acdfg.nodes += ((methodNode.id, MethodNode(
              methodNode.id,
              methodNode.invokee,
              methodNode.name,
              methodNode.argumentIds.updated(index, argVal)
            )))
          }
        }
    }

    /* Assign invokee

     [SM] to check
     */
    acdfg.edges
      .filter(_._2.isInstanceOf[UseEdge])
      .foreach { case (_, edge : UseEdge) => acdfg.nodes.get(edge.to).get match {
        case node : MethodNode =>
          if (! node.argumentIds.toStream.exists(_.equals(edge.from))) {
            val invokee = Some(edge.from)
            acdfg.nodes.remove(node.id)
            acdfg.nodes += ((node.id,
              MethodNode(node.id, invokee, node.name, node.argumentIds)
            ))
          }
        case _ => Nil
      }}

    logger.debug("### Computing transitive closure down to DFS of command edges...")
    computeTransClosure()
  }
}

object CdfgToAcdfg {
  type TrapMap = scala.collection.immutable.HashMap[(soot.Unit,soot.Unit),
    List[String]]


  /** Return the set of labels for the edge fromfromUnit to toUnit
    *
    */
  def getLabelSet(fromUnit : soot.Unit, toUnit : soot.Unit,
    dominators : MHGDominatorsFinder[soot.Unit],
    postDominators : MHGPostDominatorsFinder[soot.Unit]) : Acdfg.LabelsSet = {

    val dominates = dominators.isDominatedBy(toUnit, fromUnit)
    val postDominated = postDominators.isDominatedBy(fromUnit, toUnit)

    val l1 = if (dominates) List(EdgeLabel.SRC_DOMINATE_DST) else List[EdgeLabel.Value]()
    val l2 = if (postDominated) EdgeLabel.DST_POSDOMINATE_SRC::l1 else l1

    val labelSet = scala.collection.immutable.HashSet[EdgeLabel.Value]() ++ l2
    labelSet
  }

  /** Return a map from couples of units to a list of exceptions */
  def getExceptionMap(cdfg : UnitCdfgGraph) : TrapMap = {
    /* Process all the traps in cdfg */

    val units = cdfg.getBody().getUnits()
    val trapList = cdfg.getBody().getTraps().toList
    /* Possible optimization: reuse the throwable analyis in the exceptional
     control flow graph */
    val throwAnalysis = ThrowAnalysisFactory.checkInitThrowAnalysis();


    val initMap = new scala.collection.immutable.HashMap[(soot.Unit,soot.Unit),List[String]]()
    val exceptionMap = trapList.foldLeft (initMap) {
      (exceptionMap, trap) => {
        val catcher : soot.RefType = trap.getException().getType()
        var handler : soot.Unit = trap.getHandlerUnit();
        val trapException : String = trap.getException().toString
        val lastUnitInTrap : soot.Unit = units.getPredOf(trap.getEndUnit())

        def processUnits(trapUnitIter : Iterator[soot.Unit],
          trapMap : TrapMap) : TrapMap = {
          if (trapUnitIter.hasNext) {
            val srcUnit = trapUnitIter.next()
            val thrownSet : ThrowableSet = throwAnalysis.mightThrow(srcUnit)
            val caughtException : ThrowableSet =
              thrownSet.whichCatchableAs(catcher).getCaught()

            if (! caughtException.equals(ThrowableSet.Manager.v().EMPTY)) {
              val key = (srcUnit, handler)

              val newTrapMap : TrapMap = trapMap.get(key) match {
                case Some(exceptionList) => {
                  val newList = trapException :: exceptionList
                  trapMap + (key -> newList)
                }
                case None => trapMap + (key -> List[String](trapException))
              }

              processUnits(trapUnitIter, newTrapMap)
            }
            else processUnits(trapUnitIter, trapMap)
          }
          else {
            trapMap
          }
        } /* processUnits */

        val trapUnitIter = units.iterator(trap.getBeginUnit(), lastUnitInTrap)

        processUnits(trapUnitIter, exceptionMap)
      } /* foldLeft on traps */
    }

    exceptionMap
  }
}

/**
  * Implements the case switch on the soot statement found in the cdfg units
  *
  */
class AcdfgSootStmtSwitch(acdfg : Acdfg) extends StmtSwitch {
  val valueStmt = ???

  override def caseAssignStmt(stmt: soot.jimple.AssignStmt): Unit = ???
  override def caseBreakpointStmt(stmt: soot.jimple.BreakpointStmt): Unit = ???
  override def caseEnterMonitorStmt(stmt: soot.jimple.EnterMonitorStmt): Unit = ???
  override def caseExitMonitorStmt(stmt: soot.jimple.ExitMonitorStmt): Unit = ???
  override def caseGotoStmt(stmt: soot.jimple.GotoStmt): Unit = ???

  override def caseIdentityStmt(stmt: soot.jimple.IdentityStmt): Unit = {
    // val (id, _) = addMiscNode(stmt)
    // addDefEdges(stmt, id)
  }

  override def caseIfStmt(stmt: soot.jimple.IfStmt): Unit = {
    ???
  }

  override def caseInvokeStmt(stmt: soot.jimple.InvokeStmt): Unit = ???
  override def caseLookupSwitchStmt(stmt: soot.jimple.LookupSwitchStmt): Unit = ???
  override def caseNopStmt(stmt: soot.jimple.NopStmt): Unit = ???
  override def caseRetStmt(stmt: soot.jimple.RetStmt): Unit = ???
  override def caseReturnStmt(stmt: soot.jimple.ReturnStmt): Unit = ???
  override def caseReturnVoidStmt(stmt: soot.jimple.ReturnVoidStmt): Unit = ???
  override def caseTableSwitchStmt(stmt: soot.jimple.TableSwitchStmt): Unit = ???
  override def caseThrowStmt(stmt: soot.jimple.ThrowStmt): Unit = ???
  override def defaultCase(stmt: Any): Unit = ???
}
