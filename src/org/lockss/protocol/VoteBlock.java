/*
 * $Id: VoteBlock.java,v 1.3 2005-09-14 23:57:49 smorabito Exp $
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

import java.io.*;
import java.util.*;

import org.mortbay.util.*;

/**
 * A simple bean representing a V3 vote block -- a file, or part of a file.
 */
public class VoteBlock implements Serializable {
  
  /** Vote type enum.  Is this a vote on content, headers, or metadata? */
  public static final int CONTENT_VOTE = 0;
  public static final int HEADER_VOTE = 1;
  public static final int METADATA_VOTE = 3;
  private static final String[] voteTypeStrings = {
    "Content", "Header", "Metadata"
  };
  
  private int m_pollType;
  private String m_fileName;
  private long m_filteredLength = 0;
  private long m_filteredOffset = 0;
  private long m_unfilteredLength = 0;
  private long m_unfilteredOffset = 0;
  private byte[] m_contentHash;

  public VoteBlock() {}
  
  public VoteBlock(String fileName, long fLength, long fOffset,
		   long uLength, long uOffset, byte[] hash, int pollType) {
    m_fileName = fileName;
    m_filteredLength = fLength;
    m_filteredOffset = fOffset;
    m_unfilteredLength = uLength;
    m_unfilteredOffset = uOffset;
    m_contentHash = hash;
    m_pollType = pollType;
  }

  public String getFileName() {
    return m_fileName;
  }

  public void setFileName(String s) {
    m_fileName = s;
  }

  public long getFilteredLength() {
    return m_filteredLength;
  }

  public void setFilteredLength(long l) {
    m_filteredLength = l;
  }

  public long getFilteredOffset() {
    return m_filteredOffset;
  }

  public void setFilteredOffset(long l) {
    m_filteredOffset = l;
  }

  public long getUnfilteredLength() {
    return m_unfilteredLength;
  }

  public void setUnfilteredLength(long l) {
    m_unfilteredLength = l;
  }

  public long getUnfilteredOffset() {
    return m_unfilteredOffset;
  }

  public void setUnfilteredOffset(long l) {
    m_unfilteredOffset = l;
  }

  public byte[] getHash() {
    return m_contentHash;
  }

  public void setHash(byte[] b) {
    m_contentHash = b;
  }
  
  public void setVoteType(int type) {
    m_pollType = type;
  }
  
  public int getVoteType() {
    return m_pollType;
  }
  
  public String getVoteTypeString() {
    if (m_pollType >= 0 && m_pollType < voteTypeStrings.length) {
      return voteTypeStrings[m_pollType];
    } else {
      return "Unknown";
    }
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("[VoteBlock: ");
    sb.append("vt = " + getVoteTypeString() + ", ");
    sb.append("fn = " + m_fileName + ", ");
    sb.append("fl = " + m_filteredLength + ", ");
    sb.append("fo = " + m_filteredOffset + ", ");
    sb.append("ul = " + m_unfilteredLength + ", ");
    sb.append("uo = " + m_unfilteredOffset + ", ");
    sb.append("ch = " +
	      m_contentHash == null ? "null" : new String(B64Code.encode(m_contentHash))
	      + ", ");
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
      vb.m_pollType == m_pollType &&
      vb.m_filteredLength == m_filteredLength &&
      vb.m_filteredOffset == m_filteredOffset &&
      vb.m_unfilteredLength == m_unfilteredLength &&
      vb.m_unfilteredOffset == m_unfilteredOffset &&
      Arrays.equals(vb.m_contentHash, m_contentHash);
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + m_fileName.hashCode();
    result = 37 * result + m_pollType;
    result = (int)(37 * result + m_filteredLength);
    result = (int)(37 * result + m_filteredOffset);
    result = (int)(37 * result + m_unfilteredLength);
    result = (int)(37 * result + m_unfilteredOffset);
    for (int i = 0; i < m_contentHash.length; i++) {
      result = 37 * result + m_contentHash[i];
    }
    return result;
  }
}
