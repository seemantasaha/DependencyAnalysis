import com.ibm.wala.shrikeCT.InvalidClassFileException;
import core.LibrarySummary;
import core.Program;
import gui.MainLogic;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PReach {

    private static String  myProjPath;
    private static String  jrePathName;
    public static ArrayList<String> methodList = new ArrayList<>();
    public static ArrayList<String> methodSignList = new ArrayList<>();

    private static Long analysisStartTime;

    private String rootDir;
    private ArrayList<String> appPaths;
    private ArrayList<String> libPaths;
    private String apiPath;
    private String entryFilePath;
    private String procSign;
    private ArrayList<String> testInputParams;
    private String branchProbFile;

    //private ArrayList<String> procList = new ArrayList<>();

    private RunProgramGen programGen;
    private Thread  genThread;          // thread in which generateProgram() runs
    private final Timer timer;        // timer for checking on status of generateProgram()
    private int elapsedSecs;

    static public String classPath = "";

    PReach(ArrayList<String> appPaths, ArrayList<String> libPaths,
           String apiPath, String entryFilePath,
           String procSign, ArrayList<String> testInputParams, String branchProbFile) {
        timer = new Timer(1000, new TimerListener());
        this.appPaths = appPaths;
        this.libPaths = libPaths;
        this.apiPath = apiPath;
        this.entryFilePath = entryFilePath;
        this.procSign = procSign;
        this.testInputParams = testInputParams;
        this.branchProbFile = branchProbFile;
    }

    PReach(String rootDir, ArrayList<String> appPaths, ArrayList<String> libPaths,
           String apiPath, String entryFilePath, ArrayList<String> methodList, ArrayList<String> methodSignList, String branchProbFile) {
        timer = new Timer(1000, new TimerListener());
        this.rootDir = rootDir;
        this.appPaths = appPaths;
        this.libPaths = libPaths;
        this.apiPath = apiPath;
        this.entryFilePath = entryFilePath;
        this.branchProbFile = branchProbFile;
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

            // check if thread has completed
            if (!done && programGen.exitcode >= 0) {

                // program generation was successful
                if (programGen.exitcode == 0) {
                    try {
                        // stop this timer
                        timer.stop();
                        done = true;
                    } catch (Exception ex) {
                    }
                }
                else {
                    // error occurred. clear the thread so we can try again.
                    genThread = null;
                }
            }
        }
    }

    private int generateProgram () {
        try {
            if(this.entryFilePath.equals("")) {
                this.entryFilePath = null; //for public methods as entry points
            }
            Program.makeProgram(this.appPaths, this.libPaths, this.apiPath, this.entryFilePath);
            Program.analyzeProgram();
            Set<String> misses = Program.checkAnalysisScope();
            Set<String> unknowns = LibrarySummary.getUnknownMethodSet();


            //doAnalysis(procSign, testInputParams);
            doAnalysis(methodList);

        } catch (Exception e) {
            e.printStackTrace();
            return 2;
        }

        return 0;
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

    private void doAnalysis(String procSign, ArrayList<String> testInputParams) throws InvalidClassFileException {
        MainLogic mainLogic = new MainLogic();
        mainLogic.doDependencyAnalysis(procSign, testInputParams);
        mainLogic.collectBranchProbabilities(branchProbFile);
        mainLogic.initializeForPReachAnalysis();
        mainLogic.doMarkovChainAnalysis();
        Long analysisEndTime = System.currentTimeMillis();
        Long totalAnalysisTime = analysisEndTime - analysisStartTime;
        System.out.println("PReach Analysis Time: " + totalAnalysisTime);
    }

    private void doAnalysis(ArrayList<String> methodList) throws InvalidClassFileException {
        MainLogic mainLogic = new MainLogic();
        ArrayList<String> preachFeatureList = mainLogic.doPReachAnalysis(methodList, branchProbFile);

        //Write preach features to a file
        String preachFeatureFile = rootDir + "/PreachFeatures.csv";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(preachFeatureFile, false));
            for (String feature : preachFeatureList) {
                writer.write(feature);
            }
            writer.close();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        Long analysisEndTime = System.currentTimeMillis();
        Long totalAnalysisTime = analysisEndTime - analysisStartTime;
        System.out.println("PReach Analysis Time: " + totalAnalysisTime);
    }

    private void launchProgramGen () {
        // start the program generation in another thread
        if (genThread != null)
            return;

        // start the program generation in a seperate thread
        programGen = new RunProgramGen();
        genThread = new Thread(programGen);
        genThread.start();

        // start the timer for indicating elapsed time
        elapsedSecs = 0;
        timer.setInitialDelay(0);
        timer.start();
    }

    private static void addClassPaths(String rootDir, Set<String> classSet) {
        File root = new File(rootDir);
        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(root.listFiles()));
        for (File classFile : fileList) {
            if (classFile.isDirectory()) {
                File dir = new File(classFile.getAbsolutePath());
                addClassPaths(dir.toString(), classSet);
            } else {
                String name = classFile.getAbsolutePath();
                if(name.endsWith(".class")) {
                    classSet.add(name);
                }
            }
        }
    }

    public static void main(String args[]) throws InvalidClassFileException {

//        String[] classList = args[0].split(",");
//        String[] libList = args[1].split(",");
//        String procSign = args[2];
//        String[] paramList = args[3].split(",");
//        String branchProbFile = args[4];
//
//        ArrayList<String> testInputParams = new ArrayList(Arrays.asList(paramList));

//        PReach preach = new PReach(new ArrayList(Arrays.asList(classList)),
//                new ArrayList(Arrays.asList(libList)), "", "",
//                procSign, testInputParams, branchProbFile);

        //---------------------------------------------------------------------------
        analysisStartTime = System.currentTimeMillis();

        //Additional code for extracting code metric features from PReach
        Set<String> classSet = new HashSet<>();
        String rootDir = args[0];
        String[] libList = args[1].split(";");
        String codeMetricsFile = args[2];
        String branchProbFile = args[3];

        File file = new File(codeMetricsFile);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            String st;
            while ((st = br.readLine()) != null) {
                if(st.startsWith("ID,method"))
                    continue;
                String methodInfo = st.split(",")[1];
                methodList.add(methodInfo);
                String methodSign = st.split(",")[2];
                methodSignList.add(methodSign);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


//        for (String methodInfo : methodList) {
//            String className = methodInfo.split(":")[0];
//            className = className.replace(".","/");
//            //className = rootDir + "/target/classes/" + className + ".class";
//            String classDir = rootDir + "/classes/checkstyle/" + className + ".class";
//
//            File tmpDir = new File(classDir);
//            boolean exists = tmpDir.exists();
//            if(exists) {
//                //System.out.println(classDir);
//                classSet.add(classDir);
//            }
//            else {
//                classDir = rootDir + "/classes/tests/" + className + ".class";
//                tmpDir = new File(classDir);
//                exists = tmpDir.exists();
//                if(exists) {
//                    //System.out.println(classDir);
//                    classSet.add(classDir);
//                }
//            }
//            else
//                System.err.println("This class does not exist");
//        }

        if(rootDir.endsWith(".jar")) {
            classSet.add(rootDir);
            rootDir = rootDir.substring(0,rootDir.lastIndexOf("/"));
        } else {
            addClassPaths(rootDir, classSet);
        }

        //Write entries to entryFile
//        String entryFilePath = rootDir + "/entryMethods.txt";
//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter(entryFilePath, false));
//            for (String entry : methodSignList) {
//                writer.write(entry + "\n");
//            }
//            writer.close();
//        } catch(Exception ex) {
//            ex.printStackTrace();
//        }
        //---------------------------------------------------------------------------

        PReach preach = new PReach(rootDir, new ArrayList<String>(classSet), new ArrayList(Arrays.asList(libList)), "", "", methodList, methodSignList, branchProbFile);

        preach.launchProgramGen();
    }
}
