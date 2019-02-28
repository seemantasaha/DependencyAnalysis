/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author dmcd2356
 */
public class PropertiesFile {
    // the location of the properties file for this application
    final static private String PROPERTIES_PATH = System.getProperty("user.home") + "/.janalyzer/";
    final static private String PROPERTIES_FILE = "site.properties";

    // the types of properties that are valid to store
    public enum Type {
        LastConfigFile, ProjectPath, JrePath;
    }

    private Properties   props;
    private DebugMessage debug;

    PropertiesFile (DebugMessage debugout) {
        debug = debugout;
        setDebugColorScheme(debug);
        
        props = new Properties();
        File propfile = new File(PROPERTIES_PATH + PROPERTIES_FILE);
            if (propfile.exists()) {
            try {
                // property file exists, read it in
                FileInputStream in = new FileInputStream(PROPERTIES_PATH + PROPERTIES_FILE);
                props.load(in);
                in.close();
            } catch (FileNotFoundException ex) {
                dprintError(ex + " <" + PROPERTIES_PATH + PROPERTIES_FILE + ">");
                props = null;
            } catch (IOException ex) {
                dprintError(ex + " <" + PROPERTIES_PATH + PROPERTIES_FILE + ">");
                props = null;
            }
        }
        else {
            // property file does not exist - create a default one
            dprintMessage("Creating initial properties file");
            props = createInitialPropertiesFile();
        }
    }
    
    /**
     * sets up the DebugMessage instance with the color selections to use.
     * 
     * @param handler - the DebugMessage instance to apply it to
     */
    private void setDebugColorScheme (DebugMessage handler) {
        if (debug != null) {
            handler.setTypeColor ("WARN", Util.TextColor.DkRed, Util.FontType.Bold);
            handler.setTypeColor ("INFO", Util.TextColor.Black, Util.FontType.Bold);
        }
    }
    
    private void dprintMessage (String message) {
        if (debug != null) {
            debug.print("INFO", message);
        }
        else {
            System.out.println(message);
        }
    }
    
    private void dprintError (String message) {
        if (debug != null) {
            debug.print("WARN", message);
        }
        else {
            System.err.println(message);
        }
    }
    
    private Properties createInitialPropertiesFile() {
        // set the default content of the properties
        // init each type to null entry
        props = new Properties();
        for (Type type : Type.values()) {
            props.setProperty(type.toString(), "");
        }

        try {
            // first, check if properties directory exists
            File proppath = new File (PROPERTIES_PATH);
            if (!proppath.exists())
                proppath.mkdir();

            // now create the file and save the initial blank data
            dprintMessage("Creating initial site.properties file.");
            File file = new File(PROPERTIES_PATH + PROPERTIES_FILE);
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                props.store(fileOut, "Initialization");
            }
        } catch (IOException ex) {
            dprintError(ex + " <" + PROPERTIES_PATH + PROPERTIES_FILE + ">");
            props = null;
        }

        return props;
    }

    public String getPropertiesItem (Type tag) {
        if (props == null)
            return "";

        String value = props.getProperty(tag.toString());
        if (value == null || value.isEmpty()) {
            dprintError("site.properties <" + tag + "> : not found ");
            return "";
        }

        dprintMessage("site.properties <" + tag + "> = " + value);
        return value;
    }
  
    public void setPropertiesItem (Type tag, String value) {
        // save changes to properties file
        // (currently the only parameter is the last configuration file loaded)
        if (props == null)
            return;

        // make sure the properties file exists
        File propsfile = new File(PROPERTIES_PATH + PROPERTIES_FILE);
        if (propsfile.exists()) {
            try {
                dprintMessage("site.properties <" + tag + "> set to " + value);
                props.setProperty(tag.toString(), value);
                FileOutputStream out = new FileOutputStream(PROPERTIES_PATH + PROPERTIES_FILE);
                props.store(out, "---No Comment---");
                out.close();
            } catch (FileNotFoundException ex) {
                dprintError(ex + "- site.properties");
            } catch (IOException ex) {
                dprintError(ex + "- site.properties");
            }
        }
    }  
}
