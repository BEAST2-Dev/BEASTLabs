package test.beast.evolution.likelihood;



import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.junit.Test;

import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.likelihood.ExperimentalTreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.evolution.substitutionmodel.HKY;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;



/** This test mimics the testLikelihood.xml file from Beast 1, which compares Beast 1 results to PAUP results. 
 * So, it these tests succeed, then Beast II calculates the same for these simple models as Beast 1 and PAUP.
 * **/
public class ExperimentalTreeLikelihoodTest extends TestCase {
	final static double PRECISION = 1e-6;
	final static boolean g_bUseAmbiguities = false;
	
	public ExperimentalTreeLikelihoodTest() {
		super();
	}
	
	protected ExperimentalTreeLikelihood newTreeLikelihood() {
		return new ExperimentalTreeLikelihood();
	}
	
	
	public static Alignment getAlignment() throws Exception {
		Sequence human = new Sequence("human", "AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGGAGCTTAAACCCCCTTATTTCTACTAGGACTATGAGAATCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAATACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTG-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGACCAATGGGACTTAAACCCACAAACACTTAGTTAACAGCTAAGCACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCGGAGCTTGGTAAAAAGAGGCCTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGGCCTCCATGACTTTTTCAAAAGGTATTAGAAAAACCATTTCATAACTTTGTCAAAGTTAAATTATAGGCT-AAATCCTATATATCTTA-CACTGTAAAGCTAACTTAGCATTAACCTTTTAAGTTAAAGATTAAGAGAACCAACACCTCTTTACAGTGA");
		Sequence chimp = new Sequence("chimp","AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTCAAATCCCCTTATTTCTACTAGGACTATAAGAATCGAACTCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTAAGCACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCTCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AACCCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCGACACCTCTTTACAGTGA");
		Sequence bonobo = new Sequence("bonobo","AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTTAAATCCCCTTATTTCTACTAGGACTATGAGAGTCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAACACAGACCAAGAGCCTTCAAAGCTCTCAGTAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAGC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTTGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCCCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCAACACCTCTTTACAGTGA");
		Sequence gorilla = new Sequence("gorilla","AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGAGGTTTAAACCCCCTTATTTCTACTAGGACTATGAGAATTGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTGTCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTCACATCCTTCCCGTACTAAGAAATTTAGGTTAAACATAGACCAAGAGCCTTCAAAGCCCTTAGTAAGTTA-CAACACTTAATTTCTGTAAGGACTGCAAAACCCTACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATCAATGGGACTCAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAGTCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAT-TCACCTCGGAGCTTGGTAAAAAGAGGCCCAGCCTCTGTCTTTAGATTTACAGTCCAATGCCTTA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGACCTTCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAGGTTAAATTACGGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCGTTAACCTTTTAAGTTAAAGATTAAGAGTATCGGCACCTCTTTGCAGTGA");
		Sequence orangutan = new Sequence("orangutan","AGAAATATGTCTGACAAAAGAGTTACTTTGATAGAGTAAAAAATAGAGGTCTAAATCCCCTTATTTCTACTAGGACTATGGGAATTGAACCCACCCCTGAGAATCCAAAATTCTCCGTGCCACCCATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTA--CACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTCA-CAGCACTTAATTTCTGTAAGGACTGCAAAACCCCACTTTGCATCAACTGAGCGCAAATCAGCCACTTTAATTAAGCTAAGCCCTCCTAGACCGATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAGTCAAT-TGGCTTCAGTCCAAAGCCCCGGCAGGCCTTAAAGCTGCTCCTTCGAATTTGCAATTCAACATGACAA-TCACCTCAGGGCTTGGTAAAAAGAGGTCTGACCCCTGTTCTTAGATTTACAGCCTAATGCCTTAACTCGGCCATTTTACCGCAAAAAAGGAAGGAATCGAACCTCCTAAAGCTGGTTTCAAGCCAACCCCATAACCCCCATGACTTTTTCAAAAGGTACTAGAAAAACCATTTCGTAACTTTGTCAAAGTTAAATTACAGGTC-AGACCCTGTGTATCTTA-CATTGCAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGACTAAGAGAACCAGCCTCTCTTTGCAATGA");
		Sequence siamang = new Sequence("siamang","AGAAATACGTCTGACGAAAGAGTTACTTTGATAGAGTAAATAACAGGGGTTTAAATCCCCTTATTTCTACTAGAACCATAGGAGTCGAACCCATCCTTGAGAATCCAAAACTCTCCGTGCCACCCGTCGCACCCTGTTCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCATACTAAGAAATTTAGGTTAAACACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTAACAAAACTTAATTTCTGCAAGGGCTGCAAAACCCTACTTTGCATCAACCGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATCGATGGGACTTAAACCCATAAAAATTTAGTTAACAGCTAAACACCCTAAACAACCTGGCTTCAATCTAAAGCCCCGGCAGA-GTTGAAGCTGCTTCTTTGAACTTGCAATTCAACGTGAAAAATCACTTCGGAGCTTGGCAAAAAGAGGTTTCACCTCTGTCCTTAGATTTACAGTCTAATGCTTTA-CTCAGCCACTTTACCACAAAAAAGGAAGGAATCGAACCCTCTAAAACCGGTTTCAAGCCAGCCCCATAACCTTTATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATCACAGGTCCAAACCCCGTATATCTTATCACTGTAGAGCTAGACCAGCATTAACCTTTTAAGTTAAAGACTAAGAGAACTACCGCCTCTTTACAGTGA");
		
		Alignment data = new Alignment();
		data.initByName("sequence", human, "sequence", chimp, "sequence", bonobo, "sequence", gorilla, "sequence", orangutan, "sequence", siamang,
						"dataType","nucleotide"
						);
		return data;
	}
	
	Alignment getAscertainedAlignment() throws Exception {
		// same as getAlignment, but with first four sites the constant sites ACTG
		List<Sequence>  sequences = new ArrayList<Sequence>();
		sequences.add(new Sequence("human", "ACTGAGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGGAGCTTAAACCCCCTTATTTCTACTAGGACTATGAGAATCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAATACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTG-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGACCAATGGGACTTAAACCCACAAACACTTAGTTAACAGCTAAGCACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCGGAGCTTGGTAAAAAGAGGCCTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGGCCTCCATGACTTTTTCAAAAGGTATTAGAAAAACCATTTCATAACTTTGTCAAAGTTAAATTATAGGCT-AAATCCTATATATCTTA-CACTGTAAAGCTAACTTAGCATTAACCTTTTAAGTTAAAGATTAAGAGAACCAACACCTCTTTACAGTGA"));
		sequences.add(new Sequence("chimp","ACTGAGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTCAAATCCCCTTATTTCTACTAGGACTATAAGAATCGAACTCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTAAGCACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCTCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AACCCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCGACACCTCTTTACAGTGA"));
		sequences.add(new Sequence("bonobo","ACTGAGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTTAAATCCCCTTATTTCTACTAGGACTATGAGAGTCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAACACAGACCAAGAGCCTTCAAAGCTCTCAGTAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAGC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTTGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCCCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCAACACCTCTTTACAGTGA"));
		sequences.add(new Sequence("gorilla","ACTGAGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGAGGTTTAAACCCCCTTATTTCTACTAGGACTATGAGAATTGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTGTCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTCACATCCTTCCCGTACTAAGAAATTTAGGTTAAACATAGACCAAGAGCCTTCAAAGCCCTTAGTAAGTTA-CAACACTTAATTTCTGTAAGGACTGCAAAACCCTACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATCAATGGGACTCAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAGTCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAT-TCACCTCGGAGCTTGGTAAAAAGAGGCCCAGCCTCTGTCTTTAGATTTACAGTCCAATGCCTTA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGACCTTCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAGGTTAAATTACGGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCGTTAACCTTTTAAGTTAAAGATTAAGAGTATCGGCACCTCTTTGCAGTGA"));
		sequences.add(new Sequence("orangutan","ACTGAGAAATATGTCTGACAAAAGAGTTACTTTGATAGAGTAAAAAATAGAGGTCTAAATCCCCTTATTTCTACTAGGACTATGGGAATTGAACCCACCCCTGAGAATCCAAAATTCTCCGTGCCACCCATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTA--CACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTCA-CAGCACTTAATTTCTGTAAGGACTGCAAAACCCCACTTTGCATCAACTGAGCGCAAATCAGCCACTTTAATTAAGCTAAGCCCTCCTAGACCGATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAGTCAAT-TGGCTTCAGTCCAAAGCCCCGGCAGGCCTTAAAGCTGCTCCTTCGAATTTGCAATTCAACATGACAA-TCACCTCAGGGCTTGGTAAAAAGAGGTCTGACCCCTGTTCTTAGATTTACAGCCTAATGCCTTAACTCGGCCATTTTACCGCAAAAAAGGAAGGAATCGAACCTCCTAAAGCTGGTTTCAAGCCAACCCCATAACCCCCATGACTTTTTCAAAAGGTACTAGAAAAACCATTTCGTAACTTTGTCAAAGTTAAATTACAGGTC-AGACCCTGTGTATCTTA-CATTGCAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGACTAAGAGAACCAGCCTCTCTTTGCAATGA"));
		sequences.add(new Sequence("siamang","ACTGAGAAATACGTCTGACGAAAGAGTTACTTTGATAGAGTAAATAACAGGGGTTTAAATCCCCTTATTTCTACTAGAACCATAGGAGTCGAACCCATCCTTGAGAATCCAAAACTCTCCGTGCCACCCGTCGCACCCTGTTCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCATACTAAGAAATTTAGGTTAAACACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTAACAAAACTTAATTTCTGCAAGGGCTGCAAAACCCTACTTTGCATCAACCGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATCGATGGGACTTAAACCCATAAAAATTTAGTTAACAGCTAAACACCCTAAACAACCTGGCTTCAATCTAAAGCCCCGGCAGA-GTTGAAGCTGCTTCTTTGAACTTGCAATTCAACGTGAAAAATCACTTCGGAGCTTGGCAAAAAGAGGTTTCACCTCTGTCCTTAGATTTACAGTCTAATGCTTTA-CTCAGCCACTTTACCACAAAAAAGGAAGGAATCGAACCCTCTAAAACCGGTTTCAAGCCAGCCCCATAACCTTTATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATCACAGGTCCAAACCCCGTATATCTTATCACTGTAGAGCTAGACCAGCATTAACCTTTTAAGTTAAAGACTAAGAGAACTACCGCCTCTTTACAGTGA"));
		Alignment data = null;
		data = new Alignment();
    	for (Sequence sequence : sequences) {
    		data.sequenceInput.setValue(sequence, data);
    	}
    	//data.m_nStateCount.setValue(4, data);
    	data.dataTypeInput.setValue("nucleotide", data);
    	data.excludefromInput.setValue(0, data);
    	data.excludetoInput.setValue(4, data);
    	data.excludeeveryInput.setValue(1, data);
    	data.isAscertainedInput.setValue(true, data);
        data.initAndValidate();

		return data;
	}
	
	public static Tree getTree(Alignment data) throws Exception {
		TreeParser tree = new TreeParser();
		tree.initByName("taxa", data, 
				        "newick","((((human:0.024003,(chimp:0.010772,bonobo:0.010772):0.013231):0.012035,gorilla:0.036038):0.033087000000000005,orangutan:0.069125):0.030456999999999998,siamang:0.099582);",
				        "IsLabelledNewick", true);
		return tree;
	}


	@Test
	public void testJC69Likelihood() throws Exception {
		// Set up JC69 model: uniform freqs, kappa = 1, 0 gamma categories	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data, 
						 "estimate", false);

		HKY hky = new HKY();
		hky.initByName("kappa", "1.0", 
				       "frequencies",freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", hky);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);
		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1992.2056440317247, PRECISION);

		
		if (g_bUseAmbiguities) {
			likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
			fLogP = likelihood.calculateLogP();
			assertEquals(fLogP, -1992.2056440317247, PRECISION);
		}
		likelihood.finalize();
	}

	@Test
	public void testAscertainedJC69Likelihood() throws Exception {
		// as testJC69Likelihood but with ascertained alignment	
		Alignment data = getAscertainedAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data, 
				 "estimate", false);

		HKY hky = new HKY();
		hky.initByName("kappa", "1.0", "frequencies", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", hky);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		// the following number comes from Beast 1.6
		assertEquals(fLogP, -737.7140695360017, PRECISION);
		likelihood.finalize();
	}
	
	@Test
	public void testK80Likelihood() throws Exception {
		// Set up K80 model: uniform freqs, kappa = 27.402591, 0 gamma categories	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data, 
				 "estimate", false);

		HKY hky = new HKY();
		hky.initByName("kappa", "27.40259", "frequencies", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", hky);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1856.303048876734, PRECISION);

		if (g_bUseAmbiguities) {
			likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
			fLogP = likelihood.calculateLogP();
			assertEquals(fLogP, -1856.303048876734, PRECISION);
		}
		likelihood.finalize();
	}
	
	@Test
	public void testHKY85Likelihood() throws Exception {
		// Set up HKY85 model: estimated freqs, kappa = 29.739445, 0 gamma categories	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data); 

		HKY hky = new HKY();
		hky.initByName("kappa", "29.739445", "frequencies", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", hky);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1825.2131708068507, PRECISION);
	
		if (g_bUseAmbiguities) {
			likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
			fLogP = likelihood.calculateLogP();
			assertEquals(fLogP, -1825.2131708068507, PRECISION);
		}
		likelihood.finalize();
	}
		
	
	
	@Test
	public void testHKY85GLikelihood() throws Exception {
		// Set up HKY85+G model: estimated freqs, kappa = 38.82974, 4 gamma categories, shape = 0.137064	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data); 

		HKY hky = new HKY();
		hky.initByName("kappa", "38.82974", "frequencies", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4,
				"shape", "0.137064", 
				"proportionInvariant", "0.0",
				"substModel", hky);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		System.err.println(fLogP - -1789.7593576610134);
		assertEquals(fLogP, -1789.7593576610134, PRECISION);
	
		if (g_bUseAmbiguities) {
			likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
			fLogP = likelihood.calculateLogP();
			assertEquals(fLogP, -1789.7593576610134, PRECISION);
		}
		likelihood.finalize();
	}

	@Test
	public void testHKY85ILikelihood() throws Exception {
		// Set up HKY85+I model: estimated freqs, kappa = 38.564672, 0 gamma categories, prop invariant = 0.701211	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data); 

		HKY hky = new HKY();
		hky.initByName("kappa", "38.564672", "frequencies", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1,
				"shape", "0.137064", 
				"proportionInvariant", "0.701211",
				"substModel", hky);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1789.912401996943, PRECISION);

		if (g_bUseAmbiguities) {
			likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
			fLogP = likelihood.calculateLogP();
			assertEquals(fLogP, -1789.912401996943, PRECISION);
		}
		likelihood.finalize();
	}

	@Test
	public void testHKY85GILikelihood() throws Exception {
		// Set up HKY85+G+I model: estimated freqs, kappa = 39.464538, 4 gamma categories, shape = 0.587649, prop invariant = 0.486548	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data); 

		HKY hky = new HKY();
		hky.initByName("kappa", "39.464538", "frequencies", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4,
				"shape", "0.587649", 
				"proportionInvariant", "0.486548",
				"substModel", hky);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1789.639227747059, PRECISION);

		if (g_bUseAmbiguities) {
			likelihood.initByName("useAmbiguities", true, "data",data, "tree",tree, "siteModel", siteModel);
			fLogP = likelihood.calculateLogP();
			assertEquals(fLogP, -1789.639227747059, PRECISION);
		}
		likelihood.finalize();
	}


	@Test
	public void testGTRLikelihood() throws Exception {
		// Set up GTR model: no gamma categories, no proportion invariant 	
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data); 

		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
		gsm.initByName("rates", "1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", "frequencies", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1,
				"substModel", gsm);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1969.145839307625, PRECISION);

		likelihood.initByName("useAmbiguities", false, "data",data, "tree",tree, "siteModel", siteModel);
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1969.145839307625, PRECISION);
		likelihood.finalize();
	}

	@Test
	public void testGTRILikelihood() throws Exception {
		// Set up GTR model: prop invariant = 0.5
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data); 

		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
		gsm.initByName("rates", "1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", "frequencies", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1,
				"proportionInvariant", "0.5",
				"substModel", gsm);
		//siteModel.init("1.0", 1, null, "0.5", gsm);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1948.8417455357564, PRECISION);

		likelihood.initByName("useAmbiguities", false, "data",data, "tree",tree, "siteModel", siteModel);
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1948.8417455357564, PRECISION);
		likelihood.finalize();
	}
	
	@Test
	public void testGTRGLikelihood() throws Exception {
		// Set up GTR model: 4 gamma categories, gamma shape = 0.5
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data); 

		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
		gsm.initByName("rates", "1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", "frequencies", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4,
				"shape", "0.5", 
				"substModel", gsm);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1949.0360143622, PRECISION);

		likelihood.initByName("useAmbiguities", false, "data",data, "tree",tree, "siteModel", siteModel);
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1949.0360143622, PRECISION);
		likelihood.finalize();
	}
	
	@Test
	public void testGTRGILikelihood() throws Exception {
		// Set up GTR model: 4 gamma categories, gamma shape = 0.5, prop invariant = 0.5
		Alignment data = getAlignment();
		Tree tree = getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data); 

		GeneralSubstitutionModel gsm = new GeneralSubstitutionModel();
		gsm.initByName("rates", "1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", "frequencies", freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 4,
				"shape", "0.5", 
				"proportionInvariant", "0.5",
				"substModel", gsm);

		ExperimentalTreeLikelihood likelihood = newTreeLikelihood();
		likelihood.initByName("data",data, "tree",tree, "siteModel", siteModel);

		double fLogP = 0;
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1947.5829396144961, PRECISION);

		likelihood.initByName("useAmbiguities", false, "data",data, "tree",tree, "siteModel", siteModel);
		fLogP = likelihood.calculateLogP();
		assertEquals(fLogP, -1947.5829396144961, PRECISION);
		likelihood.finalize();
	}

} // class TreeLikelihoodTest
