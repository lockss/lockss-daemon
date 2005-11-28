/*
 * $Id: IcpSocketImpl.java,v 1.11 2005-11-28 17:17:34 thib_gc Exp $
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
import org.lockss.util.IPAddr;
import org.lockss.util.Logger;

/**
 * <p>An implementation of the {@link IcpSocket} interface, that is
 * customized to work closely with the ICP manager
 * ({@link IcpManager}).</p>
 * @author Thib Guicherd-Callin
 */
public class IcpSocketImpl extends LockssRunnable implements IcpSocket {

  /**
   * <p>An ICP decoder.</p>
   */
  private IcpDecoder decoder;

  /**
   * <p>An ICP encoder.</p>
   */
  private IcpEncoder encoder;

  /**
   * <p>A flag indicating that externally, the thread should go
   * on.</p>
   */
  private boolean goOn;

  /**
   * <p>A list of ICP handlers.</p>
   */
  private ArrayList handlers;

  /**
   * <p>A back reference to the ICP manager.</p>
   */
  private IcpManager icpManager;

  /**
   * <p>A UDP socket for use by this ICP thread.</p>
   */
  private DatagramSocket socket;

  /**
   * <p>Builds a new ICP socket with the given arguments.</p>
   * @param name       A name for the {@link LockssRunnable} thread.
   * @param socket     A UDP socket ready for a call to
   *                   {@link DatagramSocket#receive(DatagramPacket)}.
   * @param factory    An ICP factory.
   * @param icpManager A back reference to the ICP manager.
   */
  public IcpSocketImpl(String name,
                       DatagramSocket socket,
                       IcpFactory factory,
                       IcpManager icpManager) {
    // Super constructor
    super(name);

    // Initialize
    this.socket = socket;
    this.encoder = factory.makeIcpEncoder();
    this.decoder = factory.makeIcpDecoder();
    this.handlers = new ArrayList();
    this.icpManager = icpManager;
    this.goOn = false;

    // Log
    logger.debug2("constructor in IcpSocketImpl: end");
  }

  /* Inherit documentation */
  public void addIcpHandler(IcpHandler handler) {
    synchronized (handlers) {
      if (!handlers.contains(handler)) {
        handlers.add(handler);
      }
    }
  }

  /* Inherit documentation */
  public int countIcpHandlers() {
    return handlers.size();
  }

  /**
   * <p>Determines if this socket is running.</p>
   * @return True if and only if this socket is running.
   */
  public boolean isRunning() {
    return goOn;
  }

  /* Inherit documentation */
  public void removeIcpHandler(IcpHandler handler) {
    synchronized (handlers) {
      handlers.remove(handler);
    }
  }

  /**
   * <p>Asks this thread to stop listening for incoming ICP messages
   * and to exit cleanly <em>after closing the socket</em>.</p>
   */
  public void requestStop() {
    goOn = false;
    if (socket != null && !socket.isClosed()) {
      socket.close();
    }
  }

  /* Inherit documentation */
  public void send(IcpMessage message,
                   IPAddr recipient)
      throws IOException {
    send(message, recipient, IcpMessage.ICP_PORT);
  }

  /* Inherit documentation */
  public void send(IcpMessage message,
                   IPAddr recipient,
                   int port)
      throws IOException {
    DatagramPacket packet = encoder.encode(message, recipient, port);
    if (logger.isDebug3()) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("Sending the following message to ");
      buffer.append(recipient.toString());
      buffer.append(':');
      buffer.append(port);
      buffer.append(": ");
      buffer.append(message.toString());
      logger.debug3(buffer.toString());
    }

    socket.send(packet);
  }

  /* Inherit documentation */
  protected void lockssRun() {
    // Set up
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
    boolean somethingBadHappened = false;

    try {
      // Set up socket timeout
      socket.setSoTimeout((int)interval / 2); // may throw
      startWDog("icp", interval);
      nowRunning();

      // Main loop
      while (goOn && !somethingBadHappened) {
        try {
          logger.debug2("lockssRun in IcpSocketImpl: listening");
          packet = new DatagramPacket(buffer, buffer.length);
          socket.receive(packet);
          logger.debug3("lockssRun in IcpSocketImpl: receive returned");
          if (icpManager.getLimiter().isEventOk()) {
            icpManager.getLimiter().event();
            message = decoder.parseIcp(packet);
            if (logger.isDebug3()) {
              StringBuffer sb = new StringBuffer();
              sb.append("Received the following message: ");
              sb.append(message.toString());
              logger.debug3(sb.toString());
            }
            else if (logger.isDebug2()) {
              StringBuffer sb = new StringBuffer();
              sb.append("Received from ");
              sb.append(message.getUdpAddress());
              sb.append(':');
              sb.append(message.getUdpPort());
              logger.debug2(sb.toString());
            }
            notifyHandlers(message);
          }
          else {
            logger.debug3("lockssRun in IcpSocketImpl: rate limiter");
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
          if (goOn) {
            logger.warning("lockssRun in IcpSocketImpl: IOException", ioe);
          }
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
      // Clean up
      stopWDog();
      if (somethingBadHappened) {
        logger.warning("somethingBadHappened is true");
      }
      logger.debug2("lockssRun in IcpSocketImpl: end");
    }
  }

  /* Inherit documentation */
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

  /**
   * <p>Notifies all the handlers of the arrival of a new icoming
   * ICP message.</p>
   * @param message The decoded ICP message.
   */
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

  /**
   * <p>The default ICP thread priority.</p>
   */
  private static final int DEFAULT_ICP_THREAD_PRIORITY =
    Thread.NORM_PRIORITY + 2;

  /**
   * <p>The default ICP watchdog interval.</p>
   */
  private static final long DEFAULT_ICP_WDOG_INTERVAL = Constants.HOUR;

  /**
   * <p>A logger for use by instances of this class.</p>
   */
  private static Logger logger = Logger.getLogger("IcpSocketImpl");

  /**
   * <p>The ICP thread priority parameter.</p>
   */
  private static final String PARAM_ICP_THREAD_PRIORITY =
    "org.lockss.thread.icp.priority";

  /**
   * <p>The ICP watchdog interval parameter.</p>
   */
  private static final String PARAM_ICP_WDOG_INTERVAL =
    "org.lockss.thread.icp.watchdog.interval";

}
