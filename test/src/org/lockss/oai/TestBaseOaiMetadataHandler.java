/*
 * $Id: TestBaseOaiMetadataHandler.java,v 1.1 2005-01-18 10:31:11 dcfok Exp $
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
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.XmlDomBuilder.XmlDomException;
import org.w3c.dom.*;
import org.apache.xpath.NodeSet;

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

//     public void testSetupAndExecute(){
// 	String prefix = "abc";
// 	String nsUrl = "http://www.example.com/ns/";
// 	String tag = "foo";

// 	//setup a nodelist that contain different <metadata>..<tag>url</tag>..</metadata> records
// 	String url1 = "http://www.example.com/link1.html";
// 	String url2 = "http://www.example.com/link2.html";
// 	Set testUrls = SetUtil.set(url1, url2);
// 	NodeList metadataNodeList = createMockMetadataNodeList(testUrls, prefix, nsUrl, tag);

// 	//construct a bastOaiMetadataHandler
// 	BaseOaiMetadataHandler mdHandler = new BaseOaiMetadataHandler(prefix, nsUrl, tag);

// 	//run setupAndExecute
// 	mdHandler.setupAndExecute(metadataNodeList);

// 	//check by assertEquals of getArticleUrls()
// 	assertEquals(testUrls, mdHandler.getArticleUrls());
//     }

    private NodeList createMockMetadataNodeList(Set urls, String prefix, String namespaceUrl, String tagName){

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

	//XXX need to figure out how to add the namspaceUrl to this mock xml doc
	Iterator it = urls.iterator();
	while (it.hasNext()){

	    StringBuffer xmlSBText = new StringBuffer("<metadata><"+prefix);
	    xmlSBText.append("><"+tagName+">" + (String) it.next());
	    xmlSBText.append("</"+ tagName +"></"+ prefix +"></metadata>");
	    String xmlText = xmlSBText.toString();
	try {
	    XmlDomBuilder xmlDomBuilder = new XmlDomBuilder();
	    Document tempDoc = xmlDomBuilder.parseXmlString(xmlText);
	    tempDoc.createAttributeNS(namespaceUrl, "xyz");
	    nodeSet.addNode(tempDoc.getDocumentElement());
	    logger.debug3(nodeToString(tempDoc.getDocumentElement()));
	} catch (XmlDomException xde) {
	    logger.error("" ,xde);
	}

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
    
    private String nodeToString(Node domNode){
	// An array of names for DOM node-types
	// (Array indexes = nodeType() values.)
	String[] typeName = {
	    "none",
	    "Element",
	    "Attr",
	    "Text",
	    "CDATA",
	    "EntityRef",
	    "Entity",
	    "ProcInstr",
	    "Comment",
	    "Document",
	    "DocType",
	    "DocFragment",
	    "Notation",
	};
	
	String s = typeName[domNode.getNodeType()];
	String nodeName = domNode.getNodeName();
	if (! nodeName.startsWith("#")) {
	    s += ": " + nodeName;
	}
	if (domNode.getNodeValue() != null) {
	    if (s.startsWith("ProcInstr")) 
		s += ", ";
	    else
		s += ": ";
	    // Trim the value to get rid of NL's at the front
	    String t = domNode.getNodeValue().trim();
	    int x = t.indexOf("\n");
	    if (x >= 0) t = t.substring(0, x);
	    s += t;
	}
	s += "\n";
	NamedNodeMap attrMap = domNode.getAttributes();
	if (attrMap != null){
	    s += "Its attributes are : \n";
	    for (int iy=0; iy <attrMap.getLength(); iy++){
		Attr attr = (Attr) attrMap.item(iy);
		s += attr.getName() + " = " + attr.getValue() + "\n";
	    }
	}
	NodeList childList = domNode.getChildNodes();
	if (childList != null) {
	    s += "Child nodes of "+ nodeName + ": \n";
	    for (int ix=0; ix <childList.getLength(); ix++){
		s += nodeToString(childList.item(ix));
	    }
	}
	return s;
    }
    
    public static void main(String[] argv) {
	String[] testCaseList = {TestBaseOaiMetadataHandler.class.getName()};
	junit.textui.TestRunner.main(testCaseList);
    }
}
