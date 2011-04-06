package test.beast.integration;

import beast.core.Logger;
import beast.core.Runnable;
import beast.util.Expectation;
import beast.util.LogAnalyser;
import beast.util.Randomizer;
import beast.util.XMLParser;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * check that a chain can be resumed after termination *
 */
public class IntegrationTest extends TestCase {

    final static String XML_FILE = "testHKYLogAnalysis.xml";

    @Test
    public void test_ThatXmlExamplesRun() throws Exception {
        Randomizer.setSeed(127);
        Logger.FILE_MODE = Logger.FILE_OVERWRITE;
        String sDir = System.getProperty("user.dir") + "/examples";
        String sFileName = sDir + "/" + XML_FILE;

        System.out.println("Processing " + sFileName);
        XMLParser parser = new XMLParser();
        Runnable runable = parser.parseFile(sFileName);
        runable.setStateFile("tmp.state", false);
        runable.run();

        LogAnalyser logAnalyser = null;// = parser
        for (Expectation expectation : logAnalyser.m_pExpectations.get()) {
            assertTrue("Expected " + expectation.m_Name.get() + " is " + expectation.m_ExpValue.get() + " but got "
                    + expectation.getTraceStatistics().getMean() + " +/- " + expectation.getStdError(), !expectation.isFailed());
        }

        System.out.println("Done " + sFileName);
    } // test_ThatXmlExamplesRun

} // class ResumeTest