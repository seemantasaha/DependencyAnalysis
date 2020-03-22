package gui;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.ibm.wala.classLoader.IBytecodeMethod;
//import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.*;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import core.Loop;
import core.OtherAnalysis; //Added by M. Sgro 07/14/2017
import core.NestedLoop;
import core.NestedLoopAnalysis;
import core.NewObject;
import core.NewObjectAnalysis;
import core.Procedure;
import core.ProcedureDependenceGraph;
import core.Program;
import core.ProgramDependenceGraph;
import static core.ProgramDependenceGraph.getProcedureDependenceGraph;
import core.Recursion;
import core.Reporter;
import core.Statement;
import core.StatementType;
import jdk.internal.org.objectweb.asm.*;
import sun.applet.Main;

import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.IincInsnNode;
import jdk.internal.org.objectweb.asm.tree.InsnList;
import jdk.internal.org.objectweb.asm.tree.InsnNode;
import jdk.internal.org.objectweb.asm.tree.IntInsnNode;
import jdk.internal.org.objectweb.asm.tree.JumpInsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.LineNumberNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.MultiANewArrayInsnNode;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;
import vlab.cs.ucsb.edu.ModelCounter;


import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javafx.util.Pair;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

/**
 *
 * @author zzk
 */
public class MainFrame extends javax.swing.JFrame {

  /**
   * Creates new form AnalysisFrame
   * @param configInfo - configuration setup for Janalyzer
   */
  public MainFrame(ConfigInfo configInfo) throws InvalidClassFileException {
    initComponents();

    this.configInfo = configInfo;
    this.locationFrame = new LocationFrame(this);
    this.loopFilterFrame = new LoopFilterFrame();
    this.newFilterFrame = new LoopFilterFrame();

    this.recursiveBound = 1;

    //this.program.printLoopGroup();
    //loadClassHierarchy();

    loadCallGraph();
    loadNestedLoops();
    loadNewObjects();
    loadRecursions();

    cgSwitchToggleButtonActionPerformed(null);

    //this.program.printArgumentType();
  }

  /*****************************************************************************
   * Loader
   ****************************************************************************/
  /*
  private void loadClassHierarchy() {
    IClassHierarchy cha = this.program.getClassHierarchy();
    for (IClass cls : cha) {
      if (cls.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
        Set<String> mthNameSet = new TreeSet<>();
        boolean mainCls = false;
        for (IMethod mth : cls.getAllMethods()) {
          String mthName = mth.getName().toString() + mth.getDescriptor().toString();
          mthNameSet.add(mthName);
          if (mthName.equals("main([Ljava/lang/String;)V"))
            mainCls = true;
        }

        String clsName = mainCls ? "*" + cls.getName().toString() : cls.getName().toString();
        this.classHierarchyMap.put(clsName, mthNameSet);
      }
    }

    for (Map.Entry<String, Set<String>> chMapEnt : this.classHierarchyMap.entrySet())
      this.classComboBox.addItem(chMapEnt.getKey());
  }
  */

  private void loadCallGraph() throws InvalidClassFileException {
    this.callGraph = new CG();
    int numverOFProcedure = 0;
    HashSet<String> classNames = new HashSet<>();
    for (Procedure proc : Program.getProcedureSet()) {
      numverOFProcedure++;
      classNames.add(proc.getClassName());
      loadControlFlowGraph(proc);
      loadProcedureDependenceGraph(proc);
    }

    System.out.println("Number of Classes: " + classNames.size());
    System.out.println("Number of Methods: " + numverOFProcedure);
  }

  private void loadControlFlowGraph(Procedure proc) throws InvalidClassFileException {
    CFG ctrlFlowGraph = this.controlFlowGraphMap.get(proc);
    if (ctrlFlowGraph != null)
      return;
    ctrlFlowGraph = new CFG(proc);
    ctrlFlowGraph.paintNodeSet(proc.getNodeSet(), "aquamarine");
    this.controlFlowGraphMap.put(proc, ctrlFlowGraph);
    System.out.println(proc.getFullSignature());
    this.jsonMap.put(proc.getFullSignature(), ctrlFlowGraph.getJSON());
    recursiveBoundMap.put(proc.getFullSignature(), 1);
  }

  private void loadProcedureDependenceGraph(Procedure proc) {
    PDG procDepGraph = this.procedureDependenceGraphMap.get(proc);
    if (procDepGraph != null)
      return;
    procDepGraph = new PDG(proc);
    this.procedureDependenceGraphMap.put(proc, procDepGraph);
  }

  private void loadNestedLoops() {
    this.nestedLoopListMap.clear();
    Set<Loop> filteredLoopSet = this.loopFilterFrame.getFilteredLoopSet();

    Set<NestedLoop> nestedLoopSet = NestedLoopAnalysis.getInterproceduralNestedLoopSet();
    for (NestedLoop nestedLoop : nestedLoopSet) {
      // take into account the filtered loops
      int levelCnt = 0;
      for (Loop loop : nestedLoop)
        if (!filteredLoopSet.contains(loop))
          levelCnt++;
      if (levelCnt == 0)
        continue;

      List<NestedLoop> nestedLoopGroup = this.nestedLoopListMap.get(levelCnt);
      if (nestedLoopGroup == null) {
        nestedLoopGroup = new ArrayList<>();
        this.nestedLoopListMap.put(levelCnt, nestedLoopGroup);
      }
      nestedLoopGroup.add(nestedLoop);
    }
  }

  private void loadNewObjects() {
    Set<NewObject> newObjSet = NewObjectAnalysis.getNewObjectSet();
    for (NewObject newObj : newObjSet) {
      List<NestedLoop> nestedLoopList = NewObjectAnalysis.getInterproceduralNestedLoopList(newObj);
      for (NestedLoop nestedLoop : nestedLoopList) {
        int levelCnt = nestedLoop.size();
        List<NewObject> newObjList = this.newObjectListMap.get(levelCnt);
        if (newObjList == null) {
          newObjList = new ArrayList<>();
          this.newObjectListMap.put(levelCnt, newObjList);
        }
        newObjList.add(newObj);
      }
    }
  }

  private void loadRecursions() {
    Set<Recursion> recursionSet = Program.getRecursionSet();
    int groupNum = 1;
    for (Recursion recursion : recursionSet) {
      this.recursionMap.put(groupNum, recursion);
      ++groupNum;
    }
  }

  /*****************************************************************************
   * Changer
   ****************************************************************************/
  private void listNestedLoops() {
    this.loopComboBox.removeAllItems();
    this.loopComboBox.addItem("");

    Set<Loop> filteredLoopSet = this.loopFilterFrame.getFilteredLoopSet();

    for (Map.Entry<Integer, List<NestedLoop>> nestedLoopListMapEnt : this.nestedLoopListMap.entrySet()) {
      int levelCnt = nestedLoopListMapEnt.getKey();
      List<NestedLoop> nestedLoopList = nestedLoopListMapEnt.getValue();
      for (int i = 0; i < nestedLoopList.size(); i++) {
        NestedLoop nestedLoop = nestedLoopList.get(i);
        boolean suspicious = false;
        for (Loop loop : nestedLoop) {
          if (filteredLoopSet.contains(loop))
            continue;
          if (loop.isLoopSuspicious()) {
            suspicious = true;
            break;
          }
        }

        if (suspicious)
          this.loopComboBox.addItem(levelCnt + "-level: loop-" + (i + 1) + "*");
        else
          this.loopComboBox.addItem(levelCnt + "-level: loop-" + (i + 1));
      }
    }
  }

  private void listNewObjects() {
    this.newComboBox.removeAllItems();
    this.newComboBox.addItem("");
    for (Map.Entry<Integer, List<NewObject>> newObjListMapEnt : this.newObjectListMap.entrySet()) {
      int levelCnt = newObjListMapEnt.getKey();
      List<NewObject> newObjList = newObjListMapEnt.getValue();
      for (int i = 0; i < newObjList.size(); i++)
        this.newComboBox.addItem(levelCnt + "-level: new-" + (i + 1));
    }
  }

  private void listRecursions() {
    this.recursionComboBox.removeAllItems();
    this.recursionComboBox.addItem("");
    for (Map.Entry<Integer, Recursion> recursionMapEnt : this.recursionMap.entrySet()) {
      int groupNum = recursionMapEnt.getKey();
      this.recursionComboBox.addItem("group-" + groupNum);
    }
  }

  /*
  Added by Madeline Sgro 07/13/2017
  resets the otherComboBox if another comboBox is added
  */
  private void listOther() {
    this.otherComboBox.setSelectedIndex(0);
    this.otherList = null;
  }

  private void fillNestedLoopInfo(NestedLoop nestedLoop) {
    Set<Loop> filteredLoopSet = this.loopFilterFrame.getFilteredLoopSet();

    this.currentNestedLoop = nestedLoop;
    DefaultListModel listModel = (DefaultListModel)this.infoList.getModel();

    int level = 1;
    for (Loop loop : nestedLoop) {
      if (filteredLoopSet.contains(loop))
        continue;

      Procedure proc = loop.getProcedure();
      if (loop.isLoopSuspicious())
        listModel.addElement("*Level " + level + ": " + loop.getLoopIdentifier() + " in " + proc.getProcedureName());
      else
        listModel.addElement("Level " + level + ": " + loop.getLoopIdentifier() + " in " + proc.getProcedureName());

      level++;
    }
  }

  private void fillNewObjectInfo(NewObject newObj) {
    DefaultListModel listModel = (DefaultListModel)this.infoList.getModel();
    Iterator<SSAInstruction> instIter = newObj.getNewNode().iterator();
    while (instIter.hasNext()) {
      SSAInstruction inst = instIter.next();
      if (inst instanceof SSANewInstruction)
        listModel.addElement(inst);
    }
  }

  /*
  Added by Madeline Sgro 07/14/2017
  fills the infoList Jlist with methods from the passed in Set of procedures
  */
  private void fillOtherInfo(Set<Procedure> other){
    DefaultListModel listModel = (DefaultListModel)this.infoList.getModel();
    List<Procedure> toOther = new ArrayList<Procedure>();

    if (other.isEmpty()) {
      return;
    }

    for (Procedure proc : other){
      listModel.addElement(proc.getProcedureName());
      toOther.add(proc); //added 07/17/2017
    }

    this.otherList = toOther;
    this.graphPanel.add(this.infoScrollPane, BorderLayout.EAST);
  }

  private void fillSSAInfo(Procedure proc) {
    DefaultListModel listModel = (DefaultListModel)this.infoList.getModel();
    IR ir = proc.getIR();
    SymbolTable symTab = ir.getSymbolTable();
    for (int i = 0; i < symTab.getMaxValueNumber(); i++) {
      int vn = i + 1;
      String info = "v" + vn;
      if (symTab.isParameter(vn)) {
        info += "@param:";
        if (!ir.getMethod().isStatic() && vn == 1)
          info += "this";
      } else if (symTab.isConstant(vn)) {
        info += "@const:";
        if (symTab.isBooleanConstant(vn)) {
          if (symTab.isTrue(vn))
            info += "TRUE";
          else
            info += "FALSE";
        } else if (symTab.isStringConstant(vn)) {
          info += "\"" + symTab.getStringValue(vn) + "\"";
        } else if (symTab.isNumberConstant(vn)) {
          if (symTab.isIntegerConstant(vn))
            info += symTab.getIntValue(vn);
          else if (symTab.isLongConstant(vn))
            info += symTab.getLongValue(vn);
          else if (symTab.isFloatConstant(vn))
            info += symTab.getFloatValue(vn);
          else if (symTab.isDoubleConstant(vn))
            info += symTab.getDoubleValue(vn);
        } else if (symTab.isNullConstant(vn)) {
          info += "null";
        }
      }

      //if (defUse.getDef(i) == null && defUse.getNumberOfUses(i) == 0)
      //listModel.addElement("v" + i);

      listModel.addElement(info);
    }

  }

  private void scrollToProcedure(Procedure proc) {
    mxGraphComponent graphComponent = (mxGraphComponent)this.graphPanel.getComponent(0);
    if (this.currentCG == null)
      return;
    Object procVertex = this.currentCG.getVertex(proc);
    if (procVertex != null)
      graphComponent.scrollCellToVisible(procVertex);
  }

  private void drawCallGraph() {
    // clear JPanel
    this.graphPanel.removeAll();
    this.graphPanel.revalidate();
    this.graphPanel.repaint();

    if (this.currentCG == null)
      return;

    mxGraph graph = this.currentCG.getGraph();
    mxGraphComponent graphComponent = new mxGraphComponent(graph);
    graphComponent.setConnectable(false);

    graphComponent.getGraphControl().addMouseListener(new CGMouseAdapter());

    this.graphPanel.setLayout(new BorderLayout());
    this.graphPanel.add(graphComponent, BorderLayout.CENTER);
  }

  /*Modified by Madeline Sgro 07/07/2017
  added a mouseListener for the JPanel to enable selecting variables off the
  JPanel and highlighting them on the CFG
  */
  private void drawControlFlowGraph(Procedure proc) {
    // clear JPanel
    this.graphPanel.removeAll();
    this.graphPanel.revalidate();
    this.graphPanel.repaint();

    this.currentCFG = this.controlFlowGraphMap.get(proc);
    mxGraph graph = this.currentCFG.getGraph();
    mxGraphComponent graphComponent = new mxGraphComponent(graph);
    graphComponent.setConnectable(false);

    graphComponent.getGraphControl().addMouseListener(new CFGMouseAdapter());
    infoList.addMouseListener(new InfoListMouseAdapter()); //edit by Madeline Sgro

    this.graphPanel.setLayout(new BorderLayout());
    this.graphPanel.add(graphComponent, BorderLayout.CENTER);
  }

  private void drawProcedureDependenceGraph(Procedure proc) {
    // clear JPanel
    this.graphPanel.removeAll();
    this.graphPanel.revalidate();
    this.graphPanel.repaint();

    this.currentPDG = this.procedureDependenceGraphMap.get(proc);
    mxGraph graph = this.currentPDG.getGraph();
    mxGraphComponent graphComponent = new mxGraphComponent(graph);
    graphComponent.setConnectable(false);

    graphComponent.getGraphControl().addMouseListener(new PDGMouseAdapter());

    this.graphPanel.setLayout(new BorderLayout());
    this.graphPanel.add(graphComponent, BorderLayout.CENTER);
  }

  private void showNestedLoopPath(NestedLoop nestedLoop) {
    Set<Loop> filteredLoopSet = this.loopFilterFrame.getFilteredLoopSet();

    if (this.cgSwitchToggleButton.isSelected()) {
      NLP nlp = new NLP(nestedLoop, filteredLoopSet);
      this.currentCG = nlp;
    } else {
      this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
      this.callGraph.paintNestedLoop(nestedLoop, filteredLoopSet);

      // purple the tail
      Loop lastLoop = nestedLoop.getLast();
      Procedure lastProc = lastLoop.getProcedure();
      Set<ISSABasicBlock> callNodeSet = lastProc.getTopLevelCallNodeSet(lastLoop);
      for (ISSABasicBlock callNode : callNodeSet) {
        Set<Procedure> reachCalleeSet = lastProc.getReachableCalleeSet(callNode);
        this.callGraph.paintProcedureSet(reachCalleeSet, "purple");
      }

      this.currentCG = this.callGraph;
    }

    drawCallGraph();

    DefaultListModel listModel = new DefaultListModel();
    this.infoList.setModel(listModel);
    fillNestedLoopInfo(nestedLoop);
    this.graphPanel.add(this.infoScrollPane, BorderLayout.EAST);
  }

  private void showNewObjectPath(NewObject newObj, int levelCnt, int num) {
    Set<Loop> filteredLoopSet = this.newFilterFrame.getFilteredLoopSet();

    // for a levelCnt, e.g. 5-level, there may be several nested-loops leading to this new node
    // num and tempNum are used to identify which nested-loop is to draw
    int tempNum = 0;
    List<NestedLoop> nestedLoopSet = NewObjectAnalysis.getInterproceduralNestedLoopList(newObj);
    for (NestedLoop nestedLoop : nestedLoopSet) {
      if (nestedLoop.size() != levelCnt)
        continue;

      if (tempNum < num) {
        tempNum++;
        continue;
      }

      Loop lastLoop = nestedLoop.getLast();
      Procedure lastLoopProc = lastLoop.getProcedure();
      Set<ISSABasicBlock> callNodeSet = lastLoopProc.getTopLevelCallNodeSet(lastLoop);
      Set<Procedure> reachProcSet = new HashSet<>();
      for (ISSABasicBlock callNode : callNodeSet)
        reachProcSet.addAll(lastLoopProc.getReachableCalleeSet(callNode));

      Set<Procedure> trailProcSet = new HashSet<>();
      Set<Procedure> newProcSet = Program.getProcedureSet(newObj.getNewNode());
      for (Procedure reachProc : reachProcSet) {
        Set<Procedure> reachCalleeSet = reachProc.getReachableCalleeSet();
        reachCalleeSet.retainAll(newProcSet);
        if (!reachCalleeSet.isEmpty())
          trailProcSet.add(reachProc);
      }
      trailProcSet.add(lastLoopProc);
      trailProcSet.addAll(newProcSet);

      if (this.cgSwitchToggleButton.isSelected()) {
        NLP nlp = new NLP(nestedLoop, filteredLoopSet);
        nlp.appendProcedureSet(trailProcSet);
        this.currentCG = nlp;
      } else {
        this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
        this.callGraph.paintNestedLoop(nestedLoop, filteredLoopSet);
        this.currentCG = this.callGraph;
      }

      for (Procedure newProc : newProcSet)
        this.currentCG.colorVertex(newProc, "red");

      drawCallGraph();

      DefaultListModel listModel = new DefaultListModel();
      this.infoList.setModel(listModel);
      fillNestedLoopInfo(nestedLoop);
      fillNewObjectInfo(newObj);
      this.graphPanel.add(this.infoScrollPane, BorderLayout.EAST);

      break;
    }
  }

  private void showRecursion(Recursion recursion) {
    Set<Procedure> recursionBody = recursion.getRecursionBody();
    if (this.cgSwitchToggleButton.isSelected()) {
      CGPartial cgPartial = new CGPartial(recursionBody);
      cgPartial.paintProcedureSet(recursionBody, "yellow");
      this.currentCG = cgPartial;
    } else {
      this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
      this.callGraph.paintProcedureSet(recursionBody, "yellow");
      this.currentCG = this.callGraph;
    }

    drawCallGraph();

    // bring view to at least one entry of this recursion group
    Procedure oneEntry = recursion.getRecursionEntrySet().iterator().next();
    scrollToProcedure(oneEntry);
  }

  private void showControlFlowGraph(Procedure proc) {
    drawControlFlowGraph(proc);
    DefaultListModel listModel = new DefaultListModel();
    this.infoList.setModel(listModel);
    fillSSAInfo(proc);
    this.graphPanel.add(this.infoScrollPane, BorderLayout.EAST);
  }

  private void showProcedureDependecenGraph(Procedure proc) {
    drawProcedureDependenceGraph(proc);
  }

  void showjBondResult(Map<Double, Set<Procedure>> procSetMap) {
    if (this.cgSwitchToggleButton.isSelected()) {
      Set<Procedure> procSet = new HashSet<>();
      for (Map.Entry<Double, Set<Procedure>> procSetMapEnt : procSetMap.entrySet())
        procSet.addAll(procSetMapEnt.getValue());
      CGPartial cgPartial = new CGPartial(procSet);
      this.currentCG = cgPartial;
    } else {
      this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
      this.currentCG = this.callGraph;
    }

    int diffExecTimeCnt = procSetMap.size();
    int quarter = diffExecTimeCnt / 4;
    int i = 0;
    for (Map.Entry<Double, Set<Procedure>> execTimeProcSetMapEnt : procSetMap.entrySet()) {
      Set<Procedure> execTimeProcSet = execTimeProcSetMapEnt.getValue();
      if (i < quarter) {
        for (Procedure proc : execTimeProcSet)
          this.currentCG.colorVertex(proc, "#FFA07A");
      } else if (i < 2 * quarter) {
        for (Procedure proc : execTimeProcSet)
          this.currentCG.colorVertex(proc, "#FA8072");
      } else if (i < 3 * quarter) {
        for (Procedure proc : execTimeProcSet)
          this.currentCG.colorVertex(proc, "#FF0000");
      } else {
        for (Procedure proc : execTimeProcSet)
          this.currentCG.colorVertex(proc, "#8B0000");
      }
      i++;
    }

    drawCallGraph();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    cfgOptPopupMenu = new javax.swing.JPopupMenu();
    showPDGMenuItem = new javax.swing.JMenuItem();
    silceMenuItem = new javax.swing.JMenuItem();
    optSeparator1 = new javax.swing.JPopupMenu.Separator();
    retMenuItem = new javax.swing.JMenuItem();
    showJsonMenuitem = new javax.swing.JMenuItem();
    branchModelCountMenuitem = new javax.swing.JMenuItem();
    resetSlicing = new javax.swing.JMenuItem();
    invokeCoCoChannel = new javax.swing.JMenuItem();
    instrumentSecretDepBranch = new javax.swing.JMenuItem();
    pdgOptPopupMenu = new javax.swing.JPopupMenu();
    backMenuItem = new javax.swing.JMenuItem();
    fileChooser = new javax.swing.JFileChooser();
    mainPanel = new javax.swing.JPanel();
    loopComboBox = new javax.swing.JComboBox();
    newComboBox = new javax.swing.JComboBox();
    recursionComboBox = new javax.swing.JComboBox();
    loopLabel = new javax.swing.JLabel();
    newLabel = new javax.swing.JLabel();
    recursionLabel = new javax.swing.JLabel();
    cgSwitchToggleButton = new javax.swing.JToggleButton();
    filterButton = new javax.swing.JButton();
    locateButton = new javax.swing.JButton();
    runspfButton = new javax.swing.JButton();
    branchButton = new javax.swing.JButton();
    loadspfButton = new javax.swing.JButton();
    jBondButton = new javax.swing.JButton();
    jBondShowAllCheckBox = new javax.swing.JCheckBox();
    jBondFeatureComboBox = new javax.swing.JComboBox();
    jBondFeatureLabel = new javax.swing.JLabel();
    zoomComboBox = new javax.swing.JComboBox<>();
    zoomLabel = new javax.swing.JLabel();
    otherComboBox = new javax.swing.JComboBox<>();
    otherLabel = new javax.swing.JLabel();
    graphPanel = new javax.swing.JPanel();
    infoScrollPane = new javax.swing.JScrollPane();
    infoList = new javax.swing.JList();

    showPDGMenuItem.setText("Show PDG");
    showPDGMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        showPDGMenuItemActionPerformed(evt);
      }
    });
    showPDGMenuItem.setEnabled(false);
    cfgOptPopupMenu.add(showPDGMenuItem);

    silceMenuItem.setText("Slice Program");
    silceMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        silceMenuItemActionPerformed(evt);
      }
    });
    cfgOptPopupMenu.add(silceMenuItem);
    cfgOptPopupMenu.add(optSeparator1);

    retMenuItem.setText("Return to CG");
    retMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        retMenuItemActionPerformed(evt);
      }
    });
    cfgOptPopupMenu.add(retMenuItem);

    showJsonMenuitem.setText("Show JSON");
    showJsonMenuitem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        showJsonMenuitemActionPerformed(evt);
      }
    });
    cfgOptPopupMenu.add(showJsonMenuitem);

    branchModelCountMenuitem.setText("Markov Chain");
    branchModelCountMenuitem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        branchModelCountMenuitemActionPerformed(evt);
      }
    });
    cfgOptPopupMenu.add(branchModelCountMenuitem);

    resetSlicing.setText("Reset Slicing");
    resetSlicing.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        resetSlicingMenuitemActionPerformed(evt);
      }
    });
    cfgOptPopupMenu.add(resetSlicing);

    invokeCoCoChannel.setText("Invoke CoCo-Channel");
    invokeCoCoChannel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        invokeCoCoChannelMenuitemActionPerformed(evt);
      }
    });
    cfgOptPopupMenu.add(invokeCoCoChannel);

    instrumentSecretDepBranch.setText("Instrument Secret Dependent Branch");
    instrumentSecretDepBranch.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        instrumentSecretDepbranchMenuitemActionPerformed(evt);
      }
    });
    cfgOptPopupMenu.add(instrumentSecretDepBranch);

    backMenuItem.setText("Go Back");
    backMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        backMenuItemActionPerformed(evt);
      }
    });
    pdgOptPopupMenu.add(backMenuItem);

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

    mainPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
    mainPanel.setPreferredSize(new java.awt.Dimension(1217, 120));

    loopComboBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        loopComboBoxActionPerformed(evt);
      }
    });

    newComboBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        newComboBoxActionPerformed(evt);
      }
    });

    recursionComboBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        recursionComboBoxActionPerformed(evt);
      }
    });

    loopLabel.setText("Loops");

    newLabel.setText("News");

    recursionLabel.setText("Recursion");

    cgSwitchToggleButton.setText("CG ON");
    cgSwitchToggleButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cgSwitchToggleButtonActionPerformed(evt);
      }
    });

    filterButton.setText("Add Filter");
    filterButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        filterButtonActionPerformed(evt);
      }
    });

    locateButton.setText("Locate");
    locateButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        locateButtonActionPerformed(evt);
      }
    });

    runspfButton.setText("Run SPF");
    runspfButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        runspfButtonActionPerformed(evt);
      }
    });

    branchButton.setText("Branch");
    branchButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        branchButtonActionPerformed(evt);
      }
    });

    loadspfButton.setText("Load SPF");
    loadspfButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        loadspfButtonActionPerformed(evt);
      }
    });

    jBondButton.setText("jBond");
    jBondButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jBondButtonActionPerformed(evt);
      }
    });

    jBondShowAllCheckBox.setSelected(true);
    jBondShowAllCheckBox.setText("Show All");
    jBondShowAllCheckBox.setEnabled(false);
    jBondShowAllCheckBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jBondShowAllCheckBoxActionPerformed(evt);
      }
    });

    jBondFeatureComboBox.setEnabled(false);
    jBondFeatureComboBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jBondFeatureComboBoxActionPerformed(evt);
      }
    });

    jBondFeatureLabel.setText("Feature");

    zoomComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "50%", "75%", "90%", "100%", "110%", "125%", "150%", "200%", "300%", "500%" }));
    zoomComboBox.setSelectedIndex(3);
    zoomComboBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        zoomComboBoxActionPerformed(evt);
      }
    });

    zoomLabel.setText("Zoom");

    otherComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "  ", "Show Modulo", "Show String Comparison" }));
    otherComboBox.setSelectedItem(otherComboBox.getSelectedItem());
    otherComboBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        otherComboBoxActionPerformed(evt);
      }
    });

    otherLabel.setText("Other");

    javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
    mainPanel.setLayout(mainPanelLayout);
    mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addComponent(locateButton)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(newLabel))
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addComponent(cgSwitchToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(24, 24, 24)
                                            .addComponent(loopLabel)))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addComponent(loopComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(18, 18, 18)
                                            .addComponent(recursionLabel))
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addComponent(newComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(otherLabel)))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(otherComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(recursionComboBox, 0, 210, Short.MAX_VALUE))
                            .addGap(18, 18, 18)
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addComponent(filterButton)
                                            .addGap(18, 18, 18)
                                            .addComponent(runspfButton, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addComponent(branchButton)
                                            .addGap(18, 18, 18)
                                            .addComponent(loadspfButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGap(18, 18, 18)
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addComponent(jBondShowAllCheckBox)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(zoomLabel)
                                            .addGap(18, 18, 18)
                                            .addComponent(zoomComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addComponent(jBondButton)
                                            .addGap(24, 24, 24)
                                            .addComponent(jBondFeatureLabel)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jBondFeatureComboBox, 0, 179, Short.MAX_VALUE)))
                            .addContainerGap())
    );

    mainPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cgSwitchToggleButton, locateButton});

    mainPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {branchButton, filterButton, jBondButton, runspfButton});

    mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(loopLabel)
                                    .addComponent(loopComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cgSwitchToggleButton)
                                    .addComponent(filterButton)
                                    .addComponent(runspfButton)
                                    .addComponent(recursionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(recursionLabel)
                                    .addComponent(jBondButton)
                                    .addComponent(jBondFeatureComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jBondFeatureLabel))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(newComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(newLabel)
                                    .addComponent(locateButton)
                                    .addComponent(branchButton)
                                    .addComponent(loadspfButton)
                                    .addComponent(jBondShowAllCheckBox)
                                    .addComponent(zoomComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(zoomLabel)
                                    .addComponent(otherComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(otherLabel))
                            .addContainerGap(14, Short.MAX_VALUE))
    );

    infoScrollPane.setMaximumSize(new java.awt.Dimension(250, 130));
    infoScrollPane.setMinimumSize(new java.awt.Dimension(250, 130));
    infoScrollPane.setPreferredSize(new java.awt.Dimension(250, 130));

    infoList.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseReleased(java.awt.event.MouseEvent evt) {
        infoListMouseReleased(evt);
      }
    });
    infoScrollPane.setViewportView(infoList);

    javax.swing.GroupLayout graphPanelLayout = new javax.swing.GroupLayout(graphPanel);
    graphPanel.setLayout(graphPanelLayout);
    graphPanelLayout.setHorizontalGroup(
            graphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, graphPanelLayout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(infoScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
    );
    graphPanelLayout.setVerticalGroup(
            graphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(graphPanelLayout.createSequentialGroup()
                            .addComponent(infoScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 260, Short.MAX_VALUE))
    );

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1243, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(graphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addContainerGap())
    );
    layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                            .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(graphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void loopComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loopComboBoxActionPerformed
    String chosenStr = (String)this.loopComboBox.getSelectedItem();
    if (chosenStr == null || chosenStr.isEmpty()) {
      this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
      drawCallGraph();
      return;
    }
    if (this.newComboBox.getSelectedIndex() != 0)
      listNewObjects();
    else if (this.recursionComboBox.getSelectedIndex() != 0)
      listRecursions();
    else if (this.otherComboBox.getSelectedIndex() != 0) //added by M. Sgro 7/13/2017
      listOther();                                       //end addition

    Pattern pattern = Pattern.compile("(\\d+)");
    Matcher matcher = pattern.matcher(chosenStr);
    matcher.find();
    int levelCnt = Integer.parseInt(matcher.group());
    matcher.find();
    int loopNum = Integer.parseInt(matcher.group()) - 1;
    List<NestedLoop> nestedLoopList = this.nestedLoopListMap.get(levelCnt);
    NestedLoop nestedLoop = nestedLoopList.get(loopNum);
    showNestedLoopPath(nestedLoop);
    this.focus = CGFocus.Loop;
  }//GEN-LAST:event_loopComboBoxActionPerformed

  private void newComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newComboBoxActionPerformed
    String chosenStr = (String)this.newComboBox.getSelectedItem();
    if (chosenStr == null || chosenStr.isEmpty()) {
      this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
      drawCallGraph();
      return;
    }
    if (this.loopComboBox.getSelectedIndex() != 0)
      listNestedLoops();
    else if (this.recursionComboBox.getSelectedIndex() != 0)
      listRecursions();
    else if (this.otherComboBox.getSelectedIndex() != 0) //added by M. Sgro 7/13/2017
      listOther();                                       //end addition

    Pattern pattern = Pattern.compile("(\\d+)");
    Matcher matcher = pattern.matcher(chosenStr);
    matcher.find();
    int levelCnt = Integer.parseInt(matcher.group());
    matcher.find();
    int newNum = Integer.parseInt(matcher.group()) - 1;
    List<NewObject> newObjList = this.newObjectListMap.get(levelCnt);
    NewObject newObj = newObjList.get(newNum);
    int num = 0;
    for (int i = 0; i < newNum; i++)
      if (newObjList.get(i) == newObj)
        num++;
    showNewObjectPath(newObj, levelCnt, num);
    this.focus = CGFocus.New;
  }//GEN-LAST:event_newComboBoxActionPerformed

  private void recursionComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recursionComboBoxActionPerformed
    String chosenStr = (String)this.recursionComboBox.getSelectedItem();
    if (chosenStr == null || chosenStr.isEmpty()) {
      this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
      drawCallGraph();
      return;
    }
    if (this.loopComboBox.getSelectedIndex() != 0)
      listNestedLoops();
    else if (this.newComboBox.getSelectedIndex() != 0)
      listNewObjects();
    else if (this.otherComboBox.getSelectedIndex() != 0) //added by M. Sgro 7/13/2017
      listOther();                                       //end addition

    Pattern pattern = Pattern.compile("(\\d+)");
    Matcher matcher = pattern.matcher(chosenStr);
    matcher.find();
    int groupNum = Integer.parseInt(matcher.group());
    Recursion recursion = this.recursionMap.get(groupNum);
    showRecursion(recursion);
    this.focus = CGFocus.Recursion;
  }//GEN-LAST:event_recursionComboBoxActionPerformed

  /*
  modified by Madeline Sgro 07/17/2017
  JList side panel now works for options selected from in otherComboBox
  */
  private void infoListMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_infoListMouseReleased
    /*int index = this.infoList.getSelectedIndex();
    if (this.currentNestedLoop == null || this.currentNestedLoop.size() <= index || index < 0)
      return;
    Loop loop = this.currentNestedLoop.get(index);
    Procedure proc = loop.getProcedure();
    scrollToProcedure(proc); */

    int index = this.infoList.getSelectedIndex();
    if (index >= 0){
      if (this.otherList != null){
        Procedure proc = this.otherList.get(index);
        scrollToProcedure(proc);
      } else if (this.currentNestedLoop != null && this.currentNestedLoop.size() > index){
        Loop loop = this.currentNestedLoop.get(index);
        Procedure proc = loop.getProcedure();
        scrollToProcedure(proc);
      } else {
        return;
      }
    }
  }//GEN-LAST:event_infoListMouseReleased

  /*
  Modified by Madeline Sgro 07/07/2017
  Clicking "CG ON" now sets the CFG to null as well
  This allowed the zoomComboBoxAction method to function correctly for both
  the CG and CFG
  */
  private void cgSwitchToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cgSwitchToggleButtonActionPerformed
    if (this.cgSwitchToggleButton.isSelected()) {
      this.cgSwitchToggleButton.setText("CG OFF");
      this.currentCG = null;
      this.currentCFG = null; //edit by M. Sgro
    } else {
      this.cgSwitchToggleButton.setText("CG ON");
      this.currentCG = this.callGraph;
    }
    this.focus = CGFocus.Null;
    listNestedLoops();
    listNewObjects();
    listRecursions();
    listOther(); //edit by M. Sgro
    drawCallGraph();
  }//GEN-LAST:event_cgSwitchToggleButtonActionPerformed

  /*
  Modified by Thomas Marosz 07/07/2017. Now highlights callers and callees
  for any node that is selected.
  */
  class CGMouseAdapter extends MouseAdapter {

    //store the state of the last selected Procedure
    private Procedure lastSelectedProc;

    @Override
    public void mouseReleased(MouseEvent evt) {
      if (currentCG == null)
        return;
      Procedure prevProc = this.lastSelectedProc;
      if (prevProc != null) {
        for (Procedure callee : prevProc.getCalleeSet()) {
          currentCG.colorVertex(callee, "white");
        }

        for (Procedure caller : prevProc.getCallerSet()) {
          currentCG.colorVertex(caller, "white");
        }
      }

      graphPanel.revalidate();
      graphPanel.repaint();

      Procedure proc = currentCG.getSelectedNode();
      if (proc == null)
        return;

      for (Procedure callee : proc.getCalleeSet()) {
        currentCG.colorVertex(callee, "5FF03E");
      }

      for (Procedure caller : proc.getCallerSet()) {
        currentCG.colorVertex(caller, "F665F1");
      }

      graphPanel.revalidate();
      graphPanel.repaint();

      this.lastSelectedProc = proc;

      /*edit by Madeline Sgro 07/07/2017
      added some buttons, added ability to isolate descendants and ancestors
      */
      String[] options = new String[] { "OK", "CFG", "Run SPF", "Show Ancestors", "Show Descendants" };
      int which = JOptionPane.showOptionDialog(null, proc.getClassName() + "/" + proc.getProcedureName(),
              null, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
              null, options, options[0]);

      if (which < 0)
        return;

      switch (options[which]) {
        case "Run SPF":
          WCAFrame wcaFrame = new WCAFrame(proc.getClassName(),
                  proc.getSelectorName(),
                  configInfo);
          wcaFrame.setLocationRelativeTo(null);
          wcaFrame.setVisible(true);
          break;
        case "CFG":
          showControlFlowGraph(proc);
          break;
        case "Show Ancestors":
          showAncestors(proc); //added by Madeline Sgro
          break;
        case "Show Descendants":
          showDescendants(proc); //added by Madeline Sgro
          break;
        default:
          break;
      }
    }
  }

  /*added by Madeline Sgro 07/07/2017
  creates a Partial CG made from the ancestors of a given method
  */
  public void showAncestors(Procedure proc){
    Set<Procedure> ancestors = Program.getAncestorProcedureSet(proc);
    ancestors.add(proc);
    CGPartial cgPartial = new CGPartial(ancestors);
    cgPartial.paintProcedureSet(ancestors, "white");
    this.currentCG = cgPartial;
    drawCallGraph();
  }

  /*added by Madeline Sgro 07/07/2017
  creates a Partial CG made form the descendants of a given method
  */
  public void showDescendants(Procedure proc){
    Set<Procedure> descendants = Program.getDescendantProcedureSet(proc);
    descendants.add(proc);
    CGPartial cgPartial = new CGPartial(descendants);
    cgPartial.paintProcedureSet(descendants, "white");
    this.currentCG = cgPartial;
    drawCallGraph();
  }

  private void paintSlice(Set<Statement> stmtSet) {
    CG cg = this.getCG();
    cg.paintProcedureSet(Program.getProcedureSet(), "white");
    for (Procedure proc : Program.getProcedureSet()) {
      CFG cfg = this.getCFG(proc);
      cfg.getProcedure().dependentNodes.clear();
      cfg.paintNodeSet(proc.getNodeSet(), "aquamarine");
    }

    for (Statement stmt : stmtSet) {
      Procedure proc = stmt.getOwner().getProcedure();
      CFG cfg = this.getCFG(proc);
      if (proc == null || cfg == null)
        continue;
      cg.colorVertex(proc, "green");

      StatementType stmtType = stmt.getStatementType();
      if (stmtType == StatementType.Instruction) {
        SSAInstruction inst = (SSAInstruction)stmt.getContent();
        ISSABasicBlock target = proc.getNode(inst);
        int bcIndex = 0;
        try {
          if (inst.toString().startsWith("conditional branch")) {
            System.out.println ( "Bytecode : " + inst.toString() );
            bcIndex = ((IBytecodeMethod) target.getMethod()).getBytecodeIndex(inst.iindex);
            try {
              int src_line_number = target.getMethod().getLineNumber(bcIndex);
              nodeLineMap.put(target, src_line_number);
              System.out.println("Source line number = " + src_line_number);
              MainFrame.allSourceLines.put(src_line_number, proc.getProcedureName());
            } catch (Exception e) {
              System.out.println("Bytecode index is incorrect");
              System.out.println(e.getMessage());
            }
          }
        } catch (Exception e ) {
          System.err.println("it's probably not a BT method (e.g. it's a fakeroot method)");
          System.err.println(e.getMessage());
        }
        if (target != null) {
          cfg.getProcedure().dependentNodes.add(""+target.getNumber());
          cfg.paintNode(target, "green");
        }
      } else if (stmtType == StatementType.ActualIn || stmtType == StatementType.ActualOut) {
        Statement ctrlStmt = stmt.getControlStatement();
        if (ctrlStmt.getStatementType() == StatementType.Instruction) {
          SSAInstruction inst = (SSAInstruction)ctrlStmt.getContent();
          ISSABasicBlock target = proc.getNode(inst);
          int bcIndex = 0;
          try {
            if (inst.toString().startsWith("conditional branch")) {
              System.out.println ( "Bytecode : " + inst.toString() );
              bcIndex = ((IBytecodeMethod) target.getMethod()).getBytecodeIndex(inst.iindex);
              try {
                int src_line_number = target.getMethod().getLineNumber(bcIndex);
                System.out.println("Source line number = " + src_line_number);
                MainFrame.allSourceLines.put(src_line_number, proc.getProcedureName());
              } catch (Exception e) {
                System.out.println("Bytecode index no good");
                System.out.println(e.getMessage());
              }
            }
          } catch (Exception e ) {
            System.out.println("it's probably not a BT method (e.g. it's a fakeroot method)");
            System.out.println(e.getMessage());
          }
          if (target != null) {
            cfg.getProcedure().dependentNodes.add(""+target.getNumber());
            cfg.paintNode(target, "green");
          }
        }
      }

      for (ISSABasicBlock block : proc.getNodeSet()) {
        if (block.getLastInstructionIndex() < 0)
          continue;
        SSAInstruction inst = block.getLastInstruction();
        if (inst == null)
          continue;
        System.err.println(inst);
        if (inst.toString().startsWith("return") /*|| inst.toString().contains("= invokevirtual")*/) {
          ISSABasicBlock target = proc.getNode(inst);
          int bcIndex = 0;
          try {
            bcIndex = ((IBytecodeMethod) target.getMethod()).getBytecodeIndex(inst.iindex);
            try {
              int src_line_number = target.getMethod().getLineNumber(bcIndex);
              System.err.println("Source line number = " + src_line_number);
              if(MainFrame.allSourceLines.containsKey(src_line_number)) {
                MainFrame.allSourceLines.remove(src_line_number);
              }
            } catch (Exception e) {
              System.err.println("Bytecode index no good");
              System.err.println(e.getMessage());
            }
          } catch (Exception e ) {
            System.err.println("it's probably not a BT method (e.g. it's a fakeroot method)");
            System.err.println(e.getMessage());
          }
        }
      }
    }
    this.refreshDrawing();
  }

  private void paintSideChannel(List<String> nodeList) {
    CG cg = this.getCG();
    cg.paintProcedureSet(Program.getProcedureSet(), "white");
    for (Procedure proc : Program.getProcedureSet()) {
      CFG cfg = this.getCFG(proc);
      cfg.getProcedure().dependentNodes.clear();
      cfg.paintNodeSet(proc.getNodeSet(), "aquamarine");
    }

    for (String nodeString : nodeList) {
      ImbalanceAnalysisItem nodeItem = this.currentCFG.getJsonItem(nodeString);
      if (nodeItem == null)
        continue;
      Procedure proc =  nodeItem.getProcedure();
      ISSABasicBlock target = nodeItem.getBlockNode();
      if (proc == null || target == null)
        continue;
      cg.colorVertex(proc, "red");
      CFG cfg = this.getCFG(proc);
      if (cfg == null)
        continue;
      cfg.paintNode(target, "red");
    }
    this.refreshDrawing();
  }

  /*added by Seemanta Saha 01/17/2018
  check that a node is successor of another node
  */
  private ISSABasicBlock checkLoop(ISSABasicBlock node){
    ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = currentCFG.getProcedure().getCFG();
    Iterator<ISSABasicBlock> succNodeIter = cfg.getSuccNodes(node);
    if (succNodeIter.hasNext()) {
      ISSABasicBlock succNode = succNodeIter.next();
      if (succNode != null) {
        String nodeIns = "";
        Iterator<SSAInstruction> instIter = succNode.iterator();
        while (instIter.hasNext()) {
          SSAInstruction insts = instIter.next();
          nodeIns += Reporter.getSSAInstructionString(insts) + " ";
        }
        if (nodeIns.contains("phi") && nodeIns.contains("if")) {
          System.out.println("Got a loop node: " + succNode.getNumber() + " for " + node.getNumber());
          return succNode;
        }
      }
    }

    return null;
  }

  /*added by Seemanta Saha 01/17/2018
  check that a node in cfg is inside a loop
  */
  private ArrayList<ISSABasicBlock> checkNodeIsInLoop(ISSABasicBlock node, ArrayList<ISSABasicBlock> checkList) {
    ArrayList<ISSABasicBlock> nodeList = new ArrayList<ISSABasicBlock>();
    ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = currentCFG.getProcedure().getCFG();
    Iterator<ISSABasicBlock> succNodeIter = cfg.getSuccNodes(node);
    while (succNodeIter.hasNext()) {
      ISSABasicBlock succNode = succNodeIter.next();
      System.out.println(succNode.getNumber());
      if (succNode != null) {
        String nodeIns = "";
        Iterator<SSAInstruction> instIter = succNode.iterator();
        while (instIter.hasNext()) {
          SSAInstruction insts = instIter.next();
          nodeIns += Reporter.getSSAInstructionString(insts) + " ";
        }
        System.out.println(nodeIns);
        if (nodeIns.contains("phi") && nodeIns.contains("+") && nodeIns.contains("goto")) {
          System.out.println("Got a loop entering node: " + succNode.getNumber() + " for " + node.getNumber());
          ISSABasicBlock loopNode = checkLoop(succNode);
          if (loopNode != null) {
            nodeList.add(0, succNode);
            nodeList.add(1, loopNode);
          }
        } else if (!checkList.contains(succNode)){
          checkList.add(succNode);
          checkNodeIsInLoop(succNode, checkList);
        }
      }
    }
    return nodeList;
  }

  private ArrayList<ISSABasicBlock> checkEffectedBranchNode(Statement s) {
    ArrayList<ISSABasicBlock> finalNodeList = new ArrayList<ISSABasicBlock>();
    if (s.getStatementType() == StatementType.Instruction) {
      SSAInstruction inst = (SSAInstruction)s.getContent();
      ISSABasicBlock target = currentCFG.getProcedure().getNode(inst);
      if (target != null) {
        System.out.println(target.getNumber() + ":");
        String nodeIns = "";
        Iterator<SSAInstruction> instIter = target.iterator();
        while (instIter.hasNext()) {
          SSAInstruction insts = instIter.next();
          nodeIns += Reporter.getSSAInstructionString(insts) + " ";
        }
        System.out.println(nodeIns);
        //Check that node is a if conditional branch (not loop)
        if (!nodeIns.contains("phi") && nodeIns.contains("if")) {
          System.out.println("Branch condition:" + target.getNumber());
          ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = currentCFG.getProcedure().getCFG();
          Iterator<ISSABasicBlock> succNodeIter = cfg.getSuccNodes(target);
          boolean flag_branch = true;
          while (succNodeIter.hasNext()) { // only working with the first successor of if node considering it is the FALSE node
            ISSABasicBlock succNode = succNodeIter.next();
            if (flag_branch == false) {
              System.out.println("FALSE node: " + succNode.getNumber());
              //Check that the node is in a loop :(
              ArrayList<ISSABasicBlock> checkList = new ArrayList<ISSABasicBlock>();
              checkList.add(succNode);
              ArrayList<ISSABasicBlock> nodeList = checkNodeIsInLoop(succNode, checkList);
              if (nodeList.size() == 2) {
                System.out.println(target.getNumber() + " in loop");
                System.out.println("Work with node: " + succNode.getNumber());
                System.out.println("Node entering loop: " + nodeList.get(0).getNumber());
                System.out.println("Loop node: " + nodeList.get(1).getNumber());
                finalNodeList.add(0, succNode);
                finalNodeList.addAll(1, nodeList);
              }
            }
            flag_branch = false;
          }
        }
      }
    }
    return finalNodeList;
  }

  /*added by Madeline Sgro 07/07/2017
  most of content originally in CFGMouseAdapter edited last by Austin Neighbors
  enables user to highlight a variable in the CFG after selecting the name of it in
  the side panel
  */
  class InfoListMouseAdapter extends MouseAdapter {
    Set found = new HashSet<>();
    @Override
    public void mouseReleased(MouseEvent evt) {
      long start = System.currentTimeMillis();
      if (currentCFG == null){
        return;
      }

      if (!found.isEmpty()){
        currentCFG.paintNodeSet(found, "aquamarine");
        found.clear();
      }

      Set<Statement> sliceStmtSet = new HashSet<>();
      Set<Statement> stmtSet = new HashSet<>();
      Set<Statement> cntrlStmtSet = new HashSet<>();
      String selected = (String)infoList.getSelectedValue();

      if (selected != null){
        selected = selected.split("@")[0];
        MainFrame.selectedVariables.add(selected);
        Map<ISSABasicBlock, Object> mapCFG = currentCFG.getVertexMap();
        for (ISSABasicBlock entry : mapCFG.keySet()){
          String compare = currentCFG.getGraph().getLabel(mapCFG.get(entry));
          String[] compareArray = compare.split("\n");
          if (compareArray.length > 1){
            for (int i = 0; i < compareArray.length; i++) {
              if (i == 0) continue;
              compare = compareArray[i];
              if (compare.matches(".*" + selected + "([^0-9].*)?") ||
                      ((compare.contains("phi") || compare.contains("arrayload") || compare.contains("arraylength")) &&
                              compare.matches("(.*[^0-9])?" + selected.substring(1, selected.length()) + "([^0-9].*)?"))){
                found.add(entry);
                System.out.println("\n=============================\n");
                Iterator<SSAInstruction> itr = entry.iterator();
                while(itr.hasNext()) {
                  SSAInstruction ins = itr.next();
                  System.out.println(ins);
                  if(Reporter.getSSAInstructionString(ins).contains("=")) {
                    String[] inspart = Reporter.getSSAInstructionString(ins).split("=");
                    if(inspart[1].contains(selected)) {
                      stmtSet.addAll(ProgramDependenceGraph.sliceProgramForward(currentCFG.getProcedure(),ins));
                      String newvar = inspart[0].replace(" ","");
                      System.out.println(newvar);
                      MainFrame.selectedVariables.add(newvar);
                    }
                  }
                }
                System.out.println("\n=============================\n");
                String[] inspart = Reporter.getSSAInstructionString(entry.getLastInstruction()).split(" ");
                boolean runFlag = false;
                for(String s : inspart){
                  for(String sv : MainFrame.selectedVariables) {
                    if(s.equals(sv)){
                      runFlag = true;
                      break;
                    }
                  }
                }
                if(runFlag) {
                  stmtSet.addAll(ProgramDependenceGraph.sliceProgramForward(currentCFG.getProcedure(), entry.getLastInstruction()));
                  MainFrame.allStmtSet.addAll(stmtSet);
                }
                for (Statement s : MainFrame.allStmtSet) {
                  sliceStmtSet.add(s);
                  ArrayList<ISSABasicBlock> nodeList = checkEffectedBranchNode(s);
                  if (nodeList.size() == 3) {
                    ISSABasicBlock insideIfNode = nodeList.get(0);
                    ISSABasicBlock loopEnteringNode = nodeList.get(1);
                    ISSABasicBlock loopNode = nodeList.get(2);
                    if (loopEnteringNode != null) {
                      Iterator<SSAInstruction> instIter = loopEnteringNode.iterator();
                      while (instIter.hasNext()) {
                        SSAInstruction insts = instIter.next();
                        if (insts.toString().contains("phi")) {
                          String var = insts.toString().split(" = ")[0];
                          System.out.println("variable : v" + var);
                          Iterator<SSAInstruction> loopInstIter = loopNode.iterator();
                          while (loopInstIter.hasNext()) {
                            SSAInstruction instsLoop = loopInstIter.next();
                            if (instsLoop.toString().contains("phi  " + var)) {
                              String varToSlice = "v" + instsLoop.toString().split(" = ")[0];
                              System.out.println("variable to slice : " + varToSlice);
                              for (ISSABasicBlock node : mapCFG.keySet()){
                                String compareNode = currentCFG.getGraph().getLabel(mapCFG.get(node));
                                String[] compareNodeArray = compareNode.split("\n");
                                if (compareNodeArray.length > 1){
                                  for (int j = 0; j < compareNodeArray.length; j++) {
                                    if (j == 0) continue;
                                    compareNode = compareNodeArray[i];
                                    if (compareNode.matches(".*" + varToSlice + "([^0-9].*)?") ||
                                            ((compareNode.contains("phi") || compareNode.contains("arrayload") || compareNode.contains("arraylength")) &&
                                                    compareNode.matches("(.*[^0-9])?" + varToSlice.substring(1, varToSlice.length()) + "([^0-9].*)?"))){
                                      found.add(node);
                                      cntrlStmtSet = ProgramDependenceGraph.sliceProgramForward(currentCFG.getProcedure(), node.getLastInstruction());
                                      for (Statement stmt : cntrlStmtSet) {
                                        sliceStmtSet.add(stmt);
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        } else {
                          break;
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        paintSlice(sliceStmtSet);

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        dependencyAnalysisTime += timeElapsed;

        //currentCFG.paintNodeSet(found, "yellow");
        graphPanel.update(graphPanel.getGraphics());
      }
    }
  }

  class CFGMouseAdapter extends MouseAdapter {

    @Override
    public void mouseReleased(MouseEvent evt) {
      if (currentCFG == null)
        return;

      if (SwingUtilities.isRightMouseButton(evt)) {
        cfgOptPopupMenu.show(mainPanel, evt.getXOnScreen(), evt.getYOnScreen());
        currentNode = currentCFG.getSelectedNode();
        silceMenuItem.setEnabled(currentNode != null);
      }
    }
  }

  class PDGMouseAdapter extends MouseAdapter {
    @Override
    public void mouseReleased(MouseEvent evt) {
      if (currentPDG == null)
        return;

      if (SwingUtilities.isRightMouseButton(evt))
        pdgOptPopupMenu.show(mainPanel, evt.getXOnScreen(), evt.getYOnScreen());
    }
  }

  private void retMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_retMenuItemActionPerformed
    this.currentCFG = null;
    if (null != this.focus)
      switch (this.focus) {
        case Loop:
          loopComboBoxActionPerformed(evt);
          break;
        case New:
          newComboBoxActionPerformed(evt);
          break;
        case Recursion:
          recursionComboBoxActionPerformed(evt);
          break;
        case Zoom:
          zoomComboBoxActionPerformed(evt);
          break;
        default:
          drawCallGraph();
      }
  }//GEN-LAST:event_retMenuItemActionPerformed

  private void silceMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_silceMenuItemActionPerformed
    SliceFrame sliceFrame = new SliceFrame(this, this.currentCFG.getProcedure(), this.currentNode);
    sliceFrame.setLocationRelativeTo(null);
    sliceFrame.setVisible(true);
    this.focus = CGFocus.Slice;
  }//GEN-LAST:event_silceMenuItemActionPerformed

  private void filterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterButtonActionPerformed
    this.loopFilterFrame.setLocationRelativeTo(null);
    this.loopFilterFrame.setVisible(true);
    loadNestedLoops();
    cgSwitchToggleButtonActionPerformed(null);
  }//GEN-LAST:event_filterButtonActionPerformed

  private void locateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_locateButtonActionPerformed
    this.locationFrame.setLocationRelativeTo(null);
    this.locationFrame.setVisible(true);
  }//GEN-LAST:event_locateButtonActionPerformed

  private void runspfButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runspfButtonActionPerformed
    WCAFrame wcaFrame = new WCAFrame(configInfo);
    wcaFrame.setLocationRelativeTo(null);
    wcaFrame.setVisible(true);
  }//GEN-LAST:event_runspfButtonActionPerformed

  private void branchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_branchButtonActionPerformed
    UnbalancedBranchFrame branchFrame = new UnbalancedBranchFrame();
    branchFrame.setLocationRelativeTo(null);
    branchFrame.setVisible(true);
  }//GEN-LAST:event_branchButtonActionPerformed

  private void showPDGMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showPDGMenuItemActionPerformed
    if (this.currentCFG == null)
      return;
    Procedure proc = this.currentCFG.getProcedure();
    showProcedureDependecenGraph(proc);
  }//GEN-LAST:event_showPDGMenuItemActionPerformed

  private void backMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backMenuItemActionPerformed
    this.currentPDG = null;
    if (this.currentCFG == null)
      return;
    Procedure proc = this.currentCFG.getProcedure();
    showControlFlowGraph(proc);
  }//GEN-LAST:event_backMenuItemActionPerformed

  private void loadspfButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadspfButtonActionPerformed
    this.fileChooser.setMultiSelectionEnabled(false);
//        String currentPath = this.startScriptTextField.getText();
    String projpath = configInfo.getField(ConfigInfo.StringType.projectpath);
    this.fileChooser.setCurrentDirectory(new File(projpath));
    int retVal = this.fileChooser.showOpenDialog(this);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      // get the selected file
      File file = this.fileChooser.getSelectedFile();
      if (file.isFile()) {
        String fname = file.getAbsolutePath();
        WCAFrame wcaFrame = new WCAFrame(fname, configInfo);
        wcaFrame.setLocationRelativeTo(null);
        wcaFrame.setVisible(true);
      }
    }
  }//GEN-LAST:event_loadspfButtonActionPerformed

  private void jBondButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBondButtonActionPerformed
    this.fileChooser.setMultiSelectionEnabled(false);
    int retVal = this.fileChooser.showOpenDialog(this);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      File file = this.fileChooser.getSelectedFile();
      Gson gson = new Gson();
      try {
        JsonReader reader = new JsonReader(new FileReader(file));
        Map<String, Map<String, Double>> jbond = new HashMap<>();
        jbond = (Map<String, Map<String, Double>>)gson.fromJson(reader, jbond.getClass());

        this.jBondMap.clear();
        this.jBondShowAllCheckBox.setEnabled(true);
        this.jBondFeatureComboBox.setEnabled(true);

        for (Map.Entry<String, Map<String, Double>> jbondEnt : jbond.entrySet()) {
          String procSig = jbondEnt.getKey();
          Map<String, Double> valueMap = jbondEnt.getValue();
          Procedure proc = Program.getProcedure(procSig);
          if (proc != null) {
            for (Map.Entry<String, Double> valueMapEnt : valueMap.entrySet()) {
              String feature = valueMapEnt.getKey();
              Double value = valueMapEnt.getValue();
              Map<Double, Set<Procedure>> featureProcSetMap = this.jBondMap.get(feature);
              if (featureProcSetMap == null) {
                featureProcSetMap = new TreeMap<>();
                this.jBondMap.put(feature, featureProcSetMap);
              }
              Set<Procedure> featureProcSet = featureProcSetMap.get(value);
              if (featureProcSet == null) {
                featureProcSet = new HashSet<>();
                featureProcSetMap.put(value, featureProcSet);
              }
              featureProcSet.add(proc);
            }
          }
        }

      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }

      this.jBondFeatureComboBox.removeAllItems();
      for (Map.Entry<String, Map<Double, Set<Procedure>>> jBondMapEnt : this.jBondMap.entrySet()) {
        String feature = jBondMapEnt.getKey();
        this.jBondFeatureComboBox.addItem(feature);
      }
      jBondShowAllCheckBoxActionPerformed(evt);
    }
  }//GEN-LAST:event_jBondButtonActionPerformed

  private void jBondFeatureComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBondFeatureComboBoxActionPerformed
    String chosenStr = (String)this.jBondFeatureComboBox.getSelectedItem();
    Map<Double, Set<Procedure>> featureProcSetMap = this.jBondMap.get(chosenStr);
    if (featureProcSetMap != null)
      showjBondResult(featureProcSetMap);
  }//GEN-LAST:event_jBondFeatureComboBoxActionPerformed

  private void jBondShowAllCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBondShowAllCheckBoxActionPerformed
    if (this.jBondShowAllCheckBox.isSelected())
      this.cgSwitchToggleButton.setSelected(false);
    else
      this.cgSwitchToggleButton.setSelected(true);
    jBondFeatureComboBoxActionPerformed(evt);
  }//GEN-LAST:event_jBondShowAllCheckBoxActionPerformed

  /*
  Added by Madeline Sgro 07/06/2017
  takes the user's selection zoom in selection and applies it to the CG or CFG depending
  on which one the user is looking at
  */
  private void zoomComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomComboBoxActionPerformed
    String chosenStr = (String)this.zoomComboBox.getSelectedItem();
    if(chosenStr == null || chosenStr.isEmpty()){
      return;
    }
    String toDouble = chosenStr.substring(0, chosenStr.length()-1);
    double scaleFactor = (Double.parseDouble(toDouble))*.01;
    if(currentCFG != null){
      currentCFG.scaleNodeSet(currentCFG.getProcedure().getNodeSet(), scaleFactor);
      graphPanel.update(graphPanel.getGraphics());
    } else {
      this.callGraph.scaleProcedureSet(Program.getProcedureSet(), scaleFactor);
      drawCallGraph();
    }
  }//GEN-LAST:event_zoomComboBoxActionPerformed

  /*
  Added by Madeline Sgro 07/13/2017
  shows the users selection on the CG so far will be able to show instances of
  modulo or case sensitive string comparison
  */
  private void otherComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otherComboBoxActionPerformed
    String chosenStr = (String)this.otherComboBox.getSelectedItem();
    if(chosenStr == null || chosenStr.isEmpty()){
      this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
      drawCallGraph();
      return;
    }

    //reset loops, news, recursion
    if (this.newComboBox.getSelectedIndex() != 0){
      listNewObjects();
    } else if (this.recursionComboBox.getSelectedIndex() != 0){
      listRecursions();
    } else if (this.loopComboBox.getSelectedIndex() != 0) {
      listNestedLoops();
    }

    this.otherList = null; //added 07/17/2017

    //make showing selection
    if (chosenStr == "Show Modulo"){
      showModulo();
    } else if (chosenStr == "Show String Comparison") {
      showStringComparisons();
    }
  }//GEN-LAST:event_otherComboBoxActionPerformed

  private String getNodeFromID(String jsonItemID) {
    if(nodeMap.get(jsonItemID) == null) {
      nodeMap.put(jsonItemID, counter);
      idMap.put(counter,jsonItemID);
      counter++;
      numberofNodes=counter;
    }
    return nodeMap.get(jsonItemID).toString();
  }

  private void removeAllTransitions(String node, String assertNode, Procedure proc, boolean[] visited) {
    if(visited[Integer.parseInt(node)])
      return;
    visited[Integer.parseInt(node)] = true;
    List<MarkovChainInformation> list = transitionlistMap.get(node);
    if(list == null)
      return;
    for(MarkovChainInformation mi : list) {
      if(!mi.getFromNode().equals(mi.getToNode()) && !mi.getToNode().equals(assertNode)) {
        removeAllTransitions(mi.getToNode(), assertNode, proc, visited);
        ISSABasicBlock aNode = itemNodeMap.get(idMap.get(Integer.parseInt(assertNode)));
        ISSABasicBlock mNode = itemNodeMap.get(idMap.get(Integer.parseInt(mi.getToNode())));
        Set<ISSABasicBlock> aNodeDominatorSet = proc.getDominatorSet(aNode);
        if(assertNode.equals(assertionNode) && !aNodeDominatorSet.contains(mNode)) {
            //numberofNodesReduced += 1;
            transitionlistMap.remove(mi.getToNode());
        }
      }
    }
  }

  private void branchModelCountMenuitemActionPerformed(java.awt.event.ActionEvent evt) {

    long start = System.currentTimeMillis();

    long dts = System.currentTimeMillis();

    if (this.currentCFG == null)
      return;

    List<String> jsonItems = this.currentCFG.getJSON();
    List<String> invokedProcedures = new ArrayList<String>();
    invokedProcedures.add(this.currentCFG.getProcedure().getFullSignature());

    String modelName = currentCFG.getProcedure().getClassName().replace("/","_") + "_" + currentCFG.getProcedure().getProcedureName();
    modelName = modelName.replace("$","_");
    Procedure cureProc = this.currentCFG.getProcedure();

    //preprocessing source code branch condition in CFG
    List<String> jsonItemsToBeAdded = new ArrayList<>();
    List<String> jsonItemsToBeRemoved = new ArrayList<>();
    String trueNodeToUpdate = "";
    String falseNodeToUpdate = "";
    String insTotranslate = "";
    String jsonItem1 = "";
    String jsonItemId1 = "";
    String prevUpdatedJsonItem = "";


    //numberofNodes = jsonItems.size();

    String completeJSON = "[ ";
    int i = 0;
    for (String jsonItem: jsonItems) {

      String jsonItemID = jsonItem.split(" ")[4];
      String jsonItemNodeNumber = jsonItemID.split("#")[1];

      //remmeber this: Procedure cureProc = this.currentCFG.getProcedure();
      if (cureProc.dependentNodes.contains(jsonItemNodeNumber) && jsonItem.contains("\"secret_dependent_branch\" : \"branch\"")) {
        jsonItem = jsonItem.replace("\"secret_dependent_branch\" : \"branch\"", "\"secret_dependent_branch\" : \"true\"");
      }
      completeJSON += jsonItem;
      if (i < jsonItems.size() - 1)
        completeJSON += ",\n";
      i++;

      // Recursive inlining of function calls
      if (jsonItem.contains("Invoke") && !jsonItem.contains("<init>")) {
        completeJSON = recursiveInlining(invokedProcedures, jsonItems, i, jsonItemID, jsonItem, completeJSON, 0);
      }
    }
    completeJSON += " ]";

    System.out.println(completeJSON);


    String[] interProcItems = completeJSON.split("\n");
    List<String> interProcItemsList = new ArrayList<String>(interProcItems.length);
    for (String s:interProcItems) {
      interProcItemsList.add( s );
    }


    long dts5 = System.currentTimeMillis();

    //for (String jsonItem: jsonItems) {
    for(Iterator<String> it = interProcItemsList.iterator(); it.hasNext();) {
      String jsonItem = it.next();
      if(jsonItem.startsWith("[ "))
        jsonItem = jsonItem.substring(2);
      String jsonItemID = jsonItem.split(" ")[4];
      String jsonItemNodeNumber = jsonItemID.split("#")[1];
      if (cureProc.dependentNodes.contains(jsonItemNodeNumber) && jsonItem.contains("\"secret_dependent_branch\" : \"true\"")) {
        String[] outgoingNodes = jsonItem.split("\"outgoing\" : \\{ ")[1].split(" }")[0].split(",");
        String trueNode = outgoingNodes[0].split("\"")[1];
        String falseNode = "";
        String insn = "";
        String jsonItem2 = "";
        String jsonItemId2 = "";
        if(outgoingNodes.length == 2 && outgoingNodes[1].contains("\"")) {
          falseNode = outgoingNodes[1].split("\"")[1];
          insn = jsonItem.split("\"ins_to_translate\" : \"")[1].split("\"")[0];
          jsonItemId2 = jsonItemID;
          jsonItem2 = jsonItem;

//          boolean sameVarFlag = false;
//          String[] ins1 = insTotranslate.split(" ");
//          String[] ins2 = insn.split(" ");
//          HashSet<String> tmp = new HashSet<String>();
//          for (String s : ins1) {
//            if(s.contains("v"))
//              tmp.add(s);
//          }
//          for (String s : ins2) {
//            if (tmp.contains(s)) {
//              sameVarFlag = true;
//              break;
//            }
//          }

          boolean sameLineFlag = false;
          if(!jsonItemId1.equals("") && !jsonItemId2.equals("")) {
            ISSABasicBlock n1 = itemNodeMap.get(jsonItemId1);
            if(n1 == null) {
              String[] jsonItem1Arr = jsonItemId1.split("#");
              n1 = itemNodeMap.get(jsonItem1Arr[0]+"#"+jsonItem1Arr[jsonItem1Arr.length-1]);
            }
            ISSABasicBlock n2 = itemNodeMap.get(jsonItemId2);
            if(n2 == null) {
              String[] jsonItem2Arr = jsonItemId2.split("#");
              n2 = itemNodeMap.get(jsonItem2Arr[0]+"#"+jsonItem2Arr[jsonItem2Arr.length-1]);
            }
            if (n1 != null && n2 != null && nodeLineMap.get(n1) == nodeLineMap.get(n2)) {
              sameLineFlag = true;
            }
          }

          if(jsonItemID.equals(trueNodeToUpdate) /*&& sameVarFlag*/) {
              if(sameLineFlag) {
                  jsonItemsToBeRemoved.add(jsonItem);

                  List<String> itemlist = lineItemsMap.get(jsonItem1);
                  itemlist.add(jsonItem);
                  lineItemsMap.put(jsonItem1, itemlist);
                  lineItemsMap.put(jsonItem, itemlist);

                  insTotranslate = insTotranslate + " and " + insn;
                  String updatedJsonItem = jsonItem.replace(insn, insTotranslate);
                  jsonItemsToBeAdded.add(updatedJsonItem);
                  if(!prevUpdatedJsonItem.equals("")) {
                    jsonItemsToBeAdded.remove(prevUpdatedJsonItem);
                  }
                  prevUpdatedJsonItem = updatedJsonItem;
              }else {
                  insTotranslate = insn;
                  jsonItemId1 = jsonItemID;
                  jsonItem1 = jsonItem;
                  jsonItemsToBeRemoved.add(jsonItem);

                  List<String> itemlist = new ArrayList<>();
                  itemlist.add(jsonItem);
                  lineItemsMap.put(jsonItem, itemlist);

                  prevUpdatedJsonItem="";
              }
          }
          else if(jsonItemID.equals(falseNodeToUpdate) /*&& sameVarFlag*/) {
              if(sameLineFlag) {
                  jsonItemsToBeRemoved.add(jsonItem);

                  List<String> itemlist = lineItemsMap.get(jsonItem1);
                  itemlist.add(jsonItem);
                  lineItemsMap.put(jsonItem1, itemlist);
                  lineItemsMap.put(jsonItem, itemlist);

                  insTotranslate = "not (" + insTotranslate + ") and not (" + insn + ")";
                  String updatedJsonItem = jsonItem.replace(insn, insTotranslate);
                  jsonItemsToBeAdded.add(updatedJsonItem);
                  if(!prevUpdatedJsonItem.equals("")) {
                    jsonItemsToBeAdded.remove(prevUpdatedJsonItem);
                  }
                  prevUpdatedJsonItem = updatedJsonItem;
              }else {
                  insTotranslate = insn;
                  jsonItemId1 = jsonItemID;
                  jsonItem1 = jsonItem;
                  jsonItemsToBeRemoved.add(jsonItem);

                  List<String> itemlist = new ArrayList<>();
                  itemlist.add(jsonItem);
                  lineItemsMap.put(jsonItem, itemlist);

                  prevUpdatedJsonItem="";
              }
          }
          else {
            insTotranslate = insn;
            jsonItemId1 = jsonItemID;
            jsonItem1 = jsonItem;
            jsonItemsToBeRemoved.add(jsonItem);

              List<String> itemlist = new ArrayList<>();
              itemlist.add(jsonItem);
              lineItemsMap.put(jsonItem, itemlist);

            prevUpdatedJsonItem="";
          }
          trueNodeToUpdate = trueNode;
          falseNodeToUpdate = falseNode;
        }
      }
    }

    if(jsonItemsToBeAdded.size() == 0)
      jsonItemsToBeRemoved.clear();

    List<String> temp = new ArrayList<>();
      for (String item : jsonItemsToBeRemoved) {
        if(lineItemsMap.get(item).size() != 1)
          temp.add(item);
      }
      jsonItemsToBeRemoved.clear();
      jsonItemsToBeRemoved.addAll(temp);
      temp.clear();

    Map<String, String> replaceMap = new HashMap<>();
    for (int j = 0; j < jsonItemsToBeAdded.size(); j ++) {
      String it = jsonItemsToBeAdded.get(j);
      String itId = it.split(" ")[4];
      for (int k = 0; k < jsonItemsToBeRemoved.size(); k++) {
        String item = jsonItemsToBeRemoved.get(k);
        String itemId = item.split(" ")[4];
        if (itemId.equals(itId)) {
            j++;
            if (j < jsonItemsToBeAdded.size()) {
                it = jsonItemsToBeAdded.get(j);
                itId = it.split(" ")[4];
            }
            continue;
        }
        if(j < jsonItemsToBeAdded.size()) {
//            String[] itemIDArr = itemId.split("#");
//            itemId = itemIDArr[0] + "#" + itemIDArr[itemIDArr.length-1];
//            String[] itIDArr = itId.split("#");
//            itId = itIDArr[0] + "#" + itIDArr[itIDArr.length-1];
            replaceMap.put("\"" + itemId + "\"", "\"" + itId + "\"");
        }
      }
    }
    numberofNodesMerged = jsonItemsToBeRemoved.size()-jsonItemsToBeAdded.size();
    for (String item : jsonItemsToBeRemoved) {
      interProcItemsList.remove(item);
    }
    for(String item : jsonItemsToBeAdded) {
      interProcItemsList.add(item);
    }

    List<String> newJsonItems = new ArrayList<>();
    for(Iterator<String> it = interProcItemsList.iterator(); it.hasNext();) {
      String item  = it.next();
      boolean flag = false;
      for (Map.Entry<String,String> entry : replaceMap.entrySet()) {
//        String[] itIDArr = entry.getKey().split("#");
//        String itID = itIDArr[0] + "#" + itIDArr[itIDArr.length-1];
//        if(item.contains(itID)) {
        if(item.contains(entry.getKey())) {
//          String[] upitIDArr = entry.getValue().split("#");
//          String upitID = "";
//          if(upitIDArr.length == 3)
//            upitID = upitIDArr[0] + "#" + itIDArr[1] + "#" + upitIDArr[upitIDArr.length-1];
//          else
//            upitID = entry.getValue();
//          String newItem = item.replace(entry.getKey(), upitID);
          String newItem = item.replace(entry.getKey(), entry.getValue());
          newJsonItems.add(newItem);
          flag = true;
        }
      }
      if(!flag) {
        newJsonItems.add(item);
      }
    }
    interProcItemsList.clear();
    interProcItemsList.addAll(newJsonItems);


    long dfs5 = System.currentTimeMillis();
    long det5 = dfs5 - dts5;
    /*List<String> actualList = new ArrayList<>();
    for(String item : jsonItemsToBeAdded) {
      String itemId = item.split(" ")[4];
      String itemId2 = itemId;
      int n = Integer.parseInt(itemId.split("#")[1]) - 1;
      String itemId1 = itemId .split("#")[0] + "#" + n;
      int count = 0;
      List<String> tempList = new ArrayList<>();
      for(String it : jsonItemsToBeRemoved) {
        String itId = it.split(" ")[4];
        if(itId.equals(itemId1) || itId.equals(itemId2)) {
          tempList.add(it);
          count++;
          if(count == 2)
            break;
        }
      }
      if(count == 2) {
        actualList.addAll(tempList);
      }
      tempList.clear();
    }
    jsonItemsToBeRemoved.clear();
    jsonItemsToBeRemoved.addAll(actualList);

    //if(jsonItemsToBeRemoved.size() == 2 * jsonItemsToBeAdded.size()) {
    int count = 1;
    String key = "", val = "";
    Map<String, String> replaceMap = new HashMap<>();
    for(String item : jsonItemsToBeRemoved) {
      jsonItems.remove(item);
      if(count % 2 == 1) {
        key = "\""+item.split(" ")[4]+"\"";
      } else {
        val = "\""+item.split(" ")[4]+"\"";
        replaceMap.put(key,val);
      }
      count++;
    }
    for(String item : jsonItemsToBeAdded) {
      jsonItems.add(item);
    }

    List<String> newJsonItems = new ArrayList<>();
    for(Iterator<String> it = jsonItems.iterator(); it.hasNext();) {
      String item  = it.next();
      boolean flag = false;
      for (Map.Entry<String,String> entry : replaceMap.entrySet()) {
        if(item.contains(entry.getKey())) {
          String newItem = item.replace(entry.getKey(), entry.getValue());
          newJsonItems.add(newItem);
          flag = true;
        }
      }
      if(!flag) {
        newJsonItems.add(item);
      }
    }
    jsonItems.clear();
    jsonItems.addAll(newJsonItems);*/
    //}





    //MARKOV CHAIN CONSTRUCTION
    numberofNodes = interProcItemsList.size() + numberofNodesMerged;

    String graphOutput = "digraph {\n";
    String prismModel = "dtmc\n\n" + "module " + modelName + "\n\n";
    String assertionReachabilityNode="", assertionExecutionNode="";
    prismModel += "\t" + "s : [0.." + numberofNodes +"] init 0;\n\n";


    for (String jsonItem: interProcItemsList) {

      if(jsonItem.startsWith("[ "))
        jsonItem = jsonItem.substring(2);
      String jsonItemID = jsonItem.split(" ")[4];

      if (jsonItem.contains("\"secret_dependent_branch\" : \"true\"")) {
        //start: additional code for counting secret dependent branches
          long startTime = System.currentTimeMillis();
        String ins_to_translate = jsonItem.split("\"ins_to_translate\" : \"")[1].split("\"")[0];
        System.out.println("Instruction to translate: " + ins_to_translate);

        List<String> smtConsList = translateToSMTLib(ins_to_translate, itemProcMap.get(jsonItemID.split("#")[0]));
        System.out.println(smtConsList.get(1));

        modelCounter.setBound(31);
        modelCounter.setModelCountMode("abc.linear_integer_arithmetic");
        BigDecimal cons_count = modelCounter.getModelCount(smtConsList.get(1));
        BigDecimal dom_count = modelCounter.getModelCount(smtConsList.get(0));

        double true_prob = 0.0;
        double false_prob = 0.0;

        String[] ins_part = ins_to_translate.split("and");

        if(ins_part.length >= 2 && ins_part[1].contains("not")) {
          false_prob = cons_count.doubleValue() / dom_count.doubleValue();
          true_prob = 1.0 - true_prob;
        } else {
          true_prob = cons_count.doubleValue() / dom_count.doubleValue();
          false_prob = 1.0 - true_prob;
        }

        System.out.println("Probability of true branch: " + true_prob);


        //end: additional code for counting secret dependent branches
        jsonItem = jsonItem.replace("\"secret_dependent_branch\" : \"true\"", "\"secret_dependent_branch\" : \"true\", \"true_branch_probability\" : \"" + true_prob + "\", \"false_branch_probability\" : \"" + false_prob + "\"");


        String fromNode = getNodeFromID(jsonItemID);


        String[] outgoingNodes = jsonItem.split("\"outgoing\" : \\{ ")[1].split(" }")[0].split(",");

        String trueNode = getNodeFromID(outgoingNodes[0].split("\"")[1]);

        if(outgoingNodes.length == 2 && outgoingNodes[1].contains("\"")) {

          String falseNode = getNodeFromID(outgoingNodes[1].split("\"")[1]);

          String trueNodeProb = "";
          String falseNodeProb = "";

          if (fromNode.equals(assertionReachabilityNode)) {
            assertionExecutionNode = falseNode;
          }

          if (jsonItem.contains("$assertionsDisabled")) {
            assertionReachabilityNode = falseNode;
            assertionNode = assertionReachabilityNode;

            //quick naive fix to deal with assertion reachble node when there is an exception to handle
            if (!jsonItem.contains("\"exception\" : \"true\"")) {
              trueNodeProb = "0.0";
              falseNodeProb = "1.0";
            } else {
              trueNodeProb = Double.toString(true_prob);
              falseNodeProb = Double.toString(false_prob);
            }
          } else {
            trueNodeProb = jsonItem.split("\"true_branch_probability\" : \"")[1].split("\"")[0];
            falseNodeProb = jsonItem.split("\"false_branch_probability\" : \"")[1].split("\"")[0];
          }

          List<String> edgeList = edgeMap.get(fromNode);
          if (edgeList == null) {
            edgeList = new ArrayList<>();
          }
          edgeList.add(trueNode);
          edgeList.add(falseNode);
          edgeMap.put(fromNode,edgeList);

          MarkovChainInformation trueChain = new MarkovChainInformation(fromNode,trueNode,trueNodeProb,true, false, false);
          MarkovChainInformation falseChain = new MarkovChainInformation(fromNode,falseNode,falseNodeProb,true, false, false);
          transitionMap.put(new Pair<>(fromNode,trueNode), trueChain);
          transitionMap.put(new Pair<>(fromNode,falseNode), falseChain);

          List<MarkovChainInformation> list = new ArrayList<>();
          list.add(trueChain);
          list.add(falseChain);
          transitionlistMap.put(fromNode, list);

          graphOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + trueNodeProb + "\"];\n";
          graphOutput += "\t" + fromNode + " -> " + falseNode + "[label= " + "\"" + falseNodeProb + "\"];\n";

          prismModel += "\t" + "[] s = " + fromNode + " -> " + trueNodeProb + " : " + "(s' = " + trueNode + ") + " + falseNodeProb + " : " + "(s' = " + falseNode + ");\n";
        } else {
          List<String> edgeList = edgeMap.get(fromNode);
          if (edgeList == null) {
            edgeList = new ArrayList<>();
          }
          edgeList.add(trueNode);
          edgeMap.put(fromNode,edgeList);

          MarkovChainInformation trueChain = new MarkovChainInformation(fromNode,trueNode,"1.0",true, false, false);
          transitionMap.put(new Pair<>(fromNode,trueNode), trueChain);
          List<MarkovChainInformation> list = new ArrayList<>();
          list.add(trueChain);
          transitionlistMap.put(fromNode, list);

          graphOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + "1.0" + "\"];\n";

          prismModel += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + trueNode + ");\n";
        }

          String[] splittedID = jsonItemID.split("#");
          String idModelCount = splittedID[0]+"#"+splittedID[splittedID.length-1];
          long finishTime = System.currentTimeMillis();
          long elapsedTime = finishTime - startTime;
          modelCountingTimeMap.put(idModelCount, elapsedTime);

      } else if (jsonItem.contains("\"secret_dependent_branch\" : \"branch\"")) {

        String fromNode = getNodeFromID(jsonItemID);


        String[] outgoingNodes = jsonItem.split("\"outgoing\" : \\{ ")[1].split(" }")[0].split(",");

        String trueNode = getNodeFromID(outgoingNodes[0].split("\"")[1]);


        if (outgoingNodes.length == 2) {
          String falseNode = getNodeFromID(outgoingNodes[1].split("\"")[1]);

          if (fromNode.equals(assertionReachabilityNode)) {
            assertionExecutionNode = falseNode;
          }

          List<String> edgeList = edgeMap.get(fromNode);
          if (edgeList == null) {
            edgeList = new ArrayList<>();
          }
          edgeList.add(trueNode);
          edgeList.add(falseNode);
          edgeMap.put(fromNode,edgeList);


          if (jsonItem.contains("$assertionsDisabled")) {
            assertionReachabilityNode = falseNode;
            assertionNode = assertionReachabilityNode;
            if (!jsonItem.contains("\"exception\" : \"true\"")) {
              MarkovChainInformation trueChain = new MarkovChainInformation(fromNode,trueNode,"0.0",false, jsonItem.contains("$assertionsDisabled"), true);
              MarkovChainInformation falseChain = new MarkovChainInformation(fromNode,falseNode,"1.0",false, jsonItem.contains("$assertionsDisabled"), true);
              transitionMap.put(new Pair<>(fromNode,trueNode), trueChain);
              transitionMap.put(new Pair<>(fromNode,falseNode), falseChain);

              List<MarkovChainInformation> list = new ArrayList<>();
              list.add(trueChain);
              list.add(falseChain);
              transitionlistMap.put(fromNode, list);

              graphOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + "0.0" + "\"];\n";
              graphOutput += "\t" + fromNode + " -> " + falseNode + "[label= " + "\"" + "1.0" + "\"];\n";
              prismModel += "\t" + "[] s = " + fromNode + " -> " + "0.0" + " : " + "(s' = " + trueNode + ") + " + "1.0" + " : " + "(s' = " + falseNode + ");\n";
            } else {
              long startTime = System.currentTimeMillis();
              String ins_to_translate = jsonItem.split("\"ins_to_translate\" : \"")[1].split("\"")[0];
              System.out.println("Instruction to translate: " + ins_to_translate);

              List<String> smtConsList = translateToSMTLib(ins_to_translate, itemProcMap.get(jsonItemID.split("#")[0]));
              System.out.println(smtConsList.get(1));

              modelCounter.setBound(31);
              modelCounter.setModelCountMode("abc.linear_integer_arithmetic");
              BigDecimal cons_count = modelCounter.getModelCount(smtConsList.get(1));
              BigDecimal dom_count = modelCounter.getModelCount(smtConsList.get(0));

              double true_prob = 0.0;
              double false_prob = 0.0;

              String[] ins_part = ins_to_translate.split("and");

              if(ins_part.length >= 2 && ins_part[1].contains("not")) {
                false_prob = cons_count.doubleValue() / dom_count.doubleValue();
                true_prob = 1.0 - true_prob;
              } else {
                true_prob = cons_count.doubleValue() / dom_count.doubleValue();
                false_prob = 1.0 - true_prob;
              }

              //end: additional code for counting secret dependent branches
              jsonItem = jsonItem.replace("\"secret_dependent_branch\" : \"branch\"", "\"secret_dependent_branch\" : \"true\", \"true_branch_probability\" : \"" + true_prob + "\", \"false_branch_probability\" : \"" + false_prob + "\"");




              MarkovChainInformation trueChain = new MarkovChainInformation(fromNode,trueNode,Double.toString(true_prob),false, jsonItem.contains("$assertionsDisabled"), false);
              MarkovChainInformation falseChain = new MarkovChainInformation(fromNode,falseNode,Double.toString(false_prob),false, jsonItem.contains("$assertionsDisabled"), false);
              transitionMap.put(new Pair<>(fromNode,trueNode), trueChain);
              transitionMap.put(new Pair<>(fromNode,falseNode), falseChain);

              List<MarkovChainInformation> list = new ArrayList<>();
              list.add(trueChain);
              list.add(falseChain);
              transitionlistMap.put(fromNode, list);

              graphOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + true_prob + "\"];\n";
              graphOutput += "\t" + fromNode + " -> " + falseNode + "[label= " + "\"" + false_prob + "\"];\n";
              prismModel += "\t" + "[] s = " + fromNode + " -> " + true_prob + " : " + "(s' = " + trueNode + ") + " + false_prob + " : " + "(s' = " + falseNode + ");\n";

                String[] splittedID = jsonItemID.split("#");
                String idModelCount = splittedID[0]+"#"+splittedID[splittedID.length-1];
                long finishTime = System.currentTimeMillis();
                long elapsedTime = finishTime - startTime;
                modelCountingTimeMap.put(idModelCount, elapsedTime);
            }
          } else {
            MarkovChainInformation trueChain = new MarkovChainInformation(fromNode,trueNode,"1.0",false, jsonItem.contains("$assertionsDisabled"), false);
            MarkovChainInformation falseChain = new MarkovChainInformation(fromNode,falseNode,"1.0",false, jsonItem.contains("$assertionsDisabled"), false);
            transitionMap.put(new Pair<>(fromNode,trueNode), trueChain);
            transitionMap.put(new Pair<>(fromNode,falseNode), falseChain);

            List<MarkovChainInformation> list = new ArrayList<>();
            list.add(trueChain);
            list.add(falseChain);
            transitionlistMap.put(fromNode, list);

            graphOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + "1.0" + "\"];\n";
            graphOutput += "\t" + fromNode + " -> " + falseNode + "[label= " + "\"" + "1.0" + "\"];\n";
            prismModel += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + trueNode + ");\n";
            prismModel += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + falseNode + ");\n";
          }
        } else {
          List<String> edgeList = edgeMap.get(fromNode);
          if (edgeList == null) {
            edgeList = new ArrayList<>();
          }
          edgeList.add(trueNode);
          edgeMap.put(fromNode,edgeList);

          MarkovChainInformation trueChain = new MarkovChainInformation(fromNode,trueNode,"1.0",false, false, false);
          transitionMap.put(new Pair<>(fromNode,trueNode), trueChain);
          List<MarkovChainInformation> list = new ArrayList<>();
          list.add(trueChain);
          transitionlistMap.put(fromNode, list);

          graphOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + "1.0" + "\"];\n";
          prismModel += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + trueNode + ");\n";
        }
      } else {
        String fromNode = getNodeFromID(jsonItemID);

        String[] outgoingNodeStringArr = jsonItem.split("\"outgoing\" : \\{ ");

        String outgoingNodeString = "";

        if(outgoingNodeStringArr.length > 1)
            outgoingNodeString = outgoingNodeStringArr[1].split(" }")[0];

        if(outgoingNodeString.equals(""))
            continue;

        String[] outgoingNodes = outgoingNodeString.split(",");

        String selectedOutgoing = "";
        String nonSelectedOutgoing = "";

        if(outgoingNodes.length > 1) {
            selectedOutgoing = outgoingNodes[1];
            nonSelectedOutgoing = outgoingNodes[0];
        } else {
            selectedOutgoing = outgoingNodes[0];
        }

        if (selectedOutgoing.contains("#")) {
          String toJsonItemID = selectedOutgoing.split("\"")[1];
          String toNode = getNodeFromID(toJsonItemID);


          String[] jIDs = jsonItemID.split("#");
          String[] toJIDs = toJsonItemID.split("#");

          String id1=jIDs[0];
          String id2=toJIDs[0];
          for(int j=1; j < jIDs.length-1; j++) {
            id1 += "#"+jIDs[j];
          }
          for(int j=1; j < toJIDs.length-1; j++) {
            id2 += "#"+toJIDs[j];
          }

          String checkID1 = jIDs[0]+"#"+jIDs[1];
          String checkID2 = toJIDs[0]+"#"+toJIDs[1];

          if(!id1.equals(id2) && !jsonItemID.contains("1001001")) {
              procCallMap.put(checkID1,checkID2);
              interProcDomMap.put(toNode, fromNode);
              interProcPostDomMap.put(fromNode,toNode);
          }

          List<String> edgeList = edgeMap.get(fromNode);
          if (edgeList == null) {
            edgeList = new ArrayList<>();
          }
          edgeList.add(toNode);
          edgeMap.put(fromNode,edgeList);

          MarkovChainInformation trueChain = new MarkovChainInformation(fromNode,toNode,"1.0",false, false, false);
          transitionMap.put(new Pair<>(fromNode,toNode), trueChain);
          List<MarkovChainInformation> list = new ArrayList<>();
          list.add(trueChain);
          transitionlistMap.put(fromNode, list);

          graphOutput += "\t" + fromNode + " -> " + toNode + "[label= " + "\"" + "1.0" + "\"];\n";

          if (!nonSelectedOutgoing.equals("") && nonSelectedOutgoing.contains("#")) {
              String ignoredJsonItemID = nonSelectedOutgoing.split("\"")[1];
              String ignoredNode = getNodeFromID(ignoredJsonItemID);

              MarkovChainInformation ignoredChain = new MarkovChainInformation(fromNode,ignoredNode,"0.0",false, false, false);
              transitionMap.put(new Pair<>(fromNode,ignoredNode), ignoredChain);
              List<MarkovChainInformation> list2 = transitionlistMap.get(fromNode);
              list2.add(ignoredChain);
              transitionlistMap.put(fromNode, list2);

              graphOutput += "\t" + fromNode + " -> " + toNode + "[label= " + "\"" + "0.0" + "\"];\n";
              prismModel += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + toNode + ") + " + "0.0" + " : " + "(s' = " + ignoredNode + ");\n";
          } else {
              prismModel += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + toNode + ");\n";
          }
        } else {
          MarkovChainInformation trueChain = new MarkovChainInformation(fromNode,fromNode,"1.0",false, false, false);
          transitionMap.put(new Pair<>(fromNode,fromNode), trueChain);
          List<MarkovChainInformation> list = new ArrayList<>();
          list.add(trueChain);
          transitionlistMap.put(fromNode, list);

          graphOutput += "\t" + fromNode + " -> " + fromNode + "[label= " + "\"" + "1.0" + "\"];\n";
          prismModel += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + fromNode + ");\n";

          endNode = fromNode;
        }
      }
    }

    graphOutput += "}";
    System.out.println(graphOutput);

    prismModel += "\nendmodule";
    System.out.println(prismModel);

      long dfs = System.currentTimeMillis();
      long det = dfs - dts;

    //dominator analysis for probability distribution to true or false branch
    //++removing extra analyis where branching and merging does not effect result
    String id = idMap.get(Integer.parseInt(assertionReachabilityNode));
    String[] splittedID = id.split("#");
    Procedure proc = itemProcMap.get(splittedID[0]);
    ISSABasicBlock node = itemNodeMap.get(splittedID[0]+"#"+splittedID[splittedID.length-1]);
    Set<ISSABasicBlock> domSet = proc.getDominatorSet(node);


      long dts2 = System.currentTimeMillis();

    /* For all the dominators of assertionReachability node if there is no direct connection between a pair of dominators
     * 1. make a direct connection with probability 1.0
     * 2. remove all in between nodes and the transitions, it can be tricky, start from the dominator for which there
     * was no edge to the other dominator node. From both false and true node remove all the nodes untill it reaches the
     * other dominator node
     * */

    ISSABasicBlock prevImDom = node;
    ISSABasicBlock imDom = proc.getImmediateDominator(node);

    String nodeItem = nodeItemMap.get(imDom);
    String imDomNode = "";
    if(nodeMap.get(nodeItem) == null) {
      String[] nodeItemArr = nodeItem.split("#");
      nodeItem = nodeItemArr[0] + "#1#" + nodeItemArr[nodeItemArr.length-1];
    }
      imDomNode = Integer.toString(nodeMap.get(nodeItem));
      if(procCallMap.containsKey(nodeItemMap.get(imDom))) {

          String idProc = procCallMap.get(nodeItemMap.get(imDom));
          Procedure procedure = itemProcMap.get(idProc.split("#")[0]);

          while(!idProc.equals("")) {
              for (ISSABasicBlock nd : procedure.getNodeSet()) {
                  String n = nodeItemMap.get(nd);
                  if (modelCountingTimeMap.get(n) != null)
                      extraModelCountingTime += modelCountingTimeMap.get(n);
              }
              if(procCallMap.containsKey(idProc)) {
                  idProc = procCallMap.get(idProc);
                  procedure = itemProcMap.get(idProc.split("#")[0]);
              } else {
                  idProc = "";
              }
          }
      }
//      if(interProcDomMap.get(imDomNode) != null) {
//          imDomNode = interProcDomMap.get(imDomNode);
//          String imDomNodeID = idMap.get(Integer.parseInt(imDomNode));
//          String[] imDomNodeIDsplitted = imDomNodeID.split("#");
//          imDomNodeID = imDomNodeIDsplitted[0] + "#"+ imDomNodeIDsplitted[imDomNodeIDsplitted.length-1];
//          imDom = itemNodeMap.get(imDomNodeID);
//      }

    while(imDom != null) {
      //do something
      String ns = nodeItemMap.get(imDom);
      if(nodeMap.get(ns) == null) {
//          String[] itIDArr = ns.split("#");
//          String itID = itIDArr[0] + "#" + itIDArr[itIDArr.length-1];
//          String updatedNS = replaceMap.get("\""+itID+"\"");
          String updatedNS = replaceMap.get("\""+ns+"\"");
//          if(itIDArr.length == 3) {
//            String[] updateditIDArr = updatedNS.split("#");
//            updatedNS = updateditIDArr[0] + "#" + itIDArr[1] + "#" + itIDArr[itIDArr.length - 1];
//          }
          if(updatedNS != null) {
            ns = updatedNS.substring(1,updatedNS.length()-1);
            if(nodeMap.get(ns) == null) {
              ns = ns.split("#")[0] + "#1#" + ns.split("#")[1]; //todo: change this implementation
            }
          }
      }
      if(nodeMap.get(ns) != null) {
        String fromNode = Integer.toString(nodeMap.get(ns));

        ns = nodeItemMap.get(prevImDom);
          if(nodeMap.get(ns) == null) {
//              String[] itIDArr = ns.split("#");
//              String itID = itIDArr[0] + "#" + itIDArr[itIDArr.length-1];
//              String updatedNS = replaceMap.get("\""+itID+"\"");
            String updatedNS = replaceMap.get("\""+ns+"\"");
//              if(itIDArr.length == 3) {
//                String[] updateditIDArr = updatedNS.split("#");
//                updatedNS = updateditIDArr[0] + "#" + itIDArr[1] + "#" + itIDArr[itIDArr.length - 1];
//              }
            if(updatedNS != null) {
                ns = updatedNS.substring(1,updatedNS.length()-1);
                if(nodeMap.get(ns) == null) {
                    ns = ns.split("#")[0] + "#1#" + ns.split("#")[1]; //todo: change this implementation
                }
            }
          }
        if (nodeMap.get(ns) != null) {
          String toNode = Integer.toString(nodeMap.get(ns));

          List<MarkovChainInformation> toList = transitionlistMap.get(fromNode);
          boolean flagToUpdate = true;
          ISSABasicBlock fNode = itemNodeMap.get(idMap.get(Integer.parseInt(fromNode)));
          ISSABasicBlock tNode = itemNodeMap.get(idMap.get(Integer.parseInt(toNode)));
          if(fromNode.equals(toNode) || !proc.getPostDominatorSet(fNode).contains(tNode) || toList.size() == 1) {
              flagToUpdate = false;
          }
//          for (MarkovChainInformation mi : toList) {
//            if (mi.getToNode().equals(toNode) /*&& mi.getProb().equals("1.0")*/) { // to make sure that there is no connection between dominators
//              flagToUpdate = false;
//              break;
//            }
//          }


          if (flagToUpdate) {
            MarkovChainInformation directChain = new MarkovChainInformation(fromNode, toNode, "1.0", false, false, false);
            List<MarkovChainInformation> list = new ArrayList<>();
            list.add(directChain);
            transitionlistMap.put(fromNode, list);
              String m = idMap.get(Integer.parseInt(fromNode));
              if(modelCountingTimeMap.get(m) != null) {
                  //numberofNodesReduced += 2;
                  extraModelCountingTime += modelCountingTimeMap.get(m);
              }


            for (MarkovChainInformation mi : toList) {
              if (!mi.getToNode().equals(toNode)) {
                boolean[] removeVisited = new boolean[numberofNodes+1];
                removeAllTransitions(mi.getToNode(), toNode,proc, removeVisited);
                ISSABasicBlock aNode = itemNodeMap.get(idMap.get(Integer.parseInt(toNode)));
                ISSABasicBlock mNode = itemNodeMap.get(idMap.get(Integer.parseInt(mi.getToNode())));
                Set<ISSABasicBlock> aNodeDominatorSet = proc.getDominatorSet(aNode);
                if(toNode.equals(assertionNode) && !aNodeDominatorSet.contains(mNode)) {
                    //numberofNodesReduced += 1;
                    transitionlistMap.remove(mi.getToNode());
                }
                m = idMap.get(Integer.parseInt(mi.getToNode()));
                if(modelCountingTimeMap.get(m) != null) {
                    //numberofNodesReduced += 2;
                    extraModelCountingTime += modelCountingTimeMap.get(m);
                }
              }
            }
          }
        }
      }

        prevImDom = imDom;
        imDom = proc.getImmediateDominator(imDom);
//        if(procCallMap.containsKey(nodeItemMap.get(imDom))) {
//
//            String idProc = procCallMap.get(nodeItemMap.get(imDom));
//            Procedure procedure = itemProcMap.get(idProc.split("#")[0]);
//
//            while(!idProc.equals("")) {
//                for (ISSABasicBlock nd : procedure.getNodeSet()) {
//                    numberofNodesReduced++;
//                    String n = nodeItemMap.get(nd);
//                    if (modelCountingTimeMap.get(n) != null)
//                        extraModelCountingTime += modelCountingTimeMap.get(n);
//                }
//                if(procCallMap.containsKey(idProc)) {
//                    idProc = procCallMap.get(idProc);
//                    procedure = itemProcMap.get(idProc.split("#")[0]);
//                } else {
//                    idProc = "";
//                }
//            }
//        }


      if(imDom.equals(prevImDom))
        break;
    }

    long dfs2 = System.currentTimeMillis();
    long det2 = dfs2 - dts2;



    //unrolling loop
    long dts3 = System.currentTimeMillis();

    boolean[] visited = new boolean[numberofNodes+1];
    boolean[] recStack = new boolean[numberofNodes+1];


    isCyclicUtil(0, 0, visited, recStack);

    //to remove all the unnecessary branching which are not relared to assertion node
    String aNodeID = idMap.get(Integer.parseInt(assertionNode));
    if(itemNodeMap.get(aNodeID) == null) {
      String[] aNodeIDArr = aNodeID.split("#");
      aNodeID = aNodeIDArr[0] + "#" + aNodeIDArr[aNodeIDArr.length-1];
    }
    ISSABasicBlock aNode = itemNodeMap.get(aNodeID);
    aNode = proc.getImmediateDominator(aNode);

    Set<ISSABasicBlock> assertionDomSet = proc.getDominatorSet(aNode);


    //Add nodes from other methods in the dominator set
    Set<ISSABasicBlock> tempSet = new HashSet<>();
    for(ISSABasicBlock domNode : assertionDomSet) {
      String itemID = nodeItemMap.get(domNode);

      if(nodeMap.get(itemID) == null) {
        //String[] itemIDArr = itemID.split("#");
        //itemID = itemIDArr[0] + "#" + itemIDArr[itemIDArr.length - 1];
        if(replaceMap.containsKey("\""+itemID+"\"")) {

          itemID = replaceMap.get("\""+itemID+"\"");

//          if(itemIDArr.length == 3) {
//            String[] upitemIDArr = itemID.split("#");
//            itemID = upitemIDArr[0] + "#" + itemIDArr[1] + "#" + itemIDArr[upitemIDArr.length - 1];
//          }

          itemID = itemID.substring(1,itemID.length()-1);
        }
        if(nodeMap.get(itemID) == null) {
          String[] itemIDArr = itemID.split("#");
          itemID = itemIDArr[0] + "#1#" + itemIDArr[itemIDArr.length - 1];
          if(replaceMap.containsKey("\""+itemID+"\"")) {
            itemID = replaceMap.get("\""+itemID+"\"");
            itemID = itemID.substring(1,itemID.length()-1);
          }
        }
      }

      String item = Integer.toString(nodeMap.get(itemID));

      if(interProcDomMap.containsKey(item)) {
        String newItemID = idMap.get(Integer.parseInt(interProcDomMap.get(item)));
        String[] newItemIDArr = newItemID.split("#");
        newItemID = newItemIDArr[0] + "#" + newItemIDArr[newItemIDArr.length-1];
        ISSABasicBlock newNode = itemNodeMap.get(newItemID);
        Procedure newProc = itemProcMap.get(newItemID.split("#")[0]);
        tempSet.addAll(newProc.getDominatorSet(newNode));
      }
    }

    assertionDomSet.addAll(tempSet);

    for (Map.Entry<String,List<MarkovChainInformation>> entry : transitionlistMap.entrySet()) {
      String eNodeID = idMap.get(Integer.parseInt(entry.getKey()));
      if(itemNodeMap.get(eNodeID) == null) {
        String[] eNodeIDArr = eNodeID.split("#");
        eNodeID = eNodeIDArr[0] + "#" + eNodeIDArr[eNodeIDArr.length-1];
      }
      ISSABasicBlock eNode = itemNodeMap.get(eNodeID);

      //if(!nodeItemMap.get(aNode).split("#")[0].equals(nodeItemMap.get(eNode).split("#")[0]))
      //  continue;
      if(eNode != null && !assertionDomSet.contains(eNode) && entry.getValue().size() == 2) {
        List<MarkovChainInformation> miList = entry.getValue();
        miList.clear();
        Procedure nodeProc = itemProcMap.get(nodeItemMap.get(eNode).split("#")[0]);
        ISSABasicBlock directNode = nodeProc.getImmediatePostDominator(eNode);
        String eNodeItem = nodeItemMap.get(eNode);
        if(nodeMap.get(eNodeItem) == null) {
          String[] eNodeItemArr = eNodeItem.split("#");
          eNodeItem = eNodeItemArr[0] + "#1#" + eNodeItemArr[eNodeItemArr.length-1];
        }
        if(nodeMap.get(eNodeItem) == null) {
          eNodeItem = replaceMap.get("\""+eNodeItem+"\"");
          eNodeItem = eNodeItem.substring(1,eNodeItem.length()-1);
        }
        String eNodeString = Integer.toString(nodeMap.get(eNodeItem));

        String directNodeItem = nodeItemMap.get(directNode);
        if(nodeMap.get(directNodeItem) == null) {
          String[] directNodeItemArr = directNodeItem.split("#");
          directNodeItem = directNodeItemArr[0] + "#1#" + directNodeItemArr[directNodeItemArr.length-1];
        }
        if(nodeMap.get(directNodeItem) == null) {
          directNodeItem = replaceMap.get("\""+directNodeItem+"\"");
          directNodeItem = directNodeItem.substring(1,directNodeItem.length()-1);
        }
        String directNodeString = Integer.toString(nodeMap.get(directNodeItem));
        MarkovChainInformation directChain = new MarkovChainInformation(eNodeString, directNodeString, "1.0", false, false, false);
        miList.add(directChain);
        transitionlistMap.put(entry.getKey(),miList);

        String[] eNodeItemArr = eNodeItem.split("#");
        eNodeItem = eNodeItemArr[0] + "#" + eNodeItemArr[eNodeItemArr.length-1];
        if(modelCountingTimeMap.get(eNodeItem) != null) {
          extraModelCountingTime += modelCountingTimeMap.get(eNodeItem);
        }
      }
    }

    //code to remove unnecessary nodes after domination analysis
    visitedToCheck = new boolean[numberofNodes+1];
    dfsToCheck("0");
    for(int idx=0; idx < visitedToCheck.length-1; idx++) {
        if(!visitedToCheck[idx]) {
            transitionlistMap.remove(Integer.toString(idx));
            numberofNodesReduced++;
        }
    }


    String markovChainOutput = "digraph {\n";
    String prismOutput = "dtmc\n\n" + "module " + modelName + "\n\n";
    if(loopUnrolling && loopbound > 0 && backEdgeExists)
      prismOutput += "\t" + "s : [0.." + (numberofNodes * loopbound) +"] init 0;\n\n";
    else
      prismOutput += "\t" + "s : [0.." + (numberofNodes) +"] init 0;\n\n";


    for (Map.Entry<String, List<MarkovChainInformation>> entry : transitionlistMap.entrySet()) {
      List<MarkovChainInformation> mChainList = entry.getValue();

      String fromNode = entry.getKey();

      if(mChainList.size() >= 1) {
        MarkovChainInformation trueChain = mChainList.get(0);
        String trueNode = trueChain.getToNode();
        String trueNodeProb = trueChain.getProb();
        boolean depNode = trueChain.isDepBranchNode();
        boolean assertNode = trueChain.isAssertNode();
        String falseNode = "", falseNodeProb = "";
        if(mChainList.size() == 2) {
          MarkovChainInformation falseChain = mChainList.get(1);
          falseNode = falseChain.getToNode();
          falseNodeProb = falseChain.getProb();

          if(depNode) {
            markovChainOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + trueNodeProb + "\"];\n";
            markovChainOutput += "\t" + fromNode + " -> "  + falseNode + "[label= " + "\"" + falseNodeProb + "\"];\n";
            prismOutput += "\t" + "[] s = " + fromNode + " -> " + trueNodeProb + " : " + "(s' = " + trueNode + ") + " + falseNodeProb + " : " + "(s' = " + falseNode + ");\n";

          } else {
            if(assertNode) {
              markovChainOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + trueNodeProb + "\"];\n";
              markovChainOutput += "\t" + fromNode + " -> " + falseNode + "[label= " + "\"" + falseNodeProb + "\"];\n";
              prismOutput += "\t" + "[] s = " + fromNode + " -> " + trueNodeProb + " : " + "(s' = " + trueNode + ") + " + falseNodeProb + " : " + "(s' = " + falseNode + ");\n";
            } else {

              String fID = idMap.get(Integer.parseInt(falseNode));
              String[] splittedfID = fID.split("#");
              ISSABasicBlock fNode = itemNodeMap.get(splittedfID[0]+"#"+splittedfID[splittedfID.length-1]);

              String tID = idMap.get(Integer.parseInt(trueNode));
              String[] splittedtID = tID.split("#");
              ISSABasicBlock tNode = itemNodeMap.get(splittedtID[0]+"#"+splittedtID[splittedtID.length-1]);

              if(domSet.contains(tNode) && !domSet.contains(fNode)) {
                markovChainOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + "1.0" + "\"];\n";
                markovChainOutput += "\t" + fromNode + " -> " + falseNode + "[label= " + "\"" + "0.0" + "\"];\n";
                prismOutput += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + trueNode + ") + " + "0.0" + " : " + "(s' = " + falseNode + ");\n";
              }

              else if(domSet.contains(fNode) && !domSet.contains(tNode)) {
                markovChainOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + "0.0" + "\"];\n";
                markovChainOutput += "\t" + fromNode + " -> " + falseNode + "[label= " + "\"" + "1.0" + "\"];\n";
                prismOutput += "\t" + "[] s = " + fromNode + " -> " + "0.0" + " : " + "(s' = " + trueNode + ") + " + "1.0" + " : " + "(s' = " + falseNode + ");\n";
              }

              else if(!domSet.contains(fNode) && !domSet.contains(tNode)) {
                //Need to change this later
                markovChainOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + "1.0" + "\"];\n";
                markovChainOutput += "\t" + fromNode + " -> " + falseNode + "[label= " + "\"" + "1.0" + "\"];\n";
                prismOutput += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + trueNode + ");\n";
                prismOutput += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + falseNode + ");\n";
              }
              else {
                //this case is not possible, false and true node both being in dominator set
              }
            }
          }
        } else{
          markovChainOutput += "\t" + fromNode + " -> " + trueNode + "[label= " + "\"" + "1.0" + "\"];\n";
          prismOutput += "\t" + "[] s = " + fromNode + " -> " + "1.0" + " : " + "(s' = " + trueNode + ");\n";
        }
      }
    }

    markovChainOutput += "}";
    prismOutput += "\nendmodule";


    System.out.println(markovChainOutput);

    System.out.println(prismOutput);


      long dfs3 = System.currentTimeMillis();
      long det3 = dfs3 - dts3;

      long dts4 = System.currentTimeMillis();


    String fileName = currentCFG.getProcedure().getClassName().replace("/","_") + "_" + currentCFG.getProcedure().getProcedureName() + ".dot";
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      writer.write(markovChainOutput);
      writer.close();
    } catch(IOException ex) {
      System.out.println(ex);
    }

    String model_file = currentCFG.getProcedure().getClassName().replace("/","_") + "_" + currentCFG.getProcedure().getProcedureName() + ".sm";
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(model_file));
      writer.write(prismOutput);
      writer.close();
    } catch(IOException ex) {
      System.out.println(ex);
    }

    String assertionReachabilitySpec = "";
    String assertionExecutionSpec = "";

    if (!assertionReachabilityNode.equals("")) {
      if(loopUnrolling && loopbound > 1 && backEdgeExists) {
        assertionReachabilitySpec = "P=? [F (s = " + assertionReachabilityNode + ")";
        for(int b=1; b<loopbound; b++) {
          String bNode = Integer.toString(Integer.parseInt(assertionReachabilityNode) + b * numberofNodes);
          assertionReachabilitySpec += " | (s = " + bNode + ")";
        }
        assertionReachabilitySpec += "]";
      } else {
        assertionReachabilitySpec = "P=? [F s = " + assertionReachabilityNode + "]";
      }
    }

    //if (!assertionExecutionNode.equals(""))
    //  assertionExecutionSpec = "P=? [F s = " + assertionExecutionNode + "]";

    System.out.println("assertionReachabilitySpec: " + assertionReachabilitySpec);
    //System.out.println("assertionExecutionSpec: " + assertionExecutionSpec);

    int num_properties = 0;
    String proerties_file = currentCFG.getProcedure().getClassName().replace("/","_") + "_" + currentCFG.getProcedure().getProcedureName() + ".csl";
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(proerties_file));
      if(!assertionReachabilitySpec.equals("") && !assertionExecutionSpec.equals("")) {
        writer.write(assertionReachabilitySpec + "\n" + assertionExecutionSpec);
        num_properties = 2;
      }
      else if (!assertionReachabilitySpec.equals("")) {
        writer.write(assertionReachabilitySpec);
        num_properties = 1;
      }

      writer.close();
    } catch(IOException ex) {
      System.out.println(ex);
    }


    long p1timeElapsed=0,p2timeElapsed=0;
    if (num_properties >= 1) {
      long p1start = System.currentTimeMillis();
      Process proc1 = null;
      try {
        proc1 = Runtime.getRuntime().exec(new String[]{"/home/seem/Downloads/prism-4.5-linux64/bin/prism", model_file, proerties_file, "-prop", "1"});

        if (proc1 == null) return;
        ;

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc1.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc1.getErrorStream()));

        // Read the output from the command
        String s = null;
//      if ((s = stdInput.readLine()) != null) {
//        System.out.println("PRISM run output:\n");
//      }
        while ((s = stdInput.readLine()) != null) {
          System.out.println(s);
          if (s.contains("Result: ")) {
            String prob = s.split("Result: ")[1].split(" ")[0];
            System.out.println("Probability to reach assertion: " + prob);
          }
        }

        // Read any errors from the attempted command
        if ((s = stdError.readLine()) != null)
          System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
          System.out.println(s);
        }
      } catch (Exception ex) {
        System.out.println(ex);
      }
      long p1finish = System.currentTimeMillis();
      p1timeElapsed = p1finish - p1start;
    }

    if(num_properties == 2) {
      long p2start = System.currentTimeMillis();
      Process proc2 = null;
      try {
        proc2 = Runtime.getRuntime().exec(new String[]{"/home/seem/Downloads/prism-4.5-linux64/bin/prism", model_file, proerties_file, "-prop", "2"});

        if (proc2 == null) return;
        ;

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc2.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc2.getErrorStream()));

        // Read the output from the command
        String s = null;
//      if ((s = stdInput.readLine()) != null) {
//        System.out.println("PRISM run output:\n");
//      }
        while ((s = stdInput.readLine()) != null) {
          if (s.contains("Result: ")) {
            String prob = s.split("Result: ")[1].split(" ")[0];
            System.out.println("Probability for assertion failure: " + prob);
          }
        }

        // Read any errors from the attempted command
        if ((s = stdError.readLine()) != null)
          System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
          System.out.println(s);
        }
      } catch (Exception ex) {
        System.out.println(ex);
      }
      long p2finish = System.currentTimeMillis();
      p2timeElapsed = p2finish - p2start;
    }

    long finish = System.currentTimeMillis();
    long timeElapsed = finish - start;

      long dfs4 = System.currentTimeMillis();
      long det4 = dfs4 - dts4;


    long totalExecutionTime = dependencyAnalysisTime + timeElapsed;

    long executionTimeWithDominatorAnalysis = totalExecutionTime - extraModelCountingTime;

    System.out.println("CFG construction time: " + MainFrame.cfgConsTime);
    System.out.println("Dependency analysis time: " + dependencyAnalysisTime + "ms");
    System.out.println("Main time: " + det + "ms");
    System.out.println("Branch condition preprocessing time: " + det5 + "ms");
    System.out.println("Dominator Analysis time: " + det2 + "ms");
    System.out.println("Loop unrolling time: " + det3 + "ms");
      System.out.println("File writing and PRISM time: " + det4 + "ms");
    System.out.println("Execution time for probabilistic analysis: " + timeElapsed + "ms");
    System.out.println("Total Execution time: " + totalExecutionTime + "ms");
    System.out.println("Total Execution time with dominator analysis: " + executionTimeWithDominatorAnalysis + "ms");
    float percentageOfNodesReduced = (float)numberofNodesReduced/(numberofNodes+numberofNodesMerged);
    percentageOfNodesReduced = percentageOfNodesReduced * 100;
    System.out.println("Number of nodes reduced in subgraph: " + numberofNodesReduced + "(" + percentageOfNodesReduced + "%)") ;
    long p1execTime = totalExecutionTime - p2timeElapsed;
    long p2execTime = totalExecutionTime - p1timeElapsed;
    System.out.println("Execution time for P1: " + p1execTime + "ms");
    System.out.println("Execution time for P2: " + p2execTime + "ms");
  }

  private void dfsToCheck(String i) {
      visitedToCheck[Integer.parseInt(i)] = true;

      List<MarkovChainInformation> miList = transitionlistMap.get(i);

      if(miList != null) {
          for(MarkovChainInformation mi : miList) {
              if(!visitedToCheck[Integer.parseInt(mi.getToNode())])
                dfsToCheck(mi.getToNode());
          }
      }
  }

  private boolean isCyclicUtil(int s, int i, boolean[] visited, boolean[] recStack) {

    // Mark the current node as visited and
    // part of recursion stack
    if (recStack[i])
      return true;

    if (visited[i])
      return false;

    visited[i] = true;

    recStack[i] = true;
    List<String> children = edgeMap.get(Integer.toString(i));

    if (children != null) {
      for (String c : children) {
        if (isCyclicUtil(i, Integer.parseInt(c), visited, recStack) && i != Integer.parseInt(c)) {
          System.out.println("backedge: " + i + " --> " + c);
          backEdgeExists = true;


          //dominator analysis for loop structure
          //if assertion node is dominated by loop condition but not dominated by backedge from node
          //then assertion node do not need to consider the other branch from loop condition
          //so, probability to assertion condition is 1.0 and the other branch is 0.0
          ISSABasicBlock backedgeFromNode = itemNodeMap.get(idMap.get(i));
          ISSABasicBlock backedgeToNode = itemNodeMap.get(idMap.get(Integer.parseInt(c)));


          String id = idMap.get(Integer.parseInt(assertionNode));
          String[] splittedID = id.split("#");
          Procedure proc = itemProcMap.get(splittedID[0]);
          ISSABasicBlock node = itemNodeMap.get(splittedID[0]+"#"+splittedID[splittedID.length-1]);
          node = proc.getImmediateDominator(node); //to ge the branch node from where it reaches to the assert node
          Set<ISSABasicBlock> domSet = proc.getDominatorSet(node);
          Set<ISSABasicBlock> postDomSet = proc.getPostDominatorSet(node);
          if(domSet.contains(backedgeToNode) && !postDomSet.contains(backedgeFromNode)) {
            String asserDomNode = Integer.toString(nodeMap.get(nodeItemMap.get(node)));
            List<MarkovChainInformation> miList = null;
            if(!backedgeToNode.getLastInstruction().toString().contains("conditional")) {
              miList = transitionlistMap.get(Integer.toString(Integer.parseInt(c)+1)); //loop backedgetonode is not conditional branch
            } else {
              miList = transitionlistMap.get(c);
            }
            boolean flagToUpDateProb = false;
            for(MarkovChainInformation mi : miList) {
                ISSABasicBlock miToNode = itemNodeMap.get(idMap.get(Integer.parseInt(mi.getToNode())));
                if(domSet.contains(miToNode)) {
                    flagToUpDateProb = true;
                    backEdgeExists = false;
                    break;
                }
            }
            List<String> miListTORemove = new ArrayList<>();
            if(flagToUpDateProb) {
              //MarkovChainInformation miToRemove = null;
              for (MarkovChainInformation mi : miList) {
//                if (mi.getToNode().equals(asserDomNode)) {
//                  //mi.updateProb("1.0");
//                } else {
                  //miToRemove = mi;
                  ISSABasicBlock newBackedgeToNode = null;
                  if(!backedgeToNode.getLastInstruction().toString().contains("conditional")) {
                    newBackedgeToNode = itemNodeMap.get(idMap.get(Integer.parseInt(c)+1));
                  } else {
                    newBackedgeToNode = itemNodeMap.get(idMap.get(Integer.parseInt(c)));
                  }
                  ISSABasicBlock newNode = backedgeFromNode;
                  ISSABasicBlock prevNewNode = newNode;
                  while(!newNode.equals(newBackedgeToNode)) {
                    newNode = proc.getImmediateDominator(newNode);
                    String prevNewNodeID = Integer.toString(nodeMap.get(nodeItemMap.get(prevNewNode)));
                    if(transitionlistMap.get(prevNewNodeID) != null) {
                      for (MarkovChainInformation m : transitionlistMap.get(prevNewNodeID)) {
                        if(!m.getToNode().equals(c)) {
                          ISSABasicBlock nodeToCheck = itemNodeMap.get(idMap.get(Integer.parseInt(m.getToNode())));
                          Set<ISSABasicBlock> befDomSet = proc.getDominatorSet(backedgeFromNode);
                          if(!befDomSet.contains(nodeToCheck)) {
                            flagToUpDateProb = false;
                            backEdgeExists = true;
                            break;
                          }
                        }
                      }
                    }
                    if(!flagToUpDateProb)
                        break;

                    miListTORemove.add(prevNewNodeID);
                    prevNewNode = newNode;
                  }
                //}
              }
            }
              if(flagToUpDateProb) {
                  MarkovChainInformation miToRemove = null;
                  for (MarkovChainInformation mi : miList) {
                      ISSABasicBlock miToNode = itemNodeMap.get(idMap.get(Integer.parseInt(mi.getToNode())));
                      if(domSet.contains(miToNode)) {
                          mi.updateProb("1.0");
                          String m = idMap.get(Integer.parseInt(mi.getFromNode()));
                          if(modelCountingTimeMap.get(m) != null) {
                              extraModelCountingTime += modelCountingTimeMap.get(m);
                          }
                      } else {
                          miToRemove = mi;
                      }
                  }
                  miList.remove(miToRemove);
                  for(String m : miListTORemove) {
                      transitionlistMap.remove(m);
                      m = idMap.get(Integer.parseInt(m));
                      if(modelCountingTimeMap.get(m) != null) {
                          extraModelCountingTime += modelCountingTimeMap.get(m);
                      }
                  }
              }
          }


          //unroll loops
          if(loopUnrolling && loopbound > 1 && backEdgeExists) {
            int newNode = Integer.parseInt(c) + numberofNodes;
            //edgeMap.get(Integer.toString(i)).remove(c);
            //edgeMap.get(Integer.toString(i)).add(Integer.toString(newNode));
            //transitionMap.put(new Pair<>(Integer.toString(i), c), transitionMap.get(new Pair<>(Integer.toString(i), c)).updateToNode(Integer.toString(newNode)));
            //transitionMap.reprob = "1.0"move(new Pair<>(Integer.toString(i), c));

            MarkovChainInformation chain = null;
            if (loopbound > 0) {
              chain = transitionMap.get(new Pair<>(Integer.toString(i), c)).updateToNode(Integer.toString(newNode));
            } else {
              chain = transitionMap.get(new Pair<>(Integer.toString(i), c)).updateToNode(endNode);
            }
            transitionMap.put(new Pair<>(Integer.toString(i), c), chain);
            List<MarkovChainInformation> list = new ArrayList<>();
            list.add(chain);
            transitionlistMap.put(Integer.toString(i), list);

            transitionMap.remove(new Pair<>(Integer.toString(i), c));
            List<MarkovChainInformation> oldList = transitionlistMap.get(Integer.toString(i));
            for (MarkovChainInformation m : oldList) {
              if (m.getToNode().equals(c))
                oldList.remove(m);
            }

            int be_from = i;
            List<Integer> beFromList = new ArrayList<>();
            beFromList.add(be_from);
            int be = Integer.parseInt(c);
            List<Integer> beList = new ArrayList<>();
            beList.add(be);
            for (int x = 0; x < loopbound - 1; x++) {
              boolean[] visitedUnroll = new boolean[numberofNodes];
              dfsToAddUnrolledNodes(Integer.parseInt(c), visitedUnroll, be_from, be, x, beList, beFromList, c);
              be_from = be_from + numberofNodes;
              be = be + numberofNodes;
              beFromList.add(be_from);
              beList.add(be);
            }
          }
        }
      }
    }


    recStack[i] = false;

    return false;
  }

  private void dfsToAddUnrolledNodes(int i, boolean[] visited, int be_from, int be, int x, List<Integer> beList, List<Integer> beFromList, String beTONode) {

    if (visited[i])
      return;

    visited[i] = true;

    int bounded_node = (numberofNodes * (x+1));

    String from = Integer.toString(i+bounded_node);
    List<String> children = edgeMap.get(Integer.toString(i));
    List<String> fromChildren = new ArrayList<>();

    if (children != null) {
      for (String c : children) {
        if (beList.contains(Integer.parseInt(c)))
          continue;
        if (Integer.parseInt(c) > bounded_node)
          continue;
        //if (Integer.parseInt(c) != numberofNodes) {
        String to = "";
        if(Integer.parseInt(c) == /*numberofNodes-1*/Integer.parseInt(endNode))
          to = c;
        else
          to = Integer.toString(Integer.parseInt(c) + bounded_node);
        fromChildren.add(to);
        //edgeMap.put(from, fromChildren);

        MarkovChainInformation mChain = transitionMap.get(new Pair<>(Integer.toString(i),c));
        if(mChain == null)
          continue;
        MarkovChainInformation chain = new MarkovChainInformation(from, to, mChain.getProb(), mChain.isDepBranchNode(), mChain.isAssertNode(), mChain.isExceptionNode());
        transitionMap.put(new Pair<>(from, to), chain);

        List<MarkovChainInformation> list = new ArrayList<>();
        if(transitionlistMap.get(from) != null) {
          list = transitionlistMap.get(from);
        }
        list.add(chain);
        transitionlistMap.put(from, list);

        if(beFromList.contains(Integer.parseInt(c)) && !visited[Integer.parseInt(c)]) {
          if(x == loopbound-2) {
            ISSABasicBlock backedgeToNode = itemNodeMap.get(idMap.get(Integer.parseInt(beTONode)));
            if(!backedgeToNode.getLastInstruction().toString().contains("conditional")) {
              beTONode = Integer.toString(Integer.parseInt(beTONode) + 1);
            }
            List<MarkovChainInformation> listLoop = new ArrayList<>();
            if (transitionlistMap.get(beTONode) != null) {
              listLoop = transitionlistMap.get(beTONode);
            }
            for(MarkovChainInformation m : listLoop) {
              ISSABasicBlock mnode = itemNodeMap.get(idMap.get(Integer.parseInt(m.getToNode())));

              int origNode = Integer.parseInt(to) % numberofNodes;
              ISSABasicBlock node = itemNodeMap.get(idMap.get(origNode));
              String id = idMap.get(origNode);
              String[] splittedID = id.split("#");
              Procedure proc = itemProcMap.get(splittedID[0]);
              Set<ISSABasicBlock> domSet = proc.getDominatorSet(node);

              if(!domSet.contains(mnode)) {
                if(transitionMap.get(new Pair<>(to, Integer.toString(Integer.parseInt(m.getToNode()) + (numberofNodes*(x+1))))) == null) {
                  MarkovChainInformation chain2 = new MarkovChainInformation(to, Integer.toString(Integer.parseInt(m.getToNode()) + (numberofNodes*(x+1))), "1.0", false, false, false);
                  transitionMap.put(new Pair<>(to, Integer.toString(Integer.parseInt(m.getToNode()) + (numberofNodes*(x+1))) + (numberofNodes*(x+1))), chain2);

                  List<MarkovChainInformation> list2 = new ArrayList<>();
                  if (transitionlistMap.get(to) != null) {
                    list2 = transitionlistMap.get(to);
                  }
                  list2.add(chain2);
                  transitionlistMap.put(to, list2);
                }
                break;
              }
            }
          } else {
            if(transitionMap.get(new Pair<>(to, Integer.toString(be + (numberofNodes*2)))) == null) {
              MarkovChainInformation chain2 = new MarkovChainInformation(to, Integer.toString(be + (numberofNodes * 2)), "1.0", false, false, false);
              transitionMap.put(new Pair<>(to, Integer.toString(be + (numberofNodes * 2))), chain2);

              List<MarkovChainInformation> list2 = new ArrayList<>();
              if (transitionlistMap.get(to) != null) {
                list2 = transitionlistMap.get(to);
              }
              list2.add(chain2);
              transitionlistMap.put(to, list2);
            }
          }
        }

        dfsToAddUnrolledNodes(Integer.parseInt(c), visited, be_from, be, x, beList, beFromList,beTONode);
      //}
      }
    }
  }


  private List<String> translateToSMTLib(String ins_to_translate, Procedure proc) {
    System.out.println(MainFrame.selectedVariables);

    String[] consArr = ins_to_translate.split(" and ");

    String cons = "";
    String dom_cons = "";
    String consVar = "";

    Set<String> varSet = new HashSet<>();

    SymbolTable symTab = proc.getIR().getSymbolTable();

    for(String con: consArr) {
      String[] ins_comps = con.split(" ");

      String sign = "";

      List<String> vars = new ArrayList<>();
      for (int in = 0; in < ins_comps.length; in++) {
        String incomp = ins_comps[in].replace("(","").replace(")","");
        if (incomp.contains("v"))
          vars.add(incomp);
        else
          sign = incomp;
      }



      for(String var : vars) {
        //if (MainFrame.selectedVariables.contains(var)) {
        varSet.add(var);
        //}
      }


      String end = "";
      if(sign.equals("!=")) {
        if (!con.contains("not")) {
          cons += "(assert (not (= ";
          end = ")";
        }
        else
          cons += "(assert (= ";
      } else if(sign.equals("==")) {
        if (!con.contains("not"))
          cons += "(assert (= ";
        else {
          cons += "(assert (not (= ";
          end = ")";
        }
      } else {
        if (!con.contains("not"))
          cons += "(assert (" + sign + " ";
        else {
          cons += "(assert (not (" + sign + " ";
          end = ")";
        }
      }


      int var1 = Integer.parseInt(vars.get(0).substring(1));
      int var2 = Integer.parseInt(vars.get(1).substring(1));

      if (symTab.isNumberConstant(var1)) {
        int v1 = symTab.getIntValue(var1);
        if(v1 < 0)
          v1 = v1 * (-1);
//        if(v1 >= 10000)
//          cons += vars.get(0) + " ";
//        else
          cons += v1+" ";
      } else {
        cons += vars.get(0) + " ";
      }
      if (symTab.isNumberConstant(var2)) {
        int v2 = symTab.getIntValue(var2);
        if(v2 < 0)
          v2 = v2 * (-1);
//        if(v2 >= 10000)
//          cons += vars.get(1) + " ";
//        else
          cons += v2+" ";
      } else {
        cons += vars.get(1);
      }

      cons += end;
      cons += "))\n";
    }

    for(String var : varSet) {
      //if (MainFrame.selectedVariables.contains(var)) {
      int varInt = Integer.parseInt(var.substring(1));
      //String v = symTab.
      //String className = symTab.getValue(varInt).getClass().getName();
      consVar += "(declare-fun " + var + "() Int)\n";
      //}
    }
    dom_cons = consVar;

    cons = consVar + cons;
    cons += "(check-sat)";
    dom_cons += "(check-sat)";

    List<String> consList = new ArrayList<>();
    consList.add(dom_cons);
    consList.add(cons);

    return consList;
  }

  private void showJsonMenuitemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showJsonMenuitemActionPerformed
    if (this.currentCFG == null)
      return;

    //Choose a file to save the json
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Specify a file to save");

    int userSelection = fileChooser.showSaveDialog(this);
    if (userSelection == JFileChooser.APPROVE_OPTION) {
      fileToSave = fileChooser.getSelectedFile();
      System.out.println("Save as file: " + fileToSave.getAbsolutePath());
    }

    List<String> jsonItems = this.currentCFG.getJSON();
    List<String> invokedProcedures = new ArrayList<String>();
    invokedProcedures.add(this.currentCFG.getProcedure().getFullSignature());

    String completeJSON = "[ ";
    int i = 0;
    for (String jsonItem: jsonItems) {

      String jsonItemID = jsonItem.split(" ")[4];
      String jsonItemNodeNumber = jsonItemID.split("#")[1];

      Procedure cureProc = this.currentCFG.getProcedure();

      if (cureProc.dependentNodes.contains(jsonItemNodeNumber) && jsonItem.contains("\"secret_dependent_branch\" : \"branch\"")) {
        jsonItem = jsonItem.replace("\"secret_dependent_branch\" : \"branch\"", "\"secret_dependent_branch\" : \"true\"");
      }

      completeJSON += jsonItem;
      if (i < jsonItems.size() - 1)
        completeJSON += ",\n";
      i++;

      // Recursive inlining of function calls
      if (jsonItem.contains("Invoke") && !jsonItem.contains("<init>")) {
        completeJSON = recursiveInlining(invokedProcedures, jsonItems, i, jsonItemID, jsonItem, completeJSON, 0);
      }
    }
    completeJSON += " ]";

    System.out.println(completeJSON);

    BufferedWriter bw = null;
    FileWriter fw = null;

    try {

      //fw = new FileWriter("/home/baki/cfg_side_channel/cfg_analysis/input/themis/json/" + this.currentCFG.getProcedure().getProcedureName());
      fw = new FileWriter(fileToSave);
      bw = new BufferedWriter(fw);
      bw.write(completeJSON);

    } catch (IOException e) {

      e.printStackTrace();

    } finally {

      try {

        if (bw != null)
          bw.close();

        if (fw != null)
          fw.close();

      } catch (IOException ex) {

        ex.printStackTrace();

      }

    }

  }//GEN-LAST:event_showJsonMenuitemActionPerformed


  private void resetSlicingMenuitemActionPerformed(java.awt.event.ActionEvent evt) {
    dependencyAnalysisTime = 0;
    MainFrame.allSourceLines.clear();
    MainFrame.allStmtSet.clear();
    paintSlice(new HashSet<Statement>());
  }

  private void invokeCoCoChannelMenuitemActionPerformed(java.awt.event.ActionEvent evt) {
    String s = null;
    try {
      Process p = Runtime.getRuntime().exec("python src/coco-channel/main.py " + fileToSave);

      BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

      BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

      List<String> branchNodesForSideChannel = new ArrayList<String>();

      // read the output from the command
      while ((s = stdInput.readLine()) != null) {
        if(s.startsWith("side channel branch component id :")) {
          String node = s.split("side channel branch component id : ")[1];
          System.out.println(node);
          branchNodesForSideChannel.add(node);
        }
      }
      // read any errors from the attempted command
      while ((s = stdError.readLine()) != null) {
        System.out.println(s);
      }

      paintSideChannel(branchNodesForSideChannel);
    }
    catch (IOException e) {
      System.out.println("exception happened - here's what I know: ");
      e.printStackTrace();
    }
  }

  private void instrumentSecretDepbranchMenuitemActionPerformed(java.awt.event.ActionEvent evt) {
    System.out.println(MainFrame.allSourceLines);

    System.out.println(AnalyzerFrame.classPath);
    if (AnalyzerFrame.classPath.endsWith(".jar")) {
      String patharray[] = AnalyzerFrame.classPath.split("/");
      String path = "";
      for (int i=0; i < patharray.length-1; i++) {
        path = path + patharray[i] + "/";
      }
//      if(path.endsWith("/jar/")) {
//        path = path.split("/jar/")[0]+"/classes/";
//      }
      AnalyzerFrame.classPath = path + currentCFG.getProcedure().getClassName().substring(1)+".class";
      System.out.println(AnalyzerFrame.classPath);
    }

    //asm bytecode instrumentation
    try {
      //Set<Integer> allSrcLines = new HashSet<>();
      //allSrcLines.add(30); allSrcLines.add(35); allSrcLines.add(36);
      //InputStream in = new FileInputStream("src/input/Login.class");
      InputStream in = new FileInputStream(AnalyzerFrame.classPath);
      String className = this.currentCFG.getProcedure().getClassName().substring(1);
      Set<String> procNameSet = new HashSet<>();
      Set<Integer> lineVisited = new HashSet<>();
      for (Map.Entry<Integer, String> entry : allSourceLines.entrySet()) {
        procNameSet.add(entry.getValue());
      }
      try {
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(cr, Opcodes.ASM4);

        // add the static final fields
        for (int lineno : allSourceLines.keySet()) {
          cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "branchAt"+lineno+"all", "int", null, null).visitEnd();
          cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "branchAt"+lineno+"fallThrough", "int", null, null).visitEnd();
        }

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {

          class ourMethodVisitor extends MethodVisitor {
            int access;
            int line;
            boolean firstline = true;
            jdk.internal.org.objectweb.asm.Type[] params;

            ourMethodVisitor(MethodVisitor mv, int access, jdk.internal.org.objectweb.asm.Type[] param) {
              super(Opcodes.ASM4, mv);
              this.params = param;
              this.access = access;
            }

            //@Override
            //public void visitMethodc

            @Override
            public void visitInsn(int opcode) {
              //whenever we find a RETURN, we instert the code, here only crazy example code
              switch(opcode) {
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN:
                case Opcodes.LRETURN:
                case Opcodes.DRETURN:
                case Opcodes.RETURN:
                  int i = 0;
                  System.out.println(access);
                  if (access != 9 && access != 25) {
                    i = 1;
                  }
                  for (jdk.internal.org.objectweb.asm.Type tp : params) {
                    System.out.println(tp);
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                    if (tp.equals(jdk.internal.org.objectweb.asm.Type.BOOLEAN_TYPE)) {
                      mv.visitVarInsn(Opcodes.ILOAD, i);
                      mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Z)V");
                    } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.BYTE_TYPE)) {
                      mv.visitVarInsn(Opcodes.ALOAD, i);
                      mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(B)V");
                    } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.CHAR_TYPE)) {
                      mv.visitVarInsn(Opcodes.ILOAD, i);
                      mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(C)V");
                    } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.SHORT_TYPE)) {
                      mv.visitVarInsn(Opcodes.ILOAD, i);
                      mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(S)V");
                    } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.INT_TYPE)) {
                      mv.visitVarInsn(Opcodes.ILOAD, i);
                      mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(I)V");
                    } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.LONG_TYPE)) {
                      mv.visitVarInsn(Opcodes.LLOAD, i);
                      mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(J)V");
                    } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.FLOAT_TYPE)) {
                      mv.visitVarInsn(Opcodes.FLOAD, i);
                      mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(F)V");
                    } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.DOUBLE_TYPE)) {
                      mv.visitVarInsn(Opcodes.DLOAD, i);
                      mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(D)V");
                    } else {
                      if (tp.toString().equals("[B")) {
                        mv.visitVarInsn(Opcodes.ALOAD, i);
                        mv.visitFieldInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([B)Ljava/lang/String;");
                        //mv.visitTypeInsn(Opcodes.NEW, "java/lang/String");
                        //mv.visitInsn(Opcodes.DUP);
                        //mv.visitFieldInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([B)Ljava/lang/String;");
                        mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");
                      } else if (tp.toString().equals("[I")) {
                        mv.visitVarInsn(Opcodes.ALOAD, i);
                        mv.visitFieldInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([I)Ljava/lang/String;");
                        mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");
                      } else {
                        mv.visitVarInsn(Opcodes.ALOAD, i);
                        mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");
                      }
                    }

                    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                    mv.visitLdcInsn("\t");
                    mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");

                    i++;
                  }

                  for (int lineno : allSourceLines.keySet()) {
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                    mv.visitLdcInsn("branchAt"+lineno+"all = ");
                    mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");

                    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                    mv.visitFieldInsn(Opcodes.GETSTATIC, className, "branchAt"+lineno+"all", "I");
                    mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");

                    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                    mv.visitLdcInsn("\t");
                    mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");

                    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                    mv.visitLdcInsn("branchAt"+lineno+"fallThrough = ");
                    mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");

                    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                    mv.visitFieldInsn(Opcodes.GETSTATIC, className, "branchAt"+lineno+"fallThrough", "I");
                    mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");

                    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                    mv.visitLdcInsn("\t");
                    mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");
                  }
                  mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                  mv.visitLdcInsn("\n");
                  mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");
                  break;
                default: // do nothing
              }
              super.visitInsn(opcode);
            }

            @Override
            public void visitLineNumber(int line, Label l) {
              this.line = line;
              //if (!lineVisited.contains(line)) {
                int prevLinediff = 1;
                while(!this.firstline && !lineVisited.contains(line-prevLinediff)){
                  prevLinediff++;
                }
                System.err.println("line="+line);
                System.err.println("prevLinediff="+prevLinediff);
                if (allSourceLines.keySet().contains(line - prevLinediff)) {
                  System.err.println("fallThrough");
                  mv.visitVarInsn(Opcodes.ALOAD, 0);
                  mv.visitVarInsn(Opcodes.ALOAD, 0);
                  mv.visitFieldInsn(Opcodes.GETSTATIC, className, "branchAt" + (line - prevLinediff) + "fallThrough", "I");
                  mv.visitInsn(Opcodes.ICONST_1);
                  mv.visitInsn(Opcodes.IADD);
                  mv.visitFieldInsn(Opcodes.PUTSTATIC, className, "branchAt" + (line - prevLinediff) + "fallThrough", "I");
                }
                if (allSourceLines.keySet().contains(line)) {
                  System.err.println("branchAt");
                  mv.visitVarInsn(Opcodes.ALOAD, 0);
                  mv.visitVarInsn(Opcodes.ALOAD, 0);
                  mv.visitFieldInsn(Opcodes.GETSTATIC, className, "branchAt" + line + "all", "I");
                  mv.visitInsn(Opcodes.ICONST_1);
                  mv.visitInsn(Opcodes.IADD);
                  mv.visitFieldInsn(Opcodes.PUTSTATIC, className, "branchAt" + line + "all", "I");
                }
              //}
              this.firstline = false;
              lineVisited.add(line);
            }
          }

          public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (cv == null) {
              return null;
            }
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (procNameSet.contains(name)) {
              System.out.println("access = " + access);
              System.out.println("name = " + name);
              System.out.println("desc = " + desc);
              System.out.println("signature = " + signature);
              return new ourMethodVisitor(mv, access, jdk.internal.org.objectweb.asm.Type.getArgumentTypes(desc));
            }
            return mv;
          }
        };

        // feed the original class to the wrapped ClassVisitor
        cr.accept(cv, 0);

        // produce the modified class
        //Dump the class in a file
        File outDir=new File("src/output/");
        outDir.mkdirs();
//        if (className.contains("/")) {
//          String[] clsarr = className.split("/");
//          className = clsarr[clsarr.length - 1].split(".class")[0];
//        }
        DataOutputStream dout=new DataOutputStream(new FileOutputStream(new File(outDir,className+".class")));
        dout.write(cw.toByteArray());
        dout.flush();
        dout.close();
        JOptionPane.showMessageDialog(this, "Instrumented class saved to src/output directory");
        //System.out.println(getLineNumber("src/Login.class"));

      } catch(IOException ex) {
        System.err.println(ex.toString());
      }
    } catch(FileNotFoundException ex) {
      System.err.println(ex.toString());
    }
  }

  private String recursiveInlining(List<String> invokedProcedures, List<String> jsonItems, int i, String jsonItemID, String jsonItem, String completeJSON, Integer oldProcRecursiveBound) {
    //JOptionPane.showMessageDialog(MainFrame.this, "At Start: \n" + completeJSON);

    String[] jsonItemComponents = jsonItem.split("Invoke")[1].split(",");

    String inlineProcClass = jsonItemComponents[1].substring(0, jsonItemComponents[1].length() - 1);
    String inlineProcName = jsonItemComponents[2];
    String inlineProcArgs = jsonItemComponents[3].substring(0, jsonItemComponents[3].length() - 3);
    String inlineProcSignature = inlineProcClass + "." + inlineProcName + inlineProcArgs;
    Procedure inlineProc = Program.getProcedure(inlineProcSignature);

    System.out.println(inlineProcSignature);
    //System.out.println("jsonItemID : " + jsonItemID);

    if (jsonMap.get(inlineProcSignature) == null) {
      return completeJSON;
    }

    if (recursiveBoundMap.get(inlineProcSignature) == null) {
      return completeJSON;
    }

    //System.out.println("jsonItemID : " + jsonItemID);
    if (recursiveBoundMap.get(jsonItemID) == null) {
      recursiveBoundMap.put(jsonItemID, 1);
    }

    Integer oldRecursiveBound = recursiveBoundMap.get(inlineProcSignature);
    //System.out.println("oldRecursiveBound : " + oldProcRecursiveBound);
    Integer oldRecursiveBoundForItem = recursiveBoundMap.get(jsonItemID);
    //System.out.println("oldRecursiveBoundForItem : " + oldRecursiveBoundForItem);

    if (oldRecursiveBoundForItem > this.recursiveBound) {
      return completeJSON;
    }

    recursiveBoundMap.put(inlineProcSignature, oldRecursiveBound + 1);
    recursiveBoundMap.put(jsonItemID, oldRecursiveBoundForItem + 1);

    List<String> inlineProcJSON = jsonMap.get(inlineProcSignature);

    if (inlineProcJSON != null) {
      String entryOutgoingOld = jsonItem.split("\"outgoing\" : ")[1];
      String entryOutgoingNew = inlineProcJSON.get(0).split(" ")[4];
      //System.out.println("entryOutgoingOld: " + entryOutgoingOld);
      //System.out.println("entryOutgoingNew: " + entryOutgoingNew);
      String oldEntryOutgoingpart = entryOutgoingNew.split("#")[0] + "#";
      String newEntryOutgoingpart = oldEntryOutgoingpart + oldRecursiveBound + "#";
      entryOutgoingNew = entryOutgoingNew.replace(oldEntryOutgoingpart, newEntryOutgoingpart);
      completeJSON = completeJSON.replace(entryOutgoingOld, "{ \"" + entryOutgoingNew + "\" : \"Invoke\"} }");

      int k = 0;
      //JOptionPane.showMessageDialog(MainFrame.this, "Before loop: \n" + completeJSON);
      for(String item : inlineProcJSON) {

        String itemID = item.split(" ")[4];
        String itemNodeNumber = itemID.split("#")[1];

        if (inlineProc.dependentNodes.contains(itemNodeNumber) && item.contains("\"secret_dependent_branch\" : \"branch\"")) {
          item = item.replace("\"secret_dependent_branch\" : \"branch\"", "\"secret_dependent_branch\" : \"true\"");
        }

        //changing the id to set each id unique
        String oldID = itemID.split("#")[0] + "#";
        String newID = oldID + oldRecursiveBound + "#";
        //System.out.println("item = " + item + "    oldID = " + oldID + "    newID = " + newID);
        //System.out.println(item);
        item = item.replace(oldID, newID);
        //itemID = item.split(" ")[4];

        if (k == inlineProcJSON.size()-1) {

            String exitOutgoingNew = "";

            String checkString = jsonItems.get(i - 1).split("\"outgoing\" : \\{ ")[1].split(" }")[0];

            if (checkString.contains(",")) { //there are two outgoing special case for function calling and exception occurring later
                exitOutgoingNew = jsonItems.get(i - 1).split("\"outgoing\" : \\{ ")[1].split(" }")[0];
                if(item.contains("\"outgoing\" : { }"))
                    item = item.replace("\"outgoing\" : { }", "\"outgoing\" : { " + exitOutgoingNew + " }");
                else
                    item = item.replace("\"outgoing\" : {}", "\"outgoing\" : { " + exitOutgoingNew + " }");
            }

            else {

                exitOutgoingNew = jsonItems.get(i).split(" ")[4];

                //exitOutgoingNew = jsonItems.get(i).split("\"outgoing\" : \\{ \"")[1].split("\"")[0];
                //System.out.println("exitOutgoingNew: " + exitOutgoingNew);
                String oldExitOutgoingpart = exitOutgoingNew.split("#")[0] + "#";
                //System.out.println("oldExitOutgoingpart: " + oldExitOutgoingpart);
                if (oldProcRecursiveBound > 0) {
                    String newExitOutgoingpart = oldExitOutgoingpart + oldProcRecursiveBound + "#";
                    //System.out.println("newExitOutgoingpart: " + newExitOutgoingpart);
                    exitOutgoingNew = exitOutgoingNew.replace(oldExitOutgoingpart, newExitOutgoingpart);
                    //System.out.println("exitOutgoingNew: " + exitOutgoingNew);
                }
                if(item.contains("\"outgoing\" : { }"))
                    item = item.replace("\"outgoing\" : { }", "\"outgoing\" : { \"" + exitOutgoingNew + "\" : \"Implicit\" }");
                else
                    item = item.replace("\"outgoing\" : {}", "\"outgoing\" : { \"" + exitOutgoingNew + "\" : \"Implicit\" }");
            }
        }

        completeJSON += item + ",\n";
        //JOptionPane.showMessageDialog(MainFrame.this, "Adding item: \n" + completeJSON);
        k++;

        //System.out.println(item);

        if (item.contains("Invoke") && !item.contains("<init>") && !item.split("\"outgoing\" : \\{ ")[1].split(" }")[0].contains(",")) {
          //System.out.println(item);
          //JOptionPane.showMessageDialog(MainFrame.this, "At condition: \n" + completeJSON);
                /*String itemIDToPass = "";
                if (jsonItemID.contains(" "))
                    itemIDToPass = jsonItemID.split(" ")[1] + " " + itemID;
                else
                    itemIDToPass = jsonItemID + " " + itemID;*/

          //System.out.println(itemIDToPass);
          completeJSON = recursiveInlining(invokedProcedures, inlineProcJSON, k, itemID, item, completeJSON, oldRecursiveBound);
        }
      }
    }
    return completeJSON;
  }
  /*
  added by Madeline Sgro 07/13/2017
  shows the occurances of the modulo function on the call graph
  */
  private void showModulo(){
    Set<Procedure> moduloBody = getModuloBody();
    if (moduloBody.isEmpty()){
      return;
    }
    Set<Procedure> moduloAncestors = getModuloAncestors(moduloBody);
    Set<Procedure> moduloSetToShow = new HashSet<>();
    moduloSetToShow.addAll(moduloAncestors);
    moduloSetToShow.addAll(moduloBody);

    //taking care of cgToggleButton
    if(this.cgSwitchToggleButton.isSelected()){
      CGPartial cgPartial = new CGPartial(moduloSetToShow);
      cgPartial.paintProcedureSet(moduloAncestors, "FFC0CB"); //pink
      cgPartial.paintProcedureSet(getModuloBody(), "FF69B4"); //hot pink
      this.currentCG = cgPartial;
    } else {
      this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
      this.callGraph.paintProcedureSet(moduloAncestors, "FFC0CB"); //pink
      this.callGraph.paintProcedureSet(getModuloBody(), "FF69B4"); //hotpink
      this.currentCG = this.callGraph;
    }
    drawCallGraph();

    DefaultListModel listModel = new DefaultListModel();
    this.infoList.setModel(listModel);
    fillOtherInfo(getModuloBody());
    this.graphPanel.add(this.infoScrollPane, BorderLayout.EAST);

    Procedure oneEntry = moduloBody.iterator().next();
    scrollToProcedure(oneEntry);
  }

  /*
  added by Madeline Sgro 07/13/2017
  returns set of procedures that contain a modulo function
  */
  private Set<Procedure> getModuloBody(){
    return OtherAnalysis.getModuloSet();
  }

  /*
  added by Madeline Sgro 07/13/2013
  returns the set of procedures that are ancestors of methods containing the
  modulo function
  */
  private Set<Procedure> getModuloAncestors(Set<Procedure> moduloBody) {
    Set<Procedure> moduloAncestors = new HashSet<>();
    for (Procedure proc : moduloBody) {
      moduloAncestors.addAll(Program.getAncestorProcedureSet(proc));
    }
    return moduloAncestors;
  }

  /*
   Added by Thomas Marosz 07/14/17; modeled after M. Sgro's implementation of
   showModulo() and its related methods.
  */
  private void showStringComparisons() {
    Set<Procedure> stringCompBody = getStringCompBody();
    if (stringCompBody.isEmpty()){
      return;
    }
    Set<Procedure> stringCompAncestors = getStringCompAncestors(stringCompBody);
    Set<Procedure> stringCompSetToShow = new HashSet<>();
    stringCompSetToShow.addAll(stringCompAncestors);
    stringCompSetToShow.addAll(stringCompBody);

    //taking care of cgToggleButton
    if(this.cgSwitchToggleButton.isSelected()){
      CGPartial cgPartial = new CGPartial(stringCompSetToShow);
      cgPartial.paintProcedureSet(stringCompAncestors, "FFC0CB"); //pink
      cgPartial.paintProcedureSet(getStringCompBody(), "FF69B4"); //hot pink
      this.currentCG = cgPartial;
    } else {
      this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
      this.callGraph.paintProcedureSet(stringCompAncestors, "FFC0CB"); //pink
      this.callGraph.paintProcedureSet(getStringCompBody(), "FF69B4"); //hotpink
      this.currentCG = this.callGraph;
    }

    drawCallGraph();
    Procedure oneEntry = stringCompBody.iterator().next();
    scrollToProcedure(oneEntry);
  }

  private Set<Procedure> getStringCompBody() {
    return OtherAnalysis.getStringCompSet();
  }

  private Set<Procedure> getStringCompAncestors(Set<Procedure> stringCompBody) {
    Set<Procedure> stringCompAncestors = new HashSet<>();
    for ( Procedure proc : stringCompBody) {
      stringCompAncestors.addAll(Program.getAncestorProcedureSet(proc));
    }
    return stringCompAncestors;
  }

  enum CGFocus {
    Null,
    Loop,
    New,
    Recursion,
    Slice,
    Zoom
  }

  private ConfigInfo        configInfo = null;

  private CG                                        callGraph = null;
  private Map<Procedure, CFG>                       controlFlowGraphMap = new HashMap<>();
  private Map<Procedure, PDG>                       procedureDependenceGraphMap = new HashMap<>();
  private Map<Integer, List<NestedLoop>>            nestedLoopListMap = new TreeMap<>(Collections.reverseOrder());
  private Map<Integer, List<NewObject>>             newObjectListMap = new TreeMap<>(Collections.reverseOrder());
  private Map<Integer, Recursion>                   recursionMap = new TreeMap<>();
  private Map<String, List<String>>                 jsonMap = new HashMap<>();
  private Map<String, Integer>                      recursiveBoundMap = new HashMap<>();
  private List<Procedure>                           otherList = null; //Added by Madeline Sgro 07/14/2017

  private CGFocus                                   focus = CGFocus.Null;
  private BaseGraph<Procedure>                      currentCG = null;
  private CFG                                       currentCFG = null;
  private PDG                                       currentPDG = null;
  private ISSABasicBlock                            currentNode = null;
  private NestedLoop                                currentNestedLoop = null;

  private LocationFrame                             locationFrame;
  private LoopFilterFrame                           loopFilterFrame;
  private LoopFilterFrame                           newFilterFrame;

  private int                                       recursiveBound;
  private int                                       numberofNodes;
  private int                                       numberofNodesMerged;
  private int                                       numberofNodesReduced;
  private boolean                                   loopUnrolling = false;
  private int                                       loopbound = 4;
  public static long                                dependencyAnalysisTime = 0;
  private static long                               extraModelCountingTime = 0;
  private boolean backEdgeExists = false;
  public static String cfgConsTime = "";

  private static int counter = 0;
  private static Map<String, Integer> nodeMap = new HashMap<>();
  private static Map<Integer, String> idMap = new HashMap<>();
  private static Map<String, List<String>> edgeMap =  new HashMap<>();
  private static String endNode = "";
  private static String assertionNode = "";
  private static Map<Pair<String, String>, MarkovChainInformation> transitionMap = new HashMap<>();
  private static Map<String, List<MarkovChainInformation>> transitionlistMap = new HashMap<>();
  private static Map<ISSABasicBlock, Integer> nodeLineMap = new HashMap<>();
  private static Map<String, String> interProcDomMap = new HashMap<>();
    private static Map<String, String> interProcPostDomMap = new HashMap<>();
    private static Map<String, String> procCallMap = new HashMap<>();
  private static Map<String, String> procCallReverseMap = new HashMap<>();

  private Map<String, Map<Double, Set<Procedure>>>  jBondMap = new TreeMap<>();
  static public Set<Statement> allStmtSet = new HashSet<>();
  static public Set<String> selectedVariables = new HashSet<>();
  static public Map<Integer, String> allSourceLines = new HashMap<>();
  static public Map<String, Procedure> itemProcMap = new HashMap<>();
  static public Map<String, ISSABasicBlock> itemNodeMap = new HashMap<>();
  static public Map<ISSABasicBlock, String> nodeItemMap = new HashMap<>();
  static private Map<String,Long> modelCountingTimeMap = new HashMap<>();
  static private Map<String,List<String>> lineItemsMap = new HashMap<>();
  static private boolean[] visitedToCheck;

  static public ModelCounter modelCounter = new ModelCounter(4, "abc.string");

  final public void refreshDrawing() {
    if (this.currentPDG != null)showProcedureDependecenGraph(this.currentPDG.getProcedure());

    else if (this.currentCFG != null)
      showControlFlowGraph(this.currentCFG.getProcedure());
    else
      drawCallGraph();
  }

  final public CG getCG() {
    return this.callGraph;
  }

  final public CFG getCFG(Procedure proc) {
    return this.controlFlowGraphMap.get(proc);
  }

  final public void locateProcedure(Procedure proc) {
    if (this.currentCG != this.callGraph)
      return;
    this.callGraph.paintProcedureSet(Program.getProcedureSet(), "white");
    this.callGraph.paintProcedure(proc, "yellow");
    drawCallGraph();
    scrollToProcedure(proc);
  }

  final public void trimCG(Procedure srcProc, Procedure dstProc) {
    Set<Procedure> procSet = Program.getDescendantProcedureSet(srcProc);
    procSet.retainAll(Program.getAncestorProcedureSet(dstProc));
    procSet.add(srcProc);
    procSet.add(dstProc);
    CGPartial cgPartial = new CGPartial(procSet);
    this.currentCG = cgPartial;
    drawCallGraph();
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JMenuItem backMenuItem;
  private javax.swing.JButton branchButton;
  private javax.swing.JPopupMenu cfgOptPopupMenu;
  private javax.swing.JToggleButton cgSwitchToggleButton;
  private javax.swing.JFileChooser fileChooser;
  private javax.swing.JButton filterButton;
  private javax.swing.JPanel graphPanel;
  private javax.swing.JList infoList;
  private javax.swing.JScrollPane infoScrollPane;
  private javax.swing.JButton jBondButton;
  private javax.swing.JComboBox jBondFeatureComboBox;
  private javax.swing.JLabel jBondFeatureLabel;
  private javax.swing.JCheckBox jBondShowAllCheckBox;
  private javax.swing.JButton loadspfButton;
  private javax.swing.JButton locateButton;
  private javax.swing.JComboBox loopComboBox;
  private javax.swing.JLabel loopLabel;
  private javax.swing.JPanel mainPanel;
  private javax.swing.JComboBox newComboBox;
  private javax.swing.JLabel newLabel;
  private javax.swing.JPopupMenu.Separator optSeparator1;
  private javax.swing.JComboBox<String> otherComboBox;
  private javax.swing.JLabel otherLabel;
  private javax.swing.JPopupMenu pdgOptPopupMenu;
  private javax.swing.JComboBox recursionComboBox;
  private javax.swing.JLabel recursionLabel;
  private javax.swing.JMenuItem retMenuItem;
  private javax.swing.JButton runspfButton;
  private javax.swing.JMenuItem showJsonMenuitem;
  private javax.swing.JMenuItem branchModelCountMenuitem;
  private javax.swing.JMenuItem resetSlicing;
  private javax.swing.JMenuItem invokeCoCoChannel;
  private javax.swing.JMenuItem instrumentSecretDepBranch;
  private javax.swing.JMenuItem showPDGMenuItem;
  private javax.swing.JMenuItem silceMenuItem;
  private javax.swing.JComboBox<String> zoomComboBox;
  private javax.swing.JLabel zoomLabel;
  // End of variables declaration//GEN-END:variables
  public File fileToSave = null;

  public class CustomClassWriter {

    ClassReader reader;
    ClassWriter writer;

    public CustomClassWriter(String className) throws IOException {
      reader = new ClassReader(className);
      writer = new ClassWriter(reader, 0);
    }
  }

  private class InsertInitCodeBeforeReturnMethodVisitor extends MethodVisitor{

    public InsertInitCodeBeforeReturnMethodVisitor(MethodVisitor mv) {
      super(Opcodes.ASM4, mv);
    }


    @Override
    public void visitInsn(int opcode) {
      //whenever we find a RETURN, we instert the code, here only crazy example code
      switch(opcode) {
        case Opcodes.IRETURN:
        case Opcodes.FRETURN:
        case Opcodes.ARETURN:
        case Opcodes.LRETURN:
        case Opcodes.DRETURN:
        case Opcodes.RETURN:
          mv.visitVarInsn(Opcodes.ALOAD, 42);
          break;
        default: // do nothing
      }
      super.visitInsn(opcode);
    }
  }

  private class MarkovChainInformation {

    String fromNode, toNode, prob;
    boolean depBranchNode, assertNode, exceptionNode;

    public MarkovChainInformation(String fromNode, String toNode, String prob, boolean depBranchNode, boolean assertNode, boolean exceptionNode) {
      this.fromNode = fromNode;
      this.toNode = toNode;
      this.prob = prob;
      this.depBranchNode = depBranchNode;
      this.assertNode = assertNode;
      this.exceptionNode = exceptionNode;
    }

    public String getFromNode() {
      return fromNode;
    }

    public String getToNode() {
      return toNode;
    }

    public String getProb() {
      return prob;
    }

    public boolean isDepBranchNode() {
      return depBranchNode;
    }

    public boolean isAssertNode() {
      return assertNode;
    }

    public boolean isExceptionNode() { return exceptionNode; }

    public MarkovChainInformation updateToNode(String toNode) {
      this.toNode = toNode;
      return this;
    }

    public MarkovChainInformation updateProb(String prob) {
      this.prob = prob;
      return this;
    }
  }
}
