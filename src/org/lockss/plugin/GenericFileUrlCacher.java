/*
 * $Id: GenericFileUrlCacher.java,v 1.2 2002-10-24 02:16:36 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;

/**
 * This is an abstract file implementation of UrlCacher.
 * The source for the content needs to be provided in any extension.
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public abstract class GenericFileUrlCacher extends BaseUrlCacher {
  /**
   * Name of top directory in which the urls are cached.
   */
  public static final String CACHE_ROOT = "cache";

  public GenericFileUrlCacher(CachedUrlSet owner, String url) {
    super(owner, url);
  }

  public void storeContent(InputStream input, Properties headers) throws IOException {
    if (input != null) {
      File file = new File(mapUrlToCacheFileName());
      File parentDir = file.getParentFile();
      if (!parentDir.exists()) {
	parentDir.mkdirs();
      }
      OutputStream os = new FileOutputStream(file);
      StreamUtil.copy(input, os);
      os.close();
      input.close();
    }
    if (headers!=null) {
      File file = new File(mapUrlToCacheFileName() + ".props");
      File parentDir = file.getParentFile();
      if (!parentDir.exists()) {
        parentDir.mkdirs();
      }
      OutputStream os = new FileOutputStream(file);
      headers.store(os, "HTTP headers for " + url);
      os.close();
    }
  }

  public abstract InputStream getUncachedInputStream();

  public abstract Properties getUncachedProperties();

  /**
   * mapUrlToCacheFileName() is the name mapping method used by the GenericFileUrlCacher.
   * This maps a given url to a cache file location.
   * It is also used by the GenericFileCachedUrl to extract the content.
   * It creates directories under a CACHE_ROOT location which mirror the html string.
   * So 'http://www.journal.org/issue1/index.html' would be cached in the file:
   * CACHE_ROOT/www.journal.org/http/issue1/index.html
   */
  public static String mapUrlToCacheFileName(String url) {
    int idx = url.indexOf("://");
    String fileName = CACHE_ROOT + File.separator;
    if (idx>=0) {
      String prefix = url.substring(0, idx);
      String urlRemainder = url.substring(idx+3);
      idx = urlRemainder.indexOf("/");
      if (idx>=0) {
        fileName += urlRemainder.substring(0, idx) + File.separator;
        fileName += prefix + urlRemainder.substring(idx);
      } else {
        fileName += urlRemainder;
      }
    } else {
      fileName += url;
    }
    return fileName;
  }

  private String mapUrlToCacheFileName() {
    return mapUrlToCacheFileName(url);
  }
}

