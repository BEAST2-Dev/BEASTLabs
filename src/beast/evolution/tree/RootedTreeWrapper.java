package beast.evolution.tree;

import beast.evolution.taxonomy.Taxon;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A wrapper of JEBL RootedTree and BEAST2 tree
 *
 * @author Walter Xie
 */

public class RootedTreeWrapper implements RootedTree {

    private final Tree tree;

    public RootedTreeWrapper (Tree tree) {
         this.tree = tree;
    }

    @Override
    public Set<Node> getExternalNodesSet() {
        return new HashSet<Node>(tree.getExternalNodes());
    }

    @Override
    public Set<Node> getInternalNodesSet() {
        return new HashSet<Node>(tree.getInternalNodes());
    }

    @Override
    public Set<Taxon> getTaxa() {
        //TODO improper code of TaxonSet in BEAST2
        Set<Taxon> taxonSet = new HashSet<Taxon>();
        for (Node node : tree.getExternalNodes()) {
            try {
                Taxon taxon = new Taxon(node.getID());
                taxonSet.add(taxon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return taxonSet;
    }

    @Override
    public Taxon getTaxon(Node node) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean isExternal(Node node) {
        return !node.isLeaf();
    }

    @Override
    public void renameTaxa(Taxon from, Taxon to) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Node getNode(Taxon taxon) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<Node> getChildren(Node node) {
        return node.getChildren();
    }

    @Override
    public boolean hasHeights() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public double getHeight(Node node) {
        return node.getHeight();
    }

    @Override
    public boolean hasLengths() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public double getLength(Node node) {
        return node.getLength();
    }

    @Override
    public Node getParent(Node node) {
        return node.getParent();
    }

    @Override
    public Node getRootNode() {
        return tree.getRoot();
    }

    @Override
    public boolean conceptuallyUnrooted() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean isRoot(Node node) {
        return node.isRoot();
    }
}
