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
package org.lockss.ws.entities;

/**
 * A wrapper for the parameters used to perform a hash operation.
 */
public class HasherWsParams {
  private String auId;
  private String url;
  private String lower;
  private String upper;
  private Boolean recordFilteredStream;
  private Boolean excludeSuspectVersions;
  private String algorithm;
  private String hashType;
  private String resultEncoding;
  private String challenge;
  private String verifier;

  /**
   * Provides the identifier of Archival Unit to be hashed.
   * 
   * @return a String with the identifier.
   */
  public String getAuId() {
    return auId;
  }
  public void setAuId(String auId) {
    this.auId = auId;
  }

  /**
   * Provides the URL to be hashed.
   * 
   * @return a String with the URL.
   */
  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Provides the lower boundary URL.
   * 
   * @return a String with the URL.
   */
  public String getLower() {
    return lower;
  }
  public void setLower(String lower) {
    this.lower = lower;
  }

  /**
   * Provides the upper boundary URL.
   * 
   * @return a String with the URL.
   */
  public String getUpper() {
    return upper;
  }
  public void setUpper(String upper) {
    this.upper = upper;
  }

  /**
   * Provides an indication of whether the filtered stream should be recorded.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean isRecordFilteredStream() {
    return recordFilteredStream;
  }
  public void setRecordFilteredStream(Boolean recordFilteredStream) {
    this.recordFilteredStream = recordFilteredStream;
  }

  /**
   * Provides an indication of whether to exxclude suspect versions.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean isExcludeSuspectVersions() {
    return excludeSuspectVersions;
  }
  public void setExcludeSuspectVersions(Boolean excludeSuspectVersions) {
    this.excludeSuspectVersions = excludeSuspectVersions;
  }

  /**
   * Provides the name of the hashing algorithm to be used. <br />
   * The acceptable values are SHA-1 (or SHA1), MD5 and SHA-256.
   * 
   * @return a String with the hashing algorithm name.
   */
  public String getAlgorithm() {
    return algorithm;
  }
  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  /**
   * Provides the name of the type of hashing to be performed. <br />
   * The acceptable values are V1Content, V1Name, V1File, V3Tree and V3File.
   * 
   * @return a String with the hashing type name.
   */
  public String getHashType() {
    return hashType;
  }
  public void setHashType(String hashType) {
    this.hashType = hashType;
  }

  /**
   * Provides the name of the result encoding to be used. <br />
   * The acceptable values are Base64 and Hex.
   * 
   * @return a String with the identifier result encoding name.
   */
  public String getResultEncoding() {
    return resultEncoding;
  }
  public void setResultEncoding(String resultEncoding) {
    this.resultEncoding = resultEncoding;
  }

  /**
   * Provides the encoded challenge.
   * 
   * @return a String with the encoded challenge.
   */
  public String getChallenge() {
    return challenge;
  }
  public void setChallenge(String challenge) {
    this.challenge = challenge;
  }

  /**
   * Provides the encoded verifier.
   * 
   * @return a String with the encoded verifier.
   */
  public String getVerifier() {
    return verifier;
  }
  public void setVerifier(String verifier) {
    this.verifier = verifier;
  }
  @Override
  public String toString() {
    return "[HasherWsParams: auId=" + auId + ", url=" + url + ", lower=" + lower
	+ ", upper=" + upper + ", recordFilteredStream=" + recordFilteredStream
	+ ", excludeSuspectVersions=" + excludeSuspectVersions + ", algorithm="
	+ algorithm + ", hashType=" + hashType + ", resultEncoding="
	+ resultEncoding + ", challenge=" + challenge + ", verifier="
	+ verifier + "]";
  }
}
