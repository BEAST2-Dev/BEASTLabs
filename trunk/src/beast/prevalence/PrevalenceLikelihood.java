package beast.prevalence;


import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.Input.Validate;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
//import beast.evolution.tree.coalescent.TreeIntervals;
import beast.prevalence.PrevalenceList.Item;

@Description("likelihood of the prevalence sequence based on a set of parameters")
public class PrevalenceLikelihood extends Distribution {
	public Input<RealParameter> m_beta = new Input<RealParameter>("beta", "parameter for x y z", Validate.REQUIRED);
	public Input<RealParameter> m_gamma = new Input<RealParameter>("gamma", "parameter for x y z", Validate.REQUIRED);
	public Input<IntegerParameter> m_popSize = new Input<IntegerParameter>("popSize", "parameter for x y z", Validate.REQUIRED);
	public Input<PrevalenceList> m_list = new Input<PrevalenceList>("list", "prevalence list representing infection/recovery times", Validate.REQUIRED);
	//public Input<TreeIntervals> m_intervals = new Input<TreeIntervals>("intervals", "tree intervals for beast.tree", Validate.REQUIRED);
	
	@Override
	public void initAndValidate() {
	}
	
	@Override
	public double calculateLogP() {
        Double logP = 0.0;
        PrevalenceList list = m_list.get();
        List<Item> items = list.getItems();
        double beta = m_beta.get().getValue();
        double gamma = m_gamma.get().getValue();
        // RRB: N is population size? Is population size an int?
        // DW: yes, N is popSize and is a positive integer
        int N = m_popSize.get().getValue();
                
        Item start, finish;
        // keep track of number infected
        int ninf = 1;
                        
        // RRB: should this be "i >= 1" ???
        // DW: Oh yes, I forgot item(0) is the first in the list
        for (int i= items.size()-1; i >= 1; i--) {
                start = items.get(i);
                finish = items.get(i-1);
            // add contribution to likelihood of time spent in this period
            logP = logP -  (start.m_fTime - finish.m_fTime)*(beta*ninf*(N-ninf)/N + gamma*ninf);
            // check how current time period ends
            switch  (finish.m_action) {
                case RECOVERED:
                    // finishes with a recovery
                    logP = logP + Math.log(gamma*ninf);
                    ninf--;
                    break;
                case INFECTED:
                    // finishes with an infection
                        logP = logP + Math.log(beta*ninf*(N-ninf)/N);
                        ninf++;
                        break;
                case NONEVENT:
                        // no contribution from non-event
                        break;
            }
                                                
            // Check that the number infected is legal
            // RRB: so the number of infected cannot be zero anywhere in the list?
            // DW: That's right
            if (ninf < 1 || ninf > N){
                logP = Double.NEGATIVE_INFINITY;
                return logP;
            }
        }
        return logP;
	}
	

	
	/** nonsense, feel free to ignore **/
	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}
	@Override public void sample(State state, Random random) {}

}
