package beast.evolution.operators;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import beast.core.Input;
import beast.core.Operator;
import beast.core.OperatorSchedule;
import beast.core.StateNode;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;


/**
 * 
 * @author Jordan Douglas
 * An operator which selects samples from a series of other operators, with respect to their ability to explore one or more real parameters 
 * Training for each operator occurs following a burnin period
 * After training, AdaptableOperatorSampler should pick the operator which is giving the best results n a particular dataset
 */
public class AdaptableOperatorSampler extends Operator {
	
	

    final public Input<List<RealParameter>> paramInput = new Input<>("parameter", "list of parameters to compare before and after the proposal. If the tree heights"
    		+ " are a parameter then include the tree under 'tree'", new ArrayList<RealParameter>());
    final public Input<Tree> treeInput = new Input<>("tree", "tree containing node heights to compare before and after the proposal (optional)");
    final public Input<List<Operator>> operatorsInput = new Input<>("operator", "list of operators to select from", new ArrayList<Operator>());
    
    final public Input<Integer> burninInput = new Input<>("burnin", "number of operator calls until the learning process begins (default: 500 x number of operators)");
    final public Input<Integer> learninInput = new Input<>("learnin", "number of operator calls between learning begind (ie. at burnin) and before this operator starts to use what it has learned"
    		+ " (default: 1000 x number of operators)");
	
    
    final boolean DEBUG = false;
    
    // The score added onto the weight of each operator to prevent operators having weights of 0
    final double FUDGE_FACTOR = 0.1;
    
    
    List<RealParameter> parameters;
    Tree tree;
    List<Operator> operators;
    int burnin;
    int learnin;
    int numParams;
    int numOps;
    

    // Number of times this meta-operator has been called
    int nProposals;
    
    // Learning begins after burnin
    boolean learningHasBegun;
    
    // Application of the learned terms to sample operators begins after learnin
    boolean teachingHasBegun;
    
    
    // The last operator which was called
    int lastOperator;
    
    // The parameter values from before the current proposal was made
    double[][] stateBefore;
    
    
    // Sum-of-squares of each parameter across before and after each operator was accepted
    double[][] SS;
    
    
    // Cumulative sum of each parameter -> for calculating mean
    double[] param_sum = null;
    
    // Cumulative sum of squares of each parameter -> for calculating variance
    double[] param_SS = null;
    
    
    // Number of times each operator has been called
    int[] numProposals = null;
    
    // Number of times each operator has been accepted
    int[] numAccepts = null;
    

    

	@Override
	public void initAndValidate() {

		// Operators
		this.operators = operatorsInput.get();
		this.numOps = this.operators.size();
		
		// Parameters/tree
		this.tree = treeInput.get();
		this.parameters = paramInput.get();
		this.numParams = this.parameters.size() + (this.tree == null ? 0 : 1);
		
		// Burnin
		if (burninInput.get() == null) {
			this.burnin = 500 * this.numOps;
		}else {
			this.burnin = Math.max(0, burninInput.get());
		}
		
		// Learnin
		if (learninInput.get() == null) {
			this.learnin = 1000 * this.numOps;
		}else {
			this.learnin = Math.max(this.burnin, learninInput.get());
		}
		
		
		
		this.nProposals = 0;
		this.learningHasBegun = this.burnin == 0;
		this.teachingHasBegun = this.learnin == 0;
		this.lastOperator = -1;
		
		
		// Validate
		if (this.numOps < 2) {
			throw new IllegalArgumentException("Please provide at least two operators");
		}
		if (this.numParams == 0) {
			Log.warning("Warning: at least one sampled parameter or a tree should be provided to assist in measuring the efficiency of each operator.");
		}
		

		
		// Learned num proposals and accepts of all operators
		this.numProposals = new int[this.numOps];
		this.numAccepts = new int[this.numOps];
		
		for (int i = 0; i < this.numOps; i ++) {
			this.numProposals[i] = 0;
			this.numAccepts[i] = 0;
		}


		if (this.numParams > 0) {
			

			// Sum-of-squares of each difference before and after each proposal
			this.SS = new double[this.numOps][]; 
			
			for (int i = 0; i < this.numOps; i ++) {
				
				this.SS[i] = new double[this.numParams];
				
				for (int p = 0; p < this.numParams; p ++) {
					this.SS[i][p] = 0;
				}
			}
			
			
			// Cumulative sum and SS of each parameter in each state
			this.param_sum = new double[this.numParams];
			this.param_SS = new double[this.numParams];
			for (int p = 0; p < this.numParams; p ++) {
				this.param_sum[p] = 0;
				this.param_SS[p] = 0;
			}
			
		}
		

		

		
	}

	@Override
	public double proposal() {
		

		
		// Get the values of each parameter before doing the proposal
		this.stateBefore = this.getAllParameterValues();

		
		// Update sum and SS of each parameter before making the proposal
		this.updateParamStats(this.stateBefore);
		

		// Sample an operator
		double[] operatorCumulativeProbs = this.getOperatorCumulativeProbs();
		this.lastOperator = Randomizer.binarySearchSampling(operatorCumulativeProbs);
		Operator operator = this.operators.get(this.lastOperator);
		
		// Increment the number of proposals
		this.nProposals ++;
		if (this.learningHasBegun) this.numProposals[this.lastOperator] ++;
		if (this.nProposals >= this.burnin && !this.learningHasBegun) {
			if (DEBUG) Log.warning("Burnin has been achieved. Beginning learning...");
			this.learningHasBegun = true;
		}
		if (this.nProposals >= this.learnin && !this.teachingHasBegun) {
			if (DEBUG) Log.warning("Learnn has been achieved. Applying the learning now...");
			this.teachingHasBegun = true;
		}

		// Do the proposal. If it gets accepted then the differences between the two states will be calculated afterwards
		return operator.proposal();
	}
	
	
	private void updateParamStats(double[][] thisState) {
		
		if (this.learningHasBegun && this.numParams > 0) {
			

			// Update the sum and the sum-of-squares each parameter
			for (int p = 0; p < this.numParams; p ++) {
				
				// If parameter is multidimensional, take the mean 
				double val = 0;
				for (int j = 0; j < thisState[p].length; j++) {
					val += thisState[p][j] / thisState[p].length;
				}

				this.param_sum[p] += val;
				this.param_SS[p] += val*val;
		
			}
		
		}
		
	}

	/**
	 * 
	 * @return A list of cumulative probabilites for sampling each operator
	 */
	public double[] getOperatorCumulativeProbs(){
		
		double[] operatorWeights = new double[this.numOps];
		
		
		if (this.teachingHasBegun) {
			
			// If past burn-in, then sample from acceptance x squared-diff
			for (int i = 0; i < this.numOps; i ++) {
				
				//Operator op = this.operators.get(i);
				double acceptanceProb = 1.0 * this.numAccepts[i] / this.numProposals[i];
				double hScore = 0;
				
				// Calculate h for each parameter with respect to this operator
				for (int p = 0; p < this.numParams; p ++) {
					
					// p = numParams - 1 is the tree heights, all others are RealParameters 
					hScore += this.getZ(i, p) / this.numParams;
					
				}
				
				operatorWeights[i] = acceptanceProb * hScore + FUDGE_FACTOR;
				
				
				if (DEBUG) Log.warning("Operator " + i + " has acceptance prob of " + acceptanceProb + " and an hscore of " + hScore);
				
			}
			
			
		} else {
			
			// If still in burn-in, then sample uniformly at random
			for (int i = 0; i < this.operators.size(); i ++) {
				operatorWeights[i] = 1;
			}
			
		}
		
		
		// Weight sum
		double weightSum = 0;
		for (int i = 0; i < this.numOps; i ++) weightSum += operatorWeights[i];
		
		
		if (weightSum == 0) {
			
			// If the weight sum is zero, then sample uniformly at random
			for (int i = 0; i < this.numOps; i ++) operatorWeights[i] = 1.0 / this.numOps;
		}else {
			
			// Otherwise normalise weights into probabilities
			for (int i = 0; i < this.numOps; i ++) operatorWeights[i] /= weightSum;
		}
		
		// Convert to cumulative probability array
		double cumProb = 0;
		for (int i = 0; i < this.numOps; i ++) {
			cumProb += operatorWeights[i];
			operatorWeights[i] = cumProb;
		}
		
		
		return operatorWeights;
	}
	

	
	/**
	 * Get all parameter values in the current state
	 * @return
	 */
	private double[][] getAllParameterValues() {
		
		
		if (this.numParams == 0) return null;
		
		double[][] vals = new double[this.numParams][];
		for (int p = 0; p < this.numParams; p ++) {
			
			// Get the values of this parameter
			double[] p_vals;
			if (tree != null && p == this.numParams - 1 ) {
				
				// The parameter is the tree heights
				p_vals = new double[tree.getNodeCount()];
				for (int nodeNum = 0; nodeNum < tree.getNodeCount(); nodeNum++) {
					p_vals[nodeNum] = tree.getNode(nodeNum).getHeight();
				}
			}else {
				
				// A RealParameter
				p_vals = this.parameters.get(p).getDoubleValues();
			}
			
			
			vals[p] = p_vals;
			
		}

		return vals;
		
		
	}
	
	
	@Override
	public void accept() {
		

		
		// Update trained terms from the accept
		if (learningHasBegun) {
			
			// Update the num accepts of this operator
			this.numAccepts[this.lastOperator] ++;	
			
			if (this.numParams > 0) {
			
				// Get the values of each parameter after doing the proposal
				double[][] stateAfter = this.getAllParameterValues();
	
				// Compute the average squared difference between the before and after states
				double[] squaredDiffs = this.computeSS(this.stateBefore, stateAfter);
				
				// Update the sum of squared diffs for each parameter with respect to this operator
				for (int p = 0; p < this.numParams; p ++) {
					SS[this.lastOperator][p] += squaredDiffs[p];
				}
			}
		
		}
		
		this.operators.get(this.lastOperator).accept();
		super.accept();
	
	}
	
	
	/**
	 * Return the sum of squares of the difference within each parameter before and after the proposal was accepted
	 * @param before
	 * @param after
	 */
	private double[] computeSS(double[][] before, double[][] after) {
		
		double[] squaredDiff = new double[this.numParams];
		
		for (int p = 0; p < this.numParams; p ++) {
			
			double[] p_before = before[p];
			double[] p_after = after[p];
			
			
			// Average the squared difference across all dimensions of this parameter
			double meanDelta2 = 0;
			for (int j = 0; j < p_before.length; j ++) {
				meanDelta2 += Math.pow(p_before[j] - p_after[j], 2);
			}
			
			squaredDiff[p] = meanDelta2;
				
		}
		
		return squaredDiff;
		
	}

	@Override
	public void reject() {
		this.operators.get(this.lastOperator).reject();
		super.reject();
	}
	
	
    @Override
    public void optimize(double logAlpha) {
    	this.operators.get(this.lastOperator).optimize(logAlpha);
    }
	


    
    
    /**
     * Returns a variance using a cumulative sum, a cumulative sum of squared values, and the sample size n
     * @param sum
     * @param SS
     * @param n
     * @return
     */
    public double getVar(double sum, double SS, int n) {
    	double mean = sum/n;
    	return SS/n - mean*mean;
    }
    
    
    /**
     * Returns the normalised average squared-difference that this operator causes when applied to this parameter
     * This difference is normalised by dividing the mean squared-difference by the variance of the parameter 
     * This enables comparison between parameters which exist on different magnitudes
     * @param opNum
     * @param paramNum
     * @return
     */
    public double getZ(int opNum, int paramNum) {
    	
    	
    	// Contribution from the operator (ie. 1/n)
    	double opTerm = 1.0 / this.numAccepts[opNum];
    	
    	// Contribution from the parameter (ie. 1/variance)
    	double parTerm = 1.0 / this.getVar(this.param_sum[paramNum], this.param_SS[paramNum], this.nProposals);
    	
    	// Contribution from average squared-difference of applying this operator to this parameter
    	double opParTerm = this.SS[opNum][paramNum];
    	
    	
    	return opTerm * parTerm * opParTerm;
    	
    }
    
    @Override
    public void setOperatorSchedule(final OperatorSchedule operatorSchedule) {
    	super.setOperatorSchedule(operatorSchedule);
    	for (int i = 0; i < this.numOps; i ++) this.operators.get(i).setOperatorSchedule(operatorSchedule);
    }
    
    
    @Override
    public List<StateNode> listStateNodes() {
    	List<StateNode> stateNodes = super.listStateNodes();
    	for (int i = 0; i < this.numOps; i ++) {
    		stateNodes.addAll(this.operators.get(i).listStateNodes());
    	}
    	return stateNodes;
    }
    
    
    
    @Override
    public void storeToFile(final PrintWriter out) {
    	

    	
        
    	try {
	        JSONStringer json = new JSONStringer();
	        json.object();
	
	        if (getID() == null) setID("unknown");
	
	        // id
	        json.key("id").value(getID());
	
	        // N proposals
	        json.key("nProposals").value(this.nProposals);
	        
	        // Store parameter sum/SS
	        json.key("param_sum").value(Arrays.toString(this.param_sum));
	        json.key("param_SS").value(Arrays.toString(this.param_SS));
	        
	        
	        // Store accepts of each operator
	        json.key("numAccepts").value(Arrays.toString(this.numAccepts));
	        json.key("numProposals").value(Arrays.toString(this.numProposals));
	        
	        // Store SS for each operator-parameter combination
	        json.key("SS").value(Arrays.deepToString(this.SS));
	        
	        
	        
	        
	        // Store sub-operators
	        // TODO: get the strings formating properly
	        StringWriter outStr = new StringWriter();
	        PrintWriter writer = new PrintWriter(outStr);
	        
	        for (int i = 0; i < this.numOps; i ++) {
	        	this.operators.get(i).storeToFile(writer);
	        	writer.println(",");
	        }
	        writer.flush();
	        
	        //json.key("operators").value(outStr.toString());
	        
	        json.endObject();
	        out.print(json.toString());
	        

	        
    	} catch (JSONException e) {
    		// failed to log operator in state file
    		// report and continue
    		e.printStackTrace();
    	}
    }
    
    
    

    @Override
    public void restoreFromFile(JSONObject o) {

    	
    	// Load sub-operators first
    	// TODO load these back in
        for (int i = 0; i < this.numOps; i ++) {
        	//this.operators.get(i).restoreFromFile(o);
        }
    	
    	
    	try {
    		
    		
    		this.nProposals = Integer.parseInt(o.getString("param_sum"));
    		
    		
    		// Parameter sum and sum-of-squares
    		String[] param_sum_string = ((String) o.getString("param_sum")).replace("[", "").replace("]", "").split(", ");
	        String[] param_SS_string = ((String) o.getString("param_SS")).replace("[", "").replace("]", "").split(", ");
	        if (param_sum_string.length != this.numParams) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + param_sum_string.length + " elements in param_sum but " + this.numParams + " params");
	        }
	        if (param_SS_string.length != this.numParams) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + param_SS_string.length + " elements in param_SS but " + this.numParams + " params");
	        }
	        this.param_sum = new double[this.numParams];
	        this.param_SS = new double[this.numParams];
	        for (int p = 0; p < this.numParams; p++) {
	        	this.param_sum[p] = Double.parseDouble(param_sum_string[p]);
	        	this.param_SS[p] = Double.parseDouble(param_SS_string[p]);
	        }
	        
	        
	        // Operator proposals and accepts post-burnin
	        String[] numAccepts_string = ((String) o.getString("numAccepts")).replace("[", "").replace("]", "").split(", ");
	        String[] numProposals_string = ((String) o.getString("numProposals")).replace("[", "").replace("]", "").split(", ");
	        if (numAccepts_string.length != this.numOps) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + numAccepts_string.length + " elements in numAccepts but " + this.numOps + " operators");
	        }
	        if (numProposals_string.length != this.numOps) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + numProposals_string.length + " elements in numProposals but " + this.numOps + " operators");
	        }
	        this.numAccepts = new int[this.numOps];
	        this.numProposals = new int[this.numOps];
	        for (int i = 0; i < this.numOps; i++) {
	        	this.numAccepts[i] = Integer.parseInt(numAccepts_string[i]);
	        	this.numAccepts[i] = Integer.parseInt(numProposals_string[i]);
	        }
	        
	        
	        
	        // Sum of squares
	        String[] SS_string = ((String) o.getString("SS")).replace("[", "").replace("]", "").split(", ");
	        if (SS_string.length != this.numOps) {
	        	throw new IllegalArgumentException("Cannot resume because there are " + SS_string.length + " elements in SS but " + this.numOps + " operators");
	        }
	        this.SS = new double[this.numOps][];
	        for (int i = 0; i < this.numOps; i++) {
	        	String[] this_SS_string = SS_string[i].replace("[", "").replace("]", "").split(", ");
	        	if (this_SS_string.length != this.numParams) {
	 	        	throw new IllegalArgumentException("Cannot resume because there are " + SS_string.length + " elements in SS at position " + i + " but " + this.numParams + " parameters");
	 	        }
	        	this.SS[i] = new double[this.numParams];
	        	for (int p = 0; p < this.numParams; p++) {
		        	this.SS[i][p] = Double.parseDouble(this_SS_string[p]);
		        }
	        	 
	        }
	        
    		
	        super.restoreFromFile(o);  	
    	} catch (JSONException e) {
    		// failed to restore from state file
    		// report and continue
    		e.printStackTrace();
    	}
    }

    
    
    

}







