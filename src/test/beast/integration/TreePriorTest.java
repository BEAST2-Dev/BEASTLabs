package test.beast.integration;

import beast.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class TreePriorTest extends TestFramework {
    private final String[] XML_FILES = new String[]{"testCoalescentNoClock.xml", "testCoalescentNoClock1.xml",
            "testExponentialGrowth.xml", "testYuleModel_10taxa.xml", "testBirthDeathModel_10taxa.xml",
            "testBirthDeathAsYule.xml", "testBSP1.xml", "testBSPNoClock.xml", "testEBSP.xml"};

    public void testCoalescentNoClock() throws Exception {
        analyse(0);
    }

    public void testCoalescentNoClock1() throws Exception {
        analyse(1);
    }

    public void testExponentialGrowth() throws Exception {
        analyse(2);
    }

    public void testYuleModel_10taxa() throws Exception {
        analyse(3);
    }

    public void testBirthDeathModel_10taxa() throws Exception {
        analyse(4);
    }

    public void testBirthDeathAsYule() throws Exception {
        analyse(5);
    }

    public void testBSP() throws Exception {
        analyse(6);
    }

    public void testBSPNoClock() throws Exception {
        analyse(7);
    }

    public void testEBSP() throws Exception {
        analyse(8);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp(XML_FILES);
    }

    protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST 1.7
        switch (index_XML) {
            case 0: // testCoalescentNoClock.xml
//        BEAST 1 testCoalescentNoClock.xml
                addExpIntoList(expList, "posterior", -1813.059, 0.0569);
                addExpIntoList(expList, "prior", 3.705, 0.025);
                addExpIntoList(expList, "tree.height", 0.06318, 6.73E-05);
                addExpIntoList(expList, "popSize", 0.0979, 6.38E-04);
                addExpIntoList(expList, "hky.kappa", 26.262, 0.2217);
                addExpIntoList(expList, "hky.frequencies1", 0.326, 6.04E-04);
                addExpIntoList(expList, "hky.frequencies2", 0.258, 5.23E-04);
                addExpIntoList(expList, "hky.frequencies3", 0.155, 4.03E-04);
                addExpIntoList(expList, "hky.frequencies4", 0.261, 5.64E-04);
                addExpIntoList(expList, "treeLikelihood", -1816.764, 0.0556);
                addExpIntoList(expList, "coalescent", 7.242, 9.94E-03);
                break;

            case 1: // testCoalescentNoClock1.xml
//        BEAST 1 testCoalescentNoClock1.xml
                addExpIntoList(expList, "posterior", -1809.75, 5.96E-02);
                addExpIntoList(expList, "tree.height", 6.35E-02, 6.61E-05);
                addExpIntoList(expList, "popSize", 0.132, 0.2987);
                addExpIntoList(expList, "hky.kappa", 33.139, 0.1812);
                addExpIntoList(expList, "hky.frequencies1", 0.326, 5.70E-04);
                addExpIntoList(expList, "hky.frequencies2", 0.257, 6.15E-04);
                addExpIntoList(expList, "hky.frequencies3", 0.154, 4.17E-04);
                addExpIntoList(expList, "hky.frequencies4", 0.262, 4.91E-04);
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
                addExpIntoList(expList, "hky.frequencies1", 0.3266, 5.9862E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.2569, 5.3533E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.154, 4.0839E-4);
                addExpIntoList(expList, "hky.frequencies4", 0.2625, 5.7627E-4);
                addExpIntoList(expList, "treeLikelihood", -1816.7188, 4.9732E-2);
                addExpIntoList(expList, "coalescent", 9.4203, 1.31E-2);
                break;

            case 3: // testYuleModel_10taxa.xml
//        BEAST 1 testYuleModel_10taxa.xml
                addExpIntoList(expList, "yule", -2.8068, 3.894E-2); // speciationLikelihood
                addExpIntoList(expList, "tree.height", 0.9702, 4.2554E-3);
                break;

            case 4: // testBirthDeathModel_10taxa.xml
//        BEAST 1 testBirthDeathModel_10taxa.xml
                addExpIntoList(expList, "birthDeath", 0.981, 4.9536E-2); // speciationLikelihood
                addExpIntoList(expList, "tree.height", 0.7017, 3.9048E-3);
                break;

            case 5: // testBirthDeathAsYule.xml
//        BEAST 1 testBirthDeathAsYule.xml
                addExpIntoList(expList, "posterior", -84.941, 2.3521E-2);
                addExpIntoList(expList, "birthDeath", -84.7945, 2.3471E-2); // speciationLikelihood
                addExpIntoList(expList, "birthRate", 1.1636, 3.8458E-3);
                break;

            case 6: // testBSP1.xml
//        BEAST 1 testBSP1.xml
                addExpIntoList(expList, "posterior", -3849.428, 0.1743);
                addExpIntoList(expList, "prior", -76.253, 0.1631);
                addExpIntoList(expList, "siteModel.alpha", 5.434E-2, 5.0295E-4);
                addExpIntoList(expList, "clock.rate", 8.8911E-4, 2.0136E-6);
                addExpIntoList(expList, "skyline.popSize1", 5623.12, 207.941);
                addExpIntoList(expList, "skyline.popSize2", 1085.8982, 120.6415);
                addExpIntoList(expList, "skyline.popSize3", 2991.2196, 159.7476);
                addExpIntoList(expList, "skyline.popSize4", 3223.5275, 191.6626);
                addExpIntoList(expList, "skyline.popSize5", 1882.3835, 147.9595);
                addExpIntoList(expList, "skyline.groupSize1", 1.8675, 0.1262);
                addExpIntoList(expList, "skyline.groupSize2", 5.0423, 0.1292);
                addExpIntoList(expList, "skyline.groupSize3", 3.0782, 9.7576E-2);
                addExpIntoList(expList, "skyline.groupSize4", 2.7686, 0.1004);
                addExpIntoList(expList, "skyline.groupSize5", 3.2434, 9.905E-2);
                addExpIntoList(expList, "hky.kappa", 21.6604, 0.2725);
                addExpIntoList(expList, "treeModel.rootHeight", 74.2761, 0.1275);
                addExpIntoList(expList, "treeLikelihood", -3773.175, 4.2857E-2);
                addExpIntoList(expList, "skyline", -76.253, 0.1631);
                break;

            case 7: // testBSPNoClock.xml
//        BEAST 1 testBSPNoClock.xml
                addExpIntoList(expList, "posterior", -1826.0627, 0.1605);
                addExpIntoList(expList, "prior", -9.3047, 0.1419);
                addExpIntoList(expList, "tree.height", 6.3543E-2, 6.5911E-5);
//                addExpIntoList(expList, "popSizes1", 2555.892, 136.7415);
//                addExpIntoList(expList, "popSizes2", 1806.3325, 128.1372);
//                addExpIntoList(expList, "popSizes3", 1031.222, 115.9877);
                addExpIntoList(expList, "groupSizes1", 1.3147, 2.2938E-2);
                addExpIntoList(expList, "groupSizes2", 1.5633, 3.2899E-2);
                addExpIntoList(expList, "groupSizes3", 2.122, 4.0359E-2);
                addExpIntoList(expList, "hky.kappa", 26.1996, 0.2017);
                addExpIntoList(expList, "hky.frequencies1", 0.3269, 6.8444E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.2569, 6.0224E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.1547, 4.6258E-4);
                addExpIntoList(expList, "hky.frequencies4", 0.2615, 6.4427E-4);
                addExpIntoList(expList, "treeLikelihood", -1816.758, 7.8396E-2);
                addExpIntoList(expList, "skyline", -3.3184, 0.1425);
                break;

           case 8: // testEBSP.xml
//        BEAST 1 testEBSP.xml
                addExpIntoList(expList, "posterior", -1826.2014, 0.1562);
                addExpIntoList(expList, "prior", -9.4517, 0.1782);
                addExpIntoList(expList, "tree.height", 6.3528E-2, 7.0709E-5);
                addExpIntoList(expList, "popSizes1", 2435.0409, 132.4556);
                addExpIntoList(expList, "popSizes2", 2195.7116, 156.2106);
                addExpIntoList(expList, "popSizes3", 1065.3511, 119.8768);
                addExpIntoList(expList, "indicators", 1.3275, 2.1211E-2);
                addExpIntoList(expList, "indicators", 1.5219, 2.9634E-2);
                addExpIntoList(expList, "indicators", 2.1505, 3.8185E-2);
                addExpIntoList(expList, "hky.kappa", 26.4814, 0.2725);
                addExpIntoList(expList, "hky.frequencies1", 0.3252, 7.7972E-4);
                addExpIntoList(expList, "hky.frequencies2", 0.2581, 5.685E-4);
                addExpIntoList(expList, "hky.frequencies3", 0.1553, 4.3071E-4);
                addExpIntoList(expList, "hky.frequencies4", 0.2614, 6.1733E-4);
                addExpIntoList(expList, "treeLikelihood", -1816.7497, 5.4764E-2);
                addExpIntoList(expList, "populationMean", -3.4472, 0.1802);
                break;


            default:
                throw new Exception("No such XML");
        }

        return expList;
    }


} // class ResumeTest