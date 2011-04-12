package beast.trace;

import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;


@Description("It is used by LogAnalyser. assertExpectation(TraceStatistics) sets TraceStatistics instance " +
        "passed from LogAnalyser.initAndValidate(), and determines whether expectation is significantly different " +
        "to statisctial mean. If true, then set isFailed = true, which makes JUnit test assertion failed.")
@Citation("Created by Walter Xie")
public class Expectation extends Plugin {

    public Input<String> m_sTraceName = new Input<String>("traceName", "The trace name of a loggable plugin", Validate.REQUIRED);

    public Input<Double> m_fExpValue =
            new Input<Double>("expectedValue", "The expected value of the referred loggable plugin", Validate.REQUIRED);

    public Input<Double> m_fStandErrorOfMean =
            new Input<Double>("stdError", "The expected standard error of mean. If not given, it will estimate error from log",
                    0.0);

    private boolean isFailed = false; // assert result
    private TraceStatistics trace;

    // this constructor is used by Unit test
    public Expectation(String traceName, Double expValue) throws Exception {
        this.m_sTraceName.setValue(traceName, this);
        this.m_fExpValue.setValue(expValue, this);
    }

    public boolean isFailed() {
        return isFailed;
    }

//    public void setFailed(boolean failed) {
//        isFailed = failed;
//    }

    public boolean assertExpectation(TraceStatistics trace, boolean displayStatistics) {
        this.trace = trace;
        double mean = trace.getMean();
        double stderr = getStdError();

        double upper = mean + 2 * stderr;
        double lower = mean - 2 * stderr;

        if (stderr == 0) {
            isFailed = mean != m_fExpValue.get();
        } else {
            isFailed = !(upper > m_fExpValue.get() && lower < m_fExpValue.get());
        }

        if (displayStatistics) {
            System.out.println(m_sTraceName.get() + " : " + mean + " +- " + stderr + ", expectation is "
                    + m_fExpValue.get() + ", ESS = " + trace.getESS());
        }
        return !isFailed;
    }

    public TraceStatistics getTraceStatistics() {
        return trace;
    }

    public double getStdError() {
        double stderr = trace.getStdErrorOfMean();
        if (m_fStandErrorOfMean.get() != 0) {
            stderr = m_fStandErrorOfMean.get();
//            System.out.println("User defines standard error of mean = " + stderr);
        }
        return stderr;
    }
}
