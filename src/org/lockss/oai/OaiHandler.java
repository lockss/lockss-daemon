/*
 * $Id: OaiHandler.java,v 1.1 2005-01-12 02:21:41 dcfok Exp $
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
import org.lockss.util.*;

import org.lockss.config.Configuration;

import ORG.oclc.oai.harvester2.verb.ListRecords;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;

import javax.xml.transform.TransformerException;
import javax.xml.parsers.ParserConfigurationException;
import java.lang.NoSuchFieldException;
import java.io.IOException;
import org.xml.sax.SAXException;

/**   
 * there are 4 parts in the OaiHandler
 * 1. constructing and issuing an OAI request
 * 2. retriving the response and check for error
 * 3. parse the response for urls
 * 4. if there is any resumptionToken, go back to (1) 
 *    with resumptionToken in OAI request
 *
 */
public class OaiHandler {

  protected static Logger logger = Logger.getLogger("OaiHandler");
  private Set updatedUrls = new HashSet();
  protected String queryString = null;
  private ListRecords listRecords = null;
  private String baseUrl;
  private int retries = 0;
  private int maxRetries;
  private OaiRequestData oaiData;
  private String fromDate;
  private String untilDate;

  // the latest error is at the beginning of the list
  private LinkedList errList = new LinkedList();
  
  // the root node of all the oai records retrieved in this request
  private Set oaiRecords = new HashSet();

   /**
   * Constructor
   * @param oaiData OaiRequestData which is constructed by OaiCrawlSpec
   * @param fromDate create date of records the Oai request want from
   * @param untilDate create date of records the Oai request want until
   * @param maxRetrues retry limit of oai request when retriable error is encountered
   */
  public OaiHandler(OaiRequestData oaiData, String fromDate, String untilDate, int maxRetries) {

    if (fromDate == null) {
      throw new NullPointerException("Called with null fromDate");
    } else if (untilDate == null) {
      throw new NullPointerException("Called with null untilDate");
    }
    
    this.oaiData = oaiData;
    this.fromDate = fromDate;
    this.untilDate = untilDate;
    this.maxRetries = maxRetries;

    listRecords = createListRecords(oaiData, fromDate, untilDate);
    processListRecords(listRecords);
  }
  
  /**
   * Read varies things off the ListRecords
   * 1. check for error in creating ListRecords
   * 2. get all the information we need from the ListRecords
   * 3. create another ListRecords if there is a resumptionToken
   */
  protected void processListRecords(ListRecords listRecords){

    while (listRecords != null) {
      
      //check if we have error
      try {
	NodeList errors = listRecords.getErrors();   
	if (errors != null && errors.getLength() > 0) {
	  //	  errorExists = true;
	  int length = errors.getLength();
	  for (int i=0; i<length; ++i) {
	    Node item = errors.item(i);
	    
	    String errCode = ((Element)item).getAttribute("code");
	    String errMsg = errCode +  " : " +  item.getFirstChild().getNodeValue();

	    OaiResponseErrorException oaiErrEx = new OaiResponseErrorException(errMsg);
	    logError("In Oai Response's error tag", oaiErrEx);

	    if (errCode == "badResumptionToken") {
	      if ( retries < maxRetries) {
		logger.info("badResumptionToke error, re-issue a new oai request");
		listRecords = createListRecords(oaiData, fromDate, untilDate);
		retries++;
		continue;
	      } else {
		logger.warning("Exceeded maximum Oai Request retry times");
	      }
	    }

	    //testing to see what is inside that error item
	    //	    logger.debug3("nodeToString");
	    //      logger.debug3(nodeToString(item));
	  }

	  logger.debug3("Error record: " + listRecords.toString() );

	  break; //apart from badResumptionToken, we cannot do much of other error case, thus just break
	}
      } catch (TransformerException e) {
	logError("In calling getErrors", e);
      }
      
      //see what is inside the response
      logger.debug3("The content of listRecord : \n" + listRecords.toString() );

      //collect all the oai records
      collectOaiRecords(); //XXX info collected is not being used now, 
                           //can turn off to increase performance

      //======= this should be in another object, 
      // (some kind of interface to support different metadata format) ==============

      //parse URLs out from the Oai response 
// 	NodeList nodeList = 
// 	  listRecords.getDocument().getElementsByTagNameNS(oaiData.getMetadataNamespaceUrl(), 
// 							   oaiData.getUrlContainerTagName());
	
// 	logger.debug3("nodeList length = " + nodeList.getLength());

// 	// Process the elements in the nodelist
//         for (int i=0; i<nodeList.getLength(); i++) {
//           // add the Urls to the updatedUrls set
// 	  Node node = nodeList.item(i);
// 	  if (node != null) {

// 	    String str = node.getFirstChild().getNodeValue();

// 	    //do not validate url here, let the crawler handle malform Url
// 	    updatedUrls.add(str);
// 	    logger.debug3("node (" + i + ") value = " + str);
// 	    logger.debug3("in xml :" + nodeToString(node) );
// 	  }
//         }

      //==============================================================================
	NodeList metadataNodeList = 
	  listRecords.getDocument().getElementsByTagName("metadata");
	
	OaiMetadataHandler metadataHandler = oaiData.getMetadataHandler();

	metadataHandler.setupAndExecute(metadataNodeList);

	updatedUrls.addAll(metadataHandler.getArticleUrls());
      //======================================================================

      //see if all the records are include in the response by checking the presence of
      //resumptionToken. If there is more records, request them by resumptionToken
      try {
	String resumptionToken = listRecords.getResumptionToken();
	logger.debug3("resumptionToken: " + resumptionToken);
	if (resumptionToken == null || resumptionToken.length() == 0) {
	  break; //break out of the while loop as there is no more new url
	} else {
	  //TODO: Before including a resumptionToken in the URL of a subsequent request, 
	  // we must encode any special characters in it.
	  listRecords = new ListRecords(baseUrl, resumptionToken);
	} 
      } catch (IOException ioe) {
        logError("In calling getResumptionToken", ioe);
      } catch (NoSuchFieldException nsfe) {
	logError("In calling getResumptionToken", nsfe);
      } catch (TransformerException tfe) {
	logError("In calling getResumptionToken", tfe);
      } catch (SAXException saxe) {
	logError("In createListRecords calling new ListRecords", saxe);
      } catch (ParserConfigurationException pce) {
	logError("In createListRecords calling new ListRecords", pce);
      }

    } //loop until there is no resumptionToken
  }

 /**
  * By create a ListRecords, an Oai request is issued and the response is also 
  * store in the ListRecords object.
  */
  protected ListRecords createListRecords(
	    OaiRequestData oaiData, String fromDate, String untilDate){
    baseUrl = oaiData.getOaiRequestHandlerUrl();
    String setSpec = oaiData.getAuSetSpec();
    String metadataPrefix = oaiData.getMetadataPrefix();  
    ListRecords listRecords = null;
    
    // the query string that send to OAI repository
    queryString = baseUrl + "?verb=ListRecords&from=" + fromDate + "&until=" +
      untilDate + "&metadataPrefix=" + metadataPrefix + "&set=" + setSpec;

    try {
      listRecords = new ListRecords(baseUrl, fromDate, untilDate, setSpec,
				      metadataPrefix);
    } catch (IOException ioe) {
      logError("In createListRecords calling new ListRecords", ioe);
    } catch (ParserConfigurationException pce) {
      logError("In createListRecords calling new ListRecords", pce);
    } catch (SAXException saxe) {
      logError("In createListRecords calling new ListRecords", saxe);
    } catch (TransformerException tfe) {
      logError("In createListRecords calling new ListRecords", tfe);
    }
    
    return listRecords;
  }

  protected void logError(String msg, Exception ex) {
    logger.error(msg, ex);
    errList.addFirst(ex);
  }

  /**
   * Method to check if there is any error occurs in construct request, 
   * issue request, and parse response.
   * Note: the latest error is at the beginning of the list, like a stack
   *
   * @return the error list 
   */
  public List getErrors() {
    return (List) errList;
  }

  /**
   * Get the Iterator of a list of Urls extracted from the OAI response
   * 
   * @return the list of Urls extracted from Oai response
   */
  public Set getUpdatedUrls(){
    return updatedUrls;
  }

  private void collectOaiRecords(){
    NodeList nodeList = listRecords.getDocument().getElementsByTagName("record");
    for (int i=0; i<nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node != null) {
	oaiRecords.add(node);
	logger.debug3("Record ("+ i +") :" + nodeToString(node));
      }
    }
  }

  /**
   * return all the <record> nodes in the oai response
   * each element in the Set is of class "org.w3c.dom.Node".
   *
   * @return the set of <record> nodes
   */
  public Set getOaiRecords(){
    return oaiRecords;
  }

  /**
   * Print out the OAI query string for error tracing 
   */   
  public String toString(){
    return queryString;
  }

  // print out the content of a node, its attribute and its children
  // it might be useful to put it in some kind of Util
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

  public static class OaiResponseErrorException extends Exception {

    public OaiResponseErrorException(String errMsg) {
      super(errMsg);
    }

    public OaiResponseErrorException(){
      this("");
    }
  }

}
