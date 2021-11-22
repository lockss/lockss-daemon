/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.alert;

import java.util.*;
import java.net.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.*;

/** An Alert is an event or condition that should be logged, or that a user
 * may be interested in.
 */
public class Alert {
  private static final Logger log = Logger.getLogger(Alert.class);

  /** List of comma-separated pairs: <i>AlertName</i>,<i>GroupName</i> ;
   * alerts with same group are considered similar for purposes of grouping
   * into a single action (e.g., email).  If <i>GroupName</i> startts with
   * <tt>AU:</tt> then the alerts are similar only if they have the same
   * AU.  If <i>GroupName</i> startts with <tt>PLUGIN:</tt> then the alerts
   * are similar only if they have AUs with the same plugin */
  public static final String PARAM_SPECIAL_GROUPS =
    AlertManager.PREFIX + "specialGroups";
  public static final List DEFAULT_SPECIAL_GROUPS =
    ListUtil.list("CrawlFinished,CrawlEnd", "CrawlFailed,CrawlEnd");


  // Attribute names
  public static final String ATTR_NAME = "name";
  public static final String ATTR_DATE = "date";
  public static final String ATTR_GENERIC_TEXT = "generic_text";
  public static final String ATTR_TEXT = "text";
  public static final String ATTR_CACHE = "cache";
  public static final String ATTR_URL = "url";
  public static final String ATTR_AUID = "auid";
  public static final String ATTR_AU_NAME = "au_name";
  public static final String ATTR_AU_TITLE = "au_title";
  public static final String ATTR_PLUGIN_NAME = "plugin_name";
  public static final String ATTR_IS_CONTENT = "is_content";
  public static final String ATTR_IS_CACHE = "is_cache";
  /** If true, prevents delayed notification */
  public static final String ATTR_IS_TIME_CRITICAL = "is_time_critical";
  public static final String ATTR_SEVERITY = "severity";
  public static final String ATTR_RPT_ACTION = "foo";
  public static final String ATTR_EMAIL_TO = "email_to";
  public static final String ATTR_AUDITABLE_EVENT = "auditable_event";

  // Severities
  /** Conditions that require immediate attention from a human. */
  public static final int SEVERITY_CRITICAL = 50;
  /** Conditions that indicate that the system may not operate correctly,
   * but won't damage anything. */
  public static final int SEVERITY_ERROR = 40;
  /** Anomalous conditions that don't prevent the system from running
   * correctly, but are probably of interest to a human.  E.g., unexpecteed
   * content changes. */
  public static final int SEVERITY_WARNING = 30;
  /** Conditions that reflect normal operation, but may be of interest to
   * someone who wants to follow what the cache is doing. */
  public static final int SEVERITY_INFO = 20;
  /** Conditions that trace cache activity in gory detail */
  public static final int SEVERITY_TRACE = 10;


  // Predefined Alert templates
  public static final Alert DAEMON_STARTED =
    new Alert("DaemonStarted").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO).
    setAttribute(ATTR_IS_TIME_CRITICAL, true);

  public static final Alert PLUGIN_NOT_LOADED =
    new Alert("PluginNotLoaded").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  /** An attempt was made to load/create an AU whose plugin is not
   * available */
  public static final Alert PLUGIN_MISSING =
    new Alert("PluginMissing").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert PLUGIN_RELOADED =
    new Alert("PluginReloaded").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO);

  public static final Alert PLUGIN_JAR_NOT_VALIDATED =
    new Alert("PluginJarNotValidated").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert AU_CREATED =
    cAlert("AuCreated").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO);

  public static final Alert AU_DELETED =
    cAlert("AuDeleted").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO);

  public static final Alert PERMISSION_PAGE_FETCH_ERROR =
    cAlert("PermissionPageFetchError").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert NO_CRAWL_PERMISSION =
    cAlert("NoCrawlPermission").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert CRAWL_FAILED =
    cAlert("CrawlFailed").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert CRAWL_FINISHED =
    cAlert("CrawlFinished").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO);

  public static final Alert CRAWL_EXCLUDED_URL =
    cAlert("CrawlExcludedURL").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert CRAWL_NO_SUBSTANCE =
    cAlert("CrawlCollectedNoSubstantialContent").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert CRAWL_UNLINKED_SUBSTANCE =
    cAlert("NoSubstantialContentLinked").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert FILE_VERIFICATION =
    cAlert("FileVerification").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert NEW_FILE_VERSION =
    cAlert("NewFileVersion").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO);

  public static final Alert NEW_CONTENT =
    cAlert("NewContent").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO);

  public static final Alert NO_NEW_CONTENT =
    cAlert("NoNewContent").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert VOLUME_CLOSED =
    cAlert("VolumeClosed").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO);

  public static final Alert PUBLISHER_UNREACHABLE =
    cAlert("PublisherUnreachable").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert PUBLISHER_CONTENT_CHANGED =
    cAlert("PublisherContentChanged").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO);

  public static final Alert DAMAGE_DETECTED =
    cAlert("DamageDetected").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert PERSISTENT_DISAGREEMENT =
    cAlert("PersistentDisagreement").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);
  // Synonym
  public static final Alert PERSISTENT_DAMAGE = PERSISTENT_DISAGREEMENT;

  public static final Alert REPAIR_COMPLETE =
    cAlert("RepairComplete").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert PERSISTENT_NO_QUORUM =
    cAlert("PersistentNoQuorum").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert INCONCLUSIVE_POLL =
    cAlert("InconclusivePoll").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING);

  public static final Alert POLL_FINISHED =
    cAlert("PollFinished").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO);

  public static final Alert CACHE_DOWN =
    new Alert("BoxDown").
    setAttribute(ATTR_SEVERITY, SEVERITY_ERROR);

  public static final Alert CACHE_UP =
    new Alert("BoxUp").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO);

  public static final Alert DISK_SPACE_LOW =
    new Alert("DiskSpaceLow").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING).
    setAttribute(ATTR_IS_TIME_CRITICAL, true);

  public static final Alert DISK_SPACE_FULL =
    new Alert("DiskSpaceFull").
    setAttribute(ATTR_SEVERITY, SEVERITY_CRITICAL).
    setAttribute(ATTR_IS_TIME_CRITICAL, true);

  public static final Alert INTERNAL_ERROR =
    new Alert("InternalError").
    setAttribute(ATTR_SEVERITY, SEVERITY_ERROR).
    setAttribute(ATTR_IS_TIME_CRITICAL, true);

  public static final Alert CONFIGURATION_ERROR =
    new Alert("ConfigurationError").
    setAttribute(ATTR_SEVERITY, SEVERITY_ERROR).
    setAttribute(ATTR_IS_TIME_CRITICAL, true);

  public static final Alert PASSWORD_REMINDER =
    new Alert("PasswordReminder").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING).
    setAttribute(ATTR_IS_TIME_CRITICAL, true);

  public static final Alert ACCOUNT_DISABLED =
    new Alert("AccountDisabled").
    setAttribute(ATTR_SEVERITY, SEVERITY_WARNING).
    setAttribute(ATTR_IS_TIME_CRITICAL, true);

  public static final Alert AUDITABLE_EVENT =
    new Alert("AuditableEvent").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO).
    setAttribute(ATTR_IS_TIME_CRITICAL, true);

  public static final Alert CONTENT_ACCESS =
    new Alert("ContentAccess").
    setAttribute(ATTR_SEVERITY, SEVERITY_INFO).
    setAttribute(ATTR_IS_TIME_CRITICAL, false);

  private Map attributes;
//   private Throwable throwable;

  // Factories
  /**
   * Create an AU-specific alert.
   * @param prototype the prototype Alert
   * @param au the au
   * @return a new Alert instance
   */
  public static Alert auAlert(Alert prototype, ArchivalUnit au) {
    Alert res = new Alert(prototype);
    res.setAttribute(ATTR_IS_CONTENT, true);
    if (au != null) {
      res.setAttribute(ATTR_AUID, au.getAuId());
      res.setAttribute(ATTR_AU_NAME, au.getName());
      res.setAttribute(ATTR_PLUGIN_NAME, au.getPlugin().getPluginId());
//     res.setAttribute(ATTR_AU_TITLE, au.getJournalTitle());
    }
    return res;
  }

  /**
   * Create a non-AU-specific alert.
   * @param prototype the prototype Alert
   * @return a new Alert instance
   */
  public static Alert cacheAlert(Alert prototype) {
    Alert res = new Alert(prototype);
    res.setAttribute(ATTR_IS_CACHE, true);
    return res;
  }

  /** Create a new alert */
  public Alert(String name) {
    this.attributes = new HashMap();
    init();
    setAttribute(ATTR_NAME, name);
  }

  /** Create a new alert initialized with a copy of the given attributes */
  public Alert(Map attributes) {
    this.attributes = (Map)((HashMap)attributes).clone();
    init();
  }

  /** Create a new alert initialized with a copy of the given attributes */
  public Alert(String name, Map attributes) {
    this(attributes);
    setAttribute(ATTR_NAME, name);
  }

  /** Create a new alert, copying all attributes from the prototype */
  public Alert(Alert prototype)  {
    this(prototype.getAttributes());
  }

  /** Create a new alert, copying all attributes from the prototype, but
   * overriding the name */
  public Alert(String name, Alert prototype)  {
    this(name, prototype.getAttributes());
  }

  private void init() {
    setAttribute(ATTR_DATE, TimeBase.nowMs());
    String host = PlatformUtil.getLocalHostname();
    if (host != null) {
      setAttribute(ATTR_CACHE, host);
    }
  }

  /** Return the attributes map */
  public Map getAttributes() {
    return attributes;
  }

  /** Set a attribute value */
  public Alert setAttribute(String attr, Object val) {
    attributes.put(attr, val);
    return this;
  }

  /** Set a attribute to an int value */
  public Alert setAttribute(String attr, int val) {
    attributes.put(attr, new Integer(val));
    return this;
  }

  /** Set a attribute to a long value */
  public Alert setAttribute(String attr, long val) {
    attributes.put(attr, new Long(val));
    return this;
  }

  /** Set a attribute to a boolean value */
  public Alert setAttribute(String attr, boolean val) {
    if (!val) {
      attributes.remove(attr);
    } else {
      attributes.put(attr, Boolean.TRUE);
    }
    return this;
  }

  /** Return true if the attribtue is present */
  public boolean hasAttribute(String attr) {
    return attributes.containsKey(attr);
  }

  /** Return the attribute value as an Object */
  public Object getAttribute(String attr) {
    return attributes.get(attr);
  }

  /** Return the attribute value as a String */
  public String getString(String attr) {
    return (String)attributes.get(attr);
  }

  /** Return the attribute value as an int */
  public int getInt(String attr) {
    Integer n = (Integer)attributes.get(attr);
    if (n != null) {
      return n.intValue();
    }
    throw new RuntimeException();
  }

  /** Return the attribute value as a long */
  public long getLong(String attr) {
    Long n = (Long)attributes.get(attr);
    if (n != null) {
      return n.longValue();
    }
    throw new RuntimeException();
  }

  /** Return the attribute value as a boolean */
  public boolean getBool(String attr) {
    try {
      Boolean n = (Boolean)attributes.get(attr);
      if (n != null) {
	return n.booleanValue();
      }
      return false;
    } catch (ClassCastException e) {
      return false;
    }
  }

  /** Return the value of the NAME attribute */
  public String getName() {
    return getString(ATTR_NAME);
  }

  /** Return the value of the DATE attribute */
  public Date getDate() {
    return new Date(getLong(ATTR_DATE));
  }

  GroupKey getGroupKey() {
    return new GroupKey(this);
  }

  private String specialGroup() {
    return specialGroups.get(getName());
  }

  /** Multiple similar alerts may be combined into a single report */
  public boolean isSimilarTo(Alert other) {
    String group = specialGroup();
    if (!StringUtil.isNullString(group) &&
	group.equals(other.specialGroup())) {
      if (StringUtil.startsWithIgnoreCase(group, "au:") &&
	  getBool(ATTR_IS_CONTENT)) {
	return StringUtil.equalStrings((String)getAttribute(ATTR_AUID),
				       (String)other.getAttribute(ATTR_AUID));
      }
      if (StringUtil.startsWithIgnoreCase(group, "plugin:") &&
	  getBool(ATTR_IS_CONTENT)) {
	return StringUtil.equalStrings((String)getAttribute(ATTR_PLUGIN_NAME),
				       (String)other.getAttribute(ATTR_PLUGIN_NAME));
      }
      return true;
    }
    return
      equalAttrs(ATTR_NAME, other) &&
      (getBool(ATTR_IS_CONTENT) ? equalAttrs(ATTR_AUID, other) : true);
  }

  public int similarityHash() {
    int hash = 'A' << 24 | 'l' << 16 | 'r' << 8 | 't';
    String group = specialGroup();
    if (!StringUtil.isNullString(group)) {
      if (StringUtil.startsWithIgnoreCase(group, "au:") &&
	  getBool(ATTR_IS_CONTENT)) {
	hash = (hash << 1) | getAttribute(ATTR_AUID).hashCode();
      }
      if (StringUtil.startsWithIgnoreCase(group, "plugin:") &&
	  getBool(ATTR_IS_CONTENT)) {
	hash = (hash << 1) | getAttribute(ATTR_PLUGIN_NAME).hashCode();
      }
      hash = (hash << 1) | group.hashCode();
    } else {
      hash = (hash << 1) | getName().hashCode();
      if (getBool(ATTR_IS_CONTENT)) {
	hash = (hash << 1) | getAttribute(ATTR_AUID).hashCode();
      } else {
	hash = (hash << 1) | 47;
      }
    }
    return hash;
  }

  private boolean equalAttrs(String key, Alert other) {
    Object v1 = getAttribute(key);
    Object v2 = other.getAttribute(key);
    return (v1 == null) ? (v2 == null) : v1.equals(v2);
  }

  public String toString() {
    return "[Alert: " + attributes + "]";
  }

  public boolean equals(Object obj) {
    if (obj instanceof Alert ) {
      Alert other = (Alert)obj;
      return getAttributes().equals(other.getAttributes());
    }
    return false;
  }

  public int hashCode() {
    return getAttributes().hashCode();
  }

  /** Return the name of the alert severity */
  public String getSeverityString() {
    try {
      int sev = getInt(ATTR_SEVERITY);
      if (sev <= SEVERITY_TRACE) return "trace";
      if (sev <= SEVERITY_INFO) return "info";
      if (sev <= SEVERITY_WARNING) return "warning";
      if (sev <= SEVERITY_ERROR) return "error";
      if (sev <= SEVERITY_CRITICAL) return "critical";
      return "unknown";
    } catch (RuntimeException e) {
      return "unknown";
    }
  }

  public String getMailSubject() {
    StringBuilder sb = new StringBuilder();
    sb.append("LOCKSS box ");
    sb.append(getSeverityString());
    sb.append(": ");
    sb.append(getName());
    return sb.toString();
  }

  public String getMailBody() {
    StringBuilder sb = new StringBuilder();
    sb.append("LOCKSS box '");
    sb.append(getAttribute(ATTR_CACHE));
    sb.append("' raised an alert at ");
    sb.append(getDate());
    sb.append("\n\n");
    appendVal(sb, "Name: ", getName());
    appendVal(sb, "Severity: ", getSeverityString());
    if (getAttribute(ATTR_AU_NAME) != null) {
      appendVal(sb, "AU: ", getAttribute(ATTR_AU_NAME));
      appendVal(sb, "AUID: ", getAttribute(ATTR_AUID));
    }
    if (hasAttribute(ATTR_TEXT)) {
      appendVal(sb, "Explanation: ", getAttribute(ATTR_TEXT));
    }
    return sb.toString();
  }

  private void appendVal(StringBuilder sb, String heading, Object val) {
    if (val == null) {
      return;
    }
    sb.append(heading);
    sb.append(val.toString());
    sb.append("\n");
  }

  private static Alert cAlert(String name) {
    return new Alert(name).setAttribute(ATTR_IS_CONTENT, true);
  }

  public static class GroupKey {
    Alert alert;

    GroupKey(Alert alert) {
      this.alert = alert;
    }

    public boolean equals(Object obj) {
      if (obj instanceof GroupKey ) {
	return alert.isSimilarTo(((GroupKey)obj).alert);
      }
      return false;
    }
    public int hashCode() {
      return alert.similarityHash();
    }


  }

  private static Map<String,String> specialGroups =
    installSpecialGroups(DEFAULT_SPECIAL_GROUPS);

  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
                               Configuration oldConfig,
                               Configuration.Differences diffs) {
    if (diffs.contains(PARAM_SPECIAL_GROUPS)) {
      installSpecialGroups(config.getList(PARAM_SPECIAL_GROUPS,
					  DEFAULT_SPECIAL_GROUPS));
    }
  }

  private static Map<String,String> installSpecialGroups(List<String> pairs)  {
    if (pairs == null) {
      log.debug2("Installing empty special groups map");
      specialGroups = Collections.emptyMap();
    } else {
      try {
	Map<String,String> map = new HashMap<String,String>();
	for (String pair : pairs) {
	  int pos = pair.indexOf(',');
	  if (pos < 0) {
	    throw new IllegalArgumentException("Marformed Alert_name,Group pair; no comma: "
						 + pair);
	  }
	  String alertName = pair.substring(0, pos);
	  String group = pair.substring(pos + 1);
	  if (!StringUtil.isNullString(alertName) &&
	      !StringUtil.isNullString(group)) {
	    map.put(alertName, group);
	  }
	}
	specialGroups = map;
	log.debug("Installing special groups map: " + specialGroups);
      } catch (IllegalArgumentException e) {
	log.error("Illegal special groups map, ignoring", e);
	log.error("Special groups map unchanged, still: " +
		     specialGroups);
      }
    }
    return specialGroups;
  }
  
}
