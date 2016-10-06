package edu.colorado.plv.fixr.abstraction

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.DoubleLinkedList

import org.slf4j.LoggerFactory
import org.slf4j.Logger

import soot.Value
import soot.RefType
import soot.Local
import soot.Body
import soot.jimple.{StmtSwitch, ExprSwitch}
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
import soot.jimple.TableSwitchStmt
import soot.jimple.ThrowStmt
import soot.jimple.FieldRef
import soot.jimple.InstanceFieldRef
import soot.jimple.internal.AbstractInstanceInvokeExpr
import soot.jimple.EqExpr
import soot.jimple.NeExpr
import soot.jimple.GeExpr
import soot.jimple.GtExpr
import soot.jimple.LeExpr
import soot.jimple.LtExpr
import soot.jimple.AndExpr
import soot.jimple.OrExpr
import soot.jimple.XorExpr
import soot.jimple.InterfaceInvokeExpr
import soot.jimple.SpecialInvokeExpr
import soot.jimple.StaticInvokeExpr
import soot.jimple.VirtualInvokeExpr
import soot.jimple.DynamicInvokeExpr
import soot.jimple.CmpExpr
import soot.jimple.CmpgExpr
import soot.jimple.CmplExpr
import soot.jimple.RemExpr
import soot.jimple.LengthExpr
import soot.jimple.ShlExpr
import soot.jimple.ShrExpr
import soot.jimple.UshrExpr
import soot.jimple.SubExpr
import soot.jimple.NewExpr
import soot.jimple.NewArrayExpr
import soot.jimple.NewMultiArrayExpr
import soot.jimple.DivExpr
import soot.jimple.MulExpr
import soot.jimple.AddExpr
import soot.jimple.NegExpr
import soot.jimple.InstanceOfExpr
import soot.jimple.CastExpr

import soot.toolkits.exceptions.ThrowAnalysisFactory
import soot.toolkits.exceptions.ThrowableSet
import soot.toolkits.graph.MHGDominatorsFinder
import soot.toolkits.graph.MHGPostDominatorsFinder


/** Represent a simple transformation on the acdfg.
  * The semantic of remap info is the following:
  * - srcNode and dstNode are two nodes with a control edge
  * - intNodesList is a list of intermediate nodes used to create
  *   new edges
  *
  * The transformation tells to replace the edge (srcNode, dstNode)
  * with the set of edges:
  * (srcNode, n1), (n1,n2), ..., (n_k-1, n_k), (n_k, dstNode)
  * where n1,...,nk are the nodes in intNodesList
  */
case class RemapInfo(
  srcNode : soot.Unit,
  dstNode : soot.Unit,
  intNodesList : List[Long]
)

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

  /* Map from a soot object (value, unit) to a node id */
  var sootObjToId = scala.collection.mutable.HashMap[Any, Long]()
  /* Map from node IDs to the corresponding edge */
  var edgePairToId = scala.collection.mutable.HashMap[(Long, Long), Long]()

  /* Map from unit (the source node of the edges) to a list of remap 
   * info.
   *
   * This is a representation of a list of edges that has been removed
   * from the cdfg and replaced in the acdfg
   *
   * These edges should be ignored in the creation of control edges.
   *
   * NOTE: this is a list and not a set, since we may have multiple
   * edges in from the same source/destination nodes.
   *
   */
  var remappedEdges = scala.collection.mutable.HashMap[soot.Unit,List[RemapInfo]]()

  /* Map from acdfg node ids to cdfg units.
   * The map is used to compute the domniator/post-dominator relationship
   */
  var unitsForDominator = scala.collection.mutable.HashMap[Long, soot.Unit]()


  /* helper class used to create an acdfg node from a node unit */
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
            val nodeId = lookupNodeId(unit)
            nodeId match {
              case Some(id) => id
              case None =>
                /* The lookupNodeId statemebt on unit must not fail.
                 * If it fails, it means that something went wrong inside the
                 * call to unit.apply (look at the cases in AcdfgSootStmtSwitch)
                 */
                new RuntimeException("Error creating ACDFG node from unit")
                0
            }
          }
          case _ =>
            /* We do not know the type of v here, so we cannot create
             * a node. */
            new RuntimeException("Cannot create an ACDFG node for this object")
            0
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

    /* add a use edge for all the method nodes */
    arguments.foreach { fromId => addUseEdge(fromId, id) }

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
      fromId : Long, toId : Long) : Unit = {

      val labelSet = CdfgToAcdfg.getLabelSet(from, to,
        dominators, postDominators)

      exceptionMap.get((from,to)) match {
        case Some(exceptions) =>
          addExceptionalEdge(fromId, toId, exceptions, labelSet)
        case None => addControlEdge(fromId, toId, labelSet)
      }

    }

    /** Find the first element in succToSkip that has succUnit as
      * dstNode.
      * Return in a pair the list without the found element and the
      * element (a list with the original elements and None if
      * succUnit is not found)
      *
      * NOTE: the order of the list is reversed at every call.
      */
    def findElem (succToSkip : List[RemapInfo], succUnit : soot.Unit) = {
      succToSkip.foldLeft ((succToSkip, Option.empty[RemapInfo])) ({ (res, x) => {
        res match {
          case (succToSkip, intEdge) =>
            if (! intEdge.isDefined) {
              if (x.dstNode == succUnit) (succToSkip, Some(x))
              else (x::succToSkip, intEdge)
            }
            else (x::succToSkip, intEdge)
        }
      }})
    }

    val unitId = sootObjToId(unit)

    /* find the list of nodes that should be remapped */
    val succToSkip : List[RemapInfo] = remappedEdges.get(unit) match {
      case Some(l) => l
      case _ => List[RemapInfo]()
    }

    /* Iterates through the successsors of unit.
     For the same unit, we try to find the edges to be redefined.
     Note that in each iteration of the foldLeft we remove such edge
     if it has been found.
     */
    cdfg.getSuccsOf(unit).iterator().foldLeft (succToSkip) (
      (succToSkip, succUnit) => {

        /* find the edges to be redifend */
        val res = findElem(succToSkip, succUnit)

        res match {
          case (newSuccToSkip, intEdge) =>
            intEdge match {
              case Some(remapEdge) => {
                def addRemapEdge(first : Option[soot.Unit], last : soot.Unit, intNodes : List[Long]) : Unit = {
                  intNodes match {
                    case x::Nil => {
                      /* last element */
                      val srcUnit : soot.Unit = unitsForDominator.get(x).get
                      addControEdgeAux(srcUnit, last, x, sootObjToId(last))
                      /* avoid rec call, nothing to do */
                    }
                    case x :: xs if first.isDefined => {
                      /* first element */
                      addControEdgeAux(first.get, succUnit, sootObjToId(unitId), x)
                      addRemapEdge(None, last, xs)
                    }
                    case x :: y :: xs => {
                      /* intermediary element  */
                      val srcUnit = unitsForDominator.get(x).get
                      val dstUnit = unitsForDominator.get(y).get
                      addControEdgeAux(srcUnit, dstUnit, x, y)
                      addRemapEdge(None, last, xs)
                    }
                    case _ => ()
                  }
                }
                addRemapEdge(Some(unit), succUnit, remapEdge.intNodesList)
              }
              case None => addControEdgeAux(unit, succUnit, unitId, sootObjToId(succUnit))
            }
            newSuccToSkip
        }
      })
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
    /* creates all the nodes and some def edges  */
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
class AcdfgSootStmtSwitch(cdfgToAcdfg : CdfgToAcdfg) extends
    StmtSwitch {
  private def getBody() : Body = cdfgToAcdfg.cdfg.getBody()

  private def addMisc(stmt : Stmt) : Node = {
    val miscNode = cdfgToAcdfg.addMiscNode(stmt)
    cdfgToAcdfg.addDefEdges(stmt, miscNode.id)
    miscNode
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
      case x : AbstractInstanceInvokeExpr =>
        Some(cdfgToAcdfg.lookupOrCreateNode(x.getBase()))
      case _ => None
    }

    val mNode = cdfgToAcdfg.addMethodNode(stmt, assigneeId,
        invokee, fullyQualName, nodeArgs)
    cdfgToAcdfg.addDefEdges(stmt, mNode.id)
  }


  override def caseAssignStmt(stmt : AssignStmt): Unit = {
    def fieldAsMethodCall(stmt : AssignStmt,
        field : InstanceFieldRef,
        prefix : String,
        assignee : Option[Long]) = {
      val base = field.getBase
      val baseId = cdfgToAcdfg.lookupOrCreateNode(base)
      val typeStr = field.getField.getType.toString()
      val fieldName = field.getField.getName
      val declClass = field.getField.getDeclaringClass

      val methodName =
        prefix + "." +
        declClass + "."  +
        fieldName + "_" +
        typeStr

      val mNode = cdfgToAcdfg.addMethodNode(stmt, assignee,
          Some(baseId), methodName, List[Long]())
    }

    val assignee = stmt.getLeftOp
    val rhs = stmt.getRightOp
    if (stmt.containsInvokeExpr()) {
      addMethod(stmt, Some(assignee))
    }
    else if (assignee.isInstanceOf[InstanceFieldRef]) {
      fieldAsMethodCall(stmt, assignee.asInstanceOf[InstanceFieldRef],
        FakeMethods.SET_METHOD, None)
    }
    else if (rhs.isInstanceOf[InstanceFieldRef]) {
        val assigneeId = cdfgToAcdfg.lookupOrCreateNode(assignee)
        fieldAsMethodCall(stmt, rhs.asInstanceOf[InstanceFieldRef],
          FakeMethods.GET_METHOD, Some(assigneeId))
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

  override def caseIfStmt(stmt: IfStmt): Unit = {
    /* condition of the if */
    val condition = stmt.getCondition()
    /* true branch */
    val target = stmt.getTarget()
    /* false branch */
    val getNextUnit = getBody().getUnits().getSuccOf(stmt)

    /* The IfStmt node has two successors, the true and false branch.
     In the acdfg:
       - The IfStmt node becomes a MiscNode
       - We add a node in the true branch with the true condition
       - We add a node in the false branch with the negation of the true condition
     */

    /* create the misc node for the statment */
    val miscNode = addMisc(stmt)

    /* create the node for the true successor */

    /* creates the node for the false successor */

    /* add the control edges to be created */




  }

  override def caseInvokeStmt(stmt: InvokeStmt): Unit = addMethod(stmt, None)

  override def caseLookupSwitchStmt(stmt: LookupSwitchStmt): Unit = addMisc(stmt)

  override def caseNopStmt(stmt: NopStmt): Unit = addMisc(stmt)

  override def caseRetStmt(stmt: RetStmt): Unit = addMisc(stmt)

  override def caseReturnStmt(stmt: ReturnStmt): Unit = {
    val retVal = stmt.getOp()
    val retValId = cdfgToAcdfg.lookupOrCreateNode(retVal)

    /* add a fake method node for the return with parameter */
    val mNode = cdfgToAcdfg.addMethodNode(stmt, None, None,
      FakeMethods.RETURN_METHOD, List[Long](retValId))
  }

  override def caseReturnVoidStmt(stmt: ReturnVoidStmt): Unit = {
    /* add a fake method node for the void return */
    val mNode = cdfgToAcdfg.addMethodNode(stmt, None, None,
      FakeMethods.RETURN_METHOD, List[Long]())
  }

  override def caseTableSwitchStmt(stmt: TableSwitchStmt): Unit = addMisc(stmt)

  override def caseThrowStmt(stmt: ThrowStmt): Unit = addMisc(stmt)

  override def defaultCase(stmt: Any): Unit = ()
}

class AcdfgSootExprSwitch(cdfgToAcdfg : CdfgToAcdfg) extends ExprSwitch {

  /* base cases */
  def caseEqExpr(v : EqExpr) : Unit = defaultCase(v)
  def caseNeExpr(v : NeExpr) : Unit = defaultCase(v)
  def caseGeExpr(v : GeExpr) : Unit = defaultCase(v)
  def caseGtExpr(v : GtExpr) : Unit = defaultCase(v)
  def caseLeExpr(v : LeExpr) : Unit = defaultCase(v)
  def caseLtExpr(v : LtExpr) : Unit = defaultCase(v)

  /* recursive cases */
  def caseAndExpr(v : AndExpr) : Unit = defaultCase(v)
  def caseOrExpr(v : OrExpr) : Unit = defaultCase(v)
  def caseXorExpr(v : XorExpr) : Unit = defaultCase(v)

  def caseInterfaceInvokeExpr(v : InterfaceInvokeExpr) : Unit = defaultCase(v)
  def caseSpecialInvokeExpr(v : SpecialInvokeExpr) : Unit = defaultCase(v)
  def caseStaticInvokeExpr(v : StaticInvokeExpr) : Unit = defaultCase(v)
  def caseVirtualInvokeExpr(v : VirtualInvokeExpr) : Unit = defaultCase(v)
  def caseDynamicInvokeExpr(v : DynamicInvokeExpr) : Unit = defaultCase(v)

  /* unknown */
  def caseCmpExpr(v : CmpExpr) : Unit = defaultCase(v)
  def caseCmpgExpr(v : CmpgExpr) : Unit = defaultCase(v)
  def caseCmplExpr(v : CmplExpr) : Unit = defaultCase(v)
  def caseRemExpr(v : RemExpr) : Unit = defaultCase(v)
  def caseLengthExpr(v : LengthExpr) : Unit = defaultCase(v)
  def caseShlExpr(v : ShlExpr) : Unit = defaultCase(v)
  def caseShrExpr(v : ShrExpr) : Unit = defaultCase(v)
  def caseUshrExpr(v : UshrExpr) : Unit = defaultCase(v)
  def caseSubExpr(v : SubExpr) : Unit = defaultCase(v)

  /* Not important cases - for now  */
  def caseNewExpr(v : NewExpr) : Unit = defaultCase(v)
  def caseNewArrayExpr(v : NewArrayExpr) : Unit = defaultCase(v)
  def caseNewMultiArrayExpr(v : NewMultiArrayExpr) : Unit = defaultCase(v)
  def caseDivExpr(v : DivExpr) : Unit = defaultCase(v)
  def caseMulExpr(v : MulExpr) : Unit = defaultCase(v)
  def caseAddExpr(v : AddExpr) : Unit = defaultCase(v)
  def caseNegExpr(v : NegExpr) : Unit = defaultCase(v)
  def caseInstanceOfExpr(v : InstanceOfExpr) : Unit = defaultCase(v)
  def caseCastExpr(v : CastExpr) : Unit = defaultCase(v)

  def defaultCase(obj : Object) : Unit = ()
}


