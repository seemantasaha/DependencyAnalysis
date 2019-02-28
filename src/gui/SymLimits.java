/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

/**
 *
 * @author dan
 */
 public class SymLimits {
    public String  min;
    public String  max;

    public SymLimits() {
    }

    public SymLimits(String initmin, String initmax) {
        min = initmin;
        max = initmax;
    }
    
    public void setLimits (String newmin, String newmax) {
        min = newmin;
        max = newmax;
    }
    
    public void setLimits (SymLimits copy) {
        min = copy.min;
        max = copy.max;
    }
}
