package beast.inference;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.util.LogAnalyser;

/** combines log files produced by a ParticleFilter for 
 * combined analysis**/
public class ParticleLogCombiner extends LogAnalyser {

	String m_sParticleDir;
	String m_sLogFileName;
	int m_nParticles = -1;
	PrintStream m_out = System.out;
	int m_nBurninPercentage = 10;
	
	private void parseArgs(String[] args) throws Exception {
		int i = 0;
		try {
			while (i < args.length) {
				int iOld = i;
				if (i < args.length) {
					if (args[i].equals("")) {
						i += 1;
					} else if (args[i].equals("-o")) {
		                m_out = new PrintStream(args[i+1]);
						i += 2;
					} else if (args[i].equals("-b")) {
						m_nBurninPercentage = Integer.parseInt(args[i+1]);
						i += 2;
					} else if (args[i].equals("-n")) {
		                m_nParticles = Integer.parseInt(args[i+1]);
						i += 2;
					} else if (args[i].equals("-log")) {
						m_sLogFileName = args[i+1];
						i += 2;
					} else if (args[i].equals("-dir")) {
		                m_sParticleDir = args[i+1];
						i += 2;
					}
					if (i == iOld) {
						throw new Exception("Unrecognised argument");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error parsing command line arguments: " + Arrays.toString(args) + "\nArguments ignored\n\n" + getUsage());
		}
	}

	
    /** data from log file with burn-in removed **/
    Double [][] m_fCombinedTraces;

	private void combineParticleLogs() throws Exception {
		List<String> sLogs = new ArrayList<String>();
		for (int i = 0; i < m_nParticles; i++) {
			String sDir = m_sParticleDir + "/particle" + i;
			File dir = new File(sDir);
			if (!dir.exists() || !dir.isDirectory()) {
				throw new Exception ("Could not process particle " +i +". Expected " + sDir + " to be a directory, but it is not.");
			}
			sLogs.add(sDir + "/" + m_sLogFileName);
		}
		combineLogs(sLogs);
	}

	private void combineLogs(List<String> sLogs) throws Exception {
		m_fCombinedTraces = null;
		// read logs
		for (String sFile : sLogs) {
			readLogFile(sFile, m_nBurninPercentage);

			if (m_fCombinedTraces == null) {
				m_fCombinedTraces = m_fTraces;
			} else {
				for (int i = 0; i < m_fTraces.length; i++) {
					Double [] logLine = m_fTraces[i];
					Double [] oldTrace = m_fCombinedTraces[i];
					Double [] newTrace = new Double[oldTrace.length + logLine.length];
					System.arraycopy(oldTrace, 0, newTrace, 0, oldTrace.length);
					System.arraycopy(logLine, 0, newTrace, oldTrace.length, logLine.length);
					m_fCombinedTraces[i] = newTrace;
				}
			}
		}
		// reset sample column
		if (m_fCombinedTraces[0].length > 2) {
			int nDelta = (int) (m_fCombinedTraces[0][1] - m_fCombinedTraces[0][0]);
			for (int i = 0; i < m_fCombinedTraces[0].length; i++) {
				m_fCombinedTraces[0][i] = (double) (nDelta * i);
			}
		}
	}

	private void printCombinedLogs() {
		// header
		for (int i = 0; i < m_sLabels.length; i++) {
			m_out.print(m_sLabels[i] + "\t");
		}
		m_out.println();
		for (int i = 0; i < m_fCombinedTraces[0].length; i++) {
			for (int j = 0; j < m_types.length; j++) {
				switch (m_types[j]) {
				case REAL:
					m_out.print(m_fCombinedTraces[j][i]+"\t");
					break;
				case NOMINAL:
				case BOOL:
					m_out.print(m_ranges[(int)(double)m_fCombinedTraces[j][i]] + "\t");
					break;
				}
			}
			m_out.print("\n");
		}
	}

	private static String getUsage() {
		return "Usage: ParticleLogCombiner -dir <directory> -log <file> -n <int> [<options>]\n" +
		//"-xml <Beast.xml> where <Beast.xml> the name of a file specifying a Beast run with a ParticleFilter\n" +
		"                 this grabs the paricle directory and number of particles from the file\n" +
		"-dir <directory> specify particle directory\n" +
		"-log <file>      specify the name of the log file\n" +
		"-n <int>         specify the number of particles\n" +
		"-o <output.log>  specify log file to write into (default output is stdout)\n" +
		"-b <burnin>      specify the number PERCANTAGE of lines in the log file to consider burnin (default 10)\n";
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println(getUsage());
			System.exit(0);
		}
		ParticleLogCombiner combiner = new ParticleLogCombiner();
		try {
			combiner.parseArgs(args);
			combiner.combineParticleLogs();
			combiner.printCombinedLogs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main




}
