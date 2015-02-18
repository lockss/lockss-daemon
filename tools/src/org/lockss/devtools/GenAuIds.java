/*
 * $Id$
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
n
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
package org.lockss.devtools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;


/**
 * This class prints a list of auids for the specified input configuration file.
 *
 * @author  Philip Gust
 */
public class GenAuIds {
  /*
   * Return the Au ID for the specified TdbAu based on definitional parameters.
   * 
   * @param tdbAu the TdbAu
   */
  public static String getAuIdFor(PluginManager pluginMgr, TdbAu tdbAu) {
    String pluginId = tdbAu.getPluginId();
    Plugin plugin = getPluginFor(pluginMgr, pluginId);
    if (plugin == null) {
      return tdbAu.getId().toString();
    }
    
    Properties auDefProps = new Properties();
    for (Map.Entry<String,String> param : tdbAu.getParams().entrySet()) {
      String key = param.getKey();
      String val = param.getValue();
      ConfigParamDescr descr = plugin.findAuConfigDescr(key);
      if ((descr != null) && descr.isDefinitional()) {
        auDefProps.put(key,val);
      }
    }
    String auId = PluginManager.generateAuId(pluginId, auDefProps);
    return auId;
  }

  /**
   * Return the Plugin for this instance for the specified PluginManager.
   * 
   * @param pluginId the plugin ID
   * @return the Plugin instance
   */
  public static Plugin getPluginFor(PluginManager pluginMgr, String pluginId) {
    Plugin plugin = null; 
    String pluginKey = PluginManager.pluginKeyFromName(pluginId);
    if (pluginMgr.ensurePluginLoaded(pluginKey)) {
      plugin = pluginMgr.getPlugin(pluginKey);
    }
    return plugin;
  }

  /**
   * This main method reads a configuration file and generates
   * a list of AU ids for all the TdbAus in the configuration,
   * one per line.
   * 
   * @param args one or two files, the first is the configuration
   *   file to process; if the second is given, writes its output
   *   to that file, otherwise writes it to stdout.
   */
  static public void main(String[] args) {
    if ((args.length < 1) || (args.length > 2) || (args[0].equals("-usage"))) {
      System.err.println("Usage: GenAuIds configFile [auidFile]");
      System.exit(1);
    }
    
    String configFileName = args[0];
    PrintStream ps = System.out;
    if (args.length == 2) {
      try {
        ps = new PrintStream(args[1]);
      } catch (java.io.FileNotFoundException ex) {
        System.err.println("Cannot open output file.");
        System.exit(1);
      }
    }

    ConfigManager.makeConfigManager();
    Logger.resetLogs();
    LockssDaemon daemon = new MockLockssDaemon() {};
    ConfigManager configMgr = daemon.getConfigManager();

    Configuration config = null;

    try {
      config = configMgr.loadConfigFromFile(new File(configFileName).toURI().toURL().toExternalForm());
    } catch (IOException ex) {
      System.err.println("Error loading configuration file \""
                         + configFileName + "\": " + ex.getMessage());
      System.exit(1);
    }
    Tdb tdb = config.getTdb();
    if (tdb != null) {
      PluginManager pluginMgr = daemon.getPluginManager();
      for (TdbAu.Id tdbAuId : tdb.getAllTdbAuIds()) {
        String auid = getAuIdFor(pluginMgr, tdbAuId.getTdbAu());
        ps.println(auid);
      }
    }
  }
}
