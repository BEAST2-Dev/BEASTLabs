package beast.evolution.operators;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.OperatorSchedule;

@Description("Operator schedule that replaces operators with Bactrian operators")
public class BactrianOperatorSchedule extends OperatorSchedule {
	
	@Override
	public void addOperator(Operator p) {
		if (p.getClass() == ScaleOperator.class) {
			Operator bp = new BactrianScaleOperator();
			initialiseOperator(p, bp);
			p = bp;
		} else if (p.getClass() == RealRandomWalkOperator.class) {
			Operator bp = new BactrianRandomWalkOperator();
			initialiseOperator(p, bp);
			p = bp;
		} else if (p.getClass() == Uniform.class) {
			Operator bp = new BactrianNodeOperator();
			initialiseOperator(p, bp);
			p = bp;
		} else if (p.getClass() == UniformOperator.class) {
			Operator bp = new BactrianIntervalOperator();
			initialiseOperator(p, bp);
			p = bp;
		} else if (p.getClass() == DeltaExchangeOperator.class) {
			Operator bp = new BactrianDeltaExchangeOperator();
			initialiseOperator(p, bp);
			p = bp;
		} else if (p.getClass() ==  TipDatesRandomWalker.class) {
			Operator bp = new BactrianTipDatesRandomWalker();
			initialiseOperator(p, bp);
			p = bp;
		} else if (p.getClass() == UpDownOperator.class) {
			Operator bp = new BactrianUpDownOperator();
			initialiseOperator(p, bp);
			p = bp;
		}
		super.addOperator(p);
	}

	private void initialiseOperator(Operator p, Operator bp) {
		List<Object> os = new ArrayList<>();
		for (Input<?> input : bp.listInputs()) {
			os.add(input.getName());
			os.add(p.getInputValue(input.getName()));
		}
		bp.initByName(os.toArray());
	}

}
