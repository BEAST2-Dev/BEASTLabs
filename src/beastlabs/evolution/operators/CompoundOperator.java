package beastlabs.evolution.operators;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.inference.Operator;
import beast.base.inference.OperatorSchedule;
import beast.base.inference.StateNode;
import beast.base.util.Randomizer;

@Description("Operator that distributes a certain amount of operator weight. "
		+ "This is useful in setting up analyses with many operators (like a *BEAST analysis) "
		+ "and where the total amount of weight for groups of operators (say gene tree operators) "
		+ "needs to be fixed for optimal performance.")
public class CompoundOperator extends Operator {
	public Input<List<Operator>> operatorInput = new Input<>("operator", "operator the CompoundOperator chooses from with probability proportional to its weight", new ArrayList<>(), Validate.REQUIRED);
	
    private List<Operator> operators = new ArrayList<>();

    /** last operator used -- record the last choice for parameter tuning **/
    public Operator lastOperator;

    /**
     * cumulative weights, with unity as max value *
     */
    private double[] cumulativeProbs;
    
    
	@Override
	public void initAndValidate() {
		operators = operatorInput.get();

		// collect weights
		cumulativeProbs = new double[operators.size()];
		cumulativeProbs[0] = operators.get(0).getWeight();
		for (int i = 1; i < operators.size(); i++) {
			cumulativeProbs[i] = operators.get(i).getWeight() + cumulativeProbs[i-1];
		}
		// normalise
		for (int i = 0; i < operators.size(); i++) {
			cumulativeProbs[i] /= cumulativeProbs[operators.size() - 1];
		}
	}

	@Override
	public double proposal() {
        final int operatorIndex = Randomizer.randomChoice(cumulativeProbs);
        lastOperator = operators.get(operatorIndex);
		return lastOperator.proposal();
	}
	
	
	@Override
	public void optimize(double logAlpha) {
		lastOperator.optimize(logAlpha);
	}
	
	@Override
	public void accept() {
		lastOperator.accept();
	}
	
	@Override
	public void reject() {
		lastOperator.reject();
	}
	
	@Override
	public List<StateNode> listStateNodes() {
		List<StateNode> list = new ArrayList<StateNode>();
		for (Operator operator : operators) {
			List<StateNode> list2 = operator.listStateNodes();
			list.addAll(list2);
			
		}
		return list;
	}

	@Override
	public void storeToFile(PrintWriter out) {
		out.print("{\"id\":\"" + getID() + "\",\"operators\":[\n");
		int k = 0;
		for (Operator o : operators) {
			o.storeToFile(out);
            if (k++ < operators.size() - 1) {
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
            	for (Operator operator: operators) {
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
    	for (Operator operator: operators) {
    		if (operator.getID() == null) {
        		Log.warning.println("Operator (" + operator.getClass() + ") found in BEAST file that could not be restored because it has not ID");
    		}
    	}
        } catch (JSONException e) {
        	// it is not a JSON file -- probably a version 2.0.X state file
	    }
	}
	
	
	@Override
	public void setOperatorSchedule(final OperatorSchedule operatorSchedule) {
        for (Operator o : operators) {
        	o.setOperatorSchedule(operatorSchedule);
        }
    }


}
