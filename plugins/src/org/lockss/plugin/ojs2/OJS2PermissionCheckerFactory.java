/*
 * $Id: OJS2PermissionCheckerFactory.java,v 1.1 2014-10-21 01:55:51 etenbrink Exp $
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

package org.lockss.plugin.ojs2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.PermissionHelper;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class OJS2PermissionCheckerFactory
  implements PermissionCheckerFactory{
  
  public class OJS2PermissionChecker implements PermissionChecker {
    
    private final Logger logger = Logger.getLogger(OJS2PermissionCheckerFactory.class);
    protected ArchivalUnit au;
    protected String au_year;
    protected Pattern au_year_paren;
    protected Pattern au_year_colon;
    static final int READ_LIMIT = 40960;
    
    @Override
    public boolean checkPermission(PermissionHelper pHelper, Reader inputReader,
        String permissionUrl) {
      
      BufferedReader in = new BufferedReader(inputReader, READ_LIMIT);
      boolean ret = false;
      try {
        String str = null;
        while (((str = in.readLine()) != null) && !ret) {
          Matcher m1 = au_year_paren.matcher(str);
          if (m1.find()) {
            ret = true;
          } else {
            Matcher m2 = au_year_colon.matcher(str);
            if (m2.find()) {
              ret = true;
            }
          }
        }
        if (!ret) {
          logger.siteError(" ");
          logger.siteError("       ===============        ");
          logger.siteError(" ");
          logger.siteError( "did not find au_year (" + au_year + ") on manifest page");
          logger.siteError(" ");
          logger.siteError("       ===============        ");
          logger.siteError(" ");
        }
      } catch (IOException e) {
        logger.warning("IOException checking for AU year", e);
      }
      return ret;
    }
    
    public OJS2PermissionChecker(ArchivalUnit au) {
      this.au = au;
      au_year = au.getConfiguration().get(ConfigParamDescr.YEAR.getKey());
      au_year_paren = Pattern.compile("[(]" + au_year + "[)]");
      au_year_colon = Pattern.compile(":\\s+" + au_year + "[^0-9]");
    }
  }
  
  public List<OJS2PermissionChecker> createPermissionCheckers(ArchivalUnit au) {
    List<OJS2PermissionChecker> list = new ArrayList<OJS2PermissionChecker>(1);
    list.add(new OJS2PermissionChecker(au));
    return list;
  }
}

