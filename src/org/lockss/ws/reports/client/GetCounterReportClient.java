/*
 * $Id$
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * A client for the CounterReportsService.getCounterReport() web service
 * operation.
 */
package org.lockss.ws.reports.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.lockss.ws.entities.CounterReportParams;
import org.lockss.ws.entities.CounterReportResult;
import org.lockss.ws.reports.CounterReportsService;

public class GetCounterReportClient extends CounterReportsServiceBaseClient {
  /**
   * The main method.
   * 
   * @param args
   *          A String[] with the command line arguments.
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    CounterReportsService proxy = new GetCounterReportClient().getProxy();

    CounterReportResult result =
	proxy.getCounterReport(new CounterReportParams());
    System.out.println("result = " + result);

    String reportFileName = result.getFileName();
    File reportFile = new File("/tmp", reportFileName);
    System.out.println("reportFile = " + reportFile.getAbsolutePath());

    // Write the received file.
    InputStream dhis = null;
    FileOutputStream fos = null;
    byte[] buffer = new byte[1024 * 1024];
    int bytesRead = 0;

    try {
      dhis = result.getDataHandler().getInputStream();
      fos = new FileOutputStream(reportFile);

      while ((bytesRead = dhis.read(buffer)) != -1) {
	fos.write(buffer, 0, bytesRead);
      }
    } finally {
      if (dhis != null) {
	try {
	  dhis.close();
	} catch (IOException ioe) {
	  System.out
	      .println("Exception caught closing DataHandler input stream.");
	}
      }

      if (fos != null) {
	fos.flush();
	fos.close();
      }
    }
  }
}
