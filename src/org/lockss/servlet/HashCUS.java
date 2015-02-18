/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.HttpSession;
import org.mortbay.html.*;
import org.lockss.util.*;
import org.lockss.util.CloseCallbackInputStream.DeleteFileOnCloseInputStream;
import org.lockss.config.*;
import org.lockss.hasher.*;
import org.lockss.hasher.SimpleHasher.HasherStatus;
import org.lockss.hasher.SimpleHasher.HashType;
import org.lockss.hasher.SimpleHasher.ResultEncoding;
import org.lockss.plugin.*;

/** Hash a CUS on demand, display the results and filtered input stream
 */
@SuppressWarnings("serial")
public class HashCUS extends LockssServlet {

  public static final String PREFIX = Configuration.PREFIX + "hashcus.";

  /** If true, background hash requests are global - there's a single
   * namespace of request IDs.  If false each session has its own namespace
   * and hashes are private to the session */
  static final String PARAM_GLOBAL_BACKGROUND_REQUESTS = 
    PREFIX + "globalBackgroundRequests";
  static final boolean DEFAULT_GLOBAL_BACKGROUND_REQUESTS = true;

  /** If true, hash files of background requests are deleted after being
   * fetched.  If false they remain until the request is deleted. */
  static final String PARAM_AUTO_DELETE_HASH_FILES = 
    PREFIX + "autoDeleteHashFiles";
  static final boolean DEFAULT_AUTO_DELETE_HASH_FILES = true;

  static final String KEY_AUID = "auid";
  static final String KEY_URL = "url";
  static final String KEY_LOWER = "lb";
  static final String KEY_UPPER = "ub";
  static final String KEY_CHALLENGE = "challenge";
  static final String KEY_VERIFIER = "verifier";
  static final String KEY_HASH_TYPE = "hashtype";
  static final String KEY_RECORD = "record";
  static final String KEY_BACKGROUND = "background";
  static final String KEY_EXCLUDE_SUSPECT = "excludeSuspect";
  static final String KEY_ACTION = "action";
  static final String KEY_MIME = "mime";
  static final String KEY_FILE_ID = "file";
  static final String KEY_ALG = "algorithm";
  static final String KEY_RESULT_ENCODING = "encoding";
  static final String KEY_RESULT_TYPE = "result";
  static final String KEY_REQ_ID = "req_id";

  static final String SESSION_KEY_HASH_REQS = "hashCus_requests";

  static final String HASH_STRING_CONTENT = "Content";
  static final String HASH_STRING_NAME = "Name";
  static final String HASH_STRING_SNCUSS = "One file";
  static final String HASH_STRING_V3_TREE = "Tree";
  static final String HASH_STRING_V3_SNCUSS = "One file";

  enum ResultType {File, Inline};
  static final ResultType DEFAULT_RESULT_TYPE = ResultType.File;

  static final String ACTION_HASH = "Hash";
  static final String ACTION_CHECK = "Check Status";
  static final String ACTION_LIST = "List Requests";
  static final String ACTION_CANCEL = "Cancel";
  static final String ACTION_DELETE = "Delete";
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
    "May cause browser to try to render binary data.";
  static final String FOOT_REQ_ID_GLOBAL =
    "Req Id from previous background request.";
  static final String FOOT_REQ_ID_SESSION =
    "Req Id from previous background request in this session.";

  private static final Logger log = Logger.getLogger(HashCUS.class);

  private static boolean isGlobalBackgroundRequests =
    DEFAULT_GLOBAL_BACKGROUND_REQUESTS;

  private static boolean isAutoDeleteHashFiles =
    DEFAULT_AUTO_DELETE_HASH_FILES;

  private static final Map<String, SimpleHasher.ParamsAndResult>
  GLOBAL_REQUESTS = new LinkedHashMap<String, SimpleHasher.ParamsAndResult>();

  private static final NumberFormat fmt_2dec = new DecimalFormat("0.00");

  private PluginManager pluginMgr;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
  }

  public void lockssHandleRequest() throws IOException {
    final String DEBUG_HEADER = "lockssHandleRequest(): ";
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Starting...");
    errMsg = null;
    statusMsg = null;

    HasherParams params = new HasherParams(getMachineName(),
	getParameter(KEY_BACKGROUND) != null);
    SimpleHasher hasher = new SimpleHasher(null);
    HasherResult result = new HasherResult();

    String reqId = getParameter(KEY_REQ_ID);

    ResultType resType = DEFAULT_RESULT_TYPE;
    String resTypeStr = getParameter(KEY_RESULT_TYPE);
    if (!StringUtil.isNullString(resTypeStr)) {
      try {
        resType = ResultType.valueOf(resTypeStr);
      } catch (IllegalArgumentException e) {
        errMsg = "Unknown result type: " + resTypeStr;
      }
    }

    params.setHashType(getParameter(KEY_HASH_TYPE));
    errMsg = hasher.processHashTypeParam(params, result);
    if (HasherStatus.Error == result.getRunnerStatus()) {
      displayPage(resType, reqId, params, result);
      return;
    }

    params.setResultEncoding(getParameter(KEY_RESULT_ENCODING));
    errMsg = hasher.processResultEncodingParam(params, result);
    if (HasherStatus.Error == result.getRunnerStatus()) {
      displayPage(resType, reqId, params, result);
      return;
    }

    if (!StringUtil.isNullString(errMsg)) {
      displayPage(resType, null, params, result);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done.");
      return;
    }

    String action = getParameter(KEY_ACTION);
    if (log.isDebug3()) log.debug3("action = " + action);
    if (StringUtil.isNullString(action)) {
      String lAction = getParameter(ACTION_TAG);
      if (log.isDebug3()) log.debug3("lAction = " + lAction);
      if (!StringUtil.isNullString(lAction)) {
	action = lAction;
      }
    }

    if (ACTION_STREAM.equals(action)) {
      if (sendStream()) {
	return;
      }
      params.setAuId(getParameter(KEY_AUID));
      params.setUrl(getParameter(KEY_URL));
      params.setLower(getParameter(KEY_LOWER));
      params.setUpper(getParameter(KEY_UPPER));
      params.setRecordFilteredStream(getParameter(KEY_RECORD) != null);
      params
      .setExcludeSuspectVersions(getParameter(KEY_EXCLUDE_SUSPECT) != null);
      params.setAlgorithm(getParameter(KEY_ALG));
      params.setChallenge(getParameter(KEY_CHALLENGE));
      params.setVerifier(getParameter(KEY_VERIFIER));
    } else if (ACTION_CHECK.equals(action)) {
      checkStatus(reqId, result);
      displayPage(resType, reqId, getParamsData(reqId), result);
      return;
    } else if (ACTION_LIST.equals(action)) {
      listRequests(resType, params, result);
      return;
    } else if (ACTION_CANCEL.equals(action)) {
      cancelRequest(reqId);
      listRequests(resType, params, result);
      return;
    } else if (ACTION_DELETE.equals(action)) {
      cancelRequest(reqId);
      listRequests(resType, params, result);
      return;
    } else if (ACTION_HASH.equals(action)) {
      params.setAuId(getParameter(KEY_AUID));
      params.setUrl(getParameter(KEY_URL));
      params.setLower(getParameter(KEY_LOWER));
      params.setUpper(getParameter(KEY_UPPER));
      params.setRecordFilteredStream(getParameter(KEY_RECORD) != null);
      params
      .setExcludeSuspectVersions(getParameter(KEY_EXCLUDE_SUSPECT) != null);
      params.setAlgorithm(getParameter(KEY_ALG));
      params.setChallenge(getParameter(KEY_CHALLENGE));
      params.setVerifier(getParameter(KEY_VERIFIER));
      if (log.isDebug3()) log.debug3("params = " + params);

      errMsg = hasher.processParams(params, result);

      if (HasherStatus.Error == result.getRunnerStatus()) {
	displayPage(resType, reqId, params, result);
      }

      if (params.isAsynchronous() && resType == ResultType.Inline) {
	errMsg = "Cannot select both Background and Inline result";
	displayPage(resType, reqId, params, result);
      }

      if (StringUtil.isNullString(errMsg)) {
	errMsg = hash(params, hasher, result);
	if (log.isDebug3()) log.debug3("result = " + result);
	if (!params.isAsynchronous() &&
	    errMsg == null &&
	    resType == ResultType.Inline &&
	    SimpleHasher.isV3(result.getHashType())) {
	  returnDirectResponse(resType, reqId, params, result);
	} else {
	  if (params.isAsynchronous()) {
	    reqId = result.getRequestId();
	  }

	  displayPage(resType, reqId, params, result);
	}
	return;
      }
    } else if (!StringUtil.isNullString(action)) {
      errMsg = "Unknown action: " + action;
    }
    displayPage(resType, reqId, params, result);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done.");
  }

  void listRequests(ResultType resType, HasherParams params,
      HasherResult result) throws IOException {
    final String DEBUG_HEADER = "listRequests(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "resType = " + resType);
      log.debug2(DEBUG_HEADER + "params = " + params);
      log.debug2(DEBUG_HEADER + "result = " + result);
    }
    Map<String, SimpleHasher.ParamsAndResult> requestMap = getRequestMap();
    synchronized (requestMap) {
      if (requestMap.isEmpty()) {
	statusMsg = "No background requests";
	displayPage(resType, null, params, result);
	return;
      }

      Form frm = new Form(srvURL(myServletDescr()));
      frm.method("POST");
      frm.add(new Input(Input.Hidden, ACTION_TAG, ""));
      frm.add(new Input(Input.Hidden, KEY_REQ_ID, ""));

      Table tbl = new Table(0, "align=center");
      tbl.newRow();
      tbl.addHeading("");
      tbl.addHeading("Req Id");
      tbl.addHeading("Status");
      tbl.addHeading("Req Time");
      tbl.addHeading("Start Time");
      tbl.addHeading("AU");
      for (Map.Entry<String, SimpleHasher.ParamsAndResult> ent
	  : requestMap.entrySet()) {
	tbl.newRow();
	String key = ent.getKey();
	HasherParams entryRequest = ent.getValue().params;
	HasherResult entryResult = ent.getValue().result;

	tbl.newCell();
	switch (entryResult.getRunnerStatus()) {
	case NotStarted:
	case Init:
	case Starting:
	case Running: 
	  tbl.add(submitButton("Cancel", ACTION_CANCEL, KEY_REQ_ID, key));
	  break;
	case Done:
	case Error:
	  tbl.add(submitButton("Delete", ACTION_DELETE, KEY_REQ_ID, key));
	  break;
	default:
	  tbl.add("");
	  break;
	}

	tbl.newCell();
	Properties p = new Properties();
	p.setProperty(KEY_ACTION, ACTION_CHECK);
	p.setProperty(KEY_REQ_ID, ent.getKey());
	populatePropertiesFromParams(entryRequest, p);
	tbl.add(srvLink(myServletDescr(), div("RequestId", key), p));

	tbl.newCell();
	String statStr = div("RequestStatus",
	    entryResult.getRunnerStatus().toString());
	if (entryResult.getRunnerStatus() == HasherStatus.Done
	    && entryResult.getBlockFile() != null) {
	  statStr = fileLink(statStr, entryResult.getBlockFile(), "HashFile",
	      false, entryRequest);
	}
	tbl.add(statStr);

	tbl.newCell();
	tbl.add(div("RequestTime",
	    SimpleHasher.formatDateTime(entryResult.getRequestTime())));
	tbl.newCell();
	if (entryResult.getStartTime() > 0) {
	  tbl.add(div("StartTime",
	      SimpleHasher.formatDateTime(entryResult.getStartTime())));
	}

	tbl.newCell();
	ArchivalUnit au = pluginMgr.getAuFromId(entryRequest.getAuId());
	tbl.add(au != null ? au.getName() : "(Deleted AU)");
      }
      Page page = newPage();
      layoutErrorBlock(page);
      ServletUtil.layoutExplanationBlock(page, "Background HashCUS requests");
      addJavaScript(page);

      Block centeredBlock = new Block(Block.Center);
      centeredBlock.add("<font size=-1>");
      centeredBlock.add(srvLink(myServletDescr(), "Refresh",
				PropUtil.fromArgs(KEY_ACTION, ACTION_LIST)));
      centeredBlock.add("&nbsp;&nbsp;");
      centeredBlock.add(srvLink(myServletDescr(), "Back to HashCUS"));

      centeredBlock.add("</font>");
      page.add(centeredBlock);

      frm.add(tbl);
      page.add(frm);
      endPage(page);
    }
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  // Cancel or Delete
  void cancelRequest(String reqId) {
    final String DEBUG_HEADER = "cancelRequest(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "reqId = " + reqId);
    if (StringUtil.isNullString(reqId)) {
      errMsg = "Must supply req_id";
      return;
    }      
    HasherResult backgroundResult = getResultData(reqId);
    if (backgroundResult == null) {
      errMsg = "No such background hash: " + reqId;
      return;
    }
    switch (backgroundResult.getRunnerStatus()) {
    case NotStarted:
    case Init:
    case Starting:
    case Running: 
      Future<Void> fut = backgroundResult.getFuture();
      if (fut != null) {
	fut.cancel(true);
      }
      break;
    default:
    }
    FileUtil.safeDeleteFile(backgroundResult.getBlockFile());
    FileUtil.safeDeleteFile(backgroundResult.getRecordFile());

    delRequest(reqId);
    statusMsg = "Background hash " + reqId + " deleted";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  void checkStatus(String reqId, HasherResult result) {
    final String DEBUG_HEADER = "checkStatus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "reqId = " + reqId);
    if (StringUtil.isNullString(reqId)) {
      result.setRunnerStatus(HasherStatus.RequestError);
      errMsg = "Must supply req_id";
      return;
    }      
    HasherResult backgroundData = getResultData(reqId);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "backgroundData = " + backgroundData);

    if (backgroundData == null) {
      result.setRunnerStatus(HasherStatus.RequestError);
      errMsg = "No such background hash: " + reqId;
      return;
    }

    result.copyFrom(backgroundData);
    HasherStatus stat = backgroundData.getRunnerStatus();

    switch (stat) {
    case Done:
      statusMsg = "Background hash " + reqId + " status: " + stat;
      result.setShowResult(true);
      break;
    case Error:
      errMsg = "Background hash " + reqId + " status: " + stat +
	"<br>" + backgroundData.getRunnerError();
      result.setShowResult(false);
      break;
    default:
      statusMsg = "Background hash " + reqId + " status: " + stat;
      break;
    }
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  private boolean sendStream() {
    final String DEBUG_HEADER = "sendStream(): ";
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Starting...");
    if (!hasSession()) {
      errMsg = "Please enable cookies";
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done. Result = false");
      return false;
    }
    String fileId = getParameter(KEY_FILE_ID);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fileId = " + fileId);
    String file = getSessionIdString(fileId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "file = " + file);
    if (StringUtil.isNullString(file)) {
      errMsg = "Unknown file: " + fileId;
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done. Result = false");
      return false;
    }

    File fileToStream = new File(file);
    if (!fileToStream.exists()) {
      errMsg = "The result file of the previous hashing operation has been "
	  + "deleted.<br />To view it, please perform the hashing operation "
	  + "again.";
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done. Result = false");
      return false;
    }

    String mime = getParameter(KEY_MIME);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "mime = " + mime);
    try {
      if (mime != null) {
	resp.setContentType(mime);
      }
      InputStream in;
      if (isAutoDeleteHashFiles) {
	in = new DeleteFileOnCloseInputStream(fileToStream);
      } else {
	in = new FileInputStream(fileToStream);
      }
      in = new BufferedInputStream(in);
      OutputStream out = resp.getOutputStream();
      org.mortbay.util.IO.copy(in, out);
      in.close();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done. Result = true");
      return true;
    } catch (IOException e) {
      log.debug("sendStream()", e);
      errMsg = "Error sending file: " + e.toString();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done. Result = false");
      return false;
    }
  }

  private void displayPage(ResultType resType, String reqId,
      HasherParams params, HasherResult result) throws IOException {
    final String DEBUG_HEADER = "displayPage(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "resType = " + resType);
      log.debug2(DEBUG_HEADER + "reqId = " + reqId);
      log.debug2(DEBUG_HEADER + "params = " + params);
      log.debug2(DEBUG_HEADER + "result = " + result);
    }
    Page page = newPage();
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, "Hash a CachedUrlSet" +
				       addFootnote(FOOT_EXPLANATION));
    page.add(makeForm(resType, result.getHashType(), result.getResultEncoding(),
	reqId, params));
    page.add(makeQueryResult(result.getRunnerStatus()));
    page.add("<br>");
    if (result.isShowResult()) {
      switch (result.getHashType()) {
      case V1Content:
      case V1File:
      case V1Name:
	page.add(makeV1Result(params, result));
	break;
      case V3Tree:
      case V3File:
	page.add(makeV3Result(params, result));
	break;
      }

    }
    endPage(page);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  private Element makeQueryResult(HasherStatus status) {
    Composite comp = new Composite();
    if (status != null) {
      comp.add(div("RequestStatus", status.toString(), false));
    }
    return comp;
  }

  private void returnDirectResponse(ResultType resType, String reqId,
      HasherParams params, HasherResult result) throws IOException {
    final String DEBUG_HEADER = "returnDirectResponse(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "resType = " + resType);
      log.debug2(DEBUG_HEADER + "reqId = " + reqId);
      log.debug2(DEBUG_HEADER + "params = " + params);
      log.debug2(DEBUG_HEADER + "result = " + result);
    }
    if (!result.isShowResult()) {
      displayPage(resType, reqId, params, result);
    }
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result.getHashType() = "
	+ result.getHashType());
    switch (result.getHashType()) {
    case V1Content:
    case V1File:
    case V1Name:
      errMsg = "Not implemented";
      displayPage(resType, reqId, params, result);
      break;
    case V3Tree:
    case V3File:
      sendV3DirectResponse(resType, reqId, params, result);
      break;
    }
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done.");
  }

  private void addRecordFile(long bytesHashed, File recordFile,
      HasherParams params, Table tbl) {
    if (recordFile != null && recordFile.exists()) {
      tbl.newRow("valign=bottom");
      tbl.newCell();
      tbl.add("Stream:");
      if (recordFile.length() < bytesHashed) {
	tbl.add(addFootnote("First " +
			    StringUtil.sizeToString(recordFile.length())));
      }
      tbl.add(":");
      tbl.newCell();
      String fileId = getSessionObjectId(recordFile.toString());
      Properties p = new Properties();
      p.setProperty(KEY_ACTION, ACTION_STREAM);
      p.setProperty(KEY_FILE_ID, fileId);
      p.setProperty(KEY_MIME, "application/octet-stream");
      tbl.add(fileLink("binary", recordFile, true, params));
      tbl.add("&nbsp;&nbsp;");
      p.setProperty(KEY_MIME, "text/plain");
      tbl.add(fileLink("text", recordFile, false, params));
      tbl.add(addFootnote(FOOT_BIN));
    }
  }

  private String div(String id, String value) {
    return div(id, value, true);
  }

  private String div(String id, String value, boolean visible) {
    if (visible) {
      return "<div id=\""
	+ id + "\">" + value + "</div>";
    } else {
      return "<div style=\"display:none\" id=\""
	+ id + "\">" + value + "</div>";
    }
  }

  private String fileLink(String text, File file, boolean isBinary,
      HasherParams params) {
    return fileLink(text, file, null, isBinary, params);
  }

  private String fileLink(String text, File file, String id, boolean isBinary,
      HasherParams params) {
    String fileId = getSessionObjectId(file.toString());
    Properties p = new Properties();
    p.setProperty(KEY_ACTION, ACTION_STREAM);
    p.setProperty(KEY_FILE_ID, fileId);

    populatePropertiesFromParams(params, p);

    if (isBinary) {
      p.setProperty(KEY_MIME, "application/octet-stream");
      return srvLinkWithId(myServletDescr(), text, id, p);
    } else {
      p.setProperty(KEY_MIME, "text/plain");
      return srvLinkWithId(myServletDescr(), text, id, p);
    }
  }

  private void populatePropertiesFromParams(HasherParams params, Properties p) {
    if (getParameter(KEY_RESULT_TYPE) != null) {
      p.setProperty(KEY_RESULT_TYPE, getParameter(KEY_RESULT_TYPE));
    } else {
      p.setProperty(KEY_RESULT_TYPE, DEFAULT_RESULT_TYPE.toString());
    }
    if (params.getHashType() != null) {
      p.setProperty(KEY_HASH_TYPE, params.getHashType());
    } else {
      p.setProperty(KEY_HASH_TYPE, SimpleHasher.DEFAULT_HASH_TYPE.toString());
    }
    if (params.getResultEncoding() != null) {
      p.setProperty(KEY_RESULT_ENCODING, params.getResultEncoding());
    } else {
      p.setProperty(KEY_RESULT_ENCODING,
	  SimpleHasher.DEFAULT_RESULT_ENCODING.toString());
    }
    if (params.getAuId() != null) {
      p.setProperty(KEY_AUID, params.getAuId());
    }
    if (params.getUrl() != null) {
      p.setProperty(KEY_URL, params.getUrl());
    }
    if (params.getLower() != null) {
      p.setProperty(KEY_LOWER, params.getLower());
    }
    if (params.getUpper() != null) {
      p.setProperty(KEY_UPPER, params.getUpper());
    }
    if (params.isRecordFilteredStream()) {
      p.setProperty(KEY_RECORD, "true");
    }
    if (params.isExcludeSuspectVersions()) {
      p.setProperty(KEY_EXCLUDE_SUSPECT, "true");
    }
    if (params.getAlgorithm() != null) {
      p.setProperty(KEY_ALG, params.getAlgorithm());
    }
    if (params.getChallenge() != null) {
      p.setProperty(KEY_CHALLENGE, params.getChallenge());
    }
    if (params.getVerifier() != null) {
      p.setProperty(KEY_VERIFIER, params.getVerifier());
    }
    if (params.isAsynchronous()) {
      p.setProperty(KEY_BACKGROUND, "true");
    }
  }

  private void sendV3DirectResponse(ResultType resType, String reqId,
      HasherParams params, HasherResult result) throws IOException {
    final String DEBUG_HEADER = "sendV3DirectResponse(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "resType = " + resType);
      log.debug2(DEBUG_HEADER + "reqId = " + reqId);
      log.debug2(DEBUG_HEADER + "params = " + params);
      log.debug2(DEBUG_HEADER + "result = " + result);
    }
    PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result.getBlockFile() = "
	+ result.getBlockFile());
    if (result.getBlockFile() == null || !result.getBlockFile().exists()) {
      if (errMsg == null) {
	errMsg = "Unknown error - no hash output generated";
	result.setShowResult(false);
	displayPage(resType, reqId, params, result);
	return;
      }
    }
    Reader rdr = new BufferedReader(new InputStreamReader(
	new DeleteFileOnCloseInputStream(result.getBlockFile())));
    try {
      org.mortbay.util.IO.copy(rdr, wrtr);
    } finally {
      IOUtil.safeClose(rdr);
      result.getBlockFile().delete();
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  String getElapsedString(long bytesHashed, long elapsedTime) {
    String s = StringUtil.protectedDivide(bytesHashed, elapsedTime, "inf");
    if (!"inf".equalsIgnoreCase(s) && Long.parseLong(s) < 100) {
      double fbpms = ((double)bytesHashed) / ((double)elapsedTime);
      s = fmt_2dec.format(fbpms);
    }
    return elapsedTime + " ms, " + s + " bytes/ms";
  }

  private Element makeForm(ResultType resType, HashType hashType,
      ResultEncoding resultEncoding, String reqId, HasherParams params) {
    final String DEBUG_HEADER = "makeForm(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "resType = " + resType);
      log.debug2(DEBUG_HEADER + "hashType = " + hashType);
      log.debug2(DEBUG_HEADER + "resultEncoding = " + resultEncoding);
      log.debug2(DEBUG_HEADER + "reqId = " + reqId);
      log.debug2(DEBUG_HEADER + "params = " + params);
    }
    Composite comp = new Composite();
    Block centeredBlock = new Block(Block.Center);

    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");

    Table autbl = new Table(0, "cellpadding=0");
    autbl.newRow();
    autbl.addHeading("Select AU");
    Composite sel = ServletUtil.layoutSelectAu(this,
	KEY_AUID, params.getAuId());
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

    addInputRow(tbl, "URL" + addFootnote(FOOT_URL), KEY_URL, 50,
	params.getUrl());
    addInputRow(tbl, "Lower", KEY_LOWER, 50, params.getLower());
    addInputRow(tbl, "Upper", KEY_UPPER, 50, params.getUpper());
    addInputRow(tbl, "Challenge", KEY_CHALLENGE, 50,
		getParameter(KEY_CHALLENGE));
    addInputRow(tbl, "Verifier", KEY_VERIFIER, 50, getParameter(KEY_VERIFIER));
    addInputRow(tbl, "Algorithm", KEY_ALG, 50, params.getAlgorithm());

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
    tbl.add(radioButton(ResultEncoding.Hex.toString(), KEY_RESULT_ENCODING,
			resultEncoding == ResultEncoding.Hex));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(ResultEncoding.Base64.toString(),
			KEY_RESULT_ENCODING,
			resultEncoding == ResultEncoding.Base64));

    tbl.newRow();
    tbl.addHeading("V1:", "align=right");
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_CONTENT, HashType.V1Content.toString(),
			KEY_HASH_TYPE, hashType == HashType.V1Content));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_NAME, HashType.V1Name.toString(),
			KEY_HASH_TYPE, hashType == HashType.V1Name));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_SNCUSS, HashType.V1File.toString(),
			KEY_HASH_TYPE, hashType == HashType.V1File));
    tbl.newRow();
    tbl.addHeading("V3:", "align=right");
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_V3_TREE, HashType.V3Tree.toString(),
			KEY_HASH_TYPE, hashType == HashType.V3Tree));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(radioButton(HASH_STRING_V3_SNCUSS, HashType.V3File.toString(),
			KEY_HASH_TYPE, hashType == HashType.V3File));

    tbl.newRow();
    tbl.newCell();
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(checkBox("Record filtered stream", "true", KEY_RECORD,
		     params.isRecordFilteredStream()));
    tbl.newRow();
    tbl.newCell();
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(checkBox("Exclude suspect versions", "true", KEY_EXCLUDE_SUSPECT,
		     params.isExcludeSuspectVersions()));
    tbl.newRow();
    tbl.newCell();
    tbl.newCell();
    tbl.add("&nbsp;&nbsp;");
    tbl.add(checkBox("Background", "false", KEY_BACKGROUND,
		     params.isAsynchronous()));

    centeredBlock.add(tbl);
    centeredBlock.add("<br>");

    Input submitH = new Input(Input.Submit, KEY_ACTION, ACTION_HASH);
    setTabOrder(submitH);
    centeredBlock.add(submitH);

    Table tbl2 = new Table(0, "cellpadding=0");
    tbl2.newRow();
    tbl2.newCell();
    tbl2.add("&nbsp;");

    addInputRow(tbl2, "Req Id" + addFootnote(isGlobalBackgroundRequests
					     ? FOOT_REQ_ID_GLOBAL
					     : FOOT_REQ_ID_SESSION),
		KEY_REQ_ID, 20, reqId);
    centeredBlock.add(tbl2);

    if (!StringUtil.isNullString(reqId)) {
      centeredBlock.add(div("RequestId", reqId, false));
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
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
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

  private String hash(HasherParams params, SimpleHasher hasher,
      HasherResult result) {
    final String DEBUG_HEADER = "hash(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "params = " + params);
      log.debug2(DEBUG_HEADER + "result = " + result);
    }
    String errorMessage = null;

    if (params.isAsynchronous()) {
      errorMessage = hashAsynchronously(params, hasher, result);
    } else {
      errorMessage = hashSynchronously(params, hasher, result);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "Done - errorMessage = " + errorMessage);
    return errorMessage;
  }

  private String hashSynchronously(HasherParams params, SimpleHasher hasher,
      HasherResult result) {
    final String DEBUG_HEADER = "hashSynchronously(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    String errorMessage = null;

    try {
      hasher.hash(params, result);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result.getRunnerStat() = "
	  + result.getRunnerStatus());
      switch (result.getRunnerStatus()) {
      case Error:
	errorMessage = result.getRunnerError();
	break;
      case Done:
	statusMsg = "Hash done";
	break;
      default:
      }
    } catch (Exception e) {
      log.warning("hashSynchronously()", e);
      errorMessage = "Hash error: " + e.toString();
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "Done - errorMessage = " + errorMessage);
    return errorMessage;
  }

  /** Called by ServletUtil.setConfig() */
  static void setConfig(Configuration config,
                        Configuration oldConfig,
                        Configuration.Differences diffs) {
    isGlobalBackgroundRequests =
      config.getBoolean(PARAM_GLOBAL_BACKGROUND_REQUESTS,
			DEFAULT_GLOBAL_BACKGROUND_REQUESTS);
    isAutoDeleteHashFiles =
      config.getBoolean(PARAM_AUTO_DELETE_HASH_FILES,
			DEFAULT_AUTO_DELETE_HASH_FILES);
  }

  private Map<String, SimpleHasher.ParamsAndResult> getRequestMap() {
    if (isGlobalBackgroundRequests) {
      return GLOBAL_REQUESTS;
    } else {
      HttpSession session = getSession();
      synchronized (session) {
	Map<String, SimpleHasher.ParamsAndResult> map =
	    (Map<String, SimpleHasher.ParamsAndResult>)session
	    .getAttribute(SESSION_KEY_HASH_REQS);
	if (map == null) {
	  map = new HashMap<String, SimpleHasher.ParamsAndResult>();
	  session.setAttribute(SESSION_KEY_HASH_REQS, map);
	}
	return map;
      }
    }
  }

  private HasherParams getParamsData(String reqId) {
    Map<String, SimpleHasher.ParamsAndResult> map = getRequestMap();
    synchronized (map) {
      SimpleHasher.ParamsAndResult par = map.get(reqId);
      return par != null ? par.params : null;
    }
  }

  private HasherResult getResultData(String reqId) {
    Map<String, SimpleHasher.ParamsAndResult> map = getRequestMap();
    synchronized (map) {
      SimpleHasher.ParamsAndResult par = map.get(reqId);
      return par != null ? par.result : null;
    }
  }

  private void delRequest(String reqId) {
    Map<String, SimpleHasher.ParamsAndResult> requestMap = getRequestMap();
    synchronized (requestMap) {
      requestMap.remove(reqId);
    }
  }

  private String hashAsynchronously(final HasherParams params,
      final SimpleHasher hasher, final HasherResult result) {
    final String DEBUG_HEADER = "hashAsynchronously(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    String errorMessage = null;

    try {
      Map<String, SimpleHasher.ParamsAndResult> requestMap = getRequestMap();
      String reqId;
      synchronized (requestMap) {
	reqId = SimpleHasher.getReqId(params, result, getRequestMap());
      }
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "reqId = " + reqId);

      hasher.startHashingThread(params, result);
      statusMsg = "Queued background hash, Req Id: " + reqId;
      result.setShowResult(true);
    } catch (RuntimeException e) {
      log.warning("hashAsynchronously()", e);
      errorMessage = "Error starting background hash thread: " + e.toString();
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "Done - errorMessage = " + errorMessage);
    return errorMessage;
  }

  private Element makeV1Result(HasherParams params, HasherResult result) {
    Table tbl = new Table(0, "align=center");
    tbl.newRow();
    tbl.addHeading("Hash Result", COL2);

    addResultRow(tbl, "CUSS", result.getCus().getSpec().toString());
    if (result.getChallenge() != null) {
      addResultRow(tbl, "Challenge", SimpleHasher
	  .byteString(result.getChallenge(), result.getResultEncoding()));
    }
    if (result.getVerifier() != null) {
      addResultRow(tbl, "Verifier", SimpleHasher
	  .byteString(result.getVerifier(), result.getResultEncoding()));
    }
    addResultRow(tbl, "Size", Long.toString(result.getBytesHashed()));

    addResultRow(tbl, "Hash", SimpleHasher.byteString(result.getHashResult(),
	result.getResultEncoding()));

    addResultRow(tbl, "Time",
	getElapsedString(result.getBytesHashed(), result.getElapsedTime()));

    addRecordFile(result.getBytesHashed(), result.getRecordFile(), params, tbl);
    return tbl;
  }

  private Element makeV3Result(HasherParams params, HasherResult result) {
    final String DEBUG_HEADER = "makeV3Result(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "params = " + params);
      log.debug2(DEBUG_HEADER + "result = " + result);
    }

    Table tbl = new Table(0, "align=center");
    tbl.newRow();
    tbl.addHeading("Hash Result", COL2);

    if (params.isAsynchronous()) {
      HasherStatus stat = result.getRunnerStatus();
      addResultRow(tbl, "Status", stat.toString());
    }
    addResultRow(tbl, "CUSS", result.getCus().getSpec().toString());
    addResultRow(tbl, "Files", Integer.toString(result.getFilesHashed()));
    addResultRow(tbl, "Size", Long.toString(result.getBytesHashed()));
    addResultRow(tbl, "Time",
	getElapsedString(result.getBytesHashed(), result.getElapsedTime()));

    if (log.isDebug3()) {
      log.debug3(DEBUG_HEADER + "result.getBlockFile() = "
	  + result.getBlockFile());
      log.debug3(DEBUG_HEADER + "result.getBlockFile().exists() = "
	  + result.getBlockFile().exists());
    }

    if (result.getBlockFile() != null && result.getBlockFile().exists()) {
      tbl.newRow();
      tbl.newCell();
      tbl.add("Hash file");
      tbl.add(":");
      tbl.newCell();
      String link = fileLink("HashFile", result.getBlockFile(), false, params);
      tbl.add(link);
      tbl.add(div("HashFile", link, false));
    }
    addRecordFile(result.getBytesHashed(), result.getRecordFile(), params, tbl);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "tbl = " + tbl);
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
}
