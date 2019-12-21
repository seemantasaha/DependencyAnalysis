package gui;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.ssa.SSAInstruction;
import core.Procedure;
import core.Program;
import core.ProgramDependenceGraph;
import core.Reporter;
import core.Statement;
import core.StatementType;

import java.util.*;

import com.ibm.wala.shrikeBT.*;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

/**
 *
 * @author zzk
 */
public class SliceFrame extends javax.swing.JFrame {

  /**
   * Creates new form sliceFrame
   */
  public SliceFrame(MainFrame mainFrame, Procedure proc, ISSABasicBlock node) {
    initComponents();
    
    this.mainFrame = mainFrame;
    this.procedure = proc;
    this.node = node;
    
    loadInstructions();
  }
  
  private void loadInstructions() {
    int index = 0;
    Iterator<SSAInstruction> instIter = this.node.iterator();
    while (instIter.hasNext()) {
      SSAInstruction inst = instIter.next();
      String instStr = Reporter.getSSAInstructionString(inst);
      this.stmtComboBox.addItem(instStr);
      this.instructionMap.put(index, inst);
      ++index;
    }
  }
  
  private void paintSlice(Set<Statement> stmtSet) {

    MainFrame.allStmtSet.addAll(stmtSet);

    CG cg = this.mainFrame.getCG();
    cg.paintProcedureSet(Program.getProcedureSet(), "white");
    for (Procedure proc : Program.getProcedureSet()) {
      CFG cfg = this.mainFrame.getCFG(proc);
      cfg.getProcedure().dependentNodes.clear();
      cfg.paintNodeSet(proc.getNodeSet(), "aquamarine");
    }
    
    for (Statement stmt : MainFrame.allStmtSet) {
      Procedure proc = stmt.getOwner().getProcedure();
      CFG cfg = this.mainFrame.getCFG(proc);
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
            System.err.println ( "Bytecode : " + inst.toString() );
            bcIndex = ((IBytecodeMethod) target.getMethod()).getBytecodeIndex(inst.iindex);
            try {
              int src_line_number = target.getMethod().getLineNumber(bcIndex);
              System.err.println("Source line number = " + src_line_number);
              MainFrame.allSourceLines.put(src_line_number, proc.getProcedureName());
            } catch (Exception e) {
              System.err.println("Bytecode index no good");
              System.err.println(e.getMessage());
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
                    System.err.println ( "Bytecode : " + inst.toString() );
                    bcIndex = ((IBytecodeMethod) target.getMethod()).getBytecodeIndex(inst.iindex);
                    try {
                        int src_line_number = target.getMethod().getLineNumber(bcIndex);
                        System.err.println("Source line number = " + src_line_number);
                      MainFrame.allSourceLines.put(src_line_number, proc.getProcedureName());
                    } catch (Exception e) {
                        System.err.println("Bytecode index no good");
                        System.err.println(e.getMessage());
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
        }
      }
    }
    this.mainFrame.refreshDrawing();
  }
  
  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    dirComboBox = new javax.swing.JComboBox();
    dirLabel = new javax.swing.JLabel();
    stmtLabel = new javax.swing.JLabel();
    stmtComboBox = new javax.swing.JComboBox();
    sliceButton = new javax.swing.JButton();
    dataDepComboBox = new javax.swing.JComboBox();
    dataDepLabel = new javax.swing.JLabel();
    ctrlDepLabel = new javax.swing.JLabel();
    ctrlDepComboBox = new javax.swing.JComboBox();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

    dirComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Backward", "Forward" }));

    dirLabel.setText("Direction");

    stmtLabel.setText("Statement");

    sliceButton.setText("Slice");
    sliceButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        sliceButtonActionPerformed(evt);
      }
    });

    dataDepComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FULL" }));

    dataDepLabel.setText("Data");

    ctrlDepLabel.setText("Control");

    ctrlDepComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FULL" }));

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGap(0, 0, Short.MAX_VALUE)
            .addComponent(sliceButton))
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(dirLabel)
              .addComponent(dataDepLabel)
              .addComponent(ctrlDepLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
              .addComponent(ctrlDepComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addGroup(layout.createSequentialGroup()
                .addComponent(dirComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stmtLabel))
              .addComponent(dataDepComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(stmtComboBox, 0, 358, Short.MAX_VALUE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(dirComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(dirLabel)
          .addComponent(stmtLabel)
          .addComponent(stmtComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(dataDepComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(dataDepLabel))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(ctrlDepLabel)
          .addComponent(ctrlDepComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 214, Short.MAX_VALUE)
        .addComponent(sliceButton)
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void sliceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sliceButtonActionPerformed
    String dir = (String)this.dirComboBox.getSelectedItem();
    boolean forward = dir.equals("Forward");
    String dataDep = (String)this.dataDepComboBox.getSelectedItem();
    String ctrlDep = (String)this.ctrlDepComboBox.getSelectedItem();
    
    int index = this.stmtComboBox.getSelectedIndex();
    SSAInstruction inst = this.instructionMap.get(index);    
    if (forward) {
      long start = System.currentTimeMillis();
      Set<Statement> stmtSet = ProgramDependenceGraph.sliceProgramForward(procedure, inst);
      paintSlice(stmtSet);
      long finish = System.currentTimeMillis();
      long timeElapsed = finish - start;
      MainFrame.dependencyAnalysisTime += timeElapsed;
    } else {
      Set<Statement> stmtSet = ProgramDependenceGraph.sliceProgramBackward(procedure, inst);
      paintSlice(stmtSet);
    }
    
    this.dispose();
  }//GEN-LAST:event_sliceButtonActionPerformed
  
  private MainFrame                     mainFrame;
  private Procedure                     procedure;
  private ISSABasicBlock                node;
  private Map<Integer, SSAInstruction>  instructionMap = new HashMap<>();
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JComboBox ctrlDepComboBox;
  private javax.swing.JLabel ctrlDepLabel;
  private javax.swing.JComboBox dataDepComboBox;
  private javax.swing.JLabel dataDepLabel;
  private javax.swing.JComboBox dirComboBox;
  private javax.swing.JLabel dirLabel;
  private javax.swing.JButton sliceButton;
  private javax.swing.JComboBox stmtComboBox;
  private javax.swing.JLabel stmtLabel;
  // End of variables declaration//GEN-END:variables
}
