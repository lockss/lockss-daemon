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

package org.lockss.plugin.scielo;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class SciELOUrlNormalizer implements UrlNormalizer {
  
  private static final Logger log = Logger.getLogger(SciELOUrlNormalizer.class);
  
  protected static final String TARGET_URL1 = "&pdf_path="; // how we identify these URLs
  protected static final Pattern PDF_PAT = Pattern.compile("(https?://[^/]+/).*&pdf_path=([^&]+)&");
  
  protected static final String TARGET_URL2 = "/scielo.php?"; // how we identify these URLs
  // /scielo.php?download&format=A&pid=B
  protected static final String DOWNLOAD_PARAM = "download";
  protected static final String FORMAT_PARAM = "format";
  // /scielo.php?script=A&...
  protected static final String SCRIPT_PARAM = "script";
  protected static final String PID_PARAM = "pid";
  protected static final String LANG_PARAM = "lang";
  protected static final String LNG_PARAM = "lng";
  protected static final String NRM_PARAM = "nrm";
  protected static final String TLNG_PARAM = "tlng";
  protected static final String ORIGINALLANG_PARAM = "ORIGINALLANG"; // unused?
  protected static final String NRM_DEFAULT_VAL = "iso";
  protected static final String TLNG_DEFAULT_VAL = "en";
  
  protected static final String TARGET_URL3 = "scieloOrg/php/articleXML.php?"; // how we identify these URLs
  
  protected static final Set<String> KNOWN_PARAMS = new HashSet<String>(Arrays.asList(
      DOWNLOAD_PARAM,
      FORMAT_PARAM,
      SCRIPT_PARAM,
      PID_PARAM,
      LANG_PARAM,
      LNG_PARAM,
      NRM_PARAM,
      TLNG_PARAM,
      ORIGINALLANG_PARAM                                                                                  
    ));
    
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    int qmark = url.indexOf("?");
    
    // Do not process URLs with url parameter
    if ((qmark > 1) && (url.lastIndexOf(":/") > qmark)) {
      return url;
    }
    
    // Some pdf URLs redirect to other URLs
    // The URLs look like this:
    // <base_url>/readcube/epdf.php?doi=10.1590/S0102-67202014000100001&pid=S0102-67202014000100001
    //      &pdf_path=abcd/v27n1/0102-6720-abcd-27-01-00001.pdf&lang=en
    // and we make them look like this:
    // <base_url>/pdf/abcd/v27n1/0102-6720-abcd-27-01-00001.pdf
    // Trim any &tlng=en param (and any trailing params)
    if (url.contains(TARGET_URL1)) {
      log.debug3("case 1");
      Matcher pdf_mat = PDF_PAT.matcher(url);
      if (pdf_mat.find()) {
        url = pdf_mat.group(1) + "pdf/" + pdf_mat.group(2);
      }
      log.debug3(url);
      return url;
    }
    
    if (url.contains(TARGET_URL2)) {
      log.debug3("case 2");
      // There are cases where we need to specify order of arguments
      // This is because we generate the URLs with the link extractor and need to 
      // makes sure we don't get redundant URLs due to "found" links that are
      // the same but for ordering.
      
      // Only do this normalization for correctly formatted URLs
      
      StringBuilder newUrl = new StringBuilder(url.substring(0, qmark));
      Map<String, String> query = parseQueryString(url.substring(qmark + 1));
      String val;
      
      if (!(query.containsKey(DOWNLOAD_PARAM) || query.get(SCRIPT_PARAM) != null)) {
        return url; // don't alter URLs not of these two types
      }
      
      if (query.containsKey(DOWNLOAD_PARAM)) {
        newUrl.append("?" + DOWNLOAD_PARAM);
        val = query.get(PID_PARAM);
        if (val != null) {
          newUrl.append("&" + PID_PARAM + "=" + val );
        }
        val = query.get(FORMAT_PARAM);
        if (val != null) {
          newUrl.append("&" + FORMAT_PARAM + "=" + val);
        }
        url = newUrl.toString();
        log.debug3(url);
        return url;
      }
      
      val = query.get(SCRIPT_PARAM);
      newUrl.append("?" + SCRIPT_PARAM + "=" + val);
      val = query.get(PID_PARAM);
      if (val != null) {
        newUrl.append("&" + PID_PARAM + "=" + val);
      }
      val = query.get(LNG_PARAM);
      if (val == null) {
        val = "en";
      }
      newUrl.append("&" + LNG_PARAM + "=" + val);
      val = query.get(NRM_PARAM);
      if (val != null && !val.equals(NRM_DEFAULT_VAL)) {
        newUrl.append("&" + NRM_PARAM + "=" + val);
      }
      val = query.get(TLNG_PARAM);
      if (val != null && !val.equals(TLNG_DEFAULT_VAL)) {
        newUrl.append("&" + TLNG_PARAM + "=" + val);
      }
      
      url = newUrl.toString();
      log.debug3(url);
      return url;
    }
    
    if (url.contains(TARGET_URL3)) {
      log.debug3("case 3");
      StringBuilder newUrl = new StringBuilder(url.substring(0, qmark));
      Map<String, String> query = parseQueryString(url.substring(qmark + 1));
      String val;
      
      val = query.get(PID_PARAM);
      if (val == null) {
        return url;
      }
      
      newUrl.append("?" + PID_PARAM + "=" + val);
      newUrl.append("&" + LANG_PARAM + "=en"); // force LANG_PARAM to "en"

      url = newUrl.toString();
      log.debug3(url);
      return url;
    }
    
    // Otherwise...
    return url;
  }
  
  protected static Map<String, String> parseQueryString(String queryString) {
    queryString = queryString.replace("%26", "&");
    Map<String, String> ret = new HashMap<String, String>();

    for (String pair : queryString.split("&")) {
      log.debug3("pair: " + pair);
      int eq = pair.indexOf("=");
      String key;
      String val;
      if (eq < 0 || eq == pair.length() - 1) {
        key = pair;
        val = null;
      }
      else {
        key = pair.substring(0, eq);
        val = pair.substring(eq + 1);
      }
      if (!KNOWN_PARAMS.contains(key)) {
        log.debug(key + " not in known params");
      }
      ret.put(key, val);
    }

    return ret;
  }
  
}
