package beastlabs.parsimony;

import beast.base.evolution.alignment.Alignment;
import beastlabs.parsimony.FitchParsimony;
import beastlabs.parsimony.ParsimonyCriterion;

public class FitchParsimonyFactory {

	static public ParsimonyCriterion newFitchParsimony(Alignment data, boolean gapsAreStates) {
		int stateCount = data.getMaxStateCount();
		if (stateCount <= 32) {
			return new FitchParsimony32(data, gapsAreStates);
		} else if (stateCount <= 64) {
			return new FitchParsimony32(data, gapsAreStates);
		} else {
			return new FitchParsimony(data, gapsAreStates);
		}
	}
	
}
