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
    val ghr = acdfg.getGitHubRecord

    scalatags.Text.all.html(
      scalatags.Text.all.head(
      ),
      scalatags.Text.all.body(
        scalatags.Text.all.h1(cdfg.getBody.getMethod.getName + " Provenance Information"),
        scalatags.Text.all.div(
          scalatags.Text.all.p(ghr.userName),
          scalatags.Text.all.p(ghr.repoName),
          scalatags.Text.all.a(
            href := ghr.url + "/commit/" + ghr.commitHash,
            ghr.url
          ),
          scalatags.Text.all.code(ghr.commitHash)
        ),
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
        scalatags.Text.all.div(id := "cfg",
          img("")
        ),
        scalatags.Text.all.h2("Control Data Flow Graph (CDFG)"),
        scalatags.Text.all.div(id := "cdfg",
          scalatags.Text.all.img(src := "")
        ),
        scalatags.Text.all.h2("Abstract Control Data Flow Graph (ACDFG)"),
        scalatags.Text.all.div(id := "acdfg",

        )
      )
    )
  }
}
