package org.lockss.plugin.gigascience;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.LinkExtractor;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.lockss.util.IOUtil;
import org.lockss.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class TestGigaScienceDoiLinkExtractor extends LockssTestCase {

    private String getXmlFileContent(String fname) {
        String xmlContent = "";

        try (InputStream file_input = getResourceAsStream(fname)) {
            xmlContent = StringUtil.fromInputStream(file_input);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return xmlContent;
    }


    public void testDOIAPIXPathAlone() throws Exception {

        String fname = "dois_api.xml";

        try (InputStream ins = getResourceAsStream(fname)) {
          Document document = getDocument(ins);

          String xpathExpression = "";

          xpathExpression = "//doi/text()";

          evaluateXPath(document, xpathExpression);
        }
    }

    private void  evaluateXPath(Document document, String xpathExpression) throws Exception
    {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        List<String> values = new ArrayList<>();
        int dois_count = 0;

        try
        {
            XPathExpression expr = xpath.compile(xpathExpression);

            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            dois_count = nodes.getLength();
            assertNotEquals(nodes.getLength(), 0);

            for (int i = 0; i < dois_count ; i++) {
                String doi = nodes.item(i).getNodeValue();
                if (doi != null) {
                    values.add(nodes.item(i).getNodeValue());
                }
            }

        } catch (XPathExpressionException e) {
            log.error(e.getMessage(), e);
        }

        assertEquals(values.size(), dois_count);
    }

    public void testGoodInput() throws Exception {
        GigaScienceDoiLinkExtractor gigaDoi = new GigaScienceDoiLinkExtractor();
        String fname = "dois_api.xml";
        String journalXml = getXmlFileContent(fname);
        List<String> out = doExtractUrls(gigaDoi, journalXml);
        assertEquals(out.size(), 23);
    }

    private Document getDocument(InputStream ins) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(ins);
        return doc;
    }

    protected List<String> doExtractUrls(LinkExtractor le,
                                         String input)
            throws Exception {
        MockArchivalUnit mau = new MockArchivalUnit();
        mau.setConfiguration(ConfigurationUtil.fromArgs(
                ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/"));
        final List<String> out = new ArrayList<String>();
        le.extractUrls(mau,
                new StringInputStream(input),
                "UTF-8",
                "http://api.example.com/meta/v1/pam?q=issn:1234-567X%20volume:123&p=100&s=1",
                new LinkExtractor.Callback() {
                    @Override public void foundLink(String url) {
                        out.add(url);
                    }
                });
        return out;
    }
}
