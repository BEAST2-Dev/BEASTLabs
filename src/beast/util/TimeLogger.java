package beast.util;

import java.io.PrintStream;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;

@Description("Logger for reporting elapsed time since start of run")
public class TimeLogger extends BEASTObject implements Loggable {
	enum reportUnits {milliseconds, seconds, minutes, hours}
	final public Input<reportUnits> reportInput = new Input<>("report", "units to report time in", reportUnits.seconds, reportUnits.values());

	long start;
	reportUnits report;
	
	@Override
	public void initAndValidate() {
		report = reportInput.get();
	}

	@Override
	public void init(PrintStream out) {
		if (getID() == null) {
			out.append("time\n");
		} else {
			out.append(getID() + "\t");
		}
		start = System.currentTimeMillis();
	}

	@Override
	public void log(int sample, PrintStream out) {
		long elapsed = System.currentTimeMillis() - start;
		switch (report) {
		case milliseconds: 
			out.append(elapsed + "\t");
			break;
		case seconds: 
			out.append(elapsed/1000 + "\t");
			break;
		case minutes: 
			out.append(elapsed/60000 + "\t");
			break;
		case hours: 
			out.append(elapsed/3600000 + "\t");
			break;
		}
	}

	@Override
	public void close(PrintStream out) {
		// nothing to do
	}
}
