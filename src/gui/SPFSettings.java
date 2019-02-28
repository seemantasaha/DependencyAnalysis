/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

/**
 *
 * @author dan
 * 
 * This captures all of the settings of Janalyzer necessary for performing
 * an SPF build. It is intended for reading a settings file that was saved
 * and extracting the values into a structure that can then be loaded into
 * the GUI. By loading it into a structure we can first verify that the data
 * in the file is valid. Extraneous settings will be ignored, but all vital
 * information must be found or an exception will be thrown.
 */
public class SPFSettings {
    
    // Misc Parameters
    public String prjName;
    public String spfMode;
    public String useCloud;
        
    // Cloud Setup Parameters
    public String cloudJobName;
    public String cloudExecutors;
    public String cloudInitDepth;
            
    // Test Setup Parameters
    public String class2;
    public String method2;
    public String paramCount;
    public ArrayList<ArgSelect>  argListTest;
        
    // Configuration Setup - WCA
    public String wcaSolver;
    public String wcaBvlen;
    public String wcaInputMax;
    public String wcaPolicy;
    public String wcaPolicyRange;
    public String wcaPolicyEnd;
    public String wcaHistory;
    public String wcaHistoryRange;
    public String wcaHistoryEnd;
    public String wcaCostModel;
    public String wcaHeuristic;
    public String wcaDebug;
    public SymTypes wcaSymLimits;

    // Configuration Setup - Side Channel
    public String scSolver;
    public String scBvlen;
    public String scType;
    public String scDebug;
    public SymTypes scSymLimits;
    
    public SPFSettings (String content) throws SettingsException {
        // extract content into structure
        String min, max;
        wcaSymLimits = new SymTypes();
        scSymLimits = new SymTypes();
        
        // Miscellaneous settings
        prjName  = findTagField (content, true, "prjName");
        spfMode  = findTagField (content, true, "spfMode");
        useCloud = findTagField (content, true, "useCloud");
        
        // Cloud Setup (note that these may be empty fields)
        cloudJobName = "";
        cloudExecutors = "";
        cloudInitDepth = "";
        cloudJobName   = findTagField (content, false, "cloudJobName");
        cloudExecutors = findTagField (content, false, "cloudExecutors");
        cloudInitDepth = findTagField (content, false, "cloudInitDepth");
        
        // Test Setup
        class2  = findTagField (content, true, "class2");
        method2 = findTagField (content, true, "method2");
        
        // Test Parameters (note that the only fields that are required to
        // be non-empty are the argIsStatic and argMode entries)
        paramCount = findTagField (content, true, "paramCount");
        argListTest = new ArrayList<>();
        for (int ix = 0; ix < Integer.parseInt(paramCount); ix++) {
            ArgSelect param = new ArgSelect();
            param.cls       = findTagField(content, false, ix, "argClass");
            param.method    = findTagField(content, false, ix, "argMethod");
            param.value     = findTagField(content, false, ix, "argValue");
            param.size      = findTagField(content, false, ix, "argSize");
            param.element   = findTagField(content, false, ix, "argElement");
            param.primitive = findTagField(content, false, ix, "argPrimitive");
            param.arraySize = findTagField(content, false, ix, "argArray");
            param.isStatic  = findTagField(content, true,  ix, "argIsStatic").equals("yes");
            // ArgSelect.ParameterMode = None, DataStruct, Simpleton, Primitive, String, Array;
            switch (findTagField(content, true,  ix, "argMode")) {
                default: // fall through...
                case "None":       param.mode = ArgSelect.ParamMode.None;       break;
                case "DataStruct": param.mode = ArgSelect.ParamMode.DataStruct; break;
                case "Simpleton":  param.mode = ArgSelect.ParamMode.Simpleton;  break;
                case "Primitive":  param.mode = ArgSelect.ParamMode.Primitive;  break;
                case "String":     param.mode = ArgSelect.ParamMode.String;     break;
                case "Array":      param.mode = ArgSelect.ParamMode.Array;      break;
            }
            argListTest.add(param);
        }

        // Configuration Setup - WCA
        wcaSolver       = findTagField (content, true, "wcaSolver");
        wcaBvlen        = findTagField (content, true, "wcaBvlen");
        wcaInputMax     = findTagField (content, true, "wcaInputMax");
        wcaPolicy       = findTagField (content, true, "wcaPolicy");
        wcaPolicyRange  = findTagField (content, true, "wcaPolicyRange");
        wcaPolicyEnd    = findTagField (content, true, "wcaPolicyEnd");
        wcaHistory      = findTagField (content, true, "wcaHistory");
        wcaHistoryRange = findTagField (content, true, "wcaHistoryRange");
        wcaHistoryEnd   = findTagField (content, true, "wcaHistoryEnd");
        wcaCostModel    = findTagField (content, true, "wcaCostModel");
        wcaHeuristic    = findTagField (content, true, "wcaHeuristic");
        wcaDebug        = findTagField (content, true, "wcaDebug");
            
        min = findTagField (content, true, "wcaCharMin");
        max = findTagField (content, true, "wcaCharMax");
        wcaSymLimits.setLimits("char", min, max);
        min = findTagField (content, true, "wcaByteMin");
        max = findTagField (content, true, "wcaByteMax");
        wcaSymLimits.setLimits("byte", min, max);
        min = findTagField (content, true, "wcaShortMin");
        max = findTagField (content, true, "wcaShortMax");
        wcaSymLimits.setLimits("short", min, max);
        min = findTagField (content, true, "wcaIntMin");
        max = findTagField (content, true, "wcaIntMax");
        wcaSymLimits.setLimits("int", min, max);
        min = findTagField (content, true, "wcaLongMin");
        max = findTagField (content, true, "wcaLongMax");
        wcaSymLimits.setLimits("long", min, max);
        min = findTagField (content, true, "wcaDoubleMin");
        max = findTagField (content, true, "wcaDoubleMax");
        wcaSymLimits.setLimits("double", min, max);
            
        // Configuration Setup - Side Channel
        scSolver = findTagField (content, true, "scSolver");
        scBvlen  = findTagField (content, true, "scBvlen");
        scType   = findTagField (content, true, "scType");
        scDebug  = findTagField (content, true, "scDebug");
            
        min = findTagField (content, true, "scCharMin");
        max = findTagField (content, true, "scCharMax");
        scSymLimits.setLimits("char", min, max);
        min = findTagField (content, true, "scByteMin");
        max = findTagField (content, true, "scByteMax");
        scSymLimits.setLimits("byte", min, max);
        min = findTagField (content, true, "scShortMin");
        max = findTagField (content, true, "scShortMax");
        scSymLimits.setLimits("short", min, max);
        min = findTagField (content, true, "scIntMin");
        max = findTagField (content, true, "scIntMax");
        scSymLimits.setLimits("int", min, max);
        min = findTagField (content, true, "scLongMin");
        max = findTagField (content, true, "scLongMax");
        scSymLimits.setLimits("long", min, max);
        min = findTagField (content, true, "scDoubleMin");
        max = findTagField (content, true, "scDoubleMax");
        scSymLimits.setLimits("double", min, max);
    }

    /**
     * this searches the 'content' field for the specified 'tag' and returns the
     * corresponding value associated with it.
     * 
     * The line format is assumed to be each line begins with the tag name enclosed
     * enclosed in <> brackets followed by whitespace (spaces or tabs) followed by
     * the corresponding value.
     * 
     * @param content   - the content string to search
     * @param bRequired - true if the parameter cannot be empty string
     * @param tag       - the field to search for
     * @return The corresponding value associated with the field (null if not found)
     */
    private String findTagField (String content, boolean bRequired, String tag) throws SettingsException {
        // we're looking for the tag enclosed in <> at the begining of the line
        // followed by whitespace and the field value terminating on newline.
        String search = "(^<" + tag + ">)[ \t]*(.*)"; //(\\S+)";
        Pattern pattern = Pattern.compile(search, Pattern.MULTILINE);
        Matcher match = pattern.matcher(content);
        if (match.find()) {
            String entry = match.group(2);
            if (bRequired && entry.isEmpty()) {
                throw new SettingsException("Settings tag empty: " + tag);
            }
            return entry;
        }

        throw new SettingsException("Settings tag not found: " + tag);
    }

    /**
     * this searches the 'content' field for the specified indexed parameter 'tag'
     * and returns the corresponding value associated with it.
     * 
     * The line format is assumed to be each line begins with a "P" followed by the
     * index of the parameter and whitespace, followed by the tag name enclosed
     * enclosed in <> brackets, followed by whitespace (spaces or tabs), followed by
     * the corresponding value.
     * 
     * @param content   - the content string to search
     * @param bRequired - true if the parameter cannot be empty string
     * @param index     - the parameter index value (starting at 0)
     * @param tag       - the field to search for
     * @return The corresponding value associated with the field (null if not found)
     */
    private String findTagField (String content, boolean bRequired, Integer index, String tag) throws SettingsException {
        String search = "^[ \t]*P" + index.toString() + "[ \t]*(<" + tag + ">)[ \t]+(.*)";
        Pattern pattern = Pattern.compile(search, Pattern.MULTILINE);
        Matcher match = pattern.matcher(content);
        if (match.find()) {
            String entry = match.group(2);
            if (bRequired && entry.isEmpty()) {
                throw new SettingsException("Settings tag empty: " + tag);
            }
            return entry;
        }

        throw new SettingsException("Settings tag not found: " + tag);
    }

    public class SettingsException extends Exception {
        public SettingsException (String msg) {
            super(msg);
        }
    }

}
