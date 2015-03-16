package org.lockss.plugin.dspace;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;


public class DSpaceCrawlSeed extends IdentifierListOaiPmhCrawlSeed {
  Collection<String> startUrls;

  public DSpaceCrawlSeed(CrawlerFacade cf) {
    super(cf);
  }
  
  @Override
  public Collection<String> doGetStartUrls() 
      throws ConfigurationException, PluginException, IOException{
    if(startUrls == null) {
      startUrls = super.doGetStartUrls();
    }
    return startUrls;
  }
  
  @Override
  public Collection<String> doGetPermissionUrls() 
      throws ConfigurationException, PluginException, IOException {
    if(startUrls == null) {
      startUrls = super.doGetStartUrls();
    }
    if(permUrls != null) {
      return permUrls;
    }
    return Collections.EMPTY_LIST;
    
  }
}
