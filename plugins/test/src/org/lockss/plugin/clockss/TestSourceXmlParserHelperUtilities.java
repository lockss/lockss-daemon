/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

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
            log.error(e.getMessage(), e);
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
            log.error(e.getMessage(), e);
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
            log.error(e.getMessage(), e);
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
            log.error(e.getMessage(), e);
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
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
