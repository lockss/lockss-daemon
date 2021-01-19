/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.*;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A URL normalizer that first forces an incoming URL to have the same HTTP or
 * HTTPS protocol as the AU's base URL ({@link ConfigParamDescr#BASE_URL}) if
 * it is on the same host, before applying other normalization.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.70
 * @deprecated Since 1.75.4, use {@link HttpHttpsParamUrlNormalizer} instead.
 */
@Deprecated
public class BaseUrlHttpHttpsUrlNormalizer implements UrlNormalizer {

  @Override
  public String normalizeUrl(String url,
                             ArchivalUnit au)
      throws PluginException {
    if (UrlUtil.isSameHost(au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()),
                           url)) {
      url = AuUtil.normalizeHttpHttpsFromBaseUrl(au, url);
    }
    return additionalNormalization(url, au);
  }
  
  /**
   * <p>
   * By default, this parent implementation does not perform any additional
   * normalization and only returns the original URL.
   * </p>
   * 
   * @param url
   *          A URL.
   * @param au
   *          An archival unit.
   * @return A normalized URL string as needed (unchanged URL by default)
   * @throws PluginException
   *           if a plugin error arises
   * @since 1.70
   */
  public String additionalNormalization(String url,
                                        ArchivalUnit au)
      throws PluginException {
    return url;
  }

}
