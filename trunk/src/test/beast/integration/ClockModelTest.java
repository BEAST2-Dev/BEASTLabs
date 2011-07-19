package test.beast.integration;

import beast.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class ClockModelTest extends TestFramework {

    private final String[] XML_FILES = new String[]{"testStrictClock.xml", "testStrictClock2.xml",
            "testRandomLocalClock.xml", "testUCRelaxedClockLogNormal.xml"};

    public void testStrictClock() throws Exception {
        analyse(0);
    }

    public void testStrictClock2() throws Exception {
        analyse(1);
    }

    public void testRandomLocalClock() throws Exception {
        analyse(2);
    }

    public void testUCRelaxedClockLogNormal() throws Exception {
        analyse(3);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp(XML_FILES);
    }

    protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST 1.7
        switch (index_XML) {
            case 0: // testStrictClock.xml
//        BEAST 1 testStrictClockNoDate.xml
                addExpIntoList(expList, "posterior", -1812.939, 0.0581);
                addExpIntoList(expList, "prior", 3.752, 0.0205);
                addExpIntoList(expList, "tree.height", 6.32E-02, 6.76E-05);
                addExpIntoList(expList, "popSize", 9.67E-02, 5.99E-04);
                addExpIntoList(expList, "hky.kappa", 25.807, 0.1812);
                addExpIntoList(expList, "hky.frequencies0", 0.327, 6.15E-04);
                addExpIntoList(expList, "hky.frequencies1", 0.258, 6.09E-04);
                addExpIntoList(expList, "hky.frequencies2", 0.155, 3.88E-04);
                addExpIntoList(expList, "hky.frequencies3", 0.261, 5.17E-04);
                addExpIntoList(expList, "clockRate", 1.0, 0.0);
                addExpIntoList(expList, "treeLikelihood", -1816.691, 0.0522);
                addExpIntoList(expList, "coalescent", 7.24, 9.58E-03);
                break;

            case 1: // testStrictClock2.xml
//        BEAST 1 testStrictClockNoDate2.xml
                addExpIntoList(expList, "posterior", -1811.898, 2.89E-02);
                addExpIntoList(expList, "prior", 3.721, 2.34E-02);
                addExpIntoList(expList, "tree.height", 6.29E-02, 6.43E-05);
                addExpIntoList(expList, "popSize", 9.76E-02, 6.68E-04);
                addExpIntoList(expList, "hky.kappa", 26.491, 0.2089);
                addExpIntoList(expList, "clockRate", 1.0, 0.0);
                addExpIntoList(expList, "treeLikelihood", -1815.619, 2.20E-02);
                addExpIntoList(expList, "coalescent", 7.276, 9.55E-03);
                break;

            case 2: // testRandomLocalClock.xml
                addExpIntoList(expList, "posterior", -1821.0538, 0.1647);
                addExpIntoList(expList, "prior", -4.4935, 0.1553);
                addExpIntoList(expList, "tree.height", 6.4088E-2, 1.4663E-4);
                addExpIntoList(expList, "popSize", 9.6541E-2, 6.6609E-4);
                addExpIntoList(expList, "hky.kappa", 26.544, 0.2648);
                addExpIntoList(expList, "hky.frequencies0", 0.3253, 7.3002E-4);
                addExpIntoList(expList, "hky.frequencies1", 0.258, 5.5405E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.1546, 4.6881E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.262, 6.1501E-4);
                addExpIntoList(expList, "treeLikelihood", -1816.5603, 5.5936E-2);
                addExpIntoList(expList, "coalescent", 7.2815, 1.3472E-2);
                break;

            case 3: // testUCRelaxedClockLogNormal.xml
                addExpIntoList(expList, "posterior", -1812.2012, 6.7606E-2);
                addExpIntoList(expList, "prior", 4.2652, 2.7578E-2);
                addExpIntoList(expList, "treeLikelihood", -1816.4663, 6.4309E-2);
                addExpIntoList(expList, "tree.height", 6.4934E-2, 2.2071E-4);
                addExpIntoList(expList, "popSize", 9.7006E-2, 6.134E-4);
                addExpIntoList(expList, "hky.kappa", 26.5197, 0.2253);
                addExpIntoList(expList, "hky.frequencies0", 0.3262, 6.6979E-4);
                addExpIntoList(expList, "hky.frequencies1", 0.2577, 6.4235E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.1549, 5.0095E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.2611, 5.7794E-4);
                addExpIntoList(expList, "S", 0.1818, 2.8047E-3);
                addExpIntoList(expList, "coalescent", 7.2662, 1.2231E-2);
//                addExpIntoList(expList, "rateCategories1", 0.1818, 2.8047E-3);
//                addExpIntoList(expList, "rateCategories2", 0.1818, 2.8047E-3);
//                addExpIntoList(expList, "rateCategories3", 0.1818, 2.8047E-3);
//                addExpIntoList(expList, "rateCategories4", 0.1818, 2.8047E-3);
//                addExpIntoList(expList, "rateCategories5", 0.1818, 2.8047E-3);
//                addExpIntoList(expList, "rateCategories6", 0.1818, 2.8047E-3);
//                addExpIntoList(expList, "rateCategories7", 0.1818, 2.8047E-3);
//                addExpIntoList(expList, "rateCategories8", 0.1818, 2.8047E-3);
                break;
//mean	0.1818	-1816.4663	7.2662	4.7099	3.9852	5.3233	4.7279	4.9415	5.2705	4.1057	4.0044	4.2019	4.051
//st	2.8047E-3	6.4309E-2	1.2231E-2	2.9611E-2	3.0658E-2	3.179E-2	3.0321E-2	3.5701E-2	2.9287E-2	3.1264E-2	3.257E-2	3.0458E-2	3.0499E-2

            default:
                throw new Exception("No such XML");
        }

        return expList;
    }


} // class ResumeTest