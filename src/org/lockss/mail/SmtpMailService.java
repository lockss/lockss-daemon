/*
 * $Id: SmtpMailService.java,v 1.12 2006-06-04 06:25:53 tlipkis Exp $
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

package org.lockss.mail;

import java.io.*;
import java.net.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.PriorityQueue;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;

/** SmtpMailService is a MailService that sends mail directly over an smtp
 * connection, queuing them in memory and retrying each until it is
 * successfully sent */
public class SmtpMailService
  extends BaseLockssManager implements MailService, ConfigurableManager {
  protected static Logger log = Logger.getLogger("Mail");

  static final String PRIORITY_PARAM_MAILQ = "MailQueue";
  static final int PRIORITY_DEFAULT_MAILQ = Thread.NORM_PRIORITY /*+ 1*/;

  /** Outgoing SMTP relay */
  public static final String PARAM_SMTPHOST = PREFIX + "smtphost";
  public static final String PARAM_SMTPPORT = PREFIX + "smtpport";
  public static final int DEFAULT_SMTPPORT = 25;

  /** Interval at which to retry sending messages after retryable error */
  public static final String PARAM_RETRY_INTERVAL = PREFIX + "retryInterval";
  public static final long DEFAULT_RETRY_INTERVAL = Constants.HOUR;

  /** Max times to retry a message before giving up */
  public static final String PARAM_MAX_RETRIES = PREFIX + "maxRetries";
  public static final int DEFAULT_MAX_RETRIES = 3;

  /** Max length of message queue; no new messages accepted if exceeded */
  public static final String PARAM_MAX_QUEUELEN = PREFIX + "maxQueueLen";
  public static final int DEFAULT_MAX_QUEUELEN = 1000;

  /** Max rate at which to send mail; messages remain in queue longer if
   * exceeded */
  public static final String PARAM_MAX_MAIL_RATE = PREFIX + "maxMailRate";
  public static final String DEFAULT_MAX_MAIL_RATE = "10/1h";

  private boolean enabled = DEFAULT_ENABLED;
  private String smtpHost = null;
  private int smtpPort = DEFAULT_SMTPPORT;
  private String localHostName;

  protected PriorityQueue queue = new PriorityQueue();
  private MailThread mailThread;
  private RateLimiter rateLimiter;
  private long retryInterval;
  private int maxRetries;
  private int maxQueuelen = DEFAULT_MAX_QUEUELEN;

  public void startService() {
    super.startService();
  }

  public synchronized void stopService() {
    stopThread();
    super.stopService();
  }

  public synchronized void setConfig(Configuration config,
				     Configuration prevConfig,
				     Configuration.Differences changedKeys) {
    // Unconditional: not under PREFIX
    localHostName = getLocalHostname();

    // Don't rely on change to enable by default
    boolean doEnable = config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED);

    if (changedKeys.contains(PREFIX)) {
      maxRetries = config.getInt(PARAM_MAX_RETRIES, DEFAULT_MAX_RETRIES);
      maxQueuelen = config.getInt(PARAM_MAX_QUEUELEN, DEFAULT_MAX_QUEUELEN);
      retryInterval = config.getTimeInterval(PARAM_RETRY_INTERVAL,
					     DEFAULT_RETRY_INTERVAL);
      rateLimiter =
	RateLimiter.getConfiguredRateLimiter(config, rateLimiter,
					     PARAM_MAX_MAIL_RATE,
					     DEFAULT_MAX_MAIL_RATE);

      smtpHost = config.get(PARAM_SMTPHOST);
      smtpPort = config.getInt(PARAM_SMTPPORT, DEFAULT_SMTPPORT);
    }
    if (doEnable != enabled) {
      if (doEnable) {
	if (smtpHost==null) {
	  String parameter = PARAM_SMTPHOST;
	  log.error("Couldn't determine "+parameter+
		    " from Configuration.  Disabling emails");
	  enabled = false;
	  return;
	}
	if (!queue.isEmpty()) {
	  ensureThreadRunning();
	}
	enabled = doEnable;
      }
    }
  }

  private String getLocalHostname() {
    String host = Configuration.getPlatformHostname();
    if (host == null) {
      try {
        host = IPAddr.getLocalHost().getHostName();
      } catch (UnknownHostException ex) {
        log.error("Couldn't determine localhost.", ex);
	return null;
      }
    }
    return host;
  }

  public boolean sendMail(String sender, String recipient, MailMessage msg) {
    if (queue.size() >= maxQueuelen) {
      log.warning("Mail queue full, discarding message.");
      return false;
    }
    queue.put(new Req(sender, recipient, msg));
    ensureThreadRunning();
    return true;
  }

  void processReq(Req req) {
    boolean ok = true;
    int res = sendReq(req);
    log.debug2("Sent: " + res);
    switch (res) {
    case SmtpClient.RESULT_RETRY:
      if (++req.retryCount > maxRetries) {
	log.warning("Req deleted due to too many retries: " +
		    req.retryCount);
	queue.remove(req);
      } else {
	if (log.isDebug3()) {
	  log.debug3("Requeueing " + req);
	}
	req.nextRetry.expireIn(retryInterval);
	queue.sort();
      }
      break;
    case SmtpClient.RESULT_FAIL:
      // XXX need better log here - client should record smtp
      // transaction
      log.warning("Send failed");
      // fall through
    default:
      ok = false;
    case SmtpClient.RESULT_OK:
      req.msg.delete(ok);
      queue.remove(req);
    }
  }

  int sendReq(Req req) {
    try {
      SmtpClient client = makeClient();
      int res = client.sendMsg(req.sender, req.recipient, req.msg);
      if (log.isDebug3()) {
	log.debug3("Client returned " + res);
      }
      return res;
    } catch (Exception e) {
      log.error("Client threw", e);
      return SmtpClient.RESULT_RETRY;
    }
  }

  SmtpClient makeClient() throws IOException {
    log.debug3("makeClient(" + smtpHost + ", " + smtpPort + ")");
    return new SmtpClient(smtpHost, smtpPort);
  }

  static class Req implements Comparable {
    String recipient;
    String sender;
    MailMessage msg;
    Deadline nextRetry;
    int retryCount = 0;

    Req(String sender, String recipient, MailMessage msg) {
      this.recipient = recipient;
      this.sender = sender;
      this.msg = msg;
      nextRetry = Deadline.in(0);
    }

    public int compareTo(Object o) {
      return nextRetry.compareTo(((Req)o).nextRetry);
    }
  }

  // Queue runner

  BinarySemaphore threadWait = new BinarySemaphore();

  synchronized void ensureThreadRunning() {
    if (mailThread == null) {
      log.info("Starting thread");
      mailThread = new MailThread("MailQ");
      mailThread.start();
      mailThread.waitRunning();
    } else {
      threadWait.give();
    }
  }

  public void stopThread() {
    if (mailThread != null) {
      log.info("Stopping thread");
      mailThread.stopMail();
      mailThread = null;
    }
  }

  private class MailThread extends LockssThread {
    private volatile boolean goOn = true;

    private MailThread(String name) {
      super(name);
    }

    public void lockssRun() {
      triggerWDogOnExit(true);
      setPriority(PRIORITY_PARAM_MAILQ, PRIORITY_DEFAULT_MAILQ);
      nowRunning();

      while (goOn) {
	try {
	  Req req = (Req)queue.peekWait(Deadline.in(10 * Constants.MINUTE));
	  if (req != null) {
	    Deadline when = req.nextRetry;
	    if (!when.expired()) {
	      threadWait.take(when);
	    }
	    if (when.expired()) {
	      if (!rateLimiter.isEventOk()) {
		Deadline whenOk = Deadline.in(rateLimiter.timeUntilEventOk());
		log.info("Rate limited until " + whenOk);
		whenOk.sleep();
	      }
	      rateLimiter.event();
	      processReq(req);
	    }
	  }
	} catch (InterruptedException e) {
	  // no action - expected when stopping or when queue reordered
	} catch (Exception e) {
	  log.error("Unexpected exception caught in MailQueue thread", e);
	}
      }
    }

    private void stopMail() {
      triggerWDogOnExit(false);
      goOn = false;
      this.interrupt();
    }
  }
}
