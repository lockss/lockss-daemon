package org.lockss.extractor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.base.BaseArchivalUnit;
import org.lockss.plugin.base.BasePlugin;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.PrivilegedAccessor;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.daemon.MetadataManager;
import org.lockss.daemon.OpenUrlResolver;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.TitleConfig;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor.Emitter;
import org.lockss.extractor.BaseArticleMetadataExtractor.MyEmitter;

public class TestBaseArticleMetadataExtractor extends LockssTestCase {
  static Logger log = Logger.getLogger("TestBaseArticleMetadataExtractor");
  private static SimulatedArchivalUnit sau0,sau1;
  private MockLockssDaemon theDaemon;
  private PluginManager pluginManager;
  protected static String cuRole = null;
  private boolean disableMetadataManager = false;

  public void setUp() throws Exception {
    super.setUp();
    final String tempDirPath = getTempDir().getAbsolutePath();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    pluginManager = theDaemon.getPluginManager();
    pluginManager.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginManager.startService();
    theDaemon.getCrawlManager();
    Boolean.toString(!disableMetadataManager && true);
    Properties props = new Properties(); 
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigurationUtil.fromProps(props);
    Tdb tdb = new Tdb();
    // create Tdb for testing purposes
    Properties tdbProps = new Properties();
    tdbProps = new Properties();
    tdbProps.setProperty("title", "Title[10.0135/12345678]");
    tdbProps.setProperty("attributes.isbn", "976-1-58562-317-7");
    tdbProps.setProperty("journalTitle", "Journal[10.0135/12345678]");
    tdbProps.setProperty("attributes.publisher", "Publisher[10.0135/12345678]");
    tdbProps.setProperty("plugin", "org.lockss.daemon.TestBaseArticleMetadataExtractor$MySimulatedPlugin0");
    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://www.title3.org/");
    tdb.addTdbAuFromProperties(tdbProps);
   
    config.setTdb(tdb);
    ConfigurationUtil.installConfig(config);
    
    config = simAuConfig(tempDirPath + "/0");
    config.put("volume", "XI");
    config.put("base_url", "http://www.title0.org/");
    sau0 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin0.class, config);
    config = simAuConfig(tempDirPath + "/1");
    config.put("base_url", "http://www.title1.org/");
    sau1 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin1.class, config);
    
    ((MySimulatedPluginE) sau0.getPlugin()).notifyAusTitleDbChanged();
    ((MySimulatedPluginE) sau1.getPlugin()).notifyAusTitleDbChanged();

 }
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
        SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  public void tearDown() throws Exception {
    sau0.deleteContentTree();
    sau1.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  public void testEmitter() throws IOException, PluginException {
    Configuration config = ConfigManager.getCurrentConfig();
    assertNotNull(config);
    
    Tdb tdb = config.getTdb();
    assertNotNull(tdb);
    
    Plugin plugin0 = sau0.getPlugin();
    ArticleMetadataExtractor metadataExtractor0 = 
        plugin0.getArticleMetadataExtractor(MetadataTarget.OpenURL, sau0);
   
    Emitter myEmitter0 = new Emitter() {
      @Override
      public void emitMetadata(ArticleFiles af, ArticleMetadata metadata) {
        log.debug("hello from emitMetadata");
      }
    };
    
    Iterator<ArticleFiles> articleItr = 
        sau0.getArticleIterator(MetadataTarget.OpenURL);
    while (articleItr.hasNext()) {
      ArticleFiles af = articleItr.next();
      metadataExtractor0.extract(MetadataTarget.Article, af, myEmitter0);
    }
 }
  
  public static class MySubTreeArticleIteratorFactory
    implements ArticleIteratorFactory {
    String pat;
    public MySubTreeArticleIteratorFactory(String pat) {
      this.pat = pat;
    }
  
    /**
     * Create an Iterator that iterates through the AU's articles, pointing
     * to the appropriate CachedUrl of type mimeType for each, or to the
     * plugin's choice of CachedUrl if mimeType is null
     * @param au the ArchivalUnit to iterate through
     * @return the ArticleIterator
     */
    @Override
    public Iterator<ArticleFiles> 
      createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
        
      Iterator<ArticleFiles> ret;
      SubTreeArticleIterator.Spec spec = 
        new SubTreeArticleIterator.Spec().setTarget(target);
        
      if (pat != null) {
       spec.setPattern(pat);
      }
        
      ret = new SubTreeArticleIterator(au, spec);
      log.debug(  "creating article iterator for au " + au.getName() 
          + " hasNext: " + ret.hasNext());
      return ret;
    }
  }

  
  public static class MySimulatedPluginE extends SimulatedPlugin {
    ArticleMetadataExtractor simulatedArticleMetadataExtractor = null;
   
     /**
     * Returns the article iterator factory for the mime type, if any
     * @param contentType the content type
     * @return the ArticleIteratorFactory
     */
    @Override
    public ArticleIteratorFactory getArticleIteratorFactory() {
      MySubTreeArticleIteratorFactory ret =
          new MySubTreeArticleIteratorFactory(null); //"branch1/branch1");
      return ret;
    }
    @Override
    public ArticleMetadataExtractor 
      getArticleMetadataExtractor(MetadataTarget target, ArchivalUnit au) {
      return simulatedArticleMetadataExtractor;
    }
    
    @Override
    public void notifyAusTitleDbChanged() {
      super.notifyAusTitleDbChanged();
    }
  }
  
   
  public static class MySimulatedPlugin0 extends MySimulatedPluginE {
    public MySimulatedPlugin0() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        int articleNumber = 0;
        @Override
        public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
          throws IOException, PluginException {
          ArticleMetadata md = new ArticleMetadata();
          articleNumber++;
          md.put(MetadataField.FIELD_ISSN,"0740-2783");
          md.put(MetadataField.FIELD_VOLUME,"XI");
          if (articleNumber < 10) {
            md.put(MetadataField.FIELD_ISSUE,"1st Quarter");
            md.put(MetadataField.FIELD_DATE,"2010-Q1");
            md.put(MetadataField.FIELD_START_PAGE,"" + articleNumber);
          } else {
            md.put(MetadataField.FIELD_ISSUE,"2nd Quarter");
            md.put(MetadataField.FIELD_DATE,"2010-Q2");
            md.put(MetadataField.FIELD_START_PAGE,"" + (articleNumber-9));
          }
          String doiPrefix = "10.1234/12345678";
          String doi = doiPrefix + "."
      + md.get(MetadataField.FIELD_DATE) + "."
      + md.get(MetadataField.FIELD_START_PAGE); 
          md.put(MetadataField.FIELD_DOI, doi);
        //  md.put(MetadataField.FIELD_JOURNAL_TITLE,"Journal[" + doiPrefix + "]");
          md.put(MetadataField.FIELD_ARTICLE_TITLE,"Title[" + doi + "]");
          md.put(MetadataField.FIELD_AUTHOR,"Author[" + doi + "]");
          md.put(MetadataField.FIELD_ACCESS_URL, 
           "http://www.title0.org/plugin0/XI/"
             +  md.get(MetadataField.FIELD_ISSUE) 
             +"/p" + md.get(MetadataField.FIELD_START_PAGE));
          BaseArticleMetadataExtractor bm = new BaseArticleMetadataExtractor(); 
          MyEmitter myEmitter = bm.new MyEmitter(af, emitter);
          CachedUrl cu = getCuToExtract(af);
          myEmitter.emitMetadata(cu, md);
        }
      };
    }
      protected CachedUrl getCuToExtract(ArticleFiles af) {
        return cuRole != null ? af.getRoleCu(cuRole) : af.getFullTextCu();
      }
    
    public ExternalizableMap getDefinitionMap() {
      ExternalizableMap map = new ExternalizableMap();
      map.putString("au_start_url", "\"%splugin0/%s\", base_url, volume");
      map.putString("au_volume_url", "\"%splugin0/%s/toc\", base_url, volume");
      map.putString("au_issue_url", "\"%splugin0/%s/%s/toc\", base_url, volume, issue");
      map.putString("au_title_url", "\"%splugin0/toc\", base_url");
      return map;
    }
  }
  
    
    
  public static class MySimulatedPlugin1 extends MySimulatedPluginE {
    public MySimulatedPlugin1() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        int articleNumber = 0;
    public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
          throws IOException, PluginException {
          ArticleMetadata md = new ArticleMetadata();
          articleNumber++;
          md.put(MetadataField.FIELD_ACCESS_URL, 
           "http://www.title0.org/plugin0/XI/"
             +  md.get(MetadataField.FIELD_ISSUE) 
             +"/p" + md.get(MetadataField.FIELD_START_PAGE));
          emitter.emitMetadata(af, md);
        }
      };
    }
    public ExternalizableMap getDefinitionMap() {
      ExternalizableMap map = new ExternalizableMap();
      map.putString("au_start_url", "\"%splugin1/%s\", base_url, volume");
      map.putString("au_volume_url", "\"%splugin1/%s/toc\", base_url, volume");
      map.putString("au_issue_url", "\"%splugin1/%s/%s/toc\", base_url, volume, issue");
      map.putString("au_title_url", "\"%splugin1/toc\", base_url");
      return map;
    }
 
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
        SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }
  
  protected static void addAccessUrl(ArticleMetadata am, ArticleFiles af) {
    if (!am.hasValidValue(MetadataField.FIELD_ACCESS_URL)) {
      am.put(MetadataField.FIELD_ACCESS_URL, af.getFullTextUrl());
    }
  }
 
  protected CachedUrl getCuToExtract(ArticleFiles af) {
    return cuRole != null ? af.getRoleCu(cuRole) : af.getFullTextCu();
  }
  
 
  }
}

