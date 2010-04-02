/*
 * $Id: Configuration.java,v 1.25 2010-04-02 23:08:25 pgust Exp $
 */

/*

Copyright (c) 2001-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import java.io.*;
import java.util.*;

import org.lockss.util.*;

/** <code>Configuration</code> provides access to the LOCKSS configuration
 * parameters.  Instances of (concrete subclasses of)
 * <code>Configuration</code> hold a set of configuration parameters, and
 * have a standard set of accessors.  Static methods on this class provide
 * convenient access to parameter values in the "current" configuration;
 * these accessors all have <code>Param</code> in their name.  (If called
 * on a <code>Configuration</code> <i>instance</i>, they will return values
 * from the current configuration, not that instance.  So don't do that.)
 */
public abstract class Configuration {
  /** The common prefix string of all LOCKSS configuration parameters. */
  public static final String PREFIX = "org.lockss.";

  /** Common prefix of platform config params */
  public static final String PLATFORM = PREFIX + "platform.";
  public static final String DAEMON = PREFIX + "daemon.";

  // MUST pass in explicit log level to avoid recursive call back to
  // Configuration to get Config log level.  (Others should NOT do this.)
  protected static Logger log =
    Logger.getLoggerWithInitialLevel("Config",
                                     Logger.getInitialDefaultLevel());

  /**
   * The title database for this configuration
   */
  private Tdb tdb;
  
  /**
   * Convenience methods for getting useful platform settings.
   */
  public static Configuration getPlatformConfig() {
    return ConfigManager.getPlatformConfig();
  }

  public static PlatformVersion getPlatformVersion() {
    return ConfigManager.getPlatformVersion();
  }

  public String getPlatformGroups() {
    return get(ConfigManager.PARAM_DAEMON_GROUPS,
	       ConfigManager.DEFAULT_DAEMON_GROUP);
  }

  public List getPlatformGroupList() {
    return getList(ConfigManager.PARAM_DAEMON_GROUPS,
		   ConfigManager.DEFAULT_DAEMON_GROUP_LIST);
  }

  public static String getPlatformHostname() {
    return ConfigManager.getPlatformHostname();
  }

  /**
   * Gets the title database (TDB) for this configuration
   * @return
   */
  public Tdb getTdb() {
    return tdb;
  }

  /**
   * Return number of TdbAus in this instance's Tdb.
   * 
   * @return the number of TdbAus in this instances's Tdb
   */
  public int getTdbAuCount() {
    return (tdb == null) ? 0 : tdb.getTdbAuCount();
  }

  /**
   * Determine whether Tdbs for the two configurations are different.
   * 
   * @param config the other configuration
   * @return <code>true</code> if different
   */
  public boolean isTdbDifferent(Configuration config) {
    if (tdb == null) {
      return config.getTdb() != null;
    } 
    if (config.getTdb() == null) {
      return (tdb != null);
      
    }
    return !tdb.getPluginIdsForDifferences(config.getTdb()).isEmpty();
  }
  /**
   * Sets the Tdb for this configuration.
   * 
   * @param tdb the Tdb to set
   */
  public void setTdb(Tdb tdb) {
    if (isSealed()) {
      throw new IllegalStateException("Can't modify sealed configuration");
    }
    this.tdb = tdb;
  }
  
  /**
   * Special operation allows setting the title database of one
   * configuration from the title database of another.  This operation
   * is for use only within support routines in the "org.lockss.config"
   * package.
   * 
   * @param config another Configuration
   */
  protected void setTdbFromConfig(Configuration config) {
    this.tdb = config.getTdb();
  }
  
  /** Return a copy of the configuration with the specified prefix
   * prepended to all keys. */
  public Configuration addPrefix(String prefix) {
    if (!prefix.endsWith(".")) {
      prefix = prefix + ".";
    }
    Configuration res = ConfigManager.newConfiguration();
    res.addAsSubTree(this, prefix);
    return res;
  }

  /** Add to this all values in config, prepending the prefix to all keys */
  public void addAsSubTree(Configuration config, String prefix) {
    if (!prefix.endsWith(".")) {
      prefix = prefix + ".";
    }
    for (Iterator iter = config.keyIterator(); iter.hasNext();) {
      String key = (String)iter.next();
      put(prefix + key, config.get(key));
    }
  }

  /** Return a copy of the Configuration that does not share structure with
   * the original.  The copy is not sealed, even if the original was.
   * @return a copy
   */
  public Configuration copy() {
    Configuration copy = ConfigManager.newConfiguration();
    copy.copyFrom(this);
    return copy;
  }

  /** Copy contents of the argument into this config.  Duplicate
   *  keys will be overwritten.
   */
  public void copyFrom(Configuration other) {
    // merge other config tree into this one
    for (Iterator iter = other.keyIterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      put(key, other.get(key));
    }
    
    // merge other config Tdb into this one
    Tdb otherTdb = other.getTdb();
    if (otherTdb != null) {
      if (tdb == null) {
        tdb = new Tdb();
      }
      tdb.copyFrom(otherTdb);
    }
  }

  /** Copy contents of the argument into this config.  Duplicate keys
   *  will <em>not</em> be overwritten.
   */
  public void copyFromNonDestructively(Configuration other) {
    for (Iterator iter = other.keyIterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (!containsKey(key)) {
	put(key, other.get(key));
      }
    }
  }

  private ConfigCache getConfigCache() {
    return ConfigManager.getConfigManager().getConfigCache();
  }

  /**
   * Given a Config File, load its configuration into this one.  This
   * will overwrite any existing properties with the properties from
   * the Config File.
   */
  void load(ConfigFile cf) throws IOException {
    copyFrom(cf.getConfiguration());
  }

  /** Return the first ConfigFile that got an error */
  public ConfigFile getFirstErrorFile(List urls) {
    ConfigCache configCache = getConfigCache();
    for (Iterator iter = urls.iterator(); iter.hasNext();) {
      String url = (String)iter.next();
      if (StringUtil.endsWithIgnoreCase(url, ".opt")) {
	continue;
      }
      ConfigFile cf = configCache.get(url);
      if (cf != null && !cf.isLoaded()) {
	return cf;
      }
    }
    return null;
  }

  public abstract boolean store(OutputStream ostr, String header)
      throws IOException;

  /** Return a Configuration.Differences representing the set of keys whose
   * values differ.
   * @param otherConfig the config to compare with.  May be null.
   */
  public abstract Differences differences(Configuration otherConfig);

  /** Return the set of keys whose values differ.
   * @param otherConfig the config to compare with.  May be null.
   */
  public abstract Set<String> differentKeys(Configuration otherConfig);
  

  /** Return the set of pluginIds involved in differences between two configurations.
   * @param otherConfig the config to compare with.  May be null.
   */
  public abstract Set<String> differentPluginIds(Configuration otherConfig);
  

  /** Return true iff config has no keys/ */
  public boolean isEmpty() {
    if (keyIterator().hasNext()) {
      return false;
    } 
    Tdb tdb = getTdb();
    if ((tdb != null) && !tdb.isEmpty()) {
      return false;
    }
    
    return true;
  }

  /** Return the config value associated with <code>key</code>.
   * If the value is null or the key is missing, return <code>dfault</code>.
   */
  public String get(String key, String dfault) {
    String val = get(key);
    if (val == null) {
      val = dfault;
    }
    return val;
  }

  private static Map boolStrings = new HashMap();
  static {
    boolStrings.put("true", Boolean.TRUE);
    boolStrings.put("yes", Boolean.TRUE);
    boolStrings.put("on", Boolean.TRUE);
    boolStrings.put("1", Boolean.TRUE);
    boolStrings.put("false", Boolean.FALSE);
    boolStrings.put("no", Boolean.FALSE);
    boolStrings.put("off", Boolean.FALSE);
    boolStrings.put("0", Boolean.FALSE);
  }

  private Boolean stringToBool(String s) {
    if (s == null) {
      return null;
    }
    Boolean res = (Boolean)boolStrings.get(s);
    if (res != null) {
      return res;
    } else {
      return (Boolean)boolStrings.get(s.toLowerCase());
    }
  }

  /** Return the config value as a boolean.
   * @throws Configuration.InvalidParam if the value is missing or
   * not parsable as a boolean.
   */
  public boolean getBoolean(String key) throws InvalidParam {
    String val = get(key);
    Boolean bool = stringToBool(val);
    if (bool != null) {
      return bool.booleanValue();
    }
    throw newInvalid("Not a boolean value: ", key, val);
  }

  /** Return the config value as a boolean.  If it's missing, return the
   * default value.  If it's present but not parsable as a boolean, log a
   * warning and return the default value.
   */
  public boolean getBoolean(String key, boolean dfault) {
    String val = get(key);
    if (val == null) {
      return dfault;
    }
    Boolean bool = stringToBool(val);
    if (bool != null) {
      return bool.booleanValue();
    }
    log.warning("getBoolean(\"" + key + "\") = \"" + val + "\"");
    return dfault;
  }

  /** Return the config value as an int.
   * @throws Configuration.InvalidParam if the value is missing or
   * not parsable as an int.
   */
  public int getInt(String key) throws InvalidParam {
    String val = get(key);
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException e) {
      throw newInvalid("Not an int value: ", key, val);
    }
  }

  /** Return the config value as an int.  If it's missing, return the
   * default value.  If it's present but not parsable as an int, log a
   * warning and return the default value
   */
  public int getInt(String key, int dfault) {
    String val = get(key);
    if (val == null) {
      return dfault;
    }
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException e) {
      log.warning("getInt(\'" + key + "\") = \"" + val + "\"");
      return dfault;
    }
  }

  /** Return the config value as an enum.
   * @throws Configuration.InvalidParam if the value is missing or
   * not parsable as an enum.
   */
  public Enum getEnum(Class enumType, String key) throws InvalidParam {
    String name = get(key);
    try {
      return Enum.valueOf(enumType, name);
    } catch (IllegalArgumentException e) {
      throw newInvalid("Not an enum of type: " + enumType, key, name);
    }
  }

  /** Return the config value as an enum.  If it's missing, return the
   * default value.  If it's present but not parsable as an enum, log a
   * warning and return the default value
   */
  public Enum getEnum(Class enumType, String key, Enum dfault) {
    String name = get(key);
    if (name == null) {
      return dfault;
    }
    try {
      return Enum.valueOf(enumType, name);
    } catch (IllegalArgumentException e) {
      log.warning("getEnum(\'" + key + "\") illegal val: \"" + name + "\"");
      return dfault;
    }
  }

  /**
   * Return a list of values for the specified key.
   */
  public abstract List getList(String key);

  public List getList(String key, List dfault) {
    if (get(key) != null) {
      return getList(key);
    } else {
      return dfault;
    }
  }

  /** Return the config value as a long.
   * @throws Configuration.InvalidParam if the value is missing or
   * not parsable as a long.
   */
  public long getLong(String key) throws InvalidParam {
    String val = get(key);
    try {
      return Long.parseLong(val);
    } catch (NumberFormatException e) {
      throw newInvalid("Not a long value: ", key, val);
    }
  }

  /** Return the config value as a long.  If it's missing, return the
   * default value.  If it's present but not parsable as a long, log a
   * warning and return the default value
   */
  public long getLong(String key, long dfault) {
    String val = get(key);
    if (val == null) {
      return dfault;
    }
    try {
      return Long.parseLong(val);
    } catch (NumberFormatException e) {
      log.warning("getLong(\'" + key + "\") = \"" + val + "\"");
      return dfault;
    }
  }

  /** Parse the config value as a time interval.  An interval is specified
   * as an integer with an optional suffix.  No suffix means milliseconds,
   * s, m, h, d, w indicates seconds, minutes, hours, days and weeks
   * respectively.
   * @param key the configuration parameter name
   * @return time interval
   * @throws Configuration.InvalidParam if the value is missing or
   * not parsable as a time interval.
   */
  public long getTimeInterval(String key) throws InvalidParam {
    String val = get(key);
    try {
      return StringUtil.parseTimeInterval(val);
    } catch (Exception e) {
      throw newInvalid("Not a time interval value: ", key, val);
    }
  }

  /** Parse the config value as a time interval.  An interval is specified
   * as an integer with an optional suffix.  No suffix means milliseconds,
   * s, m, h, d, w indicates seconds, minutes, hours, days and weeks
   * respectively.  If the parameter is not present, return the
   * default value.  If it's present but not parsable as a long, log a
   * warning and return the default value.
   * @param key the configuration parameter name
   * @param dfault the default value in milliseconds
   * @return time interval
   */
  public long getTimeInterval(String key, long dfault) {
    String val = get(key);
    if (val == null) {
      return dfault;
    }
    try {
      return StringUtil.parseTimeInterval(val);
    } catch (Exception e) {
      log.warning("getTimeInterval(\'" + key + "\") = \"" + val + "\"");
      return dfault;
    }
  }

  /** Parse the config value as a size-in-bytes, specified
   * as an integer with an optional suffix.  No suffix means bytes,
   * kb, mb, gb, tb indicate kilo-, mega-, giga, tera-bytes respectively.
   * @param key the configuration parameter name
   * @return size in bytes
   * @throws Configuration.InvalidParam if the value is missing or
   * not parsable as a time interval.
   */
  public long getSize(String key) throws InvalidParam {
    String val = get(key);
    try {
      return StringUtil.parseSize(val);
    } catch (Exception e) {
      throw newInvalid("Not a valid size: ", key, val);
    }
  }

  /** Parse the config value as a size-in-bytes, specified
   * as an integer with an optional suffix.  No suffix means bytes,
   * kb, mb, gb, tb indicate kilo-, mega-, giga, tera-bytes respectively.
   * @param key the configuration parameter name
   * @param dfault the default value in milliseconds
   * @return size in bytes
   */
  public long getSize(String key, long dfault) {
    String val = get(key);
    if (val == null) {
      return dfault;
    }
    try {
      return StringUtil.parseSize(val);
    } catch (Exception e) {
      log.warning("getSize(\'" + key + "\") = \"" + val + "\"");
      return dfault;
    }
  }

  /** Parse the config value (which should be a non-negative integer) as a
   * percentage, returning a positive float or 0.0.  (<i>Ie</i>, "100"
   * returns 1.0.)  Percentages greater then 100 are allowed.
   * @param key the configuration parameter name
   * @return a float between 0.0 and 1.0
   * @throws Configuration.InvalidParam if the value is missing or
   * not an integer between 0 and 100.
   */
  public float getPercentage(String key) throws InvalidParam {
    int val = getInt(key);
    if (val < 0) {
      throw newInvalid("Not an integer >= 0: ", key, Integer.toString(val));
    }
    return ((float)val) / (float)100.0;
  }

  /** Parse the config value (which should be a non-negative integer) as a
   * percentage, returning a positive float or 0.0.  (<i>Ie</i>, "100"
   * returns 1.0.)  Percentages greater then 100 are allowed.  If the
   * parameter is not present, return the default value.  If it's present
   * but not parsable as an int, log a warning and return the default
   * value.
   * @param key the configuration parameter name
   * @return a float between 0.0 and 1.0
   */
  public float getPercentage(String key, double dfault) {
    int val;
    if (!containsKey(key)) {
      return (float)dfault;
    }
    try {
      val = getInt(key);
    } catch (InvalidParam e) {
      log.warning("getPercentage(\'" + key + "\") = \"" + get(key) + "\"");
      return (float)dfault;
    }
    if (val < 0) {
      log.warning("getPercentage(\'" + key + "\") = \"" + val + "\"");
      return (float)dfault;
    }
    return ((float)val) / 100.0f;
  }

  /** Parse the config value as a floating point value
   * @param key the configuration parameter name
   * @return a double
   * @throws Configuration.InvalidParam if the value is missing or not an
   * float.
   */
  public double getDouble(String key) throws InvalidParam {
    String val = get(key);
    try {
      return Double.parseDouble(val);
    } catch (NumberFormatException e) {
      throw newInvalid("Not a float value: ", key, val);
    }
  }

  /** Parse the config value as a floating point value
   * @param key the configuration parameter name
   * @param dfault the default value
   * @return a double
   */
  public double getDouble(String key, double dfault) {
    String val = get(key);
    if (val == null) {
      return dfault;
    }
    try {
      return Double.parseDouble(val);
    } catch (NumberFormatException e) {
      log.warning("getInt(\'" + key + "\") = \"" + val + "\"");
      return dfault;
    }
  }

  InvalidParam newInvalid(String msg, String key, String val) {
    return new InvalidParam(msg  + key + " = " + quoteVal(val));
  }

  String quoteVal(String val) {
    return val == null ? "(null)" : "\"" + val + "\"";
  }

  /** Remove the subtree below the specified key.
   * @param rootKey The key at the root of the subtree to be deleted.  This
   * key and all below it are removed.
   */
  public void removeConfigTree(String rootKey) {
    Configuration subtree = getConfigTree(rootKey);
    for (Iterator iter = subtree.keyIterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      remove(rootKey + "." + key);
    }
    remove(rootKey);
  }

  /**
   * Copy the contents of another configuration relative to the
   * specified "root" key.
   *
   * @param fromConfig The Configuration from which to copy.
   * @param root The root key from which to copy.
   *
   */
  public void copyConfigTreeFrom(Configuration fromConfig, String root) {
    Configuration subtree = fromConfig.getConfigTree(root);
    if (subtree.isEmpty()) {
      return;
    }
    for (Iterator iter = subtree.keyIterator(); iter.hasNext(); ) {
      String relkey = (String)iter.next();
      String key = root + "." + relkey;
      put(key, fromConfig.get(key));
    }
    if (fromConfig.containsKey(root)) {
      put(root, fromConfig.get(root));
    }
  }

  // must be implemented by implementation subclass

  abstract void reset();

  /** return true iff the configurations have the same keys
   * with the same values.
   */
  public abstract boolean equals(Object c);

  /** Return true if the key is present
   * @param key the key to test
   * @return true if the key is present.
   */
  public abstract boolean containsKey(String key);

  /** Return the config value associated with <code>key</code>.
   * @return the string, or null if the key is not present
   * or its value is null.
   */
  public abstract String get(String key);

  /** Set the config value associated with <code>key</code>.
   * @param key the config key
   * @param val the new value
   */
  public abstract void put(String key, String val);

  /** Remove the value associated with <code>key</code>.
   * @param key the config key to remove
   */
  public abstract void remove(String key);

  /** Seal the configuration so that no changes can be made */
  public abstract void seal();

  /** Return true iff the configuration is sealed */
  public abstract boolean isSealed();

  /** Returns a Configuration instance containing all the keys at or
   * below <code>key</code>
   */
  public abstract Configuration getConfigTree(String key);

  /** Returns the set of keys in the configuration.
   */
  public abstract Set<String> keySet();

  /** Returns an <code>Iterator</code> over all the keys in the configuration.
   */
  public abstract Iterator keyIterator();

  /** Returns an <code>Iterator</code> over all the top level
     keys in the configuration.
   */
  public abstract Iterator nodeIterator();

  /** Returns an <code>Iterator</code> over the top-level keys in the
   * configuration subtree below <code>key</code>
   */
  public abstract Iterator nodeIterator(String key);

  // static convenience methods


  /**
   * The <code>Configuration.Callback</code> interface defines the
   * callback registered by clients of <code>Configuration</code>
   * who want to know when the configuration has changed.
   */
  public interface Callback {
    /**
     * Callback used to inform clients that something in the configuration
     * has changed.  It is called after the new config is installed as
     * current, as well as upon registration (if there is a current
     * configuration at the time).  It is thus safe to rely solely on a
     * configuration callback to receive configuration information.
     * @param newConfig  the new (just installed) <code>Configuration</code>.
     * @param oldConfig  the previous <code>Configuration</code>, or null
     *                   if there was no previous config.
     * @param changes  the set of keys whose value has changed.
     */
    public void configurationChanged(Configuration newConfig,
				     Configuration oldConfig,
				     Configuration.Differences changes);
  }

  /** Differences represents the changes in a Configuration from the
   * previous Configuration.  It allows Configuration.Callbacks to quickly
   * determine whether a key of interest, or any key in a subtree of
   * interest, has changed value since the previous call to
   * configurationChanged().
   */
  public interface Differences {
    /** 
     * Determine whether the value of a key has changed.  Can also be used
     * to determine whether there have been any changes in a named
     * Configuration subtree.  (<i>Eg</i>, if contains() is true of
     * "org.lockss.foo.bar", it will also be true of "org.lockss.foo.",
     * "org.lockss.foo", "org.lockss.", "org.lockss", "org."  and "org".)
     * 
     * @param key the key or key prefix
     * @return true iff the value of the key, or any key in the tree below
     * it, has changed. 
     */
    public boolean contains(String key);

    /** 
     * Return the set of changed keys.
     * This method should not generally be used. 
     * 
     * @return the set of keys that have changed.  
     */
    public Set<String> getDifferenceSet();
    
    /**
     * Return a set of pluginIDs for differences between two Tdbs.
     * This method is normally used by plugins.
     * 
     * @return a set of pluginIDs for differences between two Tdbs.
     */
    public Set<String> getTdbDifferencePluginIds();

    /**
     * Determines whether a plugin ID is involved in differences between
     * to Tdbs.  This method is normally used by plugins.
     * 
     * @param pluginID the pluginID
     * @return <code>true</code iff differences between two TDBs involves
     *  the specified plugin
     */
    public boolean containsTdbPluginId(String pluginId);
    
    /**
     * Return the difference in the number of TdbAus between the old
     * and new Tdbs.
     * 
     * @return the difference in the number of Tdbs
     */
    public int getTdbAuDifferenceCount();
  }

  /** 
   * A Differences used when all keys have changed (<i>E.g.</i>, after the
   * initial config load, or when a new config callback is registered). 
   */
  static class DifferencesAll implements Differences {
    final Configuration config;
    
    public DifferencesAll(Configuration config) {
      if (config == null) {
        throw new IllegalArgumentException("config cannot be null");
      }
      this.config = config;
    }

    /**
     * Return true, indicating that the key has changed.
     * <p>
     * Note: parts of the system rely on this method returning 
     * <code>true</code> for initialization, even though the the
     * new Configuration <i>doesn't</i> contain the key. 
     * 
     * @return <code>true</code>
     */
    public boolean contains(String key) {
      return true;
    }

    /** 
     * Return null, indicating that all keys have changed.
     * 
     * @return <code>null</code>
     */
    public Set<String> getDifferenceSet() {
      return config.keySet();
    }
   
    /**
     * Return null, indicating that all Tdb entries have changed
     * 
     * @return <code>null</code>
     */
    public Set<String> getTdbDifferencePluginIds() {
      Tdb tdb = config.getTdb();
      return (tdb == null) ? Collections.EMPTY_SET : tdb.getAllTdbAus().keySet();
    }

    /**
     * Determines whether a plugin ID is involved in differences between
     * to Tdbs.  This method is normally used by plugins.
     * 
     * @param pluginID the pluginID
     * @return <code>true</code iff differences between two TDBs involves
     *  the specified plugin
     */
    public boolean containsTdbPluginId(String pluginId) {
      return getTdbDifferencePluginIds().contains(pluginId);
    }
    
    /**
     * Return the difference in the number of TdbAus between the old
     * and new Tdbs.
     * 
     * @return the difference in the number of Tdbs
     */
    public int getTdbAuDifferenceCount() {
      Tdb tdb = config.getTdb();
      return (tdb == null) ? 0 : tdb.getTdbAuCount();
    }
  }

  /** 
   * A Differences that contains a set of changed keys
   * and whether configuration TDBs are different
   */
  static class DifferencesSet implements Differences {
    private Set<String> diffKeys;
    private Set<String> diffPluginIds;
    private int tdbAuCountDiff;
    
    DifferencesSet(Set diffKeys, int tdbAuCountDiff, Set<String>diffPluginIds) {
      this.diffKeys = diffKeys;
      this.diffPluginIds = diffPluginIds;
      this.tdbAuCountDiff = tdbAuCountDiff;
    }

    public boolean contains(String key) {
      if (!key.endsWith(".")) {
        return diffKeys.contains(key);
      }
      for (String diffKey : diffKeys) {
        if (diffKey.startsWith(key)) {
          return true;
        }
      }
      return false;
    }

    public Set<String> getDifferenceSet() {
      return diffKeys;
    }
    
    public Set<String> getTdbDifferencePluginIds() {
      return diffPluginIds;
    }

    public boolean containsTdbPluginId(String pluginId) {
      return diffPluginIds.contains(pluginId);
    }
    
    /**
     * Return the difference in the number of TdbAus between the old
     * and new Tdbs.
     * 
     * @return the difference in the number of Tdbs
     */
    public int getTdbAuDifferenceCount() {
      return tdbAuCountDiff;
    }
  }

  /** Exception thrown for errors in accessors. */
  public static class InvalidParam extends Exception {
    public InvalidParam(String message) {
      super(message);
    }
  }
}
