/*
 * $Id: WatchdogService.java,v 1.2 2003-05-23 17:10:38 tal Exp $
 *

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;

/** WatchdogService pokes a file periodically to reassure the platform that
 * java is still running ok.  If the file's last-modified time is not
 * updated for a sufficiently long time, the platform should kill and
 * restart java.  (JVM segfaults generally cause it to loop.)
 */
public class WatchdogService extends BaseLockssManager {

  // It would be simpler to do this with a thread, but using the TimerQueue
  // ensures that the watchdog isn't poked if the TimerQueue dies.

  static final String PARAM_PLATFORM_WDOG_FILE =
    Configuration.PARAM_PLATFORM_PIDFILE;
  static final String PARAM_PLATFORM_WDOG_INTERVAL =
    Configuration.PLATFORM + "watchdog.interval";
  static final long DEFAULT_PLATFORM_WDOG_INTERVAL = Constants.HOUR;

  protected static Logger log = Logger.getLogger("WatchdogService");
  private File watchedFile = null;
  private boolean enabled = false;
  private long interval;
  private TimerQueue.Request req;

  protected synchronized void setConfig(Configuration config,
					Configuration prevConfig,
					Set changedKeys) {
    if (changedKeys.contains(PARAM_PLATFORM_WDOG_FILE)) {
      String name = config.get(PARAM_PLATFORM_WDOG_FILE);
      if (name != null) {
	watchedFile = new File(name);
	interval = config.getTimeInterval(PARAM_PLATFORM_WDOG_INTERVAL,
					  DEFAULT_PLATFORM_WDOG_INTERVAL);
	log.info("Platform watchdog interval: " +
		 StringUtil.timeIntervalToString(interval) +
		 ", file: " + watchedFile);
	if (!watchedFile.exists()) {
	  log.warning("Watchdog file does not exist");
	}
	if (theDaemon.isDaemonRunning()) {
	  enable();
	}
      } else {
	watchedFile = null;
	disable();
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
    if (req != null) {
      TimerQueue.cancel(req);
    }
    super.stopService();
  }

  private synchronized void enable() {
    disable();			      // First, ensure no current timer req
    enabled = true;
    woof();
  }

  private synchronized void disable() {
    enabled = false;
    if (req != null) {
      TimerQueue.cancel(req);
    }
  }

  private TimerQueue.Callback cb = new TimerQueue.Callback() {
      public void timerExpired(Object cookie) {
	woof();
      }
    };

  private synchronized void woof() {
    log.debug2("woof");
    req = null;
    if (enabled) {
      if (watchedFile != null) {
	try {
	  watchedFile.setLastModified(TimeBase.nowMs());
	} catch (Exception e) {
	  log.warning("Couldn't update platform watchdog file: " +
		      e.toString());
	}
      }
      req = TimerQueue.schedule(Deadline.in(interval), cb, null);
    }
  }

  /** For testing only */
  void setWatchedFile(File file) {
    if (watchedFile.toString() != file.toString()) {
      throw new RuntimeException("Substituting different watched file");
    }
    watchedFile = file;
  }
}
