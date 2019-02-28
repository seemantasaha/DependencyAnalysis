/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import com.ibm.wala.types.TypeReference;
import java.util.ArrayList;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

/**
 *
 * @author dan
 */
public class ArgSelect {
    // parameter mode selections
    // (Note: None is used if there are no parameters)
    public enum ParamMode {
        None, DataStruct, Simpleton, Primitive, String, Array;
    }
	
    // GUI control info
    public ParamMode mode;          // radiobutton selection
    public String    cls;           // class1ComboBox selection
    public String    method;        // method1ComboBox selection
    public String    size;          // sizeSelectComboBox selection
    public String    value;         // valueSelectComboBox selection
    public String    element;       // elementTypeComboBox selection
    public String    primitive;     // primitiveTypeComboBox selection
    public String    arraySize;     // arraySizeSpinner selection
    public boolean   isStatic;      // true if method is static

    // the argument entry from the method argument list
    public TypeReference argument;
        
    // the array of arguments for the iterative method (empty if none)
    public ArrayList<TypeReference> iterList;
        
    public ArgSelect(TypeReference newArg, ParamMode newMode, boolean bIsStatic) {
        mode      = newMode;
        cls       = "";
        method    = "";
        size      = (newMode == ParamMode.String) ? "Concrete" : "N";
        value     = "Symbolic";
        element   = "String";
        primitive = "int";
        arraySize = "Concrete";
            
        isStatic  = bIsStatic;
        argument  = newArg;
        iterList  = null;
    }
        
    public ArgSelect() {
        mode      = ParamMode.String;
        cls       = "";
        method    = "";
        size      = "N";
        value     = "Symbolic";
        element   = "String";
        primitive = "int";
        arraySize = "Concrete";
            
        isStatic  = false;
        argument  = null;
        iterList  = null;
    }
}

