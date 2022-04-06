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
