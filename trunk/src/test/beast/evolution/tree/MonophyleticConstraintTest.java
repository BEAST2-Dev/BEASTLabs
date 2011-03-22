package test.beast.evolution.tree;

import org.junit.Test;

import test.beast.evolution.likelihood.TreeLikelihoodTest;

import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.MonophyleticConstraint;
import beast.util.TreeParser;

import junit.framework.TestCase;

public class MonophyleticConstraintTest extends TestCase {

	@Test
	public void testSingleMonophyleticConstraint() throws Exception {
		Alignment data = TreeLikelihoodTest.getAlignment();
		TreeParser tree = new TreeParser();
		tree.initByName("taxa", data, 
				        "newick","((human:0.024003,(chimp:0.010772,bonobo:0.010772):0.013231):0.012035," +
				        		  "(gorilla:0.024003,(orangutan:0.010772,siamang:0.010772):0.013231):0.012035);");
			
		Taxon human = new Taxon();
		human.setID("human");
		Taxon bonobo = new Taxon();
		bonobo.setID("bonobo");
		Taxon chimp = new Taxon();
		chimp.setID("chimp");
		Taxon gorilla = new Taxon();
		gorilla.setID("gorilla");
		Taxon orangutan = new Taxon();
		orangutan.setID("orangutan");
		Taxon siamang = new Taxon();
		siamang.setID("siamang");

		TaxonSet set = new TaxonSet();
		set.initByName("taxon",human,"taxon",bonobo,"taxon",chimp);
		TaxonSet set2 = new TaxonSet();
		set2.initByName("taxon",gorilla,"taxon",orangutan,"taxon",siamang);
		
		MonophyleticConstraint contstraint = new MonophyleticConstraint();
		contstraint.initByName("taxa", data, "tree", tree, "set", set, "set", set2);

		double fLogP = contstraint.calculateLogP();
		assertEquals(fLogP, 0, 0);

		
		set2.initByName("taxon",gorilla,"taxon",siamang);
		contstraint = new MonophyleticConstraint();
		contstraint.initByName("taxa", data, "tree", tree, "set", set, "set", set2);
		fLogP = contstraint.calculateLogP();
		assertEquals(fLogP, Double.NEGATIVE_INFINITY, 0);

		set.initByName("taxon",human,"taxon",chimp);
		contstraint = new MonophyleticConstraint();
		contstraint.initByName("taxa", data, "tree", tree, "set", set, "set", set2);
		fLogP = contstraint.calculateLogP();
		assertEquals(fLogP, Double.NEGATIVE_INFINITY, 0);
}
}
