package beast.evolution.tree;

import java.util.*;

import beast.core.BEASTObject;
import beast.core.Input;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class RobinsonsFouldMetric extends BEASTObject implements TreeMetric {

	final public Input<TaxonSet> taxonInput = new Input<>("taxonset", "taxonset of the trees", Input.Validate.REQUIRED);
		
	Map<String, Integer> taxonMap = null;
	TreeInterface referenceTree = null;
	Set<String> referenceClades = null;
	
	public RobinsonsFouldMetric() {
		
	}
	
	
	public RobinsonsFouldMetric(TaxonSet taxonset) {
		taxonMap = new HashMap<>();

		// Create taxon to int mapping
		int i = 0;
		for (String taxon : taxonset.getTaxaNames()) {
			taxonMap.put(taxon, i);
			i++;
		}
	}


	@Override
	public void initAndValidate() {
		
		TaxonSet taxonset = taxonInput.get();
		taxonMap = new HashMap<>();

		// Create taxon to int mapping
		int i = 0;
		for (String taxon : taxonset.getTaxaNames()) {
			taxonMap.put(taxon, i);
			i++;
		}
		
	}

	@Override
	public double distance(TreeInterface tree1, TreeInterface tree2) {


		Set<String> clades1 = getClades(tree1);
		Set<String> clades2 = getClades(tree2);

		clades1.removeAll(clades2);

		return clades1.size();
	}
	
	
	@Override
	public double distance(TreeInterface tree) {
		if (this.referenceClades == null) throw new IllegalArgumentException("Developer error: please provide a reference tree using 'setReference' or use 'distance(t1, t2)' instead");
		Set<String> clades = getClades(tree);
		clades.removeAll(this.referenceClades);
		return clades.size();
	}


	private Set<String> getClades(TreeInterface tree) {

		Set<String> clades = new HashSet<>();
		getTips(tree, tree.getRoot(), clades);
		return clades;
	}

	private Set<Integer> getTips(TreeInterface tree, Node node, Set<String> clades) {


		Set<Integer> tips = new TreeSet<>();
		
		// A labelled taxon
		if (this.taxonMap.containsKey(node.getID())) {
			tips.add(this.taxonMap.get(node.getID()));
		}

		
		// An internal node
		for (Node child : node.getChildren()) {
			Set<Integer> tipsChild = getTips(tree, child, clades);
			tips.addAll(tipsChild);
		}
			
		

		clades.add(getCladeString(tips));
	
		return tips;
	}

	private static String getCladeString(Set<Integer> tips) {
		Iterator<Integer> iter = tips.iterator();
		if (!iter.hasNext()) return "";
		
		StringBuffer buffer = new StringBuffer();
		buffer.append(iter.next());
		while (iter.hasNext()) {
			buffer.append(",");
			buffer.append(iter.next());
		}
		return buffer.toString();
	}




	@Override
	public void setReference(TreeInterface ref) {
		this.referenceTree = ref;
		this.referenceClades = getClades(ref);
	}






}