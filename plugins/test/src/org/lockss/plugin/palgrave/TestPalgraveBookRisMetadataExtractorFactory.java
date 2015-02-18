/* $Id$

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.palgrave;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

// Sample Palgrave Book RIS Citation Export found:
// http://www.palgraveconnect.com/pc/browse/citationExport?isbn=9781137024497&WT.cg_n=eBooks&WT.cg_s=Citation%20Export
// TY  - BOOK
// AU  - Larsson, Mats
// PY  - 2012/09/25
// TI  - The Business of Global Energy Transformation: Saving Billions through Sustainable Models
// PB  - Palgrave Macmillan
// CY  - Basingstoke
// DA  - 2014/07/23
// SN  - 9781137024497
// ID  - 10.1057/9781137024497
// UR  - http://dx.doi.org/10.1057/9781137024497
// L1  - http://www.palgraveconnect.com/pc/busman2013/browse/inside/download/9781137024497.pdf
// ER  -
public class TestPalgraveBookRisMetadataExtractorFactory extends LockssTestCase {

  static Logger log = Logger.getLogger("TestPalgraveBookRisMetadataExtractorFactory");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // simulated au to generate content
  private ArchivalUnit pau; // palgrave book au
  private static final String PLUGIN_NAME = "org.lockss.plugin.palgrave.ClockssPalgraveBookPlugin";
  private static final String BASE_URL = "http://www.palgraveconnect.com/";
  private static final String BOOK_ISBN = "9781137024497";
  private static final String SIM_ROOT = BASE_URL + "pc";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
                                             simAuConfig(tempDirPath));
    pau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, palgraveBookAuConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("depth", "1");
    conf.put("branch", "2");
    conf.put("numFiles", "2");
    conf.put("fileTypes","" + (SimulatedContentGenerator.FILE_TYPE_PDF 
                               + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }

  // Configuration method. 
  Configuration palgraveBookAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("book_isbn", BOOK_ISBN);
    return conf;
  }

  // the metadata that should be extracted
  // PalgraveBook seems to now use DA for the date the citation was accessed
  // and PY for publication year, so we now ignore DA
  String goodAuthors[] = {"Larsson, Mats"};
  String goodDate = "2012";
  String goodJournalTitle = "The Business of Global Energy Transformation: Saving Billions through Sustainable Models";
  String goodPublisher = "Palgrave Macmillan";
  String goodBookIsbn = "9781137024497";
  String goodDoi = "10.1057/9781137024497";
  String goodAccessUrl = "http://www.palgraveconnect.com/pc/busman2013/browse/inside/download/9781137024497.pdf";
  String goodDA = "2014/07/23";

  private String createGoodContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("TY  - BOOK");
    for(String auth : goodAuthors) {
      sb.append("\nAU  - ");
      sb.append(auth);
    }
    sb.append("\nPY  - ");
    sb.append(goodDate);
    sb.append("\nTI  - ");
    sb.append(goodJournalTitle);
    sb.append("\nPB  - ");
    sb.append(goodPublisher);
    sb.append("\nSN  - ");
    sb.append(goodBookIsbn);
    sb.append("\nDA  - ");
    sb.append(goodDA);
    sb.append("\nDO  - ");
    sb.append(goodDoi);
    sb.append("\nL1  - ");
    sb.append(goodAccessUrl);
    sb.append("\nER  -");
    return sb.toString();
  }
      
  // Method that creates a simulated Cached URL from the source code 
  // provided by the goodContent string. It then asserts that the metadata 
  // extracted with PalgraveBookRisMetadataExtractorFactory
  // match the metadata in the source code. 
  public void testExtractFromGoodContent() throws Exception {
    String goodContent = createGoodContent();
    //log.info(goodContent);
    // ris file which the metadata extracted from.
    String url = "http://www.palgraveconnect.com/pc/browse/citationExport?doi=10.1057/9781137024497&WT.cg_n=eBooks&WT.cg_s=Citation%20Export";
    MockCachedUrl cu = new MockCachedUrl(url, pau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_RIS);
    
    // Create FileMetadataExtractor object through PalgraveBookRisMetadataExtractor().
    FileMetadataExtractor me = 
          new PalgraveBookRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), Constants.MIME_TYPE_RIS);
    // Create the metadata list containing all articles for this AU.
    // In this test case, the list has only one item.
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
System.out.println(md.toString());
    Iterator<String> actAuthIter = md.getList(MetadataField.FIELD_AUTHOR).iterator();
    for(String expAuth : goodAuthors) {
    	assertEquals(expAuth, actAuthIter.next());
    }
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodBookIsbn, md.get(MetadataField.FIELD_EISBN));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodAccessUrl, md.get(MetadataField.FIELD_ACCESS_URL));
  }
  
  // Inner class that where a number of Archival Units can be created
  // for simulated content.
  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
        throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }
  }

  // Inner class to create citation export ris source code simulated content.
  public static class MySimulatedContentGenerator extends
    SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }
  }

}