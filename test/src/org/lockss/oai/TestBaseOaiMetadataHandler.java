/*
 * $Id: TestBaseOaiMetadataHandler.java,v 1.3 2005-01-20 01:35:15 dcfok Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.oai;

import java.util.*;
import java.io.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.XmlDomBuilder.XmlDomException;
import org.w3c.dom.*;
import org.apache.xpath.NodeSet;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.DOMImplementation;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * Test cases for OaiHandler.java
 */
public class TestBaseOaiMetadataHandler extends LockssTestCase {
 
    protected static Logger logger = Logger.getLogger("TestBaseOaiMetadataHandler");

    private BaseOaiMetadataHandler baseOaiMetadataHandler;
    private String metadataPrefix = "abc";
    private String metadataNamespaceUrl = "http://www.example.com/abc/";
    private String urlContainerTagName = "foo";

    public void setUp(){
	baseOaiMetadataHandler = new BaseOaiMetadataHandler( metadataPrefix,
							     metadataNamespaceUrl,
							     urlContainerTagName);
    }

    public void testConstructor(){
	try {
	    baseOaiMetadataHandler = new BaseOaiMetadataHandler( (String) null, metadataNamespaceUrl, urlContainerTagName);
	    fail("BaseOaiMetadataHandler with null metadata prefix should throw");
	} catch (NullPointerException e) { }
	try {
	    baseOaiMetadataHandler = new BaseOaiMetadataHandler( metadataPrefix, (String) null, urlContainerTagName);
	    fail("BaseOaiMetadataHandler with null metadata namespace url should throw");
	} catch (NullPointerException e) { }
	try {
	    baseOaiMetadataHandler = new BaseOaiMetadataHandler( metadataPrefix, metadataNamespaceUrl, (String) null);
	    fail("BaseOaiMetadataHandler with null url container tag name should throw");
	} catch (NullPointerException e) { }   
    }
    
    public void testGetMetadataPrefix(){
	assertEquals(metadataPrefix, baseOaiMetadataHandler.getMetadataPrefix());
    }

    public void testGetMetadataNamespaceUrl(){
	assertEquals(metadataNamespaceUrl, baseOaiMetadataHandler.getMetadataNamespaceUrl());
    }

    public void testGetUrlContainerTagName(){
	assertEquals(urlContainerTagName, baseOaiMetadataHandler.getUrlContainerTagName());
    }

    public void testGetArticleUrls(){
	String prefix = "abc";
	String nsUrl = "http://www.younameit.com/";
	String tag = "foo";

	//setup a nodelist that contain different <metadata>..<tag>url</tag>..</metadata> records
	String url1 = "http://www.example.com/link1.html";
	String url2 = "http://www.example.com/link2.html";
	Set testUrls = SetUtil.set(url1, url2);
	NodeList metadataNodeList = createMockMetadataNodeList(testUrls, prefix, nsUrl, tag);

	//construct a bastOaiMetadataHandler
	BaseOaiMetadataHandler mdHandler = new BaseOaiMetadataHandler(prefix, nsUrl, tag);

	//run setupAndExecute
	mdHandler.setupAndExecute(metadataNodeList);

	//check by assertEquals of getArticleUrls()
	assertEquals(testUrls, mdHandler.getArticleUrls());
    }

    private NodeList createMockMetadataNodeList(Set urls, String prefix, String namespaceUrl, String tagName){
      
        DocumentBuilder builder = null;
	//        Element namespaceElement = null;

	/* Load DOM Document */
	try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    builder = factory.newDocumentBuilder();
	    
// 	    DOMImplementation impl = builder.getDOMImplementation();
// 	    Document namespaceHolder
// 		= impl.createDocument("http://www.lockss.org/oai/harvester", "harvester:namespaceHolder", null);
// 	    namespaceElement = namespaceHolder.getDocumentElement();
// 	    namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/",
// 					    "xmlns:harvester",
// 					    "http://www.lockss.org/research/software/oai/harvester");
// 	    namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/",
//                                             "xmlns:xsi",
// 					    "http://www.w3.org/2001/XMLSchema-instance");
// 	    namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/",
// 					    "xmlns:oai20",
// 					    "http://www.openarchives.org/OAI/2.0/");
// 	    namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/",
// 					    "xmlns:"+prefix,
// 					    "http://www.example.com/"+prefix);
// 	    namespaceElement.setAttributeNS("http://www.w3.org/2000/xmlns/",
// 					    "xmlns:xyz",
// 					    namespaceUrl);
					    
	} catch (ParserConfigurationException pce) {
	    logger.error("", pce);
	}


	NodeSet nodeSet = new NodeSet();
	// create sth like
	/*
	  <metadata>
	  <oai_dc:dc 
          xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
          xmlns:dc="http://purl.org/dc/elements/1.1/" 
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
          xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ 
          http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
	  <dc:identifier>http://www.perseus.tufts.edu/cgi-bin/ptext?
          doc=Perseus:text:1999.02.0084</dc:identifier>
	  </oai_dc:dc>
	  </metadata>
	*/

	Iterator it = urls.iterator();
	while (it.hasNext()){

	    StringBuffer xmlSBText = new StringBuffer();
	    xmlSBText.append("<metadata>");
	    xmlSBText.append("<"+prefix+":xyz "); //note: xyz is a namespaceUrl qualified name
	                                          //xyz is just for human read only. For the machine
	                                          //will check the whole namespaceUrl instead of this
	                                          //qualified name.

	    xmlSBText.append("xmlns:"+prefix+"=\"http://www.example.com/"+prefix+"\" ");
//	    xmlSBText.append("xmlns:xyz=\""+namespaceUrl+"\" ");
	    xmlSBText.append("xmlns:xyz=\""+namespaceUrl+"\">");

// 	    xmlSBText.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
// 	    xmlSBText.append("xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ ");
// 	    xmlSBText.append("http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">");
	    xmlSBText.append("<xyz:"+tagName+">"+it.next()+"</xyz:"+tagName+">");
	    xmlSBText.append("</"+prefix+":xyz>");
	    xmlSBText.append("</metadata>");
	    String xmlText = xmlSBText.toString();

	    try {
		Document tempDoc = builder.parse(new StringBufferInputStream(xmlText));
		nodeSet.addNode(tempDoc.getDocumentElement());
		logger.debug3(OaiHandler.displayXML(tempDoc.getDocumentElement()));
	    } catch (SAXException saxe) {
		logger.error("", saxe);
	    } catch (IOException ioe) {
		logger.error("", ioe);
	    } 
// 	    catch (XmlDomException xde) {
// 	      logger.error("", xde);
// 	    }

	    
	}

//===============================================================================
//String docString =
    //"<?xml version=""1.0"" encoding=""UTF-8""?> "+
// "<OAI-PMH xmlns=""http://www.openarchives.org/OAI/2.0/"" "+
// "         xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"" "+
// "         xsi:schemaLocation=""http://www.openarchives.org/OAI/2.0/ "+
// "         http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd""> "+
// "  <responseDate>2002-06-01T19:20:30Z</responseDate> "+
// " <request verb=""ListRecords"" from=""2002-05-01T14:15:00Z"" "+
// "          until=""2002-05-01T14:20:00Z"" metadataPrefix=""oai_dc""> "+
// "          http://www.perseus.tufts.edu/cgi-bin/pdataprov</request> "+
// " <ListRecords> "+
// "  <record> "+
// "    <header> "+
// "      <identifier>oai:perseus:Perseus:text:1999.02.0084</identifier> "+
// "     <datestamp>2002-05-01T14:16:12Z</datestamp> "+
// "    </header> "+
// "    <metadata> "+
// "      <oai_dc:dc  "+
// "          xmlns:oai_dc=""http://www.openarchives.org/OAI/2.0/oai_dc/""  "+
// "          xmlns:dc=""http://purl.org/dc/elements/1.1/""  "+
// "          xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""  "+
// "          xsi:schemaLocation=""http://www.openarchives.org/OAI/2.0/oai_dc/  "+
// "          http://www.openarchives.org/OAI/2.0/oai_dc.xsd""> "+
// "        <dc:title>Opera Minora</dc:title> "+
// "        <dc:creator>Cornelius Tacitus</dc:creator> "+
// "        <dc:type>text</dc:type> "+
// "        <dc:source>Opera Minora. Cornelius Tacitus. Henry Furneaux.  "+
// "         Clarendon Press. Oxford. 1900.</dc:source> "+
// "        <dc:language>latin</dc:language> "+
// "        <dc:identifier>http://www.perseus.tufts.edu/cgi-bin/ptext? "+
// "          doc=Perseus:text:1999.02.0084</dc:identifier> "+
// "      </oai_dc:dc> "+
// "    </metadata> "+
// "  </record> "+
// "  <record> "+
// "    <header> "+
// "      <identifier>oai:perseus:Perseus:text:1999.02.0083</identifier> "+
// "      <datestamp>2002-05-01T14:20:55Z</datestamp> "+
// "    </header> "+ 
// "    <metadata> "+
// "      <oai_dc:dc  "+
// "          xmlns:oai_dc=""http://www.openarchives.org/OAI/2.0/oai_dc/""  "+
// "          xmlns:dc=""http://purl.org/dc/elements/1.1/""  "+
// "          xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""  "+
// "          xsi:schemaLocation=""http://www.openarchives.org/OAI/2.0/oai_dc/  "+
// "          http://www.openarchives.org/OAI/2.0/oai_dc.xsd""> "+
// "        <dc:title>Germany and its Tribes</dc:title> "+
// "        <dc:creator>Tacitus</dc:creator> "+
// "        <dc:type>text</dc:type> "+
// "        <dc:source>Complete Works of Tacitus. Tacitus. Alfred John Church.  "+
// "         William Jackson Brodribb. Lisa Cerrato. edited for Perseus.  "+
// "         New York: Random House, Inc. Random House, Inc. reprinted 1942. "+
// "          </dc:source> "+
// "        <dc:language>english</dc:language> "+
// "        <dc:identifier>http://www.perseus.tufts.edu/cgi-bin/ptext? "+
// "         doc=Perseus:text:1999.02.0083</dc:identifier> "+
// "      </oai_dc:dc> "+
//     "      </metadata> ";
//     "  </record> ";
// " </ListRecords> "+
//     "</OAI-PMH> ";


// 	try {
// 	    XmlDomBuilder xmlDomBuilder = new XmlDomBuilder();
// 	    Document testDoc = xmlDomBuilder.parseXmlFile("docTemp.xml");
// 	    NodeList testNodeList = testDoc.getElementsByTagName("metadata");
// 	    logger.debug3("Printing the testing doc....");
// 	    for (int i=0; i<testNodeList.getLength(); i++) {
// 		Node node = testNodeList.item(i);
// 		if (node != null) {
// 		    logger.debug3(nodeToString(node));
// 		}
// 	    }
// 	    logger.debug3("Finish Printing the testing doc.");

// 	} catch (XmlDomException xde) {
// 	    logger.error("" ,xde);
// 	}


//==============================================================================



	return (NodeList) nodeSet;
    }
    
    public static void main(String[] argv) {
	String[] testCaseList = {TestBaseOaiMetadataHandler.class.getName()};
	junit.textui.TestRunner.main(testCaseList);
    }
}
