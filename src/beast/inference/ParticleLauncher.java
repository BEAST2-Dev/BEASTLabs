package beast.inference;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

import beast.util.Randomizer;

public class ParticleLauncher extends Thread {
	int m_iParticle;
	ParticleFilter m_filter;

	static CountDownLatch barrier;
	static CountDownLatch barrier2;
	public ParticleLauncher() {}
	
	public void setParticle(int iParticle, ParticleFilter filter) {
		m_iParticle = iParticle;
		m_filter = filter;
		if (m_iParticle == 0) {
			barrier = new CountDownLatch(filter.m_nParticles);
			barrier2 = new CountDownLatch(filter.m_nParticles);
		}
	}
	
	@Override
	public void run() {
		try {
			String sParticleDir = m_filter.getParticleDir(m_iParticle);
//        	FileOutputStream sScriptFile = new FileOutputStream(sParticleDir + "/run.sh");
//        	PrintStream out = new PrintStream(sScriptFile);
//			String sCommand = "#!/bin/sh\nchdir " + sParticleDir + "\n"+
//							"java -Dbeast.debug=false -Djava.only=true -cp " + System.getProperty("java.class.path") + " beast.app.BeastMCMC -overwrite -seed " + (m_iParticle+100)+ " " + sParticleDir + "/beast.xml >> " + sParticleDir + "/beast.log 2>&1 \n"+
//							"exit\n";
//            out.print(sCommand);
//			out.close();
//			if (!m_filter.isResuming()) {
//				Process p = Runtime.getRuntime().exec("sh " + sParticleDir + "/run.sh");
//				p.waitFor();
				m_filter.updateState(m_iParticle);
//			}

			for (int k = 0; k < m_filter.m_nSteps; k++) {
				FileOutputStream sScriptFile = new FileOutputStream(sParticleDir + "/run2.sh");
				PrintStream out = new PrintStream(sScriptFile);
				String sCommand = "#!/bin/sh\nchdir " + sParticleDir + "\n" +
				// " -Djava.only=true "
						"java -Dbeast.debug=false -Djava.library.path=" + System.getProperty("java.library.path") + 
						" -cp " + System.getProperty("java.class.path") + " beast.app.BeastMCMC -resume -seed " + Randomizer.nextInt()+ " " + sParticleDir + "/beast.xml >> " + sParticleDir + "/beast.log 2>&1 \n" +
						"exit\n";
	            out.print(sCommand);
				out.close();

				Process p = Runtime.getRuntime().exec("sh " + sParticleDir + "/run2.sh");
				p.waitFor();
				
				// synchronise
				barrier.countDown();
				barrier.await();
				if (m_iParticle == 0) {
					barrier = new CountDownLatch(m_filter.m_nParticles);
					m_filter.updateStates(k);
				}
				barrier2.countDown();
				barrier2.await();
				if (m_iParticle == 0) {
					barrier2 = new CountDownLatch(m_filter.m_nParticles);
				}
				//m_filter.updateState(m_iParticle);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		m_filter.m_nCountDown.countDown();
	}
}
