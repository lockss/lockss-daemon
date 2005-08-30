/*
 * $Id: IcpManager.java,v 1.5 2005-08-30 19:03:15 thib_gc Exp $
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
import java.net.DatagramSocket;
import java.net.SocketException;

import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.app.LockssAppException;
import org.lockss.config.Configuration;
import org.lockss.config.Configuration.Differences;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginManager;
import org.lockss.util.Logger;

public class IcpManager
    extends BaseLockssDaemonManager
    implements ConfigurableManager, IcpHandler {

  private IcpBuilder icpBuilder;
  
  private IcpFactory icpFactory;
  
  private IcpSocketImpl icpSocket;
  
  private PluginManager pluginManager;
  
  private DatagramSocket udpSocket;
  
  public void icpReceived(IcpReceiver source, IcpMessage message) {
    if (message.getOpcode() == IcpMessage.ICP_OP_QUERY) {
      try {
        String urlString = message.getPayloadUrl().toString();
        CachedUrl cu = pluginManager.findOneCachedUrl(urlString);
        IcpMessage response;
        try {
          if (cu != null && cu.hasContent()) {
            response = icpBuilder.makeHit(message);
          }
          else {
            response = icpBuilder.makeMissNoFetch(message);
          }
        }
        catch (IcpProtocolException ipe) {
          try {
            response = icpBuilder.makeError(message); // Bad query 
          }
          catch (IcpProtocolException ipe2) {
            throw new IOException(); // bail out
          }
        }
        icpSocket.send(response, message.getUdpAddress(), message.getUdpPort());
      }
      catch (IOException ioe) {
        logger.debug2("Cannot process ICP message: " + message.toString(), ioe);
      }
    }
  }

  public void setConfig(Configuration newConfig,
                        Configuration prevConfig,
                        Differences changedKeys) {
    if (   changedKeys.contains(PARAM_ICP_ENABLED)
        || changedKeys.contains(PARAM_ICP_PORT)) {
      boolean enable = newConfig.getBoolean(PARAM_ICP_ENABLED,
                                            DEFAULT_ICP_ENABLED);
      stopSocket();
      if (enable) {
        startSocket();
      }
    }
  }

  public void startService() {
    super.startService();
    boolean start = Configuration.getBooleanParam(PARAM_ICP_ENABLED,
                                                  DEFAULT_ICP_ENABLED);
    if (start) {
      startSocket();
    }
  }
  
  public void stopService() {
    icpSocket.requestStop();
    super.stopService();
  }
  
  private void forget() {
    icpSocket = null;
    icpFactory = null;
    icpBuilder = null; 
    pluginManager = null;
  }
  
  private void startSocket() {
    if (isAppInited()) {
      try {
        icpFactory = IcpFactoryImpl.makeIcpFactory();
        icpBuilder = icpFactory.makeIcpBuilder(); 
        int port = Configuration.getIntParam(PARAM_ICP_PORT,
                                             DEFAULT_ICP_PORT);
        udpSocket = new DatagramSocket(port);
        icpSocket = new IcpSocketImpl("IcpSocketImpl",
                                      udpSocket,
                                      icpFactory.makeIcpEncoder(),
                                      icpFactory.makeIcpDecoder());
        pluginManager = getDaemon().getPluginManager();
        icpSocket.addIcpHandler(this);
        new Thread(icpSocket).start();
      }
      catch (SocketException se) {
        forget(); // revert instantions
        throw new LockssAppException("Could not open UDP socket for ICP");
      }
    }
  }
  
  private void stopSocket() {
    if (icpSocket != null) {
      icpSocket.requestStop();
      forget(); // minimize footprint
    }
  }

  public static final boolean DEFAULT_ICP_ENABLED = false;

  public static final int DEFAULT_ICP_PORT = IcpMessage.ICP_PORT;
  
  public static final String PARAM_ICP_ENABLED =
    "org.lockss.proxy.icp.enabled";
  
  public static final String PARAM_ICP_PORT =
    "org.lockss.proxy.icp.port";
  
  private static Logger logger = Logger.getLogger("IcpManager");

}
