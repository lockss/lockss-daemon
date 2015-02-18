/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import javax.net.ssl.SSLPeerUnverifiedException;

import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.daemon.*;

/** Manages a stream connection to a peer.  Channels are ephemeral, coming
 * and going as needed.
 */
class BlockingPeerChannel implements PeerChannel {
  static Logger log = Logger.getLogger("Channel");

  static final int COPY_BUFFER_SIZE = 4096;

  // Channel state
  public static enum ChannelState {
    NONE,
    INIT,
    CONNECTING,
    DISSOCIATING,
    CONNECT_FAIL,
    ACCEPTED,
    STARTING,
    OPEN,
    DRAIN_INPUT,
    DRAIN_OUTPUT,
    NEED_CLOSE,
    CLOSING,
    CLOSED,
  }

  private ChannelState state = ChannelState.INIT;
  private Object stateLock = new Object();
  private long lastStateChange = -1;
  private ChannelState prevState = ChannelState.NONE;
  private boolean isOriginate = false;

  volatile private PeerIdentity peer = null;
  volatile private PeerAddress pad;
  private PeerIdentity localPeer;
  private BlockingStreamComm scomm;
  private Socket sock;
  private Queue rcvQueue;
  private Queue sendQueue;
  private InputStream ins;
  private OutputStream outs;
  private OutputStream socket_outs;
  private int sendCnt = 0;
  private boolean didOpen = false;
  private Throwable connectException;

  volatile private long lastSendTime = 0;
  volatile private long lastRcvTime = 0;
  volatile private long lastActiveTime = 0;

  volatile private ChannelReader reader;
  volatile private ChannelRunner writer;
  volatile private ChannelRunner connecter;
  // above get nulled in synchronized block; we must call waitExited()
  // unsynchronized, so copies are put here
  private ChannelRunner wtReader;
  private ChannelRunner wtWriter;
  private ChannelRunner wtConnecter;

  private byte[] rcvHeader = new byte[HEADER_LEN];
  private byte[] sndHeader = new byte[HEADER_LEN];
  private byte[] peerbuf = new byte[MAX_PEERID_LEN];

  private ChannelStats stats = new ChannelStats();

  /** All other constructors should call this one
   */
  private BlockingPeerChannel(BlockingStreamComm scomm) {
    this.scomm = scomm;
    localPeer = scomm.getMyPeerId();
    rcvQueue = scomm.getReceiveQueue();
    // This queue may be replaced if this channel becomes primary for a
    // peer that has queued messages
    sendQueue = new FifoQueue();
  }

  /** Create a channel to be connected to the peer; doesn't attempt to
   * connect (yet)
   * @param scomm parent stream comm
   * @param peer the peer to talk to
   */
  BlockingPeerChannel(BlockingStreamComm scomm, PeerIdentity peer) {
    this(scomm);
    this.peer = peer;
    setState(ChannelState.INIT);
  }

  /** Create a channel from an existing (incoming) connection socket.  The
   * peer identity is not yet known.
   * @param scomm parent stream comm
   * @param sock the socket open to the peer
   */
  BlockingPeerChannel(BlockingStreamComm scomm, Socket sock) {
    this(scomm);
    this.sock = sock;
    setSocket(sock);
    setState(ChannelState.ACCEPTED);
  }

  /** Used by tests only */
  BlockingPeerChannel(BlockingStreamComm scomm, PeerIdentity peer,
		      InputStream ins, OutputStream outs) {
    this(scomm);
    this.peer = peer;
    this.ins = ins;
    this.outs = outs;
  }

  /** Test overrides to access socket */
  void setSocket(Socket sock) {
  }

  /** Return our peer, or null if not known yet */
  public PeerIdentity getPeer() {
    return peer;
  }

  long getLastActiveTime() {
    return lastActiveTime;
  }

  /** Return true if it's worth queueing and retrying the messages this
   * channel didn't send.  True if the connection was opened successfully
   * (and presumably closed abruptly) or if it failed due to a
   * SocketException (but not, e.g., mismatching SSL keys (SSLException) */
  boolean shouldRetry() {
    if (connectException != null) {
      return connectException instanceof SocketException;
    }
    return didOpen;
  }

  private void setState(ChannelState newState) {
    if (log.isDebug3()) log.debug3(p()+"State: " + state + " -> " + newState);
    if (state != prevState) {
      prevState = state;
      lastStateChange = TimeBase.nowMs();
    }
    state = newState;
  }

  ChannelState getState() {
    return state;
  }

  /** If currently in state <code>from</code>, transition to state
   * <code>to</code> and return true.  Otherwise return false or, if
   * <code>errmsg</code> is not null, throw an exception
   */
  boolean stateTrans(ChannelState from, ChannelState to, String errmsg) {
    synchronized (stateLock) {
      if (state == from) {
	setState(to);
	return true;
      } else if (errmsg != null) {
	throw new IllegalStateException(errmsg + " in state " + state);
      } else {
	return false;
      }
    }
  }

  /** If currently in state <code>from</code>, transition to state
   * <code>to</code> and return true, otherwise return false
   */
  boolean stateTrans(ChannelState from, ChannelState to) {
    return stateTrans(from, to, null);
  }

  /** If currently not in state <code>notFrom</code>, transition to state
   * <code>to</code> and return true.  Otherwise return false or, if
   * <code>errmsg</code> is not null, throw an exception
   */
  boolean notStateTrans(ChannelState notFrom, ChannelState to, String errmsg) {
    synchronized (stateLock) {
      if (state != notFrom) {
	setState(to);
	return true;
      } else if (errmsg != null) {
	throw new IllegalStateException(errmsg + " in state " + state);
      } else {
	return false;
      }
    }
  }

  /** If currently not in any state in <code>notFrom</code>, transition to
   * state <code>to</code> and return true.  Otherwise return false or, if
   * <code>errmsg</code> is not null, throw an exception
   */
  boolean notStateTrans(ChannelState[] notFrom, ChannelState to,
			String errmsg) {
    synchronized (stateLock) {
      if (!isState(state, notFrom)) {
	setState(to);
	return true;
      } else if (errmsg != null) {
	throw new IllegalStateException(errmsg + " in state " + state);
      } else {
	return false;
      }
    }
  }

  /** If currently not in state <code>notFrom</code>, transition to state
   * <code>to</code> and return true, otherwise return false
   */
  boolean notStateTrans(ChannelState notFrom, ChannelState to) {
    return notStateTrans(notFrom, to, null);
  }

  /** If currently not in any state in <code>notFrom</code>, transition to
   * state <code>to</code> and return true, otherwise return false
   */
  boolean notStateTrans(ChannelState[] notFrom, ChannelState to) {
    return notStateTrans(notFrom, to, null);
  }

  /** Throw an IllegalStateException if not in the expectedState
   */
  void assertState(ChannelState expectedState, String errmsg) {
    if (state != expectedState)
      throw new IllegalStateException(errmsg + " in state " + state +
				      ", expected " + expectedState);
  }

  /** True if in one of the specified states
   */
  boolean isState(ChannelState state, ChannelState[] oneOf) {
    for (int ix = 0; ix < oneOf.length; ix++) {
      if (state == oneOf[ix]) return true;
    }
    return false;
  }

  /** True if current state is equal to state s
   */
  boolean isState(ChannelState s) {
    return state == s;
  }

  /** True if current state is equal to one of the states
   */
  boolean isState(ChannelState[] oneOf) {
    return isState(state, oneOf);
  }

  /** True if current state is CLOSING or CLOSED
   */
  boolean isClosed() {
    return state == ChannelState.CLOSED || state == ChannelState.CLOSING;
  }

  boolean isOpen() {
    return state == ChannelState.OPEN;
  }

  // These are the main entry points from BlockingStreamComm

  /** Start thread to connect to the peer and start channel threads.
   */
  public void startOriginate() throws IOException {
    // arrange for possible MalformedIdentityKeyException to be thrown
    // here, not in thread
    isOriginate = true;
    pad = peer.getPeerAddress();
    if (!(pad instanceof PeerAddress.Tcp)) {
      throw new IllegalArgumentException("Wrong type of PeerAddress: " + pad);
    }
    if (stateTrans(ChannelState.INIT, ChannelState.CONNECTING,
		   "startOriginate")) {
      ChannelConnecter runner = new ChannelConnecter();
      runner.setTimeout(scomm.getConnectTimeout());
      try {
	wtConnecter = connecter = runner;
	scomm.execute(runner);
      } catch (InterruptedException e) {
      // Can happen if we get aborted while starting pool thread
	log.warning("startOriginate()", e);
	wtConnecter = connecter = null;
	stateTrans(ChannelState.CONNECTING, ChannelState.CLOSED);
	throw new IOException(e.toString());
      } catch (Exception e) {
	log.warning("Can't start channel connecter", e);
	wtConnecter = connecter = null;
	stateTrans(ChannelState.CONNECTING, ChannelState.CLOSED);
	throw new IOException(e.toString());
      }
    }
  }

  /** Start threads in response to incoming connection
   */
  public void startIncoming() {
    if (stateTrans(ChannelState.ACCEPTED, ChannelState.STARTING,
		   "startIncoming")) {
      try {
	startConnectedChannel();
      } catch (IOException e) {
	abortChannel(e);
      }
    }
  }

  /** Send a message to our peer, return true iff we expect to be able to
   * send it
   */
  public boolean send(PeerMessage msg) {
    synchronized (stateLock) {
      switch (state) {
      case CLOSED:
      case CLOSING:
      case CONNECT_FAIL:
      case DISSOCIATING:
      case DRAIN_INPUT:
	return false;
      default:
	sendQueue.put(msg);
	sendCnt++;
	return true;
      }
    }
  }

  /** Enqueue all messages on queue to be sent */
  public synchronized void enqueueMsgs(Queue queue) {
    if (writer == null) {
      // if the write thread hasn't been started yet, just adopt the queue
      // as our own
      if (log.isDebug2()) log.debug2("Adopting queue len " + queue.size());
      this.sendQueue = queue;
    } else {
      // otherwise a thread is already waiting on it, copy queue
      PeerMessage msg;
      try {
	while ((msg = (PeerMessage)queue.get(Deadline.EXPIRED)) != null) {
	  if (log.isDebug3() )log.debug3("Enqueued " + msg);
	  sendQueue.put(msg);
	}
      } catch (InterruptedException e) {
	log.critical("Impossible");
      }
    }
  }

  /** Initialize streams, start reader and writer threads
   */
  private void startConnectedChannel() throws IOException {
    assertState(ChannelState.STARTING, "startConnectedChannel");
    try {
      if (scomm.isBufferedSend()) {
	socket_outs = outs; // if abort, close socket stream, not buffered
			    // stream (which will hang in flush() if
			    // ChanWriter is hung)
	outs = new BufferedOutputStream(outs, COPY_BUFFER_SIZE);
	if (log.isDebug3()) log.debug3(p()+"Buffering output");
      }

      stateTrans(ChannelState.STARTING, ChannelState.OPEN);

      // Reader starts writer (after SSL handshake, if any).
      startReader();

    } catch (Exception e) {
      log.error("Channel didn't start", e);
      throw new IOException(e.toString());
    }
  }

  void abortChannel() {
    stopChannel(true, null, null);
  }

  void abortChannel(Throwable t) {
    stopChannel(true, t != null ? t.toString() : null, t);
  }

  void abortChannel(String msg) {
    abortChannel(msg, null);
  }

  void abortChannel(String msg, Throwable t) {
    if (peer == null && sock != null) {
      msg += " (conn from " + sock.getInetAddress() + ")";
    }
    stopChannel(true, msg, t);
  }

  void stopChannel() {
    stopChannel(false, null, null);
  }

  static ChannelState[] stopIgnStates = {
    ChannelState.INIT,
    ChannelState.CLOSED,
    ChannelState.CLOSING};

  void stopChannel(boolean abort, String msg, Throwable t) {
    if (notStateTrans(stopIgnStates, ChannelState.CLOSING)) {
      scomm.dissociateChannelFromPeer(this, peer, sendQueue);
      if (msg != null || t != null) {
	if (msg == null) msg = "Aborting " + peer.getIdString();
	log.warning(msg, t);
      }
      IOUtil.safeClose(sock);
      IOUtil.safeClose(ins);
      if (abort && socket_outs != null) {
	// if aborting, don't close buffered stream as flush() might hang
	IOUtil.safeClose(socket_outs);
      } else {
	IOUtil.safeClose(outs);
      }
      connecter = wtConnecter = stopThread(connecter);
      reader = (ChannelReader)stopThread(reader);
      writer = stopThread(writer);
      stateTrans(ChannelState.CLOSING, ChannelState.CLOSED);
    } else {
      // XXX This shouldn't be necessary.  stopChannel() previously called
      // dissociateChannelFromPeer() unconditionally; retain capability for
      // a while just in case
      if (scomm.getDissociateOnEveryStop())
	scomm.dissociateChannelFromPeer(this, peer, sendQueue);
    }
  }

  private ChannelRunner stopThread(ChannelRunner runner) {
    if (runner != null) {
      if (log.isDebug3()) log.debug3("Stopping " + runner.getName());
      runner.stopRunner();
    }
    return null;
  }

  /** Wait until all threads we started have exited.  Used by
   * BlockingStreamComm.stopService() */
  void waitThreadsExited(Deadline timeout) {
    if (wtConnecter != null) {
      wtConnecter.waitExited(timeout);
    }
    if (wtReader != null) {
      wtReader.waitExited(timeout);
    }
    if (wtWriter != null) {
      wtWriter.waitExited(timeout);
    }
  }

  /** Called periodically by parent stream comm to check for hung sender
   */
  void checkHung() {
    if ((state == ChannelState.OPEN || state == ChannelState.DRAIN_OUTPUT) &&
	lastActiveTime != 0 &&
	!isSendIdle() &&
	TimeBase.msSince(lastActiveTime) > scomm.getChannelHungTime()) {
      abortChannel(p()+"Hung sending");
    }
  }

  /** Open a connection to our peer; start things running if it works
   */
  void connect(ChannelConnecter connector) {
    assertState(ChannelState.CONNECTING, "connect");
    if (pad instanceof PeerAddress.Tcp) {
      PeerAddress.Tcp tpad = (PeerAddress.Tcp)pad;
      try {
	sock = scomm.getSocketFactory().newSocket(tpad.getIPAddr(),
						  tpad.getPort());
	setSocket(sock);
	connector.cancelTimeout();
	didOpen = true;
	this.connecter = wtConnecter = null;
	log.debug2("Connected to " + peer);
      } catch (IOException e) {
	connector.cancelTimeout();
	this.connecter = wtConnecter = null;
	if (e instanceof SocketException) {
	  connectException = e;
	}
	stateTrans(ChannelState.CONNECTING, ChannelState.DISSOCIATING);
	abortChannel("Connect failed to " + peer + ": " + e.toString());
	stateTrans(ChannelState.DISSOCIATING, ChannelState.CONNECT_FAIL);
	return;
      }
      try {
	stateTrans(ChannelState.CONNECTING, ChannelState.STARTING);
	startConnectedChannel();
      } catch (IOException e) {
	abortChannel(e);
	return;
      }
    } else {
      throw new IllegalArgumentException("Unknown PeerAddress: " + pad);
    }
  }

  /** Start the reader thread
   */
  void startReader() {
    log.debug3("Starting reader");
    ChannelReader runner = new ChannelReader();
    try {
      wtReader = reader = runner;
      scomm.execute(runner);
    } catch (InterruptedException e) {
      // Can happen if we get aborted while starting pool thread
      abortChannel("startReader()", e);
    } catch (RuntimeException e) {
      log.warning("startReader()", e);
      abortChannel("startReader()", e);
    }
  }

  /** Start the writer thread
   */
  synchronized void startWriter() {
    log.debug3("Starting writer");
    ChannelWriter runner = new ChannelWriter();
    try {
      scomm.execute(runner);
      wtWriter = writer = runner;
    } catch (InterruptedException e) {
      // Can happen if we get aborted while starting pool thread
      abortChannel("startWriter()", e);
    } catch (RuntimeException e) {
      abortChannel("startWriter()", e);
    }
  }

  // Message reception, invoked by ChannelReader

  /** Process messages until error or stream closed, then close the channel,
   * cleanly if possible
   */
  void handleInputStream(ChannelRunner runner) {
    try {
      // Perform SSL handshake if this is an SSL connection.  Throw if peer
      // not verified.
      scomm.handshakeIfClientAuth(sock);

      ins = sock.getInputStream();
      outs = sock.getOutputStream();
      startWriter();

      while (true) {
	try {
	  readMessages(runner);
	  // input stream closed by peer, drain output if necessary
	  synchronized (stateLock) {
	    if (!isSendIdle() && isOpen()) {
	      stateTrans(ChannelState.OPEN, ChannelState.DRAIN_OUTPUT);
	    } else {
	      notStateTrans(stopIgnStates, ChannelState.NEED_CLOSE);
	    }
	  }
	  if (isState(ChannelState.NEED_CLOSE)) {
	    stopChannel();
	  }
	  // and exit thread
	  return;
	} catch (InterruptedIOException e) {
	  // read timeout.  if channel recently active (sending), continue
	  // reading if can do so reliably (0 bytes xferred), which will
	  // normally be the case when waiting for a message.  (Shouldn't
	  // ever be over channel idle time, as send thread should have
	  // terminated us.)
	  int xfer = e.bytesTransferred;
	  log.debug("read timeout, " + xfer + " bytes read");
	  if (xfer != 0 ||
	      TimeBase.msSince(lastActiveTime) > scomm.getChannelIdleTime()) {
	    throw e;
	  }
	  // Continuing does not work reliably; read timeouts are disabled,
	  // see BlockingStreamComm.PARAM_DATA_TIMEOUT
	  throw e;
	}
      }
    } catch (SSLPeerUnverifiedException e) {
      // Warning already issued by handshakeIfClientAuth()
      log.debug2("Not verified: " +
		 sock.getInetAddress() + ":" + sock.getPort());
      abortChannel();
    } catch (SocketException e) {
      // Expected when closing
      if (isClosed()) {
	abortChannel();
      } else {
	abortChannel("handleInputStream: " + e.toString());
      }
    } catch (IOException e) {
      // These are unexpected
      if (log.isDebug3()) {
	abortChannel("handleInputStream", e);
      } else {
	abortChannel("handleInputStream: " + e.toString());
      }
    }
    // exit thread
  }

  /** Read and process messages until error or stream closed
   */
  void readMessages(ChannelRunner runner) throws IOException {
    while (runner.goOn() && readHeader()) {
      int op = getRcvdMessageOp();
      if (peer == null && op != OP_PEERID) {
	throw new ProtocolException("Didn't receive peerid first: " + op);
      }
      switch (op) {
      case OP_PEERID:
	readPeerId();
	// Ensure thread name includes peer, for better logging.
	synchronized (stateLock) {
	  if (state != ChannelState.CLOSING) {
	    // reader, writer can get set to null while in ChannelState.CLOSING
	    ChannelRunner tmp;
	    if ((tmp = reader) != null) tmp.setRunnerName();
	    // Must ensure that writer thread has been created by the time
	    // reader thread gets here
	    if ((tmp = writer) != null) tmp.setRunnerName();
	  }
	}
	break;
      case OP_DATA:
	readDataMsg();
	break;
      case OP_CLOSE:
	// Not implemented yet
	break;
      default:
	String msg = "Received unknown opcode: " + op;
	if (scomm.getAbortOnUnknownOp()) {
	  throw new ProtocolException(msg);
	} else {
	  log.debug(msg);
	}
      }
    }
  }

  /** Read a peer id message, verify peer's id, tell comm to associate us
   * with id if not already.
   */
  void readPeerId() throws IOException {
    long len = getRcvdMessageLength();
    if (len > MAX_PEERID_LEN) {
      String msg = "Peerid too long: " + len;
      log.warning(msg);
      throw new ProtocolException(msg);
    }
    int plen = (int)len;
    if (!readBuf(peerbuf, plen)) {
      String msg = "No data in Peerid message";
      log.warning(msg);
      throw new ProtocolException(msg);
    }
    String peerkey = new String(peerbuf, 0, plen);
    PeerIdentity pid = scomm.findPeerIdentity(peerkey);
    if (peer == null) {
      peer = pid;
      log.debug3("Got peer: " + peer);
      // ensure a compatible peer address
      if (! peer.isV3()) {
	throw new ProtocolException("Incompatible PeerAddress type: " +
				    peer.getPeerAddress());
      }
      // XXX If this is an incoming connection, need to make outgoing
      // connection to peerid just received, send an echo nonce message
      // over that connection and ensure we receive the nonce on this
      // connection, then close outgoing conn

      synchronized (stateLock) {
	if (isOpen() && !isOriginate()) {
	  // This is an unassociated incoming channel, so scomm has no
	  // pointer to it, thus it's ok to call associateChannelWithPeer
	  // while holding stateLock.
	  scomm.associateChannelWithPeer(this, peer);
	}
      }
    } else if (!pid.equals(peer)) {
      String msg = "Received conflicting peerid msg: " + pid + " was: " + peer;
      log.warning(msg);
      throw new ProtocolException(msg);
    }
  }

  /** Read a data message into a new PeerMessage and enqueue it
   */
  void readDataMsg() throws IOException {
    long len = getRcvdMessageLength();
    int proto = ByteArray.decodeInt(rcvHeader, HEADER_OFF_PROTO);
    if (log.isDebug3()) log.debug3("Got data hdr: " + proto + ", len: " + len);
    if (len > scomm.getMaxMessageSize()) {
      throw new ProtocolException("Too-large incoming message: " + len);
    }
    long startTime = 0;
    if (len >= scomm.getMinMeasuredMessageSize()) {
      startTime = TimeBase.nowMs();
    }
    PeerMessage msg = scomm.newPeerMessage(len);
    msg.setProtocol(proto);
    msg.setSender(peer);
    try {
      OutputStream msgOut = msg.getOutputStream();
      copyBytes(ins, msgOut, len, stats.getInCount());
      logRate("Rcv", len, startTime);
      msgOut.close();
      // update lastActiveTime *before* queuing message; produces more
      // predictable behavior when running in simulated time in unit tests
      lastRcvTime = lastActiveTime = TimeBase.nowMs();
      RateLimiter limiter = scomm.getReceiveRateLimiter(peer);
      if (limiter == null || limiter.isEventOk()) {
	if (limiter != null) {
	  limiter.event();
	}
	rcvQueue.put(msg);
	countRcvdMsg();
      } else {
	scomm.rcvRateLimited(peer);
	log.debug3("rcv rate limited");
      }
    } catch (IOException e) {
      msg.delete();
      throw e;
    }
  }

  void countRcvdMsg() {
    stats.rcvdMsg();
    BlockingStreamComm.PeerData pd = scomm.getPeerData(peer);
    if (pd != null) {
      pd.rcvdMsg();
    }
  }

  void countSentMsg() {
    stats.sentMsg();
    BlockingStreamComm.PeerData pd = scomm.getPeerData(peer);
    if (pd != null) {
      pd.sentMsg();
    }
  }

  /** Read size bytes into buf */
  boolean readBuf(byte[] buf, int size) throws IOException {
    int len = readBytes(ins, buf, size);
    if (len == size) return true;
    if (len == 0) return false;
    throw new ProtocolException("Connection closed in middle of message");
  }

  /** Read size bytes from stream into buf.  Keeps trying to read until
   * enough bytes have been read or EOF or error.
   * @param ins stream to read from
   * @param buf buffer to read into
   * @param size number of bytes to read
   * @return number of bytes read, which will be less than size iff EOF is
   * reached
   * @throws IOException
   */
  int readBytes(InputStream ins, byte[] buf, int size) throws IOException {
    int off = 0;
    int rem = size;
    while (rem > 0) {
      int nread = ins.read(buf, off, rem);
      if (nread == -1) {
	return off;
      }
      lastRcvTime = lastActiveTime = TimeBase.nowMs();
      off += nread;
      rem -= nread;
      stats.rcvdBytes(nread);
    }
    return off;
  }

  /** Read incoming message header into rcvHeader.
   * @return true if read a complete header, false if no more incoming
   * messages on this connection.
   * @throws ProtocolException if message header is malformed or connection
   * closed before complete header is read.
   */
  boolean readHeader() throws IOException {
    if (!readBuf(rcvHeader, HEADER_LEN)) {
      // connection closed cleanly
      if (log.isDebug2()) log.debug2(p()+"Input closed");
      return false;
    }
    if (rcvHeader[HEADER_OFF_CHECK] != HEADER_CHECK) {
      throw new ProtocolException("Message doesn't start with " +
				  HEADER_CHECK);
    }
    return true;
  }

  long getRcvdMessageLength() {
    return ByteArray.decodeLong(rcvHeader, HEADER_OFF_LEN);
  }

  int getRcvdMessageOp() {
    return ByteArray.decodeByte(rcvHeader, HEADER_OFF_OP);
  }

  private void logRate(String direction, long len, long startTime) {
    if (startTime <= 0 || !log.isDebug()) {
      return;
    }
    long dur = TimeBase.msSince(startTime);
    if (dur <= 0) {
      return;
    }
    long bpms = len / dur;
    if (bpms >= 1000) {
      log.debug(String.format("%s%s rate: %.1fMB/s (%d bytes in %s)",
			      p(), direction, (bpms / 1000.0), len,
			      StringUtil.timeIntervalToString(dur)));
    } else {
      log.debug(String.format("%s%s rate: %dKB/s (%d bytes in %s)",
			      p(), direction, bpms, len,
			      StringUtil.timeIntervalToString(dur)));
    }
  }

  // Message sending, invoked by ChannelWriter

  boolean isSendIdle() {
    return sendQueue.isEmpty();
  }

  /** Wake up when channel has been idle long enough to close, or sooner to
   * poke watchdog
   */
  Deadline calcSendWaitDeadline() {
    long i = scomm.getSendWakeupTime();
    long j = (lastActiveTime == 0) ? scomm.getChannelIdleTime()
      : TimeBase.msUntil(lastActiveTime + scomm.getChannelIdleTime());
    if (j < 0) j = 0;
    if (log.isDebug3()) log.debug3("Send queue wait: " + (i < j ? i : j) +
				   ", lastActiveTime: " + lastActiveTime);
    return Deadline.in(i < j ? i : j);
  }

  /** Process the output side of a newly opened socket.  Send peerid msg
   * first, then data messages as they become available on send queue
   */
  void handleOutputStream(ChannelWriter runner) {
    try {
      writePeerId();
      PeerMessage msg;
      while (runner.goOn()) {
	// don't remove msg from sendQueue until sent.  isEmpty() implies
	// nothing to send
	while (null != (msg = (PeerMessage)sendQueue.peekWait(calcSendWaitDeadline()))) {
	  lastSendTime = lastActiveTime = TimeBase.nowMs();
	  msg.setLastRetry(lastSendTime);
	  writeDataMsg(msg);
	  // remove the message just sent
	  if (msg != sendQueue.get(Deadline.EXPIRED)) {
	    throw new IllegalStateException("Send queue not behaving as FIFO");
	  }
	  scomm.countMessageRetries(msg);
	  msg.delete();
	  lastSendTime = lastActiveTime = TimeBase.nowMs();
	  synchronized (stateLock) {
	    // if draining output and nothing left to send, close.  Check
	    // now rather than waiting for peekWait() to timeout.
	    if (isSendIdle()) {
	      stateTrans(ChannelState.DRAIN_OUTPUT, ChannelState.NEED_CLOSE);
	    }
	  }
	  if (isState(ChannelState.NEED_CLOSE)) {
	    stopChannel();
	    return;
	  }
	}
	synchronized (stateLock) {
	  if (!isSendIdle()) {
	    // if something got queued before state locked, continue sending
	    continue;
	  }
	  // if draining output, close.  Must check this again because it
	  // might have become true during peekWait()
	  if (isSendIdle()) {
	    stateTrans(ChannelState.DRAIN_OUTPUT, ChannelState.NEED_CLOSE);
	  }
	}
	if (isState(ChannelState.NEED_CLOSE)) {
	  stopChannel();
	  return;
	}

	if (TimeBase.msSince(lastActiveTime) > scomm.getChannelIdleTime()) {
	  if (notStateTrans(stopIgnStates, ChannelState.DRAIN_INPUT)) {
	    // time to close channel.  shutdown output only in case peer is
	    // now sending message
	    // No longer can send messages so must dissociate now
	    scomm.dissociateChannelFromPeer(this, peer, null);
	    reader.setTimeout(scomm.getDrainInputTime() / 2);
	    try {
	      log.debug2("Shutdown output");
	      sock.shutdownOutput();
	      break;
	    } catch (IOException e) {
	      abortChannel("shutdownOutput", e);
	      break;
	    } catch (UnsupportedOperationException e) {
	      abortChannel("shutdownOutput() not implemented for SSL");
	    }
	  }
 	  return;
	}
      }
    } catch (InterruptedException e) {
      abortChannel();
    } catch (IOException e) {
      abortChannel(e);
    }
  }

  /** send peerid msg
   */
  void writePeerId() throws IOException{
    String key = localPeer.getIdString();
    if (log.isDebug3()) log.debug3("Sending peerid: " + key);
    writeHeader(OP_PEERID, key.length(), 0);
    outs.write(key.getBytes());
    outs.flush();
    stats.sentBytes(key.length());
  }

  /** send data msg
   */
  void writeDataMsg(PeerMessage msg) throws IOException {
    long len = msg.getDataSize();
    if (log.isDebug3()) log.debug3("Sending data: " + msg.getProtocol() +
				   ", len: " + len);
    long startTime = 0;
    if (len >= scomm.getMinMeasuredMessageSize()) {
      startTime = TimeBase.nowMs();
    }
    writeHeader(OP_DATA, msg.getDataSize(), msg.getProtocol());
    copyBytes(msg.getInputStream(), outs, msg.getDataSize(),
	      stats.getOutCount());
    outs.flush();
    countSentMsg();
    logRate("Send", len, startTime);
  }

  /** send msg header
   */
  void writeHeader(int op, long len, int proto) throws IOException {
    sndHeader[HEADER_OFF_CHECK] = HEADER_CHECK;
    sndHeader[HEADER_OFF_OP] = (byte)op;
    ByteArray.encodeLong(len, sndHeader, HEADER_OFF_LEN);
    ByteArray.encodeInt(proto, sndHeader, HEADER_OFF_PROTO);
    outs.write(sndHeader);
    stats.sentBytes(HEADER_LEN);
  }

  /** Copy len bytes from input to output stream.
   * @return true if len bytes successfully copied
   * @throws ProtocolException if eof reached before len bytes
   * @throws IOException
   */
  boolean copyBytes(InputStream is, OutputStream os, long len,
		    ChannelStats.Count count)
      throws IOException {
    byte[] copybuf = new byte[COPY_BUFFER_SIZE];
    long rem = len;
    int bufsize = copybuf.length;
    while (rem > 0) {
      int nread = is.read(copybuf, 0, rem > bufsize ? bufsize : (int)rem);
      if (nread < 0) {
	throw new ProtocolException("Connection closed in middle of message");
      }
      os.write(copybuf, 0, nread);
      rem -= nread;
      count.addBytes(nread);
      lastActiveTime = TimeBase.nowMs();
    }
    return true;
  }

  /** Return true if this channel was created as an originating channel and
   * has not accepted any messages to be sent.  This is checked if we
   * refuse an outgoing message.  If true, the channel failed to start.
   */
  boolean isUnusedOriginatingChannel() {
    return isOriginate() && sendCnt == 0;
  }

  public String toString() {
    return "[BChan(" + state + "): " +
      (peer != null ? peer.toString() : "(none)")
      + "]";
  }

  // for logging convenience
  String p() {
    if (peer != null) {
      return peer.getIdString() + ": ";
    } else {
      return "";
    }
  }

  ChannelStats getStats() {
    return stats;
  }

  int getSendQueueSize() {
    return sendQueue.size();
  }

  boolean isOriginate() {
    return isOriginate;
  }

  boolean hasReader() {
    return reader != null;
  }

  boolean hasWriter() {
    return writer != null;
  }

  boolean hasConnecter() {
    return connecter != null;
  }

  long getLastSendTime() {
    return lastSendTime;
  }

  long getLastRcvTime() {
    return lastRcvTime;
  }

  long getLastStateChange() {
    return lastStateChange;
  }

  ChannelState getPrevState() {
    return prevState;
  }



  abstract class ChannelRunner extends LockssRunnable {
    volatile Thread thread;
    TimerQueue.Request timerReq;
    private volatile boolean goOn = true;

    public ChannelRunner() {
      super("Runner");
    }

    abstract void doRunner();

    public void lockssRun() {
      try {
	thread = Thread.currentThread();
	setRunnerName();

	setPriority(BlockingStreamComm.PRIORITY_PARAM_CHANNEL,
		    BlockingStreamComm.PRIORITY_DEFAULT_CHANNEL);
// 	triggerWDogOnExit(true);
// 	startWDog(BlockingStreamComm.WDOG_PARAM_CHANNEL,
// 		  BlockingStreamComm.WDOG_DEFAULT_CHANNEL);

	doRunner();
      } finally {
// 	stopWDog();
// 	setRunnerName("ChanAvail");
      }
//       triggerWDogOnExit(false);
    }

    public boolean goOn() {
      return goOn;
    }

    public synchronized void stopRunner() {
//       triggerWDogOnExit(false);
      goOn = false;
      cancelTimeout();
      if (thread != null) {
	thread.interrupt();
      }
    }

    /** Cancel any timeout waiting to interrupt this thread
     */
    public void cancelTimeout() {
      if (timerReq != null) {
	TimerQueue.cancel(timerReq);
      }
    }

    abstract String getRunnerName();

    String getRunnerName(String name) {
      if (peer == null) {
	return name;
      } else {
	return name + ": " + peer.getIdString();
      }
    }

    public void setRunnerName() {
      setRunnerName(getRunnerName());
    }

    public void setRunnerName(String name) {
      if (thread != null) {
	String oldName = thread.getName();
	if (!oldName.equals(name)) {
	  thread.setName(name);
	  log.threadNameChanged();
	}
      }
    }
  }

  class ChannelConnecter extends ChannelRunner {

    public void doRunner() {
      connect(this);
    }

    public void setTimeout(long timeout) {
      timerReq =
	TimerQueue.schedule(Deadline.in(timeout),
			    new TimerQueue.Callback() {
			      public void timerExpired(Object cookie) {
				if (state == ChannelState.CONNECTING) {
				  stopRunner();
				}
			      }
			      public String toString() {
				return "Channel connector " + peer;
			      }
			    },
			    null);
    }

    String getRunnerName() {
      return getRunnerName("ChanConnecter");
    }
  }

  class ChannelReader extends ChannelRunner {

    public void doRunner() {
      handleInputStream(this);
    }

    public void setTimeout(long timeout) {
      timerReq = TimerQueue.schedule(Deadline.in(timeout), closer,
				     new Long(timeout));
    }

    String getRunnerName() {
      return getRunnerName("ChanReader");
    }

    class Closer implements TimerQueue.Callback {
      public void timerExpired(Object cookie) {
	if (state == ChannelState.DRAIN_INPUT) {
	  if (TimeBase.msSince(lastActiveTime) >
	      scomm.getDrainInputTime()) {
	    abortChannel(p()+"Aborting DRAIN_INPUT");
	  } else {
	    setTimeout(((Long)cookie).longValue());
	  }
	}
      }
      public String toString() {
	return "Draining channel closer " + peer;
      }
    }

    Closer closer = new Closer();
  }

  class ChannelWriter extends ChannelRunner {

    public void doRunner() {
      handleOutputStream(this);
    }

    String getRunnerName() {
      return getRunnerName("ChanWriter");
    }
  }
}
