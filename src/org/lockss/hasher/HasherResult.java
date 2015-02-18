/*
 * $Id$
 */

/*

Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.Future;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.hasher.SimpleHasher.HasherStatus;
import org.lockss.hasher.SimpleHasher.HashType;
import org.lockss.hasher.SimpleHasher.ResultEncoding;

public class HasherResult {

  private HashType hashType;
  private ResultEncoding resultEncoding;
  private ArchivalUnit au;
  private byte[] challenge;
  private byte[] verifier;
  private CachedUrlSet cus;
  private String requestId;
  private boolean showResult = false;
  private long requestTime;
  private File recordFile;
  private OutputStream recordStream;
  private File blockFile;
  private Future<Void> future;
  private String runnerError;
  private HasherStatus runnerStatus = HasherStatus.NotStarted;
  private byte[] hashResult;
  private long bytesHashed;
  private int filesHashed;
  private long startTime;
  private long elapsedTime;

  public HashType getHashType() {
    return hashType;
  }

  public void setHashType(HashType hashType) {
    this.hashType = hashType;
  }

  public ResultEncoding getResultEncoding() {
    return resultEncoding;
  }

  public void setResultEncoding(ResultEncoding resultEncoding) {
    this.resultEncoding = resultEncoding;
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public void setAu(ArchivalUnit au) {
    this.au = au;
  }

  public byte[] getChallenge() {
    return challenge;
  }

  public void setChallenge(byte[] challenge) {
    this.challenge = challenge;
  }

  public byte[] getVerifier() {
    return verifier;
  }

  public void setVerifier(byte[] verifier) {
    this.verifier = verifier;
  }

  public CachedUrlSet getCus() {
    return cus;
  }

  public void setCus(CachedUrlSet cus) {
    this.cus = cus;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public boolean isShowResult() {
    return showResult;
  }

  public void setShowResult(boolean showResult) {
    this.showResult = showResult;
  }

  public long getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(long requestTime) {
    this.requestTime = requestTime;
  }

  public File getRecordFile() {
    return recordFile;
  }

  public void setRecordFile(File recordFile) {
    this.recordFile = recordFile;
  }

  public OutputStream getRecordStream() {
    return recordStream;
  }

  public void setRecordStream(OutputStream recordStream) {
    this.recordStream = recordStream;
  }

  public File getBlockFile() {
    return blockFile;
  }

  public void setBlockFile(File blockFile) {
    this.blockFile = blockFile;
  }

  public Future<Void> getFuture() {
    return future;
  }

  public void setFuture(Future<Void> future) {
    this.future = future;
  }

  public String getRunnerError() {
    return runnerError;
  }

  public void setRunnerError(String runnerError) {
    this.runnerError = runnerError;
  }

  public HasherStatus getRunnerStatus() {
    return runnerStatus;
  }

  public void setRunnerStatus(HasherStatus runnerStatus) {
    this.runnerStatus = runnerStatus;
  }

  public byte[] getHashResult() {
    return hashResult;
  }

  public void setHashResult(byte[] hashResult) {
    this.hashResult = hashResult;
  }

  public long getBytesHashed() {
    return bytesHashed;
  }

  public void setBytesHashed(long bytesHashed) {
    this.bytesHashed = bytesHashed;
  }

  public int getFilesHashed() {
    return filesHashed;
  }

  public void setFilesHashed(int filesHashed) {
    this.filesHashed = filesHashed;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  public void setElapsedTime(long elapsedTime) {
    this.elapsedTime = elapsedTime;
  }

  public void copyFrom(HasherResult source) {
    setHashType(source.getHashType());
    setResultEncoding(source.getResultEncoding());
    setAu(source.getAu());
    setChallenge(source.getChallenge());
    setVerifier(source.getVerifier());
    setCus(source.getCus());
    setRequestId(source.getRequestId());
    setShowResult(source.isShowResult());
    setRequestTime(source.getRequestTime());
    setRecordFile(source.getRecordFile());
    setBlockFile(source.getBlockFile());
    setFuture(source.getFuture());
    setRunnerError(source.getRunnerError());
    setRunnerStatus(source.getRunnerStatus());
    setHashResult(source.getHashResult());
    setBytesHashed(source.getBytesHashed());
    setFilesHashed(source.getFilesHashed());
    setStartTime(source.getStartTime());
    setElapsedTime(source.getElapsedTime());
  }

  @Override
  public String toString() {
    return "[HasherResult: hashType=" + hashType + ", resultEncoding="
	+ resultEncoding + ", au=" + au + ", challenge="
	+ Arrays.toString(challenge) + ", verifier=" + Arrays.toString(verifier)
	+ ", cus=" + cus + ", requestId=" + requestId + ", showResult="
	+ showResult + ", requestTime=" + requestTime + ", recordFile="
	+ recordFile + ", recordStream=" + recordStream + ", blockFile="
	+ blockFile + ", future=" + future + ", runnerError=" + runnerError
	+ ", runnerStatus=" + runnerStatus + ", hashResult="
	+ Arrays.toString(hashResult) + ", bytesHashed=" + bytesHashed
	+ ", filesHashed=" + filesHashed + ", startTime=" + startTime
	+ ", elapsedTime=" + elapsedTime + "]";
  }
}
