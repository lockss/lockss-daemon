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

public class HasherParams {
  private String machineName;
  private boolean asynchronous;
  private String auId;
  private String url;
  private String lower;
  private String upper;
  private boolean recordFilteredStream;
  private boolean excludeSuspectVersions;
  private String algorithm;
  private String hashType;
  private String resultEncoding;
  private String challenge;
  private String verifier;

  public HasherParams(String machineName, boolean asynchronous) {
    this.machineName = machineName;
    this.asynchronous = asynchronous;
  }

  public String getMachineName() {
    return machineName;
  }

  public void setMachineName(String machineName) {
    this.machineName = machineName;
  }

  public boolean isAsynchronous() {
    return asynchronous;
  }

  public void setAsynchronous(boolean asynchronous) {
    this.asynchronous = asynchronous;
  }

  public String getAuId() {
    return auId;
  }

  public void setAuId(String auId) {
    this.auId = auId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getLower() {
    return lower;
  }

  public void setLower(String lower) {
    this.lower = lower;
  }

  public String getUpper() {
    return upper;
  }

  public void setUpper(String upper) {
    this.upper = upper;
  }

  public boolean isRecordFilteredStream() {
    return recordFilteredStream;
  }

  public void setRecordFilteredStream(boolean recordFilteredStream) {
    this.recordFilteredStream = recordFilteredStream;
  }

  public boolean isExcludeSuspectVersions() {
    return excludeSuspectVersions;
  }

  public void setExcludeSuspectVersions(boolean excludeSuspectVersions) {
    this.excludeSuspectVersions = excludeSuspectVersions;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  public String getHashType() {
    return hashType;
  }

  public void setHashType(String hashType) {
    this.hashType = hashType;
  }

  public String getResultEncoding() {
    return resultEncoding;
  }

  public void setResultEncoding(String resultEncoding) {
    this.resultEncoding = resultEncoding;
  }

  public String getChallenge() {
    return challenge;
  }

  public void setChallenge(String challenge) {
    this.challenge = challenge;
  }

  public String getVerifier() {
    return verifier;
  }

  public void setVerifier(String verifier) {
    this.verifier = verifier;
  }

  @Override
  public String toString() {
    return "[HasherParams: machineName=" + machineName + ", asynchronous="
	+ asynchronous + ", auId=" + auId + ", url=" + url + ", lower=" + lower
	+ ", upper=" + upper + ", recordFilteredStream=" + recordFilteredStream
	+ ", excludeSuspectVersions="	+ excludeSuspectVersions
	+ ", algorithm=" + algorithm + ", hashType=" + hashType
	+ ", resultEncoding=" + resultEncoding + ", challenge=" + challenge
	+ ", verifier=" + verifier + "]";
  }
}
