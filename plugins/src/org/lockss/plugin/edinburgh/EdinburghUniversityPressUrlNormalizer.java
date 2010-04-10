/*
 * $Id: EdinburghUniversityPressUrlNormalizer.java,v 1.1 2010-04-10 01:29:21 edwardsb1 Exp $
 */
/*
 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, destroy, sublicense, and/or sell
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
package org.lockss.plugin.edinburgh;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;

/**
 * @author Brent E. Edwards
 * 
 * Based on the code for org.lockss.plugin.universityofcaliforniapress.UniversityOfCaliforniaPressUrlNormalizer,
 * org.lockss.plugin.anthrosource.AnthroSourceUrlNormalizer, and 
 * org.lockss.plugin.universityofchicagopress.UniversityOfChicagoPressUrlNormalizer.
 *
 */
public class EdinburghUniversityPressUrlNormalizer implements UrlNormalizer {

  protected static final String SUFFIX = "?cookieSet=1";

  /* (non-Javadoc)
   * @see org.lockss.plugin.UrlNormalizer#normalizeUrl(java.lang.String, org.lockss.plugin.ArchivalUnit)
   */
  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {
    return StringUtils.chomp(url, SUFFIX);
  }

}
