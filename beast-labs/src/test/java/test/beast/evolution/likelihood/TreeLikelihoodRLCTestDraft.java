package test.beast.evolution.likelihood;


import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.FilteredAlignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.branchratemodel.RandomLocalClockModel;
import beast.base.evolution.likelihood.TreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.evolution.substitutionmodel.HKY;
import beast.base.evolution.tree.Node;
import beast.base.util.Randomizer;
import beast.base.evolution.tree.TreeParser;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.List;

/**
 * Sharp spikes in posterior trace random local clock
 * https://github.com/CompEvol/beast2/issues/785
 */
public class TreeLikelihoodRLCTestDraft extends TestCase {

    public TreeLikelihoodRLCTestDraft() {
        super();
    }

    protected TreeLikelihood javaTreeLikelihood() {
        System.setProperty("java.only","true");
        return new TreeLikelihood();
    }

    protected TreeLikelihood beagleTreeLikelihood() {
        System.setProperty("java.only","false");
        return new TreeLikelihood();
    }

    private Alignment getAlignment() {
        Sequence seq1 = new Sequence("taxon1", "GTCGGTCAGTCA");
        Sequence seq2 = new Sequence("taxon2", "TCAGTTAGTCAG");
        Sequence seq3 = new Sequence("taxon3", "CAGTCAGTCAGT");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", seq1, "sequence", seq2, "sequence", seq3, "dataType", "nucleotide");
        return alignment;
    }

    private FilteredAlignment getFilteredAlignment() {
        Alignment alignment = getAlignment();

        IntegerParameter constantSiteWeights = new IntegerParameter(new Integer[]{1000000,1000000,1000000,1000000});
        FilteredAlignment filteredAlignment = new FilteredAlignment();
        filteredAlignment.initByName("data", alignment, "filter", "-",
                "constantSiteWeights", constantSiteWeights, "dataType", "nucleotide");
        return filteredAlignment;
    }

    /**
     *
     */
    @Test
    public void testBeagleRLCLikelihood() throws Exception {

//        Alignment alignment = getAlignment();
        Alignment alignment = getFilteredAlignment();

        // The start point is 6780000 having the problem from Tim's XML using seed 5010
//        TreeParser tree = new TreeParser();
        // ((2:1.0812703780105475,1:0.08127037801054748)3:0.957396117780122,0:0.0386664957906695)4:0.0
        String treeSting = "((taxon3:1.0812703780105475,taxon2:0.08127037801054748)3:0.957396117780122,taxon1:0.0386664957906695)4:0.0";
//        tree.initByName("taxa", alignment, "newick", treeSting, "IsLabelledNewick", true); //TODO diff tree in RAM?
        TreeParser tree = new TreeParser(treeSting, false, false, true, 1);

        // subst model
        RealParameter f = new RealParameter(new Double[]{0.25002332020722356,0.249757221213698,0.24989440866005042,0.250325049919028});
        Frequencies freqs = new Frequencies();
        freqs.initByName("frequencies", f, "estimate", false);

        HKY hky = new HKY();
        hky.initByName("kappa", "0.8582028379159838", "frequencies", freqs);

        SiteModel siteModel = new SiteModel();
        siteModel.initByName("gammaCategoryCount", 1, "substModel", hky);

        // RLC
        BooleanParameter indicators = new BooleanParameter(new Boolean[]{false, false, false, false});
        RealParameter clockRates = new RealParameter(new Double[]{3.9950381793702756E-8,2.0619003298766432E-5,3.3259368905282205E-18,1.006998588099822E-12});
        RandomLocalClockModel rLC = new RandomLocalClockModel();
        rLC.initByName("clock.rate", "2.435178917243108E-6", "indicators", indicators,
                "rates", clockRates, "tree", tree);

        // simulate MCMC sampling
        Randomizer.setSeed(5010);

        // number of significant different result > 1e-6
        int numSigDiff = 0;
        double scaleFactor = 0.5;
        final int numSamp = 100000;
        for (int i=0; i<numSamp; i++) {
            System.out.println("sample = " + i);

            TreeLikelihood likelihoodJava = javaTreeLikelihood();
            likelihoodJava.initByName("data", alignment, "tree", tree, "siteModel", siteModel, "branchRateModel", rLC);
            double logLJava = likelihoodJava.calculateLogP();

            System.out.println("Java Tree Likelihood = " + logLJava);

            TreeLikelihood likelihoodBeagle = beagleTreeLikelihood();
            likelihoodBeagle.initByName("data", alignment, "tree", tree, "siteModel", siteModel, "branchRateModel", rLC);
            double logLBeagle = likelihoodBeagle.calculateLogP();

            System.out.println("Beagle Tree Likelihood = " + logLBeagle);

            double absDiff = Math.abs(logLJava - logLBeagle);
            if (absDiff > 1e-6) numSigDiff++;

            System.out.println("abs(Java - Beagle) = " + absDiff + "\n");

            // all nodes
            List<Node> allNodes = tree.getRoot().getAllChildNodesAndSelf();
            System.out.println(tree.getRoot().toNewick());
            for (int n=0; n<allNodes.size(); n++) {
                Node node = allNodes.get(n);
                System.out.println(node.getID() + " (" + node.getNr() + ") : " + node.getHeight());
            }

            // next proposal
//            double scale = scaleFactor + (Randomizer.nextDouble() * ((1.0 / scaleFactor) - scaleFactor));
//            System.out.println("\nscale for next sample = " + scale);

            // scale root height
            Node root = allNodes.get(0);
//            root.setHeight(root.getHeight() * scale);
            // hard code to get random root height [2, 100],
            // where 3.getHeight() = 1.0812703780105475 and 0..getHeight() = 2
            double h = 2 + (100 - 2) * Randomizer.nextDouble();
            root.setHeight(h);
            System.out.println("root height for next sample = " + root.getHeight());

            if (Double.isInfinite(root.getHeight()))
                throw new RuntimeException("");

            System.out.println("\n");
        }

        System.out.println("Find " + numSigDiff + " significant different (> 1e-6) results in " + numSamp + " samples.\n");
    }


} // class TreeLikelihoodTest
