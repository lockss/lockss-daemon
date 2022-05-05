/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ojs2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;

public class OJS2PermissionCheckerFactory
  implements PermissionCheckerFactory{
  
  /*
   * So while this not really extending the ProbePermissionChecker
   * the ProbePermissionChecker does have the PermissionHelper pHelper param
   * rather than the new in 1.67 CrawlerFacade crawlFacade param
   */
  public class OJS2PermissionChecker implements PermissionChecker {
    
    private final Logger logger = Logger.getLogger(OJS2PermissionCheckerFactory.class);
    protected String au_year;
    protected Pattern au_year_paren;
    protected Pattern au_year_colon;
    
    private static final String CLOCKSS_FRAG = "about/editorialPolicies";
    
    public OJS2PermissionChecker(ArchivalUnit au) {
      super();
      au_year = au.getConfiguration().get(ConfigParamDescr.YEAR.getKey());
      au_year_paren = Pattern.compile("[(]" + au_year + "[)]");
      au_year_colon = Pattern.compile(":\\s+" + au_year + "[^0-9]");
    }
    
    @Override
    public boolean checkPermission(CrawlerFacade crawlFacade,
        Reader inputReader, String permissionUrl) throws CacheException {
      
      // if the permissionUrl is for CLOCKSS, then just return True
      // XXX FIXME replace the entire PremissionChecker with CrawlSeed?
      if (permissionUrl.contains(CLOCKSS_FRAG)) {
        return true;
      }
      
      BufferedReader in = new BufferedReader(inputReader);
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
  }
  
  @Override
  public List<OJS2PermissionChecker> createPermissionCheckers(ArchivalUnit au) {
    List<OJS2PermissionChecker> list = new ArrayList<OJS2PermissionChecker>(1);
    list.add(new OJS2PermissionChecker(au));
    return list;
  }
}

