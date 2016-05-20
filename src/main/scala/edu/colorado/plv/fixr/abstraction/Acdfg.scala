package edu.colorado.plv.fixr.abstraction

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import soot.jimple.DefinitionStmt
import soot.jimple.internal.{JIdentityStmt, JimpleLocal}
import soot.IdentityUnit

import scala.collection.JavaConversions.asScalaIterator

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

  private def edges = scala.collection.mutable.HashMap[Long, Edge]()
  private def nodes = scala.collection.mutable.HashMap[Long, Node]()

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

  def addNode(id : Long, node : Node) = nodes.+=((id, node))

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
      case m => {
        println("    Data node of unknown type; ignoring...")
      }
    }
  }

  println("### Adding unit/command nodes...")
  cdfg.unitIterator.foreach {
    case n =>
      println("Found unit/command node of type " + n.getClass.toString)
      n match {
        case n : IdentityUnit =>
          // must pass in null to disambiguate method invoked; better way?
          /*
          val assignee = n.getLeftOp.toString(null)
          val methodName = n.getRightOp.toString(null)
          println("    Assignee = " + assignee)
          println("    methodName = " + methodName)
          */
          println("    " + n.toString(null))
      }
  }
}
