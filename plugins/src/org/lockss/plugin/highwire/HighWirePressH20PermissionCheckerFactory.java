/*
 * $Id: HighWirePressH20PermissionCheckerFactory.java,v 1.5 2014-06-19 20:26:30 aishizaki Exp $
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.crawler.BaseCrawler;
import org.lockss.crawler.BaseCrawler.StorePermissionScheme;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.PermissionHelper;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.*;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.MonitoringReader;
import org.lockss.util.ReaderInputStream;
import org.lockss.util.StringUtil;

public class HighWirePressH20PermissionCheckerFactory
  implements PermissionCheckerFactory{
  
  protected static final Pattern H20_PATTERN = Pattern.compile("/content/.+[.]long$");
  
  public class H20ProbePermissionChecker extends ProbePermissionChecker {
    
    private final Logger logger = Logger.getLogger(H20ProbePermissionChecker.class);
    protected ArchivalUnit au;
    protected String probeUrl;
    
    @Override
    public boolean checkPermission(PermissionHelper pHelper, Reader inputReader,
        String permissionUrl) {
      
      //BufferedReader in = new BufferedReader(inputReader); 
      // as per Tom's instruction for PD-1088
      MonitoringReader in = new MonitoringReader(new BufferedReader(inputReader), "H20ProbePermissionChecker", true);

      // FIXME super_checkPermission should be super.checkPermission when 
      // probeUrl and au are visible to child class
      boolean ret = super_checkPermission(pHelper, in, permissionUrl);
      if (ret) {
        try {
          in.reset();
          ret = !StringUtil.containsString(in, "platform = DRUPAL", true);
        } catch (IOException e) {
          logger.warning("drupal flag", e);
        }
      }
      if (ret && probeUrl != null) {
        Matcher urlMat = H20_PATTERN.matcher(probeUrl);
        if (!urlMat.find()) {
          logger.siteError(" ");
          logger.siteError("       ===============        ");
          logger.siteError(" ");
          logger.siteError(probeUrl + " was not an H20 form url ");
          logger.siteError("     " + au.getUrlStems() + "content/<vol>/<iss>/<pg>.long) on ");
          logger.siteError(permissionUrl);
          logger.siteError(" ");
          logger.siteError("       ===============        ");
          logger.siteError(" ");
          ret = false;
        }
      }
      return ret;
    }
    
    public H20ProbePermissionChecker(LoginPageChecker checker, ArchivalUnit au) {
      super(checker, au);
      this.au = au;
    }
    
    public H20ProbePermissionChecker(ArchivalUnit au) {
      super(au);
      this.au = au;
    }
    
    // FIXME remove super_checkPermission when probeUrl and au are visible to child class
    protected boolean super_checkPermission(Crawler.PermissionHelper pHelper,
        Reader inputReader, String permissionUrl) {
      probeUrl = null;
      CustomHtmlLinkExtractor extractor = new CustomHtmlLinkExtractor();
      logger.debug3("Checking permission on "+permissionUrl);
      try {
        extractor.extractUrls(au, new ReaderInputStream(inputReader), null,
            permissionUrl, new MyLinkExtractorCallback());
      } catch (IOException ex) {
        logger.error("Exception trying to parse permission url "+permissionUrl,
            ex);
        return false;
      }
      if (probeUrl != null) {
        logger.debug3("Found probeUrl "+probeUrl);
        BufferedInputStream is = null;
        try {
          UrlCacher uc = pHelper.makePermissionUrlCacher(probeUrl);
          is = new BufferedInputStream(uc.getUncachedInputStream());
          logger.debug3("Non-login page: " + probeUrl);
          
          // Retain compatibility with legacy behavior of not storing probe
          // permission pages.
          Configuration config = ConfigManager.getCurrentConfig();
          StorePermissionScheme sps =
              (StorePermissionScheme)config.getEnum(StorePermissionScheme.class,
                  BaseCrawler.PARAM_STORE_PERMISSION_SCHEME,
                  BaseCrawler.DEFAULT_STORE_PERMISSION_SCHEME);
          if (StorePermissionScheme.Legacy != sps) {
            pHelper.storePermissionPage(uc, is);
          }
          return true;
        } catch (org.lockss.util.urlconn.CacheException.PermissionException ex) {
          logger.debug3("Found a login page");
          return false;
        } catch (IOException ex) {
          logger.error("Exception trying to check for login page "+probeUrl, ex);
          return false;
        } finally {
          IOUtil.safeClose(is);
        } 
      } else {
        logger.warning("Didn't find a probe URL on "+permissionUrl);
      }
      return false;
    }
    
    // FIXME remove CustomHtmlLinkExtractor when probeUrl and au are visible to child class
    private class CustomHtmlLinkExtractor
    extends GoslingHtmlLinkExtractor {
      
      private static final String LOCKSSPROBE = "lockss-probe";
      
      protected String extractLinkFromTag(StringBuffer link, ArchivalUnit au,
          LinkExtractor.Callback cb) {
        String returnStr = null;
        
        switch (link.charAt(0)) {
          case 'l': //<link href=blah.css>
          case 'L':
            logger.debug3("Looking for probe in "+link);
            if (beginsWithTag(link, LINKTAG)) {
              returnStr = getAttributeValue(HREF, link);
              String probeStr = getAttributeValue(LOCKSSPROBE, link);
              if (!"true".equalsIgnoreCase(probeStr)) {
                returnStr = null;
              }
            }
            break;
          default:
            return null;
        }
        return returnStr;
      }
    }
    
    // FIXME remove MyLinkExtractorCallback when probeUrl and au are visible to child class
    private class MyLinkExtractorCallback implements LinkExtractor.Callback {
      public MyLinkExtractorCallback() {
      }
      
      public void foundLink(String url) {
        if (probeUrl != null) {
          logger.warning("Multiple probe URLs found on manifest page.  " +
              "Old: "+probeUrl+" New: "+url);
        }
        probeUrl = url;
      }
    }
  }
  
  public List<H20ProbePermissionChecker> createPermissionCheckers(ArchivalUnit au) {
    List<H20ProbePermissionChecker> list = new ArrayList<H20ProbePermissionChecker>(1);
    list.add(new H20ProbePermissionChecker(au));
    return list;
  }
}

