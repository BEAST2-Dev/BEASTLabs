package beastlabs.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.Distribution;
import beast.base.inference.Logger;
import beast.base.inference.State;
import beast.base.inference.StateNode;

/**
 * 
 * @author jdou557
 *
 */
@Description("optimise by grid search")
public class GridSearch extends beast.base.inference.Runnable {

	
	 

	final public Input<Distribution> posteriorInput = new Input<>("distribution", "probability distribution to sample over (e.g. a posterior)", Input.Validate.REQUIRED);
 	final public Input<Integer> nsamplesinput =  new Input<Integer>("nsamples", "Number of samples to report at the end", 1000);
 	final public Input<Integer> npointsInput =  new Input<Integer>("points", "Number of grid points for each parameter", 100);
 
 	final public Input<List<Logger>> m_loggers = new Input<List<Logger>>("logger", "loggers for reporting the posterior at the end of the grid search", 
		 	new ArrayList<Logger>(), Input.Validate.REQUIRED);
 	
 	final public Input<State> startStateInput = new Input<>("state", "elements of the state space", Input.Validate.REQUIRED);
 	
 	
 	
 	protected State state;
 	Distribution posterior;
 	int nsamples;
 	
 	// Current multidimensional index within the grid
 	int[] gridIndex;
 	int[] npoints;
 	
 	// Current state node index that is being searched over
 	int stateIndex;
 	
 
	@Override
	public void initAndValidate() {
		
		System.out.println("======================================================");
        System.out.println("Please cite the following when publishing this model:\n");
        System.out.println(getCitations());
        System.out.println("======================================================");
		
        // Inputs
		this.posterior = posteriorInput.get();
		this.nsamples = nsamplesinput.get();
	
        // State initialisation
        this.state = startStateInput.get();
        this.state.m_storeEvery.setValue(0, this.state);
        this.state.initialise();
        this.state.setPosterior(this.posterior);

        
        // Count the parameters / trees in the state
        if (this.state.getNrOfStateNodes() == 0) {
        	throw new IllegalArgumentException("Please provide at least 1 state node for the grid search");
        }
       	this.gridIndex = new int[this.state.getNrOfStateNodes()];
       	this.stateIndex = this.gridIndex.length - 1;
       	
       	
       	// How many points per state node?
       	for (int i = 0; i < this.state.getNrOfStateNodes(); i ++) {
       		gridIndex[i] = 0;
       		StateNode node = this.state.getStateNode(i);
       		
       		// Trees have 1 point per topology
       		if (node instanceof TreeInterface) {
       			npoints[i] = 100;
       		}else {
       			npoints[i] = npointsInput.get();
       		}
       	}
        
        
	}
	
	
	
	
	
	@Override
	public void run() throws Exception {
		
		
		// Iterate through the grid
		while (this.hasNext()) {
			
			StateNode currentState = this.state.getStateNode(this.stateIndex);
			int currentStateGridPosition = this.gridIndex[this.stateIndex];
			
			// Reset this element and 
			//if (currentStateGridPosition == this.npoints-1) {
				
			//}else {
				
				
				
			//}
			
			
			
			
			
			
		}
		
		
		
		
	}
	
	
	
	/**
	 * Moves to the next position in the grid
	 */
	private void next() {
		
		for (int i = 0; i < this.gridIndex.length; i ++) {
			if (this.gridIndex[i] == this.npoints[i]) {
				
				// Reset this state node and all child nodes
				for (int j = i; j < this.gridIndex.length; j ++) {
					
					
				}
				
				// Increment its parent by 1
				if (i > 0) {
					
				}
				
				
			}
		}
		
	}
	
	/**
	 * 
	 * @return if there is another position in the grid
	 */
	private boolean hasNext() {
		if (this.gridIndex[0] == this.npoints[0]) return false;
		return true;
		
	}

	
	
}
