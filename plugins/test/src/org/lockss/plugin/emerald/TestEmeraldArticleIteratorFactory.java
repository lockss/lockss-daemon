/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.emerald;

import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestEmeraldArticleIteratorFactory extends ArticleIteratorTestCase {

  private SimulatedArchivalUnit sau; // Simulated AU to generate content

  private final String PLUGIN_NAME = "org.lockss.plugin.emerald.EmeraldPlugin";
  private static final int DEFAULT_FILESIZE = 3000;

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
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, emeraldAuConfig());
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_issn", "5555-5555");
    conf.put("volume_name", "16");
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put(
        "fileTypes",
        ""
            + (SimulatedContentGenerator.FILE_TYPE_HTML | SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  Configuration emeraldAuConfig() {
    return ConfigurationUtil.fromArgs("base_url", "http://www.example.com/",
        "journal_issn", "5555-5555", "volume_name", "16");
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.example.com/"), getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);

    assertNotMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=pdf");
    assertNotMatchesRE(
        pat,
        "http://www.wrong.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(
        pat,
        "http://www.example.com/journal.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(
        pat,
        "http://www.example.com/journals.html?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=0000-0000&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=5555-5555&volume=00&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=5555-5555&vol=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=5555-5555&volume=16&articleid=1465119&show=pdf&view=printarticle");
    assertNotMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid&show=html&view=printarticle");
    assertNotMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=show=html&view=printarticle");
    assertNotMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&=html&view=printarticle");
    assertNotMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=xml&view=printarticle");

    assertMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertMatchesRE(
        pat,
        "http://www.example.com/books.htm?issn=5555-5555&volume=16&chapterid=1465119&show=html&view=printarticle");
    assertMatchesRE(
        pat,
        "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertMatchesRE(
        pat,
        "http://www.example.com/books.htm?issn=5555-5555&volume=16&chapterid=1465119&show=html&view=printarticle");
  }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    // copy tree for HTML article pattern
    String pat1 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep1 = "journals.htm?issn=5555-5555&volume=16&issue=3&articleid=1482932&show=html&view=printarticle";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat1, rep1);

    // copy tree for PDF article pattern
    String pat2 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep2 = "journals.htm?issn=5555-5555&volume=16&issue=3&articleid=1482932&show=pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);

    // copy tree for article abstract pattern
    String pat3 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep3 = "journals.htm?issn=5555-5555&volume=16&issue=3&articleid=1482932&show=abstract";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat3, rep3);

    String htmlUrl = "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=3&articleid=1482932&show=html&view=printarticle";
    String pdfUrl = "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=3&articleid=1482932&show=pdf";
    String abstractUrl = "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=3&articleid=1482932&show=abstract";
    CachedUrl htmlCu = au.makeCachedUrl(htmlUrl);

    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = createArticleFiles(artIter, htmlCu);
    assertNotNull(af);
    assertEquals(htmlUrl, af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML));
    assertEquals(htmlUrl, af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML));
    assertEquals(pdfUrl, af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF));
    assertEquals(abstractUrl, af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT));
  }

}