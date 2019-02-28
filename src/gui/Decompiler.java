/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import drivergen.Util;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import org.apache.commons.io.FileUtils;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

/**
 *
 * @author dmcd2356
 */
public class Decompiler extends javax.swing.JFrame {

    /**
     * Creates new form Decompiler
     * 
     * @param configInfo  - configuration info from Analyzer Frame
     */
    public Decompiler(ConfigInfo configInfo) {
        initComponents();

        // save passed params
        this.configInfo = configInfo;

        // save original stdout and stderr for restoring after we redirect to output window
        this.standardOut = System.out;
        this.standardErr = System.err;         

        String projpath = configInfo.getField(ConfigInfo.StringType.projectpath);
        this.srcpathname = projpath + "/janalyzer/decompiler/src/";
        this.zippathname = projpath + "/janalyzer/decompiler/zip/";
        this.baseJarPath = projpath + "/";
        this.toolPathName = System.getProperty("user.dir") + "/tool/decompiler/";

        this.jarList.setModel(new DefaultListModel());
        this.jobActive = false;
        this.jobCount = 0;

        // initially disable the Start button, since no jar files are selected yet
        this.startButton.setEnabled(false);
        this.stopButton.setEnabled(false);

        // create an elapsed timer
        elapsedTimer = new ElapsedTimer(elapsedTimeLabel);

        // this creates a command launcher on a separate thread
        threadLauncher = new ThreadLauncher(outputTextArea);
        
        // this creates a command launcher that runs from the current thread
        commandLauncher = new CommandLauncher();
        
        // get access to the audio player
        audioplayer = new AudioClipPlayer();
        
        // set the decompiler selection if it was specified
        String decompiler = configInfo.getField(ConfigInfo.StringType.decompiler);
        if (!decompiler.isEmpty()) {
            decompilerComboBox.setSelectedItem(decompiler);
        }

        // set the initial base directory to select jar files from to the base project dir
        this.jarFileChooser.setCurrentDirectory(new File(baseJarPath));

        // initially select the application files defined for the project (if any)
        // (just copy the entries & eliminate the project path portion)
        DefaultListModel listModel = (DefaultListModel)this.jarList.getModel();
        listModel.clear();
        DefaultListModel appList = configInfo.getList(ConfigInfo.ListType.application);
        for (int ix = 0; ix < appList.size(); ix++) {
            String entry = (String)appList.get(ix);
            if (entry.startsWith(baseJarPath)) {
                entry = entry.substring(baseJarPath.length());
                listModel.addElement(entry);
            }
        }
        if (!listModel.isEmpty())
            this.startButton.setEnabled(true);

        // add a mouse listener for allowing use of cut/copy/paste
        statusTextPane.addMouseListener(new ContextMenuMouseListener());
        outputTextArea.addMouseListener(new ContextMenuMouseListener());
//        jarlistScrollPane.addMouseListener(new ContextMenuMouseListener());
    }

    /**
     * This performs the actions to take upon completion of the thread command.
     */
    private class StandardTermination implements ThreadLauncher.ThreadAction {

        @Override
        public void allcompleted(ThreadLauncher.ThreadInfo threadInfo) {
            // restore the stdout and stderr
            System.out.flush();
            System.err.flush();
            System.setOut(standardOut);
            System.setErr(standardErr);

            // update status display
            if (threadInfo.exitcode == 0) {
                statusMessage(StatusType.Info, "All Jobs completed");
                decompileCleanup(); // remove extraneous files generated
            }
            else {
                statusMessage(StatusType.Error, "Failure executing command.");
            }
            
            // re-enable start button and disable the stop button and the timer
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            elapsedTimer.stop();

            // update jobs pending
            jobCount = 0;
            jobsQueuedLabel.setText("");
            
            // play a sound to notify the user
            if ("yes".equals(configInfo.getField(ConfigInfo.StringType.audiofback)))
                audioplayer.playTada();
        }

        @Override
        public void jobprestart(ThreadLauncher.ThreadInfo threadInfo) {
            statusMessageJob(threadInfo.jobid, threadInfo.jobname, threadInfo.fname);
        }

        @Override
        public void jobstarted(ThreadLauncher.ThreadInfo threadInfo) {
            statusMessageJob(threadInfo.pid);
        }
        
        @Override
        public void jobfinished(ThreadLauncher.ThreadInfo threadInfo) {
            // update the items in the queue
            if (jobCount > 0)
                --jobCount;
            jobsQueuedLabel.setText(jobCount + " jobs queued");

            //outputTextArea.append(stdout.getText());
            statusMessageJob(threadInfo.exitcode);
        }
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
    private void appendToPane(JTextPane tp, String msg, Util.TextColor color, String font, int size, Util.FontType ftype)
    {
        AttributeSet aset = Util.setTextAttr(color, font, size, ftype);
        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg);
    }

    // same as above but with a fixed font of Courier 11 pt.
    private void appendToPane(JTextPane tp, String msg, Util.TextColor color, Util.FontType fonttype)
    {
        appendToPane(tp, msg, color, "Courier", 11, fonttype);
    }

    /**
     * generates space padding for text given the specfied size of the field and
     * the length of the text in it.
     * 
     * @param fieldlen - the length of the text field
     * @param textlen  - the number of chars currently occupying that field
     * @return a String of ASCII spaces to complete the field length (min length will be 1).
     */
    private String getPadding (int fieldlen, int textlen) {
        int length = (fieldlen - textlen) > 0 ? fieldlen - textlen : 1;
        String padding = new String(new char[length]).replace("\0", " ");
        return padding;
    }
    
    private boolean isTerminated (String text) {
        if (text.isEmpty())
            return true;

        int length = text.length();
        String lastChar = text.substring(length-1);
        if (newLine.equals(lastChar))
            return true;

        return false;
    }

    // remove the duplicate subclass files that have a '$' in them
    // (these classes are already embedded in the main class they are subclasses in)
    private void decompileCleanup () {
        statusMessage(StatusType.Info, "removing extraneous files...");

        String[] command = { "find", ".", "-name", "*.java" }; //, "-print" };
        int retcode = commandLauncher.start(command, srcpathname);
        if (retcode != 0) {
            statusMessage(StatusType.Info, "command error on 'find': " + retcode);
            return;
        }

        int count = 0;
        int total = 0;
        String response = commandLauncher.getResponse();
        statusMessage(StatusType.Info, "response length: " + response.length());
        try (Scanner scanner = new Scanner(response)) {
            while (scanner.hasNextLine()) {
                ++total;
                String line = scanner.nextLine();
                if (line.contains("$")) {
                    File file = new File (srcpathname + line);
                    if (file.isFile()) {
                        file.delete();
                        ++count;
                    }
                }
            }
        }
        statusMessage(StatusType.Info, "files processed: " + total);
        statusMessage(StatusType.Info, "files remaining: " + (total - count) + " (removed " + count + ")");
    }
    
    private enum StatusType {
        Info, Warning, Error;
    }

    /**
     * updates the graphics immediately for the status display
     */
    private void updateStatusDisplay () {
        // only perform this if the Status tab is selected in the output tabs.
        if ("Status".equals(outputTabbedPane.getTitleAt(
                            outputTabbedPane.getSelectedIndex())))
            this.statusTextPane.update(this.statusTextPane.getGraphics());
    }

    private void statusTerminate () {
        if (jobActive) {
            appendToPane(this.statusTextPane, newLine, Util.TextColor.Black, Util.FontType.Normal);
            jobActive = false;
        }
    }
    
    /**
     * outputs the various types of messages to the status display.
     * all messages will guarantee the previous line was terminated with a newline,
     * and will preceed the message with a timestamp value and terminate with a newline.
     * 
     * @param type    - the type of message
     * @param message - the message to display
     */
    private void statusMessage(StatusType type, String message) {
        // if we were in the middle of processing a jar, terminate the record
        statusTerminate();

        // display the timestamp
        String tstamp = "[" + elapsedTimer.getElapsed() + "] ";
        appendToPane(this.statusTextPane, tstamp,
                    Util.TextColor.Brown, Util.FontType.Bold);

        switch (type) {
            // the following preceed the message with a timestamp and terminate with a newline
            default:    // fall through...
            case Info:
                appendToPane(this.statusTextPane, message + newLine,
                            Util.TextColor.Black, Util.FontType.Normal);
                break;

            case Error:
                appendToPane(this.statusTextPane, "ERROR: " + message + newLine,
                            Util.TextColor.Red, Util.FontType.Bold);
                break;

            case Warning:
                appendToPane(this.statusTextPane, "WARNING: " + message + newLine,
                            Util.TextColor.Violet, Util.FontType.Bold);
                break;
        }

        // force an update
        updateStatusDisplay();
    }

    /**
     * outputs the job status when it has started.
     * (if this is the 1st job of a jar file, the jarfile name will be printed
     * on a line with the timestamp. the job info is then displayed followed by
     * the name of the process that the thread is running. this line is not
     * terminated by a newline to allow the status to be appended to it)
     * 
     * @param jobid - the id for the job
     * @param jobname - the name for the running job
     * @param fname - name of the file being processed
     */
    private void statusMessageJob(int jobid, String jobname, String fname) {
        jobActive = true;

        // if we were in the middle of processing a jar, terminate the record
        String tstamp = "";
        if (!isTerminated(this.statusTextPane.getText()))
            tstamp += newLine;
        tstamp += "[" + elapsedTimer.getElapsed() + "] ";

        // this prints the name of the jar file when it is the 1st job for the file
        // (it prints on a separate line from the threads related to it)
        if (jobname.equals(JobName.decompiling.toString())) {
            String message = fname + newLine;
            appendToPane(this.statusTextPane, tstamp, Util.TextColor.Brown, Util.FontType.Bold);
            appendToPane(this.statusTextPane, message, Util.TextColor.Black, Util.FontType.Italic);
        }

        // now start the thread job line with a timestamp
        appendToPane(this.statusTextPane, tstamp, Util.TextColor.Brown, Util.FontType.Bold);

        // now display info about the job
        String jobinfo = "   job " + jobid + ":";
        jobinfo += getPadding(9, jobinfo.length());
        appendToPane(this.statusTextPane, jobinfo, Util.TextColor.Blue, Util.FontType.Italic);

        // now display the process that the thread is running
        jobname += getPadding(14, jobname.length());
        appendToPane(this.statusTextPane, jobname, Util.TextColor.Black, Util.FontType.Normal);

        // force an update
        updateStatusDisplay();
    }

    /**
     * outputs the job PID.
     * (appends the status to the current line)
     * 
     * @param pid   - PID of the job
     */
    private void statusMessageJob(Long pid) {
        // if a job was not running, don't display the status
        if (jobActive && pid >= 0) {
            String jobinfo = " (pid " + pid + ")";
            jobinfo += getPadding(14, jobinfo.length());
            appendToPane(this.statusTextPane, jobinfo, Util.TextColor.Blue, Util.FontType.Italic);

            // force an update
            updateStatusDisplay();
        }
    }

    /**
     * outputs the job status when it has completed.
     * (appends the status to the current line and terminates with a newline)
     * 
     * @param exitcode   - its exit code
     */
    private void statusMessageJob(int exitcode) {
        // if a job was not running, don't display the status
        if (jobActive) {
            if (exitcode != 0) {
                appendToPane(this.statusTextPane, " (" + exitcode + ")" + newLine,
                            Util.TextColor.Red, Util.FontType.Bold);
            }
            else {
                appendToPane(this.statusTextPane, " OK" + newLine,
                            Util.TextColor.Green, Util.FontType.Bold);
            }

            // force an update
            updateStatusDisplay();
        }

        jobActive = false;
    }

    /**
     * outputs a message to the text area handling the stdout and stderr.
     * 
     * @param message - the message to display
     */
    private void outputMessageInfo(String message) {
        String oldContent = outputTextArea.getText();
        this.outputTextArea.setText(oldContent + message + newLine);
    }

    /**
     * selects all of the jar files in the list.
     */
    private void selectAllJars() {
        DefaultListModel listModel = (DefaultListModel)this.jarList.getModel();
        listModel.clear();
        // make sure we found some files
        DefaultListModel appList = configInfo.getList(ConfigInfo.ListType.application);
        if (!appList.isEmpty()) {
            for (int ix = 0; ix < appList.size(); ix++) {
                String entry = (String)appList.get(ix);
                if (entry.startsWith(baseJarPath))
                    entry = entry.substring(baseJarPath.length());
                listModel.addElement(entry);
            }
        }
        DefaultListModel libList = configInfo.getList(ConfigInfo.ListType.libraries);
        if (!libList.isEmpty()) {
            for (int ix = 0; ix < libList.size(); ix++) {
                String entry = (String)libList.get(ix);
                if (entry.startsWith(baseJarPath))
                    entry = entry.substring(baseJarPath.length());
                listModel.addElement(entry);
            }
        }
        int count = listModel.getSize();
        if (count > 0) {
            // enable Start button
            this.startButton.setEnabled(true);
            this.jarListLabel.setText("jars to decompile: " + count);
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
            statusMessage(StatusType.Info, "updating configuration file: " + tag + " = " + value);
            int retcode = AnalyzerFrame.updateConfigFile (tag, value);
            if (retcode != 0)
                statusMessage(StatusType.Error, "Invalid tag for configuration file: " + tag);
        } catch (IOException ex) {
            statusMessage(StatusType.Error, "Failure writing to configuration file");
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

        jarFileChooser = new javax.swing.JFileChooser();
        fileChooser = new javax.swing.JFileChooser();
        outputTabbedPane = new javax.swing.JTabbedPane();
        statusScrollPane = new javax.swing.JScrollPane();
        statusTextPane = new javax.swing.JTextPane();
        outputScrollPane = new javax.swing.JScrollPane();
        outputTextArea = new javax.swing.JTextArea();
        jarSelectPanel = new javax.swing.JPanel();
        selectAllButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        jarListLabel = new javax.swing.JLabel();
        jarlistScrollPane = new javax.swing.JScrollPane();
        jarList = new javax.swing.JList<>();
        jobsQueuedLabel = new javax.swing.JLabel();
        runPanel = new javax.swing.JPanel();
        startButton = new javax.swing.JButton();
        decompilerComboBox = new javax.swing.JComboBox<>();
        stopButton = new javax.swing.JButton();
        decompilerLabel = new javax.swing.JLabel();
        elapsedTimePanel = new javax.swing.JPanel();
        elapsedTimeLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Decompiler");
        setMinimumSize(new java.awt.Dimension(900, 540));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        statusScrollPane.setViewportView(statusTextPane);

        outputTabbedPane.addTab("Status", statusScrollPane);

        outputTextArea.setColumns(20);
        outputTextArea.setLineWrap(true);
        outputTextArea.setRows(5);
        outputTextArea.setWrapStyleWord(true);
        outputScrollPane.setViewportView(outputTextArea);

        outputTabbedPane.addTab("Output", outputScrollPane);

        selectAllButton.setText("Select All");
        selectAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllButtonActionPerformed(evt);
            }
        });

        clearButton.setText("Clear");
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        addButton.setText("Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        jarListLabel.setText("jars to decompile");

        jarlistScrollPane.setMinimumSize(new java.awt.Dimension(259, 128));
        jarlistScrollPane.setPreferredSize(new java.awt.Dimension(259, 128));

        jarlistScrollPane.setViewportView(jarList);

        jobsQueuedLabel.setMaximumSize(new java.awt.Dimension(131, 15));
        jobsQueuedLabel.setMinimumSize(new java.awt.Dimension(131, 15));
        jobsQueuedLabel.setPreferredSize(new java.awt.Dimension(131, 15));

        javax.swing.GroupLayout jarSelectPanelLayout = new javax.swing.GroupLayout(jarSelectPanel);
        jarSelectPanel.setLayout(jarSelectPanelLayout);
        jarSelectPanelLayout.setHorizontalGroup(
            jarSelectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jarSelectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jarSelectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(selectAllButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(clearButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(addButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jarSelectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jarlistScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE)
                    .addGroup(jarSelectPanelLayout.createSequentialGroup()
                        .addGroup(jarSelectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jarListLabel)
                            .addComponent(jobsQueuedLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jarSelectPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addButton, clearButton, selectAllButton});

        jarSelectPanelLayout.setVerticalGroup(
            jarSelectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jarSelectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jarListLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jarSelectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jarSelectPanelLayout.createSequentialGroup()
                        .addComponent(selectAllButton)
                        .addGap(18, 18, 18)
                        .addComponent(clearButton)
                        .addGap(18, 18, 18)
                        .addComponent(addButton))
                    .addComponent(jarlistScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jobsQueuedLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        startButton.setBackground(new java.awt.Color(204, 255, 204));
        startButton.setText("Start");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        decompilerComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "fernflower", "procyon" }));
        decompilerComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decompilerComboBoxActionPerformed(evt);
            }
        });

        stopButton.setBackground(new java.awt.Color(255, 204, 204));
        stopButton.setText("Stop");
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        decompilerLabel.setText("Decompiler");

        javax.swing.GroupLayout runPanelLayout = new javax.swing.GroupLayout(runPanel);
        runPanel.setLayout(runPanelLayout);
        runPanelLayout.setHorizontalGroup(
            runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(runPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(decompilerLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(decompilerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(20, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, runPanelLayout.createSequentialGroup()
                .addGap(62, 62, 62)
                .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(62, 62, 62))
        );
        runPanelLayout.setVerticalGroup(
            runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(runPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(decompilerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(decompilerLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 46, Short.MAX_VALUE)
                .addGroup(runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startButton)
                    .addComponent(stopButton))
                .addContainerGap())
        );

        elapsedTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        elapsedTimeLabel.setMaximumSize(new java.awt.Dimension(100, 15));
        elapsedTimeLabel.setMinimumSize(new java.awt.Dimension(100, 15));
        elapsedTimeLabel.setPreferredSize(new java.awt.Dimension(100, 15));

        javax.swing.GroupLayout elapsedTimePanelLayout = new javax.swing.GroupLayout(elapsedTimePanel);
        elapsedTimePanel.setLayout(elapsedTimePanelLayout);
        elapsedTimePanelLayout.setHorizontalGroup(
            elapsedTimePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(elapsedTimePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(elapsedTimeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        elapsedTimePanelLayout.setVerticalGroup(
            elapsedTimePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(elapsedTimePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(elapsedTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jarSelectPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(runPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(elapsedTimePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(outputTabbedPane))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jarSelectPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(runPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(elapsedTimePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(outputTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        // clear the display
        this.statusTextPane.setText("");
        
        // reset the timer
        elapsedTimer.reset();
        
        // get the list of selected jar files
        DefaultListModel listModel = (DefaultListModel)this.jarList.getModel();
        if (listModel.size() == 0) {
            statusMessage(StatusType.Error, "No jar files selected");
            return;
        }

        // get the selected decompiler to use
        String decompiler = "";
        Object selected = this.decompilerComboBox.getSelectedItem();
        if (selected != null)
            decompiler = selected.toString();

        // search for the jar file to use for decompiling code
        statusMessage(StatusType.Info, "Decompiling with: " + decompiler);

        // delete any existing src directory and make fresh one
        File srcpath = new File(srcpathname);
        if (srcpath.isDirectory()) {
            statusMessage (StatusType.Info, "Removing old dir: " + srcpathname);
            try {
                FileUtils.deleteDirectory(srcpath);
            } catch (IOException ex) {
                statusMessage (StatusType.Warning, ex + "deleting dir: " + srcpathname);
            }
        }
        srcpath.mkdirs();

        // delete any existing zip directory
        File zippath = new File(zippathname);
        if (zippath.isDirectory()) {
            statusMessage (StatusType.Info, "Removing old dir: " + zippathname);
            try {
                FileUtils.deleteDirectory(zippath);
            } catch (IOException ex) {
                statusMessage (StatusType.Warning, ex + "deleting dir: " + zippathname);
            }
        }

        // make sure nothing was running prior to this
        threadLauncher.init(new StandardTermination());
        
        // start the elapsed time counter
        elapsedTimer.start();
        
        // decompile each selected jar file
        for (int ix = 0; ix < listModel.size(); ix++) {
            // get the name of the jar file to decompile
            String jarname = listModel.elementAt(ix).toString();
            
            // get the name of the file after removing the path and extension.
            // the zip file will alwways be contained in "zippathname" and will
            // have an extension dictated by the decompiler used.
            String zipname = jarname.substring(0, jarname.lastIndexOf("."));
            if (zipname.contains("/"))
                zipname = zipname.substring(zipname.lastIndexOf("/")+1);
            
            // add the decompiler command to the thread
            String ziptype ;
            switch (decompiler) {
                default:
                case "fernflower":
                    zippath.mkdirs();
                    ziptype = "jar"; // this outputs the source in a jar file that needs to be unzipped
                    String[] command2 = { "java", "-jar", this.toolPathName + decompiler + ".jar",
                                         baseJarPath + jarname,
                                         zippathname };
                    threadLauncher.launch(command2, null, JobName.decompiling.toString(), jarname);
                    break;
                case "procyon":
                    // this outputs the source code directly (no zip file)
                    ziptype = "";
                    String[] command1 = { "java", "-jar", this.toolPathName + decompiler + ".jar",
                                         "-jar", baseJarPath + jarname,
                                         "-o", srcpathname };
                    threadLauncher.launch(command1, null, JobName.decompiling.toString(), jarname);
                    break;
            }

            // add the unzip command to the thread
            // (remove any lib/ entry that is prepended, because it won't have been
            // placed in the zip dir)
            if (!ziptype.isEmpty()) {
                String[] commandx = { "unzip", "-uo", zippathname + zipname + "." + ziptype, "-d", srcpathname };
                jobCount = threadLauncher.launch(commandx, null, JobName.unzipping.toString(), zipname + "." + ziptype);
            }

            // update the items in the queue
            jobsQueuedLabel.setText(jobCount + " jobs queued");

            // disable the start button and enable the stop button
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        }
    }//GEN-LAST:event_startButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        // if we were in the middle of processing a jar, terminate the record
        statusTerminate();

        ThreadLauncher.ThreadInfo threadInfo = threadLauncher.stopAll();

        // stop the running process
        if (threadInfo.pid >= 0) {
            statusMessage(StatusType.Warning, "Killing job " + threadInfo.jobid + ": pid " + threadInfo.pid);
            String[] command = { "kill", "-15", threadInfo.pid.toString() };
            commandLauncher.start(command, null);
        }

        stopButton.setEnabled(false);
    }//GEN-LAST:event_stopButtonActionPerformed

    private void selectAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllButtonActionPerformed
        selectAllJars();
    }//GEN-LAST:event_selectAllButtonActionPerformed

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        DefaultListModel listModel = (DefaultListModel)this.jarList.getModel();
        listModel.clear();
        this.jarListLabel.setText("jars to decompile: 0");
        this.startButton.setEnabled(false);
    }//GEN-LAST:event_clearButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Jar Files","jar");
        this.jarFileChooser.setFileFilter(filter);
        this.jarFileChooser.setMultiSelectionEnabled(true);
        int retVal = this.jarFileChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            DefaultListModel listModel = (DefaultListModel)this.jarList.getModel();
            File[] files = this.jarFileChooser.getSelectedFiles();
            for (File file : files) {
                String lib = file.getAbsolutePath();
                if (lib.startsWith(baseJarPath))
                    lib = lib.substring(baseJarPath.length());
                if (!listModel.contains(lib))
                    listModel.addElement(lib);
            }
            if (!listModel.isEmpty()) {
                this.startButton.setEnabled(true);
            }
            int count = this.jarList.getModel().getSize();
            this.jarListLabel.setText("jars to decompile: " + count);
        }
    }//GEN-LAST:event_addButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // upon completion, restore the stdout and stderr
        System.setOut(standardOut);
        System.setErr(standardErr);
        // re-enable the run button on the AnalyzerFrame
        AnalyzerFrame.exitFromDecompilerFrame();
    }//GEN-LAST:event_formWindowClosing

    private void decompilerComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decompilerComboBoxActionPerformed
        // update the config file
        JComboBox decomp = (JComboBox) evt.getSource();
        String value = (String) decomp.getSelectedItem();
        if (value != null) {
            updateConfigFileParam ("decompiler", value);
        }
    }//GEN-LAST:event_decompilerComboBoxActionPerformed

    // the types of jobs to run in ThreadLauncher
    private enum JobName {
        decompiling, unzipping;
    }

    private static final String newLine = System.getProperty("line.separator");

    private final ConfigInfo configInfo;
    private final String zippathname;
    private final String srcpathname;
    private final String toolPathName;
    private final String baseJarPath;
//    private final String decompiler;
    private int      jobCount;      // number of jobs currently pending
    private boolean  jobActive;     // true if a job is in process (status display not terminated)
    
    // these will contain the original stdout and stderr values to restore upon exit
    private final PrintStream     standardOut;
    private final PrintStream     standardErr; 
    private final ElapsedTimer    elapsedTimer;
    private final ThreadLauncher  threadLauncher;
    private final CommandLauncher commandLauncher;
        
    private final AudioClipPlayer audioplayer;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton clearButton;
    private javax.swing.JComboBox<String> decompilerComboBox;
    private javax.swing.JLabel decompilerLabel;
    private javax.swing.JLabel elapsedTimeLabel;
    private javax.swing.JPanel elapsedTimePanel;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JFileChooser jarFileChooser;
    private javax.swing.JList<String> jarList;
    private javax.swing.JLabel jarListLabel;
    private javax.swing.JPanel jarSelectPanel;
    private javax.swing.JScrollPane jarlistScrollPane;
    private javax.swing.JLabel jobsQueuedLabel;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JTabbedPane outputTabbedPane;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JPanel runPanel;
    private javax.swing.JButton selectAllButton;
    private javax.swing.JButton startButton;
    private javax.swing.JScrollPane statusScrollPane;
    private javax.swing.JTextPane statusTextPane;
    private javax.swing.JButton stopButton;
    // End of variables declaration//GEN-END:variables
}
