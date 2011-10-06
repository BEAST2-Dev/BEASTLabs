package beast.inference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

public class ParticleLauncherByFile extends ParticleLauncher {

	final public static int TIMEOUT = 1000;
	
	
	@Override
	public void run() {

		try {
			String sParticleDir = m_filter.getParticleDir(m_iParticle);
			m_filter.updateState(m_iParticle);

			FileOutputStream sScriptFile = new FileOutputStream(sParticleDir + "/run2.sh");
			PrintStream out = new PrintStream(sScriptFile);
			String sCommand = getCommand(sParticleDir);
            out.print(sCommand);
			out.close();

			Process p = Runtime.getRuntime().exec("sh " + sParticleDir + "/run2.sh");
			if (m_filter.m_mcmc.get() instanceof MCMCParticleAsync) {
				p.waitFor();
				m_filter.m_nCountDown.countDown();
				return;
			}

			for (int k = 0; k < m_filter.m_nSteps; k++) {
				File f = new File(sParticleDir + "/particlelock" + k);
				File f2 = new File(sParticleDir + "/threadlock" + k);
				System.out.println(m_iParticle + ": waiting for " + f.getAbsolutePath());
				if (checkstop(f, f2)) { 
					return;
				}
				int sleepCount = 0;
				while (!f.exists()) {
					if (checkstop(f, f2)) { 
						return;
					}
					Thread.sleep(TIMEOUT);
					f = new File(sParticleDir + "/particlelock" + k);
					sleepCount++;
					if (sleepCount % 100 == 0) {
						System.out.print(" " + m_iParticle);
					}
				}
				// delay 50ms to prevent the file system getting confused?!?
				Thread.sleep(50);
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
				boolean bStale = false;
				do {
					Thread.sleep(5000);
					// see if the threadlock file still exists.
					// if so, refresh the file.
					if (f2.exists()) {
						bStale = !f2.delete();
						Thread.sleep(50);
						lockout = new PrintStream(f2);
						lockout.print("X");
						lockout.close();
					}
				} while (bStale);
				
				
				//m_filter.updateState(m_iParticle);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		m_filter.m_nCountDown.countDown();
	}


	private boolean checkstop(File f, File f2) {
		File stopFile = new File(m_filter. m_sRootDir.get() + "/stop");
		if (stopFile.exists()) {
			System.out.println("Stopped by " + stopFile.getAbsolutePath());
			if (f2.exists()) {
				f2.delete();
			}
			if (f.exists()) {
				f.delete();
			}
			m_filter.m_nCountDown.countDown();
			return true;
		}
		return false;
	}

}
