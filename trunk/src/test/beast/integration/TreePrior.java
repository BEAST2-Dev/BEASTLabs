package test.beast.integration;

import beast.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class TreePrior extends TestFramework {
    private final String[] XML_FILES = new String[]{"testCoalescent.xml", "testCoalescent1.xml",
            "testExponentialGrowth.xml", "testYuleModel_10taxa.xml"};// ,"testBSP.xml"};//, "testBirthDeathModel_10taxa.xml"};
            //, "testBirthDeathAsYule.xml"};

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

            case 2: // testExponentialGrowth.xml
//        BEAST 1 testExponentialGrowthNoClock1.xml
                addExpIntoList(expList, "posterior", -1818.4448, 6.3697E-2);
                addExpIntoList(expList, "prior", -1.726, 3.6124E-2);
                addExpIntoList(expList, "tree.height", 6.1545E-2, 6.6865E-5);
                addExpIntoList(expList, "popSize", 1.0231, 5.1484E-2);
                addExpIntoList(expList, "growthRate", 58.5276, 0.6012);
                addExpIntoList(expList, "hky.kappa", 26.301, 0.211);
                addExpIntoList(expList, "hky.frequencies0", 0.3266, 5.9862E-4);
                addExpIntoList(expList, "hky.frequencies1", 0.2569, 5.3533E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.154, 4.0839E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.2625, 5.7627E-4);
                addExpIntoList(expList, "treeLikelihood", -1816.7188, 4.9732E-2);
                addExpIntoList(expList, "coalescent", 9.4203, 1.31E-2);
                break;

            case 3: // testYuleModel_10taxa.xml
//        BEAST 1 testYuleModel_10taxa.xml
                addExpIntoList(expList, "yule", -2.8068, 3.894E-2); // speciationLikelihood
                addExpIntoList(expList, "tree.height", 0.9702, 4.2554E-3);
                addExpIntoList(expList, "birthRate", 2.0, 0.0);
                break;

            case 4: // testBSP.xml
//        BEAST 1 testBSPNoClock.xml
                addExpIntoList(expList, "posterior", -1826.2014, 0.1562);
                addExpIntoList(expList, "prior", -9.4517, 0.1782);
                addExpIntoList(expList, "tree.height", 6.3528E-2, 7.0709E-5);
                addExpIntoList(expList, "popSizes1", 2435.0409, 132.4556);
                addExpIntoList(expList, "popSizes2", 2195.7116, 156.2106);
                addExpIntoList(expList, "popSizes3", 1065.3511, 119.8768);
                addExpIntoList(expList, "groupSizes1", 1.3275, 2.1211E-2);
                addExpIntoList(expList, "groupSizes2", 1.5219, 2.9634E-2);
                addExpIntoList(expList, "groupSizes3", 2.1505, 3.8185E-2);
                addExpIntoList(expList, "hky.kappa", 26.4814, 0.2725);
                addExpIntoList(expList, "hky.frequencies0", 0.3252, 7.7972E-4);
                addExpIntoList(expList, "hky.frequencies1", 0.2581, 5.685E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.1553, 4.3071E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.2614, 6.1733E-4);
                addExpIntoList(expList, "treeLikelihood", -1816.7497, 5.4764E-2);
                addExpIntoList(expList, "skyline", -3.4472, 0.1802);
                break;


            case 5: // testBirthDeathAsYule.xml
//        BEAST 1 testBirthDeathAsYule.xml
                addExpIntoList(expList, "birthDeath", 1.066, 5.4414E-2); // speciationLikelihood
                addExpIntoList(expList, "tree.height", 0.6957, 4.1537E-3);
                addExpIntoList(expList, "birthRate", 2.0, 0.0);
                addExpIntoList(expList, "relativeDeathRate", 0.0, 0.0);
                addExpIntoList(expList, "sampleProbability", 1.0, 0.0);
                break;

            case 6: // testBirthDeathModel_10taxa.xml
//        BEAST 1 testBirthDeathModel_10taxa.xml
                addExpIntoList(expList, "birthDeath", 1.066, 5.4414E-2); // speciationLikelihood
                addExpIntoList(expList, "tree.height", 0.6957, 4.1537E-3);
                addExpIntoList(expList, "birthRate", 2.0, 0.0);
                addExpIntoList(expList, "relativeDeathRate", 0.5, 0.0);
                break;

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