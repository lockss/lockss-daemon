/*
 * $Id: StatusActivityBase.java,v 1.6 2006-01-13 23:21:06 thib_gc Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.uiapi.commands;

import java.util.*;

import org.w3c.dom.Element;

import org.lockss.config.*;
import org.lockss.plugin.PluginManager;
import org.lockss.remote.*;
import org.lockss.uiapi.util.*;
import org.lockss.util.*;

/**
 * Fetch summary details for a LOCKSS machine
 */
public class StatusActivityBase extends ApiActivityBase
                                implements org.lockss.uiapi.util.Constants {
  private static Logger log = Logger.getLogger("StatusActivityBase");

  /*
   * This is privately defined in LockssServlet
   */
  final static String PARAM_PLATFORM_VERSION = "org.lockss.platform.version";

  /*
   * Required by ApiActivityBase
   *
   * Always return success
   */
  public boolean doCommand() throws Exception {
    return true;
  }

  /*
   * Status modules
   */

  /**
   * Render configuration details as XML
   * @param rootElement Parent element for disk space detail
   */
  protected void renderBuildXml(Element rootElement) {

    Element buildElement = getXmlUtils().createElement(rootElement, AP_E_BUILD);
    Element element;

    element = getXmlUtils().createElement(buildElement, AP_E_TIME);
    XmlUtils.addText(element, getBuildProperty(BuildInfo.BUILD_TIME));

    element = getXmlUtils().createElement(buildElement, AP_E_DATE);
    XmlUtils.addText(element, getBuildProperty(BuildInfo.BUILD_DATE));

    element = getXmlUtils().createElement(buildElement, AP_E_TIMESTAMP);
    XmlUtils.addText(element, getBuildProperty(BuildInfo.BUILD_TIMESTAMP));

    element = getXmlUtils().createElement(buildElement, AP_E_HOST);
    XmlUtils.addText(element, getBuildProperty(BuildInfo.BUILD_HOST));

    element = getXmlUtils().createElement(buildElement, AP_E_USER);
    XmlUtils.addText(element, getBuildProperty(BuildInfo.BUILD_USER_NAME));
  }

  /**
   * Render configuration details as XML
   * @param rootElement Parent element for disk space detail
   */
  protected void renderConfigXml(Element rootElement) {
    Configuration currentConfig;
    String        value;
    Element       element;

    /*
     * Version
     */
    currentConfig = getCurrentConfiguration();
    value = currentConfig.get(PARAM_PLATFORM_VERSION, AP_VALUE_UNKNOWN);

    element = getXmlUtils().createElement(rootElement, AP_E_PLATFORMVER);
    XmlUtils.addText(element, value);
  }

  /**
   * Get daemon uptime
   * @param rootElement Parent element for disk space detail
   */
  protected void renderUptimeXml(Element rootElement) {

    Element upElement;
    Element element;

    Date    start;
    long    millis;

    long    days, hours, minutes, seconds;

    /*
     * Set time values
     */
    start   = getLockssDaemon().getStartDate();
    millis  = new Date().getTime() - start.getTime();

    days    = millis / DAY;
    millis %= DAY;

    hours   = millis / HOUR;
    millis %= HOUR;

    minutes = millis / MINUTE;
    millis %= MINUTE;

    seconds = millis / SECOND;
    /*
     * Render as XML
     */
    upElement = getXmlUtils().createElement(rootElement, AP_E_UPTIME);

    element = getXmlUtils().createElement(upElement, AP_E_DAYS);
    XmlUtils.addText(element, String.valueOf(days));

    element = getXmlUtils().createElement(upElement, AP_E_HOURS);
    XmlUtils.addText(element, String.valueOf(hours));

    element = getXmlUtils().createElement(upElement, AP_E_MINUTES);
    XmlUtils.addText(element, String.valueOf(minutes));

    element = getXmlUtils().createElement(upElement, AP_E_SECONDS);
    XmlUtils.addText(element, String.valueOf(seconds));
  }

  /**
   * Render disk space information as XML
   * @param rootElement Parent element for disk space detail
   */
  protected void renderDiskXml(Element rootElement) {
    List resultList;

    Element diskElement;

    diskElement = getXmlUtils().createElement(rootElement, AP_E_DISKSPACE);

    try {
      resultList = RepositoryUtils.getRepositoryDetail(getStatusService());

    } catch (Exception exception) {
      log.error("Failed to get repository information", exception);

      addDiskInfo(diskElement, AP_E_FREE,  AP_VALUE_UNKNOWN);
      addDiskInfo(diskElement, AP_E_INUSE, AP_VALUE_UNKNOWN);
      return;
    }

    for (Iterator iterator = resultList.iterator(); iterator.hasNext(); ) {
      RepositoryUtils.Detail detail;
      Element element;

      detail = (RepositoryUtils.Detail) iterator.next();

      addDiskInfo(diskElement, AP_E_NAME,  detail.getName());
      addDiskInfo(diskElement, AP_E_FREE,  detail.getAvailableSpace());
      addDiskInfo(diskElement, AP_E_INUSE, detail.getSpaceInUse());
    }
  }

  /**
   * Add disk information text to a named element
   * @param root Parent element
   * @param elementName Element name
   * @param text Disk information
   */
  private void addDiskInfo(Element root, String elementName, String text) {
    Element element;

    element = getXmlUtils().createElement(root, elementName);
    XmlUtils.addText(element, text);
  }

  /**
   * Render a list of active AUs
   * @param rootElement Parent element
   */
  protected void renderAuXml(Element rootElement) {
    Element       active;
    Element       inactive;
    RemoteApi     remoteApi;
    Collection    allAus;

    /*
     * Establish list root elements (active, inactive)
     */
    active    = getXmlUtils().createElement(rootElement, AP_E_ACTIVE);
    inactive  = getXmlUtils().createElement(rootElement, AP_E_INACTIVE);

    /*
     * Active AUs?
     */
    remoteApi = getRemoteApi();

    allAus = remoteApi.getAllAus();
    if (!allAus.isEmpty()) {
      generateAuXml(allAus.iterator(), active, inactive);
    }
    /*
     * Inactive AUs?
     */
    allAus = remoteApi.getInactiveAus();
    if (allAus.isEmpty()) {
      return;
    }
    generateAuXml(allAus.iterator(), active, inactive);
  }

  /*
   * Utilities
   */

  /**
   * Generate a list of Archival Units
   * @param iterator AuProxy list iterator
   * @param activeElement
   * @param inactiveElement
   */
  private void generateAuXml(Iterator iterator,
                             Element  activeElement, Element inactiveElement) {

    XmlUtils  xmlUtils    = getXmlUtils();
    RemoteApi remoteApi   = getRemoteApi();

    Element   element;
    Element   auElement;
    Element   nameElement;

    boolean   deleted;

    /*
     * Attach all AUs
     */
    while (iterator.hasNext()) {

      AuProxy       au      = (AuProxy) iterator.next();
      Configuration config  = remoteApi.getStoredAuConfiguration(au);

      deleted = config.isEmpty() ||
                config.getBoolean(PluginManager.AU_PARAM_DISABLED, false);

      auElement = xmlUtils.createElement(deleted  ? inactiveElement
                                                  : activeElement, AP_E_AU);

      element = xmlUtils.createElement(auElement, AP_E_NAME);
      XmlUtils.addText(element, au.getName());

      element = xmlUtils.createElement(auElement, AP_E_ID);
      XmlUtils.addText(element, au.getAuId());
    }
  }

  /**
   * Fetch a named BuildInfo property
   * @param name Property name
   * @return Property value
   */
  private String getBuildProperty(String name) {
    String buildProperty = BuildInfo.getBuildProperty(name);

    return (buildProperty == null) ? AP_VALUE_UNKNOWN : buildProperty;
  }

  /**
   * Get current configuration
   * @return Configuration reference
   */
  private Configuration getCurrentConfiguration() {
    return CurrentConfig.getCurrentConfig();
  }
}

