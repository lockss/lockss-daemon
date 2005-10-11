/*
 * $Id: TestOaiHandler.java,v 1.4 2005-10-11 05:49:58 tlipkis Exp $
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

import java.util.List;
import org.lockss.test.*;
import ORG.oclc.oai.harvester2.verb.ListRecords;
import org.lockss.util.Logger;

import org.lockss.util.XmlDomBuilder;
import org.lockss.util.XmlDomBuilder.XmlDomException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerException;
import javax.xml.parsers.ParserConfigurationException;
import java.lang.NoSuchFieldException;
import java.io.IOException;
import org.xml.sax.SAXException;

import org.lockss.oai.OaiHandler.OaiResponseErrorException;

/**
 * Test cases for OaiHandler.java
 */
public class TestOaiHandler extends LockssTestCase {

  protected static Logger logger = Logger.getLogger("TestOaiHandler");
  private OaiRequestData oaiData;
  private String handler = "http://www.foo.com/handler";
  private String ns = "ns";
  private String tag = "tag";
  private String setSpec = "setSpec";
  private String prefix = "prefix";

  private String fromDate =  "2004-07-11";
  private String untilDate = "2004-12-26";

  private int retries = 3;

  private OaiHandler oaiHandler;

  public void setUp() throws Exception {
    super.setUp();
    oaiData = new OaiRequestData( handler, ns, tag, setSpec, prefix );
  }

//   public void testNullParameter(){
//     try {
//       oaiHandler = new OaiHandler( (OaiRequestData) null, fromDate, untilDate, retries);
//       fail("OaiHandler with null OaiRequestData should throw");
//     } catch (NullPointerException e) { }
//     try {
//       oaiHandler = new OaiHandler( oaiData , (String) null , untilDate, retries);
//       fail("OaiHandler with null fromDate should throw");
//     } catch (NullPointerException e) { }
//     try {
//       oaiHandler = new OaiHandler( oaiData , fromDate , (String) null, retries);
//       fail("OaiHandler with null untilDate should throw");
//     } catch (NullPointerException e) { }

//   }

  /** XXX things particular need to test in OaiHandler
   * issueRequest()
   * processResponse()
   * getErrors() with sth other than badResumptionToken
   * getErrors() with badResumptionToken
   * getUpdatedUrls()
   *
   */


  public void testGetErrorMockListRecords(){
    oaiHandler =  new MyMockOaiHandler();
    //build an element contain the error information
    try {
      Document doc = XmlDomBuilder.createDocument();
      Element elm = (Element) doc.createElement("error");
      elm.setAttribute("code", "badArgument");
      elm.appendChild( doc.createTextNode("This is an error statement") );
      ((MyMockOaiHandler)oaiHandler).setErrors(elm);
    } catch (XmlDomException xde) {
      logger.error("error when creating a Document", xde);
    }

    oaiHandler.issueRequest(oaiData, fromDate, untilDate);
    oaiHandler.processResponse(retries);

    List errList = oaiHandler.getErrors();
    String errMsg = ((OaiResponseErrorException) errList.get(0)).getMessage();
    String expectedErrMsg = "badArgument : This is an error statement";
    assertEquals(expectedErrMsg, errMsg);
  }



  /**
   * there are 4 parts in the OaiHandler
   * 1. constructing and issuing an OAI request
   * 2. retriving the response and check for error
   * 3. parse the response for urls
   * 4. if there is any resumptionToken, go back to (1)
   *    with resumptionToken in OAI request
   *
   * we need to check for the 4 part's error condition.
   * by comparing the hasError() and getUrlIterator()
   *
   * we need to make a MockListRecords, which will return a
   * nodeList of url (String), and can throw TransformerException
   *
   */


  // MockOaiHandler class to test listRecords.getErrors()
  private class MyMockOaiHandler extends OaiHandler {

    Element errElm = null;

    public MyMockOaiHandler(){
      super();
    }

    public void setErrors(Element elm) {
      errElm = elm;
    }

    public ListRecords issueRequest(
      OaiRequestData oaiData, String fromDate, String untilDate) {

      // do not check if oaiData == null, it is taken care in OaiRequestData constructor
      if (fromDate == null) {
	throw new NullPointerException("Called with null fromDate");
      } else if (untilDate == null) {
	throw new NullPointerException("Called with null untilDate");
      }

      String baseUrl = oaiData.getOaiRequestHandlerUrl();
      String setSpec = oaiData.getAuSetSpec();
      String metadataPrefix = oaiData.getMetadataPrefix();

      // the query string that send to OAI repository
      queryString = baseUrl + "?verb=ListRecords&from=" + fromDate + "&until=" +
	untilDate + "&metadataPrefix=" + metadataPrefix + "&set=" + setSpec;

      try {
	listRecords = new MockListRecords(baseUrl, fromDate, untilDate, setSpec,
				      metadataPrefix);

	//build an element contain the error information
// 	Document doc = XmlDomBuilder.createDocument();
// 	errElm = (Element) doc.createElement("error");
// 	errElm.setAttribute("code", "badArgument");
// 	errElm.appendChild( doc.createTextNode("This is an error statement") );
	if (errElm != null) {
	  ((MockListRecords)listRecords).setErrors(errElm);
	}
//       } catch (XmlDomException xde) {
// 	logError("error when creating a Document", xde);
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

  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestOaiHandler.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
