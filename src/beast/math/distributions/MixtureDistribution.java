package beast.math.distributions;



import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import beast.base.core.Description;
import beast.base.inference.Distribution;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.State;
import beast.base.math.LogTricks;;

@Description("Takes mixture of distributions")
public class MixtureDistribution extends Distribution {
	public final Input<List<Distribution>> distrsInput = new Input<>("distribution", "distribution to average over", new ArrayList<>(), Validate.REQUIRED);
    public final Input<RealParameter> weightsInput = new Input<>("weights", "weights on the distributions, should sum to 1 if specified");
    public final Input<Boolean> useLogSpaceInput = new Input<>("useLogSpace", "if false (default) use sum_i w_i P_i, if true use sum_i w_i log(P_i). "
    		+ "Note if useLogSpace=true, the resulting distribution is not normalised (unless one of the weights is 1), while "
    		+ "if useLogSpace=false, the resulting distribution is normalised iff all mixture components are normalised.", false);

	List<Distribution> distrs;
	double [] logWeights;
	double [] weights;
	boolean useLogSpace;

	@Override
	public void initAndValidate() {
		distrs = distrsInput.get();

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

		useLogSpace = useLogSpaceInput.get();
		
		logWeights = new double[weights.length];
		if (!useLogSpace) {
			for (int i = 0; i < weights.length; i++) {
				logWeights[i] = Math.log(weights[i]);			
			}
		}
		
	}
	
		
	@Override
	public double calculateLogP() {
		logP = 0;
		if (useLogSpace) {
			int k = 0;
			for (Distribution d : distrs) {
				if (d.isDirtyCalculation()) {
					logP += d.calculateLogP() * weights[k];
				} else {
					logP += d.getCurrentLogP() * weights[k];
				}
				k++;
			}
		} else {
			double [] logPs = new double[distrs.size()];
			int k = 0;
			for (Distribution d : distrs) {
				if (d.isDirtyCalculation()) {
					logPs[k] = d.calculateLogP() + logWeights[k];
				} else {
					logPs[k] = d.getCurrentLogP() + logWeights[k];
				}
				k++;
			}
			logP = LogTricks.logSum(logPs);
		}
		return logP;
	}
		
	@Override
	public List<String> getArguments() {return null;}

	@Override
	public List<String> getConditions() {return null;}

	@Override
	public void sample(State state, Random random) {}

}
