/*
 * $Id: Configuration.java,v 1.60 2004-05-28 04:57:30 smorabito Exp $
 */

/*

Copyright (c) 2001-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.io.*;
import java.net.*;
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

  public static final String PLATFORM = PREFIX + "platform.";

  // MUST pass in explicit log level to avoid recursive call back to
  // Configuration to get Config log level.  (Others should NOT do this.)
  protected static Logger log =
    Logger.getLoggerWithInitialLevel("Config",
				     Logger.getInitialDefaultLevel());

  /** Return current configuration */
  public static Configuration getCurrentConfig() {
    return ConfigManager.getCurrentConfig();
  }

  /**
   * Register a {@link Configuration.Callback}, which will be called
   * whenever the current configuration has changed.  If a configuration is
   * present when a callback is registered, the callback will be called
   * immediately.
   * @param c <code>Configuration.Callback</code> to add.  */
  public static void registerConfigurationCallback(Callback c) {
    ConfigManager.getConfigManager().registerConfigurationCallback(c);
  }

  /**
   * Unregister a <code>Configuration.Callback</code>.
   * @param c <code>Configuration.Callback</code> to remove.
   */
  public static void unregisterConfigurationCallback(Callback c) {
    ConfigManager.getConfigManager().unregisterConfigurationCallback(c);
  }

  // instance methods

  /** Return a copy of the configuration with the specified prefix
   * prepended to all keys. */
  public Configuration addPrefix(String prefix) {
    if (!prefix.endsWith(".")) {
      prefix = prefix + ".";
    }
    Configuration res = ConfigManager.newConfiguration();
    for (Iterator iter = keyIterator(); iter.hasNext();) {
      String key = (String)iter.next();
      res.put(prefix + key, get(key));
    }
    return res;
  }

  /** Return a copy of the Configuration that does not share structure with
   * the original.  The copy is not sealed, even if the original was.
   * @return a copy
   */
  public Configuration copy() {
    Configuration copy = ConfigManager.newConfiguration();
    for (Iterator iter = keyIterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      copy.put(key, get(key));
    }
    return copy;
  }

  /**
   * Try to load config from a list or urls
   * @return true iff properties were successfully loaded
   */
  boolean loadList(List urls) {
    return loadList(urls, false);
  }

  /**
   * Try to load config from a list or urls
   * @return true iff properties were successfully loaded
   */
  boolean loadList(List urls, boolean failOk) {
    for (Iterator iter = urls.iterator(); iter.hasNext();) {
      String url = (String)iter.next();
      try {
	load(url);
      } catch (IOException e) {
	if (e instanceof FileNotFoundException &&
	    StringUtil.endsWithIgnoreCase(url.toString(), ".opt")) {
	  log.info("Not loading props from nonexistent optional file: " + url);
	} else {
	  // This load failed.  Fail the whole thing.
	  if (!failOk) {
	    log.warning("Couldn't load props from " + url + ": " +
			e.toString());
	    reset();			// ensure config is empty
	  }
	  return false;
	}
      }
    }
    return true;
  }

  void load(String url) throws IOException {
    InputStream istr;
    try {
      URL u = new URL(url);
      istr = UrlUtil.openInputStream(url);
      log.debug2("load URL: " + url);
    } catch (MalformedURLException e) {
      istr = new FileInputStream(url);
      log.debug2("load file: " + url);
    }
    InputStream bis = new BufferedInputStream(istr);
    if (url.toLowerCase().endsWith(".xml")) {
      loadXmlProperties(bis);
    } else {
      loadTextProperties(bis);
    }

    bis.close();
  }

  abstract boolean loadXmlProperties(InputStream istr)
      throws IOException;

  abstract boolean loadTextProperties(InputStream istr)
      throws IOException;

  abstract boolean store(OutputStream ostr, String header)
      throws IOException;

  /** Return the set of keys whose values differ.
   * @param otherConfig the config to compare with.  May be null.
   */
  public abstract Set differentKeys(Configuration otherConfig);

  /** Return true iff config has no keys/ */
  public boolean isEmpty() {
    return !(keyIterator().hasNext());
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
    log.warning("getBoolean(\'" + key + "\") = \"" + val + "\"");
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

  /**
   * Return a list of values for the specified key.
   */
  public abstract List getList(String key);

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

  /** Parse the config value (which must be an integer between 0 and 100)
   * as a percentage, returning a float between 0.0 and 1.0.
   * @param key the configuration parameter name
   * @return a float between 0.0 and 1.0
   * @throws Configuration.InvalidParam if the value is missing or
   * not an integer between 0 and 100.
   */
  public float getPercentage(String key) throws InvalidParam {
    int val = getInt(key);
    if (val < 0 || val > 100) {
      throw newInvalid("Not an integer between 0 and 100: ", key,
		       Integer.toString(val));
    }
    return ((float)val) / (float)100.0;
  }

  /** Parse the config value (which should be an integer between 0 and 100)
   * as a percentage, returning a float between 0.0 and 1.0.  If the
   * parameter is not present, return the default value.  If it's present
   * but not parsable as an int between 0 and 100, log a warning and return
   * the default value.
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
    if (val < 0 || val > 100) {
      log.warning("getPercentage(\'" + key + "\") = \"" + val + "\"");
      return (float)dfault;
    }
    return ((float)val) / 100.0f;
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

  /** Remove the subtree below the specified key.
   * @param rootKey The key at the root of the subtree to be deleted.  This
   * key and all below it are removed.
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
  public abstract Set keySet();

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

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static String getParam(String key) {
    return getCurrentConfig().get(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static String getParam(String key, String dfault) {
    return getCurrentConfig().get(key, dfault);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static boolean getBooleanParam(String key) throws InvalidParam {
    return getCurrentConfig().getBoolean(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static boolean getBooleanParam(String key, boolean dfault) {
    return getCurrentConfig().getBoolean(key, dfault);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static int getIntParam(String key) throws InvalidParam {
    return getCurrentConfig().getInt(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static int getIntParam(String key, int dfault) {
    return getCurrentConfig().getInt(key, dfault);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static long getLongParam(String key) throws InvalidParam {
    return getCurrentConfig().getLong(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static long getLongParam(String key, long dfault) {
    return getCurrentConfig().getLong(key, dfault);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static long getTimeIntervalParam(String key) throws InvalidParam {
    return getCurrentConfig().getTimeInterval(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static long getTimeIntervalParam(String key, long dfault) {
    return getCurrentConfig().getTimeInterval(key, dfault);
  }

  /** Static convenience method to get a <code>Configuration</code>
   * subtree from the current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static Configuration paramConfigTree(String key) {
    return getCurrentConfig().getConfigTree(key);
  }

  /** Static convenience method to get key iterator from the
   * current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static Iterator paramKeyIterator() {
    return getCurrentConfig().keyIterator();
  }

  /** Static convenience method to get a node iterator from the
   * current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static Iterator paramNodeIterator(String key) {
    return getCurrentConfig().nodeIterator(key);
  }

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
     * @param changedKeys  the set of keys whose value has changed.
     * @see Configuration#registerConfigurationCallback */
    public void configurationChanged(Configuration newConfig,
				     Configuration oldConfig,
				     Set changedKeys);
  }

  /** Exception thrown for errors in accessors. */
  public class InvalidParam extends Exception {
    public InvalidParam(String message) {
      super(message);
    }
  }
}
