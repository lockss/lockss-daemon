/*
 * $Id: AuMenu.java,v 1.3 2005-10-11 05:47:42 tlipkis Exp $
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
public class AuMenu extends AuActivityBase {

  private static Logger log   = Logger.getLogger("AuMenu");


  public AuMenu() {
    super();
  }

  /**
   * Set up two Archival Unit lists - active (edit) and inactive (restore)
   * @return true
   */
  public boolean doCommand() throws IOException {

    Element infoElement;
    Element editElement;
    Element restoreElement;

   /*
    * Set up the response "root", active and inactive collection elements
    */
    infoElement     = getXmlUtils().createElement(getResponseRoot(), AP_E_INFO);
    editElement     = getXmlUtils().createElement(infoElement, AP_E_EDIT);
    restoreElement  = getXmlUtils().createElement(infoElement, AP_E_RESTORE);
    /*
     * Build the two AU lists
     */
    doAuList(editElement, restoreElement);

    return true;
  }

  /**
   * Populate the edit and restore lists with AU names and IDs
   * @param editElement Edit "list head"
   * @param restoreElement restore "list head"
   */
  private void doAuList(Element editElement, Element restoreElement) {
    RemoteApi     remoteApi;
    Collection    all;

    /*
     * Any AUs available?
     */
    remoteApi = getRemoteApi();

    all = remoteApi.getAllAus();
    if (!all.isEmpty()) {
      addAuToList(all.iterator(), editElement, restoreElement);
    }
    /*
     * Inactive AUs?
     */
    all = remoteApi.getInactiveAus();
    if (all.isEmpty()) {
      return;
    }
    addAuToList(all.iterator(), editElement, restoreElement);
  }

  /**
   * Add each AU the the appropriate list:
   *<ul>
   *<li>Edit list - the AU is still active
   *<li>Restore list - the AU has been unconfigured/deactivated (see deleted)
   *</ul>
   *
   * @param iterator AuProxy list iterator
   * @param editElement AU edit list
   *
   */
  private void addAuToList(Iterator iterator, Element editElement,
                                              Element restoreElement) {
    XmlUtils  xmlUtils;
    RemoteApi remoteApi;

    Element   actionElement;
    Element   auElement;
    Element   idElement;
    Element   nameElement;

    boolean   deleted;

    xmlUtils  = getXmlUtils();
    remoteApi = getRemoteApi();

    while (iterator.hasNext()) {

      AuProxy       au      = (AuProxy) iterator.next();
      Configuration config  = remoteApi.getStoredAuConfiguration(au);


      deleted = config.isEmpty() ||
                config.getBoolean(PluginManager.AU_PARAM_DISABLED, false);

      actionElement = deleted ? restoreElement : editElement;
      auElement     = xmlUtils.createElement(actionElement, AP_E_AU);

      nameElement = xmlUtils.createElement(auElement, AP_E_NAME);
      XmlUtils.addText(nameElement, au.getName());

      idElement = xmlUtils.createElement(auElement, AP_E_ID);
      XmlUtils.addText(idElement, au.getAuId());
    }
  }
}
