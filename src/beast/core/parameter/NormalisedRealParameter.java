package beast.core.parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.core.*;
import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.inference.parameter.RealParameter;

@Description("Constant value that acts as immutable RealParameter where values are normalised")
public class NormalisedRealParameter extends RealParameter {
	final public Input<List<Function>> functionListInput = new Input<>("x", "values makeng up the parameter", new ArrayList<>(), Validate.REQUIRED);
	
	final public Input<Boolean> logTransformInput = new Input<>("logtransform" ,"log-transform values before normalising", false);
	
	final public Input<Boolean> meanZeroVarOneInput = new Input<>("meanZeroVarOne" ,"transform values so that the mean=0 and variance=1. If false, normalise so that sum=1", false);
	
	
	public NormalisedRealParameter() {
		// we only want other Functions as input
		lowerValueInput.setRule(Validate.FORBIDDEN);
		upperValueInput.setRule(Validate.FORBIDDEN);
		valuesInput.setRule(Validate.FORBIDDEN);
	}


	@Override
	public void initAndValidate() {
		int dim = 0;
		for (Function f : functionListInput.get()) {
			dim += f.getDimension();
		}
		values = new Double[dim];
		
		// get values
		dim = 0;
		for (Function f : functionListInput.get()) {
			for (int i = 0; i < f.getDimension(); i++) {
				if (!logTransformInput.get()) {
					values[dim + i] = f.getArrayValue(i);
				} else {
					values[dim + i] = Math.log(f.getArrayValue(i));
				}
			}
			dim += f.getDimension();
		}
		Log.warning("Normalising from: " + Arrays.toString(values));
		
		// do the normalisation
		if (!meanZeroVarOneInput.get()) {
			// normalise to 1
			double sum = 0;
			for (Double d : values) {
				sum += d;
			}
			for (int i = 0; i < dim; i++) {
				values[i] = values[i] / sum;
			}
		} else {
			// set mean to 0
			double mean = 0;
			for (Double d : values) {
				mean += d;
			}
			mean /= dim;
			for (int i = 0; i < dim; i++) {
				values[i] = values[i] - mean;
			}
			// set variance to 1
			double var = 0;
			for (Double d : values) {
				var += d * d;
			}
			var /= dim;
			for (int i = 0; i < dim; i++) {
				values[i] = values[i]/Math.sqrt(var);
			}
		}
		Log.warning("Normalising to: " + Arrays.toString(values));
	}

}
