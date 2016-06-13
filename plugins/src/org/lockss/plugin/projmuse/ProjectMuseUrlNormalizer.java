/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * @since 1.67.5
 */
public class ProjectMuseUrlNormalizer extends HttpToHttpsUtil.BaseUrlHttpHttpsUrlNormalizer {

  private static final Logger log = Logger.getLogger(ProjectMuseUrlNormalizer.class);
  
  protected static final String VERSION_SUFFIX = "?v=";

  protected static Map<String, String> paths;
  
  /*
   * STATIC INITIALIZER
   */
  static {
    InputStream is = null;
    LineNumberReader lnr = null;
    try {
      is = ProjectMuseUrlNormalizer.class.getResourceAsStream("url_normalizer.dat.gz");
      if (is == null) {
        throw new ExceptionInInitializerError("Mapping file not found");
      }
      Map<String, String> map = new HashMap<String, String>();
      lnr = new LineNumberReader(new InputStreamReader(new GZIPInputStream(is), Constants.ENCODING_US_ASCII));
      for (String line = lnr.readLine() ; line != null ; line = lnr.readLine()) {
        int tab = line.indexOf('\t');
        if (tab < 0) {
          throw new IOException("Invalid mapping file format at line " + lnr.getLineNumber());
        }
        map.put(line.substring(0, tab), line.substring(tab + 1));
      }
      paths = map;
    }
    catch (IOException ioe) {
      throw new ExceptionInInitializerError(ioe);
    }
    finally {
      IOUtil.safeClose(lnr);
    }
  }
  
  @Override
  public String additionalNormalization(String url, ArchivalUnit au) throws PluginException {
    log.debug3("in: " + url);
    if (url == null) {
      return url;
    }    

    url = StringUtils.substringBeforeLast(url, VERSION_SUFFIX);
    
    final String musehost = "://muse.jhu.edu/";
    int ix = url.indexOf(musehost);
    if (ix >= 0) {
      int slash = ix + musehost.length() - 1;
      String path = url.substring(slash);
      if (path.endsWith("/")) {
        path = path.substring(0, path.length() - 1);
      }
      String oldPath = paths.get(path);
      if (oldPath != null) {
        url = url.substring(0, slash) + oldPath;
      }
    }

    log.debug3("out: " + url);
    return url;
  }

}
