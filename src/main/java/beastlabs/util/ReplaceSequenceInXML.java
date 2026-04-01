package beastlabs.util;

import java.io.*;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
import java.util.*;

import beast.base.parser.NexusParser;
import beast.base.parser.XMLProducer;

/**
 * Given nex file(s) to replace sequence in the xml, but tip names have to be same.
 * use java 1.7 File.copy
 * @author Walter Xie
 *
 * move to New Zealand Genomic Observatory Toolkit
 * https://github.com/CompEvol/NZGOT/tree/master/src/nzgo/toolkit/beast
 */
@Deprecated
public class ReplaceSequenceInXML {

    private static int treeTotal = 128;

    public static void main(String[] args) throws IOException {

        if (args.length != 2)
            throw new IllegalArgumentException("XML input file and folder containing nex files are missing !");

        String xmlFilePath = args[0];
        String nexFilePath = args[1];

        String stem_old = "tree_0_";
        String stem_end = "_new.xml";

        String xmlFileName = xmlFilePath + File.separator + "tree_" + treeTotal + "_0" + stem_end;
//        Path source = Paths.get(xmlFileName);
//        if (!Files.exists(source)) throw new IllegalArgumentException("Cannot find input xml " + xmlFileName);

        // copy sample XML to target folder
//        Path target = Paths.get(nexFilePath + File.separator + "xml" + File.separator + "tree_" + treeTotal + "_0.xml");
//        Files.copy(source, target);
//        System.out.println("\nCopy sample XML to " + target + " ...");

        for (int i = 0; i < 100; i++) { // 100
            String stem = "tree_" + Integer.toString(i) + "_";

            System.out.println("\nReading all nex files from " + nexFilePath + ", stem = " + stem);
            Map<String, String> parserMap = readAllNexus(nexFilePath, stem);

            try {
                // read XML
                BufferedReader reader = new BufferedReader(new FileReader(xmlFileName));
                System.out.println("\nReading XML " + xmlFileName + " ...");

                // write new XML
                String outFile = xmlFilePath + File.separator + treeTotal + File.separator +
                        "tree_" + treeTotal + "_" + Integer.toString(i) + stem_end;
                PrintStream out = new PrintStream(new FileOutputStream(outFile));
                System.out.println("\nWriting new XML " + outFile + " ...");

                String line = reader.readLine();
                while (line != null) {
//                    if (line.contains("<?xml")) {
//                        out.println(line);
//                        line = reader.readLine();
//                        // skip some empty lines
//                        while (line.trim().equals("")) line = reader.readLine();
//                    }
                    if (line.contains("<data id=") || line.contains("<sequence id=") || line.contains("</data>")
                            || line.contains("<taxon id=") || line.contains("</taxon>")) {
                        // skip sequence or species - individuals mapping
                    } else if (line.contains("beast.math.distributions.Beta")) {
                        // trigger to print new sequence
                        for (String xml : parserMap.values()) {
                            out.println(xml);
                        }
                        out.println(line);
                    } else if (line.contains("<taxonset id=\"taxonsuperset\"")) {
                        // trigger to print new species - individuals mapping
                        out.println(line);
                        writeSpIndMapping(out);
                    } else {
                        String newLine = line.replaceAll(stem_old, stem);
                        out.println(newLine);
                    }

                    line = reader.readLine();
                }

                reader.close();
                out.flush();
                out.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    } // main

    private static Map<String, String> readAllNexus(String nexFilePath, String stem) {
        File folder = new File(nexFilePath);
        File[] listOfFiles = folder.listFiles();

        Map<String, String> parserMap = new HashMap<String, String>();
        for (File file : listOfFiles) {
            String fileName = file.getName();
            if (file.isFile() && fileName.endsWith("nex") && fileName.startsWith(stem)) {
                try {
                    String index = fileName.substring(fileName.lastIndexOf("_")+1, fileName.lastIndexOf("."));
                    System.out.println("\nReading nex " + file + ", index = " + index);

                    if (parserMap.containsKey(index)) throw new IllegalArgumentException("parser map already had index = " + index);

                    if (Integer.parseInt(index) < treeTotal) {
                        NexusParser parser = new NexusParser();
                        parser.parseFile(file);

//                    if (parser.m_taxa != null) {
//                        System.out.println(parser.m_taxa.size() + " taxa");
//                        System.out.println(Arrays.toString(parser.m_taxa.toArray(new String[0])));
//                    } else {
//                        throw new IllegalArgumentException("No taxa in nexus file " + fileName);
//                    }
//                    if (parser.m_trees != null) {
//                        System.out.println(parser.m_trees.size() + " trees");
//                    }
                        if (parser.m_alignment != null) {
                            String sXML = new XMLProducer().toRawXML(parser.m_alignment, "alignment");
//                            System.out.println(sXML);
                            parserMap.put(index, sXML);
                        } else {
                            throw new IllegalArgumentException("No alignment in nexus file " + fileName);
                        }
//                    if (parser.m_traitSet != null) {
//                        String sXML = new XMLProducer().toXML(parser.m_traitSet);
//                        System.out.println(sXML);
//                    }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        System.out.println("\nRead " + parserMap.size() + " nex files in total");
        return parserMap;
    }

    private static void writeSpIndMapping(PrintStream out) {
        StringBuilder xml = new StringBuilder("                <taxon id=\"s0\" spec=\"TaxonSet\">\n" +
                "                    <taxon id=\"s0_tip6\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip61\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip5\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip51\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip7\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip71\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip2\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip21\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip1\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip11\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip4\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip41\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip3\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip31\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip0\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s0_tip01\" spec=\"Taxon\"/>\n" +
                "                </taxon>\n" +
                "                <taxon id=\"s2\" spec=\"TaxonSet\">\n" +
                "                    <taxon id=\"s2_tip4\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip41\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip3\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip31\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip6\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip61\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip5\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip51\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip7\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip71\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip0\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip01\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip2\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip21\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip1\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s2_tip11\" spec=\"Taxon\"/>\n" +
                "                </taxon>\n" +
                "                <taxon id=\"s1\" spec=\"TaxonSet\">\n" +
                "                    <taxon id=\"s1_tip3\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip31\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip2\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip21\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip5\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip51\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip4\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip41\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip7\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip71\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip6\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip61\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip1\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip11\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip0\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s1_tip01\" spec=\"Taxon\"/>\n" +
                "                </taxon>\n" +
                "                <taxon id=\"s3\" spec=\"TaxonSet\">\n" +
                "                    <taxon id=\"s3_tip7\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip71\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip6\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip61\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip5\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip51\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip4\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip41\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip3\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip31\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip2\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip21\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip1\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip11\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip0\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s3_tip01\" spec=\"Taxon\"/>\n" +
                "                </taxon>\n" +
                "                <taxon id=\"s4\" spec=\"TaxonSet\">\n" +
                "                    <taxon id=\"s4_tip6\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip61\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip5\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip51\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip7\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip71\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip2\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip21\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip1\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip11\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip4\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip41\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip3\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip31\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip0\" spec=\"Taxon\"/>\n" +
                "                    <taxon id=\"s4_tip01\" spec=\"Taxon\"/>\n" +
                "                </taxon>\n");

        out.print(xml);
    }
}
