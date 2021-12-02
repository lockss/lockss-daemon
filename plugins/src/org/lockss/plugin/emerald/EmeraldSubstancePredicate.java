/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.emerald;

import java.util.*;

//import org.apache.oro.text.regex.Pattern;
import org.lockss.plugin.*;
import org.lockss.state.SubstanceChecker.UrlPredicate;
import org.lockss.state.SubstanceChecker;
import org.lockss.util.Logger;
import org.lockss.util.Constants;
import org.lockss.util.SetUtil;
import org.lockss.util.HeaderUtil;

/**
 * EmeraldSubstancePredicate goes beyond just checking for
 * a matching substance pattern; 
 * additionally checks if the "substantial" url matches its mime-type
 *
 */

public class EmeraldSubstancePredicate implements SubstancePredicate {
  static Logger log; 
  private ArchivalUnit au;
  private UrlPredicate up = null;
  // Assuming that Emerald substance is only pdf files; otherwise, additional substance mime types go here
  private static final Set EM_SUBSTANCE_TYPES = SetUtil.set(Constants.MIME_TYPE_PDF);

  public EmeraldSubstancePredicate (ArchivalUnit au) {
    log = Logger.getLogger("EmeraldSubstancePredicate");
    this.au = au;
    // add substance rules to check against
    try {
      up = new SubstanceChecker.UrlPredicate(au, au.makeSubstanceUrlPatterns(), au.makeNonSubstanceUrlPatterns());

    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Error in substance or non-substance pattern for Emerald Plugin", e);
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
      boolean res = cu.hasContent() && EM_SUBSTANCE_TYPES.contains(mime);
      if (log.isDebug3()) {
	log.debug3("MimeType: " + mime + "\t Returning: "+ res);
      }
      return res;
    } finally {
      AuUtil.safeRelease(cu);
    }
  }
}
