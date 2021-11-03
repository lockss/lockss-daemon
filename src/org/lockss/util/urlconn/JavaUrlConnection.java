/*
 * $Id$
 *

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util.urlconn;

import java.util.*;
import java.io.*;
import java.net.*;
import org.lockss.util.*;

/** Encapsulates Java's native URLConnection implementation as a
 * LockssUrlConnection.  Used for all non-http(s) connections.
 */
public class JavaUrlConnection extends BaseLockssUrlConnection {
  private static Logger log = Logger.getLogger("JavaUrlConn");

  private URLConnection urlConn;

  public JavaUrlConnection(String urlString) throws IOException {
    super(urlString);
    init(new URL(urlString));
  }

  public JavaUrlConnection(URL url) throws IOException {
    super(url.toString());
    init(url);
  }

  private void init(URL url) throws IOException {
    urlConn = url.openConnection();
  }

  protected URLConnection getURLConnection() {
    return urlConn;
  }

  public boolean isHttp() {
    return false;
  }

  public void execute() throws IOException {
    assertNotExecuted();
    isExecuted = true;
    urlConn.connect();
  }

  /** Return the URL
   * @return the URL
   */
  public String getURL() {
    return urlConn.getURL().toString();
  }

  public boolean canProxy() {
    return false;
  }

  public void setRequestProperty(String key, String value) {
    assertNotExecuted();
    urlConn.setRequestProperty(key, value);
  }

  public void setRequestIfModifiedSince(long ifmodifiedsince) {
    urlConn.setIfModifiedSince(ifmodifiedsince);
  }

  public void setRequestEntity(String entity) {
    throw new UnsupportedOperationException();
  }

  public int getResponseCode() {
    throw new UnsupportedOperationException();
  }

  public String getResponseMessage() {
    throw new UnsupportedOperationException();
  }

  public String getResponseHeaderFieldVal(int n) {
    assertExecuted();
    return urlConn.getHeaderField(n);
  }

  public String getResponseHeaderFieldKey(int n) {
    assertExecuted();
    return urlConn.getHeaderFieldKey(n);
  }

  public long getResponseDate() {
    assertExecuted();
    return urlConn.getDate();
  }

  /**
   * Returns the value of the <code>content-length</code> header field.
   * @return  the content length, or -1 if not known.
   */
  public long getResponseContentLength() {
    assertExecuted();
    return urlConn.getContentLength();
  }

  /**
   * Returns the value of the <code>content-type</code> header field.
   * @return  the content type, or null if not known.
   */
  public String getResponseContentType() {
    assertExecuted();
    return urlConn.getContentType();
  }
  
  /** 
   * Return the last-modified: from the response header.
   * @return the last-modified header response
   */
  public long getResponseLastModified() {
    assertExecuted();
    return urlConn.getLastModified();
  }

  /**
   * Returns the value of the <code>content-encoding</code> header field.
   * @return  the content encoding, or null if not known.
   */
  public String getResponseContentEncoding() {
    assertExecuted();
    return urlConn.getContentEncoding();
  }

  /**
   * Returns the value of the specified header field.
   * @param name the name of a header field.
   * @return the value of the named header field, or null if there is no
   * such field in the header.
   */
  public String getResponseHeaderValue(String name) {
    assertExecuted();
    return urlConn.getHeaderField(name);
  }

  public InputStream getResponseInputStream() throws IOException {
    assertExecuted();
    return urlConn.getInputStream();
  }

  public void storeResponseHeaderInto(Properties props, String prefix) {
    // store all header properties (this is the only way to iterate)
    for (int ix = 0; true; ix++) {
      String key = urlConn.getHeaderFieldKey(ix);
      String value = urlConn.getHeaderField(ix);
      if ((key==null) && (value==null)) {
        // the first header field has a null key, so we can't break just on key
        break;
      }
      if (value!=null) {
        // only store headers with values
        // qualify header names to avoid conflict with our properties
	String propKey = (key != null) ? key : ("header_" + ix);
	if (prefix != null) {
	  propKey = prefix + propKey;
	}
	props.setProperty(prefix, value);
      }
    }
  }

  /**
   * Release resources associated with this request.
   */
  public void release() {
  }

  /**
   * Abort the request.
   */
  public void abort() {
  }

}
