/*
 * $Id$
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

package org.lockss.plugin.highwire;

import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.PermissionHelper;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class HighWirePermissionCheckerFactory
  implements PermissionCheckerFactory {
  
  protected static final Pattern H10_PATTERN = Pattern.compile("/cgi/content/");
  
  private class H10ProbePermissionChecker extends ProbePermissionChecker {
    
    private final Logger logger = Logger.getLogger(H10ProbePermissionChecker.class);
    
    @Override
    public boolean checkPermission(PermissionHelper pHelper, Reader inputReader,
        String permissionUrl) {
      
      // XXX probeUrl and au are visible to child class, since 1.66
      boolean ret = super.checkPermission(pHelper, inputReader, permissionUrl);
      logger.debug3("returned: " + ret + " probeUrl:" + probeUrl);
      if (ret && probeUrl != null) {
        Matcher urlMat = H10_PATTERN.matcher(probeUrl);
        if (!urlMat.find()) {
          logger.siteError(" ");
          logger.siteError("       ===============        ");
          logger.siteError(" ");
          logger.siteError(probeUrl + " was not an H10 form url ");
          logger.siteError("     " + au.getUrlStems() + "cgi/content/) on ");
          logger.siteError(permissionUrl);
          logger.siteError(" ");
          logger.siteError("       ===============        ");
          logger.siteError(" ");
          ret = false;
        }
      }
      return ret;
    }
    
    public H10ProbePermissionChecker(ArchivalUnit au) {
      super(au);
    }
    
  }
  
  public List<?> createPermissionCheckers(ArchivalUnit au) {
    List<PermissionChecker> list = new ArrayList<PermissionChecker>(1);
    list.add(new H10ProbePermissionChecker(au));
    return list;
  }
}
