package edu.colorado.plv.fixr.provenance

import com.google.googlejavaformat.java.Formatter
import edu.colorado.plv.fixr.abstraction.Acdfg
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import scalatags.Text.TypedTag
import scalatags.Text.all._
import soot.Body
import scalatags.stylesheet._

class Provenance(body : Body, cdfg : UnitCdfgGraph, acdfg : Acdfg) {
  def toHtml = {
    scalatags.Text.all.html(
      scalatags.Text.all.head(
        scalatags.Text.all.script("//cdnjs.cloudflare.com/ajax/libs/highlight.js/9.4.0/highlight.min.js"),
        scalatags.Text.all.link(
          href:="//cdnjs.cloudflare.com/ajax/libs/highlight.js/9.4.0/styles/default.min.css"
        )
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
        scalatags.Text.all.pre(scalatags.Text.all.code()),
        scalatags.Text.all.h2("Generated Jimple"),
        scalatags.Text.all.pre(scalatags.Text.all.code(body.toString)),
        scalatags.Text.all.h2("Control Data Flow Graph (CDFG)"),
        scalatags.Text.all.h2("Abstract Control Data Flow Graph (ACDFG)")
      )
    )
  }
}
