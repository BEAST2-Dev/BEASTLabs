package beast.evolution.tree;


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.util.ClusterTree;
import beast.util.Randomizer;


@Description("Logger to report statistics of a tree")
public class TreeDistanceLogger extends CalculationNode implements Loggable, Function {
	
	
	private enum metric {
		RF, RNNI
	}
	
	
    final public Input<Tree> treeInput = new Input<>("tree", "Tree to report height for.");
   // final public Input<TreeMetric> metricInput = new Input<>("metric", "Tree distance metric (default: Robinson Foulds).");
    final public Input<Tree> referenceInput = new Input<>("ref", "Reference tree to calculate distances from (default: the initial tree).");
    
    final public Input<Integer> numBootstrapsInput = new Input<>("bootstraps", "Number of reference trees to use where each is based on a different sample of the alignment."
    		+ " Set to 0 to use full alignment without bootstrapping.", 0);
    
    final public Input<Double> bootstrapPSitesInput = new Input<>("psites", "Proportion of sites to sample when bootstraping. Set to 0 for random number of sites per seq", 1.0);
    
    final public Input<metric> metricInput = new Input<>("metric", "Tree distance metric", metric.RF, metric.values());
    
    
    List<TreeMetric> metrics;
    List<Tree> referenceTrees;
    int nbootstraps;
    Tree tree;
    

    @Override
    public void initAndValidate() {
    	
    	
    	this.tree = treeInput.get();
    	
    	this.nbootstraps = numBootstrapsInput.get();
    	
    	// Reference tree
    	this.referenceTrees = new ArrayList<>();
    	if (referenceInput.get() != null) {
    		
    		Tree refTree = referenceInput.get();
    		
    		if (this.nbootstraps > 0 && refTree instanceof ClusterTree) {
    			Log.warning(this.getClass().getName() + ": Calculating reference tree across " + this.nbootstraps + " bootstraps");
    			ClusterTree clusterTree = (ClusterTree)refTree;
    			Alignment refAlignment = clusterTree.dataInput.get();
    			
    			int nsites = 0;
    			for (int i = 0; i < this.nbootstraps; i++) {
    				
    				
        			if (bootstrapPSitesInput.get() <= 0) {
        				nsites = Randomizer.nextInt(refAlignment.getSiteCount()) + 1;
        			}else {
        				nsites = (int) Math.ceil(bootstrapPSitesInput.get() * refAlignment.getSiteCount());
        			}

    				
    				// Subsample alignment
    				String[][] seqs = new String[nsites][];
    				for (int site = 0; site < nsites; site ++) {
    					seqs[site] = new String[refAlignment.getTaxonCount()];
    					
    					// Sample a site
    					int siteNum = Randomizer.nextInt(refAlignment.getSiteCount());
    					for (int taxon = 0; taxon < refAlignment.getTaxonCount(); taxon ++) {
    						//String taxonName = refAlignment.getTaxaNames().get(taxon);
    						//String seq = refAlignment.getSequenceAsString(taxonName);
    						String seq = refAlignment.sequenceInput.get().get(taxon).dataInput.get();
    						String siteChar = seq.substring(siteNum, siteNum+1);
    						seqs[site][taxon] = siteChar;
    					}
    					
    				}
    				
    				/*
    				System.out.println("Sampled alignment:");
    				for (int taxon = 0; taxon < refAlignment.getTaxonCount(); taxon ++) {
    					String taxonName = refAlignment.getTaxaNames().get(taxon);
    					System.out.println(">" + taxonName);
    					for (int site = 0; site < refAlignment.getSiteCount(); site ++) {
    						System.out.print(seqs[site][taxon]);
    					}
    					System.out.println();
    				}
    				*/
    				
    				// Generate sequence objects
    				List<Sequence> sequences = new ArrayList<>();
    				for (int taxon = 0; taxon < refAlignment.getTaxonCount(); taxon ++) {
    					Sequence refSeq = refAlignment.sequenceInput.get().get(taxon);
    					String taxonName = refAlignment.getTaxaNames().get(taxon);
    					String seq = "";
    					for (int site = 0; site < nsites; site ++) {
    						seq += seqs[site][taxon];
    					}
    					Sequence sequence = new Sequence();
    					
    					sequence.initByName("taxon", taxonName,
    										"value", seq,
    										"totalcount", refSeq.totalCountInput.get());
    										//"uncertain", refSeq.uncertainInput.get());
    					sequences.add(sequence);
    				}
    				
    				
    				// Init alignment
    				// Copy (shallow copy) alignment
    				Alignment aln = new Alignment();
    				Map<String, Input<?>> inputs = refAlignment.getInputs();
    				for (String key : inputs.keySet()) {
    					if (key.equals("sequence")) continue;
    					aln.setInputValue(key, inputs.get(key).get());
    				}
    				aln.setInputValue("sequence", sequences);
    				aln.setID("bootstrap" + i);
    				aln.initAndValidate();
    				
    				
    				
    				// Create tree
    				Tree ref = new ClusterTree();
    				String resume = System.getProperty("beast.resume");
    				System.setProperty("beast.resume", "false"); // this makes sure the cluster tree does clustering
    				ref.initByName("clusterType", clusterTree.clusterTypeInput.get(), 
    								"distance", clusterTree.distanceInput.get(),
    								"clock.rate", clusterTree.clockRateInput.get(),
    								"taxa", aln);
    				if (resume != null) {
    					System.setProperty("beast.resume", resume);
    				}
    				
    				// Add tree to list of trees
    				this.referenceTrees.add(ref);
    				
    			}
    			
    		}
    		
    		// Use the reference tree
    		else {
				String resume = System.getProperty("beast.resume");
				System.setProperty("beast.resume", "false"); // this makes sure the cluster tree does clustering
				refTree.initAndValidate();
				if (resume != null) {
					System.setProperty("beast.resume", resume);
				}
    			this.referenceTrees.add(refTree);
    		}
    		
    	}
    	
    	
    	// Distance metrics
    	this.metrics = new ArrayList<>();
    	for (Tree refTree : this.referenceTrees) {
    		
    		TreeMetric metric = null;
    		switch (metricInput.get()) {
    			
    			// Robinson foulds
	    		case RF:{
	        		metric = new RobinsonsFouldMetric(this.tree == null ? null : this.tree.getTaxaNames());
	    			break;
	    		}
	    		
	    		// RNNI
	    		case RNNI:{
	    			metric = new RNNIMetric(this.tree == null ? null : this.tree.getTaxaNames());
	    			break;
	    		}
    		}
    		
    		if (this.tree != null) metric.setReference(refTree);
    		this.metrics.add(metric);
    		
    	
    	}
    	
    	
    	// Remove all trees which are identical to other trees according to the metric
    	if (this.tree != null) {
	    	List<Integer> toRemove = new ArrayList<>();
	    	for (int t1 = 0; t1 < this.referenceTrees.size(); t1++) {
	    		Tree tree1 = this.referenceTrees.get(t1);
	    		if (toRemove.contains((Integer)t1)) continue;
	    		for (int t2 = t1+1; t2 < this.referenceTrees.size(); t2++) {
	    			if (toRemove.contains((Integer)t2)) continue;
	    			TreeMetric metric2 = this.metrics.get(t2);
	    			//System.out.println(metric2.distance(tree1));
	    			if (metric2.distance(tree1) == 0) {
	    				toRemove.add(t2);
	    			}
	    		}
	    	}
	    	for (int i = toRemove.size()-1; i >=0; i--) {
	    		this.referenceTrees.remove((int)toRemove.get(i));
	    		this.metrics.remove((int)toRemove.get(i));
	    	}
	    	
	    	
	    	Log.warning("Total number of unique reference trees: " + this.metrics.size());
    	}
    	
    }
    
    public void setTree(Tree tree) {
    	
    	this.tree = tree;
    	
    	// Distance metric
    	this.metrics = new ArrayList<>();
    	for (Tree refTree : this.referenceTrees) {
    		TreeMetric metric = new RobinsonsFouldMetric(this.tree == null ? null : this.tree.getTaxonset());
    		metric.setReference(refTree);
    		this.metrics.add(metric);
    	}
    }
    

    @Override
    public void init(PrintStream out) {
    	if (this.tree == null) return;
        out.print(this.getID() + ".treeDistance\t");
        if (this.referenceTrees.size() > 1) out.print(this.getID() + ".treeDistanceVar\t");
    }

    @Override
    public void log(long sample, PrintStream out) {
    	
    	if (this.tree == null) return;
    	
    	// Null reference tree? Use the tree on the first logged state
    	if (this.referenceTrees.isEmpty()) {
			Tree referenceTree = new Tree(this.tree.getRoot().copy());
			this.referenceTrees.add(referenceTree);
			
			TreeMetric metric = null;
    		switch (metricInput.get()) {
    			
    			// Robinson foulds
	    		case RF:{
	        		metric = new RobinsonsFouldMetric(this.tree.getTaxaNames());
	    			break;
	    		}
	    		
	    		// RNNI
	    		case RNNI:{
	    			metric = new RNNIMetric(this.tree.getTaxaNames());
	    			break;
	    		}
    		}
    		metric.setReference(referenceTree);
    		this.metrics.add(metric);
    	}
    	
        
        out.print(getDistance(this.tree) + "\t");
        if (this.referenceTrees.size() > 1) out.print(getDistanceVariance(this.tree) + "\t");
    }

    /**
     * Return distances between 'tree' and the reference tree
     * If there is more than 1 metric it will take the mean distance
     * @param tree
     * @return
     */
    public double getDistance(Tree tree) {
    	double dist = 0;
    	for (TreeMetric metric : this.metrics) {
    		double d = metric.distance(tree);
    		dist += d;
    	}
    	return dist / this.metrics.size();
	}
    
    
    /**
     * Returns the variance in distances between this tree and the list of reference trees
     * @param tree
     * @return
     */
    public double getDistanceVariance(Tree tree) {
    	double mean = this.getDistance(tree);
    	double SS = 0;
    	for (TreeMetric metric : this.metrics) {
    		double d = metric.distance(tree);
    		SS += Math.pow(d - mean, 2);
    	}
    	return SS / this.metrics.size();
	}

	@Override
    public void close(PrintStream out) {
        // nothing to do
    }

    @Override
    public int getDimension() {
    	if (this.tree == null) return 0;
    	return this.referenceTrees.size() == 1 ? 1 : 2;
    }

    @Override
    public double getArrayValue() {
    	if (this.tree == null) return 0;
        return this.getDistance(this.tree);
    }

    @Override
    public double getArrayValue(int dim) {
    	if (this.tree == null) return 0;
    	return this.getArrayValue();
    }
}
