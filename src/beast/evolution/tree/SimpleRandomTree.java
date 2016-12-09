/*
 * CoalescentSimulator.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
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

package beast.evolution.tree;


import beast.core.*;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.operators.DistanceProvider;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.ParametricDistribution;
import beast.util.Randomizer;

import java.util.*;

import org.apache.commons.math.MathException;


@Description("This class provides the basic engine for coalescent simulation of a given demographic model over a given time period. ")
public class SimpleRandomTree extends Tree implements StateNodeInitialiser {
    public Input<Alignment> taxaInput = new Input<>("taxa", "set of taxa to initialise tree specified by alignment");

    public Input<List<MRCAPrior>> calibrationsInput =
            new Input<List<MRCAPrior>>("constraint", "specifies (monophyletic or height distribution) constraints on internal nodes",
                    new ArrayList<MRCAPrior>());
    public Input<Double> rootHeightInput =
            new Input<Double>("rootHeight", "If specified the tree will be scaled to match the root height, if constraints allow this");

    public Input<Double> branchMeanInput = new Input<>("branchMean", "Branches will be exponentially distributed with this mean (bounds " +
            "permitting).", -1.0, Input.Validate.OPTIONAL);

    public Input<Double> clampInput = new Input<>("limitCalibrations", "Initialize node height to be in the center of its calibration. For " +
            "example, a value of 0.9 will restrict the height to be in the [5%,95%] percentile range. 1 means takes the full range.",
            0.95, Input.Validate.OPTIONAL);

    public Input<DistanceProvider> distancesInput = new Input<>("weights", "if provided, used to inform sampling distribution such that nodes that are "
    		+ "closer have a higher chance of forming a clade");

    // total nr of taxa
    int nrOfTaxa;

    class Bound {
        Double upper = Double.POSITIVE_INFINITY;
        Double lower = 0.0 ; // Double.NEGATIVE_INFINITY;

        public String toString() {
            return "[" + lower + "," + upper + "]";
        }

        public void restrict(final Bound bound) {
            upper = Math.min(upper, bound.upper);
            lower = Math.max(lower, bound.lower);
        }
    }

    private boolean intersecting(Bound bound1, Bound bound2) {
        return bound1.upper > bound2.lower && bound2.upper > bound1.lower;
    }

    // Location of last monophyletic clade in the lists below, which are grouped together at the start.
    // (i.e. the first isMonophyletic of the TaxonSets are monophyletic, while the remainder are not).
    int lastMonophyletic;

    // taxonSets,distributions, m_bounds and taxonSetIDs are indexed together (four values associated with this clade, a set of taxa.

    // taxon sets of clades that has a constraint of calibrations. Monophyletic constraints may be nested, and are sorted by the code to be at a
    // higher index, i.e iterating from zero up does post-order (descendants before parent).
    List<Set<String>> taxonSets;

    // list of parametric distribution constraining the MRCA of taxon sets, null if not present
    List<ParametricDistribution> distributions;

    // hard bound for the set, if any
    List<Bound> m_bounds;

    // The prior element involved, if any
    List<String> taxonSetIDs;

    List<Integer>[] children;

    Set<String> sTaxa;

    Bound[] boundPerNode;
    ParametricDistribution[] distPerNode;

    // number of the next internal node, used when creating new internal nodes
    int nextNodeNr;

    private DistanceProvider distances;

    // used to indicate one of the MRCA constraints could not be met
    protected class ConstraintViolatedException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    @Override
    public void initAndValidate() {
        sTaxa = new LinkedHashSet<>();
        if (taxaInput.get() != null) {
            sTaxa.addAll(taxaInput.get().getTaxaNames());
        } else {
            sTaxa.addAll(m_taxonset.get().asStringList());
        }
        distances = distancesInput.get();

        nrOfTaxa = sTaxa.size();

        doTheWork();
        super.initAndValidate();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void swap(final List list, final int i, final int j) {
        final Object tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    private boolean firstInitCall = true;

    public void initStateNodes() {
        if( firstInitCall ) {
            // first time steal from initAndValidate.
            firstInitCall = false;
        } else {
            doTheWork();
        }
        if (m_initial.get() != null) {
            m_initial.get().assignFromWithoutID(this);
        }
    }

    private final boolean ICC = false;

    public void doTheWork() {
        // find taxon sets we are dealing with
        taxonSets = new ArrayList<>();
        m_bounds = new ArrayList<>();
        distributions = new ArrayList<>();
        taxonSetIDs = new ArrayList<>();
        List<Boolean> onParent = new ArrayList<>();
        lastMonophyletic = 0;

        if (taxaInput.get() != null) {
            sTaxa.addAll(taxaInput.get().getTaxaNames());
        } else {
            sTaxa.addAll(m_taxonset.get().asStringList());
        }

        // pick up constraints from outputs, m_inititial input tree and output tree, if any
        List<MRCAPrior> calibrations = new ArrayList<MRCAPrior>();
        calibrations.addAll(calibrationsInput.get());

        // pick up constraints in m_initial tree
        for (final Object plugin : getOutputs()) {
            if (plugin instanceof MRCAPrior && !calibrations.contains(plugin) ) {
                calibrations.add((MRCAPrior) plugin);
            }
        }

        if (m_initial.get() != null) {
            for (final Object plugin : m_initial.get().getOutputs()) {
                if (plugin instanceof MRCAPrior && !calibrations.contains(plugin)) {
                    calibrations.add((MRCAPrior) plugin);
                }
            }
        }

        for (final MRCAPrior prior : calibrations) {
            final TaxonSet taxonSet = prior.taxonsetInput.get();
            if (taxonSet != null && !prior.onlyUseTipsInput.get()) {
	            final Set<String> bTaxa = new LinkedHashSet<>();
	        	if (taxonSet.asStringList() == null) {
	        		taxonSet.initAndValidate();
	        	}
	            for (final String sTaxonID : taxonSet.asStringList()) {

	                if (!sTaxa.contains(sTaxonID)) {
	                    throw new IllegalArgumentException("Taxon <" + sTaxonID + "> could not be found in list of taxa. Choose one of " +
                                Arrays.toString(sTaxa.toArray(new String[sTaxa.size()])));
	                }
	                bTaxa.add(sTaxonID);
	            }
	            final ParametricDistribution distr = prior.distInput.get();
	            final Bound bounds = new Bound();
	            if (distr != null) {
	        		List<BEASTInterface> plugins = new ArrayList<>();
	        		distr.getPredecessors(plugins);
	        		for (int i = plugins.size() - 1; i >= 0 ; i--) {
	        			plugins.get(i).initAndValidate();
	        		}
	                try {
                        final double offset = distr.offsetInput.get();
                        bounds.lower = Math.max(distr.inverseCumulativeProbability(0.0) + offset, 0.0);
		                bounds.upper = distr.inverseCumulativeProbability(1.0) + offset;
                        assert bounds.lower <= bounds.upper;
					} catch (MathException e) {
						Log.warning.println("Could not set bounds in SimpleRandomTree::doTheWork : " + e.getMessage());
					}
	            }

	            if (prior.isMonophyleticInput.get() || bTaxa.size() == 1 ) {
	                // add any monophyletic constraint
                    boolean isDuplicate = false;
                    for(int k = 0; k < lastMonophyletic; ++k) {
                        // assert prior.useOriginateInput.get().equals(onParent.get(k)) == (prior.useOriginateInput.get() == onParent.get(k));
                        if( bTaxa.size() == taxonSets.get(k).size() && bTaxa.equals(taxonSets.get(k)) &&
                                prior.useOriginateInput.get().equals(onParent.get(k)) ) {
                            if( distr != null ) {
                                if( distributions.get(k) == null ) {
                                    distributions.set(k, distr);
                                    m_bounds.set(k, bounds);
                                    taxonSetIDs.set(k, prior.getID());
                                }
                            }
                            isDuplicate = true;
                        }
                    }
                    if( ! isDuplicate ) {
                        taxonSets.add(lastMonophyletic, bTaxa);
                        distributions.add(lastMonophyletic, distr);
                        onParent.add(lastMonophyletic, prior.useOriginateInput.get());
                        m_bounds.add(lastMonophyletic, bounds);
                        taxonSetIDs.add(lastMonophyletic, prior.getID());
                        lastMonophyletic++;
                    }
	            } else {
	                // only calibrations with finite bounds are added
	                if (!Double.isInfinite(bounds.lower) || !Double.isInfinite(bounds.upper)) {
	                    taxonSets.add(bTaxa);
	                    distributions.add(distr);
	                    m_bounds.add(bounds);
	                    taxonSetIDs.add(prior.getID());
                        onParent.add(prior.useOriginateInput.get());
	                }
	            }
            }
        }

        if( ICC ) {
            for (int i = 0; i < lastMonophyletic; i++) {
                final Set<String> ti = taxonSets.get(i);
                for (int j = i + 1; j < lastMonophyletic; j++) {
                    final Set<String> tj = taxonSets.get(j);
                    boolean i_in_j = tj.containsAll(ti);
                    boolean j_in_i = ti.containsAll(tj);
                    if( i_in_j || j_in_i ) {
                        boolean ok = true;
                        if( i_in_j && j_in_i ) {
                            ok = (boolean) (onParent.get(i)) != (boolean) onParent.get(j);
                        }
                        assert ok : "" + i + ' ' + j + ' ' + ' ' + taxonSetIDs.get(i) + ' ' + taxonSetIDs.get(j);
                    } else {
                        Set<String> tmp = new HashSet<>(tj);
                        tmp.retainAll(ti);
                        assert tmp.isEmpty();
                    }
                }
            }
        }

        // assume all calibration constraints are Monophyletic
        // TODO: verify that this is a reasonable assumption
        lastMonophyletic = taxonSets.size();

        // sort constraints in increasing set inclusion order, i.e. such that if taxon set i is subset of taxon set j, then i < j
        for (int i = 0; i < lastMonophyletic; i++) {
            for (int j = i + 1; j < lastMonophyletic; j++) {

                final Set<String> taxai = taxonSets.get(i);
                final Set<String> taxaj = taxonSets.get(j);
                Set<String> intersection = new LinkedHashSet<>(taxai);
                intersection.retainAll(taxaj);

                if (intersection.size() > 0) {
                    final boolean bIsSubset  = taxai.containsAll(taxaj);
                    final boolean bIsSubset2 = taxaj.containsAll(taxai);
                    // sanity check: make sure either
                    // o taxonset1 is subset of taxonset2 OR
                    // o taxonset1 is superset of taxonset2 OR
                    // o taxonset1 does not intersect taxonset2
                    if (!(bIsSubset || bIsSubset2)) {
                        throw new IllegalArgumentException("333: Don't know how to generate a Random Tree for taxon sets that intersect, " +
                                "but are not inclusive. Taxonset " + (taxonSetIDs.get(i) == null ? taxai :  taxonSetIDs.get(i)) + 
                                		" and " + (taxonSetIDs.get(j) == null ? taxaj : taxonSetIDs.get(j)));
                    }
                    // swap i & j if b1 subset of b2. If equal sub-sort on 'useOriginate'
                    if (bIsSubset && (!bIsSubset2 || (onParent.get(i) && !onParent.get(j)) ) ) {
                        swap(taxonSets, i, j);
                        swap(distributions, i, j);
                        swap(m_bounds, i, j);
                        swap(taxonSetIDs, i, j);
                        swap(onParent, i, j);
                    }
                }
            }
        }

        if( ICC ) {
            for (int i = 0; i < lastMonophyletic; i++) {
                final Set<String> ti = taxonSets.get(i);
                for (int j = i + 1; j < lastMonophyletic; j++) {
                    final Set<String> tj = taxonSets.get(j);
                    boolean ok = tj.containsAll(ti);
                    if( ok ) {
                        ok = !tj.equals(ti) || (!onParent.get(i) && onParent.get(j));
                        assert ok : "" + i + ' ' + j + ' ' + tj.equals(ti) + ' ' + taxonSetIDs.get(i) + ' ' + taxonSetIDs.get(j);
                    } else {
                        Set<String> tmp = new HashSet<>(tj);
                        tmp.retainAll(ti);
                        assert tmp.isEmpty();
                    }
                }
            }
        }

        for (int i = 0; i < lastMonophyletic; i++) {
            if( onParent.get(i) ) {
                // make sure it is after constraint on node itself, if such exists
                assert( ! (i + 1 < lastMonophyletic && taxonSets.get(i).equals(taxonSets.get(i + 1)) && onParent.get(i) && !onParent.get(i+1) ) );
                // find something to attach to ....
                // find enclosing clade, if any. pick a non-intersecting clade in the enclosed without an onParent constraint, or one whose
                // onParent constraint is overlapping.
                final Set<String> iTaxa = taxonSets.get(i);
                int j = i+1;
                Set<String> enclosingTaxa = sTaxa;
                {
                    String someTaxon = iTaxa.iterator().next();
                    for (/**/; j < lastMonophyletic; j++) {
                        if( taxonSets.get(j).contains(someTaxon) ) {
                            enclosingTaxa = taxonSets.get(j);
                            break;
                        }
                    }
                }
                final int enclosingIndex = (j == lastMonophyletic) ? j : j;
                Set<String> candidates = new HashSet<>(enclosingTaxa);
                candidates.removeAll(iTaxa);
                Set<Integer> candidateClades = new HashSet<>(5);
                List<String> canTaxa = new ArrayList<>();
                for( String c : candidates ) {
                    for(int k = enclosingIndex-1; k >= 0; --k) {
                        if( taxonSets.get(k).contains(c) ) {
                            if( ! candidateClades.contains(k) ) {
                                if( onParent.get(k) ) {
                                    if( !intersecting(m_bounds.get(k), m_bounds.get(i)) ) {
                                        break;
                                    }
                                } else {
                                  if( ! (m_bounds.get(k).lower <= m_bounds.get(i).lower) ) {
                                      break;
                                  }
                                }
                                candidateClades.add(k);
                            }
                            break;
                        }
                        if( k == 0 ) {
                           canTaxa.add(c);
                        }
                    }
                }

                final int sz1 = canTaxa.size();
                final int sz2 = candidateClades.size();

                if( sz1 + sz2 == 0 && i + 1 == enclosingIndex  ) {
                    final Bound ebound = m_bounds.get(enclosingIndex);
                    ebound.restrict(m_bounds.get(i));
                } else {
                    assert sz1 + sz2 > 0;
                    // prefer taxa over clades (less chance of clades useOriginate clashing)
                    final int k = Randomizer.nextInt(sz1 > 0 ? sz1 : sz2);
                    Set<String> connectTo;
                    int insertPoint;
                    if( k < sz1 ) {
                        // from taxa
                        connectTo = new HashSet<>(1);
                        connectTo.add(canTaxa.get(k));
                        insertPoint = i + 1;
                    } else {
                        // from clade
                        final Iterator<Integer> it = candidateClades.iterator();
                        for (j = 0; j < k - sz1 - 1; ++j) {
                            it.next();
                        }
                        insertPoint = it.next();
                        connectTo = new HashSet<>(taxonSets.get(insertPoint));
                        insertPoint = Math.max(insertPoint, i) + 1;
                    }

                    final HashSet<String> cc = new HashSet<String>(connectTo);

                    connectTo.addAll(taxonSets.get(i));
                    if( !connectTo.equals(enclosingTaxa) || enclosingTaxa == sTaxa ) { // equal when clade already exists

                        taxonSets.add(insertPoint, connectTo);
                        distributions.add(insertPoint, distributions.get(i));
                        onParent.add(insertPoint, false);
                        m_bounds.add(insertPoint, m_bounds.get(i));
                        final String tid = taxonSetIDs.get(i);
                        taxonSetIDs.add(insertPoint, tid);
                        lastMonophyletic += 1;
                    } else {
                        // we lose distribution i :(
                        final Bound ebound = m_bounds.get(enclosingIndex);
                        ebound.restrict(m_bounds.get(i));
                    }
                }
                if( true ) {
                    taxonSets.set(i, new HashSet<>());
                    distributions.set(i, null);
                    m_bounds.set(i, new Bound());
                    final String tid = taxonSetIDs.get(i);
                    if( tid != null ) {
                        taxonSetIDs.set(i, "was-" + tid);
                    }
                }
            }
        }

        {
            int icur = 0;
            for (int i = 0; i < lastMonophyletic; ++i, ++icur) {
                final Set<String> ti = taxonSets.get(i);
                if( ti.isEmpty() ) {
                    icur -= 1;
                } else {
                    if( icur < i ) {
                        taxonSets.set(icur, taxonSets.get(i));
                        distributions.set(icur, distributions.get(i));
                        m_bounds.set(icur, m_bounds.get(i));
                        taxonSetIDs.set(icur, taxonSetIDs.get(i));
                        onParent.set(icur, onParent.get(i));
                    }
                }
            }
            taxonSets.subList(icur, lastMonophyletic).clear();
            distributions.subList(icur, lastMonophyletic).clear();
            m_bounds.subList(icur, lastMonophyletic).clear();
            taxonSetIDs.subList(icur, lastMonophyletic).clear();
            onParent.subList(icur, lastMonophyletic).clear();

            lastMonophyletic = icur;
        }

        if( ICC ) {
            for (int i = 0; i < lastMonophyletic; i++) {
                final Set<String> ti = taxonSets.get(i);
                for (int j = i + 1; j < lastMonophyletic; j++) {
                    final Set<String> tj = taxonSets.get(j);
                    boolean ok = tj.containsAll(ti);
                    if( ok ) {
                        ok = !tj.equals(ti) || (!onParent.get(i) && onParent.get(j));
                        assert ok : "" + i + ' ' + j + ' ' + taxonSetIDs.get(i) + ' ' + taxonSetIDs.get(j);
                    } else {
                        Set<String> tmp = new HashSet<>(tj);
                        tmp.retainAll(ti);
                        assert tmp.isEmpty();
                    }
                }
            }
        }

        // map parent child relationships between mono clades. nParent[i] is the immediate parent clade of i, if any. An immediate parent is the
        // smallest superset of i, children[i] is a list of all clades which have i as a parent.
        // The last one, standing for the virtual "root" of all monophyletic clades is not associated with any actual clade
        final int[] nParent = new int[lastMonophyletic];
        children = new List[lastMonophyletic + 1];
        for (int i = 0; i < lastMonophyletic + 1; i++) {
            children[i] = new ArrayList<>();
        }
        for (int i = 0; i < lastMonophyletic; i++) {
            int j = i + 1;
            while (j < lastMonophyletic && !taxonSets.get(j).containsAll(taxonSets.get(i))) {
                j++;
            }
            nParent[i] = j;
            children[j].add(i);
        }

        // make sure upper bounds of a child does not exceed the upper bound of its parent
        for (int i = lastMonophyletic-1; i >= 0 ;--i) {
            if (nParent[i] < lastMonophyletic ) {
                if (m_bounds.get(i).upper > m_bounds.get(nParent[i]).upper) {
                    m_bounds.get(i).upper = m_bounds.get(nParent[i]).upper - 1e-100;
                    assert m_bounds.get(i).lower <=  m_bounds.get(i).upper: i;
                }
            }
        }

        nodeCount = 2 * sTaxa.size() - 1;
        boundPerNode = new Bound[nodeCount];
        distPerNode = new ParametricDistribution[nodeCount];

        buildTree(sTaxa);                                         assert nextNodeNr == nodeCount : "" + nextNodeNr + ' ' + nodeCount;

        double bm = branchMeanInput.get();

        if( bm < 0 ) {
            double maxMean = 0;

            for (ParametricDistribution distr : distPerNode) {
                if( distr != null ) {
                    double m = distr.getMean();
                    if( maxMean < m ) maxMean = m;
                }
            }
            if( maxMean > 0 ) {
                double s = 0;
                for (int i = 2; i <= nodeCount; ++i) {
                    s += 1.0 / i;
                }
                bm = s / maxMean;
            }
        }

        double rate = 1 / (bm < 0 ? 1 : bm);
        boolean succ = false;
        int ntries = 6;
        final double epsi = 0.01/rate;
        double clamp = 1-clampInput.get();
        while( !succ && ntries > 0 ) {
            try {
				succ = setHeights(rate, false, epsi, clamp);
			} catch (ConstraintViolatedException e) {
				throw new RuntimeException("Constraint failed: " + e.getMessage());
			}
            --ntries;
            rate *= 2;
            clamp /= 2;
        }
        if( ! succ ) {
           try {
        	   succ = setHeights(rate, true, 0, 0);
           } catch (ConstraintViolatedException e) {
        	   throw new RuntimeException("Constraint failed: " + e.getMessage());
           }
        }
        assert succ;

        internalNodeCount = sTaxa.size() - 1;
        leafNodeCount = sTaxa.size();

        HashMap<String,Integer> taxonToNR = null;
        // preserve node numbers where possible
        if (m_initial.get() != null) {
            taxonToNR = new HashMap<>();
            for( Node n : m_initial.get().getExternalNodes() ) {
                taxonToNR.put(n.getID(), n.getNr());
            }
        }
        // re-assign node numbers
        setNodesNrs(root, 0, new int[1], taxonToNR);

        initArrays();
    }

    private int setNodesNrs(final Node node, int internalNodeCount, int[] n, Map<String,Integer> initial) {
        if( node.isLeaf() )  {
            if( initial != null ) {
                node.setNr(initial.get(node.getID()));
            } else {
                node.setNr(n[0]);
                n[0] += 1;
            }
        } else {
            for (final Node child : node.getChildren()) {
                internalNodeCount = setNodesNrs(child, internalNodeCount, n, initial);
            }
            node.setNr(nrOfTaxa + internalNodeCount);
            internalNodeCount += 1;
        }
        return internalNodeCount;
    }


	//@Override
    public void getInitialisedStateNodes(final List<StateNode> stateNodes) {
        stateNodes.add(m_initial.get());
    }

    DistanceProvider.Data[] weights = null;

    /**
     * build a tree conforming to the monophyly constraints.
     *
     * @param taxa         the set of taxa
     */
    public void buildTree(final Set<String> taxa) {
        if (taxa.size() == 0)
            return;

        nextNodeNr = nrOfTaxa;

        final Set<Node> candidates = new LinkedHashSet<>();
        int nr = 0;
        for (String taxon : taxa) {
            final Node node = new Node();
            node.setNr(nr);
            node.setID(taxon);
            node.setHeight(0.0);
            candidates.add(node);
            nr += 1;
        }

        if( distances != null ) {
            weights = new DistanceProvider.Data[2*nrOfTaxa-1];
            final Map<String, DistanceProvider.Data> init = distances.init(taxa);
            for( Node tip : candidates ) {
                weights[tip.getNr()] = init.get(tip.getID());
            }
        }

        // copy over tip traits, if any
        if (m_initial.get() != null) {
            processCandidateTraits(candidates, m_initial.get().m_traitList.get());
        } else {
            processCandidateTraits(candidates, m_traitList.get());
        }

        // set a convenience mapping from tip name (id) to node
        final Map<String,Node> allCandidates = new TreeMap<String,Node>();
        for (Node node: candidates) {
            allCandidates.put(node.getID(),node);
        }

        root = buildTree(lastMonophyletic, candidates, allCandidates);
    }

    /**
     * Apply traits to a set of nodes.
     * @param candidates List of nodes
     * @param traitSets List of TraitSets to apply
     */
    private void processCandidateTraits(Set<Node> candidates, List<TraitSet> traitSets) {
        for (TraitSet traitSet : traitSets) {
            for (Node node : candidates) {
                node.setMetaData(traitSet.getTraitName(), traitSet.getValue(node.getID()));
            }
        }
    }

    // Build tree conforming to monophyly constraints, ignoring hard time limits
    private Node buildTree(final int monoCladeIndex, final Set<Node> candidates, final Map<String, Node> allCandidates) {
        final List<Node> remainingCandidates = new ArrayList<Node>();
        final Set<String> taxaDone = new TreeSet<>();
        // build all subtrees
        for (final int iMonoNode : children[monoCladeIndex]) {
            // create list of leaf nodes for this monophyletic MRCA
            final Set<Node> candidates2 = new LinkedHashSet<>();
            final Set<String> bTaxonSet = taxonSets.get(iMonoNode);
            for (String taxon : bTaxonSet) {
                candidates2.add(allCandidates.get(taxon));
            }

            final Node MRCA = buildTree(iMonoNode, candidates2, allCandidates);
            remainingCandidates.add(MRCA);

            taxaDone.addAll(bTaxonSet);
        }

        {
            for (final Node node : candidates) {
                if( !taxaDone.contains(node.getID()) ) {
                    remainingCandidates.add(node);
                }
            }
        }

        final Node c = joinNodes(remainingCandidates);
        if( monoCladeIndex < lastMonophyletic ) {
            final Bound bound = m_bounds.get(monoCladeIndex);
            final int nr = c.getNr();

            if( boundPerNode[nr] != null ) {
                // this can happen when we have a duplicate constraint, like when two 'useOriginate' nodes share the same parent.
                boundPerNode[nr].upper = Math.min(boundPerNode[nr].upper, bound.upper);
                boundPerNode[nr].lower = Math.max(boundPerNode[nr].lower, bound.lower);
                assert boundPerNode[nr].lower <= boundPerNode[nr].upper : nr;
            } else {
                boundPerNode[nr] = bound;
            }
            final ParametricDistribution distr = distributions.get(monoCladeIndex);
            distPerNode[nr] = distr;
        }
        return c;
    }

    private Node joinNodes(List<Node> nodes) {
        if( nodes.size() == 1) {
            return nodes.get(0);
        }
        //final int nrOnExit = nextNodeNr + nodes.size() - 1;

        double h = -1;
        for( Node n : nodes ) {
            h = Math.max(h, n.getHeight());
        }
        double dt = 1;

        while (nodes.size() > 1) {
            int k = nodes.size() - 1;
            int l = k, r = k-1;
            if( k > 1 && distances != null ) {
                double[] ds = new double[(k * (k + 1)) / 2];
                int loc = 0;
                for (int i = 0; i < k; ++i) {
                    for (int j = i + 1; j < k+1; ++j) {
                        double d = distances.dist(weights[nodes.get(i).getNr()], weights[nodes.get(j).getNr()]);
                        ds[loc] = 1/d;
                        ++loc;
                    }
                }
                double s = 0.0;
                for(int i = 0; i < loc; ++i) {
                    s += ds[i];
                }

                double u = Randomizer.nextDouble();
                int m = 0;
                for (int i = 0; i < k; ++i) {
                    for (int j = i + 1; j < k+1; ++j) {
                        u -= ds[m]/s;
                        m += 1;
                        if( u < 0 ) {
                            l = j;
                            break;
                        }
                    }
                    if( u < 0 ) {
                        r = i;
                        break;
                    }
                }
                assert( u < 0 ) ;
            }
            assert( r < l );

            final Node left = nodes.remove(l);
            final Node right = nodes.get(r);

            final Node newNode = new Node();
            newNode.setNr(nextNodeNr++);   // multiple tries may generate an excess of nodes assert(nextNodeNr <= nrOfTaxa*2-1);
            h += dt;
            newNode.setHeight(h);
            newNode.setLeft(left);
            left.setParent(newNode);
            newNode.setRight(right);
            right.setParent(newNode);
            if( distances != null ) {
                final DistanceProvider.Data wr = weights[right.getNr()];
                distances.update(wr, weights[left.getNr()]);
                weights[newNode.getNr()] = wr;
            }
            nodes.set(r, newNode);
        }
        //assert nextNodeNr == nrOnExit : "" + nextNodeNr + ',' + nrOnExit;
        return nodes.get(0);
    }

    private boolean setHeights(final double rate, final boolean safe, final double epsi, final double clampBoundsLevel) throws
            ConstraintViolatedException {
        //  node low >= all child nodes low. node high < parent high
        assert rate > 0;
        assert 0 <= clampBoundsLevel && clampBoundsLevel < 1;

        Node[] post = listNodesPostOrder(null, null);
        postCache = null; // can't figure out the javanese to call TreeInterface.listNodesPostOrder

        Bound[] bounds = new Bound[boundPerNode.length];

        for(int i = post.length-1; i >= 0; --i) {
            final Node node = post[i];
            final int nr = node.getNr();
            bounds[nr] = boundPerNode[nr];

            Bound b = bounds[nr];
            if( b == null ) {
                bounds[nr] = b = new Bound();
            }
            if( b.lower < 0 ) {
                b.lower = 0.0;
            }
            if( clampBoundsLevel > 0 ) {
                final ParametricDistribution distr = distPerNode[nr];
                if( distr != null ) {
                    try {
                        final double low = distr.inverseCumulativeProbability(clampBoundsLevel / 2);
                        final double high = distr.inverseCumulativeProbability(1 - clampBoundsLevel / 2);
                        if( distr.density(low) != distr.density(distr.getMean()) ) {
                            if( b.upper >= low && high >= b.lower ) {
                                b.lower = Math.max(b.lower, low);
                                b.upper = Math.min(b.upper, high);
                            }
                        }
                    } catch (MathException e) {
                        //e.printStackTrace();
                    }
                }
            }

            final Node p = node.getParent();
            if( p != null ) {
                //assert p.getNr() < bounds.length;
                Bound pb = bounds[p.getNr()];                                               assert ( pb != null );
                if( !pb.upper.isInfinite() && !(b.upper < pb.upper ) ) {
                    b.upper = pb.upper;
                }
            }
        }

        for( Node node : post ) {
            final int nr = node.getNr();
            if( node.isLeaf() ) {
               // Bound b = boundPerNode[nr];  assert b != null;
            } else {
                Bound b = bounds[nr];       assert(b != null);
                for( Node c : node.getChildren() ) {
                    final Bound cbnd = bounds[c.getNr()];
                    b.lower = Math.max(b.lower, cbnd.lower);
                    cbnd.upper = Math.min(cbnd.upper, b.upper);
                }
                if( b.lower > b.upper ) {
                    throw new ConstraintViolatedException();
                }
            }
        }

        if (rootHeightInput.get() != null) {
            final double h = rootHeightInput.get();
            Bound b = bounds[root.getNr()];
            if( b.lower <= h && h <= b.upper ) {
                b.upper = h;
            }
        }

        for( Node node : post ) {
            if( ! node.isLeaf() ) {
                final int nr = node.getNr();
                Bound b = bounds[nr];
                double h = -1;
                for( Node c : node.getChildren() ) {
                    h = Math.max(c.getHeight(), h);
                }
                if( h > b.upper ) {
                    throw new ConstraintViolatedException();
                }
                if( b.upper.isInfinite() ) {
                    if( ! b.lower.isInfinite() ) {
                       h = Math.max(h, b.lower);
                    }
                    h += Randomizer.nextExponential(rate);
                } else {
                    if( !b.lower.isInfinite() ) {
                        h = Math.max(b.lower, h);
                    }

                    final double range = b.upper - h;
                    double r;
                    if( safe ) {
                        r = (range / post.length) * Randomizer.nextDouble();                                assert r > 0 && h + r < b.upper;
                    } else {
                        r = Randomizer.nextExponential(rate);
                        if( r >= range ) {
                            r = range * Randomizer.nextDouble();
                        }
                    }
                    assert h + r <= b.upper;
                    if( r <= epsi && h + r + epsi*1.001 < b.upper ) {
                        r += 1.001*epsi;
                    }
                    h += r;
                }
                node.setHeight(h);
            }
        }

        if (rootHeightInput.get() != null) {
            final double h = rootHeightInput.get();
            root.setHeight(h);
        }

        // for now fail - this happens rarely
        for(int i = post.length-1; i >= 0; --i) {
            final Node node = post[i];
            if( !node.isRoot() && node.getLength() <= epsi ) {
               return false;
            }
        }
        return true;
    }

    @Override
    public String[] getTaxaNames() {
        if (m_sTaxaNames == null) {
            final List<String> sTaxa;
            if (taxaInput.get() != null) {
                sTaxa = taxaInput.get().getTaxaNames();
            } else {
                sTaxa = m_taxonset.get().asStringList();
            }
            m_sTaxaNames = sTaxa.toArray(new String[sTaxa.size()]);
        }
        return m_sTaxaNames;
    }
}