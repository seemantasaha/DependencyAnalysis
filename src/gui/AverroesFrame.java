/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import drivergen.Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

/**
 *
 * @author dmcd2356
 */
public class AverroesFrame extends javax.swing.JFrame {

    /**
     * Creates new form AverroesFrame
     * 
     * @param configInfo - the configuration data from AnalyzerFrame
     */
    public AverroesFrame(ConfigInfo configInfo) {
        initComponents();

        // save original stdout and stderr for restoring after we redirect to output window
        this.standardOut = System.out;
        this.standardErr = System.err;         
        mainClassList = new ArrayList<>();
        mainClassValue = "";
        mainClassPrev = "";
        
        // save passed params
        this.configInfo = configInfo;

        // create an elapsed timer
        elapsedTimer = new ElapsedTimer(elapsedTimeLabel);

        // create a debug message handler
        debug = new DebugMessage (this.statusTextPane);
        
        // setup the tool path name
        toolPathName = System.getProperty("user.dir") + "/tool/";
        
        // setup where the library files are and where to place output
        String projpath = configInfo.getField(ConfigInfo.StringType.projectpath);
        outPathName  = projpath + "/janalyzer/averroes/";
        File outdir = new File(outPathName);
        outdir.mkdirs();
        File tamioutdir = new File(projpath + "/challenge_program/");
        if (tamioutdir.isDirectory())
            challengePathName = projpath + "/challenge_program/";
        else
            challengePathName = outPathName;

        // get access to the audio player
        audioplayer = new AudioClipPlayer();
        
        // init the application list from the config file
        this.appList.setModel(configInfo.getList(ConfigInfo.ListType.application));

        // generate a list of package paths for the library selection
        libraryPackageList = generatePackageList (configInfo.getList(ConfigInfo.ListType.libraries));
        statusMessage(StatusType.Info, libraryPackageList.size() + " library package paths");
        
        // set params passed from AnalyzerFrame
        String startScript = configInfo.getField(ConfigInfo.StringType.startScript);
        startScriptTextField.setText(startScript);

        // set default main class selection if specified
        boolean bMainclass = false;
        String mainClass = configInfo.getField(ConfigInfo.StringType.mainClass);
        if (!mainClass.isEmpty()) {
            mainClassPrev = mainClass;
            mainClassComboBox.addItem(mainClass);
            mainClassComboBox.setSelectedIndex(0);
            bMainclass = true;
        }

        // set default regex expression if specified
        String appRegEx = configInfo.getField(ConfigInfo.StringType.appRegEx);
        if (appRegEx.isEmpty())
            appRegexTextField.setText("**");
        else
            appRegexTextField.setText(appRegEx);

        // initially disable all buttons
        averroesButton.setEnabled(false);
        tamiflexButton.setEnabled(false);
        stopButton.setEnabled(false);
        saveButton.setEnabled(false);

        // init to indicate Tamiflex has not been run yet
        bTamiflexCompleted = false;

        // get the path location of the JRE rt.jar file (from properties file)
        jrePathName = gui.AnalyzerFrame.getPropertiesItem (null, "JrePath");
            
        // enable Averroes button if JRE path and main class selection are valid
        // enable Tamiflex button if the above is true & start script is valid
        File jrepath = new File(jrePathName);
        if (jrepath.isDirectory() && bMainclass) {
            averroesButton.setEnabled(true);
            if (!startScript.isEmpty())
                tamiflexButton.setEnabled(true);
        }
        
        // this creates a command launcher on a separate thread
        threadLauncher = new ThreadLauncher(outputTextArea);

        // this creates a command launcher that runs from the current thread
        commandLauncher = new CommandLauncher();

        // indicate no files have yet been generated
        bFilesGenerated = false;
        
        // add a mouse listener for allowing use of cut/copy/paste
        statusTextPane.addMouseListener(new ContextMenuMouseListener());
        outputTextArea.addMouseListener(new ContextMenuMouseListener());
        appList.addMouseListener(new ContextMenuMouseListener());
        appRegexTextField.addMouseListener(new ContextMenuMouseListener());
        startScriptTextField.addMouseListener(new ContextMenuMouseListener());
    }

    /**
     * This determines if the averroes summary files are present in janalyzer.
     * 
     * @param pathName - the path to the location in janalyzer where the
     *                       tool jar files are kept for the specified project
     * 
     * @return true if all of the averroes files are present
     */
    public static boolean isAverroesFilesPresent(String pathName) {
        // verify the directory is valid
        if (!new File(pathName).isDirectory())
            return false;

        // make sure the files exist in the directory
        if (!new File(pathName + "/" + AverroesFrame.AVERROES_APP_JAR1).isFile())
            return false;
        if (!new File(pathName + "/" + AverroesFrame.AVERROES_APP_JAR2).isFile())
            return false;
        if (!new File(pathName + "/" + AverroesFrame.AVERROES_LIB_JAR).isFile())
            return false;

        return true;
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
        
            // play a sound to notify the user
            String audiofback = configInfo.getField(ConfigInfo.StringType.audiofback);
            if ("yes".equals(audiofback))
                audioplayer.playTada();
        }

        @Override
        public void jobprestart(ThreadLauncher.ThreadInfo threadInfo) {
            if (!threadInfo.jobname.equals(JobName.zipgrep.toString()))
                statusMessageJob(threadInfo.jobid, threadInfo.jobname, null);
        }

        @Override
        public void jobstarted(ThreadLauncher.ThreadInfo threadInfo) {
            if (!threadInfo.jobname.equals(JobName.zipgrep.toString()))
                statusMessageJob(threadInfo.pid);
        }
        
        @Override
        public void jobfinished(ThreadLauncher.ThreadInfo threadInfo) {
            //outputTextArea.append(stdout.getText());

            if (threadInfo.jobname.equals(JobName.tamiflex.toString())) {
                // indicate status of Tamiflex
                statusMessageJob(threadInfo.exitcode);
                bTamiflexCompleted = true;
                boolean bFailed = false;
                switch (threadInfo.exitcode) {
                    case 0:   statusMessage(StatusType.Info, threadInfo.jobname + " completed."); break;
                    case 137: statusMessage(StatusType.Info, "User SIGKILL complete.");      break;
                    case 143: statusMessage(StatusType.Info, "User SIGTERM complete.");      break;
                    default:  statusMessage(StatusType.Error, "Failure executing command.");
                        bFailed = true;
                        break;
                }

                // stop the timer
                elapsedTimer.stop();

                // re-enable the start button and disable the stop button
                setRunMode(false);

                // check if Tamiflex output file found
                String tamiflexFile = challengePathName + "out/" + TAMIFLEX_REFL;
                File source = new File(tamiflexFile);
                if (!source.isFile()) {
                    statusMessage(StatusType.Error, "Tamiflex output file not found: " + tamiflexFile);
                }
                else {
                    // copy tamiflexFile to outPathName
                    String target = source.getName();
                    File dest = new File(outPathName + target);
                    try {
                        FileUtils.copyFile(source, dest);
                    } catch (IOException ex) {
                        statusMessage(StatusType.Error, "Tamiflex file copy failure: " + ex.getMessage());
                    }
                    statusMessage(StatusType.Info, "Tamiflex file successfully copied to: " + outPathName + target);

                    // allow user to save tamiflex file to janalyzer dir
                    saveButton.setEnabled(true);
                    
                    // indicate files have been generated
                    bFilesGenerated = true;
                }
            }
            else if (threadInfo.jobname.equals(JobName.averroes.toString())) {
                // indicate status of Averroes
                statusMessageJob(threadInfo.exitcode);
                boolean bFailed = false;
                switch (threadInfo.exitcode) {
                    case 0:   statusMessage(StatusType.Info, threadInfo.jobname + " completed."); break;
                    case 137: statusMessage(StatusType.Info, "User SIGKILL complete.");      break;
                    case 143: statusMessage(StatusType.Info, "User SIGTERM complete.");      break;
                    default:  statusMessage(StatusType.Error, "Failure executing command.");
                        bFailed = true;
                        break;
                }
                // check if output files found
                if (!bFailed) {
                    boolean bValid = checkAverroesFiles(threadInfo.jobname);
                    
                    // indicate files have been generated
                    if (bValid)
                        bFilesGenerated = true;
                }

                // stop the timer
                elapsedTimer.stop();

                // re-enable the start button and disable the stop button
                setRunMode(false);
            }
            else if (threadInfo.jobname.equals(JobName.zipgrep.toString())) {
                statusMessageZipgrep(threadInfo.fname, threadInfo.pid, threadInfo.exitcode);
                
                if (zipgrepJarlist != null) {
                    // if we have more to jars to run, do the next one
                    if (zipgrepJarlist.length > zipgrepIndex) {
                        launchNextZipGrep();
                    }
                    else {
                        // finished all jar files -
                        // process the grep output to generate the comboBox selections
                        int count = processMainClassSelections();
                        if (!zipgrepSearchTerm.equals("main") /* && count <= 0 */ ) {
                            // unsuccessful - if we were doing the 1st pass, try with different search
                            statusMessage(StatusType.Info, "Begining search for: main");
                            launchNewZipGrep ("main");
                        }
                        else {
                            statusMessage(StatusType.Info, "Zipgrep completed.");

                            // stop the timer
                            elapsedTimer.stop();

                            // if successfully found Main class entries, display the selection
                            updateMainClass();
                            
                            // re-enable the start button and disable the stop button
                            setRunMode(false);
                        }
                    }
                }
            }
        }
    }

    /**
     * sets the buttons according to whether a command is running or not.
     * 
     * @param bRun - true if running, false if not
     */
    private void setRunMode (boolean bRun) {
        if (bRun) {
            // mode is running...
            // disable the run buttons & enable the stop button
            averroesButton.setEnabled(false);
            tamiflexButton.setEnabled(false);
            mainSearchButton.setEnabled(false);
            stopButton.setEnabled(true);
        }
        else {
            // mode is not running...
            // enable the run buttons & disable the stop button
            averroesButton.setEnabled(true);
            tamiflexButton.setEnabled(true);
            mainSearchButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    /**
     * creates the classpath from the library list.
     * 
     * @return the classpath
     */
    private String createClasspath () {
        String classpath = "";
        DefaultListModel libList = configInfo.getList(ConfigInfo.ListType.libraries);
        if (!libList.isEmpty()) {
            for (int ix = 0; ix < libList.size(); ix++) {
                classpath += ":" + libList.get(ix);
            }
            // eliminate initial ":" from classpath
            classpath = classpath.substring(1);
        }
        return classpath;
    }
    
    /**
     * looks for a library directory that is packed within a jar file and asks
     * the user whether to unpack it an use it as the library selection.
     */
    private int checkForEmbeddedLibrary () {
        int count = 0;
        // determine if there is a single jar application file and no libraries
        DefaultListModel libList = configInfo.getList(ConfigInfo.ListType.libraries);
        DefaultListModel appList = configInfo.getList(ConfigInfo.ListType.application);
        if (libList.isEmpty() && appList.getSize() == 1) {
            statusMessage(StatusType.Info, "Checking for libraries to extract from application file");

            // if so, check if the application file contains a lib directory
            // with jars in it, and ask the user if he wants to pull these out
            // to allow using them as the library.
            try {
                count = extractEmbeddedLibrary( new File ((String)appList.get(0)),
                                                configInfo.getField(ConfigInfo.StringType.projectpath) + "/janalyzer/common/",
                                                libList);
                if (count == 0)
                    statusMessage(StatusType.Info, "No embedded files found");
                else if (count > 0)
                    statusMessage(StatusType.Info, "Unpack completed: " + libList.size() + " files");
                else {
                    statusMessage(StatusType.Info, "Using existing lib dir: " + libList.size() + " files");
                }
            } catch (IOException ex) {
                statusMessage(StatusType.Error, "IOException: " + ex);
            }
        }
        return count;
    }
    
    /**
     * looks for a library directory that is packed within a jar file and asks
     * the user whether to unpack it an use it as the library selection.
     * This will unpack a "lib" folder containing jar files as well as a "classes"
     * folder containing class files.
     * 
     * @throws IOException
     * 
     * @param jarFile     - the jar file to examine
     * @param dstpathname - the name of the path in which to place the extracted lib directory
     * @param liblist     - the list of library files to populate
     * 
     * @return number of jars unpacked (-1 = copy files from averroes lib dir)
     */
    public static int extractEmbeddedLibrary (File jarFile, String dstpathname, DefaultListModel liblist) throws IOException {
        if (jarFile == null)
            return 0;

        // if there is a single jar file and it contains a library of jars in it,
        // ask user if he wants to pull these out to allow using this as the classpath.
        File libOutpath = new File(dstpathname);
        if (!libOutpath.isDirectory())
            libOutpath.mkdirs();
        String dstpathclsname = dstpathname + "classes/";
        String dstpathlibname = dstpathname + "lib/";
        libOutpath = new File(dstpathlibname);

        int classcount = 0;
        int count = 0;
        String classesPathTag = "/classes/";
        String classdirname = ""; // the name of the classes path found in the jar file
        String jarlibpathname = ""; // the name of the lib path found in the jar file
        ZipInputStream zip = new ZipInputStream(jarFile.toURI().toURL().openStream());
        while (true) {
            ZipEntry entry = zip.getNextEntry();
            if (entry == null)
                break;
            String fullname = entry.getName();
            int offset = fullname.lastIndexOf('/');
            // if we haven't found a lib path in the jar file, check on this entry
            if (jarlibpathname.isEmpty() && offset > 0) {
                String path = fullname.substring(0, offset);
                String fname = fullname.substring(offset+1);
                offset = path.lastIndexOf('/');
                if (offset < 0)
                    offset = 0;
                else
                    ++offset;
                if (path.substring(offset).equals("lib") && fname.contains(".jar")) {
                    jarlibpathname = path;
                    ++count;
                }
                else if (path.contains(classesPathTag) && fname.contains(".class")) {
                    classdirname = path.substring(0,path.lastIndexOf(classesPathTag)) + classesPathTag;
                    ++classcount;
                }
            }
            else if (fullname.startsWith(jarlibpathname) && fullname.contains(".jar")) {
                ++count;
            }
            else if (fullname.startsWith(classdirname) && fullname.contains(".class")) {
                ++classcount;
            }
        }

        // exit if no packaged library found
        if (jarlibpathname.isEmpty())
            return 0;
            
        // ask user if he wants to unpack the library
        String message;
        String[] selection;
        if (libOutpath.isDirectory()) {
            File[] files = libOutpath.listFiles();
            message = "The application jar file contains a library of " + count + " jar files and " + classcount + " classes in it." + newLine +
                      "A lib folder already exists in the averroes directory that contains " + files.length + " files." + newLine + newLine +
                      "Select one of the following:" + newLine +
                      " - Extract this embedded library and overwrite the existing directory," + newLine +
                      " - Use the existing lib folder," + newLine +
                      " - Ignore the existing folder and don't extract the embedded folder";
            selection = new String[] { "Ignore", "Use", "Extract" };
        }
        else {
            message = "The application jar file contains a library of " + count + " jar files and " + classcount + " classes in it." + newLine + newLine +
                      "Do you want to Extract this library prior to running Averroes ?";
            selection = new String[] { "No", "Extract" };
        }
        String title   = "Library contained in jar file";
        int which = JOptionPane.showOptionDialog(null, // parent component
                    message,        // message
                    title,          // title
                    JOptionPane.DEFAULT_OPTION, // option type
                    JOptionPane.PLAIN_MESSAGE,  // message type
                    null,           // icon
                    selection,      // selection options
                    selection[0]);  // initial value
        
        if (which < 0 || selection[which].equals("No")) {
            // do nothing
            return 0;
        }
        if (selection[which].equals("Extract")) {
            // extract the jar files into the lib dir
            count = unpackJarsLib (jarFile, dstpathlibname, jarlibpathname);

            // extract the class files (if any) into the classes dir
            if (classcount > 0) {
                int filecount = unpackClassesLib (jarFile, dstpathclsname, classdirname);
//                if (filecount > 0) {
//                    createJarFile (dstpathlibname + "classes.jar", dstpathclsname);
//                    ++count;
//                }
            }
        }
        else { // "Use"
            // copy the lib files to the library list
            count = -1;
        }
                    
        // set the liblist to the sorted list of entries in the specified directory
        File[] jarlist = libOutpath.listFiles(new AnalyzerFrame.JarFileFilter());
        if (jarlist != null) {
            Arrays.sort(jarlist);
            liblist.clear();
            for (File jarfile : jarlist)
                liblist.addElement(jarfile.getAbsolutePath());
        }

        return count;
    }
    
    /**
     * This creates a jar file from a list of files
     * TODO: this currently does not work!
     * 
     * @param jarfilename - the name of the jar file to create
     * @param clspathname - the name of the path containing the files to place in the jar file
     * 
     * @return number of jars unpacked
     */
    private static void createJarFile (String jarfilename, String clspathname) throws IOException {
        File classdir = new File(clspathname);
        if (!classdir.isDirectory())
            return;
        
        // create the zip file to save the classes in
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(jarfilename));
        
        // TODO: I don't think this will recurse into the subdirs
        for (File file : classdir.listFiles()) {
            if (file.isDirectory())
                continue;
            
            String fname = file.getAbsolutePath();
            
            // now place in the zip file
            ZipEntry ze = new ZipEntry(fname);
            zout.putNextEntry(ze);

            // copy the file contents into StringBuilder
            BufferedReader reader = new BufferedReader(new FileReader(fname));
            StringBuilder sb = new StringBuilder();
            try {
                String line;
                while((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append(newLine);
                }
            } finally {
                reader.close();
            }
                
            // now copy to the zip file
            byte[] data = sb.toString().getBytes();
            zout.write(data, 0, data.length);
            zout.closeEntry();
        }
        
        zout.close();
    }
    
    /**
     * This unpacks the library of classes that are embedded in the spacified jar file.
     * 
     * @param jarfile     - the main jar file for the project
     * @param dstpathname - the name of the path in which to place the extracted files
     * @param clspathname - the name of the classes path found within the jar file
     * 
     * @return number of jars unpacked
     */
    private static int unpackClassesLib (File jarfile, String dstpathname, String clspathname) throws IOException {
        // create the subdir to place the files in
        File libOutpath = new File(dstpathname);
        if (!libOutpath.isDirectory()) {
            libOutpath.mkdirs();
        }

        // unpack the library
        int count = 0;
        JarFile jar = new JarFile(jarfile);
        Enumeration enumEntries = jar.entries();

        // for each entry in the main jar file...
        while (enumEntries.hasMoreElements()) {
            JarEntry file = (JarEntry) enumEntries.nextElement();

            // look for names begining with the classes path and ending in .class
            String fullname = file.getName();
            int offset = fullname.lastIndexOf('/');
            int startoff = fullname.lastIndexOf("/classes/");
            if (startoff > 0)
                startoff += "/classes/".length();
            if (offset > 0 && startoff > 0 && fullname.startsWith(clspathname) && fullname.contains(".class")) {
                // get the name of the class file with a path relative to the "classes" dir
                String relpathname = fullname.substring(startoff);
                String classname = fullname.substring(offset + 1);

                // make sure to create the entire dir parths needed
                String relpathonly = fullname.substring(startoff, offset);
                File relpath = new File(dstpathname + relpathonly);
                if (!relpath.isDirectory()) {
                    relpath.mkdirs();
                }

                // extract the file to its new location
                File fout = new File(dstpathname + relpathname);
                InputStream istream = jar.getInputStream(file);
                FileOutputStream fos = new FileOutputStream(fout);
                while (istream.available() > 0) {
                    // write contents of 'istream' to 'fos'
                    fos.write(istream.read());
                    ++count;
                }
            }
        }

        return count;
    }
    
    /**
     * This unpacks the library of jars that are embedded in the spacified jar file.
     * 
     * @param jarfile     - the main jar file for the project
     * @param dstpathname - the name of the path in which to place the extracted files
     * @param libpathname - the name of the lib path found within the jar file
     * 
     * @return number of jars unpacked
     */
    private static int unpackJarsLib (File jarfile, String dstpathname, String libpathname) throws IOException {
        // create the subdir to place the files in
        File libOutpath = new File(dstpathname);
        if (!libOutpath.isDirectory()) {
            libOutpath.mkdirs();
        }

        // unpack the library
        int count = 0;
        JarFile jar = new JarFile(jarfile);
        Enumeration enumEntries = jar.entries();

        // for each entry in the main jar file...
        while (enumEntries.hasMoreElements()) {
            JarEntry file = (JarEntry) enumEntries.nextElement();

            // look for names begining with the lib path and ending in .jar
            String fullname = file.getName();
            int offset = fullname.lastIndexOf('/');
            if (offset > 0 && fullname.startsWith(libpathname) && fullname.contains(".jar")) {
                String fname = fullname.substring(offset+1);
//                statusMessage(StatusType.Info, "Unpacking: " + fname);
                // unpack the file
                File fout = new File(dstpathname + fname);
                InputStream istream = jar.getInputStream(file);
                FileOutputStream fos = new FileOutputStream(fout);
                while (istream.available() > 0) {
                    // write contents of 'istream' to 'fos'
                    fos.write(istream.read());
                    ++count;
                }
            }
        }

        return count;
    }
    
    private boolean checkFile (String fname, String test) {
        File source = new File(outPathName + fname);
        if (source.isFile())
            return true;
        
        if (test != null)
            statusMessage(StatusType.Error, test + " output file not found: " + fname);
        return false;
    }

    private boolean checkAverroesFiles (String test) {
        boolean bValid = false;
        boolean bAverroes = test.equals(JobName.averroes.toString());
        boolean bTamiflex = test.equals(JobName.tamiflex.toString());

        // check if averroes output files found
        if (checkFile(AVERROES_APP_JAR1, bAverroes ? test : null)) bValid = true;
        if (checkFile(AVERROES_APP_JAR2, bAverroes ? test : null)) bValid = true;
        if (checkFile(AVERROES_LIB_JAR,  bAverroes ? test : null)) bValid = true;
        if (checkFile(TAMIFLEX_REFL,     bTamiflex ? test : null)) bValid = true;

        // enable the save button if some of the files are present
        if (bValid) {
            saveButton.setEnabled(true);
        }
        
        return bValid;
    }
    
    /**
     * generates a list of package names for all classes found in the specified
     * list of jar files. Note that the entries consist of the package names
     * only, omitting the class names themselves, and skipping duplicate
     * names when multiple classes are found in the specified path.
     * 
     * @param listModel - the list to use: configInfo.libList or configInfo.appList
     * @return the list of unique package names that corresponds to all of the
     * classes found in the jar list.
     */
    private ArrayList<String> generatePackageList (DefaultListModel listModel) {
        ArrayList<String> packageList = new ArrayList<>();
        if (listModel == null || listModel.isEmpty())
            return packageList;
        
        // get the list of jar files to rummage through
        ArrayList<File> files = new ArrayList<>();
        for (int ix = 0; ix < listModel.size(); ix++)
            files.add(new File(listModel.get(ix).toString()));
        if (files.isEmpty())
            return packageList;

        // now rummage through them to find all of the classes they contain
        File[] jarlist = files.toArray(new File[files.size()]);
        for (File jarfile : jarlist) {
            String jarname = jarfile.getAbsolutePath();
            try {
                ZipInputStream zip = new ZipInputStream(new FileInputStream(jarname));
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        // This entry represents a class. Now, extract the package path for that class
                        // and remove the ".class" from the name
                        String className = entry.getName().replace('/', '.');
                        className = className.substring(0, className.lastIndexOf('.'));
                        // className should now end in the name of the class. let's remove it too
                        // so all we have left is the full package path of the class
                        int offset = className.lastIndexOf('.');
                        if (offset > 0)
                            className = className.substring(0, offset);
                        // if we are filtering, check if it matches the filter entries
                        if (!packageList.contains(className))
                            packageList.add(className);
                    }
                }
            } catch (FileNotFoundException ex) {
                statusMessage(StatusType.Error, ex.getMessage());
                return packageList;
            } catch (IOException ex) {
                statusMessage(StatusType.Error, ex.getMessage());
            }
        }
        
        // sort the list and return it
        Collections.sort(packageList);
        return packageList;
    }

    /**
     * this returns a sublist of the specified pkgList that only contains entries
     * that start with the specified filter values.
     * 
     * @param pkgList - the list of package paths to filter
     * @param filterList - the list of starting values to filter
     * @return the entries in pkgList that start with filterList
     */
    private ArrayList<String> filterPackageList (ArrayList<String> pkgList, ArrayList<String> filterList) {
        ArrayList<String> resultList = new ArrayList<>();
        for (String entry : pkgList) {
            entry += "."; // append a decpt to make sure the last path name matches completely
            for (String filter : filterList) {
                if (entry.startsWith(filter + ".") && !resultList.contains(entry)) {
                    resultList.add(entry);
                }
            }
        }
        return resultList;
    }
    
    /**
     * This returns a list of the top-level packages
     * 
     * @param pkgList - list of full package names
     * @return the list of just the top level names for the packages
     */
    private ArrayList<String> getTopLevelPackageList (ArrayList<String> pkgList) {
        ArrayList<String> toplevels = new ArrayList<>();
        for(String topname : pkgList) {
            int offset = topname.indexOf(".");
            if (offset >= 0)
                topname = topname.substring(0, offset);
            if (!toplevels.contains(topname))
                toplevels.add(topname);
        }

        return toplevels;
    }
    
    /**
     * returns the shortest package path that is not contained in any library jar files.
     * 
     * @param libPkgList  - the list of library package paths containing a class
     * @param packagePath - the full package path to search for
     * @return the highest level of packagePath that is not contained in the library
     */
    private String findPackagePathInLib (ArrayList<String> libPkgList, String packagePath) {
        String[] pkglevel = packagePath.split("\\.");
        String matchname = packagePath;
        int levmax = 0; // init max path to shortest path
        for (String libName : libPkgList) {
            libName += ".";
            // search each lib entry for the lowest package path level (highest 'level' value)
            for (int level = 0; level < pkglevel.length; level++) {
                matchname = "";
                for (int ix = 0; ix <= level; ix++)
                    matchname += pkglevel[ix] + ".";
                // if the package at this level is not contained in library, we are
                // at the max level that does not conflict for this lib entry.
                // if it is the highest level so far, save it.
                if (!libName.startsWith(matchname)) {
                    levmax = (level > levmax) ? level : levmax;
                    break;
                }
            }
        }

        // return the shortest path that doesn't conflict with the library files
        matchname = "";
        for (int ix = 0; ix < levmax; ix++)
            matchname += pkglevel[ix] + ".";
        if (levmax < pkglevel.length)
            matchname += pkglevel[levmax];
        return matchname;
    }
    
    /**
     * this sets the RegEx field selecion based on the Main Class selection.
     */
    private void setRegExField () {
        // find all the top level classes in jars
        String value = "";
        statusMessage(StatusType.Info, "Generating list of application packages");
        DefaultListModel theAppList = configInfo.getList(ConfigInfo.ListType.application);
        ArrayList<String> appPkgList = generatePackageList (theAppList);
        ArrayList<String> appTopList = getTopLevelPackageList (appPkgList);
        statusMessage(StatusType.Info, appPkgList.size() + " application package paths");
        statusMessage(StatusType.Info, "Filtering library packages");
        ArrayList<String> newLibPkgList = filterPackageList (libraryPackageList, appTopList);
        statusMessage(StatusType.Info, newLibPkgList.size() + " filtered library package paths");
            
        if (!appPkgList.isEmpty()) {
            // now go through the list to set the entries to the highest level
            // of package path that is not contained in the library list
            List<String> appPkgFiltered = new ArrayList<>();
            String lastEntry = null;
            for (String className : appPkgList) {
                // find shortest packagepath that is not contained in library
                // (skip entry if last path is contained in next path)
                if (lastEntry == null || !className.startsWith(lastEntry)) {
                    String entry = findPackagePathInLib (newLibPkgList, className);
                    lastEntry = entry;

                    // (only add entry if it is not already there)
                    if (!appPkgFiltered.contains(entry)) {
                        appPkgFiltered.add(entry);
                        statusMessage(StatusType.Info, "regEx path added: " + entry);
                    }
                }
            }
            
            // now create the regex field from the final list of package paths
            String separator = "";
            for (String className : appPkgFiltered) {
                value += separator + className + ".**";
                separator = ":";
            }
        }

        // set the field to the selected value
        this.appRegexTextField.setText(value);

        // update the config file if the value has changed
        if (!value.isEmpty() && !configInfo.getField(ConfigInfo.StringType.appRegEx).equals(value))
            updateConfigFileParam ("appRegEx", value);
    }

    private void launchMainClassZipGrep () {
        // convert the list of application jar files into an array
        DefaultListModel listModel = configInfo.getList(ConfigInfo.ListType.application);
        ArrayList<File> files = new ArrayList<>();
        for (int ix = 0; ix < listModel.size(); ix++)
            files.add(new File(listModel.get(ix).toString()));
        if (files.isEmpty())
            return;
        zipgrepJarlist = files.toArray(new File[files.size()]);
        
        // start the elapsed time counter
        elapsedTimer.start();

        // init the output text area where the grep results will be captured
        outputTextArea.setText("");
        threadLauncher.init(new StandardTermination());

        // initially, we will look for the following entries from the manifests
        // (this should catch Start-Class: as well as Main-Class:
        statusMessage(StatusType.Info, "Begining search for: Main-Class & Start-Class");
        launchNewZipGrep ("[tM]a[ri][[tn]-Class:");
    }
    
    private void launchNewZipGrep (String searchTerm) {
        zipgrepSearchTerm = searchTerm;
        zipgrepIndex = 0;
        launchNextZipGrep();
    }
    
    private void launchNextZipGrep () {
        if (zipgrepJarlist != null && zipgrepIndex < zipgrepJarlist.length) {
            // get the next jar file to grep
            File jarfile = zipgrepJarlist[zipgrepIndex++];
            String jarFileName = jarfile.getAbsolutePath();

            // run the command
//            statusMessage(StatusType.Info, "zipgrep " + zipgrepSearchTerm + " " + jarFileName);

            String[] command = { "zipgrep", zipgrepSearchTerm, jarFileName };
            threadLauncher.launch(command, null, JobName.zipgrep.toString(), jarFileName);
        }
    }
    
    /**
     * this sets the selection list for the Main Class and chooses the most likely one.
     * It also updates the RegEx selection based on this selected Main Class.
     * The Main class selections are derrived by doing a grep on all of the jar files
     * for the keyword 'main' and then only choosing those entries that are specified
     * by 'xxx.class:' which signifies that it is a class name. The preferred item
     * selected from this list is the 1st one that has a class name of 'Main'.
     * If not found, it will select the 1st entry in the list.
     */
    private int processMainClassSelections () {
        // init selections to none
        mainClassValue = "";
        mainClassList.clear();

        // copy the grep results into content & reset the output pane.
        String content = outputTextArea.getText();
//        outputTextArea.setText("");

        String search;
        Pattern pattern;
        Matcher match;
        
        // first check for manifest entries that define the Main-Class
        search = "(^META-INF/MANIFEST.MF:Main-Class:)[ \t]*(.*)";
        pattern = Pattern.compile(search, Pattern.MULTILINE);
        match = pattern.matcher(content);
        while (match.find()) {
            String entry = match.group(2);
            // add the entry to the combobox selections if not already there
            if (!entry.isEmpty() && !mainClassList.contains(entry)) {
                mainClassList.add(entry);
                mainClassValue = entry;
            }
        }

        // next check for manifest entries that define the Start-Class
        // (Spring framework uses this to denote the main class to run that is
        // not a part of Spring)
        search = "(^META-INF/MANIFEST.MF:Start-Class:)[ \t]*(.*)";
        pattern = Pattern.compile(search, Pattern.MULTILINE);
        match = pattern.matcher(content);
        while (match.find()) {
            String entry = match.group(2);
            // add the entry to the combobox selections if not already there
            if (!entry.isEmpty() && !mainClassList.contains(entry)) {
                mainClassList.add(entry);
                mainClassValue = entry;
            }
        }
        
        // find all occurrances that have .class: to identify actual classes
        // that contained "main" rather than some other type of reference.
        search = "(^.*).class:.*";
        pattern = Pattern.compile(search, Pattern.MULTILINE);
        match = pattern.matcher(content);
        while (match.find()) {
            String entry = match.group(1);
            if (!entry.isEmpty()) {
                // get the class name from the entry and convert the full
                // class name to dotted format
                String cls = "";
                String pkg = "";
                if (entry.contains("/")) {
                    entry = entry.replace("/", ".");
                    cls = entry.substring(entry.lastIndexOf('.') + 1);
                    pkg = entry.substring(0, entry.lastIndexOf('.'));
                }
                    
                // add the entry to the combobox selections if not already there
                // (ignore entry if it contains the "$" char)
                if (!mainClassList.contains(entry) && !cls.contains("$")) {
                    // save the 1st entry that has Main as the class name
                    if (cls.endsWith("Main")) {
                        // if class ends in "Main", add entry to begining of list
                        // because these are the most likely ones.
                        mainClassList.add(0, entry);
                        mainClassValue = entry;
                    }
                    else {
                        // else, add to end of list
                        mainClassList.add(entry);
                    }
                }
            }
        }

        // indicate number of selections in Class list
        int count = mainClassList.size();
        statusMessage(count == 0 ? StatusType.Error : StatusType.Info,
                      count + " selections found for search term: " + zipgrepSearchTerm);

        return count;
    }
    
    /**
     * This updates the Main Class combo box selections and sets the RegEx value.
     */
    private void updateMainClass () {
        if (!mainClassList.isEmpty()) {
            // now sort the list of classes to make them easier to follow
            List<String> sublist = mainClassList.subList(0, mainClassList.size());

            // now copy the entries to the combobox
            String[] array = new String[sublist.size()];
            array = sublist.toArray(array);
            DefaultComboBoxModel cbModel = new DefaultComboBoxModel(array);
            mainClassComboBox.setModel(cbModel);
            
            // if Main class selection was specified in config file and it is
            // a valid selection, use it.
            // else, use the most likely main class if one was found, or 1st entry if not.
            if (!mainClassPrev.isEmpty() && cbModel.getIndexOf(mainClassPrev) >= 0)
                mainClassComboBox.setSelectedItem(mainClassPrev);
            else if (!mainClassValue.isEmpty())
                mainClassComboBox.setSelectedItem(mainClassValue);
            else
                mainClassComboBox.setSelectedIndex(0);
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

    /**
     * A generic function for appending formatted text to a JTextPane.
     * 
     * @param tp    - the TextPane to append to
     * @param msg   - message contents to write
     * @param color - color of text
     * @param ftype - type of font style
     */
    private void appendToPane(JTextPane tp, String msg, Util.TextColor color, Util.FontType ftype)
    {
        appendToPane(tp, msg, color, "Courier", 11, ftype);
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

    private enum StatusType {
        Info, Warning, Error;
    }

    /**
     * updates the graphics immediately for the status display
     */
    private void updateStatusDisplay () {
        if ("Status".equals(outputTabbedPane.getTitleAt(
                            outputTabbedPane.getSelectedIndex())))
            this.statusTextPane.update(this.statusTextPane.getGraphics());
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
        String tstamp = "";
        if (!isTerminated(this.statusTextPane.getText()))
            tstamp += newLine;
        tstamp += "[" + elapsedTimer.getElapsed() + "] ";
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
     * @param fname - the name of the file being processed
     */
    private void statusMessageJob(int jobid, String jobname, String fname) {

        // start the thread job line with a timestamp
        String tstamp = "[" + elapsedTimer.getElapsed() + "] ";
        appendToPane(this.statusTextPane, tstamp, Util.TextColor.Brown, Util.FontType.Bold);

        // now display info about the job
        String jobinfo = "   job " + jobid + ":";
        jobinfo += getPadding(9, jobinfo.length());
        appendToPane(this.statusTextPane, jobinfo, Util.TextColor.Blue, Util.FontType.Italic);

        // now display the process that the thread is running
        int fieldlen = 14;
        if (fname != null && !fname.isEmpty()) {
            jobname += " : " + fname;
            fieldlen = 25;
        }
        jobname += getPadding(fieldlen, jobname.length());
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
        if (pid >= 0) {
            String jobinfo = " (pid " + pid + ")";
            jobinfo += getPadding(14, jobinfo.length());
            appendToPane(this.statusTextPane, jobinfo, Util.TextColor.Blue, Util.FontType.Italic);
        }

        // force an update
        updateStatusDisplay();
    }

    /**
     * outputs the job status when it has completed.
     * (appends the status to the current line and terminates with a newline)
     * 
     * @param exitcode   - its exit code
     */
    private void statusMessageJob(int exitcode) {
        // start the thread job line with a timestamp
//        String tstamp = "[" + elapsedTimer.getElapsed() + "] ";
//        appendToPane(this.statusTextPane, tstamp, Util.TextColor.Brown, Util.FontType.Bold);

        if (exitcode != 0) {
            appendToPane(this.statusTextPane, "exitcode: " + exitcode + newLine,
                        Util.TextColor.Red, Util.FontType.Bold);
        }
        else {
            appendToPane(this.statusTextPane, "exitcode: " + exitcode + newLine,
                        Util.TextColor.Green, Util.FontType.Bold);
        }

        // force an update
        updateStatusDisplay();
    }

    /**
     * outputs the job status when it has completed.
     * (appends the status to the current line and terminates with a newline)
     * 
     * @param fname    - the filename being processed
     * @param exitcode - its exit code
     */
    private void statusMessageZipgrep(String fname, Long pid, int exitcode) {
        // start the thread job line with a timestamp
        String tstamp = "[" + elapsedTimer.getElapsed() + "] ";
        appendToPane(this.statusTextPane, tstamp, Util.TextColor.Brown, Util.FontType.Bold);

        // now display the file being processed
        String baseName = fname;
        if (baseName.contains("/"))
            baseName = baseName.substring(baseName.lastIndexOf("/") + 1);
        String jobinfo = "   " + baseName + ": ";
        jobinfo += getPadding(35, jobinfo.length());
        appendToPane(this.statusTextPane, jobinfo, Util.TextColor.Blue, Util.FontType.Italic);

        // now append the PID of the process
        if (pid >= 0) {
            jobinfo = " (pid " + pid + ")";
            jobinfo += getPadding(14, jobinfo.length());
            appendToPane(this.statusTextPane, jobinfo, Util.TextColor.Black, Util.FontType.Italic);
        }
        
        // now display the command status
        if (exitcode != 0) {
            appendToPane(this.statusTextPane, "not found" + newLine,
                        Util.TextColor.Red, Util.FontType.Bold);
        }
        else {
            appendToPane(this.statusTextPane, "found" + newLine,
                        Util.TextColor.Green, Util.FontType.Bold);
        }

        // force an update
        updateStatusDisplay();
    }
    
    /**
     * copies the specified file from the srcPath to the destPath.
     * 
     * @param srcPath - directory of file to copy
     * @param dstPath - directory to copy file into
     * @param fname  - name of file
     */
    private void fileCopy (String srcPath, String dstPath, String fname) {
        File srcFile = new File (srcPath + "/" + fname);
        File dstFile = new File (dstPath + "/" + fname);

        // make sure source file exists
        if (!srcFile.isFile()) {
            statusMessage(StatusType.Error, "File does not exist: " + srcPath + "/" + fname);
            return;
        }

        // delete any dest file by that name
        if (dstFile.exists())
            dstFile.delete();
        
        // now copy the file
        Path src = srcFile.toPath();
        Path dst = dstFile.toPath();
        try {
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            statusMessage(StatusType.Error, ">>> " + ex);
            return;
        }

        statusMessage(StatusType.Info, "Copied file to " + dstPath + " : " + fname);
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
     * copies the specified file from the project path averroes dir to the
     * janalyzer averroes dir.
     * 
     * @param fname - file name to copy (no path)
     * @param destPathName - path to copy to
     * 
     * @return true if file copied, false if file not found
     */
    private boolean copyAverroesFile (String fname, String destPathName) {
        if (!new File(outPathName + "/" + fname).isFile()) {
            statusMessage(StatusType.Warning, "File missing: " + fname);
            return false;
        }
        fileCopy (outPathName, destPathName, fname);
        return true;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jrePathChooser = new javax.swing.JFileChooser();
        startScriptChooser = new javax.swing.JFileChooser();
        fileChooser = new javax.swing.JFileChooser();
        activePanel = new javax.swing.JPanel();
        setupPanel = new javax.swing.JPanel();
        entryPanel = new javax.swing.JPanel();
        appScrollPane = new javax.swing.JScrollPane();
        appList = new javax.swing.JList<>();
        mainClassComboBox = new javax.swing.JComboBox<>();
        appRegexTextField = new javax.swing.JTextField();
        startScriptTextField = new javax.swing.JTextField();
        buttonPanel = new javax.swing.JPanel();
        appAddButton = new javax.swing.JButton();
        mainSearchButton = new javax.swing.JButton();
        startScriptButton = new javax.swing.JButton();
        appClearButton = new javax.swing.JButton();
        autoRegexButton = new javax.swing.JButton();
        labelPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        runPanel = new javax.swing.JPanel();
        elapsedTimePanel = new javax.swing.JPanel();
        elapsedTimeLabel = new javax.swing.JLabel();
        commandPanel = new javax.swing.JPanel();
        averroesButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        tamiflexButton = new javax.swing.JButton();
        outputTabbedPane = new javax.swing.JTabbedPane();
        statusScrollPane = new javax.swing.JScrollPane();
        statusTextPane = new javax.swing.JTextPane();
        outputScrollPane = new javax.swing.JScrollPane();
        outputTextArea = new javax.swing.JTextArea();

        jrePathChooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_AND_DIRECTORIES);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Averroes");
        setMinimumSize(new java.awt.Dimension(900, 540));
        setPreferredSize(new java.awt.Dimension(1000, 600));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        setupPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        setupPanel.setPreferredSize(new java.awt.Dimension(705, 249));

        entryPanel.setPreferredSize(new java.awt.Dimension(497, 245));

        appScrollPane.setPreferredSize(new java.awt.Dimension(497, 90));

        appScrollPane.setViewportView(appList);

        mainClassComboBox.setMinimumSize(new java.awt.Dimension(472, 24));
        mainClassComboBox.setPreferredSize(new java.awt.Dimension(472, 24));
        mainClassComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mainClassComboBoxActionPerformed(evt);
            }
        });

        appRegexTextField.setToolTipText("<html>\nThis is a list of regular expressions that define what application classes to<br>\ninclude in the output. Each entry is seperated by a colon \":\".<br>\nAnything not included will not be able to be analyzed by Janalyzer.<br>\nUse the following format:<br><br>\n<full_class_name>  - to include a single class<br>\n<package_name>.*   - to include all classes in a package (no recursion)<br>\n<package_name>.**  - to include all classes and subpackages (recurse)<br>\n**  - to include the default package\n</html>");
        appRegexTextField.setMinimumSize(new java.awt.Dimension(472, 24));
        appRegexTextField.setPreferredSize(new java.awt.Dimension(472, 28));
        appRegexTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                appRegexTextFieldFocusLost(evt);
            }
        });
        appRegexTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appRegexTextFieldActionPerformed(evt);
            }
        });

        startScriptTextField.setMinimumSize(new java.awt.Dimension(472, 24));
        startScriptTextField.setPreferredSize(new java.awt.Dimension(472, 28));
        startScriptTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startScriptTextFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout entryPanelLayout = new javax.swing.GroupLayout(entryPanel);
        entryPanel.setLayout(entryPanelLayout);
        entryPanelLayout.setHorizontalGroup(
            entryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entryPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(entryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mainClassComboBox, 0, 473, Short.MAX_VALUE)
                    .addComponent(appRegexTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(startScriptTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(appScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        entryPanelLayout.setVerticalGroup(
            entryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entryPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(appScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addGap(10, 10, 10)
                .addComponent(mainClassComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(appRegexTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(startScriptTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(65, 65, 65))
        );

        buttonPanel.setPreferredSize(new java.awt.Dimension(104, 245));

        appAddButton.setText("Add");
        appAddButton.setToolTipText("<html>\nThis sets the location of the Java Runtime library where the rt.jar file is located.<br>\nThis file must be included in the Libraries selection if Averroes is not enabled and is<br>\nalso used in generating the Averroes summaries files if Averroes mode is enabled.<br>\nThis entry will be saved in the .janalyzer/site.properties file in the user's home directory,<br>\nso once set it will be retained indefinitely on the machine used (NOT in the config file).<br>\nNote that this should be set for Java version 7, since Averroes does not work with version 8<br>\nand the challenge problems specify using version 7.\n</html>");
        appAddButton.setPreferredSize(new java.awt.Dimension(86, 25));
        appAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appAddButtonActionPerformed(evt);
            }
        });

        mainSearchButton.setText("Search");
        mainSearchButton.setToolTipText("<html>\nThis will search all of the jar files for instances of main classes and<br>\nallow the user to select the one to be used as the main class for the program.<br>\nIf it finds one with a class name of Main it will select that one as the default,<br>\notherwise it will select the 1st main class in the list.\n</html>");
        mainSearchButton.setPreferredSize(new java.awt.Dimension(86, 25));
        mainSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mainSearchButtonActionPerformed(evt);
            }
        });

        startScriptButton.setText("Open");
        startScriptButton.setToolTipText("<html>\nThis sets the location of the start script to run.<br>\nThis should be defined in the challenge problem description.txt file.<br>\nIt is used for setting up Tamiflex and there may be user interaction to.<br>\nperform in order to get Tamiflex to complete,<br>\n</html>");
        startScriptButton.setPreferredSize(new java.awt.Dimension(86, 25));
        startScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startScriptButtonActionPerformed(evt);
            }
        });

        appClearButton.setBackground(new java.awt.Color(255, 204, 204));
        appClearButton.setText("Clear");
        appClearButton.setToolTipText("<html>\nThis sets the location of the Java Runtime library where the rt.jar file is located.<br>\nThis file must be included in the Libraries selection if Averroes is not enabled and is<br>\nalso used in generating the Averroes summaries files if Averroes mode is enabled.<br>\nThis entry will be saved in the .janalyzer/site.properties file in the user's home directory,<br>\nso once set it will be retained indefinitely on the machine used (NOT in the config file).<br>\nNote that this should be set for Java version 7, since Averroes does not work with version 8<br>\nand the challenge problems specify using version 7.\n</html>");
        appClearButton.setPreferredSize(new java.awt.Dimension(86, 25));
        appClearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appClearButtonActionPerformed(evt);
            }
        });

        autoRegexButton.setText("Auto");
        autoRegexButton.setToolTipText("<html>\nThis sets the location of the start script to run.<br>\nThis should be defined in the challenge problem description.txt file.<br>\nIt is used for setting up Tamiflex and there may be user interaction to.<br>\nperform in order to get Tamiflex to complete,<br>\n</html>");
        autoRegexButton.setPreferredSize(new java.awt.Dimension(86, 25));
        autoRegexButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoRegexButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout buttonPanelLayout = new javax.swing.GroupLayout(buttonPanel);
        buttonPanel.setLayout(buttonPanelLayout);
        buttonPanelLayout.setHorizontalGroup(
            buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonPanelLayout.createSequentialGroup()
                .addGroup(buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(buttonPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(appAddButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(appClearButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(mainSearchButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(startScriptButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(autoRegexButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        buttonPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {appAddButton, appClearButton, mainSearchButton, startScriptButton});

        buttonPanelLayout.setVerticalGroup(
            buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(appAddButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(appClearButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mainSearchButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(autoRegexButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addComponent(startScriptButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(68, 68, 68))
        );

        jLabel2.setText("Main class");

        jLabel4.setText("Start script");

        jLabel3.setText("App regex");

        jLabel1.setText("Application");

        javax.swing.GroupLayout labelPanelLayout = new javax.swing.GroupLayout(labelPanel);
        labelPanel.setLayout(labelPanelLayout);
        labelPanelLayout.setHorizontalGroup(
            labelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(labelPanelLayout.createSequentialGroup()
                .addGroup(labelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3))
                .addGap(24, 24, 24))
        );

        labelPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel4});

        labelPanelLayout.setVerticalGroup(
            labelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(labelPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(22, 22, 22)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addGap(26, 26, 26)
                .addComponent(jLabel4)
                .addGap(73, 73, 73))
        );

        javax.swing.GroupLayout setupPanelLayout = new javax.swing.GroupLayout(setupPanel);
        setupPanel.setLayout(setupPanelLayout);
        setupPanelLayout.setHorizontalGroup(
            setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(setupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(labelPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(entryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        setupPanelLayout.setVerticalGroup(
            setupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(buttonPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
            .addComponent(labelPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(entryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
        );

        runPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        elapsedTimePanel.setPreferredSize(new java.awt.Dimension(203, 56));

        elapsedTimeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        elapsedTimeLabel.setMaximumSize(new java.awt.Dimension(100, 15));
        elapsedTimeLabel.setMinimumSize(new java.awt.Dimension(100, 15));
        elapsedTimeLabel.setPreferredSize(new java.awt.Dimension(179, 21));

        javax.swing.GroupLayout elapsedTimePanelLayout = new javax.swing.GroupLayout(elapsedTimePanel);
        elapsedTimePanel.setLayout(elapsedTimePanelLayout);
        elapsedTimePanelLayout.setHorizontalGroup(
            elapsedTimePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(elapsedTimePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(elapsedTimeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        elapsedTimePanelLayout.setVerticalGroup(
            elapsedTimePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(elapsedTimePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(elapsedTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23))
        );

        commandPanel.setPreferredSize(new java.awt.Dimension(203, 127));

        averroesButton.setBackground(new java.awt.Color(204, 255, 204));
        averroesButton.setText("Averroes");
        averroesButton.setToolTipText("Begin producing the Averroes output files.");
        averroesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                averroesButtonActionPerformed(evt);
            }
        });

        stopButton.setBackground(new java.awt.Color(255, 204, 204));
        stopButton.setText("Stop");
        stopButton.setToolTipText("Stops the Averroes file generation.");
        stopButton.setPreferredSize(new java.awt.Dimension(70, 25));
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        saveButton.setText("Save");
        saveButton.setToolTipText("<html>\nSaves the generated files in the Janalyzer repo so they can<br>\nbe used as the standard Averroes files for this challenge problem.\n</html>");
        saveButton.setPreferredSize(new java.awt.Dimension(70, 25));
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        tamiflexButton.setBackground(new java.awt.Color(204, 255, 204));
        tamiflexButton.setText("Tamiflex");
        tamiflexButton.setToolTipText("Run the Tamiflex program to generate the reflection log file used for Averroes.");
        tamiflexButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tamiflexButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout commandPanelLayout = new javax.swing.GroupLayout(commandPanel);
        commandPanel.setLayout(commandPanelLayout);
        commandPanelLayout.setHorizontalGroup(
            commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(saveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tamiflexButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(averroesButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        commandPanelLayout.setVerticalGroup(
            commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tamiflexButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(commandPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(averroesButton)
                    .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(saveButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout runPanelLayout = new javax.swing.GroupLayout(runPanel);
        runPanel.setLayout(runPanelLayout);
        runPanelLayout.setHorizontalGroup(
            runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(runPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(elapsedTimePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(commandPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        runPanelLayout.setVerticalGroup(
            runPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(runPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(commandPanel, 123, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(elapsedTimePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout activePanelLayout = new javax.swing.GroupLayout(activePanel);
        activePanel.setLayout(activePanelLayout);
        activePanelLayout.setHorizontalGroup(
            activePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(activePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(setupPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(runPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        activePanelLayout.setVerticalGroup(
            activePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(activePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(activePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(runPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(setupPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE))
                .addContainerGap())
        );

        outputTabbedPane.setPreferredSize(new java.awt.Dimension(228, 214));

        statusScrollPane.setViewportView(statusTextPane);

        outputTabbedPane.addTab("Status", statusScrollPane);

        outputTextArea.setColumns(20);
        outputTextArea.setLineWrap(true);
        outputTextArea.setRows(5);
        outputTextArea.setWrapStyleWord(true);
        outputScrollPane.setViewportView(outputTextArea);

        outputTabbedPane.addTab("Output", outputScrollPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(outputTabbedPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(activePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(activePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(outputTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void averroesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_averroesButtonActionPerformed
        // reset the timer
        elapsedTimer.reset();
        
        // read the user selections for these
        String mainClass = (String)this.mainClassComboBox.getSelectedItem();
        if (mainClass == null)
            mainClass = "";
        String appRegex = this.appRegexTextField.getText();
        if (appRegex.isEmpty())
            appRegex = "**";

        // make sure we have an application file to read from
        DefaultListModel myAppList = configInfo.getList(ConfigInfo.ListType.application);
        if (myAppList.isEmpty()) {
            statusMessage(StatusType.Error, "No files defined for application");
            return;
        }
        String applist = "";
        for (int ix = 0; ix < myAppList.size(); ix++) {
            applist += ":" + (String)myAppList.get(ix);
        }
        applist = applist.substring(1); // remove leading ":"
        
        // if an embedded library is found & hasn't been extracted yet, ask user
        DefaultListModel myLibList = configInfo.getList(ConfigInfo.ListType.libraries);
        if (myLibList.isEmpty()) {
            checkForEmbeddedLibrary();
        }

        // verify the runtime jar file exists in the selected directory
        String rtjarFile = jrePathName + "/rt.jar";
        File rtjar = new File(rtjarFile);
        if (!rtjar.isFile()) {
            statusMessage(StatusType.Error, "file not found: " + rtjarFile);
            return;
        }

        // create the subdir in the project path to store result files
        File outpath = new File(outPathName);
        if (!outpath.isDirectory()) {
            statusMessage(StatusType.Info, "creating project dir: " + outPathName);
            outpath.mkdirs();
        }
        
        // make sure nothing was running prior to this
        threadLauncher.init(new StandardTermination());

        // we need to search for the tamiflex file. the possible locations:
        // 1. one has been previously created by this tool and placed in the averroes
        //    output folder in the project path (outPathName)
        // 2. one was previously created by this tool but was unable to be copied
        //    to the output folder, so we can use it from where it was created (challengePathName).
        // check if the project contains the summary file from tamiflex
        String tamiflexFile = challengePathName + "out/" + TAMIFLEX_REFL;
        File dest = new File(tamiflexFile);
        if (!dest.isFile()) {
            tamiflexFile = outPathName + TAMIFLEX_REFL;
            File source = new File(tamiflexFile);
            if (!source.isFile()) {
                tamiflexFile = "";
            }
        }
        if (!tamiflexFile.isEmpty()) {
            statusMessage(StatusType.Info, "Tamiflex file found: " + tamiflexFile);
            // determine if Tamiflex was previously run during this launch of Averroes tool.
            // If so, assume the file is valid for this project.
            // If not, ask the user whether he wants to use it or not.
            // It might be valid, but it might be from a previous project run.
            if (!bTamiflexCompleted) {
                // determine what the user wants to do from here.
                String[] selection = new String[] { "Ignore", "Use" };
                String title   = "Old reflection file found";
                String message = "A Tamiflex reflection file was found but Tamiflex" + newLine +
                                 "was not run during this session of Averroes." + newLine +
                                 "Do you wish to Use this file or Ignore it and run" + newLine +
                                 "without Tamiflex?";
                int which = JOptionPane.showOptionDialog(null, // parent component
                        message,        // message
                        title,          // title
                        JOptionPane.DEFAULT_OPTION, // option type
                        JOptionPane.PLAIN_MESSAGE,  // message type
                        null,           // icon
                        selection,      // selection options
                        selection[0]);  // initial value
                if (which >= 0 && "Ignore".equals(selection[which])) {
                    // don't use the Tamiflex file
                    tamiflexFile = "";
                }
            }
        }
        else {
            statusMessage(StatusType.Warning, "file not found: " + outPathName + TAMIFLEX_REFL);
        }
        
        // start the elapsed time counter
        elapsedTimer.start();

        // start with the base command
        String[] command = { "java", "-jar", toolPathName + "averroes.jar",
                              "-r", appRegex.trim(),
                              "-m", mainClass.trim(),
                              "-a", applist,
                              "-o", outPathName,
                              "-j", jrePathName
                            };
        
        // add library files if any defined
        String classpath = createClasspath();
        if (!classpath.isEmpty()) {
            String[] addclasspath = { "-l", classpath };
            command = ArrayUtils.addAll(command, addclasspath);
        }

        // add the averroes command to the thread
        if (!tamiflexFile.isEmpty()) {
            String[] tamicommand  = { "-t", tamiflexFile };
            command = ArrayUtils.addAll(command, tamicommand);
        }
        else {
            statusMessage(StatusType.Warning, "Running Averroes without Tamiflex");
        }

        // run the command
        threadLauncher.launch(command, challengePathName, JobName.averroes.toString(), null);

        // disable all start action buttons and enable the stop button
        setRunMode(true);
    }//GEN-LAST:event_averroesButtonActionPerformed
   
    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        ThreadLauncher.ThreadInfo threadInfo = threadLauncher.stopAll();
        elapsedTimer.stop();    // stop the elapsed timer count

        // set zip index to end to prevent rest of list from executing
        if (zipgrepJarlist != null) {
            zipgrepIndex = zipgrepJarlist.length;
            zipgrepSearchTerm = "main";
        }
            
        // stop the running process
        if (threadInfo.pid >= 0) {
            statusMessage(StatusType.Warning, "Killing job " + threadInfo.jobid + ": pid " + threadInfo.pid);
            String[] command = { "kill", "-15", threadInfo.pid.toString() };
            commandLauncher.start(command, null);
        }

        stopButton.setEnabled(false);
    }//GEN-LAST:event_stopButtonActionPerformed

    private String getDestinationPath (boolean bShow){
        // make sure the averroes dir in the project path exists
        File averPath = new File(outPathName);
        if (!averPath.isDirectory()) {
            if (bShow)
                statusMessage(StatusType.Error, "source path not found: " + outPathName);
            return null;
        }

        // determine if janalyzer path is valid (assume janalyzer is run from current dir)
        String destPathName = System.getProperty("user.dir") + "/test/averroes";
        averPath = new File(destPathName);
        if (!averPath.isDirectory()) {
            if (bShow)
                statusMessage(StatusType.Error, "dest path not found: " + destPathName);
            return null;
        }
        
        // must also have a valid project name, since that is the subdir we will
        // place the files in.
        String projname = configInfo.getField(ConfigInfo.StringType.projectname);
        if (projname.isEmpty()) {
            if (bShow)
                statusMessage(StatusType.Error, "projName = (empty)");
            return null;
        }
        destPathName += "/" + projname;
        averPath = new File(destPathName);
        if (!averPath.isDirectory()) {
            // create directory
            averPath.mkdirs();
        }
        return destPathName;
    }
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // upon completion...
        // check if the user created files without saving them
        String destPathName = getDestinationPath (false);
        if (bFilesGenerated && destPathName != null) {
            String[] selection = new String[] { "No", "Yes" };
            String title   = "Files have not been saved";
            String message = "Tamiflex and/or Averroes files have been created" + newLine +
                             "but have not been saved to Janalyzer directory." + newLine +
                             "Do you wish to copy them to Janalyzer before exiting?";
            int which = JOptionPane.showOptionDialog(null, // parent component
                    message,        // message
                    title,          // title
                    JOptionPane.DEFAULT_OPTION, // option type
                    JOptionPane.PLAIN_MESSAGE,  // message type
                    null,           // icon
                    selection,      // selection options
                    selection[0]);  // initial value
            if (which >= 0 && "Yes".equals(selection[which])) {
                // copy the files to Janalyzer dir
                boolean bApp1 = copyAverroesFile(AVERROES_APP_JAR1, destPathName);
                boolean bApp2 = copyAverroesFile(AVERROES_APP_JAR2, destPathName);
                boolean bLib  = copyAverroesFile(AVERROES_LIB_JAR,  destPathName);
                boolean bTami = copyAverroesFile(TAMIFLEX_REFL,     destPathName);

                // update the config file to set to Averroes mode
                if (bApp1 && bApp2 && bLib) {
                    updateConfigFileParam ("averroes", "yes");
                }
            }
        }
        
        // restore the stdout and stderr
        System.setOut(standardOut);
        System.setErr(standardErr);
        // re-enable the run button on the AnalyzerFrame
        AnalyzerFrame.exitFromAverroesFrame();
    }//GEN-LAST:event_formWindowClosing

    private void appRegexTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_appRegexTextFieldActionPerformed
        // update the config file
        JTextField textField = (JTextField) evt.getSource();
        String value = textField.getText();
        if (value != null) {
            // update the config file if the value has changed
            String appRegEx = configInfo.getField(ConfigInfo.StringType.appRegEx);
            if (!appRegEx.equals(value))
                updateConfigFileParam ("appRegEx", value);
        }
    }//GEN-LAST:event_appRegexTextFieldActionPerformed

    private void appAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_appAddButtonActionPerformed
        String projpath = configInfo.getField(ConfigInfo.StringType.projectpath);
        if (!projpath.isEmpty()) {
            File dfltPath = new File(projpath);
            this.fileChooser.setCurrentDirectory(dfltPath);
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Jar Files","jar");
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

            // update RegEx selection based on the application file selection
            setRegExField ();
        }
    }//GEN-LAST:event_appAddButtonActionPerformed

    private void mainClassComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mainClassComboBoxActionPerformed
        JComboBox comboBox = (JComboBox) evt.getSource();
        String command = evt.getActionCommand();        // comboBoxChanged
        String selected = (String) comboBox.getSelectedItem();   // the new selection
        if (selected != null && command.equals("comboBoxChanged")) {
            // update the config file if the value has changed
            String mainClass = configInfo.getField(ConfigInfo.StringType.mainClass);
            if (!mainClass.equals(selected))
                updateConfigFileParam ("mainClass", selected);
        }
    }//GEN-LAST:event_mainClassComboBoxActionPerformed
    
    private void mainSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mainSearchButtonActionPerformed
        // clear the status screen
        this.statusTextPane.setText("");

        // if an embedded library is found & hasn't been extracted yet, ask user
        checkForEmbeddedLibrary();
        
        // disable the run buttons and enable the stop button
        setRunMode(true);
        
        // start the process for getting the Main Class selections
        launchMainClassZipGrep();
    }//GEN-LAST:event_mainSearchButtonActionPerformed
    
    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        // make sure the averroes dir in the project path exists
        String destPathName = getDestinationPath (true);
        if (destPathName != null) {
            // copy the files to Janalyzer dir
            boolean bApp1 = copyAverroesFile(AVERROES_APP_JAR1, destPathName);
            boolean bApp2 = copyAverroesFile(AVERROES_APP_JAR2, destPathName);
            boolean bLib  = copyAverroesFile(AVERROES_LIB_JAR,  destPathName);
            boolean bTami = copyAverroesFile(TAMIFLEX_REFL,     destPathName);

            // update the config file to set to Averroes mode
            if (bApp1 && bApp2 && bLib) {
                updateConfigFileParam ("averroes", "yes");
            }
        
            // indicate there are no outstanding files to save
            bFilesGenerated = false;
        }
    }//GEN-LAST:event_saveButtonActionPerformed

    private void startScriptTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startScriptTextFieldActionPerformed
        // update the config file
        JTextField textField = (JTextField) evt.getSource();
        String value = textField.getText();
        if (value == null)
            tamiflexButton.setEnabled(false);
        else {
            updateConfigFileParam ("startScript", value);

            // if entry is valid, enable tamiflex button
            if (!value.isEmpty())
                tamiflexButton.setEnabled(true);
            else
                tamiflexButton.setEnabled(false);
        }
    }//GEN-LAST:event_startScriptTextFieldActionPerformed

    private void startScriptButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startScriptButtonActionPerformed
        this.startScriptChooser.setMultiSelectionEnabled(false);
        String currentPath = this.startScriptTextField.getText();
        String projpath = configInfo.getField(ConfigInfo.StringType.projectpath);
        File initPath = new File(currentPath);
        if (initPath.exists())
            this.startScriptChooser.setCurrentDirectory(initPath);
        else
            this.startScriptChooser.setCurrentDirectory(new File(projpath));
        int retVal = this.startScriptChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            // get the selected file
            File file = this.startScriptChooser.getSelectedFile();
            if (file.isFile()) {
                String scriptName = file.getAbsolutePath();
                this.startScriptTextField.setText(scriptName);
                updateConfigFileParam ("startScript", scriptName);

                // enable tamiflex button
                tamiflexButton.setEnabled(true);
            }
        }
    }//GEN-LAST:event_startScriptButtonActionPerformed
    
    private void tamiflexButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tamiflexButtonActionPerformed
        // requires the main class be defined
        String mainClass = (String)this.mainClassComboBox.getSelectedItem();
        if (mainClass == null || mainClass.isEmpty()) {
            statusMessage(StatusType.Error, "main class not defined");
            return;
        }

        // remove any previous output files
        File tamipath = new File(challengePathName + "out");
        if (tamipath.exists()) {
            statusMessage (StatusType.Info, "deleting dir: " + tamipath);
            try {
                FileUtils.deleteDirectory(tamipath);
            } catch (IOException ex) {
                statusMessage (StatusType.Error, ex + "deleting dir: " + tamipath);
            }
        }
        File tamifile = new File(outPathName + TAMIFLEX_REFL);
        if (tamifile.isFile()) {
            statusMessage (StatusType.Info, "deleting file: " + outPathName + TAMIFLEX_REFL);
            tamifile.delete();
        }
        
        // make sure nothing was running prior to this
        threadLauncher.init(new StandardTermination());

        // start the elapsed time counter
        elapsedTimer.start();
        
        // add the tamiflex command to the thread
        DefaultListModel myAppList = configInfo.getList(ConfigInfo.ListType.application);
        DefaultListModel myLibList = configInfo.getList(ConfigInfo.ListType.libraries);
        if (myLibList.isEmpty() && !myAppList.isEmpty()) {
            // this is if there is an application list but not a library list
            String applist = "";
            for (int ix = 0; ix < myAppList.size(); ix++) {
                applist += ":" + (String)myAppList.get(ix);
            }
            applist = applist.substring(1); // remove leading ":"
            String[] command = { "java", "-javaagent:" + toolPathName + "poa-trunk.jar"
                                ,"-jar", applist, this.startScriptTextField.getText()
                      };
            threadLauncher.launch(command, challengePathName, JobName.tamiflex.toString(), null);
        }
        else {
            String classpath = createClasspath();
            String[] command = { "java", "-javaagent:" + toolPathName + "poa-trunk.jar"
                                ,"-cp", classpath, mainClass, this.startScriptTextField.getText()
                      };
            threadLauncher.launch(command, challengePathName, JobName.tamiflex.toString(), null);
        }

        // disable all start action buttons and enable the stop button
        setRunMode(true);
    }//GEN-LAST:event_tamiflexButtonActionPerformed

    private void appRegexTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_appRegexTextFieldFocusLost
        // update the config file
        JTextField textField = (JTextField) evt.getSource();
        String value = textField.getText();
        if (value != null) {
            // update the config file if the value has changed
            String appRegEx = configInfo.getField(ConfigInfo.StringType.appRegEx);
            if (!appRegEx.equals(value))
                updateConfigFileParam ("appRegEx", value);
        }
    }//GEN-LAST:event_appRegexTextFieldFocusLost

    private void appClearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_appClearButtonActionPerformed
        DefaultListModel listModel = (DefaultListModel)this.appList.getModel();
        listModel.clear();
        updateConfigFileParam ("application", null);
    }//GEN-LAST:event_appClearButtonActionPerformed

    private void autoRegexButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoRegexButtonActionPerformed
        // auto-generate the regEx field
        setRegExField();
    }//GEN-LAST:event_autoRegexButtonActionPerformed

    // the types of programs used
    private enum ProgName {
        none, averroes, tamiflex;
    }
    
    // the types of jobs to run in ThreadLauncher
    private enum JobName {
        building, zipgrep, averroes, tamiflex;
    }
    
    private static final String newLine = System.getProperty("line.separator");

    // these are made public so AnalyzerFrame can access them for copying the
    // files over to the janalyzer directory.
    public static final String AVERROES_APP_JAR1 = "averroes-lib-class.jar";
    public static final String AVERROES_APP_JAR2 = "organized-app.jar";
    public static final String AVERROES_LIB_JAR = "placeholder-lib.jar";
    public static final String TAMIFLEX_REFL = "refl.log";

    private final ConfigInfo configInfo;
    private final String toolPathName;
    private final String outPathName;
    private final String challengePathName;
    private final String jrePathName;
    private boolean   bTamiflexCompleted;
    private final ArrayList<String> mainClassList;
    private ArrayList<String> libraryPackageList;
    private String    mainClassValue;
    private String    mainClassPrev;
    private boolean   bFilesGenerated;

    private File[]   zipgrepJarlist;
    private String   zipgrepSearchTerm;
    private int      zipgrepIndex;
    
    // these will contain the original stdout and stderr values to restore upon exit
    private final PrintStream     standardOut;
    private final PrintStream     standardErr; 
    private final ElapsedTimer    elapsedTimer;
    private static DebugMessage   debug;

    private final ThreadLauncher  threadLauncher;
    private final CommandLauncher commandLauncher;
    private final AudioClipPlayer audioplayer;
        
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel activePanel;
    private javax.swing.JButton appAddButton;
    private javax.swing.JButton appClearButton;
    private javax.swing.JList<String> appList;
    private javax.swing.JTextField appRegexTextField;
    private javax.swing.JScrollPane appScrollPane;
    private javax.swing.JButton autoRegexButton;
    private javax.swing.JButton averroesButton;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JPanel commandPanel;
    private javax.swing.JLabel elapsedTimeLabel;
    private javax.swing.JPanel elapsedTimePanel;
    private javax.swing.JPanel entryPanel;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JFileChooser jrePathChooser;
    private javax.swing.JPanel labelPanel;
    private javax.swing.JComboBox<String> mainClassComboBox;
    private javax.swing.JButton mainSearchButton;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JTabbedPane outputTabbedPane;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JPanel runPanel;
    private javax.swing.JButton saveButton;
    private javax.swing.JPanel setupPanel;
    private javax.swing.JButton startScriptButton;
    private javax.swing.JFileChooser startScriptChooser;
    private javax.swing.JTextField startScriptTextField;
    private javax.swing.JScrollPane statusScrollPane;
    private javax.swing.JTextPane statusTextPane;
    private javax.swing.JButton stopButton;
    private javax.swing.JButton tamiflexButton;
    // End of variables declaration//GEN-END:variables
}
