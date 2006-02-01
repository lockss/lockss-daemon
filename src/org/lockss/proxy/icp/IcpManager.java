/*
 * $Id: IcpManager.java,v 1.24 2006-02-01 06:33:11 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.proxy.icp;

import java.io.IOException;
import java.net.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.config.Configuration.Differences;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.proxy.ProxyManager;
import org.lockss.util.*;

/**
 * <p>A configurable daemon manager that controls the ICP server.</p>
 * @author Thib Guicherd-Callin
 */
public class IcpManager extends BaseLockssDaemonManager implements ConfigurableManager {

  /*
   * begin PRIVATE NESTED CLASS
   * ==========================
   */
  /**
   * <p>A {@link LockssRunnable} object that listens for incoming
   * ICP packets.</p>
   * @author Thib Guicherd-Callin
   */
  private class IcpRunnable extends LockssRunnable {

    /**
     * <p>Makes a new {@link IcpRunnable}.</p>
     */
    protected IcpRunnable() {
      super("IcpRunnable");
      icpRunning = false;
    }

    /* Inherit documentation */
    protected void lockssRun() {
      // Set up
      logger.debug("lockssRun: begin");
      long interval = CurrentConfig.getLongParam(PARAM_ICP_WDOG_INTERVAL,
                                                 DEFAULT_ICP_WDOG_INTERVAL);
      byte[] buffer = new byte[IcpMessage.MAX_LENGTH];
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      icpRunning = true;
      boolean somethingBadHappened = false;
      IcpMessage message = null;

      setPriority(ICP_THREAD_PRIORITY_FRAGMENT,
                  CurrentConfig.getIntParam(PARAM_ICP_THREAD_PRIORITY,
                                            DEFAULT_ICP_THREAD_PRIORITY));

      try {
        // Set up socket timeout
        udpSocket.setSoTimeout((int)interval / 2); // may throw
        startWDog(ICP_WDOG_INTERVAL_FRAGMENT, interval);
        nowRunning();

        // Main loop
        while (icpRunning && !somethingBadHappened) {
          try {
            // Receive
            logger.debug2("lockssRun: listening");
            udpSocket.receive(packet);
            logger.debug2("lockssRun: receive returned");

            // Rate limiter
            if (rateLimiter.isEventOk()) {
              // Accept event
              rateLimiter.event();

              // Parse packet
              message = icpFactory.makeMessage(packet);
              if (logger.isDebug3()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Received the following message: ");
                sb.append(message.toString());
                sb.append(" from ");
                sb.append(message.getUdpAddress());
                sb.append(':');
                sb.append(message.getUdpPort());
                logger.debug3(sb.toString());
              }

              // Process packet
              processMessage(message);
            }
            else {
              logger.debug2("lockssRun: rate limiter");
            }
          }
          catch (SocketTimeoutException steIgnore) {
            // drop down to finally
          }
          catch (IcpProtocolException ipe) {
            // Bad packet
            if (logger.isDebug3()) {
              StringBuffer sb = new StringBuffer();
              sb.append("Bad ICP packet from ");
              sb.append(message.getUdpAddress());
              sb.append(':');
              sb.append(message.getUdpPort());
              sb.append(": ");
              sb.append(message.toString());
              logger.debug3(sb.toString(), ipe);
            }
          }
          catch (IOException ioe) {
            if (icpRunning) {
              logger.debug2("lockssRun: IOException", ioe);
            }
            somethingBadHappened = icpRunning && udpSocket.isClosed();
          }
          finally {
            pokeWDog();
          }
        } // end of main loop

        // Trigger watchdog if something bad happened
        triggerWDogOnExit(somethingBadHappened);
      }
      catch (SocketException se) {
        logger.debug("lockssRun: could not set socket timeout", se);
      }
      finally {
        // Clean up
        stopWDog();
        if (somethingBadHappened) {
          logger.debug("lockssRun: somethingBadHappened is true");
        }
        logger.debug("lockssRun in IcpSocketImpl: end");
      }
    }

    /* Inherit documentation */
    protected void threadExited() {
      logger.debug(MESSAGE_THREAD_EXITED);
      new Thread(new Runnable() {
        public void run() {
          try {
            stopSocket();
            Thread.sleep(Constants.SECOND);
            if (shouldIcpServerStart()) {
              startSocket();
            }
          }
          catch (Exception exc) {
            logger.error("Could not restart the ICP manager", exc);
          }
        }
      }).start();
    }

  }
  /*
   * end PRIVATE NESTED CLASS
   * ========================
   */

  /**
   * <p>A UDP socket for use by the ICP socket.</p>
   */
  protected DatagramSocket udpSocket;

  /**
   * <p>An ICP factory.</p>
   */
  private IcpFactory icpFactory;

  /**
   * <p>A flag indicating whether the ICP thread is running in its
   * main loop.</p>
   */
  private volatile boolean icpRunning;

  /**
   * <p>A reference to the plugin manager.</p>
   */
  private PluginManager pluginManager;

  /**
   * <p>A port number.</p>
   */
  private int port = BAD_PORT;

  /**
   * <p>A reference to the proxy manager.</p>
   */
  private ProxyManager proxyManager;

  /**
   * <p>A rate limiter for use by the ICP socket.</p>
   */
  private RateLimiter rateLimiter;

  /**
   * <p>A reference to the resource manager.</p>
   */
  private ResourceManager resourceManager;

  /**
   * <p>Retrieves the current ICP port number.</p>
   * @return The current ICP port number, or a negative number if the
   *         ICP server is not really running at the moment.
   */
  public int getCurrentPort() {
    return port;
  }

  /**
   * <p>Determines whether an ICP server is allowed to run.</p>
   * @return True if and only if and ICP server is allowed to run.
   */
  public boolean isIcpServerAllowed() {
    return isIcpServerAllowed(CurrentConfig.getCurrentConfig());
  }

  /**
   * <p>Determines if the ICP server is currently running.</p>
   * @return True if and only if the ICP server is running.
   */
  public boolean isIcpServerRunning() {
    return udpSocket != null && icpRunning;
  }

  /* Inherit documentation */
  public void setConfig(Configuration newConfig,
                        Configuration prevConfig,
                        Differences changedKeys) {
    // ICP rate limiter
    if (changedKeys.contains(PARAM_ICP_INCOMING_RATE) && rateLimiter != null) {
      rateLimiter = RateLimiter.getConfiguredRateLimiter(newConfig,
          rateLimiter, PARAM_ICP_INCOMING_RATE, DEFAULT_ICP_INCOMING_RATE);
    }

    if (   changedKeys.contains(PARAM_PLATFORM_ICP_ENABLED)
        || changedKeys.contains(PARAM_PLATFORM_ICP_PORT)
        || changedKeys.contains(PARAM_ICP_ENABLED)
        || changedKeys.contains(PARAM_ICP_PORT)
        || changedKeys.contains(PARAM_SLOW_ICP)
        || changedKeys.contains(PARAM_ICP_THREAD_PRIORITY)
        || changedKeys.contains(PARAM_ICP_WDOG_INTERVAL)) {
      stopSocket();
      if (theDaemon.isDaemonInited() && shouldIcpServerStart(newConfig)) {
        startSocket(newConfig);
      }
    }
  }

  /* Inherit documentation */
  public void startService() {
    super.startService();
    pluginManager = getDaemon().getPluginManager();
    proxyManager = getDaemon().getProxyManager();
    if (shouldIcpServerStart()) {
      resetConfig();
    }
  }

  /* Inherit documentation */
  public void stopService() {
    stopSocket();
    super.stopService();
  }

  /**
   * <p>Resets locals when the ICP server is not running.</p>
   */
  protected synchronized void forget() {
    icpFactory = null;
    pluginManager = null;
    port = BAD_PORT;
    proxyManager = null;
    rateLimiter = null;
    resourceManager = null;
    udpSocket = null;
  }

  /**
   * <p>Determines the port on which the ICP server should be
   * running, determined using the given configuration object.</p>
   * @param theConfig a {@link Configuration} instance.
   * @return A port number if set, a negative number otherwise.
   */
  protected int getPortFromConfig(Configuration theConfig) {
    return theConfig.getInt(PARAM_ICP_PORT,
                            theConfig.getInt(PARAM_PLATFORM_ICP_PORT,
                                             BAD_PORT));
  }

  /**
   * <p>Determines whether an ICP server is allowed to run based on
   * the given {@link Configuration} instance.</p>
   * @param theConfig A {@link Configuration} instance from which to
   *                  obtain configuration information.
   * @return True unless the platform indicates it prohibits ICP.
   * @see #PARAM_PLATFORM_ICP_ENABLED
   */
  protected boolean isIcpServerAllowed(Configuration theConfig) {
    /*
     * The ICP server is allowed to run unless the platform
     * says it is not.
     */
    return theConfig.getBoolean(PARAM_PLATFORM_ICP_ENABLED,
                                true);
  }

  /**
   * <p>Processes an incoming ICP message.</p>
   * @param message An incoming message.
   */
  protected void processMessage(IcpMessage message) {
    if (message.getOpcode() == IcpMessage.ICP_OP_QUERY) {
      IcpMessage response;

      try {
        // Prepare response
        if (!proxyManager.isIpAuthorized(message.getUdpAddress().getHostAddress())) {
          logger.debug2("processMessage: DENIED");
          response = message.makeDenied();
        }
        else {
          String urlString = message.getPayloadUrl();
          CachedUrl cu = pluginManager.findOneCachedUrl(urlString);
          if (cu != null && cu.hasContent()) {
            logger.debug2("processMessage: HIT");
            response = message.makeHit();
          }
          else {
            logger.debug2("processMessage: MISS_NOFETCH");
            response = message.makeMissNoFetch();
          }
        }
      }
      catch (IcpProtocolException ipe) {
        logger.debug2("processMessage: ERR", ipe);
        try {
          response = message.makeError();
        }
        catch (IcpProtocolException ipe2) {
          logger.debug2("processMessage: double exception", ipe2);
          return; // abort
        }
      }

      // Send response
      try {
        if (logger.isDebug3()) {
          StringBuffer buffer = new StringBuffer();
          buffer.append("Sending the following message to ");
          buffer.append(message.getUdpAddress());
          buffer.append(':');
          buffer.append(message.getUdpPort());
          buffer.append(" from port ");
          buffer.append(port);
          buffer.append(": ");
          buffer.append(message.toString());
          logger.debug3(buffer.toString());
        }
        udpSocket.send(response.toDatagramPacket(message.getUdpAddress(),
                                                 message.getUdpPort()));
      }
      catch (NoRouteToHostException nrthe) {
        logger.debug("NoRouteToHostException while sending ICP response", nrthe);
        logger.debug("A NoRouteToHostException may indicate a problem related to packet filtering on the underlying platform.");
      }
      catch (IOException ioe) {
        logger.debug("processMessage: IOException", ioe);
      }
    }
    else {
      if (logger.isDebug3()) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Received a non-query ICP message from ");
        buffer.append(message.getUdpAddress());
        buffer.append(':');
        buffer.append(message.getUdpPort());
        logger.debug3(buffer.toString());
      }
    }
  }

  /**
   * <p>Determines whether the ICP server should start now.</p>
   * @return True if and only if the ICP server should start now.
   */
  protected boolean shouldIcpServerStart() {
    return shouldIcpServerStart(CurrentConfig.getCurrentConfig());
  }

  /**
   * <p>Determines whether the ICP server should start now,
   * based on the given {@link Configuration} instance.</p>
   * @param theConfig A {@link Configuration} instance from which to
   *                  obtain configuration information.
   * @return True if and only if the ICP server is allowed to start,
   *         and is enabled, and has an a priori valid ICP port number
   *         set.
   */
  protected boolean shouldIcpServerStart(Configuration theConfig) {
   return    isIcpServerAllowed(theConfig)
          && theConfig.getBoolean(PARAM_ICP_ENABLED,
                                  theConfig.getBoolean(PARAM_PLATFORM_ICP_ENABLED,
                                                       false))
          && getPortFromConfig(theConfig) > 0;
  }

  /**
   * <p>Starts the ICP socket.</p>
   */
  protected void startSocket() {
    startSocket(CurrentConfig.getCurrentConfig());
  }

  /**
   * <p>Starts the ICP socket.</p>
   * @param theConfig A {@link Configuration} instance from which to
   *                  determine the current ICP port.
   */
  protected synchronized void startSocket(Configuration theConfig) {
    boolean success = false;

    try {
      logger.info(MESSAGE_STARTING);
      logger.debug("startSocket: begin");

      port = getPortFromConfig(theConfig);
      if (port <= 0) {
        logger.debug("startSocket: getCurrentPort() returned " + port);
        return; // go to finally block
      }

      resourceManager = getDaemon().getResourceManager();
      if (!resourceManager.reserveUdpPort(port, getClass())) {
        logger.debug("startSocket: could not reserve UDP port " + port);
        return; // go to finally block
      }

      icpFactory =
        CurrentConfig.getBooleanParam(PARAM_SLOW_ICP, DEFAULT_SLOW_ICP)
            ? IcpFactoryImpl.getInstance()
            : LazyIcpFactoryImpl.getInstance();

      logger.debug("startSocket: creating socket on port " + port);
      udpSocket = new DatagramSocket(port);
      logger.debug("startSocket: socket bound to port " + port);

      rateLimiter = RateLimiter.getConfiguredRateLimiter(
          CurrentConfig.getCurrentConfig(), rateLimiter,
          PARAM_ICP_INCOMING_RATE, DEFAULT_ICP_INCOMING_RATE);

      IcpRunnable icpRunnable = new IcpRunnable();
      new Thread(icpRunnable).start();
      logger.debug("startSocket: waitRunning()");
      icpRunnable.waitRunning();
      logger.debug("startSocket: end");
      success = true;
    }
    catch (SocketException se) {
      forget(); // revert instantions
      logger.debug("startSocket: SocketException", se);
    }
    finally {
      if (success) {
        logger.info(MESSAGE_STARTED);
      }
      else {
        logger.warning(MESSAGE_COULD_NOT_START);
      }
    }
  }

  /**
   * <p>Stops the ICP socket.</p>
   */
  protected synchronized void stopSocket() {
    if (isIcpServerRunning()) {
      logger.info(MESSAGE_STOPPING);
      icpRunning = false;
      if (udpSocket != null && !udpSocket.isClosed()) {
        udpSocket.close();
      }
      resourceManager.releaseUdpPort(port, getClass());
      forget();
    }
    else {
      logger.debug("stopSocket: the ICP server is not running");
    }
  }

  /**
   * <p>The ICP enabled flag parameter.</p>
   */
  public static final String PARAM_ICP_ENABLED =
    "org.lockss.proxy.icp.enabled";

  /**
   * <p>The ICP port parameter.</p>
   */
  public static final String PARAM_ICP_PORT =
    "org.lockss.proxy.icp.port";

  /**
   * <p>The ICP enabled parameter from the platform.</p>
   */
  public static final String PARAM_PLATFORM_ICP_ENABLED =
    "org.lockss.platform.icp.enabled";

  /**
   * <p>The ICP port parameter from the platform.</p>
   */
  public static final String PARAM_PLATFORM_ICP_PORT =
    "org.lockss.platform.icp.port";

  /**
   * <p>A logger for use by instances of this class.</p>
   */
  protected static Logger logger = Logger.getLogger("IcpManager");

  /**
   * <p>An invalid port number.</p>
   */
  private static final int BAD_PORT = -1;

  /**
   * <p>The default ICP rate-limiting string.</p>
   */
  private static final String DEFAULT_ICP_INCOMING_RATE =
    "400/100"; // no suffix == milliseconds

  /**
   * <p>The default ICP thread priority.</p>
   */
  private static final int DEFAULT_ICP_THREAD_PRIORITY =
    Thread.NORM_PRIORITY + 2;

  /**
   * <p>The default ICP watchdog interval.</p>
   */
  private static final long DEFAULT_ICP_WDOG_INTERVAL =
    Constants.HOUR;

  /**
   * <p>The default slow ICP flag.</p>
   */
  private static final boolean DEFAULT_SLOW_ICP = true;

  /**
   * <p>A name for the ICP thread.</p>
   */
  private static final String ICP_THREAD_NAME = "IcpRunnable";

  /**
   * <p>A name fragment for
   * {@link LockssRunnable#PARAM_NAMED_THREAD_PRIORITY}.</p>
   */
  private static final String ICP_THREAD_PRIORITY_FRAGMENT = "icp";

  /**
   * <p>A name fragment for
   * {@link LockssRunnable#PARAM_NAMED_WDOG_INTERVAL}.</p>
   */
  private static final String ICP_WDOG_INTERVAL_FRAGMENT = "icp";

  /**
   * <p>A logging message.</p>
   */
  private static final String MESSAGE_COULD_NOT_START =
    "The ICP server could not start";

  /**
   * <p>A logging message.</p>
   */
  private static final String MESSAGE_STARTED =
    "The ICP server started successfully";

  /**
   * <p>A logging message.</p>
   */
  private static final String MESSAGE_STARTING =
    "The ICP server is starting";

  /**
   * <p>A logging message.</p>
   */
  private static final String MESSAGE_STOPPING =
    "The ICP server is stopping";

  /**
   * <p>A logging message.</p>
   */
  private static final String MESSAGE_THREAD_EXITED =
    "The thread exited unexpectedly. Typically this is caused by the socket being closed at an unexpected time, for example as a result of an IOException or because the socket was closed before a call to stopSocket(). The IcpManager will now be asked to stop, then start.";

  /**
   * <p>The ICP rate-limiting string parameter.</p>
   */
  private static final String PARAM_ICP_INCOMING_RATE =
    "org.lockss.proxy.icp.rate";

  /**
   * <p>The ICP thread priority parameter.</p>
   */
  private static final String PARAM_ICP_THREAD_PRIORITY =
    "org.lockss.proxy.icp.priority";

  /**
   * <p>The ICP watchdog interval parameter.</p>
   */
  private static final String PARAM_ICP_WDOG_INTERVAL =
    "org.lockss.proxy.icp.interval";

  /**
   * <p>The slow ICP string parameter.</p>
   */
  private static final String PARAM_SLOW_ICP =
    "org.lockss.proxy.icp.slow";

}
