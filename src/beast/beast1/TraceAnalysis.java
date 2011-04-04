/*
 * TraceAnalysis.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beast.beast1;

import java.io.File;

/**
 * @author Alexei Drummond
 * @author Walter Xie
 */
public class TraceAnalysis {




    public static LogFileTraces report(String fileName, int inBurnin) throws java.io.IOException, TraceException {
        return report(fileName, inBurnin, null, true);
    }

    public static LogFileTraces report(String fileName, int inBurnin, boolean withStdError) throws java.io.IOException, TraceException {
        return report(fileName, inBurnin, null, withStdError);
    }

    public static LogFileTraces report(String fileName, int inBurnin, String likelihoodName, boolean withStdError)
            throws java.io.IOException, TraceException {

        int fieldWidth = 14;
        int firstField = 25;
        NumberFormatter formatter = new NumberFormatter(6);
        formatter.setPadding(true);
        formatter.setFieldWidth(fieldWidth);

        File file = new File(fileName);

        LogFileTraces traces = new LogFileTraces(fileName, file);
//        if (traces == null) {
//            throw new TraceException("Trace file is empty.");
//        }
        traces.loadTraces();

        int burnin = inBurnin;
        if (burnin == -1) {
            burnin = traces.getMaxState() / 10;
        }

        traces.setBurnIn(burnin);

        System.out.println();
        System.out.println("burnIn   <= " + burnin);
        System.out.println("maxState  = " + traces.getMaxState());
        System.out.println();

        System.out.print(formatter.formatToFieldWidth("statistic", firstField));
        String[] names;

        if (!withStdError)
            names = new String[]{"mean", "hpdLower", "hpdUpper", "ESS"};
        else
            names = new String[]{"mean", "stdErr", "hpdLower", "hpdUpper", "ESS"};

        for (String name : names) {
            System.out.print(formatter.formatToFieldWidth(name, fieldWidth));
        }
        System.out.println();

        int warning = 0;
        for (int i = 0; i < traces.getTraceCount(); i++) {
            
            TraceStatistics distribution = traces.analyseTrace(i);

            double ess = distribution.getESS();
            System.out.print(formatter.formatToFieldWidth(traces.getTraceName(i), firstField));
            System.out.print(formatter.format(distribution.getMean()));

            if (withStdError)
                System.out.print(formatter.format(distribution.getStdError()));

            System.out.print(formatter.format(distribution.getHpdLower()));
            System.out.print(formatter.format(distribution.getHpdUpper()));
            System.out.print(formatter.format(ess));
            if (ess < 100) {
                warning += 1;
                System.out.println("*");
            } else {
                System.out.println();
            }
        }
        System.out.println();

        if (warning > 0) {
            System.out.println(" * WARNING: The results of this MCMC analysis may be invalid as ");
            System.out.println("            one or more statistics had very low effective sample sizes (ESS)");
        }

        if (likelihoodName != null) {
//            System.out.println();
//            int traceIndex = -1;
//            for (int i = 0; i < traces.getTraceCount(); i++) {
//                String traceName = traces.getTraceName(i);
//                if (traceName.equals(likelihoodName)) {
//                    traceIndex = i;
//                    break;
//                }
//            }
//
//            if (traceIndex == -1) {
//                throw new TraceException("Column '" + likelihoodName +
//                        "' can not be found for marginal likelihood analysis.");
//            }
//
//            boolean harmonicOnly = false;
//            int bootstrapLength = 1000;
//
//            List<Double> sample = traces.getValues(traceIndex);
//
//            MarginalLikelihoodAnalysis analysis = new MarginalLikelihoodAnalysis(sample,
//                    traces.getTraceName(traceIndex), burnin, harmonicOnly, bootstrapLength);
//
//            System.out.println(analysis.toString());
        }

        System.out.flush();
        return traces;
    }

    /**
     * @param burnin     the number of states of burnin or if -1 then use 10%
     * @param filename   the file name of the log file to report on
     * @param drawHeader if true then draw header
     * @param stdErr     if true then report the standard deviation of the mean
     * @param hpds       if true then report 95% hpd upper and lower
     * @param individualESSs minimum number of ESS with which to throw warning
     * @param likelihoodName column name
     * @return the traces loaded from given file to create this short report
     * @throws java.io.IOException if general error reading file
     * @throws TraceException      if trace file in wrong format or corrupted

    public static TraceList shortReport(String filename,
                                        final int burnin, boolean drawHeader,
                                        boolean hpds, boolean individualESSs, boolean stdErr,
                                        String likelihoodName) throws java.io.IOException, TraceException {

        TraceList traces = analyzeLogFile(filename, burnin);

        int maxState = traces.getMaxState();

        double minESS = Double.MAX_VALUE;

        if (drawHeader) {
            System.out.print("file\t");
            for (int i = 0; i < traces.getTraceCount(); i++) {
                String traceName = traces.getTraceName(i);
                System.out.print(traceName + "\t");
                if (stdErr)
                    System.out.print(traceName + " stdErr\t");
                if (hpds) {
                    System.out.print(traceName + " hpdLower\t");
                    System.out.print(traceName + " hpdUpper\t");
                }
                if (individualESSs) {
                    System.out.print(traceName + " ESS\t");
                }
            }
            System.out.print("minESS\t");
            if (likelihoodName != null) {
                System.out.print("marginal likelihood\t");
                System.out.print("stdErr\t");
            }
            System.out.println("chainLength");
        }

        System.out.print(filename + "\t");
        for (int i = 0; i < traces.getTraceCount(); i++) {
            //TraceDistribution distribution = traces.getDistributionStatistics(i);
            TraceCorrelation distribution = traces.getCorrelationStatistics(i);
            System.out.print(distribution.getMean() + "\t");
            if (stdErr)
                System.out.print(distribution.getStdErrorOfMean() + "\t");
            if (hpds) {
                System.out.print(distribution.getLowerHPD() + "\t");
                System.out.print(distribution.getUpperHPD() + "\t");
            }
            if (individualESSs) {
                System.out.print(distribution.getESS() + "\t");
            }
            double ess = distribution.getESS();
            if (ess < minESS) {
                minESS = ess;
            }
        }

        System.out.print(minESS + "\t");

        if (likelihoodName != null) {
            int traceIndex = -1;
            for (int i = 0; i < traces.getTraceCount(); i++) {
                String traceName = traces.getTraceName(i);
                if (traceName.equals(likelihoodName)) {
                    traceIndex = i;
                    break;
                }
            }

            if (traceIndex == -1) {
                throw new TraceException("Column '" + likelihoodName + "' can not be found in file " + filename + ".");
            }

            boolean harmonicOnly = false;
            int bootstrapLength = 1000;

            List<Double> sample = traces.getValues(traceIndex);

            MarginalLikelihoodAnalysis analysis = new MarginalLikelihoodAnalysis(sample,
                    traces.getTraceName(traceIndex), burnin, harmonicOnly, bootstrapLength);

            System.out.print(analysis.getLogMarginalLikelihood() + "\t");
            System.out.print(analysis.getBootstrappedSE() + "\t");
        }

        System.out.println(maxState);
        return traces;
    } */

    /**
     * @param fileName the name of the log file to analyze
     * @param burnin   the state to discard up to
     * @return an array og analyses of the statistics in a log file.
     * @throws java.io.IOException if general error reading file
     * @throws TraceException      if trace file in wrong format or corrupted

    public static LogFileTraces analyzeLogFile(String fileName, int burnin) throws java.io.IOException, TraceException {

        File file = new File(fileName);
        LogFileTraces traces = new LogFileTraces(fileName, file);
        traces.loadTraces();
        traces.setBurnIn(burnin);

        for (int i = 0; i < traces.getTraceCount(); i++) {
            traces.analyseTrace(i);
        }
        return traces;
    } */
}
