/*
 * $Id: PluginManager.java,v 1.7 2003-02-24 23:32:43 tal Exp $
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

package org.lockss.plugin;

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.app.*;

/**
 * Plugin global functionality
 *
 * @author  TAL
 * @version 0.0
 */
public class PluginManager implements LockssManager {
  static final String PARAM_AU_TREE = Configuration.PREFIX + "au";
  private static String PARAM_PLUGIN_LOCATION =
    Configuration.PREFIX + "pluginDir";
  private static String DEFAULT_PLUGIN_LOCATION = "./plugins";

  private static Logger log = Logger.getLogger("PluginMgr");

  private static PluginManager theManager = null;
  private static LockssDaemon theDaemon = null;
  private String pluginDir = null;

  private static Map plugins = new HashMap();
  private static Vector archivalUnits = new Vector();


  /* ------- LockssManager implementation ------------------ */
  /**
   * init the plugin manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see LockssManager#initService(LockssDaemon)
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if (theManager == null) {
      theDaemon = daemon;
      theManager = new PluginManager();
    } else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration oldConfig,
					 Configuration newConfig,
					 Set changedKeys) {
	  setConfig(newConfig, oldConfig);
	}
      });
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // tk - checkpoint if nec.
    for (Iterator iter = plugins.values().iterator(); iter.hasNext(); ) {
      Plugin plugin = (Plugin)iter.next();
      plugin.stopPlugin();
    }

    theManager = null;
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
    pluginDir = config.get(PARAM_PLUGIN_LOCATION, DEFAULT_PLUGIN_LOCATION);
    Configuration allPlugs = config.getConfigTree(PARAM_AU_TREE);
    Configuration oldAllPlugs = oldConfig.getConfigTree(PARAM_AU_TREE);
    for (Iterator iter = allPlugs.nodeIterator(); iter.hasNext(); ) {
      String pluginId = (String)iter.next();
      log.debug("Configuring plugin id: " + pluginId);
      Configuration pluginConf = allPlugs.getConfigTree(pluginId);
      Configuration oldPluginConf = oldAllPlugs.getConfigTree(pluginId);

      // tk - only if in changedKeys.  (but, compare subkey with whole key?)
      configurePlugin(pluginId, pluginConf, oldPluginConf);
    }
  }

  String pluginNameFromId(String key) {
    // tk - needs to do real mapping from IDs obtained from all available
    // plugins.
    // for now, treat as class name with |'s instead of .'s
    return StringUtil.replaceString(key, "|", ".");
  }

  /**
   * Return the plugin with the given id.  Mostly for testing.
   * @return the plugin or null
   */
  Plugin getPlugin(String pluginId) {
    return (Plugin)plugins.get(pluginId);
  }

  private void configurePlugin(String pluginId, Configuration pluginConf,
			       Configuration oldPluginConf) {
    boolean pluginOk = ensurePluginLoaded(pluginId);
    Plugin plugin = getPlugin(pluginId);
    for (Iterator iter = pluginConf.nodeIterator(); iter.hasNext(); ) {
      String auKey = (String)iter.next();
      if (pluginOk) {
	log.debug("Configuring AU id: " + auKey);
	try {
	  Configuration auConf = pluginConf.getConfigTree(auKey);
	  Configuration oldAuConf = oldPluginConf.getConfigTree(auKey);
	  if (!auConf.equals(oldAuConf)) {
	    plugin.configureAU(auConf);
	  }
	} catch (ArchivalUnit.ConfigurationException e) {
	  log.error("Failed to configure AU " + auKey, e);
	}
      } else {
	log.warning("Not configuring AU " + auKey);
      }
    }    
  }

  /**
   * load a plugin with the given class name from somewhere in our classpath
   * @param pluginName the unique name for this plugin
   */
  boolean ensurePluginLoaded(String pluginId) {
    if (plugins.containsKey(pluginId)) {
      return true;
    }
    String pluginName = pluginNameFromId(pluginId);
    Class pluginClass;
    try {
      pluginClass = Class.forName(pluginName);
    } catch (ClassNotFoundException e) {
      log.debug(pluginName + " not on classpath");
      // not on classpath
      try {
	// tk - search plugin dir for signed jar, try loading again
	throw e;	       // for now, simulate failure of that process
      } catch (ClassNotFoundException e1) {
	// plugin is really not available
	log.error(pluginName + " not found");
	return false;
      }
    } catch (Exception e) {
      // any other exception while loading class if not recoverable
      log.error("Error loading " + pluginName, e);
      return false;
    }
    try {
      Plugin plugin = (Plugin)pluginClass.newInstance();
      plugin.initPlugin();
      plugins.put(pluginId, plugin);
      return true;
    } catch (Exception e) {
      log.error("Error instantiating " + pluginName, e);
      return false;
    }
  }

  /**
   * Register the <code>ArchivalUnit</code>, so that
   * it can be found by <code>Plugin.findArchivalUnit()</code>.
   * @param au <code>ArchivalUnit</code> to add.
   */
  public void registerArchivalUnit(ArchivalUnit au) {
    if (!archivalUnits.contains(au)) {
      archivalUnits.addElement(au);
    }
  }

  /**
   * Unregister the <code>ArchivalUnit</code>, so that
   * it will not be found by <code>Plugin.findArchivalUnit()</code>.
   * @param au <code>ArchivalUnit</code> to remove.
   */
  public void unregisterArchivalUnit(ArchivalUnit au) {
    archivalUnits.remove(au);
  }

  /**
   * Find the <code>ArchivalUnit</code>
   * that contains a URL.
   * @param url The URL to search for.
   * @return The <code>ArchivalUnit</code> that contains the URL, or
   * null if none found.  It is an error for more than one
   * <code>ArchivalUnit</code> to contain the url.
   */
  public ArchivalUnit findArchivalUnit(String url) {
    for (Iterator iter = archivalUnits.iterator();
	 iter.hasNext();) {
      Object o = iter.next();
      if (o instanceof ArchivalUnit) {
	ArchivalUnit au = (ArchivalUnit)o;
	if (au.shouldBeCached(url)) {
	  return au;
	}
      }
    }
    return null;
  }

  /**
   * Find the <code>CachedUrlSet</code>
   * that comtains a URL.
   * @param url The URL to search for.
   * @return The <code>CachedUrlSet</code> that contains the URL, or
   * null if none found.  It is an error for more than one
   * <code>ArchivalUnit</code> to contain the url.
   */
  public CachedUrlSet findAUCachedUrlSet(String url) {
    ArchivalUnit au = findArchivalUnit(url);
    if (au == null) {
      return null;
    }
    return au.getAUCachedUrlSet();
  }

  /**
   * Get the list of ArchivalUnits.
   * @return an Iterator of ArchivalUnits
   */
  public static Iterator getArchivalUnits() {
    return Collections.unmodifiableList(archivalUnits).iterator();
  }

  /**
   * Returns the number of archival units currently registered.
   * @return an integer
   */
  public static int getNumArchivalUnits() {
    return archivalUnits.size();
  }

//    /**
//     * Find or create a <code>CachedUrlSet</code> representing the content
//     * specified by the URL and pattern.
//     * @param url
//     * @param regex
//     */
//    public static CachedUrlSet findCachedUrlSet(String url, String regex) {
//      ArchivalUnit au = findArchivalUnit(url);
//      if (au == null) {
//        return null;
//      }
//      return au.makeCachedUrlSet(url, regex);
//    }
//   /**
//    * init the plugins in the plugins directory
//    */

//   protected void initPlugins() {
//     /* grab our 3rd party plugins and load them using security manager */
//     String[] files = new java.io.File(pluginDir).list();
//     for(int i= 0; i < files.length; i++) {
//       if(files[i].endsWith(".jar")) {
//         loadPlugin(files[i].substring(0,files[i].lastIndexOf(".jar")));
//       }
//       else {
//         loadPlugin(files[i]);
//       }
//     }
//   }

}
