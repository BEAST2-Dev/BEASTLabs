package beast.inference;

import java.io.File;
import java.io.PrintStream;

import beast.core.Description;
import beast.core.Input;
import beast.core.Logger;
import beast.core.MCMC;
import beast.util.Randomizer;



@Description("MCMC chain that synchronises through files. Can be used with ParticleFilter instead of plain MCMC.")
public class MCMCParticle extends MCMC {
	public Input<Integer> m_stepSize = new Input<Integer>("stepsize", "number of samples before switching state (default 10000)", 10000);

	int m_nStepSize;
	String m_sParticleDir;
	int k;
	
	@Override
	public void initAndValidate() {
		m_nStepSize = m_stepSize.get();
		m_sParticleDir = System.getProperty("beast.particle.dir");
		System.err.println("MCMCParticle living in " + m_sParticleDir);
		
		if (m_sParticleDir != null) {
			for (Logger logger : loggersInput.get()) {
				if (logger.fileNameInput.get() != null) {
					logger.fileNameInput.setValue(m_sParticleDir + "/" + logger.fileNameInput.get(), logger);
				}
			}
		}
		
		super.initAndValidate();
		k = 0;
	}
	
	
	@Override
	protected void callUserFunction(int iSample) {
		if (iSample % m_nStepSize == 0) {
			if (iSample == 0) {
				return;
			}
			doUpdateState(iSample);
		}
	}
	
	protected void doUpdateState(int iSample) {
		File f2 = new File(m_sParticleDir + "/particlelock" + k);
		File f = new File(m_sParticleDir + "/threadlock" + k);
		try {
			state.storeToFile(iSample);
			operatorSchedule.storeToFile();

			System.out.println(iSample + ": writing " + f2.getAbsolutePath());
			boolean bLockReady = false;
			while (!bLockReady) {
				try {
					PrintStream out = new PrintStream(f2);
					out.print("X");
					out.close();
					bLockReady = true;
				} catch (Exception e) {
					System.out.println("Attempt to write lock failed: " + e.getMessage());
					Thread.sleep(1000);
				}
			}
			if (checkstop(f, f2)) {
				System.exit(0);
			}
			System.out.println(iSample + ": waiting for " + f.getAbsolutePath());
			while (!lockExists()) {
				Thread.sleep(ParticleLauncherByFile.TIMEOUT);
				if (checkstop(f, f2)) {
					System.exit(0);
				}
				f = new File(m_sParticleDir + "/threadlock" + k);
			}
			// delay 50ms to prevent the file system getting confused?!?
			Thread.sleep(50);
			System.out.println(iSample + ": " + f.getAbsolutePath() + " exists");
			try {
				if (f.delete()) {
					System.out.println(iSample + ": " + f.getAbsolutePath() + " deleted");
				}
			} catch (Exception e) {
				System.out.println("could not delete " + f.getAbsolutePath() + ": " + e.getMessage());
			}
			
			Randomizer.setSeed(Randomizer.getSeed());
        	System.out.println("Seed = " + Randomizer.getSeed());
        	
			try {
				state.restoreFromFile();
				operatorSchedule.restoreFromFile();
			} catch (Exception e) {
				System.out.println("Restoring state failed: " + e.getMessage());
			}
			oldLogLikelihood = robustlyCalcPosterior(posterior);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		k++;
	}
	
	boolean lockExists() {
		File dir = new File(m_sParticleDir);
		String sLockFile = "threadlock" + k;
		for (String sFile : dir.list()) {
			if (sFile.endsWith(sLockFile)) {
				return true;
			}
		}
		return false;
	}

	final int UNKNOWN = 0, YES = 1, NO = 2;
	int isStopped = UNKNOWN;
	
	protected boolean checkstop(File f, File f2) {
		
		Thread t = new Thread() {
			public void run() {
				try {
					File dir = new File(m_sParticleDir);
					File stopFile = new File(dir.getParentFile().getAbsoluteFile() + "/stop");
					if (stopFile.exists()) {
						System.out.println("Stopped by " + stopFile.getAbsolutePath());
						isStopped = YES;
					} else { 
						isStopped = NO;
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
				
			}
		};
		

		isStopped = 0;
		t.start();
		
		for (int i = 0; i < 10; i++) {
			if (isStopped == YES) {
				if (f2.exists()) {
					f2.delete();
				}
				if (f.exists()) {
					f.delete();
				}
				return true;
			}
			if (isStopped == NO) {
				return false;
			}
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		System.err.println("Checking for stopfile got stuck, stopping check");
		t.stop();
		System.err.println("Resume normal run");
		return false;
	}
}
