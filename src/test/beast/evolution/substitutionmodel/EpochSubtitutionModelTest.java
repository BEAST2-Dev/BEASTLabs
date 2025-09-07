package test.beast.evolution.substitutionmodel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import beast.base.evolution.substitutionmodel.ComplexSubstitutionModel;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.evolution.substitutionmodel.HKY;
import beast.base.inference.parameter.RealParameter;
import beastlabs.evolution.substitutionmodel.EpochSubstitutionModel;

public class EpochSubtitutionModelTest {

	
	@Test
	public void testConsistencyForHKY() {
		// set up HKY model with unequal frequencies
		RealParameter freqs = new RealParameter("0.2 0.3 0.4 0.1");
		Frequencies frequencies = new Frequencies();
		frequencies.initByName("frequencies", freqs);
		HKY model = new HKY();
		model.initByName("frequencies", frequencies, "kappa", "2.0");
		
		// use non-unit rate
		double rate = 0.5;
		
		// get transition probabilities for HKY from time 0.5 to time 0.0
		double [] probs = new double[5*5];
		model.getTransitionProbabilities(null, 0.5, 0, rate, probs);

		// now do the same, but with EpochSubstitutionModel
		// using 3 epochs
		EpochSubstitutionModel substModel = new EpochSubstitutionModel();
		substModel.initByName("frequencies", frequencies,
				"model", model, 
				"model", model,
				"model", model,
				"epochDates","0.1 0.4"
				);
	
		double [] epochProbs = new double[5*5];
		substModel.getTransitionProbabilities(null, 0.5, 0, rate, epochProbs);
		
		// all transition probabilities should be equal
		for (int i = 0; i < 4*4; i++) {
			assertEquals(probs[i], epochProbs[i], 1e-10);
		}
	}
	
	
	@Test
	public void testConsistencyForNonReversibleModel() {
		// set up HKY model with unequal frequencies
		RealParameter freqs = new RealParameter("0.2 0.3 0.4 0.1");
		Frequencies frequencies = new Frequencies();
		frequencies.initByName("frequencies", freqs);
		ComplexSubstitutionModel model = new ComplexSubstitutionModel();
		model.initByName("frequencies", frequencies, "rates", "1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0 11.0 12.0");
		
		// use non-unit rate
		double rate = 0.5;
		
		// get transition probabilities for HKY from time 0.5 to time 0.0
		double [] probs = new double[5*5];
		model.getTransitionProbabilities(null, 0.5, 0, rate, probs);

		// now do the same, but with EpochSubstitutionModel
		// using 3 epochs
		EpochSubstitutionModel substModel = new EpochSubstitutionModel();
		substModel.initByName("frequencies", frequencies,
				"model", model, 
				"model", model,
				"model", model,
				"epochDates","0.1 0.4"
				);
	
		double [] epochProbs = new double[5*5];
		substModel.getTransitionProbabilities(null, 0.5, 0, rate, epochProbs);
		
		// all transition probabilities should be equal
		for (int i = 0; i < 4*4; i++) {
			assertEquals(probs[i], epochProbs[i], 1e-10);
		}
	}
	
	
	@Test
	public void testInConsistencyForDifferentModels() {
		// set up HKY model with unequal frequencies
		RealParameter freqs = new RealParameter("0.2 0.3 0.4 0.1");
		Frequencies frequencies = new Frequencies();
		frequencies.initByName("frequencies", freqs);
		ComplexSubstitutionModel model = new ComplexSubstitutionModel();
		model.initByName("frequencies", frequencies, "rates", "1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0 11.0 12.0");
		
		// set up HKY model with unequal frequencies
		RealParameter freqs2 = new RealParameter("0.2 0.3 0.4 0.1");
		Frequencies frequencies2 = new Frequencies();
		frequencies2.initByName("frequencies", freqs2);
		HKY model2 = new HKY();
		model2.initByName("frequencies", frequencies2, "kappa", "2.0");
		
		// use non-unit rate
		double rate = 0.5;
		
		// get transition probabilities for HKY from time 0.5 to time 0.0
		double [] probs = new double[5*5];
		model.getTransitionProbabilities(null, 0.5, 0, rate, probs);

		// now do the same, but with EpochSubstitutionModel
		// using 3 epochs
		EpochSubstitutionModel substModel = new EpochSubstitutionModel();
		substModel.initByName("frequencies", frequencies,
				"model", model, 
				"model", model2,
				"model", model,
				"epochDates","0.1 0.4"
				);
	
		double [] epochProbs = new double[5*5];
		substModel.getTransitionProbabilities(null, 0.5, 0, rate, epochProbs);
		
		// all transition probabilities should be equal
		for (int i = 0; i < 4*4; i++) {
			assertEquals(Math.abs(probs[i] - epochProbs[i]) > 1e-10, true);
		}
	}
	
	
	
}
