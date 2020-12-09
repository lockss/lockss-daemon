package org.lockss.plugin.associationforcomputingmachinery;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper function parses BITS format xml file.
 * BITS is a superset of BITS, which handles books
 */

public class ACMBitsPublishingSchemaHelper implements SourceXmlSchemaHelper {
    private static final Logger log = Logger.getLogger(ACMBitsPublishingSchemaHelper.class);

    private static final String AUTHOR_SEPARATOR = ",";

    /*
     * See official example at: https://jats.nlm.nih.gov/extensions/bits/tag-library/2.0/element/contrib-group.html
     * <contrib-group>
           <contrib contrib-type="author" id="artseq-00001">
               <contrib-id contrib-id-type="person_id" authenticated="false">P5466562</contrib-id>
               <name name-style="western">
                   <surname>Yoon</surname>
                   <given-names>Sang Ho</given-names>
               </name>
            <aff>School of Mechanical Engineering, Purdue University, West Lafayette, IN, USA</aff>
            <email>yoon87@purdue.edu</email>
            <role>Author</role>
           </contrib>
           <contrib contrib-type="author" id="artseq-00002">
               <contrib-id contrib-id-type="person_id" authenticated="false">P5466563</contrib-id>
               <name name-style="western">
                   <surname>Huo</surname>
                   <given-names>Ke</given-names>
               </name>
            <aff>School of Mechanical Engineering, Purdue University, West Lafayette, IN, USA</aff>
            <email>khuo@purdue.edu</email>
            <role>Author</role>
           </contrib>
           <contrib contrib-type="author" id="artseq-00003">
               <contrib-id contrib-id-type="person_id" authenticated="false">P5466564</contrib-id>
               <name name-style="western">
                   <surname>Ramani</surname>
                   <given-names>Karthik</given-names>
               </name>
            <aff>School of Mechanical Engineering, Purdue University, West Lafayette, IN, USA</aff>
            <email>ramani@purdue.edu</email>
            <role>Author</role>
           </contrib>
        </contrib-group>
     */
    static private final NodeValue BITS_AUTHOR_VALUE = new NodeValue() {
        @Override
        public String getValue(Node node) {

            log.debug3("getValue of BITS author");
            NodeList elementChildren = node.getChildNodes();
            // only accept no children if this is a "name" node
            if (elementChildren == null &&
                    !("name".equals(node.getNodeName()))) return null;

            String tsurname = null;
            String tgiven = null;
            String tprefix = null;

            if (elementChildren != null) {
                // perhaps pick up iso attr if it's available
                // look at each child
                for (int j = 0; j < elementChildren.getLength(); j++) {
                    Node checkNode = elementChildren.item(j);
                    String nodeName = checkNode.getNodeName();
                    if ("surname".equals(nodeName)) {
                        tsurname = checkNode.getTextContent();
                    } else if ("given-names".equals(nodeName) ) {
                        tgiven = checkNode.getTextContent();
                    } else if ("prefix".equals(nodeName)) {
                        tprefix = checkNode.getTextContent();
                    }
                }
            } else {
                // we only fall here if the node is a string-name
                // no children - just get the plain text value
                tsurname = node.getTextContent();
            }

            // where to put the prefix?
            StringBuilder valbuilder = new StringBuilder();
            //isBlank checks for null, empty & whitespace only
            if (!StringUtils.isBlank(tsurname)) {
                valbuilder.append(tsurname);
                if (!StringUtils.isBlank(tgiven)) {
                    valbuilder.append(AUTHOR_SEPARATOR + " " + tgiven);
                }
            } else {
                log.debug3("no author found");
                return null;
            }
            log.debug3("author found: " + valbuilder.toString());
            return valbuilder.toString();
        }
    };

    static private final NodeValue BITS_DATE_VALUE = new NodeValue() {
        @Override
        public String getValue(Node node) {

            log.debug3("getValue of BITS publishing date");
            NodeList elementChildren = node.getChildNodes();
            if (elementChildren == null) return null;

            // perhaps pick up iso attr if it's available
            String tyear = null;
            String tday = null;
            String tmonth = null;
            // look at each child of the TitleElement for information
            for (int j = 0; j < elementChildren.getLength(); j++) {
                Node checkNode = elementChildren.item(j);
                String nodeName = checkNode.getNodeName();
                if ("day".equals(nodeName)) {
                    tday = checkNode.getTextContent();
                } else if ("month".equals(nodeName) ) {
                    tmonth = checkNode.getTextContent();
                } else if ("year".equals(nodeName)) {
                    tyear = checkNode.getTextContent();
                }
            }

            StringBuilder valbuilder = new StringBuilder();
            if (tyear != null) {
                valbuilder.append(tyear);
                if (tday != null && tmonth != null) {
                    valbuilder.append("-" + tmonth + "-" + tday);
                }
            } else {
                log.debug3("no date found");
                return null;
            }
            log.debug3("date found: " + valbuilder.toString());
            return valbuilder.toString();
        }
    };

    /*
     *  BITS specific XPATH key definitions that we care about
     */
    private static String BITS_book = "/book-part-wrapper";
    private static String BITS_publisher= "/book-part-wrapper/collection-meta/title-group/title";
    private static String BITS_doi = "/book-part-wrapper/book-part/book-part-meta//book-part-id[@book-part-id-type=\"doi\"]";
    private static String BITS_book_title =  "/book-part-wrapper/book-meta/book-title-group/book-title";
    private static String BITS_contrib = "/book-part-wrapper/book-part/book-part-meta/contrib-group/contrib/name";
    private static String BITS_fpage = "/book-part-wrapper/book-part/book-part-meta/fpage";
    private static String BITS_lpage = "/book-part-wrapper/book-part/book-part-meta/lpage";
    public static String BITS_copydate = "/book-part-wrapper/book-part/book-part-meta/permissions/copyright-year";
    public static String BITS_date =  "/book-part-wrapper/book-part/book-part-meta/pub-date[@date-type=\"publication\"]";

    
    /*
     *  The following 3 variables are needed to construct the XPathXmlMetadataParser
     */

    /* 1.  MAP associating xpath with value type with evaluator */
    static private final Map<String, XPathValue> BITS_articleMap =
            new HashMap<String, XmlDomMetadataExtractor.XPathValue>();
    static {
        BITS_articleMap.put(BITS_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_book_title, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_doi, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_fpage, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_lpage, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_copydate, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_contrib, BITS_AUTHOR_VALUE);
        BITS_articleMap.put(BITS_date, BITS_DATE_VALUE);
    }

    /* 2. Each item (book) has its own XML file */
    static private final String BITS_articleNode = BITS_book;

    /* 3. in BITS there is no global information because one file/article */
    static private final Map<String, XPathValue> BITS_globalMap = null;

    private static final MultiValueMap cookMap = new MultiValueMap();
    static {
        cookMap.put(BITS_publisher, MetadataField.FIELD_PUBLISHER);
        cookMap.put(BITS_book_title, MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put(BITS_doi, MetadataField.FIELD_DOI);
        cookMap.put(BITS_fpage, MetadataField.FIELD_START_PAGE);
        cookMap.put(BITS_lpage, MetadataField.FIELD_END_PAGE);
        cookMap.put(BITS_contrib, MetadataField.FIELD_AUTHOR);
        cookMap.put(BITS_copydate, MetadataField.FIELD_DATE);
        cookMap.put(BITS_date, MetadataField.FIELD_DATE);
    }

    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getGlobalMetaMap() {
        return BITS_globalMap;
    }

    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
        return BITS_articleMap;
    }

    @Override
    public String getArticleNode() {
        return BITS_articleNode;
    }

    @Override
    public MultiValueMap getCookMap() {
        return cookMap;
    }

    @Override
    public String getDeDuplicationXPathKey() {
        return null;
    }

    @Override
    public String getConsolidationXPathKey() {
        return null;
    }

    @Override
    public String getFilenameXPathKey() {
        return null;
    }
}
