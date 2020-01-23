package org.lockss.plugin.clockss.actadermatovenereologica;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//https://www.nlm.nih.gov/bsd/licensee/elements_descriptions.html
public class ActaDermatoVenereologicaNLMXmlHelper implements SourceXmlSchemaHelper {

    private static final Logger log = Logger.getLogger(ActaDermatoVenereologicaNLMXmlHelper.class);

    public static String PAGINATION = "/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/Pagination/MedlinePgn";

    /*
    <AuthorList CompleteYN="Y">
        <Author ValidYN="Y">
            <LastName>Artzi</LastName>
            <ForeName>Ofir</ForeName>
            <Initials>O</Initials>
            <AffiliationInfo>
                <Affiliation>Department of Dermatology, Tel Aviv Medical Center, Tel Aviv, 6423906, Israel. benofir@gmail.com.</Affiliation>
            </AffiliationInfo>
        </Author>
        <Author ValidYN="Y">
            <LastName>Sprecher</LastName>
            <ForeName>Eli</ForeName>
            <Initials>E</Initials>
        </Author>
        <Author ValidYN="Y">
            <LastName>Koren</LastName>
            <ForeName>Amir</ForeName>
            <Initials>A</Initials>
        </Author>
        <Author ValidYN="Y">
            <LastName>Mehrabi</LastName>
            <ForeName>Joseph N</ForeName>
            <Initials>JN</Initials>
        </Author>
        <Author ValidYN="Y">
            <LastName>Katz</LastName>
            <ForeName>Oren</ForeName>
            <Initials>O</Initials>
        </Author>
        <Author ValidYN="Y">
            <LastName>Hilerowich</LastName>
            <ForeName>Yuval</ForeName>
            <Initials>Y</Initials>
        </Author>
    </AuthorList>
     */
    private final static XmlDomMetadataExtractor.NodeValue AUTHOR_VALUE = new XmlDomMetadataExtractor.NodeValue() {
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
                if ("LastName".equals(partName)) {
                    surname  = partNode.getTextContent();
                } else if ("ForeName".equals(partName)) {
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

    /*
    <Pagination>
        <MedlinePgn>53-57</MedlinePgn>
    </Pagination>
     */

    private static final String PAGINATION_PATTERN_STRING = "(\\d+)\\s*(-)?\\s*(\\d+)";
    private static Pattern PAGINATION_PATTER_PATTERN =  Pattern.compile("^\\s*" + PAGINATION_PATTERN_STRING, Pattern.CASE_INSENSITIVE);

    static private final XmlDomMetadataExtractor.NodeValue PAGE_VALUE = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {
            log.debug3("getValue of Acta DerMato Venereologica pagination");
            String paginationVal = node.getTextContent();
            Matcher iMat = PAGINATION_PATTER_PATTERN .matcher(paginationVal);
            if(!iMat.find()){ //use find not match to ignore trailing stuff
                log.debug3("Acta DerMato Venereologica pagination no match");
                return null;
            }
            return iMat.group(1) + "-" + iMat.group(3);
        }
    };

    /*
    <PubDate>
        <Year>2019</Year>
        <Month>01</Month>
        <Day>01</Day>
    </PubDate>
     */
    private final static XmlDomMetadataExtractor.NodeValue PUBDATE_VALUE = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {
            NodeList nameChildren = node.getChildNodes();
            if (nameChildren == null) return null;

            String year = null;
            String month = null;
            String day = null;

            if (nameChildren == null) return null;
            for (int p = 0; p < nameChildren.getLength(); p++) {
                Node partNode = nameChildren.item(p);
                String partName = partNode.getNodeName();
                if ("Year".equals(partName)) {
                    year  = partNode.getTextContent();
                } else if ("Month".equals(partName)) {
                    month = partNode.getTextContent();
                } else if ("Day".equals(partName)) {
                    day = partNode.getTextContent();
                }
            }
            StringBuilder valbuilder = new StringBuilder();
            //isBlank checks for null, whitespace and empty
            if (!StringUtils.isBlank(month)) {
                valbuilder.append(month);
                if (!StringUtils.isBlank(day)) {
                    valbuilder.append("-" + day);
                }
                if (!StringUtils.isBlank(year)) {
                    valbuilder.append("-" + year);
                }
                return valbuilder.toString();
            }
            return null;
        }
    };

    //top of file
    private static final String top = "/PubmedArticleSet/PubmedArticle";

    // The following are all relative to the article node ("record")
    // from the immediately preceeding sibling -
    private static String art_title = top + "/MedlineCitation/Article/ArticleTitle";
    private static String art_contrib = top + "/MedlineCitation/Article/AuthorList/Author";
    private static String art_doi = top + "/MedlineCitation/Article/ELocationID";
    private static String pub_title = top + "/MedlineCitation/Article/Journal/Title";
    private static String art_publication_date = top + "/MedlineCitation/Article/Journal/JournalIssue/PubDate";
    private static String art_eissn = top + "/MedlineCitation/Article/Journal/ISSN";
    //private static String art_start_page = top  + "/MedlineCitation/Article/Pagination/MedlinePgn";
    private static String art_end_page = top  + "/MedlineCitation/Article/Pagination/MedlinePgn";

    /*
     *  The following 3 variables are needed to use the XPathXmlMetadataParser
     */

    /* 1.  MAP associating xpath & value type definition or evaluator */
    static private final Map<String, XmlDomMetadataExtractor.XPathValue>
            articleMap = new HashMap<String, XmlDomMetadataExtractor.XPathValue>();

    static {
        articleMap.put(art_title,  XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_contrib, AUTHOR_VALUE);
        articleMap.put(PAGINATION, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_publication_date, PUBDATE_VALUE);
        articleMap.put(art_eissn, XmlDomMetadataExtractor.TEXT_VALUE);
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
        cookMap.put(art_eissn, MetadataField.FIELD_EISSN);
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