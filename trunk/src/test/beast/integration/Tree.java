package test.beast.integration;

import beast.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class Tree extends TestFramework {

    private final String[] XML_FILES = new String[]{"testCalibration.xml"}; //, ".xml"};

    protected void analyseXMLsAndLogs() throws Exception {
        super.analyseXMLsAndLogs(XML_FILES);
    }

    protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST 1.7
        switch (index_XML) {
            case 0: // testCalibration.xml
//        BEAST 1 testCalibration.xml
                addExpIntoList(expList, "posterior", -1884.6966, 6.3796E-2);
                addExpIntoList(expList, "prior", -68.0023, 2.114E-2);
                addExpIntoList(expList, "tree.height", 6.3129E-2, 6.5853E-5);
                addExpIntoList(expList, "mrcatime(human,chimp)", 2.0326E-2, 3.5906E-5);
                addExpIntoList(expList, "popSize", 9.7862E-2, 6.2387E-4);
                addExpIntoList(expList, "hky.kappa", 25.8288, 0.1962);
                addExpIntoList(expList, "hky.frequencies0", 0.3262, 5.9501E-4);
                addExpIntoList(expList, "hky.frequencies1", 0.2569, 5.0647E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.1552, 4.4638E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.2617, 5.1085E-4);
                addExpIntoList(expList, "treeLikelihood", -1816.6943, 5.8444E-2);
                addExpIntoList(expList, "coalescent", 7.2378, 9.1912E-3);
                break;

            default:
                throw new Exception("No such XML");
        }

        return expList;
    }

} // class ResumeTest