/*
 * $Id: UrlUtil.java,v 1.1 2002-08-31 06:55:04 tal Exp $
 *

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;
import java.io.*;
import java.net.*;

/** Utilities for URLs
 */
public class UrlUtil {
  /** Return input stream for url iff 200 response code, else throw.
   * In Java 1.1.7, URL.openStream() returns an InputStream in some cases
   * where it should throw, e.g., a 403 response on a filename that
   * ends in ".txt".
   * <br>In Java 1.3 and later this should not be necessary, as an
   * IOException is thrown in all the right cases.  But there's no harm
   * in continuing to use it, and it may be handy in the future.
   */
  public static InputStream openInputStream(String urlString) 
      throws IOException {
    URL url = new URL(urlString);
    URLConnection uc = url.openConnection();
    if (!(uc instanceof HttpURLConnection)) {
      return uc.getInputStream();
    }
    HttpURLConnection huc = (HttpURLConnection)uc;
    int rc = huc.getResponseCode();
    if (rc == HttpURLConnection.HTTP_OK) {
      return huc.getInputStream();
    } else {
      throw new IOException("Server returned HTTP response code: " + rc +
			    " for URL: " + urlString);
    }
  }
}
