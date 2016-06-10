package edu.colorado.plv.fixr.provenance

import java.io.{ByteArrayOutputStream, OutputStream}

import com.google.googlejavaformat.java.Formatter
import edu.colorado.plv.fixr.abstraction.{Acdfg, AcdfgToDotGraph}
import edu.colorado.plv.fixr.graphs.{CDFGToDotGraph, UnitCdfgGraph}

import scalatags.Text.TypedTag
import scalatags.Text.all._
import soot.Body
import soot.toolkits.graph.DirectedGraph
import soot.util.cfgcmd.CFGToDotGraph

import scalatags.stylesheet._

class Provenance(
  source : String,
  body : Body,
  slicedBody : Body,
  prefix : String,
  cfg : DirectedGraph[_],
  cdfg : UnitCdfgGraph,
  acdfg : Acdfg
) {
  def toHtml = {
    /*
    var cfgOut = new ByteArrayOutputStream()
    new CFGToDotGraph().drawCFG(cfg.asInstanceOf[DirectedGraph[_]], body).render(cfgOut, 2)
    val cfgDotString = cfgOut.toString

    var cdfgOut = new ByteArrayOutputStream()
    new CDFGToDotGraph().drawCFG(cdfg.asInstanceOf[UnitCdfgGraph], body).render(cdfgOut, 2)
    val cdfgDotString = cdfgOut.toString

    var acdfgOut = new ByteArrayOutputStream()
    new AcdfgToDotGraph(acdfg).draw().render(acdfgOut, 2)
    val acdfgDotString = acdfgOut.toString
    */
    scalatags.Text.all.html(
      scalatags.Text.all.head(
        // scalatags.Text.all.script(src:="http://requirejs.org/docs/release/2.2.0/minified/require.js"),
        scalatags.Text.all.script(src:="https://github.com/mdaines/viz.js/releases/download/v1.3.0/viz.js"),
        scalatags.Text.all.script(
          "var cfgReq = new XMLHttpRequest();\n" +
          "cfgReq.open('GET', '" + prefix + ".cfg.dot', true);\n" +
          "cfgReq.send();\n" +
          "cfgReq.onload = function() {\n" +
          "  console.log('Hoogabooga!');\n" +
          "  var cfgText = cfgReq.responseText;\n" +
          "  var cfgContainer = document.getElementById('cfg');\n" +
          "  var cfg = Viz(cfgText, options={ format: 'svg', engine: 'dot' });\n" +
          "  cfgContainer.innerHTML = cfg;\n" +
          "};\n" +
          "var cdfgReq = new XMLHttpRequest();\n" +
          "cdfgReq.open('GET', '" + prefix + ".cdfg.dot', true);\n" +
          "cdfgReq.send();\n" +
          "cdfgReq.onload = function() {\n" +
          "  console.log('Hoogabooga!');\n" +
          "  var cdfgText = cdfgReq.responseText;\n" +
          "  var cdfgContainer = document.getElementById('cdfg');\n" +
          "  var cdfg = Viz(cdfgText, options={ format: 'svg', engine: 'dot' });\n" +
          "  cdfgContainer.innerHTML = cdfg;\n" +
          "};\n" +
          "var acdfgReq = new XMLHttpRequest();\n" +
          "acdfgReq.open('GET', '" + prefix + ".acdfg.dot', true);\n" +
          "acdfgReq.send();\n" +
          "acdfgReq.onload = function() {\n" +
          "  console.log('Hoogabooga!');\n" +
          "  var acdfgText = acdfgReq.responseText;\n" +
          "  var acdfgContainer = document.getElementById('acdfg');\n" +
          "  var acdfg = Viz(acdfgText, options={ format: 'svg', engine: 'dot' });\n" +
          "  acdfgContainer.innerHTML = acdfg;\n" +
          "};\n"
        )
        // scalatags.Text.all.script(src:="//cdnjs.cloudflare.com/ajax/libs/highlight.js/9.4.0/highlight.min.js"),
        /*
        scalatags.Text.all.link(
          href:="//cdnjs.cloudflare.com/ajax/libs/highlight.js/9.4.0/styles/default.min.css"
        )
        */
      ),
      scalatags.Text.all.body(
        scalatags.Text.all.h1(cdfg.getBody.getMethod.getName + " Provenance Information"),
        /*
        scalatags.Text.all.div(
          body.toString.split("\n").iterator.toArray.map { a =>
            scalatags.Text.all.p(scalatags.Text.all.code(a))
          }
        )
        */
        scalatags.Text.all.h2("Java Source"),
        if (source != null) {
          scalatags.Text.all.pre(scalatags.Text.all.code(source))
        }
        else {
          scalatags.Text.all.p("Source code was not supplied.")
        },
        scalatags.Text.all.h2("Jimple"),
        scalatags.Text.all.pre(scalatags.Text.all.code(body.toString)),
        scalatags.Text.all.h2("Sliced Jimple"),
        scalatags.Text.all.pre(scalatags.Text.all.code(slicedBody.toString)),
        scalatags.Text.all.h2("Control Flow Graph (CFG)"),
        scalatags.Text.all.div(id := "cfg"),
        scalatags.Text.all.h2("Control Data Flow Graph (CDFG)"),
        scalatags.Text.all.div(id := "cdfg"),
        scalatags.Text.all.h2("Abstract Control Data Flow Graph (ACDFG)"),
        scalatags.Text.all.div(id := "acdfg")
      )
    )
  }
}
