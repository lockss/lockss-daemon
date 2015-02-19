package org.lockss.plugin.springer.api;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;
import org.lockss.util.urlconn.CacheException;


public class SpringerApiCrawlSeed extends BaseCrawlSeed {
  private static Logger logger = 
      Logger.getLogger(SpringerApiCrawlSeed.class);
  static final String CDN_URL = "http://download.springer.com/";
  //DO NOT COMMIT THE API_KEY
  static final String API_KEY = "";
  protected String volume;
  protected String baseUrl;
  protected String apiUrl;
  protected String issn;
  protected Crawler.CrawlerFacade crawlFacade;
  protected List<String> doiList = new ArrayList<String>(100);
  protected boolean continueFetching = true;
  protected UrlConsumerFactory springerApiConsumer = 
      new SpringerApiUrlConsumerFactory(this);
  

  public SpringerApiCrawlSeed(Crawler.CrawlerFacade crawlFacade) {
    super(crawlFacade.getAu());
    if(au == null) {
      throw new IllegalArgumentException(
          "Valid ArchivalUnit required for crawl seed");
    }
    TypedEntryMap props = au.getProperties();
    try {
      populateFromProps(props);
    } catch(ConfigurationException ex) {
      logger.error("Error creating crawl seed", ex);
    } catch(PluginException ex) {
      logger.error("Error creating crawl seed", ex);
    }
    this.crawlFacade = crawlFacade;
  }
  
  /**
   * Pulls needed params from the au props. Throws exceptions if
   *  expected props do not exist
   * @param props
   * @throws PluginException
   * @throws ConfigurationException
   */
  protected void populateFromProps(TypedEntryMap props) 
      throws PluginException, ConfigurationException {
    //required params
    if(props.containsKey(ConfigParamDescr.BASE_URL.getKey())) {
      this.baseUrl = props.getString(ConfigParamDescr.BASE_URL.getKey());
    } else {
      throw new PluginException.InvalidDefinition("CrawlSeed expected "
          + ConfigParamDescr.BASE_URL.getKey());
    }
    
    if(props.containsKey("api_url")) {
        this.apiUrl = props.getString("api_url");
      } else {
        throw new PluginException.InvalidDefinition("CrawlSeed expected api_url");
      }
    
    if(props.containsKey(ConfigParamDescr.VOLUME_NAME.getKey())) {
      this.volume = props.getString(ConfigParamDescr.VOLUME_NAME.getKey());
    } else {
      throw new PluginException.InvalidDefinition("CrawlSeed expected "
          + ConfigParamDescr.VOLUME_NAME.getKey());
    }
    
    if(props.containsKey(ConfigParamDescr.JOURNAL_ISSN.getKey())) {
      this.issn = props.getString(ConfigParamDescr.JOURNAL_ISSN.getKey());
    } else {
      throw new PluginException.InvalidDefinition("CrawlSeed expected "
          + ConfigParamDescr.JOURNAL_ISSN.getKey());
    }
  }
  
  protected void populateDoiList() {
    int index = 1;
    while(continueFetching) {
      String url = buildUrl(index);
      UrlFetcher uf = makeUrlFetcher(url);
      try {
    	FetchResult fr = uf.fetch();
        if(fr != FetchResult.FETCHED) {
          continueFetching = false;
        } else {
          index += 100;
        }
      } catch (CacheException e) {
        continueFetching = false;
      }
    }
  }
  
  protected UrlFetcher makeUrlFetcher(String url) {
    UrlFetcher uf = au.makeUrlFetcher(crawlFacade, url);
    BitSet permFetchFlags = uf.getFetchFlags();
    permFetchFlags.set(UrlCacher.REFETCH_FLAG);
    uf.setFetchFlags(permFetchFlags);
    uf.setUrlConsumerFactory(springerApiConsumer);
    return uf;
  }
  
  protected String buildUrl(int startingIndex) {
    return apiUrl + "meta/v1/pam?q=issn:" + issn + "volume:" + volume + "&api_key=" + API_KEY + "&p=100&s=" + startingIndex;
  }
  
  public void updateDoiList(Collection<String> dois, boolean cont) {
    doiList.addAll(dois);
    continueFetching = cont;
  }
  
  @Override
  public Collection<String> getStartUrls() 
      throws ConfigurationException, PluginException {
	if(doiList.isEmpty()) {
		populateDoiList();
	}
    Collection<String> ret = new ArrayList<String>(doiList.size());
    logger.debug("Building URLs");
    for(String doi : doiList){
      ret.add(CDN_URL + "content/pdf/" + doi + ".pdf");
    }
    return ret;
  }
}
