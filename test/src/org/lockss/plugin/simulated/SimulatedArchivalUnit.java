/*
 * $Id: SimulatedArchivalUnit.java,v 1.39 2003-11-11 23:30:50 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.File;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import java.net.*;

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
  public static final String SIMULATED_URL_STEM = "http://www.example.com";

  /**
   * This is the url which the Crawler should start at.
   */
  public static final String SIMULATED_URL_START =
    SIMULATED_URL_STEM + "/index.html";

  /**
   * This is the root of the url which the SimAU pretends to be.
   * It is replaced with the actual directory root.
   */
  public static final String SIMULATED_URL_ROOT = "http://www.example.com";

  private String fileRoot; //root directory for the generated content
  private SimulatedContentGenerator scgen;
  private String auId = StringUtil.gensym("SimAU_");

  Set toBeDamaged = new HashSet();

  public SimulatedArchivalUnit(Plugin owner) {
    super(owner);
  }

  /** Convenience methods, as most creators don't care about the plugin */
  public SimulatedArchivalUnit() {
    this(new SimulatedPlugin());
  }


  public String getName() {
    return makeName();
  }

  protected String makeName() {
    return "Simulated Content: " + fileRoot;
  }

  protected String makeStartUrl() {
    return SIMULATED_URL_START;
  }

  public String getManifestPage() {
    return SIMULATED_URL_START;
  }


  // public methods

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
    if (scgen == null) {
      scgen = new SimulatedContentGenerator(fileRoot);
    }
    return scgen;
  }

  /**
   * generateContentTree() generates the simulated content.
   */
  public void generateContentTree() {
    if (!getContentGenerator().isContentTree()) {
      getContentGenerator().generateContentTree();
    }
  }

  /**
   * resetContentTree() deletes and regenerates the simulated content,
   * restoring it to its starting state.
   */
  public void resetContentTree() {
    // clears and restores content tree to starting state
    if (getContentGenerator().isContentTree()) {
      getContentGenerator().deleteContentTree();
    }
    getContentGenerator().generateContentTree();
  }

  public void alterContentTree() {
    //XXX alters in a repeatable manner
  }

  /**
   * deleteContentTree() deletes the simulated content.
   */
  public void deleteContentTree() {
    getContentGenerator().deleteContentTree();
  }

  public void pauseBeforeFetch() {
    // no pauses since this is a test unit
  }

  /**
   * mapUrlToContentFileName()
   * This maps a given url to a content file location.
   *
   * @param url the url to map
   * @return fileName the mapping result
   */
  public static String mapUrlToContentFileName(String url) {
    return StringUtil.replaceString(url, SIMULATED_URL_ROOT,
                                    SimulatedContentGenerator.ROOT_NAME);
  }

  public List getNewContentCrawlUrls() {
    return ListUtil.list(SIMULATED_URL_START);
  }

  public Collection getUrlStems() {
    return ListUtil.list(SIMULATED_URL_STEM);
  }

  protected CrawlRule makeRules() {
    throw new UnsupportedOperationException("Not implemented");
  }

  protected void setAuParams(Configuration config) throws
      ConfigurationException {
    try {
      fileRoot = config.get(SimulatedPlugin.AU_PARAM_ROOT);
      if (fileRoot == null) {
        throw new
          ArchivalUnit.ConfigurationException("Missing configuration value for: "+
                                              SimulatedPlugin.AU_PARAM_ROOT);
      }
      SimulatedContentGenerator gen = getContentGenerator();
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
        gen.setBinaryFileSize(config.getInt(
                   SimulatedPlugin.AU_PARAM_BIN_FILE_SIZE));
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
        toBeDamaged.add(scgen.getUrlFromLoc(config.get(
          SimulatedPlugin.AU_PARAM_BAD_CACHED_FILE_LOC),
          config.get(
          SimulatedPlugin.AU_PARAM_BAD_CACHED_FILE_NUM)));
      }
      resetContentTree();
    } catch (Configuration.InvalidParam e) {
      throw new ArchivalUnit.ConfigurationException("Bad config value", e);
    }
  }

  protected void setBaseAuParams(Configuration config)
      throws ConfigurationException {
    try {
      baseUrl = new URL(SIMULATED_URL_START);
    }
    catch (MalformedURLException murle) {
      throw new ConfigurationException("Bad URL for " + SIMULATED_URL_START, murle);
    }
    fetchDelay = 0;
    newContentCrawlIntv = config.getTimeInterval(NEW_CONTENT_CRAWL_KEY,
                                                 defaultContentCrawlIntv);
    crawlSpec = new CrawlSpec(SIMULATED_URL_START, null);
    startUrlString = makeStartUrl();
    auName = makeName();

  }

  boolean isUrlToBeDamaged(String url) {
    String file = StringUtil.replaceString(url,SIMULATED_URL_ROOT,"");
    if (toBeDamaged.contains(file)) {
      boolean x = toBeDamaged.remove(file);
      return true;
    }
    else {
      return false;
    }
  }
}
