package edu.colorado.plv.fixr.abstraction

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import soot.jimple.{IdentityStmt, AssignStmt, InvokeStmt}
import soot.jimple.internal.JimpleLocal
import scala.collection.JavaConversions.asScalaIterator
import scala.collection.mutable.ArrayBuffer

/**
  * Acdfg
  *   Class implementing abstract control data flow graph (ACDFG)
  *
  *   @author Rhys Braginton Pettee Olsen <rhol9958@colorado.edu>
  *   @group  University of Colorado at Boulder CUPLV
  */

class Acdfg(cdfg : UnitCdfgGraph) {

  /* Nodes */

  trait Node {
    def id : Long
    override def toString = this.getClass.getSimpleName + "(" + id.toString + ")"
  }

  trait CommandNode extends Node

  class DataNode(
    override val id : Long,
    val name : String,
    val datatype : String
  ) extends this.Node

  class MethodNode(
    override val id : Long,
    val assignee : Option[String],
    val name : String,
    val arguments : Array[String]
  ) extends CommandNode

  class MiscNode(
    override val id : Long
  ) extends CommandNode

  /* Edges */

  trait Edge {
    val to   : Long
    val from : Long
    val id   : Long
    override def toString =
      this.getClass.getSimpleName +
      "(id: "     + id.toString   +
      ", to: "    + to.toString   +
      ", from: "  + from.toString +
      ")"
  }

  class DefEdge(
    override val id   : Long,
    override val from : Long,
    override val to   : Long
  ) extends Edge

  class UseEdge(
    override val id   : Long,
    override val from : Long,
    override val to   : Long
  ) extends Edge

  class ControlEdge(
    override val id   : Long,
    override val from : Long,
    override val to   : Long
  ) extends Edge

  // ControlEdge, ExceptionalEdge

  // var edges : ArrayBuffer[Edge] = ArrayBuffer()
  //var nodes : ArrayBuffer[Node] = ArrayBuffer()

  /*
   * Edges and nodes
   *   Design rationale: our graph will be very sparse; want indexing by ID to be fast
   */

  protected[fixr] var edges = scala.collection.mutable.HashMap[Long, Edge]()
  protected[fixr] var nodes = scala.collection.mutable.HashMap[Long, Node]()

  // the following are used to make lookup more efficient
  private var unitToId     = scala.collection.mutable.HashMap[soot.Unit, Long]()
  private var localToId    = scala.collection.mutable.HashMap[soot.Local, Long]()
  private var edgePairToId = scala.collection.mutable.HashMap[(Long, Long), Long]()

  val defEdges = cdfg.defEdges()
  val useEdges = cdfg.useEdges()

  object MinOrder extends Ordering[Int] {
    def compare(x:Int, y:Int) = y compare x
  }

  var freshIds = new scala.collection.mutable.PriorityQueue[Long]().+=(0)

  /*
   * Methods to access ids while maintaining HashMaps, RB tree correctness
   */

  def getNewId : Long = {
    val newId = freshIds.dequeue()
    // Would be nice to do:
    //   whenever freshIds is empty, add next 100 new ids instead of just the next 1
    if (freshIds.isEmpty) {
      // Must maintain invariant: freshIds always has at least one fresh id
      freshIds.enqueue(newId + 1)
    }
    newId
  }

  def removeId(id : Long) = {
    freshIds.enqueue(id)
  }

  def addNode(id : Long, node : Node) : (Long, Node) = {
    val oldCount = nodes.size
    nodes.+=((id, node))
    val newCount = nodes.size
    assert(oldCount + 1 == newCount)
    (id, node)
  }

  def addDataNode(
    local : soot.Local,
    name : String,
    datatype : String
  ) = {
    val id = getNewId
    val node = new DataNode(id, name, datatype)
    localToId += ((local,id))
    addNode(id, node)
  }

  def removeEdge(to : Long, from : Long) = {
    val id = edges.find {pair => (pair._2.from == from) && (pair._2.to == to) }.get._1
    edges.remove(id)
  }

  def removeEdgesOf(id : Long) = {
    edges.find({pair => (pair._2.from == id) || pair._2.to == id }).foreach(pair => edges remove pair._1)
  }

  def removeDataNode(name : String) = {
    nodes.find(pair => pair._2.isInstanceOf[DataNode]).foreach(
      pair => {
        if (pair._2.asInstanceOf[DataNode].name == name) {
          nodes.remove(pair._1)
        }
      }
    )
  }
  def removeNode(id : Long) = {
    nodes.remove(id)
    removeId(id)
  }

  def addMethodNode(
      unit : soot.Unit,
      assignee : Option[String],
      name : String,
      arguments : Array[String]
  ) : (Long, this.Node) = {
    val id   = getNewId
    val node = new MethodNode(id, assignee, name, arguments)
    addNode(id, node)
    unitToId += ((unit, id))
    (id, node)
  }

  def addMiscNode(
    unit : soot.Unit
  ) : (Long, this.Node) = {
    val id = getNewId
    val node = new MiscNode(id)
    addNode(id, node)
    unitToId += ((unit, id))
    (id, node)
  }

  def addDefEdge(fromId : Long, toId : Long): Unit = {
    val id = getNewId
    val edge = new DefEdge(id, fromId, toId)
    edges += ((id, edge))
    edgePairToId += (((fromId, toId), id))
  }

  def addUseEdge(fromId : Long, toId : Long): Unit = {
    val id = getNewId
    val edge = new UseEdge(id, fromId, toId)
    edges += ((id, edge))
    edgePairToId += (((fromId, toId), id))
  }


  def addControlEdge(fromId : Long, toId : Long): Unit = {
    val id = getNewId
    val edge = new ControlEdge(id, fromId, toId)
    edges += ((id, edge))
    edgePairToId += (((fromId, toId), id))
  }

  def addDefEdges(unit : soot.Unit, unitId : Long): Unit = {
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

  def addUseEdges(local : soot.Local, localId : Long): Unit = {
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

  def addControlEdges(unit : soot.Unit, unitId : Long): Unit = {
    // add predecessor edges, if not extant
    val unitId = unitToId(unit)
    cdfg.getPredsOf(unit).iterator().foreach{ (predUnit) =>
      val predUnitId = unitToId(predUnit)
      if (!edgePairToId.contains(predUnitId, unitId)) {
        addControlEdge(predUnitId, unitId)
      }
    }
    // add succesor edges, if not extant
    cdfg.getSuccsOf(unit).iterator().foreach{ (succUnit) =>
      val succUnitId = unitToId(succUnit)
      if (!edgePairToId.contains(unitId, succUnitId)) {
        addControlEdge(unitId, succUnitId)
      }
    }
  }

  override def toString = {
    var output : String = "ACDFG:" + "\n"
    output += ("  " + "Nodes:\n")
    nodes.foreach(node => output += ("    " + node.toString()) + "\n")
    output += "\n"
    output += ("  " + "Edges:\n")
    edges.foreach(edge => output += ("    " + edge.toString()) + "\n")
    output
  }

  /**
    * @constructor
    */

  println("### Adding local/data nodes...")
  cdfg.localsIter().foreach {
    case n =>
      println("Found local/data node " + n.getName + " : " + n.getType.toString)
      println("  node type of " + n.getClass.toString)
      n match {
      case n : JimpleLocal =>
        addDataNode(n, n.getName, n.getType.toString)
        println("    Node added!")
      case m =>
        println("    Data node of unknown type; ignoring...")
    }
  }

  println("### Adding unit/command nodes & def-edges ...")
  cdfg.unitIterator.foreach {
    case n =>
      println("Found unit/command node of type " + n.getClass.toString)
      n match {
        case n : IdentityStmt =>
          // must have NO arguments to toString(), which MUST have parens;
          // otherwise needs a pointer to some printer object
          println("    Data node of unknown type; adding misc node...")
          val (id, _) = addMiscNode(n)
          println("    Node added!")
          addDefEdges(n, id)
          println("    Def-edge added!")
        case n : InvokeStmt =>
          val declaringClass = n.getInvokeExpr.getMethod.getDeclaringClass.getName
          val methodName = n.getInvokeExpr.getMethod.getName
          // must have empty arguments to toString(); otherwise needs a pointer to some printer object
          val arguments = n.getInvokeExpr.getArgs
          println("    declaringClass = " + declaringClass)
          println("    methodName = " + methodName)
          println("    Arguments:")
          var i : Int = 1
          arguments.iterator.foreach({argument =>
            println("      " + i.toString + " = " + argument.toString())
            i += 1
          })
          println("    " + n.toString())
          val argumentStrings = arguments.iterator.foldRight(new ArrayBuffer[String]())(
            (argument, array) => array += (argument.toString())
          )
          val (id, _) = addMethodNode(n, None, declaringClass + "." + methodName, argumentStrings.toArray)
          println("    Node added!")
          addDefEdges(n, id)
          println("    Def-edge added!")
        case n : AssignStmt =>
          val assignee  = n.getLeftOp.toString()
          if (n.containsInvokeExpr()) {
            val declaringClass = n.getInvokeExpr.getMethod.getDeclaringClass.getName
            val methodName = n.getInvokeExpr.getMethod.getName
            // must have empty arguments to toString(); otherwise needs a pointer to some printer object
            val arguments = n.getInvokeExpr.getArgs
            println("    Assignee       = " + assignee)
            println("    declaringClass = " + declaringClass)
            println("    methodName     = " + methodName)
            println("    Arguments:")
            var i : Int = 1
            arguments.iterator.foreach({argument =>
              println("      " + i.toString + " = " + argument.toString())
              i += 1
            })
            println("    " + n.toString())
            val argumentStrings = arguments.iterator.foldRight(new ArrayBuffer[String]())(
              (argument, array) => array += (argument.toString())
            )
            val (id, _) = addMethodNode(n, Some(assignee), declaringClass + "." + methodName, argumentStrings.toArray)
            println("    Node added!")
            addDefEdges(n, id)
            println("    Def-edge added!")
          } else {
            println("    Data node doesn't use invocation; adding empty misc node...")
            val (id, _) = addMiscNode(n)
            println("    Node added!")
            addDefEdges(n, id)
            println("    Def-edge added!")
          }
        case n =>
          println("    Data node of unknown type; adding misc node...")
          val (id, _) = addMiscNode(n)
          println("    Node added!")
          addDefEdges(n, id)
          println("    Def-edge added!")
      }
  }

  println("### Adding use-edges...")
  cdfg.localsIter().foreach {
    case n =>
      println("Found local/data node " + n.getName + " : " + n.getType.toString)
      println("  node type of " + n.getClass.toString)
      n match {
        case n : JimpleLocal =>
          addUseEdges(n, localToId(n))
          println("    Use-edge(s) added!")
        case m =>
          println("    Data node of unknown type; ignoring...")
      }
  }
  println("### Adding control-edges...")
  cdfg.unitIterator.foreach { n =>
    println("Found unit/command node of type " + n.getClass.toString)
    addControlEdges(n, unitToId(n))
    println("Unadded control-edges added.")
  }
  println("### Removing unconnected nodes...")
  nodes.foreach({ case (id, _) =>
    val connection = edges.values.find(edge => edge.from == id || edge.to == id)
    if (connection.isEmpty) {
      removeNode(id)
    }
  })

}
