/*
 * $Id: ApiActivityBase.java,v 1.2 2005-10-11 05:47:42 tlipkis Exp $
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

import org.lockss.app.*;
import org.lockss.remote.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

import org.lockss.uiapi.servlet.*;
import org.lockss.uiapi.util.*;

/**
 * Base class for external command handlers (commands executed on each
 * individual LOCKSS cache via the <code>Api</code> servlet)
 */

public abstract class ApiActivityBase extends ActivityBase {

  private static Logger   log = Logger.getLogger("ApiActivity");

  /*
   * LOCKSS daemon
   */
  protected LockssDaemon  _lockssDaemon;

  /*
   * Invoked from subclass
   */
  protected ApiActivityBase() {
    super();
  }

  /*
   * Abstract methods
   */

  /**
   * Perform a command, populate the response body based on command results
   * @return True if command execution and response generation
   * complete successfully
   */
  public abstract boolean doCommand() throws Exception;

  /*
   * Base methods
   */

  /**
   * Do remote setup
   * @return true (success) or false (failure)
   * <p>
   * <code>doRemoteSetupAndVerification()</code> is executed on each cache
   * system in turn, until one of the following is true:
   * <ul>
   * <li> A success status is returned
   * <li> Every cluster member has been tried
   * </ul>
   * The status of the remote setup operation is passed back to the command
   * component that requested it - that command has to decide how to react
   * to a failure.
   */
  public boolean doRemoteSetupAndVerification() throws Exception {
    return true;
  }

  /*
   * Helpers
   */

  /**
   * Save the LOCKSS Daemon
   * @param lockssDaemon LockssDaemon Object
   * <p>
   * Invoked by the <code>Api</code> servlet
   */
  public void setLockssDaemon(LockssDaemon lockssDaemon) {
    _lockssDaemon = lockssDaemon;
  }

  /**
   * Get the LOCKSS daemon
   * @return LockssDaemon object
   */
  protected LockssDaemon getLockssDaemon() {
    return _lockssDaemon;
  }

  /**
   * Get the Status Service
   * @return StatusService object
   */
  protected StatusService getStatusService() {
    return getLockssDaemon().getStatusService();
  }

  /**
   * Get the remote API reference from the LOCKSS daemon
   * @return RemoteApi
   */
  protected RemoteApi getRemoteApi() {
    return getLockssDaemon().getRemoteApi();
  }

  /**
   * Look up a plugin for an AU title
   * @param title Title we need a plugin for
   * @return An appropriate plugin (null if none)
   */
  protected PluginProxy getTitlePlugin(String title) {
    Collection c = getRemoteApi().getTitlePlugins(title);
    if (c == null || c.isEmpty()) {
      return null;
    }
    return (PluginProxy)c.iterator().next();
  }

  /**
   * Lookup the "well known" journal title for this AU
   * @param auTitle The AU specific title
   * @return The generic journal title (null if none)
   */
  protected String getJournalTitle(String auTitle) {
    PluginProxy   pluginProxy;
    TitleConfig   titleConfig;

    if (auTitle == null) {
      return null;
    }

    pluginProxy = getTitlePlugin(auTitle);
    if (pluginProxy == null) {
      log.warning("Unknown AU title:" + auTitle);
      return null;
    }

    titleConfig = pluginProxy.getTitleConfig(auTitle);
    if (titleConfig == null) {
      log.warning("No title configuration for AU title:" + auTitle);
      return null;
    }

    return titleConfig.getJournalTitle();
  }

  /**
   * Convert an <code>ActivityBase</code> object to an
   * <code>ApiActivityBase</code> reference
   * @param activity The original ActivityBase object
   * @return An ApiActivityBase reference
   */
  public static ApiActivityBase getApiActivity(Object activity) {

    if (!(activity instanceof ApiActivityBase)) {

      throw new ResponseException("Not an ApiActivityBase object: "
                                + activity.toString());
    }
    return (ApiActivityBase) activity;
  }
}
