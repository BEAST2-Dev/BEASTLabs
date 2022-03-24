package beast.evolution.operators;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import beast.core.*;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.inference.Operator;
import beast.base.inference.OperatorSchedule;
import beast.base.inference.StateNode;
import beast.base.util.Randomizer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


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
	public void reject(int reason) {
		operator1.reject(reason);
		if (operator2 != null) {
			operator2.reject(reason);
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

	@Override
	public void storeToFile(PrintWriter out) {
		out.print("{\"id\":\"" + getID() + "\",\"operators\":[\n");
		int k = 0;
		for (Operator o : operatorGroup1 ) {
			o.storeToFile(out);
			out.println(",");
		}
		for (Operator o : operatorGroup2 ) {
			o.storeToFile(out);
			if (k++ < operatorGroup2.size() - 1) {
				out.println(",");
			}
		}
		out.print("]}");
	}

	@Override
	public void restoreFromFile(JSONObject o) {
		try {
			JSONArray operatorlist = o.getJSONArray("operators");
			for (int i = 0; i < operatorlist.length(); i++) {
				JSONObject item = operatorlist.getJSONObject(i);
				String id = item.getString("id");
				boolean found = false;
				if (!id.equals("null")) {
					for (Operator operator: operatorGroup1 ) {
						if (id.equals(operator.getID())) {
							operator.restoreFromFile(item);
							found = true;
							break;
						}
					}
					for (Operator operator: operatorGroup2 ) {
						if (id.equals(operator.getID())) {
							operator.restoreFromFile(item);
							found = true;
							break;
						}
					}
				}
				if (!found) {
					Log.warning.println("Operator (" + id + ") found in state file that is not in operator list any more");
				}
			}
			for (Operator operator: operatorGroup1) {
				if (operator.getID() == null) {
					Log.warning.println("Operator (" + operator.getClass() + ") found in BEAST file that could not be restored because it has not ID");
				}
			}
			for (Operator operator: operatorGroup2) {
				if (operator.getID() == null) {
					Log.warning.println("Operator (" + operator.getClass() + ") found in BEAST file that could not be restored because it has not ID");
				}
			}
		} catch (JSONException e) {
			// it is not a JSON file -- probably a version 2.0.X state file
		}
	}

	public void setOperatorSchedule(final OperatorSchedule operatorSchedule) {
		super.setOperatorSchedule(operatorSchedule);

		for (Operator operator : operatorGroup1) {
			operator.setOperatorSchedule(operatorSchedule);
		}
		for (Operator operator : operatorGroup2) {
			operator.setOperatorSchedule(operatorSchedule);
		}
	}
}
