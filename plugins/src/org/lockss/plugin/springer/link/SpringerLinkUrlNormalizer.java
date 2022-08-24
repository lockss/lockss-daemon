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

package org.lockss.plugin.springer.link;

import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.StringUtil;

/**
 */
public class SpringerLinkUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {

  public static final String SLASH_ENCODED = "%2F";
  public static final String SLASH = "/";
  public static final Pattern CHAPTER_DOI_PATTERN = Pattern.compile("(.+/chapter/.+)(_[0-9]+)(/fulltext.html)?");
  public static final Pattern CHAPTER_PDF_DOI_PATTERN = Pattern.compile("(.+/content/pdf/.+)(_[0-9]+)(\\.pdf)$");
  
  @Override
  public String additionalNormalization(String url, ArchivalUnit au) throws PluginException {
    
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    if(url.startsWith(baseUrl)) {
      //Some slashes were encoded and some not. We set them all to non encoded
      if(!url.contains("pdf") && !url.contains("epub")) {
        url = StringUtil.replaceString(url, SLASH_ENCODED, SLASH);
      }
      //Find chapter URLs and point them at full text. 
      //Not collecting individual chapters saves time and we already have the content in the full book
      if(url.contains("_")) {
        Matcher pdfMatcher = CHAPTER_PDF_DOI_PATTERN.matcher(url);
        Matcher chapterMatcher = CHAPTER_DOI_PATTERN.matcher(url);    
        if(chapterMatcher.matches()) {
          url = chapterMatcher.group(1);
          url = StringUtil.replaceString(url, "chapter", "book");
        } else if(pdfMatcher.matches()) {
          url = pdfMatcher.group(1) + pdfMatcher.group(3);
        }
      }
    }
    return url;
  }
}
