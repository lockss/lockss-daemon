/*
 * $Id: VoteBlock.java,v 1.1 2005-03-23 07:01:09 smorabito Exp $
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

package org.lockss.protocol;

import java.util.Arrays;
import org.mortbay.util.B64Code;

/**
 * A simple bean representing a V3 vote block -- a file, or part of a file.
 */
public class VoteBlock {
  private String m_fileName;
  private int m_filteredLength = 0;
  private int m_filteredOffset = 0;
  private int m_unfilteredLength = 0;
  private int m_unfilteredOffset = 0;
  private byte[] m_plainHash;
  private byte[] m_challengeHash;
  private byte[] m_proof;

  public VoteBlock() { }

  // Convenience constructor for testing.
  public VoteBlock(String fileName, int fLength, int fOffset,
		   int uLength, int uOffset, byte[] plHash,
		   byte[] chHash, byte[] proof) {
    m_fileName = fileName;
    m_filteredLength = fLength;
    m_filteredOffset = fOffset;
    m_unfilteredLength = uLength;
    m_unfilteredOffset = uOffset;
    m_plainHash = plHash;
    m_challengeHash = chHash;
    m_proof = proof;
  }


  public String getFileName() {
    return m_fileName;
  }

  public void setFileName(String s) {
    m_fileName = s;
  }

  public int getFilteredLength() {
    return m_filteredLength;
  }

  public void setFilteredLength(int i) {
    m_filteredLength = i;
  }

  public int getFilteredOffset() {
    return m_filteredOffset;
  }

  public void setFilteredOffset(int i) {
    m_filteredOffset = i;
  }

  public int getUnfilteredLength() {
    return m_unfilteredLength;
  }

  public void setUnfilteredLength(int i) {
    m_unfilteredLength = i;
  }

  public int getUnfilteredOffset() {
    return m_unfilteredOffset;
  }

  public void setUnfilteredOffset(int i) {
    m_unfilteredOffset = i;
  }

  public byte[] getPlainHash() {
    return m_plainHash;
  }

  public void setPlainHash(byte[] b) {
    m_plainHash = b;
  }

  public byte[] getChallengeHash() {
    return m_challengeHash;
  }

  public void setChallengeHash(byte[] b) {
    m_challengeHash = b;
  }

  public byte[] getProof() {
    return m_proof;
  }

  public void setProof(byte[] b) {
    m_proof = b;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("[VoteBlock: ");
    sb.append("fn = " + m_fileName + ", ");
    sb.append("fl = " + m_filteredLength + ", ");
    sb.append("fo = " + m_filteredOffset + ", ");
    sb.append("ul = " + m_unfilteredLength + ", ");
    sb.append("uo = " + m_unfilteredOffset + ", ");
    sb.append("ph = " +
	      m_plainHash == null ? "null" : new String(B64Code.encode(m_plainHash))
	      + ", ");
    sb.append("ch = " +
	      m_challengeHash == null ? "null" : new String(B64Code.encode(m_challengeHash))
	      + ", ");
    sb.append("pr = " +
	      m_proof == null ? "null" : new String(B64Code.encode(m_proof))
	      + " ]");
    return sb.toString();
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof VoteBlock)) {
      return false;
    }

    VoteBlock vb = (VoteBlock)o;
    return vb.m_fileName.equals(m_fileName) &&
      vb.m_filteredLength == m_filteredLength &&
      vb.m_filteredOffset == m_filteredOffset &&
      vb.m_unfilteredLength == m_unfilteredLength &&
      vb.m_unfilteredOffset == m_unfilteredOffset &&
      Arrays.equals(vb.m_plainHash, m_plainHash) &&
      Arrays.equals(vb.m_challengeHash, m_challengeHash) &&
      Arrays.equals(vb.m_proof, m_proof);
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + m_fileName.hashCode();
    result = 37 * result + m_filteredLength;
    result = 37 * result + m_filteredOffset;
    result = 37 * result + m_unfilteredLength;
    result = 37 * result + m_unfilteredOffset;
    for (int i = 0; i < m_plainHash.length; i++) {
      result = 37 * result + m_plainHash[i];
    }
    for (int i = 0; i < m_challengeHash.length; i++) {
      result = 37 * result + m_challengeHash[i];
    }
    for (int i = 0; i < m_proof.length; i++) {
      result = 37 * result + m_proof[i];
    }
    return result;
  }
}
