/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.HttpResultMap;
import org.lockss.test.*;

/**
 * SimulatedPlugin is a Plugin that simulates a website with
 * locally generated simulated content.
 */
public class SimulatedPlugin extends BasePlugin implements PluginTestable {
  static Logger log = Logger.getLogger("SimulatedPlugin");

  /**
   * The root location for the simulated content to be generated.
   */
  static final ConfigParamDescr PD_ROOT = new ConfigParamDescr();
  static {
    PD_ROOT.setKey("root");
    PD_ROOT.setDisplayName("Root");
    PD_ROOT.setType(ConfigParamDescr.TYPE_STRING);
    PD_ROOT.setSize(20);
  }
  public static final String AU_PARAM_ROOT = PD_ROOT.getKey();

  /**
   * Non-definitional base_url param, because lots of tests know that
   * SimulatedPlugin has only one definitional param
   */
  static final ConfigParamDescr PD_NON_DEF_BASE_URL =
    new ConfigParamDescr()
    .setKey("base_url")
    .setDisplayName("Base URL")
    .setType(ConfigParamDescr.TYPE_URL)
    .setSize(40)
    .setDefinitional(false)
    .setDescription("Usually of the form http://<journal-name>.com/");

  /**
   * The depth of the tree to generate (0 equals just the root dir).
   */
  static final ConfigParamDescr PD_DEPTH = new ConfigParamDescr();
  static {
    PD_DEPTH.setKey("depth");
    PD_DEPTH.setDisplayName("Depth");
    PD_DEPTH.setType(ConfigParamDescr.TYPE_INT);
    PD_DEPTH.setSize(8);
    PD_DEPTH.setDefinitional(false);
  }
  public static final String AU_PARAM_DEPTH = PD_DEPTH.getKey();

  /**
   * The number of branches in each directory.
   */
  static final ConfigParamDescr PD_BRANCH = new ConfigParamDescr();
  static {
    PD_BRANCH.setKey("branch");
    PD_BRANCH.setDisplayName("Branches");
    PD_BRANCH.setType(ConfigParamDescr.TYPE_INT);
    PD_BRANCH.setSize(8);
    PD_BRANCH.setDefinitional(false);
  }
  public static final String AU_PARAM_BRANCH = PD_BRANCH.getKey();

  /**
   * The number of files in each directory.  This will be multiplied by the
   * number of file types, so having both '.html' and '.txt' will generate
   * 'file1.html', 'file1.txt', 'file2.html', 'file2.txt', etc.
   */
  static final ConfigParamDescr PD_NUM_FILES = new ConfigParamDescr();
  static {
    PD_NUM_FILES.setKey("numFiles");
    PD_NUM_FILES.setDisplayName("Files/branch");
    PD_NUM_FILES.setType(ConfigParamDescr.TYPE_INT);
    PD_NUM_FILES.setSize(8);
    PD_NUM_FILES.setDefinitional(false);
  }
  public static final String AU_PARAM_NUM_FILES = PD_NUM_FILES.getKey();

  /**
   * The size to make binary files, if chosen as a type.
   */
  static final ConfigParamDescr PD_BIN_FILE_SIZE = new ConfigParamDescr();
  static {
    PD_BIN_FILE_SIZE.setKey("binFileSize");
    PD_BIN_FILE_SIZE.setDisplayName("Binary file size");
    PD_BIN_FILE_SIZE.setType(ConfigParamDescr.TYPE_LONG);
    PD_BIN_FILE_SIZE.setSize(8);
    PD_BIN_FILE_SIZE.setDefinitional(false);
  }
  public static final String AU_PARAM_BIN_FILE_SIZE =
    PD_BIN_FILE_SIZE.getKey();

  /**
   * The seed for random binary files, if chosen as a type.
   */
  static final ConfigParamDescr PD_BIN_RANDOM_SEED = new ConfigParamDescr();
  static {
    PD_BIN_RANDOM_SEED.setKey("binRandomSeed");
    PD_BIN_RANDOM_SEED.setDisplayName("Bin file random seed");
    PD_BIN_RANDOM_SEED.setType(ConfigParamDescr.TYPE_LONG);
    PD_BIN_RANDOM_SEED.setSize(8);
    PD_BIN_RANDOM_SEED.setDefinitional(false);
  }
  public static final String AU_PARAM_BIN_RANDOM_SEED =
    PD_BIN_RANDOM_SEED.getKey();

  /**
   * The maximum length for file names.  Currently unused.
   */
  static final ConfigParamDescr PD_MAXFILE_NAME = new ConfigParamDescr();
  static {
    PD_MAXFILE_NAME.setKey("maxFileName");
    PD_MAXFILE_NAME.setDisplayName("Max file name");
    PD_MAXFILE_NAME.setType(ConfigParamDescr.TYPE_INT);
    PD_MAXFILE_NAME.setSize(8);
    PD_MAXFILE_NAME.setDefinitional(false);
  }
  public static final String AU_PARAM_MAXFILE_NAME = PD_MAXFILE_NAME.getKey();

  /**
   * The file types to create.  A bit-wise addition of
   * {@link SimulatedContentGenerator}.FILE_TYPE_XXX values.
   */
  static final ConfigParamDescr PD_FILE_TYPES = new ConfigParamDescr();
  static {
    PD_FILE_TYPES.setKey("fileTypes");
    PD_FILE_TYPES.setDisplayName("File types");
    PD_FILE_TYPES.setType(ConfigParamDescr.TYPE_INT);
    PD_FILE_TYPES.setSize(8);
    PD_FILE_TYPES.setDefinitional(false);
  }
  public static final String AU_PARAM_FILE_TYPES = PD_FILE_TYPES.getKey();

  /**
   * ???
   */
  static final ConfigParamDescr PD_ODD_BRANCH_CONTENT = new ConfigParamDescr();
  static {
    PD_ODD_BRANCH_CONTENT.setKey("odd_branch_content");
    PD_ODD_BRANCH_CONTENT.setDisplayName("Odd Branch Contents");
    PD_ODD_BRANCH_CONTENT.setType(ConfigParamDescr.TYPE_BOOLEAN);
    PD_ODD_BRANCH_CONTENT.setDefinitional(false);
  }
  public static final String AU_PARAM_ODD_BRANCH_CONTENT =
    PD_ODD_BRANCH_CONTENT.getKey();

  /**
   * The directory location of the 'abnormal' file.  Should be a string
   * filepath (i.e. 'root/branch1/branch3').
   */
  static final ConfigParamDescr PD_BAD_FILE_LOC = new ConfigParamDescr();
  static {
    PD_BAD_FILE_LOC.setKey("badFileLoc");
    PD_BAD_FILE_LOC.setDisplayName("Bad File Path");
    PD_BAD_FILE_LOC.setType(ConfigParamDescr.TYPE_STRING);
    PD_BAD_FILE_LOC.setSize(30);
    PD_BAD_FILE_LOC.setDefinitional(false);
  }
  public static final String AU_PARAM_BAD_FILE_LOC = PD_BAD_FILE_LOC.getKey();

  /**
   * The file number of the 'abnormal' file, in the directory given by the
   * location string.
   */
  static final ConfigParamDescr PD_BAD_FILE_NUM = new ConfigParamDescr();
  static {
    PD_BAD_FILE_NUM.setKey("badFileNum");
    PD_BAD_FILE_NUM.setDisplayName("Bad File Number");
    PD_BAD_FILE_NUM.setType(ConfigParamDescr.TYPE_INT);
    PD_BAD_FILE_NUM.setSize(8);
    PD_BAD_FILE_NUM.setDefinitional(false);
  }
  public static final String AU_PARAM_BAD_FILE_NUM = PD_BAD_FILE_NUM.getKey();

  /**
   * The directory location of a file to be marked as 'damaged' in the cache.
   * Should be a string filepath.
   */
  static final ConfigParamDescr PD_BAD_CACHED_FILE_LOC =
    new ConfigParamDescr();
  static {
    PD_BAD_CACHED_FILE_LOC.setKey("badCachedFileLoc");
    PD_BAD_CACHED_FILE_LOC.setDisplayName("Damaged File Path");
    PD_BAD_CACHED_FILE_LOC.setType(ConfigParamDescr.TYPE_STRING);
    PD_BAD_CACHED_FILE_LOC.setSize(30);
    PD_BAD_CACHED_FILE_LOC.setDefinitional(false);
  }
  public static final String AU_PARAM_BAD_CACHED_FILE_LOC =
    PD_BAD_CACHED_FILE_LOC.getKey();

  /**
   * File number of the 'damaged' cache file
   */
  static final ConfigParamDescr PD_BAD_CACHED_FILE_NUM =
    new ConfigParamDescr();
  static {
    PD_BAD_CACHED_FILE_NUM.setKey("badCachedFileNum");
    PD_BAD_CACHED_FILE_NUM.setDisplayName("Damaged File Number");
    PD_BAD_CACHED_FILE_NUM.setType(ConfigParamDescr.TYPE_INT);
    PD_BAD_CACHED_FILE_NUM.setSize(8);
    PD_BAD_CACHED_FILE_NUM.setDefinitional(false);
  }
  public static final String AU_PARAM_BAD_CACHED_FILE_NUM =
    PD_BAD_CACHED_FILE_NUM.getKey();

  /**
   * Hash filter spec
   */
  static final ConfigParamDescr PD_HASH_FILTER_SPEC =
    new ConfigParamDescr();
  static {
    PD_HASH_FILTER_SPEC.setKey("hashFilterSpec");
    PD_HASH_FILTER_SPEC.setDisplayName("Hash Filters");
    PD_HASH_FILTER_SPEC.setType(ConfigParamDescr.TYPE_STRING);
    PD_HASH_FILTER_SPEC.setSize(30);
    PD_HASH_FILTER_SPEC.setDefinitional(false);
  }
  public static final String AU_PARAM_HASH_FILTER_SPEC =
    PD_HASH_FILTER_SPEC.getKey();

  /**
   * The default article mime type for the ArticleIterator
   */
  static final ConfigParamDescr PD_DEFAULT_ARTICLE_MIME_TYPE =
    new ConfigParamDescr();
  static {
    PD_DEFAULT_ARTICLE_MIME_TYPE.setKey("default_article_mime_type");
    PD_DEFAULT_ARTICLE_MIME_TYPE.setDisplayName("DefaultArticleMimeType");
    PD_DEFAULT_ARTICLE_MIME_TYPE.setType(ConfigParamDescr.TYPE_STRING);
    PD_DEFAULT_ARTICLE_MIME_TYPE.setSize(20);
  }
  public static final String AU_PARAM_DEFAULT_ARTICLE_MIME_TYPE =
    PD_DEFAULT_ARTICLE_MIME_TYPE.getKey();

  /**
   * If true, mixed case names will be generated.
   */
  static final ConfigParamDescr PD_MIXED_CASE =
    new ConfigParamDescr();
  static {
    PD_MIXED_CASE.setKey("mixed_case");
    PD_MIXED_CASE.setDisplayName("DefaultArticleMimeType");
    PD_MIXED_CASE.setType(ConfigParamDescr.TYPE_STRING);
    PD_MIXED_CASE.setSize(20);
  }
  public static final String AU_PARAM_MIXED_CASE =
    PD_MIXED_CASE.getKey();

  /**
   * The default article mime type for the ArticleIterator
   */
  static final ConfigParamDescr PD_FILTER_THROW =
    new ConfigParamDescr();
  static {
    PD_FILTER_THROW.setKey("filter_throw");
    PD_FILTER_THROW.setDisplayName("FilterThrow");
    PD_FILTER_THROW.setType(ConfigParamDescr.TYPE_STRING);
    PD_FILTER_THROW.setSize(30);
    PD_FILTER_THROW.setDefinitional(false);
  }
  public static final String AU_PARAM_FILTER_THROW =
    PD_FILTER_THROW.getKey();

  /**
   * Patterns of URLs that should be repaired from peer if vote it too close
   */
  static final ConfigParamDescr PD_REPAIR_FROM_PEER_PATS =
    new ConfigParamDescr();
  static {
    PD_REPAIR_FROM_PEER_PATS.setKey("repairFromPeerIfMissingUrlPatterns");
    PD_REPAIR_FROM_PEER_PATS.setDefinitional(false);
    PD_REPAIR_FROM_PEER_PATS.setDisplayName("Repair from peer if missing url patterns");
    PD_REPAIR_FROM_PEER_PATS.setType(ConfigParamDescr.TYPE_STRING);
    PD_REPAIR_FROM_PEER_PATS.setSize(20);
  }
  public static final String AU_PARAM_REPAIR_FROM_PEER_PATS =
    PD_REPAIR_FROM_PEER_PATS.getKey();

  private String pluginId = "SimulatedPlugin";
  private int initCtr = 0;
  private int stopCtr = 0;
  private Configuration auConfig;

  public SimulatedPlugin() {
  }

  /**
   * Called after plugin is loaded to give the plugin time to perform any
   * needed initializations
   * @param daemon the LockssDaemon
   */
  public void initPlugin(LockssDaemon daemon) {
    super.initPlugin(daemon);
    initCtr++;
  }

  /**
   * Called when the application is stopping to allow the plugin to perform
   * any necessary tasks needed to cleanly halt
   */
  public void stopPlugin() {
    stopCtr++;
  }

  public String getVersion() {
    return "SimulatedVersion";
  }

  public String getPluginName() {
    return "Simulated Content";
  }

  // SimulatedPlugin's only definitional param is the root directory,
  // typically a temp dir, so Tdb entries & TitleConfig don't make sense.
  // Suppress them, as they cause (harmless) NPEs in some unit tests.
  @Override
  protected void setTitleConfigs(Tdb tdb) {
  }

  /**
   * Return the set of configuration properties required to configure
   * an archival unit for this plugin.
   * @return a List of strings which are the names of the properties for
   * which values are needed in order to configure an AU
   */
  public List getLocalAuConfigDescrs() {
    return ListUtil.list(PD_NON_DEF_BASE_URL, PD_ROOT, PD_DEPTH,
			 PD_BRANCH, PD_NUM_FILES,
			 PD_BIN_FILE_SIZE, PD_BIN_RANDOM_SEED,
			 PD_MAXFILE_NAME,
			 PD_FILE_TYPES, PD_ODD_BRANCH_CONTENT,
                         PD_BAD_FILE_LOC, PD_BAD_FILE_NUM,
			 PD_HASH_FILTER_SPEC, PD_FILTER_THROW,
			 PD_REPAIR_FROM_PEER_PATS);
  }

  /**
   * Create an ArchivalUnit for the AU specified by the configuration.
   * @param auConfig {@link Configuration} object with values for the AU
   * config params
   * @return an {@link ArchivalUnit}
   * @throws ArchivalUnit.ConfigurationException if the configuration is
   * illegal in any way.
   */
  protected ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    log.debug("createAU(" + auConfig + ")");
    ArchivalUnit au = new SimulatedArchivalUnit(this);
    au.setConfiguration(auConfig);
    this.auConfig = auConfig;
    return au;
  }

  private String defaultArticleMimeType = null;

  public void setDefaultArticleMimeType(String val) {
    defaultArticleMimeType = val;
  }

  public String getDefaultArticleMimeType() {
    if (defaultArticleMimeType != null) {
      return defaultArticleMimeType;
    }
    if (auConfig == null) {
      return null;
    }
    String ret = auConfig.get(AU_PARAM_DEFAULT_ARTICLE_MIME_TYPE,
			      null);
// 			      "never/happens");
    log.debug("DefaultArticleMimeType is " + ret);
    return ret;
  }

  /** When fetching from simulated content, FileNotFoundException
   * exception should be treated like 404 */
  @Override
  protected void initResultMap() throws PluginException.InvalidDefinition {
    HttpResultMap hResultMap = new HttpResultMap();
    hResultMap.storeMapEntry(java.io.FileNotFoundException.class,
                            CacheException.NoRetryDeadLinkException.class);
    resultMap = hResultMap;
  }

  // SimulatedPlugin methods, not part of Plugin interface

  public int getInitCtr() {
    return initCtr;
  }

  public int getStopCtr() {
    return stopCtr;
  }

  public void registerArchivalUnit(ArchivalUnit au) {
    aus.add(au);
  }

  public void unregisterArchivalUnit(ArchivalUnit au) {
    aus.remove(au);
  }

  public SimulatedContentGenerator getContentGenerator(Configuration cf,
                                                       String fileRoot) {
    return SimulatedContentGenerator.getInstance(fileRoot);
  }

}
