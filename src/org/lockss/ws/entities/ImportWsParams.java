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
package org.lockss.ws.entities;

import java.util.Arrays;
import javax.activation.DataHandler;

/**
 * A wrapper for the parameters used to perform an import operation.
 */
public class ImportWsParams {
  private String sourceUrl;
  private DataHandler dataHandler;
  private String targetId;
  private String targetUrl;
  private String[] properties;

  /**
   * Provides the URL of the source to be imported.
   * 
   * @return a String with the URL.
   */
  public String getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

  /**
   * Provides the pushed file to be imported.
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
   * Provides the identifier of the target where to store the imported data.
   * 
   * @return a String with the identifier.
   */
  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  /**
   * Provides the URL of the target location where to store the imported data.
   * 
   * @return a String with the URL.
   */
  public String getTargetUrl() {
    return targetUrl;
  }

  public void setTargetUrl(String targetUrl) {
    this.targetUrl = targetUrl;
  }

  /**
   * Provides the additional properties of the import operation.
   * 
   * @return a String[] with the properties.
   */
  public String[] getProperties() {
    return properties;
  }

  public void setProperties(String[] properties) {
    this.properties = properties;
  }

  @Override
  public String toString() {
    return "[ImportWsParams: sourceUrl=" + sourceUrl + ", targetId=" + targetId
	+ ", targetUrl=" + targetUrl + ", properties="
	+ Arrays.toString(properties) + "]";
  }
}
