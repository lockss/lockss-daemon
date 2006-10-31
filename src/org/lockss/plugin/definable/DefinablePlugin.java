/*
 * $Id: DefinablePlugin.java,v 1.21 2006-10-31 17:53:44 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.definable;

import org.lockss.plugin.base.*;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

import java.util.*;
import java.io.FileNotFoundException;
import java.net.*;

/**
 * <p>DefinablePlugin: a plugin which uses the data stored in an
*  ExternalizableMap to configure it self.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class DefinablePlugin extends BasePlugin {
  // configuration map keys
  public static final String KEY_PLUGIN_NAME = "plugin_name";
  public static final String KEY_PLUGIN_VERSION = "plugin_version";
  public static final String KEY_REQUIRED_DAEMON_VERSION =
    "required_daemon_version";
  public static final String KEY_PLUGIN_CONFIG_PROPS = "plugin_config_props";
  public static final String KEY_EXCEPTION_HANDLER =
      "plugin_cache_result_handler";
  public static final String KEY_EXCEPTION_LIST =
      "plugin_cache_result_list";
  public static final String KEY_PLUGIN_NOTES = "plugin_notes";
  public static final String KEY_CRAWL_TYPE =
      "plugin_crawl_type";
  public static final String KEY_FOLLOW_LINKS = "plugin_follow_link";
  public static final String DEFAULT_PLUGIN_VERSION = "1";
  public static final String DEFAULT_REQUIRED_DAEMON_VERSION = "0.0.0";
  public static final String MAP_SUFFIX = ".xml";

  public static final String CRAWL_TYPE_HTML_LINKS = "HTML Links";
  public static final String CRAWL_TYPE_OAI = "OAI";
  public static final String[] CRAWL_TYPES = {
      CRAWL_TYPE_HTML_LINKS,
      CRAWL_TYPE_OAI,
  };
  public static final String DEFAULT_CRAWL_TYPE = CRAWL_TYPE_HTML_LINKS;
  
  protected String mapName = null;

  static Logger log = Logger.getLogger("DefinablePlugin");

  protected ExternalizableMap definitionMap = new ExternalizableMap();
  protected CacheResultHandler resultHandler = null;
  protected ClassLoader classLoader;
  protected String loadedFrom;

  public void initPlugin(LockssDaemon daemon, String extMapName)
      throws FileNotFoundException {
    initPlugin(daemon, extMapName, this.getClass().getClassLoader());
  }

  public void initPlugin(LockssDaemon daemon, String extMapName,
			 ClassLoader loader)
      throws FileNotFoundException {
    mapName = extMapName;
    this.classLoader = loader;
    // convert the plugin class name to an xml file name
    String mapFile = mapName.replace('.', '/') + MAP_SUFFIX;
    // load the configuration map from jar file
    definitionMap.loadMapFromResource(mapFile, classLoader);
    URL url = classLoader.getResource(mapFile);
    if (url != null) {
      loadedFrom = url.toString();
    }
    // then call the overridden initializaton.
    super.initPlugin(daemon);
  }

  public String getLoadedFrom() {
    return loadedFrom;
  }

  public String getPluginName() {
    String defaultName = getDefaultPluginName();
    return definitionMap.getString(KEY_PLUGIN_NAME, defaultName);
  }

  protected String getDefaultPluginName() {
    return StringUtil.shortName(getPluginId());
  }
  
  public String getVersion() {
    return definitionMap.getString(KEY_PLUGIN_VERSION, DEFAULT_PLUGIN_VERSION);
  }

  public String getRequiredDaemonVersion() {
    return definitionMap.getString(KEY_REQUIRED_DAEMON_VERSION,
				   DEFAULT_REQUIRED_DAEMON_VERSION);
  }

  public String getPluginNotes() {
    return definitionMap.getString(KEY_PLUGIN_NOTES, null);
  }

  public List getLocalAuConfigDescrs() throws InvalidDefinitionException {
    List auConfigDescrs =
      (List) definitionMap.getCollection(KEY_PLUGIN_CONFIG_PROPS, null);
    if (auConfigDescrs == null) {
      throw new InvalidDefinitionException(mapName +
                                           " missing ConfigParamDescrs");
    }
    return auConfigDescrs;
  }

  protected ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    DefinableArchivalUnit au =
      new DefinableArchivalUnit(this, definitionMap, classLoader);
    au.setConfiguration(auConfig);
    return au;
  }

  public ExternalizableMap getDefinitionMap() {
    return definitionMap;
  }

  CacheResultHandler getCacheResultHandler() {
    return resultHandler;
  }

  protected void initResultMap() throws InvalidDefinitionException {
    resultMap = new HttpResultMap();
    // we support two form of result handlers... either a class which handles
    // installing the numbers as well as handling any exceptions
    String handler_class = null;
    handler_class = definitionMap.getString(KEY_EXCEPTION_HANDLER, null);
    if (handler_class != null) {
      try {
        resultHandler =
            (CacheResultHandler) Class.forName(handler_class).newInstance();
        resultHandler.init(resultMap);
      }
      catch (Exception ex) {
        throw new InvalidDefinitionException(mapName
        + " has invalid Exception handler: " + handler_class);
      }
      catch (LinkageError le) {
        throw new InvalidDefinitionException(
            mapName + "has  invalid Exception handler: " + handler_class , le);

      }
    }
    else {// or a list of individual exception remappings
      Collection results;
      results = definitionMap.getCollection(KEY_EXCEPTION_LIST, null);
      if (results != null) {
        // add each entry
        for (Iterator it = results.iterator(); it.hasNext(); ) {
          String entry = (String) it.next();
          try {
            Vector s_vec = StringUtil.breakAt(entry, '=', 2, true, true);
            String class_name = (String) s_vec.get(1);
            int code = Integer.parseInt(((String) s_vec.get(0)));
            // now lets add the entry into the map.
            Class result_class = null;
            result_class = Class.forName(class_name);
            ( (HttpResultMap) resultMap).storeMapEntry(code, result_class);
          }
          catch (Exception ex1) {
            throw new InvalidDefinitionException(mapName
                                                 + " has invalid entry: "
                                                 + entry);
          }

        }
      }
    }
  }


  public String getPluginId() {
    String className;
    if(mapName != null) {
      className = mapName;
    }
    else {
      //@TODO: eliminate this when we eliminate subclasses
      className = this.getClass().getName();
    }
    return className;
  }


  public static class InvalidDefinitionException extends RuntimeException {
    public InvalidDefinitionException() {
      super();
    }
    public InvalidDefinitionException(String message) {
      super(message);
    }
    public InvalidDefinitionException(String message, Throwable cause) {
      super(message, cause);
    }
    public InvalidDefinitionException(Throwable cause) {
      super(cause);
    }
  }

}
