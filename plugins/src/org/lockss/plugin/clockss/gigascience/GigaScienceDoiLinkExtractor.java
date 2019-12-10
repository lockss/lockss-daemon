package org.lockss.plugin.clockss.gigascience;

import org.apache.commons.lang3.StringUtils;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.XPathUtil;
import org.lockss.util.urlconn.CacheException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
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

public class GigaScienceDoiLinkExtractor implements LinkExtractor {

    protected static final XPathExpression DOI;


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
            DOI = xpath.compile("doi");
        }
        catch (XPathExpressionException xpee) {
            throw new ExceptionInInitializerError(xpee);
        }
    }


    /**
     * <p>
     * A flag indicating whether work on this query is done.
     * </p>
     *
     * @since 1.67.5
     */
    protected boolean done;


    public GigaScienceDoiLinkExtractor() {
        this.done = false;
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
            dois = XPathUtil.evaluateNodeSet(DOI, doc);
        }
        catch (XPathExpressionException xpee) {
            throw new CacheException.UnknownExceptionException("Error while parsing results for " + loggerUrl, xpee);
        }

        if (dois.getLength() == 0) {
            throw new CacheException.UnknownExceptionException("Internal error parsing results for " + loggerUrl);
        }

        Node doi = null;
        String doiStr = null;
        for (int i = 0 ; i < dois.getLength() ; ++i) {
            doi = dois.item(i);
            try {
                doiStr = XPathUtil.evaluateString(DOI, doi);
            }
            catch (XPathExpressionException xpee) {
                throw new CacheException.UnknownExceptionException(
                        String.format("Error while parsing stanza for %s in %s",
                                doiStr == null ? "first DOI" : "DOI immediately after " + doiStr,
                                loggerUrl),
                        xpee);
            }
        }
    }

    public static final String loggerUrl(String srcUrl) {
        return srcUrl;
    }

    public boolean isDone() {
        return done;
    }

}
