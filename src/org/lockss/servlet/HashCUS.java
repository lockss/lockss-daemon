/*
 * $Id: HashCUS.java,v 1.50 2013-05-23 20:43:18 tlipkis Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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
import javax.servlet.http.HttpSession;
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

  /** If true, asynchronous hash requests are global - there's a single
   * namespace of request IDs.  If false each session has its own namespace
   * and hashes are private to the session */
  static final String PARAM_GLOBAL_ASYNCH_REQUESTS = 
    Configuration.PREFIX + "hashcus.globalAsynchRequests";
  static final boolean DEFAULT_GLOBAL_ASYNCH_REQUESTS = true;

  static final String KEY_AUID = "auid";
  static final String KEY_URL = "url";
  static final String KEY_LOWER = "lb";
  static final String KEY_UPPER = "ub";
  static final String KEY_CHALLENGE = "challenge";
  static final String KEY_VERIFIER = "verifier";
  static final String KEY_HASH_TYPE = "hashtype";
  static final String KEY_RECORD = "record";
  static final String KEY_ASYNCH = "asynch";
  static final String KEY_ACTION = "action";
  static final String KEY_MIME = "mime";
  static final String KEY_FILE_ID = "file";
  static final String KEY_ALG = "algorithm";
  static final String KEY_RESULT_ENCODING = "encoding";
  static final String KEY_RESULT_TYPE = "result";
  static final String KEY_REQ_ID = "req_id";

  static final String SESSION_KEY_HASH_REQS = "hashCus_requests";
  static final int MAX_RANDOM_SEARCH = 100000;

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

  enum RunnerStatus {Init, Starting, Running, Done, Error, RequestError};

  static final String ACTION_HASH = "Hash";
  static final String ACTION_CHECK = "Check Status";
  static final String ACTION_LIST = "List Requests";
  static final String ACTION_STREAM = "Stream";

  static final String COL2 = "colspan=2";
  static final String COL2CENTER = COL2 + " align=center";

  static final String FOOT_EXPLANATION =
    "Calculates hash in the servlet runner thread, " +
    "so may cause other scheduled hashes to time out. " +
    "Beware hashing a large CUS. " +
    "There is also currently no way to interrupt the hash.";
  static final String FOOT_URL =
    "To specify a whole AU, leave blank or enter <code>LOCKSSAU:</code>.";
  static final String FOOT_BIN =
    "May cause browser to try to render binary data";
  static final String FOOT_REQ_ID =
    "Req Id from previous asynch request in this session";

  static Logger log = Logger.getLogger("HashCUS");

  private static final Map<String,Data> GLOBAL_REQUESTS =
    new HashMap<String,Data>();

  private LockssDaemon daemon;
  private PluginManager pluginMgr;

  String defaultAlg = LcapMessage.getDefaultHashAlgorithm();
  Data ddd;

  // MUST be static - no references to the servlet instance
  static class Data {
    boolean ok = false;			// form params ok

    boolean showResult = false;

    String machineName;
    String auid;
    ArchivalUnit au;
    CachedUrlSet cus;
    String url;
    String lower;
    String upper;
    byte[] challenge;
    byte[] verifier;
    boolean isRecord;
    boolean isAsynch = false;
    String alg;
    HashType hType = DEFAULT_HASH_TYPE;
    ResultEncoding resEncoding = DEFAULT_RESULT_ENCODING;
    ResultType resType = DEFAULT_RESULT_TYPE;
    String reqId = null;

    File recordFile;
    File blockFile;

    MessageDigest digest;

    String runnerError;
    RunnerStatus runnerStat;

    byte[] hashResult;
    long bytesHashed;
    int filesHashed;
    long elapsedTime;

    Data(String machineName) {
      this.machineName = machineName;
    }
  }

  int nbytes = 1000;

  protected void resetLocals() {
    super.resetLocals();
    ddd = null;
  }

  void resetVars() {
    nbytes = 1000;
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
    ddd = new Data(getMachineName());
    String action = getParameter(KEY_ACTION);

    if (ACTION_STREAM.equals(action)) {
      if (sendStream()) {
	return;
      }
    } else if (ACTION_CHECK.equals(action)) {
      checkStatus();
      displayPage();
      return;
    } else if (ACTION_LIST.equals(action)) {
      listRequests();
      return;
    } else if (ACTION_HASH.equals(action)) {

      ddd = checkParams();
      if (ddd.ok) {
	doit();
	if (!ddd.isAsynch &&
	    errMsg == null &&
	    ddd.resType == ResultType.Inline &&
	    isV3(ddd.hType)) {
	  returnDirectResponse();
	} else {
	  displayPage();
	}
	return;
      }
    } else if (!StringUtil.isNullString(action)) {
      errMsg = "Unknown action: " + action;
    }
    displayPage();
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

  void listRequests() throws IOException {
    Map<String,Data> map = getRequestMap();
    if (map.isEmpty()) {
      errMsg = "No asynchronous requests";
      displayPage();
      return;
    }
    Table tbl = new Table(0, "align=center");
    tbl.newRow();
    tbl.addHeading("Req Id");
    tbl.addHeading("Status");
    tbl.addHeading("AU");
    for (Map.Entry<String,Data> ent : map.entrySet()) {
      tbl.newRow();
      Data d = ent.getValue();
      tbl.newCell();
      Properties p = new Properties();
      p.setProperty(KEY_ACTION, ACTION_CHECK);
      p.setProperty(KEY_REQ_ID, ent.getKey());
      tbl.add(srvLink(myServletDescr(), ent.getKey(), concatParams(p)));
      tbl.newCell();
      String statStr = d.runnerStat.toString();
      if (d.runnerStat == RunnerStatus.Done) {
	statStr = fileLink(statStr, d.blockFile, false);
      }
      tbl.add(statStr);
      tbl.newCell();
      tbl.add(d.au.getName());
    }
    Page page = newPage();
    layoutErrorBlock(page);
    page.add(tbl);
    endPage(page);
  }

  void checkStatus() {
    String req_id = getParameter(KEY_REQ_ID);
    if (StringUtil.isNullString(req_id)) {
      ddd.runnerStat = RunnerStatus.RequestError;
      errMsg = "Must supply req_id";
      return;
    }      
    Data asynchData = getData(req_id);
    if (asynchData == null) {
      ddd.runnerStat = RunnerStatus.RequestError;
      errMsg = "No such background hash: " + req_id;
      return;
    }
    ddd = asynchData;
    RunnerStatus stat = ddd.runnerStat;
    switch (stat) {
    case Done:
      statusMsg = "Background hash " + ddd.reqId + " status: " + stat;
      ddd.showResult = true;
      break;
    case Error:
      errMsg = "Background hash " + ddd.reqId + " status: " + stat +
	"<br>" + ddd.runnerError;
      ddd.showResult = false;
      break;
    default:
      statusMsg = "Background hash " + ddd.reqId + " status: " + stat;
      break;
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

  private Data checkParams() {
    Data params = new Data(getMachineName());
    params.auid = getParameter(KEY_AUID);
    params.url = getParameter(KEY_URL);
    params.lower = getParameter(KEY_LOWER);
    params.upper = getParameter(KEY_UPPER);
    params.isRecord = (getParameter(KEY_RECORD) != null);
    params.alg = req.getParameter(KEY_ALG);
    if (StringUtil.isNullString(params.alg)) {
      params.alg = LcapMessage.getDefaultHashAlgorithm();
    }
    params.isAsynch = (getParameter(KEY_ASYNCH) != null);
    String hTypeStr = getParameter(KEY_HASH_TYPE);
    if (StringUtil.isNullString(hTypeStr)) {
      params.hType = DEFAULT_HASH_TYPE;
    } else if (StringUtils.isNumeric(hTypeStr)) {
      try {
	int hTypeInt = Integer.parseInt(hTypeStr);
	params.hType = hashTypeCompat[hTypeInt];
	if (params.hType == null) throw new ArrayIndexOutOfBoundsException();
      } catch (ArrayIndexOutOfBoundsException e) {
	errMsg = "Unknown hash type: " + hTypeStr;
	return params;
      } catch (RuntimeException e) {
	errMsg = "Can't parse hash type: " + hTypeStr;
	return params;
      }
    } else {
      try {
	params.hType = HashType.valueOf(hTypeStr);
      } catch (IllegalArgumentException e) {
	errMsg = "Unknown hash type: " + hTypeStr;
	return params;
      }
    }
    String resTypeStr = getParameter(KEY_RESULT_TYPE);
    if (StringUtil.isNullString(resTypeStr)) {
      params.resType = DEFAULT_RESULT_TYPE;
    } else {
      try {
	params.resType = ResultType.valueOf(resTypeStr);
      } catch (IllegalArgumentException e) {
	errMsg = "Unknown result type: " + resTypeStr;
	return params;
      }
    }
    String resEncodingStr = getParameter(KEY_RESULT_ENCODING);
    if (StringUtil.isNullString(resEncodingStr)) {
      params.resEncoding = DEFAULT_RESULT_ENCODING;
    } else {
      try {
	params.resEncoding = ResultEncoding.valueOf(resEncodingStr);
      } catch (IllegalArgumentException e) {
	errMsg = "Unknown result encoding: " + resEncodingStr;
	return params;
      }
    }
    if (params.auid == null) {
      errMsg = "Select an AU";
      return params;
    }
    params.au = pluginMgr.getAuFromId(params.auid);
    if (params.au == null) {
      errMsg = "No such AU.  Select an AU";
      return params;
    }
    if (params.url == null) {
      params.url = AuCachedUrlSetSpec.URL;
//       errMsg = "URL required";
//       return params;
    }
    try {
      params.challenge = getB64Param(KEY_CHALLENGE);
    } catch (IllegalArgumentException e) {
      errMsg = "Challenge: Illegal Base64 string: " + e.getMessage();
      return params;
    }
    try {
      params.verifier = getB64Param(KEY_VERIFIER);
    } catch (IllegalArgumentException e) {
      errMsg = "Verifier: Illegal Base64 string: " + e.getMessage();
      return params;
    }
    PollSpec ps;
    try {
      switch (params.hType) {
      case V1File:
	if (params.upper != null ||
	    (params.lower != null && !params.lower.equals(PollSpec.SINGLE_NODE_LWRBOUND))) {
	  errMsg = "Upper/Lower ignored";
	}
	ps = new PollSpec(params.auid,
			  params.url,
			  PollSpec.SINGLE_NODE_LWRBOUND,
			  null,
			  Poll.V1_CONTENT_POLL);
	break;
      case V3Tree:

	ps = new PollSpec(params.auid, params.url, params.lower, params.upper, Poll.V3_POLL);
	break;
      case V3File:
	ps = new PollSpec(params.auid, params.url, PollSpec.SINGLE_NODE_LWRBOUND, null,
			  Poll.V3_POLL);
	break;
      default:
	ps = new PollSpec(params.auid, params.url, params.lower, params.upper, Poll.V1_CONTENT_POLL);
      }
    } catch (Exception e) {
      errMsg = "Error making PollSpec: " + e.toString();
      log.debug("Making Pollspec", e);
      return params;
    }
    log.debug(""+ps);
    params.cus = ps.getCachedUrlSet();
    if (params.cus == null) {
      errMsg = "No such CUS: " + ps;
      return params;
    }

    if (params.isAsynch && params.resType == ResultType.Inline) {
      errMsg = "Cannot select both Asynch and Inline result";
      return params;
    }

    log.debug(""+params.cus);
    params.ok = true;
    return params;
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
    page.add(makeQueryResult());
    page.add("<br>");
    if (ddd.showResult) {
      switch (ddd.hType) {
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
    endPage(page);
  }

  private Element makeQueryResult() {
    Composite comp = new Composite();
    if (ddd.runnerStat != null) {
      comp.add(resultDiv("RequestStatus", ddd.runnerStat.toString()));
    }
    return comp;
  }

  private String resultDiv(String id, String value) {
    return "<div style=\"display:none\" id=\"" + id + "\">" + value + "</div>";
  }

  private void returnDirectResponse() throws IOException {
    if (!ddd.showResult) {
      displayPage();
    }
    switch (ddd.hType) {
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

  private void addRecordFile(Table tbl) {
    if (ddd.recordFile != null && ddd.recordFile.exists()) {
      tbl.newRow("valign=bottom");
      tbl.newCell();
      tbl.add("Stream:");
      if (ddd.recordFile.length() < ddd.bytesHashed) {
	tbl.add(addFootnote("First " +
			    StringUtil.sizeToString(ddd.recordFile.length())));
      }
      tbl.add(":");
      tbl.newCell();
      String fileId = getSessionObjectId(ddd.recordFile.toString());
      Properties p = new Properties();
      p.setProperty(KEY_ACTION, ACTION_STREAM);
      p.setProperty(KEY_FILE_ID, fileId);
      p.setProperty(KEY_MIME, "application/octet-stream");
      tbl.add(fileLink("binary", ddd.recordFile, true));
      tbl.add("&nbsp;&nbsp;");
      p.setProperty(KEY_MIME, "text/plain");
      tbl.add(fileLink("text", ddd.recordFile, false));
      tbl.add(addFootnote(FOOT_BIN));
    }
  }

  private String fileLink(String text, File file, boolean isBinary) {
    String fileId = getSessionObjectId(file.toString());
    Properties p = new Properties();
    p.setProperty(KEY_ACTION, ACTION_STREAM);
    p.setProperty(KEY_FILE_ID, fileId);
    if (isBinary) {
      p.setProperty(KEY_MIME, "application/octet-stream");
      return srvLink(myServletDescr(), text, concatParams(p));
    } else {
      p.setProperty(KEY_MIME, "text/plain");
      return srvLink(myServletDescr(), text, concatParams(p));
    }
  }

  private void sendV3DirectResponse() throws IOException {
    PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");

    if (ddd.blockFile == null || !ddd.blockFile.exists()) {
      if (errMsg == null) {
	errMsg = "Unknown error - no hash output generated";
	ddd.showResult = false;
	displayPage();
	return;
      }
    }
    Reader rdr = new BufferedReader(new FileReader(ddd.blockFile));
    try {
      org.mortbay.util.IO.copy(rdr, wrtr);
    } finally {
      IOUtil.safeClose(rdr);
      ddd.blockFile.delete();
    }

  }

  String getElapsedString() {
    String s = StringUtil.protectedDivide(ddd.bytesHashed,
					  ddd.elapsedTime, "inf");
    if (!"inf".equalsIgnoreCase(s) && Long.parseLong(s) < 100) {
      double fbpms = ((double)ddd.bytesHashed) / ((double)ddd.elapsedTime);
      s = fmt_2dec.format(fbpms);
    }
    return ddd.elapsedTime + " ms, " + s + " bytes/ms";
  }

  private Element makeForm() {
    Composite comp = new Composite();
    Block centeredBlock = new Block(Block.Center);

    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");

    Table autbl = new Table(0, "cellpadding=0");
    autbl.newRow();
    autbl.addHeading("Select AU");
    Composite sel = ServletUtil.layoutSelectAu(this, KEY_AUID, ddd.auid);
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

    addInputRow(tbl, "URL" + addFootnote(FOOT_URL), KEY_URL, 50, ddd.url);
    addInputRow(tbl, "Lower", KEY_LOWER, 50, ddd.lower);
    addInputRow(tbl, "Upper", KEY_UPPER, 50, ddd.upper);
    addInputRow(tbl, "Challenge", KEY_CHALLENGE, 50,
		getParameter(KEY_CHALLENGE));
    addInputRow(tbl, "Verifier", KEY_VERIFIER, 50, getParameter(KEY_VERIFIER));
    addInputRow(tbl, "Algorithm", KEY_ALG, 50, ddd.alg);

    tbl.newRow();
    tbl.addHeading("Result:", "align=right");
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(ResultType.File.toString(),
			KEY_RESULT_TYPE,
			ddd.resType == ResultType.File));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(ResultType.Inline.toString(),
			KEY_RESULT_TYPE,
			ddd.resType == ResultType.Inline));

    tbl.newRow();
    tbl.addHeading("Encoding:", "align=right");
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(ResultEncoding.Hex.toString(),
			KEY_RESULT_ENCODING,
			ddd.resEncoding == ResultEncoding.Hex));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(ResultEncoding.Base64.toString(),
			KEY_RESULT_ENCODING,
			ddd.resEncoding == ResultEncoding.Base64));

    tbl.newRow();
    tbl.addHeading("V1:", "align=right");
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_CONTENT,
			HashType.V1Content.toString(),
			KEY_HASH_TYPE,
			ddd.hType == HashType.V1Content));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_NAME,
			HashType.V1Name.toString(),
			KEY_HASH_TYPE,
			ddd.hType == HashType.V1Name));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_SNCUSS,
			HashType.V1File.toString(),
			KEY_HASH_TYPE,
			ddd.hType == HashType.V1File));
    tbl.newRow();
    tbl.addHeading("V3:", "align=right");
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_V3_TREE,
			HashType.V3Tree.toString(),
			KEY_HASH_TYPE,
			ddd.hType == HashType.V3Tree));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_V3_SNCUSS,
			HashType.V3File.toString(),
			KEY_HASH_TYPE,
			ddd.hType == HashType.V3File));

    tbl.newRow();
    tbl.newCell(COL2CENTER);
    tbl.add(checkBox("Record filtered stream", "true", KEY_RECORD,
		     ddd.isRecord));
    tbl.add(checkBox("Asynchronous", "false", KEY_ASYNCH, ddd.isAsynch));

    centeredBlock.add(tbl);
    centeredBlock.add("<br>");

    Input submitH = new Input(Input.Submit, KEY_ACTION, ACTION_HASH);
    setTabOrder(submitH);
    centeredBlock.add(submitH);

    Table tbl2 = new Table(0, "cellpadding=0");
    tbl2.newRow();
    tbl2.newCell();
    tbl2.add("&nbsp;");

    addInputRow(tbl2, "Req Id" + addFootnote(FOOT_REQ_ID),
		KEY_REQ_ID, 20, ddd.reqId);
    centeredBlock.add(tbl2);

    if (!StringUtil.isNullString(ddd.reqId)) {
      centeredBlock.add(resultDiv("RequestId", ddd.reqId.toString()));
    }

    Input submitC = new Input(Input.Submit, KEY_ACTION, ACTION_CHECK);
    setTabOrder(submitC);
    Input submitL = new Input(Input.Submit, KEY_ACTION, ACTION_LIST);
    setTabOrder(submitL);
    centeredBlock.add(submitC);
    centeredBlock.add("&nbsp;");
    centeredBlock.add(submitL);

    frm.add(centeredBlock);
    comp.add(frm);
    return comp;
  }

  void addInputRow(Table tbl, String label, String key,
		   int size, String initVal) {
    tbl.newRow();
    tbl.addHeading(label + ":", "align=right");
    tbl.newCell();
    Input in = new Input(Input.Text, key, initVal);
    in.setSize(size);
    setTabOrder(in);
    tbl.add(in);
  }

  private void doit() {
    HashRunner runner = new HashRunner(ddd);
    if (ddd.isAsynch) {
      forkDoit(runner);
    } else {
      doit(runner);
    }
  }

  private void doit(final HashRunner runner) {
    try {
      runner.doit();
      switch (ddd.runnerStat) {
      case Error:
	errMsg = ddd.runnerError;
	break;
      case Done:
	statusMsg = "Hash done";
	break;
      }
    } catch (Exception e) {
      log.warning("doit()", e);
      errMsg = "Hash error: " + e.toString();
    }
  }

  private Map<String,Data> getRequestMap() {
    if (CurrentConfig.getBooleanParam(PARAM_GLOBAL_ASYNCH_REQUESTS,
				      DEFAULT_GLOBAL_ASYNCH_REQUESTS)) {
      log.critical("returning global map: " + GLOBAL_REQUESTS);
      return GLOBAL_REQUESTS;
    } else {
      HttpSession session = getSession();
      synchronized (session) {
	Map<String,Data> map =
	  (Map<String,Data>)session.getAttribute(SESSION_KEY_HASH_REQS);
	if (map == null) {
	  map = new HashMap<String,Data>();
	  session.setAttribute(SESSION_KEY_HASH_REQS, map);
	}
	return map;
      }
    }
  }

  static String randomString(int len) {
    return org.apache.commons.lang.RandomStringUtils.randomAlphabetic(len);
  }

  private String getReqId(Data d) {
    Map<String,Data> map = getRequestMap();
    synchronized (map) {
      for (int ix = 0; ix < MAX_RANDOM_SEARCH; ix++) {
	String key = randomString(5);
	if (!map.containsKey(key)) {
	  map.put(key, d);
	  return key;
	}
      }
      throw new IllegalStateException("Couldn't find an unused request key in "
				      + MAX_RANDOM_SEARCH + " tries");
    }
  }

  private Data getData(String reqId) {
    return getRequestMap().get(reqId);
  }

  private void forkDoit(final HashRunner runner) {
    try {
      ddd.reqId = getReqId(ddd);
      LockssRunnable runnable =
	new LockssRunnable(AuUtil.getThreadNameFor("HashCUS", ddd.au)) {
	  public void lockssRun() {
	    runner.doit();
	  }};
      Thread th = new Thread(runnable);
      th.start();
      statusMsg = "Started background hash, Req Id: " + ddd.reqId;
    } catch (RuntimeException e) {
      log.warning("forkDoit()", e);
      errMsg = "Error starting background hash thread: " + e.toString();
    }
  }


  static String byteString(byte[] a, ResultEncoding re) {
    switch (re) {
    case Base64: return String.valueOf(B64Code.encode(a));
    default:
    case Hex: return ByteArray.toHexString(a);
    }
  }

  private Element makeV1Result() {
    Table tbl = new Table(0, "align=center");
    tbl.newRow();
    tbl.addHeading("Hash Result", COL2);

    addResultRow(tbl, "CUSS", ddd.cus.getSpec().toString());
    if (ddd.challenge != null) {
      addResultRow(tbl, "Challenge", byteString(ddd.challenge, ddd.resEncoding));
    }
    if (ddd.verifier != null) {
      addResultRow(tbl, "Verifier", byteString(ddd.verifier, ddd.resEncoding));
    }
    addResultRow(tbl, "Size", Long.toString(ddd.bytesHashed));

    addResultRow(tbl, "Hash", byteString(ddd.hashResult, ddd.resEncoding));

    addResultRow(tbl, "Time", getElapsedString());

    addRecordFile(tbl);
    return tbl;
  }

  private Element makeV3Result() {
    Table tbl = new Table(0, "align=center");
    tbl.newRow();
    tbl.addHeading("Hash Result", COL2);

    if (ddd.isAsynch) {
      RunnerStatus stat = ddd.runnerStat;
      addResultRow(tbl, "Status", stat.toString());
    }
    addResultRow(tbl, "CUSS", ddd.cus.getSpec().toString());
    addResultRow(tbl, "Files", Integer.toString(ddd.filesHashed));
    addResultRow(tbl, "Size", Long.toString(ddd.bytesHashed));
    addResultRow(tbl, "Time", getElapsedString());
    if (ddd.blockFile != null && ddd.blockFile.exists()) {
      tbl.newRow();
      tbl.newCell();
      tbl.add("Hash file");
      tbl.add(":");
      tbl.newCell();
      String link = fileLink("HashFile", ddd.blockFile, false);
      tbl.add(link);
      tbl.add(resultDiv("HashFile", link));
    }
    addRecordFile(tbl);
    return tbl;
  }

  void addResultRow(Table tbl, String head, Object value) {
    tbl.newRow();
    tbl.newCell();
    tbl.add(head);
    tbl.add(":");
    tbl.newCell();
    tbl.add(value.toString());
  }

  // MUST be static - no references to the servlet instance
  static class HashRunner {
    Data ddd;
    SimpleHasher hasher;
    OutputStream recordStream;

    HashRunner(Data d) {
      this.ddd = d;
      ddd.runnerStat = RunnerStatus.Init;
    }

    HashRunner setStatus(RunnerStatus stat) {
      ddd.runnerStat = stat;
      log.critical("setStatus: " + stat);
      return this;
    }

    HashRunner setStatus(RunnerStatus stat, String msg) {
      ddd.runnerError = msg;
      return setStatus(stat);
    }

    HashRunner setError(String msg) {
      return setStatus(RunnerStatus.Error, msg);
    }

    private String getMachineName() {
      return ddd.machineName;
    }

    private void doit() {
      try {
	setStatus(RunnerStatus.Running);
	doit0();
      } catch (Exception e) {
	setStatus(RunnerStatus.Error, e.toString());
      }
    }

    private void doit0() {
      try {
	makeDigest();
	switch (ddd.hType) {
	case V1Content:
	case V1File:
	  doV1(ddd.cus.getContentHasher(ddd.digest));
	  break;
	case V1Name:
	  doV1(ddd.cus.getNameHasher(ddd.digest));
	  break;
	case V3Tree:
	case V3File:
	  doV3();
	  break;
	}
	ddd.bytesHashed = hasher.getBytesHashed();
	ddd.filesHashed = hasher.getFilesHashed();
	ddd.elapsedTime = hasher.getElapsedTime();
	setStatus(RunnerStatus.Done);
      } catch (NoSuchAlgorithmException e) {
	log.warning("doit0()", e);
	setError("Couldn't start hash: " + e.getMessage());
      } catch (Exception e) {
	log.warning("doit0()", e);
	setError("Error hashing: " + e.toString());
      } finally {
	IOUtil.safeClose(recordStream);
      }
    }

    void makeDigest()
	throws IOException, NoSuchAlgorithmException {
      ddd.digest = MessageDigest.getInstance(ddd.alg);
      if (ddd.isRecord) {
	ddd.recordFile = FileUtil.createTempFile("HashCUS", ".tmp");
	recordStream =
	  new BufferedOutputStream(new FileOutputStream(ddd.recordFile));
	long truncateTo =
	  CurrentConfig.getLongParam(PARAM_TRUNCATE_FILETERD_STREAM,
				     DEFAULT_TRUNCATE_FILETERD_STREAM);
	ddd.digest = new RecordingMessageDigest(ddd.digest, recordStream,
						truncateTo);
	// 	  runner.recordFile.deleteOnExit();
      }
    }

    private void doV1(CachedUrlSetHasher cush) throws IOException {
      hasher = new SimpleHasher(ddd.digest, ddd.challenge, ddd.verifier);
      hasher.setFiltered(true);
      ddd.hashResult = hasher.doV1Hash(cush);
      ddd.showResult = true;
    }

    private void doV3() throws IOException {
      ddd.blockFile = FileUtil.createTempFile("HashCUS", ".tmp");
      runV3();
      ddd.showResult = true;
    }

    private void runV3() throws IOException {
      StringBuilder sb = new StringBuilder();
      // Pylorus' hash() depends upon the first 20 characters of this string
      sb.append("# Block hashes from " + getMachineName() + ", " +
		ServletUtil.headerDf.format(new Date()) + "\n");
      sb.append("# AU: " + ddd.au.getName() + "\n");
      sb.append("# Hash algorithm: " + ddd.digest.getAlgorithm() + "\n");
      sb.append("# Encoding: " + ddd.resEncoding.toString() + "\n");
      if (ddd.challenge != null) {
	sb.append("# " + "Poller nonce: " + byteString(ddd.challenge, ddd.resEncoding) + "\n");
      }
      if (ddd.verifier != null) {
	sb.append("# " + "Voter nonce: " + byteString(ddd.verifier, ddd.resEncoding) + "\n");
      }
      hasher = new SimpleHasher(ddd.digest, ddd.challenge, ddd.verifier);
      hasher.setFiltered(true);
      hasher.setBase64Result(ddd.resEncoding == ResultEncoding.Base64);
      hasher.doV3Hash(ddd.cus, ddd.blockFile, sb.toString(), "# end\n");
    }

  }
}
