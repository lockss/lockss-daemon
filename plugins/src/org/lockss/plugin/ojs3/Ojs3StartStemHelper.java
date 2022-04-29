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

  public static Collection<String> addStartStem(ArchivalUnit au, Collection<String> origUrls) {
    String start_stem = AuUtil.getTitleAttribute(au, START_STEM_ATTRIBUTE);
    log.debug3("OJS3: In addStartStem. start_stem = " + start_stem);
    if(start_stem == null) {
      log.debug3("OJS3: No start_stem attributes, start_url is at represented in the plugin");
      return origUrls;
    }
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String journal_id = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());

    String baseUrlProtocal = getProtocal(baseUrl);

    log.debug3("OJS3: baseUrl = " + baseUrl + ", journal_id = " + journal_id);

    Collection<String> newUrls = new ArrayList<>(origUrls.size());
    for (String url : origUrls) {
      String urlProtocal = getProtocal(url);


      String urlWithoutProtocal = url.replace(urlProtocal, "");
      String baseurlWithoutProtocal = baseUrl.replace(baseUrlProtocal, "");

      log.debug3("OJS3: ------------url = " + url + ", urlWithoutProtocal = " + urlWithoutProtocal + ", base_url = "
          + baseUrl + ", baseurlWithoutProtocal = " + baseurlWithoutProtocal);
      // insert start_stem between baseUrl and anything that follows
      // There are cases "http" and "https" urls are embedded in the same html source
      // url = https://journals.vgtu.lt/BME/gateway/lockss?year=2016,
      // urlWithoutProtocal = journals.vgtu.lt/BME/gateway/lockss?year=2016,
      // base_url=https://journals.vgtu.lt/,
      // baseurlWithoutProtocal = journals.vgtu.lt/
      if (urlWithoutProtocal.startsWith(baseurlWithoutProtocal)) {
        String expected = getStartUrl(baseUrl, url, start_stem);
        if (!newUrls.contains(expected)) {
          newUrls.add(expected);
        }
        log.debug3("OJS3: =========final sb = " + expected);
      } else {
        // if it doesn't start with start_url just leave it alone, not that this is currently happening
        log.debug3("OJS3: .........adding other = " + url);
        if (!newUrls.contains(url)) {
          newUrls.add(url);
        }
      }
    }
    if (log.isDebug3()) {
      for (String url : newUrls) {
        log.debug3("OJS3: start url: " + url);
      }
    }
    return newUrls;
  }

  public static String getProtocal(String url) {
    String urlProtocal = "";

    if (url.startsWith("https://")) {
      urlProtocal = "https://";
    }  else if (url.startsWith("http://")) {
      urlProtocal = "http://";
    }

    return urlProtocal;
  }

  public static String getStartUrl(String baseUrl, String url, String start_stem) {

    String urlWithoutProtocal = url.replace(getProtocal(url), "");
    String baseurlWithoutProtocal =  baseUrl.replace(getProtocal(baseUrl), "");

    StringBuilder sb = new StringBuilder(baseUrl);

    if (urlWithoutProtocal.startsWith(baseurlWithoutProtocal)) {
      sb.append(start_stem);
      sb.append(urlWithoutProtocal.substring(baseurlWithoutProtocal.length()).replace(start_stem, ""));
    }

    return sb.toString();
  }
}