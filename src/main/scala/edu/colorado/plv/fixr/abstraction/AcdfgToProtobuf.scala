package edu.colorado.plv.fixr.abstraction

import com.google.protobuf.{CodedInputStream, CodedOutputStream}
import edu.colorado.plv.fixr.protobuf.ProtoAcdfg

/**
 * AcdfgToProtobuf
 *   Class implementing conversion from abstract control data flow graph (ACDFG)
 *   to Protobuf format.
 *
 *   @author Rhys Braginton Pettee Olsen <rhol9958@colorado.edu>
 *   @group  University of Colorado at Boulder CUPLV
 */

class AcdfgToProtobuf(acdfg : Acdfg) {
  var builder : ProtoAcdfg.Acdfg.Builder = ProtoAcdfg.Acdfg.newBuilder()
  acdfg.edges.foreach {
    case (id : Long, edge : this.acdfg.ControlEdge) =>
      val protoControlEdge: ProtoAcdfg.Acdfg.ControlEdge.Builder =
        ProtoAcdfg.Acdfg.ControlEdge.newBuilder()
      protoControlEdge.setId(id)
      protoControlEdge.setFrom(edge.from)
      protoControlEdge.setTo(edge.to)
      builder.addControlEdge(protoControlEdge)
    case (id : Long, edge : this.acdfg.DefEdge) =>
      val protoDefEdge: ProtoAcdfg.Acdfg.DefEdge.Builder =
        ProtoAcdfg.Acdfg.DefEdge.newBuilder()
      protoDefEdge.setId(id)
      protoDefEdge.setFrom(edge.from)
      protoDefEdge.setTo(edge.to)
      builder.addDefEdge(protoDefEdge)
    case (id : Long, edge : this.acdfg.UseEdge) =>
      val protoUseEdge: ProtoAcdfg.Acdfg.UseEdge.Builder =
        ProtoAcdfg.Acdfg.UseEdge.newBuilder()
      protoUseEdge.setId(id)
      protoUseEdge.setFrom(edge.from)
      protoUseEdge.setTo(edge.to)
      builder.addUseEdge(protoUseEdge)
  }
  acdfg.nodes.foreach {
    case (id : Long, node : this.acdfg.DataNode) =>
      val protoDataNode : ProtoAcdfg.Acdfg.DataNode.Builder =
        ProtoAcdfg.Acdfg.DataNode.newBuilder()
      protoDataNode.setId(id)
      protoDataNode.setName(node.name)
      protoDataNode.setType(node.datatype)
      builder.addDataNode(protoDataNode)
    case (id : Long, node : this.acdfg.MiscNode) =>
      val protoMiscNode : ProtoAcdfg.Acdfg.MiscNode.Builder =
        ProtoAcdfg.Acdfg.MiscNode.newBuilder()
      protoMiscNode.setId(id)
      builder.addMiscNode(protoMiscNode)
    case (id : Long, node : this.acdfg.MethodNode) =>
      val protoMethodNode : ProtoAcdfg.Acdfg.MethodNode.Builder =
        ProtoAcdfg.Acdfg.MethodNode.newBuilder()
        protoMethodNode.setId(id)
        if (node.invokee.isDefined) {
          protoMethodNode.setInvokee(node.invokee.get)
        }
        node.argumentIds.foreach(protoMethodNode.addArgument)
        protoMethodNode.setName(node.name)
        builder.addMethodNode(protoMethodNode)
  }
  val protobuf = builder.build()

  def writeTo(output : CodedOutputStream) = protobuf.writeTo(output)
}
