
/*
 * File ClusterTree.java
 *
 * Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
 *
 * This file is part of BEAST2.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */
package beast.util;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.List;
import java.util.ArrayList;

import beast.evolution.alignment.Alignment;
import beast.core.Description;
import beast.core.Input;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

/** Adapted from Weka's HierarchicalClustering class **/
@Description("Create initial beast.tree by hierarchical clustering, either through one of the classic link methods " +
		"or by neighbor joining. The following link methods are supported: " +
		"<br/>o single link, " +
		"<br/>o complete link, " +
		"<br/>o UPGMA=average link, " +
		"<br/>o mean link, " +
		"<br/>o centroid, " +
		"<br/>o Ward and " +
		"<br/>o adjusted complete link")
public class ClusterTree extends Tree {

	public Input<String> m_sClusterType = new Input<String>("clusterType", "type of clustering algorithm used for generating initial beast.tree. " +
			"Should be one of " + 
			M_SINGLE + ", " +
			M_AVERAGE + ", " +
			M_COMPLETE + ", " +
			M_UPGMA + ", " +
			M_MEAN + ", " +
			M_CENTROID + ", " +
			M_WARD + ", " +
			M_ADJCOMPLETE + ", or " +
			M_NEIGHBORJOINING + "."
			);
	public Input<Alignment> m_pData = new Input<Alignment>("taxa", "alignment data used for calculating distances for clustering");
	public Input<String> m_oNodeType = new Input<String>("nodetype", "type of the nodes in the beast.tree", Node.class.getName());

	/** Whether the distance represent node height (if false) or branch length (if true). */
	protected boolean m_bDistanceIsBranchLength = false;

	@Override
	public void initAndValidate() throws Exception {
		String sType = m_sClusterType.get().toLowerCase();
		if (sType.equals(M_SINGLE)) {m_nLinkType = SINGLE;}
		else if (sType.equals(M_COMPLETE)) {m_nLinkType = COMPLETE;}
		else if (sType.equals(M_AVERAGE)) {m_nLinkType = AVERAGE;}
		else if (sType.equals(M_UPGMA)) {m_nLinkType = AVERAGE;}
		else if (sType.equals(M_MEAN)) {m_nLinkType = MEAN;}
		else if (sType.equals(M_CENTROID)) {m_nLinkType = CENTROID;}
		else if (sType.equals(M_WARD)) {m_nLinkType = WARD;}
		else if (sType.equals(M_ADJCOMPLETE)) {m_nLinkType = ADJCOMLPETE;}
		else if (sType.equals(M_NEIGHBORJOINING)) {m_nLinkType = NEIGHBOR_JOINING;m_bDistanceIsBranchLength = true;}
		else {
			System.err.println("Warning: unrecognized cluster type. Using Average/UPGMA.");
			m_nLinkType = AVERAGE;
		}
		Node root = buildClusterer();
		setRoot(root);
		root.labelInternalNodes((getNodeCount()+1)/2);
		super.initAndValidate();
	}

	Node newNode() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (Node) Class.forName(m_oNodeType.get()).newInstance();
		//return new NodeData();
	}

	final static String M_SINGLE = "single";
	final static String M_AVERAGE = "average";
	final static String M_COMPLETE = "complete";
	final static String M_UPGMA = "upgma";
	final static String M_MEAN = "mean";
	final static String M_CENTROID = "centroid";
	final static String M_WARD = "ward";
	final static String M_ADJCOMPLETE = "adjcomplete";
	final static String M_NEIGHBORJOINING = "neighborjoining";
	/** the various link types */
	final static int SINGLE = 0;
	final static int COMPLETE = 1;
	final static int AVERAGE = 2;
	final static int MEAN = 3;
	final static int CENTROID = 4;
	final static int WARD = 5;
	final static int ADJCOMLPETE = 6;
	final static int NEIGHBOR_JOINING = 7;

	/**
	 * Holds the Link type used calculate distance between clusters
	 */
	int m_nLinkType = SINGLE;

	public ClusterTree() {
	} // c'tor


	/** class representing node in cluster hierarchy **/
	class NodeX {
		NodeX m_left;
		NodeX m_right;
		NodeX m_parent;
		int m_iLeftInstance;
		int m_iRightInstance;
		double m_fLeftLength = 0;
		double m_fRightLength = 0;
		double m_fHeight = 0;

		void setHeight(double fHeight1, double fHeight2) {
			m_fHeight = fHeight1;
			if (m_left == null) {
				m_fLeftLength = fHeight1;
			} else {
				m_fLeftLength = fHeight1 - m_left.m_fHeight;
			}
			if (m_right == null) {
				m_fRightLength = fHeight2;
			} else {
				m_fRightLength = fHeight2 - m_right.m_fHeight;
			}
		}
		void setLength(double fLength1, double fLength2) {
			m_fLeftLength = fLength1;
			m_fRightLength = fLength2;
			m_fHeight = fLength1;
			if (m_left != null) {
				m_fHeight += m_left.m_fHeight;
			}
		}

		public String toString() {
			DecimalFormat myFormatter = new DecimalFormat("#.#####");

			if (m_left == null) {
				if (m_right == null) {
					return "(" + m_pData.get().m_sTaxaNames.get(m_iLeftInstance) + ":" + myFormatter.format(m_fLeftLength) + "," +
					m_pData.get().m_sTaxaNames.get(m_iRightInstance) +":" + myFormatter.format(m_fRightLength) + ")";
				} else {
					return "(" + m_pData.get().m_sTaxaNames.get(m_iLeftInstance) + ":" + myFormatter.format(m_fLeftLength) + "," +
						m_right.toString() + ":" + myFormatter.format(m_fRightLength) + ")";
				}
			} else {
				if (m_right == null) {
					return "(" + m_left.toString() + ":" + myFormatter.format(m_fLeftLength) + "," +
					m_pData.get().m_sTaxaNames.get(m_iRightInstance) + ":" + myFormatter.format(m_fRightLength) + ")";
				} else {
					return "(" + m_left.toString() + ":" + myFormatter.format(m_fLeftLength) + "," +m_right.toString() + ":" + myFormatter.format(m_fRightLength) + ")";
				}
			}
		}

		Node toNode() throws Exception {
			Node node = newNode();
			node.setHeight(m_fHeight);
			if (m_left == null) {
				node.m_left = newNode();
				node.m_left.setNr(m_iLeftInstance);
				node.m_left.setID(m_pData.get().m_sTaxaNames.get(m_iLeftInstance));
				node.m_left.setHeight(m_fHeight - m_fLeftLength);
				if (m_right == null) {
					node.m_right = newNode();
					node.m_right.setNr(m_iRightInstance);
					node.m_right.setID(m_pData.get().m_sTaxaNames.get(m_iRightInstance));
					node.m_right.setHeight(m_fHeight - m_fRightLength);
				} else {
					node.m_right = m_right.toNode();
				}
			} else {
				node.m_left = m_left.toNode();
				if (m_right == null) {
					node.m_right = newNode();
					node.m_right.setNr(m_iRightInstance);
					node.m_right.setID(m_pData.get().m_sTaxaNames.get(m_iRightInstance));
					node.m_right.setHeight(m_fHeight - m_fRightLength);
				} else {
					node.m_right = m_right.toNode();
				}
			}
			node.m_right.setParent(node);
			node.m_left.setParent(node);
			return node;
		}
	} // class NodeX

	/** used for priority queue for efficient retrieval of pair of clusters to merge**/
	class Tuple {
		public Tuple(double d, int i, int j, int nSize1, int nSize2) {
			m_fDist = d;
			m_iCluster1 = i;
			m_iCluster2 = j;
			m_nClusterSize1 = nSize1;
			m_nClusterSize2 = nSize2;
		}
		double m_fDist;
		int m_iCluster1;
		int m_iCluster2;
		int m_nClusterSize1;
		int m_nClusterSize2;
	}
	/** comparator used by priority queue**/
	class TupleComparator implements Comparator<Tuple> {
		public int compare(Tuple o1, Tuple o2) {
			if (o1.m_fDist < o2.m_fDist) {
				return -1;
			} else if (o1.m_fDist == o2.m_fDist) {
				return 0;
			}
			return 1;
		}
	}

	// TODO: calculate distance through external class
	// return Hamming distance
	double distance(int iTaxon1, int iTaxon2) {
		double fDist = 0;
		for (int i = 0; i < m_pData.get().getPatternCount(); i++) {
			if (m_pData.get().getPattern(iTaxon1, i) != m_pData.get().getPattern(iTaxon2, i)) {
				fDist += m_pData.get().getPatternWeight(i);
			}
		}
		return fDist / m_pData.get().getSiteCount();
	} // distance
	// 1-norm
	double distance(double [] nPattern1, double [] nPattern2) {
		double fDist = 0;
		for (int i = 0; i < m_pData.get().getPatternCount(); i++) {
			fDist += m_pData.get().getPatternWeight(i) * Math.abs(nPattern1[i] - nPattern2[i]);
		}
		return fDist / m_pData.get().getSiteCount();
	}


	@SuppressWarnings("unchecked")
	public Node buildClusterer() throws Exception {
		int nTaxa = m_pData.get().getNrTaxa();
		// use array of integer vectors to store cluster indices,
		// starting with one cluster per instance
		List<Integer> [] nClusterID = new ArrayList[nTaxa];
		for (int i = 0; i < nTaxa; i++) {
			nClusterID[i] = new ArrayList<Integer>();
			nClusterID[i].add(i);
		}
		// calculate distance matrix
		int nClusters = nTaxa;

		// used for keeping track of hierarchy
		NodeX [] clusterNodes = new NodeX[nTaxa];
		if (m_nLinkType == NEIGHBOR_JOINING) {
			neighborJoining(nClusters, nClusterID, clusterNodes);
		} else {
			doLinkClustering(nClusters, nClusterID, clusterNodes);
		}

		// move all clusters in m_nClusterID array
		// & collect hierarchy
		for (int i = 0; i < nTaxa; i++) {
			if (nClusterID[i].size() > 0) {
				return clusterNodes[i].toNode();
			}
		}
		return null;
	} // buildClusterer

	/** use neighbor joining algorithm for clustering
	 * This is roughly based on the RapidNJ simple implementation and runs at O(n^3)
	 * More efficient implementations exist, see RapidNJ (or my GPU implementation :-))
	 * @param nClusters
	 * @param nClusterID
	 * @param clusterNodes
	 */
	void neighborJoining(int nClusters, List<Integer>[] nClusterID, NodeX [] clusterNodes) {
		int n = m_pData.get().getNrTaxa();

		double [][] fDist = new double[nClusters][nClusters];
		for (int i = 0; i < nClusters; i++) {
			fDist[i][i] = 0;
			for (int j = i+1; j < nClusters; j++) {
				fDist[i][j] = getDistance0(nClusterID[i], nClusterID[j]);
				fDist[j][i] = fDist[i][j];
			}
		}

		double [] fSeparationSums = new double [n];
		double [] fSeparations = new double [n];
	    int [] nNextActive = new int[n];

		//calculate initial separation rows
		for(int i = 0; i < n; i++){
		    double fSum = 0;
		    for(int j = 0; j < n; j++){
		        fSum += fDist[i][j];
		    }
		    fSeparationSums[i] = fSum;
		    fSeparations[i] = fSum / (nClusters - 2);
		    nNextActive[i] = i +1;
		}

		while (nClusters > 2) {
			// find minimum
			int iMin1 = -1;
			int iMin2 = -1;
			double fMin = Double.MAX_VALUE;
			{
			int i = 0;
			while (i < n) {
				double fSep1 = fSeparations[i];
				double [] fRow = fDist[i];
				int j = nNextActive[i];
				while (j < n) {
	            	double fSep2 = fSeparations[j];
	            	double fVal = fRow[j] - fSep1 - fSep2;
					if(fVal < fMin){
						// new minimum
						iMin1 = i;
						iMin2 = j;
						fMin = fVal;
					}
					j = nNextActive[j];
				}
				i = nNextActive[i];
			}
			}
			// record distance
			double fMinDistance = fDist[iMin1][iMin2];
			nClusters--;
			double fSep1 = fSeparations[iMin1];
			double fSep2 = fSeparations[iMin2];
			double fDist1 = (0.5 * fMinDistance) + (0.5 * (fSep1 - fSep2));
			double fDist2 = (0.5 * fMinDistance) + (0.5 * (fSep2 - fSep1));
			if (nClusters > 2) {
				// update separations  & distance
				double fNewSeparationSum = 0;
				double fMutualDistance = fDist[iMin1][iMin2];
				double[] fRow1 = fDist[iMin1];
				double[] fRow2 = fDist[iMin2];
				for(int i = 0; i < n; i++) {
				    if(i == iMin1 || i == iMin2 || nClusterID[i].size() == 0) {
				        fRow1[i] = 0;
				    } else {
				        double fVal1 = fRow1[i];
				        double fVal2 = fRow2[i];
				        double fDistance = (fVal1 + fVal2 - fMutualDistance) / 2.0;
				        fNewSeparationSum += fDistance;
				        // update the separationsum of cluster i.
				        fSeparationSums[i] += (fDistance - fVal1 - fVal2);
				        fSeparations[i] = fSeparationSums[i] / (nClusters -2);
				        fRow1[i] = fDistance;
				        fDist[i][iMin1] = fDistance;
				    }
				}
				fSeparationSums[iMin1] = fNewSeparationSum;
				fSeparations[iMin1] = fNewSeparationSum / (nClusters - 2);
				fSeparationSums[iMin2] = 0;
				merge(iMin1, iMin2, fDist1, fDist2, nClusterID, clusterNodes);
				int iPrev = iMin2;
				// since iMin1 < iMin2 we havenActiveRows[0] >= 0, so the next loop should be save
				while (nClusterID[iPrev].size() == 0) {
					iPrev--;
				}
				nNextActive[iPrev] = nNextActive[iMin2];
			} else {
				merge(iMin1, iMin2, fDist1, fDist2, nClusterID, clusterNodes);
				break;
			}
		}

		for (int i = 0; i < n; i++) {
			if (nClusterID[i].size() > 0) {
				for (int j = i+1; j < n; j++) {
					if (nClusterID[j].size() > 0) {
						double fDist1 = fDist[i][j];
						if(nClusterID[i].size() == 1) {
							merge(i,j,fDist1,0,nClusterID, clusterNodes);
						} else if (nClusterID[j].size() == 1) {
							merge(i,j,0,fDist1,nClusterID, clusterNodes);
						} else {
							merge(i,j,fDist1/2.0,fDist1/2.0,nClusterID, clusterNodes);
						}
						break;
					}
				}
			}
		}
	} // neighborJoining

	/** Perform clustering using a link method
	 * This implementation uses a priority queue resulting in a O(n^2 log(n)) algorithm
	 * @param nClusters number of clusters
	 * @param nClusterID
	 * @param clusterNodes
	 */
	void doLinkClustering(int nClusters, List<Integer>[] nClusterID, NodeX [] clusterNodes) {
		int nInstances = m_pData.get().getNrTaxa();
		PriorityQueue<Tuple> queue = new PriorityQueue<Tuple>(nClusters*nClusters/2, new TupleComparator());
		double [][] fDistance0 = new double[nClusters][nClusters];
		for (int i = 0; i < nClusters; i++) {
			fDistance0[i][i] = 0;
			for (int j = i+1; j < nClusters; j++) {
				fDistance0[i][j] = getDistance0(nClusterID[i], nClusterID[j]);
				fDistance0[j][i] = fDistance0[i][j];
				queue.add(new Tuple(fDistance0[i][j], i, j, 1, 1));
			}
		}
		while (nClusters > 1) {
			int iMin1 = -1;
			int iMin2 = -1;
			// use priority queue to find next best pair to cluster
			Tuple t;
			do {
				t = queue.poll();
			} while (t!=null && (nClusterID[t.m_iCluster1].size() != t.m_nClusterSize1 || nClusterID[t.m_iCluster2].size() != t.m_nClusterSize2));
			iMin1 = t.m_iCluster1;
			iMin2 = t.m_iCluster2;
			merge(iMin1, iMin2, t.m_fDist, t.m_fDist, nClusterID, clusterNodes);
			// merge  clusters

			// update distances & queue
			for (int i = 0; i < nInstances; i++) {
				if (i != iMin1 && nClusterID[i].size()!=0) {
					int i1 = Math.min(iMin1,i);
					int i2 = Math.max(iMin1,i);
					double fDistance = getDistance(fDistance0, nClusterID[i1], nClusterID[i2]);
					queue.add(new Tuple(fDistance, i1, i2, nClusterID[i1].size(), nClusterID[i2].size()));
				}
			}

			nClusters--;
		}
	} // doLinkClustering

	void merge(int iMin1, int iMin2, double fDist1, double fDist2, List<Integer>[] nClusterID, NodeX [] clusterNodes) {
		if (iMin1 > iMin2) {
			int h = iMin1; iMin1 = iMin2; iMin2 = h;
			double f = fDist1; fDist1 = fDist2; fDist2 = f;
		}
		nClusterID[iMin1].addAll(nClusterID[iMin2]);
		//nClusterID[iMin2].removeAllElements();
		nClusterID[iMin2].removeAll(nClusterID[iMin2]);

		// track hierarchy
		NodeX node = new NodeX();
		if (clusterNodes[iMin1] == null) {
			node.m_iLeftInstance = iMin1;
		} else {
			node.m_left = clusterNodes[iMin1];
			clusterNodes[iMin1].m_parent = node;
		}
		if (clusterNodes[iMin2] == null) {
			node.m_iRightInstance = iMin2;
		} else {
			node.m_right = clusterNodes[iMin2];
			clusterNodes[iMin2].m_parent = node;
		}
		if (m_bDistanceIsBranchLength) {
			node.setLength(fDist1, fDist2);
		} else {
			node.setHeight(fDist1, fDist2);
		}
		clusterNodes[iMin1] = node;
	} // merge

	/** calculate distance the first time when setting up the distance matrix **/
	double getDistance0(List<Integer> cluster1, List<Integer> cluster2) {
		double fBestDist = Double.MAX_VALUE;
		switch (m_nLinkType) {
		case SINGLE:
		case NEIGHBOR_JOINING:
		case CENTROID:
		case COMPLETE:
		case ADJCOMLPETE:
		case AVERAGE:
		case MEAN:
			// set up two instances for distance function
			fBestDist = distance(cluster1.get(0), cluster2.get(0));
			break;
		case WARD:
			{
				// finds the distance of the change in caused by merging the cluster.
				// The information of a cluster is calculated as the error sum of squares of the
				// centroids of the cluster and its members.
				double ESS1 = calcESS(cluster1);
				double ESS2 = calcESS(cluster2);
				List<Integer> merged = new ArrayList<Integer>();
				merged.addAll(cluster1);
				merged.addAll(cluster2);
				double ESS = calcESS(merged);
				fBestDist = ESS * merged.size() - ESS1 * cluster1.size() - ESS2 * cluster2.size();
			}
			break;
		}
		return fBestDist;
	} // getDistance0

	/** calculate the distance between two clusters
	 * @param cluster1 list of indices of instances in the first cluster
	 * @param cluster2 dito for second cluster
	 * @return distance between clusters based on link type
	 */
	double getDistance(double [][] fDistance, List<Integer> cluster1, List<Integer> cluster2) {
		double fBestDist = Double.MAX_VALUE;
		switch (m_nLinkType) {
		case SINGLE:
			// find single link distance aka minimum link, which is the closest distance between
			// any item in cluster1 and any item in cluster2
			fBestDist = Double.MAX_VALUE;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.get(i);
				for (int j = 0; j < cluster2.size(); j++) {
					int i2  = cluster2.get(j);
					double fDist = fDistance[i1][i2];
					if (fBestDist > fDist) {
						fBestDist = fDist;
					}
				}
			}
			break;
		case COMPLETE:
		case ADJCOMLPETE:
			// find complete link distance aka maximum link, which is the largest distance between
			// any item in cluster1 and any item in cluster2
			fBestDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.get(i);
				for (int j = 0; j < cluster2.size(); j++) {
					int i2 = cluster2.get(j);
					double fDist = fDistance[i1][i2];
					if (fBestDist < fDist) {
						fBestDist = fDist;
					}
				}
			}
			if (m_nLinkType == COMPLETE) {
				break;
			}
			// calculate adjustment, which is the largest within cluster distance
			double fMaxDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.get(i);
				for (int j = i+1; j < cluster1.size(); j++) {
					int i2 = cluster1.get(j);
					double fDist = fDistance[i1][i2];
					if (fMaxDist < fDist) {
						fMaxDist = fDist;
					}
				}
			}
			for (int i = 0; i < cluster2.size(); i++) {
				int i1 = cluster2.get(i);
				for (int j = i+1; j < cluster2.size(); j++) {
					int i2 = cluster2.get(j);
					double fDist = fDistance[i1][i2];
					if (fMaxDist < fDist) {
						fMaxDist = fDist;
					}
				}
			}
			fBestDist -= fMaxDist;
			break;
		case AVERAGE:
			// finds average distance between the elements of the two clusters
			fBestDist = 0;
			for (int i = 0; i < cluster1.size(); i++) {
				int i1 = cluster1.get(i);
				for (int j = 0; j < cluster2.size(); j++) {
					int i2 = cluster2.get(j);
					fBestDist += fDistance[i1][i2];
				}
			}
			fBestDist /= (cluster1.size() * cluster2.size());
			break;
		case MEAN:
			{
				// calculates the mean distance of a merged cluster (akak Group-average agglomerative clustering)
				List<Integer> merged = new ArrayList<Integer>();
				merged.addAll(cluster1);
				merged.addAll(cluster2);
				fBestDist = 0;
				for (int i = 0; i < merged.size(); i++) {
					int i1 = merged.get(i);
					for (int j = i+1; j < merged.size(); j++) {
						int i2 = merged.get(j);
						fBestDist += fDistance[i1][i2];
					}
				}
				int n = merged.size();
				fBestDist /= (n*(n-1.0)/2.0);
			}
			break;
		case CENTROID:
			// finds the distance of the centroids of the clusters
			int nPatterns = m_pData.get().getPatternCount();
			double [] centroid1 = new double[nPatterns];
			for (int i = 0; i < cluster1.size(); i++) {
				int iTaxon = cluster1.get(i);
				for (int j = 0; j < nPatterns; j++) {
					centroid1[j] += m_pData.get().getPattern(iTaxon, j);
				}
			}
			double [] centroid2 = new double[nPatterns];
			for (int i = 0; i < cluster2.size(); i++) {
				int iTaxon = cluster2.get(i);
				for (int j = 0; j < nPatterns; j++) {
					centroid2[j] += m_pData.get().getPattern(iTaxon, j);
				}
			}
			for (int j = 0; j < nPatterns; j++) {
				centroid1[j] /= cluster1.size();
				centroid2[j] /= cluster2.size();
			}
			fBestDist = distance(centroid1, centroid2);
			break;
		case WARD:
			{
				// finds the distance of the change in caused by merging the cluster.
				// The information of a cluster is calculated as the error sum of squares of the
				// centroids of the cluster and its members.
				double ESS1 = calcESS(cluster1);
				double ESS2 = calcESS(cluster2);
				List<Integer> merged = new ArrayList<Integer>();
				merged.addAll(cluster1);
				merged.addAll(cluster2);
				double ESS = calcESS(merged);
				fBestDist = ESS * merged.size() - ESS1 * cluster1.size() - ESS2 * cluster2.size();
			}
			break;
		}
		return fBestDist;
	} // getDistance


	/** calculated error sum-of-squares for instances wrt centroid **/
	double calcESS(List<Integer> cluster) {
		int nPatterns = m_pData.get().getPatternCount();
		double [] centroid = new double[nPatterns];
		for (int i = 0; i < cluster.size(); i++) {
			int iTaxon = cluster.get(i);
			for (int j = 0; j < nPatterns; j++) {
				centroid[j] += m_pData.get().getPattern(iTaxon, j);
			}
		}
		for (int j = 0; j < nPatterns; j++) {
			centroid[j] /= cluster.size();
		}
		// set up two instances for distance function
		double fESS = 0;
		for (int i = 0; i < cluster.size(); i++) {
			double [] instance = new double[nPatterns];
			int iTaxon = cluster.get(i);
			for (int j = 0; j < nPatterns; j++) {
				instance[j] += m_pData.get().getPattern(iTaxon, j);
			}
			fESS += distance(centroid, instance);
		}
		return fESS / cluster.size();
	} // calcESS



} // class HierarchicalClusterer
