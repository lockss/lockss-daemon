/*
 * $Id: ConfigParamDescr.java,v 1.34 2007-01-17 17:53:36 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import java.net.*;

import org.apache.commons.lang.math.NumberUtils;
import org.lockss.app.LockssApp;
import org.lockss.util.*;
import java.util.*;

/**
 * Descriptor for a configuration parameter, and instances of descriptors
 * for common parameters.  These have a sort order equal to the sort order
 * of their displayName.
 */
public class ConfigParamDescr implements Comparable, LockssSerializable {
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

  public static final String[] TYPE_STRINGS = {
      "String", "Integer", "URL", "Year", "Boolean", "Positive Integer",
      "Range", "Numeric Range", "Set"};

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

  public static final ConfigParamDescr PROTOCOL_VERSION =
    new ConfigParamDescr()
    .setDefinitional(false)
    .setDefaultOnly(true)
    .setKey("protocol_version")
    .setDisplayName("Polling Protocol Version")
    .setType(TYPE_POS_INT)
    .setDescription("The polling protocol version for the AU to use ('1' "
                    + "for V1 polling, or '3' for V3 polling)");

  public static final ConfigParamDescr[] DEFAULT_DESCR_ARRAY = {
      BASE_URL, VOLUME_NUMBER, VOLUME_NAME, YEAR, JOURNAL_ID, JOURNAL_ISSN,
      PUBLISHER_NAME, ISSUE_RANGE, NUM_ISSUE_RANGE, ISSUE_SET, OAI_REQUEST_URL,
      OAI_SPEC
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
   * Return the specified value type
   * @return the type int
   */
  public int getType() {
    return type;
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
      case TYPE_POS_INT: size = 10; break;
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

  public boolean isValidValueOfType(String val) {
    try {
      return getValueOfType(val) != null;
    }
    catch (InvalidFormatException ex) {
      return false;
    }
  }

  public Object getValueOfType(String val) throws InvalidFormatException {
    Object ret_val = null;
    switch (type) {
      case TYPE_INT:
        try {
          ret_val = new Integer(val);
        } catch (NumberFormatException nfe) {
          throw new InvalidFormatException("Invalid Int: " + val);
        }
        break;
      case TYPE_POS_INT:
          try {
            ret_val = new Integer(val);
            if(((Integer)ret_val).intValue() < 0) {
              throw new InvalidFormatException("Invalid Positive Int: " + val);
            }
          } catch (NumberFormatException nfe) {
            throw new InvalidFormatException("Invalid Positive Int: " + val);
          }
          break;

      case TYPE_STRING:
        if (!StringUtil.isNullString(val)) {
          ret_val = val;
        }
        else {
          throw new InvalidFormatException("Invalid String: " + val);
        }
        break;
      case TYPE_URL:
        try {
          ret_val = new URL(val);
        }
        catch (MalformedURLException ex) {
          throw new InvalidFormatException("Invalid URL: " + val, ex);
        }
        break;
      case TYPE_YEAR:
        if (val.length() == 4) {
          try {
            int i_val = Integer.parseInt(val);
            if (i_val > 0) {
              ret_val = new Integer(val);
            }
          }
          catch (NumberFormatException fe) {
          }
        }
        if(ret_val == null) {
          throw new InvalidFormatException("Invalid Year: " + val);
        }
        break;
      case TYPE_BOOLEAN:
        if(val.equalsIgnoreCase("true") ||
           val.equalsIgnoreCase("yes") ||
           val.equalsIgnoreCase("on") ||
           val.equalsIgnoreCase("1")) {
          ret_val = Boolean.TRUE;
        }
        else if(val.equalsIgnoreCase("false") ||
           val.equalsIgnoreCase("no") ||
           val.equalsIgnoreCase("off") ||
           val.equalsIgnoreCase("0")) {
          ret_val = Boolean.FALSE;
        }
        else
          throw new InvalidFormatException("Invalid Boolean: " + val);
        break;
      case TYPE_RANGE:
      { // case block
        ret_val = StringUtil.breakAt(val, '-', 2, true, true);
        String s_min = (String)((Vector)ret_val).firstElement();
        String s_max = (String)((Vector)ret_val).lastElement();
        if ( !(s_min.compareTo(s_max) <= 0) ) {
          throw new InvalidFormatException("Invalid Range: " + val);
        }
        break;
      } // end case block
      case TYPE_NUM_RANGE:
      { // case block
        ret_val = StringUtil.breakAt(val,'-',2,true, true);
        String s_min = (String)((Vector)ret_val).firstElement();
        String s_max = (String)((Vector)ret_val).lastElement();
        try {
          /*
           * Caution: org.apache.commons.lang.math.NumberUtils.createLong(String)
           * (which returns Long) throws NumberFormatException, whereas
           * org.apache.commons.lang.math.NumberUtils.toLong(String)
           * (which returns long) returns 0L when parsing fails.
           */
          Long l_min = NumberUtils.createLong(s_min);
          Long l_max = NumberUtils.createLong(s_max);
          if (l_min.compareTo(l_max) <= 0) {
            ((Vector)ret_val).setElementAt(l_min, 0);
            ((Vector)ret_val).setElementAt(l_max, 1);
            break;
          }
        }
        catch (NumberFormatException ex1) {
          if (s_min.compareTo(s_max) <= 0) {
            break;
          }
        }
        throw new InvalidFormatException("Invalid Numeric Range: " + val);
      } // end case block
      case TYPE_SET:
        ret_val = StringUtil.breakAt(val,',', 50, true, true);
        break;
      default:
        throw new InvalidFormatException("Unknown type: " + type);
    }

    return ret_val;
  }

  public int compareTo(Object o) {
    ConfigParamDescr od = (ConfigParamDescr)o;
    return getDisplayName().compareTo(od.getDisplayName());
  }

  /** Returns a short string suitable for error messages.  Includes the key
   * and the display name if present */
  public String shortString() {
    StringBuffer sb = new StringBuffer(40);
    sb.append(getDisplayName());
    if (!key.equals(displayName)) {
      sb.append("(");
      sb.append(key);
      sb.append(")");
    }
    return sb.toString();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(40);
    sb.append("[CPD: key: ");
    sb.append(getKey());
    sb.append("]");
    return sb.toString();
  }

  public boolean equals(Object o) {
    if (! (o instanceof ConfigParamDescr)) {
      return false;
    }
    ConfigParamDescr opd = (ConfigParamDescr)o;
    return type == opd.getType() && getSize() == opd.getSize() &&
      key.equals(opd.getKey());
  }

  public int hashCode() {
    int hash = 0x46600555;
    hash += type;
    hash += getSize();
    hash += key.hashCode();
    return hash;
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
  protected static Map uniqueInstances;

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
    return uniqueInstance(this);
  }

  /**
   * <p>Implements {@link #postUnmarshalResolve}.</p>
   * @param descr A candidate descriptor.
   * @return A canonicalized descriptor (see caveat in
   * {@link #postUnmarshalResolve})
   * @see #postUnmarshalResolve
   */
  protected static synchronized Object uniqueInstance(ConfigParamDescr descr) {
    /* Access to the map is protected by the synchronization */
    // Lazy instantiation
    if (uniqueInstances == null) {
      uniqueInstances = new HashMap();
      for (int ix = 0 ; ix < DEFAULT_DESCR_ARRAY.length ; ++ix) {
        uniqueInstances.put(DEFAULT_DESCR_ARRAY[ix], DEFAULT_DESCR_ARRAY[ix]);
      }
    }

    Object ret = uniqueInstances.get(descr);
    if (ret == null) {
      uniqueInstances.put(descr, descr);
      return descr;
    }
    else {
      return ret;
    }
  }

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
    StringBuffer buffer = new StringBuffer(100);
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
