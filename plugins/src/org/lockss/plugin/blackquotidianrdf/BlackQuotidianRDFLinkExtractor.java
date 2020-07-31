package org.lockss.plugin.blackquotidianrdf;

import org.apache.commons.lang3.StringUtils;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.XPathUtil;
import org.lockss.util.urlconn.CacheException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BlackQuotidianRDFLinkExtractor implements LinkExtractor {

    protected static final XPathExpression DOIDataSets;

    private static final Logger log = Logger.getLogger(BlackQuotidianRDFLinkExtractor.class);

    /*
        <?xml version="1.0" encoding="UTF-8"?>
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
          xmlns:scalar="http://scalar.usc.edu/2012/01/scalar-ns#"
          xmlns:art="http://simile.mit.edu/2003/10/ontologies/artstor#"
          xmlns:prov="http://www.w3.org/ns/prov#"
          xmlns:dcterms="http://purl.org/dc/terms/"
          xmlns:ov="http://open.vocab.org/terms/"
          xmlns:sioc="http://rdfs.org/sioc/ns#"
          xmlns:iptc="http://ns.exiftool.ca/IPTC/IPTC/1.0/"
          xmlns:dc="http://purl.org/dc/elements/1.1/">

          <rdf:Description rdf:about="http://blackquotidian.supdigital.org/bq/1966-ncaa-basketball-championship---texas-western-vs-kentucky">
            <rdf:type rdf:resource="http://scalar.usc.edu/2012/01/scalar-ns#Media"/>
            <scalar:isLive>1</scalar:isLive>
            <art:thumbnail rdf:resource="http://www.criticalcommons.org/Members/mattdelmont/clips/1966-ncaa-basketball-championship-texas-western-vs/thumbnailImage_thumb"/>
            <prov:wasAttributedTo rdf:resource="http://blackquotidian.supdigital.org/bq/users/1"/>
            <dcterms:created>2019-03-12T23:56:18+00:00</dcterms:created>
            <scalar:urn rdf:resource="urn:scalar:content:2"/>
            <scalar:version rdf:resource="http://blackquotidian.supdigital.org/bq/1966-ncaa-basketball-championship---texas-western-vs-kentucky.1"/>
            <dcterms:hasVersion rdf:resource="http://blackquotidian.supdigital.org/bq/1966-ncaa-basketball-championship---texas-western-vs-kentucky.1"/>
            <scalar:citation>method=instancesof/content;methodNumNodes=1932;</scalar:citation>
          </rdf:Description>
         </rdf:RDF>
     */
    static {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            DOIDataSets = xpath.compile("//doi/text()");
        }
        catch (XPathExpressionException xpee) {
            throw new ExceptionInInitializerError(xpee);
        }
    }

    @Override
    public void extractUrls(ArchivalUnit au,
                            InputStream in,
                            String encoding,
                            String srcUrl,
                            Callback cb)
            throws IOException {
        String loggerUrl = loggerUrl(srcUrl);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        InputSource inputSource = new InputSource(new InputStreamReader(in, encoding));
        Document doc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(inputSource);
        }
        catch (ParserConfigurationException pce) {
            throw new CacheException.UnknownExceptionException("Error configuring parser for " + loggerUrl, pce);
        }
        catch (SAXException se) {
            throw new CacheException.UnknownExceptionException("Error while parsing " + loggerUrl, se);
        }

        NodeList dois = null;
        try {

            dois = XPathUtil.evaluateNodeSet(DOIDataSets, doc);
            int doi_count = dois.getLength();
            log.debug3("Total DOI count : " + doi_count);
        }
        catch (XPathExpressionException xpee) {
            throw new CacheException.UnknownExceptionException("Error while parsing results for " + loggerUrl, xpee);
        }

        if (dois.getLength() == 0) {
            throw new CacheException.UnknownExceptionException("Internal error parsing results for " + loggerUrl);
        }

        for (int i = 0 ; i < dois.getLength() ; ++i) {
            log.debug3("Each DOI value " + dois.item(i).getNodeValue());
            processDoi(cb, dois.item(i).getNodeValue());
        }
    }

    public void processDoi(Callback cb, String doi) {
        if (doi != null && !StringUtils.isEmpty(doi)) {
            cb.foundLink(doi);
        }
    }

    public static final String loggerUrl(String srcUrl) {
        return srcUrl;
    }
}
