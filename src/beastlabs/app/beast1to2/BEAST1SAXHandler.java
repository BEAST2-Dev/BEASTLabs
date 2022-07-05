package beastlabs.app.beast1to2;

import beast.base.evolution.alignment.Sequence;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.util.*;

/**
 * parse BEAST 1 XML alignment section only, and create List<Sequence>
 * TODO not working, but it seems SAX bug, content is empty for sequence, even there is sequence in xml
 * @author Walter Xie
 */
public class BEAST1SAXHandler extends DefaultHandler {
//    private static boolean isBEAST2XML = false; // false, then assume to be BEAST 1 xml

    java.util.Map<String, List<Sequence>> alignments = new HashMap<>();
    List<Sequence> alignment = null;
    String algID = null;
    String dataType = null;
    Sequence sequence = null;
    String taxon = null;
    String content = null;

    @Override
    //Triggered when the start of tag is found.
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
//        if (XMLUtil.isBEASTXML(qName)) {
//            String version = attributes.getValue(BEAST_VERSION_ATTRIBUTE);
//            if (version == null)
//                isBEAST2XML = false;
//            System.out.println("Loading BEAST " + (isBEAST2XML ? "2" : "1") + " xml ...");
//        } else
        if (XMLUtil.isAlignment(qName)) {
            alignment = new ArrayList<>();
            algID = attributes.getValue(XMLUtil.ID);
            dataType = attributes.getValue(XMLUtil.DATA_TYPE);

        } else if (XMLUtil.isSequence(qName)) {

        } else if (XMLUtil.isTaxon(qName)) {
            taxon = attributes.getValue(XMLUtil.IDREF);

        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (XMLUtil.isAlignment(qName) && algID != null) {
            if (alignments.containsKey(algID))
                throw new RuntimeException("Find duplicate alignment " + algID);

            alignments.put(algID, alignment);

            System.out.println("Read alignment " + algID + ": " + alignment.size() + " sequences, dataType = " + dataType);

        } else if (XMLUtil.isSequence(qName)) {
//            String data = content;
            if (content == null || content.isEmpty())
                throw new RuntimeException("No sequence in " + taxon + " in " + algID);

            try {
                sequence = new Sequence(taxon, content);
            } catch (Exception e) {
                e.printStackTrace();
            }
            alignment.add(sequence);

            System.out.println(sequence.dataInput.get());
            System.out.println("Add sequence " + taxon + " to alignment " + algID);

        } else if (XMLUtil.isTaxon(qName)) {

        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        content = String.copyValueOf(ch, start, length).trim();
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Working path is missing in the argument !");

        String xmlPath = args[0];

        java.util.Map<String, List<Sequence>> alignments = XMLUtil.parse(new File(xmlPath));

        SortedSet<String> aligs = new TreeSet<>(alignments.keySet());
        for (String al : aligs) {
            List<Sequence> alignment = alignments.get(al);
            System.out.print("Key : " + al + ", " + alignment.size() + " Value : ");
            for (Sequence seq : alignment) {
                System.out.print(seq.taxonInput.get() + ", " + seq.dataInput.get().length() + ", ");
            }
            System.out.println();
        }


    }
}
