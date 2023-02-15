/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.projmuse;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;

/**
 * @since 1.67.5
 */
public class ProjectMuseUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {

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
