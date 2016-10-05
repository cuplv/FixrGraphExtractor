package edu.colorado.plv.fixr.abstraction

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import org.slf4j.LoggerFactory
import org.slf4j.Logger

import soot.Value
import soot.RefType
import soot.Local
import soot.jimple.Constant
import soot.jimple.AssignStmt
import soot.jimple.AssignStmt
import soot.jimple.BreakpointStmt
import soot.jimple.EnterMonitorStmt
import soot.jimple.ExitMonitorStmt
import soot.jimple.GotoStmt
import soot.jimple.IdentityStmt
import soot.jimple.IdentityStmt
import soot.jimple.IfStmt
import soot.jimple.InvokeStmt
import soot.jimple.InvokeStmt
import soot.jimple.LookupSwitchStmt
import soot.jimple.NopStmt
import soot.jimple.RetStmt
import soot.jimple.ReturnStmt
import soot.jimple.ReturnVoidStmt
import soot.jimple.Stmt
import soot.jimple.StmtSwitch
import soot.jimple.TableSwitchStmt
import soot.jimple.ThrowStmt
import soot.jimple.internal.AbstractInstanceInvokeExpr

import soot.toolkits.exceptions.ThrowAnalysisFactory
import soot.toolkits.exceptions.ThrowableSet
import soot.toolkits.graph.MHGDominatorsFinder
import soot.toolkits.graph.MHGPostDominatorsFinder


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

  var sootObjToId = scala.collection.mutable.HashMap[Any, Long]()
  var edgePairToId = scala.collection.mutable.HashMap[(Long, Long), Long]()

  val nodeCreator = new AcdfgSootStmtSwitch(this)

  def lookupNodeId(v : Any) : Option[Long] = sootObjToId.get(v)

  def lookupOrCreateNode(v : Any) : Long = {
    val nodeId = lookupNodeId(v)

    val idVal = nodeId match {
      case Some(id) => id
      case _ => {
        val nodeId = v match {
          case local : Local => {
            val id = acdfg.getNewId
            val node = new VarDataNode(id, local.getName, local.getType.toString)
            sootObjToId += ((v, id))
            acdfg.addNode(node)
            id
          }
          case constant : Constant => {
            val id = acdfg.getNewId
            val node = new ConstDataNode(id, constant.toString, constant.getType.toString)
            sootObjToId += ((v, id))
            acdfg.addNode(node)
            id
          }
          case unit : soot.Unit => {
            unit.apply(nodeCreator)
            val nodeId = lookupNodeId(v)
            nodeId match {
              case Some(id) => id
              case None => ??? // TODO raise exception
            }
          }
          case _ => ??? // TODO raise exception
        }
        nodeId
      }
    }
    idVal
  }

  /** Add a method node
    */
  def addMethodNode(unit : soot.Unit, assignee : Option[Long],
      invokee : Option[Long], name : String, arguments : List[Long])  : Node = {
    val id = acdfg.getNewId
    val node = new MethodNode(id, assignee, invokee, name, arguments.toVector)
    acdfg.addNode(node)
    sootObjToId += ((unit, id))
    node
  }

  def addMiscNode(unit : soot.Unit) : Node = {
    val id = acdfg.getNewId
    val node = new MiscNode(id)
    acdfg.addNode(node)
    sootObjToId += ((unit, id))
    node
  }

  def addUseEdge(fromId : Long, toId : Long): Unit = {
    val id = acdfg.getNewId
    val edge = new UseEdge(id, fromId, toId)
    acdfg.addEdge(edge)
    edgePairToId += (((fromId, toId), id))
  }

  def addDefEdges(unit : soot.Unit, unitId : Long): Unit = {
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
      val toId = sootObjToId.get(local)
      toId match {
        case Some(id) => id
        case None => lookupOrCreateNode(local)
      }
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
      sootObjToId(unit)
    }).toArray
    unitIds.foreach({unitId : Long =>
      this.edgePairToId.get((localId, unitId)) match {
        case Some(x) => Unit
        case None => addUseEdge(localId, unitId)
      }
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

    val unitId = sootObjToId(unit)
    // add succesor edges
    cdfg.getSuccsOf(unit).iterator().foreach{ (succUnit) =>
      addControEdgeAux(unit, succUnit, unitId, sootObjToId(succUnit))
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

    var idToUnit = sootObjToId map {_.swap}

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
            assert(fromNode.isInstanceOf[soot.Unit])
            assert(toNode.isInstanceOf[soot.Unit])

            val labelSet = CdfgToAcdfg.getLabelSet(
                fromNode.asInstanceOf[soot.Unit],
                toNode.asInstanceOf[soot.Unit], dominators, postDominators)
            addTransControlEdge(commandNodes(i).id, commandNodes(j).id, labelSet)
          }
        }
      }
    }
  }


  /** Fill the ACDFG visiting the graph
    */
  def fillAcdfg() = {
    /* creates all the nodes and some use edges?!?  */
    cdfg.getHeads().foreach(head => createNodes(head, HashSet[soot.Unit]()))

    /* Add use edges */
    cdfg.localsIter().foreach {
      case n : Local => addUseEdges(n, sootObjToId(n))
      case m =>
        logger.debug("    Data node of unknown type; ignoring...")
    }

    /* creates all the control edges */
    cdfg.unitIterator.foreach { n => addControlEdges(n, sootObjToId(n)) }

    /* computes transitive clouse */
    logger.debug("### Computing transitive closure down to DFS of command edges...")
    computeTransClosure()
  }

  /** Fill the acdfg with all the nodes reachable from head
    */
  private def createNodes(unit : soot.Unit,
      visited : HashSet[soot.Unit]) : Unit = {
    if (! visited.contains(unit)) {
      /* create node */
      unit.apply(nodeCreator)

      /* create children */
      visited += unit
      cdfg.getSuccsOf(unit).iterator().foreach{ (succ) => createNodes(succ, visited) }
    }
  }

//  def fillAcdfgOld() = {
//    /* Data nodes */
//    cdfg.localsIter().foreach {
//      case local : Local => {
//        val id = acdfg.getNewId
//        val node = new VarDataNode(id, local.getName, local.getType.toString)
//        sootObjToId += ((local, id))
//        acdfg.addNode(node)
//      }
//      case m =>
//        logger.debug("    Local of unknown type; ignoring...")
//    }
//
//    /* Creates nodes */
//    cdfg.unitIterator.foreach {
//      case n : IdentityStmt =>
//        // must have NO arguments to toString(), which MUST have parens;
//        // otherwise needs a pointer to some printer object
//        val miscNode = addMiscNode(n)
//        addDefEdges(n, miscNode.id)
//      case n : InvokeStmt =>
//
//      case n : AssignStmt =>
//        val assignee  = n.getLeftOp.toString()
//        if (n.containsInvokeExpr()) {
//          val declaringClass = n.getInvokeExpr.getMethod.getDeclaringClass.getName
//          val methodName = n.getInvokeExpr.getMethod.getName
//          // must have empty arguments to toString(); otherwise needs a pointer to some printer object
//          val arguments = n.getInvokeExpr.getArgs
//          val argumentStrings = arguments.iterator.foldRight(new ArrayBuffer[String]())(
//            (argument, array) => array += argument.toString()
//          )
//
//          //val mNode = addMethodNode(n, Some(assignee), None,
//          //  declaringClass + "." + methodName, argumentStrings.toArray)
//          //addDefEdges(n, mNode.id)
//        } else {
//          val miscNode = addMiscNode(n)
//          addDefEdges(n, miscNode.id)
//        }
//      case n =>
//        val miscNode = addMiscNode(n)
//        addDefEdges(n, miscNode.id)
//    }
//
//    /* Add use edges */
//    cdfg.localsIter().foreach {
//      case n : Local => addUseEdges(n, sootObjToId(n))
//      case m =>
//        logger.debug("    Data node of unknown type; ignoring...")
//    }
//
//    /* add the control edges */
//    cdfg.unitIterator.foreach { n => addControlEdges(n, sootObjToId(n)) }
//
//    /* Remove nodes without incoming/outgoing edges */
//    acdfg.nodes.foreach({ case (id, _) =>
//      val connection = acdfg.edges.values.find(edge => edge.from == id || edge.to == id)
//      if (connection.isEmpty) acdfg.removeNode(id)
//    })
//
//    /* Creates the edges from method nodes to data nodes
//
//     [SM] This seems very inefficient.
//     For every method node, every argument we remove the method node
//     and we add a new one.
//     */
//    val dataNodes = acdfg.nodes.filter(_._2.isInstanceOf[DataNode])
//    val methodNodes = acdfg.nodes.filter(_._2.isInstanceOf[MethodNode])
//    methodNodes.foreach {
//      case (id, methodNode : MethodNode) =>
//        val argumentNames = idToMethodStrs(id)
//        argumentNames.zipWithIndex.foreach { case (argumentName, index) =>
//          val argumentPairs = dataNodes
//            .filter(_._2.asInstanceOf[DataNode].name == argumentName)
//          if (argumentPairs.nonEmpty) {
//            val argVal : Long = argumentPairs.head._1
//            acdfg.nodes.remove(methodNode.id)
//            acdfg.nodes += ((methodNode.id, MethodNode(
//              methodNode.id,
//              methodNode.invokee,
//              methodNode.name,
//              methodNode.argumentIds.updated(index, argVal)
//            )))
//          } else {
//            val argVal : Long = 0
//            acdfg.nodes.remove(methodNode.id)
//            acdfg.nodes += ((methodNode.id, MethodNode(
//              methodNode.id,
//              methodNode.invokee,
//              methodNode.name,
//              methodNode.argumentIds.updated(index, argVal)
//            )))
//          }
//        }
//    }
//
//    /* Assign invokee
//
//     [SM] to check
//     */
//    acdfg.edges
//      .filter(_._2.isInstanceOf[UseEdge])
//      .foreach { case (_, edge : UseEdge) => acdfg.nodes.get(edge.to).get match {
//        case node : MethodNode =>
//          if (! node.argumentIds.toStream.exists(_.equals(edge.from))) {
//            val invokee = Some(edge.from)
//            acdfg.nodes.remove(node.id)
//            acdfg.nodes += ((node.id,
//              MethodNode(node.id, invokee, node.name, node.argumentIds)
//            ))
//          }
//        case _ => Nil
//      }}
//
//    logger.debug("### Computing transitive closure down to DFS of command edges...")
//    computeTransClosure()
//  }
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
        val catcher : RefType = trap.getException().getType()
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
class AcdfgSootStmtSwitch(cdfgToAcdfg : CdfgToAcdfg) extends StmtSwitch {
  private def addMisc(stmt : Stmt) : Unit = {
    val miscNode = cdfgToAcdfg.addMiscNode(stmt)
    cdfgToAcdfg.addDefEdges(stmt, miscNode.id)
  }

  private def addMethod(stmt : Stmt, assignee : Option[Value]) = {
    val invokeExpr = stmt match {
      case s : InvokeStmt => s.asInstanceOf[InvokeStmt].getInvokeExpr
      case s : AssignStmt => stmt.getInvokeExpr
    }

    val declaringClass = invokeExpr.getMethod.getDeclaringClass.getName
    val methodName = invokeExpr.getMethod.getName
    val fullyQualName = declaringClass + "." + methodName

    val reversedNodeArgs = invokeExpr.getArgs.foldLeft (List[Long]()) {
      (nodeArgs, arg : Value) =>
        cdfgToAcdfg.lookupOrCreateNode(arg) :: nodeArgs
    }
    val nodeArgs = reversedNodeArgs.reverse

    val assigneeId = assignee match {
      case Some(a) => Some(cdfgToAcdfg.lookupOrCreateNode(a))
      case None => None
    }


    /* TODO check for static method invocation, better way to get the invokee */
    val invokee = invokeExpr match {
      case x : AbstractInstanceInvokeExpr => Some(cdfgToAcdfg.lookupOrCreateNode(x.getBase()))
      case _ => None
    }

    val mNode = cdfgToAcdfg.addMethodNode(stmt, assigneeId,
        invokee, fullyQualName, nodeArgs)
    cdfgToAcdfg.addDefEdges(stmt, mNode.id)

    /* add a use edge for all the method nodes */
    nodeArgs.foreach { fromId => cdfgToAcdfg.addUseEdge(fromId, mNode.id) }
  }

  override def caseAssignStmt(stmt : AssignStmt): Unit = {
    val assignee = stmt.getLeftOp
    if (stmt.containsInvokeExpr()) {
      addMethod(stmt, Some(assignee))
    }
    else {
      addMisc(stmt)
    }
  }

  override def caseBreakpointStmt(stmt: BreakpointStmt): Unit = addMisc(stmt)

  override def caseEnterMonitorStmt(stmt: EnterMonitorStmt): Unit = addMisc(stmt)

  override def caseExitMonitorStmt(stmt: ExitMonitorStmt): Unit = ???

  override def caseGotoStmt(stmt: GotoStmt): Unit = addMisc(stmt)

  override def caseIdentityStmt(stmt: IdentityStmt): Unit = addMisc(stmt)

  override def caseIfStmt(stmt: IfStmt): Unit = addMisc(stmt)

  override def caseInvokeStmt(stmt: InvokeStmt): Unit = addMethod(stmt, None)

  override def caseLookupSwitchStmt(stmt: LookupSwitchStmt): Unit = addMisc(stmt)

  override def caseNopStmt(stmt: NopStmt): Unit = addMisc(stmt)

  override def caseRetStmt(stmt: RetStmt): Unit = addMisc(stmt)

  override def caseReturnStmt(stmt: ReturnStmt): Unit = addMisc(stmt)

  override def caseReturnVoidStmt(stmt: ReturnVoidStmt): Unit = addMisc(stmt)

  override def caseTableSwitchStmt(stmt: TableSwitchStmt): Unit = addMisc(stmt)

  override def caseThrowStmt(stmt: ThrowStmt): Unit = addMisc(stmt)

  override def defaultCase(stmt: Any): Unit = ???
}
