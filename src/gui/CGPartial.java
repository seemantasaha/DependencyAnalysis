package gui;

import core.Procedure;
import java.util.Set;

/**
 *
 * @author zzk
 */
public class CGPartial extends BaseGraph<Procedure> {
  public CGPartial(Set<Procedure> procSet) {
    for (Procedure proc : procSet) {
      if (getVertex(proc) == null)
        addVertex(proc, 1, proc.getProcedureName());
      Set<Procedure> calleeSet = proc.getCalleeSet();
      for (Procedure callee : calleeSet) {
        if (!procSet.contains(callee))
          continue;
        if (getVertex(callee) == null)
          addVertex(callee, 1, callee.getProcedureName());
        addEdge(proc, callee, null);
      }
    }
    layoutGraph();
  }

  final public void paintProcedure(Procedure proc, String color) {
    colorVertex(proc, color);
  }
  
  final public void paintProcedureSet(Set<Procedure> procSet, String color) {
    for (Procedure proc : procSet)
      colorVertex(proc, color);
  }

  /*added by Madeline Sgro 7/10/2017
  scales vertices for every vertex in a procedure set by a given scaleFactor
  */
  final public void scaleProcedureSet(Set<Procedure> procSet, double scaleFactor) {
    for (Procedure proc : procSet){
      scaleVertex(proc, scaleFactor);
    }
  }
}
