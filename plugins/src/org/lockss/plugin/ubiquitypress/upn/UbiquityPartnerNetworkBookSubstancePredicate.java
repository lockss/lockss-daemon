/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubstancePredicate;
import org.lockss.state.SubstanceChecker.UrlPredicate;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;

/**
 * UTSePressSubstancePredicate goes beyond just checking for
 * a matching substance pattern; 
 * additionally checks if the "substantial" url matches its mime-type
 *
 */

public class UbiquityPartnerNetworkBookSubstancePredicate implements SubstancePredicate {
  static Logger log; 
  private ArchivalUnit au;
  private UrlPredicate up = null;
  // Assuming that UTSePress substance is only pdf files; otherwise, additional substance mime types go here
  //https://utsepress.lib.uts.edu.au/site/books/10.5130/978-0-6481242-8-3/download/3149
  // Content-Type: ('application/pdf', None), the above link will show the content type like this

  private static final String SUBSTANCE_STRING = "application/pdf";

  public UbiquityPartnerNetworkBookSubstancePredicate(ArchivalUnit au) {
    log = Logger.getLogger("UTSePressSubstancePredicate");
    this.au = au;
    // add substance rules to check against
    try {
      up = new UrlPredicate(au, au.makeSubstanceUrlPatterns(), au.makeNonSubstanceUrlPatterns());

    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Error in substance or non-substance pattern for UTSePress Plugin", e);
    }
  }
  
  /* (non-Javadoc)
   * isSubstanceUrl(String url)
   * checking that the "substantial" url matches its mime-type
   * @see org.lockss.plugin.SubstancePredicate#isSubstanceUrl(java.lang.String)
   * @Return false if the url does not match the substance pattern, or the mime-type does not match 
   * the content type
   * @Return true when url matches the pattern and the mime-type matches and has content
   */
  @Override
  public boolean isSubstanceUrl(String url) {
    // check url against substance rules for publisher
    if (log.isDebug3()) log.debug3("isSubstanceURL("+url+")");
    if ((up == null) || !( up.isMatchSubstancePat(url))) {
      return false;
    }
    CachedUrl cu = au.makeCachedUrl(url);
    if (cu == null) {     
      return false;
    }
    try {
      String mime = HeaderUtil.getMimeTypeFromContentType(cu.getContentType());
      boolean res = cu.hasContent() && mime.contains(SUBSTANCE_STRING);
      if (log.isDebug3()) {
	    log.debug3("MimeType: " + mime + "\t Returning: "+ res);
      }
      return res;
    } finally {
      AuUtil.safeRelease(cu);
    }
  }
}
