package beast.inference;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class ParticleLauncher extends Thread {
	int m_iParticle;
	ParticleFilter m_filter;

	public ParticleLauncher() {}
	
	public void setParticle(int iParticle, ParticleFilter filter) {
		m_iParticle = iParticle;
		m_filter = filter;
	}
	
	@Override
	public void run() {
		try {
			String sParticleDir = m_filter.getParticleDir(m_iParticle);
        	FileOutputStream sScriptFile = new FileOutputStream(sParticleDir + "/run.sh");
        	PrintStream out = new PrintStream(sScriptFile);
			String sCommand = "#!/bin/sh\nchdir " + sParticleDir + "\n"+
							"java -Dbeast.debug=false -Djava.only=true -cp " + System.getProperty("java.class.path") + " beast.app.BeastMCMC -overwrite -seed " + (m_iParticle+100)+ " " + sParticleDir + "/beast.xml >> " + sParticleDir + "/beast.log 2>&1 \n"+
							"exit\n";
            out.print(sCommand);
			out.close();
			if (!m_filter.isResuming()) {
				Process p = Runtime.getRuntime().exec("sh " + sParticleDir + "/run.sh");
				p.waitFor();
				m_filter.updateState(m_iParticle);
			}

			sScriptFile = new FileOutputStream(sParticleDir + "/run2.sh");
        	out = new PrintStream(sScriptFile);
			sCommand = "#!/bin/sh\nchdir " + sParticleDir + "\n" +
					"java -Dbeast.debug=false -Djava.only=true -cp " + System.getProperty("java.class.path") + " beast.app.BeastMCMC -resume -seed " + (m_iParticle+100)+ " " + sParticleDir + "/beast.xml >> " + sParticleDir + "/beast.log 2>&1 \n" +
					"exit\n";
            out.print(sCommand);
			out.close();
			for (int k = 0; k < m_filter.m_nSteps; k++) {
				Process p = Runtime.getRuntime().exec("sh " + sParticleDir + "/run2.sh");
				p.waitFor();
				m_filter.updateState(m_iParticle);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		m_filter.m_nCountDown.countDown();
	}
}
