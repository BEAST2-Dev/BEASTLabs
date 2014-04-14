package test.beast.math.distributions;

import org.junit.Test;

import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Tree;
import beast.math.distributions.MultiMonophyleticConstraint;
import test.beast.BEASTTestCase;
import junit.framework.TestCase;

public class MultiMonophyleticConstraintTest extends TestCase {

	@Test
	public void testMultiMonophylecitConstraint() throws Exception {
		double logP = 0;
		Alignment data = BEASTTestCase.getAlignment();
//		TaxonSet taxonset = new TaxonSet();
//		taxonset.initByName("alignment", data);
		Tree tree = BEASTTestCase.getTree(data);
		MultiMonophyleticConstraint constraints = new MultiMonophyleticConstraint();
		
		// original Newick as constraint
		constraints.initByName("tree", tree,
				"newick", tree.getRoot().toNewick());
		logP = constraints.calculateLogP();
		assertEquals(0.0, logP);

		// small single constraint
		constraints.initByName("tree", tree,
				"newick", "(bonobo, chimp)");
		logP = constraints.calculateLogP();
		assertEquals(0.0, logP);

		// small single constraint with quotes
		constraints.initByName("tree", tree,
				"newick", "(\"bonobo\", 'chimp')");
		logP = constraints.calculateLogP();
		assertEquals(0.0, logP);

		
		// single constraint
		constraints.initByName("tree", tree,
				"newick", "(human, bonobo, chimp)");
		logP = constraints.calculateLogP();
		assertEquals(0.0, logP);
		
		// double constraint
		constraints = new MultiMonophyleticConstraint();
		constraints.initByName("tree", tree,
				"newick", "((human, bonobo, chimp), gorilla)");
		logP = constraints.calculateLogP();
		assertEquals(0.0, logP);

		
		// failing single constraint
		constraints = new MultiMonophyleticConstraint();
		constraints.initByName("tree", tree,
				"newick", "(human, bonobo, gorilla)");
		logP = constraints.calculateLogP();
		assertEquals(Double.NEGATIVE_INFINITY, logP);

		// failing double constraint
		constraints = new MultiMonophyleticConstraint();
		constraints.initByName("tree", tree,
				"newick", "((human, bonobo, chimp), orangutan)");
		logP = constraints.calculateLogP();
		assertEquals(Double.NEGATIVE_INFINITY, logP);

		// extended double constraint
		constraints = new MultiMonophyleticConstraint();
		constraints.initByName("tree", tree,
				"newick", "((human, bonobo, chimp), gorilla, orangutan)");
		logP = constraints.calculateLogP();
		assertEquals(0.0, logP);

		// triple constraint => should be reduced to two, ignoring root constraint
		constraints = new MultiMonophyleticConstraint();
		constraints.initByName("tree", tree,
				"newick", "(siamang,((human, bonobo, chimp), gorilla, orangutan))");
		logP = constraints.calculateLogP();
		assertEquals(0.0, logP);

	}
}
