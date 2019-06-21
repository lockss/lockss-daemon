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
import org.lockss.util.UrlUtil;

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
   */
  private static class DividedSocietyFeatureUrlHelper extends BaseFeatureUrlHelper {

    
    @Override
    public Collection<String> getAccessUrls(ArchivalUnit au) 
        throws PluginException, IOException {

      if (au == null) {
        return null;
      }
      //https://www.dividedsociety.org/archive/journals/<number>/issues
      String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      String journal_num = au.getConfiguration().get("journal_number");
      String titleUrl = baseUrl + START_JOURNAL_ARCHIVE + "/" + journal_num + "/issues";
      return (ListUtil.list(titleUrl));
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
          String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
          String journal_num = au.getConfiguration().get("journal_number");
          String titleUrl = baseUrl + START_JOURNAL_ARCHIVE + "/" + journal_num + "/issues";
          return (ListUtil.list(titleUrl));
      } else {
        return null;
      }
    }
  }
}

