/*
 * $Id$
 */
/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.bmc;

import java.io.File;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.TitleConfig;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.ArticleMetadataExtractor.Emitter;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestBioMedCentralPluginArticleIteratorFactory extends
    ArticleIteratorTestCase {

  /**
   * Simulated AU to generate content
   */
  protected static Logger log = Logger.getLogger("TestBioMedCentralPluginArticleIteratorFactory");
  private SimulatedArchivalUnit sau;
  private static String PLUGIN_NAME = "org.lockss.plugin.bmc.ClockssBioMedCentralPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.jcheminf.com/ ";
  private final String JOURNAL_ISSN = "1751-0147";
  private final String VOLUME_NAME = "54";
  private static final int DEFAULT_FILESIZE = 3000;
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
      JOURNAL_ISSN_KEY, JOURNAL_ISSN,
            VOLUME_NAME_KEY, VOLUME_NAME);

  protected String cuRole = null;
  ArticleMetadataExtractor.Emitter emitter;
  protected boolean emitDefaultIfNone = false;
  FileMetadataExtractor me = null;
  MetadataTarget target;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    au = createAu();
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("journal_issn", JOURNAL_ISSN);
    conf.put("volume_name", VOLUME_NAME);
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put("fileTypes",
        "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
              | SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

/*  http://www.infectagentscancer.com/content/pdf/1750-9378-5-9.pdf
    where the xxxx-yyyy = ISSN, and X-Y = Issue#-Article#
  http://breast-cancer-research.com/content/pdf/bcr3224.pdf
    the (journal abbrev) letters don't have a strict pattern (2-5 chars), 
    nor do the trailing numbers
  http://genomebiology.com/content/pdf/gb-2012-13-12-r126.pdf
  http://genomebiology.com/content/pdf/gb-2001-2-7-research0026.pdf
    we have the two-char name, '-' the year (but we don't have a year param)
    '-' volume# '-' issue# '-' paperIdentifier.pdf
  genome biology also has some substance that is non-pdf:
  http://genomebiology.com/2002/3/2/reports/2010
  http://genomebiology.com/2002/3/2/spotlight-20020206-01
 
  
  */

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(pat, "hhttp://www.jcheminf.com/content/pdf/1471-2253-1-2.pdfll");
    assertNotMatchesRE(pat, "http://www.jcheminf.com/contentt/volume/1014174823t49006/j0143.pdfwrong");
    assertMatchesRE(pat, "http://www.jcheminf.com/content/54/1/6/abstract");
    assertNotMatchesRE(pat, "http://www.example.com/content/");
    assertNotMatchesRE(pat, "http://www.example.com/content/j");
    assertNotMatchesRE(pat, "http://www.example.com/content/j0123/j383.pdfwrong");
  }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat2 = "/content/pdf/([^/]+)\\.pdf";
    String rep2 = "/content/([^/]+)/(\\d)/([^/]+";
    PluginTestUtil.copyAu(sau, au, ".*", pat2, rep2);
    String url = "http://www.jcheminf.com/content/2/1/7/abstract";

    CachedUrl cu = au.makeCachedUrl(url);
    assertNotNull(cu);

    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = createArticleFiles(artIter, cu);
    assertNotNull(af);    
    //assertEquals(cu, af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF));

  }

}
