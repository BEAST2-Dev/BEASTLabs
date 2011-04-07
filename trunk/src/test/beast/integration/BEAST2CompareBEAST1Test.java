package test.beast.integration;

import beast.core.Logger;
import beast.core.Runnable;
import beast.trace.Expectation;
import beast.trace.LogAnalyser;
import beast.util.Randomizer;
import beast.util.XMLParser;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * check that a chain can be resumed after termination *
 */
public class BEAST2CompareBEAST1Test extends TestCase {
    final static long SEED = 127;

    final static String XML_FILE = "testHKY.xml";

    @Test
    public void testBESAT2XmlToCompareBEAST1Result() throws Exception {
        Randomizer.setSeed(SEED);
        Logger.FILE_MODE = Logger.FILE_OVERWRITE;
        String sDir = System.getProperty("user.dir");
        String sFileName = sDir + "/examples/" + XML_FILE;

        System.out.println("Processing " + sFileName);
        XMLParser parser = new XMLParser();
        Runnable runable = parser.parseFile(sFileName);
        runable.setStateFile("tmp.state", false);
//		   runable.setInputValue("preBurnin", 0);
//		   runable.setInputValue("chainLength", 1000);
        runable.run();

//        BEAST 1 testMCMC.xml
//        <expectation name="likelihood" value="-1815.75"/>
//        <expectation name="treeModel.rootHeight" value="0.0642048"/>
//        <expectation name="hky.kappa" value="32.8941"/>
        
        List<Expectation> expList = new ArrayList<Expectation>();
        Expectation exp = new Expectation("treeLikelihood", -1815.606359635542);
        expList.add(exp);
        exp = new Expectation("hky.kappa", 32.63737656542031);
        expList.add(exp);
        exp = new Expectation("tree.height", 0.06419215275960465);
        expList.add(exp);

        String logFile = sDir + "/test." + SEED + ".log";
        System.out.println("Analysing log " + logFile);
        LogAnalyser logAnalyser = new LogAnalyser(logFile, expList);

        for (Expectation expectation : logAnalyser.m_pExpectations.get()) {
            assertTrue("Expected " + expectation.m_sTraceName.get() + " is " + expectation.m_fExpValue.get() + " but got "
                    + expectation.getTraceStatistics().getMean() + " +/- " + expectation.getStdError(), !expectation.isFailed());
        }

        System.out.println("Done " + sFileName);
    } // testBESAT2XmlToCompareBEAST1Result

} // class ResumeTest