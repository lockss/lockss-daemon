/*
 * $Id: AlertManagerImpl.java,v 1.2 2004-07-12 21:09:44 clairegriffin Exp $
 *

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;

/**
 * AlertManagerImpl matches alerts against configured filters and invokes
 * the actions associated with matching patterns.  Multiple groupable
 * alerts may be deferred and reported together.
*/
public class AlertManagerImpl extends BaseLockssManager
  implements AlertManager {
  protected static Logger log = Logger.getLogger("AlertMgr");

  static final String PARAM_ALERTS_ENABLED = PREFIX + "enabled";
  static final boolean DEFAULT_ALERTS_ENABLED = false;

  public static String CONFIG_FILE_ALERT_CONFIG = "alertconfig.xml";

  private ConfigManager configMgr;
  private AlertConfig alertConfig;
  private boolean alertsEnabled = DEFAULT_ALERTS_ENABLED;

  public void startService() {
    super.startService();
    configMgr = getDaemon().getConfigManager();
    loadConfig();
    tmpConfig();
  }

  void tmpConfig() {
    String to =
      configMgr.getCurrentConfig().get("org.lockss.platform.sysadminemail");
    if (StringUtil.isNullString(to)) return;
    AlertAction action = new AlertActionMail(to);
    AlertConfig conf =
      new AlertConfig(ListUtil.list(new AlertFilter(AlertPatterns.True(),
						    action)));
    alertConfig = conf;
  }

//   public synchronized void stopService() {
//     super.stopService();
//   }

  protected synchronized void setConfig(Configuration config,
					Configuration prevConfig,
					Set changedKeys) {
    if (changedKeys.contains(PARAM_ALERTS_ENABLED)) {
      alertsEnabled = config.getBoolean(PARAM_ALERTS_ENABLED,
					DEFAULT_ALERTS_ENABLED);
    }
  }

  public void loadConfig() {
    File file = configMgr.getCacheConfigFile(CONFIG_FILE_ALERT_CONFIG);
    alertConfig = loadAlertConfig(file);
  }

  public AlertConfig getConfig() {
    return alertConfig;
  }

  public void updateConfig(AlertConfig config) throws Exception {
    alertConfig = config;
    File file = configMgr.getCacheConfigFile(CONFIG_FILE_ALERT_CONFIG);
    storeAlertConfig(file, config);
  }

  void storeAlertConfig(File file, AlertConfig alertConfig) throws Exception {
    try {
      if (log.isDebug3()) {
        log.debug3("Storing " + alertConfig);
      }
//       store(file, new AlertConfigBean(alertConfig));
      store(file, alertConfig);
    } catch (Exception e) {
      log.error("Couldn't store alert config: ", e);
      throw e;
    }
  }

  AlertConfig loadAlertConfig(File file) {
    try {
      if (log.isDebug3()) {
        log.debug3("Loading alert config");
      }
      AlertConfig acb = (AlertConfig)load(file, AlertConfig.class);
      if (acb == null) {
        log.debug2("No alert config");
        // none found, so use default
        return new AlertConfig();
      }
      return acb;
    } catch (XmlMarshaller.MarshallingException me) {
      log.error("Marshalling exception for alert config '"+
		   "': " + me.getMessage());
      // continue with default AlertConfig
      return new AlertConfig();
    } catch (Exception e) {
      log.error("Couldn't load alert config", e);
      throw
	new RuntimeException("Couldn't load alert config: " + e.getMessage());
    }
  }

  static final String MAPPING_FILE_NAME = "/org/lockss/alert/alertmapping.xml";


  static final String[] MAPPING_FILES = {
    MAPPING_FILE_NAME,
  };


  /**
   * Utility function to marshall classes.
   * @param root the root dir
   * @param fileName the file name
   * @param storeObj the Object to marshall
   * @throws Exception
   */
  void store(File file, Object obj) throws Exception {
    XmlMarshaller marshaller = new XmlMarshaller();
    marshaller.store(file, obj, marshaller.getMapping(MAPPING_FILES));
  }

  /**
   * Utility function to unmarshall classes.
   * @param file the file
   * @param loadClass the Class type
   * @return Object the unmarshalled Object
   * @throws Exception
   */
  Object load(File file, Class loadClass) throws Exception {
    XmlMarshaller marshaller = new XmlMarshaller();
    return marshaller.load(file, loadClass,
			   marshaller.getMapping(MAPPING_FILES));
  }

  /** Raise an Alert */
  public void raiseAlert(Alert alert) {
    if (!alertsEnabled || alertConfig == null) {
      log.debug3("alerts disabled");
      return;
    }
    log.debug3(alert.toString());
    Set actions = findMatchingActions(alert, alertConfig.getFilters());
    for (Iterator iter = actions.iterator(); iter.hasNext(); ) {
      AlertAction action = (AlertAction)iter.next();
      recordOrDefer(alert, action);
    }
  }

  /** Return the actions whose pattern matches the alert. */
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

  /** Makes decisions about and keeps track of pending alerts to reports in
   * groups */
  class PendingActions {
    AlertAction action;
    List alerts;
    Alert initial;
    Deadline trigger;
    boolean isProcessed = false;

    PendingActions(AlertAction action) {
      this.action = action;
    }

    synchronized void addAlert(Alert alert) {
      if (alerts == null || isProcessed) {
	log.debug3("Recording first: " + alert);
	// record this one, start list for successive, start timer
	action.record(getDaemon(), alert);
	alerts = new ArrayList();
	isProcessed = false;
	scheduleTimer();
      } else {
	log.debug3("Adding: " + alert);
	if (alerts.isEmpty()) {
	  trigger.expireIn(getMaxPendTime());
	  log.debug3(" and resetting timer to " + trigger);
	}
	alerts.add(alert);
	if (isTime()) {
	  execute();
	}
      }
    }

    long getMaxPendTime() {
      return action.getMaxPendTime();
    }

    boolean isTime() {
      return false;
    }

    void scheduleTimer() {
      trigger = Deadline.in(getMaxPendTime());
      log.debug3("Action timer " + trigger);
      TimerQueue.schedule(trigger,
			  new TimerQueue.Callback() {
			    public void timerExpired(Object cookie) {
			      AlertManagerImpl.this.log.debug3("Timer");
			      execute();
			    }},
			  null);
    }

    synchronized void execute() {
      if (!isProcessed && (alerts != null) && !alerts.isEmpty()) {
	action.record(getDaemon(), alerts);
	alerts = null;
	isProcessed = true;
      }
    }
  }
}
