/*
 * $Id: AlertManagerImpl.java,v 1.19 2010-05-04 03:36:36 tlipkis Exp $
 *

 Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;

import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.util.*;

/**
 * <p>Matches alerts against configured filters and invokes the
 * actions associated with matching patterns; multiple groupable
 * alerts may be deferred and reported together.</p>
 */
public class AlertManagerImpl
    extends BaseLockssDaemonManager
    implements AlertManager, ConfigurableManager {

  /**
   * <p>A logger for use by instances of this class.</p>
   */
  protected static Logger log = Logger.getLogger("AlertMgr");

  /** List of names of alerts that should be ignored if raised */
  static final String PARAM_IGNORED_ALERTS = PREFIX + "ignoredAlerts";

  static final String DELAY_PREFIX = PREFIX + "notify.delay";

  static final String PARAM_DELAY_INITIAL = DELAY_PREFIX + "initial";
  static final long DEFAULT_DELAY_INITIAL = 5 * Constants.MINUTE;

  static final String PARAM_DELAY_INCR = DELAY_PREFIX + "incr";
  static final long DEFAULT_DELAY_INCR = 30 * Constants.MINUTE;

  static final String PARAM_DELAY_MAX = DELAY_PREFIX + "max";
  static final long DEFAULT_DELAY_MAX = 2 * Constants.HOUR;

  /** XML describing serialized AlertConfig */
  public static final String PARAM_CONFIG = PREFIX + "config";
  static final String DEFAULT_CONFIG = null;

  static final String PARAM_ALERT_ALL_EMAIL
    = PREFIX + "allEmail";

  public static final String CONFIG_FILE_ALERT_CONFIG = "alertconfig.xml";

  private ConfigManager configMgr;
  private AlertConfig alertConfig;
  private boolean alertsEnabled = DEFAULT_ALERTS_ENABLED;
  private Set ignoredAlerts;

  private long initialDelay = DEFAULT_DELAY_INITIAL;
  private long incrDelay = DEFAULT_DELAY_INCR;
  private long maxDelay = DEFAULT_DELAY_MAX;

  public void startService() {
    super.startService();
    configMgr = getDaemon().getConfigManager();
//  loadConfig();
  }

  void tmpConfig(String address) {
    if (StringUtil.isNullString(address)) {
      alertConfig = new AlertConfig();
    } else {
      AlertAction action = new AlertActionMail(address);
      AlertConfig conf =
        new AlertConfig(ListUtil.list(new AlertFilter(AlertPatterns.True(),
						      action)));
      alertConfig = conf;
    }
  }

  //   public synchronized void stopService() {
  //     super.stopService();
  //   }

  public synchronized void setConfig(Configuration config,
      Configuration prevConfig,
      Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      alertsEnabled = config.getBoolean(PARAM_ALERTS_ENABLED,
					DEFAULT_ALERTS_ENABLED);
      initialDelay = config.getTimeInterval(PARAM_DELAY_INITIAL,
					    DEFAULT_DELAY_INITIAL);
      incrDelay = config.getTimeInterval(PARAM_DELAY_INCR,
					 DEFAULT_DELAY_INCR);
      maxDelay = config.getTimeInterval(PARAM_DELAY_MAX,
					DEFAULT_DELAY_MAX);
      ignoredAlerts = SetUtil.theSet(config.getList(PARAM_IGNORED_ALERTS));

      if (changedKeys.contains(PARAM_ALERT_ALL_EMAIL)
	  || changedKeys.contains(PARAM_CONFIG)) {
	String allEmail = config.get(PARAM_ALERT_ALL_EMAIL);
	log.info("Installing allEmail config, to: " + allEmail);
	if (!StringUtil.isNullString(allEmail)) {
	  tmpConfig(allEmail);
	}
	Configuration cfg = config.getConfigTree(PARAM_CONFIG);
	if (cfg != null && !cfg.isEmpty()) {
	  log.info("Installing config: " + cfg);
	  loadConfig(cfg);
	}
      }
    }
  }

  void loadConfig(Configuration config) {
    AlertConfig theConfig = new AlertConfig();
    for (Iterator iter = config.nodeIterator(); iter.hasNext(); ) {
      String id = (String)iter.next();
      String xml = config.get(id);
      if (xml != null) {
	AlertConfig alertConfig = loadAlertConfig(xml);
	for (AlertFilter filt : alertConfig.getFilters()) {
	  theConfig.addFilter(filt);
	}
      }
    }
    alertConfig = theConfig;
    if (log.isDebug()) log.debug("Config: " + alertConfig);
  }

  public void loadConfig(String xml) {
    alertConfig = loadAlertConfig(xml);
    if (log.isDebug()) log.debug("Config: " + alertConfig);
  }

  public void loadConfig() {
    File file = configMgr.getCacheConfigFile(CONFIG_FILE_ALERT_CONFIG);
    alertConfig = loadAlertConfig(file);
    if (log.isDebug()) log.debug("Config: " + alertConfig);
  }

  public AlertConfig getConfig() {
    return alertConfig;
  }

  public void updateConfig(AlertConfig config) throws Exception {
    alertConfig = config;
    File file = configMgr.getCacheConfigFile(CONFIG_FILE_ALERT_CONFIG);
    storeAlertConfig(file, config);
  }

  /**
   * <p>Serializes an AlertConfig into a file, using a default
   * serializer.</p>
   * @param file        A destination file.
   * @param alertConfig An AlertConfig instance.
   * @throws Exception if any error condition arises.
   * @see #storeAlertConfig(File, AlertConfig, ObjectSerializer)
   */
  void storeAlertConfig(File file,
                        AlertConfig alertConfig)
      throws Exception {
    storeAlertConfig(file, alertConfig, makeObjectSerializer());
  }

  /**
   * <p>Serializes an AlertConfig object into a file, using the given
   * serializer.</p>
   * @param file        A destination file.
   * @param alertConfig An AlertConfig instance.
   * @param serializer  A serializer instance.
   * @throws Exception if any error condition arises.
   */
  void storeAlertConfig(File file,
                        AlertConfig alertConfig,
                        ObjectSerializer serializer)
      throws Exception {
    try {
      serializer.serialize(file, alertConfig);
    } catch (Exception e) {
      log.error("Could not store alert config", e);
      throw e;
    }
  }

  AlertConfig loadAlertConfig(String config) {
    ObjectSerializer deserializer = makeObjectSerializer();
    try {
      if (log.isDebug3()) log.debug3("Loading alert config: " + config);
      Reader rdr = new StringReader(config);
      AlertConfig ac = (AlertConfig)deserializer.deserialize(rdr);
      return ac;
    } catch (SerializationException se) {
      log.error(
          "Marshalling exception for alert config", se);
      // drop down to default value
    } catch (Exception e) {
      log.error("Could not load alert config", e);
      throw new RuntimeException(
          "Could not load alert config", e);
    }
    // Default value
    return new AlertConfig();
  }

  /**
   * <p>Load an AlertConfig object from a file, using a default
   * deserializer.</p>
   * @param file A source file.
   * @return An AlertConfig instance loaded from file (or a default
   *         value).
   * @see #loadAlertConfig(File, ObjectSerializer)
   */
  AlertConfig loadAlertConfig(File file) {
    return loadAlertConfig(file, makeObjectSerializer());
  }

  /**
   * <p>Load an AlertConfig object from a file, using the given
   * deserializer.</p>
   * @param file         A source file.
   * @param deserializer A deserializer instance.
   * @return An AlertConfig instance loaded from file (or a default
   *         value).
   */
  AlertConfig loadAlertConfig(File file,
                              ObjectSerializer deserializer) {
    try {
      log.debug3("Loading alert config");
      AlertConfig ac = (AlertConfig)deserializer.deserialize(file);
      return ac;
    } catch (SerializationException se) {
      log.error(
          "Marshalling exception for alert config", se);
      // drop down to default value
    } catch (Exception e) {
      log.error("Could not load alert config", e);
      throw new RuntimeException(
          "Could not load alert config", e);
    }

    // Default value
    return new AlertConfig();
  }

  private static ObjectSerializer makeObjectSerializer() {
    return new XStreamSerializer();
  }

  /**
   * <p>Convenience method to set the text of an alert and raise() it
   * @param alert the alert to raise
   * @param text text to be stored in text attribute of alert
   */
  public void raiseAlert(Alert alert, String text) {
    alert.setAttribute(Alert.ATTR_TEXT, text);
    raiseAlert(alert);
  }

  /**
   * <p>Raises an alert.</p>
   * @param alert the alert to raise
   */
  public void raiseAlert(Alert alert) {
    if (!alertsEnabled || alertConfig == null) {
      log.debug3("alerts disabled");
      return;
    }
    if (ignoredAlerts != null && ignoredAlerts.contains(alert.getName())) {
      if (log.isDebug3()) log.debug3("Raised but ignored: " + alert);
    } else {
      if (log.isDebug3()) log.debug3("Raised " + alert);
      try {
	Set actions = findMatchingActions(alert, alertConfig.getFilters());
	for (Iterator iter = actions.iterator(); iter.hasNext(); ) {
	  AlertAction action = (AlertAction)iter.next();
	  recordOrDefer(alert, action);
	}
      } catch (Exception e) {
	log.error("Filter or action threw", e);
      }
    }
  }

  /**
   * <p>Returns the actions whose pattern matches the alert.
   */
  Set findMatchingActions(Alert alert, List filters) {
    Set res = new HashSet();
    for (Iterator iter = filters.iterator(); iter.hasNext(); ) {
      AlertFilter filt = (AlertFilter)iter.next();
      if (filt.getPattern().isMatch(alert)) {
        res.add(filt.getAction());
      }
    }
    return res;
  }

  Map pending = new HashMap();

  // Atomically get or create and add the PendingActions for this action
  // and group of alerts
  PendingActions getPending(AlertAction action, Alert.GroupKey alertKey) {
    synchronized (pending) {
      Map actionMap = (Map)pending.get(action);
      if (actionMap == null) {
	actionMap = new HashMap();
	pending.put(action, actionMap);
      }
      PendingActions pend = (PendingActions)actionMap.get(alertKey);
      if (pend == null) {
	pend = new PendingActions(action);
	actionMap.put(alertKey, pend);
      }
      return pend;
    }
  }

  void recordOrDefer(Alert alert, AlertAction action) {
    if (!action.isGroupable()) {
      action.record(getDaemon(), alert);
      return;
    }
    PendingActions pend = getPending(action, alert.getGroupKey());
    pend.addAlert(alert);
  }

  /**
   * <p>Makes decisions about delaying notification of alerts in order
   * to report them in groups.</p>
   */
  class PendingActions {
    AlertAction action;
    List alerts;
    Deadline trigger;
    long latestTrigger;
    boolean isProcessed = false;

    PendingActions(AlertAction action) {
      this.action = action;
    }

    synchronized void addAlert(Alert alert) {
      if (alert.getBool(Alert.ATTR_IS_TIME_CRITICAL) ||
          action.getMaxPendTime() == 0) {
        action.record(getDaemon(), alert);
        return;
      }
      if (alerts == null || isProcessed) {
        if (log.isDebug3()) log.debug3("Recording first: " + alert);
        // record this one, start list for successive, start timer
        alerts = new ArrayList();
        alerts.add(alert);
        isProcessed = false;
        latestTrigger = now() + min(maxDelay, action.getMaxPendTime());
        trigger = Deadline.at(min(now() + initialDelay, latestTrigger));
        scheduleTimer();
      } else {
        if (log.isDebug3()) log.debug3("Adding: " + alert);
        alerts.add(alert);
        trigger.expireAt(min(now() + incrDelay, latestTrigger));
        if (log.isDebug3()) log.debug3(" and resetting timer to " + trigger);
        if (isTime()) {
          execute();
        }
      }
    }

    long now() {
      return TimeBase.nowMs();
    }

    long min(long a, long b) {
      return a <= b ? a : b;
    }

    boolean isTime() {
      return false;
    }

    void scheduleTimer() {
      if (log.isDebug3()) log.debug3("Action timer " + trigger);
      TimerQueue.schedule(trigger,
          new TimerQueue.Callback() {
        public void timerExpired(Object cookie) {
          execute();
        }},
        null);
    }

    synchronized void execute() {
      if (!isProcessed && (alerts != null) && !alerts.isEmpty()) {
        if (alerts.size() == 1) {
          action.record(getDaemon(), (Alert)alerts.get(0));
        } else {
          action.record(getDaemon(), alerts);
        }
        alerts = null;
      }
      isProcessed = true;
    }
  }
}
