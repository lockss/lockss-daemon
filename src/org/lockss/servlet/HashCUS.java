/*
 * $Id: HashCUS.java,v 1.46 2010-05-12 04:07:52 tlipkis Exp $
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

package org.lockss.servlet;

import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.security.*;
import org.mortbay.html.*;
import org.mortbay.util.B64Code;
import org.apache.commons.lang.StringUtils;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;

/** Hash a CUS on demand, display the results and filtered input stream
 */
public class HashCUS extends LockssServlet {
  /** If set to a positive number, the record of each filtered stream is
   * truncated to that many bytes.  -1 means no limit */
  static final String PARAM_TRUNCATE_FILETERD_STREAM = 
    Configuration.PREFIX + "hashcus.truncateFilteredStream";
  static final long DEFAULT_TRUNCATE_FILETERD_STREAM = 100 * 1024;

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
  static final String KEY_FILE_ID = "file";
  static final String KEY_ALG = "algorithm";
  static final String KEY_RESULT_ENCODING = "encoding";
  static final String KEY_RESULT_TYPE = "result";

  static final String SESSION_KEY_STREAM_FILE = "hashcus_stream_file";
  static final String SESSION_KEY_BLOCK_FILE = "hashcus_block_file";

  static final String HASH_STRING_CONTENT = "Content";
  static final String HASH_STRING_NAME = "Name";
  static final String HASH_STRING_SNCUSS = "One file";
  static final String HASH_STRING_V3_TREE = "Tree";
  static final String HASH_STRING_V3_SNCUSS = "One file";

  enum HashType {V1Content, V1Name, V1File, V3Tree, V3File};

  // Support old numeric input values
  static HashType[] hashTypeCompat = {
    null,
    HashType.V1Content,
    HashType.V1Name,
    HashType.V1File,
    HashType.V3Tree,
    HashType.V3File
  };
  static final HashType DEFAULT_HASH_TYPE = HashType.V3Tree;

  enum ResultEncoding {Base64, Hex};
  static final ResultEncoding DEFAULT_RESULT_ENCODING = ResultEncoding.Hex;

  enum ResultType {File, Inline};
  static final ResultType DEFAULT_RESULT_TYPE = ResultType.File;

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

  boolean isHash;
  boolean isRecord;
  File recordFile;
  OutputStream recordStream;
  File blockFile;
  String hashName;
  HashType hType = DEFAULT_HASH_TYPE;
  ResultEncoding resEncoding = DEFAULT_RESULT_ENCODING;
  ResultType resType = DEFAULT_RESULT_TYPE;
  String alg = LcapMessage.getDefaultHashAlgorithm();
  ArchivalUnit au;
  CachedUrlSet cus;
  SimpleHasher hasher;

  int nbytes = 1000;
  long elapsedTime;

  MessageDigest digest;
  byte[] hashResult;
  long bytesHashed;
  int filesHashed;
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

    isHash = true;
    isRecord = false;
    recordFile = null;
    recordStream = null;

    challenge = null;
    verifier = null;

    nbytes = 1000;

    bytesHashed = 0;
    filesHashed = 0;
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
    } else if (ACTION_HASH.equals(action)) {
      if (checkParams()) {
	doit();
      }
    } else if (!StringUtil.isNullString(action)) {
      errMsg = "Unknown action: " + action;
    }
    if (errMsg == null && resType == ResultType.Inline && isV3(hType)) {
      returnDirectResponse();
    } else {
      displayPage();
    }
  }

  boolean isV3(HashType t) {
    switch (t) {
    case V3Tree:
    case V3File:
      return true;
    default:
      return false;
    }
  }      

  boolean sendStream() {
    if (!hasSession()) {
      errMsg = "Please enable cookies";
      return false;
    }
    String fileId = getParameter(KEY_FILE_ID);
    String file = getSessionIdString(fileId);
    if (StringUtil.isNullString(file)) {
      errMsg = "Unknown file: " + fileId;
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
    alg = req.getParameter(KEY_ALG);
    if (StringUtil.isNullString(alg)) {
      alg = LcapMessage.getDefaultHashAlgorithm();
    }
    String hTypeStr = getParameter(KEY_HASH_TYPE);
    if (StringUtil.isNullString(hTypeStr)) {
      hType = DEFAULT_HASH_TYPE;
    } else if (StringUtils.isNumeric(hTypeStr)) {
      try {
	int hTypeInt = Integer.parseInt(hTypeStr);
	hType = hashTypeCompat[hTypeInt];
	if (hType == null) throw new ArrayIndexOutOfBoundsException();
      } catch (ArrayIndexOutOfBoundsException e) {
	errMsg = "Unknown hash type: " + hTypeStr;
	return false;
      } catch (RuntimeException e) {
	errMsg = "Can't parse hash type: " + hTypeStr;
	return false;
      }
    } else {
      try {
	hType = HashType.valueOf(hTypeStr);
      } catch (IllegalArgumentException e) {
	errMsg = "Unknown hash type: " + hTypeStr;
	return false;
      }
    }
    String resTypeStr = getParameter(KEY_RESULT_TYPE);
    if (StringUtil.isNullString(resTypeStr)) {
      resType = DEFAULT_RESULT_TYPE;
    } else {
      try {
	resType = ResultType.valueOf(resTypeStr);
      } catch (IllegalArgumentException e) {
	errMsg = "Unknown result type: " + resTypeStr;
	return false;
      }
    }
    String resEncodingStr = getParameter(KEY_RESULT_ENCODING);
    if (StringUtil.isNullString(resEncodingStr)) {
      resEncoding = DEFAULT_RESULT_ENCODING;
    } else {
      try {
	resEncoding = ResultEncoding.valueOf(resEncodingStr);
      } catch (IllegalArgumentException e) {
	errMsg = "Unknown result encoding: " + resEncodingStr;
	return false;
      }
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
      switch (hType) {
      case V1File:
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
      case V3Tree:

	ps = new PollSpec(auid, url, lower, upper, Poll.V3_POLL);
	break;
      case V3File:
	ps = new PollSpec(auid, url, PollSpec.SINGLE_NODE_LWRBOUND, null,
			  Poll.V3_POLL);
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
      switch (hType) {
      case V1Content:
      case V1File:
      case V1Name:
	page.add(makeV1Result());
	break;
      case V3Tree:
      case V3File:
	page.add(makeV3Result());
	break;
      }

    }
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  private void returnDirectResponse() throws IOException {
    if (!showResult) {
      displayPage();
    }
    switch (hType) {
    case V1Content:
    case V1File:
    case V1Name:
      errMsg = "Not implemented";
      displayPage();
      break;
    case V3Tree:
    case V3File:
      sendV3DirectResponse();
      break;
    }
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
    addResultRow(tbl, "Size", Long.toString(bytesHashed));

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
      String fileId = getSessionObjectId(recordFile.toString());
      Properties p = new Properties();
      p.setProperty(KEY_ACTION, ACTION_STREAM);
      p.setProperty(KEY_FILE_ID, fileId);
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
    addResultRow(tbl, "Files", Integer.toString(filesHashed));
    addResultRow(tbl, "Size", Long.toString(bytesHashed));
    addResultRow(tbl, "Time", getElapsedString());
    if (blockFile != null && blockFile.exists()) {
      tbl.newRow();
      tbl.newCell();
      tbl.add("Hash file");
      tbl.add(":");
      tbl.newCell();
      String fileId = getSessionObjectId(blockFile.toString());
      Properties p = new Properties();
      p.setProperty(KEY_ACTION, ACTION_STREAM);
      p.setProperty(KEY_FILE_ID, fileId);
      p.setProperty(KEY_MIME, "text/plain");
      tbl.add(srvLink(myServletDescr(), "HashFile", concatParams(p)));
    }
    addRecordFile(tbl);
    return tbl;
  }

  private void sendV3DirectResponse() throws IOException {
    PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");

    if (blockFile == null || !blockFile.exists()) {
      if (errMsg == null) {
	errMsg = "Unknown error - no hash output generated";
	showResult = false;
	displayPage();
	return;
      }
    }
    Reader rdr = new BufferedReader(new FileReader(blockFile));
    org.mortbay.util.IO.copy(rdr, wrtr);
    rdr.close();
    blockFile.delete();
  }

  String getElapsedString() {
    String s = StringUtil.protectedDivide(bytesHashed, elapsedTime, "inf");
    if (!"inf".equalsIgnoreCase(s) && Long.parseLong(s) < 100) {
      double fbpms = ((double)bytesHashed) / ((double)elapsedTime);
      s = fmt_2dec.format(fbpms);
    }
    return elapsedTime + " ms, " + s + " bytes/ms";
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
    Composite sel = ServletUtil.layoutSelectAu(this, KEY_AUID, auid);
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
    addInputRow(tbl, "Algorithm", KEY_ALG, 50, alg);

    tbl.newRow();
    tbl.addHeading("Result:", "align=right");
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(ResultType.File.toString(),
			KEY_RESULT_TYPE,
			resType == ResultType.File));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(ResultType.Inline.toString(),
			KEY_RESULT_TYPE,
			resType == ResultType.Inline));

    tbl.newRow();
    tbl.addHeading("Encoding:", "align=right");
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(ResultEncoding.Hex.toString(),
			KEY_RESULT_ENCODING,
			resEncoding == ResultEncoding.Hex));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(ResultEncoding.Base64.toString(),
			KEY_RESULT_ENCODING,
			resEncoding == ResultEncoding.Base64));

    tbl.newRow();
    tbl.addHeading("V1:", "align=right");
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_CONTENT,
			HashType.V1Content.toString(),
			KEY_HASH_TYPE,
			hType == HashType.V1Content));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_NAME,
			HashType.V1Name.toString(),
			KEY_HASH_TYPE,
			hType == HashType.V1Name));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_SNCUSS,
			HashType.V1File.toString(),
			KEY_HASH_TYPE,
			hType == HashType.V1File));
    tbl.newRow();
    tbl.addHeading("V3:", "align=right");
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_V3_TREE,
			HashType.V3Tree.toString(),
			KEY_HASH_TYPE,
			hType == HashType.V3Tree));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_V3_SNCUSS,
			HashType.V3File.toString(),
			KEY_HASH_TYPE,
			hType == HashType.V3File));

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
	String alg = req.getParameter(KEY_ALG);
	if (StringUtil.isNullString(alg)) {
	  alg = LcapMessage.getDefaultHashAlgorithm();
	}
	try {
	  digest = MessageDigest.getInstance(alg);
	} catch (NoSuchAlgorithmException ex) {
	  errMsg = "Can't get MessageDigest: " + alg;
	  return;
	}
	if (isRecord) {
	  recordFile = File.createTempFile("HashCUS", ".tmp");
	  recordStream =
	    new BufferedOutputStream(new FileOutputStream(recordFile));
	  long truncateTo =
	    CurrentConfig.getLongParam(PARAM_TRUNCATE_FILETERD_STREAM,
				       DEFAULT_TRUNCATE_FILETERD_STREAM);
	  digest = new RecordingMessageDigest(digest, recordStream,
					      truncateTo);
// 	  recordFile.deleteOnExit();
	}

	switch (hType) {
	case V1Content:
	case V1File:
	  doV1(cus.getContentHasher(digest));
	  break;
	case V1Name:
	  doV1(cus.getNameHasher(digest));
	  break;
	case V3Tree:
	case V3File:
	  doV3();
	  break;
	}
	bytesHashed = hasher.getBytesHashed();
	filesHashed = hasher.getFilesHashed();
	elapsedTime = hasher.getElapsedTime();
      } else {
      }
    } catch (Exception e) {
      log.warning("doit()", e);
      errMsg = "Error hashing: " + e.toString();
    }
    IOUtil.safeClose(recordStream);
  }

  private void doV1(CachedUrlSetHasher cush) throws IOException {
    hasher = new SimpleHasher(digest, challenge, verifier);
    hasher.setFiltered(true);
    hashResult = hasher.doV1Hash(cush);
    showResult = true;
  }

  private void doV3() throws IOException {
    StringBuilder sb = new StringBuilder();
    // Pylorus' hash() depends upon the first 20 characters of this string
    sb.append("# Block hashes from " + getMachineName() + ", " +
		      ServletUtil.headerDf.format(new Date()) + "\n");
    sb.append("# AU: " + au.getName() + "\n");
    sb.append("# Hash algorithm: " + digest.getAlgorithm() + "\n");
    sb.append("# Encoding: " + resEncoding.toString() + "\n");
    if (challenge != null) {
      sb.append("# " + "Poller nonce: " + byteString(challenge) + "\n");
    }
    if (verifier != null) {
      sb.append("# " + "Voter nonce: " + byteString(verifier) + "\n");
    }
    hasher = new SimpleHasher(digest, challenge, verifier);
    hasher.setFiltered(true);
    hasher.setBase64Result(resEncoding == ResultEncoding.Base64);
    blockFile = FileUtil.createTempFile("HashCUS", ".tmp");
    hasher.doV3Hash(cus, blockFile, sb.toString(), "# end\n");
    showResult = true;
  }

  String byteString(byte[] a) {
    switch (resEncoding) {
    case Base64: return String.valueOf(B64Code.encode(a));
    default:
    case Hex: return ByteArray.toHexString(a);
    }
  }
}
