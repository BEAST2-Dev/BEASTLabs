package beast.math.distributions;



import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.core.State;
import beast.math.LogTricks;;

@Description("Takes mixture of distributions")
public class MixtureDistribution extends Distribution {
	public final Input<List<Distribution>> distrsInput = new Input<>("distribution", "distribution to average over", new ArrayList<>(), Validate.REQUIRED);
    public final Input<RealParameter> weightsInput = new Input<>("weights", "weights on the distributions, should sum to 1 if specified");

	List<Distribution> distrs;
	double [] logWeights;

	@Override
	public void initAndValidate() {
		distrs = distrsInput.get();

		double [] weights;
		if (weightsInput.get() == null) {
			weights = new double[distrs.size()];
			for (int i = 0; i < weights.length; i++) {
				weights[i] = 1.0 / weights.length;
			}
		} else {
			weights = weightsInput.get().getDoubleValues();
			
			// sanity checks
			if (weights.length != distrs.size()) {
				throw new IllegalArgumentException("the number of weights (" + weights.length + ") does not match "
						+ "the number of distributions (" + distrs.size() + ")");
			}
			double sum = 0;
			for (double d : weights) {
				sum += d;
			}
			if (Math.abs(sum - 1)> 1e10) {
				throw new IllegalArgumentException("weights should sum to 1, not " + sum );
			}
		}
		
		logWeights = new double[weights.length];
		for (int i = 0; i < weights.length; i++) {
			logWeights[i] = Math.log(weights[i]);			
		}
	}
		
	@Override
	public double calculateLogP() {
		logP = 0;
		double [] logPs = new double[distrs.size()];
		int k = 0;
		for (Distribution d : distrs) {
			if (d.isDirtyCalculation()) {
				logPs[k++] = d.calculateLogP() + logWeights[k];
			} else {
				logPs[k++] = d.getCurrentLogP() + logWeights[k];
			}
		}
		logP = LogTricks.logSum(logPs);
		return logP;
	}
		
	@Override
	public List<String> getArguments() {return null;}

	@Override
	public List<String> getConditions() {return null;}

	@Override
	public void sample(State state, Random random) {}

}
