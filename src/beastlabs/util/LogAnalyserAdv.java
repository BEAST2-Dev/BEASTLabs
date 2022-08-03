package beastlabs.util;

import beast.base.core.Citation;
import beast.base.core.Description;
import test.beast.beast2vs1.trace.LogFileTraces;
import test.beast.beast2vs1.trace.TraceException;
import test.beast.beast2vs1.trace.TraceStatistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Description("Advanced Log Analyser: auto optimizing burnin to analyse batch proceeded logs. " +
        "It is modified from Log Analyser in BEAST 1 created by Alexei Drummond.\n" +
        "The input has to fit into a specified folder structure: workPath / tree / replicate. " +
        "For example, tree=2 and replicate=88, then analyser looks for files workPath/2/88/*.log.\n" +
        "traceNames define the logged parameters to be analysed.\n" +
        "trees and replicates provide the folder structure from batch processes, and could be changed according different simulations.\n" +
        "isCombined determines (true) to analyse combined logs, (false) to analyse an individual resumed/initial log.\n" +
        "percLower and percUpper are the percentage of all states to start and end for auto optimizing burnin.\n" +
        "percIncremental defines the incremental of percentage each search for auto optimizing burnin.\n" +
        "")
@Citation(value= "HA Ogilvie, J Heled, D Xie, AJ Drummond, " +
        "Computational performance and statistical accuracy of *BEAST and comparisons with other methods, " +
        "Systematic Biology, 2016")
public class LogAnalyserAdv {

    /**
     * @param file the name of the log file to analyze
     * @param traceNames only print the given trace, if traceName == null, print all traces
     * @return TraceStatistics     an array of analyses of the statistics in a log file
     * @throws java.io.IOException if general error reading file
     * @throws test.beast.beast2vs1.trace.TraceException      if trace file in wrong format or corrupted
     */
    public static LogFileTraces analyzeLogFileAutoBurnin(File file, String[] traceNames) throws java.io.IOException, TraceException {

        LogFileTraces traces = new LogFileTraces(file.getName(), file);
        traces.loadTraces();

        final int percLower = 5; // included
        final int percUpper = 80; // included
        final int percIncremental = 5;

        // get all ess, record burnin
        double[][] essMatrix = new double[traceNames.length][(percUpper-percLower)/percIncremental+1];
        int[] percentages = new int[essMatrix[0].length];
        int p = 0;
        for (int percentage = percLower; percentage <= percUpper; percentage+=percIncremental) {
            long burnin = (traces.getMaxState() / 100) * percentage; // state of burnin
            traces.setBurnIn(burnin);

            for (int n = 0; n < traceNames.length; n++) {
                int traceIndex = traces.getTraceIndex(traceNames[n]);
                TraceStatistics distribution = traces.analyseTrace(traceIndex);
                essMatrix[n][p] = distribution.getESS();
            }
            percentages[p] = percentage;
            p++;
        }

        long selectedBurnin = getBestBurninPercentage(essMatrix, percentages);
        long burnin = (traces.getMaxState() / 100) * selectedBurnin; // best burnin detemined by max of sum of ess
        traces.setBurnIn(burnin);
        for (int i = 0; i < traces.getTraceCount(); i++) {
            TraceStatistics distribution = traces.analyseTrace(i);
        }

        return traces;
    }

    // choose best burnin: max of sum of ratio of ess and max ess
    // essMatrix: 1st [] is traces, 2nd [] is percentage incremental
    public static int getBestBurninPercentage(double[][] essMatrix, int[] percentages) {
        final double essThre = 200;

        double[] maxESS = new double[essMatrix.length];
        for (int p = 0; p < essMatrix[0].length; p++) {
            for (int n = 0; n < essMatrix.length; n++) {
                if (essMatrix[n][p] > maxESS[n])
                    maxESS[n] = essMatrix[n][p];
            }
        }

        int bestPerc = 0;
        int bestPerc1 = 0;
        double maxESSSumRatio = 0;
        double maxESSSumRatio1 = 0;

        for (int p = 0; p < essMatrix[0].length; p++) {
            double ratioSum = 0;
            boolean allSucc = true;
            for (int n = 0; n < essMatrix.length; n++) {
                ratioSum += essMatrix[n][p] / maxESS[n];
                allSucc = allSucc && essMatrix[n][p] >= essThre;
            }
            if (allSucc) {
                if (ratioSum > maxESSSumRatio) {
                    maxESSSumRatio = ratioSum;
                    bestPerc = percentages[p];
                }
            } else {
                if (ratioSum > maxESSSumRatio1) {
                    maxESSSumRatio1 = ratioSum;
                    bestPerc1 = percentages[p];
                }
            }
        }

        return (bestPerc > 0) ? bestPerc : bestPerc1;
    }

    /**
     * @param traces   LogFileTraces instance
     * @param traceNames only print the given trace,
     * @return an array og analyses of the statistics in a log file.
     * @throws java.io.IOException if general error reading file
     * @throws test.beast.beast2vs1.trace.TraceException      if trace file in wrong format or corrupted
     */
    public static void shortReport(LogFileTraces traces, String[] traceNames) throws java.io.IOException, TraceException {
        System.out.print(traces.getName() + "\t" + traces.getMaxState() + "\t" + traces.getBurnIn());

        for (String traceName : traceNames) {
//            if (traceName != null && traceName.equalsIgnoreCase(traces.getTraceName(i))) {
            int traceIndex = traces.getTraceIndex(traceName);
            TraceStatistics distribution = traces.analyseTrace(traceIndex);

//            System.out.print("\t" + traceName);

            System.out.print("\t" + distribution.getMean());
//            System.out.print("\t" + distribution.getStdev());

//            System.out.print("\t" + distribution.getHpdLower());
//            System.out.print("\t" + distribution.getHpdUpper());

//                System.out.print("\t" + formatter.format(distribution.getStdErrorOfMean()));
//                System.out.print("\t" + formatter.format(distribution.getMinimum()));
//                System.out.print("\t" + formatter.format(distribution.getMaximum()));
//                System.out.print("\t" + formatter.format(distribution.getMedian()));
//                System.out.print("\t" + formatter.format(distribution.getGeometricMean()));
//                System.out.print("\t" + formatter.format(distribution.getVariance()));
//                System.out.print("\t" + formatter.format(distribution.getCpdLower()));
//                System.out.print("\t" + formatter.format(distribution.getCpdUpper()));
            System.out.print("\t" + distribution.getAutoCorrelationTime());
//                System.out.print("\t" + formatter.format(distribution.getStdevAutoCorrelationTime()));

            double ess = distribution.getESS();
            System.out.print("\t" + distribution.getESS());
//            if (ess < 100) System.out.print("\t" + "*");
//            System.out.println();
        }
        System.out.println();
    }

    public static void shortReportHeader(String[] traceNames) {
        System.out.print("file.name\tchain.length\tburnin");
        for (String traceName : traceNames) {
            System.out.print("\tmean." + traceName + "\tact." + traceName + "\tess." + traceName);
        }
        System.out.println();
    }

    //Main method
    public static void main(final String[] args) throws IOException, TraceException {
        Path workPath = Paths.get(System.getProperty("user.home") + "/Projects/BEAST2/PerfAccuStarBEAST/sp5-2/");
        System.out.println("\nWorking path = " + workPath);

        String[] traceNames = new String[]{"posterior", "TreeHeight.Species"}; //"posterior", "TreeHeight.Species"
//        int burnIN = -1;
        int[] trees = new int[] {256}; //2,4,8,16,32,64,128,256
        boolean isCombined = true; // "true" analyses combined logs, "false" analyses an individual resumed/initial log
        int replicates = 100;

        if (!isCombined) {
            // read non-combined beast log, and std output file for time
            for (int tree : trees) {
                String folderName = Integer.toString(tree) + "-resume8";
                System.out.println("Logs folder = " + folderName);
                for (int r=0; r<replicates; r++) {
//                System.out.println("\nGo to folder " + tree + "/" + r + " ...");
                    Path folder = Paths.get(workPath.toString(), folderName, Integer.toString(r));
                    File[] listOfFiles = folder.toFile().listFiles();
                    assert listOfFiles != null;

                    int log = 0;
                    for (File file : listOfFiles) {
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

        // read combined beast log
        for (int tree : trees) {
            System.out.print("folder\treplicate\t");
            shortReportHeader(traceNames);
            for (int r=0; r<replicates; r++) {
                String folderName = isCombined ? Integer.toString(tree) + "-combined" : Integer.toString(tree);
                System.out.print(folderName + "\t" + r + "\t");
                Path folder = Paths.get(workPath.toString(), folderName, Integer.toString(r));
                File[] listOfFiles = folder.toFile().listFiles();
                assert listOfFiles != null;

                for (File file : listOfFiles) {
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
