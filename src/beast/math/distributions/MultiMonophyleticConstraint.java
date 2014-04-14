package beast.math.distributions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

@Description("Prior over set of taxa, useful for defining multiple monophyletic constraints using a newick format")
public class MultiMonophyleticConstraint extends Distribution {
    public final Input<Tree> treeInput = new Input<Tree>("tree", "the tree containing the taxon set", Validate.REQUIRED);
    //public final Input<TaxonSet> taxonsetInput = new Input<TaxonSet>("taxonset", "set of taxa for which prior information is available");
    public final Input<String> newickInput = new Input<String>("newick", "the tree constraints specified as newick tree using polytopes, "
	    		+ "e.g. ((human,chimp,bonobo),gorilla) specifies two monophyletic constraints,"
	    		+ "one for human,chimp,bonobo' and one for 'human,chimp,bonobo,gorilla'", Validate.REQUIRED);

    
    /** Constraints are encoded as a list of taxon numbers for each constraint
     * TaxonIDList contains a list of constraints
     */
    List<List<Integer>> taxonIDList;

    Tree tree;
    String[] taxaList;
    
    @Override
    public void initAndValidate() throws Exception {
    	taxonIDList = new ArrayList<List<Integer>>();
    	tree = treeInput.get();
    	taxaList = tree.getTaxaNames();

    	parse(newickInput.get());
    }
    
	/** extract clades from Newick string,
	 * and add constraints for all internal nodes (except the root if it contains all taxa)
	 **/
	private void parse(String newick) {
		// get rid of initial and trailing spaces
		newick = newick.trim();
		// remove comments
		newick = newick.replaceAll(".*\\[[^\\]]*\\].*", "");
		// remove branch lengths
		newick = newick.replaceAll(":[^,\\(\\)]*", "");
		Pattern pattern = Pattern.compile("\\(([^\\(\\)]*)\\)");
		Matcher m = pattern.matcher(newick);
		
		//List<String> taxaList = taxonsetInput.get().asStringList();
		while (m.find()) {
			String group = m.group();
			String [] taxa = group.substring(1,group.length()-1).split(",");
			List<Integer> list = new ArrayList<Integer>();
			for (String taxon : taxa) {
				taxon = taxon.trim();
				if (taxon.length() > 0) {
					int i = indexOf(taxon);
					if (i == -1 && (taxon.startsWith("'") || taxon.startsWith("\""))) {
						i = indexOf(taxon.substring(1, taxon.length() - 1));
						if (i == -1) {
							throw new RuntimeException("Cannot find taxon " + taxon + "  in taxon list");
						}
					}
					list.add(i);
				}
			}
			if (list.size() < tree.getLeafNodeCount()) {
				taxonIDList.add(list);
				Log.info.println("Constraining " + group);// + " " + Arrays.toString(list.toArray()));			
			}
			newick = newick.replaceFirst("\\(([^\\(\\)]*)\\)", ",$1,");
			newick = newick.replaceAll("([\\(,]),", "$1");
			newick = newick.replaceAll(",\\)", ")");
			m = pattern.matcher(newick);
		}
	}


    private int indexOf(String taxon) {
		for (int k = 0; k < taxaList.length; k++) {
			if (taxon.equals(taxaList[k])) {
				return k;
			}
		}
		return -1;
	}

	@Override
    public double calculateLogP() throws Exception {
        logP = 0;
        for (List<Integer> list : taxonIDList) {
        	if (!isMonophyletic(list)) {
        		logP = Double.NEGATIVE_INFINITY;
        		return logP;
        	}
        }
        return logP;
    }


    /** walk up the tree from the nodes, to see whether a clade is monophyletic
     * After initialising steps from each of the child nodes,
     * a step is made from any internal nodes if there are two 
     * paths to an internal node. Iff the clade is monophyletic, 
     * this process halts after n-1 steps where n is the nr of taxa
     * in the clade.
     * NB assumes that the tree is binary **/
	private boolean isMonophyletic(List<Integer> list) {
		int [] parentcount = new int[tree.getNodeCount()];
		Node [] nodes = tree.getNodesAsArray();
		
		// mark parents of leaf nodes
		Set<Integer> list2 = new HashSet<Integer>();
		int k = 0;
		for (int i : list) {
			int parentNr = nodes[i].getParent().getNr();
			parentcount[parentNr]++;
			if (parentcount[parentNr] == 2) {
				list2.add(parentNr);
				k++;
			}
		}
		// mark parents of nodes that have two children marked
		while (list2.size() > 0) {
			Set<Integer> list3 = new HashSet<Integer>();
			for (int i : list2) {
				int parentNr = nodes[i].getParent().getNr();
				parentcount[parentNr]++;
				if (parentcount[parentNr] == 2) {
					list3.add(parentNr);
					k++;
				}
			}
			list2 = list3;
		}
		if (k == list.size() - 1){
			return true;
		}
		return false;
	}


	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}
	@Override public void sample(State state, Random random) {}
}
