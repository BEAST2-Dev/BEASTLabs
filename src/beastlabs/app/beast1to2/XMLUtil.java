package beastlabs.app.beast1to2;

import beast.base.evolution.alignment.Sequence;
import beast.base.parser.XMLParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * XMLUtil
 *
 * @author Walter Xie
 */
public class XMLUtil {

    final static String ID = "id";
    final static String IDREF = "idref";
//    final static String BEAST_VERSION_ATTRIBUTE = "version";
    final static String DATA_TYPE = "dataType";
    final static String direction = "direction";
    final static String UNITS = "units";

    public static boolean isBEASTXML(String name) {
        return isTag(XMLParser.BEAST_ELEMENT, name);
    }

    //****** for BEAST 1 xml *******
    public static boolean isAlignment(String name) {
        return isTag("alignment", name);
    }

    public static boolean isSequence(String name) {
        return isTag("sequence", name);
    }

    public static boolean isTaxa(String name) {
        return isTag("taxa", name);
    }

    public static boolean isTaxon(String name) {
        return isTag("taxon", name);
    }

    public static boolean isDate(String name) {
        return isTag("date", name);
    }

    /**
     * does the tag equal the given name ignoring case
     *
     * @param tag
     * @param name
     * @return
     */
    public static boolean isTag(String tag, String name) {
        if (tag.equalsIgnoreCase(name)) return true;
        return false;
    }

    /**
     * get List<Sequence> from xml file, the data section is required
     * @param xmlFile
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static java.util.Map<String, List<Sequence>> parse(File xmlFile) throws IOException, ParserConfigurationException, SAXException {
        InputStream xmlInput = new FileInputStream(xmlFile);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        BEAST1SAXHandler handler = new BEAST1SAXHandler();
        saxParser.parse(xmlInput, handler);

        return handler.alignments;
    }


}
