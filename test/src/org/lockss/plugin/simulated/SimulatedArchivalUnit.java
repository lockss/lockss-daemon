/*
 * $Id: SimulatedArchivalUnit.java,v 1.76 2012-10-25 18:16:55 aishizaki Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.simulated;

import java.net.*;
import java.util.*;
import java.io.File;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.extractor.*;
import org.lockss.state.*;

/**
 * This is ArchivalUnit of the simulated plugin, used for testing purposes.
 * It repeatably generates local content (via a file hierarchy),
 * with specific parameters obtained via Configuration.
 *
 * It emulates the fake URL 'www.example.com'.
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class SimulatedArchivalUnit extends BaseArchivalUnit {
  static final Logger log = Logger.getLogger("SAU");

  private static final String SIMULATED_URL_BASE = "http://www.example.com/";

  /**
   * This is the url which the Crawler should start at.
   */
  private static final String SIMULATED_URL_START =
    SIMULATED_URL_BASE + "index.html";

  /**
   * This is the root of the url which the SimAU pretends to be.
   * It is replaced with the actual directory root.
   */
  private static final String SIMULATED_URL_ROOT = "http://www.example.com";

  private String fileRoot; //root directory for the generated content
  private SimulatedContentGenerator scgen;
  private String auId = StringUtil.gensym("SimAU_");
  String simRoot; //sim root dir returned by content generator
  String baseUrlNoSlash;
  private boolean doFilter = false;

  Set toBeDamaged = new HashSet();

  private ArticleIteratorFactory articleIteratorFactory = null;

  public SimulatedArchivalUnit(Plugin owner) {
    super(owner);
    log.debug2("Plugin is " + owner);
  }

  /** Convenience methods, as most creators don't care about the plugin */
  public SimulatedArchivalUnit() {
    this(new SimulatedPlugin());
    log.debug2("Null plugin");
  }

  public UrlCacher makeUrlCacher(String url) {
    String fileRoot = getRootDir();
    return new SimulatedUrlCacher(this, url, fileRoot);
  }


  public String getName() {
    return makeName();
  }

  protected String makeName() {
    return "Simulated Content: " + fileRoot;
  }

  protected String makeStartUrl() {
    return paramMap.getUrl(KEY_AU_BASE_URL) + "index.html";
  }

  public ArticleIteratorFactory getArticleIteratorFactory() {
    log.debug3("getArticleIteratorFactory: " + articleIteratorFactory);
    return articleIteratorFactory;
  }

  // public methods

  public boolean shouldCallTopLevelPoll(AuState aus) {
    return true;
  }

  public void setArticleIteratorFactory(ArticleIteratorFactory aif) {
    articleIteratorFactory = aif;
  }

  /**
   * Set the directory where simulated content is generated
   * @param rootDir the new root dir
   */
  public void setRootDir(String rootDir) {
    fileRoot = rootDir;
  }

  /**
   * Returns the directory where simulated content is generated
   * @return the root dir
   */
  public String getRootDir() {
    return fileRoot;
  }

  /**
   * Returns the {@link SimulatedContentGenerator} for setting
   * parameters.
   * @return the generator
   */
  public SimulatedContentGenerator getContentGenerator() {
    return getContentGenerator(fileRoot);
  }
  /**
   * Returns the {@link SimulatedContentGenerator} for setting
   * parameters.
   * @param root the root of the file hierarchy
   * @return the generator
   */
  public SimulatedContentGenerator getContentGenerator(String root) {
    Configuration cf = getConfiguration();
    return getContentGenerator(cf, root);
  }
  /**
   * Returns the {@link SimulatedContentGenerator} for setting
   * parameters.
   * @param root the root of the file hierarchy
   * @return the generator
   */
  public SimulatedContentGenerator getContentGenerator(Configuration cf,
                                                       String root) {
    SimulatedContentGenerator ret = scgen;
    if (ret == null) {
      if (root == null) {
	root = fileRoot;
      }
      Plugin pl = getPlugin();
      if (pl instanceof SimulatedPlugin) {
        ret = ((SimulatedPlugin)pl).getContentGenerator(cf, root);
      } else {
	ret = SimulatedContentGenerator.getInstance(root);
      }
      scgen = ret;
    }
    return ret;
  }

  /**
   * generateContentTree() generates the simulated content.
   */
  public void generateContentTree() {
    if (scgen == null) {
      scgen = getContentGenerator();
    }
    if (scgen == null) {
      log.error("scgen null in generateContentTree");
      return;
    }
    if (!scgen.isContentTree()) {
      simRoot = scgen.generateContentTree();
    }
  }

  /** Return true if the simulated content has been generated */
  public boolean hasContentTree() {
    return scgen != null && scgen.isContentTree();
  }

  /**
   * resetContentTree() deletes and regenerates the simulated content,
   * restoring it to its starting state.
   */
  public void resetContentTree() {
    // clears and restores content tree to starting state
    if (scgen == null) {
      scgen = getContentGenerator();
    }
    if (scgen.isContentTree()) {
      scgen.deleteContentTree();
    }
    simRoot = scgen.generateContentTree();
  }

  public void alterContentTree() {
    //XXX alters in a repeatable manner
  }

  /** @return the top of the simulated content tree */
  public String getSimRoot() {
    return simRoot;
  }

  /**
   * deleteContentTree() deletes the simulated content.
   */
  public void deleteContentTree() {
    if (scgen == null) {
      scgen = getContentGenerator();
    }
    scgen.deleteContentTree();
  }

  public void moveToContentTree(File source) {
    File target = new File(simRoot);
    log.debug("Moving " + source.getAbsolutePath() + " to " +
            target.getAbsolutePath());
    source.renameTo(target);
  }

  /**
   * mapUrlToContentFileName()
   * This maps a given url to a content file location.
   *
   * @param url the url to map
   * @return fileName the mapping result
   */
  public String mapUrlToContentFileName(String url) {
    String baseStr =  StringUtil.replaceString(url, baseUrlNoSlash,
					       SimulatedContentGenerator.ROOT_NAME);
    return FileUtil.sysDepPath(baseStr);
  }

  /**
   * Map a content file location to its url.
   * @param filename the filename to map
   * @return fileName the mapping result
   */
  public String mapContentFileNameToUrl(String filename) {
    String baseStr = StringUtil.replaceString(filename, simRoot,
					      baseUrlNoSlash);
    return FileUtil.sysIndepPath(baseStr);
  }

  /**
   * @param url the url to parse
   * @return the number of links between the top index page and the url.
   * This knows about the structure of the simulated content */
  public int getLinkDepth(String url) {
    String relname = StringUtil.replaceString(url, baseUrlNoSlash, "");
    String absname = StringUtil.replaceString(url, baseUrlNoSlash,
					      simRoot);
    int dirDepth = StringUtil.countOccurences(relname, "/") - 1;
    File absfile = new File(absname);
    File relfile = new File(relname);
    String name = (absfile.isDirectory()
		   ? SimulatedContentGenerator.INDEX_NAME
		   : relfile.getName());
    if (SimulatedContentGenerator.INDEX_NAME.equals(name)) {
      return dirDepth;
    } else {
      return dirDepth + 1;
    }
  }

  public Collection getUrlStems() {
    try {
      String stem = UrlUtil.getUrlPrefix(paramMap.getUrl(KEY_AU_BASE_URL));
      return ListUtil.list(stem);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public String getUrlRoot() {
    return baseUrlNoSlash;
  }

  protected void loadAuConfigDescrs(Configuration config) throws
  ConfigurationException {
    super.loadAuConfigDescrs(config);
    try {
      fileRoot = config.get(SimulatedPlugin.AU_PARAM_ROOT);
      if (fileRoot == null) {
        throw new
          ArchivalUnit.ConfigurationException("Missing configuration value for: "+
                                              SimulatedPlugin.AU_PARAM_ROOT);
      }
      SimulatedContentGenerator gen = getContentGenerator(config, fileRoot);
      if (config.containsKey(SimulatedPlugin.AU_PARAM_DEPTH)) {
        gen.setTreeDepth(config.getInt(SimulatedPlugin.AU_PARAM_DEPTH));
      }
      if (config.containsKey(SimulatedPlugin.AU_PARAM_BRANCH)) {
        gen.setNumBranches(config.getInt(SimulatedPlugin.AU_PARAM_BRANCH));
      }
      if (config.containsKey(SimulatedPlugin.AU_PARAM_NUM_FILES)) {
        gen.setNumFilesPerBranch(config.getInt(
					       SimulatedPlugin.AU_PARAM_NUM_FILES));
      }
      if (config.containsKey(SimulatedPlugin.AU_PARAM_BIN_FILE_SIZE)) {
        gen.setBinaryFileSize(config.getLong(SimulatedPlugin.AU_PARAM_BIN_FILE_SIZE));
      }
      if (config.containsKey(SimulatedPlugin.AU_PARAM_BIN_RANDOM_SEED)) {
        gen.setRandomSeed(config.getLong(SimulatedPlugin.AU_PARAM_BIN_RANDOM_SEED));
      }
      if (config.containsKey(SimulatedPlugin.AU_PARAM_MAXFILE_NAME)) {
        gen.setMaxFilenameLength(config.getInt(
					       SimulatedPlugin.AU_PARAM_MAXFILE_NAME));
      }
      if (config.containsKey(SimulatedPlugin.AU_PARAM_FILE_TYPES)) {
        gen.setFileTypes(config.getInt(SimulatedPlugin.AU_PARAM_FILE_TYPES));
      }
      if (config.containsKey(SimulatedPlugin.AU_PARAM_ODD_BRANCH_CONTENT)) {
        gen.setOddBranchesHaveContent(config.getBoolean(
							SimulatedPlugin.AU_PARAM_ODD_BRANCH_CONTENT));
      }
      if (config.containsKey(SimulatedPlugin.AU_PARAM_BAD_FILE_LOC) &&
          config.containsKey(SimulatedPlugin.AU_PARAM_BAD_FILE_NUM)) {
        gen.setAbnormalFile(config.get(SimulatedPlugin.AU_PARAM_BAD_FILE_LOC),
                            config.getInt(SimulatedPlugin.AU_PARAM_BAD_FILE_NUM));
      }
      if (config.containsKey(SimulatedPlugin.AU_PARAM_BAD_CACHED_FILE_LOC) &&
          config.containsKey(SimulatedPlugin.AU_PARAM_BAD_CACHED_FILE_NUM)) {
        toBeDamaged.add(gen.getUrlFromLoc(config.get(
						     SimulatedPlugin.AU_PARAM_BAD_CACHED_FILE_LOC),
					  config.get(
						     SimulatedPlugin.AU_PARAM_BAD_CACHED_FILE_NUM)));
      }
      if (config.containsKey(SimulatedPlugin.AU_PARAM_MIXED_CASE)) {
        gen.setMixedCase(config.getBoolean(SimulatedPlugin.AU_PARAM_MIXED_CASE));
      }

      String spec = config.get(SimulatedPlugin.AU_PARAM_HASH_FILTER_SPEC);
      doFilter = !StringUtil.isNullString(spec);
      // if no previous generator, any content-determining parameters have
      // changed from last time, generate new content
      if (scgen == null ||
	  !(gen.getContentRoot().equals(scgen.getContentRoot()) &&
	    gen.getTreeDepth() == scgen.getTreeDepth() &&
	    gen.getNumBranches() == scgen.getNumBranches() &&
	    gen.getNumFilesPerBranch() == scgen.getNumFilesPerBranch() &&
	    gen.getBinaryFileSize() == scgen.getBinaryFileSize() &&
	    gen.getMaxFilenameLength() == scgen.getMaxFilenameLength() &&
	    gen.getFileTypes() == scgen.getFileTypes() &&
	    gen.oddBranchesHaveContent() == scgen.oddBranchesHaveContent() &&
	    gen.getMixedCase() == scgen.getMixedCase() &&
	    gen.getAbnormalBranchString().equals(scgen.getAbnormalBranchString()) &&
	    gen.getAbnormalFileNumber() == scgen.getAbnormalFileNumber())) {
	scgen = gen;
	resetContentTree();
      } else if (scgen != null && !scgen.isContentTree()) {
	simRoot = scgen.generateContentTree();
      }
    } catch (Configuration.InvalidParam e) {
      throw new ArchivalUnit.ConfigurationException("Bad config value", e);
    }
  }

  public void setConfiguration(Configuration config)
      throws ArchivalUnit.ConfigurationException {
    if (!config.containsKey(ConfigParamDescr.BASE_URL.getKey())) {
      if (config.isSealed()) {
	config = config.copy();
      }
      config.put(ConfigParamDescr.BASE_URL.getKey(), SIMULATED_URL_BASE);
    }
    super.setConfiguration(config);
  }

  protected void setBaseAuParams(Configuration config)
      throws ConfigurationException {
    super.setBaseAuParams(config);
    String baseurl = paramMap.getUrl(KEY_AU_BASE_URL).toString();
    baseUrlNoSlash = StringUtil.upToFinal(baseurl, "/");

    newContentCrawlIntv = config.getTimeInterval(KEY_NEW_CONTENT_CRAWL_INTERVAL,
                                                 defaultContentCrawlIntv);
    paramMap.putLong(KEY_AU_NEW_CONTENT_CRAWL_INTERVAL, newContentCrawlIntv);
    auName = makeName();
    paramMap.putString(KEY_AU_TITLE, auName);

    titleDbChanged();
  }

  protected CrawlRule makeRules() throws LockssRegexpException {
    CrawlRule rule1 =
      new CrawlRules.RE("xxxexclude", CrawlRules.RE.MATCH_EXCLUDE);
    CrawlRule rule2 =
      new CrawlRules.RE(paramMap.getUrl(KEY_AU_BASE_URL) + ".*",
			CrawlRules.RE.MATCH_INCLUDE);
    return new CrawlRules.FirstMatch(ListUtil.list(rule1, rule2));
  }

  /** No longer effective */
  public RateLimiter findFetchRateLimiter() {
    return RateLimiter.UNLIMITED;
  }

  @Override
  public RateLimiterInfo getRateLimiterInfo() {
    return new RateLimiterInfo(getFetchRateLimiterKey(), "unlimited");
  }

  public FilterRule getFilterRule(String contentType) {
    if (doFilter) {
      return new SimulatedFilterRule();
    }
    return null;
  }

  boolean isUrlToBeDamaged(String url) {
    String file = StringUtil.replaceString(url, baseUrlNoSlash, "");
    if (toBeDamaged.contains(file)) {
      boolean x = toBeDamaged.remove(file);
      return true;
    }
    else {
      return false;
    }
  }
}
