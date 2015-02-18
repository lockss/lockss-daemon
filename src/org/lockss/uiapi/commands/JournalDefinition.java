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
import org.lockss.plugin.*;
import org.lockss.remote.*;
import org.lockss.util.*;

import org.lockss.uiapi.servlet.*;
import org.lockss.uiapi.util.*;

/**
 * New journal definition
 */
public class JournalDefinition extends ApiActivityBase {

  private static Logger log = Logger.getLogger("JournalDefinition");

  /**
   * Default "data saved" message text
   */
  public final static String  DATA_SAVED  = "Title data saved";
  /**
   * Subscription states (active/inactive)
   */
  public final static String  ACTIVE      = "active";
  public final static String  INACTIVE    = "inactive";

  /**
   * Remote setup - lookup all available journal titles
   */
  public boolean doRemoteSetupAndVerification() {
    Iterator    iterator;
    Collection  auTitles;
    TreeSet     titleSet;
    String      title;
    XmlUtils    xmlUtils;
    Element     root;


    /*
     * Get the "per-AU" titles (volumes?)
     */
    auTitles = getRemoteApi().findAllTitles();
    if (auTitles.isEmpty()) {
      return true;
    }
    /*
     * Fetch and save (discarding duplicates) the "well known" titles
     */
    titleSet = new TreeSet();
    iterator = auTitles.iterator();

    while (iterator.hasNext()) {
      title = getJournalTitle((String) iterator.next());
      if (title != null) {
        titleSet.add(title);
      }
    }
    /*
     * Generate the XML list of "well known" titles
     *
     * <info>
     *  <title>aaa</title>
     *  .
     *  <title>xxx</title>
     * </info>
     */
    xmlUtils  = ParseUtils.getApiXmlUtils();
    root      = xmlUtils.createElement(getResponseRoot(), AP_E_INFO);

    iterator = titleSet.iterator();
    while (iterator.hasNext()) {
      title = (String) iterator.next();
      XmlUtils.addText(xmlUtils.createElement(root, AP_E_TITLE), title);
    }
    return true;
  }

  /**
   * Nothing to do (never invoked, due to doLocalSetupAndVerification())
   * @return true
   */
  public boolean doCommand() throws Exception {
    return true;
  }
}
