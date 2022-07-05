package beastlabs.evolution.tree;

import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.parser.NexusParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Annotate nodes of a tree according to an annotation map,
 * which is imported from a tab delimited file with header
 * (1 trait only at the moment).
 * The 1st column is tip labels, and the 2nd is trait values.
 *
 * The annotation map uses tip labels as key, and trait value as value.
 * The trait name is given by the constructor. It can be null, in that case,
 * then the 2nd column name from header is assigned to the trait name.
 *
 * Usage: TreeAnnotation treeFile mapFilePath
 *
 * @author Walter Xie
 */
public class TreeAnnotation {

    /**
     * key is tip labels, value is trait value
     */
    Map<String, String> annotation = new HashMap<>();
    /**
     * the name of the trait
     */
    String traitName;

    public TreeAnnotation(String traitName) {
        this.traitName = traitName;
    }

    public TreeAnnotation(String traitName, String mapFilePath) {
        this(traitName);
        importAnnotation(mapFilePath); // use 2nd col
    }


    /**
     * classic method to use {@link NexusParser NexusParser} to read tree file
     *
     * @param treeFilePath
     * @return trees {@link List<Tree> List<Tree>}
     * @throws IOException
     */
    public List<Tree> readNexusTreeFile(String treeFilePath) throws IOException {
        File treeFile = new File(treeFilePath);

        NexusParser parser = new NexusParser();
        parser.parseFile(treeFile);
        List<Tree> trees = parser.trees;

        System.out.println("Parsed " + trees.size() + " trees from " + treeFilePath);

        return trees;
    }

    // TODO extend to multiple traits

    /**
     * import annotation by a tab delimited file with header, 1 trait only,
     * if traitName is null then set traitName from header
     *
     * @param mapFilePath
     */
    public void importAnnotation(String mapFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(mapFilePath))) {
            int l = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (l == 0) {
                    System.out.println("Annotation file header : " + line);
                    String[] values = line.split("\t");
                    // if traitName is null then set traitName from header
                    if (this.traitName == null)
                        this.traitName = values[1];
                    System.out.println("Pick up trait : " + this.traitName);
                } else {
                    String[] values = line.split("\t");
                    // only take 1st 2 columns
                    String tip_name = values[0];
                    String meta_data = values[1];
                    annotation.put(tip_name, meta_data);
                }
                l++;
            }
            System.out.println("Load " + l + " annotations from " + mapFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getAnnotation() {
        return annotation;
    }

    /**
     *
     * annotate {@link Node Node}s of each {@link Tree Tree} in a list,
     * according to an {@link Map<String, String> annotation}
     * which is imported from {@link #importAnnotation(String)}.
     *
     * @param trees {@link List<Tree> List<Tree>}
     * @param replaceAnnotation if true then replace previous meta data to the given new trait
     * @param noMetaDataInternalNodes remove meta data and length meta data from all internal nodes,
     *                                which is used to process zero-branch tree(s).
     */
    public void annotateNodes(List<Tree> trees, boolean replaceAnnotation, boolean noMetaDataInternalNodes) {

        for (Tree tree : trees) {
            for (int i = 0; i < tree.getNodeCount(); i++) {
                Node node = tree.getNode(i);
                if (noMetaDataInternalNodes && !node.isLeaf()) {
                    // this is for Zero-branch trees
                    node.metaDataString = null;
                    node.lengthMetaDataString = null;

                } else {

                    String tipLabel = node.getID();
                    String trait = annotation.get(tipLabel);
                    if (trait == null)
                        throw new IllegalArgumentException("Cannot find trait for the tip " + tipLabel);

                    //setMetaData not working, have to change metaDataString
//                node.setMetaData(traitName, trait);
                    if (replaceAnnotation || node.metaDataString == null) {
                        node.metaDataString = this.traitName + "='" + trait + "'";
                    } else {
                        node.metaDataString = node.metaDataString + "," + this.traitName + "='" + trait + "'";
                    }

                }
            }
        }
    }

    // Usage: TreeAnnotation frq500k-10.tree homininis.txt
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: TreeAnnotation treeFile mapFilePath");
            System.exit(0);
        }

        String treeFilePath = args[0];
        String mapFilePath = args[1];


        String traitName = null; // "group";
        // if traitName is null then set traitName from header
        TreeAnnotation treeAnnotation = new TreeAnnotation(traitName, mapFilePath);

        // import trees
        List<Tree> trees = treeAnnotation.readNexusTreeFile(treeFilePath);
        // import annotation map
        treeAnnotation.importAnnotation(mapFilePath); // use 2nd col
        // annotate Nodes in tree
        treeAnnotation.annotateNodes(trees, true, true);

        // print trees
        for (int i = 0; i < trees.size(); i++) {
            Tree tree = trees.get(i);
            System.out.println("tree ANNOTATED_TREE_" + (i + 1) + " = " +
                    tree.getRoot().toSortedNewick(new int[]{0}, true) + ";");
        }
    }

}
