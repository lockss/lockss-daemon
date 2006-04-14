/*
 * $Id: OaiHandler.java,v 1.12 2006-04-14 15:23:14 troberts Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import ORG.oclc.oai.harvester2.verb.ListRecords;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

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
  protected String queryString;
  protected ListRecords listRecords;
  private String baseUrl;
  private int retries;
//   private int maxRetries;
  private OaiRequestData oaiData;
  private String fromDate;
  private String untilDate;

  // the latest error is at the beginning of the list
  private LinkedList errList = new LinkedList();

  // the root node of all the oai records retrieved in this request
  private Set oaiRecords = new HashSet();

//    /**
//    * Constructor
//    * @param oaiData OaiRequestData which is constructed by OaiCrawlSpec
//    * @param fromDate create date of records the Oai request want from
//    * @param untilDate create date of records the Oai request want until
//    * @param maxRetries retry limit of oai request when retriable error is encountered
//    */
//   public OaiHandler(OaiRequestData oaiData,
// 		    String fromDate,
// 		    String untilDate,
// 		    int maxRetries) {

//     if (fromDate == null) {
//       throw new NullPointerException("Called with null fromDate");
//     } else if (untilDate == null) {
//       throw new NullPointerException("Called with null untilDate");
//     }

//     this.oaiData = oaiData;
//     this.fromDate = fromDate;
//     this.untilDate = untilDate;
//     this.maxRetries = maxRetries;

//     //XXX need to remove this from the constructor and have the outside make
//     // a function call explicitly.
//     // make it to sth like
//     // constructor (with the oai requset setup)
//     // issueRequest (really make a request to the repository)
//     // processResult

//     ListRecords lr = issueOaiRequest(oaiData, fromDate, untilDate);
//     processListRecords(lr);
//   }

  public OaiHandler(){
    retries = 0;
    oaiData = null;
    fromDate = null;
    untilDate = null;
    listRecords = null;
    queryString = null;
  }

  /**
   * Read varies things off the ListRecords
   * 1. check for error in creating ListRecords
   * 2. get all the information we need from the ListRecords
   * 3. create another ListRecords if there is a resumptionToken
   *
   * @param maxRetries number of times to retry the request if it fails due
   * to badResumptionToken
   */
  public void processResponse(int maxRetries){
    if (listRecords == null){
      logger.error("Make sure OaiHandler.issueRequest() is executed");
      return;
    }
    if (maxRetries < 0){
      logger.warning("maxRetries is a negative number");
    }

    nextListRecords:
    while (listRecords != null) {

      //check if we have error
      NodeList errors = null;
      try {
	errors = listRecords.getErrors();
      } catch (TransformerException e) {
	logError("In calling getErrors", e);
      }
      if (errors != null && errors.getLength() > 0) {
	int length = errors.getLength();
	for (int i=0; i<length; ++i) {
	  Node item = errors.item(i);

	  //sample error code:
	  //<error code="badResumptionToken">More info about the error</error>
	  String errCode = ((Element)item).getAttribute("code");
	  String errMsg = errCode +  " : " +  item.getFirstChild().getNodeValue();

	  //create an error for logError
	  OaiResponseErrorException oaiErrEx = new OaiResponseErrorException(errMsg);

	  if (errCode == "noRecordsMatch"){ // this could be normal if there is no update
	                                    // from the repository.
	    logger.warning("The combination of the values of the from, until, set "
                           +"and metadataPrefix arguments results in an empty list.");
	  } else if (errCode == "badResumptionToken") {
	    if ( retries < maxRetries) {
	      logger.info("badResumptionToken error, re-issue a new oai request");
	      listRecords = issueRequest(oaiData, fromDate, untilDate);
	      retries++;
	      continue nextListRecords;
	    } else {
	      logger.warning("Exceeded maximum Oai Request retry times");
	      logError("badResumptionToken in Oai Response", oaiErrEx);
	    }
	  } else if (errCode == "badArgument"){
	    logError("badArgument in Oai Response, check the oai query string: "
		     + getOaiQueryString() , oaiErrEx);
	  } else if (errCode == "cannotDisseminateFormat") {
	    logError("cannotDisseminateFormat in Oai Response, "
                     +"metadata format not supported by item or repositry", oaiErrEx);
	  } else if (errCode == "noSetHierarchy") {
	    logError("The repository does not support sets.", oaiErrEx);
	  } else {
	    logError("Unexpected Error.", oaiErrEx);
	  }

	  //testing to see what is inside that error item
	  //	    logger.debug3("nodeToString");
	  //      logger.debug3(nodeToString(item));
	}

	logger.warning("Error record: " + listRecords.toString() );

	break; //apart from badResumptionToken, we cannot do much of other error case, thus just break
      }


      //see what is inside the lisRecord
//      logger.debug3("The content of listRecord : \n" + listRecords.toString() );
      //toString on ListRecords is currently not thread safe, so I commented this out
      //TSR 4/13/06
      
      //collect and store all the oai records
//       collectOaiRecords(listRecords); //XXX info collected is not being used now,
                                      //can turn off to increase performance

      //parser urls by some implementation of oaiMetadataHander
      NodeList metadataNodeList =
	listRecords.getDocument().getElementsByTagName("metadata");

      OaiMetadataHandler metadataHandler = oaiData.getMetadataHandler();

      //apart from collecting urls, more actions might be done in the
      //metadata handler w.r.t. different metadata
      metadataHandler.setupAndExecute(metadataNodeList);

      //put all the collected urls to updatedUrls
      updatedUrls.addAll(metadataHandler.getArticleUrls());

      //see if all the records are include in the response by checking the presence of
      //resumptionToken. If there is more records, request them by resumptionToken
      try {
	String resumptionToken = listRecords.getResumptionToken();
	logger.debug3("resumptionToken: " + resumptionToken);
	if (resumptionToken == null || resumptionToken.length() == 0) {
	  break; //break out of the while loop as there is no more new url
	} else {
	  //XXX TODO: Before including a resumptionToken in the URL of a
	  // subsequent request,
	  // we must encode any special characters in it.
 	  listRecords = new ListRecords(baseUrl, resumptionToken);
	}
       } catch (IOException ioe) {
         logError("In getting ResumptionToken and requesting new ListRecords",
		  ioe);
       } catch (NoSuchFieldException nsfe) {
 	logError("In getting ResumptionToken and requesting new ListRecords",
 		 nsfe);
       } catch (TransformerException tfe) {
 	logError("In getting ResumptionToken and requesting new ListRecords",
 		 tfe);
      } catch (SAXException saxe) {
	logError("In getting ResumptionToken and requesting new ListRecords",
		 saxe);
      } catch (ParserConfigurationException pce) {
	logError("In getting ResumptionToken and requesting new ListRecords",
		 pce);
      }

    } //loop until there is no resumptionToken
  }

  /**
   * By create a ListRecords, an Oai request is issued and the response is also
   * store in the ListRecords object.
   *
   * @param oaiData oaiRequestData object that stores oai related information from OaiCrawlSpec
   * @param fromDate date from when we want to query about
   * @param untilDate date until when we want to query about
   */
  public ListRecords issueRequest(OaiRequestData oaiData,
				  String fromDate, String untilDate){

    // do not check if oaiData == null, it is taken care in OaiRequestData constructor
    if (fromDate == null) {
      throw new NullPointerException("Called with null fromDate");
    } else if (untilDate == null) {
      throw new NullPointerException("Called with null untilDate");
    }

    this.oaiData = oaiData;
    this.fromDate = fromDate;
    this.untilDate = untilDate;

    baseUrl = oaiData.getOaiRequestHandlerUrl();
    String setSpec = oaiData.getAuSetSpec();
    String metadataPrefix = oaiData.getMetadataPrefix();
    //    ListRecords listRecords = null;

    // the query string that send to OAI repository
    queryString = baseUrl + "?verb=ListRecords&from=" + fromDate + "&until=" +
      untilDate + "&metadataPrefix=" + metadataPrefix + "&set=" + setSpec;

    try {
      listRecords = new ListRecords(baseUrl, fromDate, untilDate, setSpec,
				    metadataPrefix);
    } catch (IOException ioe) {
      logError("In issueOaiRequest calling new ListRecords", ioe);
    } catch (ParserConfigurationException pce) {
      logError("In issueOaiRequest calling new ListRecords", pce);
    } catch (SAXException saxe) {
      logError("In issueOaiRequest calling new ListRecords", saxe);
    } catch (TransformerException tfe) {
      logError("In issueOaiRequest calling new ListRecords", tfe);
    }

    return listRecords;
  }

  /**
   * log the error message and the corresponding exception
   *
   * XXX need to be rewritten to reflect errors in crawlStatus
   */
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
   *
   * XXX need to be rewritten to reflect errors in crawlStatus
   */
  public List getErrors() {
    return errList;
  }

  /**
   * Get a list of Urls extracted from the OAI response
   *
   * @return the list of Urls extracted from Oai response
   */
  public Set getUpdatedUrls(){
    return updatedUrls;
  }

  /**
   * Stores <record>.......</record> element in the oai response in a list.
   * Information collected maybe useful to our future developement.
   *
   * //XXX do we want to have a place in the file system to store it ?
   * should it be output as an xml ? Methods to output the OaiRecords as
   * a file needed to be implemented.
   */
  private void collectOaiRecords(ListRecords listRecords){
    NodeList nodeList = listRecords.getDocument().getElementsByTagName("record");
    for (int i=0; i<nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node != null) {
	oaiRecords.add(node);
	logger.debug3("Record ("+ i +") :" + displayXML(node));
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
  public String getOaiQueryString(){
    return queryString;
  }

  /**
   * print out the content of a node, its attribute and its children
   * it might be useful to put it in some kind of Util
   */
  String nodeToString(Node domNode){
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

  //walk the DOM tree and print as u go
  static String displayXML(Node node){

    StringBuffer tmpStr = new StringBuffer();

        int type = node.getNodeType();
        switch(type)
        {
            case Node.DOCUMENT_NODE:
            {
              tmpStr.append("<?xml version=\"1.0\" encoding=\""+
                                "UTF-8" + "\"?>");
              break;
            }//end of document
            case Node.ELEMENT_NODE:
            {
                tmpStr.append('<' + node.getNodeName() );
                NamedNodeMap nnm = node.getAttributes();
                if(nnm != null )
                {
                    int len = nnm.getLength() ;
                    Attr attr;
                    for ( int i = 0; i < len; i++ )
                    {
                        attr = (Attr)nnm.item(i);
                        tmpStr.append(' '
                             + attr.getNodeName()
                             + "=\""
                             + attr.getNodeValue()
                             +  '"' );
                    }
                }
                tmpStr.append('>');

                break;

            }//end of element
            case Node.ENTITY_REFERENCE_NODE:
            {

               tmpStr.append('&' + node.getNodeName() + ';' );
               break;

            }//end of entity
            case Node.CDATA_SECTION_NODE:
            {
                    tmpStr.append( "<![CDATA["
                            + node.getNodeValue()
                            + "]]>" );
                     break;

            }
            case Node.TEXT_NODE:
            {
                tmpStr.append(node.getNodeValue());
                break;
            }
            case Node.PROCESSING_INSTRUCTION_NODE:
            {
                tmpStr.append("<?"
                    + node.getNodeName() ) ;
                String data = node.getNodeValue();
                if ( data != null && data.length() > 0 ) {
                    tmpStr.append(' ');
                    tmpStr.append(data);
                }
                tmpStr.append("?>");
                break;

             }
        }//end of switch


        //recurse
        for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
        {
            tmpStr.append(displayXML(child));
        }

        //without this the ending tags will miss
        if ( type == Node.ELEMENT_NODE )
        {
            tmpStr.append("</" + node.getNodeName() + ">");
        }

	return tmpStr.toString();

    }//end of displayXML

  /**
   * OaiResponseErrorException will only be thrown when there is
   * error in the Oai Response.
   */
  public static class OaiResponseErrorException extends Exception {

    /**
     * Constructor
     *
     * @param errMsg Error message
     */
    public OaiResponseErrorException(String errMsg) {
      super(errMsg);
    }

    public OaiResponseErrorException(){
      this("");
    }
  }

}
