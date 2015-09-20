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
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.operators.DistanceProvider;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.ParametricDistribution;
import beast.util.Randomizer;

import java.util.*;


@Description("This class provides the basic engine for coalescent simulation of a given demographic model over a given time period. ")
public class SimpleRandomTree extends Tree implements StateNodeInitialiser {
    public Input<Alignment> taxaInput = new Input<>("taxa", "set of taxa to initialise tree specified by alignment");

    public Input<List<MRCAPrior>> calibrationsInput =
            new Input<List<MRCAPrior>>("constraint", "specifies (monophyletic or height distribution) constraints on internal nodes",
                    new ArrayList<MRCAPrior>());
    public Input<Double> rootHeightInput =
            new Input<Double>("rootHeight", "If specified the tree will be scaled to match the root height, if constraints allow this");
    public Input<Double> rateInput = new Input<Double>("branchMean", "Unrestricted brances will have an exponentialy distributed lengthwith this mean.",
            1.0, Input.Validate.OPTIONAL);

    public Input<DistanceProvider> distancesInput = new Input<>("weights", "");

    // total nr of taxa
    int nrOfTaxa;

    class Bound {
        Double upper = Double.POSITIVE_INFINITY;
        Double lower = Double.NEGATIVE_INFINITY;

        public String toString() {
            return "[" + lower + "," + upper + "]";
        }
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

    // number of the next internal node, used when creating new internal nodes
    int nextNodeNr;

    private DistanceProvider distances;

    // used to indicate one of the MRCA constraints could not be met
    protected class ConstraintViolatedException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    @Override
    public void initAndValidate() throws Exception {
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

    public void initStateNodes() throws Exception {
        doTheWork();
    }

    public void doTheWork() throws Exception {
        // find taxon sets we are dealing with
        taxonSets = new ArrayList<>();
        m_bounds = new ArrayList<>();
        distributions = new ArrayList<>();
        taxonSetIDs = new ArrayList<>();
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
	            final Set<String> bTaxa = new HashSet<>();
	        	if (taxonSet.asStringList() == null) {
	        		taxonSet.initAndValidate();
	        	}
	            for (final String sTaxonID : taxonSet.asStringList()) {

	                if (!sTaxa.contains(sTaxonID)) {
	                    throw new Exception("Taxon <" + sTaxonID + "> could not be found in list of taxa. Choose one of " + sTaxa.toArray(new String[0]));
	                }
	                bTaxa.add(sTaxonID);
	            }
	            final ParametricDistribution distr = prior.distInput.get();
	            final Bound bounds = new Bound();
	            if (distr != null) {
	        		List<BEASTInterface> plugins = new ArrayList<BEASTInterface>();
	        		distr.getPredecessors(plugins);
	        		for (int i = plugins.size() - 1; i >= 0 ; i--) {
	        			plugins.get(i).initAndValidate();
	        		}
	                bounds.lower = distr.inverseCumulativeProbability(0.0) + distr.offsetInput.get();
	                bounds.upper = distr.inverseCumulativeProbability(1.0) + distr.offsetInput.get();
	            }

	            if (prior.isMonophyleticInput.get()) {
	                // add any monophyletic constraint
	                taxonSets.add(lastMonophyletic, bTaxa);
	                distributions.add(lastMonophyletic, distr);
	                m_bounds.add(lastMonophyletic, bounds);
	                taxonSetIDs.add(prior.getID());
	                lastMonophyletic++;
	            } else {
	                // only calibrations with finite bounds are added
	                if (!Double.isInfinite(bounds.lower) || !Double.isInfinite(bounds.upper)) {
	                    taxonSets.add(bTaxa);
	                    distributions.add(distr);
	                    m_bounds.add(bounds);
	                    taxonSetIDs.add(prior.getID());
	                }
	            }
            }
        }

        // assume all calibration constraints are MonoPhyletic
        // TODO: verify that this is a reasonable assumption
        lastMonophyletic = taxonSets.size();

        // sort constraints in increasing set inclusion order, i.e. such that if taxon set i is subset of taxon set j, then i < j
        for (int i = 0; i < lastMonophyletic; i++) {
            for (int j = i + 1; j < lastMonophyletic; j++) {

                Set<String> intersection = new HashSet<>(taxonSets.get(i));
                intersection.retainAll(taxonSets.get(j));

                if (intersection.size() > 0) {
                    final boolean bIsSubset = taxonSets.get(i).containsAll(taxonSets.get(j));
                    final boolean bIsSubset2 = taxonSets.get(j).containsAll(taxonSets.get(i));
                    // sanity check: make sure either
                    // o taxonset1 is subset of taxonset2 OR
                    // o taxonset1 is superset of taxonset2 OR
                    // o taxonset1 does not intersect taxonset2
                    if (!(bIsSubset || bIsSubset2)) {
                        throw new Exception("333: Don't know how to generate a Random Tree for taxon sets that intersect, " +
                                "but are not inclusive. Taxonset " + taxonSetIDs.get(i) + " and " + taxonSetIDs.get(j));
                    }
                    // swap i & j if b1 subset of b2
                    if (bIsSubset) {
                        swap(taxonSets, i, j);
                        swap(distributions, i, j);
                        swap(m_bounds, i, j);
                        swap(taxonSetIDs, i, j);
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
            children[i] = new ArrayList<Integer>();
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
                }
            }
        }

        nodeCount = 2 * sTaxa.size() - 1;
        boundPerNode = new Bound[nodeCount];

        buildTree(sTaxa);
        assert nextNodeNr == nodeCount;
        final double rate = 1/rateInput.get();
        boolean succ = false;
        int ntries = 4;
        while( !succ && ntries > 0 ) {
            succ = setHeights(rate, false);
            --ntries;
        }
        if( ! succ ) {
           succ = setHeights(rate, true);
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

        if (m_initial.get() != null) {
            m_initial.get().assignFromWithoutID(this);
        }
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

        final Set<Node> candidates = new HashSet<>();
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
            final Set<Node> candidates2 = new HashSet<>();
            final Set<String> bTaxonSet = taxonSets.get(iMonoNode);
            for (String taxon : bTaxonSet) {
                candidates2.add(allCandidates.get(taxon));
            }

            final Node MRCA = buildTree(iMonoNode, candidates2, allCandidates);
            remainingCandidates.add(MRCA);

            taxaDone.addAll(bTaxonSet);
        }

        {
            // find leftover free nodes
            //final List<Node> left = new ArrayList<Node>();
            for (final Node node : candidates) {
                if( !taxaDone.contains(node.getID()) ) {
                    remainingCandidates.add(node);
                }
            }
           // if( left.size() > 0 ) {
             //  remainingCandidates.add(joinNodes(left));
           // }
        }

        final Node c = joinNodes(remainingCandidates);
        if( monoCladeIndex < lastMonophyletic ) {
            final Bound bound = m_bounds.get(monoCladeIndex);
            final int nr = c.getNr();
            if( boundPerNode[nr] != null ) {
                // this can happen when we have a duplicate constraint, like when we combine multi-constraints and single
                boundPerNode[nr].upper = Math.min(boundPerNode[nr].upper, bound.upper);
                boundPerNode[nr].lower = Math.max(boundPerNode[nr].lower, bound.lower);
            } else {
                boundPerNode[nr] = bound;
            }
        }
        return c;
    }

    private Node joinNodes(List<Node> nodes) {
        if( nodes.size() == 1) {
            return nodes.get(0);
        }

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
        return nodes.get(0);
    }

    private boolean setHeights(final double rate, final boolean safe) throws ConstraintViolatedException {
        //  node low >= all child nodes low. node high < parent high
        assert rate > 0;
        Node[] post = listNodesPostOrder(null, null);
        postCache = null; // can't figure out the javanese to call TreeInterface.listNodesPostOrder


        for(int i = post.length-1; i >= 0; --i) {
            final Node node = post[i];
            final int nr = node.getNr();
            Bound b = boundPerNode[nr];
            if( b == null ) {
                boundPerNode[nr] = b = new Bound();
            }
            final Node p = node.getParent();
            if( p != null ) {
                assert p.getNr() < boundPerNode.length;
                Bound pb = boundPerNode[p.getNr()];
                assert ( pb != null );
                if( !pb.upper.isInfinite() && !(b.upper < pb.upper ) ) {
                    b.upper = pb.upper;
                }
            }
        }

        for( Node node : post ) {
            final int nr = node.getNr();
            if( node.isLeaf() ) {
               //Bound b = boundPerNode[nr];  assert b != null;
            } else {
                Bound b = boundPerNode[nr];
                if( b == null ) {
                   boundPerNode[nr] = b = new Bound();
                }
                for( Node c : node.getChildren() ) {
                    final Bound cbnd = boundPerNode[c.getNr()];
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
            Bound b = boundPerNode[root.getNr()];
            if( b.lower <= h && h <= b.upper ) {
                b.upper = h;
            }
        }

        for( Node node : post ) {
            if( ! node.isLeaf() ) {
                final int nr = node.getNr();
                Bound b = boundPerNode[nr];
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
                        r = (range / post.length) * Randomizer.nextDouble();
                        assert r > 0 && h + r < b.upper;
                    } else {
                        r = Randomizer.nextExponential(rate);
                        if( r >= range ) {
                            r = range * Randomizer.nextDouble();
                        }
                    }
                    assert h + r <= b.upper;
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
            if( !node.isRoot() && node.getLength() == 0 ) {
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