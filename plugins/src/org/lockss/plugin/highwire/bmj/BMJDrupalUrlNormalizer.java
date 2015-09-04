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

package org.lockss.plugin.highwire.bmj;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWireDrupalUrlNormalizer;
import org.lockss.util.Logger;

public class BMJDrupalUrlNormalizer extends HighWireDrupalUrlNormalizer {
  
  private static final Logger log = Logger.getLogger(BMJDrupalUrlNormalizer.class);
  
  // http://static.www.bmj.com/content/bmj/347/bmj.f6041/F1.large.jpg becomes
  // http://www.bmj.com/content/bmj/347/bmj.f6041/F1.large.jpg
  public static final String BMJ_UN_STATIC = "/static.";
  public static final Pattern BMJ_UN_PREFIX_PAT = Pattern.compile("static[^/]*[.]www[.]bmj[.]com/content/");
  public static final String BMJ_UN_REPLACE = "www.bmj.com/content/";
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    
    if (url.contains(BMJ_UN_STATIC) && url.contains(BMJ_UN_REPLACE)) {
      Matcher mat = BMJ_UN_PREFIX_PAT.matcher(url);
      if (mat.find()) {
        url = mat.replaceFirst(BMJ_UN_REPLACE);
      }
    }
    
    return super.normalizeUrl(url, au);
  }
  
}
