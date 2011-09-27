package beast.inference;

import java.io.File;
import java.io.PrintStream;

import beast.core.Input;
import beast.core.MCMC;

public class MCMCParticle extends MCMC {
	public Input<Integer> m_stepSize = new Input<Integer>("stepsize", "number of samples before switching state (default 10000)", 10000);

	int m_nStepSize;
	String m_sParticleDir;
	File f;
	File f2;
	
	@Override
	public void initAndValidate() throws Exception {
		m_nStepSize = m_stepSize.get();
		m_sParticleDir = System.getProperty("beast.particle.dir");
		System.err.println("MCMCParticle living in " + m_sParticleDir);
		
		f2 = new File(m_sParticleDir + "/particlelock");
		f = new File(m_sParticleDir + "/threadlock");

		super.initAndValidate();
	}
	
	
	@Override
	protected void callUserFunction(int iSample) {
		if ((iSample +1) % m_nStepSize == 0) {
			try {
				state.storeToFile();
				operatorSet.storeToFile();

				PrintStream out = new PrintStream(f2);
				out.print("X");
				out.close();
				while (!f.exists()) {
					Thread.sleep(100);
				}
				if (f.delete()) {
					System.out.println(iSample + ": " + f.getAbsolutePath() + " deleted");
				}
				
				state.restoreFromFile();
				operatorSet.restoreFromFile();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
}
