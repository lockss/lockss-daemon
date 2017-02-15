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
package org.lockss.ws.export.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import org.lockss.ws.entities.DataHandlerWrapper;
import org.lockss.ws.entities.ExportServiceParams;
import org.lockss.ws.entities.ExportServiceParams.TypeEnum;
import org.lockss.ws.entities.ExportServiceWsResult;
import org.lockss.ws.export.ExportService;

/*
 * @author Ahmed AlSum
 */
public class ExportFilesClient extends ExportServiceBaseClient {
  private static final String USER_NAME = "lockss-u";
  private static final String PASSWORD = "lockss-p";

  /**
   * The main method.
   * 
   * @param args
   *          A String[] with the command line arguments.
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    ExportService proxy = new ExportFilesClient().getProxy();
    ExportServiceParams exportParam = new ExportServiceParams();
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
	return new PasswordAuthentication(USER_NAME, PASSWORD.toCharArray());
      }
    });
    String auId = null;
    int argCount = args.length;

    String fileTypeArg = "";
    String maxSizeFile = "";
    String compress = "";
    switch (argCount) {
    case 0:
      System.out.println("Error: No AuId specified");
      System.exit(1);
    case 1:
      auId = args[0];
      break;
    case 2:
      auId = args[0];
      fileTypeArg = args[1];
      break;
    case 3:
      auId = args[0];
      fileTypeArg = args[1];
      maxSizeFile = args[2];
      break;
    case 4:
      auId = args[0];
      fileTypeArg = args[1];
      maxSizeFile = args[2];
      compress = args[3];
      break;
    }

    System.out.println("auId = " + auId);
    System.out.println("fileType = " + fileTypeArg);
    System.out.println("maxSizeFile = " + maxSizeFile);
    System.out.println("compress = " + compress);

    exportParam.setAuid(auId);
    if ("zip".equalsIgnoreCase(fileTypeArg)) {
      exportParam.setFileType(TypeEnum.ZIP);
    } else if ("warc".equalsIgnoreCase(fileTypeArg)) {
      exportParam.setFileType(TypeEnum.WARC_RESOURCE);
    } else if ("arc".equalsIgnoreCase(fileTypeArg)) {
      exportParam.setFileType(TypeEnum.ARC_RESOURCE);
    }
    if (maxSizeFile.length() > 0) {
      exportParam.setMaxSize(Long.parseLong(maxSizeFile));
    }
    if ("false".equalsIgnoreCase(compress)) {
      exportParam.setCompress(false);
    } else if ("true".equalsIgnoreCase(compress)) {
      exportParam.setCompress(true);
    }
    ExportServiceWsResult result = proxy.createExportFiles(exportParam);
    for (int i = 0; i < result.getDataHandlerWrappers().length; i++) {
      DataHandlerWrapper handler = result.getDataHandlerWrappers()[i];
      FileOutputStream outputStream = new FileOutputStream(new File(
	  handler.getName()));
      System.out.println("Writing " + handler.getName());

      int read = 0;
      byte[] bytes = new byte[1024];
      InputStream inputStream = handler.getDataHandler().getInputStream();
      while ((read = inputStream.read(bytes)) != -1) {
	outputStream.write(bytes, 0, read);
      }
      outputStream.close();
      inputStream.close();
    }
  }
}
