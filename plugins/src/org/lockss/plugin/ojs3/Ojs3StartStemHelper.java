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

package org.lockss.plugin.ojs3;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * A helper class that adds an optional attr "start_stem" to the path
 * defined as a start_url in the plugin
 * https://foo.com/<journal_id>/gateway/clockss?year=1234
 * becomes
 * https://foo.com/<start_stem>/<journal_id>/gateway/clockss?year=1234
 * and if there is no start_stem then just stays
 * https://foo.com/<journal_id>/gateway/clockss?year=1234
 * </p>
 *
 */
public class Ojs3StartStemHelper {
  private static final Logger log = Logger.getLogger(Ojs3StartStemHelper.class);
  private static final String START_STEM_ATTRIBUTE = "start_stem";

  public static Collection<String> addStartStem(ArchivalUnit au, Collection<String> origUrls) throws MalformedURLException {
    String start_stem = AuUtil.getTitleAttribute(au, START_STEM_ATTRIBUTE);
    log.debug3("start_stem = " + start_stem);
    if(start_stem == null) {
      log.debug3("No start_stem attribute. Returning plugin defined start_url(s).");
      return origUrls;
    }
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String journal_id = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());

    log.debug3("baseUrl = " + baseUrl + ", journal_id = " + journal_id);

    Collection<String> newUrls = new ArrayList<>(origUrls.size());
    for (String url : origUrls) {
      String urlProtocol = new URL(url).getProtocol() + "://";

      String urlWithoutProtocol = UrlUtil.stripProtocol(url);
      String baseurlWithoutProtocol =  UrlUtil.stripProtocol(baseUrl);

      log.debug3("url = " + url + ", urlWithoutProtocol = " + urlWithoutProtocol + ", base_url = "
          + baseUrl + ", baseurlWithoutProtocol = " + baseurlWithoutProtocol);
      // insert start_stem between baseUrl and anything that follows
      // There are cases "http" and "https" urls are embedded in the same html source
      // url = https://journals.vgtu.lt/BME/gateway/lockss?year=2016,
      // urlWithoutProtocol = journals.vgtu.lt/BME/gateway/lockss?year=2016,
      // base_url=https://journals.vgtu.lt/,
      // baseurlWithoutProtocol = journals.vgtu.lt/
      if (urlWithoutProtocol.startsWith(baseurlWithoutProtocol)) {
        StringBuilder sb = new StringBuilder(urlProtocol + baseurlWithoutProtocol);
        sb.append(start_stem);
        // append the path, and remove the start_stem, if it exists, in the given start_url.
        sb.append(urlWithoutProtocol.substring(baseurlWithoutProtocol.length()).replace(start_stem, ""));
        String expected = sb.toString();
        if (!newUrls.contains(expected)) {
          newUrls.add(expected);
        }
        log.debug3("final sb = " + expected);
      } else {
        // if it doesn't start with start_url just leave it alone
        log.debug3("adding other = " + url);
        if (!newUrls.contains(url)) {
          newUrls.add(url);
        }
      }
    }
    if (log.isDebug3()) {
      for (String url : newUrls) {
        log.debug3("start url: " + url);
      }
    }
    return newUrls;
  }
}