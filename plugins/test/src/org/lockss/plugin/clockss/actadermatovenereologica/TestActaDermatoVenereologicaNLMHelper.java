package org.lockss.plugin.clockss.actadermatovenereologica;

import org.apache.commons.lang.StringUtils;
import org.lockss.test.LockssTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestActaDermatoVenereologicaNLMHelper extends LockssTestCase {

    public void testAPIXmlXPathAlone() throws Exception {

        String fname = "sample_nlm.xml";

        Document document = getDocumentFromResource(fname);

        String xpathArticleTitleExpression = "/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/ArticleTitle";
        String xpathPaginationExpression = "/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/Pagination/MedlinePgn";
        String xpathDoiExpression = "/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/ELocationID[@EIdType=\"doi\"]";
        String xpathAuthorExpression = "/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/AuthorList/Author";
        String xpathPubTitleExpression = "/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/Journal/Title";
        String xpathPubDateExpression = "/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/Journal/JournalIssue/PubDate";
        String xpathEISSNExpression = "/PubmedArticleSet/PubmedArticle/MedlineCitation/Article/Journal/ISSN[@IssnType=\"Electronic\"]";

        evaluateXPath(document, xpathArticleTitleExpression);
        evaluatePaginationXPath(document, xpathPaginationExpression);
        evaluateXPath(document, xpathDoiExpression);
        evaluateXPath(document, xpathPubTitleExpression );
        evaluateXPath(document, xpathPubDateExpression );
        evaluateXPath(document, xpathEISSNExpression );

        evaluatePaginationXPath(document, xpathPaginationExpression);
        evaluateAuthoNameXPath(document, xpathAuthorExpression );

    }

    private void  evaluateXPath(Document document, String xpathExpression) throws Exception
    {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        List<String> values = new ArrayList<>();
        int count = 0;

        try
        {
            XPathExpression expr = xpath.compile(xpathExpression);

            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            count = nodes.getLength();
            assertNotEquals(nodes.getLength(), 0);

            for (int i = 0; i < count ; i++) {
                String value = nodes.item(i).getTextContent();
                assertNotNull(value);
            }

        } catch (XPathExpressionException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void  evaluateAuthoNameXPath(Document document, String xpathExpression) throws Exception
    {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        List<String> values = new ArrayList<>();
        int count = 0;

        try
        {
            XPathExpression expr = xpath.compile(xpathExpression);

            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            count = nodes.getLength();
            assertNotEquals(nodes.getLength(), 0);

            for (int i = 0; i < count ; i++) {
                NodeList nameChildren = nodes.item(i).getChildNodes();

                String surname = null;
                String firstname = null;

                for (int p = 0; p < nameChildren.getLength(); p++) {
                    Node partNode = nameChildren.item(p);
                    String partName = partNode.getNodeName();

                    if ("LastName".equals(partName)) {
                        surname = partNode.getTextContent();
                        log.info("surname is " + surname);
                    }  else if ("ForeName".equals(partName)) {
                        firstname = partNode.getTextContent();
                        log.info("firstname is " + firstname);
                    }
                }
                StringBuilder valbuilder = new StringBuilder();
                if (!StringUtils.isBlank(firstname)) {
                    valbuilder.append(firstname);
                    if (!StringUtils.isBlank(surname)) {
                        valbuilder.append("  " + surname);
                    }
                    log.info("author name is " + valbuilder.toString());
                }
                assertNotNull(valbuilder.toString());
            }

        } catch (XPathExpressionException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void  evaluatePaginationXPath(Document document, String xpathExpression) throws Exception
    {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        List<String> values = new ArrayList<>();
        int count = 0;

        String PAGINATION_PATTERN_STRING = "(\\d+)\\s*(-)?\\s*(\\d+)";
        Pattern PAGINATION_PATTER_PATTERN =  Pattern.compile("^\\s*" + PAGINATION_PATTERN_STRING, Pattern.CASE_INSENSITIVE);


        try
        {
            XPathExpression expr = xpath.compile(xpathExpression);

            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            count = nodes.getLength();
            assertNotEquals(nodes.getLength(), 0);

            log.info("Expression is " + xpathExpression + ", count ======  " + count);

            for (int i = 0; i < count ; i++) {
                String value = nodes.item(i).getTextContent();
                Matcher iMat = PAGINATION_PATTER_PATTERN .matcher(value);
                if(!iMat.find()){ //use find not match to ignore trailing stuff
                    log.info("Acta DerMato Venereologica pagination no match");
                } else {
                    log.info("start_page = " + iMat.group(1) + ", end_page = " + iMat.group(3));

                }
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


