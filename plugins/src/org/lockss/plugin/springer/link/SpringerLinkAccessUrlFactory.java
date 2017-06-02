package org.lockss.plugin.springer.link;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.UrlGenerator;
import org.lockss.plugin.UrlGeneratorFactory;
import org.lockss.util.ListUtil;
import org.lockss.util.UrlUtil;

public  class SpringerLinkAccessUrlFactory implements UrlGeneratorFactory {

  @Override
  public UrlGenerator createUrlGenerator(ArchivalUnit au) {
    // return the synthetic url  
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());
    return new IUCrOaiAccessUrlGenerator(ListUtil.list(storeUrl));
  }
}

class IUCrOaiAccessUrlGenerator implements UrlGenerator {
  List res;
  IUCrOaiAccessUrlGenerator(List res) {
    this.res = res;
  }
  @Override
  public Collection<String> getUrls(ArchivalUnit au)
      throws PluginException, IOException {
    return res;
  }
}
