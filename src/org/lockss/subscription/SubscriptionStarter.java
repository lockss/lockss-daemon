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
package org.lockss.subscription;

import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.daemon.LockssRunnable;
import org.lockss.util.Logger;

/**
 * Starts the subscription management process.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class SubscriptionStarter extends LockssRunnable {
  private static Logger log = Logger.getLogger(SubscriptionStarter.class);

  private final SubscriptionManager subscriptionManager;
  private final Configuration newConfig;
  private final Configuration prevConfig;
  private final Configuration.Differences changedKeys;

  /**
   * Constructor.
   * 
   * @param subscriptionManager
   *          A SubscriptionManager with the subscription manager.
   * @param newConfig
   *          A Configuration with the new configuration.
   * @param prevConfig
   *          A Configuration with the previous configuration.
   * @param changedKeys
   *          A Configuration.Differences with the changed configuration keys.
   */
  public SubscriptionStarter(SubscriptionManager subscriptionManager,
      Configuration newConfig, Configuration prevConfig,
      Configuration.Differences changedKeys) {
    super("SubscriptionStarter");

    this.subscriptionManager = subscriptionManager;
    this.newConfig = newConfig;
    this.prevConfig = prevConfig;
    this.changedKeys = changedKeys;
  }

  /**
   * Entry point to start the process to handle the configuration changes on
   * daemon startup.
   */
  public void lockssRun() {
    final String DEBUG_HEADER = "lockssRun(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    LockssDaemon daemon = LockssDaemon.getLockssDaemon();

    // Wait until the AUs have been started.
    if (!daemon.areAusStarted()) {
      log.debug(DEBUG_HEADER + "Waiting for aus to start");

      while (!daemon.areAusStarted()) {
	try {
	  daemon.waitUntilAusStarted();
	} catch (InterruptedException ex) {
	}
      }
    }

    // Perform the actual work.
    subscriptionManager.handleConfigurationChange(newConfig, prevConfig,
	changedKeys);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }
}
