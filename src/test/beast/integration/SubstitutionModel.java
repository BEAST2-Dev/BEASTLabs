package test.beast.integration;

import beast.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class SubstitutionModel extends TestFramework {

    private final String[] XML_FILES = new String[]{"testHKY.xml"}; //, "testTipDates.xml"};

    protected void analyseXMLsAndLogs() throws Exception {
        super.analyseXMLsAndLogs(XML_FILES);
    }

    protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST 1.7
        switch (index_XML) {
            case 0: // testHKY.xml
//        BEAST 1 testMCMC.xml
//        <expectation name="likelihood" value="-1815.75"/>
//        <expectation name="treeModel.rootHeight" value="0.0642048"/>
//        <expectation name="hky.kappa" value="32.8941"/>
                addExpIntoList(expList, "treeLikelihood", -1815.766, 0.0202);
                addExpIntoList(expList, "tree.height", 6.42E-02, 6.53E-05);
                addExpIntoList(expList, "hky.kappa", 33.019, 0.1157);
                break;

//
//            case 8: // testTipDates.xml
//                addExpIntoList(expList, "treeLikelihood", -15000.435271635279);
//                addExpIntoList(expList, "tree.height", 38.6109296447932);
//                addExpIntoList(expList, "hky.kappa", 2179.425239015365);
//                break;


            default:
                throw new Exception("No such XML");
        }

        return expList;
    }

} // class ResumeTest