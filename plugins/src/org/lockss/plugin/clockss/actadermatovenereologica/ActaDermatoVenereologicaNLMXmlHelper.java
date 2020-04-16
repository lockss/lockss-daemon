package org.lockss.plugin.clockss.actadermatovenereologica;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlParserHelperUtilities;
import org.lockss.util.Logger;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

//https://www.nlm.nih.gov/bsd/licensee/elements_descriptions.html
public class ActaDermatoVenereologicaNLMXmlHelper implements SourceXmlSchemaHelper {

    private static final Logger log = Logger.getLogger(ActaDermatoVenereologicaNLMXmlHelper.class);

    public static String PAGINATION = "/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/Pagination/MedlinePgn";

    //top of file
    private static final String top = "/PubmedArticleSet/PubmedArticle";

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
    </AuthorList>
     */
    private static String art_contrib = top + "/MedlineCitation/Article/AuthorList/Author";

    private final static XmlDomMetadataExtractor.NodeValue AUTHOR_VALUE = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {
            return SourceXmlParserHelperUtilities.getAuthorNameFromAuthorNameXpathNodeValue(node);
        }
    };

    /*
    <PubDate>
        <Year>2019</Year>
        <Month>01</Month>
        <Day>01</Day>
    </PubDate>
     */
    private static String art_publication_date = top + "/MedlineCitation/Article/Journal/JournalIssue/PubDate";

    private final static XmlDomMetadataExtractor.NodeValue PUBDATE_VALUE = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {
            return SourceXmlParserHelperUtilities.getPubDateFromPubDateXpathNodeValue(node);
        }
    };

    // The following are all relative to the article node ("record")
    // from the immediately preceeding sibling -
    private static String art_title = top + "/MedlineCitation/Article/ArticleTitle";
    private static String art_doi = top + "/MedlineCitation/Article/ELocationID[@EIdType=\"doi\"]";
    private static String pub_title = top + "/MedlineCitation/Article/Journal/Title";
    private static String art_eissn = top + "/MedlineCitation/Article/Journal/ISSN[@IssnType=\"Electronic\"]";
    private static String art_issn = top + "/MedlineCitation/MedlineJournalInfo/ISSNLinking";

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
        articleMap.put(art_issn, XmlDomMetadataExtractor.TEXT_VALUE);
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
        cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put(art_contrib,new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(AUTHOR_SPLIT_CH)));
        cookMap.put(art_doi, MetadataField.FIELD_DOI);
        cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put(art_publication_date, MetadataField.FIELD_DATE);
        cookMap.put(art_eissn, MetadataField.FIELD_EISSN);
        cookMap.put(art_issn, MetadataField.FIELD_ISSN);
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