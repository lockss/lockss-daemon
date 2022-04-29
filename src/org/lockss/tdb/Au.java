/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University,
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.tdb;

import java.io.Serializable;
import java.util.*;

import org.antlr.v4.runtime.Token;

/**
 * <p>
 * A lightweight class (struct) to represent an AU during TDB processing.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class Au implements Serializable {

  /**
   * <p>
   * Make a new root AU instance.
   * </p>
   * 
   * @since 1.73 
   */
  public Au(Token tok) {
    if (tok != null) {
      this.file = tok.getTokenSource().getSourceName();
      this.line = tok.getLine();
    }
  }

  /**
   * <p>
   * Makes a new AU instance based on the given AU instance.
   * </p>
   * 
   * @param other
   *          An existing AU instance.
   * @since 1.73
   */
  public Au(Token tok, Au other) {
    this(tok);
    this.computedPlugin = other.computedPlugin;
    this.eisbn = other.eisbn;
    this.implicit = other.implicit;
    this.isbn = other.isbn;
    this.plugin = other.plugin;
    this.pluginPrefix = other.pluginPrefix;
    this.pluginSuffix = other.pluginSuffix;
    this.provider = other.provider;
    this.proxy = other.proxy;
    this.rights = other.rights;
    if (other.attrsMap != null) {
      this.attrsMap = new HashMap<String, String>(other.attrsMap);
    }
    if (other.nondefParamsMap != null) {
      this.nondefParamsMap = new HashMap<String, String>(other.nondefParamsMap);
    }
    if (other.paramsMap != null) {
      this.paramsMap = new HashMap<String, String>(other.paramsMap);
    }
    if (other.map != null) {
      this.map = new HashMap<String, String>(other.map);
    }
  }
  
  /**
   * <p>
   * Makes a new AU instance with the given parent title based on the given AU
   * instance.
   * </p>
   * 
   * @param title
   *          A parent title.
   * @param other
   *          An existing AU instance.
   * @since 1.73
   */
  public Au(Token tok, Title title, Au other) {
    this(tok, other);
    this.title = title;
  }
  
  /**
   * <p>
   * Makes a new AU instance with the given parent title (useful for tests).
   * </p>
   * 
   * @param title
   *          A parent title.
   * @since 1.73
   */
  protected Au(Token tok, Title title) {
    this(tok);
    this.title = title;
  }
  
  /**
   * <p>
   * Parent title.
   * </p>
   * 
   * @since 1.67
   */
  protected Title title;
  
  /**
   * <p>
   * Retrieves the AU's parent title.
   * </p>
   * 
   * @return The AU's parent title.
   * @since 1.67
   */
  public Title getTitle() {
    return title;
  }
  
  /**
   * <p>
   * AU context's implicit list of identifiers.
   * </p>
   * 
   * @since 1.67
   */
  protected List<String> implicit;
  
  /**
   * <p>
   * Retrieves the AU context's implicit list of identifiers.
   * </p>
   * 
   * @return The AU context's implicit list of identifiers.
   * @since 1.67
   */
  public List<String> getImplicit() {
    return implicit;
  }

  /**
   * <p>
   * Sets the AU context's implicit list of identifiers.
   * </p>
   * 
   * @param implicit
   *          A list of identifiers.
   * @since 1.67
   */
  public void setImplicit(List<String> implicit) {
    this.implicit = implicit;
  }
  
  /**
   * <p>
   * Internal storage map.
   * </p>
   * <p>
   * Definitional parameters, non-definitional parameters and attributes are
   * stored in their own maps.
   * </p>
   * 
   * @since 1.67
   * @see #put(String, String)
   * @see #getArbitraryValue(String)
   */
  protected Map<String, String> map;
  
  /**
   * <p>
   * Stores a key-value pair in this AU, either by setting a field, or placing a
   * key-value pair in a designated map (definitional parameters,
   * non-definitional parameters, attributes), or in the general storage map.
   * </p>
   * <p>
   * Since 1.70, this method returns a value (the previous value for the key
   * being assigned).
   * </p>
   * 
   * @param key
   *          A key.
   * @param value
   *          The value for the key.
   * @return The value previously associated with the key, or null if there was
   *         no previous value for this key.
   * @since 1.67
   */
  public String put(String key, String value) {
    switch (key.charAt(0)) {
      case 'a': {
        if (key.startsWith(ATTR_PREFIX)) {
          if (attrsMap == null) {
            attrsMap = new HashMap<String, String>();
          }
          return attrsMap.put(key.substring(ATTR_PREFIX.length(), key.length() - 1), value);
        }
      } break;
      case 'e': {
        if (EDITION.equals(key)) {
          String ret = edition;
          edition = value;
          return ret;
        }
        else if (EISBN.equals(key)) {
          String ret = eisbn;
          eisbn = value;
          return ret;
        }
      } break;
      case 'i': {
        if (ISBN.equals(key)) {
          String ret = isbn;
          isbn = value;
          return ret;
        }
      } break;
      case 'n': {
        if (NAME.equals(key)) {
          String ret = name;
          name = value;
          return ret;
        }
        else if (key.startsWith(NONDEFPARAM_PREFIX)) {
          if (nondefParamsMap == null) {
            nondefParamsMap = new HashMap<String, String>();
          }
          return nondefParamsMap.put(key.substring(NONDEFPARAM_PREFIX.length(), key.length() - 1), value);
        }
      } break;
      case 'p': {
        if (key.startsWith(PARAM_PREFIX)) {
          if (paramsMap == null) {
            paramsMap = new HashMap<String, String>();
          }
          return paramsMap.put(key.substring(PARAM_PREFIX.length(), key.length() - 1), value);
        }
        else if (PLUGIN.equals(key)) {
          String ret = plugin;
          plugin = value;
          return ret;
        }
        else if (PLUGIN_PREFIX.equals(key)) {
          String ret = pluginPrefix;
          pluginPrefix = value;
          return ret;
        }
        else if (PLUGIN_SUFFIX.equals(key)) {
          String ret = pluginSuffix;
          pluginSuffix = value;
          return ret;
        }
        else if (PROVIDER.equals(key)) {
          String ret = provider;
          provider = value;
          return ret;
        }
        else if (PROXY.equals(key)) {
          String ret = proxy;
          proxy = value;
          return ret;
        }
      } break;
      case 'r': {
        if (RIGHTS.equals(key)) {
          String ret = rights;
          rights = value;
          return ret;
        }
      } break;
      case 's': {
        if (STATUS.equals(key)) {
          String ret = status;
          status = value;
          return ret;
        }
        else if (STATUS1.equals(key)) {
          String ret = status1;
          status1 = value;
          return ret;
        }
        else if (STATUS2.equals(key)) {
          String ret = status2;
          status2 = value;
          return ret;
        }
      } break;
      case 'v': {
        if (VOLUME.equals(key)) {
          String ret = volume;
          volume = value;
          return ret;
        }
      } break;
      case 'y': {
        if (YEAR.equals(key)) {
          String ret = year;
          year = value;
          return ret;
        }
      } break;
    }

    // Any other key goes to the general map
    if (map == null) {
      map = new HashMap<String, String>();
    }
    return map.put(key, value);
  }
  
  /**
   * <p>
   * Retrieves a value from the AU's general storage map.
   * </p>
   * 
   * @param key
   *          A key.
   * @return The value for the key, or <code>null</code> if the key is not
   *         present in the general storage map.
   */
  public String getArbitraryValue(String key) {
    return map == null ? null : map.get(key);
  }

  /**
   * <p>
   * Attributes (key prefix).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ATTR_PREFIX = "attr[";

  /**
   * <p>
   * The AU's attributes map.
   * </p>
   * 
   * @since 1.67
   */
  protected Map<String, String> attrsMap;
  
  /**
   * <p>
   * Retrieves the AU's attributes map.
   * </p>
   * 
   * @return The AU's attributes map.
   * @since 1.67
   */
  public Map<String, String> getAttrs() {
    return attrsMap == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(attrsMap);
  }
  
  /**
   * <p>
   * AUID (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String auid = null;

  /**
   * <p>
   * Retrieves the AUID.
   * </p>
   * 
   * @return The AUID.
   * @since 1.67
   */
  public String getAuid() {
    if (auid == null) {
      String plugin = getComputedPlugin();
      Map<String, String> params = getParams();
      if (plugin != null && params != null && params.size() > 0) {
        auid = TdbUtil.generateAuId(plugin, params);
      }
    }
    return auid;
  }

  /**
   * <p>
   * AUID "plus" (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String auidplus = null;

  /**
   * <p>
   * Retrieves the AUID "plus".
   * </p>
   * 
   * @return The AUID "plus".
   * @since 1.67
   */
  public String getAuidPlus() {
    if (auidplus == null) {
      String auid = getAuid();
      if (auid != null) {
        Map<String, String> nondefParams = getNondefParams();
        if (nondefParams == null || nondefParams.size() == 0) {
          auidplus = auid;
        }
        else {
          StringBuilder sb = new StringBuilder(auid);
          boolean first = true;
          for (String nondefkey : new TreeSet<String>(nondefParams.keySet())) {
            sb.append(first ? "@@@NONDEF@@@" : "&");
            sb.append(TdbUtil.encode(nondefkey));
            sb.append("~");
            sb.append(TdbUtil.encode(nondefParams.get(nondefkey)));
            first = false;
          }
          auidplus = sb.toString();
        }
      }
    }
    return auidplus;
  }
  
  /**
   * <p>
   * The AU's computed plugin (field).
   * </p>
   * 
   * @since 1.70
   */
  protected String computedPlugin = null;
  
  /**
   * <p>
   * Retrieves the AU's computed plugin.
   * </p>
   * 
   * @return The AU's computed plugin.
   * @since 1.70
   */
  public String getComputedPlugin() {
    if (computedPlugin == null) {
      if (plugin != null) {
        computedPlugin = plugin;
      }
      else if (pluginPrefix != null && pluginSuffix != null) {
        computedPlugin = pluginPrefix + pluginSuffix;
      }
    }
    return computedPlugin;
  }
  
  /**
   * <p>
   * The AU's edition (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String EDITION = "edition";
  
  /**
   * <p>
   * The AU's edition (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String edition = null;
  
  /**
   * <p>
   * Retrieves the AU's edition.
   * </p>
   * 
   * @return The AU's edition.
   * @since 1.67
   */
  public String getEdition() {
    return edition;
  }
  
  /**
   * <p>
   * The AU's eISBN (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String EISBN = "eisbn";
  
  /**
   * <p>
   * The AU's eISBN (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String eisbn = null;
  
  /**
   * <p>
   * Retrieves the AU's eISBN.
   * </p>
   * 
   * @return The AU's eISBN.
   * @since 1.67
   */
  public String getEisbn() {
    return eisbn;
  }
  
  /**
   * <p>
   * The AU's file (key).
   * </p>
   * 
   * @since 1.73
   */
  protected static final String FILE = "file";
  
  /**
   * <p>
   * The AU's file (field).
   * </p>
   * 
   * @since 1.73
   */
  protected String file = null;
  
  /**
   * <p>
   * Retrieves the AU's file.
   * </p>
   * 
   * @return The AU's efile.
   * @since 1.73
   */
  public String getFile() {
    return file;
  }
  
  /**
   * <p>
   * The AU's ISBN (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ISBN = "isbn";

  /**
   * <p>
   * The AU's ISBN (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String isbn = null;
  
  /**
   * <p>
   * Retrieves the AU's ISBN.
   * </p>
   * 
   * @return The AU's ISBN.
   * @since 1.67
   */
  public String getIsbn() {
    return isbn;
  }
  
  /**
   * <p>
   * The AU's line (key).
   * </p>
   * 
   * @since 1.73
   */
  protected static final String LINE = "line";
  
  /**
   * <p>
   * The AU's line (field).
   * </p>
   * 
   * @since 1.73
   */
  protected int line = 0;
  
  /**
   * <p>
   * Retrieves the AU's line.
   * </p>
   * 
   * @return The AU's line.
   * @since 1.73
   */
  public int getLine() {
    return line;
  }
  
  /**
   * <p>
   * The AU's name (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String NAME = "name";
  
  /**
   * <p>
   * The AU's name (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String name = null;
  
  /**
   * <p>
   * Retrieves the AU's name.
   * </p>
   * 
   * @return The AU's name.
   * @since 1.67
   */
  public String getName() {
    return name;
  }

  /**
   * <p>
   * Non-definitional parameters (key prefix).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String NONDEFPARAM_PREFIX = "nondefparam[";

  /**
   * <p>
   * The AU's non-definitional parameters map.
   * </p>
   * 
   * @since 1.67
   */
  protected Map<String, String> nondefParamsMap;

  /**
   * <p>
   * Retrieves the AU's non-definitional parameters map.
   * </p>
   * 
   * @return The AU's non-definitional parameters map.
   * @since 1.67
   */
  public Map<String, String> getNondefParams() {
    return nondefParamsMap == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(nondefParamsMap);
  }

  /**
   * <p>
   * Definitional parameters (key prefix).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String PARAM_PREFIX = "param[";
  
  /**
   * <p>
   * The AU's definitional parameters map.
   * </p>
   * 
   * @since 1.67
   */
  protected Map<String, String> paramsMap;
  
  /**
   * <p>
   * Retrieves the AU's definitional parameters map.
   * </p>
   * 
   * @return The AU's definitional parameters map.
   * @since 1.67
   */
  public Map<String, String> getParams() {
    return paramsMap == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(paramsMap);
  }
  
  /**
   * <p>
   * The AU's plugin (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String PLUGIN = "plugin";
  
  /**
   * <p>
   * The AU's plugin (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String plugin = null;
  
  /**
   * <p>
   * Retrieves the AU's plugin.
   * </p>
   * 
   * @return The AU's plugin.
   * @since 1.67
   */
  public String getPlugin() {
    return plugin;
  }
  
  /**
   * <p>
   * The AU's plugin prefix (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String PLUGIN_PREFIX = "pluginPrefix";
  
  /**
   * <p>
   * The AU's plugin prefix (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String pluginPrefix;
  
  /**
   * <p>
   * Retrieves the AU's plugin prefix.
   * </p>
   * 
   * @return The AU's plugin prefix.
   * @since 1.67
   */
  public String getPluginPrefix() {
    return pluginPrefix;
  }

  /**
   * <p>
   * The AU's plugin suffix (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String PLUGIN_SUFFIX = "pluginSuffix";
  
  /**
   * <p>
   * The AU's plugin suffix (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String pluginSuffix;
  
  /**
   * <p>
   * Retrieves the AU's plugin suffix.
   * </p>
   * 
   * @return The AU's plugin suffix.
   * @since 1.67
   */
  public String getPluginSuffix() {
    return pluginSuffix;
  }
  
  /**
   * <p>
   * The AU's provider (key).
   * </p>
   * 
   * @since 1.67.4
   */
  protected static final String PROVIDER = "provider";
  
  /**
   * <p>
   * The AU's provider (field).
   * </p>
   * 
   * @since 1.67.4
   */
  protected String provider;
  
  /**
   * <p>
   * Retrieves the AU's provider.
   * </p>
   * 
   * @return The AU's provider.
   * @since 1.67.4
   */
  public String getProvider() {
    return provider;
  }
  
  /**
   * <p>
   * The AU's proxy (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String PROXY = "proxy";
  
  /**
   * <p>
   * The AU's proxy (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String proxy;
  
  /**
   * <p>
   * Retrieves the AU's proxy.
   * </p>
   * 
   * @return The AU's proxy.
   * @since 1.67
   */
  public String getProxy() {
    return proxy;
  }
  
  /**
   * <p>
   * The AU's rights (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String RIGHTS = "rights";
  
  /**
   * <p>
   * The AU's rights (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String rights;

  /**
   * <p>
   * Retrieves the AU's rights.
   * </p>
   * 
   * @return The AU's rights.
   * @since 1.67
   */
  public String getRights() {
    return rights;
  }
  
  /**
   * <p>
   * The AU's status (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String STATUS = "status";

  /**
   * <p>
   * The AU's status (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String status;
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_DOES_NOT_EXIST = "doesNotExist";

  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_DO_NOT_PROCESS = "doNotProcess";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_EXISTS = "exists";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_EXPECTED = "expected";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_MANIFEST = "manifest";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_WANTED = "wanted";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_TESTING = "testing";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_NOT_READY = "notReady";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_READY = "ready";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_READY_SOURCE = "readySource";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_CRAWLING = "crawling";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_DEEP_CRAWL = "deepCrawl";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_FROZEN = "frozen";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_ING_NOT_READY = "ingNotReady";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_RELEASING = "releasing";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_FINISHED = "finished";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_RELEASED = "released";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_DOWN = "down";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_SUPERSEDED = "superseded";
  
  /**
   * <p>
   * The {@value} AU status.
   * </p>
   * 
   * @since 1.67
   */
  public static final String STATUS_ZAPPED = "zapped";

  /**
   * <p>
   * An unmodifiable list of the "standard" AU statuses.
   * </p>
   * 
   * @since 1.67
   */
  public static final List<String> STATUSES = AppUtil.ul(STATUS_DOES_NOT_EXIST,
                                                         STATUS_DO_NOT_PROCESS,
                                                         STATUS_EXISTS,
                                                         STATUS_EXPECTED,
                                                         STATUS_MANIFEST,
                                                         STATUS_WANTED,
                                                         STATUS_TESTING,
                                                         STATUS_NOT_READY,
                                                         STATUS_READY,
                                                         STATUS_READY_SOURCE,
                                                         STATUS_CRAWLING,
                                                         STATUS_DEEP_CRAWL,
                                                         STATUS_FROZEN,
                                                         STATUS_ING_NOT_READY,
                                                         STATUS_FINISHED,
                                                         STATUS_RELEASING,
                                                         STATUS_RELEASED,
                                                         STATUS_DOWN,
                                                         STATUS_SUPERSEDED,
                                                         STATUS_ZAPPED);
  
  /**
   * <p>
   * Retrieves the AU's status.
   * </p>
   * 
   * @return The AU's status.
   * @since 1.67
   */
  public String getStatus() {
    return status;
  }
  
  /**
   * <p>
   * The AU's status1 (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String STATUS1 = "status1";
  
  /**
   * <p>
   * The AU's status1 (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String status1;
  
  /**
   * <p>
   * Retrieves the AU's status1.
   * </p>
   * 
   * @return The AU's status1.
   * @since 1.67
   */
  public String getStatus1() {
    return status1;
  }
  
  /**
   * <p>
   * The AU's status2 (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String STATUS2 = "status2";
  
  /**
   * <p>
   * The AU's status2 (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String status2;
  
  /**
   * <p>
   * Retrieves the AU's status2.
   * </p>
   * 
   * @return The AU's status2.
   * @since 1.67
   */
  public String getStatus2() {
    return status2;
  }

  /**
   * <p>
   * The AU's volume (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String VOLUME = "volume";

  /**
   * <p>
   * The AU's volume (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String volume;

  /**
   * <p>
   * Retrieves the AU's volume.
   * </p>
   * 
   * @return The AU's volume.
   * @since 1.67
   */
  public String getVolume() {
    return volume;
  }

  /**
   * <p>
   * The AU's year (key).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String YEAR = "year";
  
  /**
   * <p>
   * The AU's year (field).
   * </p>
   * 
   * @since 1.67
   */
  protected String year;
  
  /**
   * <p>
   * Retrieves the AU's year.
   * </p>
   * 
   * @return The AU's year.
   * @since 1.67
   */
  public String getYear() {
    return year;
  }

  /**
   * <p>
   * Mapping from string traits to functors accessing those traits. 
   * </p>
   * 
   * @since 1.67
   */
  private static final Map<String, Functor<Au, String>> functorMap = new HashMap<String, Functor<Au, String>>();
  
  /*
   * STATIC INITIALIZER
   */
  static {
    Map<String, Functor<Au, String>> m = functorMap;
    // AU traits
    abstract class A implements Functor<Au, String> {
      @Override public String apply(Au a) { return a(a); }
      abstract String a(Au a);
    }
    m.put("au:auid", new A() { @Override String a(Au a) { return a.getAuid(); } });
    m.put("au:auidplus", new A() { @Override String a(Au a) { return a.getAuidPlus(); } });
    m.put("au:edition", new A() { @Override String a(Au a) { return a.getEdition(); } });
    m.put("au:eisbn", new A() { @Override String a(Au a) { return a.getEisbn(); } });
    m.put("au:file", new A() { @Override String a(Au a) { return a.getFile(); } });
    m.put("au:fileline", new A() { @Override String a(Au a) { return String.format("%s:%d", a.getFile(), a.getLine()); } });
    m.put("au:isbn", new A() { @Override String a(Au a) { return a.getIsbn(); } });
    m.put("au:line", new A() { @Override String a(Au a) { return Integer.toString(a.getLine()); } });
    m.put("au:name", new A() { @Override String a(Au a) { return a.getName(); } });
    m.put("au:plugin", new A() { @Override String a(Au a) { return a.getComputedPlugin(); } });
    m.put("au:pluginPrefix", new A() { @Override String a(Au a) { return a.getPluginPrefix(); } });
    m.put("au:pluginSuffix", new A() { @Override String a(Au a) { return a.getPluginSuffix(); } });
    m.put("au:provider", new A() { @Override String a(Au a) { return a.getProvider(); } });
    m.put("au:proxy", new A() { @Override String a(Au a) { return a.getProxy(); } });
    m.put("au:rights", new A() { @Override String a(Au a) { return a.getRights(); } });
    m.put("au:status", new A() { @Override String a(Au a) { return a.getStatus(); } });
    m.put("au:status1", new A() { @Override String a(Au a) { return a.getStatus1(); } });
    m.put("au:status2", new A() { @Override String a(Au a) { return a.getStatus2(); } });
    m.put("au:volume", new A() { @Override String a(Au a) { return a.getVolume(); } });
    m.put("au:year", new A() { @Override String a(Au a) { return a.getYear(); } });
    // Title traits
    abstract class T implements Functor<Au, String> {
      @Override public String apply(Au a) { return t(a.getTitle()); }
      abstract String t(Title t);
    }
    m.put("title:eissn", new T() { @Override String t(Title t) { return t.getEissn(); } });
    m.put("title:doi", new T() { @Override String t(Title t) { return t.getDoi(); } });
    m.put("title:issn", new T() { @Override String t(Title t) { return t.getIssn(); } });
    m.put("title:issnl", new T() { @Override String t(Title t) { return t.getIssnl(); } });
    m.put("title:name", new T() { @Override String t(Title t) { return t.getName(); } });
    m.put("title:type", new T() { @Override String t(Title t) { return t.getType(); } });
    // Publisher traits
    abstract class P implements Functor<Au, String> {
      @Override public String apply(Au a) { return p(a.getTitle().getPublisher()); }
      abstract String p(Publisher p);
    }
    m.put("publisher:name", new P() { @Override String p(Publisher p) { return p.getName(); } });
    // Convenient abbreviations
    m.put("auid", m.get("au:auid"));
    m.put("auidplus", m.get("au:auidplus"));
    m.put("doi", m.get("title:doi"));
    m.put("edition", m.get("au:edition"));
    m.put("eisbn", m.get("au:eisbn"));
    m.put("eissn", m.get("title:eissn"));
    m.put("file", m.get("au:file"));
    m.put("fileline", m.get("au:fileline"));
    m.put("isbn", m.get("au:isbn"));
    m.put("issn", m.get("title:issn"));
    m.put("issnl", m.get("title:issnl"));
    m.put("line", m.get("au:line"));
    m.put("name", m.get("au:name"));
    m.put("plugin", m.get("au:plugin"));
    m.put("pluginPrefix", m.get("au:pluginPrefix"));
    m.put("pluginSuffix", m.get("au:pluginSuffix"));
    m.put("provider", m.get("au:provider"));
    m.put("proxy", m.get("au:proxy"));
    m.put("publisher", m.get("publisher:name"));
    m.put("rights", m.get("au:rights"));
    m.put("status", m.get("au:status"));
    m.put("status1", m.get("au:status1"));
    m.put("status2", m.get("au:status2"));
    m.put("title", m.get("title:name"));
    m.put("type", m.get("title:type"));
    m.put("volume", m.get("au:volume"));
    m.put("year", m.get("au:year"));
  }
  
  public static Functor<Au, String> traitFunctor(String trait) {
    // Well-known traits
    Functor<Au, String> functor = functorMap.get(trait);
    if (functor != null) {
      return functor;
    }
    
    final String key;

    // Publisher traits
    final String publisherPrefix = "publisher:";
    if (trait.startsWith(publisherPrefix)) {
      key = trait.substring(publisherPrefix.length());
      return new Functor<Au, String>() {
        @Override
        public String apply(Au a) {
          return a.getTitle().getPublisher().getArbitraryValue(key);
        }
      };
    }
    
    // Title traits
    final String titlePrefix = "title:";
    if (trait.startsWith(titlePrefix)) {
      key = trait.substring(titlePrefix.length());
      return new Functor<Au, String>() {
        @Override
        public String apply(Au a) {
          return a.getTitle().getArbitraryValue(key);
        }
      };
    }
    
    // AU traits
    final String auPrefix = "au:";
    boolean auColon = false;
    if (trait.startsWith(auPrefix)) {
      auColon = true;
      trait = trait.substring(auPrefix.length());
    }
    if (trait.startsWith(PARAM_PREFIX) && trait.endsWith("]")) {
      key = trait.substring(PARAM_PREFIX.length(), trait.length() - 1);
      return new Functor<Au, String>() {
        @Override
        public String apply(Au a) {
          return a.getParams().get(key);
        }
      };
    }
    if (trait.startsWith(NONDEFPARAM_PREFIX) && trait.endsWith("]")) {
      key = trait.substring(NONDEFPARAM_PREFIX.length(), trait.length() - 1);
      return new Functor<Au, String>() {
        @Override
        public String apply(Au a) {
          return a.getNondefParams().get(key);
        }
      };
    }
    if (trait.startsWith(ATTR_PREFIX) && trait.endsWith("]")) {
      key = trait.substring(ATTR_PREFIX.length(), trait.length() - 1);
      return new Functor<Au, String>() {
        @Override
        public String apply(Au a) {
          return a.getAttrs().get(key);
        }
      };
    }
    if (auColon) {
      key = trait;
      return new Functor<Au, String>() {
        @Override
        public String apply(Au a) {
          return a.getArbitraryValue(key);
        }
      }; 
    }
    
    return null;
  }
  
}