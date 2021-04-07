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
package org.lockss.hasher;

import java.io.*;
import java.security.*;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.mortbay.util.B64Code;
import org.apache.commons.lang3.StringUtils;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.poller.Poll;
import org.lockss.poller.PollSpec;
import org.lockss.protocol.LcapMessage;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.daemon.*;

/** Utilitiy class to hash a CUS as a single batch and record the results
 * in a file.  By default the content is *not* filtered; use setFiltered()
 * to change.
 */
public class SimpleHasher {
  static Logger log = Logger.getLogger("SimpleHasher");

  private static final String PREFIX = Configuration.PREFIX + "hashcus.";

  /** If set to a positive number, the record of each filtered stream is
   * truncated to that many bytes.  -1 means no limit */
  public static final String PARAM_TRUNCATE_FILTERED_STREAM = 
    PREFIX + "truncateFilteredStream";
  public static final long DEFAULT_TRUNCATE_FILTERED_STREAM = 100 * 1024;

  private static final String THREADPOOL_PREFIX = PREFIX + "threadPool.";

  /** Max number of background threads running UI-initiated hashes */
  static final String PARAM_THREADPOOL_SIZE =
    THREADPOOL_PREFIX + "size";
  static final int DEFAULT_THREADPOOL_SIZE = 1;

  /** Duration after which idle threads will be terminated..  -1 = never */
  public static final String PARAM_THREADPOOL_KEEPALIVE =
    THREADPOOL_PREFIX + "keepAlive";
  static final long DEFAULT_THREADPOOL_KEEPALIVE = 5 * Constants.MINUTE;

  /** Priority at which background hash threads should run.  Changing this
   * does not alter the priority of already running hashes. */
  static final String PARAM_THREADPOOL_PRIORITY =
    THREADPOOL_PREFIX + "priority";
  static final int DEFAULT_THREADPOOL_PRIORITY = 5;

  private static final int MAX_RANDOM_SEARCH = 100000;

  /**
   * The types of possible hashes to be performed.
   */
  public static enum HashType {
    V1Content, V1Name, V1File, V3Tree, V3File;
  }

  public static final HashType DEFAULT_HASH_TYPE = HashType.V3Tree;

  /**
   * The types of possible encodings of the results.
   */
  public static enum ResultEncoding {
    Base64, Hex;
  }

  public static final ResultEncoding DEFAULT_RESULT_ENCODING =
      ResultEncoding.Hex;

  /**
   * The possible statuses of a hasher.
   */
  public static enum HasherStatus {
    NotStarted(false),
    Init(false),
    Starting(false),
    Running(false),
    Done(true),
    Error(true),
    RequestError(true);

    final boolean isDone;

    HasherStatus(boolean isDone) {
      this.isDone = isDone;
    }
    public boolean isDone() {
      return isDone;
    }

   }

  /**
   * Structure used to record state of asynchronous hashing operations.
   */
  public static class ParamsAndResult {
    public HasherParams params;
    public HasherResult result;

    ParamsAndResult(HasherParams params, HasherResult result) {
      this.params = params;
      this.result = result;
    }
  }

  private static ThreadPoolExecutor EXECUTOR;

  // Support for old numeric input values.
  private HashType[] hashTypeCompat = {
    null,
    HashType.V1Content,
    HashType.V1Name,
    HashType.V1File,
    HashType.V3Tree,
    HashType.V3File
  };

  private MessageDigest digest;
  private PatternFloatMap resultWeightMap = null;
  private byte[] challenge;
  private byte[] verifier;
  private boolean isFiltered = false;
  private boolean isIncludeUrl = false;
  private boolean isIncludeWeight = false;
  private boolean isExcludeSuspectVersions = false;
  private boolean isBase64 = false;

  private int nbytes = 1000;
  private long bytesHashed = 0;
  private int filesHashed = 0;
  private long elapsedTime;

  public SimpleHasher(MessageDigest digest, byte[] challenge, byte[] verifier) {
    this.digest = digest;
    this.challenge = challenge;
    this.verifier = verifier;
  }

  public SimpleHasher(MessageDigest digest) {
    this(digest, null, null);
  }

  public long getBytesHashed() {
    return bytesHashed;
  }

  public int getFilesHashed() {
    return filesHashed;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  /**
   * Determines whether to hash filtered or raw content.  Default is
   * unfiltered (false)
   * @param val if true hash filtered content, if false hash raw content
   */
  public void setFiltered(boolean val) {
    isFiltered = val;
  }

  /**
   * Determines whether to include the URL of each file in its hash.
   * Default is false
   * @param val if true include URL of each file in its hash
   */
  public void setIncludeUrl(boolean val) {
    isIncludeUrl = val;
  }
  
  /**
   * Determines whether to include the weight of of each file in its hash
   * @param val if true include weight of each file in its hash
   */
  public void setIncludeWeight(boolean val) {
    isIncludeWeight = val;
  }

  /**
   * Determines whether to exclude file versions that are marked suspect
   * Default is false
   * @param val if true exclude suspect versions
   */
  public void setExcludeSuspectVersions(boolean val) {
    isExcludeSuspectVersions = val;
  }

  /**
   * If true, result is a Base64 string; if false (the default), result is
   * a hex string
   */
  public void setBase64Result(boolean val) {
    isBase64 = val;
  }

  /** Do a V1 hash of the CUSH */
  public byte[] doV1Hash(CachedUrlSetHasher cush) throws IOException {
    initDigest(digest);
    doHash(cush);
    return digest.digest();
  }

  /** Do a V3 hash of the AU, recording the results to blockFile */
  public void doV3Hash(ArchivalUnit au, File blockFile,
		       String header, String footer)
      throws IOException {
    doV3Hash(au.getAuCachedUrlSet(), blockFile, header, footer);
  }

  /** Do a V3 hash of the CUS, recording the results to blockFile */
  public void doV3Hash(CachedUrlSet cus, File blockFile,
		       String header, String footer)
      throws IOException {
    PrintStream blockOuts = new PrintStream(new BufferedOutputStream(
	new FileOutputStream(blockFile)));
    if (header != null) {
      blockOuts.println(header);
    }
    BlockHasher hasher = newBlockHasher(cus, 1,
					initHasherDigests(),
					initHasherByteArrays(),
					new BlockEventHandler(blockOuts));

    hasher.setIncludeUrl(isIncludeUrl);
    hasher.setExcludeSuspectVersions(isExcludeSuspectVersions);
    if(isIncludeWeight) {
      try {
        resultWeightMap = cus.getArchivalUnit().makeUrlPollResultWeightMap();
      } catch (NullPointerException e) {
        log.warning("No AU, thus no weightMap: " + cus);
      } catch (ArchivalUnit.ConfigurationException e) {
        log.warning("Error building weightMap", e);
      }
    }
    try {
      doHash(hasher);
      if (footer != null) {
	blockOuts.println(footer);
      }
    } catch (RuntimeInterruptedException e) {
      if (e.getCause() != null) {
	blockOuts.println("\nAborted: " + e.getCause().getMessage());
      } else {
	blockOuts.println("\nAborted: " + e.getMessage());
      }
      throw e;
    } catch (InterruptedIOException e) {
      blockOuts.println("\nAborted: " + e.getMessage());
      throw e;
    } catch (Exception e) {
      blockOuts.println("\nError: " + e.toString());
      throw e;
    } finally {
      blockOuts.close();
      // ensure files closed if terminated early, harmless otherwise
      hasher.abortHash();
    }
  }

  private void initDigest(MessageDigest digest) {
    if (challenge != null) {
      digest.update(challenge, 0, challenge.length);
    }
    if (verifier != null) {
      digest.update(verifier, 0, verifier.length);
    }
  }

  protected BlockHasher newBlockHasher(CachedUrlSet cus,
				       int maxVersions,
				       MessageDigest[] digests,
				       byte[][] initByteArrays,
				       BlockHasher.EventHandler cb) {
    return  new BlockHasher(cus, maxVersions, digests, initByteArrays, cb);
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

  private void doHash(CachedUrlSetHasher cush) throws IOException {
    cush.setFiltered(isFiltered);
    bytesHashed = 0;
    filesHashed = 0;
    long startTime = TimeBase.nowMs();
    while (!cush.finished()) {
      bytesHashed += cush.hashStep(nbytes);
      elapsedTime = TimeBase.msSince(startTime);

      // Check whether the thread has been interrupted (e.g., by the future
      // being cancel()ed) and exit if so
      if (Thread.currentThread().interrupted()) {
	log.warning("Hash interrupted, aborting: " +
		    cush.getCachedUrlSet().getArchivalUnit());
	throw new RuntimeInterruptedException("Thread interrupted");
      }
    }
  }

  private MessageDigest[] initHasherDigests() {
    MessageDigest[] digests = new MessageDigest[1];
    digests[0] = digest;
    return digests;
  }

  // XXX This should probably use PrintWriter with ISO-8859-1, as the
  // result is generally sent in an HTTP response
  private class BlockEventHandler implements BlockHasher.EventHandler {
    PrintStream outs;
    BlockEventHandler(PrintStream outs) {
      this.outs = outs;
    }
      
    public void blockDone(HashBlock block) {
      filesHashed++;
      HashBlock.Version ver = block.currentVersion();
      if (ver != null) {
	String out;
	if (ver.getHashError() != null) {
	  // Pylorus' diff() depends upon the first 20 characters of this string
	  out =  "Hash error (see log)        " + block.getUrl();
	} else {
	  if(isIncludeWeight) {
	    out = byteString(ver.getHashes()[0]) + "   " + getUrlResultWeight(block.getUrl()) + "   " + block.getUrl();
	  } else {
	    out = byteString(ver.getHashes()[0]) + "   " + block.getUrl();
	  }
	}
	log.debug3(out);
	outs.println(out);
      }
    }

    String byteString(byte[] a) {
      if (isBase64) {
        return String.valueOf(B64Code.encode(a));
      } else {
        return ByteArray.toHexString(a);
      }
    }
  }
  
  protected float getUrlResultWeight(String url) {
    if (resultWeightMap == null || resultWeightMap.isEmpty()) {
      return 1.0f;
    }
    return resultWeightMap.getMatch(url, 1.0f);
  }

  /**
   * Performs the hashing of an AU or a URL based on the passed parameters and
   * it stores the result in the passed object.
   * 
   * @param params
   *          A HasherParams with the parameters that define the hashing
   *          operation.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   */
  public void hash(HasherParams params, HasherResult result) {
    final String DEBUG_HEADER = "hash(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    result.setStartTime(TimeBase.nowMs());
    result.setRunnerStatus(HasherStatus.Init);

    processHashTypeParam(params, result);
    if (HasherStatus.Error == result.getRunnerStatus()) {
      return;
    }

    processResultEncodingParam(params, result);
    if (HasherStatus.Error == result.getRunnerStatus()) {
      return;
    }

    processParams(params, result);
    if (HasherStatus.Error == result.getRunnerStatus()) {
      return;
    }

    try {
      try {
	digest = makeDigestAndRecordStream(params.getAlgorithm(),
	    params.isRecordFilteredStream(), result);

	if (digest == null) {
	  log.warning(DEBUG_HEADER + "No digest could be obtained");
	  result.setRunnerStatus(HasherStatus.Error);
	  result.setRunnerError("No digest could be obtained");
	}
      } catch (NoSuchAlgorithmException nsae) {
	log.warning(DEBUG_HEADER, nsae);
	result.setRunnerStatus(HasherStatus.Error);
	result.setRunnerError("Invalid hashing algorithm: "
	    + nsae.getMessage());
      } catch (Exception e) {
	log.warning(DEBUG_HEADER, e);
	result.setRunnerStatus(HasherStatus.Error);
	result.setRunnerError("Error making digest: " + e.getMessage());
      }

      if (HasherStatus.Error == result.getRunnerStatus()) {
	// Clean up an empty block file, if necessary.
	if (result.getBlockFile() != null
	    && result.getBlockFile().length() == 0) {
	  FileUtil.safeDeleteFile(result.getBlockFile());
	  result.setBlockFile(null);
	}

	return;
      }

      result.setRunnerStatus(HasherStatus.Running);

      try {
	switch (result.getHashType()) {
	case V1Content:
	case V1File:
	  doV1(result.getCus().getContentHasher(digest), result);
	  break;
	case V1Name:
	  doV1(result.getCus().getNameHasher(digest), result);
	  break;
	case V3Tree:
	case V3File:
	  doV3(params.getMachineName(), params.isExcludeSuspectVersions(), params.isIncludeWeight(),
	      result);
	  break;
	}
	fillInResult(result, HasherStatus.Done, null);
      } catch (RuntimeInterruptedException e) {
	if (e.getCause() != null) {
	  fillInResult(result, HasherStatus.Error, e.getCause().getMessage());
	} else {
	  fillInResult(result, HasherStatus.Error, e.getMessage());
	}
      } catch (InterruptedIOException e) {
	fillInResult(result, HasherStatus.Error, e.getMessage());
      } catch (Exception e) {
	log.warning("hash()", e);
	fillInResult(result, HasherStatus.Error, "Error hashing: " + e.toString());
      } catch (Error e) {
	try {
	  log.warning("hash()", e);
	} catch (Error ee) {
	}
	result.setRunnerStatus(HasherStatus.Error);
	result.setRunnerError("Error hashing: " + e.toString());
	throw e;
      }
    } finally {
      if (result.getRunnerStatus() == HasherStatus.Running) {
	result.setRunnerStatus(HasherStatus.Error);
	result.setRunnerError("Unexpected abort");
      }
      IOUtil.safeClose(result.getRecordStream());
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
  }

  void fillInResult(HasherResult result, HasherStatus status, String errmsg) {
    result.setBytesHashed(getBytesHashed());
    result.setFilesHashed(getFilesHashed());
    result.setElapsedTime(getElapsedTime());
    result.setRunnerError(errmsg);
    result.setRunnerStatus(status);
  }

  /**
   * Handles the specification of the type of hashing operation to be performed.
   * 
   * @param params
   *          A HasherParams with the parameters that define the hashing
   *          operation.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   * @return a String with any error message.
   */
  public String processHashTypeParam(HasherParams params, HasherResult result) {
    final String DEBUG_HEADER = "processHashTypeParam(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    HashType hashType = null;
    String errorMessage = null;

    if (StringUtil.isNullString(params.getHashType())) {
      hashType = DEFAULT_HASH_TYPE;
    } else if (StringUtils.isNumeric(params.getHashType())) {
      try {
	int hashTypeInt = Integer.parseInt(params.getHashType());
	hashType = hashTypeCompat[hashTypeInt];
	if (hashType == null) throw new ArrayIndexOutOfBoundsException();
	params.setHashType(hashType.toString());
      } catch (ArrayIndexOutOfBoundsException aioobe) {
	result.setRunnerStatus(HasherStatus.Error);
	errorMessage = "Unknown hash type: " + params.getHashType();
	result.setRunnerError(errorMessage);
	return errorMessage;
      } catch (RuntimeException re) {
	result.setRunnerStatus(HasherStatus.Error);
	errorMessage =
	    "Can't parse hash type: " + params.getHashType() + re.getMessage();
	result.setRunnerError(errorMessage);
	return errorMessage;
      }
    } else {
      try {
	hashType = HashType.valueOf(params.getHashType());
      } catch (IllegalArgumentException iae) {
	log.warning(DEBUG_HEADER, iae);
	result.setRunnerStatus(HasherStatus.Error);
	errorMessage = "Unknown hash type: " + params.getHashType() + " - "
	    + iae.getMessage();
	result.setRunnerError(errorMessage);
	return errorMessage;
      }
    }

    result.setHashType(hashType);
    return errorMessage;
  }

  /**
   * Handles the specification of the type of encoding to use to display the
   * results of the hashing operation to be performed.
   * 
   * @param params
   *          A HasherParams with the parameters that define the hashing
   *          operation.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   * @return a String with any error message.
   */
  public String processResultEncodingParam(HasherParams params,
      HasherResult result) {
    final String DEBUG_HEADER = "processResultEncodingParam(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    ResultEncoding resultEncoding = null;
    String errorMessage = null;

    if (StringUtil.isNullString(params.getResultEncoding())) {
      resultEncoding = SimpleHasher.DEFAULT_RESULT_ENCODING;
    } else {
      try {
	resultEncoding = ResultEncoding.valueOf(params.getResultEncoding());
      } catch (IllegalArgumentException iae) {
	log.warning(DEBUG_HEADER, iae);
	result.setRunnerStatus(HasherStatus.Error);
	errorMessage = "Unknown result encoding: " + params.getResultEncoding()
	    + " - " + iae.getMessage();
	result.setRunnerError(errorMessage);
	return errorMessage;
      }
    }

    result.setResultEncoding(resultEncoding);
    return errorMessage;
  }

  /**
   * Handles other parameters of the hashing operation to be performed.
   * 
   * @param params
   *          A HasherParams with the parameters that define the hashing
   *          operation.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   * @return a String with any error message.
   */
  public String processParams(HasherParams params, HasherResult result) {
    final String DEBUG_HEADER = "processParams(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String errorMessage = null;

    if (StringUtil.isNullString(params.getAlgorithm())) {
      params.setAlgorithm(LcapMessage.getDefaultHashAlgorithm());
    }

    if (result.getAu() == null) {
      errorMessage = processAuIdParam(params, result);
      if (errorMessage != null) {
	if (log.isDebug2())
	  log.debug2(DEBUG_HEADER + "errorMessage = " + errorMessage);
	return errorMessage;
      }
    }

    if (StringUtil.isNullString(params.getUrl())) {
      params.setUrl(AuCachedUrlSetSpec.URL);
    }

    try {
      challenge = decodeBase64Value(params.getChallenge());
      result.setChallenge(challenge);
    } catch (IllegalArgumentException iae) {
      log.warning(DEBUG_HEADER, iae);
      result.setRunnerStatus(HasherStatus.Error);
      errorMessage = "Challenge: Illegal Base64 string: "
	  + params.getChallenge() + iae.getMessage();
      result.setRunnerError(errorMessage);
      return errorMessage;
    }

    try {
      verifier = decodeBase64Value(params.getVerifier());
      result.setVerifier(verifier);
    } catch (IllegalArgumentException iae) {
      log.warning(DEBUG_HEADER, iae);
      result.setRunnerStatus(HasherStatus.Error);
      errorMessage = "Verifier: Illegal Base64 string: " + params.getVerifier()
	  + iae.getMessage();
      result.setRunnerError(errorMessage);
      return errorMessage;
    }

    errorMessage = processCus(params.getAuId(), params.getUrl(),
	params.getLower(), params.getUpper(), result.getHashType(), result);
    if (errorMessage != null) {
	if (log.isDebug2())
	  log.debug2(DEBUG_HEADER + "errorMessage = " + errorMessage);
	return errorMessage;
    }

    if (isV3(result.getHashType()) && result.getBlockFile() == null) {
      try {
	result.setBlockFile(FileUtil.createTempFile("HashCUS", ".tmp"));
      } catch (IOException ioe) {
	log.warning(DEBUG_HEADER, ioe);
	result.setRunnerStatus(HasherStatus.Error);
	errorMessage = "Cannot create block file: " + ioe.getMessage();
	result.setRunnerError(errorMessage);
	return errorMessage;
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "errorMessage = " + errorMessage);
    return errorMessage;
  }

  /**
   * Handles the specification of the Archival Unit to be hashed.
   * 
   * @param params
   *          A HasherParams with the parameters that define the hashing
   *          operation.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   * @return a String with any error message.
   */
  private String processAuIdParam(HasherParams params, HasherResult result) {
    final String DEBUG_HEADER = "processAuIdParam(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String errorMessage = null;

    if (StringUtil.isNullString(params.getAuId())) {
      log.warning(DEBUG_HEADER + "No AU identifer has been specified");
      result.setRunnerStatus(HasherStatus.Error);
      result.setRunnerError("No AU identifer has been specified");
      return "Select an AU";
    }

    ArchivalUnit au = LockssDaemon.getLockssDaemon().getPluginManager()
	.getAuFromId(params.getAuId());

    if (au == null) {
      log.warning(DEBUG_HEADER + "No AU exists with the specified identifier "
	  + params.getAuId());
      result.setRunnerStatus(HasherStatus.Error);
      result.setRunnerError("No AU exists with the specified identifier "
	  + params.getAuId());
      return "No such AU.  Select an AU";
    }

    result.setAu(au);

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "errorMessage = " + errorMessage);
    return errorMessage;
  }

  /**
   * Provides an indication of whether the hashing operation is one of those of
   * version 3.
   * 
   * @param type
   *          A HashType with the hash type to be checked.
   * @return a boolean with the indication.
   */
  public static boolean isV3(HashType type) {
    switch (type) {
    case V3Tree:
    case V3File:
      return true;
    default:
      return false;
    }
  }      

  /**
   * Decodes a Base64-encoded text string.
   * 
   * @param encodedText
   *          A String with the encoded text.
   * @return a byte[] with the decoded bytes.
   */
  byte[] decodeBase64Value(String encodedText) {
    if (encodedText == null) {
      return null;
    }

    return B64Code.decode(encodedText.toCharArray());
  }

  /**
   * Obtains the CachedUrlSet of the hashing operation.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param url
   *          A String with the URL.
   * @param lower
   *          A String with the lower URL.
   * @param upper
   *          A String with the upper URL.
   * @param upper
   *          A String with the upper URL.
   * @param hashType
   *          A HashType with the type of hashing operation to be performed.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   * @return a String with any error message.
   */
  String processCus(String auId, String url, String lower, String upper,
      HashType hashType, HasherResult result) {
    final String DEBUG_HEADER = "processCus(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      log.debug2(DEBUG_HEADER + "url = " + url);
      log.debug2(DEBUG_HEADER + "lower = " + lower);
      log.debug2(DEBUG_HEADER + "upper = " + upper);
      log.debug2(DEBUG_HEADER + "hashType = " + hashType);
      log.debug2(DEBUG_HEADER + "result = " + result);
    }

    if (result.getCus() != null) {
      return null;
    }

    String errorMessage = null;
    PollSpec pollSpec;

    try {
      switch (hashType) {
      case V1File:
	if (upper != null ||
	    (lower != null && !lower.equals(PollSpec.SINGLE_NODE_LWRBOUND))) {
	  errorMessage = "Upper/Lower ignored";
	}
	pollSpec = new PollSpec(auId, url, PollSpec.SINGLE_NODE_LWRBOUND, null,
	    Poll.V1_CONTENT_POLL);
	break;
      case V3Tree:
	pollSpec = new PollSpec(auId, url, lower, upper, Poll.V3_POLL);
	break;
      case V3File:
	pollSpec = new PollSpec(auId, url, PollSpec.SINGLE_NODE_LWRBOUND, null,
	    Poll.V3_POLL);
	break;
      default:
	pollSpec = new PollSpec(auId, url, lower, upper, Poll.V1_CONTENT_POLL);
      }
    } catch (Exception e) {
      errorMessage = "Error making PollSpec: " + e.toString();
      if (log.isDebug()) log.debug("Making Pollspec", e);
      result.setRunnerStatus(HasherStatus.Error);
      result.setRunnerError(errorMessage);
      return errorMessage;
    }

    if (log.isDebug()) log.debug(DEBUG_HEADER + "pollSpec = " + pollSpec);
    result.setCus(pollSpec.getCachedUrlSet());
    if (log.isDebug()) log.debug(DEBUG_HEADER + "cus = " + result.getCus());

    if (result.getCus() == null) {
      errorMessage = "No such CUS: " + pollSpec;
      result.setRunnerStatus(HasherStatus.Error);
      result.setRunnerError(errorMessage);
      return errorMessage;
    }

    return null;
  }

  /**
   * Provides the digest to be used in the hashing operation and it sets up the
   * filtered output stream if necessary.
   * 
   * @param algorithm
   *          A String with the type of digest to be used.
   * @param isRecordFilteredStream
   *          A boolean indicating whether the stream to be hashed is to be
   *          filtered or not.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   * @return a MessageDigest with the constructed digest.
   * @throws IOException
   * @throws NoSuchAlgorithmException
   */
  MessageDigest makeDigestAndRecordStream(String algorithm,
      boolean isRecordFilteredStream, HasherResult result)
	  throws IOException, NoSuchAlgorithmException {
    final String DEBUG_HEADER = "makeDigestAndRecordStream(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "algorithm = " + algorithm);
      log.debug2(DEBUG_HEADER + "isRecordFilteredStream = "
	  + isRecordFilteredStream);
      log.debug2(DEBUG_HEADER + "result = " + result);
    }

    digest = MessageDigest.getInstance(algorithm);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "digest = " + digest);

    if (isRecordFilteredStream) {
      result.setRecordFile(FileUtil.createTempFile("HashCUS", ".tmp"));
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result.getRecordFile() = "
	  + result.getRecordFile());
      OutputStream recordStream = new BufferedOutputStream(
	  new FileOutputStream(result.getRecordFile()));
      long truncateTo = CurrentConfig.getLongParam(
	  PARAM_TRUNCATE_FILTERED_STREAM, DEFAULT_TRUNCATE_FILTERED_STREAM);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "truncateTo = " + truncateTo);
      digest = new RecordingMessageDigest(digest, recordStream, truncateTo);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "digest = " + digest);

      result.setRecordStream(recordStream);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "digest = " + digest);
    return digest;
  }

  /**
   * Performs a version 1 hashing operation.
   * 
   * @param cush
   *          A CachedUrlSetHasher with the hasher.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   * @throws IOException
   */
  void doV1(CachedUrlSetHasher cush, HasherResult result) throws IOException {
    final String DEBUG_HEADER = "doV1(): ";
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cush = " + cush);
    setFiltered(true);
    result.setHashResult(doV1Hash(cush));
    result.setShowResult(true);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done.");
  }
  
  void doV3(String machineName, boolean excludeSuspectVersions,
      HasherResult result) throws IOException {
    doV3(machineName, excludeSuspectVersions, false, result);
  }
  
  
  /**
   * Performs a version 3 hashing operation.
   * 
   * @param machineName A String with the name of the computer.
   * @param excludeSuspectVersions
   *          A boolean indicating whether the to exclude suspect versions or
   *          not.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   * @throws IOException
   */
  void doV3(String machineName, boolean excludeSuspectVersions, boolean includeWeight,
      HasherResult result) throws IOException {
    final String DEBUG_HEADER = "doV3(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "machineName = " + machineName);
      log.debug2(DEBUG_HEADER + "excludeSuspectVersions = "
	  + excludeSuspectVersions);
      log.debug2(DEBUG_HEADER + "result = " + result);
      log.debug2(DEBUG_HEADER + "digest = " + digest);
    }

    StringBuilder sb = new StringBuilder();
    // Pylorus' hash() depends upon the first 20 characters of this string
    sb.append("# Block hashes from " + machineName + ", " +
		formatDateTime(TimeBase.nowMs()) + "\n");
    sb.append("# AU: " + result.getAu().getName() + "\n");
    sb.append("# Hash algorithm: " + digest.getAlgorithm() + "\n");
    sb.append("# Encoding: " + result.getResultEncoding().toString() + "\n");

    if (challenge != null) {
      sb.append("# " + "Poller nonce: "
	  + byteString(challenge, result.getResultEncoding()) + "\n");
    }

    if (verifier != null) {
      sb.append("# " + "Voter nonce: "
	  + byteString(verifier, result.getResultEncoding()) + "\n");
    }

    setFiltered(true);
    setExcludeSuspectVersions(excludeSuspectVersions);
    setIncludeWeight(includeWeight);
    setBase64Result(result.getResultEncoding() == ResultEncoding.Base64);
    doV3Hash(result.getCus(), result.getBlockFile(), sb.toString(), "# end\n");
    result.setShowResult(true);

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Formats a timestamp in a suitable form for display.
   * 
   * @param time
   *          A long with the timestamp.
   * @return A String with the formatted timestamp.
   */
  public static String formatDateTime(long time) {
    return ServletUtil.headerDf.format(new Date(time));
  }

  /**
   * Formats for display purposes arbitrary bytes.
   * 
   * @param theBytes
   *          A byte[] with the bytes to be displayed.
   * @param encoding
   *          A ResultEncoding with the type of format encoding.
   * @return a String with the formatted text.
   */
  public static String byteString(byte[] theBytes, ResultEncoding encoding) {
    switch (encoding) {
    case Base64:
      return String.valueOf(B64Code.encode(theBytes));
    default:
    case Hex:
      return ByteArray.toHexString(theBytes);
    }
  }

  /**
   * Provides an unused identifier for a new asynchronous hashing operation.
   * Must be called from a block synchronized on the requestMap.
   * 
   * @param params
   *          A HasherParams with the parameters that define the hashing
   *          operation.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   * @param requestMap
   *          A Map<String, ParamsAndResult> with existing asynchronous hashing
   *          operations data.
   * @return a String with the requested identifier.
   */
  public static String getReqId(HasherParams params, HasherResult result,
      Map<String, ParamsAndResult> requestMap) {
    for (int i = 0; i < MAX_RANDOM_SEARCH; i++) {
      String key =
	org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(5);
      if (!requestMap.containsKey(key)) {
	result.setRequestId(key);
	requestMap.put(key, new ParamsAndResult(params, result));
	return key;
      }
    }

    throw new IllegalStateException("Couldn't find an unused request key in "
				    + MAX_RANDOM_SEARCH + " tries");
  }

  /**
   * Starts a thread used to perform a hashing operation asynchronously.
   * 
   * @param params
   *          A HasherParams with the parameters that define the hashing
   *          operation.
   * @param result
   *          A HasherResult where to store the result of the hashing operation.
   */
  public void startHashingThread(final HasherParams params,
      final HasherResult result) {
    final String DEBUG_HEADER = "startHashingThread(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "params = " + params);
      log.debug2(DEBUG_HEADER + "result = " + result);
    }

    if (result.getAu() == null) {
      String errorMessage = processAuIdParam(params, result);
      if (errorMessage != null) {
	throw new RuntimeException(errorMessage);
      }
    }

    LockssRunnable runnable =
      new LockssRunnable(AuUtil.getThreadNameFor("HashCUS", result.getAu())) {
	public void lockssRun() {
	  setPriority(CurrentConfig.getIntParam(PARAM_THREADPOOL_PRIORITY,
						DEFAULT_THREADPOOL_PRIORITY));
	  triggerWDogOnExit(true);
	  hash(params, result);
	  triggerWDogOnExit(false);
	  setThreadName("HashCUS: idle");
	  // Ensure we don't leave thread interrupt flag on
	  if (Thread.currentThread().interrupted()) {
	    log.debug("Thread interrupt flag was on when SimpleHasher finished");
	  }
	}

	@Override
	protected void threadExited(Throwable cause) {
	  if (cause instanceof OutOfMemoryError) {
	    // Don't let OOME in asynchronous hash cause daemon exit
	    log.warning("Asynchronous hash threw OOME, not exiting");
	  } else {
	    super.threadExited(cause);
	  }
	}

    };

    result.setRequestTime(TimeBase.nowMs());
    result.setFuture((Future<Void>)(getExecutor().submit(runnable)));
  }

  /**
   * Provides the executor of asynchronous hashing operations.
   * 
   * @return a ThreadPoolExecutor with the executor.
   */
  private static synchronized ThreadPoolExecutor getExecutor() {
    if (EXECUTOR == null) {
      Configuration config = ConfigManager.getCurrentConfig();
      int poolsize = config.getInt(PARAM_THREADPOOL_SIZE,
				   DEFAULT_THREADPOOL_SIZE);
      long keepalive = config.getTimeInterval(PARAM_THREADPOOL_KEEPALIVE,
					      DEFAULT_THREADPOOL_KEEPALIVE);
      EXECUTOR = new ThreadPoolExecutor(poolsize, poolsize,
					keepalive, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());
      EXECUTOR.allowCoreThreadTimeOut(true);
    }
    return EXECUTOR;
  }
}
