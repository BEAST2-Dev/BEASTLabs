package test.beast.evolution.likelihood;



import java.util.concurrent.Executors;

import junit.framework.TestCase;
import org.junit.Test;

import beast.base.core.ProgramStatus;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.likelihood.ThreadedTreeLikelihood;
import beast.base.evolution.likelihood.TreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.Blosum62;
import beast.base.evolution.substitutionmodel.CPREV;
import beast.base.evolution.substitutionmodel.Dayhoff;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.base.evolution.substitutionmodel.HKY;
import beast.base.evolution.substitutionmodel.JTT;
import beast.base.evolution.substitutionmodel.MTREV;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.substitutionmodel.WAG;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beastlabs.evolution.likelihood.MultiPartitionTreeLikelihood;
import test.beast.BEASTTestCase;

/** This test mimics the testLikelihood.xml file from Beast 1, which compares Beast 1 results to PAUP results. 
 * So, it these tests succeed, then Beast II calculates the same for these simple models as Beast 1 and PAUP.
 * **/
public class MultiParititionTreeLikelihoodTest extends TestCase {

    public MultiParititionTreeLikelihoodTest() {
		super();
		ProgramStatus.m_nThreads = 2;
		ProgramStatus.g_exec = Executors.newFixedThreadPool(ProgramStatus.m_nThreads);
	}
	
    @Override
    protected void finalize() throws Throwable {
    	ProgramStatus.g_exec.shutdown();
    	ProgramStatus.g_exec.shutdownNow();    	
    	super.finalize();
    }
    
	protected TreeLikelihood newTreeLikelihood() {
		TreeLikelihood tl = new TreeLikelihood();
		return tl;
	}

	
    static public Tree getTree(Alignment data, String tree) throws Exception {
        TreeParser t = new TreeParser();
        t.initByName("taxa", data,
                "newick", tree,
                "IsLabelledNewick", true);
        return t;
    }

    static public Alignment getAlignment() throws Exception {
//        Sequence human = new Sequence("human", "AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGGAGCTTAAACCCCCTTATTTCTACTAGGACTATGAGAATCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAATACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTG-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGACCAATGGGACTTAAACCCACAAACACTTAGTTAACAGCTAAGCACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCGGAGCTTGGTAAAAAGAGGCCTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGGCCTCCATGACTTTTTCAAAAGGTATTAGAAAAACCATTTCATAACTTTGTCAAAGTTAAATTATAGGCT-AAATCCTATATATCTTA-CACTGTAAAGCTAACTTAGCATTAACCTTTTAAGTTAAAGATTAAGAGAACCAACACCTCTTTACAGTGA");
//        Sequence chimp = new Sequence("chimp", "AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTCAAATCCCCTTATTTCTACTAGGACTATAAGAATCGAACTCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTAAGCACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCTCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AACCCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCGACACCTCTTTACAGTGA");
//        Sequence bonobo = new Sequence("bonobo", "AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTTAAATCCCCTTATTTCTACTAGGACTATGAGAGTCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAACACAGACCAAGAGCCTTCAAAGCTCTCAGTAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAGC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTTGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCCCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCAACACCTCTTTACAGTGA");

        Sequence human = new Sequence("human", "AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGGAGCTTAAACCCCCTTATTTCTACTAGGACTATGAGAATCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAATACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTG-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGACCAATGGGACTTAAACCCACAAACACTTAGTTAACAGCTAAGCACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCGGAGCTTGGTAAAAAGAGGCCTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGGCCTCCATGACTTTTTCAAAAGGTATTAGAAAAACCATTTCATAACTTTGTCAAAGTTAAATTATAGGCT-AAATCCTATATATCTTA-CACTGTAAAGCTAACTTAGCATTAACCTTTTAAGTTAAAGATTAAGAGAACCAACACCTCTTTACAGTGA");
        Sequence chimp = new Sequence("chimp", "AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTCAAATCCCCTTATTTCTACTAGGACTATAAGAATCGAACTCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTAAGCACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCTCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AACCCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCGACACCTCTTTACAGTGA");
        Sequence gorilla = new Sequence("gorilla", "AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGAGGTTTAAACCCCCTTATTTCTACTAGGACTATGAGAATTGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTGTCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTCACATCCTTCCCGTACTAAGAAATTTAGGTTAAACATAGACCAAGAGCCTTCAAAGCCCTTAGTAAGTTA-CAACACTTAATTTCTGTAAGGACTGCAAAACCCTACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATCAATGGGACTCAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAGTCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAT-TCACCTCGGAGCTTGGTAAAAAGAGGCCCAGCCTCTGTCTTTAGATTTACAGTCCAATGCCTTA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGACCTTCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAGGTTAAATTACGGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCGTTAACCTTTTAAGTTAAAGATTAAGAGTATCGGCACCTCTTTGCAGTGA");

        Alignment data = new Alignment();
//        data.initByName("sequence", human, "sequence", chimp, "sequence", bonobo, 
//                "dataType", "nucleotide"
//        );
        data.initByName("sequence", human, "sequence", chimp, "sequence", gorilla, 
                "dataType", "nucleotide"
        );
        return data;
    }

    static public Tree getTree(Alignment data) throws Exception {
        TreeParser tree = new TreeParser();
        tree.initByName("taxa", data,
                "newick", "((human:0.1,chimp:0.1):0.1,gorilla:0.2);",
//                "newick", "(human:0.024003,chimp:0.024003);",
                "IsLabelledNewick", true);
        tree.setID("Tree.t:tree");
        return tree;
    }

    @Test
	public void testJC69Likelihood() throws Exception {
    	// for (int i = 0; i < 1; i++) {
		// Set up JC69 model: uniform freqs, kappa = 1, 0 gamma categories
    	
		Alignment data = BEASTTestCase.getAlignment();
		Tree tree = BEASTTestCase.getTree(data);
//        Alignment data = getAlignment();
//        Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data, 
						 "estimate", false);

		HKY jc = new HKY();
		jc.initByName("kappa", "1.0", 
				       "frequencies",freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", jc, "shape", "0.5");

		TreeLikelihood likelihood = newTreeLikelihood();
		//System.setProperty("java.only", "true");
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
	
		HKY hky = new HKY();
		hky.initByName("kappa", "29.739445", "frequencies", freqs);

		SiteModel siteModel2 = new SiteModel();
		siteModel2.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", hky, "shape", "0.5");

		TreeLikelihood likelihood2 = newTreeLikelihood();
		likelihood2.initByName("data",data, "tree",tree, "siteModel", siteModel2);

		
		MultiPartitionTreeLikelihood tl = new MultiPartitionTreeLikelihood();
		tl.initByName("distribution", likelihood, "distribution", likelihood2);
		
		double fLogP = 0;
		fLogP = tl.calculateLogP();
		assertEquals(fLogP, -1856.3418881275286-1992.2056440317247, BEASTTestCase.PRECISION);

 
		likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
		likelihood2.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel2);
		tl = new MultiPartitionTreeLikelihood();
		tl.initByName("distribution", likelihood, "distribution", likelihood2);
		
		fLogP = tl.calculateLogP();
		assertEquals(fLogP, -1856.3418881275286-1992.2056440317247, BEASTTestCase.PRECISION);
    	// }
	}

//    @Test
//	public void testProportionsOfLikelihood() throws Exception {
//		// 2 threads
//		Alignment data = BEASTTestCase.getAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data, 
//						 "estimate", false);
//
//		HKY hky = new HKY();
//		hky.initByName("kappa", "1.0", 
//				       "frequencies",freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", hky);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel, "proportions", "1 2");
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1992.2056440317247, BEASTTestCase.PRECISION);
// 
//		likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1992.2056440317247, BEASTTestCase.PRECISION);
//
//		// 3 threads
//		ProgramStatus.m_nThreads = 3;
//		ProgramStatus.g_exec = Executors.newFixedThreadPool(ProgramStatus.m_nThreads);
//    
//		likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel, "proportions", "1 2");
//		fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1992.2056440317247, BEASTTestCase.PRECISION);
//
//		// 5 threads
//		ProgramStatus.m_nThreads = 5;
//		ProgramStatus.g_exec = Executors.newFixedThreadPool(ProgramStatus.m_nThreads);
//    
//		likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel, "proportions", "1 2");
//		fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1992.2056440317247, BEASTTestCase.PRECISION);
//
//		// restore to 2 threads
//		ProgramStatus.m_nThreads = 2;
//		ProgramStatus.g_exec = Executors.newFixedThreadPool(ProgramStatus.m_nThreads);
//}
//
//    @Test
//	public void testAscertainedJC69Likelihood() throws Exception {
//		// as testJC69Likelihood but with ascertained alignment	
//		Alignment data = BEASTTestCase.getAscertainedAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data, 
//				 "estimate", false);
//
//		HKY hky = new HKY();
//		hky.initByName("kappa", "1.0", "frequencies", freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", hky);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		// the following number comes from Beast 1.6
//		assertEquals(fLogP, -737.7140695360017, BEASTTestCase.PRECISION);
//	}
//	
//	@Test
//	public void testK80Likelihood() throws Exception {
//		// Set up K80 model: uniform freqs, kappa = 27.402591, 0 gamma categories	
//		Alignment data = BEASTTestCase.getAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data, 
//				 "estimate", false);
//
//		HKY hky = new HKY();
//		hky.initByName("kappa", "27.40259", "frequencies", freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", hky);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1856.303048876734, BEASTTestCase.PRECISION);
//
//		likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1856.303048876734, BEASTTestCase.PRECISION);
//	}
//	
//	@Test
//	public void testHKY85Likelihood() throws Exception {
//		// Set up HKY85 model: estimated freqs, kappa = 29.739445, 0 gamma categories	
//		Alignment data = BEASTTestCase.getAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data); 
//
//		HKY hky = new HKY();
//		hky.initByName("kappa", "29.739445", "frequencies", freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", hky);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1825.2131708068507, BEASTTestCase.PRECISION);
//	
//		likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1825.2131708068507, BEASTTestCase.PRECISION);
//	}
//	
//	
//	@Test
//	public void testHKY85GLikelihood() throws Exception {
//		// Set up HKY85+G model: estimated freqs, kappa = 38.82974, 4 gamma categories, shape = 0.137064	
//		Alignment data = BEASTTestCase.getAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data); 
//
//		HKY hky = new HKY();
//		hky.initByName("kappa", "38.82974", "frequencies", freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4,
//				"shape", "0.137064", 
//				"proportionInvariant", "0.0",
//				"substModel", hky);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		System.err.println(fLogP - -1789.7593576610134);
//		assertEquals(fLogP, -1789.7593576610134, BEASTTestCase.PRECISION);
//	
//		likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1789.7593576610134, BEASTTestCase.PRECISION);
//	}
//
//	@Test
//	public void testHKY85ILikelihood() throws Exception {
//		// Set up HKY85+I model: estimated freqs, kappa = 38.564672, 0 gamma categories, prop invariant = 0.701211	
//		Alignment data = BEASTTestCase.getAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data); 
//
//		HKY hky = new HKY();
//		hky.initByName("kappa", "38.564672", "frequencies", freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1,
//				"shape", "0.137064", 
//				"proportionInvariant", "0.701211",
//				"substModel", hky);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1789.912401996943, BEASTTestCase.PRECISION);
//
//		likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1789.912401996943, BEASTTestCase.PRECISION);
//	}
//
//	@Test
//	public void testHKY85GILikelihood() throws Exception {
//		// Set up HKY85+G+I model: estimated freqs, kappa = 39.464538, 4 gamma categories, shape = 0.587649, prop invariant = 0.486548	
//		Alignment data = BEASTTestCase.getAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data); 
//
//		HKY hky = new HKY();
//		hky.initByName("kappa", "39.464538", "frequencies", freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4,
//				"shape", "0.587649", 
//				"proportionInvariant", "0.486548",
//				"substModel", hky);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1789.639227747059, BEASTTestCase.PRECISION);
//
//		likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1789.639227747059, BEASTTestCase.PRECISION);
//	}
//
//
//	@Test
//	public void testGTRLikelihood() throws Exception {
//		// Set up GTR model: no gamma categories, no proportion invariant 	
//		Alignment data = BEASTTestCase.getAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data); 
//
//		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
//		gsm.initByName("rates", "1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", "frequencies", freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1,
//				"substModel", gsm);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1969.145839307625, BEASTTestCase.PRECISION);
//
//		likelihood.initByName("useAmbiguities", false, "data",data, "tree",tree, "siteModel", siteModel);
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1969.145839307625, BEASTTestCase.PRECISION);
//	}
//
//	@Test
//	public void testGTRILikelihood() throws Exception {
//		// Set up GTR model: prop invariant = 0.5
//		Alignment data = BEASTTestCase.getAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data); 
//
//		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
//		gsm.initByName("rates", "1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", "frequencies", freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1,
//				"proportionInvariant", "0.5",
//				"substModel", gsm);
//		//siteModel.init("1.0", 1, null, "0.5", gsm);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1948.8417455357564, BEASTTestCase.PRECISION);
//
//		likelihood.initByName("useAmbiguities", false, "data",data, "tree",tree, "siteModel", siteModel);
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1948.8417455357564, BEASTTestCase.PRECISION);
//	}
//	
//	@Test
//	public void testGTRGLikelihood() throws Exception {
//		// Set up GTR model: 4 gamma categories, gamma shape = 0.5
//		Alignment data = BEASTTestCase.getAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data); 
//
//		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
//		gsm.initByName("rates", "1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", "frequencies", freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4,
//				"shape", "0.5", 
//				"substModel", gsm);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1949.0360143622, BEASTTestCase.PRECISION);
//
//		likelihood.initByName("useAmbiguities", false, "data",data, "tree",tree, "siteModel", siteModel);
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1949.0360143622, BEASTTestCase.PRECISION);
//	}
//	
//	@Test
//	public void testGTRGILikelihood() throws Exception {
//		// Set up GTR model: 4 gamma categories, gamma shape = 0.5, prop invariant = 0.5
//		Alignment data = BEASTTestCase.getAlignment();
//		Tree tree = BEASTTestCase.getTree(data);
//		
//		Frequencies freqs = new Frequencies();
//		freqs.initByName("data", data); 
//
//		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
//		gsm.initByName("rates", "1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", "frequencies", freqs);
//
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4,
//				"shape", "0.5", 
//				"proportionInvariant", "0.5",
//				"substModel", gsm);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1947.5829396144961, BEASTTestCase.PRECISION);
//
//		likelihood.initByName("useAmbiguities", false, "data",data, "tree",tree, "siteModel", siteModel);
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fLogP, -1947.5829396144961, BEASTTestCase.PRECISION);
//	}
//
//	void aminoacidModelTest(SubstitutionModel substModel, double fExpectedValue) throws Exception {
//		Alignment data = BEASTTestCase.getAminoAcidAlignment();
//		Tree tree = BEASTTestCase.getAminoAcidTree(data);
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", substModel);
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fExpectedValue, fLogP, BEASTTestCase.PRECISION);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodWAG() throws Exception {
//		// Set up WAG model	
//		WAG wag = new WAG();
//		wag.initAndValidate();
//		aminoacidModelTest(wag, -338.6388785157248);
//		
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodJTT() throws Exception {
//		// JTT
//		JTT jtt = new JTT();
//		jtt.initAndValidate();
//		aminoacidModelTest(jtt, -338.80761792179726);
//
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodBlosum62() throws Exception {
//		// Blosum62
//		Blosum62 blosum62 = new Blosum62();
//		blosum62.initAndValidate();
//		aminoacidModelTest(blosum62, -345.3825963600176);
//
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodDayhoff() throws Exception {
//		// Dayhoff
//		Dayhoff dayhoff = new Dayhoff();
//		dayhoff.initAndValidate();
//		aminoacidModelTest(dayhoff, -340.6149187667345);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodcpRev() throws Exception {
//		// cpRev
//		CPREV cpRev = new CPREV();
//		cpRev.initAndValidate();
//		aminoacidModelTest(cpRev, -348.71458467304154);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodMTRev() throws Exception {
//		// MTRev
//		MTREV mtRev = new MTREV();
//		mtRev.initAndValidate();
//		aminoacidModelTest(mtRev, -369.4791633617842);
//		
//	}
//	
//	void aminoacidModelTestI(SubstitutionModel substModel, double fExpectedValue) throws Exception {
//		Alignment data = BEASTTestCase.getAminoAcidAlignment();
//		Tree tree = BEASTTestCase.getAminoAcidTree(data);
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", substModel,
//				"proportionInvariant", "0.2");
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fExpectedValue, fLogP, BEASTTestCase.PRECISION);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodIWAG() throws Exception {
//		// Set up WAG model	
//		WAG wag = new WAG();
//		wag.initAndValidate();
//		aminoacidModelTestI(wag, -338.7631166242887);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodIJTT() throws Exception {
//		// JTT
//		JTT jtt = new JTT();
//		jtt.initAndValidate();
//		aminoacidModelTestI(jtt, -338.97566093453275);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodIBlosum62() throws Exception {
//		// Blosum62
//		Blosum62 blosum62 = new Blosum62();
//		blosum62.initAndValidate();
//		aminoacidModelTestI(blosum62, -345.4456979614507);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodIDayhoff() throws Exception {
//		// Dayhoff
//		Dayhoff dayhoff = new Dayhoff();
//		dayhoff.initAndValidate();
//		aminoacidModelTestI(dayhoff, -340.7630258641759);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodIcpRev() throws Exception {
//		// cpRev
//		CPREV cpRev = new CPREV();
//		cpRev.initAndValidate();
//		aminoacidModelTestI(cpRev, -348.66316715026977);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodIMTRev() throws Exception {
//		// MTRev
//		MTREV mtRev = new MTREV();
//		mtRev.initAndValidate();
//		aminoacidModelTestI(mtRev, -369.34449408200175);
//	
//	}
//
//	void aminoacidModelTestIG(SubstitutionModel substModel, double fExpectedValue) throws Exception {
//		Alignment data = BEASTTestCase.getAminoAcidAlignment();
//		Tree tree = BEASTTestCase.getAminoAcidTree(data);
//		SiteModel siteModel = new SiteModel();
//		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", substModel,
//				"gammaCategoryCount", 4,
//				"shape", "0.15", 
//				"proportionInvariant", "0.2");
//
//		ThreadedTreeLikelihood likelihood = newThreadedTreeLikelihood();
//		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
//		double fLogP = 0;
//		fLogP = likelihood.calculateLogP();
//		assertEquals(fExpectedValue, fLogP, BEASTTestCase.PRECISION);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodGIWAG() throws Exception {
//		// Set up WAG model	
//		WAG wag = new WAG();
//		wag.initAndValidate();
//		aminoacidModelTestIG(wag, -342.69745607208495);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodGIJTT() throws Exception {
//		// JTT
//		JTT jtt = new JTT();
//		jtt.initAndValidate();
//		aminoacidModelTestIG(jtt, -343.23738058653373);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodGIBlosum62() throws Exception {
//		// Blosum62
//		Blosum62 blosum62 = new Blosum62();
//		blosum62.initAndValidate();
//		aminoacidModelTestIG(blosum62, -348.7305212479578);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodGIDayhoff() throws Exception {
//		// Dayhoff
//		Dayhoff dayhoff = new Dayhoff();
//		dayhoff.initAndValidate();
//		aminoacidModelTestIG(dayhoff, -345.11861069556966);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodGIcpRev() throws Exception {
//		
//		// cpRev
//		CPREV cpRev = new CPREV();
//		cpRev.initAndValidate();
//		aminoacidModelTestIG(cpRev, -351.35553855806137);
//	}
//
//	@Test
//	public void testAminoAcidLikelihoodGIMTRev() throws Exception {
//		// MTRev
//		MTREV mtRev = new MTREV();
//		mtRev.initAndValidate();
//		aminoacidModelTestIG(mtRev, -371.0038574147396);
//	
//	}
} // class MultiParititionTreeLikelihoodTest
