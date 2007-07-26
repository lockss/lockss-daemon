/*
 * $Id: BlockingStreamComm.java,v 1.27 2007-07-26 03:43:49 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;
import java.util.*;

import EDU.oswego.cs.dl.util.concurrent.*;

import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.app.*;
import org.lockss.poller.*;

/**
 * BlockingStreamComm implements the streaming mesaage protocol using
 * blocking sockets.
 */
public class BlockingStreamComm
  extends BaseLockssDaemonManager
  implements ConfigurableManager, LcapStreamComm, PeerMessage.Factory {

  static Logger log = Logger.getLogger("SComm");

  public static final String SERVER_NAME = "StreamComm";

  /** Use V3 over SSL **/
  public static final String PARAM_USE_V3_OVER_SSL = PREFIX + "v3OverSsl";
  static final boolean DEFAULT_USE_V3_OVER_SSL = false;

  /** Use client authentication for SSL **/
  public static final String PARAM_USE_SSL_CLIENT_AUTH = PREFIX + "SslClientAuth";
  static final boolean DEFAULT_USE_SSL_CLIENT_AUTH = true;

  /** Use temporary SSL keystore**/
  public static final String PARAM_SSL_TEMP_KEYSTORE = PREFIX + "SslTempKeystore";
  static final boolean DEFAULT_SSL_TEMP_KEYSTORE = true;

  /** File name for SSL key store **/
  public static final String PARAM_SSL_KEYSTORE = PREFIX + "SslKeyStore";
  static final String DEFAULT_SSL_KEYSTORE = ".keystore";

  /** File name for SSL key store password **/
  public static final String PARAM_SSL_PRIVATE_KEY_PASSWORD_FILE = PREFIX + "SslPrivateKeyPasswordFile";
  static final String DEFAULT_SSL_PRIVATE_KEY_PASSWORD_FILE = ".password";

  /** SSL protocol to use **/
  public static final String PARAM_SSL_PROTOCOL = PREFIX + "SslProtocol";
  static final String DEFAULT_SSL_PROTOCOL = "TLSv1";

  /** Max peer channels.  Only affects outgoing messages; incoming
   * connections are always accepted. */
  public static final String PARAM_MAX_CHANNELS =
    PREFIX + "maxChannels";
  static final int DEFAULT_MAX_CHANNELS = 50;

  /** Min threads in channel thread pool */
  public static final String PARAM_CHANNEL_THREAD_POOL_MIN =
    PREFIX + "threadPool.min";
  static final int DEFAULT_CHANNEL_THREAD_POOL_MIN = 3;

  /** Max threads in channel thread pool */
  public static final String PARAM_CHANNEL_THREAD_POOL_MAX =
    PREFIX + "threadPool.max";
  static final int DEFAULT_CHANNEL_THREAD_POOL_MAX = 3 * DEFAULT_MAX_CHANNELS;

  /** Duration after which idle threads will be terminated..  -1 = never */
  public static final String PARAM_CHANNEL_THREAD_POOL_KEEPALIVE =
    PREFIX + "threadPool.keepAlive";
  static final long DEFAULT_CHANNEL_THREAD_POOL_KEEPALIVE =
    10 * Constants.MINUTE;

  /** Connect timeout */
  public static final String PARAM_CONNECT_TIMEOUT =
    PREFIX + "timeout.connect";
  static final long DEFAULT_CONNECT_TIMEOUT = 2 * Constants.MINUTE;

  /** Data timeout (SO_TIMEOUT), channel is aborted if read times out.
   * This should be disabled (zero) because the read side of a channel may
   * legitimately be idle for a long time (if the channel is sending), and
   * interrupted reads apparently cannot reliably be resumed.  If the
   * channel is truly idle, the send side should close it. */
  public static final String PARAM_DATA_TIMEOUT =
    PREFIX + "timeout.data";
  static final long DEFAULT_DATA_TIMEOUT = 0;

  /** Time after which idle channel will be closed */
  public static final String PARAM_CHANNEL_IDLE_TIME =
    PREFIX + "channelIdleTime";
  static final long DEFAULT_CHANNEL_IDLE_TIME = 2 * Constants.MINUTE;

  /** Interval at which send thread checks idle timer */
  public static final String PARAM_SEND_WAKEUP_TIME =
    PREFIX + "sendWakeupTime";
  static final long DEFAULT_SEND_WAKEUP_TIME = 1 * Constants.MINUTE;

  /** FilePeerMessage will be used for messages larger than this, else
   * MemoryPeerMessage */
  public static final String PARAM_MIN_FILE_MESSAGE_SIZE =
    PREFIX + "minFileMessageSize";
  static final int DEFAULT_MIN_FILE_MESSAGE_SIZE = 1024;

  /** FilePeerMessage will be used for messages larger than this, else
   * MemoryPeerMessage */
  public static final String PARAM_MAX_MESSAGE_SIZE =
    PREFIX + "maxMessageSize";
  static final int DEFAULT_MAX_MESSAGE_SIZE = 100 * 1024;

  /** Dir for PeerMessage data storage */
  public static final String PARAM_DATA_DIR = PREFIX + "messageDataDir";
  /** Default is PlatformInfo.getSystemTempDir() */
  static final String DEFAULT_DATA_DIR = "Platform tmp dir";

  /** Wrap Socket OutputStream in BufferedOutputStream? */
  public static final String PARAM_IS_BUFFERED_SEND = PREFIX + "bufferedSend";
  static final boolean DEFAULT_IS_BUFFERED_SEND = true;

  /** TCP_NODELAY */
  public static final String PARAM_TCP_NODELAY = PREFIX + "tcpNodelay";
  static final boolean DEFAULT_TCP_NODELAY = true;

  /** Amount of time BlockingStreamComm.stopService() should wait for
   * worker threads to exit.  Zero disables wait.  */
  public static final String PARAM_WAIT_EXIT = PREFIX + "waitExit";
  static final long DEFAULT_WAIT_EXIT = 2 * Constants.SECOND;

  static final String WDOG_PARAM_SCOMM = "SComm";
  static final long WDOG_DEFAULT_SCOMM = 1 * Constants.HOUR;

  static final String PRIORITY_PARAM_SCOMM = "SComm";
  static final int PRIORITY_DEFAULT_SCOMM = -1;

  static final String PRIORITY_PARAM_SLISTEN = "SListen";
  static final int PRIORITY_DEFAULT_SLISTEN = -1;

  static final String WDOG_PARAM_CHANNEL = "Channel";
  static final long WDOG_DEFAULT_CHANNEL = 30 * Constants.MINUTE;

  static final String PRIORITY_PARAM_CHANNEL = "Channel";
  static final int PRIORITY_DEFAULT_CHANNEL = -1;


  private boolean paramUseV3OverSsl = DEFAULT_USE_V3_OVER_SSL;
  private boolean paramSslClientAuth = DEFAULT_USE_SSL_CLIENT_AUTH;
  private boolean paramSslTempKeystore = DEFAULT_SSL_TEMP_KEYSTORE;
  private String paramSslKeyStore = DEFAULT_SSL_KEYSTORE;
  private String paramSslPrivateKeyPasswordFile =
    DEFAULT_SSL_PRIVATE_KEY_PASSWORD_FILE;
  private String paramSslProtocol = DEFAULT_SSL_PROTOCOL;
  private int paramMinFileMessageSize = DEFAULT_MIN_FILE_MESSAGE_SIZE;
  private int paramMaxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;
  private File dataDir = null;
  private int paramBacklog = DEFAULT_LISTEN_BACKLOG;
  private int paramMaxChannels = DEFAULT_MAX_CHANNELS;
  private int paramMinPoolSize = DEFAULT_CHANNEL_THREAD_POOL_MIN;
  private int paramMaxPoolSize = DEFAULT_CHANNEL_THREAD_POOL_MAX;
  private long paramPoolKeepaliveTime = DEFAULT_CHANNEL_THREAD_POOL_KEEPALIVE;
  private long paramConnectTimeout = DEFAULT_CONNECT_TIMEOUT;
  private long paramSoTimeout = DEFAULT_DATA_TIMEOUT;
  private long paramSendWakeupTime = DEFAULT_SEND_WAKEUP_TIME;
  private long paramChannelIdleTime = DEFAULT_CHANNEL_IDLE_TIME;
  private boolean paramIsBufferedSend = DEFAULT_IS_BUFFERED_SEND;
  private boolean paramIsTcpNodelay = DEFAULT_TCP_NODELAY;
  private long paramWaitExit = DEFAULT_WAIT_EXIT;
  private long lastHungCheckTime = 0;
  private PooledExecutor pool;
  protected SSLSocketFactory sslSocketFactory = null;
  protected SSLServerSocketFactory sslServerSocketFactory = null;
  private String paramSslKeyStorePassword = null;

  private boolean enabled = DEFAULT_ENABLED;
  private boolean running = false;

  private SocketFactory sockFact;
  private ServerSocket listenSock;
  private PeerIdentity myPeerId;
  private PeerAddress.Tcp myPeerAddr;

  private IdentityManager idMgr;
  private OneShot configShot = new OneShot();

  private FifoQueue rcvQueue;	     // PeerMessages received from channels
  private ReceiveThread rcvThread;
  private ListenThread listenThread;
  // Synchronization lock for rcv thread, listen thread manipulations
  private Object threadLock = new Object();

  // Maps PeerIdentity to primary PeerChannel (and used as lock for both maps)
  MaxSizeRecordingMap channels = new MaxSizeRecordingMap();
  // Maps PeerIdentity to secondary PeerChannel
  MaxSizeRecordingMap rcvChannels = new MaxSizeRecordingMap();

  private Vector messageHandlers = new Vector(); // Vector is synchronized

  public BlockingStreamComm() {
    sockFact = null;
  }

  /**
   * start the stream comm manager.
   */
  public void startService() {
    super.startService();
    idMgr = getDaemon().getIdentityManager();
    resetConfig();
    try {
      myPeerId = getLocalPeerIdentity();
    } catch (Exception e) {
      log.critical("No V3 identity, not starting stream comm", e);
      enabled = false;
      return;
    }
    log.debug("Local V3 peer: " + myPeerId);
    try {
      PeerAddress pad = myPeerId.getPeerAddress();
      if (pad instanceof PeerAddress.Tcp) {
	myPeerAddr = (PeerAddress.Tcp)pad;
      } else {
	log.error("Disabling stream comm; no local TCP peer address: " + pad);
	enabled = false;
      }
    } catch (IdentityManager.MalformedIdentityKeyException e) {
      log.error("Disabling stream comm; local address malformed", e);
      enabled = false;
    }
    if (enabled) {
      start();
//       getDaemon().getStatusService().registerStatusAccessor("SCommStats",
// 							    new Status());
    }
  }

  /**
   * stop the stream comm manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
//     getDaemon().getStatusService().unregisterStatusAccessor("SCommStats");
    if (running) {
      stop();
    }
    super.stopService();
  }


  /**
   * Set communication parameters from configuration, once only.
   * This service currently cannot be reconfigured.
   * @param config the Configuration
   */
  public void setConfig(Configuration config,
			Configuration prevConfig,
			Configuration.Differences changedKeys) {
    if (getDaemon().isDaemonInited()) {
      // one-time only init
      if (configShot.once()) {
	configure(config, prevConfig, changedKeys);
      }
      // these params can be changed on the fly
      paramMinFileMessageSize = config.getInt(PARAM_MIN_FILE_MESSAGE_SIZE,
					      DEFAULT_MIN_FILE_MESSAGE_SIZE);
      paramMaxMessageSize = config.getInt(PARAM_MAX_MESSAGE_SIZE,
					  DEFAULT_MAX_MESSAGE_SIZE);
      paramIsBufferedSend = config.getBoolean(PARAM_IS_BUFFERED_SEND,
					      DEFAULT_IS_BUFFERED_SEND);
      paramIsTcpNodelay = config.getBoolean(PARAM_TCP_NODELAY,
					    DEFAULT_TCP_NODELAY);
      paramWaitExit = config.getTimeInterval(PARAM_WAIT_EXIT,
					     DEFAULT_WAIT_EXIT);

      if (changedKeys.contains(PARAM_DATA_DIR)) {
	String paramDataDir = config.get(PARAM_DATA_DIR,
					 PlatformUtil.getSystemTempDir());
	File dir = new File(paramDataDir);
	if (dir.exists() || dir.mkdirs()) {
	  dataDir = dir;
	  log.debug2("Message data dir: " + dataDir);
	} else {
	  log.warning("No message data dir: " + dir);
	  dataDir = null;
	}
      }

      paramMaxChannels = config.getInt(PARAM_MAX_CHANNELS,
				       DEFAULT_MAX_CHANNELS);
      paramConnectTimeout = config.getTimeInterval(PARAM_CONNECT_TIMEOUT,
						   DEFAULT_CONNECT_TIMEOUT);
      paramSoTimeout = config.getTimeInterval(PARAM_DATA_TIMEOUT,
					      DEFAULT_DATA_TIMEOUT);
      paramSendWakeupTime = config.getTimeInterval(PARAM_SEND_WAKEUP_TIME,
						   DEFAULT_SEND_WAKEUP_TIME);
      paramChannelIdleTime = config.getTimeInterval(PARAM_CHANNEL_IDLE_TIME,
						    DEFAULT_CHANNEL_IDLE_TIME);
    }
  }

  /** Internal config, so can invoke from test constructor  */
  void configure(Configuration config,
		 Configuration prevConfig,
		 Configuration.Differences changedKeys) {
    enabled = config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED);
    if (!enabled) {
      return;
    }
    paramMinPoolSize = config.getInt(PARAM_CHANNEL_THREAD_POOL_MIN,
				     DEFAULT_CHANNEL_THREAD_POOL_MIN);
    paramMaxPoolSize = config.getInt(PARAM_CHANNEL_THREAD_POOL_MAX,
				     DEFAULT_CHANNEL_THREAD_POOL_MAX);
    paramPoolKeepaliveTime =
      config.getTimeInterval(PARAM_CHANNEL_THREAD_POOL_KEEPALIVE,
			     DEFAULT_CHANNEL_THREAD_POOL_KEEPALIVE);
    if (changedKeys.contains(PARAM_USE_V3_OVER_SSL)) {
      paramUseV3OverSsl = config.getBoolean(PARAM_USE_V3_OVER_SSL,
					    DEFAULT_USE_V3_OVER_SSL);
      sockFact = null;
      // XXX shut down old listen socket, do exponential backoff
      // XXX on bind() to bring up new listen socket
      // XXX then move this to the "change on the fly" above
    }
    if (!paramUseV3OverSsl)
	return;
    // We're trying to use SSL
    if (changedKeys.contains(PARAM_USE_SSL_CLIENT_AUTH)) {
      paramSslClientAuth = config.getBoolean(PARAM_USE_SSL_CLIENT_AUTH,
					     DEFAULT_USE_SSL_CLIENT_AUTH);
      sockFact = null;
    }
    if (changedKeys.contains(PARAM_SSL_TEMP_KEYSTORE)) {
      paramSslTempKeystore = config.getBoolean(PARAM_SSL_TEMP_KEYSTORE,
					     DEFAULT_SSL_TEMP_KEYSTORE);
      sockFact = null;
    }
    if (sslServerSocketFactory != null && sslSocketFactory != null) {
      // already initialized
      return;
    }
    if (paramSslTempKeystore) {
      // We're using the temporary keystore
      paramSslKeyStore = System.getProperty("javax.net.ssl.keyStore", null);
      paramSslKeyStorePassword =
	System.getProperty("javax.net.ssl.keyStorePassword", null);
      log.debug("Using temporary keystore from " + paramSslKeyStore);
      // Now create the SSL socket factories from the context
      sslServerSocketFactory =
	(SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
      sslSocketFactory =
	(SSLSocketFactory)SSLSocketFactory.getDefault();
      return;
    }
    // We're using the real keystore
    if (changedKeys.contains(PARAM_SSL_KEYSTORE)) {
	paramSslKeyStore = config.get(PARAM_SSL_KEYSTORE,
				      DEFAULT_SSL_KEYSTORE);
	// The password for the keystore is the machine's FQDN.
	paramSslKeyStorePassword = config.get("org.lockss.platform.fqdn", "");
	log.debug("Using permanent keystore from " + paramSslKeyStore);
	sockFact = null;
    }
    byte[] sslPrivateKeyPassword = null;
    if (changedKeys.contains(PARAM_SSL_PRIVATE_KEY_PASSWORD_FILE)) {
      paramSslPrivateKeyPasswordFile = config.get(PARAM_SSL_PRIVATE_KEY_PASSWORD_FILE,
						  DEFAULT_SSL_PRIVATE_KEY_PASSWORD_FILE);
      sockFact = null;
    }
    if (changedKeys.contains(PARAM_SSL_PROTOCOL)) {
      paramSslProtocol = config.get(PARAM_SSL_PROTOCOL,
				    DEFAULT_SSL_PROTOCOL);
      sockFact = null;
    }
    try {
      File keyStorePasswordFile = new File(paramSslPrivateKeyPasswordFile);
      if (keyStorePasswordFile.exists()) {
	FileInputStream fis = new FileInputStream(keyStorePasswordFile);
	sslPrivateKeyPassword = new byte[(int)keyStorePasswordFile.length()];
	if (fis.read(sslPrivateKeyPassword) != sslPrivateKeyPassword.length) {
	  throw new IOException("short read");
	}
	fis.close();
	FileOutputStream fos = new FileOutputStream(keyStorePasswordFile);
	byte[] junk = new byte[(int)keyStorePasswordFile.length()];
	for (int i = 0; i < junk.length; i++)
	  junk[i] = 0;
	fos.write(junk);
	fos.close();
	keyStorePasswordFile.delete();
      } else {
        log.debug("SSL password file " + paramSslPrivateKeyPasswordFile + " missing");
	return;
      }
    } catch (IOException ex) {
      log.error(paramSslPrivateKeyPasswordFile + " threw " + ex);
      return;
    }
    // We now have a password to decrypt the private key in the
    // SSL keystore.  Next create the keystore from the file.
    KeyStore keyStore = null;
    InputStream fis = null;
    try {
      keyStore = KeyStore.getInstance("JCEKS");
      fis = new FileInputStream(paramSslKeyStore);
      keyStore.load(fis, paramSslKeyStorePassword.toCharArray());
    } catch (KeyStoreException ex) {
      log.error("loading SSL key store threw " + ex);
      return;
    } catch (IOException ex) {
      log.error("loading SSL key store threw " + ex);
      return;
    } catch (NoSuchAlgorithmException ex) {
      log.error("loading SSL key store threw " + ex);
      return;
    } catch (CertificateException ex) {
      log.error("loading SSL key store threw " + ex);
      return;
    } finally {
      IOUtil.safeClose(fis);
    }
    {
      String temp = new String(sslPrivateKeyPassword);
      logKeyStore(keyStore, temp.toCharArray());
    }
    // Now create a KeyManager from the keystore using the password.
    KeyManager[] kma = null;
    try {
      KeyManagerFactory kmf =
	KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keyStore, new String(sslPrivateKeyPassword).toCharArray());
      kma = kmf.getKeyManagers();
    } catch (NoSuchAlgorithmException ex) {
      log.error("creating SSL key manager threw " + ex);
      return;
    } catch (KeyStoreException ex) {
      log.error("creating SSL key manager threw " + ex);
      return;
    } catch (UnrecoverableKeyException ex) {
      log.error("creating SSL key manager threw " + ex);
      return;
    }
    // Now create a TrustManager from the keystore using the password
    TrustManager[] tma = null;
    try {
      TrustManagerFactory tmf = 
	TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(keyStore);
      tma = tmf.getTrustManagers();
    } catch (NoSuchAlgorithmException ex) {
      log.error("creating SSL trust manager threw " + ex);
      return;
    } catch (KeyStoreException ex) {
      log.error("creating SSL trust manager threw " + ex);
      return;
    } finally {
      // Now forget the password
      for (int i = 0; i < sslPrivateKeyPassword.length; i++) {
	sslPrivateKeyPassword[i] = 0;
      }
      sslPrivateKeyPassword = null;
    }
    // Now create an SSLContext from the KeyManager
    SSLContext sslContext = null;
    try {
      sslContext = SSLContext.getInstance(paramSslProtocol);
      sslContext.init(kma, tma, null);
      // Now create the SSL socket factories from the context
      sslServerSocketFactory = sslContext.getServerSocketFactory();
      sslSocketFactory = sslContext.getSocketFactory();
    } catch (NoSuchAlgorithmException ex) {
      log.error("Creating SSL context threw " + ex);
      sslContext = null;
    } catch (KeyManagementException ex) {
      log.error("Creating SSL context threw " + ex);
      sslContext = null;
    }
  }

  // private debug output of keystore
  private void logKeyStore(KeyStore ks, char[] privateKeyPassWord) {
    log.debug3("start of key store");
    try {
      for (Enumeration en = ks.aliases(); en.hasMoreElements(); ) {
        String alias = (String) en.nextElement();
	log.debug3("Next alias " + alias);
        if (ks.isCertificateEntry(alias)) {
	  log.debug3("About to Certificate");
          java.security.cert.Certificate cert = ks.getCertificate(alias);
          if (cert == null) {
            log.debug3(alias + " null cert chain");
          } else {
            log.debug3("Cert for " + alias + " is " + cert.toString());
          }
        } else if (ks.isKeyEntry(alias)) {
	  log.debug3("About to getKey");
  	  Key privateKey = ks.getKey(alias, privateKeyPassWord);
  	  log.debug3(alias + " key " + privateKey.getAlgorithm() + "/" + privateKey.getFormat());
        } else {
  	  log.debug3(alias + " neither key nor cert");
        }
      }
      log.debug3("end of key store");
    } catch (Exception ex) {
      log.error("logKeyStore() threw " + ex);
    }
  }

  // overridable for testing
  protected PeerIdentity getLocalPeerIdentity() {
    return idMgr.getLocalPeerIdentity(Poll.V3_PROTOCOL);
  }

  PeerIdentity findPeerIdentity(String idkey) {
    return idMgr.findPeerIdentity(idkey);
  }

  PeerIdentity getMyPeerId() {
    return myPeerId;
  }

  Queue getReceiveQueue() {
    return rcvQueue;
  }

  SocketFactory getSocketFactory() {
    if (sockFact == null)
      if (paramUseV3OverSsl) {
	sockFact = new SslSocketFactory();
      } else {
        sockFact = new NormalSocketFactory();
      }
    return sockFact;
  }

  long getConnectTimeout() {
    return paramConnectTimeout;
  }

  long getSoTimeout() {
    return paramSoTimeout;
  }

  long getSendWakeupTime() {
    return paramSendWakeupTime;
  }

  long getChannelIdleTime() {
    return paramChannelIdleTime;
  }

  long getChannelHungTime() {
    return paramChannelIdleTime + 1000;
  }

  int getMaxMessageSize() {
    return paramMaxMessageSize;
  }

  boolean isBufferedSend() {
    return paramIsBufferedSend;
  }

  boolean isTcpNodelay() {
    return paramIsTcpNodelay;
  }

  /**
   * Called by channel when it learns its peer's identity
   */
  void associateChannelWithPeer(BlockingPeerChannel chan, PeerIdentity peer) {
    synchronized (channels) {
      BlockingPeerChannel currentChan =
	(BlockingPeerChannel)channels.get(peer);
      if (currentChan == null) {
	channels.put(peer, chan);	// normal association
	log.debug2("Associated " + chan);
      } else if (currentChan == chan) {
	log.warning("Redundant peer-channel association (" + chan + ")");
      } else {
	BlockingPeerChannel rcvChan =
	  (BlockingPeerChannel)rcvChannels.get(peer);
	if (rcvChan == null) {
	  rcvChannels.put(peer, chan);	// normal secondary association
	  log.debug2("Associated secondary " + chan);
	} else if (rcvChan == chan) {
	  log.debug2("Redundant secondary peer-channel association(" +
		     chan +")");
	} else {
	  // maybe should replace if new working and old not.  but old will
	  // eventually timeout and close anyway
	  log.warning("Conflicting peer-channel association(" + chan +
		      "), was " + peer);
	}
      }
    }
  }

  /**
   * Called by channel when closing
   */
  void dissociateChannelFromPeer(BlockingPeerChannel chan, PeerIdentity peer) {
    synchronized (channels) {
      BlockingPeerChannel currentChan =
	(BlockingPeerChannel)channels.get(peer);
      if (currentChan == chan) {
	channels.remove(peer);
	log.debug2("Removed: " + chan);
      }
      BlockingPeerChannel rcvChan = (BlockingPeerChannel)rcvChannels.get(peer);
      if (rcvChan == chan) {
	rcvChannels.remove(peer);
	log.debug2("Removed secondary: " + chan);
      }
    }
  }

  /**
   * Return an existing channel for the peer or create and start one
   */
  BlockingPeerChannel findOrMakeChannel(PeerIdentity pid)
      throws IOException {
    synchronized (channels) {
      BlockingPeerChannel chan = (BlockingPeerChannel)channels.get(pid);
      if (chan != null) {
	return chan;
      }
      chan = (BlockingPeerChannel)rcvChannels.get(pid);
      if (chan != null) {
	// found secondary, no primary.  promote secondary to primary
	channels.put(pid, chan);
	rcvChannels.remove(pid);
	log.debug2("Promoted " + chan);
	return chan;
      }
      // new primary channel, if we have room
      if (channels.size() >= paramMaxChannels) {
	// need to maintain queue of messages waiting for active channel?
	throw new IOException("Too many open channels");
      }
      chan = getSocketFactory().newPeerChannel(this, pid);
      channels.put(pid, chan);
      log.debug2("Added " + chan);
      try {
	chan.startOriginate();
	return chan;
      } catch (IOException e) {
	log.warning("Can't make channel", e);
	channels.remove(pid);
	log.debug2("Removed " + chan);
	throw e;
      }
    }
  }

  /** Send a message to a peer.
   * @param msg the message to send
   * @param id the identity of the peer to which to send the message
   * @throws IOException if message couldn't be queued
   */
  public void sendTo(PeerMessage msg, PeerIdentity id, RateLimiter limiter)
      throws IOException {
    if (!running) throw new IllegalStateException("SComm not running");
    if (msg == null) throw new NullPointerException("Null message");
    if (id == null) throw new NullPointerException("Null peer");
    if (log.isDebug3()) log.debug3("sending "+ msg +" to "+ id);
    if (limiter == null || limiter.isEventOk()) {
      sendToChannel(msg, id);
      if (limiter != null) limiter.event();
    } else {
      log.debug2("Pkt rate limited");
    }
  }

  private void sendToChannel(PeerMessage msg, PeerIdentity id)
      throws IOException {
    // closing channel might refuse the message (return false), in which
    // case it will have removed itself so try again with a new channel
    BlockingPeerChannel last = null;
    for (int rpt = 0; rpt < 3; rpt++) {
      BlockingPeerChannel chan = findOrMakeChannel(id);
      if (last == chan)
	throw new IllegalStateException("Got same channel as last time: "
					+ chan);
      if (chan.send(msg)) {
	return;
      }
      if (!chan.wasOpen()) {
	log.warning("Couldn't start channel");
	return;
      }

      last = chan;
    }
    log.error("Couldn't enqueue msg after 3 tries: " + msg);
  }

  void start() {
    pool = new PooledExecutor(paramMaxPoolSize);
    pool.setMinimumPoolSize(paramMinPoolSize);
    pool.setKeepAliveTime(paramPoolKeepaliveTime);
    log.debug2("Channel thread pool min, max: " +
	      pool.getMinimumPoolSize() + ", " + pool.getMaximumPoolSize());
    pool.abortWhenBlocked();

    rcvQueue = new FifoQueue();
    try {
      int port = myPeerAddr.getPort();
      if (!getDaemon().getResourceManager().reserveTcpPort(port,
							   SERVER_NAME)) {
	throw new IOException("TCP port " + port + " unavailable");
      }
      log.debug("Listening on port " + port);
      listenSock =
	getSocketFactory().newServerSocket(port, paramBacklog);
    } catch (IOException e) {
      log.critical("Can't create listen socket", e);
      return;
    }
    ensureQRunner();
    ensureListener();
    running = true;
  }

  void ensureQRunner() {
    synchronized (threadLock) {
      if (rcvThread == null) {
	log.info("Starting receive thread");
	rcvThread = new ReceiveThread("SCommRcv: " +
				      myPeerId.getIdString());
	rcvThread.start();
	rcvThread.waitRunning();
      }
    }
  }

  void ensureListener() {
    synchronized (threadLock) {
      if (listenThread == null) {
	log.info("Starting listen thread");
	listenThread = new ListenThread("SCommListen: " +
					myPeerId.getIdString());
	listenThread.start();
	listenThread.waitRunning();
      }
    }
  }

  // stop all threads and channels
  void stop() {
    running = false;
    Deadline timeout = null;
    synchronized (threadLock) {
      if (paramWaitExit > 0) {
	timeout = Deadline.in(paramWaitExit);
      }
      ListenThread lth = listenThread;
      if (lth != null) {
	log.info("Stopping listen thread");
	lth.stopListenThread();
	if (timeout != null) {
	  lth.waitExited(timeout);
	}
	listenThread = null;
      }
      ReceiveThread rth = rcvThread;
      if (rth != null) {
	log.info("Stopping receive thread");
	rth.stopRcvThread();
	if (timeout != null) {
	  rth.waitExited(timeout);
	}
	rcvThread = null;
      }
    }
    stopChannels(channels, timeout);
    stopChannels(rcvChannels, timeout);
    log.debug2("shutting down pool");
    if (pool != null) {
      pool.shutdownNow();
    }
    log.debug2("pool shut down ");
  }

  // stop all channels in channel map
  void stopChannels(Map map, Deadline timeout) {
    List lst;
    synchronized (channels) {
      // make copy while map is locked
      lst = new ArrayList(map.values());
    }
    for (Iterator iter = lst.iterator(); iter.hasNext(); ) {
      BlockingPeerChannel chan = (BlockingPeerChannel)iter.next();
      chan.abortChannel();
    }
    // Wait until the threads have exited before proceeding.  Useful in
    // testing to keep debug output straight.

    // Any channels that had already dissociated themselves are not waited
    // for.  It would take extra bookkeeping to handle those and they don't
    // seem to cause nearly as much trouble.

    if (timeout != null) {
      for (Iterator iter = lst.iterator(); iter.hasNext(); ) {
	BlockingPeerChannel chan = (BlockingPeerChannel)iter.next();
	chan.waitThreadsExited(timeout);
      }
    }
  }

  // poke channels that might have hung sender
  void checkHungChannels() {
    log.debug3("Doing hung check");
    List lst;
    synchronized (channels) {
      // make copy while map is locked
      lst = new ArrayList(channels.values());
    }
    for (Iterator iter = lst.iterator(); iter.hasNext(); ) {
      BlockingPeerChannel chan = (BlockingPeerChannel)iter.next();
      chan.checkHung();
    }
  }

  /**
   * Execute the runnable in a pool thread
   * @param run the Runnable to be run
   * @throws RuntimeException if no pool thread is available
   */
  void execute(Runnable run) throws InterruptedException {
    if (run == null)
      log.warning("Executing null", new Throwable());
    pool.execute(run);
  }

  void XXXexecute(Runnable run) {
    try {
      if (run == null)
	log.warning("Executing null", new Throwable());
      pool.execute(run);
    } catch (InterruptedException e) {
      // Shouldn't happen in abortWhenBlocked mode
      log.warning("Shouldn't happen", e);
      throw new RuntimeException("InterruptedException in pool.excute(): " +
				 e.toString());
    }
  }

  // process a socket returned by accept()
  // overridable for testing
  void processIncomingConnection(Socket sock) throws IOException {
    if (sock.isClosed()) {
      throw new SocketException("socket closed during handshake");
    }
    log.debug2("Accepted connection from " +
	       new IPAddr(sock.getInetAddress()));
    BlockingPeerChannel chan = getSocketFactory().newPeerChannel(this, sock);
    chan.startIncoming();
  }

  private void processReceivedPacket(PeerMessage msg) {
    log.debug2("Received " + msg);
    try {
      runHandlers(msg);
    } catch (ProtocolException e) {
      log.warning("Cannot process incoming packet", e);
    }
  }

  protected void runHandler(MessageHandler handler,
			    PeerMessage msg) {
    try {
      handler.handleMessage(msg);
    } catch (Exception e) {
      log.error("callback threw", e);
    }
  }

  private void runHandlers(PeerMessage msg)
      throws ProtocolException {
    try {
      int proto = msg.getProtocol();
      MessageHandler handler;
      if (proto >= 0 && proto < messageHandlers.size() &&
	  (handler = (MessageHandler)messageHandlers.get(proto)) != null) {
	runHandler(handler, msg);
      } else {
	log.warning("Received message with unregistered protocol: " + proto);
      }
    } catch (RuntimeException e) {
      log.warning("Unexpected error in runHandlers", e);
      throw new ProtocolException(e.toString());
    }
  }

  /**
   * Register a {@link LcapStreamComm.MessageHandler}, which will be called
   * whenever a message is received.
   * @param protocol an int representing the protocol
   * @param handler MessageHandler to add
   */
  public void registerMessageHandler(int protocol, MessageHandler handler) {
    synchronized (messageHandlers) {
      if (protocol >= messageHandlers.size()) {
	messageHandlers.setSize(protocol + 1);
      }
      if (messageHandlers.get(protocol) != null) {
	throw
	  new RuntimeException("Protocol " + protocol + " already registered");
      }
      messageHandlers.set(protocol, handler);
    }
  }

  /**
   * Unregister a {@link LcapStreamComm.MessageHandler}.
   * @param protocol an int representing the protocol
   */
  public void unregisterMessageHandler(int protocol) {
    if (protocol < messageHandlers.size()) {
      messageHandlers.set(protocol, null);
    }
  }

  // PeerMessage.Factory implementation

  public PeerMessage newPeerMessage() {
    return new MemoryPeerMessage();
  }

  public PeerMessage newPeerMessage(int estSize) {
    if (estSize < 0) {
      return newPeerMessage();
    } else if (estSize > 0 &&
	       dataDir != null &&
	       estSize >= paramMinFileMessageSize) {
      return new FilePeerMessage(dataDir);
    } else {
      return new MemoryPeerMessage();
    }
  }

  protected void handShake(SSLSocket s) {
    SSLSession session = s.getSession();
    try {
      java.security.cert.Certificate[] certs = session.getPeerCertificates();
      log.debug(session.getPeerHost() + " via " + session.getProtocol() + " verified");
    } catch (SSLPeerUnverifiedException ex) {
      log.error(session.getPeerHost() + " threw " + ex);
      try {
        s.close();
      } catch (IOException ex2) {
	log.error("Socket close threw " + ex2);
      }
    }
  }


  // Receive thread
  private class ReceiveThread extends LockssThread {
    private volatile boolean goOn = true;
    private Deadline timeout = Deadline.in(getChannelHungTime());

    ReceiveThread(String name) {
      super(name);
    }

    public void lockssRun() {
      setPriority(PRIORITY_PARAM_SCOMM, PRIORITY_DEFAULT_SCOMM);
      triggerWDogOnExit(true);
      startWDog(WDOG_PARAM_SCOMM, WDOG_DEFAULT_SCOMM);
      nowRunning();

      while (goOn) {
	pokeWDog();
	try {
	  synchronized (timeout) {
	    if (goOn) {
	      timeout.expireIn(getChannelHungTime());
	    }
	  }
	  if (log.isDebug3()) log.debug3("rcvQueue.get(" + timeout + ")");
	  Object qObj = rcvQueue.get(timeout);
	  if (qObj != null) {
	    if (qObj instanceof PeerMessage) {
	      if (log.isDebug3()) log.debug3("Rcvd " + qObj);
	      processReceivedPacket((PeerMessage)qObj);
	    } else {
	      log.warning("Non-PeerMessage on rcv queue" + qObj);
	    }
	  }
	  if (TimeBase.msSince(lastHungCheckTime) >
	      getChannelHungTime()) {
	    checkHungChannels();
	    lastHungCheckTime = TimeBase.nowMs();
	  }
	} catch (InterruptedException e) {
	  // just wake up and check for exit
	} finally {
	}
      }
      rcvThread = null;
    }

    private void stopRcvThread() {
      synchronized (timeout) {
	stopWDog();
	triggerWDogOnExit(false);
	goOn = false;
	timeout.expire();
      }
    }
  }

  // Listen thread
  private class ListenThread extends LockssThread {
    private volatile boolean goOn = true;

    private ListenThread(String name) {
      super(name);
    }

    public void lockssRun() {
      setPriority(PRIORITY_PARAM_SLISTEN, PRIORITY_DEFAULT_SLISTEN);
      triggerWDogOnExit(true);
//       startWDog(WDOG_PARAM_SLISTEN, WDOG_DEFAULT_SLISTEN);
      nowRunning();

      while (goOn) {
// 	pokeWDog();
	log.debug3("accept()");
	try {
	  Socket sock = listenSock.accept();
	  if (sock instanceof SSLSocket && paramSslClientAuth) {
	    // Ensure handshake is complete before doing anything else
	    handShake((SSLSocket)sock);
	  }
	  processIncomingConnection(sock);
	} catch (SocketException e) {
	  if (goOn) {
	    log.warning("Listener", e);
	  }
	} catch (Exception e) {
	  if (listenSock instanceof SSLServerSocket) {
	    log.debug("SSL Listener ", e);
	  } else {
	    log.warning("Listener", e);
          }
	}
      }
      listenThread = null;
    }

    private void stopListenThread() {
      stopWDog();
      triggerWDogOnExit(false);
      goOn = false;
      IOUtil.safeClose(listenSock);
      this.interrupt();
    }
  }

  /** SocketFactory interface allows test code to use instrumented or mock
      sockets and peer channels */
  interface SocketFactory {
    ServerSocket newServerSocket(int port, int backlog) throws IOException;

    Socket newSocket(IPAddr addr, int port) throws IOException;

    BlockingPeerChannel newPeerChannel(BlockingStreamComm comm,
				       Socket sock)
	throws IOException;

    BlockingPeerChannel newPeerChannel(BlockingStreamComm comm,
				       PeerIdentity peer)
	throws IOException;
  }

  /** Normal socket factory creates real TCP Sockets */
  class NormalSocketFactory implements SocketFactory {
    public ServerSocket newServerSocket(int port, int backlog)
	throws IOException {
      return new ServerSocket(port, backlog);
    }

    public Socket newSocket(IPAddr addr, int port) throws IOException {
      return new Socket(addr.getInetAddr(), port);
    }

    public BlockingPeerChannel newPeerChannel(BlockingStreamComm comm,
					      Socket sock)
	throws IOException {
      return new BlockingPeerChannel(comm, sock);
    }

    public BlockingPeerChannel newPeerChannel(BlockingStreamComm comm,
					      PeerIdentity peer)
	throws IOException {
      return new BlockingPeerChannel(comm, peer);
    }
  }

  /** SSL socket factory */
  class SslSocketFactory implements SocketFactory {
    public ServerSocket newServerSocket(int port, int backlog)
      throws IOException {
      if (sslServerSocketFactory == null) {
	throw new IOException("no SSL server socket factory");
      }
      SSLServerSocket s = (SSLServerSocket)
	sslServerSocketFactory.createServerSocket(port, backlog);
      s.setNeedClientAuth(paramSslClientAuth);
      log.debug("New SSL server socket: " + port + " backlog " + backlog +
		" clientAuth " + paramSslClientAuth);
      String cs[] = s.getEnabledCipherSuites();
      for (int i = 0; i < cs.length; i++) {
	log.debug2(cs[i] + " enabled cipher suite");
      }
      cs = s.getSupportedCipherSuites();
      for (int i = 0; i < cs.length; i++) {
	log.debug2(cs[i] + " supported cipher suite");
      }
      cs = s.getEnabledProtocols();
      for (int i = 0; i < cs.length; i++) {
	log.debug2(cs[i] + " enabled protocol");
      }
      cs = s.getSupportedProtocols();
      for (int i = 0; i < cs.length; i++) {
	log.debug2(cs[i] + " supported protocol");
      }
      log.debug2("enable session creation " + s.getEnableSessionCreation());
      return s;
    }

    public Socket newSocket(IPAddr addr, int port) throws IOException {
      if (sslSocketFactory == null) {
	throw new IOException("no SSL client socket factory");
      }
      SSLSocket s = (SSLSocket)
	  sslSocketFactory.createSocket(addr.getInetAddr(), port);
      log.debug2("New SSL client socket: " + port + "@" + addr.toString());
      if (paramSslClientAuth) {
	handShake(s);
      }
      return s;
    }

    public BlockingPeerChannel newPeerChannel(BlockingStreamComm comm,
					      Socket sock)
	throws IOException {
      return new BlockingPeerChannel(comm, sock);
    }

    public BlockingPeerChannel newPeerChannel(BlockingStreamComm comm,
					      PeerIdentity peer)
	throws IOException {
      return new BlockingPeerChannel(comm, peer);
    }
  }

  private static final List statusColDescs =
    ListUtil.list(
		  new ColumnDescriptor("Peer", "Peer",
				       ColumnDescriptor.TYPE_STRING)
		  );

  private class Status implements StatusAccessor {
    // port, proto, u/m, direction, compressed, pkts, bytes
    long start;

    public String getDisplayName() {
      return "Comm Statistics";
    }

    public boolean requiresKey() {
      return false;
    }

    public void populateTable(StatusTable table) {
//       table.setResortable(false);
//       table.setDefaultSortRules(statusSortRules);
      String key = table.getKey();
      table.setColumnDescriptors(statusColDescs);
      table.setRows(getRows(key));
      table.setSummaryInfo(getSummaryInfo(key));
    }

    private List getSummaryInfo(String key) {
      List res = new ArrayList();
      res.add(new StatusTable.SummaryInfo("Max channels",
					  ColumnDescriptor.TYPE_INT,
					  channels.getMaxSize()));
      
      res.add(new StatusTable.SummaryInfo("Max rcvChannels",
					  ColumnDescriptor.TYPE_INT,
					  rcvChannels.getMaxSize()));
      return res;
    }

    private List getRows(String key) {
      List table = new ArrayList();
      synchronized (channels) {
	for (Iterator iter = channels.entrySet().iterator(); iter.hasNext();) {
	  Map.Entry ent = (Map.Entry)iter.next();
	  PeerIdentity pid = (PeerIdentity)ent.getKey();
	  BlockingPeerChannel chan = (BlockingPeerChannel)ent.getValue();
	  table.add(makeRow(pid, chan));
	}
      }
      return table;
    }

    private Map makeRow(PeerIdentity pid, BlockingPeerChannel chan) {
      Map row = new HashMap();
      row.put("Peer", pid.getIdString());
      return row;
    }
  }
}
