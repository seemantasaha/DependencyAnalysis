package gui;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import core.CGType;
import core.LibrarySummary;
import core.Program;
import core.ProgramOption;
import drivergen.Util;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;

import vlab.cs.ucsb.edu.ModelCounter;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.Opcodes.*;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.commons.AdviceAdapter;
import org.apache.commons.io.FileUtils;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

  /**
 *
 * @author zzk
 */
public class AnalyzerFrame extends javax.swing.JFrame {

  final static private String JANALYZER_CONFIG_PATH = System.getProperty("user.home") + "/.janalyzer/site.properties";

  final static public String  TOOL_PATH_AVERROES   = "averroes";
  final static public String  TOOL_PATH_TAMIFLEX   = "tamiflex";
  final static public String  TOOL_PATH_DECOMPILER = "bytecode-viewer";
  
  final static private String newLine = System.getProperty("line.separator");
  final static private int tagTabLength = 16;
  static public String classPath = "";
  
  /**
   * Creates new form JanalyzerGUI
   */
  @SuppressWarnings("unchecked")
  public AnalyzerFrame() {
    initComponents();
    
    // The rest of this constructor is added by hand!!!!!
    myProjPath = "";
    configInfo = new ConfigInfo();
    //propfile = new PropertiesFile(null);
    savedApiTextField = "";

    // set these up to allow these other frames affect their corresponding buttons on this frame
    button_averroesButton = this.averroesRunButton;
    button_decompilerButton = this.decompilerRunButton;

    // this will allow writing msgs to status display to be run from static methods
    statusMessage = new StatusMessage(this.cmdStatusTextField);
    statusMessage.init();
    
    // now copy the GUI settings from the static configInfo
    setGUIConfigSettings(configInfo);

    // initialize the config file selection to "janalyzer.config" in the current directory
    // (if the LastConfigFile tag in the janalyzer properties file is defined, it
    // will replace this value).
    this.configFile = System.getProperty("user.dir") + "/janalyzer.config";
    this.configFileChooser.setCurrentDirectory(new File(this.configFile));
    bConfigUpdate = true;
    
    // this sets the labels to bold because they don't get set from the configuration
    Font f = setupLabel1.getFont();
    setupLabel1.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
    setupLabel2.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
    setupLabel3.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
    setupLabel4.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

    // start the timer to run every 1000 msec with no delay
    timer = new Timer(1000, new TimerListener());

    // see if a properties file is found for Janalyzer
    this.properties = loadPropertiesFile();
    if (this.properties != null) {
        // check if jre path is already set and is valid
        jrePathName = getPropertiesItem (this.properties, "JrePath");

        // if rt.jar file not found in specified dir, mark it as invalid
        if (!jrePathName.isEmpty()) {
            File jrepath = new File(jrePathName + "/rt.jar");
            if (!jrepath.isFile()) {
                jrePathName = "";
            }
        }

        // check for project path entry
        myProjPath = getPropertiesItem (this.properties, "ProjectPath");

        // check for last configuration file entry
        String propValue = getPropertiesItem (this.properties, "LastConfigFile");
        if (!propValue.isEmpty()) {
            // parse the last configuration file
            int rc = setConfigurationFromFile(propValue);
            if (rc == 0) {
                // init the config file chooser to the last selection
                configFile = propValue;
                bConfigUpdate = false;
                
                // update the initial directory selection
                setJanalyzerPaths();
            }
        }
            
        // update the configInfo value from the project path from the properties file.
        String projpath = "~";
        if (!myProjPath.isEmpty())
            projpath = myProjPath;
        configInfo.setField(ConfigInfo.StringType.projectpath, projpath);
    
        // append the project name if it is not already
        String projname = configInfo.getField(ConfigInfo.StringType.projectname);
        updateProjectPathName(projname);
    }
    
    // JRE Path not found in properties file or not valid...
    if (jrePathName.isEmpty()) {
        // attempt to find a java path that contains the rt.jar file
        jrePathName = javaLibPathFinder();
        
        // now save it so everyone can use it
        if (jrePathName.isEmpty()) {
            statusMessage.info("unable to ascertain a valid JRE path");
        }
        else {
            statusMessage.info("-> JRE path from javaLibPathFinder: " + jrePathName);
            setPropertiesItem (this.properties, "JrePath", jrePathName);
        }
    }
    else {
        statusMessage.info("-> JRE path from properties file: " + jrePathName);
    }

    // set the GUI selections that come from properties elements
    jrePathTextField.setText(jrePathName);
    String projpath = configInfo.getField(ConfigInfo.StringType.projectpath);
    prjpathTextField.setText(projpath);

    // if project path is valid, save the location of the jars
    isProjectPathValid(projpath);

    // add a mouse listener for allowing use of cut/copy/paste
//    appScrollPane.addMouseListener(new ContextMenuMouseListener());
//    libScrollPane.addMouseListener(new ContextMenuMouseListener());
    apiTextField.addMouseListener(new ContextMenuMouseListener());
    entryTextField.addMouseListener(new ContextMenuMouseListener());
    prjpathTextField.addMouseListener(new ContextMenuMouseListener());
    prjnameTextField.addMouseListener(new ContextMenuMouseListener());
    jrePathTextField.addMouseListener(new ContextMenuMouseListener());
  }

    private static class StatusMessage {
        private final JTextField  cmdStatus;
        
        StatusMessage (JTextField textField) {
            cmdStatus = textField;
            cmdStatus.setVisible(false);
            cmdStatus.setForeground(Color.black);
            cmdStatus.setText("");
        }
        
        public void init () {
            cmdStatus.setVisible(false);
            cmdStatus.setForeground(Color.black);
            cmdStatus.setText("");
        }
        
        public void error (String message) {
            cmdStatus.setVisible(true);
            cmdStatus.setForeground(Color.red);
            cmdStatus.setText("ERROR: " + message + newLine);
            System.err.println(message);
        }
        
        public void info (String message) {
            //cmdStatus.setVisible(true);
            //cmdStatus.setForeground(Color.black);
            //cmdStatus.setText(message + newLine);
            System.out.println(message);
        }
    }
    
  private static Properties createInitialPropertiesFile() {
    // set the default content of the properties
    Properties props = new Properties();
    props.setProperty("JrePath", "");
    props.setProperty("ProjectPath", "");
    props.setProperty("LastConfigFile", "");

    try {
        // first, check if directory exists
        String ppath = System.getProperty("user.home") + "/.janalyzer";
        File proppath = new File (ppath);
        if (!proppath.exists())
            proppath.mkdir();

        // now create the file and save the initial blank data
        System.out.println("Creating initial site.properties file.");
        File file = new File(JANALYZER_CONFIG_PATH);
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            props.store(fileOut, "Initialization");
        }
    } catch (IOException ex) {
        System.err.println(ex + " <" + JANALYZER_CONFIG_PATH + ">");
        props = null;
    }

    return props;
  }
  
  public static Properties loadPropertiesFile() {
    Properties props = new Properties();
    File propfile = new File(JANALYZER_CONFIG_PATH);
    if (propfile.exists()) {
        try {
            // property file exists, read it in
            FileInputStream in = new FileInputStream(JANALYZER_CONFIG_PATH);
            props.load(in);
            in.close();
        } catch (FileNotFoundException ex) {
            System.err.println(ex + " <" + JANALYZER_CONFIG_PATH + ">");
            props = null;
        } catch (IOException ex) {
            System.err.println(ex + " <" + JANALYZER_CONFIG_PATH + ">");
            props = null;
        }
    }
    else {
        // property file does not exist - create a default one
        System.err.println("<FILE_NOT_FOUND> - " + JANALYZER_CONFIG_PATH);
        props = createInitialPropertiesFile();
    }

    return props;
  }

  public static String getPropertiesItem (Properties props, String tag) {
      String value = null;
        if (props == null)
            props = loadPropertiesFile();
        if (props != null) {
            value = props.getProperty(tag);
            if (value != null && !value.isEmpty())
                System.out.println("site.properties <" + tag + "> = " + value);
            else
                System.err.println("site.properties <" + tag + "> : not found ");
        }

        return (value == null) ? "" : value; // guarantee no null return
  }
  
    public static void setPropertiesItem (Properties props, String tag, String value) {

        // save changes to properties file
        // (currently the only parameter is the last configuration file loaded)
        if (props == null)
            props = loadPropertiesFile();
        if (props == null)
            return;

        // make sure the properties file exists
        File propsfile = new File(JANALYZER_CONFIG_PATH);
        if (!propsfile.exists()) {
            System.err.println("<FILE_NOT_FOUND> - site.properties");
            return;
        }
        try {
            System.out.println("site.properties <" + tag + "> set to " + value);
            props.setProperty(tag, value);
            FileOutputStream out = new FileOutputStream(JANALYZER_CONFIG_PATH);
            props.store(out, "---No Comment---");
            out.close();
        } catch (FileNotFoundException ex) {
            System.err.println(ex + "- site.properties");
        } catch (IOException ex) {
            System.err.println(ex + "- site.properties");
        }
  }  
  
    /**
     * allows AverroesFrame to indicate it has terminated
     */
    public static void exitFromAverroesFrame() {
        button_averroesButton.setEnabled(true);
    }

    /**
     * allows DecompilerFrame to indicate it has terminated
     */
    public static void exitFromDecompilerFrame() {
        button_decompilerButton.setEnabled(true);
    }

    /**
     * allows external updates of information to the configuration file
     * @param tag   - the field to process
     * @param field - data value for the tag
     * @return 0 if successful, -1 if invalid tag
     * @throws java.io.IOException if error writing to config file
     */
    public static int updateConfigFile(String tag, String field) throws IOException {
        // add entry to config settings
        System.out.println("updateConfigFile: <" + tag + "> = " + field);
        int retcode = configInfo.setFieldValue (tag, field);
        if (retcode == 0) {
            // generate an updated config file
            String content = generateConfigFile();
            if (content != null && !content.isEmpty()) {
                String configName = configInfo.getName();
                FileUtils.writeStringToFile(new File(configName), content, "UTF-8");
            }
        }
        return retcode;
    }
    
    /**
     * a FileFilter class for jar files
     */
    public static class JarFileFilter implements FileFilter {
        private final String[] okFileExtensions = new String[] {"jar", "war"};

        public boolean accept(File file) {
            for (String extension : okFileExtensions) {
                if (file.getName().toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Checks if the specified project path is valid.
     * The path is considered valid if the project directory exists and either
     * contains one or more jar files in it or contains a folder 'lib' that contains
     * at least one jar file. There may optionally be a challenge_program
     * directory in the project path that holds the jars, and below this dir there
     * may also be a server dir that contains the jars.
     * 
     * @param path - the project path
     * @return the path location of the jar files (if error, the 1st char will be '<').
     */
    public static String findPathToProjLib (String path) {
        // first verify the project path exists
        File folder = new File(path);
        if (!folder.isDirectory()) {
            return "<DIR_NOT_FOUND> - Project path : " + path;
        }
        // now check to see if project dir contains a challenge_program folder.
        // if so, start from there.
        String libname = path + "/challenge_program";
        folder = new File(libname);
        if (folder.isDirectory()) {
            path = libname;
        }
        // check to see if this dir contains a server folder.
        // if so, start from there.
        libname = path + "/server";
        folder = new File(libname);
        if (folder.isDirectory()) {
            path = libname;
        }

        // check to see if jars are in this dir
        libname = path;
        folder = new File(libname);
        File[] files = folder.listFiles(new JarFileFilter());
        if (files != null && files.length <= 0) {
            // no, check to see if dir contains a lib folder
            String oldlibname = libname;
            libname = path + "/lib";
            folder = new File(libname);
            if (!folder.isDirectory()) {
                return "<JARS_NOT_FOUND> - Project path : " + oldlibname;
            }

            // search dir for jar files in this dir
            files = folder.listFiles(new JarFileFilter());
            if (files.length <= 0)
                return "<JARS_NOT_FOUND> - Project path : " + libname;
        }

//       statusMessage.info(libname + " contained " + files.length + " jar files");
        return libname + "/";
    }

    private static boolean isProjectPathValid (String path) {
        // check if path is valid
        String libpath = findPathToProjLib(path);
        if (libpath.startsWith("<"))
            return false;
        
        return true;
    }
    
    public static String javaLibPathFinder() {
        String javaLibPath = System.getProperty("java.library.path");
        if (javaLibPath != null && !javaLibPath.isEmpty()) {
            System.out.println("javaLibPathFinder: java.library.path = " + javaLibPath);
            
            // if multiple entries, use the 1st one
            if (javaLibPath.startsWith(":"))
                javaLibPath = javaLibPath.substring(1);
            String javaFullPath = javaLibPath; // save original string
            
            while (javaFullPath.contains(":")) {
                javaLibPath = javaFullPath.substring(0, javaFullPath.indexOf(":"));
                javaFullPath = javaFullPath.substring(javaFullPath.indexOf(":") + 1);
        
                // remove any "xxx/../" entries from the path
                int offset = javaLibPath.lastIndexOf("/../");
                if (offset >= 0) {
                    String endPath = javaLibPath.substring(offset + 3);
                    String startPath = javaLibPath.substring(0, offset);
                    int begin = startPath.lastIndexOf("/");
                    if (begin >= 0)
                        startPath = startPath.substring(0, begin);
                    else
                        startPath = "";
                    javaLibPath = startPath + endPath;
                }
        
                // remove additional entries that follow the last "/lib" entry
                int eol = javaLibPath.lastIndexOf("/lib/");
                if (eol >= 0) {
                    javaLibPath = javaLibPath.substring(0, eol + 5);
                }
                System.out.println("testing path: " + javaLibPath);
                File rtfile = new File(javaLibPath + "/rt.jar");
                if (rtfile.isFile()) {
                    System.out.println("javaLibPathFinder: selection = " + javaLibPath);
                    return javaLibPath;
                }
            }
        }
        
        System.err.println("javaLibPathFinder: java.library.path - not set.");
        javaLibPath = "/usr/bin/java";
        File rtfile = new File(javaLibPath + "/rt.jar");
        if (rtfile.isFile()) {
            System.out.println("javaLibPathFinder: selection = " + javaLibPath);
            return javaLibPath;
        }
        System.out.println("javaLibPathFinder: null");
        return "";
    }

  /**
   * This extracts the configuration content from the GUI controls and saves it
   * in a static structure.
   */
  private void extractGUIConfigSettings (String configName) {
        configInfo.setName(configName);

        // listmodel controls
        if (this.aveCheckBox.isSelected()) {
            configInfo.setList(ConfigInfo.ListType.applAverroes, (DefaultListModel)this.appList.getModel());
            configInfo.setList(ConfigInfo.ListType.libAverroes,  (DefaultListModel)this.libList.getModel());
            // since the GUI field should be blanked for Averroes mode,
            // set the configInfo value to the saved value
            configInfo.setField(ConfigInfo.StringType.usedapis, savedApiTextField);
        }
        else {
            configInfo.setList(ConfigInfo.ListType.application, (DefaultListModel)this.appList.getModel());
            configInfo.setList(ConfigInfo.ListType.libraries,   (DefaultListModel)this.libList.getModel());
            configInfo.setField(ConfigInfo.StringType.usedapis, this.apiTextField.getText());
        }

        // textfield controls
        configInfo.setField(ConfigInfo.StringType.projectname, this.prjnameTextField.getText());
        configInfo.setField(ConfigInfo.StringType.projectpath, this.prjpathTextField.getText());
        configInfo.setField(ConfigInfo.StringType.addentries,  this.entryTextField.getText());

        // checkbox controls
        configInfo.setField(ConfigInfo.StringType.averroes,   this.aveCheckBox.isSelected()    ? "yes" : "no");
        configInfo.setField(ConfigInfo.StringType.exception,  this.exceptCheckBox.isSelected() ? "yes" : "no");
        configInfo.setField(ConfigInfo.StringType.infloop,    this.infCheckBox.isSelected()    ? "yes" : "no");
        configInfo.setField(ConfigInfo.StringType.audiofback, this.audioFeedbackCheckBox.isSelected() ? "yes" : "no");
        configInfo.setField(ConfigInfo.StringType.pubmeths,   this.publicMethodsCheckBox.isSelected() ? "yes" : "no");

        // combobox controls
        String optCombo = (String)this.optComboBox.getSelectedItem();
        configInfo.setField(ConfigInfo.StringType.cgoptions,  (optCombo == null) ? "" : optCombo);
  }
  
  /**
   * This sets the configuration GUI controls from the static structure.
   */
    private void setGUIConfigSettings (ConfigInfo configData) {
        // listmodel controls
        String aveChecked = configData.getField(ConfigInfo.StringType.averroes);
        if ("yes".equals(aveChecked)) {
            this.appList.setModel(configData.getList(ConfigInfo.ListType.applAverroes));
            this.libList.setModel(configData.getList(ConfigInfo.ListType.libAverroes));
            this.apiTextField.setText("");
            // save the config data in a temp location so we can restore it
            savedApiTextField = configData.getField(ConfigInfo.StringType.usedapis);
        }
        else {
            this.appList.setModel(configData.getList(ConfigInfo.ListType.application));
            this.libList.setModel(configData.getList(ConfigInfo.ListType.libraries));
            this.apiTextField.setText(configData.getField(ConfigInfo.StringType.usedapis));
        }

        // disable/enable the entry selection based on this checkbox value
        String pubmeths = configData.getField(ConfigInfo.StringType.pubmeths);
        if (pubmeths.equals("yes")) {
            this.entryTextField.setEnabled(false);
            this.entryOpenButton.setEnabled(false);
        }
        else {
            this.entryTextField.setEnabled(true);
            this.entryOpenButton.setEnabled(true);
        }

        // textfield controls
        this.prjnameTextField.setText (configData.getField(ConfigInfo.StringType.projectname));
        this.prjpathTextField.setText (configData.getField(ConfigInfo.StringType.projectpath));
        this.entryTextField.setText   (configData.getField(ConfigInfo.StringType.addentries));

        // checkbox controls
        this.aveCheckBox.setSelected       ("no".equals(configData.getField(ConfigInfo.StringType.averroes)));
        this.exceptCheckBox.setSelected    ("yes".equals(configData.getField(ConfigInfo.StringType.exception)));
        this.infCheckBox.setSelected       ("yes".equals(configData.getField(ConfigInfo.StringType.infloop)));
        this.audioFeedbackCheckBox.setSelected("yes".equals(configData.getField(ConfigInfo.StringType.audiofback)));
        this.publicMethodsCheckBox.setSelected("yes".equals(configData.getField(ConfigInfo.StringType.pubmeths)));

        // combobox controls
        this.optComboBox.setSelectedItem(configData.getField(ConfigInfo.StringType.cgoptions));
    }
  
  /**
   * this searches the 'content' block for the version and returns it.
   * 
   * @param content - the content string to search
   * 
   * @return The verssion as a String
   */
  private String findConfigVersion (String content) {
    // we're looking for the tag enclosed in <> at the begining of the line
    // followed by whitespace and the field value terminating on whitespace.
    String search = "(^# Version:)[ \t]*(.*)"; //(\\S+)";
    String entry = "";
    Pattern pattern = Pattern.compile(search, Pattern.MULTILINE);
    Matcher match = pattern.matcher(content);
    if (match.find()) {
        entry = match.group(2);
    }

    return entry;
  }

  /**
   * this searches the 'content' block for the specified 'tag' and
   * returns the corresponding value associated with it. An additional 'type'
   * argument specifies whether to additionally verify if the value found
   * is a valid file or directory.
   * 
   * tags are defined in the configuration file at the begining of a
   * line and enclosed in <> brackets. The corresponding value must be placed
   * after 1 or more whitespace chars (space or tab only) and must not contain
   * any whitespace.
   * 
   * @param content - the content string to search
   * @param tag     - the field to search for
   * 
   * @return The corresponding value associated with the field. An empty string
   * will be returned if either the field was not found or no value was defined
   * for it or if was not a valid directory or file (only if 'type' field was set.
   */
  private String findTagField (String content, ConfigInfo.StringType tag) {
    // we're looking for the tag enclosed in <> at the begining of the line
    // followed by whitespace and the field value terminating on whitespace.
    String search = "^(<" + tag + ">)[ \t]+(.*)$"; //(\\S+)";
    String entry = "";
    Pattern pattern = Pattern.compile(search, Pattern.MULTILINE);
    Matcher match = pattern.matcher(content);
    if (match.find()) {
        entry = match.group(2);
        statusMessage.info("- " + tag + ": " + entry);
    }
    else {
        statusMessage.info("- " + tag + ": ---");
    }

    return entry;
  }

  /**
   * this searches the 'content' block for the specified 'tag' and
   * adds the corresponding value to the list. Note that there may be more than
   * one of this type of tag in the configuration file to accomodate
   * multiple file selections.
   * 
   * tags are defined in the configuration file at the begining of a
   * line and enclosed in <> brackets. The corresponding value must be placed
   * after 1 or more whitespace chars (space or tab only) and must not contain
   * any whitespace.
   * 
   * @param content - the content string to search
   * @param tag     - the field to search for
   * @param listModel - the list from the widget corresponding to the tag
   * @return 0 if no errors
   */
  private int findTagFileList (String content, ConfigInfo.ListType tag, DefaultListModel listModel) {
    // we're looking for the tag enclosed in <> at the begining of the line
    // followed by whitespace and the field value terminating on whitespace.
    int retcode = 0;
    String search = "^(<" + tag + ">)[ \t]+(.*)$"; //(\\S+)";
//    String search = "(^<" + tag + ">)[ \t]*(\\S+)";
    String entry;
    Pattern pattern = Pattern.compile(search, Pattern.MULTILINE);
    Matcher match = pattern.matcher(content);
    int count = 0;
    while (match.find()) {
        entry = match.group(2);
        if (!entry.isEmpty()) {
            // verify file exists
            entry = getRelativeFileTag (tag, entry);
            if (entry != null) {
                ++count;
                statusMessage.info("- " + tag + ": " + entry);
                // add selection to Libraries List
                if (!listModel.contains(entry))
                    listModel.addElement(entry);
            }
            else {
                retcode = 1;
            }
        }
    }
    if (count == 0) {
        statusMessage.info("- " + tag + ": ---");
    }

    return retcode;
  }

  private static String addTagField (ConfigInfo.ListType tag, DefaultListModel listModel) {
        String content = "";

        int length = AnalyzerFrame.tagTabLength - 2 - tag.toString().length();
        length = (length > 0) ? length : 2;
        String spacing = new String(new char[length]).replace("\0", " ");

        for (int ix = 0; ix < listModel.getSize(); ix++) {
            String entry = listModel.getElementAt(ix).toString();
            // all ListType entries are relative paths, so convert
            entry = convertPathToRelative (entry);
            content += "<" + tag + ">" + spacing + entry + newLine;
        }

        return content;
  }
  
  /**
   * creates a config file entry for the specified tag using the field value
   * from configInfo.
   * 
   * @param tag - the config file tag to add
   * @return the entry to add to the config file
   */
  private static String addTagField (ConfigInfo.StringType tag) {
        String field = configInfo.getField (tag);
        if (field == null || field.isEmpty())
            return "";
        
        int length = AnalyzerFrame.tagTabLength - 2 - tag.toString().length();
        length = (length > 0) ? length : 2;
        String spacing = new String(new char[length]).replace("\0", " ");

        // if field is for a relative path parameter, convert it
        if (configInfo.isParamRelativePath(tag))
            field = convertPathToRelative (field);
        return "<" + tag + ">" + spacing + field + newLine;
  }

  /**
   * searches the content field for specified tag, verifies it is valid, and
   * saves the value in the corresponding config entry.
   * 
   * @param content - configuration file text content to parse
   * @param tag  - the tag to look for
   */
  private void setTagField (String content, ConfigInfo.StringType tag, ConfigInfo configData) {
    String value = findTagField (content, tag);
    configData.setField (tag, value);
  }

  /**
   * replaces the origPath portion of a string value with replacePath.
   * 
   * @param tagname  - the tag being replaced
   * @param value    - the original filename with original path
   * @param origPath - the pathname to remove from the value
   * @param newPath  - the pathname to replace it with
   * 
   * @return the updated value with the new path (null if error)
   */
  private String replacePath (String tagname, String value, String origPath, String newPath) {

    // get the value after removing the path portion and replace this in value
    // with the new replacement path
    String relPath = value.substring(origPath.length());
    value = newPath + relPath;
    
    // make sure replacement path is valid
    if (newPath.isEmpty()) {
        statusMessage.error("<FILE_NOT_FOUND> - " + value);
        return null;
    }

    // for startScript, the filename may have arguments after it.
    // let's remove them before we do a check on whther the file exists or not
    String filetemp = value;
    if (tagname.equals("startScript")) {
        int offset = filetemp.indexOf(' ');
        if (offset > 0)
            filetemp = filetemp.substring(0, offset);
    }
          
    // verify file exists in this new path
    File file = new File(filetemp);
    if (!file.isFile()) {
        statusMessage.error("<FILE_NOT_FOUND> - " + filetemp);
        return null;
    }

    return value;
  }
  
  /**
   * modifies the file path to replace it with either the janalyzer or project path.
   * This is used for saving the config file away so it doesn't contain absolute paths.
   * 
   * @param value - the config parameter value (a file path)
   * 
   * @return the relative file path (uses tags)
   */
  private static String convertPathToRelative (String value) {
      String relPath;
      String origPath = myProjPath;
      if (!origPath.isEmpty() && value.startsWith(origPath)) {
          relPath = value.substring(origPath.length());
          value = "${PRJPATH}" + relPath;
      }
      else {
          origPath = System.getProperty("user.dir");
          if (!origPath.isEmpty() && value.startsWith(origPath)) {
              relPath = value.substring(origPath.length());
              value = "${JANPATH}" + relPath;
          }
          else {
              origPath = jrePathName;
              if (!origPath.isEmpty() && value.startsWith(origPath)) {
                  relPath = value.substring(origPath.length());
                  value = "${JREPATH}" + relPath;
              }
        }
      }
      
      return value;
  }
  
  /**
   * checks a file path value to find the relative path replacement for it.
   * 
   * @param tagname - the tag for the config parameter
   * @param value - the config parameter value (a file path)
   * 
   * @return the absolute file path adjusted for the current system
   *         (null if not found or error condition)
   */
  private String checkRelativePath (String tagname, String value) {
    if (tagname == null || value == null) {
        statusMessage.error("checkRelativePath: invalid input");
        return null;
    }

    String[] pathtypelist = new String[] { "JREPATH", "PRJPATH", "JANPATH" };
    for (String type : pathtypelist) {
        // form the relative path tag from the type value
        String taggedPath = "${" + type + "}";
        String newPath;
    
        // find the corresponding replacement value
        switch (type) {
            case "PRJPATH":
                newPath  = myProjPath;
                break;
            case "JREPATH":
                newPath  = jrePathName;
                break;
            default:
            case "JANPATH":
                newPath  = System.getProperty("user.dir");
                break;
        }
    
        // generate replacement path and verify the file exists
        if (value.startsWith(taggedPath)) {
            // if the corresponding path is empty, just use absolute path
            if (newPath.isEmpty())
                return value;
            value = replacePath (tagname, value, taggedPath, newPath);
            if (value != null)
                statusMessage.info(tagname + " [" + type + "]: " + value);
            return value;
        }
    }
    
    // else, path did not match any of the above
    return null;
  }
  
  /**
   * returns the value for a 'relative' path file from a ListType parameter.
   * A 'relative' path is one that is based on either the project path, the
   * janalyzer path, or the JRE path, all of which will vary from one installation
   * to another. Note that this assumes the 'value' passed is a file and
   * will return a null if the file is not found.
   * 
   * @param tag - the tag of the entry to load
   * @param value - the value of the tag read from the loaded config file
   * 
   * @return the value for the tag after modifying for the users path selection (null if error)
   */
  private String getRelativeFileTag (ConfigInfo.ListType tag, String value) {

    // check if file exists as is, we use it
    File file = new File(value);
    if (file.isFile())
        return value;

    statusMessage.info(tag + "[ORIGINAL]: " + value);

    // else, file does not exist as stated...
    // first do the relative path def comparisons
    String newValue, relPath;
    newValue = checkRelativePath (tag.toString(), value);
    if (newValue != null) {
        return newValue;
    }

    // check if the tag is for library files and the entry is the rt.jar file.
    // the rt.jar file is always pulled from the user's specified JRE path,
    // and should only be found in the "libraries" entries. If it is here,
    // we can simply substitute the correct file path.
    if (tag ==  ConfigInfo.ListType.libraries && value.contains("/rt.jar")) {
        // YES - ignore the path and simply use the rt.jar file from the specified
        // JRE path for this user
        statusMessage.info(tag + " [JREPATH]: " + value);
        relPath = "/rt.jar";

        // make sure we have a valid JRE path specified for the user
        if (jrePathName == null || jrePathName.isEmpty()) {
            statusMessage.error("JRE Path not defined!");
            return null;
        }
        
        value = jrePathName + relPath;
        file = new File(value);

        // now verify file with updated path is valid
        if (!file.isFile()) {
            statusMessage.error("<FILE_NOT_FOUND> - " + tag + " : " + value);
            return null;
        }
    }

    return value;
  }
  
  /**
   * returns the value for a 'relative' path file.
   * same as above but for StringType parameters
   * 
   * @param tag - the tag of the entry to load
   * @param value - the value of the tag read from the loaded config file
   * 
   * @return the value for the tag after modifying for the users path selection (null if error)
   */
  private String getRelativeFileTag (ConfigInfo.StringType tag, String value) {

    // check if file exists as is, we use it
    File file = new File(value);
    if (file.isFile())
        return value;

    statusMessage.info(tag + "[ORIGINAL]: " + value);

    // do the relative path def comparisons
    String newValue = checkRelativePath (tag.toString(), value);
    if (newValue != null)
        return newValue;

    return value;
  }
  
  private int setRelativeFileField (String content, ConfigInfo.StringType tag, ConfigInfo configData) {
    // search for tag entry in content to get corresponding value
    int retcode = 0;
    String value = findTagField (content, tag);
    if (value == null || value.isEmpty()) {
        configData.setField (tag, "");
    }
    else {
        // check if file exists as is or needs to be modified
        value = getRelativeFileTag (tag, value);
        if (value != null)
            configData.setField (tag, value);
        else
            retcode = 1;
    }
    
    return retcode;
  }
  
  /**
   *  generates a configuration file from the current Frame settings.
     * @throws java.io.IOException
   */
  private static String generateConfigFile() {
    String configVers = configInfo.getVersion();
    String configName = configInfo.getName();
    if (configName == null || configName.isEmpty())
        return null;

    // start with a header defining the file
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    String content = "";
    content += "# This janalyzer configuration file was created on: " + dateFormat.format(date) + newLine;
    content += "# Version: " + configVers + newLine + newLine;

    content += addTagField (ConfigInfo.StringType.projectname);

    // this defines the debugger entry, but always sets it to disabled
    // (you can manually edit the config file to enable debug mode)
    content += addTagField (ConfigInfo.ListType.application,  configInfo.getList(ConfigInfo.ListType.application));
    content += addTagField (ConfigInfo.ListType.libraries,    configInfo.getList(ConfigInfo.ListType.libraries));
    content += addTagField (ConfigInfo.ListType.applAverroes, configInfo.getList(ConfigInfo.ListType.applAverroes));
    content += addTagField (ConfigInfo.ListType.libAverroes,  configInfo.getList(ConfigInfo.ListType.libAverroes));
    content += addTagField (ConfigInfo.StringType.usedapis);
    content += addTagField (ConfigInfo.StringType.addentries);
    content += addTagField (ConfigInfo.StringType.mainClass);
    content += addTagField (ConfigInfo.StringType.startScript);
    content += addTagField (ConfigInfo.StringType.appRegEx);
    content += addTagField (ConfigInfo.StringType.averroes);
    content += addTagField (ConfigInfo.StringType.exception);
    content += addTagField (ConfigInfo.StringType.infloop);
    content += addTagField (ConfigInfo.StringType.audiofback);
    content += addTagField (ConfigInfo.StringType.cgoptions);
    content += addTagField (ConfigInfo.StringType.pubmeths);
    content += addTagField (ConfigInfo.StringType.decompiler);
    content += addTagField (ConfigInfo.StringType.debugger);

    return content;
  }

  /**
   *  parses the configuration file to initialize the settings for the Frame.
   * 
   * @param configName - the name of the config file to parse
   * @param configData - the config file sructure to save the configuration in
   * #param prjPath - the current project path value
   * @return configuration data if successful, null if failure
   */
  private ConfigInfo parseConfigFile(String configName) {

    int failure = 0;
    File configfile = new File(configName);
    if (!configfile.isFile()) {
        statusMessage.error("<FILE_NOT_FOUND> - " + configName);
        return null;
    }
    statusMessage.info("[JANPATH] = " + System.getProperty("user.dir"));
    String oldProjname = configInfo.getField(ConfigInfo.StringType.projectname);
    String projpath = configInfo.getField(ConfigInfo.StringType.projectpath);

    // create a new ConfigInfo instance to read the file into (keep the current project path)
    ConfigInfo configData = new ConfigInfo(configName, projpath);
    try {
        // read the file into a String
        String content = FileUtils.readFileToString(new File(configName), "UTF-8");
        statusMessage.info("Parsing: " + configName);

        // get the version of the file we just read
        String configVers = findConfigVersion(content);
        configData.setVersion(configVers);
        statusMessage.info("Version: " + configVers);
        
        // load the project name first and update the project path accordingly,
        // so all the relative paths get set properly
        setTagField (content, ConfigInfo.StringType.projectname, configData);
        String newProjname = configData.getField(ConfigInfo.StringType.projectname);
        updateProjectPathName (newProjname);

        // if user has defined a project path, we need to modify the prjpathTextField
        // entry to replace the previous project name with the new one (if it changed)
        if (!myProjPath.isEmpty() && !oldProjname.equals(newProjname)) {
            if (projpath.endsWith(oldProjname)) {
                int length = projpath.length() - oldProjname.length();
                projpath = projpath.substring(0, length) + newProjname;
                configData.setField(ConfigInfo.StringType.projectpath, projpath);
                statusMessage.info("New Project Path: " + projpath);
            }
        }
            
        // now parse the string to initialize fields
        // (these entries may have paths specific to a user, so see if we can fix them)
        failure |= findTagFileList(content, ConfigInfo.ListType.application,  configData.getList(ConfigInfo.ListType.application));
        failure |= findTagFileList(content, ConfigInfo.ListType.libraries,    configData.getList(ConfigInfo.ListType.libraries));
        failure |= findTagFileList(content, ConfigInfo.ListType.applAverroes, configData.getList(ConfigInfo.ListType.applAverroes));
        failure |= findTagFileList(content, ConfigInfo.ListType.libAverroes,  configData.getList(ConfigInfo.ListType.libAverroes));

        failure |= setRelativeFileField (content, ConfigInfo.StringType.usedapis   , configData);
        failure |= setRelativeFileField (content, ConfigInfo.StringType.addentries , configData);
        failure |= setRelativeFileField (content, ConfigInfo.StringType.startScript, configData);

        setTagField (content, ConfigInfo.StringType.mainClass  , configData);
        setTagField (content, ConfigInfo.StringType.appRegEx   , configData);
        setTagField (content, ConfigInfo.StringType.averroes   , configData);
        setTagField (content, ConfigInfo.StringType.exception  , configData);
        setTagField (content, ConfigInfo.StringType.infloop    , configData);
        setTagField (content, ConfigInfo.StringType.audiofback , configData);
        setTagField (content, ConfigInfo.StringType.cgoptions  , configData);
        setTagField (content, ConfigInfo.StringType.pubmeths   , configData);
        setTagField (content, ConfigInfo.StringType.decompiler , configData);
        setTagField (content, ConfigInfo.StringType.debugger   , configData);
        
    } catch (IOException ex) {
        return null;
    }

    // ignore file if it had problems
    if (failure != 0)
        return null;
    
    return configData;
  }

  private boolean writeConfigFile (String content, String fname) {
    if (content != null && !content.isEmpty()) {
        try {
            FileUtils.writeStringToFile(new File(fname), content, "UTF-8");
        } catch (IOException ex) {
            statusMessage.error("<IO_EXCEPTION> - writing Configuration file: " + fname);
            return false;
        }
    }
    return true;
  }
  
  /**
   *  parses the configuration file to initialize the settings for the Frame.
   * 
   * @param configName - the name of the config file to parse
   * @return 0 on success
   */
  private int setConfigurationFromFile(String configName) {
    // make sure the config file is valid
    if (configName == null || configName.isEmpty()) {
        statusMessage.error("<INVALID_NAME> - Configuration file is null");
        return -1;
    }
    File cfgfile = new File(configName);
    if (!cfgfile.isFile()) {
        statusMessage.error("<FILE_NOT_FOUND> - Configuration file: " + configName);
        return -1;
    }
    ConfigInfo configData = parseConfigFile(configName);
    if (configData == null) {
        statusMessage.error("Configuration file parameter error in file: " + configName);
        return -1;
    }
    
    // success - update configuration
    configInfo = configData;
    String configVersion = configInfo.getVersion();
    String latestVersion = new ConfigInfo().getVersion();

    // Double version_val = Double.parseDouble(version);
    // determine if an old version should be modified
    if (!configVersion.equals(latestVersion)) {

        String[] selection = new String[] { "No", "Yes" };
        String title   = "Old configuration file version: " + configVersion;
        String message = "The configuration file version does not match" + newLine +
                         "the latest version: " + latestVersion + newLine + newLine +
                         "Do you want to update this file to the latest version ?";
        int which = JOptionPane.showOptionDialog(null, // parent component
            message,        // message
            title,          // title
            JOptionPane.DEFAULT_OPTION, // option type
            JOptionPane.PLAIN_MESSAGE,  // message type
            null,           // icon
            selection,      // selection options
            selection[0]);  // initial value
        if (which >= 0 && selection[which].equals("Yes")) {
            statusMessage.info("updating " + configName + " from version " + configVersion + " to" + latestVersion);

            // generate the configuration info
            String content = generateConfigFile();
            writeConfigFile (content, configName);
        }
    }

    // set the GUI config settings from the static structure
    setGUIConfigSettings(configInfo);
        
    // if the project path is valid, init the config file selection to it
    File file = new File(configInfo.getField(ConfigInfo.StringType.projectpath));
    if (file.isDirectory()) {
        this.configFileChooser.setCurrentDirectory(file);
    }
    return 0;
  }
  
    /**
     * This searches for the end of the current field (A-Z, a-z, 0-9, _) and
     * returns the length of the field.
     * 
     * @param text - the string to search for the end of the current field
     * @return the length of the field
     */
    private int findEndOfField (String text) {
        int ix;
        for(ix = 0; ix < text.length(); ix++) {
            if (text.charAt(ix) < '0' ||
               (text.charAt(ix) > '9' && text.charAt(ix) < 'A') ||
               (text.charAt(ix) > 'Z' && text.charAt(ix) < 'a' && text.charAt(ix) != '_') ||
                text.charAt(ix) > 'z')
                return ix;
        }
        return ix;
    }

    /**
     * split a project path selection into a base path and a project name.
     * 
     * @param prjpath - the full project path name
     * @return an array of strings, the 1st for the base path and the 2nd for the project name.
     */
    private ArrayList<String> splitProjectPath (String prjpath) {
        ArrayList<String> project = new ArrayList<>();
        String prjname = "";

        if (prjpath != null && !prjpath.isEmpty()) {
            // split path selection into base project path and project name
            // first, remove trailing '/' char if present
            if (prjpath.charAt(prjpath.length() - 1) == '/')
                prjpath = prjpath.substring(0, prjpath.length() - 1);

            // now find the last '/' that delineates the base path from the project name
            int offset = prjpath.lastIndexOf('/');
            if (offset >= 0 && offset < prjpath.length() - 1) {
                prjname = prjpath.substring(prjpath.lastIndexOf('/') + 1);
                prjpath = prjpath.substring(0, offset);
            }
        }

        project.add(prjpath);
        project.add(prjname);
        return project;
    }

    /**
     * Uses the Project Tools setup selections to determine where Janalyzer
     * setup directories should be set.
     * 
     * @return the path to the janalyzer app & lib files, or null if not found
     */
  private String setJanalyzerPaths () {
    String libPathName;
    String projpath = configInfo.getField(ConfigInfo.StringType.projectpath);
    String projname = configInfo.getField(ConfigInfo.StringType.projectname);
    
    if (aveCheckBox.isSelected()) {
        // AVERROES operation:
        // determine if janalyzer's averroes path is valid - this will be the lib dir
        // (assume janalyzer is run from current dir)
        libPathName = System.getProperty("user.dir") + "/test/averroes";
        File averPath = new File(libPathName);
        if (!averPath.isDirectory()) {
            System.err.println("Averroes path in Janalyzer not found: " + libPathName);
            return null;
        }
    
        if (projname.isEmpty()) {
            System.err.println("Project Name = (empty)");
            return null;
        }

        // check if janalyzer averroes dir has the specified project name
        String prjAverPath = libPathName + "/" + projname;
        File newPath = new File(prjAverPath);
        if (newPath.isDirectory()) {
            libPathName = prjAverPath;
        }
    }
    else {
        // verify project path and project name are valid and jar files are located
        if (projpath.isEmpty()) {
            System.err.println("Project Path = (empty)");
            return null;
        }
        if (projname.isEmpty()) {
            System.err.println("Project Name = (empty)");
            return null;
        }
        libPathName = findPathToProjLib(projpath);
        if (libPathName.startsWith("<")) {
            System.err.println("No jars found in project path: " + projpath);
            return null;
        }
    }

    // set the default dir for selecting applications, libraries, etc.
    this.fileChooser.setCurrentDirectory(new File(libPathName));
    System.out.println("Library Path = " + libPathName);
    return libPathName;
  }  
  
  private int generateProgram () {
    ArrayList<String> appPaths = new ArrayList<>();
    ArrayList<String> libPaths = new ArrayList<>();
    String apiPath, entryFilePath;

    // use the selected files
    DefaultListModel appListModel = (DefaultListModel)this.appList.getModel();
    DefaultListModel libListModel = (DefaultListModel)this.libList.getModel();
    
    for (int i = 0; i < appListModel.getSize(); i++)
      appPaths.add((String)appListModel.getElementAt(i));

    classPath = appPaths.get(0);
  
    for (int i = 0; i < libListModel.getSize(); i++)
      libPaths.add((String)libListModel.getElementAt(i));

    // if Averroes is not selected and the JRE path is setup and no rt.jar file
    // was added to the library list, add it automatically
    if (!this.aveCheckBox.isSelected() && !jrePathName.isEmpty()) {
      String jrename = jrePathName + "/rt.jar";
      File jrepath = new File(jrename);
      if (jrepath.isFile()) {
        // make sure the file isn't already in the list
        boolean bFound = false;
        for (int ix = 0; ix < libListModel.getSize(); ix++) {
          if (libListModel.get(ix).toString().endsWith("/rt.jar")) {
            bFound = true;
            break;
          }
        }
        if (!bFound) {
          libListModel.addElement(jrename);
          libPaths.add(jrename);
        }
      }
    }

    if (appPaths.isEmpty() || libPaths.isEmpty()) {
      JOptionPane.showMessageDialog (null, "APP/LIB can't be empty", "Error", JOptionPane.ERROR_MESSAGE);
      return 1;
    }
    
    apiPath = this.apiTextField.getText();
    entryFilePath = this.entryTextField.getText();
    if (this.publicMethodsCheckBox.isSelected())
      entryFilePath = null;

    String optStr = (String)this.optComboBox.getSelectedItem();
    if (optStr.equals("0-1-CFA"))
      ProgramOption.setCGType(CGType.ZeroOneCFA);
    else if (optStr.equals("0-CFA"))
      ProgramOption.setCGType(CGType.ZeroCFA);
    else if (optStr.equals("RTA"))
      ProgramOption.setCGType(CGType.RTA);
    
    boolean ave = this.aveCheckBox.isSelected();
    ProgramOption.setAverroesFlag(ave);
    
    boolean except = this.exceptCheckBox.isSelected();
    ProgramOption.setExceptionFlag(except);
    
    boolean inf = this.infCheckBox.isSelected();
    ProgramOption.setInfiniteLoopFlag(inf);
    
    try {
      Program.makeProgram(appPaths, libPaths, apiPath, entryFilePath);
      Program.analyzeProgram();
      Set<String> misses = Program.checkAnalysisScope();
      if (!misses.isEmpty()) {
        this.libMissTextArea.setText(null);
        for (String miss : misses)
          this.libMissTextArea.append(miss + newLine);
        //JOptionPane.showConfirmDialog(null, libMissPane, "Missing Methods", JOptionPane.DEFAULT_OPTION);
      }
      
      Set<String> unknowns = LibrarySummary.getUnknownMethodSet();
      if (!unknowns.isEmpty()) {
        String msg = "Missing Library Summaries -- If proceed, analysis depends on this will become imprecise";
        this.libMissTextArea.setText(null);
        for (String unknown : unknowns)
          this.libMissTextArea.append(unknown + newLine);
        //JOptionPane.showConfirmDialog(null, libMissPane, msg, JOptionPane.DEFAULT_OPTION);
      }
    } catch (Exception e) {
      e.printStackTrace();
      //JOptionPane.showMessageDialog (null, "APP/LIB can't be loaded", "Error", JOptionPane.ERROR_MESSAGE);
      dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
      return 2;
    }
   
    return 0;
  }

    /**
     * A generic function for appending formatted text to a JTextPane.
     * 
     * @param tp    - the TextPane to append to
     * @param msg   - message contents to write
     * @param color - color of text
     * @param font  - the font selection
     * @param size  - the font point size
     * @param ftype - type of font style
     */
    private void appendToPane(JTextPane tp, Util.TextColor color, int size, Util.FontType ftype, String msg)
    {
        AttributeSet aset = Util.setTextAttr(color, "Dialog", size, ftype);
        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg);
    }

    private void appendToPane(JTextPane tp, Util.FontType ftype, String msg)
    {
        appendToPane(tp, Util.TextColor.Black, 12, ftype, msg);
    }

    /**
     * This converts a line of text for wiki-page documents.
     * 
     * @param fileText - the text pane to output to
     * @param line  - the line of text
     */
    private void wikiFileConvert (JTextPane fileText, String line) {
        
        // the heading entry is always assumed to be at the start of the line
        // and the line will not contain any other metas
        if (line.startsWith("===") && line.endsWith("===")) {
            line = line.substring(3, line.length()-3);
            appendToPane(fileText, Util.TextColor.Brown, 14, Util.FontType.BoldItalic, line + "\n");
            return;
        }
        if (line.startsWith("==") && line.endsWith("==")) {
            line = line.substring(2, line.length()-2);
            appendToPane(fileText, Util.TextColor.Green, 16, Util.FontType.Bold, line + "\n");
            return;
        }
        if (line.startsWith("=") && line.endsWith("=")) {
            line = line.substring(1, line.length()-1);
            appendToPane(fileText, Util.TextColor.Blue, 18, Util.FontType.Bold, line + "\n");
            return;
        }

        // add terminator to string
        line += "\n";

        // search for the meta chars and place in an array
        ArrayList<FontEntry> list = new ArrayList<>();
        Util.FontType fontType = Util.FontType.Normal;
        for (int ix = 0; ix < line.length(); ix++) {
            if (line.substring(ix).startsWith("**")) {
                FontEntry entry = new FontEntry(0, fontType);
                fontType = entry.changeBold(ix);
                list.add(entry);
                ++ix; // bump again to skip meta chars, since they are length of 2
            }
            else if (line.substring(ix).startsWith("//")) {
                FontEntry entry = new FontEntry(0, fontType);
                fontType = entry.changeItalic(ix);
                list.add(entry);
                ++ix; // bump again to skip meta chars, since they are length of 2
            }
        }
        
        if (list.isEmpty()) {
            // no meta character pairs found - just print line
            appendToPane(fileText, Util.FontType.Normal, line);
            return;
        }

        int offset = 0;
        fontType = Util.FontType.Normal;
        while (list.size() > 0) {
            FontEntry entry = list.remove(0);
            // get offset of terminating meta (end of section to display)
            int index = entry.getIndex();
            if (index < 0) // this would be an error
                break;
            // this will always be true except for the case of a line starting with
            // a meta char. we only output the text preceeding a meta on each pass,
            // so skip this pass and we'll use the next meta to determine the ending
            // section to set.
            if (index > offset)
                appendToPane(fileText, fontType, line.substring(offset, index));

            // for next text section, get specified font & start of section
            fontType = entry.getFontType();
            offset = index + 2;             // skip over the meta string
        }
        // now print any remaining chars in the line
        if (offset >= 0 && offset < line.length())
            appendToPane(fileText, fontType, line.substring(offset));
    }  
    
    private void addTextTab (JTabbedPane tabbedPane, String tabName, File textfile) {

        if (textfile.isFile() && tabName != null && !tabName.isEmpty()) {
            BufferedReader in = null;
            try {
                // create a text area and place it in a scrollable panel
                JTextPane fileText = new JTextPane();
                JScrollPane fileScroll = new JScrollPane(fileText);
                
                // place the scroll pane in the tabbed pane
                tabbedPane.addTab(tabName, fileScroll);

                // now read the file contents into the text area
                in = new BufferedReader(new FileReader(textfile));
                String line = in.readLine();
                while (line != null) {
                    boolean bFound = false;
                    if (tabName.equals("README"))
                        wikiFileConvert(fileText, line);
                    else
                        appendToPane(fileText, Util.FontType.Normal, line + "\n");
                    line = in.readLine();
                }
                fileText.setCaretPosition(0); // set position to start of file
            } catch (FileNotFoundException ex) {
                statusMessage.error(ex + "<FILE_NOT_FOUND> - accessing " + tabName + " file");
            } catch (IOException ex) {
                statusMessage.error(ex + "<IO_EXCEPTION>");
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        statusMessage.error(ex + "<IO_EXCEPTION>");
                    }
                }
            }
        }
    }
  
    /**
     * clears all of the file selection fields
     */
    private void clearAllFileFields() {
        DefaultListModel listModel;
        listModel = (DefaultListModel)this.appList.getModel();
        listModel.clear();
        configInfo.clearList(ConfigInfo.ListType.application);
        configInfo.clearList(ConfigInfo.ListType.applAverroes);

        listModel = (DefaultListModel)this.libList.getModel();
        listModel.clear();
        configInfo.clearList(ConfigInfo.ListType.libraries);
        configInfo.clearList(ConfigInfo.ListType.libAverroes);

        entryTextField.setText("");
        configInfo.setField(ConfigInfo.StringType.addentries, "");

        apiTextField.setText("");
        configInfo.setField(ConfigInfo.StringType.usedapis, "");
    }
  
    /**
     * this updates the project path value in the config file with the specified
     * project name, if it is not already added.
     * 
     * @param projname - the project name selection
     */
    private void updateProjectPathName (String projname) {
        if (!projname.isEmpty()) {
            // if the project path does not end with the project name value,
            // add it to the end of the project path.
            String projpath = configInfo.getField(ConfigInfo.StringType.projectpath);
            int offset = projpath.lastIndexOf('/');
            String lastfield = null;
            if (offset > 0 && offset < projpath.length() - 1) {
                lastfield = projpath.substring(offset + 1);
                if (!lastfield.equals(projname))
                    lastfield = null;
            }
            if (lastfield == null) {
                projpath += "/" + projname;
                configInfo.setField(ConfigInfo.StringType.projectpath, projpath);
            }
            
            // update the GUI
            prjpathTextField.setText(projpath);
        }
    }
    
    /**
     * updates the project path and project name fields from a directory selection.
     * Note that it is assumed that the last entry in the path name will be used
     * as the project name.
     * 
     * @param projPath - the new project path selection
     */
    private void updateNewProjectPath (String projPath) {
        if (projPath.isEmpty()) {
            // allow a blank field so user can eliminate the path entirely
            setPropertiesItem(this.properties, "ProjectPath", projPath);

            // update the config info
            configInfo.setField(ConfigInfo.StringType.projectpath, projPath);

            // if we changed projects, these Averroes selections should be reset
            // since they are project-dependant
            configInfo.setField(ConfigInfo.StringType.appRegEx, "");
            configInfo.setField(ConfigInfo.StringType.mainClass, "");
            configInfo.setField(ConfigInfo.StringType.startScript, "");
            
            // probably a good idea to clear the file selections for the new project
            clearAllFileFields();
            return;
        }

        // verify the jar files can be found under this dir
        if (!isProjectPathValid(projPath)) {
            JOptionPane.showMessageDialog(null,
                "Directory: " + projPath,
                "Invalid Project Path", JOptionPane.ERROR_MESSAGE);
        }
        else {
            // split path selection into base project path and project name
            ArrayList<String> project = splitProjectPath(projPath);
            String projRoot = project.get(0);
            String projName = project.get(1);

            // save the current path selection minus the project name
            // (this is what gets saved in properties file for the project path)
            myProjPath = projRoot;
            setPropertiesItem(this.properties, "ProjectPath", myProjPath);

            // update the file chooser path for Load/Save if it was equal to
            // the previous project path
            File chooserPath = this.configFileChooser.getCurrentDirectory();
            String chooserName = chooserPath.getAbsolutePath();
            if (chooserName.startsWith(this.prjpathTextField.getText())) {
                File folder = new File(projPath);
                this.configFileChooser.setCurrentDirectory(folder);
            }

            // update the config info
            configInfo.setField(ConfigInfo.StringType.projectpath, projPath);
            configInfo.setField(ConfigInfo.StringType.projectname, projName);

            // if we changed projects, these Averroes selections should be reset
            // since they are project-dependant
            configInfo.setField(ConfigInfo.StringType.appRegEx, "");
            configInfo.setField(ConfigInfo.StringType.mainClass, "");
            configInfo.setField(ConfigInfo.StringType.startScript, "");
            
            // set default path for selecting libraries
            setJanalyzerPaths();
                
            // probably a good idea to clear the file selections for the new project
            clearAllFileFields();
                
            // update the text fields with the full path and the name of the project
            this.prjpathTextField.setText(projPath);
            this.prjnameTextField.setText(projName);
        }
    }
    
    // this launches the Program generation in a separate thread and puts up
    // a temp panel to show the user it is busy.
    private void launchProgramGen () {
        // start the program generation in another thread
        if (genThread != null)
            return;
        
        // the elapsed time label
        elapsedTimeLabel = new JLabel("00:00", SwingConstants.CENTER);
        elapsedTimeLabel.setFont(new java.awt.Font("Ubuntu", 1, 18));
        elapsedTimeLabel.setVerticalAlignment(SwingConstants.CENTER);

        // the label indicating we are loading
        JLabel loadingLabel = new JLabel("", SwingConstants.CENTER);
        loadingLabel.setFont(new java.awt.Font("Ubuntu", 1, 40));
        loadingLabel.setText("Loading..........");
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        // the STOP button
        JButton stopButton = new JButton();
        stopButton.setText("Stop");
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        
        // create the frame to display
        loadingFrame = new JFrame();
        String projname = configInfo.getField(ConfigInfo.StringType.projectname);
        if (!projname.isEmpty())
            loadingFrame.setTitle(projname);
        else
            loadingFrame.setTitle("Loading");
        loadingFrame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        Dimension dim = new Dimension(400,210);

        JPanel loadingPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        loadingPanel.add(loadingLabel);
        loadingPanel.add(elapsedTimeLabel);
        loadingPanel.add(stopButton);

        loadingFrame.setContentPane(loadingPanel);
        loadingFrame.setSize(dim);
        loadingFrame.setVisible(true);
        
        loadingFrame.setLocationRelativeTo(null);
        loadingFrame.setVisible(true);

        // start the program generation in a seperate thread
        programGen = new RunProgramGen();
        genThread = new Thread(programGen);
        genThread.start();

        // start the timer for indicating elapsed time
        elapsedSecs = 0;
        timer.setInitialDelay(0);
    	timer.start(); 
    }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings({"unchecked", "deprecation"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        folderChooser = new javax.swing.JFileChooser();
        fileChooser = new javax.swing.JFileChooser();
        configFileChooser = new javax.swing.JFileChooser();
        libMissPane = new javax.swing.JScrollPane();
        libMissTextArea = new javax.swing.JTextArea();
        repoChooser = new javax.swing.JFileChooser();
        jrePathChooser = new javax.swing.JFileChooser();
        entryChooser = new javax.swing.JFileChooser();
        janTabbedPane = new javax.swing.JTabbedPane();
        setupPanel = new javax.swing.JPanel();
        appScrollPane = new javax.swing.JScrollPane();
        appList = new javax.swing.JList();
        appAddButton = new javax.swing.JButton();
        appRemoveButton = new javax.swing.JButton();
        appClearButton = new javax.swing.JButton();
        libScrollPane = new javax.swing.JScrollPane();
        libList = new javax.swing.JList();
        libAddButton = new javax.swing.JButton();
        libRemoveButton = new javax.swing.JButton();
        libClearButton = new javax.swing.JButton();
        apiTextField = new javax.swing.JTextField();
        apiOpenButton = new javax.swing.JButton();
        entryOpenButton = new javax.swing.JButton();
        setupLabel1 = new javax.swing.JLabel();
        setupLabel2 = new javax.swing.JLabel();
        setupLabel3 = new javax.swing.JLabel();
        setupLabel4 = new javax.swing.JLabel();
        selectionPanel = new javax.swing.JPanel();
        optLabel = new javax.swing.JLabel();
        aveCheckBox = new javax.swing.JCheckBox();
        exceptCheckBox = new javax.swing.JCheckBox();
        runButton = new javax.swing.JButton();
        infCheckBox = new javax.swing.JCheckBox();
        optComboBox = new javax.swing.JComboBox();
        clearAllButton = new javax.swing.JButton();
        entryTextField = new javax.swing.JTextField();
        publicMethodsCheckBox = new javax.swing.JCheckBox();
        toolPanel = new javax.swing.JPanel();
        toolButtonPanel = new javax.swing.JPanel();
        decompilerRunButton = new javax.swing.JButton();
        averroesRunButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        pathSelectionsPanel = new javax.swing.JPanel();
        projpathOpenButton = new javax.swing.JButton();
        prjpathTextField = new javax.swing.JTextField();
        prjpathLabel = new javax.swing.JLabel();
        prjnameTextField = new javax.swing.JTextField();
        prjnameLabel = new javax.swing.JLabel();
        jrePathTextField = new javax.swing.JTextField();
        jrepathOpenButton = new javax.swing.JButton();
        jrepathLabel = new javax.swing.JLabel();
        audioFeedbackCheckBox = new javax.swing.JCheckBox();
        cmdStatusTextField = new javax.swing.JTextField();
        helpButton = new javax.swing.JButton();
        loadSetupButton = new javax.swing.JButton();
        saveSetupButton = new javax.swing.JButton();

        folderChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        libMissPane.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        libMissPane.setPreferredSize(new java.awt.Dimension(1000, 300));

        libMissTextArea.setColumns(20);
        libMissTextArea.setRows(5);
        libMissPane.setViewportView(libMissTextArea);

        repoChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Janalyzer");
        setMaximumSize(new java.awt.Dimension(32767, 600));
        setMinimumSize(new java.awt.Dimension(500, 600));
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
                formWindowGainedFocus(evt);
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        janTabbedPane.setMinimumSize(new java.awt.Dimension(0, 0));
        janTabbedPane.setPreferredSize(new java.awt.Dimension(580, 540));

        setupPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        setupPanel.setPreferredSize(new java.awt.Dimension(580, 580));

        appScrollPane.setMinimumSize(new java.awt.Dimension(0, 0));
        appScrollPane.setPreferredSize(new java.awt.Dimension(410, 90));

        appList.setMaximumSize(new java.awt.Dimension(32768, 32768));
        appScrollPane.setViewportView(appList);

        appAddButton.setText("Add");
        appAddButton.setToolTipText("<html>\nAdd an entry to the list of application jar files to analyze.<br>\nIf Averroes is enabled, this is always averroes-lib-class.jar and organized-app.jar,<br>\nIf Averroes is not enabled, this is the jar file (or files) for the application.\n</html>");
        appAddButton.setMaximumSize(new java.awt.Dimension(89, 25));
        appAddButton.setMinimumSize(new java.awt.Dimension(89, 25));
        appAddButton.setPreferredSize(new java.awt.Dimension(89, 25));
        appAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appAddButtonActionPerformed(evt);
            }
        });

        appRemoveButton.setBackground(new java.awt.Color(255, 204, 204));
        appRemoveButton.setText("Remove");
        appRemoveButton.setToolTipText("Remove an entry from the list of application jar files to analyze");
        appRemoveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appRemoveButtonActionPerformed(evt);
            }
        });

        appClearButton.setBackground(new java.awt.Color(255, 204, 204));
        appClearButton.setText("Clear");
        appClearButton.setToolTipText("Clear all entries from the list of application jar files to analyze");
        appClearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appClearButtonActionPerformed(evt);
            }
        });

        libScrollPane.setMinimumSize(new java.awt.Dimension(0, 0));
        libScrollPane.setPreferredSize(new java.awt.Dimension(410, 90));

        libList.setMaximumSize(new java.awt.Dimension(32768, 32768));
        libScrollPane.setViewportView(libList);

        libAddButton.setText("Add");
        libAddButton.setToolTipText("<html>\nAdd an entry to the list of library jar files to analyze.<br>\nIf Averroes is enabled, this is always placeholder-lib.jar<br>\nIf Averroes is not enabled, this is the library jar files only (not including the application).<br>\nplus the rt.jar file for the Java Runtime Engine. (Java 7)\n</html>");
        libAddButton.setMaximumSize(new java.awt.Dimension(89, 25));
        libAddButton.setMinimumSize(new java.awt.Dimension(89, 25));
        libAddButton.setPreferredSize(new java.awt.Dimension(89, 25));
        libAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                libAddButtonActionPerformed(evt);
            }
        });

        libRemoveButton.setBackground(new java.awt.Color(255, 204, 204));
        libRemoveButton.setText("Remove");
        libRemoveButton.setToolTipText("Remove an entry from the list of library jar files to analyze");
        libRemoveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                libRemoveButtonActionPerformed(evt);
            }
        });

        libClearButton.setBackground(new java.awt.Color(255, 204, 204));
        libClearButton.setText("Clear");
        libClearButton.setToolTipText("Clear all entries from the list of library jar files to analyze");
        libClearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                libClearButtonActionPerformed(evt);
            }
        });

        apiTextField.setMinimumSize(new java.awt.Dimension(0, 0));
        apiTextField.setPreferredSize(new java.awt.Dimension(410, 28));
        apiTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                apiTextFieldFocusLost(evt);
            }
        });
        apiTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                apiTextFieldActionPerformed(evt);
            }
        });

        apiOpenButton.setText("Open");
        apiOpenButton.setToolTipText("<html>\nLoads an ApisUsed text file that lists methods where Janalyzer<br>\ncan stop searching (to reduce execution time)\n</html>");
        apiOpenButton.setMaximumSize(new java.awt.Dimension(89, 25));
        apiOpenButton.setMinimumSize(new java.awt.Dimension(89, 25));
        apiOpenButton.setPreferredSize(new java.awt.Dimension(89, 25));
        apiOpenButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                apiOpenButtonActionPerformed(evt);
            }
        });

        entryOpenButton.setText("Open");
        entryOpenButton.setToolTipText("<html>\nLoads a file containing a list of additional entry points not found by WALA<br>\n(e.g. web interface entry points)\n</html>");
        entryOpenButton.setMaximumSize(new java.awt.Dimension(89, 25));
        entryOpenButton.setMinimumSize(new java.awt.Dimension(89, 25));
        entryOpenButton.setPreferredSize(new java.awt.Dimension(89, 25));
        entryOpenButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                entryOpenButtonActionPerformed(evt);
            }
        });

        setupLabel1.setText("Application");

        setupLabel2.setText("Libraries");

        setupLabel3.setText("Used APIs");

        setupLabel4.setText("Additional Entries");

        selectionPanel.setMinimumSize(new java.awt.Dimension(0, 0));

        optLabel.setText("CG Options");

        aveCheckBox.setSelected(true);
        aveCheckBox.setText("Averroes");
        aveCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        aveCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aveCheckBoxActionPerformed(evt);
            }
        });

        exceptCheckBox.setText("Exception");
        exceptCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        runButton.setBackground(new java.awt.Color(204, 255, 204));
        runButton.setText("Run");
        runButton.setToolTipText("<html>\nBegin running janalyzer using the current setup.<br>\nThis will generate the call graph chart and allow the user to interact<br>\nwith it for analysis.\n</html>");
        runButton.setMaximumSize(new java.awt.Dimension(117, 25));
        runButton.setMinimumSize(new java.awt.Dimension(117, 25));
        runButton.setPreferredSize(new java.awt.Dimension(92, 25));
        runButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runButtonActionPerformed(evt);
            }
        });

        infCheckBox.setSelected(true);
        infCheckBox.setText("Infinite Loop");
        infCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        optComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0-1-CFA", "0-CFA", "RTA" }));
        optComboBox.setPreferredSize(new java.awt.Dimension(120, 24));

        clearAllButton.setBackground(new java.awt.Color(255, 204, 204));
        clearAllButton.setText("Clear All");
        clearAllButton.setToolTipText("Clear all entries from the list of application jar files to analyze");
        clearAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearAllButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectionPanelLayout = new javax.swing.GroupLayout(selectionPanel);
        selectionPanel.setLayout(selectionPanelLayout);
        selectionPanelLayout.setHorizontalGroup(
            selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectionPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(selectionPanelLayout.createSequentialGroup()
                        .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(selectionPanelLayout.createSequentialGroup()
                                .addComponent(aveCheckBox)
                                .addGap(40, 40, 40)
                                .addComponent(optLabel))
                            .addComponent(infCheckBox))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(optComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 93, Short.MAX_VALUE)
                        .addComponent(runButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(selectionPanelLayout.createSequentialGroup()
                        .addComponent(exceptCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(clearAllButton))))
        );

        selectionPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {clearAllButton, runButton});

        selectionPanelLayout.setVerticalGroup(
            selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectionPanelLayout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(selectionPanelLayout.createSequentialGroup()
                        .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(optComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(optLabel)
                            .addComponent(runButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(clearAllButton)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(selectionPanelLayout.createSequentialGroup()
                        .addComponent(aveCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(infCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(exceptCheckBox)
                        .addGap(4, 4, 4))))
        );

        entryTextField.setPreferredSize(new java.awt.Dimension(410, 28));
        entryTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                entryTextFieldFocusLost(evt);
            }
        });
        entryTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                entryTextFieldActionPerformed(evt);
            }
        });

        publicMethodsCheckBox.setText("Use all public methods as entries");
        publicMethodsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                publicMethodsCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout setupPanelLayout = new javax.swing.GroupLayout(setupPanel);
        setupPanel.setLayout(setupPanelLayout);
        setupPanelLayout.setHorizontalGroup(
            setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, setupPanelLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(publicMethodsCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(selectionPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(setupPanelLayout.createSequentialGroup()
                        .addComponent(apiTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(apiOpenButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(setupPanelLayout.createSequentialGroup()
                        .addComponent(entryTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(entryOpenButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, setupPanelLayout.createSequentialGroup()
                        .addComponent(appScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(appRemoveButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(appClearButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(appAddButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, setupPanelLayout.createSequentialGroup()
                        .addComponent(libScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(libAddButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(libRemoveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(libClearButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, setupPanelLayout.createSequentialGroup()
                        .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(setupLabel2)
                            .addComponent(setupLabel3)
                            .addComponent(setupLabel4)
                            .addComponent(setupLabel1))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(12, 12, 12))
        );

        setupPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {libAddButton, libClearButton, libRemoveButton});

        setupPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {appAddButton, appClearButton, appRemoveButton});

        setupPanelLayout.setVerticalGroup(
            setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(setupPanelLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(selectionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(setupLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(setupPanelLayout.createSequentialGroup()
                        .addComponent(appAddButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(appRemoveButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(appClearButton))
                    .addComponent(appScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(8, 8, 8)
                .addComponent(setupLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(setupPanelLayout.createSequentialGroup()
                        .addComponent(libAddButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(libRemoveButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(libClearButton))
                    .addComponent(libScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE))
                .addGap(8, 8, 8)
                .addComponent(setupLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(apiTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(apiOpenButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addComponent(setupLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entryOpenButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(entryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(publicMethodsCheckBox)
                .addGap(4, 4, 4))
        );

        janTabbedPane.addTab("Configuration", setupPanel);

        toolPanel.setPreferredSize(new java.awt.Dimension(580, 580));

        toolButtonPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Tools", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        toolButtonPanel.setPreferredSize(new java.awt.Dimension(551, 120));

        decompilerRunButton.setBackground(new java.awt.Color(204, 204, 255));
        decompilerRunButton.setText("Decompiler");
        decompilerRunButton.setToolTipText("Runs the decompiler and places the resulting source code in the Project Path location.");
        decompilerRunButton.setMaximumSize(new java.awt.Dimension(117, 25));
        decompilerRunButton.setMinimumSize(new java.awt.Dimension(117, 25));
        decompilerRunButton.setPreferredSize(new java.awt.Dimension(117, 25));
        decompilerRunButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decompilerRunButtonActionPerformed(evt);
            }
        });

        averroesRunButton.setBackground(new java.awt.Color(204, 204, 255));
        averroesRunButton.setText("Averroes");
        averroesRunButton.setToolTipText("<html>\nRuns the Averroes file generator and places the resulting source code in the Project Path location.<br>\nAssumes that Tamiflex has been used to generate the summary file required for this to run.\n</html>");
        averroesRunButton.setMaximumSize(new java.awt.Dimension(117, 25));
        averroesRunButton.setMinimumSize(new java.awt.Dimension(117, 25));
        averroesRunButton.setPreferredSize(new java.awt.Dimension(117, 25));
        averroesRunButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                averroesRunButtonActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel1.setText("Generates source code from project jar files");

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel2.setText("Generates summary file stubs used by Janalyzer");

        javax.swing.GroupLayout toolButtonPanelLayout = new javax.swing.GroupLayout(toolButtonPanel);
        toolButtonPanel.setLayout(toolButtonPanelLayout);
        toolButtonPanelLayout.setHorizontalGroup(
            toolButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolButtonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(toolButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(toolButtonPanelLayout.createSequentialGroup()
                        .addComponent(decompilerRunButton, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel1))
                    .addGroup(toolButtonPanelLayout.createSequentialGroup()
                        .addComponent(averroesRunButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel2)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        toolButtonPanelLayout.setVerticalGroup(
            toolButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolButtonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(toolButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(decompilerRunButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(toolButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(averroesRunButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        decompilerRunButton.getAccessibleContext().setAccessibleDescription("");

        pathSelectionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Project Configuration", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        pathSelectionsPanel.setPreferredSize(new java.awt.Dimension(551, 120));

        projpathOpenButton.setText("Open");
        projpathOpenButton.setToolTipText("<html>\nSelects the base location of the challenge problem being examined.<br>\nThis folder typically contains a description of the problem, a \"questions\"<br>\nfolder that describes the challenge questions that pertain to the problem<br>\nand a \"challenge_program\" folder containing the problem itself, which will<br>\nconsist of one or more jar files or a \"lib\" folder that contains all of the jar files.<br>\nAll output files produced by any of the Janalyzer tools will be placed a \"janalyzer\"<br>\nsubdirectory in this path (each tool creates a unique subdirectory in this folder).<br>\nWhen setting this Project Path, the name of the final folder in the path will be<br>\nused as the default Project Name.\n</html>");
        projpathOpenButton.setMaximumSize(new java.awt.Dimension(89, 25));
        projpathOpenButton.setMinimumSize(new java.awt.Dimension(89, 25));
        projpathOpenButton.setPreferredSize(new java.awt.Dimension(89, 25));
        projpathOpenButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                projpathOpenButtonActionPerformed(evt);
            }
        });

        prjpathTextField.setToolTipText("");
        prjpathTextField.setMaximumSize(new java.awt.Dimension(2147483647, 28));
        prjpathTextField.setMinimumSize(new java.awt.Dimension(0, 0));
        prjpathTextField.setPreferredSize(new java.awt.Dimension(304, 28));
        prjpathTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                prjpathTextFieldFocusLost(evt);
            }
        });
        prjpathTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prjpathTextFieldActionPerformed(evt);
            }
        });

        prjpathLabel.setText("Project Path");

        prjnameTextField.setToolTipText("<html>\nSets the name of the project that is used in defining the folder in Averroes<br>\nto copy the output files to and also for defining the name of the driver files<br>\nusing the SPF tool. The final folder of the Project Path is initially used to set<br>\nthis value, but this can also be set manually.\n</html>");
        prjnameTextField.setMinimumSize(new java.awt.Dimension(4, 28));
        prjnameTextField.setPreferredSize(new java.awt.Dimension(304, 28));
        prjnameTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                prjnameTextFieldFocusLost(evt);
            }
        });

        prjnameLabel.setText("Project Name");

        jrePathTextField.setToolTipText("");
        jrePathTextField.setMaximumSize(new java.awt.Dimension(2147483647, 28));
        jrePathTextField.setMinimumSize(new java.awt.Dimension(0, 0));
        jrePathTextField.setPreferredSize(new java.awt.Dimension(304, 28));
        jrePathTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jrePathTextFieldFocusLost(evt);
            }
        });
        jrePathTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jrePathTextFieldActionPerformed(evt);
            }
        });

        jrepathOpenButton.setText("Open");
        jrepathOpenButton.setToolTipText("<html>\nThis sets the location of the Java Runtime library where the rt.jar file is located.<br>\nThis file must be included in the Libraries selection if Averroes is not enabled and is<br>\nalso used in generating the Averroes summaries files if Averroes mode is enabled.<br>\nThis entry will be saved in the .janalyzer/site.properties file in the user's home directory,<br>\nso once set it will be retained indefinitely on the machine used (NOT in the config file).<br>\nNote that this should be set for Java version 7, since Averroes does not work with version 8<br>\nand the challenge problems specify using version 7.\n</html>");
        jrepathOpenButton.setMaximumSize(new java.awt.Dimension(89, 25));
        jrepathOpenButton.setMinimumSize(new java.awt.Dimension(89, 25));
        jrepathOpenButton.setPreferredSize(new java.awt.Dimension(89, 25));
        jrepathOpenButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jrepathOpenButtonActionPerformed(evt);
            }
        });

        jrepathLabel.setText("JRE Path");

        audioFeedbackCheckBox.setText("Play audio tone upon completing lengthy operations");
        audioFeedbackCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audioFeedbackCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pathSelectionsPanelLayout = new javax.swing.GroupLayout(pathSelectionsPanel);
        pathSelectionsPanel.setLayout(pathSelectionsPanelLayout);
        pathSelectionsPanelLayout.setHorizontalGroup(
            pathSelectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pathSelectionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pathSelectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pathSelectionsPanelLayout.createSequentialGroup()
                        .addComponent(audioFeedbackCheckBox)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(pathSelectionsPanelLayout.createSequentialGroup()
                        .addGroup(pathSelectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(prjpathLabel)
                            .addComponent(prjnameLabel)
                            .addComponent(jrepathLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pathSelectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pathSelectionsPanelLayout.createSequentialGroup()
                                .addComponent(jrePathTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jrepathOpenButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(pathSelectionsPanelLayout.createSequentialGroup()
                                .addGroup(pathSelectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(prjnameTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                    .addComponent(prjpathTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(projpathOpenButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        pathSelectionsPanelLayout.setVerticalGroup(
            pathSelectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pathSelectionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pathSelectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jrepathOpenButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jrePathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jrepathLabel))
                .addGap(10, 10, 10)
                .addGroup(pathSelectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(prjpathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prjpathLabel)
                    .addComponent(projpathOpenButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pathSelectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(prjnameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prjnameLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 83, Short.MAX_VALUE)
                .addComponent(audioFeedbackCheckBox)
                .addContainerGap())
        );

        cmdStatusTextField.setPreferredSize(new java.awt.Dimension(551, 25));

        javax.swing.GroupLayout toolPanelLayout = new javax.swing.GroupLayout(toolPanel);
        toolPanel.setLayout(toolPanelLayout);
        toolPanelLayout.setHorizontalGroup(
            toolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(toolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(toolButtonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 547, Short.MAX_VALUE)
                    .addComponent(pathSelectionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 547, Short.MAX_VALUE)
                    .addComponent(cmdStatusTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 547, Short.MAX_VALUE))
                .addContainerGap())
        );
        toolPanelLayout.setVerticalGroup(
            toolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(pathSelectionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 254, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(toolButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(cmdStatusTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(88, 88, 88))
        );

        janTabbedPane.addTab("Setup", toolPanel);

        helpButton.setBackground(new java.awt.Color(255, 204, 204));
        helpButton.setFont(new java.awt.Font("DejaVu Sans", 1, 14)); // NOI18N
        helpButton.setText("Help");
        helpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpButtonActionPerformed(evt);
            }
        });

        loadSetupButton.setText("Load");
        loadSetupButton.setToolTipText("Loads selected configuration file setup");
        loadSetupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadSetupButtonActionPerformed(evt);
            }
        });

        saveSetupButton.setText("Save");
        saveSetupButton.setToolTipText("Saves the current setup in a configuration file");
        saveSetupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSetupButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(janTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(helpButton)
                        .addGap(123, 123, 123)
                        .addComponent(loadSetupButton, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(saveSetupButton, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(janTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 579, Short.MAX_VALUE)
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(loadSetupButton)
                        .addComponent(saveSetupButton))
                    .addComponent(helpButton))
                .addGap(10, 10, 10))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void appAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_appAddButtonActionPerformed
    statusMessage.init();
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Jar and Class Files","jar", "class");
    this.fileChooser.setCurrentDirectory(new File("/home/seem/research/tools/jqf/tutorial/jayhorn-recursive-assert-reach/")); //temporary
    this.fileChooser.setFileFilter(filter);
    this.fileChooser.setSelectedFile(null);
    this.fileChooser.setMultiSelectionEnabled(true);
    int retVal = this.fileChooser.showOpenDialog(this);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      DefaultListModel listModel = (DefaultListModel)this.appList.getModel();
      File[] files = this.fileChooser.getSelectedFiles();
      for (File file : files) {
        String app = file.getAbsolutePath();
        if (!listModel.contains(app))
          listModel.addElement(app);
      }
    }
  }//GEN-LAST:event_appAddButtonActionPerformed

  private void appRemoveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_appRemoveButtonActionPerformed
    statusMessage.init();
    String app = (String)this.appList.getSelectedValue();
    DefaultListModel listModel = (DefaultListModel)this.appList.getModel();
    listModel.removeElement(app);
  }//GEN-LAST:event_appRemoveButtonActionPerformed

  private void libAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_libAddButtonActionPerformed
    statusMessage.init();
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Jar Files","jar");
    this.fileChooser.setCurrentDirectory(new File("/home/seem/research/benchmarks/blazer")); //temporary
    this.fileChooser.setFileFilter(filter);
    this.fileChooser.setSelectedFile(null);
    this.fileChooser.setMultiSelectionEnabled(true);
    int retVal = this.fileChooser.showOpenDialog(this);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      DefaultListModel listModel = (DefaultListModel)this.libList.getModel();
      File[] files = this.fileChooser.getSelectedFiles();
      for (File file : files) {
        String lib = file.getAbsolutePath();
        
        // if user added an rt.jar file, ask if he wants to use it as default JRE path selection
        if (lib.endsWith("/rt.jar")) {
            String newJrePathName = lib.substring(0, lib.lastIndexOf('/'));
            String[] selection = new String[] { "No", "Yes" };
            String title   = "Update JRE Path";
            String message = "Do you wish to use this location as the default" + newLine +
                             "JRE Path selection?" + newLine + newLine +
                             "Current JRE Path: " + jrePathName + newLine +
                             "New JRE Path:     " + newJrePathName;
            int which = JOptionPane.showOptionDialog(null, // parent component
                    message,        // message
                    title,          // title
                    JOptionPane.DEFAULT_OPTION, // option type
                    JOptionPane.PLAIN_MESSAGE,  // message type
                    null,           // icon
                    selection,      // selection options
                    selection[0]);  // initial value
            if (which >= 0 && "Yes".equals(selection[which])) {
                jrePathName = newJrePathName;
                setPropertiesItem (this.properties, "JrePath", jrePathName);
                jrePathTextField.setText(jrePathName);
            }
            
            // restore default directory selection to the project path, since
            // that is where the rest of the files come from
            String libPathName = configInfo.getField(ConfigInfo.StringType.projectpath);
            libPathName = findPathToProjLib(libPathName);
            if (!libPathName.startsWith("<"))
                this.fileChooser.setCurrentDirectory(new File(libPathName));
        }
        
        if (!listModel.contains(lib))
          listModel.addElement(lib);
      }
    }
  }//GEN-LAST:event_libAddButtonActionPerformed

  private void libRemoveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_libRemoveButtonActionPerformed
    statusMessage.init();
    String lib = (String)this.libList.getSelectedValue();
    DefaultListModel listModel = (DefaultListModel)this.libList.getModel();
    listModel.removeElement(lib);
  }//GEN-LAST:event_libRemoveButtonActionPerformed

  private void apiOpenButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_apiOpenButtonActionPerformed
    statusMessage.init();
    this.fileChooser.setFileFilter(null);
    //this.fileChooser.setSelectedFile(null);
    this.fileChooser.setMultiSelectionEnabled(false);
    int retVal = this.fileChooser.showOpenDialog(this);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      File file = this.fileChooser.getSelectedFile();
      this.apiTextField.setText(file.getAbsolutePath());
      String field = apiTextField.getText();
      configInfo.setField(ConfigInfo.StringType.usedapis, field);
      statusMessage.info("apiTextField = " + field);
    }
  }//GEN-LAST:event_apiOpenButtonActionPerformed

  private void entryOpenButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_entryOpenButtonActionPerformed
    statusMessage.init();
    this.fileChooser.setFileFilter(null);
    //this.fileChooser.setSelectedFile(new File("entry_point"));
    this.fileChooser.setMultiSelectionEnabled(false);
    int retVal = this.fileChooser.showOpenDialog(this);
    if (retVal == JFileChooser.APPROVE_OPTION) {
        File file = this.fileChooser.getSelectedFile();
        this.entryTextField.setText(file.getAbsolutePath());
        String field = entryTextField.getText();
        configInfo.setField(ConfigInfo.StringType.addentries, field);
        statusMessage.info("entryTextField = " + field);
    }
  }//GEN-LAST:event_entryOpenButtonActionPerformed
    
    private void projpathOpenButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_projpathOpenButtonActionPerformed
        statusMessage.init();
        String projpath = configInfo.getField(ConfigInfo.StringType.projectpath);
        if (!projpath.isEmpty()) {
            File dfltPath = new File(projpath);
            this.folderChooser.setCurrentDirectory(dfltPath);
        }
        this.folderChooser.setMultiSelectionEnabled(false);
        int retVal = this.folderChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {

            // update the GUI and configInfo
            File folder = this.folderChooser.getSelectedFile();
            String prjpath = folder.getAbsolutePath();

            // update the path
            updateNewProjectPath (prjpath);
        }
    }//GEN-LAST:event_projpathOpenButtonActionPerformed

    private void decompilerRunButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decompilerRunButtonActionPerformed
        statusMessage.init();

        // only run if jar files are found
        String projPath = this.prjpathTextField.getText();
        if (!isProjectPathValid(projPath)) {
            JOptionPane.showMessageDialog(null,
                "Directory: " + projPath,
                "Invalid Project Path", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // run the decompiler gui
        Decompiler decompilerFrame = new Decompiler(AnalyzerFrame.configInfo);
        decompilerFrame.setLocationRelativeTo(null);
        decompilerFrame.setVisible(true);
        decompilerRunButton.setEnabled(false);
    }//GEN-LAST:event_decompilerRunButtonActionPerformed

    private void saveSetupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSetupButtonActionPerformed
        /*statusMessage.init();
        // generate a configuration file from the current settings and save it to the specified file.
        File defaultFile = new File("janalyzer.config");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Config Files","config");
        this.configFileChooser.setFileFilter(filter);
        this.configFileChooser.setSelectedFile(defaultFile);
        this.configFileChooser.setApproveButtonText("Save");
        this.configFileChooser.setMultiSelectionEnabled(false);
        int retVal = this.configFileChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = this.configFileChooser.getSelectedFile();
            configFile = file.getAbsolutePath();
            bConfigUpdate = true;

            // extract the GUI config settings into the static structure
            extractGUIConfigSettings(configFile);

            // generate the configuration file
            String content = generateConfigFile();
            boolean bSuccess = writeConfigFile(content, configFile);
            if (bSuccess) {
                // save the setup file as the default on startup
                setPropertiesItem(this.properties, "LastConfigFile", configFile);
            }
        }*/

        /*try {
            Set<Integer> allSrcLines = new HashSet<>();
            //allSrcLines.add(6); allSrcLines.add(11); allSrcLines.add(12);
            allSrcLines.add(30); allSrcLines.add(35); allSrcLines.add(36);
            InputStream in = new FileInputStream("src/input/Login.class");
            String className = "Login";
            String procName = "login_safe";
            try {
                ClassReader cr = new ClassReader(in);
                //ClassNode classNode = new ClassNode();

                //ClassNode is a ClassVisitor
                //cr.accept(classNode, 0);

                //Let's move through all the methods

//                for (MethodNode methodNode : classNode.methods) {
//                    System.out.println(methodNode.name + "  " + methodNode.desc);
//                }

                ClassWriter cw = new ClassWriter(cr, Opcodes.ASM4);

                // add the static final fields
                for (int lineno : allSrcLines) {
                    cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "branchAt"+lineno+"all", "int", null, null).visitEnd();
                    cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "branchAt"+lineno+"fallThrough", "int", null, null).visitEnd();
                }

                ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {

                    class ourMethodVisitor extends MethodVisitor {

                        int line;
                        jdk.internal.org.objectweb.asm.Type[] params;

                        ourMethodVisitor(MethodVisitor mv, jdk.internal.org.objectweb.asm.Type[] param) {
                            super(Opcodes.ASM4, mv);
                            this.params = param;
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
                                    for (jdk.internal.org.objectweb.asm.Type tp : params) {
                                        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                                        if (tp.equals(jdk.internal.org.objectweb.asm.Type.BOOLEAN_TYPE)) {
                                            mv.visitVarInsn(Opcodes.ILOAD, i);
                                        } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.BYTE)) {
                                            mv.visitVarInsn(Opcodes.ILOAD, i);
                                        } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.CHAR_TYPE)) {
                                            mv.visitVarInsn(Opcodes.ILOAD, i);
                                        } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.SHORT_TYPE)) {
                                            mv.visitVarInsn(Opcodes.ILOAD, i);
                                        } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.INT_TYPE)) {
                                            mv.visitVarInsn(Opcodes.ILOAD, i);
                                        } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.LONG_TYPE)) {
                                            mv.visitVarInsn(Opcodes.LLOAD, i);
                                            i++;
                                        } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.FLOAT_TYPE)) {
                                            mv.visitVarInsn(Opcodes.FLOAD, i);
                                        } else if (tp.equals(jdk.internal.org.objectweb.asm.Type.DOUBLE_TYPE)) {
                                            mv.visitVarInsn(Opcodes.DLOAD, i);
                                            i++;
                                        } else {
                                            if (tp.toString().equals("[B")) {
                                                mv.visitVarInsn(Opcodes.ALOAD, i);
                                                mv.visitFieldInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([B)Ljava/lang/String;");
                                            } else {
                                                mv.visitVarInsn(Opcodes.ALOAD, i);
                                            }
                                        }
                                        mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");

                                        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                                        mv.visitLdcInsn("\t");
                                        mv.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");
                                        i++;
                                    }

                                    for (int lineno : allSrcLines) {
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
                            if (allSrcLines.contains(line)) {
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                mv.visitFieldInsn(Opcodes.GETSTATIC, className, "branchAt"+line+"all", "I");
                                mv.visitInsn(Opcodes.ICONST_1);
                                mv.visitInsn(Opcodes.IADD);
                                mv.visitFieldInsn(Opcodes.PUTSTATIC, className, "branchAt"+line+"all", "I");
                            }
                            if (allSrcLines.contains(line-1)) {
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                mv.visitFieldInsn(Opcodes.GETSTATIC, className, "branchAt"+(line-1)+"fallThrough", "I");
                                mv.visitInsn(Opcodes.ICONST_1);
                                mv.visitInsn(Opcodes.IADD);
                                mv.visitFieldInsn(Opcodes.PUTSTATIC, className, "branchAt"+(line-1)+"fallThrough", "I");
                            }
                        }
                    }

                    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                        if (cv == null) {
                            return null;
                        }
                        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//                        if ("main".equals(name)) {
//                            return new ourMethodVisitor(mv, jdk.internal.org.objectweb.asm.Type.getArgumentTypes(desc));
//                        } else
                        if (procName.equals(name)) {
                            return new ourMethodVisitor(mv, jdk.internal.org.objectweb.asm.Type.getArgumentTypes(desc));
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
                DataOutputStream dout=new DataOutputStream(new FileOutputStream(new File(outDir,className+".class")));
                dout.write(cw.toByteArray());
                dout.flush();
                dout.close();

                //System.out.println(getLineNumber("src/Login.class"));

            } catch(IOException ex) {
                System.err.println(ex.toString());
            }
        } catch(FileNotFoundException ex) {
            System.err.println(ex.toString());
        }*/

        /*String s = null;

        try {
            Process p = Runtime.getRuntime().exec("python src/coco-channel/main.py src/coco-channel/input/blazer/json/Login_UNSAFE");

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

            System.exit(0);
        }
        catch (IOException e) {
            System.out.println("exception happened - here's what I know: ");
            e.printStackTrace();
            System.exit(-1);
        }*/

        ModelCounter mc = new ModelCounter(4, "abc.string");

        String constraint = "(declare-fun h () String)\n" +
                "(declare-fun l () String)\n" +
                "(assert (in h /[A-Z]{0,4}/))\n" +
                "(assert (in l /[A-Z]{0,4}/))\n" +
                "\n" +
                "(assert (not (= (len  l) (len  h))))\n" +
                "(assert (<= (len  l) 4))\n" +
                "(assert (<= (len  h) 4))\n" +
                "(check-sat)";

        constraint = "(declare-fun h () String)(declare-fun l () String)\n" +
                "\n" +
                "(assert (and (in h /[A-Z]{0,4}/)(in l /[A-Z]{0,4}/)))\n" +
                "\n" +
                "(assert (or (and (not (= (len \"A\") (len l))))))\n" +
                "\n" +
                "(assert (or (and (not (= (charAt \"FRPZ\" 0) (charAt l 0)))(= (len \"FRPZ\") (len l)))))\n" +
                "\n" +
                "(assert (or (and (not (= (charAt \"AIZC\" 0) (charAt l 0)))(= (len \"AIZC\") (len l)))))\n" +
                "\n" +
                "(assert (or (and (not (= (charAt \"DUKC\" 0) (charAt l 0)))(= (len \"DUKC\") (len l)))))\n" +
                "\n" +
                "(assert (not (= l \"\")))\n" +
                "(assert (not (= l \"A\")))\n" +
                "(assert (not (= l \"FRPZ\")))\n" +
                "(assert (not (= l \"AIZC\")))\n" +
                "(assert (not (= l \"DUKC\")))\n" +
                "\n" +
                "(check-sat)";

        BigDecimal count = mc.getModelCount(constraint);

        System.out.println(count);

        mc.disposeABC();

    }//GEN-LAST:event_saveSetupButtonActionPerformed

    /*public static int getLineNumber(String path) throws IOException {
        final File f = new File(path);
        try (FileInputStream fis = new FileInputStream(f)) {
            ClassReader reader = new ClassReader(fis);
            ClassNode clNode = new ClassNode(Opcodes.ASM5);
            reader.accept(clNode, Opcodes.ASM5);
            for (MethodNode mNode : (List<MethodNode>) clNode.methods) {
                if (mNode.name.equals("login_safe")) {
                    ListIterator<AbstractInsnNode> it = mNode.instructions.iterator();
                    while (it.hasNext()) {
                        AbstractInsnNode  inNode = it.next();
                        if (inNode instanceof LineNumberNode) {
                            int lineno = ((LineNumberNode) inNode).line;
                            System.out.println("line : " + lineno);
                            //}
                            if(lineno == 30 || lineno ==35 || lineno == 36)
                                System.out.println("good line number");
                            if ((inNode.getOpcode() == 198 || inNode.getOpcode() == 199 || (inNode.getOpcode() >= 153 && inNode.getOpcode() <= 166)) ||
                                    (inNode.getNext() != null && (inNode.getNext().getOpcode() == 198 || inNode.getNext().getOpcode() == 199 || (inNode.getNext().getOpcode() >= 153 && inNode.getNext().getOpcode() <= 166)) ||
                                    (inNode.getNext() != null && inNode.getNext().getNext() != null && (inNode.getNext().getNext().getOpcode() == 198 || inNode.getNext().getNext().getOpcode() == 199 || (inNode.getNext().getNext().getOpcode() >= 153 && inNode.getNext().getNext().getOpcode() <= 166))))) {

                                System.out.println("this is it");

                            }
                        }
                    }
                }
            }
        }
        return -1;
    }*/

    private void loadSetupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadSetupButtonActionPerformed
        statusMessage.init();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Config Files","config");
        this.configFileChooser.setFileFilter(filter);
        this.configFileChooser.setApproveButtonText("Open");
        this.configFileChooser.setMultiSelectionEnabled(false);
        int retVal = this.configFileChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = this.configFileChooser.getSelectedFile();
            configFile = file.getAbsolutePath();
            bConfigUpdate = false;

            // run the config file
            setConfigurationFromFile(configFile);

            // save the setup file as the default on startup
            setPropertiesItem(this.properties, "LastConfigFile", configFile);

            // update jar path & jar list from project path
            isProjectPathValid(this.prjpathTextField.getText());
                
            // set default path for selecting libraries
            setJanalyzerPaths();
        }
    }//GEN-LAST:event_loadSetupButtonActionPerformed

    private void appClearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_appClearButtonActionPerformed
        DefaultListModel listModel = (DefaultListModel)this.appList.getModel();
        listModel.clear();
    }//GEN-LAST:event_appClearButtonActionPerformed

    private void libClearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_libClearButtonActionPerformed
        DefaultListModel listModel = (DefaultListModel)this.libList.getModel();
        listModel.clear();
    }//GEN-LAST:event_libClearButtonActionPerformed

    private void averroesRunButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_averroesRunButtonActionPerformed
        statusMessage.init();

        // make sure we have a valid JRE path specified for the user
        if (jrePathName == null || jrePathName.isEmpty()) {
            statusMessage.error("JRE Path not defined!");
            return;
        }
        
        // must have a valid project name before running averroes
        if (this.prjnameTextField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Project Name must be defined",
                "Invalid Project Name", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // only run if jar files are found and JRE has been specified
        String projPath = this.prjpathTextField.getText();
        if (!isProjectPathValid(projPath)) {
            JOptionPane.showMessageDialog(null,
                "Directory: " + projPath,
                "Invalid Project Path", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // run the decompiler gui
        AverroesFrame averroesFrame = new AverroesFrame(AnalyzerFrame.configInfo);
        averroesFrame.setLocationRelativeTo(null);
        averroesFrame.setVisible(true);
        averroesRunButton.setEnabled(false);
    }//GEN-LAST:event_averroesRunButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // exit if we have no valid config file name (should always be something)
        if (configFile == null || configFile.isEmpty())
            return;
        
        // this is the default config file
        String defaultConfig = System.getProperty("user.dir") + "/default.config";
        
        // determine if the config settings have changed
        boolean bModified = false;
        File cfgfile = new File(configFile);
        if (cfgfile.isFile()) {
            ConfigInfo fileData = parseConfigFile(configFile);
            if (fileData == null)
                statusMessage.error("Configuration file parameter error in file: " + configFile);
            else if (fileData.compareTo(configInfo) == 0)
                statusMessage.info("No changes made to config file: " + configFile);
            else
                bModified = true;
        }

        // bConfigUpdate is true if config file has been saved.
        // it will be false if file is loaded and not saved.
        
        // if user has loaded a configuration from a file and has made changes
        // on it but never saved it, ask him if he wants to update the loaded
        // config file or save modifications in default config file.
        if (bConfigUpdate != true && bModified && !configFile.equals(defaultConfig)) {
            String[] selection = new String[] { "No", "Yes" };
            String title   = "Unsaved Configuration Settings";
            String message = "The configuration settings have been modified from the loaded file:" + newLine +
                             configFile + newLine + newLine +
                             "Do you want to update this file?" + newLine +
                             "(If not, the changes will be saved in the default config file)";
            int which = JOptionPane.showOptionDialog(null, // parent component
                message,        // message
                title,          // title
                JOptionPane.DEFAULT_OPTION, // option type
                JOptionPane.PLAIN_MESSAGE,  // message type
                null,           // icon
                selection,      // selection options
                selection[0]);  // initial value
            if (which >= 0 && selection[which].equals("Yes")) {
                statusMessage.info("Updating " + configFile);

                // generate the configuration file from the current settings
                String currcontent = generateConfigFile();
                writeConfigFile (currcontent, configFile);

                // save the setup file for next startup startup
                setPropertiesItem(this.properties, "LastConfigFile", configFile);
                return;
            }
        }
        
        // if config info has never been saved to a file, choose the default
        // file in the current dir as the location to save it.
        if (bConfigUpdate != true) {
            configFile = defaultConfig;
            bConfigUpdate = true;

            // create the config file if it doesn't exist, or overwrite it if it does
            statusMessage.info("Saving Configuration file: " + configFile);
            extractGUIConfigSettings(configFile);
            writeConfigFile (generateConfigFile(), configFile);

            // save the setup file as the default on startup
            setPropertiesItem(this.properties, "LastConfigFile", configFile);
            return;
        }
        
        // else, config file has been previously saved.
        // if file has changed, go ahead and update it
        if (bModified) {
            statusMessage.info("Saving Configuration file: " + configFile);
            extractGUIConfigSettings(configFile);
            writeConfigFile (generateConfigFile(), configFile);
        }
    }//GEN-LAST:event_formWindowClosing

    private void prjpathTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prjpathTextFieldFocusLost
        JTextField textField = (JTextField) evt.getSource();
        String prjpath = textField.getText();
        if (prjpath != null) {
            // update the path
            updateNewProjectPath (prjpath);
        }
    }//GEN-LAST:event_prjpathTextFieldFocusLost
    
    private void helpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpButtonActionPerformed
        // search for the README file in janalyzer
        File readmeFile = new File(System.getProperty("user.dir") + "/README");
        if (!readmeFile.isFile())
            return;
        
        // create the frame to hold all the helpful tabs
        String projname = this.prjnameTextField.getText();
        javax.swing.JFrame helpFrame = new javax.swing.JFrame();
        helpFrame.setLocationRelativeTo(null);
        helpFrame.setSize(new java.awt.Dimension(700, 500));
        javax.swing.JTabbedPane helpTab = new javax.swing.JTabbedPane();
        helpFrame.add(helpTab);
        if (projname != null && !projname.isEmpty())
            helpFrame.setTitle("Help: " + projname);
        else
            helpFrame.setTitle("Help");

        // get the other text files to add
        File dirFile = new File(this.prjpathTextField.getText() + "/description.txt");
        File qpath = new File(this.prjpathTextField.getText() + "/questions");

        // add the tabs
        addTextTab (helpTab, "README", readmeFile);
        addTextTab (helpTab, "Description.txt", dirFile);
        if (qpath.isDirectory()) {
            File[] qlist = qpath.listFiles();
            if (qlist != null) {
                for (File qFile : qlist) {
                    addTextTab (helpTab, qFile.getName(), qFile);
                }
            }
        }

        // now turn on the lights
        helpFrame.setVisible(true);
    }//GEN-LAST:event_helpButtonActionPerformed

    private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
        // in the event something was changed in the config file that affects
        // this gui, update the gui from the config file
//        setGUIConfigSettings(configInfo);
    }//GEN-LAST:event_formWindowGainedFocus

    private void runButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runButtonActionPerformed
        statusMessage.init();

        // disable this button so user can't start another instance
        this.runButton.setEnabled(false);
        setVisible(false);

        // run a seperate thread to begin the program generation
        launchProgramGen();
    }//GEN-LAST:event_runButtonActionPerformed

    private void aveCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aveCheckBoxActionPerformed
        setJanalyzerPaths();

        // update the application and library selections
        if (this.aveCheckBox.isSelected()) {
            this.appList.setModel(configInfo.getList(ConfigInfo.ListType.applAverroes));
            this.libList.setModel(configInfo.getList(ConfigInfo.ListType.libAverroes));
            // clear the Used APIs field
            savedApiTextField = this.apiTextField.getText();
            this.apiTextField.setText("");
        }
        else {
            this.appList.setModel(configInfo.getList(ConfigInfo.ListType.application));
            this.libList.setModel(configInfo.getList(ConfigInfo.ListType.libraries));
            // set the Used APIs field to the value from configInfo
            this.apiTextField.setText(savedApiTextField);
        }
    }//GEN-LAST:event_aveCheckBoxActionPerformed

    private void apiTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_apiTextFieldActionPerformed
        // update the config file to allow the user to clear this field out
        String field = apiTextField.getText();
        configInfo.setField(ConfigInfo.StringType.usedapis, field);
        statusMessage.info("apiTextField = " + field);
    }//GEN-LAST:event_apiTextFieldActionPerformed

    private void entryTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_entryTextFieldActionPerformed
        // update the config file to allow the user to clear this field out
        String field = entryTextField.getText();
        configInfo.setField(ConfigInfo.StringType.addentries, field);
        statusMessage.info("entryTextField = " + field);
    }//GEN-LAST:event_entryTextFieldActionPerformed

    private void jrepathOpenButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jrepathOpenButtonActionPerformed
        // this filechooser is setup to allow file and directory selections so that
        // the user can look for a directory with jar files in search for the
        // required rt.jar file.
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Jar Files","jar");
        this.jrePathChooser.setFileFilter(filter);
        this.jrePathChooser.setMultiSelectionEnabled(false);
        String currentPath = this.jrePathTextField.getText();
        File initPath = new File(currentPath);
        if (initPath.isDirectory())
            this.jrePathChooser.setCurrentDirectory(initPath);
        int retVal = this.jrePathChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            // get the selected path - then check to see if the user selected a file or dir
            File file = this.jrePathChooser.getSelectedFile();
            String path = file.getAbsolutePath();
            this.jrePathTextField.setText(path);
            if (file.isFile()) {
                // file selected - just use the path
                path = file.getParent();
                this.jrePathTextField.setText(path);
            }

            // save the path selection
            jrePathName = path;
            
            // update the properties file
            setPropertiesItem (this.properties, "JrePath", path);

            // now check to see if required file is present
            String rtjarFile = path + "/rt.jar";
            File rtjar = new File(rtjarFile);
            if (!rtjar.isFile()) {
                JOptionPane.showMessageDialog(null,
                    "Selected path is missing the required file: rt.jar",
                    "Missing jar file", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jrepathOpenButtonActionPerformed

    private void prjnameTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prjnameTextFieldFocusLost
        // verify the data is valid
        String prjname = this.prjnameTextField.getText();
        if (prjname.length() != findEndOfField(prjname)) {
            // nope - let's restore the previous value
            this.prjnameTextField.setText(configInfo.getField(ConfigInfo.StringType.projectname));
            // and notify user of the format of the project name entry
            JOptionPane.showMessageDialog(null,
                "Project Name must consist only of alphanumeric characters" + newLine +
                "and the underscore (_) character." + newLine + newLine +
                "Please rename using valid characters only.",
                "Invalid characters in Project Name", JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            // update the config file
            configInfo.setField(ConfigInfo.StringType.projectname, prjname);
        }
    }//GEN-LAST:event_prjnameTextFieldFocusLost

    private void jrePathTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jrePathTextFieldActionPerformed
        // determine if entry is valid
        String path = jrePathTextField.getText();
        if (path != null) {
            String rtjarFile = path + "/rt.jar";
            File rtjar = new File(rtjarFile);
            if (rtjar.isFile()) {
                // update setting in properties file
                setPropertiesItem(null, "JrePath", path);
            }
            else {
                statusMessage.error("Invalid entry for jrePathTextField: " + path);
            }
        }

        // update the display (in case we rejected the user's request)
        jrePathName = getPropertiesItem (this.properties, "JrePath");
        jrePathTextField.setText(jrePathName);
    }//GEN-LAST:event_jrePathTextFieldActionPerformed

    private void apiTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_apiTextFieldFocusLost
        // update the config file to allow the user to clear this field out
        String field = apiTextField.getText();
        configInfo.setField(ConfigInfo.StringType.usedapis, field);
        statusMessage.info("apiTextField = " + field);
    }//GEN-LAST:event_apiTextFieldFocusLost

    private void entryTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_entryTextFieldFocusLost
        // update the config file to allow the user to clear this field out
        String field = entryTextField.getText();
        configInfo.setField(ConfigInfo.StringType.addentries, field);
        statusMessage.info("entryTextField = " + field);
    }//GEN-LAST:event_entryTextFieldFocusLost

    private void clearAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearAllButtonActionPerformed
        clearAllFileFields();
    }//GEN-LAST:event_clearAllButtonActionPerformed

    private void audioFeedbackCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audioFeedbackCheckBoxActionPerformed
        String audiofback = configInfo.getField(ConfigInfo.StringType.audiofback);
        audiofback = ("yes".equals(audiofback)) ? "no" : "yes";
        configInfo.setField(ConfigInfo.StringType.audiofback, audiofback);
    }//GEN-LAST:event_audioFeedbackCheckBoxActionPerformed

    private void publicMethodsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_publicMethodsCheckBoxActionPerformed
        String pubmeths = configInfo.getField(ConfigInfo.StringType.pubmeths);
        pubmeths = ("yes".equals(pubmeths)) ? "no" : "yes";
        configInfo.setField(ConfigInfo.StringType.pubmeths, pubmeths);

        // disable/enable the entry selection based on this checkbox value
        if (pubmeths.equals("yes")) {
            this.entryTextField.setEnabled(false);
            this.entryOpenButton.setEnabled(false);
        }
        else {
            this.entryTextField.setEnabled(true);
            this.entryOpenButton.setEnabled(true);
        }
    }//GEN-LAST:event_publicMethodsCheckBoxActionPerformed

    private void prjpathTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prjpathTextFieldActionPerformed
        JTextField textField = (JTextField) evt.getSource();
        String prjpath = textField.getText();
        if (prjpath != null) {
            // update the path
            updateNewProjectPath (prjpath);
        }
    }//GEN-LAST:event_prjpathTextFieldActionPerformed

    private void jrePathTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jrePathTextFieldFocusLost
        // determine if entry is valid
        String path = jrePathTextField.getText();
        if (path != null) {
            String rtjarFile = path + "/rt.jar";
            File rtjar = new File(rtjarFile);
            if (rtjar.isFile()) {
                // update setting in properties file
                setPropertiesItem(null, "JrePath", path);
            }
            else {
                statusMessage.error("Invalid entry for jrePathTextField: " + path);
            }
        }

        // update the display (in case we rejected the user's request)
        jrePathName = getPropertiesItem (this.properties, "JrePath");
        jrePathTextField.setText(jrePathName);
    }//GEN-LAST:event_jrePathTextFieldFocusLost

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
     * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
     */
    try {
      for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          javax.swing.UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (ClassNotFoundException ex) {
      java.util.logging.Logger.getLogger(AnalyzerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      java.util.logging.Logger.getLogger(AnalyzerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      java.util.logging.Logger.getLogger(AnalyzerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (javax.swing.UnsupportedLookAndFeelException ex) {
      java.util.logging.Logger.getLogger(AnalyzerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AnalyzerFrame().setVisible(true);
            }
        });
    }

    private void stopButtonActionPerformed (java.awt.event.ActionEvent evt) {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
    
    // this keeps track of when the thread finishes so it can make changes to the GUI
    class TimerListener implements ActionListener {
    	boolean done = false;
        
        @Override
        public void actionPerformed(ActionEvent event) {
            // update the elapsed time
            ++elapsedSecs;
            Integer secs = elapsedSecs % 60;
            Integer mins = elapsedSecs / 60;
            String timestamp = ((mins < 10) ? "0" : "") + mins.toString() + ":" +
                               ((secs < 10) ? "0" : "") + secs.toString();
            elapsedTimeLabel.setText(timestamp);

            // check if thread has completed
            if (!done && programGen.exitcode >= 0) {
                System.out.println("CFG construction time: " + timestamp);
                statusMessage.info("generateProgram returned: " + programGen.exitcode);
                
                // program generation was successful
                if (programGen.exitcode == 0) {
                    try {
                        // create the main frame panel and activate it
                        MainFrame mainFrame = new MainFrame(AnalyzerFrame.configInfo);
                        mainFrame.setVisible(true);
                        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                        mainFrame.setTitle(configInfo.getField(ConfigInfo.StringType.projectname));
                        
                        // disable the temp message
                        loadingFrame.setVisible(false);
                        
                        // stop this timer
                        timer.stop();
                        done = true;
                    } catch (InvalidClassFileException ex) {
                        Logger.getLogger(AnalyzerFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else {
                    // error occurred. clear the thread so we can try again.
                    genThread = null;
                }
            }
        }
    }
    
    // this generates the Program info in a separate thread
    public class RunProgramGen implements Runnable {

        private int exitcode = -1;

        public RunProgramGen() {
        }

        @Override
        public void run() {
            exitcode = generateProgram();
        }
        
        public int getExitcode () {
            return exitcode;
        }
    }


    // the configuration data
    private static ConfigInfo configInfo;

    // current proj path loaded (less the project name entry)
    private static String  myProjPath;

    // the following are static copies from the widget controls to allow external 
    // operations to modify their behavior.
    private static JButton button_averroesButton;
    private static JButton button_decompilerButton;

    private static StatusMessage statusMessage;
    private static String  jrePathName;        // the JRE path

    // the Janalyzer configuration properties
    private final Properties properties;
    //private final PropertiesFile propfile;
    
    // these are swing components that are created dynamically and used by the
    // AnalyzerFrame class and the TimerListener class.
    private JFrame loadingFrame;
    private JLabel elapsedTimeLabel;

    private RunProgramGen programGen;   // class for running generateProgram() in a thread
    private Thread  genThread;          // thread in which generateProgram() runs
    private final Timer   timer;        // timer for checking on status of generateProgram()
    private int     elapsedSecs;        // the elapsed time
    private String  configFile;         // the file we are associating with the current configuration
    private String  savedApiTextField;  // holds the GUI value when Averroes blanks the display
    private boolean bConfigUpdate;      // true if we are to update the config file upon closing janalyzer
        
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton apiOpenButton;
    private javax.swing.JTextField apiTextField;
    private javax.swing.JButton appAddButton;
    private javax.swing.JButton appClearButton;
    private javax.swing.JList appList;
    private javax.swing.JButton appRemoveButton;
    private javax.swing.JScrollPane appScrollPane;
    private javax.swing.JCheckBox audioFeedbackCheckBox;
    private javax.swing.JCheckBox aveCheckBox;
    private javax.swing.JButton averroesRunButton;
    private javax.swing.JButton clearAllButton;
    private javax.swing.JTextField cmdStatusTextField;
    private javax.swing.JFileChooser configFileChooser;
    private javax.swing.JButton decompilerRunButton;
    private javax.swing.JFileChooser entryChooser;
    private javax.swing.JButton entryOpenButton;
    private javax.swing.JTextField entryTextField;
    private javax.swing.JCheckBox exceptCheckBox;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JFileChooser folderChooser;
    private javax.swing.JButton helpButton;
    private javax.swing.JCheckBox infCheckBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTabbedPane janTabbedPane;
    private javax.swing.JFileChooser jrePathChooser;
    private javax.swing.JTextField jrePathTextField;
    private javax.swing.JLabel jrepathLabel;
    private javax.swing.JButton jrepathOpenButton;
    private javax.swing.JButton libAddButton;
    private javax.swing.JButton libClearButton;
    private javax.swing.JList libList;
    private javax.swing.JScrollPane libMissPane;
    private javax.swing.JTextArea libMissTextArea;
    private javax.swing.JButton libRemoveButton;
    private javax.swing.JScrollPane libScrollPane;
    private javax.swing.JButton loadSetupButton;
    private javax.swing.JComboBox optComboBox;
    private javax.swing.JLabel optLabel;
    private javax.swing.JPanel pathSelectionsPanel;
    private javax.swing.JLabel prjnameLabel;
    private javax.swing.JTextField prjnameTextField;
    private javax.swing.JLabel prjpathLabel;
    private javax.swing.JTextField prjpathTextField;
    private javax.swing.JButton projpathOpenButton;
    private javax.swing.JCheckBox publicMethodsCheckBox;
    private javax.swing.JFileChooser repoChooser;
    private javax.swing.JButton runButton;
    private javax.swing.JButton saveSetupButton;
    private javax.swing.JPanel selectionPanel;
    private javax.swing.JLabel setupLabel1;
    private javax.swing.JLabel setupLabel2;
    private javax.swing.JLabel setupLabel3;
    private javax.swing.JLabel setupLabel4;
    private javax.swing.JPanel setupPanel;
    private javax.swing.JPanel toolButtonPanel;
    private javax.swing.JPanel toolPanel;
    // End of variables declaration//GEN-END:variables
}
