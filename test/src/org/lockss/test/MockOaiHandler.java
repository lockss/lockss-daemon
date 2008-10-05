/*
 * $Id: MockOaiHandler.java,v 1.1 2005-10-13 22:44:17 troberts Exp $
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
package org.lockss.test;

import java.util.*;
import java.util.List;

import ORG.oclc.oai.harvester2.verb.ListRecords;

import org.lockss.oai.*;
import org.lockss.oai.OaiHandler;

public class MockOaiHandler extends OaiHandler {

  Set updatedUrls;

  OaiRequestData oaiData;
  String fromDate;
  String untilDate;

  public MockOaiHandler() {
    super();
  }

  public void processResponse(int maxRetries) {
//intentionally blank
  }

  public ListRecords issueRequest(OaiRequestData oaiData,
                                  String fromDate, String untilDate) {
    return null;
  }

  public List getErrors() {
    return new ArrayList();
  }

  public Set getUpdatedUrls(){
    return updatedUrls;
  }

  public void setUpdatedUrls(Set urls) {
    this.updatedUrls = urls;
  }


  public Set getOaiRecords(){
    throw new UnsupportedOperationException("not implemented");
  }

  public String getOaiQueryString(){
    throw new UnsupportedOperationException("not implemented");
  }

}
