package edu.colorado.plv.fixr.provenance

import edu.colorado.plv.fixr.abstraction.Acdfg
import edu.colorado.plv.fixr.graphs.UnitCdfgGraph
import scalatags.Text.all._
import soot.Body

class Provenance(body : Body, cdfg : UnitCdfgGraph, acdfg : Acdfg) {
  def toHtml = {
    scalatags.Text.all.html(
      scalatags.Text.all.head(
        scalatags.Text.all.script("some script")
      ),
      scalatags.Text.all.body(
        scalatags.Text.all.h1("This is my title"),
        scalatags.Text.all.div(
          scalatags.Text.all.p("This is my first paragraph"),
          scalatags.Text.all.p("This is my second paragraph")
        )
      )
    )
    0
  }
}
