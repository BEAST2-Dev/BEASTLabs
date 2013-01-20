/*

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
/*
 * TreeFileParser.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package beast.app.treeannotator;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import beast.evolution.tree.Node;


public class TreeSetParser {
	/**
	 * default tree branch length, used when that info is not in the Newick tree
	 **/
	final static float DEFAULT_LENGTH = 0.001f;

	int m_nOffset = 0;
	/** labels of leafs **/
	List<String> m_sLabels;
	/** position information for the leafs (if available) **/
	List<Float> m_fLongitude;
	List<Float> m_fLatitude;
	/** extreme values for position information **/
	float m_fMaxLong, m_fMaxLat, m_fMinLong, m_fMinLat;
	/** nr of labels in dataset **/
	int m_nNrOfLabels;
	/** burn in = nr of trees ignored at the start of tree file, can be set by command line option **/
	int m_nBurnIn = 0;
	//DensiTree m_densiTree;
	/** for memory saving, set to true **/
	boolean m_bSurpressMetadata = true;
	/** if there is no translate block. This solves issues where the taxa labels are numbers e.g. in generated tree data **/
	boolean m_bIsLabelledNewick = false;
	/** flag to indicate that single child nodes are allowed **/
	boolean m_bAllowSingleChild = false;
	
	public TreeSetParser(int nBurnIn, boolean bAllowSingleChild) {
		m_sLabels = new ArrayList<String>();
		m_fLongitude = new ArrayList<Float>();
		m_fLatitude = new ArrayList<Float>();
		m_nBurnIn = nBurnIn;
		m_fMinLat = 90; m_fMinLong = 180;
		m_fMaxLat = -90; m_fMaxLong = -180;
		m_bAllowSingleChild = bAllowSingleChild;
	} // c'tor
	
	public TreeSetParser(List<String> sLabels, List<Float> fLongitude, List<Float> fLatitude, int nBurnIn) {
		m_sLabels = sLabels;
		if (m_sLabels != null) {
			m_bIsLabelledNewick = true;
			m_nNrOfLabels = m_sLabels.size();
		}
		m_fLongitude = fLongitude;
		m_fLatitude = fLatitude;
		m_nBurnIn = nBurnIn;
		m_fMinLat = 90; m_fMinLong = 180;
		m_fMaxLat = -90; m_fMaxLong = -180;
	}
	
	public Node [] parseFile(String sFile) throws Exception {
		//List<String> sNewickTrees = new List<String>();
		List<Node> trees = new ArrayList<Node>();
		m_nOffset = 0;
		// parse Newick tree file
		BufferedReader fin = new BufferedReader(new FileReader(sFile));
		String sStr = fin.readLine();
		// grab translate block
		while (fin.ready() && sStr.toLowerCase().indexOf("translate") < 0) {
			sStr = fin.readLine();
		}
		m_bIsLabelledNewick = false;
		m_nNrOfLabels = m_sLabels.size();
		boolean bAddLabels = (m_nNrOfLabels == 0);
		if (sStr.toLowerCase().indexOf("translate") < 0) {
			m_bIsLabelledNewick = true;
			// could not find translate block, assume it is a list of Newick trees instead of Nexus file
			fin.close();
			fin = new BufferedReader(new FileReader(sFile));
			while (fin.ready() && m_nNrOfLabels == 0) {
				sStr = fin.readLine();
				if (sStr.length() > 2 && sStr.indexOf("(") >= 0) {
					String sStr2 = sStr;
					sStr2 = sStr2.substring(sStr2.indexOf("("));
					while (sStr2.indexOf('[') >= 0) {
						int i0 = sStr2.indexOf('[');
						int i1 = sStr2.indexOf(']');
						sStr2 = sStr2.substring(0, i0) + sStr2.substring(i1 + 1);
					}
					sStr2 = sStr2.replaceAll("[;\\(\\),]"," ");
					sStr2 = sStr2.replaceAll(":[0-9\\.Ee-]+"," ");
					String [] sLabels = sStr2.split("\\s+");
					if (bAddLabels) {
						m_nNrOfLabels = 0;
						for (int i = 0; i < sLabels.length; i++) {
							if (sLabels[i].length() > 0) {
									m_sLabels.add(sLabels[i]);
								m_nNrOfLabels++;
							}
						}
					}
					Node tree = parseNewick(sStr);
					tree.sort();
					tree.labelInternalNodes(m_nNrOfLabels);
					trees.add(tree);
//					sNewickTrees.add(sStr);
				}
			}

			while (fin.ready()) {
				sStr = fin.readLine();
				if (sStr.length() > 2 && sStr.indexOf("(") >= 0) {
					Node tree = parseNewick(sStr);
					tree.sort();
					tree.labelInternalNodes(m_nNrOfLabels);
					trees.add(tree);
					if (trees.size() % 100 ==0) {if (m_nNrOfLabels>=100||trees.size() % 1000 ==0) {System.err.print(trees.size() + " ");}}
//					sNewickTrees.add(sStr);
				}
			}
		} else {
			// read tree set from file, and store in individual strings
			sStr = fin.readLine();
			//m_nNrOfLabels = 0;
			boolean bLastLabel = false;
			while (fin.ready() && !bLastLabel) {
				if (sStr.indexOf(";") >= 0) {
					sStr = sStr.replace(';',' ');
					sStr = sStr.trim();
					if (sStr.isEmpty()) {
						break;
					}
					bLastLabel = true;
				}
				sStr = sStr.replaceAll(",", "");
				sStr = sStr.replaceAll("^\\s+", "");
				String[] sStrs = sStr.split("\\s+");
				int iLabel = new Integer(sStrs[0]).intValue();
				String sLabel = sStrs[1];
				if (m_sLabels.size() < iLabel) {
					//m_sLabels.add("__dummy__");
					m_nOffset = 1;
				}
				// check if there is geographic info in the name
				if (sLabel.contains("(")) {
					int iStr = sLabel.indexOf('(');
					int iStr2 = sLabel.indexOf('x', iStr);
					if (iStr2 >= 0) {
						int iStr3 = sLabel.indexOf(')', iStr2);
						if (iStr3 >= 0) {
							float fLat = Float.parseFloat(sLabel.substring(iStr+1, iStr2));// + 180;
							float fLong = Float.parseFloat(sLabel.substring(iStr2+1, iStr3));// + 360)%360;
							if (fLat!=0 || fLong!=0) {
								m_fMinLat = Math.min(m_fMinLat, fLat);
								m_fMaxLat = Math.max(m_fMaxLat, fLat);
								m_fMinLong = Math.min(m_fMinLong, fLong);
								m_fMaxLong = Math.max(m_fMaxLong, fLong);
							}
							while (m_fLatitude.size() < m_sLabels.size()) {
								m_fLatitude.add(0f);
								m_fLongitude.add(0f);
							}
							m_fLatitude.add(fLat);
							m_fLongitude.add(fLong);
						}
					}
					sLabel = sLabel.substring(0, sLabel.indexOf("("));
				}
				if (bAddLabels) {
					m_sLabels.add(sLabel);
					m_nNrOfLabels++;
				}
				if (!bLastLabel) {
					sStr = fin.readLine();
				}
			}
			
			// read trees
			int nBurnIn = m_nBurnIn;
			//int k = 0;
			while (fin.ready()) {
				sStr = fin.readLine();
				sStr = sStr.trim();
				if (sStr.length() > 5) {
					String sTree = sStr.substring(0,5);
					if (sTree.toLowerCase().startsWith("tree ")) {
						//k++;
						if (nBurnIn <= 0) {
							int i = sStr.indexOf('(');
							if (i > 0) {
								sStr = sStr.substring(i);
							}
//						if (m_bSurpressMetadata) {
//							while (sStr.indexOf('[') >= 0) {
//								int i0 = sStr.indexOf('[');
//								int i1 = sStr.indexOf(']');
//								sStr = sStr.substring(0, i0) + sStr.substring(i1 + 1);
//							}
//						}
							Node tree = parseNewick(sStr);
							//System.err.println(k + " " + tree);
							tree.sort();
							tree.labelInternalNodes(m_nNrOfLabels);
							trees.add(tree);
							if (trees.size() % 100 ==0) {if (m_nNrOfLabels>=100||trees.size() % 1000 ==0) {System.err.print(trees.size() + " ");}}
							//sNewickTrees.add(sStr);
						} else {
							nBurnIn--;
						}
					}
				}
			}
			fin.close();
			if (nBurnIn > 0) {
				System.err.println("WARNING: Burn-in too large, resetting burn-in to zero");
				m_sLabels.clear();
				m_nBurnIn = 0;
				return parseFile(sFile);
			}
		}
		
		
		// convert lengths (stored as node heights) to heights
		double fMaxHeight = 0;
		double [] heights = new double[trees.size()];
		for (int i = 0; i < trees.size(); i++) {
			heights[i] = lengthToHeight(trees.get(i), 0);
			fMaxHeight = Math.max(fMaxHeight, heights[i]);
		}
		for (int i = 0; i < trees.size(); i++) {
			offsetHeight(trees.get(i), heights[i]);
		}

		System.err.println();
		System.err.println("Geo: " +m_fMinLong + "x" + m_fMinLat + " " + m_fMaxLong + "x" + m_fMaxLat);
		return trees.toArray(new Node[1]);
	} // parseFile

	
	/** move y-position of a tree with offset f **/
	public void offsetHeight(Node node, double f) {
		if (!node.isLeaf()) {
			offsetHeight(node.getLeft(), f);
			if (node.getRight() != null) {
				offsetHeight(node.getRight(), f);
			}
		}
		node.setHeight(node.getHeight() + f);
	}

	/** convert length to height
	 *  and set ID of leafs
	 */
	private double lengthToHeight(Node node, double fOffSet) {
		if (node.isLeaf()) {
			node.setHeight(-fOffSet - node.getHeight());
			node.setID(m_sLabels.get(node.getNr()));
			return -node.getHeight();
		} else {
			double fPosY = fOffSet + node.getHeight();
			double fYMax = 0;
			fYMax = Math.max(fYMax, lengthToHeight(node.getLeft(), fPosY));
			if (node.getRight() != null) {
				fYMax = Math.max(fYMax, lengthToHeight(node.getRight(), fPosY));
			}
			node.setHeight(-fPosY);
			return fYMax;
		}
	}

	/** Try to map sStr into an index. First, assume it is a number.
	 * If that does not work, look in list of labels to see whether it is there.
	 */
	private int getLabelIndex(String sStr) throws Exception {
		if (!m_bIsLabelledNewick) {
			try {
				return Integer.parseInt(sStr) - m_nOffset;
			} catch (Exception e) {
			}
		}
		for (int i = 0; i < m_nNrOfLabels; i++) {
			if (sStr.equals(m_sLabels.get(i))) {
				return i;
			}
		}
		// sStr may have (double) qoutes missing
		for (int i = 0; i < m_nNrOfLabels; i++) {
			String sLabel = m_sLabels.get(i);
			if (sLabel.startsWith("'") && sLabel.endsWith("'") ||
					sLabel.startsWith("\"") && sLabel.endsWith("\"")) {
				sLabel = sLabel.substring(1, sLabel.length()-1);
				if (sStr.equals(sLabel)) {
					return i;
				}
			}
		}
		// sStr may have extra (double) qoutes
		if (sStr.startsWith("'") && sStr.endsWith("'") ||
				sStr.startsWith("\"") && sStr.endsWith("\"")) {
			sStr = sStr.substring(1, sStr.length()-1);
			return getLabelIndex(sStr);
		}
		throw new Exception("Label '" + sStr + "' in Newick tree could not be identified");
	}
	

	 double height(Node node) {
		 if (node.isLeaf()) {
			 return node.getLength();
		 } else {
			 return node.getLength() + Math.max(height(node.getLeft()), height(node.getRight()));
		 }
	 }
	 
	 char [] m_chars;
	 int m_iTokenStart;
	 int m_iTokenEnd;
	 final static int COMMA = 1;
	 final static int BRACE_OPEN = 3;
	 final static int BRACE_CLOSE = 4;
	 final static int COLON = 5;
	 final static int SEMI_COLON = 8;
	 final static int META_DATA = 6;
	 final static int TEXT = 7;
	 final static int UNKNOWN = 0;
	 
	 int nextToken() {
		 m_iTokenStart = m_iTokenEnd;
		 while (m_iTokenEnd < m_chars.length) {
			 // skip spaces
			 while (m_iTokenEnd < m_chars.length && (m_chars[m_iTokenEnd] == ' ' || m_chars[m_iTokenEnd] == '\t')) {
				 m_iTokenStart++;
				 m_iTokenEnd++;
			 }
			 if (m_chars[m_iTokenEnd] == '(') {
				 m_iTokenEnd++;
				 return BRACE_OPEN;
			 }
			 if (m_chars[m_iTokenEnd] == ':') {
				 m_iTokenEnd++;
				 return COLON;
			 }
			 if (m_chars[m_iTokenEnd] == ';') {
				 m_iTokenEnd++;
				 return SEMI_COLON;
			 }
			 if (m_chars[m_iTokenEnd] == ')') {
				 m_iTokenEnd++;
				 return BRACE_CLOSE;
			 }
			 if (m_chars[m_iTokenEnd] == ',') {
				 m_iTokenEnd++;
				 return COMMA;
			 }
			 if (m_chars[m_iTokenEnd] == '[') {
				 m_iTokenEnd++;
				 while (m_iTokenEnd < m_chars.length && m_chars[m_iTokenEnd-1] != ']') {
					 m_iTokenEnd++;
				 }
				 return META_DATA;
			 }
			 while (m_iTokenEnd < m_chars.length && (m_chars[m_iTokenEnd] != ' ' && m_chars[m_iTokenEnd] != '\t'
				 && m_chars[m_iTokenEnd] != '('  && m_chars[m_iTokenEnd] != ')'  && m_chars[m_iTokenEnd] != '['
					 && m_chars[m_iTokenEnd] != ':'&& m_chars[m_iTokenEnd] != ','&& m_chars[m_iTokenEnd] != ';')) {
				 m_iTokenEnd++;
			 }
			 return TEXT;
		 }
		 return UNKNOWN;
	 }

	 public Node parseNewick(String sStr) throws Exception {
		 try {
		if (sStr == null || sStr.length() == 0) {
			return null;
		}
		
		m_chars = sStr.toCharArray();
		m_iTokenStart = sStr.indexOf('(');
		if (m_iTokenStart < 0) {
			return null;
		}
		m_iTokenEnd = m_iTokenStart;
		Vector<Node> stack = new Vector<Node>();
		Vector<Boolean> isFirstChild =  new Vector<Boolean>();
		Vector<String> sMetaData =  new Vector<String>();
		stack.add(new Node());
		isFirstChild.add(true);
		stack.lastElement().setHeight(DEFAULT_LENGTH);
		sMetaData.add(null);
		boolean bIsLabel = true;
		while (m_iTokenEnd < m_chars.length) {
			switch (nextToken()) {
			case BRACE_OPEN:
			{
				Node node2 = new Node();
				node2.setHeight(DEFAULT_LENGTH);
				stack.add(node2);
				isFirstChild.add(true);
				sMetaData.add(null);
				bIsLabel = true;
			}
				break;
			case BRACE_CLOSE:
			{
				if (isFirstChild.lastElement()) {
					if (m_bAllowSingleChild) {
						// process single child nodes
						Node left = stack.lastElement();
						stack.remove(stack.size()-1);
						isFirstChild.remove(isFirstChild.size()-1);
						Node dummyparent = new Node();
						dummyparent.setHeight(DEFAULT_LENGTH);
						dummyparent.setLeft(left);
						left.setParent(dummyparent);
						dummyparent.setRight(null);
						Node parent = stack.lastElement();
						parent.setLeft(left);
						left.setParent(parent);
						String metaData = sMetaData.remove(sMetaData.size() - 1);
						left.m_sMetaData = metaData;
						parseMetaData(left, metaData);
						break;
					} else {
						// don't know how to process single child nodes
						throw new Exception("Node with single child found.");
					}
				}
				// process multi(i.e. more than 2)-child nodes by pairwise merging.
				while (isFirstChild.elementAt(isFirstChild.size()-2) == false) {
					Node right = stack.lastElement();
					stack.remove(stack.size()-1);
					isFirstChild.remove(isFirstChild.size()-1);
					Node left = stack.lastElement();
					stack.remove(stack.size()-1);
					isFirstChild.remove(isFirstChild.size()-1);
					Node dummyparent = new Node();
					dummyparent.setHeight(DEFAULT_LENGTH);
					dummyparent.setLeft(left);
					left.setParent (dummyparent);
					dummyparent.setRight(right);
					right.setParent(dummyparent);
					stack.add(dummyparent);
					isFirstChild.add(false);
					String metaData = sMetaData.remove(sMetaData.size() - 1);
					parseMetaData(left, metaData);
				}
				// last two nodes on stack merged into single parent node 
				Node right = stack.lastElement();
				stack.remove(stack.size()-1);
				isFirstChild.remove(isFirstChild.size()-1);
				String metaData = sMetaData.remove(sMetaData.size() - 1);
				parseMetaData(right, metaData);

				Node left = stack.lastElement();
				stack.remove(stack.size()-1);
				isFirstChild.remove(isFirstChild.size()-1);
				metaData = sMetaData.remove(sMetaData.size() - 1);
				parseMetaData(right, metaData);

				Node parent = stack.lastElement();
				parent.setLeft(left);
				left.setParent(parent);
				parent.setRight(right);
				right.setParent(parent);
				metaData = sMetaData.lastElement();
				parseMetaData(parent, metaData);
			}
				break;
			case COMMA:
			{
				Node node2 = new Node();
				node2.setHeight(DEFAULT_LENGTH);
				stack.add(node2);
				isFirstChild.add(false);
				sMetaData.add(null);
				bIsLabel = true;
			}
				break;
			case COLON:
				bIsLabel = false;
				break;
			case TEXT:
				if (bIsLabel) {
					String sLabel = sStr.substring(m_iTokenStart, m_iTokenEnd);
					stack.lastElement().setNr(getLabelIndex(sLabel)); 
				} else {
					String sLength = sStr.substring(m_iTokenStart, m_iTokenEnd);
					stack.lastElement().setHeight(Float.parseFloat(sLength)); 
				}
				break;
			case META_DATA:
				if (sMetaData.lastElement() == null) {
					sMetaData.set(sMetaData.size()-1, sStr.substring(m_iTokenStart+1, m_iTokenEnd-1));
				} else {
					sMetaData.set(sMetaData.size()-1, sMetaData.lastElement() 
					 + ("," +sStr.substring(m_iTokenStart+1, m_iTokenEnd-1)));
				}
				break;
			case SEMI_COLON:
				//System.err.println(stack.lastElement().toString());
				return stack.lastElement();
			default:
				throw new Exception("parseNewick: unknown token");	
			}
		}
		return stack.lastElement();
		 } catch (Exception e) {
			 e.printStackTrace();
			 throw new Exception(e.getMessage() + ": " + sStr.substring(Math.max(0, m_iTokenStart-100), m_iTokenStart) + " >>>" + sStr.substring(m_iTokenStart, m_iTokenEnd) + " <<< ..."); 
		 }
		//return node;
	 }
	 
	 
		public void parseMetaData(Node node, String sMetaData) {
			node.m_sMetaData = sMetaData;
			if (sMetaData == null) {
				return;
			}
			// parse by key=value pairs
			int i = 0;
			int start = 1;
			try {
				while ((i = sMetaData.indexOf('=', i)) >= 0) {
					String key = sMetaData.substring(start, i).trim();
					String value = null;
					int k = 0;
					if ((k = sMetaData.indexOf('=', i+1)) >= 0) {
						int j = sMetaData.lastIndexOf(',', k);
						value = sMetaData.substring(i + 1, j);
						start = j + 1;
					} else {
						value = sMetaData.substring(i+1);
					}
					if (value.length() > 0 && value.charAt(0) != '{') {
						try {
							Double dvalue = Double.parseDouble(value);
							node.setMetaData(key, dvalue);
							
						} catch (Exception e) {
							node.setMetaData(key, value);
						}
					} else 	if (value.length() > 0 && value.charAt(0) == '{' && value.charAt(value.length() - 1) == '}') {
						try {
							String str = value.substring(1, value.length() - 1); 
							String [] strs = str.split(",");
							Double [] values = new Double[strs.length];
							for (int j = 0; j < strs.length; j++) {
								values[j] = Double.parseDouble(strs[j]); 
							}
							node.setMetaData(key, values);
						} catch (Exception e) {
							node.setMetaData(key, value);
						}
					} else {
						node.setMetaData(key, value);
					}
					i++;
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			// parse as list of numbers
//			i = 0;
//			start = 1;
//			int iNumber = 0;
//			//Pattern pattern = Pattern.compile("^([+-0-9\\.]+)([eE]-??[0-9]+)??.*");
//			Pattern pattern = Pattern.compile("((-?[0-9]+(\\.[0-9]+)?)([eE]-?[0-9]+)?)");
//			try {
//				while (i < m_sMetaData.length()) {
//					char c = m_sMetaData.charAt(i);
//					if ((c >= '0' && c <= '9') || c=='+' || c=='-') {
//						String str = m_sMetaData.substring(i);
//						Matcher m = pattern.matcher(str);
//						m.find();
//						String mantissa = m.group(1);
//						i  += mantissa.length();
//						double value = 0;
//						try {
//							value = Double.parseDouble(mantissa);
//						} catch (Exception e) {
//							System.err.println(e.getMessage());
//						}
//						node.setMetaData(name, value);
//						
//						metaDataList.add(value);
//						if (g_maxListValue.size() < metaDataList.size()) {
//							g_maxListValue.add(Double.NEGATIVE_INFINITY);
//							g_minListValue.add(Double.POSITIVE_INFINITY);
//						}
//						g_maxListValue.set(iNumber, Math.max(g_maxListValue.get(iNumber), value));
//						g_minListValue.set(iNumber, Math.min(g_minListValue.get(iNumber), value));
//						iNumber++;
//					} else {
//						i++;
//					}
//				}
//			} catch (Exception e) {
//				// TODO: handle exception
//			}
//
		}

} // class TreeFileParser
