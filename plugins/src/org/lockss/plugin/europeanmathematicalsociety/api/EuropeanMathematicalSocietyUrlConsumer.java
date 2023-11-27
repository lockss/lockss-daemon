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

package org.lockss.plugin.europeanmathematicalsociety.api;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.base.SimpleUrlConsumer;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EuropeanMathematicalSocietyUrlConsumer extends SimpleUrlConsumer {

  protected Pattern pat;

  public EuropeanMathematicalSocietyUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    super(facade, fud);
    pat = Pattern.compile(String.format("^%sdownload(.*)$",
                                        facade.getAu().getConfiguration().get(ConfigParamDescr.BASE_URL.getKey())),
                          Pattern.CASE_INSENSITIVE);
  }

  @Override
  public void consume() throws IOException {
    if (shouldStoreAtOrigUrl()) {
      storeAtOrigUrl();
    }
    super.consume();
  }
  
  public boolean shouldStoreAtOrigUrl() {
    if (   fud.redirectUrls != null
        && fud.redirectUrls.size() == 1
        && fud.fetchUrl.equals(fud.redirectUrls.get(0))) {
      Matcher mat = pat.matcher(fud.origUrl);
      return    mat.matches()
             && fud.fetchUrl.endsWith(mat.group(1));
    }
    return false;
  }

}
