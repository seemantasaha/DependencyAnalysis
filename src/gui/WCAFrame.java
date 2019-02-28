/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import cloudinterface.CloudInterface;
import cloudinterface.CloudInterface.CloudFileType;
import cloudinterface.WorkerStatistics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import core.Procedure;
import core.Program;
import drivergen.ConfigGenerator;
import drivergen.DriverGenerator;
import drivergen.GenerationException;
import drivergen.Options;
import drivergen.Options.Parameter;
import drivergen.Util;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

/**
 *
 * Class for the SPF Analyzer GUI Frame
 * @author dmcd2356
 */
public class WCAFrame extends javax.swing.JFrame {

    final static String JPF_CONFIG_PATH = System.getProperty("user.home") + "/.jpf/site.properties";
	
    public WCAFrame(ConfigInfo cfgInfo) {
    	
        initComponents();
        this.projectName = "";

        // start initialization
        startup(cfgInfo);

        // add listeners to the Test and Parameter Setup comboBoxes
        enableTestSetupListeners();
        enableParameterSetupListeners();
    }
    
    public WCAFrame(String initClass, String initMethod, ConfigInfo cfgInfo) {
    	
        initComponents();

        // set project name from the method selection
        String driverName = initMethod;
        int offset = driverName.indexOf('(');
        if (offset >= 0)
            driverName = driverName.substring(0, offset);
        if (!driverName.isEmpty())
            this.projectName = driverName;

        // start initialization
        startup(cfgInfo);

        // select the initial Test class and method
        initTestMethod (initClass, initMethod);
        
        // add listeners to the Test and Parameter Setup comboBoxes
        enableTestSetupListeners();
        enableParameterSetupListeners();
    }
    
    public WCAFrame(String setupFile, ConfigInfo cfgInfo) {
    	
        initComponents();
        this.projectName = "";

        // start initialization
        startup(cfgInfo);

        // parse the file and load settings from it
        loadSettingsFile (setupFile);

        // add listeners to the Test and Parameter Setup comboBoxes
        enableTestSetupListeners();
        enableParameterSetupListeners();
    }

    /**
     * converts the DebugType to the corresponding String key value for DebugMessage.print.
     * Note that ErrorExit and EntryExit are not listed because they are turned into
     * the other types when sending to print.
     * 
     * @param type - the DebugType value to convert
     * @return the corresponding string entry
     */
    private String getDebugTypeString (DebugType type) {
        switch (type) {
            default :
            case Normal :   return "DEBUG";
            case Error :    return "ERROR";
            case Warning :  return "WARN ";
            case Success :  return "PASS ";
            case Event :    return "EVENT";
            case Details :  return "DEBUG";
            case Argument : return "ARGS ";
            case Options :  return "OPTS ";
            case Autoset :  return "AUTO ";
            case Entry :    return "ENTRY";
            case Exit :     return "EXIT ";
        }
    }
    
    /**
     * sets up the DebugMessage instance with the color selections to use.
     * 
     * @param handler - the DebugMessage instance to apply it to
     */
    private void setDebugColorScheme (DebugMessage handler) {
        handler.setTypeColor (getDebugTypeString(DebugType.Normal),    gui.Util.TextColor.Black,  gui.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Error),     gui.Util.TextColor.Red,    gui.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Warning),   gui.Util.TextColor.DkRed,  gui.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Success),   gui.Util.TextColor.Green,  gui.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Event),     gui.Util.TextColor.Gold,   gui.Util.FontType.BoldItalic);
        handler.setTypeColor (getDebugTypeString(DebugType.Details),   gui.Util.TextColor.DkGrey, gui.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Argument),  gui.Util.TextColor.Blue,   gui.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Options),   gui.Util.TextColor.Violet, gui.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Autoset),   gui.Util.TextColor.Green,  gui.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Entry),     gui.Util.TextColor.Brown,  gui.Util.FontType.Bold);
        handler.setTypeColor (getDebugTypeString(DebugType.Exit),      gui.Util.TextColor.Brown,  gui.Util.FontType.Bold);
    }
    
    private void startup (ConfigInfo cfgInfo) {
        // save config info
        configInfo = cfgInfo;

        // create debug and status message handlers
        debugmsg = new DebugMessage(this.debugTextPane);
        debugmsg.enableTime(true);
        debugmsg.enableType(true);
        setDebugColorScheme(debugmsg);
        statusmsg = new DebugMessage(this.statusTextPane);
        statusmsg.enableTime(true);
        setDebugColorScheme(statusmsg);

        // create the Text Panes for the Driver and Config file displays
        driverTextPane = null;
        configTextPane = null;
        driverPanelSetup(DriverType.Driver, null);
        driverPanelSetup(DriverType.Config, null);

        // save original stdout and stderr for restoring after we redirect to output window
        standardOut = System.out;
        standardErr = System.err;         
        
        argListTest = new ArrayList<>();
        argOffset = 0;
        spfMode = Options.ConfigType.WCA;
        lastSpfMode = spfMode;
        lastOptions = null;
        debugEntryLevel = 0;
        cloudJobList = new ArrayList<>();
        cloudJobs = new ArrayList<>();
        bFlag_overwrite = false;
        bFlag_Created = false;
        bFlag_Modified = false;
        bFlag_Compiled = false;
        bPauseCloudUpdate = false;
        bCloudFilterUpdate = false;
        cloudRowSelection = -1;
        cloudJobsCount = 0;
        cloudColSortSelection = 5;  // init the cloud job sort column to the startTime
        bCloudSortOrder = true;     // init the cloud job sort dir to descending

        // let's make the Save button noticable so when you've modified a file, you'll know
        saveButton.setBackground(Color.yellow);
        
        // set the default range limits to the min and max limit of that data type
        wcaSymLimits = new SymTypes();
        scSymLimits = new SymTypes();

        // set default values for parameter selections
        ArgSelect defaultArgs = new ArgSelect();
        setGUIParamSetup_FromArg(defaultArgs);
        
        // init selections for running multiple passes
        setGUI_spfSetRange (wcaPolicyRangeCheckBox, wcaPolicyEndSpinner, 0);
        setGUI_spfSetRange (wcaHistoryRangeCheckBox, wcaHistoryEndSpinner, 0);
        
        // cost models
        costModels = new HashMap<>();
        costModels.put("Depth (default)","wcanalysis.heuristic.model.DepthState$DepthStateBuilder");
        costModels.put("Instruction count","wcanalysis.heuristic.model.InstructionCountState$InstructionCountStateBuilder");
        costModels.put("Heap - alloc","wcanalysis.heuristic.model.HeapAllocState$HeapAllocStateBuilder");
        costModels.put("Heap - live","wcanalysis.heuristic.model.MaxLiveHeapState$MaxLiveHeapStateBuilder");
        costModels.put("Stack","wcanalysis.heuristic.model.StackSizeState$StackSizeStateBuilder");
        for (String k : costModels.keySet()) {
            this.wcaCostModelComboBox.addItem(k);
            if (k.contains("default")) {
                this.wcaCostModelComboBox.setSelectedItem(k);
            }
        }

        // disable the debug panel if not enabled
        if (configInfo.getField(ConfigInfo.StringType.debugger).equals("0")) {
            this.viewerTabbedPane.remove(debugScrollPane);
        }
        
        // setup the initial symbol selections
        String dtype = "int";
        SymLimits limit;
        this.wcaSymTypeComboBox.setSelectedItem(dtype);
        limit = wcaSymLimits.getLimits(dtype);
        this.wcaSymMinTextField.setText(limit.min);
        this.wcaSymMaxTextField.setText(limit.max);
        this.scSymTypeComboBox.setSelectedItem(dtype);
        limit = scSymLimits.getLimits(dtype);
        this.scSymMinTextField.setText(limit.min);
        this.scSymMaxTextField.setText(limit.max);
        
        // initialize the project base path and name from the passed arguments
        String prjpath = configInfo.getField(ConfigInfo.StringType.projectpath);
        if (!prjpath.isEmpty()) {
            this.prjpathTextField.setText(prjpath);
            this.wcaCreateButton.setEnabled(true); // enable Create if this is valid
            this.scCreateButton.setEnabled(true); // enable Create if this is valid
        }

        // set default cloud name selection to the project name
        String prjname = configInfo.getField(ConfigInfo.StringType.projectname);
        this.cloudNameTextField.setText(prjname);

        // if driver name wasn't set to test method, set it to the project name from config
        if (this.projectName == null || this.projectName.isEmpty()) {
            this.projectName = prjname;
        }
        if (!this.projectName.isEmpty()) {
            this.prjnameTextField.setText(this.projectName);
        }
    
        // collect all the classes used in this application (including library classes)
        // to support iterative method in WCA driver generation
        CallGraph cg = Program.getCallGraph();
        Iterator<CGNode> cgNodeIter = cg.iterator();
        while (cgNodeIter.hasNext()) {
          CGNode cgNode = cgNodeIter.next();
          IMethod mth = cgNode.getMethod();
          String clsName = mth.getDeclaringClass().getName().toString();
          String mthName = mth.getSelector().toString();
          if (clsName.equals("Laverroes/Library"))
            continue;
          
          Map<String, Set<String>> clsMap = 
              mth.getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application) ? 
              this.applicationClassMap : this.libraryClassMap;
          Set<String> mthNameSet = clsMap.get(clsName);
          if (mthNameSet == null) {
            mthNameSet = new TreeSet<>();
            clsMap.put(clsName, mthNameSet);
          }
          mthNameSet.add(mthName);
        }
     
        // add the application classes to the Test and Iterative class selections
        for (Map.Entry<String, Set<String>> chMapEnt : this.applicationClassMap.entrySet()) {
            this.class1ComboBox.addItem(chMapEnt.getKey());
            this.class2ComboBox.addItem(chMapEnt.getKey());
        }
        
        // add the library classes to the Iterative class selections only
        for (Map.Entry<String, Set<String>> chMapEnt : this.libraryClassMap.entrySet())
            this.class1ComboBox.addItem(chMapEnt.getKey());

        // by default, select the 1st entry in the class comboboxes
        this.class2ComboBox.setSelectedIndex(0);
        this.class1ComboBox.setSelectedIndex(0);
        
        // if a bin directory does not exist in the project directory, create one
        String binpathname = getSPFBasePath() + "bin/";
        File binpath = new File(binpathname);
        if (!binpath.exists()) {
            debugDisplayMessage(DebugType.Normal, "bin dir not found - creating one");
            binpath.mkdirs();
        }

        // this creates a command launcher on a separate thread
        threadLauncher = new ThreadLauncher(outputTextArea);
        
        // this creates a command launcher that runs from the current thread
        commandLauncher = new CommandLauncher();
        
        // add a mouse listener for allowing use of cut/copy/paste
        outputTextArea.addMouseListener(new ContextMenuMouseListener());
        debugTextPane.addMouseListener(new ContextMenuMouseListener());

        // init the cloud controls
        initCloud();
        
//        DebugMessage.testColors();
        statusDisplayMessage(DebugType.Exit, "Initialization complete");
    }

    private void initTestMethod (String initClass, String initMethod) {
        // if a Test class was passed, find it in the list and make it the current selection
        // for both classes.
        if (initClass != null && !initClass.isEmpty()) {
            this.class2ComboBox.setSelectedItem(initClass);
            this.class1ComboBox.setSelectedItem(initClass); // init Iterative class to same as Test
        }

        // generate the method lists for both method selections
        generateTestMethodList();
        generateIterMethodList((String)this.class1ComboBox.getSelectedItem(), null);

        // if a Test method was passed, find it in the list and make it the current selection.
        if (initMethod != null && !initMethod.isEmpty())
            this.method2ComboBox.setSelectedItem(initMethod);

        // initialize the method argument data type selections
        updateTestMethodArguments();
    }
    
    private void initCloud () {
        // align columns in cloud jobs table to center
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment( SwingConstants.CENTER );
        cloudJobsTable.setDefaultRenderer(String.class, centerRenderer);
        TableCellRenderer headerRenderer = cloudJobsTable.getTableHeader().getDefaultRenderer();
        JLabel headerLabel = (JLabel) headerRenderer;
        headerLabel.setHorizontalAlignment( SwingConstants.CENTER );
        
        // create a timer for updating the cloud job information
// TODO: for now, disable the old cloud job update listener
        cloundListener = new CloudUpdateListener();
        cloudTimer = new Timer(2000, cloundListener);
        cloudTimer.start();

        // do the same for the MCTS job information
        // (we will start the timer when the Submit button is pressed)
        mctsListener = new MCTSUpdateListener();
        mctsTimer = new Timer(1000, mctsListener);
        mctsGraphResults = new ArrayList();

        // create up & down key handlers for cloud row selection
        AbstractAction tableUpArrowAction = new UpArrowAction();
        AbstractAction tableDnArrowAction = new DnArrowAction();
        cloudJobsTable.getInputMap().put(KeyStroke.getKeyStroke("UP"), "upAction");
        cloudJobsTable.getActionMap().put( "upAction", tableUpArrowAction );
        cloudJobsTable.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "dnAction");
        cloudJobsTable.getActionMap().put( "dnAction", tableDnArrowAction );
        
        // add a mouse listener for the cloud table header selection
        JTableHeader cloudTableHeader = cloudJobsTable.getTableHeader();
        cloudTableHeader.addMouseListener(new CloudJobsHeaderMouseListener());
    }
    
    /**
     * This performs the actions to take upon completion of the thread command.
     */
    private class StandardTermination implements ThreadLauncher.ThreadAction {

        @Override
        public void allcompleted(ThreadLauncher.ThreadInfo threadInfo) {
            // disable the stop button
            stopButton.setEnabled(false);
           		
            // restore the stdout and stderr
            System.out.flush();
            System.err.flush();
            System.setOut(standardOut);
            System.setErr(standardErr);
            debugDisplayMessage (DebugType.Exit, "StandardTermination");
        }

        @Override
        public void jobprestart(ThreadLauncher.ThreadInfo threadInfo) {
            outputTextArea.append(threadInfo.jobname + ": " + threadInfo.fname + newLine);
        }

        @Override
        public void jobstarted(ThreadLauncher.ThreadInfo threadInfo) {
            statusDisplayMessage(DebugType.Details, "Job " + threadInfo.jobid + " PID = " + threadInfo.pid);
        }
        
        @Override
        public void jobfinished(ThreadLauncher.ThreadInfo threadInfo) {
            if (threadInfo.exitcode == 0)
                statusDisplayMessage(DebugType.Success, "Thread job <" + threadInfo.jobname
                    + "> file <" + threadInfo.fname+ "> completed successfully");
            else
                statusDisplayMessage(DebugType.Error, "Thread job <" + threadInfo.jobname
                    + "> file <" + threadInfo.fname+ "> failed with exitcode " + threadInfo.exitcode);
        }
    }

    /**
     * this is called to create the Text panes for the driver files and to
     * set the contents, title and text color. There is a problem with using
     * setText() to modify the panel contents (it throws a null pointer exception)
     * so this is used to simply replace the old pane with a new pane having
     * the new text contents.
     * 
     * @param type - type of driver file
     * @param contents  - text contents to place in the pane
     */
    private void driverPanelSetup (DriverType type, String contents) {
        // determine the title to place in the pane
        String title = type.toString() + " File";
        if (contents != null && !contents.isEmpty()) {
            if (type == DriverType.Config) {
                Integer count = getSpfIterations(WCAFrame.spfMode);
                int id = (count <= 1) ? 0 : 1;
                title = Util.getConfigFileName(projectName, id);
            }
            else {
                title = Util.getDriverFileName(projectName);
            }
        }
        
        // create the text pane
        JTextPane textPane = new javax.swing.JTextPane();
        textPane.setBorder(javax.swing.BorderFactory.createTitledBorder(null,
                            title,
                            javax.swing.border.TitledBorder.LEFT,
                            javax.swing.border.TitledBorder.DEFAULT_POSITION));

        // add a mouse listener for allowing use of cut/copy/paste
        textPane.addMouseListener(new ContextMenuMouseListener());
        
        // add the contents
        textPane.setForeground(Color.black);
        textPane.setText(contents);
        textPane.setCaretPosition(0); // set position to start of file

        // specify the document listener
        textPane.getDocument().addDocumentListener(new FileDocumentListener(type));
        
        // save the text pane and add it to its corresponding scroll pane
        if (type == DriverType.Config) {
            configTextPane = textPane;
            if (configTextPane != null)
                configScrollPane.remove(configTextPane);
            configScrollPane.setViewportView(textPane);
        }
        else {
            driverTextPane = textPane;
            if (driverTextPane != null)
                driverScrollPane.remove(driverTextPane);
            driverScrollPane.setViewportView(textPane);
        }
    }

    /**
     * this is called to update the title of the text pane.
     * 
     * @param type - the type of driver file
     * @param title - the new title
     */
    private void driverPanelSetTitle (DriverType type, String title) {
        TitledBorder border;
        if (type == DriverType.Config) {
            border = (TitledBorder)configTextPane.getBorder();
        }
        else {
            border = (TitledBorder)driverTextPane.getBorder();
        }

        border.setTitle(title);
    }
        
    /**
     * this is called to update the color of the text in the pane.
     * 
     * @param type - the type of driver file
     * @param bChanged - true if text has been changed
     */
    private void driverPanelShowTextChanged (DriverType type, boolean bChanged) {
        Color color = bChanged ? Color.blue : Color.black;
        if (type == DriverType.Config)
            configTextPane.setForeground(color);
        else
            driverTextPane.setForeground(color);
    }

    private class UpArrowAction extends AbstractAction {

        @Override
        public void actionPerformed (ActionEvent evt) {
            String itemName = "UpArrowAction";
            debugDisplayEvent (DebugEvent.ActionPerformed, itemName, "old row = " + cloudRowSelection);
            JTable table = (JTable) evt.getSource();

            // if selection is not at the min, decrement the row selection
            if (cloudRowSelection > 0) {
                --cloudRowSelection;

                // highlight the new selection
                table.setRowSelectionInterval(cloudRowSelection, cloudRowSelection);

                // now scroll if necessary to make selection visible
                Rectangle rect = new Rectangle(table.getCellRect(cloudRowSelection, 0, true));
                table.scrollRectToVisible(rect);
            }
        }
    }
    
    private class DnArrowAction extends AbstractAction {

        @Override
        public void actionPerformed (ActionEvent evt) {
            String itemName = "DnArrowAction";
            debugDisplayEvent (DebugEvent.ActionPerformed, itemName, "old row = " + cloudRowSelection);
            JTable table = (JTable) evt.getSource();
            int maxrow = table.getRowCount();

            // if selection is valid & not at the max, increment the row selection
            if (cloudRowSelection >= 0 && cloudRowSelection < maxrow - 1) {
                ++cloudRowSelection;

                // highlight the new selection
                table.setRowSelectionInterval(cloudRowSelection, cloudRowSelection);

                // now scroll if necessary to make selection visible
                Rectangle rect = new Rectangle(table.getCellRect(cloudRowSelection, 0, true));
                table.scrollRectToVisible(rect);
            }
        }
    }
    
    private class MCTSUpdateListener implements ActionListener{

        /**
         * handles the action to perform when the specified event occurs.
         * @param e - event that occurred
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            // exit if data not initialized
            if (mctsGraphResults == null || mctsChart == null || currentMCTSJobId.isEmpty()) {
                return;
            }
            
            int tabix = viewerTabbedPane.getSelectedIndex();
            String tabtitle = viewerTabbedPane.getTitleAt(tabix);
            ++mctsCounter;
            int mctsValid = 0;
            boolean bChanged = false;

            // if Cloud panel is showing and we have delayed an initial 6 sec, update the panel
            if ("MCTS".equals(tabtitle) && mctsCounter > 6) {
//                debugDisplayMessage (DebugType.Normal, "MCTS updating job: " + currentMCTSJobId);

                // reset the local array list used for graph selections 2&3
                ArrayList<Long> localGraphResults = new ArrayList();
                
                // get the resultant gson file from the cloud
                CloudInterface cloud = new CloudInterface(debugmsg);
                String result = cloud.getMCTSResult(currentMCTSJobId, "outputs", "results.json");
                java.lang.reflect.Type type = new TypeToken<HashMap<String, WorkerStatistics>>(){}.getType();
                HashMap<String, WorkerStatistics> map = (new Gson()).fromJson(result, type);
                if (map != null) {
                    // read each of the values retruned and look for the best reward
                    for (Map.Entry<String, WorkerStatistics> it : map.entrySet()) {
                        String key = it.getKey();
                        WorkerStatistics stats = it.getValue();
            
                        String index = key.substring(key.lastIndexOf("_") + 1);
            
                        // ignore any hung worker results (will report -1)
//                        debugDisplayMessage (DebugType.Normal, "MCTS Best Reward: path " + index + ", reward = " + stats.bestReward);
                        if (stats.bestReward >= 0) {
//                            int bestRewardPathIx = Integer.parseInt(index, 2);
//                            mctsChart.update(bestRewardPathIx, stats.bestReward, 0, bFlag_Compiled);
                            localGraphResults.add(stats.bestReward);
                            if (mctsBestReward < stats.bestReward) {
                                mctsBestReward = stats.bestReward;
                                mctsBestRewardPath = index;
                                bChanged = true;
                            }
                            ++mctsValid;
                        }
                    }

                    // determine which kind of graph we are plotting
                    if (mctsGraphRadioButton1.isSelected()) {
                        // cumulative maximum best reward over time
                        if (mctsValid > 0) {
                            mctsGraphResults.add(mctsBestReward);
                            for (int ix = 0; ix < mctsGraphResults.size(); ix++) {
                                mctsChart.update(ix, mctsGraphResults.get(ix), 0, bFlag_Compiled);
                            }
                        }
                    }
                    else if (!localGraphResults.isEmpty()) {
                        // for types 2 & 3, sort the best reward data and graph the results
                        Collections.sort(localGraphResults);
                        for (int ix = 0; ix < localGraphResults.size(); ix++) {
                            // for type 3, don't plot the 0 entries
                            if (mctsGraphRadioButton2.isSelected() || localGraphResults.get(ix) > 0)
                                mctsChart.update(ix, localGraphResults.get(ix), 0, bFlag_Compiled);
                        }
                    }
                    mctsCountLabel.setText("" + (mctsCounter-6));
                    mctsValidLabel.setText("" + mctsValid);
                    
                    mctsChart.flush();
                    if (bChanged) {
                        debugDisplayMessage (DebugType.Normal, "MCTS Best Reward: path " + mctsBestRewardPath + ", reward = " + mctsBestReward);
                    }
                }
            }
        }
    }

    private class CloudUpdateListener implements ActionListener{

        /**
         * handles the action to perform when the specified event occurs.
         * @param e - event that occurred
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            // if Cloud panel is showing, update the panel
            int tabix = viewerTabbedPane.getSelectedIndex();
            String tabtitle = viewerTabbedPane.getTitleAt(tabix);
            if ("Cloud Jobs".equals(tabtitle) && !bPauseCloudUpdate) {

                // request the cloud jobs list from the interface
                CloudInterface cloud = new CloudInterface(debugmsg);
                CloudInterface.Job[] jobs = cloud.getJobs(); 
                if (jobs == null) {
                    statusDisplayMessage (DebugType.Error, "Cloud getJobs failure");
                    return;
                }
                
                int lastJobsCount = cloudJobsCount;
                cloudJobsCount = (Integer) jobs.length;
                jobsTotalTextField.setText((cloudJobsCount).toString());
                
                // clear flag indicating no new jobs reported and no change has been made to filters
                // to reduce unnecessary debug messages
                // (can't simply skip this section or it won't update status
                // changes in current entries, which might also affect the sort)
                boolean bShowStatus = bCloudFilterUpdate;
                if (lastJobsCount != cloudJobsCount)
                    bShowStatus = true;
                
                bCloudFilterUpdate = false;
                if (lastJobsCount != cloudJobsCount)
                    debugDisplayMessage (DebugType.Normal, "Cloud jobs found: " + cloudJobsCount);
//                statusDisplayMessage (DebugType.Normal, "Cloud jobs found: " + cloudJobsCount);
                
                // re-direct stdout and stderr to the text window
                PrintStream printStream = new PrintStream(new RedirectOutputStream(outputTextArea)); 
                System.setOut(printStream);
                System.setErr(printStream);

                // rummage through the cloud jobs list to create an ArrayList
                // of displayable job entries
                Integer count = 0;
                cloudJobList.clear();
                for (CloudInterface.Job job : jobs) {
                    
                    // this should not happen unless something is very, very wrong!
                    if (job == null)
                        continue;
                    
                    // find the entry in the list of jobs started by this Janalyzer session
                    CloudJob cloudJob = getCloudJob (job.id);
                    String session_id = (cloudJob == null) ? "---" : cloudJob.run.toString();
                    
                    // filter out the User if selected
                    if (userFilterCheckBox.isSelected() && !userFilterTextField.getText().isEmpty()) {
                        if (!job.user.equals(userFilterTextField.getText()))
                            continue;
                    }
                    
                    // filter out the Job Name if selected
                    if (nameFilterCheckBox.isSelected() && !nameFilterTextField.getText().isEmpty()) {
                        if (!job.name.startsWith(nameFilterTextField.getText()))
                            continue;
                    }

                    // filter out jobs not started in this Janalyzer session if selected
                    if (sessionOnlyCheckBox.isSelected() && cloudJob == null) {
                        continue;
                    }
                    
                    // filter out the Start Time if selected
                    if (earliestTimeCheckBox.isSelected()) {
                        SpinnerDateModel dateModel = (SpinnerDateModel)earliestTimeSpinner.getModel();
                        Long dateValue = ((Date)dateModel.getValue()).getTime();
                        // eliminate any entries that were prior to the specified earliest time
                        if (job.startedTime < dateValue)
                            continue;
                        if (latestTimeCheckBox.isSelected()) {
                            dateModel = (SpinnerDateModel)latestTimeSpinner.getModel();
                            dateValue = ((Date)dateModel.getValue()).getTime();
                            // eliminate any entries that were after the specified latest time
                            if (job.startedTime > dateValue)
                                continue;
                        }
                    }
                    
                    // format the date display
                    String datestr = new SimpleDateFormat("YYYY-MM-dd  HH:mm:ss").format(new Date(job.startedTime));
                    String elapsed = formatElapsedTime(job.finishedTime - job.startedTime);

                    // create the entry to add to the table
                    CloudTableInfo tableEntry = new CloudTableInfo();
                    tableEntry.run       = session_id;
                    tableEntry.jobname   = job.name;
                    tableEntry.jobid     = job.id;
                    tableEntry.user      = job.user;
                    tableEntry.status    = (job.state.equals("FINISHED")) ? job.finalStatus : job.state;
                    tableEntry.startTime = datestr;
                    tableEntry.elapsed   = elapsed;
                    cloudJobList.add(tableEntry); // this keeps a list of the displayed table

                    ++count;
                    
                    // if one of the jobs started in this Janalyzer session has completed
                    // and hasn't been downloaded, indicate it is ready.
                    if (cloudJob != null) {
                        if (job.state.equals("FINISHED") && cloudJob.download == DownloadState.None && !cloudJob.bInformed) {
                            cloudJob.bInformed = true;
                            if (job.finalStatus.equals("SUCCEEDED"))
                                statusDisplayMessage(DebugType.Normal, "Cloud job <"  + job.id + "> has completed: " + job.finalStatus);
                            else
                                statusDisplayMessage(DebugType.Error, "Cloud job <"  + job.id + "> has completed: " + job.finalStatus);
                        }
                    }
                }
                    
                // sort the table entries based on current selections
                cloudJobsSortAndDisplay(bShowStatus);
                
                // re-mark the selected row (if any)
                if (cloudRowSelection >= 0)
                    cloudJobsTable.setRowSelectionInterval(cloudRowSelection, cloudRowSelection);
                    
                // show the number of filtered entries
                jobsFilteredTextField.setText(count.toString());

                // restore stdout and stderr
                System.setOut(standardOut);
                System.setErr(standardErr);
            }
        }
    }    

    /**
     * This performs a sort on the cloudJobList and updates the table display.
     * The sort based on the current criteria of:
     * 'cloudColSortSelection' which specifies the column on which to sort and
     * 'bCloudSortOrder' which specifies either ascending (false) or descending (true) order.
     * 
     * @param bShowDebug - true if display debug message showing status
     */
    private void cloudJobsSortAndDisplay (boolean bShowDebug) {
        // determine the sort criteria
        String colname = cloudJobsTable.getColumnName(cloudColSortSelection);
        String dir = bCloudSortOrder ? "1" : "0";
        if (bShowDebug)
            debugDisplayMessage(DebugType.Normal, "Cloud job sorting dir (" + dir+ ") : " + colname);
        
        // sort the table entries
        Collections.sort(cloudJobList, new Comparator<CloudTableInfo>() {
            @Override
            public int compare(CloudTableInfo job1, CloudTableInfo job2)
            {
                String object1, object2;
                switch(cloudColSortSelection) {
                    case 0: object1 = job1.run;        object2 = job2.run;        break;
                    case 1: object1 = job1.jobname;    object2 = job2.jobname;    break;
                    case 2: object1 = job1.jobid;      object2 = job2.jobid;      break;
                    case 3: object1 = job1.user;       object2 = job2.user;       break;
                    case 4: object1 = job1.status;     object2 = job2.status;     break;
                    default: // fall through...
                    case 5: object1 = job1.startTime;  object2 = job2.startTime;  break;
                    case 6: object1 = job1.elapsed;    object2 = job2.elapsed;    break;
                }
                if (!bCloudSortOrder)
                    return  object1.compareTo(object2);
                else
                    return  object2.compareTo(object1);
            }
        });

        // clear out the table entries
        DefaultTableModel model = (DefaultTableModel) cloudJobsTable.getModel();
        model.setRowCount(0); // this clears all the entries from the table

        // reset the names of the columns and modify the currently selected one
        String[] columnNames = new String [] {
            "Run", "Job Name", "Job Number", "User", "Status", "Start Time", "Elapsed"
        };
        for (int ix = 0; ix < model.getColumnCount(); ix++) {
            if (ix == cloudColSortSelection) {
                if (bCloudSortOrder)
                    columnNames[ix] += " " + "\u2193".toCharArray()[0]; // DOWN arrow
                else
                    columnNames[ix] += " " + "\u2191".toCharArray()[0]; // UP arrow
            }
        }
        model.setColumnIdentifiers(columnNames);
        
        // now copy the entries to the displayed table
        for (int ix = 0; ix < cloudJobList.size(); ix++) {
            CloudTableInfo tableEntry = cloudJobList.get(ix);
            model.addRow(new Object[]{
                                tableEntry.run,
                                tableEntry.jobname,
                                tableEntry.jobid,
                                tableEntry.user,
                                tableEntry.status,
                                tableEntry.startTime,
                                tableEntry.elapsed
                                });
        }

        // auto-resize column width
        resizeColumnWidth(cloudJobsTable);
    }
    
    private CloudJob getCloudJob (String jobid) {
        for (Iterator<CloudJob> it = cloudJobs.iterator(); it.hasNext();) {
            CloudJob cloudJob = it.next();
            if (jobid != null && jobid.equals(cloudJob.jobid)) {
                return cloudJob;
            }
        }

        return null;
    }
    
    private CloudTableInfo getCloudTableInfo (int row) {
        CloudTableInfo tableEntry = new CloudTableInfo();
        tableEntry.run       = (String)cloudJobsTable.getValueAt(row, 0);
        tableEntry.jobname   = (String)cloudJobsTable.getValueAt(row, 1);
        tableEntry.jobid     = (String)cloudJobsTable.getValueAt(row, 2);
        tableEntry.user      = (String)cloudJobsTable.getValueAt(row, 3);
        tableEntry.status    = (String)cloudJobsTable.getValueAt(row, 4);
        tableEntry.startTime = (String)cloudJobsTable.getValueAt(row, 5);
        tableEntry.elapsed   = (String)cloudJobsTable.getValueAt(row, 6);
        return tableEntry;
    }
    
    private String gatherCloudResults (String jobid, String jobname, CloudInterface.CloudFileType ftype) {
        debugDisplayMessage (DebugType.Entry, "gatherCloudResults");
        String content = null;
        String pathname = getSPFBasePath() + "downloads/" + jobid + "/" + ftype.toString() + "/";

        // get the result files for the job
        CloudInterface cloud = new CloudInterface(debugmsg);
        CloudInterface.JobOutput[] cloudfiles = cloud.getJobFileList(jobid, ftype);
        
        // check for failure
        if (cloudfiles == null || cloudfiles.length == 0) {
            statusDisplayMessage(DebugType.Error, "Cloud getJobFileList failure (" + ftype + ") for job <" + jobid + ">");
        }
        else {
            // first check if current dir exists & delete if so
            File resultdir = new File(pathname);
            if (resultdir.isDirectory()) {
                debugDisplayMessage (DebugType.Normal, "deleting dir: " + pathname);
                try {
                    FileUtils.deleteDirectory(resultdir);
                } catch (IOException ex) {
                    debugDisplayMessage (DebugType.Warning, ex + "deleting dir: " + pathname);
                }
            }
            
            // indicate the cloud info has been requested
            CloudJob cloudJob = getCloudJob (jobid);
            if (cloudJob != null)
                cloudJob.download = DownloadState.Requested;
    
            // now create the dir for files
            debugDisplayMessage (DebugType.Normal, "creating dir: " + pathname);
            resultdir.mkdirs();

            if (ftype == CloudInterface.CloudFileType.inputs) {
                // for each input file listed...
                for (CloudInterface.JobOutput inputfile : cloudfiles) {
                    String filename = jobname + ".zip";
                    if (cloudfiles.length > 1)
                        filename = inputfile.pathSuffix;
                    statusDisplayMessage (DebugType.Normal, "Input file job <" + jobid + "> " + inputfile.pathSuffix);

                    // download and save the file (should be a single zip file)
                    ByteArrayOutputStream filedata = cloud.getJobFile(jobid, ftype, inputfile.pathSuffix);
                    saveCloudResults (filedata, pathname + filename);
                }
            }
            else {
                // for output file, create the frame to hold all the tabbed text areas
                JFrame cloudFrame = new JFrame();
                cloudFrame.setLocationRelativeTo(null);
                cloudFrame.setSize(new java.awt.Dimension(900, 500));
                cloudFrame.setTitle(jobid);
                JTabbedPane cloudTab = new JTabbedPane();
                cloudFrame.add(cloudTab);

                // for each output file listed...
                for (CloudInterface.JobOutput outputfile : cloudfiles) {
                    statusDisplayMessage (DebugType.Normal, "Output file job <" + jobid + "> " + outputfile.pathSuffix);
                    String filename = outputfile.pathSuffix;
                    // this is if we want to show .csv files in a table instead of as a text file
                    String fileext = "";
                    int offset = filename.indexOf('.');
                    if (offset >= 0 && offset < filename.length())
                        fileext = filename.substring(offset+1);

                    // download the files and determine if they are the csv type
                    ByteArrayOutputStream filedata = cloud.getJobFile(jobid, ftype, outputfile.pathSuffix);
                    if (filedata != null && filedata.size() > 0)
                        content = filedata.toString();
                    else {
                        content = "** File unable to be retrieved from cloud **";
                        if (cloudJob != null)
                            cloudJob.download = DownloadState.Failure;
                    }
                    
                    // save the file
                    saveCloudResults (filedata, pathname + filename);
                    if (cloudJob != null)
                        cloudJob.download = DownloadState.Received;
                    
                    // create a text area to place the text into
                    javax.swing.JTextArea fileText = new javax.swing.JTextArea(0, 0);
                    fileText.setLineWrap(true);
                    fileText.setWrapStyleWord(true);
                    fileText.setText(content);
                    fileText.setCaretPosition(0); // set position to start of file

                    // place text area in a scrollable panel
                    JScrollPane fileScroll = new JScrollPane(fileText);
                    
                    // place scroll pane into the tabbed pane
                    cloudTab.addTab(filename, fileScroll);
                }
            
                // if no data was downloaded, eliminate the frame
                if (cloudTab.getComponentCount() == 0)
                    cloudFrame.dispose();
                else
                    cloudFrame.setVisible(true);
            }
        }
        
        debugDisplayMessage (DebugType.Exit, "gatherCloudResults");
        return content;
    }
    
    private void saveCloudResults (ByteArrayOutputStream content, String filename) {
        if (content == null || content.size() == 0) {
            statusDisplayMessage(DebugType.Error, "File is empty: " + filename);
        }
        else {
            String shortname = filename.substring(filename.lastIndexOf('/') + 1);
            try {
                OutputStream outputStream = new FileOutputStream(filename);
                outputStream.write(content.toByteArray());            
                outputStream.close();
                statusDisplayMessage(DebugType.Normal, "Cloud job written to file: " + shortname + ", size = " + content.size());
            } catch (IOException ex) {
                statusDisplayMessage(DebugType.Error, "Writing cloud job file: " + filename);
            }
        }
    }

    private void selectFileToDownload (int row) {
        // this allows the user to select the test results to download
        CloudTableInfo tableEntry = getCloudTableInfo (row);
        String resultsmsg = "Job " + tableEntry.jobid + " results download: ";

        // save the row selection
        debugDisplayMessage(DebugType.Normal, "selectFileToDownload: row = " + row);
        cloudRowSelection = row;
                
        switch (tableEntry.status) {
            case "SUCCEEDED":

                // determine what the user wants to do from here.
                String[] selection = new String[] { "Exit", "Get Inputs", "Get Outputs" };
                String title   = "Job " + tableEntry.jobid; //"Download / View Results";
                String message = "Select whether to view the input or output files";
                int which = JOptionPane.showOptionDialog(null, // parent component
                        message,        // message
                        title,          // title
                        JOptionPane.DEFAULT_OPTION, // option type
                        JOptionPane.PLAIN_MESSAGE,  // message type
                        null,           // icon
                        selection,      // selection options
                        selection[0]);  // initial value
                if (which < 0)
                    break;
                switch (selection[which]) {
                    case "Get Outputs":
                        gatherCloudResults(tableEntry.jobid, tableEntry.jobname, CloudFileType.outputs);
                        break;
                    case "Get Inputs":
                        gatherCloudResults(tableEntry.jobid, tableEntry.jobname, CloudFileType.inputs);
                        break;
                    case "Exit": // fallthrough... do nothing
                    default:
                        break;
                }
                break;

            case "FAILED": // fall through...
            case "KILLED":
                statusDisplayMessage(DebugType.Warning, resultsmsg + "no results (failed)");
                break;

            default:
                statusDisplayMessage(DebugType.Warning, resultsmsg + "not completed yet");
                break;
        }
    }

    private class CloudJobsHeaderMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked (MouseEvent evt) {
            String itemName = "cloudJobsHeader";

            // get the selected header column
            int newSelection = cloudJobsTable.columnAtPoint(evt.getPoint());
            if (newSelection >= 0) {
                int oldSelection = cloudColSortSelection;
                cloudColSortSelection = newSelection;
                String colname = cloudJobsTable.getColumnName(newSelection);
                debugDisplayEvent (DebugEvent.MouseClicked, itemName, "(" + evt.getX() + "," + evt.getY() + ") -> col " + newSelection + " = " + colname);

                // invert the order selection if the same column is specified
                if (oldSelection == newSelection)
                    bCloudSortOrder = !bCloudSortOrder;
                    
                // sort the table entries based on current selections
                cloudJobsSortAndDisplay(true);
            }
            else {
                debugDisplayEvent (DebugEvent.MouseClicked, itemName, "(" + evt.getX() + "," + evt.getY() + ") -> col " + newSelection);
            }
        }
    }
    
    /**
     * This is used to resize the colums of a table to accomodate some columns
     * requiring more width than others.
     * 
     * @param table - the table to resize
     */
    private void resizeColumnWidth (JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 15; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width +1 , width);
            }
            if(width > 300) // Max width
                width=300;
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    /**
     * converts an time value (in msec) to a String representation.
     * The format of the String is: "HHH:MM:SS"
     * 
     * @param time_ms - the time in msec
     * @return a String of the formatted time
     */
    private String formatElapsedTime (long time_ms) {
        // make sure we have a valid value
        if (time_ms < 0)
            time_ms = 0;
        
        // split value into hours, min and secs
        Long msecs = time_ms % 1000;
        Long secs = (time_ms / 1000);
        secs += msecs >= 500 ? 1 : 0;
        Long hours = secs / 3600;
        secs %= 3600;
        Long mins = secs / 60;
        secs %= 60;

        // now stringify it
        String elapsed = "";
        if (hours < 10)       elapsed = "00";
        else if (hours < 100) elapsed = "0";
        elapsed += hours.toString();
        elapsed += ":";
        elapsed += (mins < 10) ? "0" + mins.toString() : mins.toString();
        elapsed += ":";
        elapsed += (secs < 10) ? "0" + secs.toString() : secs.toString();
        return elapsed;
    }

    /**
     * enables all the Test Setup control listeners so the controls will function.
     */
    private void enableTestSetupListeners () {
        debugDisplayMessage(DebugType.EntryExit, "enableTestSetupListeners");
        listener_TestClass     = new Class2ChangedListener();
        listener_TestMethod    = new Method2ChangedListener();

        this.class2ComboBox.addActionListener(listener_TestClass);
        this.method2ComboBox.addActionListener(listener_TestMethod);
    }

    /**
     * disables all the Test Setup control listeners so the controls will not
     * work until re-enabled.
     * 
     * This can be used when handling a control that
     * may change one of these controls, which would cause the listener action
     * to run prior to the current control completing. It should always be
     * followed by the enable function to re-enable the listener functionality
     * when the control action completes.
     */
    private void disableTestSetupListeners () {
        debugDisplayMessage(DebugType.EntryExit, "disableTestSetupListeners");
        this.class2ComboBox.removeActionListener(listener_TestClass);
        this.method2ComboBox.removeActionListener(listener_TestMethod);
    }
    
    /**
     * enables all the Parameter Setup control listeners so the controls will function.
     */
    private void enableParameterSetupListeners () {
        debugDisplayMessage(DebugType.EntryExit, "enableParameterSetupListeners");
        listener_ParamClass     = new Class1ChangedListener();
        listener_ParamMethod    = new Method1ChangedListener();
        listener_ParamValue     = new ValueSelectChangedListener();
        listener_ParamSize      = new SizeSelectChangedListener();
        listener_ParamElement   = new ElementTypeChangedListener();
        listener_ParamPrimitive = new PrimitiveTypeChangedListener();
        listener_ParamArraySize = new ArraySizeChangedListener();

        this.class1ComboBox.addActionListener(listener_ParamClass);
        this.method1ComboBox.addActionListener(listener_ParamMethod);
        this.valueSelectComboBox.addActionListener(listener_ParamValue);
        this.sizeSelectComboBox.addActionListener(listener_ParamSize);
        this.elementTypeComboBox.addActionListener(listener_ParamElement);
        this.primitiveTypeComboBox.addActionListener(listener_ParamPrimitive);
        this.arraySizeComboBox.addActionListener(listener_ParamArraySize);
    }
    
    /**
     * disables all the Parameter Setup control listeners so the controls will not
     * work until re-enabled.
     * 
     * This can be used when handling a control that
     * may change one of these controls, which would cause the listener action
     * to run prior to the current control completing. It should always be
     * followed by the enable function to re-enable the listener functionality
     * when the control action completes.
     */
    private void disableParameterSetupListeners () {
        debugDisplayMessage(DebugType.EntryExit, "disableParameterSetupListeners");
        this.class1ComboBox.removeActionListener(listener_ParamClass);
        this.method1ComboBox.removeActionListener(listener_ParamMethod);
        this.valueSelectComboBox.removeActionListener(listener_ParamValue);
        this.sizeSelectComboBox.removeActionListener(listener_ParamSize);
        this.elementTypeComboBox.removeActionListener(listener_ParamElement);
        this.primitiveTypeComboBox.removeActionListener(listener_ParamPrimitive);
        this.arraySizeComboBox.removeActionListener(listener_ParamArraySize);
    }
    
    /**
     * This document listener function is used to check if the user has edited
     * the Driver or Config file so that the Save button can be enabled to
     * notify that there is an outstanding change that has not been saved.
     */
    private class FileDocumentListener implements DocumentListener {

        DriverType driverType;
        FileDocumentListener (DriverType type) {
            driverType = type;
        }
        
        @Override
        public void insertUpdate(DocumentEvent e) {
            // TODO: it would be nice to be able to only do these if the file
            //  does not match the editable text area.
            if (bFlag_Created) {
                // enable the save button if user changes text on a created driver.
                saveButton.setEnabled(true);
                undoButton.setEnabled(true);

                // change text color to let user know there is a change
//                driverPanelShowTextChanged (driverType, true);
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            // TODO: it would be nice to be able to only do these if the file
            //  does not match the editable text area.
            if (bFlag_Created) {
                // enable the save button if user changes text on a created driver.
                saveButton.setEnabled(true);
                undoButton.setEnabled(true);

                // change text color to let user know there is a change
//                driverPanelShowTextChanged (driverType, true);
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            //Plain text components do not fire these events
        }
    }
    
    /**
     * This does some checks for invalid conditions on the parameter setup radioButton
     * selection and makes the corresponding selection.
     * 
     * @param itemName  - comboBox item name
     * @param mode      - the parameter mode selection made
     */
    private void argRadioButtonActionPerformed (String itemName, ArgSelect.ParamMode mode) {
        DebugEvent event = DebugEvent.ActionPerformed;
        if (argListTest.size() <= 0) {
            debugDisplayEvent (event, itemName, "argList empty");
            return;
        }
        if (argOffset >= argListTest.size()) {
            debugDisplayEvent (event, itemName, "argOffset " + argOffset + " >= size " + argListTest.size());
            return;
        }

        debugDisplayEvent (event, itemName, "argOffset " + argOffset);
        setGUIParamSetup_Mode(mode);

        // update the argList from the GUI (this will set the mode selection in ArgListTest)
        copyGUIToArgList();

        // since the Parameter method may have not been setup, we need to
        // re-generate the method list and find the preferred method (if any)
        disableParameterSetupListeners();
        updateTestMethodParameter(argOffset, itemName);
        enableParameterSetupListeners();
        
        // update the Advanced tab info
        setGUI_ParamInfoPanel(0);
    }

    /**
     * This does some preliminary checks on a change in one of the comboBox
     * items for the parameter setup.
     * 
     * @param evt       - the event that occurred
     * @param itemName  - comboBox item name
     * @param ptype     - true if this is a parameter entry
     * @param nochange  - true if allow running even if there is no change
     * @return the selected item if entry was valid to run
     */
    private String argComboBoxActionCheck (ActionEvent evt, String itemName, boolean ptype, boolean nochange) {
        DebugEvent eventName = DebugEvent.ActionListener;
        JComboBox comboBox = (JComboBox) evt.getSource();
        String command = evt.getActionCommand();        // comboBoxChanged
        Object selected = comboBox.getSelectedItem();   // the new selection
        if (selected == null) {
            debugDisplayEvent (eventName, itemName, "null selection");
            return null;
        }
           
        if (ptype) {
            if (command.equals("comboBoxChanged")) {
                if (argListTest.size() <= 0) {
                    debugDisplayEvent (eventName, itemName, "argList empty");
                    return null;
                }
                if (argOffset >= argListTest.size()) {
                    debugDisplayEvent (eventName, itemName, "argOffset " + argOffset + " >= size " + argListTest.size());
                    return null;
                }
            }

            String currentSel;
            ArgSelect argSelect = argListTest.get(argOffset);
            switch (itemName) {
                case "valueSelectComboBox":   currentSel = argSelect.value;     break;
                case "sizeSelectComboBox":    currentSel = argSelect.size;      break;
                case "elementTypeComboBox":   currentSel = argSelect.element;   break;
                case "primitiveTypeComboBox": currentSel = argSelect.primitive; break;
                case "arraySizeComboBox":     currentSel = argSelect.arraySize; break;
                case "class1ComboBox":        currentSel = argSelect.cls;       break;
                case "method1ComboBox":       currentSel = argSelect.method;    break;
                default:
                    debugDisplayEvent (eventName, itemName, "invalid item");
                    return null;
            }

            // now check if the selected value is different from the current setting
            if (selected.toString().equals(currentSel)) {
                debugDisplayEvent (eventName, itemName, "no change");
                if (nochange)
                    return selected.toString();
                else
                    return null;
            }
        }

        // yes, we have a change
        debugDisplayEvent (eventName, itemName, "(selected: " + selected + ")");

        // if it's a Parameter Setup type, update arglist from selection
        if (ptype) {
            copyGUIToArgList();
        }
        
        return selected.toString();
    }

    private class ValueSelectChangedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            argComboBoxActionCheck(evt, "valueSelectComboBox", true, false);
        }
    }
        
    private class SizeSelectChangedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            argComboBoxActionCheck(evt, "sizeSelectComboBox", true, false);
        }
    }
        
    private class ElementTypeChangedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            argComboBoxActionCheck(evt, "elementTypeComboBox", true, false);
        }
    }
        
    private class PrimitiveTypeChangedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            argComboBoxActionCheck(evt, "primitiveTypeComboBox", true, false);
        }
    }
        
    private class ArraySizeChangedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            argComboBoxActionCheck(evt, "arraySizeComboBox", true, false);
        }
    }
        
    private class Class1ChangedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            // check if we have a valid change in the parameter
            String itemName = "class1ComboBox";
            String selected = argComboBoxActionCheck(evt, itemName, true, true);
            if (selected != null) {
                disableParameterSetupListeners();

                // generate method selection list for parameter from the new class
                // and then update the corresonding arglist entry for it.
                String paramMethod = generateIterMethodList(selected, null);
                ArgSelect entry = argListTest.get(argOffset);
                if (entry != null) {
                    entry.method = paramMethod;
                }
                
                // update the argument selection
                updateTestMethodParameter(argOffset, itemName);

                // update the Advanced tab info
                setGUI_ParamInfoPanel(0);

                enableParameterSetupListeners();
            }
        }
    }

    private class Method1ChangedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            // check if we have a valid change in the parameter
            String itemName = "method1ComboBox";
            String selected = argComboBoxActionCheck(evt, itemName, true, false);
            if (selected != null) {
                disableParameterSetupListeners();

                // update the argument selection
                updateTestMethodParameter(argOffset, itemName);

                // update the Advanced tab info
                setGUI_ParamInfoPanel(0);

                enableParameterSetupListeners();
            }
        }
    }

    private class Class2ChangedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            // check if we have a valid change in the parameter
            String selected = argComboBoxActionCheck(evt, "class2ComboBox", false, true);
            if (selected != null) {
                // disable the Test and Parameter listeners to prevent them from
                // running during these changes that will cause many of them to
                // run before all the parameters have been setup.
                disableTestSetupListeners();
                disableParameterSetupListeners();
                
                // generate list for Test method
                generateTestMethodList();

                // update the argument selections (this will also generate the IterClassList
                updateTestMethodArguments();

                // update the Advanced tab info
                setGUI_ParamInfoPanel(0);

                enableTestSetupListeners();
                enableParameterSetupListeners();
            }
        }
    }

    private class Method2ChangedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            String selected = argComboBoxActionCheck(evt, "method2ComboBox", false, true);
            if (selected != null) {
                // disable the Test and Parameter listeners to prevent them from
                // running during these changes that will cause many of them to
                // run before all the parameters have been setup.
                disableTestSetupListeners();
                disableParameterSetupListeners();
                
                // update data type selection
                updateTestMethodArguments();

                enableTestSetupListeners();
                enableParameterSetupListeners();
                
                // set the driver name to the method name
                String driverName = (String)method2ComboBox.getSelectedItem();
                if (driverName != null && !driverName.isEmpty()) {
                    int offset = driverName.indexOf('(');
                    if (offset >= 0)
                        driverName = driverName.substring(0, offset);
                    if (!driverName.isEmpty()) {
                        projectName = driverName;
                        prjnameTextField.setText(projectName);
                    }
                }
            }
        }
    }

    /**
     * this updates the janalyzer config file with the specified parameter
     * 
     * @param tag - the tag value defined by the config file in AnalyzerFrame
     * @param value  - the value to set the parameter to
     */
    private void updateConfigFileParam (String tag, String value) {
        // update the config file
        try {
            debugDisplayMessage(DebugType.Normal, "updating configuration file: " + tag + " = " + value);
            int retcode = AnalyzerFrame.updateConfigFile (tag, value);
            if (retcode != 0)
                debugDisplayMessage(DebugType.Error, "Invalid tag for configuration file: " + tag);
        } catch (IOException ex) {
            debugDisplayMessage(DebugType.Error, "Failure writing to configuration file");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        folderChooser = new javax.swing.JFileChooser();
        jpfZipFileChooser = new javax.swing.JFileChooser();
        spfExecutePanel = new javax.swing.JPanel();
        saveButton = new javax.swing.JButton();
        launchButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        wcaCreateButton = new javax.swing.JButton();
        prjnameTextField = new javax.swing.JTextField();
        prjnameLabel = new javax.swing.JLabel();
        prjpathTextField = new javax.swing.JTextField();
        prjpathButton = new javax.swing.JButton();
        prjpathLabel = new javax.swing.JLabel();
        scCreateButton = new javax.swing.JButton();
        useCloudCheckBox = new javax.swing.JCheckBox();
        buildButton = new javax.swing.JButton();
        undoButton = new javax.swing.JButton();
        statusLabel = new javax.swing.JLabel();
        mainSplitPane = new javax.swing.JSplitPane();
        setupPanel = new javax.swing.JPanel();
        methodPanel = new javax.swing.JPanel();
        testMethodPanel = new javax.swing.JPanel();
        class2ComboBox = new javax.swing.JComboBox();
        method2ComboBox = new javax.swing.JComboBox();
        class2Label = new javax.swing.JLabel();
        method2Label = new javax.swing.JLabel();
        parameterSetupPanel = new javax.swing.JPanel();
        argTypePanel = new javax.swing.JPanel();
        typeDataStructRadioButton = new javax.swing.JRadioButton();
        typeSimpletonRadioButton = new javax.swing.JRadioButton();
        typePrimitiveRadioButton = new javax.swing.JRadioButton();
        typeStringRadioButton = new javax.swing.JRadioButton();
        typeArrayRadioButton = new javax.swing.JRadioButton();
        argSelectPanel = new javax.swing.JPanel();
        argPrevButton = new javax.swing.JButton();
        argNextButton = new javax.swing.JButton();
        argCountLabel = new javax.swing.JLabel();
        argTabbedPane = new javax.swing.JTabbedPane();
        argSetupPanel = new javax.swing.JPanel();
        class1ComboBox = new javax.swing.JComboBox();
        method1ComboBox = new javax.swing.JComboBox();
        valueSelectComboBox = new javax.swing.JComboBox<>();
        sizeSelectComboBox = new javax.swing.JComboBox<>();
        elementTypeComboBox = new javax.swing.JComboBox<>();
        primitiveTypeComboBox = new javax.swing.JComboBox<>();
        arraySizeComboBox = new javax.swing.JComboBox<>();
        sizeSelectLabel = new javax.swing.JLabel();
        valueSelectLabel = new javax.swing.JLabel();
        class1Label = new javax.swing.JLabel();
        method1Label = new javax.swing.JLabel();
        elementTypeLabel = new javax.swing.JLabel();
        primitiveTypeLabel = new javax.swing.JLabel();
        arraySizeLabel = new javax.swing.JLabel();
        argInfoPanel = new javax.swing.JPanel();
        methSelectPanel1 = new javax.swing.JPanel();
        testMethodRadioButton = new javax.swing.JRadioButton();
        iterMethodRadioButton = new javax.swing.JRadioButton();
        methodNameLabel = new javax.swing.JLabel();
        noParamsLabel = new javax.swing.JLabel();
        paramListScrollPane = new javax.swing.JScrollPane();
        paramListTextArea = new javax.swing.JTextArea();
        setupTabbedPane = new javax.swing.JTabbedPane();
        wcaSetupPanel = new javax.swing.JPanel();
        wcaSelectionPanel = new javax.swing.JPanel();
        wcaSolverComboBox = new javax.swing.JComboBox<>();
        wcaBvlenSpinner = new javax.swing.JSpinner();
        wcaInputMaxSpinner = new javax.swing.JSpinner();
        wcaPolicySpinner = new javax.swing.JSpinner();
        wcaHistorySpinner = new javax.swing.JSpinner();
        wcaCostModelComboBox = new javax.swing.JComboBox<>();
        wcaPolicyRangeCheckBox = new javax.swing.JCheckBox();
        wcaHistoryRangeCheckBox = new javax.swing.JCheckBox();
        wcaPolicyEndSpinner = new javax.swing.JSpinner();
        wcaHistoryEndSpinner = new javax.swing.JSpinner();
        wcaBvlenLabel = new javax.swing.JLabel();
        wcaInputMaxLabel = new javax.swing.JLabel();
        wcaSolverLabel = new javax.swing.JLabel();
        wcaCostModelLabel = new javax.swing.JLabel();
        wcaPolicyLabel = new javax.swing.JLabel();
        wcaHistorySizeLabel = new javax.swing.JLabel();
        wcaMultirunLabel = new javax.swing.JLabel();
        wcaOptionPanel = new javax.swing.JPanel();
        wcaHeuristicCheckBox = new javax.swing.JCheckBox();
        wcaDebugCheckBox = new javax.swing.JCheckBox();
        wcaSymbolRangePanel = new javax.swing.JPanel();
        wcaSymTypeComboBox = new javax.swing.JComboBox<>();
        wcaSymMinTextField = new javax.swing.JFormattedTextField();
        wcaSymMaxTextField = new javax.swing.JFormattedTextField();
        wcaSymMaxLabel = new javax.swing.JLabel();
        wcaSymTypeLabel = new javax.swing.JLabel();
        wcaSymMinLabel = new javax.swing.JLabel();
        sideChanSetupPanel = new javax.swing.JPanel();
        scSelectionPanel = new javax.swing.JPanel();
        scSolverComboBox = new javax.swing.JComboBox<>();
        scBvlenSpinner = new javax.swing.JSpinner();
        scTypeComboBox = new javax.swing.JComboBox<>();
        scBvlenLabel = new javax.swing.JLabel();
        scSolverLabel = new javax.swing.JLabel();
        scMultirunLabel = new javax.swing.JLabel();
        scTypeLabel = new javax.swing.JLabel();
        scOptionPanel = new javax.swing.JPanel();
        scDebugCheckBox = new javax.swing.JCheckBox();
        scSymbolRangePanel = new javax.swing.JPanel();
        scSymTypeComboBox = new javax.swing.JComboBox<>();
        scSymMinTextField = new javax.swing.JFormattedTextField();
        scSymMaxTextField = new javax.swing.JFormattedTextField();
        scSymMaxLabel = new javax.swing.JLabel();
        scSymTypeLabel = new javax.swing.JLabel();
        scSymMinLabel = new javax.swing.JLabel();
        cloudPanel = new javax.swing.JPanel();
        cloudSetupPanel = new javax.swing.JPanel();
        cloudNameTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        cloudExecutorsTextField = new javax.swing.JTextField();
        cloudInitialDepthTextField = new javax.swing.JTextField();
        cloudFilterPanel = new javax.swing.JPanel();
        sessionOnlyCheckBox = new javax.swing.JCheckBox();
        sessionOnlyLabel = new javax.swing.JLabel();
        userFilterCheckBox = new javax.swing.JCheckBox();
        userFilterTextField = new javax.swing.JTextField();
        nameFilterCheckBox = new javax.swing.JCheckBox();
        nameFilterTextField = new javax.swing.JTextField();
        earliestTimeCheckBox = new javax.swing.JCheckBox();
        earliestTimeSpinner = new javax.swing.JSpinner();
        latestTimeCheckBox = new javax.swing.JCheckBox();
        latestTimeSpinner = new javax.swing.JSpinner();
        earliestLabel = new javax.swing.JLabel();
        latestLabel = new javax.swing.JLabel();
        cloudSummaryPanel = new javax.swing.JPanel();
        jobsFilteredLabel = new javax.swing.JLabel();
        jobsTotalTextField = new javax.swing.JTextField();
        jobsFilteredTextField = new javax.swing.JTextField();
        jobsTotalLabel = new javax.swing.JLabel();
        viewerTabbedPane = new javax.swing.JTabbedPane();
        driverScrollPane = new javax.swing.JScrollPane();
        configScrollPane = new javax.swing.JScrollPane();
        outputScrollPane = new javax.swing.JScrollPane();
        outputTextArea = new javax.swing.JTextArea();
        statusScrollPane = new javax.swing.JScrollPane();
        statusTextPane = new javax.swing.JTextPane();
        debugScrollPane = new javax.swing.JScrollPane();
        debugTextPane = new javax.swing.JTextPane();
        cloudScrollPane = new javax.swing.JScrollPane();
        cloudJobsTable = new javax.swing.JTable();
        mctsPanel = new javax.swing.JPanel();
        mctsSubmitJobButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        mctsZipFileTextField = new javax.swing.JTextField();
        mctsSelectButton = new javax.swing.JButton();
        mctsStopButton = new javax.swing.JButton();
        mctsJpfFileComboBox = new javax.swing.JComboBox<>();
        mctsCountLabel = new javax.swing.JLabel();
        mctsValidLabel = new javax.swing.JLabel();
        mctsGraphRadioButton1 = new javax.swing.JRadioButton();
        mctsGraphRadioButton2 = new javax.swing.JRadioButton();
        mctsGraphRadioButton3 = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();

        folderChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("SPF Analyzer");
        setMinimumSize(new java.awt.Dimension(1150, 775));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        spfExecutePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        spfExecutePanel.setEnabled(false);
        spfExecutePanel.setMaximumSize(new java.awt.Dimension(32767, 55));
        spfExecutePanel.setMinimumSize(new java.awt.Dimension(1031, 55));
        spfExecutePanel.setPreferredSize(new java.awt.Dimension(1031, 55));

        saveButton.setText("Save");
        saveButton.setToolTipText("Saves the edited Driver and Configuration files");
        saveButton.setEnabled(false);
        saveButton.setMaximumSize(new java.awt.Dimension(110, 25));
        saveButton.setMinimumSize(new java.awt.Dimension(110, 25));
        saveButton.setPreferredSize(new java.awt.Dimension(110, 25));
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        launchButton.setBackground(new java.awt.Color(204, 255, 204));
        launchButton.setText("Launch");
        launchButton.setToolTipText("Compiles the current driver code then Launches the SPF Worst Case Analyzer.");
        launchButton.setMaximumSize(new java.awt.Dimension(110, 25));
        launchButton.setMinimumSize(new java.awt.Dimension(110, 25));
        launchButton.setPreferredSize(new java.awt.Dimension(110, 25));
        launchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                launchButtonActionPerformed(evt);
            }
        });

        stopButton.setBackground(new java.awt.Color(255, 204, 204));
        stopButton.setText("Stop");
        stopButton.setToolTipText("Stops the SPF Worst Case Analysis.");
        stopButton.setEnabled(false);
        stopButton.setMaximumSize(new java.awt.Dimension(110, 25));
        stopButton.setMinimumSize(new java.awt.Dimension(110, 25));
        stopButton.setPreferredSize(new java.awt.Dimension(110, 25));
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        wcaCreateButton.setBackground(new java.awt.Color(204, 255, 204));
        wcaCreateButton.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        wcaCreateButton.setText("Create WCA");
        wcaCreateButton.setToolTipText("Creates the SPF driver and configuration files for Worst Case Analysis.");
        wcaCreateButton.setEnabled(false);
        wcaCreateButton.setMaximumSize(new java.awt.Dimension(110, 25));
        wcaCreateButton.setMinimumSize(new java.awt.Dimension(110, 25));
        wcaCreateButton.setPreferredSize(new java.awt.Dimension(110, 25));
        wcaCreateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wcaCreateButtonActionPerformed(evt);
            }
        });

        prjnameTextField.setMaximumSize(new java.awt.Dimension(160, 28));
        prjnameTextField.setMinimumSize(new java.awt.Dimension(160, 28));
        prjnameTextField.setPreferredSize(new java.awt.Dimension(160, 28));
        prjnameTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                prjnameTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                prjnameTextFieldFocusLost(evt);
            }
        });

        prjnameLabel.setText("Driver Name");

        prjpathTextField.setToolTipText("Specifies the path at which to create the WCA driver code.");
        prjpathTextField.setMaximumSize(new java.awt.Dimension(32767, 28));
        prjpathTextField.setMinimumSize(new java.awt.Dimension(441, 28));
        prjpathTextField.setPreferredSize(new java.awt.Dimension(441, 28));
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

        prjpathButton.setText("Select");
        prjpathButton.setToolTipText("<html>Selects the project base path location.<br>\nThe generated driver code will be placed in the 'drivers' folder at this location..</html>");
        prjpathButton.setMaximumSize(new java.awt.Dimension(110, 25));
        prjpathButton.setMinimumSize(new java.awt.Dimension(110, 25));
        prjpathButton.setPreferredSize(new java.awt.Dimension(110, 25));
        prjpathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prjpathButtonActionPerformed(evt);
            }
        });

        prjpathLabel.setText("Project Path");

        scCreateButton.setBackground(new java.awt.Color(204, 255, 204));
        scCreateButton.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        scCreateButton.setText("Create SChan");
        scCreateButton.setToolTipText("Creates the SPF driver and configuration files for Side Channel Analysis.");
        scCreateButton.setEnabled(false);
        scCreateButton.setMaximumSize(new java.awt.Dimension(110, 25));
        scCreateButton.setMinimumSize(new java.awt.Dimension(110, 25));
        scCreateButton.setPreferredSize(new java.awt.Dimension(110, 25));
        scCreateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scCreateButtonActionPerformed(evt);
            }
        });

        useCloudCheckBox.setText("Use Cloud");
        useCloudCheckBox.setToolTipText("If selected, use the cloud instead of local machine when Launch is pressed.");
        useCloudCheckBox.setEnabled(false);
        useCloudCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        useCloudCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useCloudCheckBoxActionPerformed(evt);
            }
        });

        buildButton.setBackground(new java.awt.Color(204, 255, 204));
        buildButton.setText("Build");
        buildButton.setToolTipText("Compiles the current driver code.");
        buildButton.setMaximumSize(new java.awt.Dimension(110, 25));
        buildButton.setMinimumSize(new java.awt.Dimension(110, 25));
        buildButton.setPreferredSize(new java.awt.Dimension(110, 25));
        buildButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buildButtonActionPerformed(evt);
            }
        });

        undoButton.setText("Undo");
        undoButton.setToolTipText("<html>\nUndo all unsaved changes in the Driver and Configuration files.<br>\nThis simply restores the last saved copy of the file.<br>\nNote that since there is no Redo button, all your unsaved changes will be lost.\n</html>");
        undoButton.setEnabled(false);
        undoButton.setMaximumSize(new java.awt.Dimension(110, 25));
        undoButton.setMinimumSize(new java.awt.Dimension(110, 25));
        undoButton.setPreferredSize(new java.awt.Dimension(110, 25));
        undoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout spfExecutePanelLayout = new javax.swing.GroupLayout(spfExecutePanel);
        spfExecutePanel.setLayout(spfExecutePanelLayout);
        spfExecutePanelLayout.setHorizontalGroup(
            spfExecutePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spfExecutePanelLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(spfExecutePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(spfExecutePanelLayout.createSequentialGroup()
                        .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(spfExecutePanelLayout.createSequentialGroup()
                        .addGroup(spfExecutePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(spfExecutePanelLayout.createSequentialGroup()
                                .addComponent(wcaCreateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(scCreateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(spfExecutePanelLayout.createSequentialGroup()
                                .addComponent(prjnameLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(prjnameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addGroup(spfExecutePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(spfExecutePanelLayout.createSequentialGroup()
                                .addComponent(prjpathLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(prjpathTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(prjpathButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(spfExecutePanelLayout.createSequentialGroup()
                                .addComponent(saveButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(undoButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 48, Short.MAX_VALUE)
                                .addComponent(useCloudCheckBox)
                                .addGap(18, 18, 18)
                                .addComponent(buildButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(28, 28, 28)
                                .addComponent(launchButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(30, 30, 30))))
        );
        spfExecutePanelLayout.setVerticalGroup(
            spfExecutePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spfExecutePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(spfExecutePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(prjnameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prjpathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prjpathButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prjpathLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(prjnameLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(spfExecutePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wcaCreateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(saveButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(launchButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(scCreateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(useCloudCheckBox)
                    .addComponent(buildButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(undoButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        mainSplitPane.setDividerLocation(400);
        mainSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.7);

        setupPanel.setMaximumSize(new java.awt.Dimension(32767, 400));
        setupPanel.setMinimumSize(new java.awt.Dimension(1031, 0));
        setupPanel.setPreferredSize(new java.awt.Dimension(1031, 400));

        methodPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        methodPanel.setMinimumSize(new java.awt.Dimension(630, 350));

        testMethodPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Test Method", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        testMethodPanel.setMinimumSize(new java.awt.Dimension(610, 100));
        testMethodPanel.setPreferredSize(new java.awt.Dimension(610, 100));

        class2ComboBox.setToolTipText("<html>\nSpecifies the Class of the method to be tested.\n</html>");
        class2ComboBox.setMaximumSize(new java.awt.Dimension(32767, 24));
        class2ComboBox.setMinimumSize(new java.awt.Dimension(441, 24));
        class2ComboBox.setPreferredSize(new java.awt.Dimension(500, 24));

        method2ComboBox.setToolTipText("<html>\nSpecifies the Method to be tested.\n</html>");
        method2ComboBox.setMaximumSize(new java.awt.Dimension(32767, 24));
        method2ComboBox.setMinimumSize(new java.awt.Dimension(441, 24));
        method2ComboBox.setPreferredSize(new java.awt.Dimension(500, 24));

        class2Label.setText("Class");

        method2Label.setText("Method");

        javax.swing.GroupLayout testMethodPanelLayout = new javax.swing.GroupLayout(testMethodPanel);
        testMethodPanel.setLayout(testMethodPanelLayout);
        testMethodPanelLayout.setHorizontalGroup(
            testMethodPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(testMethodPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(testMethodPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(class2Label)
                    .addComponent(method2Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(testMethodPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(method2ComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(class2ComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        testMethodPanelLayout.setVerticalGroup(
            testMethodPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(testMethodPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(testMethodPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(class2ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(class2Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(testMethodPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(method2ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(method2Label))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        class2ComboBox.getAccessibleContext().setAccessibleName("class2ComboBox");
        method2ComboBox.getAccessibleContext().setAccessibleName("method2ComboBox");

        parameterSetupPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Test Parameters", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        parameterSetupPanel.setMinimumSize(new java.awt.Dimension(500, 166));
        parameterSetupPanel.setPreferredSize(new java.awt.Dimension(610, 284));

        argTypePanel.setPreferredSize(new java.awt.Dimension(165, 149));

        typeDataStructRadioButton.setSelected(true);
        typeDataStructRadioButton.setText("Data Structure");
        typeDataStructRadioButton.setToolTipText("<html>\nSpecifies the parameter is a structure that will<br>\nbe filled using a specified method.\n</html>");
        typeDataStructRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeDataStructRadioButtonActionPerformed(evt);
            }
        });

        typeSimpletonRadioButton.setText("Simpleton Object");
        typeSimpletonRadioButton.setToolTipText("<html>\nSpecifies the parameter is a structure that will<br>\nbe filled using the 'put' method for its class.\n</html>");
        typeSimpletonRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeSimpletonRadioButtonActionPerformed(evt);
            }
        });

        typePrimitiveRadioButton.setText("Primitive");
        typePrimitiveRadioButton.setToolTipText("<html>\nSpecifies the parameter is a primitive data type\n</html>");
        typePrimitiveRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typePrimitiveRadioButtonActionPerformed(evt);
            }
        });

        typeStringRadioButton.setText("String");
        typeStringRadioButton.setToolTipText("<html>\nSpecifies the parameter is a string.\n</html>");
        typeStringRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeStringRadioButtonActionPerformed(evt);
            }
        });

        typeArrayRadioButton.setText("Array type");
        typeArrayRadioButton.setToolTipText("<html>\nSpecifies the parameter is an array of objects.\n</html>");
        typeArrayRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeArrayRadioButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout argTypePanelLayout = new javax.swing.GroupLayout(argTypePanel);
        argTypePanel.setLayout(argTypePanelLayout);
        argTypePanelLayout.setHorizontalGroup(
            argTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(argTypePanelLayout.createSequentialGroup()
                .addContainerGap(11, Short.MAX_VALUE)
                .addGroup(argTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(typeArrayRadioButton)
                    .addComponent(typeStringRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(typePrimitiveRadioButton)
                    .addComponent(typeSimpletonRadioButton)
                    .addComponent(typeDataStructRadioButton))
                .addContainerGap())
        );
        argTypePanelLayout.setVerticalGroup(
            argTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(argTypePanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(typeDataStructRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(typeSimpletonRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(typePrimitiveRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(typeStringRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(typeArrayRadioButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        typeDataStructRadioButton.getAccessibleContext().setAccessibleName("typeDataStructRadioButton");
        typeSimpletonRadioButton.getAccessibleContext().setAccessibleName("typeSimpletonRadioButton");
        typePrimitiveRadioButton.getAccessibleContext().setAccessibleName("typePrimitiveRadioButton");
        typeStringRadioButton.getAccessibleContext().setAccessibleName("typeStringRadioButton");
        typeArrayRadioButton.getAccessibleContext().setAccessibleName("typeArrayRadioButton");

        argSelectPanel.setMinimumSize(new java.awt.Dimension(158, 74));
        argSelectPanel.setPreferredSize(new java.awt.Dimension(170, 70));

        argPrevButton.setText("<");
        argPrevButton.setToolTipText("<html>\nallows editing of the previous parameter of the test method\n</html>");
        argPrevButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                argPrevButtonActionPerformed(evt);
            }
        });

        argNextButton.setText(">");
        argNextButton.setToolTipText("<html>\nallows editing of the next parameter of the test method\n</html>");
        argNextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                argNextButtonActionPerformed(evt);
            }
        });

        argCountLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        argCountLabel.setText("1 of 1");
        argCountLabel.setMaximumSize(new java.awt.Dimension(106, 15));
        argCountLabel.setMinimumSize(new java.awt.Dimension(106, 15));
        argCountLabel.setPreferredSize(new java.awt.Dimension(106, 15));

        javax.swing.GroupLayout argSelectPanelLayout = new javax.swing.GroupLayout(argSelectPanel);
        argSelectPanel.setLayout(argSelectPanelLayout);
        argSelectPanelLayout.setHorizontalGroup(
            argSelectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(argSelectPanelLayout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(argPrevButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(argNextButton)
                .addContainerGap(43, Short.MAX_VALUE))
            .addGroup(argSelectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(argCountLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE)
                .addContainerGap())
        );
        argSelectPanelLayout.setVerticalGroup(
            argSelectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(argSelectPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(argCountLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(argSelectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(argPrevButton)
                    .addComponent(argNextButton))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        argPrevButton.getAccessibleContext().setAccessibleName("argPrevButton");
        argNextButton.getAccessibleContext().setAccessibleName("argNextButton");

        argTabbedPane.setPreferredSize(new java.awt.Dimension(450, 245));

        argSetupPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        argSetupPanel.setMinimumSize(new java.awt.Dimension(400, 191));
        argSetupPanel.setPreferredSize(new java.awt.Dimension(445, 191));

        class1ComboBox.setToolTipText("<html>\nSelects the Class of the method used to populate the object used by the Test method.\n</html>");
        class1ComboBox.setMaximumSize(new java.awt.Dimension(32767, 24));
        class1ComboBox.setMinimumSize(new java.awt.Dimension(280, 24));
        class1ComboBox.setPreferredSize(new java.awt.Dimension(315, 24));

        method1ComboBox.setToolTipText("<html>\nSelects the method used to populate the object used by the Test method.\n</html>");
        method1ComboBox.setMaximumSize(new java.awt.Dimension(32767, 24));
        method1ComboBox.setMinimumSize(new java.awt.Dimension(280, 24));
        method1ComboBox.setPreferredSize(new java.awt.Dimension(315, 24));

        valueSelectComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "N", "Symbolic", "Concrete" }));
        valueSelectComboBox.setSelectedIndex(1);
        valueSelectComboBox.setToolTipText("<html>\nSpecifies if the element values are to be symbolized or not.\n</html>");
        valueSelectComboBox.setPreferredSize(new java.awt.Dimension(176, 24));

        sizeSelectComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "N", "Symbolic", "Concrete" }));
        sizeSelectComboBox.setToolTipText("<html>\nSpecifies if the element size is to be symbolized or not.\n</html>");

        elementTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "byte", "char", "short", "int", "long", "double", "bool", "String" }));
        elementTypeComboBox.setSelectedIndex(7);
        elementTypeComboBox.setToolTipText("<html>\nSpecifies the type of Array or Data Structure elements.\n</html>");

        primitiveTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "byte", "char", "short", "int", "long", "double" }));
        primitiveTypeComboBox.setSelectedIndex(3);
        primitiveTypeComboBox.setToolTipText("<html>\nSpecifies the type of Primitive elements.\n</html>");

        arraySizeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "N", "Symbolic", "Concrete" }));
        arraySizeComboBox.setToolTipText("<html>\nSpecifies if the Array size is to be symbolized or not.\n</html>");

        sizeSelectLabel.setText("Size");

        valueSelectLabel.setText("Values");

        class1Label.setText("Class");

        method1Label.setText("Method");

        elementTypeLabel.setText("Element type");

        primitiveTypeLabel.setText("Primitive type");

        arraySizeLabel.setText("Array Size");

        javax.swing.GroupLayout argSetupPanelLayout = new javax.swing.GroupLayout(argSetupPanel);
        argSetupPanel.setLayout(argSetupPanelLayout);
        argSetupPanelLayout.setHorizontalGroup(
            argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(argSetupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(class1Label)
                    .addComponent(method1Label)
                    .addComponent(valueSelectLabel)
                    .addComponent(sizeSelectLabel)
                    .addComponent(elementTypeLabel)
                    .addComponent(primitiveTypeLabel)
                    .addComponent(arraySizeLabel))
                .addGap(8, 8, 8)
                .addGroup(argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(argSetupPanelLayout.createSequentialGroup()
                        .addGroup(argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(primitiveTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(elementTypeComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(arraySizeComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(valueSelectComboBox, 0, 176, Short.MAX_VALUE)
                            .addComponent(method1ComboBox, 0, 276, Short.MAX_VALUE)
                            .addComponent(class1ComboBox, 0, 276, Short.MAX_VALUE))
                        .addGap(10, 10, 10))
                    .addGroup(argSetupPanelLayout.createSequentialGroup()
                        .addComponent(sizeSelectComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        argSetupPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {arraySizeComboBox, elementTypeComboBox, primitiveTypeComboBox, valueSelectComboBox});

        argSetupPanelLayout.setVerticalGroup(
            argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(argSetupPanelLayout.createSequentialGroup()
                .addGroup(argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(class1ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(class1Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(method1ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(method1Label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(valueSelectComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueSelectLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sizeSelectLabel)
                    .addComponent(sizeSelectComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(elementTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(elementTypeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(primitiveTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(primitiveTypeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(argSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(arraySizeLabel)
                    .addComponent(arraySizeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        argSetupPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {class1ComboBox, method1ComboBox});

        class1ComboBox.getAccessibleContext().setAccessibleName("class1ComboBox");
        method1ComboBox.getAccessibleContext().setAccessibleName("method1ComboBox");
        valueSelectComboBox.getAccessibleContext().setAccessibleName("valueSelectComboBox");
        sizeSelectComboBox.getAccessibleContext().setAccessibleName("sizeSelectComboBox");
        elementTypeComboBox.getAccessibleContext().setAccessibleName("elementTypeComboBox");
        primitiveTypeComboBox.getAccessibleContext().setAccessibleName("primitiveTypeComboBox");
        arraySizeComboBox.getAccessibleContext().setAccessibleName("arraySizeComboBox");

        argTabbedPane.addTab("Param Setup", argSetupPanel);

        argInfoPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        argInfoPanel.setPreferredSize(new java.awt.Dimension(445, 226));

        methSelectPanel1.setPreferredSize(new java.awt.Dimension(400, 116));

        testMethodRadioButton.setSelected(true);
        testMethodRadioButton.setText("Test Method");
        testMethodRadioButton.setPreferredSize(new java.awt.Dimension(144, 23));
        testMethodRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testMethodRadioButtonActionPerformed(evt);
            }
        });

        iterMethodRadioButton.setText("Iterative Method");
        iterMethodRadioButton.setPreferredSize(new java.awt.Dimension(144, 23));
        iterMethodRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                iterMethodRadioButtonActionPerformed(evt);
            }
        });

        methodNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        methodNameLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        methodNameLabel.setMaximumSize(new java.awt.Dimension(400, 20));
        methodNameLabel.setMinimumSize(new java.awt.Dimension(200, 20));
        methodNameLabel.setPreferredSize(new java.awt.Dimension(400, 20));

        noParamsLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        noParamsLabel.setText("method has no parameters");

        javax.swing.GroupLayout methSelectPanel1Layout = new javax.swing.GroupLayout(methSelectPanel1);
        methSelectPanel1.setLayout(methSelectPanel1Layout);
        methSelectPanel1Layout.setHorizontalGroup(
            methSelectPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(methSelectPanel1Layout.createSequentialGroup()
                .addGap(153, 153, 153)
                .addGroup(methSelectPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(testMethodRadioButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(iterMethodRadioButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(methodNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(noParamsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        methSelectPanel1Layout.setVerticalGroup(
            methSelectPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(methSelectPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(testMethodRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(iterMethodRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(methodNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(noParamsLabel))
        );

        paramListScrollPane.setMaximumSize(new java.awt.Dimension(400, 32767));
        paramListScrollPane.setMinimumSize(new java.awt.Dimension(400, 20));
        paramListScrollPane.setPreferredSize(new java.awt.Dimension(400, 164));

        paramListTextArea.setColumns(20);
        paramListTextArea.setRows(5);
        paramListTextArea.setBorder(null);
        paramListTextArea.setMinimumSize(new java.awt.Dimension(20, 20));
        paramListTextArea.setPreferredSize(new java.awt.Dimension(600, 100));
        paramListScrollPane.setViewportView(paramListTextArea);

        javax.swing.GroupLayout argInfoPanelLayout = new javax.swing.GroupLayout(argInfoPanel);
        argInfoPanel.setLayout(argInfoPanelLayout);
        argInfoPanelLayout.setHorizontalGroup(
            argInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(paramListScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(methSelectPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE)
        );
        argInfoPanelLayout.setVerticalGroup(
            argInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(argInfoPanelLayout.createSequentialGroup()
                .addComponent(methSelectPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(paramListScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        argTabbedPane.addTab("Param Info", argInfoPanel);

        javax.swing.GroupLayout parameterSetupPanelLayout = new javax.swing.GroupLayout(parameterSetupPanel);
        parameterSetupPanel.setLayout(parameterSetupPanelLayout);
        parameterSetupPanelLayout.setHorizontalGroup(
            parameterSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parameterSetupPanelLayout.createSequentialGroup()
                .addGroup(parameterSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(argTypePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(argSelectPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addComponent(argTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        parameterSetupPanelLayout.setVerticalGroup(
            parameterSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parameterSetupPanelLayout.createSequentialGroup()
                .addGroup(parameterSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(parameterSetupPanelLayout.createSequentialGroup()
                        .addComponent(argSelectPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(argTypePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(argTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout methodPanelLayout = new javax.swing.GroupLayout(methodPanel);
        methodPanel.setLayout(methodPanelLayout);
        methodPanelLayout.setHorizontalGroup(
            methodPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(methodPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(methodPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(testMethodPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(parameterSetupPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        methodPanelLayout.setVerticalGroup(
            methodPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(methodPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(testMethodPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(parameterSetupPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setupTabbedPane.setMinimumSize(new java.awt.Dimension(455, 400));
        setupTabbedPane.setPreferredSize(new java.awt.Dimension(455, 400));

        wcaSetupPanel.setMaximumSize(new java.awt.Dimension(455, 250));
        wcaSetupPanel.setMinimumSize(new java.awt.Dimension(455, 250));
        wcaSetupPanel.setPreferredSize(new java.awt.Dimension(455, 400));

        wcaSelectionPanel.setMaximumSize(new java.awt.Dimension(452, 158));
        wcaSelectionPanel.setMinimumSize(new java.awt.Dimension(452, 158));
        wcaSelectionPanel.setPreferredSize(new java.awt.Dimension(452, 200));

        wcaSolverComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "none", "z3", "z3bitvector", "choco", "cvc3" }));
        wcaSolverComboBox.setSelectedIndex(1);
        wcaSolverComboBox.setToolTipText("<html>\nSpecifies the solver to use in the analysis.\n</html>");
        wcaSolverComboBox.setMaximumSize(new java.awt.Dimension(105, 24));
        wcaSolverComboBox.setMinimumSize(new java.awt.Dimension(105, 24));
        wcaSolverComboBox.setPreferredSize(new java.awt.Dimension(105, 24));
        wcaSolverComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wcaSolverComboBoxActionPerformed(evt);
            }
        });

        wcaBvlenSpinner.setModel(new javax.swing.SpinnerNumberModel(32, 1, 9999, 1));
        wcaBvlenSpinner.setToolTipText("<html>\nSpecifies the Bit Vector length to use for z3 solver\n(only valid for z3bitvector solver selection)\n</html>");
        wcaBvlenSpinner.setEnabled(false);
        wcaBvlenSpinner.setMaximumSize(new java.awt.Dimension(105, 20));
        wcaBvlenSpinner.setMinimumSize(new java.awt.Dimension(105, 20));
        wcaBvlenSpinner.setPreferredSize(new java.awt.Dimension(105, 20));
        wcaBvlenSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                wcaBvlenSpinnerStateChanged(evt);
            }
        });

        wcaInputMaxSpinner.setModel(new javax.swing.SpinnerNumberModel(5, 5, 2147483647, 1));
        wcaInputMaxSpinner.setToolTipText("<html>\nSpecifies the maximum value for the WCA test argument N.<br>\nThe larger this value is, the longer the test will take.\n</html>");
        wcaInputMaxSpinner.setMaximumSize(new java.awt.Dimension(105, 20));
        wcaInputMaxSpinner.setMinimumSize(new java.awt.Dimension(105, 20));
        wcaInputMaxSpinner.setPreferredSize(new java.awt.Dimension(105, 20));
        wcaInputMaxSpinner.setRequestFocusEnabled(false);
        wcaInputMaxSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                wcaInputMaxSpinnerStateChanged(evt);
            }
        });

        wcaPolicySpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, 2147483647, 1));
        wcaPolicySpinner.setToolTipText("<html>\nSpecifies the input size at which to find the policy.\n</html>");
        wcaPolicySpinner.setMaximumSize(new java.awt.Dimension(105, 20));
        wcaPolicySpinner.setMinimumSize(new java.awt.Dimension(105, 20));
        wcaPolicySpinner.setPreferredSize(new java.awt.Dimension(105, 20));
        wcaPolicySpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                wcaPolicySpinnerStateChanged(evt);
            }
        });

        wcaHistorySpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 2147483647, 1));
        wcaHistorySpinner.setToolTipText("<html>\nSpecifies the history depth.\n</html>");
        wcaHistorySpinner.setMaximumSize(new java.awt.Dimension(105, 20));
        wcaHistorySpinner.setMinimumSize(new java.awt.Dimension(105, 20));
        wcaHistorySpinner.setPreferredSize(new java.awt.Dimension(105, 20));
        wcaHistorySpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                wcaHistorySpinnerStateChanged(evt);
            }
        });

        wcaCostModelComboBox.setToolTipText("<html>Specifies the type of cost model to use in the Worst Case Analysis:<br>\n<b>Depth</b> - (temporal) looks for the deepest path<br>\n<b>Instruction Count</b> - (temporal) looks for the path with the most instructions consumed<br>\n<b>Heap - alloc</b> - (spatial) looks for the path allocating the most heap<br>\n<b>Heap - live</b> - (spatial) looks for the path with the most active memory usage<br>\n<b>Stack</b> - (spatial) looks for the path using the most stack\n</html>");
        wcaCostModelComboBox.setMinimumSize(new java.awt.Dimension(160, 24));
        wcaCostModelComboBox.setPreferredSize(new java.awt.Dimension(160, 24));
        wcaCostModelComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wcaCostModelComboBoxActionPerformed(evt);
            }
        });

        wcaPolicyRangeCheckBox.setText("enable range");
        wcaPolicyRangeCheckBox.setToolTipText("<html>\nAllows selection of a range of policy values to be run.<br>\nNote that this can cause multiple configuration files to be generated.\n</html>");
        wcaPolicyRangeCheckBox.setMinimumSize(new java.awt.Dimension(25, 23));
        wcaPolicyRangeCheckBox.setPreferredSize(new java.awt.Dimension(83, 23));
        wcaPolicyRangeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wcaPolicyRangeCheckBoxActionPerformed(evt);
            }
        });

        wcaHistoryRangeCheckBox.setText("enable range");
        wcaHistoryRangeCheckBox.setToolTipText("<html>\nAllows selection of a range of history values to be run.<br>\nNote that this can cause multiple configuration files to be generated.\n</html>");
        wcaHistoryRangeCheckBox.setMinimumSize(new java.awt.Dimension(25, 23));
        wcaHistoryRangeCheckBox.setPreferredSize(new java.awt.Dimension(83, 23));
        wcaHistoryRangeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wcaHistoryRangeCheckBoxActionPerformed(evt);
            }
        });

        wcaPolicyEndSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, 2147483647, 1));
        wcaPolicyEndSpinner.setToolTipText("<html>\nSpecifies the upper limit input size at which to find the policy.\n</html>");
        wcaPolicyEndSpinner.setMaximumSize(new java.awt.Dimension(105, 20));
        wcaPolicyEndSpinner.setMinimumSize(new java.awt.Dimension(105, 20));
        wcaPolicyEndSpinner.setPreferredSize(new java.awt.Dimension(105, 20));
        wcaPolicyEndSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                wcaPolicyEndSpinnerStateChanged(evt);
            }
        });

        wcaHistoryEndSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 2147483647, 1));
        wcaHistoryEndSpinner.setToolTipText("<html>\nSpecifies the upper limit history depth.\n</html>");
        wcaHistoryEndSpinner.setMaximumSize(new java.awt.Dimension(105, 20));
        wcaHistoryEndSpinner.setMinimumSize(new java.awt.Dimension(105, 20));
        wcaHistoryEndSpinner.setPreferredSize(new java.awt.Dimension(105, 20));
        wcaHistoryEndSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                wcaHistoryEndSpinnerStateChanged(evt);
            }
        });

        wcaBvlenLabel.setText("BV Length");

        wcaInputMaxLabel.setText("Input Max");

        wcaSolverLabel.setText("Solver");

        wcaCostModelLabel.setText("Cost Model");

        wcaPolicyLabel.setText("Policy");

        wcaHistorySizeLabel.setText("History");

        wcaMultirunLabel.setForeground(new java.awt.Color(51, 51, 255));
        wcaMultirunLabel.setPreferredSize(new java.awt.Dimension(424, 15));

        javax.swing.GroupLayout wcaSelectionPanelLayout = new javax.swing.GroupLayout(wcaSelectionPanel);
        wcaSelectionPanel.setLayout(wcaSelectionPanelLayout);
        wcaSelectionPanelLayout.setHorizontalGroup(
            wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wcaSelectionPanelLayout.createSequentialGroup()
                .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(wcaSelectionPanelLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(wcaSolverLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(wcaBvlenLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(wcaInputMaxLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(wcaPolicyLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(wcaHistorySizeLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(wcaCostModelLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(6, 6, 6)
                        .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(wcaCostModelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(wcaInputMaxSpinner, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(wcaBvlenSpinner, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(wcaSolverComboBox, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(wcaSelectionPanelLayout.createSequentialGroup()
                                .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(wcaPolicySpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(wcaHistorySpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(wcaPolicyRangeCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(wcaHistoryRangeCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(wcaPolicyEndSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(wcaHistoryEndSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, wcaSelectionPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(wcaMultirunLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE)))
                .addGap(9, 9, 9))
        );

        wcaSelectionPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {wcaBvlenSpinner, wcaHistorySpinner, wcaInputMaxSpinner, wcaPolicySpinner});

        wcaSelectionPanelLayout.setVerticalGroup(
            wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wcaSelectionPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wcaSolverLabel)
                    .addComponent(wcaSolverComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wcaBvlenSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(wcaBvlenLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wcaInputMaxSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(wcaInputMaxLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(wcaPolicySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(wcaPolicyLabel))
                    .addComponent(wcaPolicyRangeCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(wcaPolicyEndSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(wcaHistorySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(wcaHistorySizeLabel))
                    .addComponent(wcaHistoryRangeCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(wcaHistoryEndSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(wcaSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wcaCostModelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(wcaCostModelLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(wcaMultirunLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        wcaSelectionPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {wcaCostModelComboBox, wcaSolverComboBox});

        wcaOptionPanel.setPreferredSize(new java.awt.Dimension(452, 60));

        wcaHeuristicCheckBox.setText("Disable solver for heuristics");
        wcaHeuristicCheckBox.setToolTipText("<html> Disables the solver for heuristics to reduce computation time </html>");
        wcaHeuristicCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wcaHeuristicCheckBoxActionPerformed(evt);
            }
        });

        wcaDebugCheckBox.setText("Debug");
        wcaDebugCheckBox.setToolTipText("<html>\nEnables the debugging mode\n</html>");
        wcaDebugCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wcaDebugCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout wcaOptionPanelLayout = new javax.swing.GroupLayout(wcaOptionPanel);
        wcaOptionPanel.setLayout(wcaOptionPanelLayout);
        wcaOptionPanelLayout.setHorizontalGroup(
            wcaOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wcaOptionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(wcaOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(wcaHeuristicCheckBox)
                    .addComponent(wcaDebugCheckBox))
                .addContainerGap(221, Short.MAX_VALUE))
        );
        wcaOptionPanelLayout.setVerticalGroup(
            wcaOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wcaOptionPanelLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(wcaHeuristicCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(wcaDebugCheckBox)
                .addContainerGap())
        );

        wcaSymbolRangePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Symbol Range", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        wcaSymbolRangePanel.setMinimumSize(new java.awt.Dimension(427, 78));
        wcaSymbolRangePanel.setPreferredSize(new java.awt.Dimension(452, 78));

        wcaSymTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "byte", "char", "short", "int", "long", "double" }));
        wcaSymTypeComboBox.setToolTipText("<html>\nSpecifies the data type of the symbol.\n</html>");
        wcaSymTypeComboBox.setMaximumSize(new java.awt.Dimension(132, 24));
        wcaSymTypeComboBox.setMinimumSize(new java.awt.Dimension(132, 24));
        wcaSymTypeComboBox.setPreferredSize(new java.awt.Dimension(132, 24));
        wcaSymTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wcaSymTypeComboBoxActionPerformed(evt);
            }
        });

        wcaSymMinTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("###0.###"))));
        wcaSymMinTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        wcaSymMinTextField.setToolTipText("<html>\nSpecifies the minimum value of the Symbol for the current data type selection.\n</html>");
        wcaSymMinTextField.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        wcaSymMinTextField.setMaximumSize(new java.awt.Dimension(132, 20));
        wcaSymMinTextField.setMinimumSize(new java.awt.Dimension(132, 20));
        wcaSymMinTextField.setPreferredSize(new java.awt.Dimension(132, 20));
        wcaSymMinTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                wcaSymMinTextFieldFocusLost(evt);
            }
        });

        wcaSymMaxTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("###0.###"))));
        wcaSymMaxTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        wcaSymMaxTextField.setToolTipText("<html>\nSpecifies the maximum value of the Symbol for the current data type selection.\n</html>");
        wcaSymMaxTextField.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        wcaSymMaxTextField.setMaximumSize(new java.awt.Dimension(132, 20));
        wcaSymMaxTextField.setMinimumSize(new java.awt.Dimension(132, 20));
        wcaSymMaxTextField.setPreferredSize(new java.awt.Dimension(132, 20));
        wcaSymMaxTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                wcaSymMaxTextFieldFocusLost(evt);
            }
        });

        wcaSymMaxLabel.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        wcaSymMaxLabel.setText("Max");

        wcaSymTypeLabel.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        wcaSymTypeLabel.setText("Type");

        wcaSymMinLabel.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        wcaSymMinLabel.setText("Min");

        javax.swing.GroupLayout wcaSymbolRangePanelLayout = new javax.swing.GroupLayout(wcaSymbolRangePanel);
        wcaSymbolRangePanel.setLayout(wcaSymbolRangePanelLayout);
        wcaSymbolRangePanelLayout.setHorizontalGroup(
            wcaSymbolRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, wcaSymbolRangePanelLayout.createSequentialGroup()
                .addGap(0, 28, Short.MAX_VALUE)
                .addComponent(wcaSymTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(wcaSymMinTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(wcaSymMaxTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
            .addGroup(wcaSymbolRangePanelLayout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addComponent(wcaSymTypeLabel)
                .addGap(110, 110, 110)
                .addComponent(wcaSymMinLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(wcaSymMaxLabel)
                .addGap(75, 75, 75))
        );
        wcaSymbolRangePanelLayout.setVerticalGroup(
            wcaSymbolRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, wcaSymbolRangePanelLayout.createSequentialGroup()
                .addGap(0, 13, Short.MAX_VALUE)
                .addGroup(wcaSymbolRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wcaSymMaxTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(wcaSymMinTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(wcaSymTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(wcaSymbolRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wcaSymMinLabel)
                    .addComponent(wcaSymTypeLabel)
                    .addComponent(wcaSymMaxLabel)))
        );

        javax.swing.GroupLayout wcaSetupPanelLayout = new javax.swing.GroupLayout(wcaSetupPanel);
        wcaSetupPanel.setLayout(wcaSetupPanelLayout);
        wcaSetupPanelLayout.setHorizontalGroup(
            wcaSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wcaSetupPanelLayout.createSequentialGroup()
                .addGroup(wcaSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(wcaSymbolRangePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(wcaOptionPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(wcaSelectionPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        wcaSetupPanelLayout.setVerticalGroup(
            wcaSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wcaSetupPanelLayout.createSequentialGroup()
                .addComponent(wcaSelectionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(wcaOptionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(wcaSymbolRangePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
        );

        setupTabbedPane.addTab("WCA Setup", wcaSetupPanel);

        sideChanSetupPanel.setPreferredSize(new java.awt.Dimension(455, 400));

        scSelectionPanel.setMaximumSize(new java.awt.Dimension(452, 158));
        scSelectionPanel.setMinimumSize(new java.awt.Dimension(452, 158));
        scSelectionPanel.setPreferredSize(new java.awt.Dimension(452, 200));

        scSolverComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "none", "z3", "z3bitvector", "choco", "cvc3" }));
        scSolverComboBox.setSelectedIndex(1);
        scSolverComboBox.setToolTipText("<html>\nSpecifies the solver to use in the analysis.\n</html>");
        scSolverComboBox.setMaximumSize(new java.awt.Dimension(105, 24));
        scSolverComboBox.setMinimumSize(new java.awt.Dimension(105, 24));
        scSolverComboBox.setPreferredSize(new java.awt.Dimension(105, 24));
        scSolverComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scSolverComboBoxActionPerformed(evt);
            }
        });

        scBvlenSpinner.setModel(new javax.swing.SpinnerNumberModel(32, 1, 9999, 1));
        scBvlenSpinner.setToolTipText("<html>\nSpecifies the Bit Vector length to use for z3 solver\n(only valid for z3bitvector solver selection)\n</html>");
        scBvlenSpinner.setEnabled(false);
        scBvlenSpinner.setMaximumSize(new java.awt.Dimension(105, 20));
        scBvlenSpinner.setMinimumSize(new java.awt.Dimension(105, 20));
        scBvlenSpinner.setPreferredSize(new java.awt.Dimension(105, 20));
        scBvlenSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                scBvlenSpinnerStateChanged(evt);
            }
        });

        scTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Time", "Memory", "File", "Socket" }));
        scTypeComboBox.setPreferredSize(new java.awt.Dimension(105, 24));
        scTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scTypeComboBoxActionPerformed(evt);
            }
        });

        scBvlenLabel.setText("BV Length");

        scSolverLabel.setText("Solver");

        scMultirunLabel.setForeground(new java.awt.Color(51, 51, 255));
        scMultirunLabel.setPreferredSize(new java.awt.Dimension(360, 15));

        scTypeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        scTypeLabel.setText("Type");
        scTypeLabel.setPreferredSize(new java.awt.Dimension(83, 15));

        javax.swing.GroupLayout scSelectionPanelLayout = new javax.swing.GroupLayout(scSelectionPanel);
        scSelectionPanel.setLayout(scSelectionPanelLayout);
        scSelectionPanelLayout.setHorizontalGroup(
            scSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scSelectionPanelLayout.createSequentialGroup()
                .addGroup(scSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(scSelectionPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(scMultirunLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 424, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(scSelectionPanelLayout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addGroup(scSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(scSolverLabel)
                            .addComponent(scBvlenLabel)
                            .addComponent(scTypeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(6, 6, 6)
                        .addGroup(scSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(scTypeComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(scSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(scBvlenSpinner, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(scSolverComboBox, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap(16, Short.MAX_VALUE))
        );
        scSelectionPanelLayout.setVerticalGroup(
            scSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scSelectionPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(scSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scSolverLabel)
                    .addComponent(scSolverComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(scSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scBvlenSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(scBvlenLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(scSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(scTypeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(86, 86, 86)
                .addComponent(scMultirunLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        scOptionPanel.setPreferredSize(new java.awt.Dimension(452, 60));

        scDebugCheckBox.setText("Debug");
        scDebugCheckBox.setToolTipText("<html>\nEnables the debugging mode\n</html>");
        scDebugCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scDebugCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout scOptionPanelLayout = new javax.swing.GroupLayout(scOptionPanel);
        scOptionPanel.setLayout(scOptionPanelLayout);
        scOptionPanelLayout.setHorizontalGroup(
            scOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scOptionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scDebugCheckBox)
                .addContainerGap(373, Short.MAX_VALUE))
        );
        scOptionPanelLayout.setVerticalGroup(
            scOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scOptionPanelLayout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(scDebugCheckBox)
                .addContainerGap())
        );

        scSymbolRangePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Symbol Range", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        scSymbolRangePanel.setMinimumSize(new java.awt.Dimension(427, 78));
        scSymbolRangePanel.setPreferredSize(new java.awt.Dimension(452, 78));

        scSymTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "byte", "char", "short", "int", "long", "double" }));
        scSymTypeComboBox.setToolTipText("<html>\nSpecifies the data type of the symbol.\n</html>");
        scSymTypeComboBox.setMaximumSize(new java.awt.Dimension(132, 24));
        scSymTypeComboBox.setMinimumSize(new java.awt.Dimension(132, 24));
        scSymTypeComboBox.setPreferredSize(new java.awt.Dimension(132, 24));
        scSymTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scSymTypeComboBoxActionPerformed(evt);
            }
        });

        scSymMinTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("###0.###"))));
        scSymMinTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        scSymMinTextField.setToolTipText("<html>\nSpecifies the minimum value of the Symbol for the current data type selection.\n</html>");
        scSymMinTextField.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        scSymMinTextField.setMaximumSize(new java.awt.Dimension(132, 20));
        scSymMinTextField.setMinimumSize(new java.awt.Dimension(132, 20));
        scSymMinTextField.setPreferredSize(new java.awt.Dimension(132, 20));
        scSymMinTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                scSymMinTextFieldFocusLost(evt);
            }
        });

        scSymMaxTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("###0.###"))));
        scSymMaxTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        scSymMaxTextField.setToolTipText("<html>\nSpecifies the maximum value of the Symbol for the current data type selection.\n</html>");
        scSymMaxTextField.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        scSymMaxTextField.setMaximumSize(new java.awt.Dimension(132, 20));
        scSymMaxTextField.setMinimumSize(new java.awt.Dimension(132, 20));
        scSymMaxTextField.setPreferredSize(new java.awt.Dimension(132, 20));
        scSymMaxTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                scSymMaxTextFieldFocusLost(evt);
            }
        });

        scSymMaxLabel.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        scSymMaxLabel.setText("Max");

        scSymTypeLabel.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        scSymTypeLabel.setText("Type");

        scSymMinLabel.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        scSymMinLabel.setText("Min");

        javax.swing.GroupLayout scSymbolRangePanelLayout = new javax.swing.GroupLayout(scSymbolRangePanel);
        scSymbolRangePanel.setLayout(scSymbolRangePanelLayout);
        scSymbolRangePanelLayout.setHorizontalGroup(
            scSymbolRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, scSymbolRangePanelLayout.createSequentialGroup()
                .addGap(0, 28, Short.MAX_VALUE)
                .addComponent(scSymTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scSymMinTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scSymMaxTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
            .addGroup(scSymbolRangePanelLayout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addComponent(scSymTypeLabel)
                .addGap(110, 110, 110)
                .addComponent(scSymMinLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(scSymMaxLabel)
                .addGap(75, 75, 75))
        );
        scSymbolRangePanelLayout.setVerticalGroup(
            scSymbolRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, scSymbolRangePanelLayout.createSequentialGroup()
                .addGap(0, 13, Short.MAX_VALUE)
                .addGroup(scSymbolRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scSymMaxTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(scSymMinTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(scSymTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(scSymbolRangePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scSymMinLabel)
                    .addComponent(scSymTypeLabel)
                    .addComponent(scSymMaxLabel)))
        );

        javax.swing.GroupLayout sideChanSetupPanelLayout = new javax.swing.GroupLayout(sideChanSetupPanel);
        sideChanSetupPanel.setLayout(sideChanSetupPanelLayout);
        sideChanSetupPanelLayout.setHorizontalGroup(
            sideChanSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sideChanSetupPanelLayout.createSequentialGroup()
                .addGroup(sideChanSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sideChanSetupPanelLayout.createSequentialGroup()
                        .addComponent(scSelectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sideChanSetupPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(sideChanSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(scSymbolRangePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(scOptionPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        sideChanSetupPanelLayout.setVerticalGroup(
            sideChanSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sideChanSetupPanelLayout.createSequentialGroup()
                .addComponent(scSelectionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(scOptionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(scSymbolRangePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35))
        );

        setupTabbedPane.addTab("Side Channel", sideChanSetupPanel);

        cloudPanel.setPreferredSize(new java.awt.Dimension(455, 400));

        cloudSetupPanel.setPreferredSize(new java.awt.Dimension(455, 124));

        cloudNameTextField.setToolTipText("<html>\nThe human-readable name of the job to be displayed in the application list.<br>\nDefaults to the project name but can be modified or blank to omit.\n</html>");
        cloudNameTextField.setPreferredSize(new java.awt.Dimension(250, 28));

        jLabel1.setText("Job Name");

        jLabel2.setText("Number of Executors");

        jLabel3.setText("Initial Depth");

        cloudExecutorsTextField.setText("4");
        cloudExecutorsTextField.setToolTipText("<html>\nThe number of executors to use in the exploration.<br>\nNumeric integer value range 1 - 999999999 or blank to omit.\n</html>");
        cloudExecutorsTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                cloudExecutorsTextFieldFocusLost(evt);
            }
        });
        cloudExecutorsTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cloudExecutorsTextFieldActionPerformed(evt);
            }
        });

        cloudInitialDepthTextField.setToolTipText("<html>\nThe depth of the initial single-threaded exploration.<br>\nNumeric integer value range 1 - 999999999 or blank to omit.\n</html>");
        cloudInitialDepthTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                cloudInitialDepthTextFieldFocusLost(evt);
            }
        });
        cloudInitialDepthTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cloudInitialDepthTextFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout cloudSetupPanelLayout = new javax.swing.GroupLayout(cloudSetupPanel);
        cloudSetupPanel.setLayout(cloudSetupPanelLayout);
        cloudSetupPanelLayout.setHorizontalGroup(
            cloudSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cloudSetupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cloudSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(cloudSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cloudNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cloudExecutorsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cloudInitialDepthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(35, Short.MAX_VALUE))
        );
        cloudSetupPanelLayout.setVerticalGroup(
            cloudSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cloudSetupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cloudSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cloudNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(cloudSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cloudExecutorsTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(cloudSetupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cloudInitialDepthTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE))
                .addContainerGap())
        );

        cloudFilterPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Filter Setup", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        cloudFilterPanel.setPreferredSize(new java.awt.Dimension(455, 176));

        sessionOnlyCheckBox.setText("Session only");
        sessionOnlyCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        sessionOnlyCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        sessionOnlyCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sessionOnlyCheckBoxActionPerformed(evt);
            }
        });

        sessionOnlyLabel.setText("(only display jobs started in this session)");

        userFilterCheckBox.setText("User");
        userFilterCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        userFilterCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        userFilterCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userFilterCheckBoxActionPerformed(evt);
            }
        });

        userFilterTextField.setEnabled(false);
        userFilterTextField.setPreferredSize(new java.awt.Dimension(200, 24));
        userFilterTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                userFilterTextFieldFocusLost(evt);
            }
        });
        userFilterTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userFilterTextFieldActionPerformed(evt);
            }
        });

        nameFilterCheckBox.setText("Job Name");
        nameFilterCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        nameFilterCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        nameFilterCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nameFilterCheckBoxActionPerformed(evt);
            }
        });

        nameFilterTextField.setEnabled(false);
        nameFilterTextField.setPreferredSize(new java.awt.Dimension(200, 24));
        nameFilterTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                nameFilterTextFieldFocusLost(evt);
            }
        });
        nameFilterTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nameFilterTextFieldActionPerformed(evt);
            }
        });

        earliestTimeCheckBox.setText("Time");
        earliestTimeCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        earliestTimeCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        earliestTimeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                earliestTimeCheckBoxActionPerformed(evt);
            }
        });

        earliestTimeSpinner.setModel(new javax.swing.SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.HOUR));
        earliestTimeSpinner.setEnabled(false);
        earliestTimeSpinner.setPreferredSize(new java.awt.Dimension(130, 20));
        earliestTimeSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                earliestTimeSpinnerStateChanged(evt);
            }
        });

        latestTimeCheckBox.setEnabled(false);
        latestTimeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                latestTimeCheckBoxActionPerformed(evt);
            }
        });

        latestTimeSpinner.setModel(new javax.swing.SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.HOUR));
        latestTimeSpinner.setEnabled(false);
        latestTimeSpinner.setPreferredSize(new java.awt.Dimension(130, 20));
        latestTimeSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                latestTimeSpinnerStateChanged(evt);
            }
        });

        earliestLabel.setText("earliest");

        latestLabel.setText("latest");

        javax.swing.GroupLayout cloudFilterPanelLayout = new javax.swing.GroupLayout(cloudFilterPanel);
        cloudFilterPanel.setLayout(cloudFilterPanelLayout);
        cloudFilterPanelLayout.setHorizontalGroup(
            cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cloudFilterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sessionOnlyCheckBox, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(userFilterCheckBox, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(nameFilterCheckBox, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(earliestTimeCheckBox, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(cloudFilterPanelLayout.createSequentialGroup()
                        .addGap(48, 48, 48)
                        .addComponent(earliestLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(latestLabel)
                        .addGap(35, 35, 35))
                    .addGroup(cloudFilterPanelLayout.createSequentialGroup()
                        .addGroup(cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(cloudFilterPanelLayout.createSequentialGroup()
                                .addComponent(earliestTimeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(26, 26, 26)
                                .addComponent(latestTimeCheckBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(latestTimeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(userFilterTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(nameFilterTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(sessionOnlyLabel))
                        .addGap(0, 5, Short.MAX_VALUE)))
                .addContainerGap())
        );
        cloudFilterPanelLayout.setVerticalGroup(
            cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cloudFilterPanelLayout.createSequentialGroup()
                .addGroup(cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sessionOnlyCheckBox)
                    .addComponent(sessionOnlyLabel))
                .addGap(8, 8, 8)
                .addGroup(cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(cloudFilterPanelLayout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addComponent(nameFilterCheckBox)
                        .addGap(8, 8, 8)
                        .addComponent(earliestTimeCheckBox))
                    .addGroup(cloudFilterPanelLayout.createSequentialGroup()
                        .addGroup(cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(userFilterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(userFilterCheckBox))
                        .addGap(8, 8, 8)
                        .addComponent(nameFilterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addGroup(cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(cloudFilterPanelLayout.createSequentialGroup()
                                .addGroup(cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(earliestTimeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(latestTimeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(8, 8, 8)
                                .addGroup(cloudFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(earliestLabel)
                                    .addComponent(latestLabel)))
                            .addComponent(latestTimeCheckBox))))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        cloudSummaryPanel.setPreferredSize(new java.awt.Dimension(455, 43));

        jobsFilteredLabel.setText("Filtered");

        jobsTotalTextField.setEditable(false);

        jobsFilteredTextField.setEditable(false);

        jobsTotalLabel.setText("Jobs Total");

        javax.swing.GroupLayout cloudSummaryPanelLayout = new javax.swing.GroupLayout(cloudSummaryPanel);
        cloudSummaryPanel.setLayout(cloudSummaryPanelLayout);
        cloudSummaryPanelLayout.setHorizontalGroup(
            cloudSummaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cloudSummaryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jobsTotalLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jobsTotalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addComponent(jobsFilteredLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jobsFilteredTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(96, Short.MAX_VALUE))
        );
        cloudSummaryPanelLayout.setVerticalGroup(
            cloudSummaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cloudSummaryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cloudSummaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jobsTotalLabel)
                    .addComponent(jobsFilteredLabel)
                    .addComponent(jobsFilteredTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jobsTotalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout cloudPanelLayout = new javax.swing.GroupLayout(cloudPanel);
        cloudPanel.setLayout(cloudPanelLayout);
        cloudPanelLayout.setHorizontalGroup(
            cloudPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cloudPanelLayout.createSequentialGroup()
                .addGroup(cloudPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(cloudSetupPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cloudFilterPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cloudSummaryPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        cloudPanelLayout.setVerticalGroup(
            cloudPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cloudPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cloudSetupPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cloudFilterPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(cloudSummaryPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        setupTabbedPane.addTab("Cloud Setup", cloudPanel);

        javax.swing.GroupLayout setupPanelLayout = new javax.swing.GroupLayout(setupPanel);
        setupPanel.setLayout(setupPanelLayout);
        setupPanelLayout.setHorizontalGroup(
            setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(setupPanelLayout.createSequentialGroup()
                .addComponent(methodPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 618, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(setupTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(7, Short.MAX_VALUE))
        );
        setupPanelLayout.setVerticalGroup(
            setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(setupTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(methodPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        setupTabbedPane.getAccessibleContext().setAccessibleName("TabPane");

        mainSplitPane.setLeftComponent(setupPanel);

        viewerTabbedPane.setMinimumSize(new java.awt.Dimension(1031, 100));
        viewerTabbedPane.setName(""); // NOI18N
        viewerTabbedPane.setPreferredSize(new java.awt.Dimension(1031, 200));
        viewerTabbedPane.addTab("Driver File", driverScrollPane);

        configScrollPane.setMinimumSize(new java.awt.Dimension(1031, 272));
        configScrollPane.setPreferredSize(new java.awt.Dimension(1031, 272));
        viewerTabbedPane.addTab("Config File", configScrollPane);

        outputScrollPane.setMinimumSize(new java.awt.Dimension(1031, 272));
        outputScrollPane.setPreferredSize(new java.awt.Dimension(1031, 272));

        outputTextArea.setColumns(20);
        outputTextArea.setRows(5);
        outputTextArea.setToolTipText("");
        outputScrollPane.setViewportView(outputTextArea);

        viewerTabbedPane.addTab("Output", outputScrollPane);

        statusScrollPane.setViewportView(statusTextPane);

        viewerTabbedPane.addTab("Status", statusScrollPane);

        debugTextPane.setFont(new java.awt.Font("Courier 10 Pitch", 0, 10)); // NOI18N
        debugScrollPane.setViewportView(debugTextPane);

        viewerTabbedPane.addTab("Debug", debugScrollPane);

        cloudJobsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Run", "Job Name", "Job Number", "User", "Status", "Start Time", "Elapsed"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        cloudJobsTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        cloudJobsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                cloudJobsTableMouseClicked(evt);
            }
        });
        cloudJobsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                cloudJobsTableKeyPressed(evt);
            }
        });
        cloudScrollPane.setViewportView(cloudJobsTable);

        viewerTabbedPane.addTab("Cloud Jobs", cloudScrollPane);

        mctsSubmitJobButton.setBackground(new java.awt.Color(204, 255, 204));
        mctsSubmitJobButton.setText("Submit");
        mctsSubmitJobButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mctsSubmitJobButtonActionPerformed(evt);
            }
        });

        jLabel4.setText("JPF filename");

        jLabel5.setText("MCTS zip file");

        mctsSelectButton.setText("Select");
        mctsSelectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mctsSelectButtonActionPerformed(evt);
            }
        });

        mctsStopButton.setBackground(new java.awt.Color(255, 204, 204));
        mctsStopButton.setText("Stop");
        mctsStopButton.setEnabled(false);
        mctsStopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mctsStopButtonActionPerformed(evt);
            }
        });

        mctsCountLabel.setText("0");

        mctsValidLabel.setText("0");

        mctsGraphRadioButton1.setSelected(true);
        mctsGraphRadioButton1.setText("Graph the max Best Reward over time");
        mctsGraphRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mctsGraphRadioButton1ActionPerformed(evt);
            }
        });

        mctsGraphRadioButton2.setText("Graph the Best Reward values (sorted)");
        mctsGraphRadioButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mctsGraphRadioButton2ActionPerformed(evt);
            }
        });

        mctsGraphRadioButton3.setText("Graph the Best Reward values (sorted, ignore 0)");
        mctsGraphRadioButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mctsGraphRadioButton3ActionPerformed(evt);
            }
        });

        jLabel6.setText("Count:");

        jLabel7.setText("Frontiers reached:");

        javax.swing.GroupLayout mctsPanelLayout = new javax.swing.GroupLayout(mctsPanel);
        mctsPanel.setLayout(mctsPanelLayout);
        mctsPanelLayout.setHorizontalGroup(
            mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mctsPanelLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mctsPanelLayout.createSequentialGroup()
                        .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addGap(4, 4, 4)
                        .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(mctsZipFileTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
                            .addComponent(mctsJpfFileComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(mctsSubmitJobButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(mctsSelectButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(mctsStopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(36, 36, 36)
                        .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mctsGraphRadioButton2)
                            .addComponent(mctsGraphRadioButton1)
                            .addComponent(mctsGraphRadioButton3)))
                    .addGroup(mctsPanelLayout.createSequentialGroup()
                        .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7))
                        .addGap(12, 12, 12)
                        .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mctsCountLabel)
                            .addComponent(mctsValidLabel))))
                .addContainerGap(171, Short.MAX_VALUE))
        );
        mctsPanelLayout.setVerticalGroup(
            mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mctsPanelLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(mctsZipFileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mctsSelectButton)
                    .addComponent(mctsGraphRadioButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mctsSubmitJobButton)
                    .addComponent(jLabel4)
                    .addComponent(mctsJpfFileComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mctsStopButton)
                    .addComponent(mctsGraphRadioButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mctsGraphRadioButton3)
                .addGap(10, 10, 10)
                .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mctsCountLabel)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(mctsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mctsValidLabel)
                    .addComponent(jLabel7))
                .addContainerGap())
        );

        viewerTabbedPane.addTab("MCTS", mctsPanel);

        mainSplitPane.setRightComponent(viewerTabbedPane);
        viewerTabbedPane.getAccessibleContext().setAccessibleName("viewerTab");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spfExecutePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1094, Short.MAX_VALUE)
            .addComponent(mainSplitPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(mainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 601, Short.MAX_VALUE)
                .addGap(12, 12, 12)
                .addComponent(spfExecutePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    /**
     * determines if a String value contains a numeric value in the specified range.
     * 
     * @param value - the String containing the numeric value to test
     * @param min - the min value
     * @param max - the max value
     * @return true if the String represents a numeric value between the range
     */
    private boolean verifyIntegerValue (String value, int min, int max) {
        // verify output is valid
        if (value.length() > 9) {
            statusDisplayMessage(DebugType.Error, "Invalid entry: must be integer less than 9 digits in length");
            return false;
        }
        for (int ix = 0; ix < value.length(); ix++) {
            if (value.charAt(ix) < '0' || value.charAt(ix) > '9') {
                statusDisplayMessage(DebugType.Error, "Invalid entry: must be numeric integer");
                return false;
            }
        }
        if (Integer.parseInt(value) < min || Integer.parseInt(value) > max) {
            statusDisplayMessage(DebugType.Error, "Invalid entry: must be integer value > " + min + " and < " + max);
            return false;
        }

        return true;
    }

    private void debugDisplayMessage(DebugType type, String message) {
        // set entry/exit tab level
        String tabs = "";
        if ((type == DebugType.Exit || type == DebugType.ErrorExit) && debugEntryLevel > 0)
            --debugEntryLevel;
        for (int ix = 0; ix < debugEntryLevel; ix++)
            tabs += "|   ";
        if (type == DebugType.Entry && debugEntryLevel < 9)
            ++debugEntryLevel;
        if (type == DebugType.ErrorExit) // now we can change ErrorExit to simply Error
            type = DebugType.Error;

        // if EntryExit, insert a ENTRY & EXIT entries
        if (type == DebugType.EntryExit) {
            debugmsg.print (getDebugTypeString(DebugType.Entry), tabs + message);
            debugmsg.print (getDebugTypeString(DebugType.Exit), tabs + message);
            return;
        }
            
        debugmsg.print (getDebugTypeString(type), tabs + message);
    }
    
    /**
     * displays a GUI event in the debug window.
     * 
     * @param event - the name of the event that occurred (GUI action)
     * @param item  - the name of the item changed (GUI widget)
     * @param message - (optional) message to append to the event
     */
    private void debugDisplayEvent(DebugEvent event, String item, String message) {
        String msgout = "<" + event + "> " + item;
        if (message != null && !message.isEmpty())
            msgout += " - " + message;
        debugDisplayMessage (DebugType.Event, msgout);
    }
    
    /**
     * clears the status display field.
     */
    private void statusDisplayMessage() {
        this.statusLabel.setText("");
    }

    /**
     * displays error and informational messages in the status display field.
     * Note there are 2 different colors for the message because the label is
     * displayed on the current panel with a grey background and the other is
     * displayed in the Status tab panel which has a white background.
     * 
     * @param message - the error message to display
     */
    private void statusDisplayMessage(DebugType type, String message) {
        switch (type) {
            case IntError :
                // report to stderr, but don't display on command status line
                internalDisplayMessage(type, "ERROR: " + message);
                break;

            case ErrorExit : // fall through...
            case Error :
                // display on the command status line
                this.statusLabel.setForeground(Color.RED);
                this.statusLabel.setText("ERROR: " + message);
                // report to stderr
                internalDisplayMessage(type, "ERROR: " + message);
                break;
                
            case Warning :
                // display on the command status line
                this.statusLabel.setForeground(Color.BLUE);
                this.statusLabel.setText("WARNING: " + message);
                // report to stderr
                internalDisplayMessage(type, "WARNING: " + message);
                break;
                
            case EntryExit : // fall through...
            case Entry : // fall through...
            case Exit : // fall through...
            case Success :
                // these messages are only sent to the Status & Debug tab panels
                break;
                
            default:
            case Normal :
                // display on the command status line
                this.statusLabel.setForeground(Color.BLACK);
                this.statusLabel.setText(message);
                break;
        }

        statusmsg.print(type.toString(), message);

        // report all status messages to debug as well
        debugDisplayMessage(type, message);
    }

    /**
     * displays internally reported error messages.
     * reports the message on stderr or stdout.
     * 
     * @param message - the error message to display
     */
    private void internalDisplayMessage(DebugType type, String message) {
        if (type.equals(DebugType.Error) || type.equals(DebugType.IntError)) {
            System.err.println(message);
        }
        else {
            System.out.println(message);
        }
    }

    /**
     * @return the base path for the output of Janalyzer.
     */
    private String getProjRootPath () {
        return this.prjpathTextField.getText() + "/janalyzer/";
    }
    
    /**
     * @return the base path for the output of the SPF utility of Janalyzer.
     */
    private String getSPFBasePath () {
        return getProjRootPath() + "spf/";
    }
    
    /**
     * @return the base path for the output of the SPF driver generation.
     */
    private String getSPFDriverPath () {
        return getSPFBasePath() + "drivers/";
    }
    
    /**
     * this method checks if the Driver and Config files for the project exist
     * and enable or disable the Launch button accordingly.
     */
    private boolean checkIfDriverFilesExist() {
        String driverpathname = getSPFDriverPath();

        String driverName = driverpathname + Util.getDriverFileName(projectName);
        File drvfile = new File(driverName);
        if (!drvfile.exists())
            return false;

        // determine the number of config files specified
        Integer count = getSpfIterations (WCAFrame.spfMode);
        String configName;
        for (int ix = 1; ix <= count; ix++) {
            int id = (count <= 1) ? 0 : ix;
            configName = driverpathname + Util.getConfigFileName(projectName, id);
            File cfgfile = new File(configName);
            if (!cfgfile.exists())
                return false;
        }

        return true;
    }

    /**
     * Generate the Test method list for combo boxes from Class selection.
     * Copies the lists into the appropriate combo boxes for selecting the methods
     * 
     * @return the selected item, null if none
     */
    private String generateTestMethodList () {
        debugDisplayMessage(DebugType.Entry, "generateTestMethodList");
        // init the method lists to null
        JComboBox comboBox = this.method2ComboBox;
        comboBox.removeAllItems();
        
        // build the Method list to select from based on the class selection
        // (only use application classes for Test method selection)
        String clsName = (String)this.class2ComboBox.getSelectedItem();
        if (clsName == null)
            return null;
        Set<String> mthNameSet = this.applicationClassMap.get(clsName);
        if (mthNameSet == null || mthNameSet.isEmpty())
            return null;
        // generate the method list in the combobox
        for (String mthName : mthNameSet) {
            comboBox.addItem(mthName);
        }

        int cbSize = comboBox.getModel().getSize();
        debugDisplayMessage(DebugType.Normal, "generateTestMethodList found " + cbSize + " entries");

        // init the selection to last entry
        comboBox.setSelectedIndex(cbSize-1);
        String selected = (String) comboBox.getItemAt(cbSize-1);

        debugDisplayMessage(DebugType.Exit, "generateTestMethodList");
        return selected;
    }
    
    /**
     * Generate the Iterative method lists for combo boxes from Class selection.
     * Copies the lists into the appropriate combo boxes for selecting the methods
     * 
     * @param clsName - the class selection to get the methods for
     * @param methName - the method selection (null if select preferred method)
     * @return the selected item, null if none
     */
    private String generateIterMethodList (String clsName, String methName) {
        debugDisplayMessage(DebugType.Entry, "generateIterMethodList: class = " + clsName);

        // init the method lists to null (even if no class selection)
        JComboBox comboBox = this.method1ComboBox;
        comboBox.removeAllItems();
        boolean bFound = false;
        
        // build the Method list to select from based on the class selection
        // (use both application and library classes for Iterative method selection)
        if (clsName == null) {
            debugDisplayMessage(DebugType.Exit, "generateIterMethodList");
            return null;
        }
        Set<String> mthNameSet = this.applicationClassMap.get(clsName);
        if (mthNameSet == null || mthNameSet.isEmpty())
            mthNameSet = this.libraryClassMap.get(clsName);
        if (mthNameSet == null || mthNameSet.isEmpty()) {
            debugDisplayMessage(DebugType.Warning, "generateIterMethodList: no methods found for class");
            debugDisplayMessage(DebugType.Exit, "generateIterMethodList");
            return null;
        }

        // generate the method list in the combobox
        for (String name : mthNameSet) {
            comboBox.addItem(name);
            if (methName != null && methName.equals(name))
                bFound = true;
        }
            
        int cbSize = comboBox.getModel().getSize();
        debugDisplayMessage(DebugType.Normal, "found " + cbSize + " method entries");

        // init the selection (substitute the preferred method if found)
        String selected = methName;
        if (selected != null && bFound == true) {
            // if method was specified and was found in the list, use it
            comboBox.setSelectedItem(selected);
            debugDisplayMessage(DebugType.Normal, "- method set to specified: " + selected);
        }
        else {
            // method not specified, find preferred method in list
            selected = findPreferredMethod(mthNameSet);
            if (selected != null) {
                debugDisplayMessage(DebugType.Autoset, "- method set to preferred: " + selected);
                comboBox.setSelectedItem(selected);
            }
            else {
                // no preferred method found - set the selection to last entry
                comboBox.setSelectedIndex(cbSize-1);
                selected = (String) comboBox.getItemAt(cbSize-1);
                debugDisplayMessage(DebugType.Normal, "- method set to default: " + selected);
            }
        }

        debugDisplayMessage(DebugType.Exit, "generateIterMethodList");
        return selected;
    }
    
    /**
     * Generate the Iterative class list for class combo box from the specified
     * Class selection. The selection will be used to choose the specified
     * class and all subclasses of it as the list the user can select from,
     * and then the selection will be set. It will then generate the corresponding
     * method list and will make the specified selection, or will choose a
     * preferred method if a specific method is not selected.
     * 
     * @param paramType - the parameter data type
     * @param prefClass - the preferred class selection for the parameter (for interface types)
     * @param method - the method selection (null if select preferred method)
     * @return the selected item, null if none
     */
    private void generateIterClassList (TypeReference paramType, String prefClass, String method) {
        debugDisplayMessage(DebugType.Entry, "generateIterClassList: P" + this.argOffset +
                            ": Class = " + prefClass + ", Method = " + method);

        // this indicates the param type is neither ItObject nor OtherObject,
        // so there is no class selection
        if (prefClass == null || prefClass.isEmpty()) {
            debugDisplayMessage(DebugType.Exit, "generateIterClassList");
            return;
        }

        String paramClass = prefClass;
        if (paramType == null) {
            // this should not happen. ever.
            debugDisplayMessage(DebugType.Warning, "generateIterClassList: paramClass is NULL !!!");
        }
        else if (paramType.isReferenceType()) {
            paramClass = getArgArrayDataType(paramType, true);
            // convert from dotted form
            if (paramClass.contains("."))
                paramClass = paramClass.replace(".", "/");
            paramClass = "L" + paramClass;
            debugDisplayMessage(DebugType.Normal, "generateIterClassList: paramClass = " + paramClass);
        }

        // reset the class combobox selections to empty
        JComboBox comboBox = this.class1ComboBox;
        comboBox.removeAllItems();
        boolean bFound = false;

        // first add the entries from the application classes that are subclasses
        // of the specified class.
        for (Map.Entry<String, Set<String>> chMapEnt : this.applicationClassMap.entrySet()) {
            String clsEntry = chMapEnt.getKey();

            boolean bSubclass = Program.isSubclassOf(clsEntry, paramClass);
            boolean bImplement = Program.isImplementationOf(clsEntry, paramClass);
            String type = (bSubclass == true) ? "subclass" : "implementation";
            
            if (bSubclass || bImplement) {
                comboBox.addItem(clsEntry);
                if (clsEntry.equals(prefClass)) {
                    debugDisplayMessage(DebugType.Normal, "- found entry in application classes: " + clsEntry);
                    bFound = true;
                }
                else {
                    debugDisplayMessage(DebugType.Normal, "- add " + type + " entry: " + clsEntry);
                }
            }
        }

        // now repeat for the standard library classes.
        for (Map.Entry<String, Set<String>> chMapEnt : libraryClassMap.entrySet()) {
            String clsEntry = chMapEnt.getKey();
            
            boolean bSubclass = Program.isSubclassOf(clsEntry, paramClass);
            boolean bImplement = Program.isImplementationOf(clsEntry, paramClass);
            String type = (bSubclass == true) ? "subclass" : "implementation";
            
            if (bSubclass || bImplement) {
                comboBox.addItem(clsEntry);
                if (clsEntry.equals(prefClass)) {
                    debugDisplayMessage(DebugType.Normal, "- found entry in library classes: " + clsEntry);
                    bFound = true;
                }
                else {
                    debugDisplayMessage(DebugType.Normal, "- add " + type + " entry: " + clsEntry);
                }
            }
        }

        int cbSize = comboBox.getModel().getSize();
        debugDisplayMessage(DebugType.Normal, "filtered class entries = " + cbSize);
        if (bFound != true) {
            debugDisplayMessage(DebugType.ErrorExit, "generateIterClassList: selected class not found: " + prefClass);
            return;
        }

        // now we can make the class selection
        class1ComboBox.setSelectedItem(prefClass);
        debugDisplayMessage(DebugType.Normal, "- set to specified class: " + prefClass);
        
        // since the class has changed, we now have to update the iterative
        // method selection list for the new class and make the method selection.
        generateIterMethodList(prefClass, method);
        debugDisplayMessage(DebugType.Exit, "generateIterClassList");
    }
    
    /**
     * check if the specified limit value exceeds the valid range
     * 
     * @param dtype  - the data type
     * @param svalue - the data value to check
     * @return the limited value if valid range exceeded, null if not
     */
    private String absLimitCheck (String dtype, String svalue) {
        // get the symbol type range limits
        SymTypes range = new SymTypes();
        SymLimits limits = range.getLimits(dtype);

        if (dtype.equals("double")) {
            Double limMin = Double.parseDouble(limits.min);
            Double limMax = Double.parseDouble(limits.max);
            Double value  = Double.parseDouble(svalue);

            // make sure the current value is within the specified range limits
            if (value < limMin) return limMin.toString();
            if (value > limMax) return limMax.toString();
        }
        else {
            Long limMin = Long.parseLong(limits.min);
            Long limMax = Long.parseLong(limits.max);
            Long value  = Long.parseLong(svalue);

            // make sure the current value is within the specified range limits
            if (value < limMin) return limMin.toString();
            if (value > limMax) return limMax.toString();
        }
        
        return null;
    }
    
    /**
     * check if the specified min value exceeds the specified max value.
     * 
     * @param dtype - the data type
     * @param min   - the min value
     * @param max   - the max value
     * @return true if min exceeds max
     */
    private boolean isMinExceedMax (String dtype, String min, String max) {
        if (dtype.equals("double")) {
            Double minValue = Double.parseDouble(min);
            Double maxValue = Double.parseDouble(max);
            if (minValue > maxValue)
                return true;
        }
        else {
            Long minValue = Long.parseLong(min);
            Long maxValue = Long.parseLong(max);
            if (minValue > maxValue)
                return true;
        }
        return false;
    }

    /**
     * determines the number of SPF files for the specified type
     * 
     * @param configType - the configuration type
     * @return the number of files specified
     */
    private Integer getSpfIterations (Options.ConfigType configType) {
        // there are no ranges for side channel
        if (configType == Options.ConfigType.SideChannel) {
            return 1;
        }

        JCheckBox checkbox1, checkbox2;
        Integer startRange1, startRange2, endRange1, endRange2;
        checkbox1 = wcaPolicyRangeCheckBox;
        startRange1 = (Integer)wcaPolicySpinner.getValue();
        endRange1   = (Integer)wcaPolicyEndSpinner.getValue();

        checkbox2 = wcaHistoryRangeCheckBox;
        startRange2 = (Integer)wcaHistorySpinner.getValue();
        endRange2   = (Integer)wcaHistoryEndSpinner.getValue();

        // calculate the number of iterations that will run
        Integer count1 = 1;
        if (checkbox1.isSelected()) {
            count1 = endRange1 - startRange1;
            if (count1 < 0)
                count1 *= -1;
            ++count1;
        }
        Integer count2 = 1;
        if (checkbox2.isSelected()) {
            count2 = endRange2 - startRange2;
            if (count2 < 0)
                count2 *= -1;
            ++count2;
        }
        return count1 * count2;
    }

    private void nextOptionsRangeSelection (Options.ConfigType configType, Options opt) {
        // there are no ranges for side channel
        if (configType == Options.ConfigType.SideChannel) {
            return;
        }

        boolean bRange1, bRange2;
        Integer startRange1, endRange1, stepRange1 = 0;
        Integer startRange2, endRange2, stepRange2 = 0;

        // get the user selections for the range
        bRange1 = wcaPolicyRangeCheckBox.isSelected();
        startRange1 = (Integer)wcaPolicySpinner.getValue();
        endRange1   = (Integer)wcaPolicyEndSpinner.getValue();

        bRange2 = wcaHistoryRangeCheckBox.isSelected();
        startRange2 = (Integer)wcaHistorySpinner.getValue();
        endRange2   = (Integer)wcaHistoryEndSpinner.getValue();

        // determine the step size and whether the value is to be incremented at all
        if (bRange1)
            stepRange1 = (endRange1 > startRange1) ? 1 : -1;
        else
            endRange1 = startRange1;
        if (bRange2)
            stepRange2 = (endRange2 > startRange2) ? 1 : -1;
        else
            endRange2 = startRange2;

        // bump the settings to the next value to test
        if (opt.policyAt < endRange1)
            opt.policyAt += stepRange1;
        else if (opt.historySize < endRange2) {
            opt.policyAt = startRange1;
            opt.historySize += stepRange2;
        }
    }
    
    /**
     * setup GUI for the specified SPF range settings based on the range combobox selection
     * 
     * @param checkBox   - the range checkbox control
     * @param endSpinner - the upper limit range spinner control
     * @param startVal   - the lower limit range value
     */
    private void setGUI_spfSetRange (JCheckBox checkBox, JSpinner endSpinner, Integer startVal) {
        if (checkBox.isSelected()) {
            // enabled - show range & set control to disable
            checkBox.setText("disable range");
            endSpinner.setVisible(true);
            endSpinner.setValue(startVal + 1);
        }
        else {
            // disabled - blank range & set control to enable
            checkBox.setText("enable range");
            endSpinner.setVisible(false);
        }
    }
    
    /**
     * this is used to parse the argument list defined in the method name.
     * It is used for the case of the standard library methods, which do not
     * provide any argument information using getMethod().getParameterType().
     * 
     * @param method - the full method name that includes the argument list
     * @return an array of the argument types as String entries
     */
    private ArrayList<String> getArgListFromMethodName (String method) {
        ArrayList<String> argList = new ArrayList<>();
        
        if (method.contains("(") && method.contains(")")) {
            // find the start of the arguments
            String dtype = method.substring(method.indexOf('(') + 1, method.indexOf(')'));
            while ( dtype.length() > 0 ) {
                String arg;
                if (dtype.contains(";")) {
                    arg = dtype.substring(0,dtype.indexOf(';'));
                    if (dtype.indexOf(';') < dtype.length() - 1)
                        dtype = dtype.substring(dtype.indexOf(';') + 1);
                    else
                        dtype = "";
                }
                else {
                    arg = dtype;
                    dtype = "";
                }
                argList.add(arg);
            }
        }
        
        return argList;
    }

    /**
     * converts the argument types returned in the method name to TypeReference types.
     * NOTE: this does not handle array types.
     * 
     * @param dtype - the String format of the data type to convert
     * @return the corresponding TypeReference value
     */
    private TypeReference getArgTypeFromString (String dtype) {
        // extract the specified argument name
        switch (dtype) {
            // the short form primitives
            case "C":       return TypeReference.Char;
            case "B":       return TypeReference.Byte;
            case "S":       return TypeReference.Short;
            case "I":       return TypeReference.Int;
            case "J":       return TypeReference.Long;
            case "F":       return TypeReference.Float;
            case "D":       return TypeReference.Double;
            case "Z":       return TypeReference.Boolean;

            // the long form primitives
            case "Ljava/lang/Char":    return TypeReference.Char;
            case "Ljava/lang/Byte":    return TypeReference.Byte;
            case "Ljava/lang/Short":   return TypeReference.Short;
            case "Ljava/lang/Integer": return TypeReference.Int;
            case "Ljava/lang/Long":    return TypeReference.Long;
            case "Ljava/lang/Float":   return TypeReference.Float;
            case "Ljava/lang/Double":  return TypeReference.Double;
            case "Ljava/lang/Bool":    return TypeReference.Boolean;

            // other important types
            case "Ljava/lang/String":  return TypeReference.JavaLangString;
            case "Ljava/lang/Object":  return TypeReference.JavaLangObject;
            default:
                break;
        }
        debugDisplayMessage(DebugType.Warning, "getArgTypeFromString: unknown type: " + dtype);
        return TypeReference.JavaLangObject;
    }
    
    private String getArgArrayDataType (TypeReference argType, boolean bBraces) {
        if (argType == null)
            return "";
        
        String dtype = argType.getName().toString();

        // remove the thread type value and the <> enclosure if formatted this way
        if (dtype.startsWith("<")) {
            dtype = dtype.substring(dtype.indexOf(',') + 1);
            dtype = dtype.substring(0, dtype.indexOf('>'));
        }

        // if argument is array type, count the level of the array
        int arraySize = 0;
        if (argType.isArrayType()) {
            arraySize = argType.getDimensionality();
            // remove the leading bracket chars from the string
            while (dtype.startsWith("["))
                dtype = dtype.substring(1);
        }
        
        // convert primitive types (single char) to boxed primitive strings and
        // reference types (classes) to dotted format strings (without leading 'L').
        // (removed check for Primitive type prior to getJavaPrimitiveType because
        // arrays are not flagged as a primitive, although the underlying type
        // may be.
        // if (argType.isPrimitiveType())
        dtype = Util.getJavaPrimitiveType(dtype);
        if (argType.isReferenceType())
            dtype = Util.getJavaReferenceType(dtype);

        // add array braces at end of string if array type
        if (bBraces) {
            for (int ix = 0; ix < arraySize; ix++)
                dtype += "[]";
        }
        
        return dtype;
    }
    
    /**
     * search method list for preferred selection (add, put, enqueue)
     * 
     * @param mthNameSet - the set of methods
     * @return the preferred method (null if none found)
     */
    private String findPreferredMethod(Set<String> mthNameSet) {
        if (mthNameSet != null) {
            for (String m : mthNameSet) {
                if (m.matches("^.*?(add|put|enqueue).*$")) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * check if there is a preferred class to replace the specified one.
     * 
     * @param className - the class value to verify (in dotted form)
     * @return the type of ItObject (empty if not an ItObject)
     */
    private String findPreferredClass (String className) {
        if (className.matches("^.*?(List|Collection|Queue).*$")) {
            String itObject = "Ljava/util/ArrayList";
            if (findClassEntry(itObject))
                return itObject;
        }
        if (className.matches("^.*?(Map).*$")) {
            String itObject = "Ljava/util/HashMap";
            if (findClassEntry(itObject))
                return itObject;
        }
        if (className.matches("^.*?(Set).*$")) {
            String itObject = "Ljava/util/HashSet";
            if (findClassEntry(itObject))
                return itObject;
        }
        if (className.matches("^.*?(Vector).*$")) {
            String itObject = "Ljava/util/Vector";
            if (findClassEntry(itObject))
                return itObject;
        }

        return null;
    }
    
    /**
     * check if the specified class is found in the full list of classes.
     * 
     * @param cls - the class to search for
     * @return true if the entry was found
     */
    private boolean findClassEntry (String cls) {
        // check application library classes
        for (Map.Entry<String, Set<String>> chMapEnt : this.applicationClassMap.entrySet()) {
            String clsEntry = chMapEnt.getKey();
            if (clsEntry.equals(cls)) {
                return true;
            }
        }
        // repeat for the standard library classes
        for (Map.Entry<String, Set<String>> chMapEnt : libraryClassMap.entrySet()) {
            String clsEntry = chMapEnt.getKey();
            if (clsEntry.equals(cls)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * This copies the current GUI control info to the Iterated method argList for the
     * currently selected index.
     */
    private void copyGUIToArgList() {
        debugDisplayMessage(DebugType.Entry, "copyGUIToArgList");
        int ix = this.argOffset;
        List<ArgSelect> argList = this.argListTest;
        
        if (argList.isEmpty())
            debugDisplayMessage(DebugType.Argument, "copyGUIToArgList: No copy - (argList empty)");
        else if (ix >= argList.size())
            debugDisplayMessage(DebugType.Argument, "copyGUIToArgList: No copy - (argList size = " + argList.size() + ", ix = " + ix + " )");
        else {
            // init to default selections
            ArgSelect defaultArgs = new ArgSelect();

            // save the current argList data selection from the GUI selections
            if (this.class1ComboBox.getSelectedItem() != null)
                defaultArgs.cls     = this.class1ComboBox.getSelectedItem().toString();
            if (this.method1ComboBox.getSelectedItem() != null)
                defaultArgs.method  = this.method1ComboBox.getSelectedItem().toString();
            if (this.sizeSelectComboBox.getSelectedItem() != null)
                defaultArgs.size    = this.sizeSelectComboBox.getSelectedItem().toString();
            if (this.valueSelectComboBox.getSelectedItem() != null)
                defaultArgs.value   = this.valueSelectComboBox.getSelectedItem().toString();
            if (this.elementTypeComboBox.getSelectedItem() != null)
                defaultArgs.element = this.elementTypeComboBox.getSelectedItem().toString();
            if (this.primitiveTypeComboBox.getSelectedItem() != null)
                defaultArgs.primitive = this.primitiveTypeComboBox.getSelectedItem().toString();
            if (this.arraySizeComboBox.getSelectedItem() != null)
                defaultArgs.arraySize = this.arraySizeComboBox.getSelectedItem().toString();

            // set the mode based on the radio button selections
            if (this.typePrimitiveRadioButton.isSelected())
                defaultArgs.mode = ArgSelect.ParamMode.Primitive;
            else if (this.typeStringRadioButton.isSelected())
                defaultArgs.mode = ArgSelect.ParamMode.String;
            else if (this.typeSimpletonRadioButton.isSelected())
                defaultArgs.mode = ArgSelect.ParamMode.Simpleton;
            else if (this.typeArrayRadioButton.isSelected())
                defaultArgs.mode = ArgSelect.ParamMode.Array;
            else
                defaultArgs.mode = ArgSelect.ParamMode.DataStruct;

            // save the GUI selections to the argList struct
            argList.get(ix).cls       = defaultArgs.cls;
            argList.get(ix).method    = defaultArgs.method;
            argList.get(ix).value     = defaultArgs.value;
            argList.get(ix).size      = defaultArgs.size;
            argList.get(ix).element   = defaultArgs.element;
            argList.get(ix).primitive = defaultArgs.primitive;
            argList.get(ix).arraySize = defaultArgs.arraySize;
            argList.get(ix).mode      = defaultArgs.mode;
            
            debugDisplayMessage(DebugType.Argument, "copyGUIToArgList: ix = " + ix + " (size " + argList.size() + ")");
            debugDisplayMessage(DebugType.Argument, "    cls       = " + argList.get(ix).cls);
            debugDisplayMessage(DebugType.Argument, "    method    = " + argList.get(ix).method);
            debugDisplayMessage(DebugType.Argument, "    value     = " + argList.get(ix).value);
            debugDisplayMessage(DebugType.Argument, "    size      = " + argList.get(ix).size);
            debugDisplayMessage(DebugType.Argument, "    element   = " + argList.get(ix).element);
            debugDisplayMessage(DebugType.Argument, "    primitive = " + argList.get(ix).primitive);
            debugDisplayMessage(DebugType.Argument, "    arraySize = " + argList.get(ix).arraySize);
            debugDisplayMessage(DebugType.Argument, "    argType   = " + getArgArrayDataType(argList.get(ix).argument, true));
            debugDisplayMessage(DebugType.Argument, "    mode      = " + argList.get(ix).mode);
        }
        debugDisplayMessage(DebugType.Exit, "copyGUIToArgList");
    }

    /**
     * This sets the GUI selections from the currently selected argList entry for
     * the Iterated method.
     */
    private TypeReference copyArgListToGUI() {
        debugDisplayMessage(DebugType.Entry, "copyArgListToGUI");
        TypeReference argType = null;
        int ix = this.argOffset;
        List<ArgSelect> argList = this.argListTest;

        if (argList.isEmpty())
            debugDisplayMessage(DebugType.Argument, "copyArgListToGUI: No copy - (argList empty)");
        else if (ix >= argList.size())
            debugDisplayMessage(DebugType.Argument, "copyArgListToGUI: No copy - (argList size = " + argList.size() + ", ix = " + ix + " )");
        else {
            // get the argument information from the specified Test parameter
            // and copy to the GUI controls.
            setGUIParamSetup_FromArg (argList.get(ix));

            argType = argList.get(ix).argument;
            debugDisplayMessage(DebugType.Argument, "copyArgListToGUI: ix = " + ix + " (size " + argList.size() + ")");
            debugDisplayMessage(DebugType.Argument, "    cls       = " + argList.get(ix).cls);
            debugDisplayMessage(DebugType.Argument, "    method    = " + argList.get(ix).method);
            debugDisplayMessage(DebugType.Argument, "    value     = " + argList.get(ix).value);
            debugDisplayMessage(DebugType.Argument, "    size      = " + argList.get(ix).size);
            debugDisplayMessage(DebugType.Argument, "    element   = " + argList.get(ix).element);
            debugDisplayMessage(DebugType.Argument, "    primitive = " + argList.get(ix).primitive);
            debugDisplayMessage(DebugType.Argument, "    arraySize = " + argList.get(ix).arraySize);
            debugDisplayMessage(DebugType.Argument, "    argType   = " + getArgArrayDataType(argType, true));
            debugDisplayMessage(DebugType.Argument, "    mode      = " + argList.get(ix).mode);
        }

        debugDisplayMessage(DebugType.Exit, "copyArgListToGUI");
        return argType;
    }

    /**
     * copies the ArgSelect data to the GUI parameter combobox and radiobutton controls.
     * 
     * This should be called on startup for initialization and when recalling the
     * next or previous parameter using the Next/Prev keys.
     * 
     * @param arg - the data to copy to the combobox controls
     */
    private void setGUIParamSetup_FromArg (ArgSelect arg) {
        debugDisplayMessage(DebugType.Entry, "setGUIParamSetup_FromArg");
        this.class1ComboBox.setSelectedItem(arg.cls);
        this.method1ComboBox.setSelectedItem(arg.method);
        this.valueSelectComboBox.setSelectedItem(arg.value);
        this.sizeSelectComboBox.setSelectedItem(arg.size);
        this.elementTypeComboBox.setSelectedItem(arg.element);
        this.primitiveTypeComboBox.setSelectedItem(arg.primitive);
        this.arraySizeComboBox.setSelectedItem(arg.arraySize);

        // set the radio button selection
        setGUIParamSetup_Mode (arg.mode);
        debugDisplayMessage(DebugType.Exit, "setGUIParamSetup_FromArg");
    }
    
    /**
     * This makes the custom argument panel controls either visible or not.
     * (Note: this assumes the argListTest list has been setup prior to this,
     * as it will make all controls invisible if the size is 0)
     * 
     * This should be called when the parameter mode changes, which can be directly
     * from the radiobutton selection or when the parameter selection is changed
     * with the Prev/Next buttons, or the method has changed.
     * 
     * @param mode - the mode selection being set
     */
    private void setGUIParamSetup_Visible(ArgSelect.ParamMode mode) {
        debugDisplayMessage(DebugType.Entry, "setGUIParamSetup_Visible");
        // init all selections to disabled
        boolean bClass = false;
        boolean bMethod = false;
        boolean bValues = false;
        boolean bSize = false;
        boolean bElement = false;
        boolean bPrimitive = false;
        boolean bArraySize = false;

        // determine the number of parameters for the method
        int count = this.argListTest.size();
        
        // set flags to enable controls based on the mode selection
        switch (mode) {
            default: // fall through...
            case None:
                break;
            case DataStruct :
                bClass = true;
                bMethod = true;
                bValues = true;
                bSize = true;
                bElement = true;
                break;
            case Simpleton :
                bClass = true;
                break;
            case Primitive :
                bValues = true;
                bPrimitive = true;
                break;
            case String :
                bValues = true;
                bSize = true;
                break;
            case Array :
                bValues = true;
                bArraySize = true;
                bElement = true;
                break;
        }
        
        // the radio buttons are always visible if there are any parameters
        this.typeDataStructRadioButton.setVisible(count > 0);
        this.typeSimpletonRadioButton.setVisible (count > 0);
        this.typePrimitiveRadioButton.setVisible (count > 0);
        this.typeStringRadioButton.setVisible    (count > 0);
        this.typeArrayRadioButton.setVisible     (count > 0);
        
        // the arg count is always visible (we use it to indicate no parameters, too)
        this.argCountLabel.setVisible(true);

        // the < and > keys are always visible if there is more than 1 parameter
        this.argPrevButton.setVisible(count > 1);
        this.argNextButton.setVisible(count > 1);

        // These are set based on the mode
        this.class1Label.setVisible          (count > 0 && bClass);
        this.class1ComboBox.setVisible       (count > 0 && bClass);

        this.method1Label.setVisible         (count > 0 && bMethod);
        this.method1ComboBox.setVisible      (count > 0 && bMethod);

        this.valueSelectLabel.setVisible     (count > 0 && bValues);
        this.valueSelectComboBox.setVisible  (count > 0 && bValues);

        this.sizeSelectLabel.setVisible      (count > 0 && bSize);
        this.sizeSelectComboBox.setVisible   (count > 0 && bSize);

        this.elementTypeLabel.setVisible     (count > 0 && bElement);
        this.elementTypeComboBox.setVisible  (count > 0 && bElement);

        this.primitiveTypeLabel.setVisible   (count > 0 && bPrimitive);
        this.primitiveTypeComboBox.setVisible(count > 0 && bPrimitive);

        this.arraySizeLabel.setVisible       (count > 0 && bArraySize);
        this.arraySizeComboBox.setVisible    (count > 0 && bArraySize);

        debugDisplayMessage(DebugType.Exit, "setGUIParamSetup_Visible");
    }

    /**
     * This sets the custom argument panel and radio button selections based
     * on the type of test method parameter found.
     * 
     * This should be called when the parameter mode changes, which can be directly
     * from the radiobutton selection or when the parameter selection is changed
     * with the Prev/Next buttons, or the method has changed.
     * 
     * @param mode - the mode selection being set
     * 
     * This assumes the following has been setup prior to the call:
     *  argOffset   - specifies the parameter being examined
     *  argListTest - the list of parameters for the Test method
     *                (uses the data type of the argument value)
     */
    private void setGUIParamSetup_Mode(ArgSelect.ParamMode mode) {
        if (this.argListTest == null || this.argListTest.isEmpty()) {
            debugDisplayMessage (DebugType.EntryExit, "setGUIParamSetup_Mode: " + mode + " - argList empty");
            setGUIParamSetup_Visible(ArgSelect.ParamMode.None);
            return;
        }
        if (this.argOffset >= this.argListTest.size()) {
            debugDisplayMessage (DebugType.EntryExit, "setGUIParamSetup_Mode: " + mode + " - offset " +
                                 this.argOffset + "exceeds size " + this.argListTest.size());
            return;
        }
        
        // save the selection
        this.argListTest.get(this.argOffset).mode = mode;
        debugDisplayMessage (DebugType.Entry, "setGUIParamSetup_Mode: " + mode + " - offset " + this.argOffset);

        // setup which controls on this panel are visible
        setGUIParamSetup_Visible(mode);

        // set the radio button selections
        switch (mode) {
            case Primitive :
                this.typePrimitiveRadioButton.setSelected(true);
                this.typeStringRadioButton.setSelected(false);
                this.typeSimpletonRadioButton.setSelected(false);
                this.typeArrayRadioButton.setSelected(false);
                this.typeDataStructRadioButton.setSelected(false);
                break;
            case String :
                this.typePrimitiveRadioButton.setSelected(false);
                this.typeStringRadioButton.setSelected(true);
                this.typeSimpletonRadioButton.setSelected(false);
                this.typeArrayRadioButton.setSelected(false);
                this.typeDataStructRadioButton.setSelected(false);
                break;
            case Simpleton :
                this.typePrimitiveRadioButton.setSelected(false);
                this.typeStringRadioButton.setSelected(false);
                this.typeSimpletonRadioButton.setSelected(true);
                this.typeArrayRadioButton.setSelected(false);
                this.typeDataStructRadioButton.setSelected(false);
                break;
            case Array :
                this.typePrimitiveRadioButton.setSelected(false);
                this.typeStringRadioButton.setSelected(false);
                this.typeSimpletonRadioButton.setSelected(false);
                this.typeArrayRadioButton.setSelected(true);
                this.typeDataStructRadioButton.setSelected(false);
                break;
            default: // fall through...
            case DataStruct :
                this.typePrimitiveRadioButton.setSelected(false);
                this.typeStringRadioButton.setSelected(false);
                this.typeSimpletonRadioButton.setSelected(false);
                this.typeArrayRadioButton.setSelected(false);
                this.typeDataStructRadioButton.setSelected(true);
                break;
        }
        debugDisplayMessage (DebugType.Exit, "setGUIParamSetup_Mode");
    }

    /**
     * This enables/disables the radiobutton selections for the parameter
     * mode selection. (the selections are greyed out when disabled).
     * 
     * This should be called when the parameter type changes, which can be when
     * the parameter selection is changed with the Prev/Next buttons, or the
     * method has changed (but not when the mode selection has changed).
     * 
     * @param argType - the data type for the current parameter selection (null if no parameters)
     * 
     * This assumes the following has been setup prior to the call:
     *  argOffset   - specifies the parameter being examined
     *  argListTest - the list of parameters for the Test method
     *                (uses the data type of the argument value)
     */
    private void setGUIParamSetup_ModesEnabled(TypeReference argType) {
    	debugDisplayMessage(DebugType.Entry, "setGUIParamSetup_ModesEnabled: arg " + argOffset);
     	
    	// init all buttons to disabled
        boolean bDataStruct = false;
        boolean bSimpleton = false;
        boolean bPrimitive = false;
        boolean bString = false;
        boolean bArray = false;
    	
        // determine which selections are valid
    	if (argOffset==0 && !this.argListTest.isEmpty() && !this.argListTest.get(0).isStatic) {
            // this is the 'this' parameter; only DataStruct and Simpleton allowed
            debugDisplayMessage(DebugType.Normal, " - 'this' parameter");
            bDataStruct = true;
            bSimpleton = true;
        }
        else {
            // else, base selection on the parameter type
            if (argType == null) {
                debugDisplayMessage(DebugType.Normal, " - argType = null");
            }
            else {
                // get the mode selection as specified by the data type
                ArgSelect.ParamMode mode = getArgumentParamMode(argType);
                switch (mode) {
                    case Primitive :
                        bPrimitive = true;
                        break;
                    case String :
                        bString = true;
                        bPrimitive = true; // allow Primitive selection for String type
                        break;
                    case Array :
                        bArray = true;
                        break;
                    default:
                        // this is for other objects like com.cyberpointllc.stac.hashmap.HashMap
                        bDataStruct = true;
                        bSimpleton = true;
                        break;
                }

                // for Object types, allow DataStruct and Simpleton as well as Strings
                String dtype = getArgArrayDataType(argType, true);
                if (dtype.equals("Object")) {
                    bDataStruct = true;
                    bSimpleton = true;
                    bString = true;
                }
                debugDisplayMessage(DebugType.Normal, " - argType " + dtype + ": " +
                        bDataStruct + "," + bSimpleton + "," + bPrimitive + "," + bString + "," + bArray);
            }
        }

        // make the selections
        if (this.typeDataStructRadioButton.isEnabled() != bDataStruct)
            this.typeDataStructRadioButton.setEnabled(bDataStruct);
        if (this.typeSimpletonRadioButton.isEnabled() != bSimpleton)
            this.typeSimpletonRadioButton.setEnabled(bSimpleton);
        if (this.typePrimitiveRadioButton.isEnabled() != bPrimitive)
            this.typePrimitiveRadioButton.setEnabled(bPrimitive);
        if (this.typeStringRadioButton.isEnabled() != bString)
            this.typeStringRadioButton.setEnabled(bString);
        if (this.typeArrayRadioButton.isEnabled() != bArray)
            this.typeArrayRadioButton.setEnabled(bArray);

        debugDisplayMessage(DebugType.Exit, "setGUIParamSetup_ModesEnabled");
    }

    /**
     * This updates the argument selection number and data type in the
     * Parameter Setup section.
     * 
     * This should be called when the Next/Prev buttons have changed the
     * selection and when a Test method has been selected to initialize
     * it for the parameter list.
     * 
     * @param adjust - 0 to initialize, +/-1 to modify selection
     * 
     * This assumes the following has been setup prior to the call:
     *  argOffset   - specifies the parameter being examined
     *  argListTest - the list of parameters for the Test method
     *                (uses the data type of the argument value)
     */
    private void setGUIParamSetup_ArgSelect(int adjust) {
        // get the number of argument entries for the iterative method selection
        // and the current saved argument selection
        debugDisplayMessage(DebugType.Entry, "setGUIParamSetup_ArgSelect (" + adjust + ")");
        int count = this.argListTest.size();
        if (adjust != 0 && count > 1) {
            int prev = argOffset; // save previous value
            // update the argument selection index
            argOffset += adjust;
            if (argOffset >= count)
                argOffset = count - 1;
            if (argOffset < 0)
                argOffset = 0;
            if (argOffset == prev) {
                debugDisplayMessage(DebugType.Exit, "setGUIParamSetup_ArgSelect: no change");
                return;
            }
        }

        if (count == 0) {
            this.methodNameLabel.setText("");
            this.argCountLabel.setText("no parameters");
            debugDisplayMessage(DebugType.Exit, "setGUIParamSetup_ArgSelect: no parameters");
            return;
        }
        if (argOffset >= count || argOffset < 0) {
            debugDisplayMessage(DebugType.Warning, "setGUIParamSetup_ArgSelect: Invalid index: " + argOffset);
            debugDisplayMessage(DebugType.Exit, "setGUIParamSetup_ArgSelect");
            return;
        }

        // get the parameter data type to display
        TypeReference argType = this.argListTest.get(argOffset).argument;
        String dtype = getArgArrayDataType(argType, true);
        if (dtype.isEmpty())
            dtype = "null";

        this.methodNameLabel.setText("param = " + getArgArrayDataType(argType, true));
        this.argCountLabel.setText((argOffset+1) + " of " + count);
        debugDisplayMessage(DebugType.Normal, "setGUIParamSetup: param " + (argOffset+1) + " = " + dtype);
        
        // update the param info data
        setGUI_ParamInfoPanel(adjust);
        debugDisplayMessage(DebugType.Exit, "setGUIParamSetup_ArgSelect");
    }

    /**
     * This updates the GUI display for the Param Info Panel that displays the
     * parameter values for each viable Set for the Test and Iterative methods.
     * The user can scroll through each Set, displaying the list of parameter
     * types specified for each argument in the method.
     * 
     * @param adjust - the amount of change to make to the index selection (0 or +/-1)
     */
    private void setGUI_ParamInfoPanel(int adjust) {
        // init the parameter list text area
        this.paramListTextArea.setText("");
        String content = "";

        // show selected method name and parameters
        if (testMethodRadioButton.isSelected()) {

            // display the Test method name
            this.methodNameLabel.setText(this.setMethodNameTest);

            if (this.argListTest.isEmpty()) {
                this.noParamsLabel.setVisible(true);
                this.paramListScrollPane.setVisible(false);
            }
            else {
                this.noParamsLabel.setVisible(false);
                this.paramListScrollPane.setVisible(true);

                // build the text display from the list of parameters
                for (int argix = 0; argix < this.argListTest.size(); argix++) {
                    String param = getArgArrayDataType(this.argListTest.get(argix).argument, true);
                    content += "param " + (argix+1) + ": " + param + newLine;
                }
                this.paramListTextArea.setText(content);
            }
        }
        else {
            // verify the argList for Test has been constructed
            if (this.argListTest.isEmpty()) {
                this.methodNameLabel.setText("(no method defined)");
                this.noParamsLabel.setVisible(false);
                this.paramListScrollPane.setVisible(false);
                return;
            }

            // now get the list of parameters for the its Iterative method
            ArgSelect argSelect = this.argListTest.get(this.argOffset);
            ArrayList<TypeReference> iterList = argSelect.iterList;
            String methName = argSelect.method;
            String className = argSelect.cls;
            if (className.isEmpty() || methName.isEmpty()) {
                this.methodNameLabel.setText("(no method defined)");
                this.noParamsLabel.setVisible(false);
                this.paramListScrollPane.setVisible(false);
                return;
            }

            // display the selected Iter method name
            methName = Util.toDotName(className) + "." + Util.toRawMethodName(methName);
            this.methodNameLabel.setText(methName);

            // determine the number of parameters for the method
            if (iterList == null || iterList.isEmpty()) {
                this.noParamsLabel.setVisible(true);
                this.paramListScrollPane.setVisible(false);
                return;
            }

            this.noParamsLabel.setVisible(false);
            this.paramListScrollPane.setVisible(true);

            // build the Iter text display from the list of parameters in the
            // parameter selection made in the Parameter Setup section.
            // Note that this will only apply if a method is defined for it.
            for (int argix = 0; argix < iterList.size(); argix++) {
                String param = getArgArrayDataType(iterList.get(argix), true);
                content += "param " + (argix+1) + ": " + param + newLine;
            }
            this.paramListTextArea.setText(content);
        }
    }
    
    /**
     * returns true if the data type passed is a primitive.
     * @param argType - the data type to check
     * @return true if type is primitive
     */
    private boolean isPrimitiveType (TypeReference argType) {
        return argType == TypeReference.JavaLangBoolean
            || argType == TypeReference.JavaLangByte
            || argType == TypeReference.JavaLangShort
            || argType == TypeReference.JavaLangLong
            || argType == TypeReference.JavaLangInteger
            || argType == TypeReference.JavaLangCharacter
            || argType == TypeReference.JavaLangDouble
            || argType == TypeReference.JavaLangFloat;
    }
    
    /**
     * This returns the parameter mode selection for the specified arguemnt type.
     * 
     * Note that sometimes, Object gets recorded as <Application, Ljava/lang/Object>
     * instead of <Primordial, Ljava/lang/Object>, which causes it to not
     * match the value TypeReference.JavaLangObject. To check on this case
     * we use getArgArrayDataType() to convert the type to a String and check
     * it manually.
     * 
     * @param argType - the selected arguemnt type
     * @return the corresponding parameter mode selection
     */
    private ArgSelect.ParamMode getArgumentParamMode (TypeReference argType) {
        ArgSelect.ParamMode argMode;
        String pClass = getArgArrayDataType(argType, false);
        String primitive = Util.getWrapperName(pClass);
        if (primitive.equals("String")) // this doesn't count as a primitive
            primitive = "INVALID";
        
        if (argType.isArrayType()) {
            argMode = ArgSelect.ParamMode.Array;
        } else if (argType.isPrimitiveType() || isPrimitiveType(argType) || !primitive.equals("INVALID")) {
            argMode = ArgSelect.ParamMode.Primitive;
        } else if (argType == TypeReference.JavaLangString || pClass.equals("String")) {
            argMode = ArgSelect.ParamMode.String;
        } else if (argType == TypeReference.JavaLangObject || pClass.equals("Object")) {
            argMode = ArgSelect.ParamMode.String; // Object defaults to String
        } else if (pClass.matches("^.*?(List|Set|Collection|Queue|Tree|Vector|Stack|Heap|Map).*$")) {
            argMode = ArgSelect.ParamMode.DataStruct;
        }
        else {
            argMode = ArgSelect.ParamMode.Simpleton;
        }

        debugDisplayMessage(DebugType.Normal, "getArgumentParamMode: " + argType + " -> " + argMode);
        return argMode;
    }
    
    /**
     * this method generates the parameter list for the Test method.
     * This List consists of the argument entries, as well as parameters corresponding
     * to each of the GUI Parameter controls. These entries are initialized to
     * the preferred values, but are changeable by the GUI.
     */
    private void updateTestMethodArguments () {
        String testClass  = this.class2ComboBox.getSelectedItem().toString();
        String testMethod = this.method2ComboBox.getSelectedItem().toString();
        String paramClass  = (String) this.class1ComboBox.getSelectedItem();
        String paramMethod = (String) this.method1ComboBox.getSelectedItem();

        String methodName = testClass + "." + testMethod;
        this.setMethodNameTest = Util.toDotName(testClass) + "." + Util.toRawMethodName(testMethod);
        debugDisplayMessage(DebugType.Entry, "updateTestMethodArguments: " + methodName);

        // initialize test parameter list we are going to generate
        ArrayList<ArgSelect> argList = this.argListTest;
        argList.clear();
        this.argOffset = 0;     // init offset index to the begining

        // get the procedure for the Test method
        int size = 0;
        Procedure proc = null;
        IMethod imethod = null;
        if (testClass != null && testMethod != null)
            proc = Program.getProcedure(methodName);
        if (proc == null || proc.getIR() == null || proc.getIR().getMethod() == null) {
            debugDisplayMessage(DebugType.Warning, "updateTestMethodArguments: unable to retrieve Procedure");
        }
        else {
            imethod = proc.getIR().getMethod();
            size = imethod.getNumberOfParameters();
        }

        // create a List for the GUI parameter selections and initialize it
        for (int argix = 0; argix < size; argix++) {
            	
            if (imethod == null) {
                debugDisplayMessage(DebugType.Warning, "- Test P" + (argix+1) + ": method not found");
                continue;
            }

            // get the type specified for the method from the IR call
            TypeReference argType = imethod.getParameterType(argix);
            String dtype = getArgArrayDataType(argType, true);
            ArgSelect.ParamMode argMode = getArgumentParamMode(argType);
            debugDisplayMessage(DebugType.Argument, "- Test P" + (argix+1) + ": type = " + dtype + ", mode = " + argMode);
            
            // initialize the parameter values and save the argument value and mode
            ArgSelect param = new ArgSelect(argType, argMode, imethod.isStatic());
                
            // set the primitive types based on the argument type for the selected set
            // (eliminate any terminating array brackets on end of type)
            String elemType;
            String primType = dtype;
            if (primType.contains("["))
                primType = primType.substring(0, primType.indexOf("["));
            elemType = Util.cvtTypeRefToElementCbox(primType);
            primType = Util.cvtTypeRefToPrimitiveCbox(primType);
            if (!primType.isEmpty()) {
                param.primitive = primType;
                debugDisplayMessage(DebugType.Autoset, "- Test P" + (argix+1) + ": Primitive set to " + primType);
            }
            if (!elemType.isEmpty()) {
                param.element = elemType;
                debugDisplayMessage(DebugType.Autoset, "- Test P" + (argix+1) + ": Element set to " + elemType);
            }

            // Init the class and method selection for DataStruct and Simpleton types.
            if (argMode == ArgSelect.ParamMode.DataStruct || argMode == ArgSelect.ParamMode.Simpleton) {
                param.cls = paramClass;
                if (param.mode == ArgSelect.ParamMode.DataStruct)
                    param.method = paramMethod;
            }

            // add parameter info to the argList
            argList.add(param);

            // make any corrections to the class and method to get preferred types
            updateTestMethodParameter(argix, "INIT");
        }

        // when done, reset the parameter selection to the 1st entry and setup
        // the GUI class and method selections for it.
        if (size == 0) {
            // if no params, make sure we update the display to indicate this
            setGUIParamSetup_Visible(ArgSelect.ParamMode.None);
            setGUIParamSetup_ArgSelect(0);
        }
        else {
            // generate the class and method selections for the comboboxes.
            // these are taken from the 1st parameter, since that is the one
            // that will be displayed after this call because argOffset is set to 0.
            ArgSelect first = argList.get(0);
            generateIterClassList(first.argument, first.cls, first.method);
            setGUIParamSetup_FromArg(first);

            // update the arg count display, the radiobutton selection and the
            // argument selection mode.
            setGUIParamSetup_ArgSelect(0);
            setGUIParamSetup_Mode(first.mode);
            setGUIParamSetup_ModesEnabled(first.argument);
        }

        // update the set list information
        setGUI_ParamInfoPanel(0);
        debugDisplayMessage(DebugType.Exit, "updateTestMethodArguments");
    }

    /**
     * updates the specified Test parameter selection.
     * 
     * assumes the 'mode', 'cls' and 'method' elements have already been
     * set in the selected parameter entry of the argListTest array and that
     * the 'argument' element has been initialized to the argument data type
     * by updateTestMethodArguments() prior to this call.
     * 
     * @param argix - the Test parameter index to update
     * @param paramChange - indicates which of the Parameter Setup entities was changed
     */
    private void updateTestMethodParameter (int argix, String paramChange) {
        if (argix >= this.argListTest.size()) {
            debugDisplayMessage(DebugType.Warning, "updateTestMethodParameter: index " + (argix + 1) + " exceeds number of Test parameters " + this.argListTest.size());
            return;
        }

        debugDisplayMessage(DebugType.Entry, "updateTestMethodParameter: P" + (argix+1) + " -> " + paramChange);
        if (this.argListTest.isEmpty()) {
            debugDisplayMessage(DebugType.Exit, "updateTestMethodParameter: no Test parameters");
            return;
        }

        ArgSelect argSelect = this.argListTest.get(argix);

        // determine what actions if any we will need to take
        boolean bSetClass = false;
        boolean bSetMethod = false;
        switch (paramChange) {
            case "typeStringRadioButton":
                // disable the method selection (not needed in this mode).
                argSelect.method = "";
                break;
                
            case "typeSimpletonRadioButton":
                // disable the method selection (not needed in this mode) and
                // re-generate the class selection only if none is selected.
                // (can be caused by going from a String type to Simpleton type)
                argSelect.method = "";
                if (argSelect.cls.isEmpty())
                    bSetClass = true;
                break;
                
            case "method1ComboBox":
                // method changed - update the parameter list for the method
                // in the event the method has changed.
//                if (argSelect.mode == ArgSelect.ParamMode.DataStruct ||
//                    argSelect.mode == ArgSelect.ParamMode.Simpleton) {
//                    argSelect.iterList = updateIterMethodArguments (argSelect.cls, argSelect.method);
//                }
                break;
                
            case "class1ComboBox":
                // this one needs to setup the method selections to choose from
                // and to pick a preferred one if it exists (defaults to 1st entry
                // if preferred not found).
                // Not necessary if not DataStruct mode, since method is not specified
                // in that case.
                if (argSelect.mode == ArgSelect.ParamMode.DataStruct) {
                    bSetMethod = true;
                }
                break;

            case "INIT":
                // this is the case when we are initially setting up the Test Parameters
                if (argSelect.mode == ArgSelect.ParamMode.DataStruct ||
                    argSelect.mode == ArgSelect.ParamMode.Simpleton) {
                    bSetClass = true;
                }
                break;

            case "typeDataStructRadioButton":
                // Going to Data Struct selection means we enable the method selection,
                // which means we have to generate a method list selection for the
                // current class selection & try to find a preferred entry for it.
                // Then it will have to pich the appropriate method, the same as
                // the case for changing the class1ComboBox.
                bSetClass = true;
                break;

            default:
                break;
        }

        if (bSetClass) {
            // if no class selection is defined, set it to the Test class selection
            if (argSelect.cls.isEmpty()) {
                argSelect.cls = (String)this.class2ComboBox.getSelectedItem();
                if (argSelect.cls == null) {
                    argSelect.cls = "";
                    debugDisplayMessage(DebugType.Error, "- Test P" + (argix+1) + ": null class selection");
                    return;
                }
                debugDisplayMessage(DebugType.Argument, "- Test P" + (argix+1) + ": cls empty - setting from comboBox");
            }

            // the current argument type for a DataStruct should be a class
            // reference. we will use this to help find a suitable preferred class.
            String prevClass = argSelect.cls;
            String paramClass = null;
            TypeReference argType = argSelect.argument;
            if (argType == null)
                debugDisplayMessage(DebugType.Warning, "- Test P" + (argix+1) + ": class = " + argSelect.cls + " -> argType NULL, should be a class type");
            else {
                paramClass = argType.toString();
                debugDisplayMessage(DebugType.Argument, "- Test P" + (argix+1) + ": class = " + argSelect.cls + " -> argType = " + paramClass);
            }
            debugDisplayMessage(DebugType.Argument, "- Test P" + (argix+1) + ": method = " + argSelect.method);
                
            // parameter is a class - use it as the class selection
            String pClass = getArgArrayDataType(argType, true);
            if (argType != null && argType.isClassType() && !pClass.equals("Object")) {
                paramClass = "L" + pClass;
                if (paramClass.contains("."))
                    paramClass = paramClass.replace(".", "/");
                debugDisplayMessage(DebugType.Autoset, "- Test P" + (argix+1) + "-> parameter used for class: " + paramClass);
                argSelect.cls = paramClass;
            }

            // replace class with preferred type (HashMap, ArrayList, etc.)
            // if class is a form of that type.
            String subClass = findPreferredClass(argSelect.cls);
            if (subClass != null && !subClass.equals(argSelect.cls)) {
                // if class changed, update the parameter selection
                if (!subClass.isEmpty() && !subClass.equals(prevClass)) {
                    debugDisplayMessage(DebugType.Autoset, "- Test P" + (argix+1) + ": class changed to " + subClass);
                    argSelect.cls = subClass;
                }
            }
            
            // now let's filter the class selection down to just the specified
            // class and its subclasses only to minimize what the user has to
            // select from. Note that this will also update the method list and
            // obtain a new method selection.
            //
            // NOTE: We do have to make sure to use the original class selection
            // and not the preferred one since the preferred one is a subClass of
            // the original and this function will filter so only subClasses of
            // the original selection are displayed in the list.
            generateIterClassList(argType, argSelect.cls, null);

            // if the class was changed and this is a DataStruct, we need to
            // re-generate the methods and select one.
            if (argSelect.mode == ArgSelect.ParamMode.DataStruct) {
                String method = (String)this.method1ComboBox.getSelectedItem();
                if (method != null) {
                    debugDisplayMessage(DebugType.Autoset, "- Test P" + (argix+1) + ": method changed to " + method);
                    argSelect.method = method;
                }
            }
        }
        
        if (bSetMethod) {
            // generate the methods list for the class and select the preferred one
            String method = generateIterMethodList(argSelect.cls, null);
            if (method != null && !method.equals(argSelect.method)) {
                debugDisplayMessage(DebugType.Autoset, "- Test P" + (argix+1) + ": method changed to " + method);
                argSelect.method = method;
            }
        }

        // now that we have the appropriate class and method selected,
        // get the parameter list for that method
        if (argSelect.mode == ArgSelect.ParamMode.DataStruct)
            argSelect.iterList = updateIterMethodArguments (argSelect.cls, argSelect.method);

        debugDisplayMessage(DebugType.Exit, "updateTestMethodParameter");
    }

    /**
     * this method generates the parameter list for the Iterative method.
     * 
     * @param paramClass  - the class selection for the parameter
     * @param paramMethod - the method selection for the parameter
     */
    private ArrayList<TypeReference> updateIterMethodArguments (String paramClass, String paramMethod) {

        // get the location to store the argument list for the method
        ArrayList<TypeReference> argList = new ArrayList<>();
        //argList.clear();
        debugDisplayMessage(DebugType.Entry, "updateIterMethodArguments: " + paramClass + "." + paramMethod);

        // get the procedure for the Test method
        int argix = 0;
        Procedure proc = null;
        if (paramClass != null && paramMethod != null)
            proc = Program.getProcedure(paramClass + "." + paramMethod);
        if (proc == null) {
            debugDisplayMessage(DebugType.Warning, "updateIterMethodArguments: unable to retrieve Procedure, retrieving from method name");
            ArrayList<String> argArray = getArgListFromMethodName (paramMethod);
            for (argix = 0; argix < argArray.size(); argix++) {
                TypeReference argType = getArgTypeFromString (argArray.get(argix));
                argList.add(argType);
                debugDisplayMessage(DebugType.Argument, "  - Iter P" + (argix+1) + ": " + getArgArrayDataType(argType, true));
            }
        }
        else {
            // get the list of types for each argument for the method from the IR call
            int size = proc.getIR().getMethod().getNumberOfParameters();
            for (argix = 0; argix < size; argix++) {
                if (proc.getIR().getMethod() != null) {
                    TypeReference argType = proc.getIR().getMethod().getParameterType(argix);
                    argList.add(argType);
                    debugDisplayMessage(DebugType.Argument, "  - Iter P" + (argix+1) + ": " + getArgArrayDataType(argType, true));
                }
            }
        }
        
        debugDisplayMessage(DebugType.Exit, "pdateIterMethodArguments: " + argix + " arguments defined");
        return argList;
    }

    /**
     * This saves the min and max limit values for the currently selected data type
     * from the gui controls to the class parameters.
     * 
     * @param configType - configuration selection (WCA or SideChannel)
     * @param dtype      - the data type selection
     */
    private void saveSymbolValues (Options.ConfigType configType, String dtype) {
        String min, max;
        if (configType == Options.ConfigType.WCA) {
            min = wcaSymMinTextField.getText();
            max = wcaSymMaxTextField.getText();
            wcaSymLimits.setLimits(dtype, min, max);
        }
        else {
            min = scSymMinTextField.getText();
            max = scSymMaxTextField.getText();
            scSymLimits.setLimits(dtype, min, max);
        }
    }

    /**
     * formats an individual setting value entry for placing in a file
     * 
     * @param tag - the tag name for the value
     * @param value - the value
     * @return the formatted String entry to place in the file
     */
    private String saveSetting (String tag, String value) {
        String padding = "                    ".substring(tag.length());
        if (padding.isEmpty())
            padding = "  ";
        return "<" + tag + ">" + padding + value + newLine;
    }
    
    /**
     * formats an individual indexed parameter setting value entry for placing in a file
     * 
     * @param index - the index of the parameter
     * @param tag - the tag name for the parameter value
     * @param value - the parameter value
     * @return the formatted String entry to place in the file
     */
    private String saveSetting (int index, String tag, String value) {
        return "    P" + index + " " + saveSetting (tag, value);
    }
    
    /**
     * This generates a file that contains the settings at the time of a launch.
     * This will allow us to be able to load the file at a later time (or by
     * someone else) to attempt to duplicate the build.
     */
    private void saveSettingsFile () {
        debugDisplayMessage(DebugType.Entry, "saveSettingsFile");
        String content = "";
        
        // Miscellaneous settings
        content += "# Miscellaneous settings" + newLine;
        content += saveSetting ("prjName"  , this.projectName);
        content += saveSetting ("spfMode"  , WCAFrame.spfMode.toString());
        content += saveSetting ("useCloud" , this.useCloudCheckBox.isSelected() ? "yes" : "no");
        
        // Cloud Setup
        content += newLine + "# Cloud Setup" + newLine;
        content += saveSetting ("cloudJobName"  , this.cloudNameTextField.getText());
        content += saveSetting ("cloudExecutors", this.cloudExecutorsTextField.getText());
        content += saveSetting ("cloudInitDepth", this.cloudInitialDepthTextField.getText());
            
        // Test Setup
        content += newLine + "# Test Setup" + newLine;
        content += saveSetting ("class2"  , (String)this.class2ComboBox.getSelectedItem());
        content += saveSetting ("method2" , (String)this.method2ComboBox.getSelectedItem());
        
        // Test Parameters
        List <ArgSelect> argList = this.argListTest;
        content += saveSetting ("paramCount"     , ((Integer)argList.size()).toString());
        for (int ix = 0; ix < argList.size(); ix++) {
            content += saveSetting (ix, "argMode"     , argList.get(ix).mode.toString());
            content += saveSetting (ix, "argClass"    , argList.get(ix).cls);
            content += saveSetting (ix, "argMethod"   , argList.get(ix).method);
            content += saveSetting (ix, "argValue"    , argList.get(ix).value);
            content += saveSetting (ix, "argSize"     , argList.get(ix).size);
            content += saveSetting (ix, "argElement"  , argList.get(ix).element);
            content += saveSetting (ix, "argPrimitive", argList.get(ix).primitive);
            content += saveSetting (ix, "argArray"    , argList.get(ix).arraySize);
            content += saveSetting (ix, "argIsStatic" , (argList.get(ix).isStatic ? "yes" : "no"));
        }
        
        // Configuration Setup - WCA
        content += newLine + "# WCA Configuration" + newLine;
        content += saveSetting ("wcaSolver"      , (String)this.wcaSolverComboBox.getSelectedItem());
        content += saveSetting ("wcaBvlen"       , ((Integer)this.wcaBvlenSpinner.getValue()).toString());
        content += saveSetting ("wcaInputMax"    , ((Integer)this.wcaInputMaxSpinner.getValue()).toString());
        content += saveSetting ("wcaPolicy"      , ((Integer)this.wcaPolicySpinner.getValue()).toString());
        content += saveSetting ("wcaPolicyRange" , this.wcaPolicyRangeCheckBox.isSelected() ? "yes" : "no");
        content += saveSetting ("wcaPolicyEnd"   , ((Integer)this.wcaPolicyEndSpinner.getValue()).toString());
        content += saveSetting ("wcaHistory"     , ((Integer)this.wcaHistorySpinner.getValue()).toString());
        content += saveSetting ("wcaHistoryRange", this.wcaHistoryRangeCheckBox.isSelected() ? "yes" : "no");
        content += saveSetting ("wcaHistoryEnd"  , ((Integer)this.wcaHistoryEndSpinner.getValue()).toString());
        content += saveSetting ("wcaCostModel"   , (String) this.wcaCostModelComboBox.getSelectedItem());
        content += saveSetting ("wcaHeuristic"   , this.wcaHeuristicCheckBox.isSelected() ? "yes" : "no");
        content += saveSetting ("wcaDebug"       , this.wcaDebugCheckBox.isSelected() ? "yes" : "no");

        content += saveSetting ("wcaCharMin"     , wcaSymLimits.getLimits("char").min);
        content += saveSetting ("wcaCharMax"     , wcaSymLimits.getLimits("char").max);
        content += saveSetting ("wcaByteMin"     , wcaSymLimits.getLimits("byte").min);
        content += saveSetting ("wcaByteMax"     , wcaSymLimits.getLimits("byte").max);
        content += saveSetting ("wcaShortMin"    , wcaSymLimits.getLimits("short").min);
        content += saveSetting ("wcaShortMax"    , wcaSymLimits.getLimits("short").max);
        content += saveSetting ("wcaIntMin"      , wcaSymLimits.getLimits("int").min);
        content += saveSetting ("wcaIntMax"      , wcaSymLimits.getLimits("int").max);
        content += saveSetting ("wcaLongMin"     , wcaSymLimits.getLimits("long").min);
        content += saveSetting ("wcaLongMax"     , wcaSymLimits.getLimits("long").max);
        content += saveSetting ("wcaDoubleMin"   , wcaSymLimits.getLimits("double").min);
        content += saveSetting ("wcaDoubleMax"   , wcaSymLimits.getLimits("double").max);

        // Configuration Setup - Side Channel
        content += newLine + "# Side Channel Configuration" + newLine;
        content += saveSetting ("scSolver"       , (String)this.scSolverComboBox.getSelectedItem());
        content += saveSetting ("scBvlen"        , ((Integer)this.scBvlenSpinner.getValue()).toString());
        content += saveSetting ("scType"         , (String)this.scTypeComboBox.getSelectedItem());
        content += saveSetting ("scDebug"        , this.scDebugCheckBox.isSelected() ? "yes" : "no");

        content += saveSetting ("scCharMin"     , scSymLimits.getLimits("char").min);
        content += saveSetting ("scCharMax"     , scSymLimits.getLimits("char").max);
        content += saveSetting ("scByteMin"     , scSymLimits.getLimits("byte").min);
        content += saveSetting ("scByteMax"     , scSymLimits.getLimits("byte").max);
        content += saveSetting ("scShortMin"    , scSymLimits.getLimits("short").min);
        content += saveSetting ("scShortMax"    , scSymLimits.getLimits("short").max);
        content += saveSetting ("scIntMin"      , scSymLimits.getLimits("int").min);
        content += saveSetting ("scIntMax"      , scSymLimits.getLimits("int").max);
        content += saveSetting ("scLongMin"     , scSymLimits.getLimits("long").min);
        content += saveSetting ("scLongMax"     , scSymLimits.getLimits("long").max);
        content += saveSetting ("scDoubleMin"   , scSymLimits.getLimits("double").min);
        content += saveSetting ("scDoubleMax"   , scSymLimits.getLimits("double").max);
        
        // save content to file
        if (!content.isEmpty()) {
            String fname = getSPFDriverPath() + "janalyzer.setup";
            File file = new File(fname);
            file.delete();
            try {
                FileUtils.writeStringToFile(file, content, "UTF-8");
                statusDisplayMessage(DebugType.Success, "Settings file created");
            } catch (IOException ex) {
                statusDisplayMessage(DebugType.Error, "Writing to file: " + fname);
            }
        }
        debugDisplayMessage(DebugType.Exit, "saveSettingsFile");
    }

    private class SettingsException extends Exception {
        public SettingsException (String msg) {
            super(msg);
        }
    }

    private void setComboBoxEntry (JComboBox combobox, String value, String name) throws SettingsException {
        if (!value.isEmpty()) {
            combobox.setSelectedItem(value);
            String check = (String)combobox.getSelectedItem();
            if (check == null || !check.equals(value)) {
                throw new SettingsException("Settings invalid entry " + name + ": " + value);
            }
        }
    }
    
    /**
     * This reads a settings file and sets up the GUI selections and argListTest from them.
     * It will also set the project name and will set the parameter's class and method
     * selections based on these settings. It is assumed that the call graph that is
     * loaded prior to entry matches what was valid for the loaded file, otherwise
     * there may be some errors in setting the class and method selections.
     */
    private void loadSettingsFile (String fname) {
        statusDisplayMessage(DebugType.Normal, "Loading Settings File");
        debugDisplayMessage(DebugType.Entry, "loadSettingsFile: " + fname);

        // read content from file
        String content;
        File file = new File(fname);
        try {
            content = FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException ex) {
            statusDisplayMessage(DebugType.ErrorExit, "Reading from file: " + fname);
            return;
        }

        // extract contents into settings structure
        SPFSettings settings;
        try {
            settings = new SPFSettings(content);
        } catch (SPFSettings.SettingsException ex) {
            statusDisplayMessage(DebugType.ErrorExit, ex.getMessage());
            return;
        }

        // SUCCESS! - now setup GUI...

        // setup the misc settings
        prjnameTextField.setText(settings.prjName);
        useCloudCheckBox.setSelected("yes".equals(settings.useCloud));
        cloudNameTextField.setText(settings.cloudJobName);
        cloudExecutorsTextField.setText(settings.cloudExecutors);
        cloudInitialDepthTextField.setText(settings.cloudInitDepth);

        // copy all of the limit values over
        wcaSymLimits.copyLimits(settings.wcaSymLimits);
        scSymLimits.copyLimits(settings.scSymLimits);
        
        // init argOffset to 1st param entry
        argOffset = 0;
        argListTest.clear();
        
        // Setup the Test Class and Method selections...
        // (note that the ActionPerformed callbacks are disabled during this call,
        // so we have to manually call any actions we need)
        try {
            setComboBoxEntry (class2ComboBox , settings.class2 , "Test cls");
            generateTestMethodList(); // generate the test method list
            setComboBoxEntry (method2ComboBox, settings.method2, "Test method");
        } catch (SettingsException ex) {
            statusDisplayMessage(DebugType.Error, ex.getMessage());
        }

        // update the argument selections (this will also generate the IterClassList
//        updateTestMethodArguments();

        // get the test class info from wala
        Procedure proc = Program.getProcedure(settings.class2 + "." + settings.method2);
        if (proc == null || proc.getIR() == null || proc.getIR().getMethod() == null) {
            statusDisplayMessage(DebugType.ErrorExit, "Settings invalid: unable to retrieve Test method Procedure");
            return;
        }
        IMethod imethod = proc.getIR().getMethod();
        int count = 0;
        if (imethod != null) {
            count = imethod.getNumberOfParameters();

            // make sure the Test param count matches
            if (count != settings.argListTest.size()) {
                statusDisplayMessage(DebugType.ErrorExit, "Settings param count mismatch: " + settings.argListTest.size() + " != " + count);
                return;
            }
            // now we need to get the argument and iterParam values for the entry
            for (int ix = 0; ix < count; ix++) {
                ArgSelect setparm = settings.argListTest.get(ix);
                debugDisplayMessage(DebugType.Argument, "- Settings P" + (ix+1) + ": mode      = " + setparm.mode);
                debugDisplayMessage(DebugType.Argument, "- Settings P" + (ix+1) + ": cls       = " + setparm.cls);
                debugDisplayMessage(DebugType.Argument, "- Settings P" + (ix+1) + ": method    = " + setparm.method);
                debugDisplayMessage(DebugType.Argument, "- Settings P" + (ix+1) + ": value     = " + setparm.value);
                debugDisplayMessage(DebugType.Argument, "- Settings P" + (ix+1) + ": size      = " + setparm.size);
                debugDisplayMessage(DebugType.Argument, "- Settings P" + (ix+1) + ": element   = " + setparm.element);
                debugDisplayMessage(DebugType.Argument, "- Settings P" + (ix+1) + ": primitive = " + setparm.primitive);
                debugDisplayMessage(DebugType.Argument, "- Settings P" + (ix+1) + ": arraySize = " + setparm.arraySize);
                debugDisplayMessage(DebugType.Argument, "- Settings P" + (ix+1) + ": isStatic  = " + setparm.isStatic);

                // get the type specified for the method from the IR call
                setparm.argument = imethod.getParameterType(ix);
                
                String dtype = getArgArrayDataType(setparm.argument, true);
                ArgSelect.ParamMode argMode = getArgumentParamMode(setparm.argument);
                debugDisplayMessage(DebugType.Argument, "- Settings P" + (ix+1) + ": type = " + dtype + ", mode = " + argMode);

                // need to set the iterList entries for the iter method selection
                setparm.iterList = new ArrayList<>();
                if (argMode == ArgSelect.ParamMode.DataStruct) {
                    if (setparm.cls.isEmpty() || setparm.method.isEmpty()) {
                        statusDisplayMessage(DebugType.ErrorExit, "Settings P" + (ix+1) + ": is missing either cls or method");
                        return;
                    }
                    setparm.iterList = updateIterMethodArguments (setparm.cls, setparm.method);
                }
            
                // verify some settings are correct
                if (imethod.isStatic() != setparm.isStatic) {
                    statusDisplayMessage(DebugType.ErrorExit, "Settings P" + (ix+1) + ": isStatic = " + setparm.isStatic);
                    return;
                }

                // add parameter info to the argList
                argListTest.add(setparm);
            }
        }
        
        // Next setup the 1st Parameter selections...
        ArgSelect param = settings.argListTest.get(0);
        try {
            setComboBoxEntry (class1ComboBox        , param.cls      , "P0 cls");
            generateIterMethodList(param.cls, null); // generate list for param methods
                
            setComboBoxEntry (method1ComboBox       , param.method   , "P0 method");
            updateTestMethodParameter(argOffset, "class1ComboBox");

            setComboBoxEntry (valueSelectComboBox   , param.value    , "P0 value");
            setComboBoxEntry (sizeSelectComboBox    , param.size     , "P0 size");
            setComboBoxEntry (elementTypeComboBox   , param.element  , "P0 element");
            setComboBoxEntry (primitiveTypeComboBox , param.primitive, "P0 primitive");
            setComboBoxEntry (arraySizeComboBox     , param.arraySize, "P0 arraySize");
        } catch (SettingsException ex) {
            statusDisplayMessage(DebugType.Error, ex.getMessage());
        }

        // make sure the GUI gets updated
        if (count == 0) {
            // if no params, make sure we update the display to indicate this
            setGUIParamSetup_Visible(ArgSelect.ParamMode.None);
            setGUIParamSetup_ArgSelect(0);
        }
        else {
            // generate the class and method selections for the comboboxes.
            // these are taken from the 1st parameter, since that is the one
            // that will be displayed after this call because argOffset is set to 0.
            ArgSelect first = argListTest.get(0);
            generateIterClassList(first.argument, first.cls, first.method);
            setGUIParamSetup_FromArg(first);
            setGUIParamSetup_ArgSelect(0);
            setGUIParamSetup_Mode(first.mode);
            setGUIParamSetup_ModesEnabled(first.argument);
        }
        setGUI_ParamInfoPanel(0);
        
        // now set all of the WCA config settings
        // comboboxes
        try {
            setComboBoxEntry (wcaSolverComboBox     , settings.wcaSolver   , "wcaSolver");
            setComboBoxEntry (wcaCostModelComboBox  , settings.wcaCostModel, "wcaCostModel");
        } catch (SettingsException ex) {
            statusDisplayMessage(DebugType.Error, ex.getMessage());
        }
        // spinners
        wcaBvlenSpinner     .setValue(Integer.parseInt(settings.wcaBvlen)); 
        wcaInputMaxSpinner  .setValue(Integer.parseInt(settings.wcaInputMax)); 
        wcaPolicySpinner    .setValue(Integer.parseInt(settings.wcaPolicy)); 
        wcaPolicyEndSpinner .setValue(Integer.parseInt(settings.wcaPolicyEnd)); 
        wcaHistorySpinner   .setValue(Integer.parseInt(settings.wcaHistory)); 
        wcaHistoryEndSpinner.setValue(Integer.parseInt(settings.wcaHistoryEnd)); 
        // check boxes
        wcaPolicyRangeCheckBox .setSelected("yes".equals(settings.wcaPolicyRange));
        wcaHistoryRangeCheckBox.setSelected("yes".equals(settings.wcaHistoryRange));
        wcaHeuristicCheckBox   .setSelected("yes".equals(settings.wcaHeuristic));
        wcaDebugCheckBox       .setSelected("yes".equals(settings.wcaDebug));
        
        // now set all of the SC config settings
        // comboboxes
        try {
            setComboBoxEntry (scSolverComboBox, settings.scSolver, "scSolver");
            setComboBoxEntry (scTypeComboBox  , settings.scType  , "scType");
        } catch (SettingsException ex) {
            statusDisplayMessage(DebugType.Error, ex.getMessage());
        }
        // spinners
        scBvlenSpinner.setValue(Integer.parseInt(settings.scBvlen)); 
        // check boxes
        scDebugCheckBox.setSelected("yes".equals(settings.scDebug));
        
        // set the current limit selections
        String dtype = (String)wcaSymTypeComboBox.getSelectedItem();
        if (dtype != null) {
            wcaSymMinTextField.setText(wcaSymLimits.getLimits(dtype).min);
            wcaSymMaxTextField.setText(wcaSymLimits.getLimits(dtype).max);
        }
        dtype = (String)scSymTypeComboBox.getSelectedItem();
        if (dtype != null) {
            scSymMinTextField.setText(scSymLimits.getLimits(dtype).min);
            scSymMaxTextField.setText(scSymLimits.getLimits(dtype).max);
        }
        
        // should be good to go now!
        debugDisplayMessage(DebugType.Exit, "loadSettingsFile");
    }
    
    /**
     * creates the classpath from the application & library lists for SPF.
     * (note: uses a ',' separator between entries instead of the usual ':'.
     * 
     * @param separator - the separator char to use
     * @param bFlatpath - true if don't use paths (for cloud)
     * 
     * @return the classpath
     */
    private String createClasspath (String separator, boolean bFlatpath) {
        String classpath = "";
        DefaultListModel appList = configInfo.getList(ConfigInfo.ListType.application);
        if (!appList.isEmpty()) {
            for (int ix = 0; ix < appList.size(); ix++) {
                String entry = (String)appList.get(ix);
                if (bFlatpath) {
                    int offset = entry.lastIndexOf('/');
                    if (offset >= 0)
                        entry = entry.substring(offset+1);
                }
                classpath += separator + entry;
            }
        }
        DefaultListModel libList = configInfo.getList(ConfigInfo.ListType.libraries);
        if (!libList.isEmpty()) {
            for (int ix = 0; ix < libList.size(); ix++) {
                String entry = (String)libList.get(ix);
                // isolate just the name of the jar file
                String jarname = entry;
                int offset = jarname.lastIndexOf('/');
                if (offset >= 0)
                    jarname = jarname.substring(offset+1);
                // skip the rt.jar file
                if (jarname.equals("rt.jar"))
                    continue;

                if (bFlatpath)
                    entry = jarname;
                classpath += separator + entry;
            }
        }
        // eliminate initial ":" from classpath
        if (!classpath.isEmpty())
            classpath = classpath.substring(1);

        return classpath;
    }
    
    /**
     * creates the Options class to pass to drivergen for generating the driver files.
     * It uses the current user settings from the GUI and the argListTest list of
     * parameters for the Test method.
     *
     * This requires the following be setup prior to running (other than GUI controls):
     * - argListTest - for determining the values for each Test method parameter
     * - projectName - to set the name of the driver
     * 
     * @param configType - the type of configuration (WCA or SideChannel)
     * @param bCheck - true if don't output debug msgs since this is just for verifying changes
     * @return the Options structure (null if error)
     */
    private Options createOptions(Options.ConfigType configType, boolean bCheck) {
        Options options = new Options();
        options.configType = configType;

        if (this.class2ComboBox.getSelectedItem() == null ||
            this.method2ComboBox.getSelectedItem() == null) {
            statusDisplayMessage(DebugType.ErrorExit, "Invalid Test Class or Method");
            return null;
        }
        
        debugDisplayMessage(DebugType.Entry, "createOptions: " + bCheck);

        options.projectName = projectName;
        String drivername = Util.getDriverFileName(projectName);
        drivername = drivername.substring(0, drivername.lastIndexOf('.'));

        // the spf classpath uses comma seperator instead of colon & preceeds this
        // with the path to the library (seperated by a colon). Note that the cloud
        // does NOT need this initial path since it has no dir structure.
        // <base_dir>bin:
        boolean bCloud = useCloudCheckBox.isSelected();
        if (bCloud) {
            options.baseDir = "";
            options.srcDir = "";
            options.outDir = "";
            options.jarsclasspath = createClasspath(",", true);
            // the cloud also needs to have the driver jar file in the classpath
            options.jarsclasspath += "," + drivername + ".jar";
        }
        else {
            options.baseDir = getSPFBasePath();
            options.srcDir = getProjRootPath() + "decompiler/src/";
            options.outDir = getSPFBasePath() + "results/";
            options.jarsclasspath = options.baseDir + "bin:";
            options.jarsclasspath += createClasspath(",", false);
        }

        options.solver = (String) this.wcaSolverComboBox.getSelectedItem();
        options.bvlen = (Integer) this.wcaBvlenSpinner.getValue();
        options.debug = this.wcaDebugCheckBox.isSelected();
        options.heuristicNoSolver = this.wcaHeuristicCheckBox.isSelected();
        options.historySize = (Integer) this.wcaHistorySpinner.getValue();
        options.policyAt = (Integer) this.wcaPolicySpinner.getValue();
        options.inputMax = (Integer) this.wcaInputMaxSpinner.getValue();
        options.costModel = this.costModels.get((String) this.wcaCostModelComboBox.getSelectedItem());
        // set this just so it gets a valid value
        options.sideChanType = Options.SideChannelType.Time;

        // for Side channel, substitute these values
        if (configType == Options.ConfigType.SideChannel) {
            options.solver = (String) this.scSolverComboBox.getSelectedItem();
            options.bvlen = (Integer) this.scBvlenSpinner.getValue();
            options.debug = this.scDebugCheckBox.isSelected();
            options.sideChanType = Options.cvtCboxToSideChannelType(this.scTypeComboBox.getSelectedItem().toString());
        }
	
        // Methods
        options.testClass  = this.class2ComboBox.getSelectedItem().toString();
        options.testMethod = this.method2ComboBox.getSelectedItem().toString();
        
        // get the number of parameters in the Test method
        int tstCount = 0;
    	String testMethod = options.testClass + "." + options.testMethod;
        Procedure tstProc = Program.getProcedure(testMethod);
        if (tstProc != null) {
            tstCount = tstProc.getIR().getNumberOfParameters();
            tstCount -= tstProc.getIR().getMethod().isStatic() ? 0 : 1;
        }
        options.testParamCount = tstCount;

        // we are interested in the Test arguments here
        List <ArgSelect> argList = this.argListTest;
        
        // build the list of parameter types passed to the Test method
        // (this should include the 1st entry which is actually the 'this' ptr type)
        options.testMethodTypeList = "";
        String rawList = "";
        if (!argList.isEmpty()) {
            // get the 'this' entry
            TypeReference argType = argList.get(0).argument;
            String typestr = getArgArrayDataType(argType, true);
            rawList += typestr;
            options.testMethodTypeList += typestr;
            // now add the arguments
            for (int ix = 1; ix < argList.size(); ix++) {
                argType = argList.get(ix).argument;
                typestr = getArgArrayDataType(argType, true);
                rawList += ", " + typestr;
                options.testMethodTypeList += ", " + Util.getWrapperName(typestr);
            }
        }
        if (!bCheck) {
            debugDisplayMessage(DebugType.Options, "  testClass  = " + options.testClass);
            debugDisplayMessage(DebugType.Options, "  testMethod = " + options.testMethod);
            debugDisplayMessage(DebugType.Options, "  testMethodTypeList = " + options.testMethodTypeList);
            if (options.testMethodTypeList.contains("INVALID"))
                debugDisplayMessage(DebugType.Options, "  (rawList = " + rawList + ")");
        }
	
        // Setup Test Parameters (including 1st entry = the 'this' ptr)
        options.parameters = new ArrayList<>();
        for (int ix = 0; ix < argList.size(); ix++) {
            if (!bCheck) {
                debugDisplayMessage(DebugType.Options, "  param " + ix + ":");
            }

            // get the Test method parameter type
            TypeReference argType = argList.get(ix).argument;

            Options.Type opttype;
            switch (argList.get(ix).mode) {
                case DataStruct:    opttype = Options.Type.ItObject;    break;
                case Simpleton:     opttype = Options.Type.OtherObject; break;
                case String:        opttype = Options.Type.String;      break;
                case Array:         opttype = Options.Type.Array;       break;
                case Primitive:
                    String dtype = getArgArrayDataType(argType, true);
                    switch (dtype) {
                        case "Boolean": opttype = Options.Type.Bool;   break;
                        case "Bool":    opttype = Options.Type.Bool;   break;
                        case "Char":    opttype = Options.Type.Char;   break;
                        case "Byte":    opttype = Options.Type.Byte;   break;
                        case "Short":   opttype = Options.Type.Short;  break;
                        case "Integer": opttype = Options.Type.Int;    break;
                        case "Long":    opttype = Options.Type.Long;   break;
                        case "Float":   opttype = Options.Type.Double; break;
                        case "Double":  opttype = Options.Type.Double; break;
                        case "String":  opttype = Options.Type.String; break;
                        default:
                            statusDisplayMessage(DebugType.ErrorExit, "Boxing of primitive types is not yet supported: " + dtype);
                            return null;
                    }
                    break;
                default:
                    statusDisplayMessage(DebugType.ErrorExit, "Unknown selection for parameter type radio button");
                    return null;
            }

            // create the parameter
            Parameter param = options.new Parameter();
            param.values   = Options.cvtCboxToVar(argList.get(ix).value);
            param.size     = Options.cvtCboxToVar(argList.get(ix).size);
            param.elementType = Options.cvtCboxToType(argList.get(ix).element);
            param.cls      = "";
            param.method   = "";
            param.type     = opttype;
            param.typeList = "";

            // if ItObject or OtherObject, we need the method info and list of
            // parameter types to pass to the method.
            if (param.type.equals(Options.Type.ItObject) || param.type.equals(Options.Type.OtherObject)) {

                // get the class and method selection from the argument selection
                param.cls = argList.get(ix).cls;
                param.method = Util.toRawMethodName(argList.get(ix).method);
                
                // build the list of parameter types passed to the Iterative method
                // (skip 1st entry which is the 'this' ptr)
                ArrayList<TypeReference> iterList = argList.get(ix).iterList;
                if (iterList != null) {
                    rawList = "";
                    for (int argix = 1; argix < iterList.size(); argix++) {
                        if (argix != 1) {
                            param.typeList += ", ";
                            rawList += ", ";
                        }

                        TypeReference argIterType = iterList.get(argix);
                        String typestr = getArgArrayDataType(argIterType, true);
                        rawList += typestr;
//                        String typestr = "String"; // TEMP until we have a selector for this
                        param.typeList += Util.getWrapperName(typestr);
                    }
                }
            }

            if (!bCheck) {
                if (!param.cls.isEmpty())
                    debugDisplayMessage(DebugType.Options, "    class    = " + param.cls);
                if (!param.method.isEmpty())
                    debugDisplayMessage(DebugType.Options, "    meth     = " + param.method);
                if (!param.typeList.isEmpty())
                    debugDisplayMessage(DebugType.Options, "    typelist = " + param.typeList);
                if (param.typeList.contains("INVALID"))
                    debugDisplayMessage(DebugType.Options, "   (rawList = " + rawList + ")");
                debugDisplayMessage(DebugType.Options, "    type     = " + param.type);
                if (!opttype.equals(param.type))
                    debugDisplayMessage(DebugType.Options, "   (opttype  = " + opttype + ")");
                debugDisplayMessage(DebugType.Options, "    values   = " + param.values);
                debugDisplayMessage(DebugType.Options, "    size     = " + param.size);
            }
    
            // add parameter to options list
            options.parameters.add(param);
        }

        // min/max values
        SymTypes symbolTypes;
        if (configType == Options.ConfigType.WCA)
            symbolTypes = wcaSymLimits;
        else
            symbolTypes = scSymLimits;
        options.minValChar   = symbolTypes.getLimits("char").min;
        options.maxValChar   = symbolTypes.getLimits("char").max;
        options.minValByte   = symbolTypes.getLimits("byte").min;
        options.maxValByte   = symbolTypes.getLimits("byte").max;
        options.minValShort  = symbolTypes.getLimits("short").min;
        options.maxValShort  = symbolTypes.getLimits("short").max;
        options.minValInt    = symbolTypes.getLimits("int").min;
        options.maxValInt    = symbolTypes.getLimits("int").max;
        options.minValLong   = symbolTypes.getLimits("long").min;
        options.maxValLong   = symbolTypes.getLimits("long").max;
        options.minValDouble = symbolTypes.getLimits("double").min;
        options.maxValDouble = symbolTypes.getLimits("double").max;

        debugDisplayMessage(DebugType.Exit, "createOptions");
        return options;
    }

    /**
     * checks whether the the specified option entry has changed in value.
     * 
     * @param tag - the name of the option entry
     * @param origValue - the original value of the entry
     * @param currValue - the current value of the entry
     * @return true if the current value does not match the original (i.e. the value has changed)
     */
    private boolean compareOption (String tag, int origValue, int currValue) {
        if (origValue != currValue) {
            debugDisplayMessage(DebugType.Options, "Option <" + tag + "> changed to: " + currValue);
            return true;
        }
        return false;
    }

    /**
     * checks whether the the specified option entry has changed in value.
     * 
     * @param tag - the name of the option entry
     * @param origValue - the original value of the entry
     * @param currValue - the current value of the entry
     * @return true if the current value does not match the original (i.e. the value has changed)
     */
    private boolean compareOption (String tag, boolean origValue, boolean currValue) {
        if (origValue != currValue) {
            debugDisplayMessage(DebugType.Options, "Option <" + tag + "> changed to: " + currValue);
            return true;
        }
        return false;
    }

    /**
     * checks whether the the specified option entry has changed in value.
     * 
     * @param tag - the name of the option entry
     * @param origValue - the original value of the entry
     * @param currValue - the current value of the entry
     * @return true if the current value does not match the original (i.e. the value has changed)
     */
    private boolean compareOption (String tag, String origValue, String currValue) {
        if (!origValue.equals(currValue)) {
            debugDisplayMessage(DebugType.Options, "Option <" + tag + "> changed to: " + currValue);
            return true;
        }
        return false;
    }

    /**
     * checks whether the the specified parameter option entry has changed in value.
     * 
     * @param ix  - the index of the specified parameter
     * @param tag - the name of the option's parameter entry
     * @param origValue - the original value of the entry
     * @param currValue - the current value of the entry
     * @return true if the current value does not match the original (i.e. the value has changed)
     */
    private boolean compareOptionParam (int ix, String tag, String origValue, String currValue) {
        if (!origValue.equals(currValue)) {
            debugDisplayMessage(DebugType.Options, "Option P" + ix + " <" + tag + "> changed to: " + currValue);
            return true;
        }
        return false;
    }

    /**
     * This verifies whether any settings have been changed that require the
     * driver file to be re-created.
     * 
     * @return true if settings have changed
     */
    private boolean checkIfDriverSettingsChanged () {
        // check if SPF mode selection changed
        if (lastSpfMode != WCAFrame.spfMode) {
            debugDisplayMessage(DebugType.Options, "<spfMode> changed to: " + WCAFrame.spfMode);
            return true;
        }
        if (lastOptions == null) { // create has not yet been run
            debugDisplayMessage(DebugType.Options, "Create not yet run");
            return false;
        }

        // now check each setting for the selected mode
        Options currentOptions = createOptions(WCAFrame.spfMode, true);
        if (currentOptions == null) {
            return false;
        }
        if (compareOption("projectName", lastOptions.projectName, currentOptions.projectName)) return true;
        if (compareOption("testClass"  , lastOptions.testClass  , currentOptions.testClass  )) return true;
        if (compareOption("testMethod" , lastOptions.testMethod , currentOptions.testMethod )) return true;
        if (compareOption("testMethodTypeList", lastOptions.testMethodTypeList, currentOptions.testMethodTypeList)) return true;
        if (compareOption("testParamCount"    , lastOptions.testParamCount    , currentOptions.testParamCount    )) return true;

        // Parameter List
        for (int ix = 0; ix < lastOptions.testParamCount; ix++) {
            Parameter lastPar = lastOptions.parameters.get(ix);
            Parameter curPar = currentOptions.parameters.get(ix);
            // strings
            if (compareOptionParam(ix, "cls"     , lastPar.cls     , curPar.cls     )) return true;
            if (compareOptionParam(ix, "method"  , lastPar.method  , curPar.method  )) return true;
            if (compareOptionParam(ix, "typeList", lastPar.typeList, curPar.typeList)) return true;
            // enums
            if (compareOptionParam(ix, "type"  , lastPar.type.toString()  , curPar.type.toString()  )) return true;
            if (compareOptionParam(ix, "size"  , lastPar.size.toString()  , curPar.size.toString()  )) return true;
            if (compareOptionParam(ix, "values", lastPar.values.toString(), curPar.values.toString())) return true;
            if (compareOptionParam(ix, "elementType", lastPar.elementType.toString(), curPar.elementType.toString())) return true;
        }

        debugDisplayMessage(DebugType.Options, "Driver Options match!");
        return false;
    }
    
    /**
     * This verifies whether any settings have been changed that require the
     * config file(s) to be re-created.
     * 
     * @return true if settings have changed
     */
    private boolean checkIfConfigSettingsChanged () {
        // check if SPF mode selection changed
        if (lastSpfMode != WCAFrame.spfMode) {
            debugDisplayMessage(DebugType.Options, "<spfMode> changed to: " + WCAFrame.spfMode);
            return true;
        }
        if (lastOptions == null) { // create has not yet been run
            debugDisplayMessage(DebugType.Options, "Create not yet run");
            return false;
        }

        // now check each setting that affects the Config file
        Options currentOptions = createOptions(WCAFrame.spfMode, true);
        if (currentOptions == null) {
            return false;
        }
        // Strings
        if (compareOption("projectName"  , lastOptions.projectName  , currentOptions.projectName  )) return true;
        if (compareOption("baseDir"      , lastOptions.baseDir      , currentOptions.baseDir      )) return true;
        if (compareOption("srcDir"       , lastOptions.srcDir       , currentOptions.srcDir       )) return true;
        if (compareOption("solver"       , lastOptions.solver       , currentOptions.solver       )) return true;
        if (compareOption("minValChar"   , lastOptions.minValChar   , currentOptions.minValChar   )) return true;
        if (compareOption("maxValChar"   , lastOptions.maxValChar   , currentOptions.maxValChar   )) return true;
        if (compareOption("minValByte"   , lastOptions.minValByte   , currentOptions.minValByte   )) return true;
        if (compareOption("maxValByte"   , lastOptions.maxValByte   , currentOptions.maxValByte   )) return true;
        if (compareOption("minValShort"  , lastOptions.minValShort  , currentOptions.minValShort  )) return true;
        if (compareOption("maxValShort"  , lastOptions.maxValShort  , currentOptions.maxValShort  )) return true;
        if (compareOption("minValInt"    , lastOptions.minValInt    , currentOptions.minValInt    )) return true;
        if (compareOption("maxValInt"    , lastOptions.maxValInt    , currentOptions.maxValInt    )) return true;
        if (compareOption("minValLong"   , lastOptions.minValLong   , currentOptions.minValLong   )) return true;
        if (compareOption("maxValLong"   , lastOptions.maxValLong   , currentOptions.maxValLong   )) return true;
        if (compareOption("minValDouble" , lastOptions.minValDouble , currentOptions.minValDouble )) return true;
        if (compareOption("maxValDouble" , lastOptions.maxValDouble , currentOptions.maxValDouble )) return true;
        if (compareOption("jarsclasspath", lastOptions.jarsclasspath, currentOptions.jarsclasspath)) return true;
        // enums
        if (compareOption("sideChanType"     , lastOptions.sideChanType.toString(), currentOptions.sideChanType.toString())) return true;
        if (compareOption("configType"       , lastOptions.configType.toString()  , currentOptions.configType.toString()  )) return true;
        // Numerics
        if (compareOption("bvlen"            , lastOptions.bvlen            , currentOptions.bvlen            )) return true;
        if (compareOption("debug"            , lastOptions.debug            , currentOptions.debug            )) return true;
        if (compareOption("heuristicNoSolver", lastOptions.heuristicNoSolver, currentOptions.heuristicNoSolver)) return true;
        if (compareOption("historySize"      , lastOptions.historySize      , currentOptions.historySize      )) return true;
        if (compareOption("policyAt"         , lastOptions.policyAt         , currentOptions.policyAt         )) return true;
        if (compareOption("inputMax"         , lastOptions.inputMax         , currentOptions.inputMax         )) return true;
        if (compareOption("testParamCount"   , lastOptions.testParamCount   , currentOptions.testParamCount   )) return true;

        // now check if our range has changed for WCA
        if (WCAFrame.spfMode == Options.ConfigType.WCA) {
            int currRangeHistory = this.wcaHistoryRangeCheckBox.isSelected() ? (Integer)this.wcaHistoryEndSpinner.getValue() : currentOptions.historySize;
            int currRangePolicy  = this.wcaPolicyRangeCheckBox.isSelected() ? (Integer)this.wcaPolicyEndSpinner.getValue() : currentOptions.policyAt;
            if (compareOption("historySize range", lastRangeHistory, currRangeHistory)) return true;
            if (compareOption("policyAt range"   , lastRangePolicy , currRangePolicy )) return true;
        }
        
        debugDisplayMessage(DebugType.Options, "Config Options match!");
        return false;
    }
    
    /**
     * this updates the last settings following a file creation.
     * 
     * @param spfMode - the current SPF mode selection
     * @param opt - the current Options settings used for the driver files
     */
    private void updateSettingsChange (Options.ConfigType spfMode, Options opt) {
        lastSpfMode = spfMode;
        lastOptions = opt;
        lastRangeHistory = this.wcaHistoryRangeCheckBox.isSelected() ? (Integer)this.wcaHistoryEndSpinner.getValue() : opt.historySize;
        lastRangePolicy  = this.wcaPolicyRangeCheckBox.isSelected() ? (Integer)this.wcaPolicyEndSpinner.getValue() : opt.policyAt;
    }

    private boolean isSPFInstalled () {
        try {
            Properties props = new Properties();
            props.load(new FileReader(JPF_CONFIG_PATH));
            String propname = "jpf-core";
            String propvalue =  props.getProperty(propname);
            if (propvalue == null || propvalue.isEmpty()) {
                debugDisplayMessage(DebugType.Error, propname + " property not found in JPF site.properties");
                return false;
            }
            propname = "jpf-symbc";
            propvalue = props.getProperty(propname);
            if (propvalue == null || propvalue.isEmpty()) {
                debugDisplayMessage(DebugType.Error, propname + " property not found in JPF site.properties");
                return false;
            }
            propname = "spf-wca";
            propvalue =  props.getProperty(propname);
            if (propvalue == null || propvalue.isEmpty()) {
                debugDisplayMessage(DebugType.Error, propname + " property not found in JPF site.properties");
                return false;
            }
        } catch (IOException e) {
            debugDisplayMessage(DebugType.Error, "loading JPF site.properties from location: " + JPF_CONFIG_PATH);
            return false;
        }

        return true;
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
     * This searches the file contents for the location of the current driver name
     * and replaces it with the new name.
     * 
     * NOTE: This makes the assumption that there is only 1 occurrance of
     * the prefix value in the file and it uniquely identifies where the
     * driver name is placed.
     * 
     * @param content - the original file contents
     * @param prefix  - the unique string that identifies the start of the driver name
     * @return the original file with the name replaced (unchanged if no name change)
     */
    private String replaceDriverName(String content, String prefix) {
        String newname = Util.getDriverBaseName(projectName);
        String head, tail, oldname = "";
        if (content == null || prefix == null)
            return content;
        int offset, length = prefix.length();

        // first, split string at end of 'prefix' match
        offset = content.indexOf(prefix);
        if (offset < 0)
            return content;
        head = content.substring(0, offset + length);
        tail = content.substring(offset + length);
        // now extract all non-whitespace from beining of tail as oldentry
        offset = findEndOfField(tail);
        //if (offset < 0)
        //    return content;
        if (offset > 0) {
            oldname = tail.substring(0, offset);
            tail = tail.substring(offset);
        }
        // if the name has changed, create the ammended string
        if (!oldname.equals(newname))
            content = head + newname + tail;

        return content;
    }  

    /**
     * assure that the driver directory exists. If it doesn't, make one.
     * If it does, remove all files in it if so directed.
     * 
     * @param driverpath - the driver path
     * @param bFlush - true if empty the directory if it already exists
     * @return true if driver path is valid
     */
    private boolean makeDriverDir(String driverPathName, boolean bFlush) {
        File driverpath = new File(driverPathName);
        if (driverpath.isFile())
            driverpath.delete();
        if (driverpath.isDirectory()) {
            // driver dir already exists - if requested, remove all current files in the folder
            if (bFlush) {
                String[] entries = driverpath.list();
                for (String fname : entries) {
                    File file = new File(driverpath.getPath(),fname);
                    file.delete();
                }
            }
        }
        else {
            // driver dir does not exist - make one
            if (driverpath.mkdirs() != true) {
                statusDisplayMessage(DebugType.Error, "Failed to create dir: " + driverPathName);
                return false;
            }
        }
        return true;
    }

    /**
     * loads the driver content from the designated file.
     * 
     * @return the text content of the file if successful
     */
    private String loadDriverFile() {
		
	// set the filenames in the corresponding editor boxes
        String content;
        String fname_driver = Util.getDriverFileName(projectName);
        driverPanelSetTitle (DriverType.Driver, fname_driver);
        
        debugDisplayMessage(DebugType.Entry, "loadDriverFile: " + fname_driver);

        // get the input file name to read from
        String driverPathName = getSPFDriverPath();
        fname_driver = driverPathName + fname_driver;
        
        // copy the String data to the driver file location
        try {
            content = FileUtils.readFileToString(new File(fname_driver), "UTF-8");
        } catch (IOException ex) {
            statusDisplayMessage(DebugType.ErrorExit, "Reading from file: " + fname_driver);
            return null;
        }

        // the driver file has been changed - clear the flag to indicate it needs to be compiled
        bFlag_Compiled = false;

        // set flag indicating driver files have been created
        bFlag_Created = true;

        // clear flag to indicate files have not been modified since last create
        bFlag_Modified  = false;

        // disable the save button since the text contents should match the saved files
        saveButton.setEnabled(false);
        undoButton.setEnabled(false);

        // success - clear msg & restore text color of editor to black
        statusDisplayMessage();
        driverPanelShowTextChanged (DriverType.Driver, false);
        debugDisplayMessage(DebugType.Exit, "loadDriverFile");
        return content;
    }

    /**
     * saves the driver content into the designated file.
     * This will check for the existance of the output directory in which to place
     * the driver and config files and will create one if not. It also checks if
     * a driver currently exists at this location and the user has not yet been
     * prompted as to whether to over write the files or not.
     * 
     * @param content  - text content to write to file
     * @param bFlush - true if empty the directory if it already exists
     * @return true if successful
     */
    private boolean saveDriverFile(String content, boolean bFlush) {
		
	// set the filenames in the corresponding editor boxes
        String fname_driver = Util.getDriverFileName(projectName);
        driverPanelSetTitle (DriverType.Driver, fname_driver);
        
        debugDisplayMessage(DebugType.Entry, "saveDriverFile: " + fname_driver);

        // get the output file name to create
        String driverPathName = getSPFDriverPath();
        fname_driver = driverPathName + fname_driver;
        
        // create a drivers folder if one is not already created
        if (makeDriverDir(driverPathName, bFlush) != true) {
            statusDisplayMessage(DebugType.ErrorExit, "Failed to create drivers folder");
            return false;
        }
        
        // copy the String data to the driver file location
        try {
            FileUtils.writeStringToFile(new File(fname_driver), content, "UTF-8");
        } catch (IOException ex) {
            statusDisplayMessage(DebugType.ErrorExit, "Writing to file: " + fname_driver);
            return false;
        }

        // the driver file has been changed - clear the flag to indicate it needs to be compiled
        bFlag_Compiled = false;

        // Indicate the created data has changed so we can warn user if he
        // attempts to Create again.
        bFlag_Modified  = true;        
        
        // success - clear msg & restore text color of editor to black
        statusDisplayMessage();
        driverPanelShowTextChanged (DriverType.Driver, false);
        debugDisplayMessage(DebugType.Exit, "saveDriverFile");
        return true;
    }

    /**
     * saves the content to the specified config file
     * 
     * @param content - the content of the file
     * @param ix - the index of the config file (0 if there is only 1)
     */
    private void saveConfigFile(String content, int ix) {
        // determine the number of config files specified
        Integer count = getSpfIterations (WCAFrame.spfMode);
        int id = (count <= 1) ? 0 : ix + 1;

        // set the filenames in the corresponding editor boxes
        String fname_config = Util.getConfigFileName(projectName, id);
        driverPanelSetTitle (DriverType.Config, fname_config);

        debugDisplayMessage(DebugType.Entry, "saveConfigFile: " + fname_config);

        // copy the String data to the driver file location
        String driverPath = getSPFDriverPath();
        fname_config = driverPath + fname_config;

        try {
            FileUtils.writeStringToFile(new File(fname_config), content, "UTF-8");
        } catch (IOException ex) {
            statusDisplayMessage(DebugType.ErrorExit, "Writing to file: " + fname_config);
            return;
        }

        // success - clear msg & restore text color of editor to black
        statusDisplayMessage();
        driverPanelShowTextChanged (DriverType.Config, false);
        debugDisplayMessage(DebugType.Exit, "saveConfigFile");
    }

    /**
     * generates and saves the driver content into the designated file.
     * This will check for the existance of the output directory in which to place
     * the driver and config files and will create one if not. It also checks if
     * a driver currently exists at this location and the user has not yet been
     * prompted as to whether to over write the files or not.
     * 
     * @param opt  - the initial options setup
     * @return content of 1st file (null if error)
     */
    private String generateConfigFiles(Options opt) {

        if (opt == null)
            return null;

        // we will save the content of the 1st file for return, so it can be displayed
        String content_first = "";
        
        // determine the number of config files specified
        Integer count = getSpfIterations (WCAFrame.spfMode);
        debugDisplayMessage(DebugType.Entry, "generateConfigFiles: count = " + count);
        statusDisplayMessage(DebugType.Normal, "Generating Config Files");

        // copy the String data to the driver file location
        String driverPath = getSPFDriverPath();

        for (int ix = 0; ix < count; ix++) {
            // generate the file for the specified iteration
            ConfigGenerator cg = new ConfigGenerator(opt);
            String content;
            try {
                content = cg.generate();
            } catch (GenerationException ge) {
                statusDisplayMessage(DebugType.ErrorExit, "Config file generation failed." + newLine + ge.getMessage());
                return null;
            }
            
            int id = (count <= 1) ? 0 : ix + 1;
            String configName = Util.getConfigFileName(projectName, id);
            String fname_config = driverPath + configName;
            
            // save the content for the 1st of the config files to return
            if (ix == 0) {
                content_first = content;
            }

            try {
                FileUtils.writeStringToFile(new File(fname_config), content, "UTF-8");
            } catch (IOException ex) {
                statusDisplayMessage(DebugType.ErrorExit, "Writing to file: " + fname_config);
                return null;
            }

            // modify the settings in the Options for the next iteration
            nextOptionsRangeSelection (WCAFrame.spfMode, opt);
        }

        // success - clear msg
        statusDisplayMessage();
        debugDisplayMessage(DebugType.Exit, "generateConfigFiles");
        return content_first;
    }

    /**
     * creates the driver and config files
     * 
     * @param spfMode - the SPF type to create (WCA or SideChannel)
     * @return 0 on success
     */
    private int createDriverFiles (Options.ConfigType spfMode) {
        debugDisplayMessage(DebugType.Entry, "createDriverFiles: mode = " + spfMode);
        statusDisplayMessage(DebugType.Normal, "Generating Driver File");
        WCAFrame.spfMode = spfMode;
        Options opt = this.createOptions(spfMode, false);
        if (opt == null) {
            return -1;
        }

        // generate the driver file
        DriverGenerator dg = new DriverGenerator(opt);
	String driver;
	try {
            driver = dg.generate();
	} catch (GenerationException ge) {
            statusDisplayMessage();
            statusDisplayMessage(DebugType.ErrorExit, "Driver generation failed." + newLine + ge.getMessage());
            return -1;
	}

        // save the driver file
        boolean bSuccess = saveDriverFile(driver, false);

        // generate and save the config file(s)
        String config = "";
        if (bSuccess) {
            // generate the config file(s)
            config = generateConfigFiles(opt);

            // a new driver file has been generated - clear the flag to indicate it needs to be compiled
            bFlag_Compiled = false;

            // set flag indicating driver files have been created
            bFlag_Created = true;

            // clear flag to indicate files have not been modified since last create
            bFlag_Modified  = false;

            // disable the save button since the text contents should match the saved files
            saveButton.setEnabled(false);
            undoButton.setEnabled(false);
        }
        
        // save last run values
        updateSettingsChange(spfMode, opt);

	// display the files and init the text color to black
        driverPanelSetup(DriverType.Driver, driver);
        driverPanelSetup(DriverType.Config, config);

        debugDisplayMessage(DebugType.Exit, "createDriverFiles");
        return 0;
    }

    /**
     * creates the config files.
     * NOTE: assumes driver file has been generated and matches the current settings
     * 
     * @param spfMode - the SPF type to create (WCA or SideChannel)
     * @return 0 on success
     */
    private int createConfigFilesOnly (Options.ConfigType spfMode) {
        debugDisplayMessage(DebugType.Entry, "createConfigFilesOnly: mode = " + spfMode);
        statusDisplayMessage(DebugType.Normal, "Creating config files (only)");
        WCAFrame.spfMode = spfMode;
        Options opt = this.createOptions(spfMode, false);
        if (opt == null) {
            return -1;
        }

        // generate and save the config file(s)
        String config = generateConfigFiles(opt);
        
        // save last run values
        updateSettingsChange(spfMode, opt);

	// display the files and init the text color to black
        driverPanelSetup(DriverType.Config, config);

        // clear flag to indicate files have not been modified since last create
        bFlag_Modified  = false;

        // disable the save button since the text contents should match the saved files
        saveButton.setEnabled(false);
        undoButton.setEnabled(false);

        debugDisplayMessage(DebugType.Exit, "createConfigFilesOnly");
        return 0;
    }
    
    /**
     * This performs the compilation of the driver and any packaging needs prior
     * to launching the SPF. For running SPF on local machine, these commands
     * can just be queued in a thread that is also used for queuing the SPF.
     * For running on the cloud, the zip file needs to be created prior to
     * sending the commands, since the zipfile needs to be sent to each job
     * instance. Therefore for the cloud we must run these commands and wait
     * for them to complete before returning.
     * 
     * @param cpath  - base path for the SPF subdirectory of the project
     * @param spfjar - SPF jar file to compile the driver file with
     * @param bCloud - true if this is to run in the cloud
     */
    private void compileDriverFile (String spfjar, boolean bCloud) {
        
        debugDisplayMessage(DebugType.Entry, "compileDriverFile" + (bCloud ? ": (on cloud)" : "(local)"));
        statusDisplayMessage(DebugType.Normal, "Compiling Driver File");
        String binpath = getSPFBasePath() + "bin";
        String driverRawName = Util.getDriverFileName(projectName);
        String driverPath = getSPFDriverPath();
        String driverName = driverPath + driverRawName;
        File bpath = new File(binpath);
        if (!bpath.isDirectory())
            bpath.mkdirs();
        File driverFile = new File(driverName);
        if (!driverFile.isFile()) {
            statusDisplayMessage(DebugType.ErrorExit, "Driver file not found: " + driverName);
            return;
        }
        
        DefaultListModel appList = configInfo.getList(ConfigInfo.ListType.application);
        DefaultListModel libList = configInfo.getList(ConfigInfo.ListType.libraries);
        debugDisplayMessage(DebugType.Normal, "applist size = " + appList.size() + ", liblist size = " + libList.size());
        // determine if there is a single jar application file and no libraries defined.
        // (there might be 1 that is just the rt.lib file that we added, so we'll ignore that one)
        // if so, check if the application file contains a lib directory
        // with jars in it, and ask the user if he wants to pull these out
        // to allow using them as the library.
        if (libList.getSize() <= 1 && appList.getSize() == 1) {
            debugDisplayMessage(DebugType.Normal, "Checking for libraries to extract from application file");
            try {
                int count = AverroesFrame.extractEmbeddedLibrary(
                                                new File ((String)appList.get(0)),
                                                configInfo.getField(ConfigInfo.StringType.projectpath) + "/janalyzer/common/",
                                                libList);
                if (count > 0)
                    debugDisplayMessage(DebugType.Normal, "Unpacked library from application jar: " + libList.size() + " files");
                else if (count < 0) {
                    debugDisplayMessage(DebugType.Normal, "Using existing extracted lib dir: " + libList.size() + " files");
                }
            } catch (IOException ex) {
                debugDisplayMessage(DebugType.Error, "IOException: " + ex);
            }
        }

        // save the current settings in a file (so we can potentially load them later)
        saveSettingsFile();

        // make sure no threads are currently scheduled
        threadLauncher.init(new StandardTermination());
        
        zipFileName = null;
        bFlag_Compiled = false;
        String classpath = createClasspath(":", false);

        // 1. compile the driver
        String[] command1 = { "/usr/bin/javac",
                             "-cp",
                             spfjar + ":" + classpath,
                             "-d",
                             binpath,
                             driverName };
        threadLauncher.launch(command1, null, "Compiling driver", driverRawName);

        // for submission to the cloud, we need some additional items
        if (bCloud) {
            // 2. generate the driver jar file.
            // we need the jar file to be referenced from the project bin dir,
            // so the only path in the jar is 'drivers'.
            String workdir = binpath;
            driverRawName = Util.getDriverBaseName(projectName);

            String[] command2 = { "/usr/bin/jar",
                                 "cfe",
                                 driverPath + driverRawName + ".jar",
                                 driverRawName,
                                 "drivers/" + driverRawName + ".class" };
            threadLauncher.launch(command2, workdir, "Building driver jar", driverRawName + ".jar");

            // 3. create a zip file containing the all the config files, the driver jar
            // file and all of the library files.
            // let's run this command from the project's SPF base path, so all references
            // should be made from there.
            workdir = getSPFBasePath();
            driverPath = "drivers/";

            // specify the path & name of the zip file (relative to the working dir)
            zipFileName = driverPath + projectName + ".zip";

            // get a String[] of all of the application and library files needed for the build
            ArrayList<String> fileArray = new ArrayList();
            for (int ix=0; ix < appList.size(); ix++) {
                fileArray.add(appList.get(ix).toString());
            }
            for (int ix=0; ix < libList.size(); ix++) {
                fileArray.add(libList.get(ix).toString());
            }
            String[] fileList = new String[fileArray.size()];
            fileList = fileArray.toArray(fileList);

            // use the options -j and -r to strip paths from the filenames and to
            // recurse the directory, respectively.
            String[] cmdPrefix = { "zip", "-jr",
                                 zipFileName,       // the zip output filename
                                 driverPath };      // input path to zip (driver files)
            // now append each application and library jar file
            String[] command3 = ArrayUtils.addAll(cmdPrefix, fileList);
            threadLauncher.launch(command3, workdir, "Zipping up files", driverRawName + ".zip");
            
            // return the full path to the zip file
            zipFileName = workdir + zipFileName;
        }

        // enable the stop button
        stopButton.setEnabled(true);

        // indicate the compiler has completed
        bFlag_Compiled = true;
        debugDisplayMessage(DebugType.Exit, "compileDriverFile");
    }

    private void updateProjectPathSelection (String prjpath) {
        // verify the path has either a lib dir containing jars or contains jars itself
        String path = prjpath;
        String libpath = AnalyzerFrame.findPathToProjLib(prjpath);
        if (libpath.startsWith("<")) {
            int offset = libpath.indexOf(':');
            if (offset >= 0)
                path = libpath.substring(offset+1);
            
            JOptionPane.showMessageDialog(null,
                "Directory: " + path,
                "Invalid Project Path", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // selection is valid - save it
        this.prjpathTextField.setText(prjpath);

        // extract the last field from the path and use it as the project name.
        // remove this field from the project path for saving in properties file.
        int offset = prjpath.lastIndexOf('/');
        String prjname = "";
        if (offset >= 0 && offset < prjpath.length() - 1) {
            prjname = prjpath.substring(prjpath.lastIndexOf('/') + 1);
            prjpath = prjpath.substring(0, offset);
        }
        
        // if no driver name is currently selected, use final dir of project path
        if (this.projectName.isEmpty()) {
            this.projectName = prjname;
            this.prjnameTextField.setText(prjname);
        }

        // update cloud job name to same as project name
        this.cloudNameTextField.setText(prjname);

        // update the properties file with the project path selection
        gui.AnalyzerFrame.setPropertiesItem (null, "ProjectPath", prjpath);

        // update the config file if we have Loaded or Saved one
        updateConfigFileParam ("projectname", prjname);

        // Enable the Create buttons
        this.wcaCreateButton.setEnabled(true);
        this.scCreateButton.setEnabled(true);
    }
    
    private void wcaCreateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wcaCreateButtonActionPerformed
        statusDisplayMessage();
        String itemName = "wcaCreateButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // determine if no settings have been changed that should cause the
        // driver file to change (and it has already been created)
        boolean bConfigOnly = false;
        if (bFlag_Created && !checkIfDriverSettingsChanged()) {
            bConfigOnly = true;
        }

        // if the user has made changes to the previously created driver files,
        // query if the user wants really to overwrite his changes.
        if (bFlag_Modified) {
            String title   = "Driver files have been modified";
            if (bConfigOnly) {
                String message = "No parameters have changed that affect the driver file and" + newLine +
                          " you have made manual changes to either the driver or config file" + newLine +
                          " which will be lost." + newLine +
                          " You may choose:" + newLine + 
                          "  -Yes-    to update the driver and config files," + newLine +
                          "  -Config- to update only the config file," + newLine +
                          "  -No- to exit without making any changes." + newLine + newLine +
                          "Do you want to overwrite your files?";
                String[] yesno = new String[] { "No", "Config", "Yes" };
                int which = JOptionPane.showOptionDialog(null, // parent component
                            message,        // message
                            title,          // title
                            JOptionPane.DEFAULT_OPTION, // option type
                            JOptionPane.PLAIN_MESSAGE,  // message type
                            null,           // icon
                            yesno,          // selection options
                            yesno[0]);      // initial value
                if (which < 0 || yesno[which].equals("No"))
                    return;
                if (yesno[which].equals("Yes"))
                    bConfigOnly = false;
            }
            else {
                String message = "You have made manual changes to either the driver or config file" + newLine +
                          " which will be lost if they are regenerated." + newLine +
                          " You may choose:" + newLine + 
                          "  -Yes-    to update the driver and config files," + newLine +
                          "  -No- to exit without making any changes." + newLine + newLine +
                          "Do you want to overwrite your files?";
                String[] yesno = new String[] { "No", "Yes" };
                int which = JOptionPane.showOptionDialog(null, // parent component
                            message,        // message
                            title,          // title
                            JOptionPane.DEFAULT_OPTION, // option type
                            JOptionPane.PLAIN_MESSAGE,  // message type
                            null,           // icon
                            yesno,          // selection options
                            yesno[0]);      // initial value
                if (which < 0 || yesno[which].equals("No"))
                    return;
            }
        }

        // if a driver file already exists and we haven't nagged the user about it yet,
        // prompt him whether to overwrite the files or not.
        // - only check for the driver file, not the config file(s) for brevity
        String driverPathName = getSPFDriverPath();
        File driverpath = new File(driverPathName);
        if (driverpath.exists()) {
            File drvfile = new File(driverPathName + Util.getDriverFileName(projectName));
            if (drvfile.exists()) {
                // If the the file exists and user hasn't been prompted for overwrite,
                // warn him that he will be overwriting existing files.
                // Once he has committed this act, we will no longer prompt him.
                if (!bFlag_overwrite) {
                    // query if the user wants to overwrite the existing files
                    String[] yesno = new String[] { "Exit", "Load", "Overwrite" };
                    String title   = "Driver file currently exists";
                    String message = "You may may choose from the following:" + newLine +
                                  "  'Overwrite' to create a new driver file, overwriting the old one" + newLine +
                                  "  'Load'      to load the existing driver file by this name" + newLine +
                                  "  'Exit'      to exit the driver creation" + newLine + newLine +
                                  "Do you want to overwrite the existing files?";
                    int which = JOptionPane.showOptionDialog(null, // parent component
                        message,        // message
                        title,          // title
                        JOptionPane.DEFAULT_OPTION, // option type
                        JOptionPane.PLAIN_MESSAGE,  // message type
                        null,           // icon
                        yesno,          // selection options
                        yesno[0]);      // initial value
                    if (which < 0 || yesno[which].equals("Exit")) {
                        return;
                    }
                    if (yesno[which].equals("Load")) {
                        // load existing driver file
                        String driver = loadDriverFile();
                        
                        // display the driver file and init the text color to black
                        driverPanelSetup(DriverType.Driver, driver);
                        
                        // indicate only the config file(s) need to be generated
                        bConfigOnly = true;
                    }

                    // indicate the user has accepted responsibility for overwriting
                    // existing files so we aren't nagging him every time.
                    bFlag_overwrite = true;
                }
            }
        }

        // create the driver files
        if (bConfigOnly)
            createConfigFilesOnly (Options.ConfigType.WCA);
        else
            createDriverFiles (Options.ConfigType.WCA);
    }//GEN-LAST:event_wcaCreateButtonActionPerformed
    
    private void launchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launchButtonActionPerformed
        statusDisplayMessage();
        String itemName = "launchButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // clear the output window
        outputTextArea.setText("");

        // don't allow if user does not have SPF installed
        if (!isSPFInstalled()) {
            JOptionPane.showMessageDialog(null,
                    "SPF must be installed in order to run the driver.",
                    "SPF is not installed", JOptionPane.ERROR_MESSAGE);
            statusDisplayMessage(DebugType.Error, "SPF not installed");
            return;
        }
        
        // don't allow build if user has not created a file
        if (!bFlag_Created) {
            JOptionPane.showMessageDialog(null,
                    "Driver files have not been created",
                    "Missing Driver Files", JOptionPane.ERROR_MESSAGE);
            statusDisplayMessage(DebugType.Error, "Driver files have not been created");
            return;
        }
        
        // if settings have changed since last time Create was pressed,
        // automatically re-Create the files.
        boolean bNeedsRecompile = false;
        if (checkIfDriverSettingsChanged()) {
            createDriverFiles (spfMode);
            bNeedsRecompile = true;
        }
        else if (checkIfConfigSettingsChanged()) {
            createConfigFilesOnly (spfMode);
        }
        else if (saveButton.isEnabled()) {
            // otherwise, check if user has made changes to driver files without
            // saving them, and ask whether he want to Save the changes or
            // Undo them by re-Create the files so they match the parameters.
            String[] selection = new String[] { "Save", "Undo" };
            String title   = "File changes not saved";
            String message = "The driver files have been manually edited" + newLine +
                             "without saving the changes." + newLine + newLine +
                             "Do you want to Save or Undo the changes ?";
            int which = JOptionPane.showOptionDialog(null, // parent component
                message,        // message
                title,          // title
                JOptionPane.DEFAULT_OPTION, // option type
                JOptionPane.PLAIN_MESSAGE,  // message type
                null,           // icon
                selection,      // selection options
                selection[0]);  // initial value
            if (which < 0 || selection[which].equals("Undo")) {
                // to undo the file changes, simply re-Create the files from the settings
                createDriverFiles (spfMode);
            }
            else {
                // otherwise, save the current edit changes to the files
                boolean bSuccess = saveDriverFile(driverTextPane.getText(), false);
                if (bSuccess) {
                    saveConfigFile(configTextPane.getText(), 0);

                    // disable the save button since the text contents should match the saved files
                    saveButton.setEnabled(false);
                    undoButton.setEnabled(false);
                }
            }
            
            // in either case, the driver files have been changed
            bNeedsRecompile = true;
        }

        // if running on the cloud, any change in the driver files
        // means it must be compiled (built) before launching, since
        // this is a seperate step for cloud processing.
        // If so, indicate that the user must run Compile first.
        boolean bCloud = useCloudCheckBox.isSelected();
        if (bCloud && bNeedsRecompile) {
            JOptionPane.showMessageDialog(null,
                    "Either the driver has never been compiled or" + newLine +
                    "settings were changed that caused the driver." + newLine +
                    "to be re-created. Launching in the cloud requires" + newLine +
                    "Build to be performed prior to Launch." + newLine + newLine +
                    "Please Build files prior to Launch",
                    "Driver out of date", JOptionPane.ERROR_MESSAGE);
            statusDisplayMessage(DebugType.Error, "Drivers need to be compiled before launching");
            return;
        }

        // make sure we have valid driver files.
        if (!checkIfDriverFilesExist()) {
            JOptionPane.showMessageDialog(null,
                    "One of more driver files were not found",
                    "Missing Driver Files", JOptionPane.ERROR_MESSAGE);
            statusDisplayMessage(DebugType.Error, "Driver files are missing");
            return;
        }

        // now everything should be hunky-dory for launching...

        // find the SPF jar and JPF binary location
        String driverPath = getSPFDriverPath();
        String spfjar = "";
        String jpf = "";
        try {
            Properties props = new Properties();
            props.load(new FileReader(JPF_CONFIG_PATH));
            String userhome = "${user.home}";
            String localhome = System.getProperty("user.home");
            spfjar =  props.getProperty("jpf-symbc").replace(userhome, localhome) + "/build/jpf-symbc-classes.jar";
            jpf = props.getProperty("jpf-core").replace(userhome, localhome) + "/build/RunJPF.jar";
        } catch (IOException e) {
            statusDisplayMessage(DebugType.IntError, "loading JPF site.properties from location: " + JPF_CONFIG_PATH);
            e.printStackTrace();
        }

        // determine the number of config files specified
        Integer count = getSpfIterations (WCAFrame.spfMode);

        // re-direct stdout and stderr to the text window
        PrintStream printStream = new PrintStream(new RedirectOutputStream(this.outputTextArea)); 
        System.setOut(printStream);
        System.setErr(printStream);

        if (bCloud) {
            // TODO: currently the new cloud architecture doesn't support heuristic search...
            statusDisplayMessage(DebugType.Error, "Cloud currently unavailable for WCA!");
/*
            statusDisplayMessage(DebugType.Normal, "Launching (in cloud)");
            // for the cloud, the Compile must be run prior to the Launch,
            // because these run on a seperate thread and there is not a good way
            // to wait for this to complete before launching to the cloud, which
            // runs in the current thread.
            if (!bFlag_Compiled) {
                statusDisplayMessage(DebugType.Error, "Build driver before Launching!");
                return;
            }
            if (zipFileName == null || zipFileName.isEmpty()) {
                statusDisplayMessage(DebugType.Error, "Zip file was not created");
                return;
            }
            
            // send the file to the cloud
            File zipFile = new File(zipFileName);
            if (!zipFile.isFile()) {
                statusDisplayMessage(DebugType.Error, "Zip file not found for cloud job");
                return;
            }

            // change tab selection to show the Output window
            this.viewerTabbedPane.setSelectedIndex(2);
            this.viewerTabbedPane.update(this.viewerTabbedPane.getGraphics());
        
            this.outputTextArea.append("Running SPF in cloud..." + newLine);
            ++cloudnumber; // this keeps a count of the number of spf executions performed
            for (int ix = 1; ix <= count; ix++) {
                // get the config file name
                int id = (count <= 1) ? 0 : ix;
                String jpfFileName = Util.getConfigFileName(projectName, id);

                // queue up the cloud
                String jobName = this.cloudNameTextField.getText();
                String executors = this.cloudExecutorsTextField.getText();
                String initDepth = this.cloudInitialDepthTextField.getText();
                debugDisplayMessage(DebugType.Normal, "cloud postJob: " + jobName + ", " + executors + ", "
                                                                        + initDepth + ", " + jpfFileName);

                CloudInterface cloud = new CloudInterface(debugmsg);
                String response = cloud.postJob(zipFile, jobName, executors, initDepth, jpfFileName);
                String response = ""; // old interface 
                if (response.isEmpty())
                    statusDisplayMessage(DebugType.Error, "Error starting cloud job for index " + id + " of " + count);
                else {
                    // else, log the job id started for referencing later
                    CloudJob entry = new CloudJob(cloudnumber, ix, count, response);
                    cloudJobs.add(entry);
                }
            }
*/
        }
        else {
            statusDisplayMessage(DebugType.Normal, "Launching (locally)");
            
            // change tab selection to show the Output window
            this.viewerTabbedPane.setSelectedIndex(2);
            this.viewerTabbedPane.update(this.viewerTabbedPane.getGraphics());
        
            // make sure no threads are currently scheduled
            threadLauncher.init(new StandardTermination());
        
            // compile the driver file if it hasn't been performed yet
            if (!bFlag_Compiled)
                compileDriverFile (spfjar, bCloud);

            // determine the number of config files specified
            String configName;
            for (int ix = 1; ix <= count; ix++) {
                int id = (count <= 1) ? 0 : ix;
                configName = Util.getConfigFileName(projectName, id);
        
                String[] command = { "java",
//                                     "-Xmx1024m", "-ea",
                                      "-jar",
                                      jpf, 
                                      driverPath + configName };
                threadLauncher.launch(command, null, "Running SPF", configName);
            }
            
            // enable the stop button
            stopButton.setEnabled(true);
        }

        // restore stdout and stderr
        System.setOut(this.standardOut);
        System.setErr(this.standardErr);
    }//GEN-LAST:event_launchButtonActionPerformed

    private void prjpathButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prjpathButtonActionPerformed
        statusDisplayMessage();
        String itemName = "projectPathButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        File dfltPath = new File(configInfo.getField(ConfigInfo.StringType.projectpath));
        this.folderChooser.setCurrentDirectory(dfltPath);
        this.folderChooser.setMultiSelectionEnabled(false);
        int retVal = this.folderChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File folder = this.folderChooser.getSelectedFile();
            String prjpath = folder.getAbsolutePath();

            // verify the path has either a lib dir containing jars or contains jars itself
            updateProjectPathSelection(prjpath);
        }
    }//GEN-LAST:event_prjpathButtonActionPerformed
            
    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        statusDisplayMessage();
        String itemName = "saveButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        boolean bSuccess = saveDriverFile(driverTextPane.getText(), false);
        if (bSuccess) {
            saveConfigFile(configTextPane.getText(), 0);

            // disable the save button since the text contents should match the saved files
            saveButton.setEnabled(false);
            undoButton.setEnabled(false);
        }
    }//GEN-LAST:event_saveButtonActionPerformed

    private void wcaSolverComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wcaSolverComboBoxActionPerformed
        statusDisplayMessage();
        String itemName = "wcaSolverComboBox";

        // the bvlen selection is only valid for z3bitvector solver
        String chosenStr = (String) this.wcaSolverComboBox.getSelectedItem();
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, chosenStr);
        if (chosenStr.equals("z3bitvector"))
            this.wcaBvlenSpinner.setEnabled(true);
        else
            this.wcaBvlenSpinner.setEnabled(false);
    }//GEN-LAST:event_wcaSolverComboBoxActionPerformed
    
    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        statusDisplayMessage();
        String itemName = "stopButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        ThreadLauncher.ThreadInfo threadInfo = threadLauncher.stopAll();

        // stop the running process
        if (threadInfo.pid >= 0) {
            debugDisplayMessage(DebugType.Warning, "Killing job " + threadInfo.jobid + ": pid " + threadInfo.pid);
            String[] command = { "kill", "-15", threadInfo.pid.toString() };
            commandLauncher.start(command, null);
        }
        
        this.stopButton.setEnabled(false);
    }//GEN-LAST:event_stopButtonActionPerformed
    
    private void wcaSymTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wcaSymTypeComboBoxActionPerformed
        statusDisplayMessage();
        String itemName = "wcaSymTypeComboBox";
        String chosenStr = (String)this.wcaSymTypeComboBox.getSelectedItem();
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, chosenStr);

        // set the min and max limits (and current value if out of range)
        // when the data type is changed.
        SymLimits limit = wcaSymLimits.getLimits(chosenStr);
        this.wcaSymMinTextField.setText(limit.min);
        this.wcaSymMaxTextField.setText(limit.max);
    }//GEN-LAST:event_wcaSymTypeComboBoxActionPerformed

    private void wcaSymMaxTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_wcaSymMaxTextFieldFocusLost
        statusDisplayMessage();
        String itemName = "wcaSymMaxTextField";

        JFormattedTextField maxValue = (JFormattedTextField)evt.getSource();
        JFormattedTextField minValue = wcaSymMinTextField;

        // get the data type selection
        String dtype = (String)this.wcaSymTypeComboBox.getSelectedItem();
        if (dtype == null) {
            debugDisplayEvent (DebugEvent.FocusLost, itemName, "no symbol selection");
            return;
        }
        debugDisplayEvent (DebugEvent.FocusLost, itemName, dtype);

        // make sure the current value is within the specified range limits
        String result = absLimitCheck(dtype, maxValue.getText());
        if (result != null)
            maxValue.setText(result);
        boolean bExceeds = isMinExceedMax(dtype, minValue.getText(),
                                                 maxValue.getText());
        if (bExceeds == true)
            minValue.setText(maxValue.getText());
                
        // update the class limit parameter for the current data type selection
        saveSymbolValues(Options.ConfigType.WCA, dtype);
    }//GEN-LAST:event_wcaSymMaxTextFieldFocusLost

    private void wcaSymMinTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_wcaSymMinTextFieldFocusLost
        statusDisplayMessage();
        String itemName = "wcaSymMinTextField";

        JFormattedTextField minValue = (JFormattedTextField)evt.getSource();
        JFormattedTextField maxValue = wcaSymMaxTextField;

        // get the data type selection
        String dtype = (String)this.wcaSymTypeComboBox.getSelectedItem();
        if (dtype == null) {
            debugDisplayEvent (DebugEvent.FocusLost, itemName, "no symbol selection");
            return;
        }
        debugDisplayEvent (DebugEvent.FocusLost, itemName, dtype);

        // make sure the current value is within the specified range limits
        String result = absLimitCheck(dtype, minValue.getText());
        if (result != null)
            minValue.setText(result);
        boolean bExceeds = isMinExceedMax(dtype, minValue.getText(),
                                                 maxValue.getText());
        if (bExceeds == true)
            maxValue.setText(minValue.getText());
                
        // update the class limit parameter for the current data type selection
        saveSymbolValues(Options.ConfigType.WCA, dtype);
    }//GEN-LAST:event_wcaSymMinTextFieldFocusLost
        
    private void argNextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_argNextButtonActionPerformed
        statusDisplayMessage();
        String itemName = "argNextButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        int prev = argOffset; // save previous value
        setGUIParamSetup_ArgSelect(+1);
        if (argOffset != prev) {
            disableParameterSetupListeners();
            // need to setup class and method comboboxes for new class/param selection
            ArgSelect argSelect = argListTest.get(argOffset);
            generateIterClassList(argSelect.argument, argSelect.cls, argSelect.method);
            TypeReference argType = copyArgListToGUI(); // update the GUI from the argList
            enableParameterSetupListeners();
            setGUIParamSetup_ModesEnabled(argType);     // update the parameter type selection
        }
    }//GEN-LAST:event_argNextButtonActionPerformed

    private void argPrevButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_argPrevButtonActionPerformed
        statusDisplayMessage();
        String itemName = "argPrevButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        int prev = argOffset; // save previous value
        setGUIParamSetup_ArgSelect(-1);
        if (argOffset != prev) {
            disableParameterSetupListeners();
            // need to setup class and method comboboxes for new class/param selection
            ArgSelect argSelect = argListTest.get(argOffset);
            generateIterClassList(argSelect.argument, argSelect.cls, argSelect.method);
            TypeReference argType = copyArgListToGUI(); // update the GUI from the argList
            enableParameterSetupListeners();
            setGUIParamSetup_ModesEnabled(argType);     // update the parameter type selection
        }
    }//GEN-LAST:event_argPrevButtonActionPerformed

    private void typeDataStructRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeDataStructRadioButtonActionPerformed
        statusDisplayMessage();
        argRadioButtonActionPerformed ("typeDataStructRadioButton", ArgSelect.ParamMode.DataStruct);
    }//GEN-LAST:event_typeDataStructRadioButtonActionPerformed

    private void typeSimpletonRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeSimpletonRadioButtonActionPerformed
        statusDisplayMessage();
        argRadioButtonActionPerformed ("typeSimpletonRadioButton", ArgSelect.ParamMode.Simpleton);
    }//GEN-LAST:event_typeSimpletonRadioButtonActionPerformed

    private void typePrimitiveRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typePrimitiveRadioButtonActionPerformed
        statusDisplayMessage();
        argRadioButtonActionPerformed ("typePrimitivenRadioButton", ArgSelect.ParamMode.Primitive);
    }//GEN-LAST:event_typePrimitiveRadioButtonActionPerformed

    private void typeStringRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeStringRadioButtonActionPerformed
        statusDisplayMessage();
        argRadioButtonActionPerformed ("typeStringRadioButton", ArgSelect.ParamMode.String);
    }//GEN-LAST:event_typeStringRadioButtonActionPerformed

    private void typeArrayRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeArrayRadioButtonActionPerformed
        statusDisplayMessage();
        argRadioButtonActionPerformed ("typeArrayRadioButton", ArgSelect.ParamMode.Array);
    }//GEN-LAST:event_typeArrayRadioButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // restore stdout and stderr
        System.setOut(this.standardOut);
        System.setErr(this.standardErr);
        
        // save debug screen contents to log file in project path
        String fname = getSPFBasePath() + "janalyzer.log";
        File file = new File(fname);
        file.delete();
        
        DateFormat dfmt = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        Date dateobj = new Date();
        String content = dfmt.format(dateobj) + newLine + "---------------------------------" + newLine;
        content += debugTextPane.getText();
        if (!content.isEmpty()) {
            try {
                FileUtils.writeStringToFile(file, content, "UTF-8");
            } catch (IOException ex) {
                // ignore error
            }
        }
    }//GEN-LAST:event_formWindowClosing

    private void wcaPolicyRangeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wcaPolicyRangeCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "wcaPolicyRangeCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // update the range selection
        JCheckBox checkBox = (JCheckBox) evt.getSource();
        setGUI_spfSetRange (checkBox, wcaPolicyEndSpinner, (Integer) this.wcaPolicySpinner.getValue());

        // update the calculated number of iterations to run
        String iterations = "";
        Integer count = getSpfIterations(Options.ConfigType.WCA);
        if (count > 1)
            iterations = "Number of iterations to run: " + count;
        wcaMultirunLabel.setText(iterations);
    }//GEN-LAST:event_wcaPolicyRangeCheckBoxActionPerformed

    private void wcaHistoryRangeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wcaHistoryRangeCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "wcaHistoryRangeCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // update the range selection
        JCheckBox checkBox = (JCheckBox) evt.getSource();
        setGUI_spfSetRange (checkBox, wcaHistoryEndSpinner, (Integer) this.wcaHistorySpinner.getValue());

        // update the calculated number of iterations to run
        String iterations = "";
        Integer count = getSpfIterations(Options.ConfigType.WCA);
        if (count > 1)
            iterations = "Number of iterations to run: " + count;
        wcaMultirunLabel.setText(iterations);
    }//GEN-LAST:event_wcaHistoryRangeCheckBoxActionPerformed

    private void wcaPolicySpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_wcaPolicySpinnerStateChanged
        statusDisplayMessage();
        String itemName = "wcaPolicySpinner";
        debugDisplayEvent (DebugEvent.StateChanged, itemName, null);

        // update the calculated number of iterations to run
        String iterations = "";
        Integer count = getSpfIterations(Options.ConfigType.WCA);
        if (count > 1)
            iterations = "Number of iterations to run: " + count;
        wcaMultirunLabel.setText(iterations);
    }//GEN-LAST:event_wcaPolicySpinnerStateChanged

    private void wcaHistorySpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_wcaHistorySpinnerStateChanged
        statusDisplayMessage();
        String itemName = "wcaHistorySpinner";
        debugDisplayEvent (DebugEvent.StateChanged, itemName, null);

        // update the calculated number of iterations to run
        String iterations = "";
        Integer count = getSpfIterations(Options.ConfigType.WCA);
        if (count > 1)
            iterations = "Number of iterations to run: " + count;
        wcaMultirunLabel.setText(iterations);
    }//GEN-LAST:event_wcaHistorySpinnerStateChanged

    private void wcaPolicyEndSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_wcaPolicyEndSpinnerStateChanged
        statusDisplayMessage();
        String itemName = "wcaPolicyEndSpinner";
        debugDisplayEvent (DebugEvent.StateChanged, itemName, null);

        // update the calculated number of iterations to run
        String iterations = "";
        Integer count = getSpfIterations(Options.ConfigType.WCA);
        if (count > 1)
            iterations = "Number of iterations to run: " + count;
        wcaMultirunLabel.setText(iterations);
    }//GEN-LAST:event_wcaPolicyEndSpinnerStateChanged

    private void wcaHistoryEndSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_wcaHistoryEndSpinnerStateChanged
        statusDisplayMessage();
        String itemName = "wcaHistoryEndSpinner";
        debugDisplayEvent (DebugEvent.StateChanged, itemName, null);

        // update the calculated number of iterations to run
        String iterations = "";
        Integer count = getSpfIterations(Options.ConfigType.WCA);
        if (count > 1)
            iterations = "Number of iterations to run: " + count;
        wcaMultirunLabel.setText(iterations);
    }//GEN-LAST:event_wcaHistoryEndSpinnerStateChanged

    private void scSolverComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scSolverComboBoxActionPerformed
        statusDisplayMessage();
        String itemName = "scSolverComboBox";
        String chosenStr = (String) this.scSolverComboBox.getSelectedItem();
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, chosenStr);

        // the bvlen selection is only valid for z3bitvector solver
        if (chosenStr.equals("z3bitvector"))
            this.scBvlenSpinner.setEnabled(true);
        else
            this.scBvlenSpinner.setEnabled(false);
    }//GEN-LAST:event_scSolverComboBoxActionPerformed

    private void scSymTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scSymTypeComboBoxActionPerformed
        statusDisplayMessage();
        String itemName = "scSymTypeComboBox";
        String chosenStr = (String)this.scSymTypeComboBox.getSelectedItem();
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, chosenStr);

        // set the min and max limits (and current value if out of range)
        // when the data type is changed.
        SymLimits limit = scSymLimits.getLimits(chosenStr);
        this.scSymMinTextField.setText(limit.min);
        this.scSymMaxTextField.setText(limit.max);
    }//GEN-LAST:event_scSymTypeComboBoxActionPerformed

    private void scSymMinTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_scSymMinTextFieldFocusLost
        statusDisplayMessage();
        String itemName = "scSymMinTextField";

        JFormattedTextField minValue = (JFormattedTextField)evt.getSource();
        JFormattedTextField maxValue = scSymMaxTextField;

        // get the data type selection
        String dtype = (String)this.scSymTypeComboBox.getSelectedItem();
        if (dtype == null) {
            debugDisplayEvent (DebugEvent.FocusLost, itemName, "no symbol selection");
            return;
        }
        debugDisplayEvent (DebugEvent.FocusLost, itemName, dtype);

        // make sure the current value is within the specified range limits
        String result = absLimitCheck(dtype, minValue.getText());
        if (result != null)
            minValue.setText(result);
        boolean bExceeds = isMinExceedMax(dtype, minValue.getText(),
                                                 maxValue.getText());
        if (bExceeds == true)
            maxValue.setText(minValue.getText());
                
        // update the class limit parameter for the current data type selection
        saveSymbolValues(Options.ConfigType.SideChannel, dtype);
    }//GEN-LAST:event_scSymMinTextFieldFocusLost

    private void scSymMaxTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_scSymMaxTextFieldFocusLost
        statusDisplayMessage();
        String itemName = "scSymMaxTextField";

        JFormattedTextField maxValue = (JFormattedTextField)evt.getSource();
        JFormattedTextField minValue = scSymMinTextField;

        // get the data type selection
        String dtype = (String)this.scSymTypeComboBox.getSelectedItem();
        if (dtype == null) {
            debugDisplayEvent (DebugEvent.FocusLost, itemName, "no symbol selection");
            return;
        }
        debugDisplayEvent (DebugEvent.FocusLost, itemName, dtype);

        // make sure the current value is within the specified range limits
        String result = absLimitCheck(dtype, maxValue.getText());
        if (result != null)
            maxValue.setText(result);
        boolean bExceeds = isMinExceedMax(dtype, minValue.getText(),
                                                 maxValue.getText());
        if (bExceeds == true)
            minValue.setText(maxValue.getText());
                
        // update the class limit parameter for the current data type selection
        saveSymbolValues(Options.ConfigType.SideChannel, dtype);
    }//GEN-LAST:event_scSymMaxTextFieldFocusLost

    private void scCreateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scCreateButtonActionPerformed
        statusDisplayMessage();
        String itemName = "scCreateButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // determine if no settings have been changed that should cause the
        // driver file to change (and it has already been created)
        boolean bConfigOnly = false;
        if (bFlag_Created && !checkIfDriverSettingsChanged()) {
            bConfigOnly = true;
        }

        // if the user has made changes to the previously created driver files,
        // query if the user wants really to overwrite his changes.
        if (bFlag_Modified) {
            String title   = "Driver files have been modified";
            if (bConfigOnly) {
                String message = "No parameters have changed that affect the driver file and" + newLine +
                          " you have made manual changes to either the driver or config file" + newLine +
                          " which will be lost." + newLine +
                          " You may choose:" + newLine + 
                          "  -Yes-    to update the driver and config files," + newLine +
                          "  -Config- to update only the config file," + newLine +
                          "  -No- to exit without making any changes." + newLine + newLine +
                          "Do you want to overwrite your files?";
                String[] yesno = new String[] { "No", "Config", "Yes" };
                int which = JOptionPane.showOptionDialog(null, // parent component
                            message,        // message
                            title,          // title
                            JOptionPane.DEFAULT_OPTION, // option type
                            JOptionPane.PLAIN_MESSAGE,  // message type
                            null,           // icon
                            yesno,          // selection options
                            yesno[0]);      // initial value
                if (which < 0 || yesno[which].equals("No"))
                    return;
                if (yesno[which].equals("Yes"))
                    bConfigOnly = false;
            }
            else {
                String message = "You have made manual changes to either the driver or config file" + newLine +
                          " which will be lost if they are regenerated." + newLine +
                          " You may choose:" + newLine + 
                          "  -Yes-    to update the driver and config files," + newLine +
                          "  -No- to exit without making any changes." + newLine + newLine +
                          "Do you want to overwrite your files?";
                String[] yesno = new String[] { "No", "Yes" };
                int which = JOptionPane.showOptionDialog(null, // parent component
                            message,        // message
                            title,          // title
                            JOptionPane.DEFAULT_OPTION, // option type
                            JOptionPane.PLAIN_MESSAGE,  // message type
                            null,           // icon
                            yesno,          // selection options
                            yesno[0]);      // initial value
                if (which < 0 || yesno[which].equals("No"))
                    return;
            }
        }

        // if a driver file already exists and we haven't nagged the user about it yet,
        // prompt him whether to overwrite the files or not.
        // - only check for the driver file, not the config file(s) for brevity
        String driverPathName = getSPFDriverPath();
        File driverpath = new File(driverPathName);
        if (driverpath.exists()) {
            File drvfile = new File(driverPathName + Util.getDriverFileName(projectName));
            if (drvfile.exists()) {
                // If the the file exists and user hasn't been prompted for overwrite,
                // warn him that he will be overwriting existing files.
                // Once he has committed this act, we will no longer prompt him.
                if (!bFlag_overwrite) {
                    // query if the user wants to overwrite the existing files
                    String[] yesno = new String[] { "No", "Yes" };
                    String title   = "Driver file currently exists";
                    String message = "You may may choose 'Yes' to overwrite file " + Util.getDriverFileName(projectName) + newLine +
                                  " or 'No' to load the existing driver file." + newLine + newLine +
                                  "Do you want to overwrite the existing files?";
                    int which = JOptionPane.showOptionDialog(null, // parent component
                        message,        // message
                        title,          // title
                        JOptionPane.DEFAULT_OPTION, // option type
                        JOptionPane.PLAIN_MESSAGE,  // message type
                        null,           // icon
                        yesno,          // selection options
                        yesno[0]);      // initial value
                    if (which < 0 || yesno[which].equals("No")) {
                        // load existing driver file
                        String driver = loadDriverFile();
                        
                        // display the driver file and init the text color to black
                        driverPanelSetup(DriverType.Driver, driver);
                        
                        // indicate only the config file(s) need to be generated
                        bConfigOnly = true;
                    }

                    // indicate the user has accepted responsibility for overwriting
                    // existing files so we aren't nagging him every time.
                    bFlag_overwrite = true;
                }
            }
        }

        // create the driver files
        if (bConfigOnly)
            createConfigFilesOnly (Options.ConfigType.SideChannel);
        else
            createDriverFiles (Options.ConfigType.SideChannel);
    }//GEN-LAST:event_scCreateButtonActionPerformed

    private void cloudJobsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_cloudJobsTableMouseClicked
        statusDisplayMessage();
        String itemName = "cloudJobsTable";

        bPauseCloudUpdate = true;
        int row = cloudJobsTable.rowAtPoint(evt.getPoint());
        int col = cloudJobsTable.columnAtPoint(evt.getPoint());
        String entry = (String)cloudJobsTable.getValueAt(row, col);
        String colname = cloudJobsTable.getColumnName(col);
        debugDisplayEvent (DebugEvent.MouseClicked, itemName, "(" + row + "," + col + ") -> " + colname + " = " + entry);

        // this allows the user to select the test results to download
        selectFileToDownload (row);
        bPauseCloudUpdate = false;
    }//GEN-LAST:event_cloudJobsTableMouseClicked

    private void wcaBvlenSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_wcaBvlenSpinnerStateChanged
        statusDisplayMessage();
        String itemName = "wcaBvlenSpinner";
        debugDisplayEvent (DebugEvent.StateChanged, itemName, null);
    }//GEN-LAST:event_wcaBvlenSpinnerStateChanged

    private void wcaInputMaxSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_wcaInputMaxSpinnerStateChanged
        statusDisplayMessage();
        String itemName = "wcaInputMaxSpinner";
        debugDisplayEvent (DebugEvent.StateChanged, itemName, null);
    }//GEN-LAST:event_wcaInputMaxSpinnerStateChanged

    private void wcaCostModelComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wcaCostModelComboBoxActionPerformed
        statusDisplayMessage();
        String itemName = "wcaCostModelComboBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);
    }//GEN-LAST:event_wcaCostModelComboBoxActionPerformed

    private void wcaHeuristicCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wcaHeuristicCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "wcaHeuristicCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);
    }//GEN-LAST:event_wcaHeuristicCheckBoxActionPerformed

    private void wcaDebugCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wcaDebugCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "wcaDebugCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);
    }//GEN-LAST:event_wcaDebugCheckBoxActionPerformed

    private void scBvlenSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_scBvlenSpinnerStateChanged
        statusDisplayMessage();
        String itemName = "scBvlenSpinner";
        debugDisplayEvent (DebugEvent.StateChanged, itemName, null);
    }//GEN-LAST:event_scBvlenSpinnerStateChanged

    private void scTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scTypeComboBoxActionPerformed
        statusDisplayMessage();
        String itemName = "scTypeComboBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);
    }//GEN-LAST:event_scTypeComboBoxActionPerformed

    private void scDebugCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scDebugCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "scDebugCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);
    }//GEN-LAST:event_scDebugCheckBoxActionPerformed

    private void useCloudCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useCloudCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "useCloudCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);
    }//GEN-LAST:event_useCloudCheckBoxActionPerformed

    private void userFilterCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userFilterCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "userFilterCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;

        if (userFilterCheckBox.isSelected())
            userFilterTextField.setEnabled(true);
        else
            userFilterTextField.setEnabled(false);
    }//GEN-LAST:event_userFilterCheckBoxActionPerformed

    private void nameFilterCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nameFilterCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "nameFilterCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;

        if (nameFilterCheckBox.isSelected())
            nameFilterTextField.setEnabled(true);
        else
            nameFilterTextField.setEnabled(false);
    }//GEN-LAST:event_nameFilterCheckBoxActionPerformed

    private void earliestTimeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_earliestTimeCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "earliestTimeCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;

        if (earliestTimeCheckBox.isSelected()) {
            earliestTimeSpinner.setEnabled(true);
            latestTimeCheckBox.setEnabled(true);
            if (latestTimeCheckBox.isSelected())
                latestTimeSpinner.setEnabled(true);
        }
        else {
            earliestTimeSpinner.setEnabled(false);
            latestTimeCheckBox.setEnabled(false);
            latestTimeSpinner.setEnabled(false);
        }
    }//GEN-LAST:event_earliestTimeCheckBoxActionPerformed

    private void latestTimeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_latestTimeCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "latestTimeCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;

        if (latestTimeCheckBox.isSelected())
            latestTimeSpinner.setEnabled(true);
        else
            latestTimeSpinner.setEnabled(false);
    }//GEN-LAST:event_latestTimeCheckBoxActionPerformed

    private void buildButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buildButtonActionPerformed
        statusDisplayMessage();
        String itemName = "buildButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // clear the output window
        outputTextArea.setText("");
        
        // don't allow if user does not have SPF installed
        if (!isSPFInstalled()) {
            JOptionPane.showMessageDialog(null,
                    "SPF must be installed in order to run the driver.",
                    "SPF is not installed", JOptionPane.ERROR_MESSAGE);
            statusDisplayMessage(DebugType.Error, "SPF not installed");
            return;
        }
        
        // don't allow build if user has not created a file
        if (!bFlag_Created) {
            JOptionPane.showMessageDialog(null,
                    "Driver files have not been created",
                    "Missing Driver Files", JOptionPane.ERROR_MESSAGE);
            statusDisplayMessage(DebugType.Error, "Missing driver files");
            return;
        }
        
        // if settings have changed since last time Create was pressed,
        // automatically re-Create the files before building.
        if (checkIfDriverSettingsChanged()) {
            createDriverFiles (spfMode);
        }
        else if (checkIfConfigSettingsChanged()) {
            createConfigFilesOnly (spfMode);
        }
        else if (saveButton.isEnabled()) {
            // otherwise, check if user has made changes to driver files without
            // saving them, and ask whether he want to Save the changes or
            // Undo them by re-Create the files so they match the parameters.
            String[] selection = new String[] { "Save", "Undo" };
            String title   = "File changes not saved";
            String message = "The driver files have been manually edited" + newLine +
                             "without saving the changes." + newLine + newLine +
                             "Do you want to Save or Undo the changes ?";
            int which = JOptionPane.showOptionDialog(null, // parent component
                message,        // message
                title,          // title
                JOptionPane.DEFAULT_OPTION, // option type
                JOptionPane.PLAIN_MESSAGE,  // message type
                null,           // icon
                selection,      // selection options
                selection[0]);  // initial value
            if (which < 0 || selection[which].equals("Undo")) {
                // to undo the file changes, simply re-Create the files from the settings
                createDriverFiles (spfMode);
            }
            else {
                // otherwise, save the current edit changes to the files
                boolean bSuccess = saveDriverFile(driverTextPane.getText(), false);
                if (bSuccess) {
                    saveConfigFile(configTextPane.getText(), 0);

                    // disable the save button since the text contents should match the saved files
                    saveButton.setEnabled(false);
                    undoButton.setEnabled(false);
                }
            }
        }
        
        if (!checkIfDriverFilesExist()) {
            JOptionPane.showMessageDialog(null,
                    "One of more driver files were not found",
                    "Missing Driver Files", JOptionPane.ERROR_MESSAGE);
            statusDisplayMessage(DebugType.Error, "Missing driver files");
            return;
        }

        // now everything should be hunky-dory for building...
        
        // find the SPF jar
        String spfjar = "";
        try {
            Properties props = new Properties();
            props.load(new FileReader(JPF_CONFIG_PATH));
            String userhome = "${user.home}";
            spfjar =  props.getProperty("jpf-symbc").replace(userhome, System.getProperty("user.home")) + "/build/jpf-symbc-classes.jar";
        } catch (IOException e) {
            statusDisplayMessage(DebugType.IntError, "loading JPF site.properties from location: " + JPF_CONFIG_PATH);
            e.printStackTrace();
        }

        // change tab selection to show the Output window
        this.viewerTabbedPane.setSelectedIndex(2);
        this.viewerTabbedPane.update(this.viewerTabbedPane.getGraphics());
        
        // determine whether to use cloud to run SPF
        boolean bCloud = useCloudCheckBox.isSelected();

        // re-direct stdout and stderr to the text window
        PrintStream printStream = new PrintStream(new RedirectOutputStream(this.outputTextArea)); 
        System.setOut(printStream);
        System.setErr(printStream);

        // compile the driver file and create the zip file
        compileDriverFile (spfjar, bCloud);

        // restore stdout and stderr
        System.setOut(this.standardOut);
        System.setErr(this.standardErr);
    }//GEN-LAST:event_buildButtonActionPerformed

    private void userFilterTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userFilterTextFieldActionPerformed
        statusDisplayMessage();
        String itemName = "userFilterTextField";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;
    }//GEN-LAST:event_userFilterTextFieldActionPerformed

    private void userFilterTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_userFilterTextFieldFocusLost
        statusDisplayMessage();
        String itemName = "userFilterTextField";
        debugDisplayEvent (DebugEvent.FocusLost, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;
    }//GEN-LAST:event_userFilterTextFieldFocusLost

    private void nameFilterTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nameFilterTextFieldActionPerformed
        statusDisplayMessage();
        String itemName = "nameFilterTextField";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;
    }//GEN-LAST:event_nameFilterTextFieldActionPerformed

    private void nameFilterTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_nameFilterTextFieldFocusLost
        statusDisplayMessage();
        String itemName = "nameFilterTextField";
        debugDisplayEvent (DebugEvent.FocusLost, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;
    }//GEN-LAST:event_nameFilterTextFieldFocusLost

    private void earliestTimeSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_earliestTimeSpinnerStateChanged
        statusDisplayMessage();
        String itemName = "earliestTimeSpinner";
        debugDisplayEvent (DebugEvent.StateChanged, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;
    }//GEN-LAST:event_earliestTimeSpinnerStateChanged

    private void latestTimeSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_latestTimeSpinnerStateChanged
        statusDisplayMessage();
        String itemName = "latestTimeSpinner";
        debugDisplayEvent (DebugEvent.StateChanged, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;
    }//GEN-LAST:event_latestTimeSpinnerStateChanged
    
    private void cloudJobsTableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_cloudJobsTableKeyPressed
        statusDisplayMessage();
        String itemName = "cloudJobsTable";
        int  keycode = evt.getKeyCode();
        debugDisplayEvent (DebugEvent.KeyPressed, itemName, "code = " + keycode);

        switch (keycode) {
            case KeyEvent.VK_ENTER: // ENTER key
                debugDisplayMessage (DebugType.Normal, "<VK_ENTER> : row = " + cloudRowSelection);
                if (cloudRowSelection >= 0)
                    selectFileToDownload (cloudRowSelection);
                
                // the ENTER key will by default advance the row selection.
                // let's restore it, since we are using it to download files instead.
                // TODO: disabled for now. the row selection is reset correctly,
                //       but the display still highlights the next line.
                //       If we enable this line, it causes the up/down arrows
                //       to behave funny because the hilighted row does not match
                //       the value of cloudRowSelection anymore.
//                cloudJobsTable.setRowSelectionInterval(cloudRowSelection, cloudRowSelection);
                break;
            /*
            // These didn't work correctly - they caused the cursor selection to
            // move rather randomly. These are now handled using KeyBindings for
            // the table.
            case KeyEvent.VK_UP:    // UP ARROW key
            case KeyEvent.VK_DOWN:  // DOWN ARROW key
                break;
            */
                
            default:
                break;
        }
    }//GEN-LAST:event_cloudJobsTableKeyPressed

    private void testMethodRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testMethodRadioButtonActionPerformed
        statusDisplayMessage();

        testMethodRadioButton.setSelected(true);
        iterMethodRadioButton.setSelected(false);
        setGUI_ParamInfoPanel(0);
    }//GEN-LAST:event_testMethodRadioButtonActionPerformed

    private void iterMethodRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_iterMethodRadioButtonActionPerformed
        statusDisplayMessage();

        testMethodRadioButton.setSelected(false);
        iterMethodRadioButton.setSelected(true);
        setGUI_ParamInfoPanel(0);
    }//GEN-LAST:event_iterMethodRadioButtonActionPerformed

    private void undoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoButtonActionPerformed
        statusDisplayMessage();
        String itemName = "undoButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // get existing driver contents
        String driver, config;
        String fname = getSPFDriverPath() + Util.getDriverFileName(projectName);
        File drv_file = new File(fname);
        if (!drv_file.isFile()) {
            statusDisplayMessage(DebugType.Error, "File not found: " + fname);
            return;
        }
        try {
            driver = FileUtils.readFileToString(drv_file, "UTF-8");
        } catch (IOException ex) {
            statusDisplayMessage(DebugType.Error, "Reading from file: " + fname);
            return;
        }
        Integer count = getSpfIterations (WCAFrame.spfMode);
        int id = (count <= 1) ? 0 : 1;
        fname = getSPFDriverPath() + Util.getConfigFileName(projectName, id);
        File cfg_file = new File(fname);
        if (!cfg_file.isFile()) {
            statusDisplayMessage(DebugType.Error, "File not found: " + fname);
            return;
        }
        try {
            config = FileUtils.readFileToString(cfg_file, "UTF-8");
        } catch (IOException ex) {
            statusDisplayMessage(DebugType.Error, "Reading from file: " + fname);
            return;
        }

	// display the files and init the text color to black
        driverPanelSetup(DriverType.Driver, driver);
        driverPanelSetup(DriverType.Config, config);

        // disable the save button since the text contents should match the saved files
        saveButton.setEnabled(false);
        undoButton.setEnabled(false);
    }//GEN-LAST:event_undoButtonActionPerformed

    private void prjnameTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prjnameTextFieldFocusLost
        statusDisplayMessage();
        String itemName = "prjnameTextField";
        debugDisplayEvent (DebugEvent.FocusLost, itemName, null);

        // verify the data is valid
        String project = this.prjnameTextField.getText();
        if (project.length() != findEndOfField(project)) {
            // nope - let's restore the previous value
            this.prjnameTextField.setText(projectName);
            // and notify user of the format of the project name entry
            JOptionPane.showMessageDialog(null,
                "Project Name must consist only of alphanumeric characters" + newLine +
                "and the underscore (_) character." + newLine + newLine +
                "Please rename using valid characters only.",
                "Invalid characters in Project Name", JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            // it's valid -save it
            projectName = this.prjnameTextField.getText();
            // init the cloud job name to the same
            this.cloudNameTextField.setText(projectName);

            // if we have generated driver files already, generate them again using the new filename.
            if (bFlag_Created) {
                createDriverFiles (spfMode);
            }
        }
    }//GEN-LAST:event_prjnameTextFieldFocusLost
    
    private void prjnameTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prjnameTextFieldFocusGained
        statusDisplayMessage();
        String itemName = "prjnameTextField";
        debugDisplayEvent (DebugEvent.FocusGained, itemName, null);

        // save the current project name in case we have to restore it
        projectName = this.prjnameTextField.getText();
    }//GEN-LAST:event_prjnameTextFieldFocusGained

    private void cloudExecutorsTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cloudExecutorsTextFieldActionPerformed
        statusDisplayMessage();
        String itemName = "cloudExecutorsTextField";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // verify output is valid
        if (!verifyIntegerValue(cloudExecutorsTextField.getText(), 1, 999999999))
            cloudExecutorsTextField.setText("");
    }//GEN-LAST:event_cloudExecutorsTextFieldActionPerformed

    private void cloudInitialDepthTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cloudInitialDepthTextFieldActionPerformed
        statusDisplayMessage();
        String itemName = "cloudInitialDepthTextField";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // verify output is valid
        if (!verifyIntegerValue(cloudInitialDepthTextField.getText(), 1, 999999999))
            cloudInitialDepthTextField.setText("");
    }//GEN-LAST:event_cloudInitialDepthTextFieldActionPerformed

    private void cloudExecutorsTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_cloudExecutorsTextFieldFocusLost
        statusDisplayMessage();
        String itemName = "cloudExecutorsTextField";
        debugDisplayEvent (DebugEvent.FocusLost, itemName, null);

        // verify output is valid
        if (!verifyIntegerValue(cloudExecutorsTextField.getText(), 1, 999999999))
            cloudExecutorsTextField.setText("");
    }//GEN-LAST:event_cloudExecutorsTextFieldFocusLost

    private void cloudInitialDepthTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_cloudInitialDepthTextFieldFocusLost
        statusDisplayMessage();
        String itemName = "cloudInitialDepthTextField";
        debugDisplayEvent (DebugEvent.FocusLost, itemName, null);

        // verify output is valid
        if (!verifyIntegerValue(cloudInitialDepthTextField.getText(), 1, 999999999))
            cloudInitialDepthTextField.setText("");
    }//GEN-LAST:event_cloudInitialDepthTextFieldFocusLost

    private void sessionOnlyCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sessionOnlyCheckBoxActionPerformed
        statusDisplayMessage();
        String itemName = "sessionOnlyCheckBox";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        bCloudFilterUpdate = true;
        cloudRowSelection = -1;
    }//GEN-LAST:event_sessionOnlyCheckBoxActionPerformed

    private void prjpathTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prjpathTextFieldFocusLost
        statusDisplayMessage();
        String itemName = "prjpathTextField";
        debugDisplayEvent (DebugEvent.FocusLost, itemName, null);

        updateProjectPathSelection(this.prjpathTextField.getText());
    }//GEN-LAST:event_prjpathTextFieldFocusLost

    private void prjpathTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prjpathTextFieldActionPerformed
        statusDisplayMessage();
        String itemName = "prjpathTextField";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        updateProjectPathSelection(this.prjpathTextField.getText());
    }//GEN-LAST:event_prjpathTextFieldActionPerformed

    private void mctsSelectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mctsSelectButtonActionPerformed
        statusDisplayMessage();
        String itemName = "SelectZipFile";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // allow the user to make a file selection for the zip file
        File dfltPath = new File(getSPFDriverPath());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Zip Files","zip");
        this.jpfZipFileChooser.setFileFilter(filter);
        this.jpfZipFileChooser.setCurrentDirectory(dfltPath);
        this.jpfZipFileChooser.setMultiSelectionEnabled(false);
        int retVal = this.jpfZipFileChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            // get the user's selection
            File folder = this.jpfZipFileChooser.getSelectedFile();
            String zipFilename = folder.getAbsolutePath();

            // find the jpf files (if any) in the selected zip file
            File zipFile = new File(zipFilename);
            try {
                ArrayList<String> jpfList = new ArrayList();
                ZipInputStream zip = new ZipInputStream(zipFile.toURI().toURL().openStream());
                while (true) {
                    ZipEntry entry = zip.getNextEntry();
                    if (entry == null)
                        break;
                    String fullname = entry.getName();
                    if (fullname.endsWith(".jpf")) {
                        jpfList.add(fullname);
                        debugDisplayMessage (DebugType.Normal, "zip file contained: " + fullname);
                    }
                }
                
                // check if the zip file contained at least 1 jpf file (required)
                if (jpfList.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                        "File: " + zipFilename + newLine +
                        "does not contain .jpf file",
                        "Invalid File", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    // save the zip filename & copy the files to the combo box
                    this.mctsZipFilename = zipFilename;
                    this.mctsJpfFileComboBox.removeAllItems();
                    for (String jpfFile : jpfList) {
                        this.mctsJpfFileComboBox.addItem(jpfFile);
                    }
                    
                    // display the selected zip file
                    this.mctsZipFileTextField.setText(mctsZipFilename);
                    // display the 1st entry in jpf list as iniital selection
                    this.mctsJpfFileComboBox.setSelectedIndex(0);
                }
            } catch (Exception ex) {
                statusDisplayMessage(DebugType.Error, "Zip file read failure");
                debugDisplayMessage (DebugType.Error, ex.getMessage());
            }
        }
    }//GEN-LAST:event_mctsSelectButtonActionPerformed

    private void mctsSubmitJobButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mctsSubmitJobButtonActionPerformed
        statusDisplayMessage();
        String itemName = "submitMCTSJobButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        File zipFile = new File(this.mctsZipFilename);
        if (!zipFile.isFile()) {
            statusDisplayMessage(DebugType.Error, "Zip file not found for cloud job.");
            return;
        }
        
        String jpfFile = (String)this.mctsJpfFileComboBox.getSelectedItem();
        if (jpfFile == null) {
            statusDisplayMessage(DebugType.Error, "Jpf file selection not valid for cloud job.");
            return;
        }

        // start the cloud job and check for error
        CloudInterface cloud = new CloudInterface(debugmsg);
        this.currentMCTSJobId = cloud.postJob(zipFile, "128", "32", jpfFile);
        if (this.currentMCTSJobId.isEmpty()) {
            statusDisplayMessage(DebugType.Error, "MCTS job submission failed - jpf filename: " + this.mctsZipFilename);
        }
        else {
            statusDisplayMessage(DebugType.Normal, "MCTS job submission -  Job ID: " + this.currentMCTSJobId);
            // determine the type of graph
            gui.LiveTrackerChart.GraphType gtype = gui.LiveTrackerChart.GraphType.vsPathIx;
            if (mctsGraphRadioButton1.isSelected())
                gtype = gui.LiveTrackerChart.GraphType.vsSample;
            this.mctsChart = new LiveTrackerChart(4096, gtype);
            mctsCounter = 0;
            mctsBestReward = 0;
            mctsTimer.start();
            
            // reset the counter info
            mctsCountLabel.setText("0");
            mctsValidLabel.setText("0");
            
            // disable the radio buttons
            mctsGraphRadioButton1.setEnabled(false);
            mctsGraphRadioButton2.setEnabled(false);
            mctsGraphRadioButton3.setEnabled(false);
            
            // enable the Stop button and disable the Submit button
            mctsStopButton.setEnabled(true);
            mctsSubmitJobButton.setEnabled(false);
        }
    }//GEN-LAST:event_mctsSubmitJobButtonActionPerformed

    private void mctsStopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mctsStopButtonActionPerformed
        statusDisplayMessage();
        String itemName = "mctsStopButton";
        debugDisplayEvent (DebugEvent.ActionPerformed, itemName, null);

        // stop the timer
        mctsTimer.stop();

        if (mctsChart != null) {
            // save the last info in the debug buffer
            CloudInterface cloud = new CloudInterface(debugmsg);
            String result = cloud.getMCTSResult(currentMCTSJobId, "outputs", "results.json");
            java.lang.reflect.Type type = new TypeToken<HashMap<String, WorkerStatistics>>(){}.getType();
            HashMap<String, WorkerStatistics> map = (new Gson()).fromJson(result, type);
            if (map != null) {
                for (Map.Entry<String, WorkerStatistics> it : map.entrySet()) {
                    String key = it.getKey();
                    WorkerStatistics stats = it.getValue();
                    String index = key.substring(key.lastIndexOf("_") + 1);
                    debugDisplayMessage (DebugType.Normal, "MCTS results: path " + index + ", reward = " + stats.bestReward);
                }
            }
        
            // dispose of the chart
            this.currentMCTSJobId = "";
            this.mctsChart.dispose();
            this.mctsChart = null;
            statusDisplayMessage(DebugType.Normal, "MCTS job stopped");
            
            // re-enable the radio buttons
            mctsGraphRadioButton1.setEnabled(true);
            mctsGraphRadioButton2.setEnabled(true);
            mctsGraphRadioButton3.setEnabled(true);
            
            // disable the Stop button and enable the Submit button
            mctsStopButton.setEnabled(false);
            mctsSubmitJobButton.setEnabled(true);
        }
    }//GEN-LAST:event_mctsStopButtonActionPerformed

    private void mctsGraphRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mctsGraphRadioButton1ActionPerformed
        mctsGraphRadioButton1.setSelected(true);
        mctsGraphRadioButton2.setSelected(false);
        mctsGraphRadioButton3.setSelected(false);
    }//GEN-LAST:event_mctsGraphRadioButton1ActionPerformed

    private void mctsGraphRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mctsGraphRadioButton2ActionPerformed
        mctsGraphRadioButton1.setSelected(false);
        mctsGraphRadioButton2.setSelected(true);
        mctsGraphRadioButton3.setSelected(false);
    }//GEN-LAST:event_mctsGraphRadioButton2ActionPerformed

    private void mctsGraphRadioButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mctsGraphRadioButton3ActionPerformed
        mctsGraphRadioButton1.setSelected(false);
        mctsGraphRadioButton2.setSelected(false);
        mctsGraphRadioButton3.setSelected(true);
    }//GEN-LAST:event_mctsGraphRadioButton3ActionPerformed
   
    // specifies the debug message types (and, hence, the associated text color)
    public enum DebugType {
        IntError, Error, Warning, Success, Normal,
        Entry, Exit, EntryExit, ErrorExit,
        Event, Details, Argument, Options, Autoset;
    }
    
    // specifies the type of GUI event to report
    private enum DebugEvent {
        ActionPerformed, ActionListener, FocusLost, FocusGained, StateChanged, MouseClicked, KeyPressed;
    }

    private enum MethodType {
        Test, Iterative;
    }

    private enum DriverType {
        Driver, Config;
    }

    private enum ParamChange {
        None, Method, Class, DataStructMode;
    }
    
    private enum DownloadState {
        None, Requested, Received, Failure;
    }
   
    // the info saved in the table for each cloud job
    private class CloudTableInfo implements Comparable<CloudTableInfo> {
        public String run;
        public String jobname;
        public String jobid;
        public String user;
        public String status;
        public String startTime;
        public String elapsed;

        @Override
        public int compareTo(CloudTableInfo other) {
            return startTime.compareTo(other.startTime);
        }
    }
    
    private class CloudJob {
        public Integer run;      // the job number run 
        public Integer count;    // the number of instances run for this job
        public Integer index;    // the index of the current job
        public String  jobid;    // the job id returned from the cloud
        public DownloadState download; // download state
        public boolean bInformed; // true if download complete has been reported to user
        
        CloudJob (int run, int index, int count, String jobid) {
            this.run   = run;
            this.count = count;
            this.index = index;
            this.jobid = jobid;
            this.download = DownloadState.None;
            this.bInformed = false;
        }
    }        
    
    private final static String newLine = System.getProperty("line.separator");

    // static values
    private static int cloudnumber;
    private static Options.ConfigType  spfMode;

    // MCTS values
    private String mctsZipFilename = "";
    private LiveTrackerChart mctsChart;
    private String currentMCTSJobId = "";

    
    // swing objects
    private javax.swing.JTextPane configTextPane;
    private javax.swing.JTextPane driverTextPane;
    
    // this is a mapping of the Classes with the Methods for each
    private Map<String, Set<String>> libraryClassMap = new TreeMap<>();
    private Map<String, Set<String>> applicationClassMap = new TreeMap<>();
    private ConfigInfo     configInfo; // Janalyzer configuration info
    
    // listeners for the Test Setup frame
    private ActionListener listener_TestClass;
    private ActionListener listener_TestMethod;
    
    // listeners for the Parameter Setup frame
    private ActionListener listener_ParamClass;
    private ActionListener listener_ParamMethod;
    private ActionListener listener_ParamValue;
    private ActionListener listener_ParamSize;
    private ActionListener listener_ParamElement;
    private ActionListener listener_ParamPrimitive;
    private ActionListener listener_ParamArraySize;

    private String         projectName;    // the name of thye project specified by the user
    private int            argOffset;      // index offset for the scrollable method argument
    // the full method names for the sets examined
    private String         setMethodNameTest;
    // List of GUI parameter selections for the method.
    private ArrayList<ArgSelect>  argListTest;
    // these are the min and max ranges for the symbol data type selections
    // (one for WCA and the other for side channel)
    private SymTypes       wcaSymLimits;
    private SymTypes       scSymLimits;
    // these will contain the original stdout and stderr values to restore upon exit
    private PrintStream    standardOut;
    private PrintStream    standardErr; 

    // these hold the status flags for the driver files
    private boolean bFlag_overwrite;    // true if user has already okayed overwriting an existing file
    private boolean bFlag_Created;      // true if driver & config files have been created
    private boolean bFlag_Modified;     // true if files have been modified (create clears it)
    private boolean bFlag_Compiled;     // true if files have been compiled (save & create clears it)

    private Options.ConfigType  lastSpfMode;
    private Options             lastOptions;
    private int      lastRangeHistory;
    private int      lastRangePolicy;
    private int      debugEntryLevel;

    private ArrayList<CloudTableInfo> cloudJobList;
    private ArrayList<CloudJob>  cloudJobs;
    private long        mctsCounter;
    private long        mctsBestReward;
    private String      mctsBestRewardPath;
    private ArrayList<Long> mctsGraphResults;
    private Timer       cloudTimer;
    private Timer       mctsTimer;
    private CloudUpdateListener cloundListener;
    private MCTSUpdateListener  mctsListener;
    private String      zipFileName;
    private Integer     cloudJobsCount;
    private boolean     bPauseCloudUpdate;
    private boolean     bCloudFilterUpdate;
    private boolean     bCloudSortOrder;
    private int         cloudColSortSelection;
    private static int  cloudRowSelection;
    
    private HashMap<String, String> costModels;

    private ThreadLauncher threadLauncher;
    private CommandLauncher commandLauncher;
    private DebugMessage debugmsg;
    private DebugMessage statusmsg;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel argCountLabel;
    private javax.swing.JPanel argInfoPanel;
    private javax.swing.JButton argNextButton;
    private javax.swing.JButton argPrevButton;
    private javax.swing.JPanel argSelectPanel;
    private javax.swing.JPanel argSetupPanel;
    private javax.swing.JTabbedPane argTabbedPane;
    private javax.swing.JPanel argTypePanel;
    private javax.swing.JComboBox<String> arraySizeComboBox;
    private javax.swing.JLabel arraySizeLabel;
    private javax.swing.JButton buildButton;
    private javax.swing.JComboBox class1ComboBox;
    private javax.swing.JLabel class1Label;
    private javax.swing.JComboBox class2ComboBox;
    private javax.swing.JLabel class2Label;
    private javax.swing.JTextField cloudExecutorsTextField;
    private javax.swing.JPanel cloudFilterPanel;
    private javax.swing.JTextField cloudInitialDepthTextField;
    private javax.swing.JTable cloudJobsTable;
    private javax.swing.JTextField cloudNameTextField;
    private javax.swing.JPanel cloudPanel;
    private javax.swing.JScrollPane cloudScrollPane;
    private javax.swing.JPanel cloudSetupPanel;
    private javax.swing.JPanel cloudSummaryPanel;
    private javax.swing.JScrollPane configScrollPane;
    private javax.swing.JScrollPane debugScrollPane;
    private javax.swing.JTextPane debugTextPane;
    private javax.swing.JScrollPane driverScrollPane;
    private javax.swing.JLabel earliestLabel;
    private javax.swing.JCheckBox earliestTimeCheckBox;
    private javax.swing.JSpinner earliestTimeSpinner;
    private javax.swing.JComboBox<String> elementTypeComboBox;
    private javax.swing.JLabel elementTypeLabel;
    private javax.swing.JFileChooser folderChooser;
    private javax.swing.JRadioButton iterMethodRadioButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jobsFilteredLabel;
    private javax.swing.JTextField jobsFilteredTextField;
    private javax.swing.JLabel jobsTotalLabel;
    private javax.swing.JTextField jobsTotalTextField;
    private javax.swing.JFileChooser jpfZipFileChooser;
    private javax.swing.JLabel latestLabel;
    private javax.swing.JCheckBox latestTimeCheckBox;
    private javax.swing.JSpinner latestTimeSpinner;
    private javax.swing.JButton launchButton;
    private javax.swing.JSplitPane mainSplitPane;
    private javax.swing.JLabel mctsCountLabel;
    private javax.swing.JRadioButton mctsGraphRadioButton1;
    private javax.swing.JRadioButton mctsGraphRadioButton2;
    private javax.swing.JRadioButton mctsGraphRadioButton3;
    private javax.swing.JComboBox<String> mctsJpfFileComboBox;
    private javax.swing.JPanel mctsPanel;
    private javax.swing.JButton mctsSelectButton;
    private javax.swing.JButton mctsStopButton;
    private javax.swing.JButton mctsSubmitJobButton;
    private javax.swing.JLabel mctsValidLabel;
    private javax.swing.JTextField mctsZipFileTextField;
    private javax.swing.JPanel methSelectPanel1;
    private javax.swing.JComboBox method1ComboBox;
    private javax.swing.JLabel method1Label;
    private javax.swing.JComboBox method2ComboBox;
    private javax.swing.JLabel method2Label;
    private javax.swing.JLabel methodNameLabel;
    private javax.swing.JPanel methodPanel;
    private javax.swing.JCheckBox nameFilterCheckBox;
    private javax.swing.JTextField nameFilterTextField;
    private javax.swing.JLabel noParamsLabel;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JScrollPane paramListScrollPane;
    private javax.swing.JTextArea paramListTextArea;
    private javax.swing.JPanel parameterSetupPanel;
    private javax.swing.JComboBox<String> primitiveTypeComboBox;
    private javax.swing.JLabel primitiveTypeLabel;
    private javax.swing.JLabel prjnameLabel;
    private javax.swing.JTextField prjnameTextField;
    private javax.swing.JButton prjpathButton;
    private javax.swing.JLabel prjpathLabel;
    private javax.swing.JTextField prjpathTextField;
    private javax.swing.JButton saveButton;
    private javax.swing.JLabel scBvlenLabel;
    private javax.swing.JSpinner scBvlenSpinner;
    private javax.swing.JButton scCreateButton;
    private javax.swing.JCheckBox scDebugCheckBox;
    private javax.swing.JLabel scMultirunLabel;
    private javax.swing.JPanel scOptionPanel;
    private javax.swing.JPanel scSelectionPanel;
    private javax.swing.JComboBox<String> scSolverComboBox;
    private javax.swing.JLabel scSolverLabel;
    private javax.swing.JLabel scSymMaxLabel;
    private javax.swing.JFormattedTextField scSymMaxTextField;
    private javax.swing.JLabel scSymMinLabel;
    private javax.swing.JFormattedTextField scSymMinTextField;
    private javax.swing.JComboBox<String> scSymTypeComboBox;
    private javax.swing.JLabel scSymTypeLabel;
    private javax.swing.JPanel scSymbolRangePanel;
    private javax.swing.JComboBox<String> scTypeComboBox;
    private javax.swing.JLabel scTypeLabel;
    private javax.swing.JCheckBox sessionOnlyCheckBox;
    private javax.swing.JLabel sessionOnlyLabel;
    private javax.swing.JPanel setupPanel;
    private javax.swing.JTabbedPane setupTabbedPane;
    private javax.swing.JPanel sideChanSetupPanel;
    private javax.swing.JComboBox<String> sizeSelectComboBox;
    private javax.swing.JLabel sizeSelectLabel;
    private javax.swing.JPanel spfExecutePanel;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JScrollPane statusScrollPane;
    private javax.swing.JTextPane statusTextPane;
    private javax.swing.JButton stopButton;
    private javax.swing.JPanel testMethodPanel;
    private javax.swing.JRadioButton testMethodRadioButton;
    private javax.swing.JRadioButton typeArrayRadioButton;
    private javax.swing.JRadioButton typeDataStructRadioButton;
    private javax.swing.JRadioButton typePrimitiveRadioButton;
    private javax.swing.JRadioButton typeSimpletonRadioButton;
    private javax.swing.JRadioButton typeStringRadioButton;
    private javax.swing.JButton undoButton;
    private javax.swing.JCheckBox useCloudCheckBox;
    private javax.swing.JCheckBox userFilterCheckBox;
    private javax.swing.JTextField userFilterTextField;
    private javax.swing.JComboBox<String> valueSelectComboBox;
    private javax.swing.JLabel valueSelectLabel;
    private javax.swing.JTabbedPane viewerTabbedPane;
    private javax.swing.JLabel wcaBvlenLabel;
    private javax.swing.JSpinner wcaBvlenSpinner;
    private javax.swing.JComboBox<String> wcaCostModelComboBox;
    private javax.swing.JLabel wcaCostModelLabel;
    private javax.swing.JButton wcaCreateButton;
    private javax.swing.JCheckBox wcaDebugCheckBox;
    private javax.swing.JCheckBox wcaHeuristicCheckBox;
    private javax.swing.JSpinner wcaHistoryEndSpinner;
    private javax.swing.JCheckBox wcaHistoryRangeCheckBox;
    private javax.swing.JLabel wcaHistorySizeLabel;
    private javax.swing.JSpinner wcaHistorySpinner;
    private javax.swing.JLabel wcaInputMaxLabel;
    private javax.swing.JSpinner wcaInputMaxSpinner;
    private javax.swing.JLabel wcaMultirunLabel;
    private javax.swing.JPanel wcaOptionPanel;
    private javax.swing.JSpinner wcaPolicyEndSpinner;
    private javax.swing.JLabel wcaPolicyLabel;
    private javax.swing.JCheckBox wcaPolicyRangeCheckBox;
    private javax.swing.JSpinner wcaPolicySpinner;
    private javax.swing.JPanel wcaSelectionPanel;
    private javax.swing.JPanel wcaSetupPanel;
    private javax.swing.JComboBox<String> wcaSolverComboBox;
    private javax.swing.JLabel wcaSolverLabel;
    private javax.swing.JLabel wcaSymMaxLabel;
    private javax.swing.JFormattedTextField wcaSymMaxTextField;
    private javax.swing.JLabel wcaSymMinLabel;
    private javax.swing.JFormattedTextField wcaSymMinTextField;
    private javax.swing.JComboBox<String> wcaSymTypeComboBox;
    private javax.swing.JLabel wcaSymTypeLabel;
    private javax.swing.JPanel wcaSymbolRangePanel;
    // End of variables declaration//GEN-END:variables
}
