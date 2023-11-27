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

package org.lockss.plugin.michigan;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class UMichUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  protected static final Logger log = Logger.getLogger(UMichUrlNormalizer.class);

  /*
  examples:
  https://www.fulcrum.org/concern/file_sets/zc77ss02p
  https://www.fulcrum.org/concern/file_sets/zc77ss02p?locale=en
  https://www.fulcrum.org/concern/monographs/xg94hr617
  https://www.fulcrum.org/concern/monographs/xg94hr617?locale=en
  https://www.fulcrum.org/concern/monographs/xg94hr617?locale=en&page=2
  https://www.fulcrum.org/concern/monographs/xg94hr617?locale=en?utf8=%E2%9C%93&locale=en
   */

  // need to replace more than once
  protected static final String LOCALE_PARAM = "locale=en";
  // local and utf may be linked with '?', not '&'
  protected static final String UTF_PARAM = "utf8=.*";
  /* not used as patterns, no need to escape questionmark and dot*/
  private static final String IMAGE_SERVICE  = "/image-service/";
  private static final String JPEG_ARGUMENT  = ".jpg?";
    
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au)
		  throws PluginException {

    log.debug2("------UMichUrlNormalizer, original url = " + url);
    //url = url.replaceFirst(LOCALE_PARAM, "");
    //url = url.replaceFirst(UTF_PARAM, "");
    url = url.replaceAll(LOCALE_PARAM, "");
    url = url.replaceAll(UTF_PARAM, "");
    // remove changing argument on end of images
    if (url.matches("/image-service/.*\\.(jpe?g|png|json)\\?")) {
      url = url.replaceFirst("\\?.*", "");
    }
    url = url.replaceAll("\\?\\?", "?");
    if (url.contains("??&")) {
      url = url.replaceAll("\\?\\?&", "");
    } else if (url.contains("?&")) {
      url = url.replaceAll("\\?&", "?");
    }

    if (url.endsWith("?"))  {
      url = url.substring(0, url.length() - 1);
    }

    log.debug2("=========UMichUrlNormalizer, after replaced url = " + url);
    return(url);
  }



}
