/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class HighWireUrlNormalizer implements UrlNormalizer {
  
  protected static final Logger log = Logger.getLogger(HighWireUrlNormalizer.class);
  protected static final String TOC_SUFFIX = ".toc";
  protected static final Pattern H20_LINK = Pattern.compile("(https?://[^/]+)/content/([^/]+)/([^/]+)[.]toc");
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {
    
    // map H20 links on manifest pages to H10 links
    // <base_url>content/168/1.toc to <base_url>content/vol168/issue1/
    
    if (url.endsWith(TOC_SUFFIX)) {
      Matcher mat = H20_LINK.matcher(url);
      if (mat.matches()) {
        url = mat.group(1) + "/content/vol" + mat.group(2) + "/issue" + mat.group(3) + "/";
      }
    }
    
    return(url);
  }
}
