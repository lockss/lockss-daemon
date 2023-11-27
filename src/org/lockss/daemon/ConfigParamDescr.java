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
DERIVED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.daemon;

import java.util.*;
import org.apache.commons.lang3.builder.*;

import org.lockss.app.LockssApp;
import org.lockss.util.*;

/**
 * Descriptor for a configuration parameter, and instances of descriptors
 * for common parameters.  These have a sort order equal to the sort order
 * of their displayName.
 */
public class ConfigParamDescr implements Comparable, LockssSerializable {

  private static final Logger log = Logger.getLogger(ConfigParamDescr.class);

  /** Value is any string */
  public static final int TYPE_STRING = 1;
  /** Value is an integer */
  public static final int TYPE_INT = 2;
  /** Value is a URL */
  public static final int TYPE_URL = 3;
  /** Value is a 4 digit year */
  public static final int TYPE_YEAR = 4;
  /** Value is a true or false */
  public static final int TYPE_BOOLEAN = 5;
  /** Value is a positive integer */
  public static final int TYPE_POS_INT = 6;
  /** Value is a range string */
  public static final int TYPE_RANGE = 7;
  /** Value is a numeric range string */
  public static final int TYPE_NUM_RANGE = 8;
  /** Value is a set string */
  public static final int TYPE_SET = 9;
  /** Value is a user:passwd pair (colon separated) */
  public static final int TYPE_USER_PASSWD = 10;
  /** Value is a long */
  public static final int TYPE_LONG = 11;
  /** Value is a time interval string */
  public static final int TYPE_TIME_INTERVAL = 12;

  public static final int MAX_TYPE = 12;

  /** Largest set allowed by TYPE_SET */
  public static final int MAX_SET_SIZE = 10000;

  public static final String[] TYPE_STRINGS = {
      "String", "Integer", "URL", "Year", "Boolean", "Positive Integer",
      "Range", "Numeric Range", "Set", "User:Passwd String", "Long"};

  static Map<Integer,String> SAMPLE_VALUES = new HashMap<Integer,String>();
  static {
    SAMPLE_VALUES.put(TYPE_STRING, "SampleString");
    SAMPLE_VALUES.put(TYPE_INT, "-42");
    SAMPLE_VALUES.put(TYPE_URL, "http://example.com/path/file.ext");
    SAMPLE_VALUES.put(TYPE_YEAR, "2038");
    SAMPLE_VALUES.put(TYPE_BOOLEAN, "true");
    SAMPLE_VALUES.put(TYPE_POS_INT, "42");
    SAMPLE_VALUES.put(TYPE_RANGE, "abc-def");
    SAMPLE_VALUES.put(TYPE_NUM_RANGE, "52-63");
    SAMPLE_VALUES.put(TYPE_SET, "winter,spring,summer,fall");
    SAMPLE_VALUES.put(TYPE_USER_PASSWD, "username:passwd");
    SAMPLE_VALUES.put(TYPE_LONG, "1099511627776");
    SAMPLE_VALUES.put(TYPE_TIME_INTERVAL, "10d");
  };

  /** An element of a set that's a brace-enclosed integer range will be
   * expanded into multiple set elements.  */
  public static final String SET_RANGE_OPEN = "{";
  public static final String SET_RANGE_CLOSE = "}";

  public static final ConfigParamDescr VOLUME_NUMBER =
    new ConfigParamDescr()
    .setKey("volume")
    .setDisplayName("Volume No.")
    .setType(TYPE_POS_INT)
    .setSize(8);

  public static final ConfigParamDescr VOLUME_NAME =
    new ConfigParamDescr()
    .setKey("volume_name")
    .setDisplayName("Volume Name")
    .setType(TYPE_STRING)
    .setSize(20);

  public static final ConfigParamDescr ISSUE_RANGE =
    new ConfigParamDescr()
    .setKey("issue_range")
    .setDisplayName("Issue Range")
    .setType(TYPE_RANGE)
    .setSize(20)
    .setDescription("A Range of issues in the form: aaa-zzz");

  public static final ConfigParamDescr NUM_ISSUE_RANGE =
    new ConfigParamDescr()
    .setKey("num_issue_range")
    .setDisplayName("Numeric Issue Range")
    .setType(TYPE_NUM_RANGE)
    .setSize(20)
    .setDescription("A Range of issues in the form: min-max");

  public static final ConfigParamDescr ISSUE_SET =
    new ConfigParamDescr()
    .setKey("issue_set")
    .setDisplayName("Issue Set")
    .setType(TYPE_SET)
    .setSize(20)
    .setDescription("A comma delimited list of issues. (eg issue1, issue2)");

  public static final ConfigParamDescr YEAR =
    new ConfigParamDescr()
    .setKey("year")
    .setDisplayName("Year")
    .setType(TYPE_YEAR)
    .setSize(4)
    .setDescription("Four digit year (e.g., 2004)");

  public static final ConfigParamDescr BASE_URL =
    new ConfigParamDescr()
    .setKey("base_url")
    .setDisplayName("Base URL")
    .setType(TYPE_URL)
    .setSize(40)
    .setDescription("Usually of the form http://<journal-name>.com/");

  public static final ConfigParamDescr BASE_URL2 =
    new ConfigParamDescr()
    .setKey("base_url2")
    .setDisplayName("Second Base URL")
    .setType(TYPE_URL)
    .setSize(40)
    .setDescription("Use if AU spans two hosts");

  public static final ConfigParamDescr JOURNAL_DIR =
    new ConfigParamDescr()
    .setKey("journal_dir")
    .setDisplayName("Journal Directory")
    .setType(TYPE_STRING)
    .setSize(40)
    .setDescription("Directory name for journal content (i.e. 'american_imago').");

  public static final ConfigParamDescr JOURNAL_ABBR =
    new ConfigParamDescr()
    .setKey("journal_abbr")
    .setDisplayName("Journal Abbreviation")
    .setType(TYPE_STRING)
    .setSize(10)
    .setDescription("Abbreviation for journal (often used as part of file names).");

  public static final ConfigParamDescr JOURNAL_ID =
    new ConfigParamDescr()
    .setKey("journal_id")
    .setDisplayName("Journal Identifier")
    .setType(TYPE_STRING)
    .setSize(40)
    .setDescription("Identifier for journal (often used as part of file names)");

  public static final ConfigParamDescr JOURNAL_ISSN =
    new ConfigParamDescr()
    .setKey("journal_issn")
    .setDisplayName("Journal ISSN")
    .setType(TYPE_STRING)
    .setSize(20)
    .setDescription("International Standard Serial Number.");

  public static final ConfigParamDescr PUBLISHER_NAME =
    new ConfigParamDescr()
    .setKey("publisher_name")
    .setDisplayName("Publisher Name")
    .setType(TYPE_STRING)
    .setSize(40)
    .setDescription("Publisher Name for Archival Unit");

  public static final ConfigParamDescr OAI_REQUEST_URL =
    new ConfigParamDescr()
    .setKey("oai_request_url")
    .setDisplayName("OAI Request URL")
    .setType(TYPE_URL)
    .setSize(40)
    .setDescription("Usually of the form http://<journal-name>.com/");

  public static final ConfigParamDescr OAI_SPEC =
    new ConfigParamDescr()
    .setKey("oai_spec")
    .setDisplayName("OAI Journal Spec")
    .setType(TYPE_STRING)
    .setSize(40)
    .setDescription("Spec for journal in the OAI crawl");

  public static final ConfigParamDescr USER_CREDENTIALS =
    new ConfigParamDescr()
    .setDefinitional(false)
    .setKey("user_pass")
    .setDisplayName("Username:Password")
    .setType(TYPE_USER_PASSWD)
    .setSize(30);

  public static final ConfigParamDescr COLLECTION =
    new ConfigParamDescr()
    .setKey("collection")
    .setDisplayName("Collection")
    .setType(TYPE_STRING)
    .setSize(20)
    .setDescription("Name of ArchiveIt collection");

  // Internal use
  public static final ConfigParamDescr AU_CLOSED =
    new ConfigParamDescr()
    .setDefinitional(false)
    .setDefaultOnly(true)
    .setKey("au_closed")
    .setDisplayName("AU Closed")
    .setType(TYPE_BOOLEAN)
    .setDescription("If true, AU is complete, no more content will be added");

  public static final ConfigParamDescr PUB_DOWN =
    new ConfigParamDescr()
    .setDefinitional(false)
    .setDefaultOnly(true)
    .setKey("pub_down")
    .setDisplayName("Pub Down")
    .setType(TYPE_BOOLEAN)
    .setDescription("If true, AU is no longer available from the publisher");

  // See ProxyHandler
  public static final ConfigParamDescr PUB_NEVER =
    new ConfigParamDescr()
    .setDefinitional(false)
    .setDefaultOnly(true)
    .setKey("pub_never")
    .setDisplayName("Pub Never")
    .setType(TYPE_BOOLEAN)
    .setDescription("If true, don't try to access any content from publisher");

  public static final ConfigParamDescr PROTOCOL_VERSION =
    new ConfigParamDescr()
    .setDefinitional(false)
    .setDefaultOnly(true)
    .setKey("protocol_version")
    .setDisplayName("Polling Protocol Version")
    .setType(TYPE_POS_INT)
    .setDescription("The polling protocol version for the AU to use ('1' "
                    + "for V1 polling, or '3' for V3 polling)");

  public static final ConfigParamDescr CRAWL_PROXY =
    new ConfigParamDescr()
    .setDefinitional(false)
    .setDefaultOnly(true)
    .setKey("crawl_proxy")
    .setDisplayName("Crawl Proxy")
    .setType(TYPE_STRING)
    .setSize(40)
    .setDescription("If set to host:port, crawls of this AU will be proxied." +
		    " If set to DIRECT, crawls will not be proxied," +
		    " even if a global crawl proxy has been set.");

  public static final ConfigParamDescr CRAWL_INTERVAL =
    new ConfigParamDescr()
    .setDefinitional(false)
    .setDefaultOnly(true)
    .setKey("nc_interval")
    .setDisplayName("Crawl Interval")
    .setType(TYPE_TIME_INTERVAL)
    .setSize(10)
    .setDescription("The interval at which the AU should crawl "
		    + "the publisher site.");

  public static final ConfigParamDescr CRAWL_TEST_SUBSTANCE_THRESHOLD =
    new ConfigParamDescr()
    .setDefinitional(false)
    .setDefaultOnly(false)
    .setKey("crawl_test_substance_threshold")
    .setDisplayName("Crawl Test Substance Threshold")
    .setType(TYPE_STRING)
    .setSize(20)
    .setDescription("Minimum number of substance URL necessary for "
		    + "successful abbreviated crawl test.");

  public static final ConfigParamDescr[] DEFAULT_DESCR_ARRAY = {
      BASE_URL, VOLUME_NUMBER, VOLUME_NAME, YEAR, JOURNAL_ID, JOURNAL_ISSN,
      PUBLISHER_NAME, ISSUE_RANGE, NUM_ISSUE_RANGE, ISSUE_SET, OAI_REQUEST_URL,
      OAI_SPEC, BASE_URL2, USER_CREDENTIALS, COLLECTION, CRAWL_INTERVAL,
      CRAWL_TEST_SUBSTANCE_THRESHOLD,
  };

  private String key;			// param (prop) key
  private String displayName;		// human readable name
  private String description;		// explanatory test
  private int type = TYPE_STRING;
  private int size = -1;		// size of input field

  // A parameter is definitional if its value is integral to the identity
  // of the AU.  (I.e., if changing it results in a different AU.)
  private boolean definitional = true;

  // default-only parameters in a TitleConfig are not copied to the AU
  // config; they are used only to provide a default value if the value is
  // not explicitly set in the AU config
  private boolean defaultOnly = false;

  // Describes a derived value, such as xxx_host and xxx_path for TYPE_URL
  // These are created on the fly when needed and should never appear in
  // plugins.  No need to save the flag as it would always be false.
  private transient boolean derived = false;


  public ConfigParamDescr() {
  }

  public ConfigParamDescr(String key) {
    setKey(key);
  }

  /**
   * Return the parameter key
   * @return the key String
   */
  public String getKey() {
    return key;
  }

  /**
   * Set the parameter key
   * @param key the new key
   * @return this
   */
  public ConfigParamDescr setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * Return the display name, or the key if no display name set
   * @return the display name String
   */
  public String getDisplayName() {
    return displayName != null ? displayName : getKey();
  }

  /**
   * Set the parameter display name
   * @param displayName the new display name
   * @return this
   */
  public ConfigParamDescr setDisplayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Return the parameter description
   * @return the description String
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the parameter description
   * @param description the new description
   * @return this
   */
  public ConfigParamDescr setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Return the int representing the value type 
   * @return the type int
   */
  public int getType() {
    return type;
  }

  /**
   * Return the AuParamType representing the value type 
   * @return the type
   */
  public AuParamType getTypeEnum() {
    return AuParamType.fromTypeInt(type);
  }

  /**
   * Set the expected value type.  If {@link #setSize(int)} has not been
   * called, and the type is one for which there is a reasonable default
   * size, this will also set the size to the reasonable default.
   * @param type the new type
   * @return this
   */
  public ConfigParamDescr setType(AuParamType type) {
    return setType(type.typeInt());
  }

  /**
   * Set the expected value type.  If {@link #setSize(int)} has not been
   * called, and the type is one for which there is a reasonable default
   * size, this will also set the size to the reasonable default.
   * @param type the new type
   * @return this
   */
  public ConfigParamDescr setType(int type) {
    this.type = type;
    // if no size has been set, set a reasonable default for some types
    if (size == -1) {
      switch (type) {
      case TYPE_YEAR: size = 4; break;
      case TYPE_BOOLEAN: size = 4; break;
      case TYPE_INT:
      case TYPE_LONG:
      case TYPE_POS_INT: size = 10; break;
      case TYPE_TIME_INTERVAL: size = 10; break;
      default:
      }
    }
    return this;
  }

  /**
   * Return the suggested input field size, or 0 if no suggestion
   * @return the size int
   */
  public int getSize() {
    return (size != -1) ? size : 0;
  }

  /**
   * Set the suggested input field size
   * @param size the new size
   * @return this
   */
  public ConfigParamDescr setSize(int size) {
    this.size = size;
    return this;
  }

  /**
   * Set the "definitional" flag.
   * @param isDefinitional the new value
   * @return this
   */
  public ConfigParamDescr setDefinitional(boolean isDefinitional) {
    definitional = isDefinitional;
    return this;
  }

  /** A parameter is definitional if its value is integral to the identity
   * of the AU.  (I.e., if changing it results in a different AU.)
   * @return true if the parameter is definitional
   */
  public boolean isDefinitional() {
    return definitional;
  }

  /**
   * Set the "defaultOnly" flag.
   * @param isDefaultOnly the new value
   * @return this
   */
  public ConfigParamDescr setDefaultOnly(boolean isDefaultOnly) {
    defaultOnly = isDefaultOnly;
    return this;
  }

  /** Default-only parameters in a TitleConfig are not copied to the AU
   * config; they are used only to provide a default value if the value is
   * not explicitly set in the AU config.
   * @return true if the parameter is defaultOnly
   */
  public boolean isDefaultOnly() {
    return defaultOnly;
  }

  /**
   * Set the "derived" flag.
   * @param isDerived the new value
   * @return this
   */
  public ConfigParamDescr setDerived(boolean isDerived) {
    derived = isDerived;
    return this;
  }

  /** Derived values are computed from explicitly set values, such as
   * base_url_host and base_url_path.  They should never be set directly.
   * @return true if the parameter is derived
   */
  public boolean isDerived() {
    return derived;
  }

  public ConfigParamDescr getDerivedDescr(String derivedKey) {
    ConfigParamDescr res = new ConfigParamDescr()
      .setKey(derivedKey)
      .setDerived(true)
      .setDefinitional(false)
      .setDefaultOnly(false)
      .setDisplayName(derivedKey + " (derived from " + getDisplayName() + ")")
      .setSize(getSize())
      .setType(getType());
    return res;
  }


  public boolean isValidValueOfType(String val) {
    try {
      return getValueOfType(val) != null;
    }
    catch (InvalidFormatException ex) {
      return false;
    }
  }

  public Object getValueOfType(String val)
      throws AuParamType.InvalidFormatException {
    return getTypeEnum().parse(val);
  }

  /** Return a legal value for the parameter.  Useful for generic plugin
   * tests */
  public String getSampleValue() {
    String res = SAMPLE_VALUES.get(type);
    if (res == null) {
      res = "SampleValue";
    }
    return res;
  }

  public int compareTo(Object o) {
    ConfigParamDescr od = (ConfigParamDescr)o;
    return getDisplayName().compareTo(od.getDisplayName());
  }

  /** Returns a short string suitable for error messages.  Includes the key
   * and the display name if present */
  public String shortString() {
    StringBuilder sb = new StringBuilder(40);
    sb.append(getDisplayName());
    if (!key.equals(displayName)) {
      sb.append("(");
      sb.append(key);
      sb.append(")");
    }
    return sb.toString();
  }

  public String toString() {
    return toDetailedString();
  }

  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (obj == this) { return true; }
    if (obj.getClass() != getClass()) {
      return false;
    }
    ConfigParamDescr other = (ConfigParamDescr)obj;
    return new EqualsBuilder()
      .append(getKey(), other.getKey())
      .append(getType(), other.getType())
      .append(getSize(), other.getSize())
      .append(isDerived(), other.isDerived())
      .append(isDefinitional(), other.isDefinitional())
      .append(isDefaultOnly(), other.isDefaultOnly())
      .isEquals();
  }

  public int hashCode() {
    return new HashCodeBuilder(23, 43)
      .append(getKey())
      .append(getType())
      .append(getSize())
      .append(isDerived())
      .append(isDefinitional())
      .append(isDefaultOnly())
      .toHashCode();
  }

  static String PREFIX_RESERVED =
    org.lockss.plugin.PluginManager.AU_PARAM_RESERVED + ".";

  /** Return true if the key is a reserved parameter name (<i>ie</i>,
   * starts with <code>reserved.</code>) */
  public static boolean isReservedParam(String key) {
    return key.startsWith(PREFIX_RESERVED);
  }

  /**
   * <p>A map of canonicalized instances (see caveat in
   * {@link #postUnmarshalResolve}).</p>
   * @see #postUnmarshalResolve
   */
  protected static Map<ConfigParamDescr,ConfigParamDescr> uniqueInstances;

  /**
   * <p>A post-deserialization resolution method for serializers that
   * support it.</p>
   * <p>This class ({@link ConfigParamDescr}) does not use the context
   * argument.</p>
   * <p><em>Caveat.</em> The various instances of this class are
   * unique in principle, but the class itself allows for mutable
   * instances. The instance cache implemented as part of this
   * post-deserialization mechanism may hold on to instances for
   * ever if their canonical representation (as evaluated by
   * {@link #hashCode} and {@link #equals}) changes. The loss is
   * acceptable for reasonable use cases of the pre-defined
   * instances of this class.</p>
   * @param context A context instance.
   * @return A canonicalized object (see caveat above).
   */
  protected Object postUnmarshalResolve(LockssApp context) {
    return intern(this);
  }

  /**
   * Intern the ConfigParamDescr and return a unique instance
   * @param descr A candidate descriptor.
   * @return A canonicalized descriptor (see caveat in
   * {@link #postUnmarshalResolve})
   * @see #postUnmarshalResolve
   */
  public static synchronized ConfigParamDescr intern(ConfigParamDescr descr) {

    /* Access to the map is protected by the synchronization */
    // Lazy instantiation
    if (uniqueInstances == null) {
      uniqueInstances = new HashMap();
      for (int ix = 0 ; ix < DEFAULT_DESCR_ARRAY.length ; ++ix) {
        uniqueInstances.put(DEFAULT_DESCR_ARRAY[ix], DEFAULT_DESCR_ARRAY[ix]);
      }
    }

    ConfigParamDescr ret = uniqueInstances.putIfAbsent(descr, descr);
    if (ret == null) {
      return descr;
    }
    else {
      return ret;
    }
  }

  /**
   * @deprecated After 1.67 is released, plugins should be converted to use
   * AuParamType.InvalidFormatException instead
   */
  public static class InvalidFormatException extends Exception {
    private Throwable nestedException;

    public InvalidFormatException(String msg) {
      super(msg);
    }

    public InvalidFormatException(String msg, Throwable e) {
      super(msg + (e.getMessage() == null ? "" : (": " + e.getMessage())));
      this.nestedException = e;
    }

    public Throwable getNestedException() {
      return nestedException;
    }
  }

  public String toDetailedString() {
    StringBuilder buffer = new StringBuilder(100);
    buffer.append("[");
    buffer.append(getClass().getName());
    buffer.append(";key=");
    buffer.append(getKey());
    buffer.append(";type=");
    buffer.append(getType());
    buffer.append(";size=");
    buffer.append(getSize());
    buffer.append(";isDefinitional=");
    buffer.append(isDefinitional());
    buffer.append(";displayName=");
    buffer.append(getDisplayName());
    buffer.append("]");
    return buffer.toString();
  }

}
