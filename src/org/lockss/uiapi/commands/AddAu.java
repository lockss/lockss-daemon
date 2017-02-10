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
import java.util.*;
import java.text.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.remote.*;
import org.lockss.servlet.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

import org.lockss.uiapi.servlet.*;
import org.lockss.uiapi.util.*;

/**
 * Implements the "get status table" command
 */
public class AddAu extends AuActivityBase {

  private static String NAME  = "AddAu";
  private static Logger log   = Logger.getLogger(NAME);


  public AddAu() {
    super();
  }


  /**
   * Command setup
   * @return true On success
   */
  public boolean doRemoteSetupAndVerification() throws IOException {

    Element infoElement;
    String  className;
    boolean validClassName;

    /*
     * If we have a class name, make sure it's valid
     */
    validClassName = false;
    if ((className = getParameter(AP_E_CLASSNAME)) != null) {

      if (pluginLoaded(RemoteApi.pluginKeyFromId(className))) {
        validClassName = true;
      }
    }
    /*
     * Set up the response "root", format title and plugin lists
     */
    infoElement = getXmlUtils().createElement(getResponseRoot(), AP_E_INFO);

    doTitles(infoElement);
    doPlugins(infoElement);
    doClassName(infoElement, validClassName);

    return true;
  }

  /**
   * Command body (never executed)
   * @return true On success
   */
  public boolean doCommand() throws IOException {
    return true;
  }

  /*
   * Utility methods
   */

  /**
   * Lookup all configurable titles
   * @param root The parent element for elements generated here
   */
  private void doTitles(Element root) {
    SortedSet   titleSet;
    Collection  titles;
    Iterator    iterator;


    titles = getRemoteApi().findAllTitles();
    if (titles.isEmpty()) {
      return;
    }

    titleSet = new TreeSet();

    iterator = titles.iterator();
    while (iterator.hasNext()) {
      titleSet.add((String) iterator.next());
    }

    iterator = titleSet.iterator();
    while (iterator.hasNext()) {
      XmlUtils.addText(getXmlUtils().createElement(root, AP_E_PUBLICATION),
                       (String) iterator.next());
    }
  }

  /**
   * Lookup all configurable plugins
   * @param root Root for any elements generated here
   */
  private void doPlugins(Element root) {
    SortedMap pluginMap;
    Element   pluginRoot;
    Iterator  iterator;

    pluginMap = new TreeMap();
    iterator  = getRemoteApi().getRegisteredPlugins().iterator();

    while (iterator.hasNext()) {
      PluginProxy pluginProxy = (PluginProxy) iterator.next();

      pluginMap.put(pluginProxy.getPluginName(), pluginProxy);
    }

    if (pluginMap.isEmpty()) {
      return;
    }

    iterator = pluginMap.keySet().iterator();

    while (iterator.hasNext()) {
      String  pluginName  = (String) iterator.next();
      String  pluginId    = ((PluginProxy)
                                    pluginMap.get(pluginName)).getPluginId();
      Element element;

      /*
       * NAME is the publication name (HighWire)
       * ID   is the class name (org.lockss.plugins.highwire.HighWirePlugin)
       */
      pluginRoot  = getXmlUtils().createElement(root, AP_E_PLUGIN);

      element = getXmlUtils().createElement(pluginRoot, AP_E_NAME);
      XmlUtils.addText(element, pluginName);

      element = getXmlUtils().createElement(pluginRoot, AP_E_ID);
      XmlUtils.addText(element, pluginId);

    }
  }

  /**
   * Indicate that the user-supplied classname is valid
   * @param root Root for any elements generated here
   */
  private void doClassName(Element root, boolean valid) {

    Element element = getXmlUtils().createElement(root, AP_E_CLASSNAME);

    if (valid) {
      XmlUtils.addText(element, getParameter(AP_E_CLASSNAME));
    }
  }
}
