package org.lockss.plugin.gigascience;

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
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GigaScienceDoiLinkExtractor implements LinkExtractor {

    protected static final XPathExpression DOIDataSets;

    private static final Logger log = Logger.getLogger(GigaScienceDoiLinkExtractor.class);

    /*
     * <?xml version="1.0" encoding="UTF-8"?>
     * <datasets>
     * <doi prefix="10.5524">100541</doi>
     * <doi prefix="10.5524">100546</doi>
     * </datasets>
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
