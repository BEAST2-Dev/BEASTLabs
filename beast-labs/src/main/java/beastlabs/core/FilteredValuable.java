package beastlabs.core;

import java.io.PrintStream;

import beast.base.inference.CalculationNode;
import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Loggable;
import beast.base.core.Input.Validate;



@Description("selects values from a parameter, for instance all even indexed entries")
public class FilteredValuable extends CalculationNode implements Function, Loggable {

	public Input<Function> parameterInput = new Input<Function>("parameter", "the parameter to select values from",
			Validate.REQUIRED, Function.class);
	public Input<String> rangeInput = new Input<String>("range", "specifies list of indices " + "First site is 1."
			+ "Filter specs are comma separated, either a range [from]-[to] or iteration [from]:[to]:[step]; "
			+ "1-100 defines a range, " + "1-100\3 or 1:100:3 defines every third in range 1-100, "
			+ "1::3,2::3 removes every third site. "
			+ "negative values count from the last: -1 indicate the last element, -2 the one but last. "
			+ "Default for range [1]-[last site], default for iterator [1]:[last site]:[1]", Validate.REQUIRED);

	Function parameter;
	int[] indices;

	@Override
	public void initAndValidate() {
		parameter = parameterInput.get();
		parseFilterSpec();
	}

	private void parseFilterSpec() {
		// parse filter specification
		String range = rangeInput.get();
		String[] filters = range.split(",");
		int[] from = new int[filters.length];
		int[] to = new int[filters.length];
		int[] step = new int[filters.length];
		for (int i = 0; i < filters.length; i++) {
			range = " " + filters[i] + " ";
			if (range.matches(".*:.*:.+")) {
				// iterator, e.g. 1:100:3
				String[] strs = range.split(":");
				from[i] = parseInt(strs[0], 1) - 1;
				to[i] = parseInt(strs[1], parameter.getDimension()) - 1;
				step[i] = parseInt(strs[2], 1);
			} else if (range.matches(".*-.*")) {
				// range, e.g. 1-100/3
				if (range.indexOf('\\') >= 0) {
					String str2 = range.substring(range.indexOf('\\') + 1);
					step[i] = parseInt(str2, 1);
					range = range.substring(0, range.indexOf('\\'));
				} else {
					step[i] = 1;
				}
				String[] strs = range.split("-");
				from[i] = parseInt(strs[0], 1) - 1;
				to[i] = parseInt(strs[1], parameter.getDimension()) - 1;
			} else if (range.trim().matches("[0-9]*")) {
				from[i] = parseInt(range.trim(), 1) - 1;
				to[i] = from[i];
				step[i] = 1;
			} else {
				throw new IllegalArgumentException("Don't know how to parse filter " + range);
			}
		}

		boolean[] used = new boolean[parameter.getDimension()];
		for (int i = 0; i < to.length; i++) {
			for (int k = from[i]; k <= to[i]; k += step[i]) {
				used[k] = true;
			}
		}
		// count
		int k = 0;
		for (int i = 0; i < used.length; i++) {
			if (used[i]) {
				k++;
			}
		}
		// set up index set
		indices = new int[k];
		k = 0;
		for (int i = 0; i < used.length; i++) {
			if (used[i]) {
				indices[k++] = i;
			}
		}
	}

	int parseInt(String str, int _default) {
		str = str.replaceAll("\\s+", "");
		try {
			int value = Integer.parseInt(str);
			if (value < 0) {
				value = parameter.getDimension() + 1 + value;
			}
			return value;
		} catch (Exception e) {
			return _default;
		}
	}

	@Override
	public int getDimension() {
		return indices.length;
	}

	@Override
	public double getArrayValue() {
		return parameter.getArrayValue(indices[0]);
	}

	@Override
	public double getArrayValue(int iDim) {
		return parameter.getArrayValue(indices[iDim]);
	}

    /**
     * Loggable interface implementation follows (partly, the actual
     * logging of values happens in derived classes) *
     */
    @Override
    public void init(final PrintStream out) {
        if (getDimension() == 1) {
            out.print(getID() + "\t");
        } else {
            for (int iValue = 0; iValue < getDimension(); iValue++) {
                out.print(getID() + (iValue + 1) + "\t");
            }
        }
    }

	@Override
	public void log(long nSample, PrintStream out) {
        for (int iValue = 0; iValue < getDimension(); iValue++) {
            out.print(getArrayValue(iValue) + "\t");
        }
	}

    @Override
    public void close(final PrintStream out) {
        // nothing to do
    }

}
