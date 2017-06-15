package org.lockss.plugin.internationalunionofcrystallography.oai;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

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

public  class IUCrOaiFeatureUrlHelperFactory implements FeatureUrlHelperFactory {
  
  private static final Logger log = Logger.getLogger(IUCrOaiFeatureUrlHelperFactory.class);


  @Override
  public FeatureUrlHelper createFeatureUrlHelper(Plugin plug) {
    return new IUCrOaiFeatureUrlHelper();
    
  }
  
  private static class IUCrOaiFeatureUrlHelper extends BaseFeatureUrlHelper {
    
    @Override
    public List<String> getFeatureUrls(ArchivalUnit au,
                                       OpenUrlResolver.OpenUrlInfo.ResolvedTo itemType,
                                       TypedEntryMap paramMap) 
        throws PluginException, IOException {
      
      // this can get called with a null au from OpenUrlResolver
      if (au == null) {
        return null;
      }
      if (itemType == OpenUrlResolver.OpenUrlInfo.ResolvedTo.VOLUME) {
        String baseUrl = au.getConfiguration().get("script_url");
        String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());
        return(ListUtil.list(storeUrl));
      } else {
        return null;
      }
    }

 
    @Override
    public Collection<String> getAccessUrls(ArchivalUnit au) 
        throws PluginException, IOException {
      
      if (au == null) {
          return null;
      }
      // return the synthetic url  
      String baseUrl = au.getConfiguration().get("script_url");
      String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());
      return(ListUtil.list(storeUrl));
    }
    
  }

}
