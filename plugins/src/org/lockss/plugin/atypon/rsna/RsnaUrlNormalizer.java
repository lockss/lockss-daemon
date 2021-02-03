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

package org.lockss.plugin.atypon.rsna;

import java.util.regex.Pattern;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.util.Logger;

/*
 * Adds in the RSNA specific normalizations
 */
public class RsnaUrlNormalizer extends BaseAtyponUrlNormalizer {
  protected static Logger log = Logger.getLogger(RsnaUrlNormalizer.class);
  /*
    https://pubs.rsna.org/products/rsna/fonts/icomoon/icomoon.eot?yq99jl
    https://pubs.rsna.org/products/rsna/fonts/icomoon/icomoon.svg?ubwa67
    https://pubs.rsna.org/products/rsna/fonts/icomoon/icomoon.ttf?yq99jl
    https://pubs.rsna.org/products/rsna/fonts/icomoon/icomoon.woff?ubwa67
   */
  protected static final Pattern FONT_ARG_PATTERN = Pattern.compile("(\\.(:?eot|svg|ttf|woff))\\?.+$");

  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    // some font files have an argument that isn't needed
    String returnString = FONT_ARG_PATTERN.matcher(url).replaceFirst("$1");
    if (!returnString.equals(url)) {
      log.debug3("normalized font url: " + returnString);
      url = returnString;
    }

    // http to https
    HttpHttpsUrlHelper helper = new HttpHttpsUrlHelper(au,
        ConfigParamDescr.BASE_URL.getKey(),
        "base_url");
    url = helper.normalize(url);

    return super.normalizeUrl(url, au);
  }
}