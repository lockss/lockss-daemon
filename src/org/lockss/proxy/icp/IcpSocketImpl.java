/**
 * 
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
import org.lockss.util.RateLimiter;

public class IcpSocketImpl
    extends LockssRunnable
    implements IcpSocket, IcpHandler {

  private IcpDecoder decoder;
  
  private IcpEncoder encoder;
  
  private boolean goOn;
  
  private ArrayList handlers;
  
  private Logger inLogger;
  
  private Logger logger;
  
  private Logger outLogger;
  
  private DatagramSocket socket;
  
  private RateLimiter limiter;
  
  public IcpSocketImpl(String name,
                       DatagramSocket socket,
                       IcpEncoder encoder,
                       IcpDecoder decoder) {
    // Super constructor
    super(name);
    
    // Initialize
    this.socket = socket;
    this.encoder = encoder;
    this.decoder = decoder;
    this.handlers = new ArrayList();
    this.limiter = new RateLimiter(
        Configuration.getIntParam(PARAM_ICP_INCOMING_PER_SECOND,
                                  DEFAULT_ICP_INCOMING_PER_SECOND),
        Constants.SECOND
    );

    // Loggers
    this.logger = Logger.getLogger("IcpSocketImpl-default");
    this.inLogger = Logger.getLogger("IcpScoketImpl-in");
    this.outLogger = Logger.getLogger("IcpSocketImpl-out");
    
    // Log
    if (this.logger.isDebug2()) {
      this.logger.debug2("constructor in IcpManager.IcpSocketImpl: end");
    }
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
    if (inLogger.isDebug3()) {
      inLogger.debug3(message.toString());
    }
  }
  
  public void removeIcpHandler(IcpHandler handler) {
    synchronized (handlers) {
      if (handlers.contains(handler)) {
        handlers.remove(handler);
      }
    }
  }

  public void requestStop() {
    goOn = false;
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
    if (outLogger.isDebug3()) {
      outLogger.debug3(message.toString());
    }
    socket.send(packet);
  }

  protected void lockssRun() {
    long interval =
      Configuration.getLongParam(PARAM_ICP_WDOG_INTERVAL,
                                 DEFAULT_ICP_WDOG_INTERVAL);
    
    if (logger.isDebug2()) {
      logger.debug2("lockssRun in IcpManager.IcpSocketImpl: begin");
    }
    
    try {
      socket.setSoTimeout((int)interval / 2);
    }
    catch (SocketException se) {
      logger.warning("Could not set socket timeout", se);
      return;
    }

    byte[] buffer = new byte[IcpMessage.MAX_LENGTH];
    goOn = true;
    DatagramPacket packet = null;
    IcpMessage message = null;
    addIcpHandler(this);
    nowRunning();
    startWDog("icp", interval);
    setPriority("icp",
                Configuration.getIntParam(PARAM_ICP_THREAD_PRIORITY,
                                          DEFAULT_ICP_THREAD_PRIORITY));
    
    while (goOn) {
      try {
        if (logger.isDebug2()) {
          logger.debug2("lockssRun in IcpManager.IcpSocketImpl: listening");
        }
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        if (limiter.isEventOk()) {
          limiter.event();
          message = decoder.parseIcp(packet);
          notifyHandlers(message);
        }
      }
      catch (SocketTimeoutException ste) {
        // drop down to finally
      }
      catch (IcpProtocolException ipe) {
        if (inLogger.isDebug3()) {
          inLogger.debug3("Bad ICP packet from " + packet.getAddress(), ipe);
        }
      }
      catch (IOException ioe) {
        logger.warning("lockssRun in IcpManager.IcpSocketImpl: IOException", ioe);
        goOn = !socket.isClosed();
      }
      finally {
        pokeWDog();
      }
    }
  
    stopWDog();
    if (logger.isDebug2()) {
      logger.warning("lockssRun in IcpManager.IcpSocketImpl: end");
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
  
  private static final int DEFAULT_ICP_INCOMING_PER_SECOND = 50;

  private static final long DEFAULT_ICP_WDOG_INTERVAL = Constants.HOUR;
  
  private static final String PARAM_ICP_INCOMING_PER_SECOND =
    "org.lockss.proxy.icp.incomingRequestsPerSecond";
  
  private static final String PARAM_ICP_WDOG_INTERVAL =
    "org.lockss.thread.icp.watchdog.interval";
  
  private static final String PARAM_ICP_THREAD_PRIORITY =
    "org.lockss.thread.icp.priority";
  
  private static final int DEFAULT_ICP_THREAD_PRIORITY =
    Thread.NORM_PRIORITY + 1;
  
}
