/*
 * $Id: GeorgThiemeVerlagUrlNormalizer.java,v 1.1 2014-02-27 21:04:13 etenbrink Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.georgthiemeverlag;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class GeorgThiemeVerlagUrlNormalizer implements UrlNormalizer {
  
  protected static Logger log = Logger.getLogger(GeorgThiemeVerlagUrlNormalizer.class);
  protected static final String PARAM_SUFFIX = "?issue=";
  
  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {
    // map 
    // https://www.thieme-connect.de/ejournals/html/10.1055/s-0032-1315814?issue=10.1055/s-003-25342
    // to https://www.thieme-connect.de/ejournals/html/10.1055/s-0032-1315814
    
    if (url.contains(PARAM_SUFFIX)) {
      url = url.replaceFirst("[?].+$", "");
    }
    
    return(url);
    
  }
  
}
