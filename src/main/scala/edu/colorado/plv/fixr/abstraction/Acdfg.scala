package edu.colorado.plv.fixr.abstraction

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import soot.jimple.{InvokeStmt, DefinitionStmt}
import soot.jimple.internal.{JIdentityStmt, JimpleLocal}
import soot.IdentityUnit

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

  // ControlEdge, ExceptionalEdge

  // var edges : ArrayBuffer[Edge] = ArrayBuffer()
  //var nodes : ArrayBuffer[Node] = ArrayBuffer()

  /*
   * Edges and nodes
   *   Design rationale: our graph will be very sparse; want indexing by ID to be fast
   */

  private var edges = scala.collection.mutable.HashMap[Long, Edge]()
  private var nodes = scala.collection.mutable.HashMap[Long, Node]()

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

  def addNode(id : Long, node : Node) : Unit = {
    val oldCount = nodes.size
    nodes.+=((id, node))
    val newCount = nodes.size
    assert(oldCount + 1 == newCount)
  }

  def addDataNode(name : String, datatype : String) = {
    val id = getNewId
    val node = new DataNode(id, name, datatype)
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

  override def toString = {
    var output : String = "ACDFG:" + "\n"
    output += ("  " + "Nodes:")
    nodes.foreach(node => output += ("    " + node.toString()))
    output += "\n"
    output += ("  " + "Edges:")
    edges.foreach(edge => output += ("    " + edge.toString()))
    output
  }

  def addMethodNode(assignee : Option[String], name : String, arguments : Array[String]) = {
    val id   = getNewId
    val node = new MethodNode(id, assignee, name, arguments)
    addNode(id, node)
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
        addDataNode(n.getName, n.getType.toString)
        println("    Node added!")
        println(this.nodes.size)
      case m =>
        println("    Data node of unknown type; ignoring...")
    }
  }

  println("### Adding unit/command nodes...")
  cdfg.unitIterator.foreach {
    case n =>
      println("Found unit/command node of type " + n.getClass.toString)
      n match {
        case n : IdentityUnit =>
          // must have NO arguments to toString(), which MUST have parens;
          // otherwise needs a pointer to some printer object
          val assignee     = n.getLeftOp.toString()
          val methodName   = n.getRightOp.getType.toString()
          println("    Assignee     = " + assignee)
          println("    methodName   = " + methodName)
          println("    " + n.toString())
          addMethodNode(Some(assignee), methodName, new Array[String](0))
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
          addMethodNode(None, declaringClass + "." + methodName, argumentStrings.toArray)
          println("    Node added!")
          println(this.nodes.size)
        case n =>
          println("    Data node of unknown type; ignoring...")
      }
  }
  println("ACDFG populated. Outputting...")
  println(this)
}
