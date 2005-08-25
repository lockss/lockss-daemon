/**
 * 
 */
package org.lockss.proxy.icp;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;

import org.lockss.daemon.LockssRunnable;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

public class IcpSocketImpl
    extends LockssRunnable
    implements IcpSocket, IcpHandler {

  private Logger logger;
  
  private boolean debug2;
  
  private boolean debug3;
  
  private IcpDecoder decoder;
  
  private IcpEncoder encoder;
  
  private boolean goOn;
  
  private ArrayList handlers;
  
  private DatagramSocket socket;
  
  public IcpSocketImpl(String name,
                       DatagramSocket socket,
                       IcpEncoder encoder,
                       IcpDecoder decoder) {
    this(name,
         Logger.getLogger("IcpSocketImpl_default"),
         socket,
         encoder,
         decoder);
  }
      
  
  public IcpSocketImpl(String name,
                       Logger logger,
                       DatagramSocket socket,
                       IcpEncoder encoder,
                       IcpDecoder decoder) {
    // Super constructor
    super(name);
    
    // Initialize
    this.logger = logger;
    this.socket = socket;
    this.encoder = encoder;
    this.decoder = decoder;
    this.handlers = new ArrayList();

    this.debug2 = this.logger.isDebug2();
    this.debug3 = this.logger.isDebug3();
    triggerWDogOnExit(true);
    
    // Log
    if (debug2) {
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
    logger.debug3(message.toString());
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
    socket.send(packet);  
  }

  protected void lockssRun() {
    if (debug2) {
      logger.debug2("lockssRun in IcpManager.IcpSocketImpl: begin");
    }
    
    try {
      socket.setSoTimeout((int)DEFAULT_ICP_WDOG_INTERVAL / 2);
    }
    catch (SocketException se) {
      logger.warning("Could not set socket timeout", se);
      return;
    }

    byte[] buffer = new byte[IcpMessage.MAX_LENGTH];
    goOn = true;
    DatagramPacket packet = null;
    IcpMessage message = null;

    if (debug3) {
      addIcpHandler(this);
    }
    nowRunning();
    startWDog("icp", DEFAULT_ICP_WDOG_INTERVAL);
    
    while (goOn) {
      try {
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        message = decoder.parseIcp(packet);
        notifyHandlers(message);
      }
      catch (SocketTimeoutException ste) {
        // drop down to finally
      }
      catch (IcpProtocolException ipe) {
        if (debug3) {
          logger.debug3("Bad ICP packet from " + packet.getAddress(), ipe);
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
    if (debug2) {
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
        catch (Exception e) {}
      }
    }
  }
  
  private static final long DEFAULT_ICP_WDOG_INTERVAL = Constants.HOUR;

  private static final String PARAM_ICP_WDOG_INTERVAL =
    "org.lockss.thread.icp.watchdog.interval";
  
}