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

package org.lockss.exporter;

import java.io.*;
import java.util.*;
import java.net.*;

import org.archive.io.*;
import org.archive.io.warc.*;
import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.*;
import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.plugin.exploded.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.state.*;
import org.lockss.app.*;
import org.lockss.crawler.*;

/**
 * Functional tests for the WARC file crawler and
 * the WARC exporter.
 *
 * Uses WarcExporter to export simulated content,
 * replaces the simulated content with resulting
 * warc.gz file and the corresponding permission
 * page that links to that file. Verifies that
 * exploding the file does not lead to new
 * versions in the AU. Also tests crawling a WARC
 * file with additional URLs adds those to the AU
 * while leaving the orginial content intact.
 *
 * @author  Felix Ostrowski
 * @version 0.0
 */

public class FuncWarcRoundtrip extends LockssTestCase {

  public static final String INDEX_NAME = "index.html";
  protected MockLockssDaemon daemon;
  protected SimulatedArchivalUnit sau;
  int lastCrawlResult = Crawler.STATUS_UNKNOWN;
  String lastCrawlMessage = null;

  static String[] url = {
      "http://www.example.com/001file.bin",
      "http://www.example.com/002file.bin",
      "http://www.example.com/003file.bin",
      "http://www.example.com/branch1/001file.bin",
      "http://www.example.com/branch1/002file.bin",
      "http://www.example.com/branch1/003file.bin",
      "http://www.example.com/branch1/branch1/001file.bin",
      "http://www.example.com/branch1/branch1/002file.bin",
      "http://www.example.com/branch1/branch1/003file.bin",
      "http://www.example.com/branch1/branch1/index.html",
      "http://www.example.com/branch1/branch2/001file.bin",
      "http://www.example.com/branch1/branch2/002file.bin",
      "http://www.example.com/branch1/branch2/003file.bin",
      "http://www.example.com/branch1/branch2/index.html",
      "http://www.example.com/branch1/index.html",
      "http://www.example.com/branch2/001file.bin",
      "http://www.example.com/branch2/002file.bin",
      "http://www.example.com/branch2/003file.bin",
      "http://www.example.com/branch2/branch1/001file.bin",
      "http://www.example.com/branch2/branch1/002file.bin",
      "http://www.example.com/branch2/branch1/003file.bin",
      "http://www.example.com/branch2/branch1/index.html",
      "http://www.example.com/branch2/branch2/001file.bin",
      "http://www.example.com/branch2/branch2/002file.bin",
      "http://www.example.com/branch2/branch2/003file.bin",
      "http://www.example.com/branch2/branch2/index.html",
      "http://www.example.com/branch2/index.html",
      "http://www.example.com/index.html",
  };

  static String[] addurl = {
      "http://www.example.com/add.txt",
      "http://www.example.com/branch1/add.txt",
      "http://www.example.com/branch1/branch1/add.txt",
      "http://www.example.com/branch1/branch2/add.txt",
      "http://www.example.com/branch2/add.txt",
      "http://www.example.com/branch2/branch1/add.txt",
      "http://www.example.com/branch2/branch2/add.txt",
  };

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
		      tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    daemon.getPluginManager();
    daemon.setDaemonInited(true);
    daemon.getPluginManager().startService();
    daemon.getPluginManager().startLoadablePlugins();

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    sau.setUrlConsumerFactory(new ExplodingUrlConsumerFactory());
  }

  public void tearDown() throws Exception {
    daemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + SimulatedContentGenerator.FILE_TYPE_BIN);
    return conf;
  }

  public void testOriginalUrls() throws Exception {
    File exportDir = getTempDir();
    sau.generateContentTree();
    crawlContent();
    //doExport(exportDir, true, false, 5000);
    doExport(exportDir, true, false, -1);
    generateIndex(exportDir);
    sau.deleteContentTree();
    sau.moveToContentTree(exportDir);
    crawlContent();
    // Version of http://www.example.com/index.html should be 3
    checkExplodedUrls(url, 3);
  }

  public void testOriginalAndAdditionalUrls() throws Exception {
    File exportDir = getTempDir();
    sau.generateContentTree();
    crawlContent();
    //doExport(exportDir, true, false, 5000);
    doExport(exportDir, true, false, -1);

    // Add canned warc file containing additional urls
    String warcName = "warc-20110302184004-00000.warc.gz";
    InputStream in = null;
    OutputStream os = null;
    try {
        in = getResourceAsStream(warcName);
        File of = new File(exportDir.getAbsolutePath() + File.separator + warcName);
        os = new FileOutputStream(of);
        byte[] buffer = new byte[4096];
        int i = 0;
        while ((i = in.read(buffer)) > 0) {
            os.write(buffer, 0, i);
            log.debug2("Wrote " + i + " bytes of WARC");
        }
    } catch (IOException ex) {
        log.error("copy threw " + ex);
        return;
    } finally {
        IOUtil.safeClose(os);
        IOUtil.safeClose(in);
    }
    generateIndex(exportDir);
    sau.deleteContentTree();
    sau.moveToContentTree(exportDir);
    crawlContent();
    // Version of http://www.example.com/index.html should be 3
    checkExplodedUrls(url, 3);
    checkExplodedUrls(addurl, 3);
  }

  public void testAdditionalUrlsOnly() throws Exception {
    File exportDir = getTempDir();
    sau.generateContentTree();
    crawlContent();
    //doExport(exportDir, true, false, 5000);
    //doExport(exportDir, true, false, -1);

    // Add canned warc file containing additional urls
    String warcName = "warc-20110302184004-00000.warc.gz";
    InputStream in = null;
    OutputStream os = null;
    try {
        in = getResourceAsStream(warcName);
        File of = new File(exportDir.getAbsolutePath() + File.separator + warcName);
        os = new FileOutputStream(of);
        byte[] buffer = new byte[4096];
        int i = 0;
        while ((i = in.read(buffer)) > 0) {
            os.write(buffer, 0, i);
            log.debug2("Wrote " + i + " bytes of WARC");
        }
    } catch (IOException ex) {
        log.error("copy threw " + ex);
        return;
    } finally {
        IOUtil.safeClose(os);
        IOUtil.safeClose(in);
    }
    generateIndex(exportDir);
    sau.deleteContentTree();
    sau.moveToContentTree(exportDir);
    crawlContent();
    // Version of http://www.example.com/index.html should be 2
    checkExplodedUrls(url, 2);
    // Version of http://www.example.com/index.html does not matter since it is
    // not contained in the additional url set.
    checkExplodedUrls(addurl, 0);
  }

  private void doExport(File exportDir, boolean isCompress, boolean isResponse,
      long maxSize) throws Exception {
    WarcExporter exp = new WarcExporter(daemon, sau, isResponse);
    exp.setDir(exportDir);
    exp.setPrefix("warcpre");
    exp.setCompress(isCompress);
    if (maxSize > 0) {
      exp.setMaxSize(maxSize);
    }
    exp.export();
  }

  private void generateIndex(File dir) {
    if (dir.isDirectory()) {
      File index = new File(dir, INDEX_NAME);
      try {
        FileOutputStream fos = new FileOutputStream(index);
        PrintWriter pw = new PrintWriter(fos);
        log.debug3("Re-creating index file at " + index.getAbsolutePath());
        String file_content =
          getIndexContent(dir, INDEX_NAME, LockssPermission.LOCKSS_PERMISSION_STRING);
        pw.print(file_content);
        pw.flush();
        pw.close();
        fos.close();
      } catch (IOException ex) {
        log.error("generateIndex() threw " + ex);
      }
    } else {
      log.error("Directory " + dir + " missing");
    }
  }

  private String getIndexContent(File directory,
      String filename,
      String permission) {
    if ((directory==null) || (!directory.exists()) ||
        (!directory.isDirectory())) {
      return "";
    }
    String fullName = directory.getName() + File.separator + filename;
    String file_content =
      "<HTML><HEAD><TITLE>" + fullName + "</TITLE></HEAD><BODY>";
    file_content += "<B>"+fullName+"</B>";

    if(permission != null) {
      file_content += "<BR>" + permission;
    }
    File[] children = directory.listFiles();

    Arrays.sort(children);    // must sort to ensure index page always same

    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      String subLink = child.getName();
      if (child.isDirectory()) {
        subLink += File.separator + SimulatedContentGenerator.INDEX_NAME;
      }
      file_content += "<BR><A HREF=\"" + FileUtil.sysIndepPath(subLink) +
        "\">" + subLink + "</A>";
    }
    file_content += "</BODY></HTML>";
    return file_content;
  }

  protected void crawlContent() {
    Collection<String> urls = sau.getStartUrls();
    sau.setStartUrls(urls);
    sau.setRule(new MyCrawlRule());
    sau.setExploderPattern(".warc.gz$");
    sau.setExploderHelper(new MyExploderHelper(null));
    
    AuState maus = new MyMockAuState();
    Crawler crawler = new NoCrawlEndActionsFollowLinkCrawler(sau, maus);
    boolean res = crawler.doCrawl();
    lastCrawlResult = maus.getLastCrawlResult();
    lastCrawlMessage = maus.getLastCrawlResultMsg();
    log.debug2("End crawl " + res + " " + lastCrawlResult + " " +
        (lastCrawlMessage != null ? lastCrawlMessage : "null"));
  }

  private void checkExplodedUrls(String[] urls, int indexVersion) {
    log.debug2("Checking Exploded URLs.");
    for (int i = 0; i < urls.length; i++) {
      CachedUrl cu = daemon.getPluginManager().findCachedUrl(urls[i]);
      assertTrue(urls[i] + " not in any AU", cu != null);
      log.debug2("Check: " + urls[i] + " cu " + cu + " au " + cu.getArchivalUnit().getAuId());
      assertTrue(cu + " has no content", cu.hasContent());
      assertTrue(cu + " isn't SimulatedArchivalUnit",
          (cu.getArchivalUnit() instanceof SimulatedArchivalUnit));
      assertEquals(sau, cu.getArchivalUnit());
      int version = cu.getVersion();
      if ("http://www.example.com/index.html" == urls[i]) {
          assertEquals(indexVersion, version);
      } else {
          assertEquals(version, 1);
      }
    }
    log.debug2("Checking Exploded URLs done.");
  }

  public static class MyCrawlRule implements CrawlRule {
    public int match(String url) {
      if (url.startsWith("http://www.example.com")) {
        return CrawlRule.INCLUDE;
      }
      return CrawlRule.EXCLUDE;
    }
  }

  public static class MyMockAuState extends MockAuState {

    public MyMockAuState() {
      super();
    }

    public void newCrawlFinished(int result, String msg) {
      log.debug("Crawl finished " + result + " " + msg);
    }
  }

  public static class MyExploderHelper implements ExploderHelper {
    private static String badName;
    public MyExploderHelper(String bad) {
      badName = bad;
    }

    private static final String suffix[] = {
      ".txt",
      ".html",
      ".pdf",
      ".jpg",
      ".bin",
    };
    public static final String[] mimeType = {
      "text/plain",
      "text/html",
      "application/pdf",
      "image/jpg",
      "application/octet-stream",
    };

    public void process(ArchiveEntry ae) {
      String baseUrl = null;
      String restOfUrl = ae.getName();
      log.debug3("process(" + restOfUrl + ") " + badName);
      if (restOfUrl == null || restOfUrl.equals(badName)) {
        log.debug("Synthetic failure at " + badName);
        return;
      }
      URL url;
      try {
        url = new URL(restOfUrl);
        if (!"http".equals(url.getProtocol())) {
          log.debug2("ignoring: " + url.toString());
        }
      } catch (MalformedURLException ex) {
        log.debug2("Bad URL: " + (restOfUrl == null ? "null" : restOfUrl));
        return;
      }
      // XXX For now, put the content in an AU per host
      baseUrl = "http://" + url.getHost() + "/";
      restOfUrl = url.getFile();
      log.debug(ae.getName() + " mapped to " +
          baseUrl + " plus " + restOfUrl);
      ae.setBaseUrl(baseUrl);
      ae.setRestOfUrl(restOfUrl);
      // XXX may be necessary to synthesize some header fields
      CIProperties props = new CIProperties();
      props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
      ae.setAuProps(props);
    }

    @Override
    public void setWatchdog(LockssWatchdog wdog) {
      //Do nothing
      
    }

    @Override
    public void pokeWDog() {
      //Do nothing
      
    }
  }
  
  public static class MyPluginManager extends PluginManager {
    MyPluginManager() {
      super();
    }
    protected String getConfigurablePluginName(String pluginName) {
      pluginName = MockExplodedPlugin.class.getName();
      log.debug("getConfigurablePluginName returns " + pluginName);
      return pluginName;
    }
  }

}
