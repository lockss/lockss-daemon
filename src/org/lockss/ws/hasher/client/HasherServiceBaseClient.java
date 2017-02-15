/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.hasher.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.ws.AllServicesBaseClient;
import org.lockss.ws.entities.HasherWsResult;
import org.lockss.ws.hasher.HasherService;

/**
 * A base client for the Hasher web service.
 */
public abstract class HasherServiceBaseClient extends AllServicesBaseClient {
  private static final String ADDRESS_LOCATION =
      "http://localhost:8081/ws/HasherService?wsdl";
  private static final String TARGET_NAMESPACE =
      "http://hasher.ws.lockss.org/";
  private static final String SERVICE_NAME = "HasherServiceImplService";

  private static final String TIMEOUT_KEY =
      "com.sun.xml.internal.ws.request.timeout";

  // The length of the client connection timeout in seconds.
  private static final int TIMEOUT_VALUE = 600;

  /**
   * Provides the proxy to the hasher service.
   * @return a HasherService with the proxy to the hasher service.
   * 
   * @throws Exception
   */
  protected HasherService getProxy() throws Exception {
    super.authenticate();
    Service service = Service.create(new URL(ADDRESS_LOCATION), new QName(
	TARGET_NAMESPACE, SERVICE_NAME));

    HasherService port = service.getPort(HasherService.class);

    // Set the client connection timeout.
    ((javax.xml.ws.BindingProvider) port).getRequestContext().put(TIMEOUT_KEY,
	new Integer(TIMEOUT_VALUE*1000));

    return port;
  }

  /**
   * Writes to the filesystem any files received with the result of an
   * operation.
   * 
   * @param result
   *          A HasherWsResult with the web service operation result.
   * @param tempDirName
   *          A String with the name of the temporary directory where to write
   *          the files.
   * @throws IOException
   */
  protected void handleResultFiles(HasherWsResult result, String tempDirName)
      throws IOException {
    String recordFileName = result.getRecordFileName();
    DataHandler recordFileDataHandler = result.getRecordFileDataHandler();

    if (recordFileName != null && recordFileDataHandler != null) {
      writeFile(recordFileName, recordFileDataHandler, tempDirName);
    }

    String blockFileName = result.getBlockFileName();
    DataHandler blockFileDataHandler = result.getBlockFileDataHandler();

    if (blockFileName != null && blockFileDataHandler != null) {
      writeFile(blockFileName, blockFileDataHandler, tempDirName);
    }
  }

  /**
   * Writes to the filesystem a file received with the result of an operation.
   * 
   * @param fileName
   *          A String with the name of the file.
   * @param dataHandler
   *          A DataHandler with the contents of the file.
   * @param dirName
   *          A String with the name of the directory where to write the file.
   * @throws IOException
   */
  private void writeFile(String fileName, DataHandler dataHandler,
      String dirName) throws IOException {
    File file = new File(dirName, fileName);
    System.out.println("file = " + file.getAbsolutePath());

    // Write the received file.
    InputStream dhis = null;
    FileOutputStream fos = null;
    byte[] buffer = new byte[1024 * 1024];
    int bytesRead = 0;

    try {
      dhis = dataHandler.getInputStream();
      fos = new FileOutputStream(file);

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
