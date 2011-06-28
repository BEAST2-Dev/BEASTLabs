package test.beast.integration;

import beast.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class ClockModel extends TestFramework {

    private final String[] XML_FILES = new String[]{"testStrictClock.xml", "testStrictClock2.xml"};//, "testRandomLocalClock.xml"};
//            "testUCRelaxedClockLogNormal.xml"};

    protected void analyseXMLsAndLogs() throws Exception {
        super.analyseXMLsAndLogs(XML_FILES);
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

//            case 2: // testUCRelaxedClockLogNormal.xml
//                addExpIntoList(expList, "treeLikelihood", -1815.6833629399468);
//                addExpIntoList(expList, "tree.height", 0.02501244505827084);
//                addExpIntoList(expList, "coalescent", 12.323228234924514);
//                addExpIntoList(expList, "popSize", 0.04698473959788077);
//                addExpIntoList(expList, "hky.kappa", 32.17682957277128);
//                addExpIntoList(expList, "S", 2.611532662341314);
//                addExpIntoList(expList, "rateCategories0", 6.37037037037037);
//                addExpIntoList(expList, "rateCategories1", 5.765432098765432);
//                addExpIntoList(expList, "posterior", -1803.3601347050233);
//                break;

            case 2: // testRandomLocalClock.xml
                addExpIntoList(expList, "posterior", -1821.7238, 0.1538);
                addExpIntoList(expList, "prior", -5.0977, 0.1429);
                addExpIntoList(expList, "tree.height", 6.3713E-2, 1.128E-4);
                addExpIntoList(expList, "popSize", 9.8913E-2, 6.8336E-4);
                addExpIntoList(expList, "hky.kappa", 26.3161, 0.2575);
                addExpIntoList(expList, "hky.frequencies0", 0.326, 8.5172E-4);
                addExpIntoList(expList, "hky.frequencies1", 0.2582, 6.0012E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.1546, 4.6971E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.2613, 6.8585E-4);
                addExpIntoList(expList, "treeLikelihood", -1816.6262, 6.1374E-2);
                addExpIntoList(expList, "coalescent", 7.2642, 1.1661E-2);
                break;

           default:
                throw new Exception("No such XML");
        }

        return expList;
    }


} // class ResumeTest