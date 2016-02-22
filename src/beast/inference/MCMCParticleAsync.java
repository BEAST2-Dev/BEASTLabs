package beast.inference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import beast.core.Description;
import beast.core.Input;
import beast.math.statistic.DiscreteStatistics;
import beast.util.Randomizer;



@Description("MCMC chain to be launched by ParticleFilter. It updates its state without " +
		"synchronisation with any of the other particles.")
public class MCMCParticleAsync extends MCMCParticle {
	
	public Input<Double> thresholdInput = new Input<Double>("threshold","threshold for resampling a particle. If the difference " +
			"between highest and current posterior exceeds threshold, this state will be resampled. " +
			"(default -1e10, which means resample always)",-1e10);
	
	/** states and associated posteriors. Used to sample next state from **/
	String [] m_sStates;
	double [] m_fPosteriors;
	String [] m_sOldStates;
	double [] m_fOldPosteriors;
	int m_nParticles;
	String m_sRootDir;
	DecimalFormat formatter;
	
	int m_iCurrentParticleNr;
	double m_fThreshHold;
	
	@Override
	public void initAndValidate() {
		super.initAndValidate();

		m_nParticles = 0;
		if (m_sParticleDir != null) {
			File particleDir = new File(m_sParticleDir);
			String s = particleDir.getName().substring(8);
			m_iCurrentParticleNr = Integer.parseInt(s);
			File main = particleDir.getParentFile();
			m_sRootDir = main.getAbsolutePath();
			for (File f : main.listFiles()) {
				if (f.isDirectory() && f.getName().startsWith("particle")) {
					m_nParticles++;
				}
			}
			// set up directories with beast.xml files in each of them
			String sFormat = "";
			for (int i = m_nParticles; i > 0; i /= 10) {
				sFormat += "#";
			}
			formatter = new DecimalFormat(sFormat);
		}
		
		m_sStates = new String[m_nParticles];
		m_fPosteriors = new double[m_nParticles];
		m_sOldStates = new String[m_nParticles];
		m_fOldPosteriors = new double[m_nParticles];
		m_fThreshHold = thresholdInput.get();
	}


	protected void doUpdateState(int iSample) {
		try {
			// park old state file, so that processes reading this file 
			// do not get interrupted (though they will have an old state)
			File f = new File(m_sParticleDir + "/beast.xml.state");
			File oldF = new File(m_sParticleDir + "/beast.xml.state.old");
			f.renameTo(oldF);

			// write new state file
			state.storeToFile(iSample);
			operatorSchedule.storeToFile();

			updateStates(iSample);

			try {        	
				state.restoreFromFile();
				operatorSchedule.restoreFromFile();
				oldLogLikelihood = robustlyCalcPosterior(posterior);
			} catch (Exception e) {
				System.out.println("Could not restore from state " + e.getMessage() + " trying to go back to old state");
				m_sStates[m_iCurrentParticleNr] = m_sOldStates[m_iCurrentParticleNr];
				String sStateFileName = getParticleDir(m_iCurrentParticleNr) + "/beast.xml.state";
				FileOutputStream xmlFile = new FileOutputStream(sStateFileName);
				PrintStream out = new PrintStream(xmlFile);
			    out.print(m_sStates[m_iCurrentParticleNr]);
				out.close();
				Thread.sleep(4000);
				state.restoreFromFile();
				operatorSchedule.restoreFromFile();
				oldLogLikelihood = robustlyCalcPosterior(posterior);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		k++;
	}
	
	
	
	String getParticleDir(int iParticle) {
		return m_sRootDir + "/particle" + formatter.format(iParticle);
	}

	synchronized void updateStates(int iSample ) throws Exception {
		boolean [] bNeedsUpdate = new boolean[m_nParticles];
		{
			String sStateFileName = m_sParticleDir + "/beast.xml.state";
			System.out.println("Loading " + sStateFileName);
			String sState = getTextFile(sStateFileName);
			if (sState == null) {
				System.out.println("State == null, retrying");
				state.storeToFile(iSample);
				operatorSchedule.storeToFile();
				Thread.sleep(1000);
				sState = getTextFile(sStateFileName);
			}
			String sLog = getTextFile(m_sParticleDir + "/" + ParticleFilter.POSTERIOR_LOG_FILE);
			String [] sLogs = sLog.split("\n");
			String sPosterior = sLogs[sLogs.length-1].split("\t")[1];
			double fPosteriors = Double.parseDouble(sPosterior);
			m_sOldStates[m_iCurrentParticleNr] = sState;
			m_fOldPosteriors[m_iCurrentParticleNr] = fPosteriors;
			m_sStates[m_iCurrentParticleNr] = sState;
			m_fPosteriors[m_iCurrentParticleNr] = fPosteriors;
			
			for (int i = 0; i < m_nParticles; i++) {
				if (m_sOldStates[i] == null) {
					m_sOldStates[i] = sState;
					m_fOldPosteriors[i] = fPosteriors;
				}
			}
		}

		try {
			for (int iParticle = 0; iParticle < m_nParticles; iParticle++) {
				System.out.println("Loading " + iParticle);
				m_sOldStates[iParticle] = m_sStates[iParticle];
				m_fOldPosteriors[iParticle] = m_fPosteriors[iParticle];
				
				// load state
				String sStateFileName = getParticleDir(iParticle) + "/beast.xml.state";
				if (new File(sStateFileName).exists()) {
					try {
						String sState = getTextFile(sStateFileName);
						// ensure we retreived proper XML
				        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				        //factory.setValidating(true);
				        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sState)));
				        doc.normalize();
				        // if we got this far, the state  is probably ok
						m_sStates[iParticle] = sState;
					} catch (Exception e) {
						// something went wrong loading the state,
						// let's stick with the old state
						System.out.println("Could not parse " + e.getMessage());
					}
					
					// load posterior
					String sLog = getTextFile(getParticleDir(iParticle) + "/" + ParticleFilter.POSTERIOR_LOG_FILE);
					String [] sLogs = sLog.split("\n");
					String sPosterior = sLogs[sLogs.length-1].split("\t")[1];
					m_fPosteriors[iParticle] = Double.parseDouble(sPosterior);
				} else {
					if (m_sStates[iParticle] == null) {
						m_sStates[iParticle] = m_sStates[0];
						m_fPosteriors[iParticle] = m_fPosteriors[0];
					}
				}
			}
	
			System.out.print(" " + DiscreteStatistics.mean(m_fPosteriors) + " " + DiscreteStatistics.variance(m_fPosteriors));
			System.out.print(" " + Arrays.toString(m_fPosteriors));
			System.out.println();
	
			// sample new states
			double fMax = m_fPosteriors[0];
			int iMax = 0;
			for (int i = 0; i < m_nParticles; i++) {
				if (fMax < m_fPosteriors[i]) {
					fMax = m_fPosteriors[i];
					iMax = i;
				}
			}
			
			for (int i = 0; i < m_nParticles; i++) {
				if (fMax - m_fPosteriors[i] > m_fThreshHold) {
					bNeedsUpdate[i] = true;
				}
			}
	
			
			double [] fPosteriors = new double[m_nParticles];
			for (int i = 0; i < m_nParticles; i++) {
				fPosteriors[i] = Math.exp(m_fPosteriors[i] - fMax);
			}
			double fSum = 0;
			for (int i = 0; i < m_nParticles; i++) {
				fSum += fPosteriors[i];
			}
			
			double [] fNewPosteriors = new double[m_nParticles];
			String [] sNewStates = new String[m_nParticles];
			
System.out.print("x1");
			fNewPosteriors[0] = m_fPosteriors[0];
			sNewStates[0] = m_sStates[0];
			for (int iParticle = m_iCurrentParticleNr; iParticle < m_iCurrentParticleNr+1; iParticle++) {
				if (bNeedsUpdate[iParticle]) {
					System.out.println(iSample + " Updating particle " + iParticle);
					double fRand = Randomizer.nextDouble() * fSum;
					int iNewState = 0;
					while (fRand > fPosteriors[iNewState]) {
						fRand -= fPosteriors[iNewState];
						iNewState++;
					}
System.out.print("B");
					fNewPosteriors[iParticle] = m_fPosteriors[iNewState];
					sNewStates[iParticle] = m_sStates[iNewState];
				}
			}	
System.out.print("x2");
	
	
if (false) {
			final double DELTA = 0.0025;
			
			// slightly perturb weights of operators
			for (int iParticle = m_iCurrentParticleNr; iParticle < m_iCurrentParticleNr+1; iParticle++) {
				if (bNeedsUpdate[iParticle]) {
					String [] sXML = sNewStates[iParticle].split("</itsabeastystatewerein>\n");
					String [] sStrs = sXML[1].split("\n");
					int nOperators = sStrs.length - 3; 
					double [] fWeights = new double[nOperators];
System.out.print("C");
		            for (int i = 0; i < nOperators; i++) {
		            	String [] sStrs2 = sStrs[i+2].split(" ");
		            	fWeights[i] = Double.parseDouble(sStrs2[1]);
		            }
System.out.print("D");
		            // convert from cumulative weights
		            for (int i = nOperators - 1; i > 0; i--) {
		            	fWeights[i] -= fWeights[i - 1];
		            }
System.out.print("E");
		            // delta exchange
		            for (int i = 0; i < nOperators; i++) {
		            	double fDelta = Randomizer.nextDouble() * DELTA;
		            	int iFrom = Randomizer.nextInt(nOperators);
		            	int iTo = Randomizer.nextInt(nOperators);
		            	if (iFrom != iTo && fWeights[iFrom] > fDelta && fWeights[iTo] < 1.0 - fDelta) {
		            		fWeights[iFrom] -= fDelta;
		            		fWeights[iTo] += fDelta;
		            	}
		            }
System.out.print("F");
		            // convert to cumulative weights
		            for (int i = 1; i < nOperators; i++) {
		            	fWeights[i] += fWeights[i - 1];
		            }
System.out.print("G");
		            fWeights[nOperators-1] = 1.0;
		            
		            String sStates = sXML[0] + "</itsabeastystatewerein>\n";
		            sStates += "<!--\nID Weight Paramvalue #Accepted #Rejected #CorrectionAccepted #CorrectionRejected\n";
		            for (int i = 0; i < nOperators; i++) {
		            	String [] sStrs2 = sStrs[i+2].split(" ");
		            	String sStr = sStrs2[0] + " " + fWeights[i] + " " + sStrs2[2] + " " + sStrs2[3] + " " + sStrs2[4] + " " + sStrs2[5] + " " + sStrs2[6] + "\n";
		            	sStates += sStr;
		            }
System.out.print("H");
		            sStates += "-->";
		            sNewStates[iParticle] = sStates;
				}
System.out.print("x3");
			}
}
			
			m_fPosteriors = fNewPosteriors;
			m_sStates = sNewStates;
		} catch (Exception e) {
			System.out.println("Something went wrong " + e.getClass().getName() + " " + e.getMessage() +", restoring to previous state");
			String [] tmp = m_sStates;
			m_sStates = m_sOldStates;
			m_sOldStates = tmp;
			double [] tmp2 = m_fPosteriors;
			m_fPosteriors = m_fOldPosteriors;
			m_fOldPosteriors = tmp2;
		}

		// write state files
		for (int iParticle = m_iCurrentParticleNr; iParticle < m_iCurrentParticleNr+1; iParticle++) {
			if (bNeedsUpdate[iParticle]) {
				String sStateFileName = getParticleDir(iParticle) + "/beast.xml.state";
		    	FileOutputStream xmlFile = new FileOutputStream(sStateFileName);
		    	PrintStream out = new PrintStream(xmlFile);
		        out.print(m_sStates[iParticle]);
				out.close();
			}
		}
	} // updateStates

	private String getTextFile(String sFileName) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(sFileName));
		StringBuffer buf = new StringBuffer();
		while (fin.ready()) {
			String sStr = fin.readLine();
			buf.append(sStr);
			buf.append('\n');
		}
		fin.close();
		return buf.toString();
	}
	
}
