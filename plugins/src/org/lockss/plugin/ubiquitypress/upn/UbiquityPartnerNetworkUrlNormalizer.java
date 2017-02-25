/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.ubiquitypress.upn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.BaseUrlHttpHttpsUrlNormalizer;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class UbiquityPartnerNetworkUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  private static final Logger log = Logger.getLogger(UbiquityPartnerNetworkUrlNormalizer.class);

  protected String baseUrl;
  
  protected String journalId;
  // this pattern matches closely the allowed issue toc pattern in the crawl rules
  // group1 is the base_url
  private static final Pattern ISSUE_TOC_PAT = Pattern.compile("(https?://[^/]+/)[^/]+/volume/[^/]+/issue/[^/]+/$",Pattern.CASE_INSENSITIVE);
  private static final String MANIFEST_PATH = "lockss/year/";
  
  @Override
   public String normalizeUrl(String url,ArchivalUnit au)
      throws PluginException {
    if (url.endsWith("print/")) {
      return url.replace("print/", "");
    }
    /*
     * Manifest pages have issue TOC links on them that we do not follow because
     * a) the articles have direct links from the landing page and
     * b) sometimes the issue TOC links are incorrect or missing
     * BUT - in the case of a trigger this means 404 links front and center of the start page
     * so accept the link to the issue TOC but turn it back in to the manifest page
     * making it an actionless link - better than a 404
     * https://www.xyz.nl/20/volume/0/issue/13/
     * becomes
     * https://www.xyz.nl/lockss/year/2014/
     *  (the year, unfortunately, has to come from the au param)
     * 
     */
    Matcher tocMat = ISSUE_TOC_PAT.matcher(url);
    if (tocMat.matches()) {
      String baseUrl = tocMat.group(1);
      String year = au.getConfiguration().get(ConfigParamDescr.YEAR.getKey());
      String newurl = baseUrl + MANIFEST_PATH + year + "/";
      log.debug3("turning toc url " + url + " in to " + newurl);
      return newurl;
    }
    return url;
  }
}
