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
