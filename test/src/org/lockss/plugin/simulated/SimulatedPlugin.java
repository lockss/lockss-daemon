/*
 * $Id: SimulatedPlugin.java,v 1.16 2003-09-26 23:47:46 eaalto Exp $
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
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
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
  public static final String AU_PARAM_ROOT = "root";
  /**
   * The depth of the tree to generate (0 equals just the root dir).
   */
  public static final String AU_PARAM_DEPTH = "depth";
  /**
   * The number of branches in each directory.
   */
  public static final String AU_PARAM_BRANCH = "branch";
  /**
   * The number of files in each directory.  This will be multiplied by the
   * number of file types, so having both '.html' and '.txt' will generate
   * 'file1.html', 'file1.txt', 'file2.html', 'file2.txt', etc.
   */
  public static final String AU_PARAM_NUM_FILES = "numFiles";

  /**
   * The size to make binary files, if chosen as a type.
   */
  public static final String AU_PARAM_BIN_FILE_SIZE = "binFileSize";

  /**
   * The maximum length for file names.  Currently unused.
   */
  public static final String AU_PARAM_MAXFILE_NAME = "maxFileName";

  /**
   * The file types to create.  A bit-wise addition of
   * {@link SimulatedContentGenerator}.FILE_TYPE_XXX values.
   */
  public static final String AU_PARAM_FILE_TYPES = "fileTypes";

  /**
   * The maximum length for file names.  Currently unused.
   */
  public static final String AU_PARAM_ODD_BRANCH_CONTENT = "oddBranchContent";

  /**
   * The directory location of the 'abnormal' file.  Should be a string filepath
   * (i.e. 'root/branch1/branch3').
   */
  public static final String AU_PARAM_BAD_FILE_LOC = "badFileLoc";

  /**
   * The file number of the 'abnormal' file, in the directory given by the
   * location string.
   */
  public static final String AU_PARAM_BAD_FILE_NUM = "badFileNum";

  /**
   * The directory location of a file to be marked as 'damaged' in the cache.
   * Should be a string filepath.
   */
  public static final String AU_PARAM_BAD_CACHED_FILE_LOC = "badCachedFileLoc";

  /**
   * File number of the 'damaged' cache file
   */
  public static final String AU_PARAM_BAD_CACHED_FILE_NUM = "badCachedFileNum";

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

  /**
   * Return the list of names of the Archival Units and volranges supported by
   * this plugin
   * @return a List of Strings
   */
  public List getSupportedTitles() {
    return ListUtil.list("SimulatedSupportedTitle",
			 "odd </html>chars");
  }

  /**
   * Return the set of configuration properties required to configure
   * an archival unit for this plugin.
   * @return a List of strings which are the names of the properties for
   * which values are needed in order to configure an AU
   */
  public List getAuConfigProperties() {
    return ListUtil.list(AU_PARAM_ROOT, AU_PARAM_DEPTH,
			 AU_PARAM_BRANCH, AU_PARAM_NUM_FILES,
			 AU_PARAM_BIN_FILE_SIZE, AU_PARAM_MAXFILE_NAME,
			 AU_PARAM_FILE_TYPES, AU_PARAM_ODD_BRANCH_CONTENT,
                         AU_PARAM_BAD_FILE_LOC, AU_PARAM_BAD_FILE_NUM);
  }

  /**
   * Create an ArchivalUnit for the AU specified by the configuration.
   * @param auConfig Configuration object with values for all properties
   * returned by {@link #getAUConfigProperties()}
   * @return an {@link ArchivalUnit}
   * @throws ArchivalUnit.ConfigurationException if the configuration is
   * illegal in any way.
   */
  public ArchivalUnit createAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    log.debug("createAU(" + auConfig + ")");
    ArchivalUnit au = new SimulatedArchivalUnit(this);
    au.setConfiguration(auConfig);
    return au;
  }

  public Collection getDefiningConfigKeys() {
    return ListUtil.list(AU_PARAM_ROOT);
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

  public UrlCacher makeUrlCacher(CachedUrlSet owner, String url) {
    String fileRoot =
      ((SimulatedArchivalUnit)owner.getArchivalUnit()).getRootDir();
    return new SimulatedUrlCacher(owner, url, fileRoot);
  }

}
