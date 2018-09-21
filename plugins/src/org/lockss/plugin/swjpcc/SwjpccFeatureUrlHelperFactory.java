package org.lockss.plugin.swjpcc;

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

public  class SwjpccFeatureUrlHelperFactory implements FeatureUrlHelperFactory {
  private static final Logger log = Logger.getLogger(SwjpccFeatureUrlHelperFactory.class);

  @Override
  public FeatureUrlHelper createFeatureUrlHelper(Plugin plug) {
    return new SwjpccFeatureUrlHelper();
    
  }
  
  private static class SwjpccFeatureUrlHelper extends BaseFeatureUrlHelper {

    
    @Override
    public Collection<String> getAccessUrls(ArchivalUnit au) 
        throws PluginException, IOException {

      if (au == null) {
        return null;
      }
      // return the synthetic url  
      String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());
      return (ListUtil.list(storeUrl));
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
        String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
        String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());
        return(ListUtil.list(storeUrl));
      } else {
        return null;
      }
    }
  }
}

