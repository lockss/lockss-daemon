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
import java.sql.*;
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
import org.lockss.config.*;

import org.lockss.uiapi.servlet.*;
import org.lockss.uiapi.util.*;

/**
 * Implements the "get status table" command
 */
public class AddAuConfigure extends AuActivityBase {

  private static String NAME  = "AddAuConfigure";
  private static Logger log   = Logger.getLogger(NAME);


  public AddAuConfigure() {
    super();
  }

  /**
   * Populate the response body
   * @return true on success
   */
  public boolean doRemoteSetupAndVerification() throws IOException {

    /*
     * Stop if any required parameters are missing (error)
     */
    if (!verifyMinimumParameters()) {
      throw new ResponseException("Missing required parameters");
    }
    /*
     * Initial page setup
     */
    return commandSetup();
  }

  /**
   * Populate the response body
   * @return true on success
   */
  public boolean doCommand() throws IOException {
    Element infoElement;

    /*
     * Return disk space
     */
    infoElement = getXmlUtils().createElement(getResponseRoot(), AP_E_INFO);
    renderDiskXml(infoElement);

    /*
     * No further action if this isn't a create command (success)
     */
    if (!isCreateCommand()) {
      return true;
    }
    /*
     * Stop if any required parameters are missing (error)
     */
    if (!verifyTarget() ||
        !verifyMinimumParameters()  ||
        !verifyDefiningParameters()) {
      throw new ResponseException("Missing required parameters");
    }
    /*
     * Create the AU
     */
    if (!commandSetup()) {
      return false;
    }
    return createAu();
  }

  /*
   * "Helpers"
   */

  /**
   * Did the client provide the minimal parameters required?
   * @return true If so
   */
  private boolean verifyMinimumParameters() {
    int count = 0;

    if (!StringUtil.isNullString(getParameter(AP_E_PUBLICATION))) count++;
    if (!StringUtil.isNullString(getParameter(AP_E_CLASSNAME)))   count++;
    if (!StringUtil.isNullString(getParameter(AP_E_PLUGIN)))      count++;

    return (count > 0);
  }

  /**
   * A target system is required to create an AU - was it provided?
   * @return true If at least one target was specified
   */
  private boolean verifyTarget() {
    if (!isCreateCommand()) {
      return true;
    }

    return !StringUtil.isNullString(getParameter(AP_E_TARGET));
  }

  /**
   * Are all of the "defining parameters" required to create an AU available?
   * @return true If so
   */
  private boolean verifyDefiningParameters() {
    KeyedList parameters;
    int       size;

    if (!isCreateCommand()) {
      return true;
    }

    parameters = ParseUtils.getDynamicFields(getXmlUtils(),
                                             getRequestDocument(),
                                             AP_MD_AUDEFINING);
    size = parameters.size();

    for (int i = 0; i < size; i++) {
      if (StringUtil.isNullString((String) parameters.getValue(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * "Create" command?
   * @return true If so...
   */
  private boolean isCreateCommand() {
    return "create".equalsIgnoreCase(getParameter(AP_E_ACTION));
  }

  /**
   * Query the daemon for information required to set up this command
   */
  private boolean commandSetup() {

    Configuration configuration = null;
    Collection    noEditKeys    = null;
    String        key;
    String        value;

    /*
     * Configure a well known publication?
     */
    if ((value = getParameter(AP_E_PUBLICATION)) != null) {
      PluginProxy plugin = getTitlePlugin(value);

      /*
       * Set plugin and Title configuration information
       */
      if (plugin == null) {
        String message = "Unknown Publication:" + value;

        log.warning(message);
        return error(message);
      }

      setPlugin(plugin);
      setTitleConfig(plugin.getTitleConfig(value));

      configuration = getTitleConfig().getConfig();
      noEditKeys    = getNoEditKeys();

    } else {
      /*
       * Lookup by Plugin or Class name - set the plugin
       *
       * NB: As of 23-Feb-04, this is not supported from AddAuPage.java.  See
       *     AddAuWithCompleteFunctionalityPage.java for full support.
       */
      if ((value = getParameter(AP_E_PLUGIN)) != null) {
        key = RemoteApi.pluginKeyFromId(value);

      } else if ((value = getParameter(AP_E_CLASSNAME)) != null) {
        key = RemoteApi.pluginKeyFromId(value);

      } else {
        return error("Supply a Publication, Plugin, or Class name");
      }

      if (StringUtil.isNullString(key)) {
        return error("Supply a valid Publication, Plugin, or Class name");
      }

      if (!pluginLoaded(key)) {
        return error("Plugin is not loaded: " + key);
      }

      setPlugin(getPluginProxy(key));
    }

    /*
     * Finally, return an XML rendition of the Plugin and AU key set up
     */
    generateSetupXml(configuration, noEditKeys);
    return true;
  }

  /**
   * Create an Archival Unit
   * @return true If successful
   */
  private boolean createAu() {

    Configuration config = getAuConfigFromForm();

    AuProxy   au;
    Element   element;

    try {
      au = getRemoteApi().createAndSaveAuConfiguration(getPlugin(), config);

    } catch (ArchivalUnit.ConfigurationException exception) {
      return error("Configuration failed: " + exception.getMessage());

    } catch (IOException exception) {
      return error("Unable to save configuration: " + exception.getMessage());
    }
    /*
     * Successful creation - add the AU name and ID to the response document
     */
    element = getXmlUtils().createElement(getResponseRoot(), AP_E_AU);
    XmlUtils.addText(element, au.getName());

    element = getXmlUtils().createElement(getResponseRoot(), AP_E_AUID);
    XmlUtils.addText(element, au.getAuId());

    return true;
  }
}
