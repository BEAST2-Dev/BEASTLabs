package beastlabs.math.distributions;

import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WeightedDirichletSimulator {

//    static String homePath = System.getProperty("user.home");
    static File outFile = new File("WeightedDirichletA127.log");

    public static void main(String[] args) {
        WeightedDirichlet weightedDirichlet = new WeightedDirichlet();
        RealParameter alphaParam = new RealParameter(new Double[]{1.0,2.0,7.0});
        Integer[] weights = new Integer[]{100,200,700};
        IntegerParameter weightParam = new IntegerParameter(weights);

        weightedDirichlet.initByName("alpha", alphaParam, "weights", weightParam);

        final int size = 100000;
        Double[][] val2d = weightedDirichlet.sample(size);
        System.out.println("Simulate " + size + " samples");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            // header
            writer.write("Sample\t");
            if (alphaParam.getDimension() == 3) {
                writer.write("r1\tr2\tr3");
            } else {
                for (int i = 0; i < val2d[0].length; i++) {
                    writer.write("Var" + (i + 1));
                    if (i < val2d[0].length - 1) {
                        writer.write('\t'); // tab delimiter
                    }
                }
            }
            writer.newLine(); // new line after each row

            for (int n = 0; n < val2d.length; n++) {
                // the weight mean = sum(x[i] * weight[i]) / sum(weight[i])
//                double weightedMeanX = WeightedDirichlet.getWeightedMean(Arrays.stream(val2d[n])
//                                .mapToDouble(Double::doubleValue).toArray(),
//                        Arrays.stream(weights).mapToDouble(Integer::doubleValue).toArray());
//
//                if (Math.abs(weightedMeanX  - weightedDirichlet.meanInput.get()) > 1e-6)
//                    throw new RuntimeException("Replicate " + n + ", values = " + Arrays.toString(val2d[n]));

                writer.write(Integer.toString(n) + '\t');
                for (int i = 0; i < val2d[n].length; i++) {
                    writer.write(Double.toString(val2d[n][i]));
                    if (i < val2d[n].length - 1) {
                        writer.write('\t'); // tab delimiter
                    }
                }
                writer.newLine(); // new line after each row

                if (n % 1000 == 0) {
                    System.out.println("Wrote " + n + " lines...");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Finish writing file to " + outFile.getAbsolutePath());
    }

}
