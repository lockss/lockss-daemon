/*
 * $Id: MockListRecords.java,v 1.5 2005-10-11 05:52:05 tlipkis Exp $
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

package org.lockss.test;

import org.lockss.test.*;
import org.lockss.oai.*;
import org.lockss.util.Logger;

import ORG.oclc.oai.harvester2.verb.ListRecords;
import org.w3c.dom.NodeList;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import javax.xml.transform.TransformerException;

// import org.lockss.util.XmlDomBuilder;
// import org.lockss.util.XmlDomBuilder.XmlDomException;
// import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * class mock the behaviour of ListRecords
 */
public class MockListRecords extends ListRecords{
  private static Logger logger = Logger.getLogger("MockListRecords");

  private Exception listRecordsException;
  private int numListRecordsExceptionThrown=0;

  private TransformerException getErrorException;
  private int numGetErrorExceptionThrown=0;

  private TransformerException getNodeListException;
  private int numGetNodeListExceptionThrown=0;

  private Exception getResumptionTokenException;
  private int numGetResumptionTokenExceptionThrown=0;

  //  private Document doc = null;
  private Element errElm = null;

  private String baseUrl;
  private String fromDate;
  private String untilDate;
  private String setSpec;
  private String metadataPrefix;

  public MockListRecords(){
  }

  public MockListRecords(String baseUrl, String fromDate, String untilDate,
			   String setSpec, String metadataPrefix)
	throws IOException, ParserConfigurationException, SAXException, TransformerException{
    this();
    this.baseUrl = baseUrl;
    this.fromDate = fromDate;
    this.untilDate = untilDate;
    this.setSpec = setSpec;
    this.metadataPrefix = metadataPrefix;

    while (numListRecordsExceptionThrown > 0) {
      if ( listRecordsException instanceof IOException){
	throw (IOException) listRecordsException;
      } else if ( listRecordsException instanceof ParserConfigurationException){
	throw (ParserConfigurationException) listRecordsException;
      } else if ( listRecordsException instanceof SAXException){
	throw (SAXException) listRecordsException;
      } else if ( listRecordsException instanceof TransformerException){
	throw (TransformerException) listRecordsException;
      }
    }

//     // create a *Element Factory*
//     try {
//       doc = XmlDomBuilder.createDocument();
//     } catch (XmlDomException xde) {
//       logger.error("", xde);
//     }

  }

  public void setListRecordsException(Exception ex, int num){
    if ( ex instanceof IOException ||
	 ex instanceof ParserConfigurationException ||
	 ex instanceof SAXException ||
	 ex instanceof TransformerException) {
      listRecordsException = ex;
      numListRecordsExceptionThrown = num;
    } else {
      logger.error("Exception set never be thrown in real ListRecords() call");
    }
  }

  public void setGetErrorException(TransformerException ex, int num){
    getErrorException = ex;
    numGetErrorExceptionThrown = num;
  }

  public void setGetNodeListException(TransformerException ex, int num){
    getNodeListException = ex;
    numGetNodeListExceptionThrown = num;
  }

  public void setGetResumptionToken(Exception ex, int num){
    if ( ex instanceof NoSuchFieldException ||
	 ex instanceof TransformerException) {
      getResumptionTokenException = ex;
      numGetResumptionTokenExceptionThrown = num;
    } else {
      logger.error("Exception set never be thrown in real ListRecords() call");
    }
  }

  /**
   * set errors in oai response format
   *
   * e.g.
   * To construct:
   * <error code="badArgument">This is an error statement</error>
   *
   * The code is:
   * // create a document to create an element
   * Document doc;
   * try {
   *   doc = XmlDomBuilder.createDocument();
   * } catch (XmlDomException xde) {
   *   logger.error("", xde);
   * }
   * Element elm = (Element) doc.createElement("error");
   * elm.setAttribute("code", "badArgument");
   * elm.appendChild( doc.createTextNode("This is an error statement") );
   *
   * Note: in a real oai response there is a finit set of error code
   * They can be found in http://www.openarchives.org/OAI/openarchivesprotocol.html#ErrorConditions
   */
  public void setErrors(Element elm){
    errElm = elm;
  }

  public NodeList getErrors() throws TransformerException{
    while (numGetErrorExceptionThrown > 0) {
      throw getErrorException;
    }
    return new MockNodeList().addNode(errElm);
  }

  /**
   * set a node list in a particular format
   *
   * e.g.
   * Element elm = (Element) doc.createElement("error");
   * elm.setAttribute("code", "InGetErrorsInMyMockListRecords");
   * elm.appendChild( doc.createTextNode("This is an error statement") );
   */
  public void setNodeList(Element elm){

  }

  public NodeList getNodeList(String xpath){

    return null;
  }

  public String getResumptionToken() throws NoSuchFieldException, TransformerException{

    while (numGetResumptionTokenExceptionThrown > 0) {
      if ( getResumptionTokenException instanceof NoSuchFieldException){
	throw (NoSuchFieldException) getResumptionTokenException;
      } else if ( getResumptionTokenException instanceof TransformerException){
	throw (TransformerException) getResumptionTokenException;
      }
    }

    return null;
  }

  public String toString() {
    return "[MockListRecords: ]";
  }

}
