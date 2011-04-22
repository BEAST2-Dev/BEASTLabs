package beast.app.simulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

/** Determines size of epidemic by monte carlo
 * Discretided time.
 * SIR model, where P(R|I)=1 after 1 time step (i.e. Infecteds 
 * only remain infectious for 1 time step)
 * Works on graph that connects Infecteds with Susceptibles.
 */
public class SIRonAGraph extends JPanel {
	final static int SUSCEPTABLE = 0;
	final static int INFECTED = 1;
	final static int RECOVERED = 2;

	final static int SEED = 2;
	Random rand = new Random(SEED);
	
	int [][] m_edges;
	List<Integer> m_infected;
	double [] m_fPosX;
	double [] m_fPosY;
	
	SIRonAGraph(int nSize, double fAverageDegree, double propInfected) {
		// generate random graph
		
		// first put items on a unit square
		m_fPosX = new double[nSize];
		m_fPosY = new double[nSize];
		for (int i = 0; i < nSize; i++) {
			m_fPosX[i] = rand.nextDouble();
			m_fPosY[i] = rand.nextDouble();
		}
		// determine distance between points
		double [][] fDist = new double[nSize][nSize];
		System.err.println("calc distance matrix");
		for (int i = 0; i < nSize; i++) {
			double posX1 = m_fPosX[i];
			double posY1 = m_fPosY[i];
			for (int j = i+1; j < nSize; j++) {
				double posX2 = m_fPosX[j];
				double posY2 = m_fPosY[j];
				fDist[i][j] = (posX1- posX2)*(posX1- posX2)+(posY1- posY2)*(posY1- posY2);
				fDist[j][i] = fDist[i][j];
			}
		}
		
		// transform distance matrix to 1/distance
		double [] fSumDist = new double[nSize];
		for (int i = 0; i < nSize; i++) {
			double [] fDist1 = fDist[i];
			fDist1[i] = Double.MAX_VALUE;
			for (int j = 0; j < nSize; j++) {
				fDist1[j] = 1.0/(fDist1[j] *fDist1[j]);
				//fDist1[j] = 1.0/fDist1[j];
				fSumDist[i] += fDist1[j];
			}
		}
		
		System.err.println("place edges");
		// place edges
		boolean[][]bEdges = new boolean[nSize][nSize];
		int nEdges = 0;
		int [] nNrOfEdges = new int[nSize];
		while (nEdges < fAverageDegree * nSize / 2) {
			double fNode1 = rand.nextDouble();
			int iNode1 = 0;
			if (nEdges > 0) {
				fNode1 *= nEdges * 2;
				fNode1 -= nNrOfEdges[iNode1];
				while (fNode1 > 0) {
					iNode1++;
					fNode1 -= nNrOfEdges[iNode1];
				}
			} else {
				iNode1 = rand.nextInt(nSize);
			}
			
			double[]fDist1 = fDist[iNode1];
			// select second node with probability proportional to inverse distance
			double fNode2 = rand.nextDouble() * fSumDist[iNode1];
			int iNode2 = 0;
			fNode2 -= fDist1[iNode2];
			while (fNode2 > 0) {
				iNode2++;
				fNode2 -= fDist1[iNode2];
			}
			if (iNode1 != iNode2 && !bEdges[iNode1][iNode2]) {
				bEdges[iNode1][iNode2] = true;
				bEdges[iNode2][iNode1] = true;
				nNrOfEdges[iNode1]++;
				nNrOfEdges[iNode2]++;
				nEdges++;
			}
		}

		System.err.print("connect graph");
		
//		fSumDist = new double[nSize];
//		for (int i = 0; i < nSize; i++) {
//			double [] fDist1 = fDist[i];
//			fDist1[i] = 0;
//			for (int j = 0; j < nSize; j++) {
//				fDist1[j] = Math.sqrt(fDist1[j]);
//				//fDist1[j] = 1.0/fDist1[j];
//				fSumDist[i] += fDist1[j];
//			}
//		}
		
		
		// Ensure graph is Connected
		boolean [] bDone = new boolean[nSize];
		int nMax = 0, iMax = 0;
		for (int i = 0; i < nSize; i++) {
			if (nNrOfEdges[i] > nMax) {
				iMax = i;
				nMax = nNrOfEdges[i];
			}
		}
		bDone[iMax] = true;
		getConnected(bEdges, bDone);
		int nDone = 0;
		for (int i = 0; i < nSize; i++) {
			if (bDone[i]) {
				nDone++;
			}
		}
		System.err.println(" " + (nSize - nDone));
		
		for (int i = 0; i < nSize; i++) {
			if (!bDone[i]) {
				boolean bProgress = false;
				do {
					int iNode1 = i;
					double[]fDist1 = fDist[iNode1];
					
					double fSum = 0;
					for (int j = 0; j < nSize; j++) {
						if (bDone[j]) {
							fSum += fDist1[j];
						}
					}
					double fNode2 = rand.nextDouble() * fSum;
					int iNode2 = 0;
					while (!bDone[iNode2]) {
						iNode2++;
					}
					fNode2 -= fDist1[iNode2];
					while (fNode2 > 0) {
						iNode2++;
						while (!bDone[iNode2]) {
							iNode2++;
						}
						fNode2 -= fDist1[iNode2];
					}
					
//					double fNode2 = rand.nextDouble() * fSumDist[iNode1];
//					int iNode2 = 0;
//					fNode2 -= fDist1[iNode2];
//					while (fNode2 > 0) {
//						iNode2++;
//						fNode2 -= fDist1[iNode2];
//					}
					if (iNode1 != iNode2 && bDone[iNode2] && !bEdges[iNode1][iNode2]) {
						nDone++;
						if (nDone % 100 == 0) { 
							System.err.print('x');
						}
						bEdges[iNode1][iNode2] = true;
						bEdges[iNode2][iNode1] = true;
						bDone[iNode1] = true;
						getConnected(bEdges, bDone);
						bProgress = true;
					}
				} while (!bProgress);

			}
		}
		
		// convert matrix data structure to edge list data structure
		
		System.err.println("convert data structure");
		m_edges = new int[nSize][];
		for (int i = 0; i < nSize; i++) {
			nEdges = 0;
			boolean [] bEdges2 = bEdges[i]; 
			for (int j = 0; j < nSize; j++) {
				if (bEdges2[j]) {
					nEdges++;
				}
			}
			m_edges[i] = new int[nEdges];
			nEdges = 0;
			for (int j = 0; j < nSize; j++) {
				if (bEdges2[j]) {
					m_edges[i][nEdges++] = j;
				}
			}
		}
			
		System.err.println("initial infections");
		m_infected = new ArrayList<Integer>();
		int i = 0;
		while (i < nSize * propInfected) {
			int j = rand.nextInt(nSize);
			if (!m_infected.contains(j)) {
				i++;
				m_infected.add(j);
			}
			
		}
		Collections.sort(m_infected);
		
		// calc degree stats
		int [] degrees = new int[nSize];
		for (i = 0; i < nSize; i++) {
			degrees[m_edges[i].length]++;
		}
		for (i = 0; i < 50; i++) {
			System.out.println(i + " " + degrees[i]);
		}
	}

	/** return which nodes are connected to node 0 **/ 
	void getConnected(boolean [][] bEdges, boolean [] bDone) {
		int nSize = bEdges.length;
		//boolean [] bDone = new boolean[nSize];
		//bDone[0] = true;
		boolean bProgress = true;
		while (bProgress) {
			bProgress = false;
			for (int j = 0; j < nSize; j++) {
				if (bDone[j]) {
					for (int edge = 0; edge < nSize; edge++) {
						if (bEdges[j][edge] && !bDone[edge]) {
							bDone[edge] = true;
							bProgress = true;
						}
					}
				}
			}
		}
	}
	
	public void printDotty() {
		System.out.println("digraph randomgraph {");
		for (int i = 0; i < m_edges.length; i++) {
			int [] edges = m_edges[i];
			for (int j: edges) {
				System.out.print(i + "->" + j + ";");
			}
			System.out.println();
		}
		System.out.println("}");
	}
	
	private int simulateOnce(double rho) {
		List<Integer> infected = new ArrayList<Integer>();
		infected.addAll(m_infected);
		int nTotalInfected = 0;
		int nSize = m_edges.length;
		int [] status = new int[nSize]; 
		for (int i : infected) {
			status[i] = INFECTED;
		}
		while (infected.size() > 0) {
			nTotalInfected += infected.size();
			List<Integer> newlyInfected = new ArrayList<Integer>();
			for (int i : infected) {
				for (int edge : m_edges[i]) {
					if (rand.nextDouble() < rho && status[edge] == SUSCEPTABLE) {
						status[edge] = INFECTED;
						newlyInfected.add(edge);
					}
				}
			}
			for (int i : infected) {
				status[i] = RECOVERED;
			}
			infected = newlyInfected;
		}
		return nTotalInfected;
	}

	private double simulate(double rho, int nSamples) {
		int [] samples = new int[nSamples];
		for (int i = 0; i < nSamples; i++) {
			int nTotalInfected = simulateOnce(rho);
			samples[i] = nTotalInfected;
		}
		double fSum = 0;
		for (int i : samples) {
			fSum+=i;
		}
		return fSum / nSamples;
	}


	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setBackground(Color.WHITE);
		int nWidth = getWidth();
		int nHeight = getHeight();
		g2.clearRect(0, 0, nWidth, nHeight);
		
		int nSize = m_edges.length;
		for (int i = 0; i < nSize; i++) {
			int x = (int)(nWidth * m_fPosX[i]);
			int y = (int)(nHeight * m_fPosY[i]);
			g2.drawRect(x, y, 2, 2);
			int [] edges = m_edges[i];
			for (int j = 0; j < edges.length; j++) {
				int k = edges[j];
				int x2 = (int)(nWidth * m_fPosX[k]);
				int y2 = (int)(nHeight * m_fPosY[k]);
				g2.drawLine(x, y, x2, y2);
			}
		}
		
	} // paintComponent	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int nNodes = 5000;
		double fAverageDegree = 10;
		double propInfected = 0.01;
		double rho = 0.3;
		int nSamples = 2500;
		SIRonAGraph sirSimulator = new SIRonAGraph(nNodes, fAverageDegree, propInfected);
		System.err.println("Start sampling");

		JFrame frame = new JFrame("RateMatrixBySampling");		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		sirSimulator.setVisible(true);
		frame.add(sirSimulator,BorderLayout.CENTER);
		frame.setSize(600, 800);
		frame.setVisible(true);
	
		long nStart = System.currentTimeMillis();
		double f = sirSimulator.simulate(rho, nSamples);
		long nEnd = System.currentTimeMillis();
		System.err.println("Mean size " + f + " time " + (nEnd - nStart)/1000.0 + " seconds");
	}


} // class SIR
