package beast.prevalence;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.Input.Validate;
import beast.util.Randomizer;

@Description("Randomly moves item in prevalence list")
public class MoveOperator extends Operator {
	public Input<PrevalenceList> m_list = new Input<PrevalenceList>("list","raw prevalence list with only times and actions", Validate.REQUIRED);

	@Override
	public void initAndValidate() {
	}

	@Override
	public double proposal() {
		// test that at least one element in the list can be moved (is not linked)
		if (!m_list.get().hasDeletables()) {
			return Double.NEGATIVE_INFINITY;
		}

		PrevalenceList list = m_list.get(this);
		// choose unlinked node at random, but not the last one
		int iTime = Randomizer.nextInt(list.getSize() - 1);
		while (list.isLinked(iTime)) {
			iTime = Randomizer.nextInt(list.getSize() - 1);
		}
		double fTargetTime = Randomizer.nextDouble() * list.startTime();
		try {
			list.move(iTime, fTargetTime);
		} catch (Exception e) {
			e.printStackTrace();
			// addition did not succeed
			return Double.NEGATIVE_INFINITY;
		}
		return 0;
	}

}
