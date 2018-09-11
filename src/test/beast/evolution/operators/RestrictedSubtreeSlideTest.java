package test.beast.evolution.operators;

import org.junit.Test;

import beast.core.State;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.operators.RestrictedSubtreeSlide;
import beast.evolution.tree.Node;
import beast.util.TreeParser;
import junit.framework.TestCase;


public class RestrictedSubtreeSlideTest extends TestCase {
	
	boolean isMonophyletic = false;
	
	@Test
	public void testRestrictedSubtreeSlide() throws Exception {
	    TaxonSet clade = new TaxonSet();
	    clade.initByName("taxon", new Taxon("C"), "taxon", new Taxon("D"));
	    testRestrictedSubtreeSlide(clade, null);
	}
	
	@Test
	public void testRestrictedSubtreeSlide2() throws Exception {
	    TaxonSet clade = new TaxonSet();
	    clade.initByName("taxon", new Taxon("C"), "taxon", new Taxon("D"), "taxon", new Taxon("E"));
	    testRestrictedSubtreeSlide(clade, null);
	}

	@Test
	public void testRestrictedSubtreeSlide3() throws Exception {
	    TaxonSet clade = new TaxonSet();
	    clade.initByName("taxon", new Taxon("A"), "taxon", new Taxon("B"));
	    testRestrictedSubtreeSlide(clade, null);
	}
	
	@Test
	public void testRestrictedSubtreeSlide4() throws Exception {
	    TaxonSet clade = new TaxonSet();
	    clade.initByName("taxon", new Taxon("A"), "taxon", new Taxon("B"));
	    TaxonSet clade2 = new TaxonSet();
	    clade2.initByName("taxon", new Taxon("C"), "taxon", new Taxon("D"));
	    testRestrictedSubtreeSlide(clade, clade2);
	}

	
	public void testRestrictedSubtreeSlide(TaxonSet clade, TaxonSet clade2) throws Exception {
		Alignment data = getData();

        String sourceTree = "(((A:5.0,B:5.0):2.0,((C:5.0,D:5.0):1.0,E:6.0):1.0):1.0,F:8.0):0.0"; 

	    TreeParser tree = new TreeParser();
	    tree.initByName("taxa", data, "newick", sourceTree, "IsLabelledNewick", true, "IsLabelledNewick", true);
		State state = new State();
		state.initByName("stateNode", tree);
		state.initialise();
	    
	    
	    String [] sTaxaNames = tree.getTaxaNames();
	    
	    boolean [] isInTaxaSet = new boolean[tree.getLeafNodeCount()];
	    int nrOfTaxa = 0;
        for (final Taxon taxon : clade.taxonsetInput.get()) {
        	String sTaxon = taxon.getID();
        	for (int iTaxon = 0; iTaxon < sTaxaNames.length; iTaxon++) {
        		if (sTaxaNames[iTaxon].equals(sTaxon)) {
                    isInTaxaSet[iTaxon] = true;
                    nrOfTaxa++;
        		}
        	}
        }
	    
	    RestrictedSubtreeSlide operator = new RestrictedSubtreeSlide();
	    if (clade2 == null) {
	    	operator.initByName("tree", tree, "clade", clade, "weight", 1.0);
	    } else {
	    	operator.initByName("tree", tree, "clade", clade, "clade", clade2, "weight", 1.0);
	    }
	    //operator.initByName("tree", tree);
	    for (int i = 0; i < 100; i++) {
	    	operator.proposal();
	    	isMonophyletic = false;
	    	hasClade(tree.getRoot(), isInTaxaSet, nrOfTaxa, new int[1]);
	    	if (!isMonophyletic) {
	    		throw new Exception("test failed: clade is not in tree any more");
	    	}
	    	System.err.println(tree.getRoot().toNewick());
	    }
	    
	}
	
	private Alignment getData() throws Exception {
        Sequence A = new Sequence("A", "A");
        Sequence B = new Sequence("B", "A");
        Sequence C = new Sequence("C", "A");
        Sequence D = new Sequence("D", "A");
        Sequence E = new Sequence("E", "A");
        Sequence F = new Sequence("F", "A");

        Alignment data = new Alignment();
        data.initByName("sequence", A, "sequence", B, "sequence", C, "sequence", D, "sequence", E, "sequence", F,
                "dataType", "nucleotide"
        );
		return data;
	}

	private int hasClade(Node node, boolean[] isInTaxaSet, int nrOfTaxa, int[] nTaxonCount) {
	        if (node.isLeaf()) {
	            nTaxonCount[0]++;
	            if (isInTaxaSet[node.getNr()]) {
	                return 1;
	            } else {
	                return 0;
	            }
	        } else {
	            int iTaxons = hasClade(node.getLeft(), isInTaxaSet, nrOfTaxa, nTaxonCount);
	            final int nLeftTaxa = nTaxonCount[0];
	            nTaxonCount[0] = 0;
	            if (node.getRight() != null) {
	                iTaxons += hasClade(node.getRight(), isInTaxaSet, nrOfTaxa, nTaxonCount);
	                final int nRightTaxa = nTaxonCount[0];
	                nTaxonCount[0] = nLeftTaxa + nRightTaxa;
	                if (iTaxons == nrOfTaxa) {
	                    isMonophyletic = (nTaxonCount[0] == nrOfTaxa);
	                    return iTaxons + 1;
	                }
	            }
	            return iTaxons;
	        }
	    }

}
