package org.lockss.plugin.gigascience;

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

public class GigaScienceAPIHelper implements SourceXmlSchemaHelper {

    private static final Logger log = Logger.getLogger(GigaScienceAPIHelper.class);

    /*
    <authors>
        <author>
            <firstname>Guangqi</firstname>
            <middlename/>
            <surname>Gao</surname>
            <orcid/>
        </author>
        <author>
            <firstname>Meng</firstname>
            <middlename/>
            <surname>Xu</surname>
            <orcid>0000-0003-4747-7319</orcid>
        </author>
     </authors>
     */
    private final static NodeValue AUTHOR_VALUE = new NodeValue() {
        @Override
        public String getValue(Node node) {
            NodeList nameChildren = node.getChildNodes();
            if (nameChildren == null) return null;

            String surname = null;
            String firstname = null;

            if (nameChildren == null) return null;
            for (int p = 0; p < nameChildren.getLength(); p++) {
                Node partNode = nameChildren.item(p);
                String partName = partNode.getNodeName();
                if ("surname".equals(partName)) {
                    surname  = partNode.getTextContent();
                } else if ("firstname".equals(partName)) {
                    firstname = partNode.getTextContent();
                }
            }
            StringBuilder valbuilder = new StringBuilder();
            //isBlank checks for null, whitespace and empty
            if (!StringUtils.isBlank(firstname)) {
                valbuilder.append(firstname);
                if (!StringUtils.isBlank(surname)) {
                    valbuilder.append(" " + surname);
                }
                return valbuilder.toString();
            }
            return null;
        }
    };

    //top of file
    private static final String top = "/gigadb_entry/dataset";

    // The following are all relative to the article node ("record")
    // from the immediately preceeding sibling -
    private static String art_title = top + "/title";
    private static String art_contrib = top + "/authors/author";
    private static String art_doi = top + "/links/manuscript_links/manuscript_link/manuscript_DOI";
    private static String pub_title = top + "/publication/publisher/@name";
    private static String art_publication_date = top + "/publication/@date";

    /*
     *  The following 3 variables are needed to use the XPathXmlMetadataParser
     */

    /* 1.  MAP associating xpath & value type definition or evaluator */
    static private final Map<String, XPathValue>
            articleMap = new HashMap<String, XPathValue>();

    static {
        articleMap.put(art_title,  XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_contrib, AUTHOR_VALUE);
        articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_publication_date, XmlDomMetadataExtractor.TEXT_VALUE);
    }

    /* 2.  Top level per-article node - optional div level*/
    static private final String articleNode = top;

    /* 3. Global metadata is the publisher - work around if it gets troublesome */
    static private final Map<String, XmlDomMetadataExtractor.XPathValue>
            globalMap = new HashMap<String, XmlDomMetadataExtractor.XPathValue>();
    static {
        globalMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE);
    }

    /*
     * The emitter will need a map to know how to cook raw values
     */
    private static final String AUTHOR_SPLIT_CH = ",";
    protected static final MultiValueMap cookMap = new MultiValueMap();

    static {
        cookMap.put(art_contrib,new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(AUTHOR_SPLIT_CH)));
        cookMap.put(art_doi, MetadataField.FIELD_DOI);
        cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put(art_publication_date, MetadataField.FIELD_DATE);
    }

    /**
     * publisher comes from a global node
     */
    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getGlobalMetaMap() {
        return globalMap;
    }

    /**
     * return  article paths representing metadata of interest
     */
    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
        return articleMap;
    }

    /**
     * Return the article node path
     */
    @Override
    public String getArticleNode() {
        return articleNode;
    }

    /**
     * Return a map to translate raw values to cooked values
     */
    @Override
    public MultiValueMap getCookMap() {
        return cookMap;
    }

    /**
     *
     */
    @Override
    public String getDeDuplicationXPathKey() {
        return null;
    }

    /**
     * Return the path for product form so when multiple records for the same
     * item are combined, the product forms are combined together
     */

    @Override
    public String getConsolidationXPathKey() {
        return null;
    }

    /**
     * using filenamePrefix (see above)
     */
    @Override
    public String getFilenameXPathKey() {
        return null;
    }
}
