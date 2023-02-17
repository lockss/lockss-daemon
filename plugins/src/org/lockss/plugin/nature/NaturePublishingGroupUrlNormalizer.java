/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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
