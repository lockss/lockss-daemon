/*
 * $Id: MockListRecords.java,v 1.1 2004-12-18 01:44:55 dcfok Exp $
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

import org.apache.xpath.NodeSet;
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
  
  private Exception getErrorException;
  private int numGetErrorExceptionThrown=0;
  
  private Exception getNodeListException;
  private int numGetNodeListExceptionThrown=0;
  
  private Exception getResumptionTokenException;
  private int numGetResumptionTokenExceptionThrown=0;
  
  //  private Document doc = null;
  private Element errElm = null;

  public MockListRecords(String baseUrl, String fromDate, String untilDate, 
			   String setSpec, String metadataPrefix) 
	throws IOException, ParserConfigurationException, SAXException, TransformerException {
    super();   

//     // create a *Element Factory*
//     try {
//       doc = XmlDomBuilder.createDocument();
//     } catch (XmlDomException xde) {
//       logger.error("", xde);
//     }

  }
    
  public void setListRecordsException(Exception ex, int num){
    listRecordsException = ex;
    numListRecordsExceptionThrown = num;
  } 

  public void setGetErrorException(Exception ex, int num){
    getErrorException = ex;
    numGetErrorExceptionThrown = num;
  }
  
  public void setGetNodeListException(Exception ex, int num){
    getNodeListException = ex;
    numGetNodeListExceptionThrown = num;
  }
  
  public void setGetResumptionToken(Exception ex, int num){
    getResumptionTokenException = ex;
    numGetResumptionTokenExceptionThrown = num;
  }
 
  /**
   * setErrors in a particular format
   *
   * e.g.
   * Element elm = (Element) doc.createElement("error"); 
   * elm.setAttribute("code", "InGetErrorsInMyMockListRecords");
   * elm.appendChild( doc.createTextNode("This is an error statement") );
   */
  public void setErrors(Element elm){
    errElm = elm;
  }

  public NodeList getErrors() {
    return (NodeList) new NodeSet(errElm);  
  }
  
  public NodeList getNodeList(String xpath){
    
    return null;
  }
  
  public String getResumptionToken(){
      
    return null;
  }
  
}
