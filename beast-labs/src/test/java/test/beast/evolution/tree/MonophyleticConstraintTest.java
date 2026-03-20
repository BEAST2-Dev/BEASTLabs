package test.beast.evolution.tree;

import org.junit.Test;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TreeParser;
import beastlabs.evolution.tree.MonophyleticConstraint;
import test.beast.evolution.likelihood.ExperimentalTreeLikelihoodTest;


import junit.framework.TestCase;

public class MonophyleticConstraintTest extends TestCase {

	@Test
	public void testSingleMonophyleticConstraint() throws Exception {
		Alignment data = ExperimentalTreeLikelihoodTest.getAlignment();
		TreeParser tree = new TreeParser();
		tree.initByName("taxa", data, 
				        "newick","((human:0.024003,(chimp:0.010772,bonobo:0.010772):0.013231):0.012035," +
				        		  "(gorilla:0.024003,(orangutan:0.010772,siamang:0.010772):0.013231):0.012035);",
				         "IsLabelledNewick", true);
			
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
