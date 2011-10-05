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

import beast.math.statistic.DiscreteStatistics;
import beast.util.Randomizer;

public class MCMCParticleAsync extends MCMCParticle {
	
	/** states and associated posteriors. Used to sample next state from **/
	String [] m_sStates;
	double [] m_fPosteriors;
	int m_nParticles;
	String m_sRootDir;
	DecimalFormat formatter;
	
	int m_iCurrentParticleNr;
	
	@Override
	public void initAndValidate() throws Exception {
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
	}


	protected void doUpdateState(int iSample) {
		try {
			// park old state file, so that processes reading this file 
			// do not get interrupted (though they will have an old state)
			File f = new File(m_sParticleDir + "/beast.xml.state");
			File oldF = new File(m_sParticleDir + "/beast.xml.state.old");
			f.renameTo(oldF);

			// write new state file
			state.storeToFile();
			operatorSet.storeToFile();

			updateStates();
        	
			state.restoreFromFile();
			operatorSet.restoreFromFile();
			fOldLogLikelihood = robustlyCalcPosterior(posterior);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		k++;
	}
	
	
	
	String getParticleDir(int iParticle) {
		return m_sRootDir + "/particle" + formatter.format(iParticle);
	}

	synchronized void updateStates() throws Exception {
		for (int iParticle = 0; iParticle < m_nParticles; iParticle++) {
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
				}
				
				// load posterior
				String sLog = getTextFile(getParticleDir(iParticle) + "/" + ParticleFilter.POSTERIOR_LOG_FILE);
				String [] sLogs = sLog.split("\n");
				String sPosterior = sLogs[sLogs.length-1].split("\t")[1];
				m_fPosteriors[iParticle] = Double.parseDouble(sPosterior);
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
		
		fNewPosteriors[0] = m_fPosteriors[0];
		sNewStates[0] = m_sStates[0];
		for (int iParticle = m_iCurrentParticleNr; iParticle < m_iCurrentParticleNr+1; iParticle++) {
			double fRand = Randomizer.nextDouble() * fSum;
			int iNewState = 0;
			while (fRand > fPosteriors[iNewState]) {
				fRand -= fPosteriors[iNewState];
				iNewState++;
			}
			fNewPosteriors[iParticle] = m_fPosteriors[iNewState];
			sNewStates[iParticle] = m_sStates[iNewState];
		}	

		
		final double DELTA = 0.0025;
		
		// slightly perturb weights of operators
		for (int iParticle = m_iCurrentParticleNr; iParticle < m_iCurrentParticleNr+1; iParticle++) {
			String [] sXML = sNewStates[iParticle].split("</itsabeastystatewerein>\n");
			String [] sStrs = sXML[1].split("\n");
			int nOperators = sStrs.length - 3; 
			double [] fWeights = new double[nOperators];
            for (int i = 0; i < nOperators; i++) {
            	String [] sStrs2 = sStrs[i+2].split(" ");
            	fWeights[i] = Double.parseDouble(sStrs2[1]);
            }
            // convert from cumulative weights
            for (int i = nOperators - 1; i > 0; i--) {
            	fWeights[i] -= fWeights[i - 1];
            }
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
            // convert to cumulative weights
            for (int i = 1; i < nOperators; i++) {
            	fWeights[i] += fWeights[i - 1];
            }
            fWeights[nOperators-1] = 1.0;
            
            String sStates = sXML[0] + "</itsabeastystatewerein>\n";
            sStates += "<!--\nID Weight Paramvalue #Accepted #Rejected #CorrectionAccepted #CorrectionRejected\n";
            for (int i = 0; i < nOperators; i++) {
            	String [] sStrs2 = sStrs[i+2].split(" ");
            	String sStr = sStrs2[0] + " " + fWeights[i] + " " + sStrs2[2] + " " + sStrs2[3] + " " + sStrs2[4] + " " + sStrs2[5] + " " + sStrs2[6] + "\n";
            	sStates += sStr;
            }
            sStates += "-->";
            sNewStates[iParticle] = sStates;
		}
		
		
		m_fPosteriors = fNewPosteriors;
		m_sStates = sNewStates;
		// write state files
		for (int iParticle = m_iCurrentParticleNr; iParticle < m_iCurrentParticleNr+1; iParticle++) {
			String sStateFileName = getParticleDir(iParticle) + "/beast.xml.state";
	    	FileOutputStream xmlFile = new FileOutputStream(sStateFileName);
	    	PrintStream out = new PrintStream(xmlFile);
	        out.print(m_sStates[iParticle]);
			out.close();
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
