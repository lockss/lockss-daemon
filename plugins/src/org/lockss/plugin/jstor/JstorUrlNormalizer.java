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

package org.lockss.plugin.jstor;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;

/*
 * JSTOR limited plugin
 * We only collect a TOC of the form provided by a manifest page
 * and PDF files (and in the rare cases that they exist a "full" version)
 * as well as the engineered RIS citation pages.
 * 
 *1) In the rare cases that a "full" version of the article exists, we might
 * also get access to a "media" or "select" page. These need normalizing.
 *   http://www.jstor.org/stable/select/4436970?seq=1&thumbView=thumbs&thumbPager=one
 * should become:
 *   http://www.jstor.org/stable/select/4436970
 *
 *2) In a few journals (American Biology Teacher)
 *   http://www.jstor.org/stable/pdfplus/10.1525/abt.2013.75.6.4.pdf?&acceptTC=true&jpdConfirm=true
 * should become:
 *   http://www.jstor.org/stable/pdfplus/10.1525/abt.2013.75.6.4.pdf
 */

public class JstorUrlNormalizer implements UrlNormalizer {

  protected static final String SEQ_SUFFIX = "?seq=1";
  protected static final String ACCEPT_SUFFIX = "?acceptTC=true";
  protected static final String OTHER_ACCEPT_SUFFIX = "?&amp;acceptTC";

  public String normalizeUrl(String url,
      ArchivalUnit au)
          throws PluginException {

    // only try to cleanup if we have an argument list 
    if ( url.contains("?")) {

      /*
       *  This is slightly inefficient because we will only have one of the three,
       *  but calling substringBeforeLast just returns the orig if the substring
       *  isn't found. And this avoids doing additional comparisons just to see
       *  if the substring is there first - which is net better.
       */
      url = StringUtils.substringBeforeLast(url, SEQ_SUFFIX);
      url = StringUtils.substringBeforeLast(url, ACCEPT_SUFFIX);
      url = StringUtils.substringBeforeLast(url, OTHER_ACCEPT_SUFFIX);
    }
    return url;
  }

}
