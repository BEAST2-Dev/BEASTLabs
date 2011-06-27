package test.beast.integration;

import beast.core.Logger;
import beast.trace.Expectation;
import beast.trace.LogAnalyser;
import beast.util.Randomizer;
import beast.util.XMLParser;
import junit.framework.TestCase;

import java.util.List;

public abstract class TestFramework {
    protected static long SEED = 127;

    protected abstract List<Expectation> giveExpectations(int index_XML) throws Exception;

    protected abstract void analyseXMLsAndLogs() throws Exception;
    
    protected void analyseXMLsAndLogs(String[] xmls) throws Exception {
        for (int i = 0; i < xmls.length; i++) {
            if (giveExpectations(i).size() > 0) {
                Randomizer.setSeed(SEED);
                Logger.FILE_MODE = Logger.FILE_OVERWRITE;
                String sDir = System.getProperty("user.dir");

                String sFileName = sDir + "/examples/beast2vsbeast1/" + xmls[i];

                System.out.println("Processing " + sFileName);
                XMLParser parser = new XMLParser();
                beast.core.Runnable runable = parser.parseFile(sFileName);
                runable.setStateFile("tmp.state", false);
//		   runable.setInputValue("preBurnin", 0);
//		   runable.setInputValue("chainLength", 1000);
                runable.run();

                String logFile = sDir + "/test." + SEED + ".log";
                System.out.println("\nAnalysing log " + logFile);
                LogAnalyser logAnalyser = new LogAnalyser(logFile, giveExpectations(i)); // burnIn = 0.1 * maxState

                for (Expectation expectation : logAnalyser.m_pExpectations.get()) {
                    TestCase.assertTrue(xmls[i] + ": Expected " + expectation.traceName.get() + " delta mean: "
                            + expectation.expValue.get() + " - " + expectation.getTraceStatistics().getMean()
                            + " <= delta stdErr: 2*(" + expectation.getStdError() + " + "
                            + expectation.getTraceStatistics().getStdErrorOfMean() + ")", expectation.isPassed());

                    TestCase.assertTrue(xmls[i] + ":  has very low effective sample sizes (ESS) "
                            + expectation.getTraceStatistics().getESS(), expectation.isValid());
                }

                System.out.println("\nSucceed " + sFileName);
                System.out.println("\n***************************************\n");
            }
        }
    }

    protected void addExpIntoList(List<Expectation> expList, String traceName, Double expValue, Double stdError) throws Exception {
        Expectation exp = new Expectation(traceName, expValue, stdError);
        expList.add(exp);
    }

}