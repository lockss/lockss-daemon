/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.anu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

public class AnuUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  protected static final Logger log = Logger.getLogger(AnuUrlNormalizer.class);
  
  protected static final String REPL_STR = "[?].+$";
  protected static final String CSS_SUFFIX = ".css?";
  protected static final String JS_SUFFIX = ".js?";
  
  protected static final String FID_PARAM = "?field_id_value=";
  protected static final String ITOK_PARAM = "?itok=";
  protected static final String REFR_PARAM = "?referer=";
  
  protected static final String PAGE_PARAM = "page=";
  protected static final Pattern PAGE_PAT = Pattern.compile("(?<!xhtml)[?].*(page=[0-9]+)", Pattern.CASE_INSENSITIVE);
  
  
  @Override
  public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
    if (url.contains(PAGE_PARAM)) {
      Matcher mat = PAGE_PAT.matcher(url);
      if (mat.find()) {
        url = url.replaceFirst(REPL_STR, "?" + mat.group(1));
      }
    }
    if (url.contains(CSS_SUFFIX) ||
        url.contains(JS_SUFFIX) ||
        url.contains(FID_PARAM) ||
        url.contains(ITOK_PARAM) ||
        url.contains(REFR_PARAM)) {
      url = url.replaceFirst(REPL_STR, "");
    }
    
    return(url);
  }


  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    /* NOTE: 
     * Adding special handling to normalize http://press-files.anu.edu.au/ 
     * as well as https://press.anu.edu.au/
     * Check for same host or "press-files.anu.edu.au", then normalize
     * This should be safe as wget of any https://press-files.anu.edu.au files did not redirect, just returned content with 200
     * Also, if the press-files.anu.edu.au site changes protocol, we will not collect both versions of the files
     */
    if (UrlUtil.isSameHost(au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()), url) ||
        url.contains("://press-files.anu.edu.au")) {
      url = AuUtil.normalizeHttpHttpsFromBaseUrl(au, url);
    }
    return additionalNormalization(url, au);
  }
}
