package edu.colorado.plv.fixr.abstraction

import java.util
import java.util.Iterator

import edu.colorado.plv.fixr.graphs.CFGToDotGraph.{DotNamer, NodeComparator}
import edu.colorado.plv.fixr.graphs.{UnitCdfgGraph, CFGToDotGraph}
import soot.{Body, Unit, Local}
import soot.util.dot.{DotGraphEdge, DotGraphNode, DotGraphConstants, DotGraph}
import scala.collection.JavaConversions._

/**
  * AcdfgToDotGraph
  *   Class implementing conversion from abstract control data flow graph (ACDFG)
  *   to .dot graph format.
  *
  *   @author Rhys Braginton Pettee Olsen <rhol9958@colorado.edu>
  *   @group  University of Colorado at Boulder CUPLV
  */

class AcdfgToDotGraph(acdfg : Acdfg) extends CFGToDotGraph {
  private def getDataNodeName(optNode : Option[Node]) : String = {
    val invokeeNode : Option[Node] = optNode
    val invokeeName = invokeeNode match {
      case Some(x) =>
        if (x.isInstanceOf[DataNode]) (x.asInstanceOf[DataNode]).name
        else "_"
      case None => "_"
    }
    invokeeName
  }

  def drawNode(canvas: DotGraph, id : Long) : scala.Unit = {
    def getNodeLine(id : Long) : String = {
      acdfg.getLine(id) match {
        case Some(lineNum) => s"${lineNum}"
        case _ => "na"
      }
    }
    val n : Node = acdfg.nodes(id)
    val nodeLine : String = getNodeLine(id)
    var dotNode : DotGraphNode = canvas.drawNode(id.toString)
    n match {
      case (node : DataNode) =>
        // "#" + id.toString + ": " + node.datatype.toString + " " + node.name
        val label = s"${node.name} : ${node.datatype} (# ${id})\\n${nodeLine}"
        dotNode.setLabel(label)
        dotNode.setStyle(DotGraphConstants.NODE_STYLE_DASHED)
        dotNode.setAttribute("shape", "ellipse")
        dotNode.setAttribute("group", "1")
      case (node : MethodNode) =>
        val (invokeeId, invokeeName) =
          if (node.invokee.isDefined) {
            val invokeeName = getDataNodeName(acdfg.nodes.get(node.invokee.get))
            (s"(#${node.invokee.get}).", s"${invokeeName}.")
          } else ("","")
        val initVal = (List[String](),List[String]())
        val (argsId, argsName) = node.argumentIds.foldLeft(initVal)(
          (pair, id : Long) => {
            val varName = getDataNodeName(acdfg.nodes.get(id))
            val lres = s"(#${id.toString()})" :: pair._1
            val rres = varName ::  pair._2
            (lres, rres)
          }
        )
        val argsNameList = argsName.mkString(",")
        val argsIdList = argsId.mkString(",")
        val label = s"${invokeeName}${node.name}(${argsNameList})"
        val labelId = s"${invokeeId}(#${node.id})(${argsIdList})"

        dotNode.setLabel(s"${label}\\n${labelId}\\n${nodeLine}")
        dotNode.setAttribute("group", "0")
        ()
      case (node : MiscNode) =>
        dotNode.setLabel(s"(#${id.toString})\\n${nodeLine}")
        dotNode.setAttribute("group", "0")
        ()
      case n => ()
    }
    ()
  }

  def drawEdge(canvas : DotGraph, e : Edge, id : Long, ignoreData : Boolean,
    edgeToDraw : List[Long]) : List[Long] = {
    e match {
      case (edge : DefEdge) => {
        if (! ignoreData) {
          var dotEdge : DotGraphEdge = canvas.drawEdge(e.from.toString, e.to.toString)
          dotEdge.setAttribute("color", "blue")
          edgeToDraw
        } else {
          id :: edgeToDraw
        }
      }
      case (edge : UseEdge) if (! ignoreData) => {
        var dotEdge : DotGraphEdge = canvas.drawEdge(e.from.toString, e.to.toString)
        dotEdge.setAttribute("color", "red")
        dotEdge.setAttribute("Damping", "0.7")
        edgeToDraw
      }
      case (edge : TransControlEdge) => edgeToDraw
      case (edge : ControlEdge) => {
        var dotEdge : DotGraphEdge = canvas.drawEdge(e.from.toString, e.to.toString)
        dotEdge.setAttribute("color", "black")
        dotEdge.setAttribute("Damping", "0.7")
        edgeToDraw
      }
      case (edge : ExceptionalControlEdge) => {
        var dotEdge : DotGraphEdge = canvas.drawEdge(e.from.toString, e.to.toString)
        dotEdge.setAttribute("color", "purple")
        dotEdge.setAttribute("Damping", "0.7")
        edgeToDraw
      }      
      case _ => edgeToDraw
    }
  }

  def draw() : DotGraph = {

    var canvas : DotGraph = initDotGraph(null)
    var sourceInfo = acdfg.getSourceInfo
    var acdfgLabel = if (null != sourceInfo && (sourceInfo.className != "" ||
      sourceInfo.methodName != ""))
      s"${sourceInfo.className}.${sourceInfo.methodName}"
    else "ACDFG"

    canvas.setGraphLabel(acdfgLabel)

    /* build a node -> edge map */
    val roots = scala.collection.mutable.HashSet[Long]()
    val node2edge = scala.collection.mutable.HashMap[Long, List[Long]]()
    acdfg.nodes.foreach { x => {
      node2edge += x._1 -> List[Long]()
      roots.add(x._1)
    }}
    acdfg.edges.foreach { (values : (Long, Edge)) => {
      val edge : Edge = values._2
      val edgeId : Long = values._1
      node2edge(edge.from) = edgeId :: node2edge(edge.from)
      if (roots.contains(edge.to)) roots.remove(edge.to)
    }}

    /* visit all the roots in dfs  */
    val visited = scala.collection.mutable.HashSet[Long]()
    def visitNodes(nodeId : Long, ignoreData : Boolean,
      edgeToDraw : List[Long]) : List[Long] = {

      if (! visited.contains(nodeId)) {
        val node : Node = acdfg.nodes(nodeId)
        if (! (node.isInstanceOf[DataNode] && ignoreData)) {
          visited.add(nodeId)
          drawNode(canvas, nodeId)
          if (node2edge.contains(nodeId)) {
            val edges : List[Long] = node2edge(nodeId)
            var newEdgeToDraw : List[Long] = edgeToDraw
            newEdgeToDraw = edges.foldLeft (newEdgeToDraw) { (edgeToDraw, edgeId) => {
              val edge = acdfg.edges(edgeId)
              val newEdge = drawEdge(canvas, edge, edgeId, ignoreData, edgeToDraw)
              visitNodes(edge.to, ignoreData, newEdge)
            }}
            newEdgeToDraw
          }
          else edgeToDraw
        }
        else edgeToDraw
      }
      else edgeToDraw
    }

    /* vusut all the control edges */
    var useEdgesToAdd : List[Long] = roots.foldLeft (List[Long]()) {
      (list, x) => visitNodes(x, true, list) }

    /* visit the rest */
    useEdgesToAdd = acdfg.nodes.foldLeft(useEdgesToAdd) {
      (list,x) => visitNodes(x._1, false, list) }

    /* draw the use edges */
    useEdgesToAdd.foreach { id => drawEdge(canvas, acdfg.edges(id), id, false,
      List[Long]())}

    canvas
  }

}
