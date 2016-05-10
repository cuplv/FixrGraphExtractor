package edu.colorado.plv.fixr.graphs;

import soot.Local;
import soot.Type;

/**
 * Data structure representing Soot locals annotated with types.
 *
 * @author Rhys Braginton Pettee Olsen
 */
public class TypedLocal {
    protected Local local = null;
    protected Type  type = null;

    public TypedLocal(Local local) {
        this.local = local;
        this.type  = local.getType();
    }

    public Type  getType()  { return this.type;  }
    public Local getLocal() { return this.local; }
}
