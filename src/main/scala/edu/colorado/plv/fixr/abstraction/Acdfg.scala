
package edu.colorado.plv.fixr.abstraction

import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import soot.jimple.{AssignStmt, IdentityStmt, InvokeStmt}
import soot.jimple.internal.JimpleLocal

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.mutable.ArrayBuffer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import edu.colorado.plv.fixr.protobuf.ProtoAcdfg

import scala.collection.mutable.{Set, HashSet}
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConverters._

/**
  * Acdfg
  * Class implementing abstract control data flow graph (ACDFG)
  *
  * The class represent a ACDFG as follows:
  * - nodes: a hash table from the node id to the Node object
  * - edges: a hash table from the edge id to the Edge object
  *
  * Kinds of nodes:
  * - Node: abstract generic node
  * - CommandNode: abstract class that represent a node
  *   containing a statement
  * - DataNode: represent a variable of the program
  * - MethodNode: represent a method call
  * - MiscNode: any statement different from a method call
  *
  * Kind of edges:
  * - Edge: abstract generic edge
  * - DefEdge: the source node defines the (variable in the) destination node
  *   Edge from a control node to a data node
  * - UseEdge: the source node uses the (variable in the) destination node
  *   Edge from a control node to a data node
  * - ControlEdge: edge from a control node to another control node
  * - TransControlEdge: represent a transitive control edge (i.e. the
  *   source node can reach the destination node through several
  *   control edge in the original program).
  *
  * Additionally, the edges are labeled. The label contains a set of
  * elements from Label.
  *
  * @author Rhys Braginton Pettee Olsen <rhol9958@colorado.edu>
  * @group  University of Colorado at Boulder CUPLV
  */
abstract class Node {
  def id : Long

  override def toString = this match {
    case n : MethodNode =>
      n.getClass.getSimpleName + "(" +
        "id: "+ n.id.toString + ", " +
        "invokee: "   + n.invokee.toString + ", " +
        "name: "      + n.name.toString + ", " +
        "arguments: [" + n.argumentIds.map(_.toString).mkString(", ") + "]" +
        ")"
    case n : DataNode =>
      n.getClass.getSimpleName + "(" +
        "id: "+ n.id.toString + ", " +
        "name: "   + n.name +
        "type: "   + n.datatype +
        ")"        
    case n => this.getClass.getSimpleName + "(" + id.toString + ")"    
  }  
  
}

abstract class CommandNode extends Node

case class DataNode(override val id : Long, name : String, datatype : String) extends Node

case class MethodNode(override val id : Long,
  //  note: assignee is NOT used to generate Protobuf
  //  assignee : Option[String],
  invokee : Option[Long],
  name : String,
  argumentIds : Vector[Long]
) extends CommandNode

case class MiscNode(override val id : Long) extends CommandNode

/* Edges */
abstract class Edge {
  val to   : Long
  val from : Long
  val id   : Long
  
  override def toString = this.getClass.getSimpleName +
      "(id: "     + id.toString   +
      ", to: "    + to.toString   +
      ", from: "  + from.toString +
      ")"
}

case class DefEdge(
  override val id   : Long,
  override val from : Long,
  override val to   : Long
) extends Edge

case class UseEdge(
  override val id   : Long,
  override val from : Long,
  override val to   : Long
) extends Edge

case class ControlEdge(
  override val id   : Long,
  override val from : Long,
  override val to   : Long
) extends Edge

case class TransControlEdge(
  override val id   : Long,
  override val from : Long,
  override val to   : Long
) extends Edge

case class GitHubRecord(
  userName   : String,
  repoName   : String,
  url        : String,
  commitHash : String
)

case class SourceInfo(
  packageName : String,
  className : String,
  methodName : String,
  classLineNumber : Int,
  methodLineNumber : Int,
  sourceClassName : String,
  absSourceClassName : String // Absolute path to the source file
)

case class AdjacencyList(nodes : Vector[Node], edges : Vector[Edge])

object EdgeLabel extends Enumeration {
  type EdgeLabel = Value
  val EXCEPTIONAL_EDGE, SRC_DOMINATE_DST, DST_DOMINATE_SRC = Value
}

class Acdfg(adjacencyList: AdjacencyList,
  cdfg : UnitCdfgGraph,
  protobuf : ProtoAcdfg.Acdfg,
  gitHubRecord : GitHubRecord,
  sourceInfo : SourceInfo) {

  val logger : Logger = LoggerFactory.getLogger(classOf[Acdfg])

  /*
   * Edges and nodes
   * Design rationale: our graph will be very sparse; want indexing by
   * ID to be fast
   */
  var edges = scala.collection.mutable.HashMap[Long, Edge]()
  var nodes = scala.collection.mutable.HashMap[Long, Node]()

  /**
    *  Map from the edge id to a set of labels
    */
  type LabelsSet = Set[EdgeLabel.Value]
  var edgesLabel = scala.collection.mutable.HashMap[Long, LabelsSet]()

  var methodBag = new scala.collection.mutable.ArrayBuffer[String]()
  var freshIds = new scala.collection.mutable.PriorityQueue[Long]().+=(0)


  private def prepareMethodBag() = {
    logger.debug("### Preparing bag of methods...")
    nodes.filter(_._2.isInstanceOf[MethodNode]).foreach { case (_, node) =>
      methodBag.append(node.asInstanceOf[MethodNode].name)
    }
    methodBag = methodBag.sorted
  }

  /* Internal Protobuf value generated as needed

   [SM] What does it happen if someone access the protobuffer field,
   then changes the ACDFG, and re-access the pb field?
   I think the pb field will not be updated.
   This is a bug.
  */
  private lazy val pb : ProtoAcdfg.Acdfg = {
    var builder : ProtoAcdfg.Acdfg.Builder = ProtoAcdfg.Acdfg.newBuilder()

    /* add the edges */
    edges.foreach {
      case (id : Long, edge : ControlEdge) =>
        val protoControlEdge: ProtoAcdfg.Acdfg.ControlEdge.Builder =
          ProtoAcdfg.Acdfg.ControlEdge.newBuilder()
        protoControlEdge.setId(id)
        protoControlEdge.setFrom(edge.from)
        protoControlEdge.setTo(edge.to)
        builder.addControlEdge(protoControlEdge)
      case (id : Long, edge : DefEdge) =>
        val protoDefEdge: ProtoAcdfg.Acdfg.DefEdge.Builder =
          ProtoAcdfg.Acdfg.DefEdge.newBuilder()
        protoDefEdge.setId(id)
        protoDefEdge.setFrom(edge.from)
        protoDefEdge.setTo(edge.to)
        builder.addDefEdge(protoDefEdge)
      case (id : Long, edge : UseEdge) =>
        val protoUseEdge: ProtoAcdfg.Acdfg.UseEdge.Builder =
          ProtoAcdfg.Acdfg.UseEdge.newBuilder()
        protoUseEdge.setId(id)
        protoUseEdge.setFrom(edge.from)
        protoUseEdge.setTo(edge.to)
        builder.addUseEdge(protoUseEdge)
      case (id : Long, edge : TransControlEdge) =>
        val protoTransEdge: ProtoAcdfg.Acdfg.TransEdge.Builder =
          ProtoAcdfg.Acdfg.TransEdge.newBuilder()
        protoTransEdge.setId(id)
        protoTransEdge.setFrom(edge.from)
        protoTransEdge.setTo(edge.to)
        builder.addTransEdge(protoTransEdge)
    }

    var methodBag : scala.collection.mutable.ArrayBuffer[String] =
      new scala.collection.mutable.ArrayBuffer[String]()

    /* Add the nodes */
    nodes.foreach {
      case (id : Long, node : DataNode) =>
        val protoDataNode : ProtoAcdfg.Acdfg.DataNode.Builder =
          ProtoAcdfg.Acdfg.DataNode.newBuilder()
        protoDataNode.setId(id)
        protoDataNode.setName(node.name)
        protoDataNode.setType(node.datatype)
        builder.addDataNode(protoDataNode)
      case (id : Long, node : MiscNode) =>
        val protoMiscNode : ProtoAcdfg.Acdfg.MiscNode.Builder =
          ProtoAcdfg.Acdfg.MiscNode.newBuilder()
        protoMiscNode.setId(id)
        builder.addMiscNode(protoMiscNode)
      case (id : Long, node : MethodNode) =>
        val protoMethodNode : ProtoAcdfg.Acdfg.MethodNode.Builder =
          ProtoAcdfg.Acdfg.MethodNode.newBuilder()
        protoMethodNode.setId(id)
        if (node.invokee.isDefined) {
          protoMethodNode.setInvokee(node.invokee.get)
        }
        node.argumentIds.foreach(protoMethodNode.addArgument)
        protoMethodNode.setName(node.name)
        builder.addMethodNode(protoMethodNode)

        // add method to bag of methods representation
        methodBag.append(node.name)
    }

    /* copy the repotag informations */
    val protoRepoTag = ProtoAcdfg.Acdfg.RepoTag.newBuilder()
    protoRepoTag.setUserName(this.gitHubRecord.userName)
    protoRepoTag.setRepoName(this.gitHubRecord.repoName)
    protoRepoTag.setUrl(this.gitHubRecord.url)
    protoRepoTag.setCommitHash(this.gitHubRecord.commitHash)
    builder.setRepoTag(protoRepoTag)

    // add bag of methods
    val protoMethodBag = ProtoAcdfg.Acdfg.MethodBag.newBuilder()
    methodBag.sorted.foreach(protoMethodBag.addMethod)
    builder.setMethodBag(protoMethodBag)

    builder.build()
  } /* creation of pb */

  private def getNewId : Long = {
    val newId = freshIds.dequeue()
    if (freshIds.isEmpty) {
      // Must maintain invariant: freshIds always has at least one fresh id
      freshIds.enqueue(newId + 1)
    }
    newId
  }

  private def removeId(id : Long) = freshIds.enqueue(id)

  private def addEdge(id : Long, edge : Edge) = {
    edges += ((edge.id, edge))
    edgesLabel += ((edge.id, new HashSet[EdgeLabel.Value]()))
  }

  def addNode(id : Long, node : Node) : (Long, Node) = {
    val oldCount = nodes.size
    nodes.+=((id, node))
    val newCount = nodes.size
    assert(oldCount + 1 == newCount)
    (id, node)
  }

  def removeEdge(to : Long, from : Long) = {
    val id = edges.find {pair => (pair._2.from == from) && (pair._2.to == to) }.get._1
    edges.remove(id)
  }

  def removeEdgesOf(id : Long) = {
    val edgesOfId = edges.find({
      pair => (pair._2.from == id) || pair._2.to == id
    })
    edgesOfId.foreach(pair => {
      edges.remove(pair._1)
      edgesLabel.remove(pair._1)
    })
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

  /**
    * Creates a ACDFG from the adjacencylist
    */
  def this(adjacencyList: AdjacencyList, gitHubRecord: GitHubRecord,
    sourceInfo : SourceInfo) = {
    this(adjacencyList, null, null, gitHubRecord, sourceInfo)
    assert(this.gitHubRecord == gitHubRecord)
    adjacencyList.nodes.foreach {node => nodes += ((node.id, node))}
    adjacencyList.edges.foreach {edge => addEdge(edge.id, edge)}
    prepareMethodBag()
  }

 /**
    * Creates a ACDFG from a CDFG
    */
  def this(cdfg : UnitCdfgGraph, gitHubRecord: GitHubRecord,
    sourceInfo : SourceInfo) = {
    this(null, cdfg, null, gitHubRecord, sourceInfo)
    assert(this.gitHubRecord == gitHubRecord)

    // the following are used to make lookup more efficient
    var unitToId       = scala.collection.mutable.HashMap[soot.Unit, Long]()
    var localToId      = scala.collection.mutable.HashMap[soot.Local, Long]()
    var edgePairToId   = scala.collection.mutable.HashMap[(Long, Long), Long]()
    var idToMethodStrs = scala.collection.mutable.HashMap[Long, Array[String]]()

    val defEdges = cdfg.defEdges()
    val useEdges = cdfg.useEdges()

    def addMethodNode(unit : soot.Unit,
      assignee : Option[String],
      invokee : Option[Long],
      name : String,
      argumentStrings : Array[String]) : (Long, Node) = {
      val id   = getNewId
      val node = new MethodNode(
        id,
        invokee,
        name,
        Vector.fill(argumentStrings.length)(0) : Vector[Long]
      )
      addNode(id, node)
      unitToId += ((unit, id))
      idToMethodStrs += ((id, argumentStrings))
      (id, node)
    }

    def addMiscNode(unit : soot.Unit) : (Long, Node) = {
      val id = getNewId
      val node = new MiscNode(id)
      addNode(id, node)
      unitToId += ((unit, id))
      (id, node)
    }

    def addUseEdge(fromId : Long, toId : Long): Unit = {
      val id = getNewId
      val edge = new UseEdge(id, fromId, toId)
      addEdge(id, edge)
      edgePairToId += (((fromId, toId), id))
    }

    def addDefEdges(unit : soot.Unit, unitId : Long): Unit = {
      def addDefEdge(fromId : Long, toId : Long): Unit = {
        val id = getNewId
        val edge = new DefEdge(id, fromId, toId)
        addEdge(id, edge)
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
      def addControlEdge(fromId : Long, toId : Long): Unit = {
        val id = getNewId
        val edge = new ControlEdge(id, fromId, toId)
        addEdge(id, edge)
        edgePairToId += (((fromId, toId), id))
      }

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

    def computeTransClosure(): Unit = {
      val commandNodesMap = nodes.filter(_._2.isInstanceOf[CommandNode])
      val commandNodes = commandNodesMap.values.toVector
      val commandNodeCount = commandNodes.size
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
          edges.filter { case ((id, edge)) =>
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
              addTransControlEdge(commandNodes(i).id, commandNodes(j).id)
            }
          }
        }
      }
    }

    def addTransControlEdge(fromId : Long, toId : Long): Unit = {
      val id = getNewId
      val edge = new TransControlEdge(id, fromId, toId)
      addEdge(id, edge)
      edgePairToId += (((fromId, toId), id))
    }

    /* Data nodes */
    cdfg.localsIter().foreach {
      case local : JimpleLocal => {
        val id = getNewId
        val node = new DataNode(id, local.getName, local.getType.toString)
        localToId += ((local, id))
        addNode(id, node)
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
    nodes.foreach({ case (id, _) =>
      val connection = edges.values.find(edge => edge.from == id || edge.to == id)
      if (connection.isEmpty) removeNode(id)
    })

    /* Creates the edges from method nodes to data nodes

     [SM] This seems very inefficient.
     For every method node, every argument we remove the method node
     and we add a new one.
    */
    val dataNodes = nodes.filter(_._2.isInstanceOf[DataNode])
    val methodNodes = nodes.filter(_._2.isInstanceOf[MethodNode])
    methodNodes.foreach {
      case (id, methodNode : MethodNode) =>
        val argumentNames = idToMethodStrs(id)
        argumentNames.zipWithIndex.foreach { case (argumentName, index) =>
          val argumentPairs = dataNodes
            .filter(_._2.asInstanceOf[DataNode].name == argumentName)
          if (argumentPairs.nonEmpty) {
            val argVal : Long = argumentPairs.head._1
            nodes.remove(methodNode.id)
            nodes += ((methodNode.id, MethodNode(
              methodNode.id,
              methodNode.invokee,
              methodNode.name,
              methodNode.argumentIds.updated(index, argVal)
            )))
          } else {
            val argVal : Long = 0
            nodes.remove(methodNode.id)
            nodes += ((methodNode.id, MethodNode(
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
    edges
      .filter(_._2.isInstanceOf[UseEdge])
      .foreach { case (_, edge : UseEdge) => nodes.get(edge.to).get match {
        case node : MethodNode =>
          if (! node.argumentIds.toStream.exists(_.equals(edge.from))) {
            val invokee = Some(edge.from)
            nodes.remove(node.id)
            nodes += ((node.id,
              MethodNode(node.id, invokee, node.name, node.argumentIds)
            ))
          }
        case _ => Nil
      }}

    logger.debug("### Computing transitive closure down to DFS of command edges...")
    computeTransClosure()

    prepareMethodBag()

    logger.debug("### Done")
  }

  /**
    *  Creates the ACDFG structure from the protobuf representation
    */
  def this(protobuf : ProtoAcdfg.Acdfg) = {
    this(null, null, protobuf,
      GitHubRecord(
        if (protobuf.getRepoTag.hasUserName)
          protobuf.getRepoTag.getUserName else "",
        if (protobuf.getRepoTag.hasRepoName)
          protobuf.getRepoTag.getRepoName else "",
        if (protobuf.getRepoTag.hasUrl)
          protobuf.getRepoTag.getUrl else "",
        if (protobuf.getRepoTag.hasCommitHash)
          protobuf.getRepoTag.getCommitHash else ""
      ),
      SourceInfo(protobuf.getSourceInfo.getPackageName,
        protobuf.getSourceInfo.getClassName,
        protobuf.getSourceInfo.getMethodName,
        protobuf.getSourceInfo.getClassLineNumber,
        protobuf.getSourceInfo.getMethodLineNumber,
        protobuf.getSourceInfo.getSourceClassName,
        protobuf.getSourceInfo.getAbsSourceClassName
      )
    )

    /* add data nodes */
    protobuf.getDataNodeList.foreach { dataNode =>
      val node = new DataNode(dataNode.getId, dataNode.getName, dataNode.getType)
      addNode(dataNode.getId, node)
    }
    /* method nodes */
    protobuf.getMethodNodeList.foreach { methodNode =>
      val invokee = if (methodNode.hasInvokee) Some(methodNode.getInvokee) else None
      val node = new MethodNode(methodNode.getId, invokee, methodNode.getName,
        methodNode.getArgumentList.asScala.toVector.map(_.longValue()))
      addNode(methodNode.getId, node)
      (methodNode.getId, node)
    }
    /* misc nodes */
    protobuf.getMiscNodeList.foreach { miscNode =>
      val node = new MiscNode(miscNode.getId)
      addNode(miscNode.getId, node)
      (miscNode.getId, node)
    }

    /* edges */
    protobuf.getControlEdgeList.foreach { protoEdge =>
      val edge = new ControlEdge(protoEdge.getId, protoEdge.getFrom, protoEdge.getTo)
      addEdge(protoEdge.getId, edge)
    }

    protobuf.getUseEdgeList.foreach { protoEdge =>
      val edge = new UseEdge(protoEdge.getId, protoEdge.getFrom, protoEdge.getTo)
      addEdge(protoEdge.getId, edge)
    }

    protobuf.getDefEdgeList.foreach { protoEdge =>
      val edge = new DefEdge(protoEdge.getId, protoEdge.getFrom, protoEdge.getTo)
      addEdge(protoEdge.getId, edge)
    }

    protobuf.getTransEdgeList.foreach { protoEdge =>
      val edge = new TransControlEdge(protoEdge.getId, protoEdge.getFrom, protoEdge.getTo)
      addEdge(protoEdge.getId, edge)
    }


    if ((!protobuf.getMethodBag.isInitialized) ||
      (protobuf.getMethodBag.getMethodCount == 0)) {
      prepareMethodBag()
    } else {
      protobuf.getMethodBag.getMethodList.foreach { method => methodBag.append(method) }
    }
  } /* end of constructor from protobuf */  
 
  
  def union(that : Acdfg) : AdjacencyList =
    AdjacencyList(
      (this.nodes.values.toSet | this.nodes.values.toSet).toVector,
      (this.edges.values.toSet | this.edges.values.toSet).toVector
    )
  def |(that : Acdfg) = union(that)

  def disjointUnion(that : Acdfg) : AdjacencyList =
    AdjacencyList(
      ((this.nodes.values.toSet &~ that.nodes.values.toSet) ++
      (that.nodes.values.toSet -- this.nodes.values.toSet)).toVector,
     ((this.edges.values.toSet -- that.edges.values.toSet) ++
      (that.edges.values.toSet -- this.edges.values.toSet)).toVector
    )
  def +|(that : Acdfg) = disjointUnion(that)

  def intersection(that : Acdfg) : AdjacencyList =
    AdjacencyList(
      (this.nodes.values.toSet | this.nodes.values.toSet).toVector,
      (this.edges.values.toSet | this.edges.values.toSet).toVector
    )
  def &(that : Acdfg) = intersection(that)

  def diff(that : Acdfg) : AdjacencyList =
    AdjacencyList(
      (this.nodes.values.toSet -- this.nodes.values.toSet).toVector,
      (this.edges.values.toSet -- this.edges.values.toSet).toVector
    )
  def --(that : Acdfg) = diff(that)

  def equals(that : Acdfg) : Boolean = {
    val du = this +| that
    du.nodes.isEmpty && du.edges.isEmpty &&
    this.gitHubRecord == that.getGitHubRecord
  }

  def == (that : Acdfg) : Boolean =
    if (that != null) this.equals(that) else false


  def toProtobuf = pb
  def getGitHubRecord = gitHubRecord
  def getSourceInfo = sourceInfo

  override def toString = {
    var output : String = "ACDFG:" + "\n"
    output += ("  " + "Nodes:\n")
    nodes.foreach(node => output += ("    " + node.toString()) + "\n")
    output += "\n"
    output += ("  " + "Edges:\n")
    edges.foreach(edge => output += ("    " + edge.toString()) + "\n")
    output
  }

}
