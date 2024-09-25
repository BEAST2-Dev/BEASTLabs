package test.beast.evolution.likelihood;

import java.io.FileWriter;
import java.util.Arrays;

import org.junit.Test;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.likelihood.TreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.Blosum62;
import beast.base.evolution.substitutionmodel.CPREV;
import beast.base.evolution.substitutionmodel.Dayhoff;
import beast.base.evolution.substitutionmodel.EmpiricalSubstitutionModel;
import beast.base.evolution.substitutionmodel.JTT;
import beast.base.evolution.substitutionmodel.MTREV;
import beast.base.evolution.substitutionmodel.WAG;
import beast.base.evolution.tree.Tree;
import beastlabs.evolution.substitutionmodel.EmpiricalAAModelFromFile;
import junit.framework.TestCase;
import test.beast.BEASTTestCase;

public class EmpiricalModelTest extends TestCase {
	
    @Test
    public void testAminoAcidLikelihoodWAG() throws Exception {
        // Set up WAG model
        WAG wag = new WAG();
        wag.initAndValidate();
        aminoacidModelTest(wag, -338.6388785157248);
    }

    @Test
    public void testAminoAcidLikelihoodJTT() throws Exception {
        // JTT
        JTT jtt = new JTT();
        jtt.initAndValidate();
        aminoacidModelTest(jtt, -338.80761792179726);

    }

    @Test
    public void testAminoAcidLikelihoodBlosum62() throws Exception {
        // Blosum62
        Blosum62 blosum62 = new Blosum62();
        blosum62.initAndValidate();
        aminoacidModelTest(blosum62, -345.3825963600176);

    }

    @Test
    public void testAminoAcidLikelihoodDayhoff() throws Exception {
        // Dayhoff
        Dayhoff dayhoff = new Dayhoff();
        dayhoff.initAndValidate();
        aminoacidModelTest(dayhoff, -340.6149187667345);
    }

    @Test
    public void testAminoAcidLikelihoodcpRev() throws Exception {
        // cpRev
        CPREV cpRev = new CPREV();
        cpRev.initAndValidate();
        aminoacidModelTest(cpRev, -348.71458467304154);
    }

    @Test
    public void testAminoAcidLikelihoodMTRev() throws Exception {
        // MTRev
        MTREV mtRev = new MTREV();
        mtRev.initAndValidate();
        aminoacidModelTest(mtRev, -369.4791633617842);

    }

    void aminoacidModelTest(EmpiricalSubstitutionModel substModel0, double expectedValue) throws Exception {
    	substModel0.setupRelativeRates();

    	double [] freqs = substModel0.getFrequencies();
    	String freqsString = Arrays.toString(freqs);
    	freqsString = freqsString.substring(1, freqsString.length() - 1);
        FileWriter outfile = new FileWriter("/tmp/freqs.csv");
        outfile.write(freqsString);
        outfile.close();

    	double [] rates = substModel0.getRelativeRates();
    	String rateString = Arrays.toString(rates);
    	rateString = rateString.substring(1, rateString.length() - 1);
        outfile = new FileWriter("/tmp/rates.csv");
        outfile.write(rateString);
        outfile.close();
        
    	EmpiricalAAModelFromFile substModel = new EmpiricalAAModelFromFile();
    	substModel.initByName("matrixDir", "/tmp", "rateFile", "rates.csv", "freqFile", "freqs.csv", "encoding", "ACDEFGHIKLMNPQRSTVWY");
    	
        Alignment data = BEASTTestCase.getAminoAcidAlignment();
        Tree tree = BEASTTestCase.getAminoAcidTree(data);
        SiteModel siteModel = new SiteModel();
        siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", substModel);

        TreeLikelihood likelihood = new TreeLikelihood();
        likelihood.initByName("data", data, "tree", tree, "siteModel", siteModel);
        double logP = 0;
        logP = likelihood.calculateLogP();
        assertEquals(expectedValue, logP, BEASTTestCase.PRECISION);
    }


}
