package beast.prevalence;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.core.Input.Validate;
import beast.prevalence.PrevalenceList.Action;
import beast.base.util.Randomizer;

@Description("Randomly adds Infection or recovery to prevalence list")
public class AddOperator extends Operator {
	public Input<PrevalenceList> m_list = new Input<PrevalenceList>("list","raw prevalence list with only times and actions", Validate.REQUIRED);

	@Override
	public void initAndValidate() {
	}

	@Override
	public double proposal() {
		PrevalenceList list = m_list.get();
		Double fTime = Randomizer.nextDouble() * list.startTime();
		Action action = Action.INFECTED;
		if (Randomizer.nextDouble() > 0.5) {
			action = Action.RECOVERED;
		}
		if (list.add(fTime, action, -1)) {
			return 0;
		}
		// addition did not succeed
		return Double.NEGATIVE_INFINITY;
	}
}
