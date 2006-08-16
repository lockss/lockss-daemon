/*
 * $Id: BlockingPeerChannel.java,v 1.15 2006-08-16 00:13:04 dshr Exp $
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

package org.lockss.protocol;

import java.io.*;
import java.net.*;

import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.daemon.*;

/** Manages a stream connection to a peer.  Channels are ephemeral, coming
 * and going as needed.
 */
class BlockingPeerChannel implements PeerChannel {
  static Logger log = Logger.getLogger("Channel");

  static final int COPY_BUFFER_SIZE = 256;

  // Channel state
  static final int STATE_INIT = 0;
  static final int STATE_CONNECTING = 1;
  static final int STATE_CONNECT_FAIL = 2;
  static final int STATE_ACCEPTED = 3;
  static final int STATE_STARTING = 4;
  static final int STATE_OPEN = 5;
  static final int STATE_DRAIN_INPUT = 6;
  static final int STATE_DRAIN_OUTPUT = 7;
  static final int STATE_CLOSING = 8;
  static final int STATE_CLOSED = 9;

  private int state;
  private Object stateLock = new Object();

  volatile private PeerIdentity peer;
  volatile private PeerAddress pad;
  private PeerIdentity localPeer;
  private BlockingStreamComm scomm;
  private Socket sock;
  private Queue rcvQueue;
  private Queue sendQueue;
  private InputStream ins;
  private OutputStream outs;

  volatile private long lastSendTime = 0;
  volatile private long lastRcvTime = 0;
  volatile private long lastActiveTime = 0;

  volatile private ChannelRunner reader;
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

  private Stats stats = new Stats();

  /** All other constructors should call this one
   */
  private BlockingPeerChannel(BlockingStreamComm scomm) {
    this.scomm = scomm;
    localPeer = scomm.getMyPeerId();
    rcvQueue = scomm.getReceiveQueue();
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
    setState(STATE_INIT);
  }

  /** Create a channel from an existing (incoming) connection socket.  The
   * peer identity is not yet known.
   * @param scomm parent stream comm
   * @param sock the socket open to the peer
   */
  BlockingPeerChannel(BlockingStreamComm scomm, Socket sock) {
    this(scomm);
    this.sock = sock;
    setState(STATE_ACCEPTED);
  }

  /** Used by tests only */
  BlockingPeerChannel(BlockingStreamComm scomm, PeerIdentity peer,
		      InputStream ins, OutputStream outs) {
    this(scomm);
    this.peer = peer;
    this.ins = ins;
    this.outs = outs;
  }

  /** Return our peer, or null if not known yet */
//   public PeerIdentity getPeer() {
//     return peer;
//   }

  long getLastActiveTime() {
    return lastActiveTime;
  }

  private void setState(int newState) {
    if (log.isDebug3()) log.debug3(p()+"State: " + state + " -> " + newState);
    state = newState;
  }

  int getState() {
    return state;
  }

  /** If currently in state <code>from</code>, transition to state
   * <code>to</code> and return true.  Otherwise return false or, if
   * <code>errmsg</code> is not null, throw an exception
   */
  boolean stateTrans(int from, int to, String errmsg) {
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
  boolean stateTrans(int from, int to) {
    return stateTrans(from, to, null);
  }

  /** If currently not in state <code>notFrom</code>, transition to state
   * <code>to</code> and return true.  Otherwise return false or, if
   * <code>errmsg</code> is not null, throw an exception
   */
  boolean notStateTrans(int notFrom, int to, String errmsg) {
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
  boolean notStateTrans(int[] notFrom, int to, String errmsg) {
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
  boolean notStateTrans(int notFrom, int to) {
    return notStateTrans(notFrom, to, null);
  }

  /** If currently not in any state in <code>notFrom</code>, transition to
   * state <code>to</code> and return true, otherwise return false
   */
  boolean notStateTrans(int[] notFrom, int to) {
    return notStateTrans(notFrom, to, null);
  }

  /** Throw an IllegalStateException if not in the expectedState
   */
  void assertState(int expectedState, String errmsg) {
    if (state != expectedState)
      throw new IllegalStateException(errmsg + " in state " + state +
				      ", expected " + expectedState);
  }

  /** True if in one of the specified states
   */
  boolean isState(int state, int[] oneOf) {
    for (int ix = 0; ix < oneOf.length; ix++) {
      if (state == oneOf[ix]) return true;
    }
    return false;
  }

  // These are the main entry points from BlockingStreamComm

  /** Start thread to connect to the peer and start channel threads.
   */
  public void startOriginate() throws IOException {
    // arrange for possible MalformedIdentityKeyException to be thrown
    // here, not in thread
    pad = peer.getPeerAddress();
    if (!(pad instanceof PeerAddress.Tcp)) {
      throw new IllegalArgumentException("Wrong type of PeerAddress: " + pad);
    }
    if (stateTrans(STATE_INIT, STATE_CONNECTING, "startOriginate")) {
      ChannelConnecter runner = new ChannelConnecter();
      runner.setTimeout(scomm.getConnectTimeout());
      try {
	scomm.execute(runner);
	wtConnecter = connecter = runner;
      } catch (InterruptedException e) {
      // Can happen if we get aborted while starting pool thread
	log.warning("startOriginate()", e);
	stateTrans(STATE_CONNECTING, STATE_CLOSED);
	throw new IOException(e.toString());
      } catch (Exception e) {
	log.warning("Can't start channel connecter", e);
	stateTrans(STATE_CONNECTING, STATE_CLOSED);
	throw new IOException(e.toString());
      }
    }
  }

  /** Start threads in response to incoming connection
   */
  public void startIncoming() {
    if (stateTrans(STATE_ACCEPTED, STATE_STARTING, "startIncoming")) {
      try {
	startConnectedChannel();
      } catch (IOException e) {
	abortChannel();
      }
    }
  }

  /** Send a message to our peer, return true iff we expect to be able to
   * send it
   */
  public boolean send(PeerMessage msg) {
    synchronized (stateLock) {
      switch (state) {
      case STATE_CLOSED:
      case STATE_CLOSING:
      case STATE_CONNECT_FAIL:
	return false;
      default:
	sendQueue.put(msg);
	return true;
      }
    }
  }

  /** Initialize streams, start reader and writer threads
   */
  private void startConnectedChannel() throws IOException {
    assertState(STATE_STARTING, "startConnectedChannel");
    try {
      sock.setSoTimeout((int)scomm.getSoTimeout());
      boolean nodelay = scomm.isTcpNodelay();
      if (log.isDebug3()) log.debug3(p()+"Setting NoDelay " + nodelay);
      sock.setTcpNoDelay(nodelay);
      ins = sock.getInputStream();
      outs = sock.getOutputStream();
      if (scomm.isBufferedSend()) {
	outs = new BufferedOutputStream(outs, COPY_BUFFER_SIZE);
	if (log.isDebug3()) log.debug3(p()+"Buffering output");
      }
      // writer must be started first as reader refers to writer thread (to
      // set name when peerid received)
      startWriter();
      startReader();
      stateTrans(STATE_STARTING, STATE_OPEN);
    } catch (IOException e) {
      log.error("Channel didn't start", e);
      throw e;
    } catch (Exception e) {
      log.error("Channel didn't start", e);
      throw new IOException(e.toString());
    }
  }

  void abortChannel() {
    stopChannel(true);
  }

  void stopChannel() {
    stopChannel(false);
  }

  static int[] stopIgnStates = {STATE_INIT, STATE_CLOSED, STATE_CLOSING};

  void stopChannel(boolean abort) {
    if (notStateTrans(stopIgnStates, STATE_CLOSING)) {
      if (abort && peer != null) log.warning("Aborting " + peer.getIdString());
      scomm.dissociateChannelFromPeer(this, peer);
      IOUtil.safeClose(sock);
      IOUtil.safeClose(ins);
      IOUtil.safeClose(outs);
      connecter = stopThread(connecter);
      reader = stopThread(reader);
      writer = stopThread(writer);
      stateTrans(STATE_CLOSING, STATE_CLOSED);
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
    if ((state == STATE_OPEN || state == STATE_DRAIN_OUTPUT) &&
	lastActiveTime != 0 &&
	!isSendIdle() &&
	TimeBase.msSince(lastActiveTime) > scomm.getChannelHungTime()) {
      log.warning(p()+"Hung sending");
      abortChannel();
    }
  }

  /** Open a connection to our peer; start things running if it works
   */
  void connect(ChannelConnecter connector) {
    assertState(STATE_CONNECTING, "connect");
    if (pad instanceof PeerAddress.Tcp) {
      PeerAddress.Tcp tpad = (PeerAddress.Tcp)pad;
      try {
	sock = scomm.getSocketFactory().newSocket(tpad.getIPAddr(),
						  tpad.getPort());
	connector.cancelTimeout();
	log.debug2("Connected to " + peer);
      } catch (IOException e) {
	connector.cancelTimeout();
	log.warning("Connect failed to " + peer + ": " + e.toString());
	stateTrans(STATE_CONNECTING, STATE_CONNECT_FAIL);
	abortChannel();
	return;
      }
      try {
	stateTrans(STATE_CONNECTING, STATE_STARTING);
	startConnectedChannel();
      } catch (IOException e) {
	abortChannel();
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
      log.warning("startReader()", e);
      abortChannel();
    } catch (RuntimeException e) {
      log.warning("startReader()", e);
      abortChannel();
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
      log.warning("startWriter()", e);
      abortChannel();
    } catch (RuntimeException e) {
      log.warning("startWriter()", e);
      abortChannel();
    }
  }

  // Message reception, invoked by ChannelReader

  /** Process messages until error or stream closed, then close the channel,
   * cleanly if possible
   */
  void handleInputStream(ChannelRunner runner) {
    try {
      while (true) {
	try {
	  readMessages(runner);
	  // input stream closed by peer, drain output if necessary
	  synchronized (stateLock) {
	    if (isSendIdle()) {
	      stopChannel();
	    } else {
	      stateTrans(STATE_OPEN, STATE_DRAIN_OUTPUT);
	    }
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
    } catch (SocketException e) {
      // Expected when closing
      if (!(state == STATE_CLOSED || state == STATE_CLOSING)) {
	log.warning("handleInputStream: " + e.toString());
      }
      abortChannel();
    } catch (IOException e) {
      // These are unexpected
      if (log.isDebug3()) {
	log.warning("handleInputStream", e);
      } else {
	log.warning("handleInputStream: " + e.toString());
      }
      abortChannel();
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
	  if (state != STATE_CLOSING) {
	    // reader, writer can get set to null while in STATE_CLOSING
	    if (reader != null) reader.setRunnerName();
	    if (writer != null) writer.setRunnerName();
	  }
	}
	break;
      case OP_DATA:
	readDataMsg();
	break;
      default:
	throw new ProtocolException("Received unknown opcode: " + op);
      }
    }
  }

  /** Read a peer id message, verify peer's id, tell comm to associate us
   * with id if not already.
   */
  void readPeerId() throws IOException {
    int len = getRcvdMessageLength();
    if (len > MAX_PEERID_LEN) {
      String msg = "Peerid too long: " + len;
      log.warning(msg);
      throw new ProtocolException(msg);
    }
    if (!readBuf(peerbuf, len)) {
      String msg = "No data in Peerid message";
      log.warning(msg);
      throw new ProtocolException(msg);
    }
    String peerkey = new String(peerbuf, 0, len);
    PeerIdentity pid = scomm.findPeerIdentity(peerkey);
    if (peer == null) {
      peer = pid;
      log.debug3("Got peer: " + peer);
      // ensure a compatible peer address
      PeerAddress hispad = peer.getPeerAddress();
      if (!hispad.isStream()) {
	throw new ProtocolException("Incompatible PeerAddress type: " +
				    hispad);
      }
      // XXX If this is an incoming connection, need to make outgoing
      // connection to peerid just received, send an echo nonce message
      // over that connection and ensure we receive the nonce on this
      // connection, then close outgoing conn
      scomm.associateChannelWithPeer(this, peer);
    } else if (!pid.equals(peer)) {
      String msg = "Received conflicting peerid msg: " + pid + " was: " + peer;
      log.warning(msg);
      throw new ProtocolException(msg);
    }
  }

  /** Read a data message into a new PeerMessage and enqueue it
   */
  void readDataMsg() throws IOException {
    int len = getRcvdMessageLength();
    int proto = ByteArray.decodeInt(rcvHeader, HEADER_OFF_PROTO);
    if (log.isDebug3()) log.debug3("Got data hdr: " + proto + ", len: " + len);
    if (len > scomm.getMaxMessageSize()) {
      throw new ProtocolException("Too-large incoming message: " + len);
    }
    PeerMessage msg = scomm.newPeerMessage(len);
    msg.setProtocol(proto);
    msg.setSender(peer);
    try {
      OutputStream msgOut = msg.getOutputStream();
      copyBytes(ins, msgOut, len);
      msgOut.close();
      // update lastActiveTime *before* queuing message; produces more
      // predictable behavior when running in simulated time in unit tests
      lastRcvTime = lastActiveTime = TimeBase.nowMs();
      rcvQueue.put(msg);
      stats.msgsRcvd++;
    } catch (IOException e) {
      msg.delete();
      throw e;
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
      log.debug2("Input closed");
      return false;
    }
    if (rcvHeader[HEADER_OFF_CHECK] != HEADER_CHECK) {
      throw new ProtocolException("Message doesn't start with " +
				  HEADER_CHECK);
    }
    return true;
  }

  int getRcvdMessageLength() {
    return ByteArray.decodeInt(rcvHeader, HEADER_OFF_LEN);
  }

  int getRcvdMessageOp() {
    return ByteArray.decodeByte(rcvHeader, HEADER_OFF_OP);
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
	  writeDataMsg(msg);
	  stats.msgsSent++;
	  // remove the message just sent
	  if (msg != sendQueue.get(Deadline.EXPIRED)) {
	    throw new IllegalStateException("Send queue not behaving as FIFO");
	  }
	  msg.delete();
	  lastSendTime = lastActiveTime = TimeBase.nowMs();
	  synchronized (stateLock) {
	    // if draining output and nothing left to send, close.  Check
	    // now rather than waiting for peekWait() to timeout.
	    if (state == STATE_DRAIN_OUTPUT && isSendIdle()) {
	      stopChannel();
	      return;
	    }
	  }
	}
	synchronized (stateLock) {
	  if (!isSendIdle()) {
	    // if something got queued before state locked, continue sending
	    continue;
	  }
	  // if draining output, close.  Must check this again because it
	  // might have become true during peekWait()
	  if (state == STATE_DRAIN_OUTPUT) {
	    stopChannel();
	    return;
	  }
	  if (TimeBase.msSince(lastActiveTime) > scomm.getChannelIdleTime()) {
	    // time to close channel.  shutdown output only in case peer is
	    // now sending message
	    setState(STATE_DRAIN_INPUT);
	    try {
	      log.debug2("Shutdown output");
	      sock.shutdownOutput();
	      break;
	    } catch (IOException e) {
	      log.debug("shutdownOutput", e);
	      abortChannel();
	      break;
	    } catch (UnsupportedOperationException e) {
	      log.debug("shutdownOutput() not implemented for SSL");
	      abortChannel();
	    }
	  }
	}
      }
    } catch (InterruptedException e) {
      abortChannel();
    } catch (IOException e) {
      abortChannel();
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
  }

  /** send data msg
   */
  void writeDataMsg(PeerMessage msg) throws IOException {
    if (log.isDebug3()) log.debug3("Sending data: " + msg.getProtocol() +
				   ", len: " + msg.getDataSize());
    writeHeader(OP_DATA, msg.getDataSize(), msg.getProtocol());
    copyBytes(msg.getInputStream(), outs, msg.getDataSize());
    outs.flush();
  }

  /** send msg header
   */
  void writeHeader(int op, int len, int proto) throws IOException {
    sndHeader[HEADER_OFF_CHECK] = HEADER_CHECK;
    sndHeader[HEADER_OFF_OP] = (byte)op;
    ByteArray.encodeInt(len, sndHeader, HEADER_OFF_LEN);
    ByteArray.encodeInt(proto, sndHeader, HEADER_OFF_PROTO);
    outs.write(sndHeader);
  }

  /** Copy len bytes from input to output stream.
   * @return true if len bytes successfully copied
   * @throws ProtocolException if eof reached before len bytes
   * @throws IOException
   */
  boolean copyBytes(InputStream is, OutputStream os, int len)
      throws IOException {
    byte[] copybuf = new byte[COPY_BUFFER_SIZE];
    int rem = len;
    int bufsize = copybuf.length;
    while (rem > 0) {
      int nread = is.read(copybuf, 0, rem > bufsize ? bufsize : rem);
      if (nread < 0) {
	throw new ProtocolException("Connection closed in middle of message");
      }
      os.write(copybuf, 0, nread);
      rem -= nread;
      lastActiveTime = TimeBase.nowMs();
    }
    return true;
  }

  /** Return true if this channel has transmitted any data.  If a channel
   * refuses an outgoing message and this is true then a new channel should
   * be created.  If false then the channel failed on opening so a new
   * channel should not be tried.
   */
  boolean wasOpen() {
    return (stats.msgsSent != 0) && (stats.msgsRcvd != 0);
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

  Stats getStats() {
    return stats;
  }

  static class Stats {
    int msgsSent = 0;
    int msgsRcvd = 0;
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
				if (state == STATE_CONNECTING) {
				  stopRunner();
				}
			      }
			      public String toString() {
				return "Channel connector" + peer;
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

    String getRunnerName() {
      return getRunnerName("ChanReader");
    }
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
