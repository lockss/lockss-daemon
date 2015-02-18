/*
 * $Id$
 *

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.io.*;
import java.net.*;

import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.util.*;

/** WatchdogService pokes a file periodically to reassure the platform that
 * java is still running ok.  If the file's last-modified time is not
 * updated for a sufficiently long time, the platform should kill and
 * restart java.  (JVM segfaults generally cause it to loop.)
 */
public class WatchdogService
  extends BaseLockssManager implements ConfigurableManager  {

  // It would be simpler to do this with a thread, but using the TimerQueue
  // ensures that the watchdog isn't poked if the TimerQueue dies.  If this
  // is changed to not use TimerQueue, then TimerQueue should have a
  // watchdog enabled around the timer callbacks.

  static final String PREFIX = Configuration.PLATFORM + "watchdog.";

  static final String PARAM_PLATFORM_WDOG_FILE = PREFIX + "file";
  static final String PARAM_PLATFORM_WDOG_INTERVAL = PREFIX + "interval";
  static final long DEFAULT_PLATFORM_WDOG_INTERVAL = Constants.HOUR;

  static final String PARAM_PLATFORM_WDOG_DNS = PREFIX + "dns.enabled";
  static final boolean DEFAULT_PLATFORM_WDOG_DNS = false;
  static final String PARAM_PLATFORM_WDOG_DNS_DOMAIN = PREFIX + "dns.domain";
  static final String DEFAULT_PLATFORM_WDOG_DNS_DOMAIN = "lockss.org";

  protected static Logger log = Logger.getLogger("PlatformWatchdog");
  private File watchedFile = null;
  private boolean enabled = false;
  private boolean stopped = false;
  private long interval;
  private TimerQueue.Request req;

  private boolean doDns = DEFAULT_PLATFORM_WDOG_DNS;
  private String dnsDomain = DEFAULT_PLATFORM_WDOG_DNS_DOMAIN;
  private long dnsCtr = 0;
  String dnsProbeHost = null;
  boolean dnsProbeAttempted = false;

  public synchronized void setConfig(Configuration config,
				     Configuration prevConfig,
				     Configuration.Differences changedKeys) {
    if (changedKeys.contains(PARAM_PLATFORM_WDOG_DNS)) {
      doDns = config.getBoolean(PARAM_PLATFORM_WDOG_DNS,
				DEFAULT_PLATFORM_WDOG_DNS);
    }
    if (changedKeys.contains(PARAM_PLATFORM_WDOG_DNS_DOMAIN)) {
      dnsDomain = config.get(PARAM_PLATFORM_WDOG_DNS_DOMAIN,
			     DEFAULT_PLATFORM_WDOG_DNS_DOMAIN);
    }
    if (changedKeys.contains(PARAM_PLATFORM_WDOG_FILE) ||
	changedKeys.contains(PARAM_PLATFORM_WDOG_INTERVAL)) {
      String name = config.get(PARAM_PLATFORM_WDOG_FILE);
      interval = config.getTimeInterval(PARAM_PLATFORM_WDOG_INTERVAL,
					DEFAULT_PLATFORM_WDOG_INTERVAL);
      if (!StringUtil.isNullString(name) && interval > 0) {
	watchedFile = new File(name);
	log.info("Platform watchdog interval: " +
		 StringUtil.timeIntervalToString(interval) +
		 ", file: " + watchedFile);
	if (theApp.isAppRunning()) {
	  enable();
	}
      } else {
	disable();
	watchedFile = null;
      }
    }
  }

  public void startService() {
    super.startService();
    if (watchedFile != null) {
      enable();
    }
  }

  public synchronized void stopService() {
    disable();
    super.stopService();
  }

  /** Force the platform to kill the app, by stopping the updating of
   * the file */
  public synchronized void forceStop() {
    stopped = true;
    stopRunning();
  }

  private synchronized void enable() {
    if (!stopped) {
      stopRunning();		      // First, ensure no current timer req
      enabled = true;
      woof();
    }
  }

  private synchronized void disable() {
    if (enabled) {
      log.info("Disabling watchdog");
    }
    stopRunning();
    if (!stopped && watchedFile != null && watchedFile.exists()) {
      try {
	watchedFile.delete();
      } catch (Exception e) {
      }
    }
  }

  private void stopRunning() {
    enabled = false;
    if (req != null) {
      TimerQueue.cancel(req);
    }
  }

  // TimerQueue callback.  Not inline because don't need a new instance
  // each time.
  private TimerQueue.Callback timerCallback =
    new TimerQueue.Callback() {
      public void timerExpired(Object cookie) {
	woof();
      }
      public String toString() {
	return "Java watchdog keepalive";
      }
    };

  // Touch the watchdog file, schedule a timer event for the next one.  If
  // can't update time on file, try to delete it so platform will know
  // we're not running.
  private synchronized void woof() {
    log.debug2("woof");
    if (doDns) {
      // try a DNS lookup to ensure that DNS lookups aren't hanging
      dnsProbeHost = "" + (++dnsCtr) + "." + dnsDomain;
      if (log.isDebug3()) log.debug3("dns: " + dnsProbeHost);
      try {
	InetAddress ina = InetAddress.getByName(dnsProbeHost);
	dnsProbeAttempted = true;
      } catch (UnknownHostException e) {
	dnsProbeAttempted = true;
      }
    }
    req = null;
    if (watchedFile != null) {
      String verbing = "testing existence of"; // for error msg
      try {
	if (!watchedFile.exists()) {
	  verbing = "creating";
	  if (watchedFile.createNewFile()) {
	    log.debug("Created watchdog file: " + watchedFile.toString());
	  } else {
	    log.warning("Couldn't create watchdog file: " +
			watchedFile.toString());
	    disable();
	  }
	}
	verbing = "setting last-modified time of";
	if (!watchedFile.setLastModified(TimeBase.nowMs())) {
	  disable();
	}
      } catch (Exception e) {
	log.warning("Error " + verbing + " platform watchdog file: " +
		    watchedFile.toString() + ": " + e.toString());
	disable();
      }
    }
    if (enabled) {
      req = TimerQueue.schedule(Deadline.in(interval), timerCallback, null);
    }
  }
}
