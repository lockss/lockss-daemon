/*
 * $Id$
 */

/*

 Copyright (c) 2014-2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.entities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.activation.DataHandler;

/**
 * A wrapper for the result of a Content web service operation.
 */
public class ContentResult {
  private DataHandler dataHandler;
  private Properties properties;

  /**
   * Provides the requested content.
   *
   * @return a DataHandler through which to obtain the content.
   */
  public DataHandler getDataHandler() {
    return dataHandler;
  }

  public void setDataHandler(DataHandler dataHandler) {
    this.dataHandler = dataHandler;
  }

  /**
   * Provides the properties of the requested content.
   *
   * @return a Properties with the properties of the content.
   */
  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  /**
   * Writes the result content to a file.
   * 
   * @param outputFileSpec
   *          A String with the specification of the file where to write the
   *          result content.
   * @return a File containing the result content.
   * @throws IOException
   */
  public File writeContentToFile(String outputFileSpec) throws IOException {
    File outputFile = new File(outputFileSpec);

    // Write the received file.
    InputStream dhis = null;
    FileOutputStream fos = null;
    byte[] buffer = new byte[1024 * 1024];
    int bytesRead = 0;

    try {
      dhis = dataHandler.getInputStream();
      fos = new FileOutputStream(outputFile);

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

    return outputFile;
  }

  @Override
  public String toString() {
    return ("[CounterResult properties=" + properties + "]");
  }
}
