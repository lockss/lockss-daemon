/*
 * $Id:  $
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

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWireDrupalUrlNormalizer;
import org.lockss.util.Logger;

public class BMJDrupalUrlNormalizer extends HighWireDrupalUrlNormalizer {
  
  private static final Logger log = Logger.getLogger(BMJDrupalUrlNormalizer.class);
  
  // http://static.www.bmj.com/content/bmj/347/bmj.f6041/F1.large.jpg becomes
  // http://www.bmj.com/content/bmj/347/bmj.f6041/F1.large.jpg
  private static final String PREFIX = "static.www.bmj.com/content/";
  private static final String REPLACE = "www.bmj.com/content/";
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    
    if (url.contains(PREFIX)) {
      log.debug(url);
      url = url.replace(PREFIX, REPLACE);
      log.debug(url);
    }
    
    return super.normalizeUrl(url, au);
  }
  
}
