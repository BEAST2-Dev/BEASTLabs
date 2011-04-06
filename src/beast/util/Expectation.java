package beast.util;

import beast.beast1.TraceStatistics;
import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;


@Description("")
@Citation("Created by Walter Xie")
public class Expectation extends Plugin {

    public Input<String> m_Name = new Input<String>("name", "The name of a loggable plugin", Validate.REQUIRED);

    public Input<Double> m_ExpValue =
            new Input<Double>("expectedValue", "The expected value of the referred loggable plugin", Validate.REQUIRED);

    public Input<Double> m_StandErrorOfMean =
            new Input<Double>("stdError", "The expected standard error of mean. If not given, it will estimate error from log",
                    0.0, Validate.OPTIONAL);

    private boolean isFailed = false;
    private TraceStatistics trace;

    public boolean isFailed() {
        return isFailed;
    }

//    public void setFailed(boolean failed) {
//        isFailed = failed;
//    }

    public boolean assertExpectation(TraceStatistics trace) {
        this.trace = trace;
        double mean = trace.getMean();
        double stderr = getStdError();

        double upper = mean + 2 * stderr;
        double lower = mean - 2 * stderr;

        isFailed = !(upper > m_ExpValue.get() && lower < m_ExpValue.get());

        return !isFailed;
    }

    public TraceStatistics getTraceStatistics() {
        return trace;
    }


    public double getStdError() {
        double stderr = trace.getStdErrorOfMean();
        if (m_StandErrorOfMean.get() != 0) {
            stderr = m_StandErrorOfMean.get();
//            System.out.println("User defines standard error of mean = " + stderr);
        }
        return stderr;
    }
}
