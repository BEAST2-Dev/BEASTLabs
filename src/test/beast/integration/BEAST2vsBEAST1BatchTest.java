package test.beast.integration;

import junit.framework.TestCase;
import org.junit.Test;

/**
 * @author Walter Xie
 * test whehter BEAST 2 gives reasonably same results as BEAST 1 by given same XML
 */
public class BEAST2vsBEAST1BatchTest extends TestCase {

    @Test
    public void testSubstitutionModels() throws Exception {
        SubstitutionModel test = new SubstitutionModel();
        test.analyseXMLsAndLogs();
    }

    @Test
    public void testTreePriors() throws Exception {
        TreePrior test = new TreePrior();
        test.analyseXMLsAndLogs();
    }

    @Test
    public void testTree() throws Exception {
        Tree test = new Tree();
        test.analyseXMLsAndLogs();
    }

    @Test
    public void testClockModels() throws Exception {
        ClockModel test = new ClockModel();
        test.analyseXMLsAndLogs();
    }

} // class ResumeTest