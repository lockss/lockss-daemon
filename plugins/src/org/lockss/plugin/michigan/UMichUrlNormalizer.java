/*
 * $Id$
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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
