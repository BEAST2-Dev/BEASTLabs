package test.beast.math.distributions;

import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.math.distributions.WeightedDirichlet;
import junit.framework.TestCase;
import org.junit.Before;

/**
 * require(gtools)
 * x <- rdirichlet(20, c(2,2,2) )
 * sprintf("%.15f", x[1,])
 * sprintf("%.15f",log(ddirichlet(x[1,], c(2,2,2) )))
 *
 * @author Walter Xie
 */
public class WeightedDirichletTest extends TestCase {

    RealParameter alpha;
    RealParameter p1;
    RealParameter p2;

    @Before
    public void setUp() throws Exception {
        alpha = new RealParameter(new Double[]{2.0,2.0,2.0});
    }

    public void testLogPEqualWeights() {
        IntegerParameter weights = new IntegerParameter(new Integer[]{1,1,1});
        WeightedDirichlet weightedDirichlet = new WeightedDirichlet();
        weightedDirichlet.initByName("alpha", alpha, "weights", weights);

        // sum = 1
        p1 = new RealParameter(new Double[]{0.589929287159556,0.254172576287574,0.155898136552870});
        p2 = new RealParameter(new Double[]{0.671034770323350,0.169251509344698,0.159713720331952});

        // 15 decimals
        double prob = weightedDirichlet.calcLogP(p1);
        assertEquals(1.031444876947574, prob, 1e-10);

        prob = weightedDirichlet.calcLogP(p2);
        assertEquals(0.777815654415272, prob, 1e-10);
    }

    public void testLogP() {
        IntegerParameter weights = new IntegerParameter(new Integer[]{100,150,250});
        WeightedDirichlet weightedDirichlet = new WeightedDirichlet();
        weightedDirichlet.initByName("alpha", alpha, "weights", weights);

        // sum != 1, [0.3333333333333333, 0.3333333333333333, 0.3333333333333333]
        p1 = new RealParameter(new Double[]{10.0,10.0,10.0});
        p2 = new RealParameter(new Double[]{100.0,100.0,100.0});

        // 15 decimals
        double prob = weightedDirichlet.calcLogP(p1);
        assertEquals(1.491654876777717, prob, 1e-10);

        prob = weightedDirichlet.calcLogP(p2);
        assertEquals(1.491654876777717, prob, 1e-10);
    }



}
