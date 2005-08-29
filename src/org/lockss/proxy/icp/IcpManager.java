/*
 * $Id: IcpManager.java,v 1.3 2005-08-29 17:31:17 thib_gc Exp $
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

import java.net.DatagramSocket;
import java.net.SocketException;

import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.LockssAppException;
import org.lockss.config.Configuration;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginManager;
import org.lockss.util.Logger;

public class IcpManager
    extends BaseLockssDaemonManager
    implements IcpHandler {

  private IcpSocketImpl icpSocket;
  
  private DatagramSocket udpSocket;
  
  private PluginManager pluginManager;
  
  private IcpFactory icpFactory;
  
  private IcpBuilder icpBuilder;
  
  public void startService() {
    super.startService();
    
    icpFactory = IcpFactoryImpl.makeIcpFactory();
    icpBuilder = icpFactory.makeIcpBuilder(); 
    
    try {
      int port = Configuration.getIntParam(PARAM_ICP_PORT,
                                           DEFAULT_ICP_PORT);
      udpSocket = new DatagramSocket(port);
      icpSocket = new IcpSocketImpl("IcpSocketImpl",
                                    udpSocket,
                                    icpFactory.makeIcpEncoder(),
                                    icpFactory.makeIcpDecoder());
    }
    catch (SocketException se) {
      throw new LockssAppException("Could not open UDP socket for ICP");
    }
    
    pluginManager = getDaemon().getPluginManager();
    icpSocket.addIcpHandler(this);
    new Thread(icpSocket).start();
  }

  public void stopService() {
    icpSocket.requestStop();
    super.stopService();
  }

  private static final int DEFAULT_ICP_PORT = IcpMessage.ICP_PORT;
  
  private static Logger logger = Logger.getLogger("IcpManager");
  
  private static final String PARAM_ICP_PORT =
    "org.lockss.proxy.icp.port";

  public void icpReceived(IcpReceiver source, IcpMessage message) {
    if (message.getOpcode() == IcpMessage.ICP_OP_QUERY) {
      try {
        String urlString = message.getPayloadUrl().toString();
        CachedUrl cu = pluginManager.findOneCachedUrl(urlString);
        IcpMessage response;
        if (cu == null) {
          response = icpBuilder.makeMissNoFetch(message);
        }
        else if (!cu.hasContent()) {
          response = icpBuilder.makeMiss(message);
        }
        else {
          response = icpBuilder.makeHit(message);
        }
        icpSocket.send(response, message.getUdpAddress(), message.getUdpPort());
      }
      catch (Exception exc) {
        logger.warning("Exception in icpReceived", exc);
      }
    }
  }
  
}
