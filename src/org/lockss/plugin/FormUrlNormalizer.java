/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.util.Logger;

import java.util.HashMap;
import java.util.Set;

/** @author mlanken */
public class FormUrlNormalizer implements UrlNormalizer {

  private static final Logger logger = Logger.getLogger(FormUrlNormalizer.class);

  private boolean m_sortAllUrls;
  private HashMap<String, Integer> m_limits;
  private FormUrlHelper m_converter;

  public FormUrlNormalizer(boolean sortAllUrls, HashMap<String,
      Integer> limits) {
    m_sortAllUrls = sortAllUrls;
    m_limits = limits;
    m_converter = new FormUrlHelper();
  }

  public FormUrlNormalizer() {
    this(false, null);
  }

  /**
   * Normalize a form urls. This does (optional) sorting,
   * limits and mandatory
   * encoding for GET urls
   *
   * @param url base url
   * @param au  The au to this plugin is targeting
   * @return the normalized url string
   * @throws PluginException
   * @see org.lockss.plugin.UrlNormalizer#normalizeUrl(java.lang.String,
   *      org.lockss.plugin.ArchivalUnit)
   */
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws
                                                          PluginException {

    if (url == null) { return url;}
    //return all non-form urls unchanged
    if (StringUtils.indexOf(url, "?") == -1) { return url; }
    //if there is a problem converting the url, return the original url;
    if (!m_converter.convertFromEncodedString(url)) { return url; }

    if (m_sortAllUrls) {
      m_converter.sortKeyValues();
    }

    if (m_limits != null) {
      Set<String> limitedKeys = (Set<String>) m_limits.keySet();
      for (String key : limitedKeys) {
        m_converter.applyLimit(key, m_limits.get(key));
      }
    }

    String outputUrl = m_converter.toEncodedString();
    if (logger.isDebug() && !outputUrl.equals(url)) {
      logger.debug(" converted " + url + " to " + outputUrl);
    }
    return outputUrl;
  }
}

