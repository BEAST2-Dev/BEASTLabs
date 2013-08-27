package beast.util;

import beast.core.Citation;
import beast.core.Description;
import test.beast.beast2vs1.trace.LogFileTraces;
import test.beast.beast2vs1.trace.TraceException;
import test.beast.beast2vs1.trace.TraceStatistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Description("Log Analyser Advanced: auto optimize burnin. Extend from Log Analyser in BEAST 1 created by Alexei Drummond.")
@Citation("Created by Walter Xie")
public class LogAnalyserAdv {
    public LogAnalyserAdv() throws Exception {
        super();
    }
    /**
     * @param file the name of the log file to analyze
     * @param traceNames only print the given trace, if traceName == null, print all traces
     * @return TraceStatistics     an array og analyses of the statistics in a log file
     * @throws java.io.IOException if general error reading file
     * @throws test.beast.beast2vs1.trace.TraceException      if trace file in wrong format or corrupted
     */
    public static LogFileTraces analyzeLogFileAutoBurnin(File file, String[] traceNames) throws java.io.IOException, TraceException {

        LogFileTraces traces = new LogFileTraces(file.getName(), file);
        traces.loadTraces();

        int percLower = 5;
        int percUpper = 80;
        int percIncremental = 5;
//        double[][] essMatrix = new double[traceNames.length][(percUpper-percLower)/percIncremental+1];
        double essThreshould = 100;

        // get all ess, record burnin
        int[] bestPercentage = new int[traceNames.length];
        double[] maxESS = new double[traceNames.length];
        int bestPerc = 0;
        double maxSumESS = 0;
        for (int percentage = percLower; percentage <= percUpper; percentage+=percIncremental) {
            long burnin = (traces.getMaxState() / 100) * percentage; // start from 10%
            traces.setBurnIn(burnin);

            double sumESS = 0;
            for (int n = 0; n < traceNames.length; n++) {
                int traceIndex = traces.getTraceIndex(traceNames[n]);
                TraceStatistics distribution = traces.analyseTrace(traceIndex);

                double ess = distribution.getESS();
                if (ess > maxESS[n]) {
                    maxESS[n] = ess;
                    bestPercentage[n] = percentage;
                }
                sumESS += ess;
            }
            if (sumESS > maxSumESS) {
                maxSumESS = sumESS;
                bestPerc = percentage;
            }
        }

//        for (int n = 0; n < traceNames.length; n++) {
//            if (maxESS[n] < 100) System.out.println(traceNames[n]  + "\t" + maxESS[n]);
//        }

        long burnin = (traces.getMaxState() / 100) * bestPerc; // best burnin detemined by max of sum of ess
        traces.setBurnIn(burnin);
        for (int i = 0; i < traces.getTraceCount(); i++) {
            TraceStatistics distribution = traces.analyseTrace(i);
        }

        return traces;
    }

    /**
     * @param file the name of the log file to analyze
     * @param inBurnin   the state to discard up to
     * @param traceName only print the given trace, if traceName == null, print all traces
     * @return an array og analyses of the statistics in a log file.
     * @throws java.io.IOException if general error reading file
     * @throws test.beast.beast2vs1.trace.TraceException      if trace file in wrong format or corrupted
     */
    public static void shortReport(LogFileTraces traces, String[] traceNames) throws java.io.IOException, TraceException {
        System.out.print(traces.getName() + "\t" + traces.getMaxState() + "\t" + traces.getBurnIn() + "\t");

        for (String traceName : traceNames) {
//            if (traceName != null && traceName.equalsIgnoreCase(traces.getTraceName(i))) {
            int traceIndex = traces.getTraceIndex(traceName);
            TraceStatistics distribution = traces.analyseTrace(traceIndex);

            System.out.print(traceName + "\t");

            System.out.print(distribution.getMean() + "\t");
//            System.out.print(distribution.getStdev() + "\t");

//            System.out.print(distribution.getHpdLower() + "\t");
//            System.out.print(distribution.getHpdUpper() + "\t");

//                System.out.print(formatter.format(distribution.getStdErrorOfMean()));
//                System.out.print(formatter.format(distribution.getMinimum()));
//                System.out.print(formatter.format(distribution.getMaximum()));
//                System.out.print(formatter.format(distribution.getMedian()));
//                System.out.print(formatter.format(distribution.getGeometricMean()));
//                System.out.print(formatter.format(distribution.getVariance()));
//                System.out.print(formatter.format(distribution.getCpdLower()));
//                System.out.print(formatter.format(distribution.getCpdUpper()));
            System.out.print(distribution.getAutoCorrelationTime() + "\t");
//                System.out.print(formatter.format(distribution.getStdevAutoCorrelationTime()));

            double ess = distribution.getESS();
//            if (ess < 100) System.out.print("*");
            System.out.print(distribution.getESS());
//            if (ess < 100) System.out.print("*");
            System.out.print("\t");
//            System.out.println();
//            }
        }
        System.out.println();
    }

    //Main method
    public static void main(final String[] args) throws IOException, TraceException {
        String workPath = "/Users/dxie004/Documents/BEAST2/*BEAST-sim/Evolution2013/new100/sp8-4/";
        System.out.println("\nWorking path = " + workPath);

        String[] traceNames = new String[]{"posterior", "TreeHeight.Species"}; //"posterior", "TreeHeight.Species"
//        int burnIN = -1;
        int[] trees = new int[] {128}; //2,4,8,16,32,64,128,256
        boolean isCombined = false;
        int replicates = 100;

        if (!isCombined) {
            for (int tree : trees) {
                for (int r=0; r<replicates; r++) {
//                System.out.println("\nGo to folder " + tree + "/" + r + " ...");
                    String folderName = Integer.toString(tree) + "-resume";
                    File folder = new File(workPath + folderName + File.separator + r);
                    File[] listOfFiles = folder.listFiles();

                    int log = 0;
                    for (int i = 0; i < listOfFiles.length; i++) {
                        File file = listOfFiles[i];
                        if (file.isFile()) {
                            String fileName = file.getName();
                            if (fileName.endsWith("stdout.txt")) {
//                            System.out.println("\nReading screen log " + fileName + " ...");
                                BufferedReader reader = new BufferedReader(new FileReader(file));
                                String line = reader.readLine();
                                String time = "?";
                                while (line != null) {
                                    if (line.startsWith("Total calculation time:")) {
                                        String[] fields = line.split(" ", -1);
                                        time = fields[3];
                                    }
                                    line = reader.readLine();
                                }
                                reader.close();
                                System.out.println(r + "\t" + time + "\tseconds");
                            } else if (fileName.endsWith(".log")) {
                                log++;
                            }
                        }
                    }

                    if (log < 1) {
                        System.err.println(r + "\tno log !");
                    } else if (log > 1) {
                        System.err.println(r + "\t" + log + " logs !");
                    }
                }
            }
        }

        for (int tree : trees) {
            for (int r=0; r<replicates; r++) {
                String folderName = isCombined ? Integer.toString(tree) + "-combined" : Integer.toString(tree);
                System.out.print(folderName + "\t" + r + "\t");
                File folder = new File(workPath + folderName + File.separator + r);
                File[] listOfFiles = folder.listFiles();

                for (int i = 0; i < listOfFiles.length; i++) {
                    File file = listOfFiles[i];
                    if (file.isFile()) {
                        String fileName = file.getName();
                        if (fileName.endsWith(".log")) {
//                            System.out.println("\nReading beast log " + fileName + " ...");
                            LogFileTraces traces = analyzeLogFileAutoBurnin(file, traceNames);
                            shortReport(traces, traceNames);
                        }
                    }
                }
            }
        }

//        for (int tree : trees) {
//            for (int r=0; r<replicates; r++) {
////                System.out.println("\nGo to folder " + tree + "/" + r + " ...");
//                File folder = new File(workPath + tree + File.separator + r);
//                File[] listOfFiles = folder.listFiles();
//
//                for (int i = 0; i < listOfFiles.length; i++) {
//                    File file = listOfFiles[i];
//                    if (file.isFile()) {
//                        String fileName = file.getName();
//                        if (fileName.endsWith(".log")) {
////                            System.out.println("\nReading beast log " + fileName + " ...");
//                            shortReport(file, burnIN, "TreeHeight.Species");
//                        }
//                    }
//                }
//            }
//        }
        System.out.println("\nDone");
    }

}
