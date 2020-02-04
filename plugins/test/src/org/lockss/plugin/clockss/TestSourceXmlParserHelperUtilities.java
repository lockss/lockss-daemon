package org.lockss.plugin.clockss;

import org.lockss.test.LockssTestCase;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.StringReader;
import org.xml.sax.InputSource;

public class TestSourceXmlParserHelperUtilities extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestSourceXmlParserHelperUtilities.class);

    public void testGoodPubDate() throws Exception {

        String pubDate = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "   <PubDate>\n" +
                "       <Year>2019</Year>\n" +
                "       <Month>11</Month>\n" +
                "       <Day>15</Day>\n" +
                "   </PubDate>";

        Document doc = convertStringToXMLDocument(pubDate);
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        String xpathPubDateExpression = "/PubDate";

        try
        {
            XPathExpression expr = xpath.compile(xpathPubDateExpression);
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            String nodeValue = SourceXmlParserHelperUtilities.getPubDateFromPubDateXpathNodeValue(nodes.item(0));
            assertEquals(nodeValue, "11-15-2019");
            assertNotEquals(nodeValue, "01-01-2019");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

    }

    public void testBadPubDate() throws Exception {

        String pubDate = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "   <PubDate>\n" +
                "       <Year>2019</Year>\n" +
                "       <Month>11</Month>\n" +
                "   </PubDate>";

        Document doc = convertStringToXMLDocument(pubDate);
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        String xpathPubDateExpression = "/PubDate";

        try
        {
            XPathExpression expr = xpath.compile(xpathPubDateExpression);
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            String nodeValue = SourceXmlParserHelperUtilities.getPubDateFromPubDateXpathNodeValue(nodes.item(0));
            assertNotEquals(nodeValue, "11-15-2019");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

    }

    public void testGoodAuthorName() throws Exception {

        String author = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "      <Author ValidYN=\"Y\">\n" +
                "             <LastName>Artzi</LastName>\n" +
                "             <ForeName>Ofir</ForeName>\n" +
                "             <Initials>O</Initials>\n" +
                "             <AffiliationInfo>\n" +
                "                 <Affiliation>Department of Dermatology, Tel Aviv Medical Center, Tel Aviv, 6423906, Israel. benofir@gmail.com.</Affiliation>\n" +
                "             </AffiliationInfo>\n" +
                "         </Author>";

        Document doc = convertStringToXMLDocument(author);
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        String xpathPubDateExpression = "/Author";

        try
        {
            XPathExpression expr = xpath.compile(xpathPubDateExpression);
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            String nodeValue = SourceXmlParserHelperUtilities.getAuthorNameFromAuthorNameXpathNodeValue(nodes.item(0));
            log.info("nodeValue = " + nodeValue);
            assertEquals(nodeValue, "Ofir Artzi");
            assertNotEquals(nodeValue, "Artzi Ofir");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

    }

    public void testGoodAuthorName2() throws Exception {

        String author = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "      <Author ValidYN=\"Y\">\n" +
                "             <surName>Artzi</surName>\n" +
                "             <FirstName>Ofir</FirstName>\n" +
                "             <Initials>O</Initials>\n" +
                "             <AffiliationInfo>\n" +
                "                 <Affiliation>Department of Dermatology, Tel Aviv Medical Center, Tel Aviv, 6423906, Israel. benofir@gmail.com.</Affiliation>\n" +
                "             </AffiliationInfo>\n" +
                "         </Author>";

        Document doc = convertStringToXMLDocument(author);
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        String xpathPubDateExpression = "/Author";

        try
        {
            XPathExpression expr = xpath.compile(xpathPubDateExpression);
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            String nodeValue = SourceXmlParserHelperUtilities.getAuthorNameFromAuthorNameXpathNodeValue(nodes.item(0));
            log.info("nodeValue = " + nodeValue);
            assertEquals(nodeValue, "Ofir Artzi");
            assertNotEquals(nodeValue, "Artzi Ofir");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

    }

    private static Document convertStringToXMLDocument(String xmlString)
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try
        {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
            return doc;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
