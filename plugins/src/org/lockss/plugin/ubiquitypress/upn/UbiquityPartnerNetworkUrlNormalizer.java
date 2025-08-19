/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ubiquitypress.upn;

import java.util.Arrays;
import java.util.regex.*;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

// subclass the BaseUrlHttpHttpsUrlNormalizer - which redirects protocol to that of declared base_url
// and then does whatever specific "additionalNormalization that is specified
public class UbiquityPartnerNetworkUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  private static Logger log = Logger.getLogger(UbiquityPartnerNetworkUrlNormalizer.class);
  
  protected static Pattern CSS_WITH_DATE =
      Pattern.compile("\\.css(?:\\?|%3F)\\d{4}-\\d{1,2}-\\d{1,2}$",
                      Pattern.CASE_INSENSITIVE);
  protected static Pattern PNG_WITH_TIMESTAMP =
      Pattern.compile("\\.png(?:\\?|%3F)t(?:=|%3D)\\d+$",
                      Pattern.CASE_INSENSITIVE);
  protected static Pattern PNG_WITH_TIMESTAMP_AND_WIDTH =
      Pattern.compile("\\.png(?:\\?|%3F)t(?:=|%3D)\\d+(?:&|%26|&amp;)w=",
                      Pattern.CASE_INSENSITIVE);
  protected static Pattern JPG_WITH_TIMESTAMP =
      Pattern.compile("\\.jpg(?:\\?|%3F)t(?:=|%3D)\\d+$",
                      Pattern.CASE_INSENSITIVE);
  protected static Pattern JPG_WITH_TIMESTAMP_AND_WIDTH =
      Pattern.compile("\\.jpg(?:\\?|%3F)t(?:=|%3D)\\d+(?:&|%26|&amp;)w=",
                      Pattern.CASE_INSENSITIVE);

  @Override
   public String additionalNormalization(String url,ArchivalUnit au)
      throws PluginException {
    /*
     * 'print/':
     * 
     * Make the print version equivalent to the full text HTML version.
     * 
     * '?action=download':
     * 
     * Make the "original" link of each figure refer to the image itself. Example:
     * 
     * https://www.gewina-studium.nl/articles/10.18352/studium.10120/figures/Cocquyt_fig1.jpg?action=download
     * ->
     * https://www.gewina-studium.nl/articles/10.18352/studium.10120/figures/Cocquyt_fig1.jpg
     */
    for (String suffix : Arrays.asList("print/",
                                       "?action=download")) {
      url = StringUtils.removeEnd(url, suffix);
    }
    
    /*
     * Various .css URLs have a date appended, e.g. .css?2020-12-02
     */
    url = CSS_WITH_DATE.matcher(url).replaceFirst(".css");
    /*
     * Various .png and .jpg URLs have a timestamp, e.g. .png?t=1745443500000 or .jpg?t=1745443500000 or .jpg?t=1745443500000&w=100
     */
    url = PNG_WITH_TIMESTAMP.matcher(url).replaceFirst(".png");
    url = PNG_WITH_TIMESTAMP_AND_WIDTH.matcher(url).replaceFirst(".png?w=");
    url = JPG_WITH_TIMESTAMP.matcher(url).replaceFirst(".jpg");
    url = JPG_WITH_TIMESTAMP_AND_WIDTH.matcher(url).replaceFirst(".jpg?w=");
    return url;
  }
}
