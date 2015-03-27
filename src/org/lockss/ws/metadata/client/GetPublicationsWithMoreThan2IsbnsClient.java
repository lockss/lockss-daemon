/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.List;
import org.lockss.ws.entities.KeyIdNamePairListPair;
import org.lockss.ws.metadata.MetadataMonitorService;

/**
 * A client for the MetadataMonitorService.getPublicationsWithMoreThan2Isbns()
 * web service operation.
 */
public class GetPublicationsWithMoreThan2IsbnsClient extends
    MetadataMonitorServiceBaseClient {
  /**
   * The main method.
   * 
   * @param args
   *          A String[] with the command line arguments.
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    // Call the service and get the results of the query.
    MetadataMonitorService proxy =
	new GetPublicationsWithMoreThan2IsbnsClient().getProxy();
    List<KeyIdNamePairListPair> publicationsWithMoreThan2Isbns =
	proxy.getPublicationsWithMoreThan2Isbns();

    if (publicationsWithMoreThan2Isbns != null) {
      System.out.println("publicationsWithMoreThan2Isbns.size() = "
	  + publicationsWithMoreThan2Isbns.size());

      for (KeyIdNamePairListPair publication : publicationsWithMoreThan2Isbns) {
	System.out.println("publication = " + publication.getKey()
	    + ", ISBNs" + " = " + publication.getValues());
      }
    } else {
      System.out.println("publicationsWithMoreThan2Isbns = "
	  + publicationsWithMoreThan2Isbns);
    }
  }
}