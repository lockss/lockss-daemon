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

package org.lockss.plugin.highwire.bmj;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWireJCoreUrlNormalizer;
import org.lockss.util.Logger;

public class BMJJCoreUrlNormalizer extends HighWireJCoreUrlNormalizer {
  
  private static final Logger log = Logger.getLogger(BMJJCoreUrlNormalizer.class);
  
  // http://static.www.bmj.com/content/bmj/351/bmj.h3779.full.pdf becomes
  // http://www.bmj.com/content/bmj/351/bmj.h3779.full.pdf
  public static final String BMJ_UN_STATIC = "/static.";
  public static final String BMJ_UN_PARTIAL = "bmj.com/content/";
  public static final String BMJ_UN_REPLACE = "www.bmj.com/content/";
  public static final Pattern BMJ_UN_PREFIX_PAT = Pattern.compile("static[^/]*[.]bmj[.]com/content/");
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    
    if (url.contains(BMJ_UN_STATIC) && url.contains(BMJ_UN_PARTIAL)) {
      Matcher mat = BMJ_UN_PREFIX_PAT.matcher(url);
      if (mat.find()) {
        url = mat.replaceFirst(BMJ_UN_REPLACE);
      }
    }
    
    return super.normalizeUrl(url, au);
  }
  
}
