/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.extractor;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.AuUtil;
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
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.TitleConfig;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor.Emitter;
import org.lockss.extractor.BaseArticleMetadataExtractor.MyEmitter;
import org.apache.commons.collections.MultiHashMap;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.List; 

public class TestBaseArticleMetadataExtractor<Titleconfig> extends LockssTestCase {
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
    
    String rootPath0 = tempDirPath + "/0";
    String rootPath1 = tempDirPath + "/1";
    
    
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigurationUtil.fromProps(props);
    Tdb tdb = new Tdb();
    Tdb tdb1 = new Tdb();
    // create Tdb for testing purposes
    Properties tdbProps = new Properties();
    tdbProps = new Properties();
    tdbProps.setProperty("title", "Air & Space Volume 1");
    tdbProps.setProperty("attributes.isbn", "976-1-58562-317-7");
    tdbProps.setProperty("journalTitle", "Air and Space");
    tdbProps.setProperty("issn","0740-2783");
    tdbProps.setProperty("eissn","0740-2783");
    tdbProps.setProperty("attributes.publisher", "Publisher[10.0135/12345678]");
    tdbProps.setProperty("plugin", "org.lockss.extractor.TestBaseArticleMetadataExtractor$MySimulatedPlugin0");
    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://www.title0.org/");
    tdbProps.setProperty("param.2.key", "root");
    tdbProps.setProperty("param.2.value", rootPath0);
    tdb.addTdbAuFromProperties(tdbProps);
    
    
    tdbProps = new Properties();
    tdbProps.setProperty("title", "Air & Space Volume 2");
    tdbProps.setProperty("attributes.isbn", "976-1-58562-317-8");
    tdbProps.setProperty("journalTitle", "Air and Space2");
    tdbProps.setProperty("issn","0740-2784");
    tdbProps.setProperty("eissn","0740-2784");
    tdbProps.setProperty("attributes.publisher", "Publisher[10.0135/12345678]");
    tdbProps.setProperty("plugin", "org.lockss.extractor.TestBaseArticleMetadataExtractor$MySimulatedPlugin1");
    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://www.title1.org/");
    tdbProps.setProperty("param.2.key", "root");
    tdbProps.setProperty("param.2.value", rootPath1);
    tdb.addTdbAuFromProperties(tdbProps);
    
        
    config.setTdb(tdb);
    ConfigurationUtil.installConfig(config);
    config = simAuConfig(rootPath0);
    config.put("volume", "XI");
    config.put("base_url", "http://www.title0.org/");
    sau0 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin0.class, config);
    config = simAuConfig(rootPath1);
    config.put("base_url", "http://www.title1.org/");
    sau1 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin1.class, config);
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
  
  protected CachedUrl getCuToExtract(ArticleFiles af) {
    return cuRole != null ? af.getRoleCu(cuRole) : af.getFullTextCu();
  }
  
  
  public void testEmitter() throws IOException, PluginException {
    Configuration config = ConfigManager.getCurrentConfig();
    assertNotNull(config);
    Tdb tdb = config.getTdb();
    assertNotNull(tdb);
    
    TitleConfig tc = sau0.getTitleConfig();
    assertNotNull(tc);
    
    TdbAu tdbau = tc.getTdbAu();
    assertNotNull(tdbau);
    
    
    Plugin plugin0 = sau0.getPlugin();
    ArticleMetadataExtractor metadataExtractor0 = 
        plugin0.getArticleMetadataExtractor(MetadataTarget.OpenURL, sau0);
   
    Emitter myEmitter0 = new Emitter() {
      @Override
      public void emitMetadata(ArticleFiles af, ArticleMetadata am) {
     
        assertNotNull(am); 
        assertEquals("0740-2783",am.get(MetadataField.FIELD_ISSN));
        assertNotNull(am.get(MetadataField.FIELD_ISSN));
        assertNotNull(am.get(MetadataField.FIELD_ISBN));
        assertNotNull(am.get(MetadataField.FIELD_EISSN));
        assertNotNull(am.get(MetadataField.FIELD_VOLUME));
        
       } 
      
    };
  
    Plugin plugin1 = sau1.getPlugin();
    ArticleMetadataExtractor metadataExtractor1 = 
        plugin1.getArticleMetadataExtractor(MetadataTarget.OpenURL, sau1);
    
    Emitter myEmitter1 = new Emitter() {
      @Override
      public void emitMetadata(ArticleFiles af, ArticleMetadata am) {
     
        assertNotNull(am);
        assertEquals("0740-2784",am.get(MetadataField.FIELD_ISSN));
        assertNotNull(am.get(MetadataField.FIELD_ISSN));
        assertNotNull(am.get(MetadataField.FIELD_ISBN));
        assertNotNull(am.get(MetadataField.FIELD_VOLUME));
        assertNotNull(am.get(MetadataField.FIELD_ACCESS_URL));
        assertNotNull(am.get(MetadataField.FIELD_DOI));   
       } 
      
    };
     
    try {
      PluginTestUtil.crawlSimAu(sau0);
      PluginTestUtil.crawlSimAu(sau1);
      Iterator<ArticleFiles> ai = sau0.getArticleIterator();
      Iterator<ArticleFiles> ai0 = sau1.getArticleIterator();
      ArticleFiles af = ai.next();
      ArticleFiles af0 = ai0.next();
       metadataExtractor0.extract(MetadataTarget.OpenURL, af, myEmitter0);
       metadataExtractor1.extract(MetadataTarget.OpenURL, af0, myEmitter1);
    } catch (Exception e) {
       e.printStackTrace();
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
      return new BaseArticleMetadataExtractor();
    }
    
    @Override
    public void notifyAusTitleDbChanged() {
      super.notifyAusTitleDbChanged();
    }
  }
  
   
  public static class MySimulatedPlugin0 extends MySimulatedPluginE {
    ArticleMetadata md = new ArticleMetadata();
    
    @Override
    public FileMetadataExtractor
    getFileMetadataExtractor(MetadataTarget target,
           String contentType,
           ArchivalUnit au) {
      return  new  FileMetadataExtractor() {
       @Override
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException, PluginException {
       
          md.put(MetadataField.FIELD_ISSN,"abc");
          md.put(MetadataField.FIELD_VOLUME,"XI");
          md.put(MetadataField.FIELD_ACCESS_URL, 
           "http://www.title0.org/plugin0/XI/"
             +  md.get(MetadataField.FIELD_ISSUE) 
             +"/p" + md.get(MetadataField.FIELD_START_PAGE));
          emitter.emitMetadata(cu,md);
          
        }
      };
    }
   }
  
  
  
public static class MySimulatedPlugin1 extends MySimulatedPluginE {
  ArticleMetadata md = new ArticleMetadata();
  public FileMetadataExtractor
  getFileMetadataExtractor(MetadataTarget target,
         String contentType,
         ArchivalUnit au) {
    return  new  FileMetadataExtractor() {
      @Override
      public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
          throws IOException, PluginException {
       
        md.put(MetadataField.FIELD_ISSN,"junkvalue");
        md.put(MetadataField.FIELD_VOLUME,"XI");
        md.put(MetadataField.FIELD_ACCESS_URL, 
         "http://www.title0.org/plugin1/XI/"
           +  md.get(MetadataField.FIELD_ISSUE) 
           +"/p" + md.get(MetadataField.FIELD_START_PAGE));
        String doiPrefix = "10.1234/12345678";
        String doi = doiPrefix + "."
        + md.get(MetadataField.FIELD_DATE) + "."
        + md.get(MetadataField.FIELD_START_PAGE); 
        md.put(MetadataField.FIELD_DOI, doi);
        emitter.emitMetadata(cu, md);
      }
    };
  }
  
  
} 
}
  

    
  

  
  
  
 
  


