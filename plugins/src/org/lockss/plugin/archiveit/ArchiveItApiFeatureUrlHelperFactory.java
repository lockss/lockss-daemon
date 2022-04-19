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

package org.lockss.plugin.archiveit;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.OpenUrlResolver;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;
import org.lockss.util.UrlUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class ArchiveItApiFeatureUrlHelperFactory implements FeatureUrlHelperFactory {
  private static final Logger log = Logger.getLogger(ArchiveItApiFeatureUrlHelperFactory.class);


  @Override
  public FeatureUrlHelper createFeatureUrlHelper(Plugin plug) {
    return new ArchiveItFeatureUrlHelper();
  }

  private static class ArchiveItFeatureUrlHelper extends BaseFeatureUrlHelper {

    @Override
    public Collection<String> getAccessUrls(ArchivalUnit au)
        throws PluginException, IOException {

      if (au == null) {
        return null;
      }
      // return the synthetic url
      return (ListUtil.list(getSyntheticUrl(au)));
    }

    @Override
    public List<String> getFeatureUrls(ArchivalUnit au,
                                       OpenUrlResolver.OpenUrlInfo.ResolvedTo itemType,
                                       TypedEntryMap paramMap)
        throws PluginException, IOException {

      // This can actually get called with null AU from OpenUrlResolver
      if (au == null) {
        return null;
      }
      if (itemType == OpenUrlResolver.OpenUrlInfo.ResolvedTo.VOLUME) {
        return (ListUtil.list(getSyntheticUrl(au)));
      } else {
        return null;
      }
    }
    public String getSyntheticUrl(ArchivalUnit au) {
      // synthetic url, if you want to update the pattern you must update it in all of these places
      // 1. ArchiveItApiPlugin - crawl_rules
      // 2. ArchiveItApiCrawlSeed.populateUrlList()
      // 3. ArchiveItApiFeatureUrlHelperFactory.getSyntheticUrl()
      String sytheticUrl =
          au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()) +
          "organization=" + UrlUtil.encodeUrl(au.getConfiguration().get("organization")) +
          "&collection=" + UrlUtil.encodeUrl(au.getConfiguration().get(ConfigParamDescr.COLLECTION.getKey()));
      return sytheticUrl;
    }
  }
}
