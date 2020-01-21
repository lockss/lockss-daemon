package org.lockss.plugin.gigascience;

import org.apache.commons.io.FileUtils;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.LinkExtractor;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class TestGigaScienceDoiLinkExtractor extends LockssTestCase {

    private static String getXmlFileContent(String fname) {
        String xmlContent = "";
        try {
            String currentDirectory = System.getProperty("user.dir");
            String pathname = currentDirectory +
                    "/plugins/test/src/org/lockss/plugin/gigascience/" + fname;
            xmlContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xmlContent;
    }


    public void testDOIAPIXPathAlone() throws Exception {

        String fname = "dois_api.xml";

        String fileName= System.getProperty("user.dir") +
                "/plugins/test/src/org/lockss/plugin/gigascience/" + fname;
        Document document = getDocument(fileName);

        String xpathExpression = "";

        xpathExpression = "//doi/text()";

        evaluateXPath(document, xpathExpression);
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
            e.printStackTrace();
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

    private Document getDocument(String fileName) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(fileName);
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
