/*
 * $Id: SimulatedPlugin.java,v 1.4 2003-03-27 00:50:23 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

  private static final String SIMPLUGIN_PREFIX = Configuration.PREFIX +
      "test.simplugin.";

  /**
   * The root location for the simulated content to be generated.
   */
  public static final String PARAM_ROOT = SIMPLUGIN_PREFIX + "root";
  /**
   * The depth of the tree to generate (0 equals just the root dir).
   */
  public static final String PARAM_DEPTH = SIMPLUGIN_PREFIX + "depth";
  /**
   * The number of branches in each directory.
   */
  public static final String PARAM_BRANCH = SIMPLUGIN_PREFIX + "branch";
  /**
   * The number of files in each directory.  This will be multiplied by the
   * number of file types, so having both '.html' and '.txt' will generate
   * 'file1.html', 'file1.txt', 'file2.html', 'file2.txt', etc.
   */
  public static final String PARAM_NUM_FILES = SIMPLUGIN_PREFIX + "numFiles";

  /**
   * The size to make binary files, if chosen as a type.
   */
  public static final String PARAM_BIN_FILE_SIZE = SIMPLUGIN_PREFIX + "binFileSize";

  /**
   * The maximum length for file names.  Currently unused.
   */
  public static final String PARAM_MAXFILE_NAME = SIMPLUGIN_PREFIX + "maxFileName";

  /**
   * The file types to create.  A bit-wise addition of
   * {@link SimulatedContentGenerator}.FILE_TYPE_XXX values.
   */
  public static final String PARAM_FILE_TYPES = SIMPLUGIN_PREFIX + "fileTypes";

  /**
   * The directory location of the 'abnormal' file.  Should be a string filepath
   * (i.e. 'root/branch1/branch3').
   */
  public static final String PARAM_BAD_FILE_LOC = SIMPLUGIN_PREFIX + "badFileLoc";

  /**
   * The file number of the 'abnormal' file, in the directory given by the
   * location string.
   */
  public static final String PARAM_BAD_FILE_NUM = SIMPLUGIN_PREFIX + "badFileNum";

  private String pluginId = "SimulatedPlugin";
  private int initCtr = 0;
  private int stopCtr = 0;
  private Configuration auConfig;

  public SimulatedPlugin(){
  }

  /**
   * Called after plugin is loaded to give the plugin time to perform any
   * needed initializations
   */
  public void initPlugin() {
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

  /**
   * Return the list of names of the Archival Units and volranges supported by
   * this plugin
   * @return a List of Strings
   */
  public List getSupportedAUNames() {
    return ListUtil.list("SimulatedSupportedTitle");
  }

  /**
   * Return the set of configuration properties required to configure
   * an archival unit for this plugin.
   * @return a List of strings which are the names of the properties for
   * which values are needed in order to configure an AU
   */
  public List getAUConfigProperties() {
    return ListUtil.list(PARAM_ROOT, PARAM_DEPTH,
			 PARAM_BRANCH, PARAM_NUM_FILES,
			 PARAM_BIN_FILE_SIZE, PARAM_MAXFILE_NAME,
			 PARAM_FILE_TYPES, PARAM_BAD_FILE_LOC,
			 PARAM_BAD_FILE_NUM);
  }

  /**
   * Return the AU Id string for the ArchivalUnit handling the AU specified
   * by the given configuration. This must be completely determined by the
   * subset of the configuration info that's necessary to identify the AU.
   * @param config the {@link Configuration}
   * @return the AUId string
   * @throws ArchivalUnit.ConfigurationException if the configuration is
   * illegal in any way.
   */
  public String getAUIdFromConfig(Configuration config)
      throws ArchivalUnit.ConfigurationException {
    // tk - this should include at least the root, possibly more
    return config.get(PARAM_ROOT);
  }

  /**
   * Create an ArchivalUnit for the AU specified by the configuration.
   * @param auConfig Configuration object with values for all properties
   * returned by {@link #getAUConfigProperties()}
   * @return an {@link ArchivalUnit}
   * @throws ArchivalUnit.ConfigurationException if the configuration is
   * illegal in any way.
   */
  public ArchivalUnit createAU(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    log.debug("createAU(" + auConfig + ")");
    ArchivalUnit au = new SimulatedArchivalUnit(this);
    au.setConfiguration(auConfig);
    return au;
  }

  // SimulatedPlugin methods, not part of Plugin interface

  public int getInitCtr() {
    return initCtr;
  }

  public int getStopCtr() {
    return stopCtr;
  }

  public void registerArchivalUnit(ArchivalUnit au) {
    auMap.put(au.getAUId(), au);
  }

  public void unregisterArchivalUnit(ArchivalUnit au) {
    auMap.remove(au.getAUId());
  }
}
