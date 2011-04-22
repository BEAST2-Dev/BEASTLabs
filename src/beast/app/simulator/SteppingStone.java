package beast.app.simulator;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentListener;
import java.awt.print.Printable;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;
import beast.util.Randomizer;


@Description("Simluator for stochastis stepping stone model of migration/drift." +
		"Median allele density is shown on a display, log files contain median, min, max and 95 HDP")
public class SteppingStone extends beast.core.Runnable {
	
	public Input<Double> m_gammaInput = new Input<Double>("gamma", "gamma parameter of genetic drift", Validate.REQUIRED);
	public Input<Double> m_deltaTInput = new Input<Double>("deltaT", "Size of time step in Euler's method", Validate.REQUIRED);
	public Input<Integer> m_stepsInput = new Input<Integer>("steps", "Number of steps to run in discrete time approximation -- Euler's method (default 1000)", 1000);
	public Input<Integer> m_samplesInput = new Input<Integer>("samples", "number of samples to run the simulation (defaul 100)", 100);
	public Input<List<Island>> m_islandsInput = new Input<List<Island>>("island", "list of island and its neighbors", new ArrayList<Island>(), Validate.REQUIRED); 
	
	List<String> m_sIslandNames;
	/** m_nNeighbors[x][y] is neighbor nr y for island x **/
	int[][] m_nNeighbors;
	/** m_nNeighbors[x][y] is migration rate from island x to island nr m_nNeighbors[x][y] **/
	Double[][] m_fRates;
	
	Double [] m_fInitial;
	Double [] m_fPosX;
	Double [] m_fPosY;
	
	double m_fGamma;
	double m_fSqrtDeltaT;
	
	@Override
	public void initAndValidate() throws Exception {
		m_fGamma = m_gammaInput.get();
		List<Island> islands = m_islandsInput.get();
		m_sIslandNames = new ArrayList<String>();
		m_fInitial = new Double[islands.size()];
		m_fPosX = new Double[islands.size()];
		m_fPosY = new Double[islands.size()];
		for (int i = 0; i < islands.size(); i++) {
			m_sIslandNames.add(islands.get(i).getID());
			m_fInitial[i] = islands.get(i).m_initialPropInput.get();
			m_fPosX[i] = islands.get(i).m_posXInput.get();
			m_fPosY[i] = islands.get(i).m_posYInput.get();
		}
		m_nNeighbors = new int[islands.size()][];
		m_fRates = new Double[islands.size()][];
		for (int i = 0; i < islands.size(); i++) {
			Island island = islands.get(i);
			int nNeighbours = island.m_neighborsInput.get().size();
			m_nNeighbors[i] = new int[nNeighbours];
			m_fRates[i] = new Double[nNeighbours];
			for (int j = 0; j < nNeighbours; j++) {
				Neighbor neighbor = island.m_neighborsInput.get().get(j);
				String sID = neighbor.m_neighborsInput.get();
				m_nNeighbors[i][j] = m_sIslandNames.indexOf(sID);
				if (m_nNeighbors[i][j] < 0) {
					throw new Exception("Could not find island with id " + sID);
				}
				m_fRates[i][j] = neighbor.m_migrationRateInput.get() * m_deltaTInput.get();
			}
		}
		m_fSqrtDeltaT = Math.sqrt(m_deltaTInput.get());
	}

	
	double [][][] m_fProp;
	
	void  log() throws Exception {
		int nSamples = m_samplesInput.get();
		int iMedian = nSamples / 2;
		log("_median.log", iMedian);
		logHPD(0.95, iMedian);
		log("_min.log", 0);
		log("_max.log", nSamples - 1);
	}
	
	private void logHPD(double d, int iMedian) throws FileNotFoundException {
		int nSteps = m_stepsInput.get();
		int nIslands = m_sIslandNames.size();

		PrintStream outUp = new PrintStream("_95HPDUp.log");
		PrintStream outLow = new PrintStream("_95HPDLow.log");
		outUp.print("samplenr\t");
		outLow.print("samplenr\t");
		for (int i = 0; i < nIslands; i++) {
			outUp.print(m_sIslandNames.get(i) +"\t");
			outLow.print(m_sIslandNames.get(i) +"\t");
		}
		outUp.println();
		outLow.println();
		for (int iTime = 0; iTime < nSteps; iTime++) {
			outUp.print(iTime+"\t");
			outLow.print(iTime+"\t");
			for (int i = 0; i < nIslands; i++) {
				double [] f = HPDInterval(d, m_fProp[i][iTime]);
				outLow.print(f[0] +"\t");
				outUp.print(f[1] +"\t");
			}
			outUp.println();
			outLow.println();
		}
		outUp.close();
		outLow.close();
		
	}

	void log(String sFile, int iMedian) throws FileNotFoundException {
		int nSteps = m_stepsInput.get();
		int nIslands = m_sIslandNames.size();

		PrintStream out = new PrintStream(sFile);
		out.print("samplenr\t");
		for (int i = 0; i < nIslands; i++) {
			out.print(m_sIslandNames.get(i) +"\t");
		}
		out.println();
		for (int iTime = 0; iTime < nSteps; iTime++) {
			out.print(iTime+"\t");
			for (int i = 0; i < nIslands; i++) {
				out.print(m_fProp[i][iTime][iMedian] +"\t");
			}
			out.println();
		}
		out.close();
	}

	
    public static double[] HPDInterval(double proportion, double[] x) {

        double minRange = Double.MAX_VALUE;
        int hpdIndex = 0;

        final int diff = (int) Math.round(proportion * (double) x.length);
        for (int i = 0; i <= (x.length - diff); i++) {
            final double minValue = x[i];
            final double maxValue = x[i + diff - 1];
            final double range = Math.abs(maxValue - minValue);
            if (range < minRange) {
                minRange = range;
                hpdIndex = i;
            }
        }
        return new double[]{x[hpdIndex], x[hpdIndex + diff - 1]};
    }	
	
	void sort() {
		int nSteps = m_stepsInput.get();
		int nIslands = m_sIslandNames.size();
		for (int i = 0; i < nIslands; i++) {
			for (int iTime = 1; iTime < nSteps; iTime++) {
				Arrays.sort(m_fProp[i][iTime]);
			}
		}
	}
	
	void runSimulation() {
		int nSteps = m_stepsInput.get();
		int nIslands = m_sIslandNames.size();
		int nSamples = m_samplesInput.get();
		m_fProp = new double[nIslands][nSteps][nSamples];
		
		// run the desired nr of samples
		for (int iSample = 0; iSample < nSamples; iSample++) {
			// initialize
			for (int i = 0; i < nIslands; i++) {
				m_fProp[i][0][iSample] = m_fInitial[i];
			}
			// do the remaining steps
			for (int iTime = 1; iTime < nSteps; iTime++) {
				// process islands one by one
				processIslands(iTime, iSample);
			}
		}
		
	}
	
	private void processIslands(int iTime, int iSample) {
		final int nIslands = m_sIslandNames.size();
		for (int i = 0; i < nIslands; i++) {
			double fOldProp = m_fProp[i][iTime-1][iSample];
			double fNewProp = fOldProp;
			int [] nNeighbors = m_nNeighbors[i];
			for (int iNeighbor = 0; iNeighbor < nNeighbors.length; iNeighbor++) {
				int nNeighbor = nNeighbors[iNeighbor]; 
				double fDiff = m_fProp[nNeighbor][iTime-1][iSample] - fOldProp;
				fNewProp += fDiff * m_fRates[i][iNeighbor];
			}
			
	
			double f = Math.sqrt(m_fGamma * fOldProp * (1.0 - fOldProp));
			fNewProp += f * Randomizer.nextGaussian() * m_fSqrtDeltaT;
			if (fNewProp < 0) {
				fNewProp = 0;
			}
			if (fNewProp > 1.0) {
				fNewProp = 1.0;
			}
			m_fProp[i][iTime][iSample] = fNewProp; 
		}
	}

public class IslandModelPanel extends JPanel implements ChangeListener {
	private static final long serialVersionUID = 1L;


	IslandModelPanel() {
	}

	int m_nCurrentTime = 0;
	
	public void simulate() {
		m_nCurrentTime = 0;
		repaint();
	}
	
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setBackground(Color.WHITE);
		int nWidth = getWidth();
		int nHeight = getHeight();
		g2.clearRect(0, 0, nWidth, nHeight);
		

		int nIslands = m_sIslandNames.size();
		int [] x = new int[nIslands];
		int [] y = new int[nIslands];
		
		for (int i = 0; i < nIslands; i++) {
			x[i] = (int)(m_fPosX[i] * nWidth);
			y[i] = (int)(m_fPosY[i] * nHeight);
			g2.drawOval(x[i]-3, y[i]-3, 6, 6);
			g2.drawString(m_sIslandNames.get(i), x[i], y[i]-10);
		}
		for (int i = 0; i < nIslands; i++) {
			for (int j = 0; j < m_nNeighbors[i].length; j++) {
				g2.drawLine(x[i], y[i], x[m_nNeighbors[i][j]], y[m_nNeighbors[i][j]]);
			}
		}
		g2.setColor(Color.BLUE);
		int nMedian = m_fProp[0][0].length / 2;
		for (int i = 0; i < nIslands; i++) {
			int w = (int)(m_fProp[i][m_nCurrentTime][nMedian] * 100);
			g2.setColor(Color.BLUE);
			g2.fillRect(x[i], y[i], w, 8);
			g2.setColor(Color.BLACK);
			g2.drawRect(x[i], y[i], 100, 8);
		}
		
		if (m_nCurrentTime < m_fProp[0].length) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			m_slider.setValue(m_slider.getValue()+1);
			//repaint();
		}
	} // paintComponent	

	@Override
	public void stateChanged(ChangeEvent e) {
		m_nCurrentTime = m_slider.getValue();
		repaint();
	}


} // class IslandModelPanel


	JSlider m_slider; 

	@Override
	public void run() throws Exception {
		runSimulation();
		sort();
		log();

		IslandModelPanel islandModelSimulator = new IslandModelPanel();
		int nSamples = m_fProp[0].length;
		m_slider = new JSlider(JSlider.HORIZONTAL, 0, nSamples, nSamples);
		m_slider.addChangeListener(islandModelSimulator);
	
		JFrame frame = new JFrame("Stepping Stone Model");		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		islandModelSimulator.setVisible(true);
		frame.add(islandModelSimulator,BorderLayout.CENTER);
		frame.add(m_slider,BorderLayout.SOUTH);
		frame.setSize(600, 800);
		frame.setVisible(true);
	
		islandModelSimulator.simulate();
		// wait forever
		try {
			Thread.sleep(1000000000);
		} catch (InterruptedException e) {
		}
	}


} // class IslandModel
