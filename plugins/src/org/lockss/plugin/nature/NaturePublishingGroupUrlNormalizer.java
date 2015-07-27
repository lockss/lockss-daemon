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

package org.lockss.plugin.nature;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class NaturePublishingGroupUrlNormalizer implements UrlNormalizer {
  protected static final Logger log = 
      Logger.getLogger(NaturePublishingGroupUrlNormalizer.class);  
  
  //"message=remove" argument and variants; normalize out
  // example: http://www.nature.com/am/journal/v5/n11/suppinfo/am201360s1.html?message=remove&url=/am//journal/v5/n11/abs/am201360a.html
  // should become:
  // http://www.nature.com/am/journal/v5/n11/suppinfo/am201360s1.html?url=/am//journal/v5/n11/abs/am201360a.html
  // if it's the ending argument, just remove it along with the preceeding & or the ?
  protected static final Pattern MSG_ENDING_PATTERN = Pattern.compile("(&|\\?)message(-global)?=remove$");
  // if it's not the end it will have an ampersand following that needs removal as well
  protected static final Pattern MSG_MIDDLE_PATTERN = Pattern.compile("message(-global)?=remove&");
  
  // some very early crawls picked up the issue TOC at both:
  //     <foo>/journal/v109/n6/index.html and
  //     <foo>/journal/v109/n6   
  // now the latter is just a path to the "index.html" and we can't reach agreement with older crawls
  // Normalize the path version to the index version
  protected static final Pattern ISSUE_TOC_PATTERN = Pattern.compile("journal/v[0-9]+/n[0-9]{1,2}$");
 
  public String normalizeUrl(String url,
                             ArchivalUnit au)
      throws PluginException {
    
    // make table of contents end with "index.html" 
    Matcher tocMat = ISSUE_TOC_PATTERN.matcher(url);
    if (tocMat.find()) {
      return url + "/index.html";  
    }
    
    // don't waste time if there is no ? argument
    if (url.contains("?")) {
      // if it's at the end, simply take it off
      Matcher mat = MSG_ENDING_PATTERN.matcher(url);
      if (mat.find()) {
        return url.substring(0, mat.start());
      } else {
         // special case if there are additional argument - manage ?& issues 
        Matcher midmat = MSG_MIDDLE_PATTERN.matcher(url);
        if (midmat.find()) {
          return midmat.replaceFirst("");
        }
      }
    }
    return url;
  }

}
