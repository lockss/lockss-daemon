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

import java.util.regex.*;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.silverchair.SilverchairScholarlyPublishingCollectiveUrlNormalizer;
import org.lockss.util.Logger;

/**
 * <p>
 * URLs on OUP meeting abstract articles have param ?searchresult=1, which can be removed
 * </p>
 * <ul>
 * <li><code>https://academic.oup.com/ageing/article/46/suppl_1/i39/3828923?searchresult=1</code></li>
 * <li>https://academic.oup.com/ageing/article/46/suppl_1/i39/3828923</li>
 * </ul>
 */
public class OupScUrlNormalizer implements UrlNormalizer {

  private static final Logger log = Logger.getLogger(OupScUrlNormalizer.class);

  protected static Pattern VERSIONED_DIRECTORY_NAME =
          Pattern.compile("(.*)\\/(v-\\d+)/(.*)\\.(css|js|ico|png|json|bmp|css|eot|gif|ico|jpe\\?g|js|otf|png|svg|tif\\?f|ttf|woff\\?)",
                  Pattern.CASE_INSENSITIVE);


  private static final Pattern RESULT_PATTERN = Pattern.compile("[?]searchresult=\\d+$", Pattern.CASE_INSENSITIVE);
  private static final String RESULT_CANONICAL = "";

  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {

    String[] excludedFileTypes = new String[] {"css", "js", "ico", "png", "json", "bmp", "eot", "gif", "ico", "jpeg", "jpg", "otf", "png", "svg", "tiff", "tif","ttf", "woff", "wof"};

    Matcher m1 = VERSIONED_DIRECTORY_NAME.matcher(url);

    String newUrl = url;

    if (StringUtils.indexOfAny(url, excludedFileTypes) > -1) {

      log.debug3("Version based diretory check: UrlNormalizer url = " + url );
      if (m1.find()) {
        newUrl = m1.group(1) + "/" + m1.group(3) + "." + m1.group(4);

        log.debug3("Version based diretory check: UrlNormalizer newUrl = " + newUrl );
      }

      return newUrl;
    }

    url = RESULT_PATTERN.matcher(url).replaceFirst(RESULT_CANONICAL);

    return url;
  }

}
