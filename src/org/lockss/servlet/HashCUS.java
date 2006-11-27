/*
 * $Id: HashCUS.java,v 1.32 2006-11-27 06:33:35 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.*;
import java.text.*;
import java.security.*;
import org.mortbay.html.*;
import org.mortbay.util.B64Code;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.daemon.*;

/** Hash a CUS on demand, display the results and filtered input stream
 */
public class HashCUS extends LockssServlet {
  static int MAX_RECORD = 100 * 1024;

  static final String KEY_AUID = "auid";
  static final String KEY_URL = "url";
  static final String KEY_LOWER = "lb";
  static final String KEY_UPPER = "ub";
  static final String KEY_CHALLENGE = "challenge";
  static final String KEY_VERIFIER = "verifier";
  static final String KEY_HASH_TYPE = "hashtype";
  static final String KEY_RECORD = "record";
  static final String KEY_ACTION = "action";
  static final String KEY_MIME = "mime";
  static final String KEY_FILE_WHICH = "file";
  static final String WHICH_STREAM = "stream";
  static final String WHICH_BLOCKS = "blocks";

  static final String SESSION_KEY_STREAM_FILE = "hashcus_stream_file";
  static final String SESSION_KEY_BLOCK_FILE = "hashcus_block_file";

  static final String HASH_STRING_CONTENT = "Content";
  static final String HASH_STRING_NAME = "Name";
  static final String HASH_STRING_SNCUSS = "Sncuss";
  static final String HASH_STRING_V3 = "V3";

  static final int HASH_TYPE_CONTENT = 1;
  static final int HASH_TYPE_NAME = 2;
  static final int HASH_TYPE_SNCUSS = 3;
  static final int HASH_TYPE_V3 = 4;
  static final int HASH_TYPE_MAX = 4;

  static final String ACTION_HASH = "Hash";
  static final String ACTION_STREAM = "Stream";

  static final String COL2 = "colspan=2";
  static final String COL2CENTER = COL2 + " align=center";

  static final String FOOT_EXPLANATION =
    "Calculates hash in the servlet runner thread, " +
    "so may cause other scheduled hashes to time out. " +
    "Beware hashing a large CUS. " +
    "There is also currently no way to interrupt the hash.";
  static final String FOOT_URL =
    "To specify a whole AU, enter <code>LOCKSSAU:</code>. " +
    "Think twice before doing this.";
  static final String FOOT_BIN =
    "May cause browser to try to render binary data";

  static Logger log = Logger.getLogger("HashCUS");

  private LockssDaemon daemon;
  private PluginManager pluginMgr;

  String auid;
  String url;
  String upper;
  String lower;
  byte[] challenge;
  byte[] verifier;

  HttpSession session;
  boolean isHash;
  boolean isRecord;
  File recordFile;
  OutputStream recordStream;
  File blockFile;
  String hashName;
  int hashType;
  ArchivalUnit au;
  CachedUrlSet cus;
  CachedUrlSetHasher cush;
  int nbytes = 1000;
  long elapsedTime;

  MessageDigest digest;
  byte[] hashResult;
  int bytesHashed;
  boolean showResult;
  protected void resetLocals() {
    resetVars();
    super.resetLocals();
  }

  void resetVars() {
    auid = null;
    url = null;
    upper = null;
    lower = null;
    challenge = null;
    verifier = null;
    session = null;

    isHash = true;
    isRecord = false;
    recordFile = null;
    recordStream = null;

    challenge = null;
    verifier = null;

    nbytes = 1000;

    bytesHashed = 0;
    digest = null;
    hashResult = null;
    showResult = false;
    errMsg = null;
    statusMsg = null;
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    daemon = getLockssDaemon();
    pluginMgr = daemon.getPluginManager();
  }


  public void lockssHandleRequest() throws IOException {
    resetVars();
    String action = getParameter(KEY_ACTION);

    if (ACTION_STREAM.equals(action)) {
      if (sendStream()) {
	return;
      }
    } else
      if (ACTION_HASH.equals(action)) {
	if (checkParams()) {
	  doit();
	}
      }
    displayPage();
  }

  boolean sendStream() {
    String whichFile = getParameter(KEY_FILE_WHICH);
    String file;
    if (WHICH_STREAM.equals(whichFile)) {
      file = (String)getSession().getAttribute(SESSION_KEY_STREAM_FILE);
    } else if (WHICH_BLOCKS.equals(whichFile)) {
      file = (String)getSession().getAttribute(SESSION_KEY_BLOCK_FILE);
    } else {
      errMsg = "Unknown file: " + whichFile;
      return false;
    }
    String mime = getParameter(KEY_MIME);
    try {
      if (mime != null) {
	resp.setContentType(mime);
      }
      InputStream in = new BufferedInputStream(new FileInputStream(file));
      OutputStream out = resp.getOutputStream();
      org.mortbay.util.IO.copy(in, out);
      in.close();
      return true;
    } catch (IOException e) {
      log.debug("sendStream()", e);
      errMsg = "Error sending file: " + e.toString();
      return false;
    }
  }

  private boolean checkParams() {
    auid = getParameter(KEY_AUID);
    url = getParameter(KEY_URL);
    lower = getParameter(KEY_LOWER);
    upper = getParameter(KEY_UPPER);
    isRecord = (getParameter(KEY_RECORD) != null);
    String hashIntStr = getParameter(KEY_HASH_TYPE);
    try {
      hashType = Integer.parseInt(hashIntStr);
    } catch (UnsupportedOperationException e) {
      errMsg = "Unknown hash type: " + hashIntStr;
      return false;
    }
    if (hashType <= 0 || hashType > HASH_TYPE_MAX) {
      errMsg = "Unknown hash type: " + hashType;
      return false;
    }

    if (auid == null) {
      errMsg = "Select an AU";
      return false;
    }
    au = pluginMgr.getAuFromId(auid);
    if (au == null) {
      errMsg = "No such AU.  Select an AU";
      return false;
    }
    if (url == null) {
      url = AuCachedUrlSetSpec.URL;
//       errMsg = "URL required";
//       return false;
    }
    try {
      challenge = getB64Param(KEY_CHALLENGE);
    } catch (IllegalArgumentException e) {
      errMsg = "Challenge: Illegal Base64 string: " + e.getMessage();
      return false;
    }
    try {
      verifier = getB64Param(KEY_VERIFIER);
    } catch (IllegalArgumentException e) {
      errMsg = "Verifier: Illegal Base64 string: " + e.getMessage();
      return false;
    }
    PollSpec ps;
    try {
      switch (hashType) {
      case HASH_TYPE_SNCUSS:
	if (upper != null ||
	    (lower != null && !lower.equals(PollSpec.SINGLE_NODE_LWRBOUND))) {
	  errMsg = "Upper/Lower ignored";
	}
	ps = new PollSpec(auid,
			  url,
			  PollSpec.SINGLE_NODE_LWRBOUND,
			  null,
			  Poll.V1_CONTENT_POLL);
	break;
      case HASH_TYPE_V3:
	ps = new PollSpec(auid, url, lower, upper, Poll.V3_POLL);
	break;
      default:
	ps = new PollSpec(auid, url, lower, upper, Poll.V1_CONTENT_POLL);
      }
    } catch (Exception e) {
      errMsg = "Error making PollSpec: " + e.toString();
      log.debug("Making Pollspec", e);
      return false;
    }
    log.debug(""+ps);
    cus = ps.getCachedUrlSet();
    if (cus == null) {
      errMsg = "No such CUS: " + ps;
      return false;
    }
    log.debug(""+cus);
    return true;
  }

  private byte[] getB64Param(String key) {
    String val = getParameter(key);
    if (val == null) {
      return null;
    }
    return B64Code.decode(val.toCharArray());
  }

  private void displayPage() throws IOException {
    Page page = newPage();
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, "Hash a CachedUrlSet" +
	addFootnote(FOOT_EXPLANATION));
    page.add(makeForm());
    page.add("<br>");
    if (showResult) {
      switch (hashType) {
      case HASH_TYPE_CONTENT:
      case HASH_TYPE_SNCUSS:
      case HASH_TYPE_NAME:
	page.add(makeV1Result());
	break;
      case HASH_TYPE_V3:
	page.add(makeV3Result());
	break;
      }

    }
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  private static final NumberFormat fmt_2dec = new DecimalFormat("0.00");

  private Element makeV1Result() {
    Table tbl = new Table(0, "align=center");
    tbl.newRow();
    tbl.addHeading("Hash Result", COL2);

    addResultRow(tbl, "CUSS", cus.getSpec().toString());
    if (challenge != null) {
      addResultRow(tbl, "Challenge", byteString(challenge));
    }
    if (verifier != null) {
      addResultRow(tbl, "Verifier", byteString(verifier));
    }
    addResultRow(tbl, "Size", Integer.toString(bytesHashed));
    addResultRow(tbl, "Hash", byteString(hashResult));

    addResultRow(tbl, "Time", getElapsedString());

    addRecordFile(tbl);
    return tbl;
  }

  private void addRecordFile(Table tbl) {
    if (recordFile != null && recordFile.exists()) {
      tbl.newRow("valign=bottom");
      tbl.newCell();
      tbl.add("Stream:");
      if (recordFile.length() < bytesHashed) {
	tbl.add(addFootnote("First " + recordFile.length() + " bytes only."));
      }
      tbl.add(":");
      tbl.newCell();
      getSession().setAttribute(SESSION_KEY_STREAM_FILE,
				recordFile.toString());
      Properties p = new Properties();
      p.setProperty(KEY_ACTION, ACTION_STREAM);
      p.setProperty(KEY_FILE_WHICH, WHICH_STREAM);
      p.setProperty(KEY_MIME, "application/octet-stream");
      tbl.add(srvLink(myServletDescr(), "binary", concatParams(p)));
      tbl.add("&nbsp;&nbsp;");
      p.setProperty(KEY_MIME, "text/plain");
      tbl.add(srvLink(myServletDescr(), "text", concatParams(p)));
      tbl.add(addFootnote(FOOT_BIN));
      

    }
  }

  private Element makeV3Result() {
    Table tbl = new Table(0, "align=center");
    tbl.newRow();
    tbl.addHeading("Hash Result", COL2);

    addResultRow(tbl, "CUSS", cus.getSpec().toString());
    addResultRow(tbl, "Size", Integer.toString(bytesHashed));
    addResultRow(tbl, "Time", getElapsedString());
    if (blockFile != null && blockFile.exists()) {
      tbl.newRow();
      tbl.newCell();
      tbl.add("Hash file");
      tbl.add(":");
      tbl.newCell();
      getSession().setAttribute(SESSION_KEY_BLOCK_FILE, blockFile.toString());
      Properties p = new Properties();
      p.setProperty(KEY_ACTION, ACTION_STREAM);
      p.setProperty(KEY_FILE_WHICH, WHICH_BLOCKS);
      p.setProperty(KEY_MIME, "text/plain");
      tbl.add(srvLink(myServletDescr(), "HashFile", concatParams(p)));
    }
    addRecordFile(tbl);
    return tbl;
  }

  String getElapsedString() {
    String s = StringUtil.protectedDivide(bytesHashed, elapsedTime, "inf");
    if (!"inf".equalsIgnoreCase(s) && Long.parseLong(s) < 100) {
      double fbpms = ((double)bytesHashed) / ((double)elapsedTime);
      s = fmt_2dec.format(fbpms);
    }
    return elapsedTime + " ms, " + s + " bytes/ms";
  }

  HttpSession getSession() {
    if (session == null) {
      session = req.getSession(true);
      setSessionTimeout(session);
    }
    return session;
  }

  void addResultRow(Table tbl, String head, Object value) {
    tbl.newRow();
    tbl.newCell();
    tbl.add(head);
    tbl.add(":");
    tbl.newCell();
    tbl.add(value.toString());
  }

  private Element makeForm() {
    Composite comp = new Composite();
    Block centeredBlock = new Block(Block.Center);

    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");

    Table autbl = new Table(0, "cellpadding=0");
    autbl.newRow();
    autbl.addHeading("Select AU");
    Select sel = new Select(KEY_AUID, false);
    sel.add("", auid == null, "");
    for (Iterator iter = pluginMgr.getAllAus().iterator(); iter.hasNext(); ) {
      ArchivalUnit au0 = (ArchivalUnit)iter.next();
      String id = au0.getAuId();
      sel.add(au0.getName(), id.equals(auid), id);
    }
    autbl.newRow(); autbl.newCell();
    setTabOrder(sel);
    autbl.add(sel);

    Table tbl = new Table(0, "cellpadding=0");
    tbl.newRow();
    tbl.newCell(COL2CENTER);
    tbl.add(autbl);
    tbl.newRow();
    tbl.newCell();
    tbl.add("&nbsp;");

    addInputRow(tbl, "URL" + addFootnote(FOOT_URL), KEY_URL, 50, url);
    addInputRow(tbl, "Lower", KEY_LOWER, 50, lower);
    addInputRow(tbl, "Upper", KEY_UPPER, 50, upper);
    addInputRow(tbl, "Challenge", KEY_CHALLENGE, 50,
		getParameter(KEY_CHALLENGE));
    addInputRow(tbl, "Verifier", KEY_VERIFIER, 50, getParameter(KEY_VERIFIER));
    tbl.newRow();
    tbl.newCell(COL2CENTER);
    tbl.add(radioButton(HASH_STRING_V3,
			Integer.toString(HASH_TYPE_V3),
			KEY_HASH_TYPE,
			(hashType == 0 || hashType == HASH_TYPE_V3)));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_CONTENT,
			Integer.toString(HASH_TYPE_CONTENT),
			KEY_HASH_TYPE,
			hashType == HASH_TYPE_CONTENT));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_NAME,
			Integer.toString(HASH_TYPE_NAME),
			KEY_HASH_TYPE,
			hashType == HASH_TYPE_NAME));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_SNCUSS,
			Integer.toString(HASH_TYPE_SNCUSS),
			KEY_HASH_TYPE,
			hashType == HASH_TYPE_SNCUSS));
    tbl.newRow();
    tbl.newCell(COL2CENTER);
    tbl.add(checkBox("Record filtered stream", "true", KEY_RECORD, isRecord));

    centeredBlock.add(tbl);
    frm.add(centeredBlock);
    Input submit = new Input(Input.Submit, KEY_ACTION, ACTION_HASH);
    setTabOrder(submit);
    frm.add("<br><center>"+submit+"</center>");
    comp.add(frm);
    return comp;
  }

  void addInputRow(Table tbl, String label, String key,
		   int size, String initVal) {
    tbl.newRow();
    //     tbl.newCell();
    tbl.addHeading(label + ":", "align=right");
    tbl.newCell();
    Input in = new Input(Input.Text, key, initVal);
    in.setSize(size);
    setTabOrder(in);
    tbl.add(in);
  }

  private void doit() {
    try {
      if (isHash) {
	digest = LcapMessage.getDefaultMessageDigest();
	if (digest == null) {
	  errMsg = "Can't get default MessageDigest";
	  return;
	}
	if (isRecord) {
	  recordFile = File.createTempFile("HashCUS", ".tmp");
	  recordStream =
	    new BufferedOutputStream(new FileOutputStream(recordFile));
	  digest = new RecordingMessageDigest(digest, recordStream,
					      MAX_RECORD);
// 	  recordFile.deleteOnExit();
	}
	CachedUrlSetHasher cush = null;
	switch (hashType) {
	case HASH_TYPE_CONTENT:
	case HASH_TYPE_SNCUSS:
	  initDigest(digest);
	  doV1(cus.getContentHasher(digest));
	  break;
	case HASH_TYPE_NAME:
	  initDigest(digest);
	  doV1(cus.getNameHasher(digest));
	  break;
	case HASH_TYPE_V3:
	  doV3();
	  break;
	}
      } else {
      }
    } catch (Exception e) {
      log.warning("doit()", e);
      errMsg = "Error hashing: " + e.toString();
    }
    IOUtil.safeClose(recordStream);
  }

  private void initDigest(MessageDigest digest) {
    if (challenge != null) {
      digest.update(challenge, 0, challenge.length);
    }
    if (verifier != null) {
      digest.update(verifier, 0, verifier.length);
    }
  }

  private class BlockEventHandler implements BlockHasher.EventHandler {
    PrintStream outs;
    BlockEventHandler(PrintStream outs) {
      this.outs = outs;
    }
      
    public void blockDone(HashBlock block) {
      HashBlock.Version ver = block.currentVersion();
      String url = block.getUrl();
      outs.println(byteString(ver.getHashes()[0]) + "   " + block.getUrl());
    }
  }

  private byte[][] initHasherByteArrays() {
    byte[][] initBytes = new byte[1][];
    initBytes[0] = ((challenge != null)
		    ? ( (verifier != null)
			? ByteArray.concat(challenge, verifier)
			: challenge)
		    : (verifier != null) ? verifier : new byte[0]);
    return initBytes;
  }

  private MessageDigest[] initHasherDigests() {
    MessageDigest[] digests = new MessageDigest[1];
    digests[0] = digest;
    return digests;
  }

  private void doV1(CachedUrlSetHasher cush) {
    doHash(cush);
    hashResult = digest.digest();
  }

  private void doV3() throws IOException {
    blockFile = FileUtil.createTempFile("HashCUS", ".tmp");
    PrintStream blockOuts =
      new PrintStream(new BufferedOutputStream(new FileOutputStream(blockFile)));
    blockOuts.println("# Block hashes from " + getMachineName() + ", " +
		      ServletUtil.headerDf.format(new Date()));
    blockOuts.println("# AU: " + au.getName());
    BlockHasher hasher = new BlockHasher(cus, 1,
					 initHasherDigests(),
					 initHasherByteArrays(),
					 new BlockEventHandler(blockOuts));

    if (challenge != null) {
      blockOuts.println("# " + "Poller nonce: " + byteString(challenge));
    }
    if (verifier != null) {
      blockOuts.println("# " + "Voter nonce: " + byteString(verifier));
    }
    doHash(hasher);
    blockOuts.close();
  }

  private void doHash(CachedUrlSetHasher cush) {
    bytesHashed = 0;
    long startTime = TimeBase.nowMs();
    try {
      while (!cush.finished()) {
	bytesHashed += cush.hashStep(nbytes);
      }
    } catch (Exception e) {
      log.warning("doHash()", e);
      errMsg = "Error hashing: " + e.toString();
    }
    elapsedTime = TimeBase.msSince(startTime);

    log.debug("Bytes hashed: " + bytesHashed);
    showResult = true;
  }

  String byteString(byte[] a) {
    return String.valueOf(B64Code.encode(a));
  }

  String optByteString(byte[] a) {
    return a ==null ? null : byteString(a);
  }
}
