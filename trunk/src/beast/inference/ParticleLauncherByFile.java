package beast.inference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

public class ParticleLauncherByFile extends ParticleLauncher {

	final public static int TIMEOUT = 200;
	
	File stopFile;
	
	@Override
	public void run() {
		if (stopFile == null) {
			stopFile = new File(m_filter. m_sRootDir.get() + "/stop");
		}

		try {
			String sParticleDir = m_filter.getParticleDir(m_iParticle);
			m_filter.updateState(m_iParticle);

			FileOutputStream sScriptFile = new FileOutputStream(sParticleDir + "/run2.sh");
			PrintStream out = new PrintStream(sScriptFile);
			String sCommand = getCommand(sParticleDir);
            out.print(sCommand);
			out.close();

			Process p = Runtime.getRuntime().exec("sh " + sParticleDir + "/run2.sh");

			for (int k = 0; k < m_filter.m_nSteps; k++) {
				File f = new File(sParticleDir + "/particlelock" + k);
				File f2 = new File(sParticleDir + "/threadlock" + k);
				System.out.println(m_iParticle + ": waiting for " + f.getAbsolutePath());
				if (checkstop(f, f2)) { 
					return;
				}
				while (!f.exists()) {
					if (checkstop(f, f2)) { 
						return;
					}
					Thread.sleep(TIMEOUT);
				}
				//System.out.println(m_iParticle + ": " + f.getAbsolutePath() + " exists");
				if (f.delete()) {
					//System.out.println(m_iParticle + ": " + f.getAbsolutePath() + " deleted");
				}
				
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
				PrintStream lockout = new PrintStream(f2);
				lockout.print("X");
				lockout.close();
				
				//m_filter.updateState(m_iParticle);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		m_filter.m_nCountDown.countDown();
	}


	private boolean checkstop(File f, File f2) {
		if (stopFile.exists()) {
			System.out.println("Stopped by " + stopFile.getAbsolutePath());
			if (f2.exists()) {
				f2.delete();
			}
			if (f.exists()) {
				f.delete();
			}
			return true;
		}
		return false;
	}

}
