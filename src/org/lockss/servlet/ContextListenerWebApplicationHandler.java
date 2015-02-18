/*
 * $Id$
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.lockss.util.Logger;
import org.mortbay.jetty.servlet.WebApplicationHandler;

/**
 * WebApplicationHandler that allows the registration of servlet context
 * listeners.<br />
 * This is needed to get Apache CXF web services working because the Jetty 5
 * WebApplicationHandler base class ignores any ServletContextListener that it
 * is being told to notify.
 * 
 * @author Fernando Garcia-Loygorri
 * 
 */
@SuppressWarnings("serial")
public class ContextListenerWebApplicationHandler extends WebApplicationHandler
{
  private static Logger log = Logger
      .getLogger(ContextListenerWebApplicationHandler.class);

  // The registered servlet context listeners.
  private List<EventListener> contextListeners = new ArrayList<EventListener>();

  /**
   * Starts the handler.
   * 
   * @throws Exception
   */
  protected synchronized void doStart() throws Exception {
    log.debug2("doStart: Invoked.");

    // Check whether there are servlet context listeners to notify.
    if (contextListeners.size() > 0) {

      // Yes: Remember the original servlet initialization setting.
      boolean originalSetting = isAutoInitializeServlets();

      // Turn servlet initialization off.
      setAutoInitializeServlets(false);

      // Start the underlying handler.
      super.doStart();

      // Notify the registered context listeners.
      ServletContextEvent event = new ServletContextEvent(getServletContext());
      for (EventListener listener : contextListeners) {
	if (log.isDebug2()) log.debug2("doStart: listener = " + listener);
	((ServletContextListener)listener).contextInitialized(event);
      }

      // Initialize the servlets.
      initializeServlets();

      // Reset the servlet initialization original setting.
      setAutoInitializeServlets(originalSetting);
    } else {
      // No: Just start the underlying handler.
      super.doStart();
    }
    log.debug2("doStart: Done.");
  }

  /**
   * Adds a listener to the list of registered listeners.
   * 
   * @param listener
   *          An EventListener with the listener to be added.
   * @throws IllegalArgumentException
   */
  public synchronized void addEventListener(EventListener listener) {
    // Handle locally only ServletContextListeners.
    if (listener instanceof ServletContextListener) {
      contextListeners.add(listener);
    }

    // Pass the listener up the chain.
    super.addEventListener(listener);
  }

  /**
   * Removes a listener from the list of registered listeners.
   * 
   * @param listener
   *          An EventListener with the listener to be removed.
   */
  public synchronized void removeEventListener(EventListener listener) {
    contextListeners.remove(listener);

    // Pass the listener up the chain.
    super.removeEventListener(listener);
  }
}
