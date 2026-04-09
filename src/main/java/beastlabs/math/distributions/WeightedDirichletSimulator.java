package beastlabs.math.distributions;

import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class WeightedDirichletSimulator {

    static File outFile = new File("WeightedDirichletA127.log");

    public static void main(String[] args) {
        RealVectorParam<Real> alphaParam = new RealVectorParam<>();
        alphaParam.initByName("value", "1.0 2.0 7.0");
        RealVectorParam<Real> weightParam = new RealVectorParam<>();
        weightParam.initByName("value", "100.0 200.0 700.0");
        SimplexParam simplexParam = new SimplexParam();
        simplexParam.initByName("value", "0.1 0.2 0.7");

        WeightedDirichlet weightedDirichlet = new WeightedDirichlet(simplexParam, alphaParam, weightParam);

        final int size = 100000;
        System.out.println("Simulate " + size + " samples");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            // header
            writer.write("Sample\t");
            if (alphaParam.size() == 3) {
                writer.write("r1\tr2\tr3");
            } else {
                for (int i = 0; i < alphaParam.size(); i++) {
                    writer.write("Var" + (i + 1));
                    if (i < alphaParam.size() - 1) writer.write('\t');
                }
            }
            writer.newLine();

            for (int n = 0; n < size; n++) {
                List<Double> sample = weightedDirichlet.sample();

                writer.write(Integer.toString(n) + '\t');
                for (int i = 0; i < sample.size(); i++) {
                    writer.write(Double.toString(sample.get(i)));
                    if (i < sample.size() - 1) writer.write('\t');
                }
                writer.newLine();

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
