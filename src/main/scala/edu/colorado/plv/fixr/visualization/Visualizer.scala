package edu.colorado.plv.fixr.visualization

import java.io.FileInputStream

import edu.colorado.plv.fixr.abstraction.{TransControlEdge, _}
import edu.colorado.plv.fixr.protobuf.ProtoAcdfg
import edu.colorado.plv.fixr.protobuf.ProtoIso.Iso
import edu.colorado.plv.fixr.protobuf.ProtoAcdfg.Acdfg
import soot.util.dot._
import edu.colorado.plv.fixr.graphs.CFGToDotGraph.{DotNamer, NodeComparator}
import edu.colorado.plv.fixr.graphs.{CFGToDotGraph, UnitCdfgGraph}

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

  def drawEdge(e : (Long, Edge), canvas : DotGraph, graphNum : Int) = {
    //println("$$$ edge " + graphNum.toString + " id: " + e._2.id.toString)
    //println("$$$   edge " + graphNum.toString + " from: " + e._2.from.toString)
    //println("$$$   edge " + graphNum.toString + " to: " + e._2.to.toString)
    var dotEdge : DotGraphEdge = canvas.drawEdge(
      graphNum.toString + "_" + e._2.from.toString,
      graphNum.toString + "_" +  e._2.to.toString
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

  def drawNode(n : (Long, Node), canvas : DotGraph, graphNum : Int) = {
    //println("$$$ node " + graphNum.toString + "  id: " + n._2.id.toString)
    var dotNode : DotGraphNode = canvas.drawNode(graphNum.toString + "_" + n._1.toString)
    n match {
      case n@(id : Long, node : DataNode) =>
        dotNode.setLabel("#" + id.toString + ": " + node.datatype.toString + " " + node.name)
        dotNode.setStyle(DotGraphConstants.NODE_STYLE_DASHED)
        dotNode.setAttribute("shape", "ellipse")
        // println("$$$   node " + graphNum.toString + " name: " + "#" + id.toString + ": " +
        //  node.datatype.toString + " " + node.name)
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
        //println("$$$   node " + graphNum.toString + " name: " + name)
      case n@(id : Long, node : MiscNode) =>
        dotNode.setLabel("#" + id.toString)
        //println("$$$   node " + graphNum.toString + " name: #" + id.toString)
      case n => Nil
    }
  }

  def draw() : DotGraph = {

    var canvas : DotGraph = initDotGraph(null)

    canvas.setGraphLabel("ACDFG")
    var canvas1 = canvas.createSubGraph("ACDFG 1: " + protoIso.getGraph1Id)
    var canvas2 = canvas.createSubGraph("ACDFG 2: " + protoIso.getGraph2Id)

    canvas1.setGraphAttribute("rank", "same")
    canvas2.setGraphAttribute("rank", "same")

    // add contents of graph 1
    graph1.nodes.foreach {n => drawNode(n, canvas1, 1)}
    graph1.edges.foreach {e => drawEdge(e, canvas1, 1)}

    // add contents of graph 2
    graph2.nodes.foreach {n => drawNode(n, canvas2, 2)}
    graph2.edges.foreach {e => drawEdge(e, canvas2, 2)}

    for (e <- protoIso.getMapNodeList) {
      assert(this.graph1.nodes.contains(e.getId1))
      assert(this.graph2.nodes.contains(e.getId2))      
      var dotEdge : DotGraphEdge = canvas.drawEdge(
        "1_" + e.getId1.toString,
        "2_" + e.getId2.toString
      )
      dotEdge.setAttribute("color", "green")
      dotEdge.setAttribute("Damping", "0.8")
      dotEdge.setAttribute("style", "dotted")
    }
    canvas
  }
}
