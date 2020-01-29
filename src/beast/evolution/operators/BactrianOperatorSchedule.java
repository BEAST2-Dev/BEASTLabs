package beast.evolution.operators;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.OperatorSchedule;
import beast.core.util.Log;

@Description("Operator schedule that replaces operators with Bactrian operators")
public class BactrianOperatorSchedule extends OperatorSchedule {

	public BactrianOperatorSchedule() {
		super();
	}
	
	@Override
	public void addOperator(Operator p) {
		if (p.getClass() == ScaleOperator.class) {
			Operator bp = new BactrianScaleOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == RealRandomWalkOperator.class) {
			Operator bp = new BactrianRandomWalkOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == Uniform.class) {
			Operator bp = new BactrianNodeOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == UniformOperator.class) {
			Operator bp = new BactrianIntervalOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == DeltaExchangeOperator.class) {
			Operator bp = new BactrianDeltaExchangeOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() ==  TipDatesRandomWalker.class) {
			Operator bp = new BactrianTipDatesRandomWalker();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == UpDownOperator.class) {
			Operator bp = new BactrianUpDownOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == SubtreeSlide.class) {
			Operator bp = new BactrianSubtreeSlide();
			p = initialiseOperator(p, bp);
		}
		super.addOperator(p);
	}

	private Operator initialiseOperator(Operator p, Operator bp) {
		Log.warning("replacing " + p.getID() + " with " + bp.getClass().getSimpleName());

		List<Object> os = new ArrayList<>();
		Set<String> inputNames = new LinkedHashSet<>();
		for (Input<?> input : p.listInputs()) {
			inputNames.add(input.getName());
		}
		
		for (Input<?> input : bp.listInputs()) {
			if (inputNames.contains(input.getName())) {
				Object value = p.getInputValue(input.getName());
				if (value != null && !(value instanceof List && ((List)value).size() == 0)) {
				    os.add(input.getName());
				    os.add(value);
				}	
			}
		}
		bp.initByName(os.toArray());
		bp.setID(p.getID());
		return bp;
	}

}
