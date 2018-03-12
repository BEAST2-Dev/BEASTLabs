package beast.core.util;

import java.io.PrintStream;

import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.BEASTObject;
import beast.core.Input.Validate;
import beast.core.parameter.BooleanParameter;
import beast.core.parameter.IntegerParameter;



@Description("Logs the sum of anything that is Valuable")
public class LoggableSum extends BEASTObject implements Loggable {
	public Input<Function> m_valuableInput = new Input<Function>("value", "Valuable Plugin that needs logging the sum of", Validate.REQUIRED);

	Function m_valuable;
	enum Mode {integer_mode, double_mode}
	Mode m_mode;
	
	@Override
	public void initAndValidate() {
		m_valuable =  m_valuableInput.get();
		if (m_valuable instanceof IntegerParameter || m_valuable instanceof BooleanParameter) {
			m_mode = Mode.integer_mode;
		} else {
			m_mode = Mode.double_mode;
		}
	}
	
    /**
     * Loggable interface implementation follows  
     */
    @Override
	public void init(PrintStream out) {
        out.print("sum("+((BEASTObject)m_valuable).getID() + ")\t");
    }

    @Override
	public void log(long nSample, PrintStream out) {
        final int nDimension = m_valuable.getDimension();
        double fSum = 0;
        for (int iValue = 0; iValue < nDimension; iValue++) {
        	fSum += m_valuable.getArrayValue(iValue);
        }
        if (m_mode == Mode.integer_mode) {
        	out.print((int)fSum + "\t");
        } else {
        	out.print(fSum + "\t");
        }
	}

    @Override
	public void close(PrintStream out) {
		// nothing to do
	}
	
}
