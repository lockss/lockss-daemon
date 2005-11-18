/*
 * $Id: IcpManager.java,v 1.16 2005-11-18 21:40:02 thib_gc Exp $
 */

/*

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

package org.lockss.proxy.icp;

import java.io.IOException;
import java.net.*;

import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.config.Configuration.*;
import org.lockss.daemon.ResourceManager;
import org.lockss.plugin.*;
import org.lockss.proxy.ProxyManager;
import org.lockss.util.*;

/**
 * <p>A daemon manager that handles the ICP server.</p>
 * @author Thib Guicherd-Callin
 * @see IcpSocketImpl
 */
public class IcpManager
    extends BaseLockssDaemonManager
    implements ConfigurableManager, IcpHandler {

  /**
   * <p>An ICP socket.</p>
   */
  protected IcpSocketImpl icpSocket;

  /**
   * <p>An ICP builder.</p>
   */
  private IcpBuilder icpBuilder;

  /**
   * <p>An ICP factory.</p>
   */
  private IcpFactory icpFactory;

  /**
   * <p>A rate limiter for use by the ICP socket.</p>
   */
  private RateLimiter limiter;

  /**
   * <p>A reference to the plugin manager.</p>
   */
  private PluginManager pluginManager;

  /**
   * <p>A reference port number.</p>
   */
  private int port;

  /**
   * <p>A reference to the proxy manager.</p>
   */
  private ProxyManager proxyManager;

  /**
   * <p>A reference to the resource manager.</p>
   */
  private ResourceManager resourceManager;

  /**
   * <p>A UDP socket for use by the ICP socket.</p>
   */
  private DatagramSocket udpSocket;

  /**
   * <p>Determines the port on which the ICP server should be
   * running.</p>
   * @return A port number if set, a negative number otherwise.
   */
  public int getCurrentPort() {
    return getCurrentPort(Configuration.getCurrentConfig());
  }

  /**
   * <p>Determines the port on which the ICP server should be
   * running, determined using the given configuration object.</p>
   * @param theConfig a {@link Configuration} instance.
   * @return A port number if set, a negative number otherwise.
   */
  protected int getCurrentPort(Configuration theConfig) {
    return theConfig.getInt(PARAM_ICP_PORT,
                            theConfig.getInt(PARAM_PLATFORM_ICP_PORT,
                                             -1));
  }

  /**
   * <p>Returns a rate limiter governing the acceptable reception rate
   * of ICP messages.</p>
   * @return This manager's rate limiter.
   */
  public RateLimiter getLimiter() {
    return limiter;
  }

  /* Inherit documentation */
  public void icpReceived(IcpReceiver source, IcpMessage message) {
    if (message.getOpcode() == IcpMessage.ICP_OP_QUERY) {
        IcpMessage response;
        try {
          // Prepare response
          if (!proxyManager.isIpAuthorized(message.getUdpAddress().getHostAddress())) {
            logger.debug2("Response: DENIED");
            response = icpBuilder.makeDenied(message);
          }
          else {
            String urlString = message.getPayloadUrl();
            CachedUrl cu = pluginManager.findOneCachedUrl(urlString);
            if (cu != null && cu.hasContent()) {
              logger.debug2("Response: HIT");
              response = icpBuilder.makeHit(message);
            }
            else {
              logger.debug2("Response: MISS_NOFETCH");
              response = icpBuilder.makeMissNoFetch(message);
            }
          }
        }
        catch (IcpProtocolException ipe) {
          logger.debug2("Encountered an IcpProtocolException", ipe);
          logger.debug2("Response: ERR");
          try {
            // Last attempt to make something out of it
            response = icpBuilder.makeError(message);
          }
          catch (IcpProtocolException ipe2) {
            logger.warning(
                "Two consecutive IcpProtocolExceptions thrown; aborting", ipe2);
            return;
          }
        }

        // Send response
        try {
          icpSocket.send(response, message.getUdpAddress(), message.getUdpPort());
        }
        catch (NoRouteToHostException nrthe) {
          logger.warning("NoRouteToHostException while sending ICP response", nrthe);
          logger.debug("A NoRouteToHostException may indicate a problem related to packet filtering on the underlying platform.");
        }
        catch (IOException ioe) {
          logger.warning("IOException while sending ICP response", ioe);
        }
    }
    else {
      if (logger.isDebug3()) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Received a non-query ICP message from ");
        buffer.append(message.getUdpAddress());
        buffer.append(": ");
        buffer.append(message.toString());
        logger.debug3(buffer.toString());
      }
    }
  }

  public boolean isIcpServerAllowed() {
    return isIcpServerAllowed(Configuration.getCurrentConfig());
  }

  protected boolean isIcpServerAllowed(Configuration theConfig) {
    return theConfig.getBoolean(PARAM_PLATFORM_ICP_ENABLED, true);
  }

  /**
   * <p>Determines if the ICP server is currently running.</p>
   * @return True if and only if the ICP server is running.
   */
  public boolean isIcpServerRunning() {
    return icpSocket != null && icpSocket.isRunning();
  }

  /* Inherit documentation */
  public void setConfig(Configuration newConfig,
                        Configuration prevConfig,
                        Differences changedKeys) {
    // ICP rate limiter
    if (changedKeys.contains(PARAM_ICP_INCOMING_RATE)) {
      limiter = RateLimiter.getConfiguredRateLimiter(newConfig,
          limiter, PARAM_ICP_INCOMING_RATE, DEFAULT_ICP_INCOMING_RATE);
    }

    if (   changedKeys.contains(PARAM_PLATFORM_ICP_ENABLED)
        || changedKeys.contains(PARAM_PLATFORM_ICP_PORT)
        || changedKeys.contains(PARAM_ICP_ENABLED)
        || changedKeys.contains(PARAM_ICP_PORT)) {
      stopSocket();
      if (theDaemon.isDaemonInited() && shouldIcpServerStart(newConfig)) {
        startSocket(newConfig);
      }
    }
  }

  protected boolean shouldIcpServerStart() {
    return shouldIcpServerStart(Configuration.getCurrentConfig());
  }

  protected boolean shouldIcpServerStart(Configuration theConfig) {
    return    isIcpServerAllowed(theConfig)
           && theConfig.getBoolean(PARAM_ICP_ENABLED,
                                   theConfig.getBoolean(PARAM_PLATFORM_ICP_ENABLED,
                                                        false))
           && getCurrentPort(theConfig) > 0;
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
   * <p>Starts the ICP socket.</p>
   */
  protected void startSocket(Configuration theConfig) {
    try {
      logger.debug("startSocket in IcpManager: begin");

      port = getCurrentPort(theConfig);
      if (port <= 0) {
        logger.debug("startSocket() called but getCurrentPort() returns an invalid port number.");
        return; // don't start
      }

      resourceManager = getDaemon().getResourceManager();
      if (!resourceManager.reserveUdpPort(port, getClass())) {
        logger.error("Could not reserve UDP port " + port);
        throw new SocketException();
      }

      udpSocket = new DatagramSocket(port);
      icpFactory = IcpFactoryImpl.makeIcpFactory();
      icpBuilder = icpFactory.makeIcpBuilder();
      icpSocket = new IcpSocketImpl("IcpSocketImpl",
                                    udpSocket,
                                    icpFactory.makeIcpEncoder(),
                                    icpFactory.makeIcpDecoder(),
                                    this);
      icpSocket.addIcpHandler(this);
      limiter = RateLimiter.getConfiguredRateLimiter(
          Configuration.getCurrentConfig(), limiter,
          PARAM_ICP_INCOMING_RATE, DEFAULT_ICP_INCOMING_RATE);
      new Thread(icpSocket).start();
      logger.debug("startSocket in IcpManager: end");
    }
    catch (SocketException se) {
      forget(); // revert instantions
      logger.error("Could not start ICP socket", se);
    }
  }

  /**
   * <p>Stops the ICP socket.</p>
   */
  protected void stopSocket() {
    if (isIcpServerRunning()) {
      logger.debug2("stopSocket in IcpManager: action");
      icpSocket.requestStop();
      resourceManager.releaseUdpPort(port, getClass());
      forget(); // minimize footprint
    }
  }

  /**
   * <p>Sets several internals to null.</p>
   */
  private void forget() {
    icpBuilder = null;
    icpFactory = null;
    icpSocket = null;
    resourceManager = null;
    limiter = null;
    port = -1;
  }

  /**
   * <p>The default ICP enabled flag.</p>
   */
  public static final boolean DEFAULT_ICP_ENABLED = false;

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
   * <p>The default ICP rate-limiting string.</p>
   */
  private static final String DEFAULT_ICP_INCOMING_RATE =
    "50/1s";

  /**
   * <p>The ICP rate-limiting string parameter.</p>
   */
  private static final String PARAM_ICP_INCOMING_RATE =
  "org.lockss.proxy.icp.incomingRequestsPerSecond";

}
