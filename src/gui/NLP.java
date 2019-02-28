package gui;

import core.Loop;
import core.NestedLoop;
import core.Procedure;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author zzk
 */
public class NLP extends BaseGraph<Procedure> {  
  public NLP(NestedLoop nestedLoop, Set<Loop> filteredLoopSet) {
    Iterator<Loop> nestedLoopIter = nestedLoop.iterator();
    // this is safe, since a nested loop has at least one loop level
    Loop loop = nestedLoopIter.next();
    Procedure proc = loop.getProcedure();
    addVertex(proc, 1, proc.getProcedureName());
    
    if (!filteredLoopSet.contains(loop))
      colorVertex(proc, "yellow");
    else
      colorVertex(proc, "cyan");
    
    while (nestedLoopIter.hasNext()) {
      Loop subLoop = nestedLoopIter.next();
      Procedure subProc = subLoop.getProcedure();
      if (subProc == proc) {
        if (!filteredLoopSet.contains(subLoop))
          colorVertex(subProc, "yellow");
        loop = subLoop;
        continue;
      }
      addVertex(subProc, 1, subProc.getProcedureName());
      
      if (!filteredLoopSet.contains(subLoop))
        colorVertex(subProc, "yellow");
      else
        colorVertex(subProc, "cyan");
        
      Set<ArrayList<Procedure>> subLoopPathSet = loop.getSubsidiaryLoopPathSet(subLoop);
      for (ArrayList<Procedure> subLoopPath : subLoopPathSet) {
        Procedure tempProc = proc;
        for (Procedure pathProc : subLoopPath) {
          if (getVertex(pathProc) == null) {
            addVertex(pathProc, 1, pathProc.getProcedureName());
            colorVertex(pathProc, "pink");
          }
          addEdge(tempProc, pathProc, null);
          tempProc = pathProc;
        }
        addEdge(tempProc, subProc, null);
      }
      if (subLoopPathSet.isEmpty())
        addEdge(proc, subProc, null);
      
      loop = subLoop;
      proc = subProc;
    }
    layoutGraph();
  }
  
  final public void appendProcedureSet(Set<Procedure> procSet) {
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
}
