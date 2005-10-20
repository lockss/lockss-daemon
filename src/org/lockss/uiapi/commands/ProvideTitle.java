/*
 * $Id: ProvideTitle.java,v 1.4 2005-10-20 22:57:49 troberts Exp $
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
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.remote.*;
import org.lockss.servlet.*;
import org.lockss.util.*;

import org.lockss.uiapi.util.*;

/**
 * Fetch detail information for a LOCKSS machine
 */
public class ProvideTitle extends ApiActivityBase {
  private static Logger log   = Logger.getLogger("ProvideTitle");

  private int   _missingCount;
  private int   _titleCount;
  private int   _totalCount;

  /**
   * Populate response with Lockss build and version information
   * @return true
   */
  public boolean doCommand() {
    Element element;

    _missingCount = 0;
    _titleCount   = 0;
    _totalCount   = 0;

    doTitleListXml(getResponseRoot());

    element = getXmlUtils().createElement(getResponseRoot(), AP_E_ACTIVE);
    doAuListXml(element);

    doStatisticsXml(getResponseRoot());
    return true;
  }

  /*
   * Utilities
   */

  /**
   * Render varous statistics
   */
  private void doStatisticsXml(Element root) {

    XmlUtils.addText(getXmlUtils().createElement(root, AP_E_FIELD1),
                          String.valueOf(_titleCount));

    XmlUtils.addText(getXmlUtils().createElement(root, AP_E_FIELD2),
                          String.valueOf(_missingCount));

    XmlUtils.addText(getXmlUtils().createElement(root, AP_E_TOTAL),
                          String.valueOf(_totalCount));
  }

  /**
   * Lookup all configurable titles
   * @param root Title details appended to this element
   */
  private void doTitleListXml(Element root) {
    SortedSet   titleSet;
    Collection  titles;
    Iterator    iterator;

    titles = getRemoteApi().findAllTitles();
    if (titles.isEmpty()) {
      log.warning("No journal titles defined");
      return;
    }

    titleSet = new TreeSet();

    iterator = titles.iterator();
    while (iterator.hasNext()) {
      String title = getJournalTitle((String) iterator.next());

      if (title != null) {
        titleSet.add(title);
      }
    }

    iterator = titleSet.iterator();
    while (iterator.hasNext()) {
      XmlUtils.addText(getXmlUtils().createElement(root, AP_E_PUBLICATION),
                           (String) iterator.next());
    }
  }

  /**
   * Get all "title DB" journal names (from the AU configuration)
   */
  private Collection getJournalTitles(Collection auList) {
    TreeSet journalTitles = new TreeSet();

    for (Iterator iterator = auList.iterator(); iterator.hasNext(); ) {
      AuProxy     auProxy     = (AuProxy) iterator.next();
      TitleConfig titleConfig = auProxy.getTitleConfig();

      if (titleConfig != null) {
        String journalTitle = titleConfig.getJournalTitle();

        if (journalTitle != null) {
          journalTitles.add(journalTitle);
          log.debug("adding title: " + journalTitle);
        }
      }
    }
    return journalTitles;
  }


  /**
   * Generate XML to detail
   *
   */
  private void doAuListXml(Element root) {
    Collection    auList;
    Collection    journalTitles;
    Element       titleElement;
    String        lastTitle;

    /*
     * Fetch all available AUs and all of the "per-AU" titles
     */
    auList = getRemoteApi().getAllAus();
    if (auList.isEmpty()) {
      return;
    }
    /*
     * Handle the special case: no information for these AUs in the title DB
     */
    journalTitles = getJournalTitles(auList);
    if (journalTitles.isEmpty()) {

      titleElement = getXmlUtils().createElement(root, AP_E_TITLE);
      getXmlUtils().createElement(titleElement, AP_E_COMMENT);
      /*
       * Generate XML for each AU: title (no name), AU name, AU ID
       */
      for (Iterator iterator = auList.iterator(); iterator.hasNext(); ) {
        AuProxy     auProxy     = (AuProxy) iterator.next();
        TitleConfig titleConfig = auProxy.getTitleConfig();
        String      auTitle     = null;

        if (titleConfig != null) {
          auTitle = titleConfig.getDisplayName();
        }

				if (auTitle == null) {
          auTitle = auProxy.getName();
        }

				renderAuXml(titleElement, auTitle, auProxy.getAuId());
        _missingCount++;
        _totalCount++;
      }
      return;
    }
    /*
     * The normal case: some title DB information available
     */
    lastTitle     = null;
    titleElement  = null;

    for (Iterator iterator = journalTitles.iterator(); iterator.hasNext(); ) {
      String  title;
      Element nameElement;
      /*
       * Generate XML for each title: title, list of AU collecting this title)
       */
      title = (String) iterator.next();
      if (!title.equalsIgnoreCase(lastTitle)) {

        titleElement  = getXmlUtils().createElement(root, AP_E_TITLE);
        nameElement   = getXmlUtils().createElement(titleElement, AP_E_COMMENT);
        XmlUtils.addText(nameElement, title);

        lastTitle = title;
      }
      renderAuTitleXml(titleElement, title, auList);
    }
    renderAuMinusTitleXml(root, auList);
  }

  /**
   * Render AUs and associated title configuration data
   * @param root All generated XML is appended here
   * @param journalTitle journal title
   * @param auList All available AUs
   */
  private void renderAuTitleXml(Element root,
                                String journalTitle, Collection auList) {
    for (Iterator iterator = auList.iterator(); iterator.hasNext(); ) {

      AuProxy     auProxy     = (AuProxy) iterator.next();
      TitleConfig titleConfig = auProxy.getTitleConfig();
      String      auTitle;

      if (titleConfig != null) {
        if (journalTitle.equalsIgnoreCase(titleConfig.getJournalTitle())) {

          auTitle = titleConfig.getDisplayName();
          if (auTitle == null) {
            auTitle = auProxy.getName();
          }

          renderAuXml(root, auTitle, auProxy.getAuId());
          _titleCount++;
          _totalCount++;
        }
      }
    }
  }

  /**
   * Render those AUs that have no title configuration data
   * @param root Generated XML appended here
   * @param auList All available AUs
   */
  private void renderAuMinusTitleXml(Element root, Collection auList) {
    for (Iterator iterator = auList.iterator(); iterator.hasNext(); ) {

      AuProxy     auProxy     = (AuProxy) iterator.next();
      TitleConfig titleConfig = auProxy.getTitleConfig();
      String      auTitle;

      if ((titleConfig == null) || (titleConfig.getJournalTitle() == null)) {
        Element titleElement;

        titleElement = getXmlUtils().createElement(root, AP_E_TITLE);
        getXmlUtils().createElement(titleElement, AP_E_COMMENT);

        auTitle = (titleConfig == null) ? auProxy.getName()
                                        : titleConfig.getDisplayName();
        renderAuXml(titleElement, auTitle, auProxy.getAuId());
        _missingCount++;
        _totalCount++;
      }
    }
  }

  /**
   * Generate per-AU XML
   * @param root XML data appended here
   * @param auTitle AU name
   * @param auId The AU ID
   */
  private void renderAuXml(Element root, String auTitle, String auId) {
    Element auElement = getXmlUtils().createElement(root, AP_E_AU);

    XmlUtils.addText(getXmlUtils().createElement(auElement, AP_E_NAME),
                          auTitle == null ? "" : auTitle);
    XmlUtils.addText(getXmlUtils().createElement(auElement, AP_E_AUID),
                          auId);
  }
}
