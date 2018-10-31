/*
 *
 *  * $Id: $
 *
 *
 *
 * Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Stanford University.
 *
 * /
 */

package org.lockss.plugin.silverchair.oup;

import java.util.regex.Pattern;

import org.lockss.plugin.silverchair.BaseScHtmlHttpResponseHandler;
import org.lockss.util.Logger;

public class OupScHtmlHttpResponseHandler extends BaseScHtmlHttpResponseHandler {

  private static final Logger log = Logger.getLogger(OupScHtmlHttpResponseHandler.class);

  // OUP images are at "stable" expiring URLS - in this case it wasn't actually supp_zip but supposed to be image source which was the 403
  // https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/hmg/26/3/10.1093_hmg_ddw419/2/m_ddw419_supp.zip?Expires=2147483647&Signature=MJDYDzdo...&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA
  // PDF files use a changing Expires date and get consumed. Only supporting content uses the specific stable Expires date so use that as the pattern match
  // oup/backfile/Content_public/[^?]+\?Expires=2147483647&Signature="
  protected static final Pattern NON_FATAL_PAT = 
      Pattern.compile("(oup/backfile/Content_public/[^?]+\\?Expires=2147483647&Signature=|\\.(bmp|css|eot|gif|ico|jpe?g|js|otf|png|svg|tif?f|ttf|woff)$)");

  @Override
  protected Pattern getNonFatalPattern() {
    return NON_FATAL_PAT;
  }

}
