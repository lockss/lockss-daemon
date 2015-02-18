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

package org.lockss.plugin;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.SubTreeArticleIterator.Spec;
import org.lockss.extractor.*;
import org.lockss.plugin.simulated.*;

public class TestSubTreeArticleIterator extends LockssTestCase {
  static Logger log = Logger.getLogger("TestSubTreeArticleIterator");

  static final String BASE_URL = "http://example.org/wombat/";

  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;
  private static int urlCount = 32;
  private static int testExceptions = 3;
  String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "3");
    conf.put("branch", "2");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", ""+fileSize);
    return conf;
  }

  private void crawlSimAu(SimulatedArchivalUnit sau) throws IOException {
    PluginTestUtil.crawlSimAu(sau);
    // Ensure there's at least one content file with a child, as that was a
    // failing case in SubTreeArticleIterator
    String url = "http://example.org/wombat/branch1/001file.html";
    CachedUrl cu = sau.makeCachedUrl(url);
    assertTrue(cu.hasContent());
    String url2 = url + "/child.html";
    CIProperties props =
        CIProperties.fromProperties(PropUtil.fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html"));
    UrlCacher uc = sau.makeUrlCacher(
        new UrlData(new StringInputStream("child content"), props, url2));
    uc.storeContent();
  }

  // Multiple tests combined just to avoid recreating simulated content
  public void testLots() throws Exception {
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath + "1/"));
    crawlSimAu(sau);

    MySubTreeArticleIterator iter =
      new MySubTreeArticleIterator(sau, new Spec());
    iter.hasNext();
    assertClass(CuContentIterator.class, iter.getCuIterator());

    MySubTreeArticleIterator iter2 =
      new MySubTreeArticleIterator(sau,
				   new Spec().setVisitArchiveMembers(true));
    iter2.hasNext();
    assertClass(BaseCachedUrlSet.ArcMemIterator.class, iter2.getCuIterator());

    doTestArticleCounts();

    doTestCreateArticleFiles();

    doTestCustomVisitArticleCu();

    doTestIncludeSubTree();
    doTestExcludeSubTree();

    doTestIncludeFilesChangedAfter();
  }

  void doTestArticleCounts() {
    assertEquals(226, countArticles(new Spec()));
    assertEquals(121, countArticles(new Spec().setMimeType("text/html")));
    assertEquals(105, countArticles(new Spec().setMimeType("application/pdf")));
    assertEquals(121, countArticles(new Spec().setPattern("\\.html")));
    assertEquals(15, countArticles(new Spec().setPattern("index\\.html")));
    assertEquals(105, countArticles(new Spec().setPattern("\\.pdf")));
    assertEquals(0, countArticles(new Spec()
				  .setMimeType("text/html")
				  .setPattern("\\.pdf")));

    assertEquals(0, countArticles(new Spec()
				  .setRoot("http://example.com/")));
    assertEquals(226, countArticles(new Spec()
				    .setRoot("http://example.org/")));
    assertEquals(226,
		 countArticles(new Spec()
			       .setRoots(ListUtil.list("http://example.com/",
						       "http://example.org/"))));
    assertEquals(106, countArticles(new Spec().setRoot(BASE_URL + "branch1")));
    assertEquals(45, countArticles(new Spec().setRoot(BASE_URL +
						       "branch2/branch1")));
    assertEquals(45, countArticles(new Spec().setRoot(BASE_URL +
						       "branch2/branch2")));
    assertEquals(196,
		 countArticles(new Spec()
			       .setRoots(ListUtil.list(BASE_URL + "branch1",
						       BASE_URL + "branch2/branch1",
						       BASE_URL + "branch2/branch2"))));
    assertEquals(226, countArticles(new Spec()
				    .setRootTemplate("\"%s\",base_url")));
    assertEquals(106, countArticles(new Spec()
				    .setRootTemplate("\"%sbranch1\",base_url")));
    assertEquals(49, countArticles(new Spec()
				   .setRootTemplate("\"%sbranch1\",base_url")
				   .setPattern("\\.pdf")));
    assertEquals(49, countArticles(new Spec()
				   .setRootTemplate("\"%sbranch1\",base_url")
				   .setPattern("\\.pdf$")));
    assertEquals(49, countArticles(new Spec()
				   .setRootTemplate("\"%sbranch1\",base_url")
				   .setPattern("^.*\\.pdf$")));
    assertEquals(98, countArticles(new Spec()
				   .setRootTemplates(ListUtil.list("\"%sbranch1\",base_url",
								   "\"%sbranch2\",base_url"))
				   .setPattern("^.*\\.pdf$")));
    assertEquals(0, countArticles(new Spec()
				   .setRootTemplate("\"%sbranch1\",base_url")
				   .setPattern("^\\.pdf")));
  }

  void doTestIncludeFilesChangedAfter() {
    SubTreeArticleIterator.Spec specRec = new SubTreeArticleIterator.Spec()
      .setTarget(new MetadataTarget().setIncludeFilesChangedAfter(123456))
      .setVisitArchiveMembers(true);

    MySubTreeArticleIterator iter = new MySubTreeArticleIterator(sau, specRec);
    Collection<CachedUrlSet> roots = iter.makeRoots();
    assertEquals(1, roots.size());
    for (CachedUrlSet cus : roots) {
      assertEquals(123456,
		   ((BaseCachedUrlSet)cus).getExcludeFilesUnchangedAfter());
    }
  }

  void doTestCreateArticleFiles() {
    Spec s1 = new Spec().setPatternTemplate("\"00%dfile\\.html\",branch");
    Spec s2 = new Spec().setPattern("002file\\.html");
    List<ArticleFiles> l1 = getArticles(s1);
    assertEquals(15, l1.size());

    assertEquals(getFullTextUrls(l1), getFullTextUrls(getArticles(s2)));

    Spec s3 = new Spec().setPattern("branch2/002file\\.html");
    String a = "http://example.org/wombat/";
    String b = "branch2/002file.html";
    List<String> exp = ListUtil.list(a+"branch1/branch1/"+b,
			     a+"branch1/"+b,
			     a+"branch1/branch2/"+b,
			     a+""+b,
			     a+"branch2/branch1/"+b,
			     a+"branch2/"+b,
			     a+"branch2/branch2/"+b);

    assertEquals(exp,  getFullTextUrls(getArticles(s3)));

    // Test overriding createArticleFiles() to return a non-default
    // ArticleFiles
    List<ArticleFiles> l2 = getArticles(new SubTreeArticleIterator(sau, s3) {
	protected ArticleFiles createArticleFiles(CachedUrl cu) {
	  ArticleFiles res = new ArticleFiles();
	  res.setFullTextCu(cu);
	  res.setRoleString("akey", cu.getUrl() + "arole");
	  return res;
	}
      });
    assertEquals(concatEach(exp, "arole"), getRoleStrings(l2, "akey"));
  }

  public void testFlags() throws Exception {
    Configuration config2 = simAuConfig(tempDirPath + "1/");
    // arrange for different numbers of upper and lower case names
    config2.put("branch", "3");
    config2.put("depth", "2");
    config2.put("mixed_case", "true");
    sau = PluginTestUtil.createAndStartSimAu(config2);
    crawlSimAu(sau);

    assertEquals(151, countArticles(new Spec()
				    .setPattern("/branch")));
    assertEquals(181, countArticles(new Spec()
				    .setPattern("/branch",
						Pattern.CASE_INSENSITIVE)));
    assertEquals(90, countArticles(new Spec()
				   .setPattern("/BRANCH")));
    assertEquals(181, countArticles(new Spec()
				    .setPattern("/BRANCH",
						Pattern.CASE_INSENSITIVE)));

    assertEquals(8,
		 countArticles(new Spec()
			       .setPatternTemplate("\"/branch[0-9]+/00%dfile\\.html\",branch")));
    assertEquals(12,
		 countArticles(new Spec()
			       .setPatternTemplate("\"/branch[0-9]+/00%dfile\\.html\",branch",
						   Pattern.CASE_INSENSITIVE)));
  }

  // Test overriding visitArticleCu() to emit zero, one, two or three
  // ArticleFiles per CU
  void doTestCustomVisitArticleCu() {
    String a = "http://example.org/wombat/";
    Spec s3 = new Spec().setPattern("branch2/002file\\.html");
    List<String> exp3 =
      ListUtil.list(a+"branch1/branch2/002file.html",
		    a+"branch1/branch2/branch2/002file.html",
		    a+"branch2/002file.html",
		    a+"branch2/branch1/branch2/002file.html",
		    a+"branch2/branch1/branch2/002file.html/v2",
		    a+"branch2/branch2/002file.html",
		    a+"branch2/branch2/002file.html/v2",
		    a+"branch2/branch2/002file.html/v3",
		    a+"branch2/branch2/branch2/002file.html",
		    a+"branch2/branch2/branch2/002file.html/v2",
		    a+"branch2/branch2/branch2/002file.html/v3");

    List<ArticleFiles> l3 = getArticles(new SubTreeArticleIterator(sau, s3) {
	protected void visitArticleCu(CachedUrl cu) {
	  String url = cu.getUrl();
	  ArchivalUnit au = cu.getArchivalUnit();
	  ArticleFiles af = new ArticleFiles();
	  af.setFullTextCu(cu);
	  if (url.matches(".*bat/branch1/branch1.*")) {
	    // generate nothing for branch1/branch1
	    return;
	  } else if (url.matches(".*bat/branch1/branch2.*")) {
	    // generate one ArticleFiles for branch1/branch2
	    emitArticleFiles(af);
	  } else if (url.matches(".*bat/branch2/branch1.*")) {
	    // generate two ArticleFiles for branch2/branch1
	    emitArticleFiles(af);
	    ArticleFiles af2 = new ArticleFiles();
	    af2.setFullTextCu(au.makeCachedUrl(url + "/v2"));
	    emitArticleFiles(af2);
	  } else if (url.matches(".*bat/branch2/branch2.*")) {
	    // generate three ArticleFiles for branch2/branch2
	    emitArticleFiles(af);
	    ArticleFiles af2 = new ArticleFiles();
	    af2.setFullTextCu(au.makeCachedUrl(url + "/v2"));
	    emitArticleFiles(af2);
	    ArticleFiles af3 = new ArticleFiles();
	    af3.setFullTextCu(au.makeCachedUrl(url + "/v3"));
	    emitArticleFiles(af3);
	  } else {
	    // generate one ArticleFiles for everything else
	    emitArticleFiles(af);
	  }
	}
      });
    assertEquals(exp3, getFullTextUrls(l3));
  }

  // Test including only specified subtrees
  void doTestIncludeSubTree() {
    // whole tree, for comparison
    Spec s1 = new Spec();
    MySubTreeArticleIterator iter1 = new MySubTreeArticleIterator(sau, s1);
    iter1.setRecordPredInvocations();
    List<ArticleFiles> art1 = getArticles(iter1);
    assertEquals(226, art1.size());
    assertEquals(226, iter1.getPredInvocations().size());

    // add a pattern that matches a subtree, reduces article count but not
    // number of nodes visited
    Spec s2 = new Spec().setPatternTemplate("\"%sbranch2\",base_url");
    MySubTreeArticleIterator iter2 = new MySubTreeArticleIterator(sau, s2);
    iter2.setRecordPredInvocations();
    List<ArticleFiles> art2 = getArticles(iter2);
    assertEquals(105, art2.size());
    assertEquals(226, iter2.getPredInvocations().size());

    // replace pattern with a subtree include pattern, should reduce number
    // of nodes visited
    Spec s3 = new Spec().setIncludeSubTreePatternTemplate("\"%sbranch2\",base_url");
    MySubTreeArticleIterator iter3 = new MySubTreeArticleIterator(sau, s3);
    iter3.setRecordPredInvocations();
    List<ArticleFiles> art3 = getArticles(iter3);
    assertEquals(105, art3.size());
    assertEquals(105, iter3.getPredInvocations().size());

    // Same thing with explicit root, which is slightly different case in
    // SubTreeArticleIterator
    Spec s4 = new Spec()
      .setRoot("http://example.org")
      .setIncludeSubTreePatternTemplate("\"%sbranch2\",base_url");
    MySubTreeArticleIterator iter4 = new MySubTreeArticleIterator(sau, s4);
    iter4.setRecordPredInvocations();
    List<ArticleFiles> art4 = getArticles(iter4);
    assertEquals(105, art4.size());
    assertEquals(105, iter4.getPredInvocations().size());
  }
    
  // Test excluding specified subtrees
  void doTestExcludeSubTree() {
    // whole tree, for comparison
    Spec s1 = new Spec().setRoot("http://example.org");
    MySubTreeArticleIterator iter1 = new MySubTreeArticleIterator(sau, s1);
    iter1.setRecordPredInvocations();
    List<ArticleFiles> art1 = getArticles(iter1);
    assertEquals(226, art1.size());
    assertEquals(226, iter1.getPredInvocations().size());

    // add a pattern that matches a subtree, reduces article count but not
    // number of nodes visited
    Spec s2 = new Spec()
      .setRoot("http://example.org")
      .setPatternTemplate("\"%sbranch[13]\",base_url");
    MySubTreeArticleIterator iter2 = new MySubTreeArticleIterator(sau, s2);
    iter2.setRecordPredInvocations();
    List<ArticleFiles> art2 = getArticles(iter2);
    assertEquals(106, art2.size());
    assertEquals(226, iter2.getPredInvocations().size());

    // replace pattern with a subtree exclude pattern, should reduce number
    // of nodes visited
    Spec s3 = new Spec()
      .setRoot("http://example.org")
      .setExcludeSubTreePatternTemplate("\"%sbranch2\",base_url");
    MySubTreeArticleIterator iter3 = new MySubTreeArticleIterator(sau, s3);
    iter3.setRecordPredInvocations();
    List<ArticleFiles> art3 = getArticles(iter3);
    assertEquals(121, art3.size());
    assertEquals(121, iter3.getPredInvocations().size());
  }

  public void testException() throws Exception {
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath + "1/"));
    crawlSimAu(sau);

    int count = 0;
    
    for (Iterator<ArticleFiles> it =
	   new MySubTreeArticleIterator(sau,
					(new Spec()
					 .setPattern("branch1/branch1")),
					testExceptions);
	 it.hasNext(); ) {
      ArticleFiles af = it.next();
      CachedUrl cu = af.getFullTextCu();
      assertNotNull(cu);
      assertTrue(""+cu.getClass(), cu instanceof CachedUrl);
      log.debug("count " + count + " url " + cu.getUrl());
      count++;
    }
    log.debug("Article count is " + count);
    assertEquals(60 - testExceptions, count);
  }

  int countArticles(Spec spec) {
    return getArticles(spec).size();
  }

  List<ArticleFiles> getArticles(Spec spec) {
    return getArticles(new SubTreeArticleIterator(sau, spec));
  }

  List<String> getFullTextUrls(List<ArticleFiles> aflist) {
    List<String> res = new ArrayList<String>();
    for (ArticleFiles af : aflist) {
      res.add(af.getFullTextUrl());
    }
    return res;
  }

  List<String> getRoleStrings(List<ArticleFiles> aflist, String role) {
    List<String> res = new ArrayList<String>();
    for (ArticleFiles af : aflist) {
      res.add(af.getRoleString(role));
    }
    return res;
  }

  List<ArticleFiles> getArticles(SubTreeArticleIterator it) {
    List<ArticleFiles> res = new ArrayList<ArticleFiles>();
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.debug2("iter af: " + af);
      CachedUrl cu = af.getFullTextCu();
      assertNotNull(cu);
      res.add(af);
    }
    return res;
  }

  List<String> concatEach(List<String> lst, String suffix) {
    List<String> res = new ArrayList<String>();
    for (String str : lst) {
      res.add(str + suffix);
    }
    return res;
  }
      
  static class MySubTreeArticleIterator extends SubTreeArticleIterator {
    int exceptionCount;
    List<CachedUrl> predList = null;

    MySubTreeArticleIterator(ArchivalUnit au, Spec spec) {
      this(au, spec, 0);
    }

    MySubTreeArticleIterator(ArchivalUnit au, Spec spec, int exceptionCount) {
      super(au, spec);
      this.exceptionCount = exceptionCount;
    }

    @Override
    protected boolean isArticleCu(CachedUrl cu) {
      if (predList != null) {
	predList.add(cu);
      }
      if (super.isArticleCu(cu)) {
	if (exceptionCount > 0 && cu.getUrl().endsWith(".html")) {
	  exceptionCount--;
	  throw new UnsupportedOperationException("expected");
	}
	return true;
      }
      return false;
    }

    Iterator<?> getCuIterator() {
      return cuIter;
    }

    void setRecordPredInvocations() {
      predList = new ArrayList<CachedUrl>();
    }

    List<CachedUrl> getPredInvocations() {
      return predList;
    }
  }

  static class AF extends ArticleFiles {
    AF(String url) {
      super();
      fullTextCu = new MockCachedUrl(url);
    }
  }
}
