package edu.colorado.plv.fixr.visualization

import java.io.FileInputStream

import edu.colorado.plv.fixr.abstraction.{TransControlEdge, _}
import edu.colorado.plv.fixr.protobuf.ProtoAcdfg
import edu.colorado.plv.fixr.protobuf.ProtoIso.Iso
import edu.colorado.plv.fixr.protobuf.ProtoAcdfg.Acdfg
import soot.util.dot.{DotGraph, DotGraphConstants, DotGraphEdge, DotGraphNode}

import edu.colorado.plv.fixr.graphs.CFGToDotGraph.{DotNamer, NodeComparator}
import edu.colorado.plv.fixr.graphs.{UnitCdfgGraph, CFGToDotGraph}

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConverters._
import edu.colorado.plv.fixr.abstraction

/**
  * Created by cuplv on 8/4/16.
  */
class Visualizer(
    graph1Stream : FileInputStream,
    graph2Stream : FileInputStream,
    isoStream : FileInputStream
  ) extends CFGToDotGraph {
    val protoGraph1 : Acdfg = ProtoAcdfg.Acdfg.parseFrom(graph1Stream)
    val protoGraph2 : Acdfg = ProtoAcdfg.Acdfg.parseFrom(graph2Stream)
    val protoIso : Iso = Iso.parseFrom(isoStream)
    val graph1 : abstraction.Acdfg = new abstraction.Acdfg(protoGraph1)
    val graph2 : abstraction.Acdfg = new abstraction.Acdfg(protoGraph2)

  def draw() : DotGraph = {

    var canvas : DotGraph = initDotGraph(null)

    canvas.setGraphLabel("ACDFG")
    var canvas1 = canvas.createSubGraph("ACDFG 1: " + protoIso.getGraph1Id)
    var canvas2 = canvas.createSubGraph("ACDFG 2: " + protoIso.getGraph2Id)

    // add contents of graph 1
    for (n <- graph1.nodes) {
      var dotNode : DotGraphNode = canvas1.drawNode("1_" + n._1.toString)
      n match {
        case n@(id : Long, node : DataNode) =>
          dotNode.setLabel("#" + id.toString + ": " + node.datatype.toString + " " + node.name)
          dotNode.setStyle(DotGraphConstants.NODE_STYLE_DASHED)
          dotNode.setAttribute("shape", "ellipse")
        case n@(id : Long, node : MethodNode) =>
          var name : String = "#" + node.id.toString + ": "
          val arguments = node.argumentIds.map { case id =>
            if (id == 0) {
              name // + " [string constant]"
            } else {
              name + " [#" + id.toString + "]"
            }
          }
          name += node.name
          if (node.invokee.isDefined) {
            name += "[#" + node.invokee.get + "]"
          }
          name += "(" + arguments.mkString(",") + ")"
          dotNode.setLabel(name)
        case n@(id : Long, node : MiscNode) =>
          dotNode.setLabel("#" + id.toString)
        case n => Nil
      }
    }
    for (e <- graph1.edges) {
      var dotEdge : DotGraphEdge = canvas1.drawEdge(
        "1_" + e._2.from.toString, "1_" +  e._2.to.toString
      )
      e match {
        case e@(id : Long, edge : DefEdge) =>
          dotEdge.setAttribute("color", "blue")
        case e@(id : Long, edge : UseEdge) =>
          dotEdge.setAttribute("color", "red")
          dotEdge.setAttribute("Damping", "0.7")
        case e@(id : Long, edge : TransControlEdge) =>
          dotEdge.setAttribute("color", "gray58")
          dotEdge.setAttribute("Damping", "0.7")
          dotEdge.setAttribute("style", "dotted")
        case _ => null
      }
    }

    // add contents of graph 1
    for (n <- graph1.nodes) {
      var dotNode : DotGraphNode = canvas2.drawNode("2_" + n._1.toString)
      n match {
        case n@(id : Long, node : DataNode) =>
          dotNode.setLabel("#" + id.toString + ": " + node.datatype.toString + " " + node.name)
          dotNode.setStyle(DotGraphConstants.NODE_STYLE_DASHED)
          dotNode.setAttribute("shape", "ellipse")
        case n@(id : Long, node : MethodNode) =>
          var name : String = "#" + node.id.toString + ": "
          val arguments = node.argumentIds.map { case id =>
            if (id == 0) {
              name // + " [string constant]"
            } else {
              name + " [#" + id.toString + "]"
            }
          }
          name += node.name
          if (node.invokee.isDefined) {
            name += "[#" + node.invokee.get + "]"
          }
          name += "(" + arguments.mkString(",") + ")"
          dotNode.setLabel(name)
        case n@(id : Long, node : MiscNode) =>
          dotNode.setLabel("#" + id.toString)
        case n => Nil
      }
    }
    for (e <- graph1.edges) {
      var dotEdge : DotGraphEdge = canvas2.drawEdge(
        "2_" + e._2.from.toString, "2_" +  e._2.to.toString
      )
      e match {
        case e@(id : Long, edge : DefEdge) =>
          dotEdge.setAttribute("color", "blue")
        case e@(id : Long, edge : UseEdge) =>
          dotEdge.setAttribute("color", "red")
        case e@(id : Long, edge : TransControlEdge) =>
          dotEdge.setAttribute("color", "gray58")
          dotEdge.setAttribute("Damping", "0.5")
          dotEdge.setAttribute("style", "dotted")
        case _ => null
      }
    }

    for (e <- protoIso.getMapNodeList) {
      var dotEdge : DotGraphEdge = canvas.drawEdge(
      "1_" + e.getId1.toString,
      "2_" + e.getId2.toString
      )
      dotEdge.setAttribute("color", "olive")
      dotEdge.setAttribute("Damping", "0.8")
      dotEdge.setAttribute("style", "dotted")
    }
    canvas
  }
}
