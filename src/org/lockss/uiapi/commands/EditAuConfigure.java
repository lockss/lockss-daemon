/*
 * $Id: EditAuConfigure.java,v 1.3 2005-10-11 05:47:42 tlipkis Exp $
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
import org.lockss.config.*;

import org.lockss.uiapi.servlet.*;
import org.lockss.uiapi.util.*;

/**
 * Implements the "get status table" command
 */
public class EditAuConfigure extends AuActivityBase {

  private static String NAME  = "EditAuConfigure";
  private static Logger log   = Logger.getLogger(NAME);
  /**
   * Common "restore the AU" text
   */
  public static final String RESTORE_AU = "Restore Archival Unit";

  public EditAuConfigure() {
    super();
  }

  /**
   * Command setup
   * @return true on success
   */
  public boolean doRemoteSetupAndVerification() {

    /*
     * Verify arguments
     */
    if (!verifyMinimumParameters()) {
      return false;
    }
    /*
     * Lookup (and save) the plugin element, generate page setup information
     */
    if (!setPlugin()) {
      return false;
    }
    generateSetupXml();
    return true;
  }

  /**
   * Act on the requested command (restore, update, delete)
   * @return true on success
   */
  public boolean doCommand() throws IOException {

    /*
     * Stop now if we can't proceed (no action, missing arguments)
     */
    if (!isRestoreAction() && !isUpdateAction()       &&
        !isRemoveAction()  && !isDeactivateAction())  {
      return true;
    }

    if (!verifyMinimumParameters()) {
      return false;
    }
    /*
     * Lookup (and save) the plugin element
     */
    if (!setPlugin()) {
      return false;
    }
    /*
     * Act on the client request - remove this AU?
     */
    if (isRemoveAction()) {
      return removeOrDeactivateAu("Delete");
    }
    /*
     * Deactivate?
     */
    if (isDeactivateAction()) {
      return removeOrDeactivateAu("Deactivate");
    }
    /*
     * Preserve user data on page refresh, even following a successful update.
     * This allows us to override the "page setup" XML generated above.
     */
    if (isUpdateAction()) {
      addResponseStatusOption(AP_OPTION_PRESERVE, COM_TRUE);
    }
    return updateAu();
  }

  /*
   * "Helpers"
   */

  /**
   * Verify parameter isn't null
   */
  protected String getAndVerifyParameter(String name) {
    String value = getParameter(name);

    if (StringUtil.isNullString(value)) {
      throw new ResponseException("Missing mandatory parameter: " + name);
    }
    return value;
  }

  /**
   * Ensure required parameters are present
   * @return true If so
   */
  private boolean verifyMinimumParameters() {
    int count = 0;

    if (!StringUtil.isNullString(getParameter(AP_E_AUID)))      count++;
    if (!StringUtil.isNullString(getParameter(AP_E_PARAMETER))) count++;
    if (!StringUtil.isNullString(getParameter(AP_E_TARGET)))    count++;

    return (count == 3);
  }

  /**
   * Set up the environment for this command
   * <p>
   * Save the Archival Unit we'll modify, verify our plugin is loaded
   * @return false If an error occurs
   */
  private boolean setPlugin() {
    PluginManager pluginManager = getLockssDaemon().getPluginManager();
    String        auId;
    AuProxy       auProxy;
    String        key;


    auId = getAndVerifyParameter(AP_E_AUID);
    setPlugin(getAnyAuProxy(auId));

    key = PluginManager.pluginKeyFromId(getPlugin().getPluginId());
    if (!pluginLoaded(key)) {
      return error("Plugin is not loaded: " + key);
    }
    return true;
  }

  /*
   * Generate XML to provide the required page setup information
   */
  private void generateSetupXml() {
    Configuration config;
    AuProxy       auProxy;

    auProxy = getAnyAuProxy(getAndVerifyParameter(AP_E_AUID));
    config = auProxy.getConfiguration();

    generateSetupXml(config);
  }

  /**
   * Delete/deactivate an AU
   * @param action What to do (delete or deactivate?)
   * @return true On success
   */
  private boolean removeOrDeactivateAu(String action) {
    AuProxy auProxy;

    if ((auProxy = getAuProxy(getParameter(AP_E_AUID))) == null) {
      return error("Invalid Archival Unit ID");
    }

    try {
      if ("delete".equalsIgnoreCase(action)) {
        getRemoteApi().deleteAu(auProxy);

      } else if ("deactivate".equalsIgnoreCase(action)) {
        getRemoteApi().deactivateAu(auProxy);

      } else {
        throw new ResponseException("Unknown activity: " + action);
      }
    } catch (ArchivalUnit.ConfigurationException exception) {
      return error(action + " failed: " + exception.getMessage());

    } catch (IOException exception) {
      return error("Failed to save configuraton: " + exception.getMessage());
    }
    return true;
  }

  /**
   * Update AU configuration data
   * @return true On success
   */
  private boolean updateAu() {

    AuProxy       auProxy;
    Configuration formConfig;
    Configuration auConfig;

    if ((auProxy = getAnyAuProxy(getParameter(AP_E_AUID))) == null) {
      return error("Invalid Archival Unit ID");
    }

    try {
      /*
       * Inactive AU?
       */
      if (!auProxy.isActiveAu()) {
        formConfig = getAuConfigFromForm();
        auProxy = getRemoteApi().createAndSaveAuConfiguration(getPlugin(),
                                                              formConfig);
        return true;
      }
      /*
       * Active AU
       */
      auConfig   = auProxy.getConfiguration();
      formConfig = getAuConfigFromForm(auConfig);

      if (isChanged(auConfig, formConfig) ||
          isChanged(getRemoteApi().getStoredAuConfiguration(auProxy),
                    formConfig)) {
        getRemoteApi().setAndSaveAuConfiguration(auProxy, formConfig);
      }

    } catch (ArchivalUnit.ConfigurationException exception) {
      return error("Reconfiguration failed: " + exception.getMessage());

    } catch (IOException exception) {
      return error("Unable to save configuraton: " + exception.getMessage());
    }
    return true;
  }

  /**
   * "Remove" command?
   * @return true If so
   */
  private boolean isRemoveAction() {
    return "delete".equalsIgnoreCase(getParameter(AP_E_ACTION));
  }

  /**
   * "Deactivate" command?
   * @return true If so
   */
  private boolean isDeactivateAction() {
    return "deactivate".equalsIgnoreCase(getParameter(AP_E_ACTION));
  }

  /**
   * "Update" command?
   * @return true If so
   */
  private boolean isUpdateAction() {
    return "update".equalsIgnoreCase(getParameter(AP_E_ACTION));
  }

  /**
   * "Restore" command?
   * @return true If so
   */
  private boolean isRestoreAction() {
    return RESTORE_AU.equalsIgnoreCase(getParameter(AP_E_ACTION));
  }
}
