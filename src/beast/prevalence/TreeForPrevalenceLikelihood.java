package beast.prevalence;

import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.Input.Validate;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.prevalence.PrevalenceList.Action;
import beast.prevalence.PrevalenceList.Item;



@Description("likelihood of a tree conditioned on a prevalence sequence")
public class TreeForPrevalenceLikelihood extends Distribution {
	public Input<Double> m_initialRootTime = new Input<Double>("initialTime", "initial start time for prevalence curve, defaults to root height times 2");
	public Input<PrevalenceList> m_list = new Input<PrevalenceList>("list", "prevalence list representing infection/recovery times", Validate.REQUIRED);
	public Input<Tree> m_tree = new Input<Tree>("tree","beast tree associated with list", Validate.REQUIRED);
	
	@Override
	public void initAndValidate() {
		// initialise m_list based on info in m_tree
		Tree tree = m_tree.get();
		Double t0 = m_initialRootTime.get();
		if (t0 == null) {
			t0 = 2 * tree.getRoot().getHeight();
		}
		PrevalenceList list = m_list.get();
		// add root prevalence
		list.add(t0, PrevalenceList.Action.INFECTED, -1);
		// add internal nodes with infections to list
		addInfections(tree.getRoot(), list);
	}
	
	void addInfections(Node node, PrevalenceList list) {
		if (!node.isLeaf()) {
			list.add(node.getHeight(), PrevalenceList.Action.INFECTED, node.getNr());
			addInfections(node.getLeft(), list);
			addInfections(node.getRight(), list);
		}
	}

	@Override
	public double calculateLogP() {
        PrevalenceList list = m_list.get();
        List<Item> items = list.getItems();
        
        logP = 0.0;
        
        Item start, finish;
        // keep track of number infected
        int ninf = 1;
        // keep track of number of lineages in tree
        int nlin = 1;

                        
        // RRB: should this be "i >= 1" ???
        // DW: Yes
        for (int i= items.size()-1; i >= 1; i--) {
                start = items.get(i);
                finish = items.get(i-1);
        
                // RRB: code dies here due to division by zero at first Action.INFECTED, since ninf=1
                // DW:  OK, had the block of code updating number of infected after the LogPTree stuff rather than before, have moved it up
                
            // update number of infected
            switch  (finish.m_action) {
                case RECOVERED:
                    // finishes with a recovery
                    ninf--;
                    break;
                case INFECTED:
                    // finishes with an infection
                    ninf++;
                    break;
                case NONEVENT:
                    // no contribution from non-event
                    break;
            }
        
            // contribution to LogPTree only if interval started with an infection
            if (start.m_action == Action.INFECTED) {
                if (list.isLinked(i)){
                    // have a coalescence, add a lineage
                        nlin++;
                        // RRB: code dies here due to division by zero at first Action.INFECTED, since ninf=1
                    logP = logP + Math.log(2.0/(ninf*(ninf-1.0)));
                } else {
                        // need have at least 2 lineages
                    if (nlin >1) {
                        logP = logP + Math.log(1.0 -(nlin*(nlin-1.0))/(ninf*(ninf-1.0)));
                    }
                }
            } else if (start.m_action == Action.NONEVENT && list.isLinked(i)){
                // have a leaf node, lose a lineage
                nlin--;
            }

            if (logP == Double.NEGATIVE_INFINITY) {
            	int h = 5;
            	h++;
            }
        
            // check that number lineages is legal (need at least as many infected as there are lineages)
            if (nlin == 0 || ninf < nlin){
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
