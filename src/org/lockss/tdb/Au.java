/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.tdb;

import java.util.*;

import org.lockss.plugin.PluginManager;
import org.lockss.util.*;

/**
 * <p>
 * A lightweight class (struct) to represent an AU during TDB processing.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class Au {

  /**
   * <p>
   * Make a new root AU instance.
   * </p>
   * 
   * @since 1.67 
   */
  public Au() {
    // Intentionally left blank
  }

  /**
   * <p>
   * Makes a new AU instance based on the given AU instance.
   * </p>
   * 
   * @param other
   *          An existing AU instance.
   * @since 1.67
   */
  public Au(Au other) {
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
   * @since 1.67
   */
  public Au(Title title, Au other) {
    this(other);
    this.title = title;
  }
  
  /**
   * <p>
   * Makes a new AU instance with the given parent title (useful for tests).
   * </p>
   * 
   * @param title
   *          A parent title.
   * @since 1.67
   */
  protected Au(Title title) {
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
   * 
   * @param key
   *          A key.
   * @param value
   *          The value for the key.
   * @since 1.67
   */
  public void put(String key, String value) {
    switch (key.charAt(0)) {
      case 'a': {
        if (key.startsWith(ATTR_PREFIX)) {
          if (attrsMap == null) {
            attrsMap = new HashMap<String, String>();
          }
          attrsMap.put(key.substring(ATTR_PREFIX.length(), key.length() - 1), value);
          return;
        }
      } break;
      case 'e': {
        if (EDITION.equals(key)) {
          edition = value;
          return;
        }
        else if (EISBN.equals(key)) {
          eisbn = value;
          return;
        }
      } break;
      case 'i': {
        if (ISBN.equals(key)) {
          isbn = value;
          return;
        }
      } break;
      case 'n': {
        if (NAME.equals(key)) {
          name = value;
          return;
        }
        else if (key.startsWith(NONDEFPARAM_PREFIX)) {
          if (nondefParamsMap == null) {
            nondefParamsMap = new HashMap<String, String>();
          }
          nondefParamsMap.put(key.substring(NONDEFPARAM_PREFIX.length(), key.length() - 1), value);
          return;
        }
      } break;
      case 'p': {
        if (key.startsWith(PARAM_PREFIX)) {
          if (paramsMap == null) {
            paramsMap = new HashMap<String, String>();
          }
          paramsMap.put(key.substring(PARAM_PREFIX.length(), key.length() - 1), value);
          return;
        }
        else if (PLUGIN.equals(key)) {
          plugin = value;
          return;
        }
        else if (PLUGIN_PREFIX.equals(key)) {
          pluginPrefix = value;
          return;
        }
        else if (PLUGIN_SUFFIX.equals(key)) {
          pluginSuffix = value;
          return;
        }
        else if (PROVIDER.equals(key)) {
          provider = value;
          return;
        }
        else if (PROXY.equals(key)) {
          proxy = value;
          return;
        }
      } break;
      case 'r': {
        if (RIGHTS.equals(key)) {
          rights = value;
          return;
        }
      } break;
      case 's': {
        if (STATUS.equals(key)) {
          status = value;
          return;
        }
        else if (STATUS1.equals(key)) {
          status1 = value;
          return;
        }
        else if (STATUS2.equals(key)) {
          status2 = value;
          return;
        }
      } break;
      case 'v': {
        if (VOLUME.equals(key)) {
          volume = value;
          return;
        }
      } break;
      case 'y': {
        if (YEAR.equals(key)) {
          year = value;
          return;
        }
      } break;
    }

    // Any other key goes to the general map
    if (map == null) {
      map = new HashMap<String, String>();
    }
    map.put(key, value);
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
      String plugin = getPlugin();
      Map<String, String> params = getParams();
      if (plugin != null && params != null && params.size() > 0) {
        auid = PluginManager.generateAuId(plugin, PropUtil.propsToCanonicalEncodedString(params));
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
            sb.append(PropKeyEncoder.encode(nondefkey));
            sb.append("~");
            sb.append(PropKeyEncoder.encode(nondefParams.get(nondefkey)));
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
    if (plugin == null) {
      String prefix = getPluginPrefix();
      String suffix = getPluginSuffix();
      if (prefix != null && suffix != null) {
        plugin = prefix + suffix;
      }
    }
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
    m.put("au:isbn", new A() { @Override String a(Au a) { return a.getIsbn(); } });
    m.put("au:name", new A() { @Override String a(Au a) { return a.getName(); } });
    m.put("au:plugin", new A() { @Override String a(Au a) { return a.getPlugin(); } });
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
    m.put("isbn", m.get("au:isbn"));
    m.put("issn", m.get("title:issn"));
    m.put("issnl", m.get("title:issnl"));
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