/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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


package org.lockss.plugin.highwire.annalsfamilymedicine;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWireJCoreUrlNormalizer;
import org.lockss.util.Logger;

public class AnnalsFamilyMedicineDrupalUrlNormalizer extends HighWireJCoreUrlNormalizer {

    private static final Logger log = Logger.getLogger(AnnalsFamilyMedicineDrupalUrlNormalizer.class);
    /*
      We want to put all results on one page so that all articles can be found during crawl. 
      Normalize https://www.annfammed.org/search/%20volume%3A21%20issue%3ASupplement%2B3%20jcode%3Aannalsfm%20sort%3Arelevance-rank?facet%5Btoc-section-id%5D%5B0%5D=Acute%20and%20emergency%20care
      to https://www.annfammed.org/search/volume%3A21%20issue%3ASupplement%2B3%20jcode%3Aannalsfm%20sort%3Arelevance-rank%20numresults%3A100?facet%5Btoc-section-id%5D%5B0%5D=Acute%20and%20emergency%20care
    */
    @Override
    public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
        
        if (url.contains("annfammed.org/search/") && url.contains("Supplement") && url.contains("?") && !url.contains("%20numresults%3A100")) {
            int qmark = url.indexOf("?");
            String primary = url.substring(0,qmark);
            String query = url.substring(qmark+1);
            StringBuilder result = new StringBuilder(primary + "%20numresults%3A100?" + query);
            log.info("Old URL: " + url + "\\nNew URL: " + result.toString());
            url = result.toString();
        }
        return super.normalizeUrl(url, au);
    }

}
