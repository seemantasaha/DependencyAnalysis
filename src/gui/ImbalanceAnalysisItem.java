/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import java.util.Iterator;

/**
 *
 * @author seemanta
 */
public class ImbalanceAnalysisItem {
    private String itemID;
    private int numberOfInstructions;
    private String itemInstructions;
    private Integer nodeCost;
    private String[] incomingItems;
    private String[] outgoingItems;
    
    public ImbalanceAnalysisItem(String id, int numberOfInstructions, String instructions, Integer nodeCost, String[] incomingItems, String[] outgoingItems) {
        this.itemID = id;
        this.numberOfInstructions = numberOfInstructions;
        this.itemInstructions = instructions;
        this.nodeCost = nodeCost;
        this.incomingItems = incomingItems;
        this.outgoingItems = outgoingItems;
    }
    
    public String getID() {
        return this.itemID;
    }
    
    public int getNumberOfInstructions() {
        return this.numberOfInstructions;
    }
    
    public String getInstruction() {
        return this.itemInstructions;
    }
    
    public Integer getNodeCost() {
        return this.nodeCost;
    }
    
    public String[] getIncomingItems() {
        return this.incomingItems;
    }
    
    public String[] getOutgoingItems() {
        return this.outgoingItems;
    }
}
