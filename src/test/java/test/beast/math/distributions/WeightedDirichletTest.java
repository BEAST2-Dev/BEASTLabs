package test.beast.math.distributions;

import static org.junit.jupiter.api.Assertions.*;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
import beastlabs.math.distributions.WeightedDirichlet;
import org.junit.jupiter.api.BeforeEach;

/**
 * require(gtools)
 * x <- rdirichlet(20, c(2,2,2) )
 * sprintf("%.15f", x[1,])
 * sprintf("%.15f",log(ddirichlet(x[1,], c(2,2,2) )))
 *
 * @author Walter Xie
 */
public class WeightedDirichletTest  {

    RealVectorParam<PositiveReal> alpha;

    @BeforeEach
    public void setUp() throws Exception {
        alpha = new RealVectorParam<>(new double[]{2.0,2.0,2.0}, PositiveReal.INSTANCE);
    }

    public void testLogPEqualWeights() {
        RealVectorParam<PositiveReal> weights = new RealVectorParam<>(new double[]{1.0,1.0,1.0}, PositiveReal.INSTANCE);
        SimplexParam p1 = new SimplexParam(new double[]{0.589929287159556,0.254172576287574,0.155898136552870});
        WeightedDirichlet wd1 = new WeightedDirichlet(p1, alpha, weights);

        // 15 decimals
        assertEquals(1.031444876947574, wd1.calculateLogP(), 1e-10);

        SimplexParam p2 = new SimplexParam(new double[]{0.671034770323350,0.169251509344698,0.159713720331952});
        WeightedDirichlet wd2 = new WeightedDirichlet(p2, alpha, weights);

        assertEquals(0.777815654415272, wd2.calculateLogP(), 1e-10);
    }

    public void testLogP() {
        RealVectorParam<PositiveReal> weights = new RealVectorParam<>(new double[]{100.0,150.0,250.0}, PositiveReal.INSTANCE);

        // sum != 1, [0.3333333333333333, 0.3333333333333333, 0.3333333333333333]
        SimplexParam p1 = new SimplexParam(new double[]{10.0,10.0,10.0});
        WeightedDirichlet wd1 = new WeightedDirichlet(p1, alpha, weights);

        // 15 decimals
        assertEquals(1.491654876777717, wd1.calculateLogP(), 1e-10);

        SimplexParam p2 = new SimplexParam(new double[]{100.0,100.0,100.0});
        WeightedDirichlet wd2 = new WeightedDirichlet(p2, alpha, weights);

        assertEquals(1.491654876777717, wd2.calculateLogP(), 1e-10);
    }



}
