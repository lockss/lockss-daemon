package org.lockss.plugin.clockss.aslha;

import org.apache.commons.lang.StringUtils;
import org.lockss.test.LockssTestCase;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.util.ArrayList;
import java.util.List;

public class TestAslhaSchemaHelper extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestAslhaSchemaHelper.class);

    public void testAPIXmlXPathAlone() throws Exception {

        String fname = "article_sample.xml";

        Document document = getDocumentFromResource(fname);

        String article_doi = "//*[local-name()=\"identifier\"]";
        String article_title = "//*[local-name()=\"title\"]";


        evaluateXPath(document, article_title);
        evaluateXPath(document, article_doi);

    }

    private void  evaluateXPath(Document document, String xpathExpression) throws Exception
    {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        int count = 0;

        try
        {
            XPathExpression expr = xpath.compile(xpathExpression);

            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            count = nodes.getLength();

            log.info("count === " + Integer.toString(count));

            for (int i = 0; i < count ; i++) {
                String value = nodes.item(i).getTextContent();
                log.info("value === " + value);
                assertNotNull(value);
            }

        } catch (XPathExpressionException e) {
            log.error(e.getMessage(), e);
        }
    }


    private Document getDocumentFromResource(String resource) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(getResourceAsStream(resource));
        return doc;
    }
}

