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
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALLimport org.lockss.extractor.BaseArticleMetadataExtractor.MyEmitter;
import org.apache.commons.collections.MultiHashMap;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.extractor;

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
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

public class TestBaseArticleMetadataExtractor<Titleconfig> extends
    LockssTestCase {
  static Logger log = Logger.getLogger("TestBaseArticleMetadataExtractor");
  private static SimulatedArchivalUnit sau0, sau1, sau2, sau3, sau4;
  private MockLockssDaemon theDaemon;
  private PluginManager pluginManager;
  protected static String cuRole = null;
  private boolean disableMetadataManager = false;
  protected Exception IOException;

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
    String rootPath2 = tempDirPath + "/2";
    String rootPath3 = tempDirPath + "/3";
    String rootPath4 = tempDirPath + "/4";

    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);
    Configuration config = ConfigurationUtil.fromProps(props);
    Tdb tdb = new Tdb();

    // create Tdb for testing purposes
    Properties tdbProps = new Properties();
    tdbProps = new Properties();
    tdbProps.setProperty("title", "Air & Space Volume 1");
    tdbProps.setProperty("attributes.isbn", "976-1-58562-317-7");
    tdbProps.setProperty("journalTitle", "Air and Space");
    tdbProps.setProperty("issn", "0740-2783");
    tdbProps.setProperty("eissn", "0740-2783");
    tdbProps.setProperty("attributes.publisher", "Publisher[10.0135/12345678]");
    tdbProps
        .setProperty("plugin",
            "org.lockss.extractor.TestBaseArticleMetadataExtractor$MySimulatedPlugin0");
    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://www.title0.org/");
    tdbProps.setProperty("param.2.key", "root");
    tdbProps.setProperty("param.2.value", rootPath0);
    tdb.addTdbAuFromProperties(tdbProps);

    tdbProps = new Properties();
    tdbProps.setProperty("title", "Air & Space Volume 2");
    tdbProps.setProperty("attributes.isbn", "976-1-58562-317-8");
    tdbProps.setProperty("journalTitle", "Air and Space2");
    tdbProps.setProperty("issn", "0740-2784");
    tdbProps.setProperty("eissn", "0740-2784");
    tdbProps.setProperty("attributes.publisher", "Publisher[10.0135/12345678]");
    tdbProps
        .setProperty("plugin",
            "org.lockss.extractor.TestBaseArticleMetadataExtractor$MySimulatedPlugin1");
    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://www.title1.org/");
    tdbProps.setProperty("param.2.key", "root");
    tdbProps.setProperty("param.2.value", rootPath1);
    tdb.addTdbAuFromProperties(tdbProps);

    tdbProps = new Properties();
    tdbProps.setProperty("title", "Air & Space Volume 3");
    tdbProps.setProperty("attributes.isbn", "976-1-58562-317-9");
    tdbProps.setProperty("journalTitle", "Air and Space3");
    tdbProps.setProperty("issn", "0740-2785");
    tdbProps.setProperty("eissn", "0740-2785");
    tdbProps.setProperty("attributes.publisher", "Publisher[10.0135/12345679]");
    tdbProps
        .setProperty("plugin",
            "org.lockss.extractor.TestBaseArticleMetadataExtractor$MySimulatedPlugin2");
    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://www.title2.org/");
    tdbProps.setProperty("param.2.key", "root");
    tdbProps.setProperty("param.2.value", rootPath2);
    tdb.addTdbAuFromProperties(tdbProps);

    tdbProps = new Properties();
    tdbProps.setProperty("title", "Air & Space Volume 4");
    tdbProps.setProperty("attributes.isbn", "976-1-58562-315-9");
    tdbProps.setProperty("journalTitle", "Air and Space4");
    tdbProps.setProperty("issn", "0740-2786");
    tdbProps.setProperty("eissn", "0740-2786");
    tdbProps.setProperty("attributes.publisher", "Publisher[10.0139/12345679]");
    tdbProps
        .setProperty("plugin",
            "org.lockss.extractor.TestBaseArticleMetadataExtractor$MySimulatedPlugin4");
    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://www.title5.org/");
    tdbProps.setProperty("param.2.key", "root");
    tdbProps.setProperty("param.2.value", rootPath4);
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
    config = simAuConfig(rootPath2);
    config.put("volume", "XI");
    config.put("base_url", "http://www.title2.org/");
    sau2 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin2.class, config);
    config = simAuConfig(rootPath3);
    config.put("volume", "XI");
    config.put("base_url", "http://www.title3.org/");
    sau3 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin3.class, config);
    config.put("volume", "XI");
    config.put("base_url", "http://www.title4.org/");
    sau4 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin4.class, config);

  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put(
        "fileTypes",
        ""
            + (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
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
    ArticleMetadataExtractor metadataExtractor0 = plugin0
        .getArticleMetadataExtractor(MetadataTarget.OpenURL, sau0);

    Emitter myEmitter0 = new Emitter() {
      @Override
      public void emitMetadata(ArticleFiles af, ArticleMetadata am) {

        assertNotNull(am);
        assertEquals("0740-2783", am.get(MetadataField.FIELD_ISSN));
        assertNotNull(am.get(MetadataField.FIELD_ISSN));
        assertNotNull(am.get(MetadataField.FIELD_ISBN));
        assertNotNull(am.get(MetadataField.FIELD_EISSN));
        assertNotNull(am.get(MetadataField.FIELD_VOLUME));

      }

    };

    Plugin plugin3 = sau3.getPlugin();
    ArticleMetadataExtractor metadataExtractor3 = plugin3
        .getArticleMetadataExtractor(MetadataTarget.OpenURL, sau3);

    Emitter myEmitter3 = new Emitter() {
      @Override
      public void emitMetadata(ArticleFiles af, ArticleMetadata am) {

      }

    };

    Plugin plugin4 = sau4.getPlugin();
    ArticleMetadataExtractor metadataExtractor4 = plugin4
        .getArticleMetadataExtractor(MetadataTarget.OpenURL, sau4);

    Emitter myEmitter4 = new Emitter() {
      @Override
      public void emitMetadata(ArticleFiles af, ArticleMetadata am) {

      }

    };

    Plugin plugin1 = sau1.getPlugin();
    ArticleMetadataExtractor metadataExtractor1 = plugin1
        .getArticleMetadataExtractor(MetadataTarget.OpenURL, sau1);

    Emitter myEmitter1 = new Emitter() {
      @Override
      public void emitMetadata(ArticleFiles af, ArticleMetadata am) {

        assertNotNull(am);
        assertEquals("0740-2784", am.get(MetadataField.FIELD_ISSN));
        assertNotNull(am.get(MetadataField.FIELD_ISSN));
        assertNotNull(am.get(MetadataField.FIELD_ISBN));
        assertNotNull(am.get(MetadataField.FIELD_VOLUME));
        assertNotNull(am.get(MetadataField.FIELD_ACCESS_URL));
        assertNotNull(am.get(MetadataField.FIELD_DOI));
      }

    };

    Plugin plugin2 = sau2.getPlugin();
    FileMetadataExtractor fileMetaE = plugin2.getFileMetadataExtractor(
        MetadataTarget.OpenURL, "text", sau2);
    /***
     * For testing the cases for empty File MetadataExtractor
     */
    assertNull(fileMetaE);

    ArticleMetadataExtractor metadataExtractor2 = plugin2
        .getArticleMetadataExtractor(MetadataTarget.OpenURL, sau2);

    Emitter myEmitter2 = new Emitter() {

      @Override
      public void emitMetadata(ArticleFiles af, ArticleMetadata am) {
        assertNotNull(am);
        assertEquals("0740-2785", am.get(MetadataField.FIELD_ISSN));
        assertNotNull(am.get(MetadataField.FIELD_ISSN));
        assertNotNull(am.get(MetadataField.FIELD_ISBN));
        assertNotNull(am.get(MetadataField.FIELD_EISSN));
      }
    };

    try {
      PluginTestUtil.crawlSimAu(sau0);
      PluginTestUtil.crawlSimAu(sau1);
      PluginTestUtil.crawlSimAu(sau2);
      PluginTestUtil.crawlSimAu(sau3);

      Iterator<ArticleFiles> ai = sau0.getArticleIterator();
      Iterator<ArticleFiles> ai0 = sau1.getArticleIterator();
      Iterator<ArticleFiles> ai1 = sau2.getArticleIterator();
      Iterator<ArticleFiles> ai2 = sau3.getArticleIterator();

      ArticleFiles af = ai.next();
      ArticleFiles af0 = ai0.next();
      ArticleFiles af1 = ai1.next();
      ArticleFiles af2 = ai2.next();

      metadataExtractor0.extract(MetadataTarget.OpenURL, af, myEmitter0);
      metadataExtractor1.extract(MetadataTarget.OpenURL, af0, myEmitter1);
      metadataExtractor2.extract(MetadataTarget.OpenURL, af1, myEmitter2);
      metadataExtractor3.extract(MetadataTarget.OpenURL, af2, myEmitter3);
    } catch (Exception e) {
      fail();
    }
    try {
      PluginTestUtil.crawlSimAu(sau4);
      Iterator<ArticleFiles> ai3 = sau4.getArticleIterator();
      ArticleFiles af3 = ai3.next();
      metadataExtractor4.extract(MetadataTarget.OpenURL, af3, myEmitter4);

      fail();
    } catch (PluginException e) {

    }

  }

  public static class MySubTreeArticleIteratorFactory implements
      ArticleIteratorFactory {
    String pat;

    public MySubTreeArticleIteratorFactory(String pat) {
      this.pat = pat;
    }

    /**
     * Create an Iterator that iterates through the AU's articles, pointing to
     * the appropriate CachedUrl of type mimeType for each, or to the plugin's
     * choice of CachedUrl if mimeType is null
     * 
     * @param au
     *          the ArchivalUnit to iterate through
     * @return the ArticleIterator
     */
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
        MetadataTarget target) throws PluginException {

      Iterator<ArticleFiles> ret;
      SubTreeArticleIterator.Spec spec = new SubTreeArticleIterator.Spec()
          .setTarget(target);

      if (pat != null) {
        spec.setPattern(pat);
      }

      ret = new SubTreeArticleIterator(au, spec);
      log.debug("creating article iterator for au " + au.getName()
          + " hasNext: " + ret.hasNext());
      return ret;
    }
  }

  public static class MySimulatedPluginE extends SimulatedPlugin {
    /**
     * Returns the article iterator factory for the mime type, if any
     * 
     * @param contentType
     *          the content type
     * @return the ArticleIteratorFactory
     */
    @Override
    public ArticleIteratorFactory getArticleIteratorFactory() {
      MySubTreeArticleIteratorFactory ret = new MySubTreeArticleIteratorFactory(
          null); // "branch1/branch1");
      return ret;
    }

    @Override
    public ArticleMetadataExtractor getArticleMetadataExtractor(
        MetadataTarget target, ArchivalUnit au) {
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
    public FileMetadataExtractor getFileMetadataExtractor(
        MetadataTarget target, String contentType, ArchivalUnit au) {
      return new FileMetadataExtractor() {
        @Override
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException, PluginException {

          md.put(MetadataField.FIELD_ISSN, "abc");
          md.put(MetadataField.FIELD_VOLUME, "XI");
          md.put(
              MetadataField.FIELD_ACCESS_URL,
              "http://www.title0.org/plugin0/XI/"
                  + md.get(MetadataField.FIELD_ISSUE) + "/p"
                  + md.get(MetadataField.FIELD_START_PAGE));
          emitter.emitMetadata(cu, md);

        }
      };
    }
  }

  public static class MySimulatedPlugin2 extends MySimulatedPluginE {

    public FileMetadataExtractor getFileMetadataExtractor(
        MetadataTarget target, String contentType, ArchivalUnit au) {
      return null;
    }
  }

  public static class MySimulatedPlugin3 extends MySimulatedPluginE {
    public FileMetadataExtractor getFileMetadataExtractor(
        MetadataTarget target, String contentType, ArchivalUnit au) {
      return new FileMetadataExtractor() {
        @Override
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException, PluginException {
          cu = null;
          if (cu == null) {
            throw new IOException();
          }
        }
      };

    }
  }

  public static class MySimulatedPlugin4 extends MySimulatedPluginE {
    public FileMetadataExtractor getFileMetadataExtractor(
        MetadataTarget target, String contentType, ArchivalUnit au) {
      return new FileMetadataExtractor() {
        @Override
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException, PluginException {
          cu = null;
          if (cu == null) {
            throw new PluginException();
          }
        }
      };

    }
  }

  public static class MySimulatedPlugin1 extends MySimulatedPluginE {
    ArticleMetadata md = new ArticleMetadata();

    public FileMetadataExtractor getFileMetadataExtractor(
        MetadataTarget target, String contentType, ArchivalUnit au) {
      return new FileMetadataExtractor() {
        @Override
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException, PluginException {

          md.put(MetadataField.FIELD_ISSN, "junkvalue");
          md.put(MetadataField.FIELD_VOLUME, "XI");
          md.put(
              MetadataField.FIELD_ACCESS_URL,
              "http://www.title0.org/plugin1/XI/"
                  + md.get(MetadataField.FIELD_ISSUE) + "/p"
                  + md.get(MetadataField.FIELD_START_PAGE));
          String doiPrefix = "10.1234/12345678";
          String doi = doiPrefix + "." + md.get(MetadataField.FIELD_DATE) + "."
              + md.get(MetadataField.FIELD_START_PAGE);
          md.put(MetadataField.FIELD_DOI, doi);
          emitter.emitMetadata(cu, md);
        }
      };
    }

  }
}
