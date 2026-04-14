package test.beast.math.distributions;

import static org.junit.jupiter.api.Assertions.*;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.RealVector;
import beastlabs.math.distributions.WeightedDirichlet;
import org.apache.commons.numbers.gamma.LogGamma;
import org.junit.jupiter.api.Test;

/**
 * require(gtools)
 * x <- rdirichlet(20, c(2,2,2) )
 * sprintf("%.15f", x[1,])
 * sprintf("%.15f",log(ddirichlet(x[1,], c(2,2,2) )))
 *
 * WeightedDirichlet param uses mean (sum / dim) = 1,
 * which is diff to Simplex whose values sum to 1.
 * @author Walter Xie
 */
public class WeightedDirichletTest  {

    /**
     * Dirichlet case, copied from DirichletTest.
     */
    @Test
    public void testLogPEqualWeights() {
        RealVectorParam<PositiveReal> weights = new RealVectorParam<>(new double[]{1.0,1.0,1.0,1.0,1.0}, PositiveReal.INSTANCE);
        double[] a = new double[]{1.0, 1.0, 1.0, 1.0, 1.0};
        RealVector<PositiveReal> alpha = new RealVectorParam<>(a, PositiveReal.INSTANCE);
        RealVectorParam<PositiveReal> p = new RealVectorParam<>(new double[]{0.2, 0.2, 0.2, 0.2, 0.2}, PositiveReal.INSTANCE);

        WeightedDirichlet wd = new WeightedDirichlet();
        // mean (sum / dim) = 0.2
        wd.initByName("param", p, "alpha", alpha, "weights", weights, "mean", 0.2);
        int n = alpha.size();
        double f0 = wd.calculateLogP();

        // Compute expected log density
        double sumAlpha = 0.0;
        for (int i = 0; i < n; i++) {
            sumAlpha += a[i];
        }

        double logGammaSumAlpha = LogGamma.value(sumAlpha);

        double sumLogGammaAlpha = 0.0;
        for (int i = 0; i < n; i++) {
            sumLogGammaAlpha += LogGamma.value(a[i]);
        }

        double sumLogX = 0.0;
        for (int i = 0; i < n; i++) {
            sumLogX += (a[i] - 1.0) * Math.log(p.get(i));
        }

        double exp = logGammaSumAlpha - sumLogGammaAlpha + sumLogX;

        assertEquals(exp, f0, 1e-6);
    }

    @Test
    public void testLogP() {
        RealVectorParam<PositiveReal> alpha = new RealVectorParam<>(new double[]{2.0,2.0,2.0}, PositiveReal.INSTANCE);
        RealVectorParam<PositiveReal> weights = new RealVectorParam<>(new double[]{100.0,150.0,250.0}, PositiveReal.INSTANCE);

        // mean (sum / dim) = 1
        RealVectorParam<PositiveReal> p1 = new RealVectorParam<>(new double[]{1.0,1.0,1.0}, PositiveReal.INSTANCE);
        WeightedDirichlet wd1 = new WeightedDirichlet(p1, alpha, weights);

        // 15 decimals
        assertEquals(-2.2256240518579173, wd1.calculateLogP(), 1e-10);

        RealVectorParam<PositiveReal> p2 = new RealVectorParam<>(new double[]{100.0,100.0,100.0}, PositiveReal.INSTANCE);
        WeightedDirichlet wd2 = new WeightedDirichlet();
        // mean (sum / dim) = 100
        wd2.initByName("param", p2, "alpha", alpha, "weights", weights, "mean", 100.0);

        assertEquals(-16.041134609822194, wd2.calculateLogP(), 1e-10);
    }



}
