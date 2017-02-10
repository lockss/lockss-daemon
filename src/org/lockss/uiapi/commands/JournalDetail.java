/*
 * $Id$
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

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.remote.*;
import org.lockss.util.*;
import org.lockss.servlet.*;

import org.lockss.uiapi.util.*;

/**
 * Fetch detail information for a particular title
 */
public class JournalDetail extends StatusActivityBase {
  private static Logger log = Logger.getLogger("JournalDetail");

  /**
   * Return size and damage information for the requested LOCKSS title.
   * @return true
   */
  public boolean doCommand() throws StatusService.NoSuchTableException,
                                    XmlDomBuilder.XmlDomException {
    ArrayList   auIdList;

    /*
     * Fetch information for all AUs associated with specified LOCKSS
     * title (parameter name = AP_E_TITLE)
     */
    auIdList = getAuIdList();
    /*
     * Generate response XML (AU IDs, size, damage detail)
     */
    generateXml(auIdList);
    return true;
  }

  /*
   * Helpers
   */

  /**
   * Lookup the all AU IDs associated with a "well known" title
   * @return A populated <code>ArrayList</code> of AU IDs (empty if none)
   */
  private ArrayList getAuIdList() {
    Collection    auList;
    Collection    auTitles;
    ArrayList     auIdList;

    /*
     * Establish the AU ID list, fetch all available AUs and all of the
     * "per-AU" titles
     */
    auIdList = new ArrayList();
    auList   = getRemoteApi().getAllAus();

    auTitles = getRemoteApi().findAllTitles();
    if (auTitles.isEmpty()) {
      return auIdList;
    }
    /*
     * Examine each AU title - is it associated with the requested
     * "LOCKSS title"?  If so, we're finished.
     */
    for (Iterator iterator = auTitles.iterator(); iterator.hasNext(); ) {
      String auTitle = (String) iterator.next();

      if (getAuIds(auTitle, auList, auIdList)) break;
    }
    return auIdList;
  }

  /**
   * Find all AUs for the requested "LOCKSS title"
   * @param auTitle AU title
   * @param auList All available AUs
   * @param auIdList List of matching AU IDs
   * @return true If at least one match was found
   */
  private boolean getAuIds(String auTitle,
                           Collection auList,
                           ArrayList auIdList) {
    PluginProxy   pluginProxy;
    TitleConfig   titleConfig;
    String        title;
    boolean       foundAu;

    foundAu = false;
    title = getParameter(AP_E_TITLE);

    /*
     * Find the plugin and title information for this AU title
     */
    if ((pluginProxy = getTitlePlugin(auTitle)) == null) {
      log.warning("No matching plugin for title: " + auTitle);
      return foundAu;
    }

    if ((titleConfig = pluginProxy.getTitleConfig(auTitle)) == null) {
      log.warning("No title configuration for AU title: " + auTitle);
      return foundAu;
    }
    /*
     * Examine every AU: look for matching AU and title plugins
     */
    for (Iterator iterator = auList.iterator(); iterator.hasNext(); ) {
      AuProxy auProxy = (AuProxy) iterator.next();

      if (auProxy.getPluginId().equals(pluginProxy.getPluginId())) {
        String journalTitle;
        /*
         * A match: Is this the title we're looking for?
         */
        if ((journalTitle = titleConfig.getJournalTitle()) == null) {
          /*
           * ?? Should this ever really occur ??
           */
          log.warning("No title configuration for AU title: " + auTitle);
          continue;
        }

        if (journalTitle.equalsIgnoreCase(title)) {
          auIdList.add(auProxy.getAuId());
          foundAu = true;
        }
      }
    }
    return foundAu;
  }

  /**
   * Generate the XML list of AU IDs.
   * @param auIdList ArrayList if AU IDs
   *
   * The XML looks like:
   *<code>
   * <archivalunit>
   *  <auid>id</auid>
   *  <size>n</size>
   *  <detail>damage</detail>
   * </archivalunit>
   *</code>
   */
  private void generateXml(ArrayList auIdList)
                                    throws StatusService.NoSuchTableException,
                                           XmlDomBuilder.XmlDomException {
    XmlUtils    apiUtils, statusUtils;
    Element     responseRoot, statusRoot;
    BitSet      statusOptions;

    statusUtils   = ParseUtils.getStatusXmlUtils();
    apiUtils      = ParseUtils.getApiXmlUtils();

    statusOptions = new BitSet();
    statusOptions.set(StatusTable.OPTION_NO_ROWS);

    for (Iterator iterator = auIdList.iterator(); iterator.hasNext(); ) {

      StatusTable     statusTable;
      XmlStatusTable  xmlStatusTable;
      NodeList        nodeList;
      String          auId;
      int             found;

      /*
       * Get the ArchivalUnitTable for this AU ID
       */
      auId            = (String) iterator.next();
      statusTable     = getStatusService().getTable("ArchivalUnitTable",
                                                    auId,
                                                    statusOptions);
      xmlStatusTable  = new XmlStatusTable(statusTable);
      /*
       * Set up an AU element to house the details, look through the daemon
       * status document for the desired components (size and damage)
       */
      responseRoot    = apiUtils.createElement(getResponseRoot(), AP_E_AU);
      XmlUtils.addText(apiUtils.createElement(responseRoot, AP_E_AUID), auId);

      statusRoot = xmlStatusTable.getTableDocument().getDocumentElement();
      nodeList   = statusUtils.getElementList(statusRoot,
                                              XmlStatusConstants.SUMMARYINFO);
      found = 0;
      for (int i = 0; i < nodeList.getLength(); i++) {

        String title  = ParseUtils.getText(statusUtils,
                                           (Element) nodeList.item(i),
                                           XmlStatusConstants.TITLE);

        String value  = ParseUtils.getText(statusUtils,
                                           (Element) nodeList.item(i),
                                           XmlStatusConstants.VALUE);
        if (value == null) value = "";

        if ("Content Size".equals(title)) {
          Element element = apiUtils.createElement(responseRoot, AP_E_SIZE);

          XmlUtils.addText(element, value);
          if (++found == 2) break;

        } else if ("Status".equals(title)) {
          Element element = apiUtils.createElement(responseRoot, AP_E_DETAIL);

          XmlUtils.addText(element, value.toLowerCase());
          if (++found == 2) break;
        }
      }
    }
  }
}
