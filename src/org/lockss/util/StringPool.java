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

package org.lockss.util;
import java.util.*;
import org.lockss.config.*;

/**
 * Named intern() pools for Strings.  Similer to String.intern(), but use
 * of context-dependent pools should allow for smaller maps with less
 * waste.
 */
public class StringPool {

  static final String PREFIX = Configuration.PREFIX + "stringPool.";

  /** List of mep keys whose value should be interned in the named pool.
   * Defaults are pool-specific; See {@link org.lockss.StringPool} static
   * fields. */
  static final String PARAM_MAP_KEYS = PREFIX + "<poolname>.mapKeys";
  static final String SUFFIX_MAP_KEYS = "mapKeys";


  /** Pool for AU config property names. */
  public static StringPool AU_CONFIG_PROPS =
    new StringPool("AU config props").setMapKeys(ListUtil.list("year"));
  /** Pool for TdbAu props. */
  public static StringPool TDBAU_PROPS =
    new StringPool("TdbAu props").setMapKeys(ListUtil.list("type", "issn",
							   "issn1", "eissn"));
  /** Pool for TdbAu attrs. */
  public static StringPool TDBAU_ATTRS =
    new StringPool("TdbAu attrs").setMapKeys(ListUtil.list("publisher",
							   "au_feature_key",
							   "year",
							   "rights"));
  /** Pool for HTTP header names. */
  public static StringPool HTTP_HEADERS = new StringPool("HTTP headers");
  /** Pool for plugin IDs. */
  public static StringPool PLUGIN_IDS = new StringPool("Plugin IDs");
  /** Pool for feature version strings. */
  public static StringPool FEATURE_VERSIONS = new StringPool("Feature versions");
  /** Pool for PropertyTree keys and subkeys. */
  public static StringPool PROPERTY_TREE = new StringPool("Property trees");

  private static Map<String,StringPool> pools;

  private String name;
  private Map<String,String> map;
  private boolean sealed = false;
  private Set mapKeys = Collections.EMPTY_SET;
  private int hits = 0;

  public StringPool(String name) {
    this(name, 20);
  }

  /** Create a StringPool with a name and initial size */
  public StringPool(String name, int initialSize) {
    this.name = name;
    map = new HashMap<String,String>(initialSize);
    registerPool(name, this);
  }

  private static void registerPool(String name, StringPool pool) {
    if (pools == null) {
      pools = new HashMap<String,StringPool>();
    }
    pools.put(name, pool);
    Configuration poolConf =
      ConfigManager.getCurrentConfig().getConfigTree(PREFIX + "." + name);
    if (poolConf != null) {
      pool.setPoolConfig(poolConf);
    }
  }

  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
                               Configuration oldConfig,
                               Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      Configuration allPools = config.getConfigTree(PREFIX);
      for (Iterator<String> iter = allPools.nodeIterator(); iter.hasNext(); ) {
	String poolName = iter.next();
	StringPool pool = getPool(poolName);
	if (pool != null) {
	  Configuration poolConf = allPools.getConfigTree(poolName);
	  if (poolConf != null) {
	    pool.setPoolConfig(poolConf);
	  }
	}
      }
    }
  }

  public void setPoolConfig(Configuration poolConfig) {
    setMapKeys(poolConfig.getList(SUFFIX_MAP_KEYS, Collections.EMPTY_LIST));
  }    

  public static StringPool getPool(String name) {
    return pools.get(name);
  }

  private StringPool setMapKeys(Collection<String> keys) {
    mapKeys = SetUtil.theSet(keys);
    return this;
  }

  /** Return the instance of the string already in the pool, if any, else
   * add this instance and return it.
   * @param str the String to be interned.  If null, null is returned. */
  public synchronized String intern(String str) {
    if (str == null) {
      return str;
    }
    String res = map.get(str);
    if (res != null) {
      hits++;
      return res;
    }
    if (sealed) {
      return str;
    }
    map.put(str, str);
    return str;
  }

  /** Intern the value iff the key is a member of this StringPool's set of
   * map keys whose values should be interned.
   * @param key the map key
   * @param val the String to be stored in the map.
   * @return the interned value if the key is contained in the set of map
   * keys whose values should be interned, else the original value.
   */
  public synchronized String internMapValue(String key, String val) {
    if (mapKeys.contains(key)) {
      return intern(val);
    } else {
      return val;
    }
  }

  /** Seal the pool, so that no new additions will be made.  If {@link
   * #intern(String)} is called with a string that matches an existing
   * entry the interned entry will be returned, else the argument.
   * Intended for contexts in which a predictable standard set of strings
   * appear as well as one-off strings that would needlessly fill the
   * pool. */
  public void seal() {
    sealed = true;
  }

  private int sumStringChars() {
    int res = 0;
    for (String val : map.values()) {
      res += val.length();
    }
    return res;
  }

  public String toString() {
    return "[StringPool " + name + ", " + map.size() + " entries]";
  }

  public String toStats() {
    return "[StringPool " + name + ", " + map.size() + " entries, " +
      hits + " hits, " +
      sumStringChars() + " total chars]";
  }

  public static String allStats() {
    StringBuilder sb = new StringBuilder();
    for (StringPool pool : pools.values()) {
      sb.append(pool.toStats());
      sb.append("\n");
    }
    return sb.toString();
  }

}
