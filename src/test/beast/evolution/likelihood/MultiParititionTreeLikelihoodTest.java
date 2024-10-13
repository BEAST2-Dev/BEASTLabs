package test.beast.evolution.likelihood;



import java.io.File;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import org.junit.Test;

import beast.base.core.BEASTInterface;
import beast.base.core.ProgramStatus;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.likelihood.TreeLikelihood;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.evolution.substitutionmodel.HKY;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.Distribution;
import beast.base.inference.MCMC;
import beast.base.inference.State;
import beast.base.inference.parameter.RealParameter;
import beast.base.parser.XMLParser;
import beastlabs.evolution.likelihood.MultiPartitionTreeLikelihood;
import test.beast.BEASTTestCase;

/** test MultiParititionTreeLikelihood with two partitions **/
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

	@Test
	public void testJC69Likelihood() throws Exception {
    	
		Alignment data = BEASTTestCase.getAlignment();
		Tree tree = BEASTTestCase.getTree(data);
		
		Frequencies freqs = new Frequencies();
		freqs.initByName("data", data, 
						 "estimate", false);

		HKY jc = new HKY();
		jc.initByName("kappa", "1.0", 
				       "frequencies",freqs);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "gammaCategoryCount", 1, "substModel", jc, "shape", "0.5");

		TreeLikelihood likelihood = newTreeLikelihood();
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
    }
    
    
    @Test
    public void testStateUpdate() throws Exception {
    	XMLParser parser = new XMLParser();
    	MCMC mcmc = (MCMC) parser.parseFile(new File("examples/testMultiParitionTreeLikelihood.xml"));
    	State state = mcmc.startStateInput.get();
    	int k = 0;
    	Tree tree = (Tree) state.stateNodeInput.get().get(k++);
    	TreeParser tp = new TreeParser("((((((Gorilla:0.0723395143713732,(Homo_sapiens:0.052879787958654334,Pan:0.052879787958654334):0.01945972641271887):0.07620607670523938,Pongo:0.14854559107661247):0.04562010226275853,Hylobates:0.19416569333937111):0.14412091592210913,((M_fascicularis:0.05467554166298294,(M_mulatta:0.019285644045882844,Macaca_fuscata:0.019285644045882844):0.0353898976171001):0.03224313872198703,M_sylvanus:0.08691868038496997):0.25136792887651027):0.11469256513277382,Saimiri_sciureus:0.45297917439425395):0.13803066465903857,(Lemur_catta:0.4409509877855818,Tarsius_syrichta:0.4409509877855818):0.15005885126771074):0.0");
    	tree.assignFrom(tp);
    	
    	MultiPartitionTreeLikelihood beagleTL = null;
    	for (BEASTInterface o : tree.getOutputs()) {
    		if (o instanceof TreeLikelihood) {
    			for (BEASTInterface o2 : o.getOutputs()) {
    				if (o2 instanceof MultiPartitionTreeLikelihood) {
    	    			beagleTL = (MultiPartitionTreeLikelihood) o2;
    	    			break;
    				}
    			}
    		}
    	}
    	if (beagleTL == null) {
    		throw new RuntimeException("Expected MultiPartitionTreeLikelihood in output of tree");
    	}
    	RealParameter birthRatetree = (RealParameter) state.stateNodeInput.get().get(k++);
    	RealParameter gammaShapefirsthalf = (RealParameter) state.stateNodeInput.get().get(k++);
    	RealParameter kappafirsthalf = (RealParameter) state.stateNodeInput.get().get(k++);
    	RealParameter freqParameterfirsthalf = (RealParameter) state.stateNodeInput.get().get(k++);
    	RealParameter freqParametersecondhalf = (RealParameter) state.stateNodeInput.get().get(k++);
    	RealParameter gammaShapesecondhalf = (RealParameter) state.stateNodeInput.get().get(k++);
    	RealParameter kappasecondhalf = (RealParameter) state.stateNodeInput.get().get(k++);
    	RealParameter mutationRatefirsthalf = (RealParameter) state.stateNodeInput.get().get(k++);
    	RealParameter mutationRatesecondhalf = (RealParameter) state.stateNodeInput.get().get(k++);
    	Distribution posterior = mcmc.posteriorInput.get();
    	

    	double logP = state.robustlyCalcPosterior(mcmc.posteriorInput.get());
    	assertEquals(-16297.263551345248, logP);
    	assertEquals(44, beagleTL.totalMatrixUpdateCount);
    	assertEquals(22, beagleTL.totalPartialsUpdateCount);
    	assertEquals(1, beagleTL.totalEvaluationCount);

    	long lastMUC = beagleTL.totalMatrixUpdateCount;
    	long lastPUC = beagleTL.totalPartialsUpdateCount;
    	
    	state.store(0);
    	testParameterUpdate(birthRatetree, 3.5, -16291.090775947769, 44, 22, 1, state, posterior, beagleTL);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);
    	
    	state.store(0);
    	testParameterUpdate(gammaShapefirsthalf, 0.3
    			,/*logP*/-16161.361498166185
    	    	,/*expectedMatrixUpdateCount*/66
    	    	,/*expectedPartialsUpdateCount*/33
    	    	,/*expectedEvaluationCount*/2, state, posterior, beagleTL);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);

    	state.store(0);
    	testParameterUpdate(gammaShapesecondhalf, 0.38
    			,/*logP*/-16015.435567151202
    	    	,/*expectedMatrixUpdateCount*/88
    	    	,/*expectedPartialsUpdateCount*/44
    	    	,/*expectedEvaluationCount*/3, state, posterior, beagleTL);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);

    	state.store(0);
    	testParameterUpdate(kappafirsthalf, 13.8
    			,/*logP*/-15968.91544381202
    	    	,/*expectedMatrixUpdateCount*/110
    	    	,/*expectedPartialsUpdateCount*/55
    	    	,/*expectedEvaluationCount*/4, state, posterior, beagleTL);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);

    	state.store(0);
    	testParameterUpdate(kappasecondhalf, 11.7
    			,/*logP*/-15840.822384672445
    	    	,/*expectedMatrixUpdateCount*/132
    	    	,/*expectedPartialsUpdateCount*/66
    	    	,/*expectedEvaluationCount*/5, state, posterior, beagleTL);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);
    	
    	state.store(0);
		System.out.println("setting " + mutationRatefirsthalf.getID() + " to " + 1.22);
    	mutationRatefirsthalf.setValue(1.22);
		System.out.println("setting " + mutationRatesecondhalf.getID() + " to " + 0.88);
    	mutationRatesecondhalf.setValue(0.88);
    	state.storeCalculationNodes();
        state.checkCalculationNodesDirtiness();
        logP = posterior.calculateLogP();
        state.acceptCalculationNodes();
    	
    	assertEquals(-15828.783962608246, logP);
    	assertEquals(176, beagleTL.totalMatrixUpdateCount);
    	assertEquals(88, beagleTL.totalPartialsUpdateCount);
    	assertEquals(6, beagleTL.totalEvaluationCount);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);

    
    	state.store(0);
    	freqParameterfirsthalf.setValue(1, 0.35);
    	freqParameterfirsthalf.setValue(2, 0.08);
    	freqParameterfirsthalf.setValue(3, 0.22);
    	testParameterUpdate(freqParameterfirsthalf, 0.35
    			,/*logP*/-15415.55555897793
    	    	,/*expectedMatrixUpdateCount*/198
    	    	,/*expectedPartialsUpdateCount*/99
    	    	,/*expectedEvaluationCount*/7, state, posterior, beagleTL);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);

    	state.store(0);
    	freqParametersecondhalf.setValue(1, 0.28);
    	freqParametersecondhalf.setValue(2, 0.09);
    	freqParametersecondhalf.setValue(3, 0.24);
    	testParameterUpdate(freqParametersecondhalf, 0.39
    			,/*logP*/-15096.62559406815
    	    	,/*expectedMatrixUpdateCount*/220
    	    	,/*expectedPartialsUpdateCount*/110
    	    	,/*expectedEvaluationCount*/8, state, posterior, beagleTL);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);
    	
    	state.store(0);
    	tree.getRoot().setHeight(0.65);
    	testParameterUpdate(birthRatetree, 3.5, -15130.412022712584, 224, 112, 9, state, posterior, beagleTL);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);

    	
    	state.robustlyCalcPosterior(posterior);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);
    	
    	state.store(0);
    	tree.scale(1.1);
    	testParameterUpdate(birthRatetree, 3.5, -15078.013548252191, 312, 156, 11, state, posterior, beagleTL);
    	lastMUC = report(lastMUC, beagleTL.totalMatrixUpdateCount);
    	lastPUC = report2(lastPUC, beagleTL.totalPartialsUpdateCount);

    }

	private long report(long lastMUC, long totalMatrixUpdateCount) {
		System.out.println("Matrix operations: " + (totalMatrixUpdateCount - lastMUC) + " out of " + totalMatrixUpdateCount);
		return totalMatrixUpdateCount;
	}

	private long report2(long lastPUC, long totalPartialsUpdateCount) {
		System.out.println("Partials operations: " + (totalPartialsUpdateCount - lastPUC) + " out of " + totalPartialsUpdateCount);
		return totalPartialsUpdateCount;
	}

	private void testParameterUpdate(RealParameter parameter, double value, double expectedLogP, 
			int expectedMatrixUpdateCount, int expectedPartialsUpdateCount, int expectedEvaluationCount, 
			State state, Distribution posterior, MultiPartitionTreeLikelihood beagleTL) {
		System.out.println("setting " + parameter.getID() + " to " + value);
		
    	parameter.setValue(value);
    	state.storeCalculationNodes();
        state.checkCalculationNodesDirtiness();
        double logP = posterior.calculateLogP();
        state.acceptCalculationNodes();
    	assertEquals(expectedLogP, logP);
    	assertEquals(expectedMatrixUpdateCount, beagleTL.totalMatrixUpdateCount);
    	assertEquals(expectedPartialsUpdateCount, beagleTL.totalPartialsUpdateCount);
    	assertEquals(expectedEvaluationCount, beagleTL.totalEvaluationCount);

//		System.out.println("logP "+ logP);
//		System.out.println("expectedMatrixUpdateCount "+ beagleTL.totalMatrixUpdateCount);
//		System.out.println("expectedPartialsUpdateCount "+ beagleTL.totalPartialsUpdateCount);
//		System.out.println("expectedEvaluationCount "+ beagleTL.totalEvaluationCount);
	}
    
    
} // class MultiParititionTreeLikelihoodTest
