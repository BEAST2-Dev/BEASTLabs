package beast.evolution.tree;

import java.util.*;

import beast.core.BEASTObject;
import beast.core.Input;
import beast.core.util.Log;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class RobinsonsFouldMetric extends BEASTObject implements TreeMetric {

	final public Input<TaxonSet> taxonInput = new Input<>("taxonset", "taxonset of the trees", Input.Validate.REQUIRED);
	final public Input<Boolean> recursiveInput = new Input<>("recursive", "whether to recurse down the taxon set and take only 'taxon' objects", false);
		
	Map<String, Integer> taxonMap = null;
	TreeInterface referenceTree = null;
	Set<String> referenceClades = null;
	
	public RobinsonsFouldMetric() {
		this.taxonMap = null;
	}
	
	
	public RobinsonsFouldMetric(TaxonSet taxonset) {
		this.taxonMap = new HashMap<>();
		this.setTaxonMap(taxonset, 0);
	}
	
	
	public RobinsonsFouldMetric(String [] taxaNames) {
		if (taxaNames == null) return;
		this.taxonMap = new HashMap<>();

		// Create taxon to int mapping
		int i = 0;
		for (String taxon : taxaNames) {
			taxonMap.put(taxon, i);
			i++;
		}
	}


	private int setTaxonMap(TaxonSet taxonset, int i) {
		
		if (taxonset == null) return i;

		// Create taxon to int mapping
		for (String taxon : taxonset.getTaxaNames()) {
			if (recursiveInput.get() && taxonset.getTaxon(taxon) instanceof TaxonSet) {
				i = setTaxonMap((TaxonSet)taxonset.getTaxon(taxon), i);
			}else {
				taxonMap.put(taxon, i);
				i++;
			}
		}
		
		return i;
	}
	
	public Map<String, Integer> getTaxonMap(){
		return this.taxonMap;
	}


	@Override
	public void initAndValidate() {
		
		TaxonSet taxonset = taxonInput.get();
		this.taxonMap = new HashMap<>();
		setTaxonMap(taxonset, 0);
		
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