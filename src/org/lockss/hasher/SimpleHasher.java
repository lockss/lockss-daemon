/*
 * $Id: SimpleHasher.java,v 1.8 2010-05-12 03:52:37 tlipkis Exp $
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

package org.lockss.hasher;

import java.io.*;
import java.security.*;
import org.mortbay.util.B64Code;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;

/** Utilitiy class to hash a CUS as a single batch and record the results
 * in a file.  By default the content is *not* filtered; use setFiltered()
 * to change.
 */
public class SimpleHasher {
  static Logger log = Logger.getLogger("SimpleHasher");

  private MessageDigest digest;
  private byte[] challenge;
  private byte[] verifier;
  private boolean isFiltered = false;
  private boolean isIncludeUrl = false;
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
    PrintStream blockOuts =
      new PrintStream(new BufferedOutputStream(new FileOutputStream(blockFile)));
    if (header != null) {
      blockOuts.println(header);
    }
    BlockHasher hasher = new BlockHasher(cus, 1,
					 initHasherDigests(),
					 initHasherByteArrays(),
					 new BlockEventHandler(blockOuts));

    hasher.setIncludeUrl(isIncludeUrl);
    doHash(hasher);
    if (footer != null) {
      blockOuts.println(footer);
    }
    blockOuts.close();
  }

  private void initDigest(MessageDigest digest) {
    if (challenge != null) {
      digest.update(challenge, 0, challenge.length);
    }
    if (verifier != null) {
      digest.update(verifier, 0, verifier.length);
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

  private void doHash(CachedUrlSetHasher cush) throws IOException {
    cush.setFiltered(isFiltered);
    bytesHashed = 0;
    filesHashed = 0;
    long startTime = TimeBase.nowMs();
    while (!cush.finished()) {
      bytesHashed += cush.hashStep(nbytes);
    }
    elapsedTime = TimeBase.msSince(startTime);
  }

  private MessageDigest[] initHasherDigests() {
    MessageDigest[] digests = new MessageDigest[1];
    digests[0] = digest;
    return digests;
  }

  String byteString(byte[] a) {
    if (isBase64) {
      return String.valueOf(B64Code.encode(a));
    } else {
      return ByteArray.toHexString(a);
    }
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
      if (ver.getHashError() != null) {
	// Pylorus' diff() depends upon the first 20 characters of this string
	outs.println("Hash error (see log)        " + block.getUrl());
      } else {
	outs.println(byteString(ver.getHashes()[0]) + "   " + block.getUrl());
      }
    }
  }

}
