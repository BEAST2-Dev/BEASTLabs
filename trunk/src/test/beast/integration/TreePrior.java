package test.beast.integration;

import beast.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class TreePrior extends TestFramework {

    private final String[] XML_FILES = new String[]{"testCoalescent.xml", "testCoalescent1.xml"};
    //, "testCoalescentUnit.xml","testExponentialGrowth.xml", "testYuleOneSite.xml"};

    protected void analyseXMLsAndLogs() throws Exception {
        super.analyseXMLsAndLogs(XML_FILES);
    }
    
    protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST 1.7
        switch (index_XML) {
            case 0: // testCoalescent.xml
//        BEAST 1 testCoalescentNoClock.xml
                addExpIntoList(expList, "posterior", -1813.059, 0.0569);
                addExpIntoList(expList, "prior", 3.705, 0.025);
                addExpIntoList(expList, "tree.height", 0.06318, 6.73E-05);
                addExpIntoList(expList, "popSize", 0.0979, 6.38E-04);
                addExpIntoList(expList, "hky.kappa", 26.262, 0.2217);
                addExpIntoList(expList, "hky.frequencies0", 0.326, 6.04E-04);
                addExpIntoList(expList, "hky.frequencies1", 0.258, 5.23E-04);
                addExpIntoList(expList, "hky.frequencies2", 0.155, 4.03E-04);
                addExpIntoList(expList, "hky.frequencies3", 0.261, 5.64E-04);
                addExpIntoList(expList, "treeLikelihood", -1816.764, 0.0556);
                addExpIntoList(expList, "coalescent", 7.242, 9.94E-03);
                break;

            case 1: // testCoalescent1.xml
//        BEAST 1 testCoalescentNoClock1.xml
                addExpIntoList(expList, "posterior", -1809.75, 5.96E-02);
                addExpIntoList(expList, "tree.height", 6.35E-02, 6.61E-05);
                addExpIntoList(expList, "popSize", 0.132, 0.2987);
                addExpIntoList(expList, "hky.kappa", 33.139, 0.1812);
                addExpIntoList(expList, "hky.frequencies0", 0.326, 5.70E-04);
                addExpIntoList(expList, "hky.frequencies1", 0.257, 6.15E-04);
                addExpIntoList(expList, "hky.frequencies2", 0.154, 4.17E-04);
                addExpIntoList(expList, "hky.frequencies3", 0.262, 4.91E-04);
                addExpIntoList(expList, "treeLikelihood", -1816.698, 5.82E-02);
                addExpIntoList(expList, "coalescent", 6.948, 1.24E-02);
                break;

//            case 2: // testExponentialGrowth.xml
////coalescent : -0.4014183147929467 +- 0.00381600199385509, expectation is 6.860816283805018
////popSize : Infinity +- 0.0, expectation is 0.13377527552728488
//                addExpIntoList(expList, "treeLikelihood", -1815.8383981132436);
//                addExpIntoList(expList, "tree.height", 0.06462423202463202);
//                addExpIntoList(expList, "coalescent", -0.4014183147929467);
//                addExpIntoList(expList, "popSize", 0.13377527552728488);
//                addExpIntoList(expList, "hky.kappa", 33.88522193182046);
//                addExpIntoList(expList, "posterior", -1816.2398164280376);
//                break;
//
//            case 9: // testYuleOneSite.xml todo XML not workign
//                addExpIntoList(expList, "treeLikelihood", -1815.498433460167);
//                addExpIntoList(expList, "tree.height", 0.06262400993502577);
//                addExpIntoList(expList, "coalescent", 6.860816283805018);
//                addExpIntoList(expList, "popSize", 0.13377527552728488);
//                addExpIntoList(expList, "hky.kappa", 34.273735441171816);
//                addExpIntoList(expList, "posterior", -1808.6376171763627);
//                break;

//            case 1: // testCoalescentUnit.xml
//                addExpIntoList(expList, "tree.height", 15000.0);
//                addExpIntoList(expList, "popSize", 10000.0);
////                addExpIntoList(expList, "coalescent", -30.6);
////                addExpIntoList(expList, "posterior", -30.6);
//                break;

            default:
                throw new Exception("No such XML");
        }

        return expList;
    }


} // class ResumeTest