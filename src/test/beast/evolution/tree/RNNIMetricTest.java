package test.beast.evolution.tree;

import org.junit.Test;

import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.RNNIMetric;
import beast.util.TreeParser;
import junit.framework.TestCase;

public class RNNIMetricTest extends TestCase {
	
	@Test
	public void testRNNIMetricBasic() {
		TaxonSet taxonset = new TaxonSet();
		taxonset.initByName("taxon", new Taxon("A"), "taxon", new Taxon("B"), "taxon", new Taxon("C"));
		
		TreeParser tree1 = new TreeParser("((A:1,B:1):1,C:2)");
		TreeParser tree2 = new TreeParser("((A:1,C:1):1,B:2)");
		
		RNNIMetric metric = new RNNIMetric();
		metric.initByName("taxonset", taxonset);
		double d = metric.distance(tree1, tree2);
		assertEquals(1.0, d);
	}
	
	@Test
	public void testRNNIMetricTaxonOrder() {
		TaxonSet taxonset = new TaxonSet();
		taxonset.initByName("taxon", new Taxon("C"), "taxon", new Taxon("B"), "taxon", new Taxon("A"));
		
		TreeParser tree1 = new TreeParser("((B:1,A:1):1,C:2)");
		TreeParser tree2 = new TreeParser("((C:1,A:1):1,B:2)");
		
		RNNIMetric metric = new RNNIMetric();
		metric.initByName("taxonset", taxonset);
		double d = metric.distance(tree1, tree2);
		assertEquals(1.0, d);
	}

	
	@Test
	public void testRNNIMetricBasic4() {
		TaxonSet taxonset = new TaxonSet();
		taxonset.initByName("taxon", new Taxon("A"), "taxon", new Taxon("B"), "taxon", new Taxon("C"), "taxon", new Taxon("D"));
		
		TreeParser tree1 = new TreeParser("(((A:1,B:1):1,C:2):1,D:3)");
		TreeParser tree2 = new TreeParser("(((C:1,D:1):1,B:2):1,A:3)");
		
		RNNIMetric metric = new RNNIMetric();
		metric.initByName("taxonset", taxonset);
		double d = metric.distance(tree1, tree2);
		assertEquals(3.0, d);
	}
}
