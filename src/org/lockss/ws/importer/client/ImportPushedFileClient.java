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
package org.lockss.ws.importer.client;

import java.io.File;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import org.lockss.ws.entities.ImportWsParams;
import org.lockss.ws.entities.ImportWsResult;
import org.lockss.ws.importer.ImportService;

/**
 * A client for the ImportService.importPushedFile() web service operation.
 */
public class ImportPushedFileClient extends ImportServiceBaseClient {
  /**
   * The main method.
   * 
   * @param args
   *          A String[] with the command line arguments. The first argument
   *          must be the specification of the file to be pushed, the second
   *          argument must be the archival unit identifier and the third
   *          argument must be the target URL.
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    ImportPushedFileClient thisClient = new ImportPushedFileClient();
    ImportService proxy = thisClient.getProxy();

    if (args.length < 3) {
      System.err.println("Error: File specification, target ID and target URL "
	  + "required");
      System.exit(2);
    }

    ImportWsParams params = new ImportWsParams();
    params.setDataHandler(new DataHandler(new FileDataSource(new File(
	args[0]))));
    params.setTargetId(args[1]);
    params.setTargetUrl(args[2]);

    int i = 3;

    if (i < args.length) {
      String[] properties = new String[args.length - i];
      System.arraycopy(args, i, properties, 0, args.length - i);
      params.setProperties(properties);
    }

    ImportWsResult result = proxy.importPushedFile(params);
    System.out.println("result = " + result);
  }
}
