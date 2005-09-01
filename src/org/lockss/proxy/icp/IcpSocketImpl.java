/*
 * $Id: IcpSocketImpl.java,v 1.4 2005-09-01 01:45:59 thib_gc Exp $
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
import java.util.ArrayList;
import java.util.Iterator;

import org.lockss.config.Configuration;
import org.lockss.daemon.LockssRunnable;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

public class IcpSocketImpl
    extends LockssRunnable
    implements IcpSocket, IcpHandler {

  private IcpDecoder decoder;
  
  private IcpEncoder encoder;
  
  private boolean goOn;
  
  private ArrayList handlers;
  
  private IcpManager icpManager;
  
  private Logger logger;
  
  private DatagramSocket socket;
  
  public IcpSocketImpl(String name,
                       DatagramSocket socket,
                       IcpEncoder encoder,
                       IcpDecoder decoder,
                       IcpManager icpManager) {
    // Super constructor
    super(name);
    
    // Initialize
    this.socket = socket;
    this.encoder = encoder;
    this.decoder = decoder;
    this.handlers = new ArrayList();
    this.icpManager = icpManager;
    
    // Loggers
    this.logger = Logger.getLogger("IcpSocketImpl-default");
    
    // Log
    this.logger.debug2("constructor in IcpSocketImpl: end");
  }

  public void addIcpHandler(IcpHandler handler) {
    synchronized (handlers) {
      if (!handlers.contains(handler)) {
        handlers.add(handler);
      }
    }
  }

  public int countIcpHandlers() {
    return handlers.size();
  }

  public void icpReceived(IcpReceiver source, IcpMessage message) {
    if (logger.isDebug3()) {
      logger.debug3(message.toString());
    }
  }
  
  public void removeIcpHandler(IcpHandler handler) {
    synchronized (handlers) {
      handlers.remove(handler);
    }
  }

  public void requestStop() {
    goOn = false;
    if (socket != null && !socket.isClosed()) {
      socket.close();
    }
  }

  public void send(IcpMessage message,
                   InetAddress recipient)
      throws IOException {
    send(message, recipient, IcpMessage.ICP_PORT);
  }
  
  public void send(IcpMessage message,
                   InetAddress recipient,
                   int port)
      throws IOException {
    DatagramPacket packet = encoder.encode(message, recipient, port);
    if (logger.isDebug3()) {
      logger.debug3(message.toString());
    }
    socket.send(packet);
  }

  protected void lockssRun() {
    logger.debug2("lockssRun in IcpSocketImpl: begin");
    long interval =
      Configuration.getLongParam(PARAM_ICP_WDOG_INTERVAL,
                                 DEFAULT_ICP_WDOG_INTERVAL);
    byte[] buffer = new byte[IcpMessage.MAX_LENGTH];
    goOn = true;
    DatagramPacket packet = null;
    IcpMessage message = null;
    setPriority("icp",
        Configuration.getIntParam(PARAM_ICP_THREAD_PRIORITY,
                                  DEFAULT_ICP_THREAD_PRIORITY));
    nowRunning();
    
    try {
      // Set up socket timeout
      socket.setSoTimeout((int)interval / 2); // may throw
      addIcpHandler(this);
      startWDog("icp", interval);

      // Main loop
      boolean somethingBadHappened = false;
      while (goOn && !somethingBadHappened) {
        try {
          logger.debug2("lockssRun in IcpSocketImpl: listening");
          packet = new DatagramPacket(buffer, buffer.length);
          socket.receive(packet);
          if (icpManager.getLimiter().isEventOk()) {
            icpManager.getLimiter().event();
            message = decoder.parseIcp(packet);
            notifyHandlers(message);
          }
        }
        catch (SocketTimeoutException ste) {
          // drop down to finally
        }
        catch (IcpProtocolException ipe) {
          if (logger.isDebug3()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Bad ICP packet from ");
            sb.append(message.getUdpAddress());
            sb.append(": ");
            sb.append(message.toString());
            logger.debug3(sb.toString(), ipe);
          }
        }
        catch (IOException ioe) {
          logger.warning("lockssRun in IcpSocketImpl: IOException", ioe);
          somethingBadHappened = goOn && socket.isClosed();
        }
        finally {
          pokeWDog();
        }
      }
      
      triggerWDogOnExit(somethingBadHappened);
    }
    catch (SocketException se) {
      logger.warning("Could not set socket timeout", se);
    }
    finally {
      removeIcpHandler(this);
      stopWDog();
      logger.debug2("lockssRun in IcpSocketImpl: end");
    }
  }

  protected void threadExited() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("The thread exited unexpectedly. Typically this is ");
    buffer.append("caused by the socket being closed at an unexpected ");
    buffer.append("time, for example as a result of an IOException ");
    buffer.append("or because the socket was closed before a call to ");
    buffer.append("requestStop. The IcpManager will now be asked to ");
    buffer.append("stop, then start.");
    logger.error(buffer.toString());
    try {
      // FIXME: creates too many threads?
      icpManager.stopService();
      icpManager.startService();
    }
    catch (Exception exc) {
      logger.error("Could not restart the ICP manager", exc);
    }
  }

  private void notifyHandlers(IcpMessage message) {
    synchronized (handlers) {
      for (Iterator iter = handlers.iterator() ; iter.hasNext() ; ) {
        // JAVA5: foreach
        try {
          IcpHandler handler = (IcpHandler)iter.next();
          handler.icpReceived(this, message);
        }
        catch (Exception exc) {
          if (logger.isDebug3()) {
            logger.debug3("Exception thrown by handler", exc);
          }
        }
      }
    }
  }
  
  private static final int DEFAULT_ICP_THREAD_PRIORITY =
    Thread.NORM_PRIORITY + 1;
  
  private static final long DEFAULT_ICP_WDOG_INTERVAL = Constants.HOUR;
  
  private static final String PARAM_ICP_THREAD_PRIORITY =
    "org.lockss.thread.icp.priority";
  
  private static final String PARAM_ICP_WDOG_INTERVAL =
    "org.lockss.thread.icp.watchdog.interval";
  
}
