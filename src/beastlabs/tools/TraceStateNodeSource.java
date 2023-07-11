package beastlabs.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.inference.parameter.Parameter;
import beastfx.app.tools.LogAnalyser;
import beastfx.app.util.LogFile;


public class TraceStateNodeSource extends BEASTObject implements StateNodeSource {
	final public Input<LogFile> traceInput = new Input<>("trace", "Trace logs containing parameter settings. "
			+ "First trace log should contain posterior.", new LogFile("[[none]]"));
	final public Input<String> srcIdInput = new Input<>("value", "comma delimited string of map enties. "
			+ "Each entry maps a trace label to a state node entry. Use square brackets to indicate the dimension. "
			+ "For example kappa.s:dna=kappa, freq.s:dna[0]=freq.A, freq.s:dna[1]=freq.C");
    final public Input<State> stateInput = new Input<>("state", "elements of the state space");
	
	LogAnalyser tracelog;
	private Map<String, Integer> mapTraceLabelToStateNodeID;
	private Map<String, Integer> mapTraceLabelToStateNodeDimension;
	private List<String> labels;
	
	@Override
	public void initAndValidate() {
		try {
			tracelog = new LogAnalyser(traceInput.get().getPath());
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		
		Map<String, Integer> mapStateNodeIDtoStateNodeNr;
		mapStateNodeIDtoStateNodeNr = new HashMap<>();
		List<StateNode> sns = stateInput.get().stateNodeInput.get();
		for (int i = 0; i < sns.size(); i++) {
			mapStateNodeIDtoStateNodeNr.put(sns.get(i).getID(), i);
		}
		
		
		labels = tracelog.getLabels();
		mapTraceLabelToStateNodeID = new HashMap<>();
		mapTraceLabelToStateNodeDimension = new HashMap<>();
		String [] strs = srcIdInput.get().split(",");
		for (String str : strs) {
			int k = str.indexOf('=');
			if (k < 0) {
				throw new IllegalArgumentException("map should be of the form `id=value`, but could not find `=` in " + str);
			}
			String to = str.substring(0, k).trim();
			String from = str.substring(k+1).trim();
			int dimension = 0;
			if (to.contains("[")) {
				int j = to.indexOf('[');
				String dim = to.substring(j+1, to.indexOf(']'));
				dimension = Integer.valueOf(dim);
				to = to.substring(0, j);
			}
			
			if (!labels.contains(from)) {
				throw new IllegalArgumentException("Could not find label " + from + " in trace " + traceInput.get().getName());
			}
			
			if (!mapStateNodeIDtoStateNodeNr.keySet().contains(to)) {
				throw new IllegalArgumentException("Could not find statenode with id " + to);
			}
			int stateNodeNr = mapStateNodeIDtoStateNodeNr.get(to);
			mapTraceLabelToStateNodeID.put(from, stateNodeNr);
			mapTraceLabelToStateNodeDimension.put(from, dimension);
		}
	}	
	
	@Override
	public void initStateNodes(int i) {		
		List<StateNode> sns = stateInput.get().stateNodeInput.get();
		for (String label : labels) {
			if (mapTraceLabelToStateNodeID.containsKey(label)) {
				int snNr = mapTraceLabelToStateNodeID.get(label);
				int dim = mapTraceLabelToStateNodeDimension.get(label);
				StateNode sn = sns.get(snNr);
				if (sn instanceof Parameter) {
					Parameter p = (Parameter) sn;
					p.setValue(dim, tracelog.getTrace(label)[i]);
				} else {
					throw new IllegalArgumentException("don't know how to initialise non-parameter statenode " + sn.getID());
				}
			}
		}		
	}
	
}
