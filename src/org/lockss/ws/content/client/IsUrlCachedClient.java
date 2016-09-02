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
package org.lockss.ws.content.client;

import org.lockss.ws.content.ContentService;

/**
 * A client for the ContentService.isUrlCached() web service operation.
 */
public class IsUrlCachedClient extends ContentServiceBaseClient {
  /**
   * The main method.
   * 
   * @param args
   *          A String[] with the command line arguments.
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    ContentService proxy = new IsUrlCachedClient().getProxy();

    String url = null;
    String auId = null;
    String versionStr = null;
    int argCount = args.length;

    switch (argCount) {
    case 0:
      System.out.println("Error: No URL passed");
      System.exit(1);
    case 1:
      url = args[0];
      break;
    case 2:
      url = args[0];
      auId = args[1];
      break;
    default:
      url = args[0];
      auId = args[1];
      versionStr = args[2];
    }

    System.out.println("url = " + url);
    System.out.println("auId = " + auId);
    System.out.println("versionStr = " + versionStr);

    Integer version = null;

    if (versionStr != null) {
      try {
	version = Integer.parseInt(versionStr);
      } catch (NumberFormatException nfe) {
	System.out.println("Error: Invalid version number '" + versionStr
	    + "' passed");
	System.exit(1);
      }
    }

    boolean result = proxy.isUrlVersionCached(url, auId, version);
    System.out.println("result = " + result);
  }
}
