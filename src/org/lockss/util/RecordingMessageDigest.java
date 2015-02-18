/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;
import java.io.*;
import java.util.*;
import java.security.*;

/** Wrapper for a MessageDigest that writes the digested bytes to a file */
public class RecordingMessageDigest
  extends MessageDigest implements Cloneable {

  MessageDigest dig;
  String fileName = null;
  OutputStream out;
  long maxLen = -1;
  long bytes = 0;

  /** Create a message digest that records in the specified stream */
  public RecordingMessageDigest(MessageDigest wrapped, OutputStream out,
				long maxLen)
      throws FileNotFoundException {
    super(wrapped.getAlgorithm());
    dig = wrapped;
    this.out = out;
    this.maxLen = maxLen;
  }

  /** Create a message digest that records in the specified stream */
  public RecordingMessageDigest(MessageDigest wrapped, OutputStream out)
      throws FileNotFoundException {
    this(wrapped, out, -1);
  }

  /** Create a message digest that records in the specified file */
  public RecordingMessageDigest(MessageDigest wrapped, File file)
      throws FileNotFoundException {
    this(wrapped, file, -1);
  }

  /** Create a message digest that records in the specified file, up to
   * maxLen bytes */
  public RecordingMessageDigest(MessageDigest wrapped, File file, long maxLen)
      throws FileNotFoundException {
    this(wrapped, new BufferedOutputStream(new FileOutputStream(file)),
	 maxLen);
    this.fileName = file.getName();
  }

  /** Create a message digest that records in the specified file */
  public RecordingMessageDigest(MessageDigest wrapped, String fileName)
      throws FileNotFoundException {
    this(wrapped, fileName, -1);
  }

  /** Create a message digest that records in the specified file, up to
   * maxLen bytes */
  public RecordingMessageDigest(MessageDigest wrapped, String fileName,
				long maxLen)
      throws FileNotFoundException {
    this(wrapped, new File(fileName), maxLen);
  }

  protected void engineUpdate(byte input) {
    try {
      if (maxLen < 0 || bytes < maxLen) {
	out.write(input);
	bytes += 1;
      }
    } catch (IOException e) {
      throw new RuntimeException("Error recording digest: " + e.toString());
    }
    dig.update(input);
  }

  protected void engineUpdate(byte[] input, int offset, int len){
    try {
      int outlen = maxLen < 0 ? len : Math.min(len, (int)(maxLen - bytes));
      if (outlen > 0) {
	out.write(input, offset, outlen);
	bytes += outlen;
      }
    } catch (IOException e) {
      throw new RuntimeException("Error recording digest: " + e.toString());
    }
    dig.update(input, offset, len);
  }

  public String toString() {
    return "[RecordingDigest: "+ fileName +"]";
  }

  protected void engineReset() {
    dig.reset();
  }

  protected byte[] engineDigest() {
    closeRecord();
    return dig.digest();
  }

  protected int engineDigest(byte[] buf, int offset, int len)
      throws DigestException {
    closeRecord();
    return dig.digest(buf, offset, len);
  }

  private void closeRecord() {
    // close stream only if we opened it
    if (fileName != null) {
      try {
	out.close();
      } catch (IOException e) {
	throw new RuntimeException("Error closing digest record: " + e);
      }
    }
  }
}
