package beast.evolution.operators;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.StateNode;
import beast.core.Input.Validate;
import beast.util.Randomizer;



@Description("Combines propoosals by randomly selecting from two groups of operators")
public class CombinedOperator extends Operator {
	public Input<List<Operator>> operatorGroup1Input = new Input<List<Operator>>("operator1","operator for doing first proposal", new ArrayList<Operator>(), Validate.REQUIRED);
	public Input<List<Operator>> operatorGroup2Input = new Input<List<Operator>>("operator2","operator for doing second proposal", new ArrayList<Operator>(), Validate.REQUIRED);

	List<Operator> operatorGroup1;
	List<Operator> operatorGroup2;
	Operator operator1; 
	Operator operator2;
	
	@Override
	public void initAndValidate() {
		operatorGroup1 = operatorGroup1Input.get();
		operatorGroup2 = operatorGroup2Input.get();
	}
	
	@Override
	public double proposal() {
		operator1 = operatorGroup1.get(Randomizer.nextInt(operatorGroup1.size()));
		operator2 = operatorGroup2.get(Randomizer.nextInt(operatorGroup2.size()));
		double logHR = operator1.proposal();
		if (logHR == Double.NEGATIVE_INFINITY) {
			operator2 = null;
			return logHR;
		}
		logHR += operator2.proposal();
		return logHR;
	}
	
	@Override
	public void accept() {
		operator1.accept();
		operator2.accept();
	}
	
	@Override
	public void reject() {
		operator1.reject();
		if (operator2 != null) {
			operator2.reject();
		}
	}
	
	@Override
	public List<StateNode> listStateNodes() {
		List<StateNode> list = new ArrayList<StateNode>();
		for (Operator operator : operatorGroup1) {
			List<StateNode> list2 = operator.listStateNodes();
			list.addAll(list2);
			
		}
		for (Operator operator : operatorGroup2) {
			List<StateNode> list2 = operator.listStateNodes();
			list.addAll(list2);
			
		}
		return list;
	}
}
