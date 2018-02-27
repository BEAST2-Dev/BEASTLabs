package beast.math.distributions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sun.org.glassfish.gmbal.Description;

import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.math.LogTricks;;

@Description("Takes mixture of distributions")
public class MixtureDistribution extends Distribution {
	public final Input<List<Distribution>> distrsInput = new Input<>("distribution", "distribution to average over", new ArrayList<>(), Validate.REQUIRED);
	
	List<Distribution> distrs;
	
	@Override
	public void initAndValidate() {
		distrs = distrsInput.get();
	}
		
	@Override
	public double calculateLogP() {
		logP = 0;
		double [] logPs = new double[distrs.size()];
		int k = 0;
		for (Distribution d : distrs) {
			logPs[k++] = d.getCurrentLogP();
		}
		logP = LogTricks.logSum(logPs) - Math.log(distrs.size());
		return logP;
	}
		
	@Override
	public List<String> getArguments() {return null;}

	@Override
	public List<String> getConditions() {return null;}

	@Override
	public void sample(State state, Random random) {}

}
