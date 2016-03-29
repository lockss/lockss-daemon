/*
 * $Id$
 */

/*

 Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.metadata.client;

import org.lockss.ws.entities.MetadataControlResult;
import org.lockss.ws.metadata.MetadataControlService;

/**
 * A client for the MetadataControlService.deletePublicationIssn() web service
 * operation.
 */
public class DeletePublicationIssnClient extends
    MetadataControlServiceBaseClient {
  /**
   * The main method.
   * 
   * @param args
   *          A String[] with the command line arguments.
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    Long mdItemSeq = Long.valueOf(args[0]);
    String issn = args[1];

    if (issn.indexOf("-") != -1) {
      issn = issn.replaceAll("-", "");
    }

    // Call the service and get the results of the query.
    MetadataControlService proxy = new DeletePublicationIssnClient().getProxy();
    MetadataControlResult result =
	proxy.deletePublicationIssn(mdItemSeq, issn, args[2]);

    System.out.println("result = " + result);
  }
}
