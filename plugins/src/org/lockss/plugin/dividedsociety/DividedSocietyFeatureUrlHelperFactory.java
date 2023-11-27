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

package org.lockss.plugin.dividedsociety;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.OpenUrlResolver;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.BaseFeatureUrlHelper;
import org.lockss.plugin.FeatureUrlHelper;
import org.lockss.plugin.FeatureUrlHelperFactory;
import org.lockss.plugin.Plugin;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

public  class DividedSocietyFeatureUrlHelperFactory implements FeatureUrlHelperFactory {
  private static final Logger log = Logger.getLogger(DividedSocietyFeatureUrlHelperFactory.class);
  private static final String START_JOURNAL_ARCHIVE = "archive/journals";

  @Override
  public FeatureUrlHelper createFeatureUrlHelper(Plugin plug) {
    return new DividedSocietyFeatureUrlHelper();
    
  }
  
  
  /*
   * Divided Socity has two start_urls.  The first is the way we're granted our cookies for access
   * but isn'tuseful as an access url to the preserved content.
   * Bypass that and use the second start_url which happens to be hard-coded to the top of all collected content
   * Each AU will only hold the content for its one journal. 
   * Use the journal number to access the AU at the logical top of the specific title 
   * 
   * The ClockssDividedSocietySnapshotPlugin starts in one place and the 
   * ClockssDividedSocietyCollectionSnapshotPlugin starts in another 
   */
  private static class DividedSocietyFeatureUrlHelper extends BaseFeatureUrlHelper {

    
    @Override
    public Collection<String> getAccessUrls(ArchivalUnit au) 
        throws PluginException, IOException {

      if (au == null) {
    	  return null;
      }
      return (ListUtil.list(getTitleUrl(au)));
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
      //https://www.dividedsociety.org/archive/journals/1000/issues
      if (itemType == OpenUrlResolver.OpenUrlInfo.ResolvedTo.TITLE) {
          return (ListUtil.list(getTitleUrl(au)));
      } else {
        return null;
      }
    }
    
    private String getTitleUrl(ArchivalUnit au) {
        //https://www.dividedsociety.org/archive/journals/<number>/issues

        if (au == null) {
          return null;
        }
        String pluginId = au.getPluginId();
        String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
        String titleUrl;
        if (pluginId.contains("CollectionSnapshot")) {
           //https://www.dividedsociety.org/posters
           String coll_id = au.getConfiguration().get("collection_id");    	  
           titleUrl = baseUrl +  coll_id;
        } else {
            //https://www.dividedsociety.org/archive/journals/<number>/issues
            String journal_num = au.getConfiguration().get("journal_number");
            titleUrl = baseUrl + START_JOURNAL_ARCHIVE + "/" + journal_num + "/issues";
        }
        return titleUrl;
    }
  }
}

