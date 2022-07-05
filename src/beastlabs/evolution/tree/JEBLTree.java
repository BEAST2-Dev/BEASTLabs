/*
 * Tree.java
 *
 * (c) 2005 JEBL Development Team
 *
 * This package is distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */
package beastlabs.evolution.tree;

import java.util.Set;

import beast.base.evolution.tree.Node;

/**
 * A rooted or unrooted tree. This interface is the common base class for all trees,
 * and contains only operations for unrooted trees. The subinterface RootedTree
 * contains additional methods that make sense only on rooted trees.
 *
 * Both interfaces contain no mutator methods. As of 2006-12-08, the only way
 * to mutate a tree after it has been built is to use its concrete class
 * instead of the JEBLTree or RootedTree interface.
 *
 * @author rambaut
 * @author Alexei Drummond
 *
 * @version $Id: JEBLTree.java 627 2007-01-15 03:50:40Z pepster $
 */
public interface JEBLTree {

    /**
     * @return a set of all nodes that have degree 1.
     * These nodes are often refered to as 'tips'.
     */
    Set<Node> getExternalNodesSet(); // conflict to BEAST 2 Tree, return List<Node>

    /**
     * @return a set of all nodes that have degree 2 or more.
     * These nodes are often refered to as internal nodes.
     */
    Set<Node> getInternalNodesSet(); // conflict to BEAST 2 Tree, return List<Node>

    /**
     * @return the set of taxa associated with the external
     * nodes of this tree. The size of this set should be the
     * same as the size of the external nodes set.
     */
    Set<beast.base.evolution.alignment.Taxon> getTaxa();

    /**
     * @param node the node whose associated taxon is being requested.
     * @return the taxon object associated with the given node, or null
     * if the node is an internal node.
     */
    beast.base.evolution.alignment.Taxon getTaxon(Node node);

    /**
     * @param node the node
     * @return true if the node is of degree 1.
     */
    boolean isExternal(Node node);

    /**
     * @param taxon the taxon
     * @return the external node associated with the given taxon, or null
     * if the taxon is not a member of the taxa set associated with this tree.
     */
    Node getNode(beast.base.evolution.alignment.Taxon taxon);

    void renameTaxa(beast.base.evolution.alignment.Taxon from, beast.base.evolution.alignment.Taxon to);
}